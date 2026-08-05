CREATE TABLE product_reviews
(
    id         BIGSERIAL PRIMARY KEY,

    product_id BIGINT      NOT NULL
        REFERENCES products (id)
            ON DELETE CASCADE,

    user_id    BIGINT      NOT NULL
        REFERENCES users (id)
            ON DELETE SET NULL,

    rating     INTEGER     NOT NULL,

    comment    TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,

    CONSTRAINT chk_review_rating
        CHECK (rating BETWEEN 1 AND 5)
);

CREATE UNIQUE INDEX ux_review_user_product
    ON product_reviews (user_id, product_id) WHERE deleted_at IS NULL;

CREATE INDEX idx_reviews_product
    ON product_reviews (product_id);

CREATE INDEX idx_reviews_user
    ON product_reviews (user_id);

CREATE INDEX idx_reviews_product_active
    ON product_reviews(product_id, created_at DESC)
    WHERE deleted_at IS NULL;