-- =============================================================================
-- SEDSP — Seed realistic demand history for DSS on existing 4 catalog products
-- Marker: [SELLER-SEDSP-TREND]
-- Manual run: psql -f seed_seller_sedsp_demand_trends.sql
-- Flyway equivalent: V66__seed_realistic_dss_demand_patterns.sql
--
-- Target products (seller@sedsp.vn / 12345678):
--   1. tai-nghe-bluetooth-pro-anc      → DOWNWARD (Đang giảm: early 6-9, mid 4-7, late 2-5)
--   2. noi-chien-khong-dau-5l          → STABLE (Tương đối ổn định: baseline ~5)
--   3. ban-phim-co-rgb-keypro-k87      → UPWARD (Đang tăng: early 2-4, mid 4-7, late 7-10)
--   4. giay-chay-bo-airflex-marathon   → SEASONAL (Weekly seasonality: weekend peak)
--
-- Deterministic pseudo-random seed = 2026 (reproducible across database resets)
-- 90 continuous days: CURRENT_DATE - 89 … CURRENT_DATE
-- =============================================================================

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

CREATE TEMP TABLE seller_sedsp_demo_products ON COMMIT DROP AS
SELECT
    product.id,
    product.seller_id,
    product.name,
    product.price,
    profile.profile_name
FROM (
    VALUES
        ('tai-nghe-bluetooth-pro-anc', 'DOWNWARD'),
        ('noi-chien-khong-dau-5l', 'STABLE'),
        ('ban-phim-co-rgb-keypro-k87', 'UPWARD'),
        ('giay-chay-bo-airflex-marathon', 'SEASONAL')
) AS profile(product_slug, profile_name)
JOIN products product ON product.slug = profile.product_slug
JOIN users seller ON seller.id = product.seller_id
WHERE product.deleted_at IS NULL
  AND product.status = 'ACTIVE'
  AND seller.email = 'seller@sedsp.vn'
  AND seller.deleted_at IS NULL;

CREATE TEMP TABLE orders_after_demo_item_removal ON COMMIT DROP AS
SELECT o.id
FROM orders o
WHERE EXISTS (
    SELECT 1
    FROM order_items oi
    WHERE oi.order_id = o.id
      AND oi.product_id IN (SELECT id FROM seller_sedsp_demo_products)
);

DELETE FROM order_items oi
WHERE oi.product_id IN (SELECT id FROM seller_sedsp_demo_products);

CREATE TEMP TABLE empty_orders ON COMMIT DROP AS
SELECT o.id
FROM orders o
WHERE o.id IN (SELECT id FROM orders_after_demo_item_removal)
  AND NOT EXISTS (SELECT 1 FROM order_items oi WHERE oi.order_id = o.id);

DELETE FROM order_tracking ot
WHERE ot.order_id IN (SELECT id FROM empty_orders);

DELETE FROM payments p
WHERE p.order_id IN (SELECT id FROM empty_orders);

DELETE FROM orders o
WHERE o.id IN (SELECT id FROM empty_orders);

UPDATE orders o
SET subtotal_amount = COALESCE(totals.subtotal, 0),
    total_amount = COALESCE(totals.subtotal, 0) + o.shipping_fee - o.discount_amount,
    updated_at = CURRENT_TIMESTAMP
FROM (
    SELECT oi.order_id, ROUND(SUM(oi.subtotal), 2) AS subtotal
    FROM order_items oi
    WHERE oi.order_id IN (SELECT id FROM orders_after_demo_item_removal)
    GROUP BY oi.order_id
) totals
WHERE o.id = totals.order_id;

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
    sale.sale_date + TIME '10:30:00',
    sale.sale_date + TIME '18:30:00'
FROM GENERATE_SERIES(CURRENT_DATE - 89, CURRENT_DATE, INTERVAL '1 day') AS sale(sale_date)
CROSS JOIN (
    SELECT id FROM users WHERE email = 'customer@sedsp.vn' AND deleted_at IS NULL LIMIT 1
) customer
WHERE EXISTS (SELECT 1 FROM seller_sedsp_demo_products);

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
JOIN seller_sedsp_demo_products product ON TRUE
CROSS JOIN LATERAL (
    SELECT (89 - (CURRENT_DATE - forecast_order.created_at::DATE)) AS day_index,
           EXTRACT(ISODOW FROM forecast_order.created_at)::INTEGER AS isodow
) timeline
CROSS JOIN LATERAL (
    SELECT ((('x' || SUBSTR(MD5(timeline.day_index || '-' || product.profile_name || '-2026'), 1, 8))::BIT(32)::BIGINT / 4294967295.0) * 2.0 - 1.0) AS noise
) noise_calc
CROSS JOIN LATERAL (
    SELECT GREATEST(
        1,
        ROUND(
            CASE product.profile_name
                WHEN 'DOWNWARD' THEN
                    9.0 - (timeline.day_index / 89.0) * 6.5 + (noise_calc.noise * 1.2)
                WHEN 'STABLE' THEN
                    5.0 + (noise_calc.noise * 1.2)
                WHEN 'UPWARD' THEN
                    2.5 + (timeline.day_index / 89.0) * 6.5 + (noise_calc.noise * 1.2)
                WHEN 'SEASONAL' THEN
                    (CASE timeline.isodow
                        WHEN 1 THEN 3.5
                        WHEN 2 THEN 4.0
                        WHEN 3 THEN 4.0
                        WHEN 4 THEN 5.0
                        WHEN 5 THEN 7.5
                        WHEN 6 THEN 8.5
                        WHEN 7 THEN 6.5
                    END) + (noise_calc.noise * 1.2)
            END
        )::INTEGER
    ) AS quantity
) demand
WHERE forecast_order.shipping_address LIKE '[SELLER-SEDSP-TREND] %'
  AND forecast_order.created_at::DATE BETWEEN CURRENT_DATE - 89 AND CURRENT_DATE;

UPDATE orders forecast_order
SET subtotal_amount = totals.subtotal,
    total_amount = totals.subtotal + forecast_order.shipping_fee - forecast_order.discount_amount,
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
  AND forecast_order.total_amount > 0;

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
GROUP BY forecast_order.id, forecast_order.created_at;
