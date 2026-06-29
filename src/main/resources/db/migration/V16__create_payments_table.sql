CREATE TABLE payments
(
    id                  BIGSERIAL PRIMARY KEY,

    order_id            BIGINT NOT NULL UNIQUE
        REFERENCES orders(id),

    payment_method      payment_method_enum NOT NULL,

    amount              NUMERIC(12,2) NOT NULL,

    status              payment_status DEFAULT 'PENDING'::payment_status,

    transaction_id      VARCHAR(255) UNIQUE,

    currency            VARCHAR(10) DEFAULT 'VND',

    gateway_response    TEXT,

    paid_at             TIMESTAMP,

    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_payment_amount
        CHECK (amount >= 0)
);

CREATE INDEX idx_payments_order
    ON payments(order_id);

CREATE INDEX idx_payments_status
    ON payments(status);