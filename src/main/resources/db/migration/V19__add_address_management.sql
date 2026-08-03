CREATE TABLE customer_addresses
(
    id             BIGSERIAL PRIMARY KEY,

    user_id        BIGINT       NOT NULL,

    receiver_name  VARCHAR(150) NOT NULL,

    receiver_phone VARCHAR(20)  NOT NULL,

    province       VARCHAR(100) NOT NULL,

    district       VARCHAR(100) NOT NULL,

    ward           VARCHAR(100) NOT NULL,

    address_line   TEXT         NOT NULL,

    is_default     BOOLEAN   DEFAULT FALSE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_customer_address_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE
);

ALTER TABLE orders
    ADD COLUMN address_id BIGINT;

ALTER TABLE orders
    ADD CONSTRAINT fk_orders_address
        FOREIGN KEY (address_id)
            REFERENCES customer_addresses (id);

CREATE UNIQUE INDEX uq_user_default_address
    ON customer_addresses(user_id)
    WHERE is_default = TRUE;