-- Platform / shop discount vouchers + seller approval workflow

CREATE TYPE voucher_discount_type AS ENUM ('PERCENTAGE', 'FIXED');
CREATE TYPE voucher_scope AS ENUM ('PLATFORM', 'SHOP');
CREATE TYPE voucher_applies_to AS ENUM ('ALL_PRODUCTS', 'SELECTED_PRODUCTS');
CREATE TYPE voucher_request_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED');

CREATE TABLE vouchers (
    id                  BIGSERIAL PRIMARY KEY,
    code                VARCHAR(50) NOT NULL,
    name                VARCHAR(255) NOT NULL,
    description         TEXT,
    discount_type       voucher_discount_type NOT NULL DEFAULT 'PERCENTAGE',
    discount_value      NUMERIC(12, 2) NOT NULL,
    scope               voucher_scope NOT NULL DEFAULT 'PLATFORM',
    seller_id           BIGINT REFERENCES users (id) ON DELETE CASCADE,
    applies_to          voucher_applies_to NOT NULL DEFAULT 'ALL_PRODUCTS',
    minimum_order_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    maximum_discount_amount NUMERIC(12, 2),
    usage_limit         INTEGER,
    used_count          INTEGER NOT NULL DEFAULT 0,
    starts_at           TIMESTAMPTZ NOT NULL,
    ends_at             TIMESTAMPTZ NOT NULL,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_by          BIGINT REFERENCES users (id),
    request_id          BIGINT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_voucher_discount_value CHECK (discount_value > 0),
    CONSTRAINT chk_voucher_dates CHECK (ends_at > starts_at),
    CONSTRAINT chk_voucher_shop_seller CHECK (
        (scope = 'PLATFORM' AND seller_id IS NULL)
        OR (scope = 'SHOP' AND seller_id IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uq_voucher_platform_code ON vouchers (UPPER(code))
    WHERE scope = 'PLATFORM';

CREATE UNIQUE INDEX uq_voucher_shop_code ON vouchers (seller_id, UPPER(code))
    WHERE scope = 'SHOP';

CREATE TABLE voucher_products (
    voucher_id BIGINT NOT NULL REFERENCES vouchers (id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    PRIMARY KEY (voucher_id, product_id)
);

CREATE TABLE voucher_requests (
    id                      BIGSERIAL PRIMARY KEY,
    seller_id               BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    code                    VARCHAR(50) NOT NULL,
    name                    VARCHAR(255) NOT NULL,
    description             TEXT,
    discount_type           voucher_discount_type NOT NULL DEFAULT 'PERCENTAGE',
    discount_value          NUMERIC(12, 2) NOT NULL,
    applies_to              voucher_applies_to NOT NULL DEFAULT 'ALL_PRODUCTS',
    minimum_order_amount    NUMERIC(12, 2) NOT NULL DEFAULT 0,
    maximum_discount_amount NUMERIC(12, 2),
    usage_limit             INTEGER,
    starts_at               TIMESTAMPTZ NOT NULL,
    ends_at                 TIMESTAMPTZ NOT NULL,
    status                  voucher_request_status NOT NULL DEFAULT 'PENDING',
    manager_note            TEXT,
    reviewed_by             BIGINT REFERENCES users (id),
    reviewed_at             TIMESTAMPTZ,
    voucher_id              BIGINT REFERENCES vouchers (id),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE voucher_request_products (
    request_id BIGINT NOT NULL REFERENCES voucher_requests (id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    PRIMARY KEY (request_id, product_id)
);

ALTER TABLE voucher_requests
    ADD CONSTRAINT fk_voucher_requests_voucher
        FOREIGN KEY (voucher_id) REFERENCES vouchers (id);

ALTER TABLE vouchers
    ADD CONSTRAINT fk_vouchers_request
        FOREIGN KEY (request_id) REFERENCES voucher_requests (id);

CREATE TABLE voucher_usages (
    id         BIGSERIAL PRIMARY KEY,
    voucher_id BIGINT NOT NULL REFERENCES vouchers (id) ON DELETE CASCADE,
    user_id    BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    order_id   BIGINT REFERENCES orders (id) ON DELETE SET NULL,
    used_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS voucher_id BIGINT REFERENCES vouchers (id);

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS voucher_code VARCHAR(50);

CREATE INDEX idx_vouchers_active ON vouchers (is_active, starts_at, ends_at);
CREATE INDEX idx_voucher_requests_status ON voucher_requests (status, created_at DESC);
