CREATE TABLE product_images
(
    id         BIGSERIAL PRIMARY KEY,

    product_id BIGINT NOT NULL
        REFERENCES products (id)
            ON DELETE CASCADE,

    image_url  TEXT   NOT NULL,

    is_primary BOOLEAN   DEFAULT FALSE,

    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_product_images_product
    ON product_images (product_id);