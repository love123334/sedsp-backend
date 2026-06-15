-- =====================================================
-- V18__create_role_profile_tables.sql
-- Role-specific profile tables
-- =====================================================

CREATE TABLE admins
(
    id          BIGSERIAL PRIMARY KEY,


    user_id     BIGINT NOT NULL UNIQUE
        REFERENCES users (id)
            ON DELETE CASCADE,

    admin_level INTEGER   DEFAULT 1,

    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP


);

CREATE TABLE managers
(
    id         BIGSERIAL PRIMARY KEY,


    user_id    BIGINT NOT NULL UNIQUE
        REFERENCES users (id)
            ON DELETE CASCADE,

    department VARCHAR(100),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP


);

CREATE TABLE sellers
(
    id             BIGSERIAL PRIMARY KEY,


    user_id        BIGINT NOT NULL UNIQUE
        REFERENCES users (id)
            ON DELETE CASCADE,

    store_name     VARCHAR(255),

    business_email VARCHAR(150),

    business_phone VARCHAR(20),

    description    TEXT,

    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP


);

CREATE TABLE customers
(
    id             BIGSERIAL PRIMARY KEY,


    user_id        BIGINT NOT NULL UNIQUE
        REFERENCES users (id)
            ON DELETE CASCADE,

    loyalty_points INTEGER   DEFAULT 0,

    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP

);

CREATE INDEX idx_admins_user
    ON admins (user_id);

CREATE INDEX idx_managers_user
    ON managers (user_id);

CREATE INDEX idx_sellers_user
    ON sellers (user_id);

CREATE INDEX idx_customers_user
    ON customers (user_id);
