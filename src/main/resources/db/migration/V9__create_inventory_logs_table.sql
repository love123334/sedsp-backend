CREATE TABLE inventory_logs
(
    id                BIGSERIAL PRIMARY KEY,

    product_id        BIGINT               NOT NULL
        REFERENCES products (id)
            ON DELETE CASCADE,

    change_amount     INTEGER              NOT NULL,

    previous_quantity INTEGER              NOT NULL,
    current_quantity  INTEGER              NOT NULL,

    reason            inventory_log_reason NOT NULL,

    updated_by        BIGINT
        REFERENCES users (id),

    created_at        TIMESTAMP            NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_inventory_log_current_non_negative
        CHECK (current_quantity >= 0)
);

CREATE INDEX idx_inventory_logs_product
    ON inventory_logs(product_id);

CREATE INDEX idx_inventory_logs_updated_by
    ON inventory_logs(updated_by);