-- =====================================================
-- V19__refactor_business_relationships_to_role_profiles.sql
-- Refactor business tables to use seller/customer profiles
-- =====================================================

-- =====================================================
-- PRODUCTS -> SELLERS
-- =====================================================

ALTER TABLE products
    ADD COLUMN seller_profile_id BIGINT;

UPDATE products p
SET seller_profile_id = s.id FROM sellers s
WHERE s.user_id = p.seller_id;

ALTER TABLE products
    ADD CONSTRAINT fk_products_seller_profile
        FOREIGN KEY (seller_profile_id)
            REFERENCES sellers (id);

ALTER TABLE products
    ALTER COLUMN seller_profile_id SET NOT NULL;

ALTER TABLE products
DROP
CONSTRAINT products_seller_id_fkey;

ALTER TABLE products
DROP
COLUMN seller_id;

ALTER TABLE products
    RENAME COLUMN seller_profile_id TO seller_id;

CREATE INDEX idx_products_seller_profile
    ON products (seller_id);

-- =====================================================
-- CARTS -> CUSTOMERS
-- =====================================================

ALTER TABLE carts
    ADD COLUMN customer_id BIGINT;

UPDATE carts c
SET customer_id = cu.id FROM customers cu
WHERE cu.user_id = c.user_id;

ALTER TABLE carts
    ADD CONSTRAINT fk_carts_customer
        FOREIGN KEY (customer_id)
            REFERENCES customers (id);

ALTER TABLE carts
    ALTER COLUMN customer_id SET NOT NULL;

ALTER TABLE carts
DROP
CONSTRAINT carts_user_id_fkey;

ALTER TABLE carts
DROP
CONSTRAINT carts_user_id_key;

ALTER TABLE carts
DROP
COLUMN user_id;

CREATE UNIQUE INDEX uq_cart_customer
    ON carts (customer_id);

-- =====================================================
-- ORDERS -> CUSTOMERS
-- =====================================================

ALTER TABLE orders
    ADD COLUMN customer_id BIGINT;

UPDATE orders o
SET customer_id = c.id FROM customers c
WHERE c.user_id = o.user_id;

ALTER TABLE orders
    ADD CONSTRAINT fk_orders_customer
        FOREIGN KEY (customer_id)
            REFERENCES customers (id);

ALTER TABLE orders
    ALTER COLUMN customer_id SET NOT NULL;

ALTER TABLE orders
DROP
CONSTRAINT orders_user_id_fkey;

ALTER TABLE orders
DROP
COLUMN user_id;

CREATE INDEX idx_orders_customer
    ON orders (customer_id);

-- =====================================================
-- PRODUCT REVIEWS -> CUSTOMERS
-- =====================================================

ALTER TABLE product_reviews
    ADD COLUMN customer_id BIGINT;

UPDATE product_reviews pr
SET customer_id = c.id FROM customers c
WHERE c.user_id = pr.user_id;

ALTER TABLE product_reviews
    ADD CONSTRAINT fk_reviews_customer
        FOREIGN KEY (customer_id)
            REFERENCES customers (id);

ALTER TABLE product_reviews
    ALTER COLUMN customer_id SET NOT NULL;

ALTER TABLE product_reviews
DROP
CONSTRAINT product_reviews_user_id_fkey;

ALTER TABLE product_reviews
DROP
CONSTRAINT uq_review_user_product;

ALTER TABLE product_reviews
DROP
COLUMN user_id;

ALTER TABLE product_reviews
    ADD CONSTRAINT uq_review_customer_product
        UNIQUE (customer_id, product_id);

CREATE INDEX idx_reviews_customer
    ON product_reviews (customer_id);

-- =====================================================
-- ORDER TRACKING
-- =====================================================

ALTER TABLE order_tracking
    RENAME COLUMN updated_by TO updated_by_user_id;
