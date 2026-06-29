CREATE TABLE orders
(
    id               BIGSERIAL PRIMARY KEY,

    user_id          BIGINT         NOT NULL
        REFERENCES users (id),

    subtotal_amount  NUMERIC(12, 2) NOT NULL DEFAULT 0,
    shipping_fee     NUMERIC(12, 2) NOT NULL DEFAULT 0,
    discount_amount  NUMERIC(12, 2) NOT NULL DEFAULT 0,

    total_amount     NUMERIC(12, 2) NOT NULL,

    status           order_status            DEFAULT 'PENDING'::order_status,

    shipping_address TEXT           NOT NULL,

    created_at       TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_order_subtotal
        CHECK (subtotal_amount >= 0),

    CONSTRAINT chk_order_shipping
        CHECK (shipping_fee >= 0),

    CONSTRAINT chk_order_discount
        CHECK (discount_amount >= 0),

    CONSTRAINT chk_order_total
        CHECK (total_amount >= 0),

    CONSTRAINT chk_order_total_correct
        CHECK (
            total_amount =
            subtotal_amount + shipping_fee - discount_amount
            )
);

CREATE INDEX idx_orders_user
    ON orders (user_id);

CREATE INDEX idx_orders_status
    ON orders (status);