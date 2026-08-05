CREATE TABLE roles
(
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ
);

INSERT INTO roles (name, description)
VALUES ('ADMIN', 'System Administrator'),
       ('MANAGER', 'Business Manager'),
       ('SELLER', 'Product Seller'),
       ('CUSTOMER', 'Customer User');