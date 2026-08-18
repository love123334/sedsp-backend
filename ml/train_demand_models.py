#!/usr/bin/env python3
"""Train one global LightGBM demand model for all eligible products."""

from __future__ import annotations

import argparse
import json
import math
import os
import sys
from dataclasses import dataclass
from datetime import date
from pathlib import Path
from typing import Sequence

import lightgbm as lgb
import numpy as np
import onnxruntime as ort
import pandas as pd
import psycopg
from onnxmltools import convert_lightgbm
from onnxmltools.convert.common.data_types import FloatTensorType
from sklearn.metrics import mean_absolute_error, mean_squared_error


FEATURE_NAMES = [
    "history_days", "day_of_week", "day_of_month", "month", "is_weekend",
    "lag_1", "lag_7", "rolling_mean_7", "rolling_mean_14",
    "rolling_mean_30", "rolling_std_7", "momentum_7", "trend_slope_14",
]
LOOKBACK_OPTIONS = (7, 14, 30, 60, 90, 120, 180)


@dataclass(frozen=True)
class ProductSeries:
    product_id: int
    product_name: str
    dates: pd.DatetimeIndex
    quantities: np.ndarray


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Train a global LightGBM model from DELIVERED orders."
    )
    parser.add_argument("--product-id", type=int, action="append", dest="product_ids")
    parser.add_argument("--min-days", type=int, default=60)
    parser.add_argument("--output-dir", type=Path, default=Path("models/demand"))
    return parser.parse_args()


def database_connection() -> psycopg.Connection:
    database_url = os.getenv("DATABASE_URL")
    if database_url:
        return psycopg.connect(database_url)
    return psycopg.connect(
        host=os.getenv("PGHOST", "localhost"),
        port=int(os.getenv("PGPORT", "5432")),
        dbname=os.getenv("PGDATABASE", "sedsp"),
        user=os.getenv("PGUSER", "postgres"),
        password=os.getenv("PGPASSWORD", "123456"),
    )


def load_product_series(
    connection: psycopg.Connection,
    product_ids: Sequence[int] | None,
) -> list[ProductSeries]:
    product_filter = ""
    parameters: list[object] = []
    if product_ids:
        product_filter = "AND p.id = ANY(%s)"
        parameters.append(list(product_ids))
    query = f"""
        SELECT p.id AS product_id, p.name AS product_name,
               CAST(o.created_at AS DATE) AS sale_date,
               SUM(oi.quantity)::BIGINT AS quantity
        FROM products p
        JOIN order_items oi ON oi.product_id = p.id
        JOIN orders o ON o.id = oi.order_id
        WHERE p.deleted_at IS NULL
          AND p.status = 'ACTIVE'
          AND o.status = 'DELIVERED'
          {product_filter}
        GROUP BY p.id, p.name, CAST(o.created_at AS DATE)
        ORDER BY p.id, sale_date
    """
    with connection.cursor() as cursor:
        cursor.execute(query, parameters)
        columns = [column.name for column in cursor.description]
        frame = pd.DataFrame(cursor.fetchall(), columns=columns)
    if frame.empty:
        return []
    frame["sale_date"] = pd.to_datetime(frame["sale_date"])
    result: list[ProductSeries] = []
    for product_id, rows in frame.groupby("product_id"):
        rows = rows.sort_values("sale_date")
        end_date = max(rows["sale_date"].max(), pd.Timestamp.now().normalize())
        full_dates = pd.date_range(rows["sale_date"].min(), end_date, freq="D")
        daily = (
            rows.set_index("sale_date")["quantity"]
            .reindex(full_dates, fill_value=0)
            .astype(float)
        )
        result.append(ProductSeries(
            product_id=int(product_id),
            product_name=str(rows.iloc[0]["product_name"]),
            dates=full_dates,
            quantities=daily.to_numpy(dtype=np.float32),
        ))
    return result


def tail(values: np.ndarray, window: int) -> np.ndarray:
    return values[-min(window, len(values)):] if len(values) else values


def average(values: np.ndarray, window: int) -> float:
    selected = tail(values, window)
    return float(selected.mean()) if len(selected) else 0.0


def previous_average(values: np.ndarray, window: int) -> float:
    end = max(0, len(values) - window)
    if end == 0:
        return average(values, window)
    selected = values[max(0, end - window):end]
    return float(selected.mean()) if len(selected) else 0.0


def slope(values: np.ndarray, window: int) -> float:
    selected = tail(values, window).astype(float)
    if len(selected) < 2:
        return 0.0
    x = np.arange(1, len(selected) + 1, dtype=float)
    return float(np.polyfit(x, selected, 1)[0])


def build_features(
    target_date: pd.Timestamp | date,
    complete_history: np.ndarray,
    history_days: int,
) -> list[float]:
    history = tail(complete_history, history_days)
    recent_average = average(history, 7)
    target = pd.Timestamp(target_date)
    return [
        float(history_days), float(target.isoweekday()), float(target.day),
        float(target.month), 1.0 if target.isoweekday() >= 6 else 0.0,
        float(history[-1]) if len(history) >= 1 else 0.0,
        float(history[-7]) if len(history) >= 7 else 0.0,
        recent_average, average(history, 14), average(history, 30),
        float(tail(history, 7).std(ddof=0)) if len(history) else 0.0,
        recent_average - previous_average(history, 7), slope(history, 14),
    ]


