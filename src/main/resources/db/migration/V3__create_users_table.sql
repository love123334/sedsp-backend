CREATE TABLE users
(
    id                 BIGSERIAL PRIMARY KEY,

    username           VARCHAR(100) NOT NULL,
    email              VARCHAR(150) NOT NULL,
    password           VARCHAR(255) NOT NULL,

    full_name          VARCHAR(150),
    phone              VARCHAR(20),

    status             user_status  NOT NULL DEFAULT 'ACTIVE',

    role_id            BIGINT       NOT NULL
        REFERENCES roles (id)
            ON DELETE RESTRICT
            ON UPDATE CASCADE,

    store_name         VARCHAR(255),
    business_email     VARCHAR(150),
    business_phone     VARCHAR(20),
    seller_description TEXT,

    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at         TIMESTAMPTZ
);

CREATE INDEX idx_users_role
    ON users (role_id);

CREATE UNIQUE INDEX uq_users_username
    ON users (username) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_users_email
    ON users (email) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_users_phone
    ON users (phone) WHERE deleted_at IS NULL
      AND phone IS NOT NULL;

CREATE INDEX idx_users_active
    ON users(id)
    WHERE deleted_at IS NULL;