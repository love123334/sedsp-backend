-- =============================================================================
-- SEDSP — Seed demand-forecast trends for seller@sedsp.vn
-- Marker: [SELLER-SEDSP-TREND]
-- Idempotent: safe to re-run (clears prior marker orders for these products).
--
-- PURPOSE (for tester):
--   After this seed, login as seller@sedsp.vn / 12345678 → DSS → Dự báo nhu cầu,
--   pick each "DSS Forecast - …" product and verify Xu hướng:
--     Đang tăng | Đang giảm | Tương đối ổn định
--   (FE: LightGbmDemandDemoView — trendSlope > 0.05 / < -0.05 / else)
--
-- ML (backend):
--   Primary: LightGBM → ONNX file models/demand/global-demand.onnx
--            method = lightgbm_onnx | lightgbm_onnx_with_baseline_fallback
--   Fallback: DemandForecastEngine statistical baseline (trend_blended_feature_forecast)
--   Sales source: order_items JOIN orders WHERE status = 'DELIVERED'
--
-- Profiles (same logic as V57, + random noise):
--   UPWARD              → rising daily qty → trendSlope > 0.05  → "Đang tăng"
--   DOWNWARD            → falling daily qty → trendSlope < -0.05 → "Đang giảm"
--   STABLE_SEASONAL     → flat + weekend bump → |slope| ≤ 0.05 → "Tương đối ổn định"
--   INTERMITTENT_UPWARD → sparse days + mild rise (zero-sale days omitted)
-- =============================================================================

-- 0) Cleanup previous runs of this seed (orders + items + payments + tracking)
WITH doomed AS (
    SELECT o.id
    FROM orders o
    WHERE o.shipping_address LIKE '[SELLER-SEDSP-TREND] %'
)
DELETE FROM order_tracking ot
WHERE ot.order_id IN (SELECT id FROM doomed);

WITH doomed AS (
    SELECT o.id
    FROM orders o
    WHERE o.shipping_address LIKE '[SELLER-SEDSP-TREND] %'
)
DELETE FROM payments p
WHERE p.order_id IN (SELECT id FROM doomed);

WITH doomed AS (
    SELECT o.id
    FROM orders o
    WHERE o.shipping_address LIKE '[SELLER-SEDSP-TREND] %'
)
DELETE FROM order_items oi
WHERE oi.order_id IN (SELECT id FROM doomed);

DELETE FROM orders
WHERE shipping_address LIKE '[SELLER-SEDSP-TREND] %';

-- Soft-delete old seeded products (keep history if referenced elsewhere)
UPDATE products
SET deleted_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP,
    status = 'INACTIVE'::product_status
WHERE slug LIKE 'seller-sedsp-trend-%'
  AND deleted_at IS NULL;

-- 1) Products owned by seller@sedsp.vn (category: Điện tử)
INSERT INTO products (
    seller_id,
    category_id,
    name,
    slug,
    description,
    price,
    cost_price,
    status,
    created_at,
    updated_at
)
SELECT
    seller.id,
    category.id,
    demo.name,
    demo.slug,
    demo.description,
    demo.price,
    demo.cost_price,
    'ACTIVE'::product_status,
    CURRENT_TIMESTAMP - INTERVAL '200 days',
    CURRENT_TIMESTAMP
FROM users seller
CROSS JOIN categories category
CROSS JOIN (
    VALUES
        (
            'DSS Forecast - Nhu cầu tăng',
            'seller-sedsp-trend-upward',
            'Seed seller@sedsp.vn — profile UPWARD (random noise).',
            450000.00::NUMERIC,
            270000.00::NUMERIC
        ),
        (
            'DSS Forecast - Nhu cầu giảm',
            'seller-sedsp-trend-downward',
            'Seed seller@sedsp.vn — profile DOWNWARD (random noise).',
            850000.00::NUMERIC,
            510000.00::NUMERIC
        ),
        (
            'DSS Forecast - Ổn định theo tuần',
            'seller-sedsp-trend-stable',
            'Seed seller@sedsp.vn — profile STABLE_SEASONAL (random noise).',
            1250000.00::NUMERIC,
            750000.00::NUMERIC
        ),
        (
            'DSS Forecast - Bán gián đoạn',
            'seller-sedsp-trend-intermittent',
            'Seed seller@sedsp.vn — profile INTERMITTENT_UPWARD (random noise).',
            320000.00::NUMERIC,
            190000.00::NUMERIC
        )
) AS demo(name, slug, description, price, cost_price)
WHERE seller.email = 'seller@sedsp.vn'
  AND seller.deleted_at IS NULL
  AND category.slug = 'dien-tu'
  AND category.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM products existing_product
      WHERE existing_product.slug = demo.slug
        AND existing_product.deleted_at IS NULL
  );

