CREATE TABLE order_tracking
(
    id            BIGSERIAL PRIMARY KEY,

    order_id      BIGINT NOT NULL
        REFERENCES orders(id)
            ON DELETE CASCADE,

    status        order_status NOT NULL,

    note          TEXT,

    updated_by    BIGINT
        REFERENCES users(id),

    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_order_tracking_order
    ON order_tracking(order_id);