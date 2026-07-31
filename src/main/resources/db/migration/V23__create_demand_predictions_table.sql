CREATE TABLE demand_predictions
(
    prediction_id        BIGSERIAL PRIMARY KEY,

    product_id           BIGINT         NOT NULL
        REFERENCES products (id),

    historical_days      INTEGER        NOT NULL,
    forecast_period      INTEGER        NOT NULL,
    average_daily_demand NUMERIC(19, 2) NOT NULL,
    predicted_quantity   NUMERIC(19, 2) NOT NULL,

    generated_by         BIGINT         NOT NULL
        REFERENCES users (id),

    created_at           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_demand_prediction_historical_days
        CHECK (historical_days > 0),

    CONSTRAINT chk_demand_prediction_forecast_period
        CHECK (forecast_period > 0),

    CONSTRAINT chk_demand_prediction_average
        CHECK (average_daily_demand >= 0),

    CONSTRAINT chk_demand_prediction_quantity
        CHECK (predicted_quantity >= 0)
);

CREATE INDEX idx_demand_predictions_product
    ON demand_predictions (product_id);

CREATE INDEX idx_demand_predictions_generated_by
    ON demand_predictions (generated_by);

CREATE INDEX idx_demand_predictions_created_at
    ON demand_predictions (created_at);
