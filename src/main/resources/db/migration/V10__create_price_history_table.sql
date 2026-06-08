CREATE TABLE price_history
(
    id            BIGSERIAL PRIMARY KEY,

    product_id    BIGINT NOT NULL
        REFERENCES products(id)
            ON DELETE CASCADE,

    old_price     NUMERIC(12,2),
    new_price     NUMERIC(12,2),

    changed_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_old_price
        CHECK (old_price IS NULL OR old_price >= 0),

    CONSTRAINT chk_new_price
        CHECK (new_price IS NULL OR new_price >= 0)
);

CREATE INDEX idx_price_history_product
    ON price_history(product_id);