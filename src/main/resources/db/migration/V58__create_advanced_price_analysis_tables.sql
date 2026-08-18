-- Advanced price recommendation sessions and their five most recent scenarios.

CREATE TABLE advanced_price_sessions
(
    id                         BIGSERIAL PRIMARY KEY,
    seller_id                  BIGINT NOT NULL REFERENCES users (id),
    product_id                 BIGINT NOT NULL REFERENCES products (id),
    product_name               VARCHAR(255) NOT NULL,
    from_date                  DATE NOT NULL,
    to_date                    DATE NOT NULL,
    forecast_period            INTEGER NOT NULL,
    estimated_order_cost       NUMERIC(14, 2) NOT NULL,
    base_price                 NUMERIC(14, 2) NOT NULL,
    cost_price                 NUMERIC(14, 2) NOT NULL,
    historical_quantity_sold   BIGINT NOT NULL,
    average_elasticity         NUMERIC(12, 6) NOT NULL,
    elasticity_source          VARCHAR(40) NOT NULL,
    baseline_forecast_demand   BIGINT NOT NULL,
    forecast_method            VARCHAR(100) NOT NULL,
    status                     VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    applied_at                 TIMESTAMPTZ,
    version                    BIGINT NOT NULL DEFAULT 0,
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                 TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_advanced_price_date_range
        CHECK (from_date <= to_date),
    CONSTRAINT chk_advanced_price_forecast_period
        CHECK (forecast_period IN (7, 14, 30)),
    CONSTRAINT chk_advanced_price_order_cost
        CHECK (estimated_order_cost >= 0),
    CONSTRAINT chk_advanced_price_base_price
        CHECK (base_price > 0),
    CONSTRAINT chk_advanced_price_cost_price
        CHECK (cost_price >= 0),
    CONSTRAINT chk_advanced_price_historical_quantity
        CHECK (historical_quantity_sold >= 0),
    CONSTRAINT chk_advanced_price_baseline_demand
        CHECK (baseline_forecast_demand >= 0),
    CONSTRAINT chk_advanced_price_session_status
        CHECK (status IN ('ACTIVE', 'APPLIED'))
);

CREATE INDEX idx_advanced_price_sessions_seller_created
    ON advanced_price_sessions (seller_id, created_at DESC);

CREATE INDEX idx_advanced_price_sessions_product
    ON advanced_price_sessions (product_id);

CREATE TABLE advanced_price_scenarios
(
    id                       BIGSERIAL PRIMARY KEY,
    session_id               BIGINT NOT NULL
        REFERENCES advanced_price_sessions (id) ON DELETE CASCADE,
    price_change_percent     NUMERIC(6, 2) NOT NULL,
    new_price                NUMERIC(14, 2) NOT NULL,
    profit_per_product       NUMERIC(14, 2) NOT NULL,
    demand_multiplier        NUMERIC(14, 6) NOT NULL,
    forecast_demand          BIGINT NOT NULL,
    expected_profit          NUMERIC(18, 2) NOT NULL,
    applied_at               TIMESTAMPTZ,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_advanced_price_session_change
        UNIQUE (session_id, price_change_percent),
    CONSTRAINT chk_advanced_price_change_percent
        CHECK (price_change_percent BETWEEN -70 AND 100),
    CONSTRAINT chk_advanced_price_new_price
        CHECK (new_price > 0),
    CONSTRAINT chk_advanced_price_demand_multiplier
        CHECK (demand_multiplier >= 0),
    CONSTRAINT chk_advanced_price_forecast_demand
        CHECK (forecast_demand >= 0)
);

CREATE INDEX idx_advanced_price_scenarios_session_created
    ON advanced_price_scenarios (session_id, created_at DESC);
