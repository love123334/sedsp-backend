-- =====================================================
-- V24__create_dss_scenario_tables.sql
-- DSS What-if Analysis & Recommendation Engine
-- =====================================================

-- =====================================================
-- DSS SCENARIOS
-- =====================================================

CREATE TABLE dss_scenarios
(
    id BIGSERIAL PRIMARY KEY,

    seller_id BIGINT NOT NULL,

    name VARCHAR(255) NOT NULL,

    description TEXT,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_dss_scenario_seller
        FOREIGN KEY (seller_id)
            REFERENCES sellers(id)
            ON DELETE CASCADE
);

CREATE INDEX idx_dss_scenarios_seller
    ON dss_scenarios(seller_id);

-- =====================================================
-- DSS SCENARIO ITEMS
-- =====================================================

CREATE TABLE dss_scenario_items
(
    id BIGSERIAL PRIMARY KEY,

    scenario_id BIGINT NOT NULL,

    product_id BIGINT NOT NULL,

    current_price NUMERIC(12,2),

    simulated_price NUMERIC(12,2),

    current_stock INTEGER,

    simulated_stock INTEGER,

    CONSTRAINT fk_dss_item_scenario
        FOREIGN KEY (scenario_id)
            REFERENCES dss_scenarios(id)
            ON DELETE CASCADE,

    CONSTRAINT fk_dss_item_product
        FOREIGN KEY (product_id)
            REFERENCES products(id),

    CONSTRAINT chk_dss_current_price
        CHECK (current_price IS NULL OR current_price >= 0),

    CONSTRAINT chk_dss_simulated_price
        CHECK (simulated_price IS NULL OR simulated_price >= 0),

    CONSTRAINT chk_dss_current_stock
        CHECK (current_stock IS NULL OR current_stock >= 0),

    CONSTRAINT chk_dss_simulated_stock
        CHECK (simulated_stock IS NULL OR simulated_stock >= 0)
);

CREATE INDEX idx_dss_items_scenario
    ON dss_scenario_items(scenario_id);

CREATE INDEX idx_dss_items_product
    ON dss_scenario_items(product_id);

-- =====================================================
-- DSS RESULTS
-- =====================================================

CREATE TABLE dss_results
(
    id BIGSERIAL PRIMARY KEY,

    scenario_id BIGINT NOT NULL UNIQUE,

    predicted_sales INTEGER,

    predicted_revenue NUMERIC(14,2),

    predicted_profit NUMERIC(14,2),

    recommendation TEXT,

    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_dss_result_scenario
        FOREIGN KEY (scenario_id)
            REFERENCES dss_scenarios(id)
            ON DELETE CASCADE,

    CONSTRAINT chk_dss_predicted_sales
        CHECK (predicted_sales IS NULL OR predicted_sales >= 0),

    CONSTRAINT chk_dss_predicted_revenue
        CHECK (predicted_revenue IS NULL OR predicted_revenue >= 0),

    CONSTRAINT chk_dss_predicted_profit
        CHECK (predicted_profit IS NULL OR predicted_profit >= 0)
);

CREATE INDEX idx_dss_results_scenario
    ON dss_results(scenario_id);

-- =====================================================
-- DSS RECOMMENDATIONS
-- =====================================================

CREATE TYPE recommendation_type AS ENUM
    (
    'PRICE',
    'INVENTORY',
    'DEMAND',
    'TREND'
);

CREATE TABLE dss_recommendations
(
    id BIGSERIAL PRIMARY KEY,

    seller_id BIGINT NOT NULL,

    product_id BIGINT,

    recommendation_type recommendation_type NOT NULL,

    title VARCHAR(255) NOT NULL,

    message TEXT NOT NULL,

    confidence_score NUMERIC(5,2),

    is_read BOOLEAN DEFAULT FALSE,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_dss_rec_seller
        FOREIGN KEY (seller_id)
            REFERENCES sellers(id)
            ON DELETE CASCADE,

    CONSTRAINT fk_dss_rec_product
        FOREIGN KEY (product_id)
            REFERENCES products(id)
            ON DELETE SET NULL,

    CONSTRAINT chk_confidence_score
        CHECK (
            confidence_score IS NULL
                OR
            (confidence_score >= 0 AND confidence_score <= 100)
            )
);

CREATE INDEX idx_dss_rec_seller
    ON dss_recommendations(seller_id);

CREATE INDEX idx_dss_rec_product
    ON dss_recommendations(product_id);

CREATE INDEX idx_dss_rec_type
    ON dss_recommendations(recommendation_type);

-- =====================================================
-- END
-- =====================================================