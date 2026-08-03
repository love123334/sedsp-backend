CREATE TABLE products
(
    id          BIGSERIAL PRIMARY KEY,

    seller_id   BIGINT         NOT NULL
        REFERENCES users (id)
            ON DELETE RESTRICT
            ON UPDATE CASCADE,

    category_id BIGINT
        REFERENCES categories (id)
            ON DELETE SET NULL
            ON UPDATE CASCADE,

    name        VARCHAR(255)   NOT NULL,
    slug        VARCHAR(255)   NOT NULL,

    description TEXT,

    price       NUMERIC(12, 2) NOT NULL,
    cost_price  NUMERIC(12, 2),

    status      product_status          DEFAULT 'ACTIVE'::product_status,

    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ,

    CONSTRAINT chk_product_price
        CHECK (price >= 0),

    CONSTRAINT chk_product_cost_price
        CHECK (cost_price IS NULL OR cost_price >= 0),

    CONSTRAINT chk_cost_not_greater_than_price
        CHECK (cost_price IS NULL OR cost_price <= price)
);

CREATE INDEX idx_products_seller
    ON products (seller_id);

CREATE INDEX idx_products_category
    ON products (category_id);

CREATE INDEX idx_products_status
    ON products (status);

CREATE UNIQUE INDEX uq_product_slug
    ON products (slug) WHERE deleted_at IS NULL;

CREATE INDEX idx_products_category_not_deleted
    ON products (category_id) WHERE deleted_at IS NULL;

CREATE INDEX idx_products_seller_not_deleted
    ON products (seller_id) WHERE deleted_at IS NULL;

CREATE INDEX idx_products_category_status_active
    ON products (category_id, status) WHERE deleted_at IS NULL;