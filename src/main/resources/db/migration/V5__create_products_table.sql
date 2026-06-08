CREATE TABLE products
(
    id           BIGSERIAL PRIMARY KEY,

    seller_id    BIGINT NOT NULL
        REFERENCES users(id),

    category_id  BIGINT
        REFERENCES categories(id),

    name         VARCHAR(255) NOT NULL,
    slug         VARCHAR(255) UNIQUE,

    description  TEXT,

    price        NUMERIC(12,2) NOT NULL,
    cost_price   NUMERIC(12,2),

    status       product_status DEFAULT 'ACTIVE'::product_status,

    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at   TIMESTAMP,

    CONSTRAINT chk_product_price
        CHECK (price >= 0),

    CONSTRAINT chk_product_cost_price
        CHECK (cost_price IS NULL OR cost_price >= 0)
);

CREATE INDEX idx_products_seller
    ON products(seller_id);

CREATE INDEX idx_products_category
    ON products(category_id);

CREATE INDEX idx_products_status
    ON products(status);