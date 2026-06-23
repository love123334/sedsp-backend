CREATE TABLE wishlists
(
    id         BIGSERIAL PRIMARY KEY,

    user_id    BIGINT NOT NULL UNIQUE,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_wishlist_customer
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE
);

CREATE TABLE wishlist_items
(
    id          BIGSERIAL PRIMARY KEY,

    wishlist_id BIGINT NOT NULL,

    product_id  BIGINT NOT NULL,

    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_wishlist_item_wishlist
        FOREIGN KEY (wishlist_id)
            REFERENCES wishlists (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_wishlist_item_product
        FOREIGN KEY (product_id)
            REFERENCES products (id)
            ON DELETE CASCADE,

    CONSTRAINT uq_wishlist_product
        UNIQUE (wishlist_id, product_id)
);