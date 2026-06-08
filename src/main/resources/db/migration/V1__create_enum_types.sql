CREATE TYPE user_status AS ENUM (
    'ACTIVE',
    'INACTIVE',
    'BLOCKED'
);

CREATE TYPE product_status AS ENUM (
    'ACTIVE',
    'INACTIVE',
    'OUT_OF_STOCK'
);

CREATE TYPE order_status AS ENUM (
    'PENDING',
    'PAID',
    'PROCESSING',
    'SHIPPING',
    'DELIVERED',
    'CANCELLED',
    'REFUNDED'
);

CREATE TYPE payment_status AS ENUM (
    'PENDING',
    'SUCCESS',
    'FAILED'
);

CREATE TYPE payment_method_enum AS ENUM (
    'MOMO',
    'BANK',
    'COD'
);