def build_training_frame(series: ProductSeries) -> pd.DataFrame:
    rows: list[dict[str, float | pd.Timestamp]] = []
    for target_index in range(7, len(series.quantities)):
        target_date = series.dates[target_index]
        history = series.quantities[:target_index]
        for lookback in LOOKBACK_OPTIONS:
            if target_index < lookback and lookback != 180:
                continue
            values = build_features(target_date, history, lookback)
            row = dict(zip(FEATURE_NAMES, values, strict=True))
            row["target"] = float(series.quantities[target_index])
            row["target_date"] = target_date
            rows.append(row)
    return pd.DataFrame(rows)


def split_product_frame(
    series: ProductSeries,
) -> tuple[pd.DataFrame, pd.DataFrame]:
    frame = build_training_frame(series)
    if frame.empty:
        raise ValueError(f"Product {series.product_id} has no training samples")
    unique_dates = sorted(frame["target_date"].unique())
    cutoff = max(1, int(len(unique_dates) * 0.8))
    validation_dates = set(unique_dates[cutoff:])
    validation_mask = frame["target_date"].isin(validation_dates)
    train = frame.loc[~validation_mask].copy()
    validation = frame.loc[validation_mask].copy()
    if train.empty or validation.empty:
        raise ValueError("Not enough chronological samples for train/validation split")
    train["product_id"] = series.product_id
    validation["product_id"] = series.product_id
    return train, validation


def train_global(
    products: Sequence[ProductSeries],
    output_dir: Path,
) -> dict[str, object]:
    train_parts: list[pd.DataFrame] = []
    validation_parts: list[pd.DataFrame] = []
    for series in products:
        train, validation = split_product_frame(series)
        train_parts.append(train)
        validation_parts.append(validation)

    train = pd.concat(train_parts, ignore_index=True)
    validation = pd.concat(validation_parts, ignore_index=True)

    model = lgb.LGBMRegressor(
        objective="regression_l1", n_estimators=400, learning_rate=0.03,
        num_leaves=15, max_depth=5, min_child_samples=5, subsample=0.9,
        colsample_bytree=0.9, reg_lambda=0.2, random_state=42, verbosity=-1,
    )
    model.fit(
        train[FEATURE_NAMES], train["target"],
        eval_set=[(validation[FEATURE_NAMES], validation["target"])],
        callbacks=[lgb.early_stopping(30, verbose=False)],
    )
    predictions = np.maximum(0.0, model.predict(validation[FEATURE_NAMES]))
    actual = validation["target"].to_numpy(dtype=float)
    metrics = {
        "mae": round(float(mean_absolute_error(actual, predictions)), 4),
        "rmse": round(float(math.sqrt(mean_squared_error(actual, predictions))), 4),
        "mapePercent": round(float(
            np.mean(np.abs(actual - predictions) / np.maximum(actual, 1.0)) * 100
        ), 4),
    }

    output_dir.mkdir(parents=True, exist_ok=True)
    model_path = output_dir / "global-demand.onnx"
    onnx_model = convert_lightgbm(
        model,
        initial_types=[("features", FloatTensorType([None, len(FEATURE_NAMES)]))],
        target_opset=15,
    )
    model_path.write_bytes(onnx_model.SerializeToString())
    runtime = ort.InferenceSession(str(model_path), providers=["CPUExecutionProvider"])
    sample_frame = validation[FEATURE_NAMES].head(10)
    sample = sample_frame.to_numpy(dtype=np.float32)
    onnx_values = np.asarray(
        runtime.run(None, {runtime.get_inputs()[0].name: sample})[0]
    ).reshape(-1)
    native_values = np.asarray(model.predict(sample_frame)).reshape(-1)
    metrics["maxOnnxPredictionDelta"] = round(
        float(np.max(np.abs(onnx_values - native_values))), 6
    )

    metadata: dict[str, object] = {
        "scope": "global",
        "productCount": len(products),
        "productIds": [series.product_id for series in products],
        "products": [
            {"productId": series.product_id, "productName": series.product_name}
            for series in products
        ],
        "modelType": "LightGBMRegressor",
        "format": "ONNX",
        "featureNames": FEATURE_NAMES,
        "lookbackOptions": list(LOOKBACK_OPTIONS),
        "historyStart": min(series.dates.min() for series in products).date().isoformat(),
        "historyEnd": max(series.dates.max() for series in products).date().isoformat(),
        "trainingRows": len(train),
        "validationRows": len(validation),
        "bestIteration": int(model.best_iteration_ or model.n_estimators),
        "metrics": metrics,
    }
    (output_dir / "global-demand.json").write_text(
        json.dumps(metadata, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    return metadata


def main() -> None:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    args = parse_args()
    with database_connection() as connection:
        products = load_product_series(connection, args.product_ids)
    eligible = [item for item in products if len(item.dates) >= args.min_days]
    if not eligible:
        raise SystemExit("No eligible products. Check DB connection, IDs and sales history.")
    summary = train_global(eligible, args.output_dir)
    print(
        f"Trained global model from {summary['productCount']} products: "
        f"MAE={summary['metrics']['mae']}"
    )
    manifest = {
        "generatedAt": pd.Timestamp.now(tz="Asia/Ho_Chi_Minh").isoformat(),
        "model": summary,
    }
    (args.output_dir / "manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8"
    )


if __name__ == "__main__":
    main()