INSERT INTO inventory (product_id, available_quantity, reserved_quantity)
SELECT product.id, 1000, 0
FROM products product
WHERE product.slug LIKE 'seller-sedsp-trend-%'
  AND product.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM inventory existing_inventory
      WHERE existing_inventory.product_id = product.id
  );

-- Profile map
CREATE TEMP TABLE seller_sedsp_trend_profiles ON COMMIT DROP AS
SELECT
    product.id,
    product.seller_id,
    product.name,
    product.price,
    profile.profile_name
FROM (
    VALUES
        ('seller-sedsp-trend-upward', 'UPWARD'),
        ('seller-sedsp-trend-downward', 'DOWNWARD'),
        ('seller-sedsp-trend-stable', 'STABLE_SEASONAL'),
        ('seller-sedsp-trend-intermittent', 'INTERMITTENT_UPWARD')
) AS profile(product_slug, profile_name)
JOIN products product ON product.slug = profile.product_slug
WHERE product.deleted_at IS NULL
  AND product.status = 'ACTIVE';

-- 2) One DELIVERED order per day (180 days → today), buyer = customer@sedsp.vn
INSERT INTO orders (
    user_id,
    subtotal_amount,
    shipping_fee,
    discount_amount,
    total_amount,
    status,
    shipping_address,
    created_at,
    updated_at
)
SELECT
    customer.id,
    0.00,
    15000.00,
    0.00,
    15000.00,
    'DELIVERED'::order_status,
    '[SELLER-SEDSP-TREND] ' || TO_CHAR(sale.sale_date, 'YYYY-MM-DD'),
    sale.sale_date + TIME '11:00:00',
    sale.sale_date + TIME '19:00:00'
FROM GENERATE_SERIES(CURRENT_DATE - 179, CURRENT_DATE, INTERVAL '1 day') AS sale(sale_date)
CROSS JOIN (
    SELECT id FROM users WHERE email = 'customer@sedsp.vn' AND deleted_at IS NULL LIMIT 1
) customer
WHERE NOT EXISTS (
    SELECT 1
    FROM orders existing_order
    WHERE existing_order.shipping_address =
          '[SELLER-SEDSP-TREND] ' || TO_CHAR(sale.sale_date, 'YYYY-MM-DD')
);

-- 3) Order lines — random qty around trend formulas (quantity >= 1)
INSERT INTO order_items (
    order_id,
    product_id,
    seller_id,
    product_name_at_purchase,
    quantity,
    unit_price_at_purchase,
    subtotal
)
SELECT
    forecast_order.id,
    product.id,
    product.seller_id,
    product.name,
    demand.quantity,
    product.price,
    ROUND(product.price * demand.quantity, 2)
