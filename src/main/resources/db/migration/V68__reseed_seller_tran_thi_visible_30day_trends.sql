-- =============================================================================
-- V68: Reseed seller@sedsp.vn (Trần Thị Bán) so the default 30-day DSS window
-- shows the intended trend instead of a near-flat tail of a 180-day ramp.
--
--   tai-nghe-bluetooth-pro-anc    → DOWNWARD  (last 40 days 12 → 3)
--   noi-chien-khong-dau-5l        → STABLE    (~6 / ngày, nhiễu nhỏ)
--   ban-phim-co-rgb-keypro-k87    → UPWARD    (last 40 days 3.5 → 13)
--   giay-chay-bo-airflex-marathon → SEASONAL  (T2 thấp, T6–CN cao)
--
-- 180 continuous days: CURRENT_DATE - 179 … CURRENT_DATE
-- Marker: [SELLER-SEDSP-TREND]
-- =============================================================================

-- 1) Cleanup all prior trend orders
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

-- 2) Target demo products for seller@sedsp.vn
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

-- Remove legacy sales on these 4 demo products so DSS patterns stay pure
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

-- 3) Create 180 continuous daily DELIVERED orders (CURRENT_DATE - 179 … CURRENT_DATE)
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
FROM GENERATE_SERIES(CURRENT_DATE - 179, CURRENT_DATE, INTERVAL '1 day') AS sale(sale_date)
CROSS JOIN (
    SELECT id FROM users WHERE email = 'customer@sedsp.vn' AND deleted_at IS NULL LIMIT 1
) customer
WHERE EXISTS (SELECT 1 FROM seller_sedsp_demo_products);

-- 4) Create order items for the 4 demand profiles using deterministic random seed 2026 + holiday factors
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
    SELECT (179 - (CURRENT_DATE - forecast_order.created_at::DATE)) AS day_index,
           EXTRACT(MONTH FROM forecast_order.created_at)::INTEGER AS sale_month,
           EXTRACT(DAY FROM forecast_order.created_at)::INTEGER AS sale_day,
           EXTRACT(ISODOW FROM forecast_order.created_at)::INTEGER AS isodow
) timeline
CROSS JOIN LATERAL (
    SELECT
        CASE
            WHEN timeline.sale_day = timeline.sale_month THEN 3
            WHEN timeline.sale_day IN (15, 25) THEN 1
            WHEN (timeline.sale_month = 4 AND timeline.sale_day = 30)
              OR (timeline.sale_month = 5 AND timeline.sale_day = 1)
              OR (timeline.sale_month = 9 AND timeline.sale_day = 2)
              OR (timeline.sale_month = 1 AND timeline.sale_day = 1) THEN 2
            ELSE 0
        END AS holiday_boost
) holiday_calc
CROSS JOIN LATERAL (
    SELECT ((('x' || SUBSTR(MD5(timeline.day_index || '-' || product.profile_name || '-2026'), 1, 8))::BIT(32)::BIGINT / 4294967295.0) * 2.0 - 1.0) AS noise
) noise_calc
CROSS JOIN LATERAL (
    SELECT GREATEST(
        1,
        ROUND(
            CASE product.profile_name
                -- DOWNWARD: cao ổn định rồi dốc rõ trong 40 ngày gần nhất (cửa sổ 30 ngày mặc định = Đang giảm)
                WHEN 'DOWNWARD' THEN
                    CASE
                        WHEN timeline.day_index < 140 THEN
                            12.0 + (noise_calc.noise * 0.35)
                        ELSE
                            12.0 - ((timeline.day_index - 140) / 39.0) * 9.0
                            + (noise_calc.noise * 0.30)
                    END
                    + CASE WHEN holiday_calc.holiday_boost >= 3 AND timeline.day_index < 140 THEN 2 ELSE 0 END

                -- STABLE: ~6/ngày, nhiễu nhỏ — độ dốc 30 ngày ≈ 0
                WHEN 'STABLE' THEN
                    6.0 + (noise_calc.noise * 0.30)
                    + CASE WHEN holiday_calc.holiday_boost >= 3 THEN 1 ELSE 0 END

                -- UPWARD: thấp ổn định rồi dốc lên rõ trong 40 ngày gần nhất
                WHEN 'UPWARD' THEN
                    CASE
                        WHEN timeline.day_index < 140 THEN
                            3.5 + (noise_calc.noise * 0.35)
                        ELSE
                            3.5 + ((timeline.day_index - 140) / 39.0) * 9.5
                            + (noise_calc.noise * 0.30)
                    END
                    + CASE WHEN holiday_calc.holiday_boost >= 3 THEN 2 ELSE 0 END

                -- SEASONAL: T2 thấp, T6–CN cao — OLS 30 ngày ≈ ổn định, đường dự báo có mùa tuần
                WHEN 'SEASONAL' THEN
                    (CASE timeline.isodow
                        WHEN 1 THEN 3.0
                        WHEN 2 THEN 3.5
                        WHEN 3 THEN 4.0
                        WHEN 4 THEN 5.0
                        WHEN 5 THEN 8.0
                        WHEN 6 THEN 10.0
                        WHEN 7 THEN 7.5
                    END)
                    + (noise_calc.noise * 0.25)
                    + CASE WHEN holiday_calc.holiday_boost >= 3 THEN 2 ELSE 0 END
            END
        )::INTEGER
    ) AS quantity
) demand
WHERE forecast_order.shipping_address LIKE '[SELLER-SEDSP-TREND] %'
  AND forecast_order.created_at::DATE BETWEEN CURRENT_DATE - 179 AND CURRENT_DATE;

-- 5) Update order totals
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

-- 6) Create matching payment records
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

-- 7) Create tracking records
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
