CREATE TABLE order_items
(
    id                           BIGSERIAL PRIMARY KEY,

    order_id                     BIGINT NOT NULL
        REFERENCES orders(id)
            ON DELETE CASCADE,

    product_id                   BIGINT NOT NULL
        REFERENCES products(id),

    product_name_at_purchase     VARCHAR(255) NOT NULL,

    quantity                     INTEGER NOT NULL,

    unit_price_at_purchase       NUMERIC(12,2) NOT NULL,

    subtotal                     NUMERIC(12,2) NOT NULL,

    CONSTRAINT chk_order_item_quantity
        CHECK (quantity > 0),

    CONSTRAINT chk_order_item_price
        CHECK (unit_price_at_purchase >= 0),

    CONSTRAINT chk_order_item_subtotal
        CHECK (subtotal >= 0)
);

CREATE INDEX idx_order_items_order
    ON order_items(order_id);

CREATE INDEX idx_order_items_product
    ON order_items(product_id);