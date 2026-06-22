CREATE TABLE categories
(
    id         BIGSERIAL PRIMARY KEY,

    name       VARCHAR(150) NOT NULL,
    slug       VARCHAR(150) UNIQUE,

    parent_id  BIGINT
        REFERENCES categories(id),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,

    CONSTRAINT chk_category_self_parent
        CHECK (parent_id IS NULL OR parent_id != id),

    CONSTRAINT uq_category_name_parent
        UNIQUE(name, parent_id)
);

CREATE INDEX idx_categories_parent
    ON categories(parent_id);