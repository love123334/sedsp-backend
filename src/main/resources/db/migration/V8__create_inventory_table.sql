CREATE TABLE inventory
(
    id                 BIGSERIAL PRIMARY KEY,

    product_id         BIGINT    NOT NULL UNIQUE
        REFERENCES products (id)
            ON DELETE CASCADE,

    available_quantity INTEGER   NOT NULL DEFAULT 0,

    reserved_quantity  INTEGER   NOT NULL DEFAULT 0,

    updated_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_inventory_available
        CHECK (available_quantity >= 0),

    CONSTRAINT chk_inventory_reserved
        CHECK (reserved_quantity >= 0)
);