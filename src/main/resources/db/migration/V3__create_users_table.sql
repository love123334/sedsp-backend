CREATE TABLE users
(
    id             BIGSERIAL PRIMARY KEY,

    username       VARCHAR(100) NOT NULL UNIQUE,
    email          VARCHAR(150) NOT NULL UNIQUE,
    password       VARCHAR(255) NOT NULL,

    full_name      VARCHAR(150),
    phone          VARCHAR(20) UNIQUE,

    status         user_status DEFAULT 'ACTIVE'::user_status,

    role_id        BIGINT NOT NULL
        REFERENCES roles (id),

    store_name     VARCHAR(255),
    business_email VARCHAR(150),
    business_phone VARCHAR(20),
    seller_description TEXT,

    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at     TIMESTAMPTZ
);

CREATE INDEX idx_users_role
    ON users (role_id);