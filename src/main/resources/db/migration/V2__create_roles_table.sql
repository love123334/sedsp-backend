CREATE TABLE roles
(
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO roles (name, description)
VALUES
    ('ADMIN', 'System Administrator'),
    ('MANAGER', 'Business Manager'),
    ('SELLER', 'Product Seller'),
    ('CUSTOMER', 'Customer User');