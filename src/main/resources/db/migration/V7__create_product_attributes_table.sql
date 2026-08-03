CREATE TABLE product_attributes
(
    id              BIGSERIAL PRIMARY KEY,

    product_id      BIGINT       NOT NULL
        REFERENCES products (id)
            ON DELETE CASCADE,

    attribute_name  VARCHAR(100) NOT NULL,
    attribute_value VARCHAR(255) NOT NULL,

    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ
);

CREATE INDEX idx_product_attributes_product
    ON product_attributes (product_id);