FROM orders forecast_order
JOIN seller_sedsp_trend_profiles product ON TRUE
CROSS JOIN LATERAL (
    SELECT 179 - (CURRENT_DATE - forecast_order.created_at::DATE) AS recency_index
) age
CROSS JOIN LATERAL (
    -- Base trend (deterministic shape) + random noise in [-1, +2]
    SELECT GREATEST(
        1,
        (
            CASE product.profile_name
                WHEN 'UPWARD' THEN
                    2
                    + (age.recency_index / 22)
                    + CASE
                        WHEN EXTRACT(ISODOW FROM forecast_order.created_at) IN (6, 7)
                            THEN 2
                        ELSE 0
                      END
                WHEN 'DOWNWARD' THEN
                    GREATEST(
                        2,
                        12
                        - (age.recency_index / 20)
                        + CASE
                            WHEN EXTRACT(ISODOW FROM forecast_order.created_at) IN (6, 7)
                                THEN 1
                            ELSE 0
                          END
                    )
                WHEN 'STABLE_SEASONAL' THEN
                    6
                    + CASE
                        WHEN EXTRACT(ISODOW FROM forecast_order.created_at) IN (6, 7)
                            THEN 3
                        ELSE 0
                      END
                    + CASE
                        WHEN MOD(EXTRACT(DAY FROM forecast_order.created_at)::INTEGER, 15) = 0
                            THEN 2
                        ELSE 0
                      END
                ELSE
                    -- INTERMITTENT_UPWARD
                    3
                    + (age.recency_index / 45)
                    + CASE
                        WHEN EXTRACT(ISODOW FROM forecast_order.created_at) = 7
                            THEN 2
                        ELSE 0
                      END
            END
            + FLOOR(RANDOM() * 4)::INTEGER - 1
        )::INTEGER
    ) AS quantity
) demand
WHERE forecast_order.shipping_address LIKE '[SELLER-SEDSP-TREND] %'
  AND forecast_order.created_at::DATE BETWEEN CURRENT_DATE - 179 AND CURRENT_DATE
  -- Intermittent: skip ~1 of every 4 calendar days (true zero-demand days)
  AND NOT (
      product.profile_name = 'INTERMITTENT_UPWARD'
      AND MOD(CURRENT_DATE - forecast_order.created_at::DATE, 4) = 0
  )
  AND NOT EXISTS (
      SELECT 1
      FROM order_items existing_item
      WHERE existing_item.order_id = forecast_order.id
        AND existing_item.product_id = product.id
  );

UPDATE orders forecast_order
SET subtotal_amount = totals.subtotal,
    total_amount = totals.subtotal + forecast_order.shipping_fee
        - forecast_order.discount_amount,
    updated_at = forecast_order.created_at + INTERVAL '8 hours'
FROM (
    SELECT item.order_id, ROUND(SUM(item.subtotal), 2) AS subtotal
    FROM order_items item
    JOIN orders scoped_order ON scoped_order.id = item.order_id
    WHERE scoped_order.shipping_address LIKE '[SELLER-SEDSP-TREND] %'
    GROUP BY item.order_id
) totals
WHERE totals.order_id = forecast_order.id
  AND forecast_order.shipping_address LIKE '[SELLER-SEDSP-TREND] %';

INSERT INTO payments (
    order_id,
    payment_method,
    gateway_name,
    amount,
    status,
    transaction_id,
    currency,
    paid_at,
    created_at
)
SELECT
    forecast_order.id,
    'VNPAY'::payment_method_enum,
    'SELLER_SEDSP_TREND_SEED',
    forecast_order.total_amount,
    'SUCCESS'::payment_status,
    'SELLER-SEDSP-TREND-' || forecast_order.id::TEXT,
    'VND',
    forecast_order.created_at + INTERVAL '10 minutes',
    forecast_order.created_at
FROM orders forecast_order
WHERE forecast_order.shipping_address LIKE '[SELLER-SEDSP-TREND] %'
  AND forecast_order.total_amount > 0
  AND NOT EXISTS (
      SELECT 1
      FROM payments existing_payment
      WHERE existing_payment.order_id = forecast_order.id
  );

INSERT INTO order_tracking (
    order_id,
    event,
    note,
    updated_by,
    created_at
)
SELECT
    forecast_order.id,
    'DELIVERED'::order_tracking_event,
    'Delivered seed for seller@sedsp.vn demand-forecast trend demos.',
    MIN(item.seller_id),
    forecast_order.created_at + INTERVAL '8 hours'
FROM orders forecast_order
JOIN order_items item ON item.order_id = forecast_order.id
WHERE forecast_order.shipping_address LIKE '[SELLER-SEDSP-TREND] %'
  AND NOT EXISTS (
      SELECT 1
      FROM order_tracking existing_tracking
      WHERE existing_tracking.order_id = forecast_order.id
        AND existing_tracking.event = 'DELIVERED'
  )
GROUP BY forecast_order.id, forecast_order.created_at;

-- Sanity check (run after seed)
-- SELECT p.name, p.slug, COUNT(oi.id) AS line_days, SUM(oi.quantity) AS units
-- FROM products p
-- JOIN order_items oi ON oi.product_id = p.id
-- JOIN orders o ON o.id = oi.order_id AND o.status = 'DELIVERED'
-- WHERE p.slug LIKE 'seller-sedsp-trend-%' AND p.deleted_at IS NULL
-- GROUP BY p.name, p.slug
-- ORDER BY p.slug;
