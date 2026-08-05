CREATE TABLE carts
(
    id         BIGSERIAL PRIMARY KEY,

    user_id    BIGINT      NOT NULL UNIQUE
        REFERENCES users (id)
            ON DELETE CASCADE
            ON UPDATE CASCADE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);