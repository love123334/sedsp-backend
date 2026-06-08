CREATE TABLE inventory
(
    id                   BIGSERIAL PRIMARY KEY,

    product_id           BIGINT NOT NULL UNIQUE
        REFERENCES products(id)
            ON DELETE CASCADE,

    available_quantity   INTEGER DEFAULT 0,
    reserved_quantity    INTEGER DEFAULT 0,

    updated_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_inventory_available
        CHECK (available_quantity >= 0),

    CONSTRAINT chk_inventory_reserved
        CHECK (reserved_quantity >= 0)
);