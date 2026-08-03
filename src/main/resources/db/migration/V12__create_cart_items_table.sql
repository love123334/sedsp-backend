CREATE TABLE cart_items
(
    id         BIGSERIAL PRIMARY KEY,

    cart_id    BIGINT      NOT NULL
        REFERENCES carts (id)
            ON DELETE CASCADE
            ON UPDATE CASCADE,

    product_id BIGINT      NOT NULL
        REFERENCES products (id)
            ON DELETE RESTRICT
            ON UPDATE CASCADE,

    quantity   INTEGER     NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,

    CONSTRAINT chk_cart_item_quantity
        CHECK (quantity > 0)
);

CREATE INDEX idx_cart_items_cart
    ON cart_items (cart_id);

CREATE INDEX idx_cart_items_product
    ON cart_items (product_id);

CREATE UNIQUE INDEX uq_cart_product_active
    ON cart_items (cart_id, product_id) WHERE deleted_at IS NULL;

CREATE INDEX idx_cart_items_cart_active
    ON cart_items (cart_id) WHERE deleted_at IS NULL;