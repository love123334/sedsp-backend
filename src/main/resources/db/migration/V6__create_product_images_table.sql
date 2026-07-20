CREATE TABLE product_images
(
    id          BIGSERIAL PRIMARY KEY,

    product_id  BIGINT       NOT NULL
        REFERENCES products (id)
            ON DELETE CASCADE,

    image_url   TEXT         NOT NULL,

    public_id   VARCHAR(255) NOT NULL,

    is_primary  BOOLEAN      NOT NULL DEFAULT FALSE,

    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP
);

CREATE INDEX idx_product_images_product
    ON product_images (product_id);