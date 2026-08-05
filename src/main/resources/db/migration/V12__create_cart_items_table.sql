CREATE TABLE cart_items
(
    id         BIGSERIAL PRIMARY KEY,

    cart_id    BIGINT    NOT NULL
        REFERENCES carts (id),

    product_id BIGINT    NOT NULL
        REFERENCES products (id),

    quantity   INTEGER   NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,

    CONSTRAINT chk_cart_item_quantity
        CHECK (quantity > 0),

    CONSTRAINT uq_cart_product
        UNIQUE (cart_id, product_id)
);

CREATE INDEX idx_cart_items_cart
    ON cart_items (cart_id);

CREATE INDEX idx_cart_items_product
    ON cart_items (product_id);