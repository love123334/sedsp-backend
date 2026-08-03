CREATE TABLE orders
(
    id               BIGSERIAL PRIMARY KEY,

    user_id          BIGINT         NOT NULL
        REFERENCES users (id)
            ON DELETE RESTRICT
            ON UPDATE CASCADE,

    subtotal_amount  NUMERIC(12, 2) NOT NULL DEFAULT 0,
    shipping_fee     NUMERIC(12, 2) NOT NULL DEFAULT 0,
    discount_amount  NUMERIC(12, 2) NOT NULL DEFAULT 0,

    total_amount     NUMERIC(12, 2) NOT NULL,

    status           order_status            DEFAULT 'PENDING'::order_status,

    shipping_address TEXT           NOT NULL,

    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),

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
            ROUND(total_amount, 2) =
            ROUND(subtotal_amount + shipping_fee - discount_amount, 2)
            )
);

CREATE INDEX idx_orders_user
    ON orders (user_id);

CREATE INDEX idx_orders_status
    ON orders (status);

CREATE INDEX idx_orders_user_status_created
    ON orders (user_id, status, created_at DESC);