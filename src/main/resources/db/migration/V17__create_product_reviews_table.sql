CREATE TABLE product_reviews
(
    id             BIGSERIAL PRIMARY KEY,

    product_id     BIGINT NOT NULL
        REFERENCES products(id)
            ON DELETE CASCADE,

    user_id        BIGINT NOT NULL
        REFERENCES users(id),

    rating         INTEGER NOT NULL,

    comment        TEXT,

    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_review_rating
        CHECK (rating BETWEEN 1 AND 5),

    CONSTRAINT uq_review_user_product
        UNIQUE(user_id, product_id)
);

CREATE INDEX idx_reviews_product
    ON product_reviews(product_id);

CREATE INDEX idx_reviews_user
    ON product_reviews(user_id);