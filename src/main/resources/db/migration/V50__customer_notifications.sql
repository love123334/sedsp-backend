CREATE TABLE customer_notifications (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    order_id        BIGINT REFERENCES orders (id) ON DELETE SET NULL,
    notification_type VARCHAR(32) NOT NULL DEFAULT 'ORDER_STATUS',
    title           VARCHAR(255) NOT NULL,
    message         TEXT NOT NULL,
    is_read         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_customer_notifications_user_unread
    ON customer_notifications (user_id, is_read, created_at DESC);
