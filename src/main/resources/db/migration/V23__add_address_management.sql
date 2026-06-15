CREATE TABLE customer_addresses
(
    id BIGSERIAL PRIMARY KEY,

    customer_id BIGINT NOT NULL,

    receiver_name VARCHAR(150) NOT NULL,

    receiver_phone VARCHAR(20) NOT NULL,

    province VARCHAR(100) NOT NULL,

    district VARCHAR(100) NOT NULL,

    ward VARCHAR(100) NOT NULL,

    address_line TEXT NOT NULL,

    is_default BOOLEAN DEFAULT FALSE,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_customer_address_customer
        FOREIGN KEY (customer_id)
            REFERENCES customers(id)
            ON DELETE CASCADE
);

ALTER TABLE orders
    ADD COLUMN address_id BIGINT;

ALTER TABLE orders
    ADD CONSTRAINT fk_orders_address
        FOREIGN KEY (address_id)
            REFERENCES customer_addresses(id);