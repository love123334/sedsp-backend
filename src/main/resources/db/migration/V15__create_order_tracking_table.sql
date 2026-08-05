CREATE TABLE order_tracking
(
    id         BIGSERIAL PRIMARY KEY,

    order_id   BIGINT               NOT NULL
        REFERENCES orders (id)
            ON DELETE CASCADE,

    event      order_tracking_event NOT NULL,

    note       TEXT,

    updated_by BIGINT               NOT NULL
        REFERENCES users (id),

    created_at TIMESTAMPTZ          NOT NULL DEFAULT now()
);

CREATE INDEX idx_order_tracking_order
    ON order_tracking (order_id);

CREATE INDEX idx_order_tracking_order_created
    ON order_tracking (order_id, created_at DESC);