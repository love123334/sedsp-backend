CREATE TABLE product_images
(
    id         BIGSERIAL PRIMARY KEY,

    product_id BIGINT       NOT NULL
        REFERENCES products (id)
            ON DELETE CASCADE,

    image_url  TEXT         NOT NULL,
    public_id  VARCHAR(255) NOT NULL,
    is_primary BOOLEAN      NOT NULL DEFAULT FALSE,

    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_product_images_product
    ON product_images (product_id);

CREATE UNIQUE INDEX uq_product_primary_image
    ON product_images (product_id) WHERE is_primary = true AND deleted_at IS NULL;