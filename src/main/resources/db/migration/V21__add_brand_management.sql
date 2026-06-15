-- =====================================================
-- V21__add_brand_management.sql
-- Add Brand Entity For Products
-- =====================================================

-- =====================================================
-- 1. CREATE BRANDS TABLE
-- =====================================================

CREATE TABLE brands
(
    id           BIGSERIAL PRIMARY KEY,

    name         VARCHAR(150) NOT NULL UNIQUE,

    slug         VARCHAR(150) UNIQUE,

    description  TEXT,

    logo_url     TEXT,

    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    deleted_at   TIMESTAMP
);

-- =====================================================
-- 2. ADD BRAND REFERENCE TO PRODUCTS
-- =====================================================

ALTER TABLE products
    ADD COLUMN brand_id BIGINT;

ALTER TABLE products
    ADD CONSTRAINT fk_products_brand
        FOREIGN KEY (brand_id)
            REFERENCES brands(id);

-- =====================================================
-- 3. INDEX
-- =====================================================

CREATE INDEX idx_products_brand
    ON products(brand_id);