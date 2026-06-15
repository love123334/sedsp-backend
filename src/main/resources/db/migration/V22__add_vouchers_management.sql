CREATE TABLE vouchers
(
    id BIGSERIAL PRIMARY KEY,

    code VARCHAR(50) NOT NULL UNIQUE,

    name VARCHAR(255) NOT NULL,

    description TEXT,

    discount_type VARCHAR(20) NOT NULL,

    discount_value NUMERIC(12,2) NOT NULL,

    minimum_order_amount NUMERIC(12,2),

    maximum_discount_amount NUMERIC(12,2),

    usage_limit INTEGER,

    used_count INTEGER DEFAULT 0,

    start_date TIMESTAMP NOT NULL,

    end_date TIMESTAMP NOT NULL,

    is_active BOOLEAN DEFAULT TRUE,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE voucher_usages
(
    id BIGSERIAL PRIMARY KEY,

    voucher_id BIGINT NOT NULL,

    customer_id BIGINT NOT NULL,

    order_id BIGINT,

    used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_usage_voucher
        FOREIGN KEY (voucher_id)
            REFERENCES vouchers(id),

    CONSTRAINT fk_usage_customer
        FOREIGN KEY (customer_id)
            REFERENCES customers(id),

    CONSTRAINT fk_usage_order
        FOREIGN KEY (order_id)
            REFERENCES orders(id)
);

ALTER TABLE orders
    ADD COLUMN voucher_id BIGINT;

ALTER TABLE orders
    ADD CONSTRAINT fk_orders_voucher
        FOREIGN KEY (voucher_id)
            REFERENCES vouchers(id);