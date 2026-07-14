CREATE TYPE user_status AS ENUM (
    'ACTIVE',
    'INACTIVE',
    'PENDING',
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

CREATE TYPE inventory_log_reason AS ENUM (
    'MANUAL_ADJUST',
    'ORDER',
    'ORDER_CANCEL',
    'RETURN'
);

CREATE TYPE order_tracking_event AS ENUM (
    'CREATED',
    'CONFIRMED',
    'SHIPPED',
    'DELIVERED',
    'CANCELLED_BY_USER',
    'CANCELLED_BY_ADMIN',
    'PAYMENT_FAILED',
    'PAYMENT_SUCCESS'
);