# SEDSP — ERD (Entity Relationship Diagram)

> **Tách biệt với schema:** Mô tả cột chi tiết xem [`DATABASE_SCHEMA.md`](./DATABASE_SCHEMA.md).

## Sơ đồ quan hệ lõi (Mermaid)

```mermaid
erDiagram
    roles ||--o{ users : "role_id"
    users ||--o{ products : "seller_id"
    categories ||--o{ products : "category_id"
    products ||--o{ product_images : "product_id"
    products ||--o| inventory : "product_id"
    products ||--o{ price_history : "product_id"
    users ||--o{ orders : "user_id"
    orders ||--o{ order_items : "order_id"
    products ||--o{ order_items : "product_id"
    orders ||--o{ payments : "order_id"
    products ||--o{ demand_predictions : "product_id"
    users ||--o{ demand_predictions : "generated_by"

    roles {
        bigint id PK
        varchar name UK
    }

    users {
        bigint id PK
        varchar email UK_partial
        varchar username UK_partial
        bigint role_id FK
        timestamp deleted_at
    }

    products {
        bigint id PK
        bigint seller_id FK
        bigint category_id FK
        numeric price
        numeric cost_price
        timestamp deleted_at
    }

    orders {
        bigint id PK
        bigint user_id FK
        numeric shipping_fee
        enum status
    }

    order_items {
        bigint id PK
        bigint order_id FK
        bigint product_id FK
        int quantity
    }

    demand_predictions {
        bigint id PK
        bigint product_id FK
        int historical_days
        int forecast_period
        numeric predicted_quantity
    }
```

## Luồng dữ liệu DSS

```mermaid
flowchart LR
    OI[order_items] --> DP[DemandPredictionService]
    PH[price_history] --> PP[PricePredictionService]
    P[products.cost_price] --> PC[DssProfitCalculator]
    DP --> WI[WhatIf / Target scenarios]
    PP --> WI
    PC --> UI[DSS UI explainability]
```

## Ghi chú trình bày báo cáo

1. **Slide/word:** dùng ERD (diagram) riêng — không chèn bảng cột dài vào cùng hình.
2. **Phụ lục:** dùng `DATABASE_SCHEMA.md` cho PK/FK/constraint.
3. PK = khóa chính, FK = khóa ngoại, UK_partial = unique có điều kiện `deleted_at IS NULL`.
