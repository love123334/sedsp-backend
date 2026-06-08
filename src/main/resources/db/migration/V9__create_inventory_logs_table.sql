CREATE TABLE inventory_logs
(
    id               BIGSERIAL PRIMARY KEY,

    product_id       BIGINT NOT NULL
        REFERENCES products(id)
            ON DELETE CASCADE,

    change_amount    INTEGER NOT NULL,

    reason           VARCHAR(255),

    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_inventory_logs_product
    ON inventory_logs(product_id);