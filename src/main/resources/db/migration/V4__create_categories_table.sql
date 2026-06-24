CREATE TABLE categories
(
    id         BIGSERIAL PRIMARY KEY,

    name       VARCHAR(150) NOT NULL,
    slug       VARCHAR(150) NOT NULL,

    parent_id  BIGINT REFERENCES categories(id),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,

    CONSTRAINT chk_category_self_parent
        CHECK (parent_id IS NULL OR parent_id != id)
    );

CREATE INDEX idx_categories_parent
    ON categories(parent_id);

-- Unique root
CREATE UNIQUE INDEX uq_category_root_name
    ON categories(name)
    WHERE parent_id IS NULL
      AND deleted_at IS NULL;

-- Unique child
CREATE UNIQUE INDEX uq_category_name_parent
    ON categories(name, parent_id)
    WHERE parent_id IS NOT NULL
      AND deleted_at IS NULL;

-- Unique slug
CREATE UNIQUE INDEX uq_category_slug
    ON categories(slug)
    WHERE deleted_at IS NULL;