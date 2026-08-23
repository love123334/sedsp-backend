-- =============================================================================
-- V65: Fix seller@sedsp.vn demand seed — use existing 4 catalog products only.
-- Supersedes product-creation in V64 (seller-sedsp-trend-*).
--
-- Demo products (seller@sedsp.vn / 12345678):
--   ban-phim-co-rgb-keypro-k87      → UPWARD (Đang tăng)
--   tai-nghe-bluetooth-pro-anc      → DOWNWARD (Đang giảm)
--   noi-chien-khong-dau-5l          → STABLE_SEASONAL (Tương đối ổn định)
--   giay-chay-bo-airflex-marathon   → INTERMITTENT_UPWARD (bán gián đoạn)
--
-- Sales window: CURRENT_DATE - 179 … CURRENT_DATE (180 days, ends today).
-- Marker: [SELLER-SEDSP-TREND] — idempotent re-run.
-- =============================================================================

-- 0) Remove cart lines for fake V64 SKUs
DELETE FROM cart_items
WHERE product_id IN (
    SELECT id FROM products WHERE slug LIKE 'seller-sedsp-trend-%'
);

-- 1) Drop fake V64 products + their trend orders
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

UPDATE products
SET deleted_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP,
    status = 'INACTIVE'::product_status
WHERE slug LIKE 'seller-sedsp-trend-%'
  AND deleted_at IS NULL;

-- 2) Real catalog SKUs for seller@sedsp.vn
CREATE TEMP TABLE seller_sedsp_demo_products ON COMMIT DROP AS
SELECT
    product.id,
    product.seller_id,
    product.name,
    product.price,
    profile.profile_name
FROM (
    VALUES
        ('ban-phim-co-rgb-keypro-k87', 'UPWARD'),
        ('tai-nghe-bluetooth-pro-anc', 'DOWNWARD'),
        ('noi-chien-khong-dau-5l', 'STABLE_SEASONAL'),
        ('giay-chay-bo-airflex-marathon', 'INTERMITTENT_UPWARD')
) AS profile(product_slug, profile_name)
JOIN products product ON product.slug = profile.product_slug
JOIN users seller ON seller.id = product.seller_id
WHERE product.deleted_at IS NULL
  AND product.status = 'ACTIVE'
  AND seller.email = 'seller@sedsp.vn'
  AND seller.deleted_at IS NULL;

-- Strip prior sales on these SKUs (V39/V60/V61 noise) so DSS trends stay clean
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

-- Remove orders left empty after line removal
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

-- Recalculate totals for orders that still have other lines
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

-- 3) One DELIVERED order per day (180 days → today)
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
WHERE EXISTS (SELECT 1 FROM seller_sedsp_demo_products)
  AND NOT EXISTS (
      SELECT 1
      FROM orders existing_order
      WHERE existing_order.shipping_address =
            '[SELLER-SEDSP-TREND] ' || TO_CHAR(sale.sale_date, 'YYYY-MM-DD')
  );

-- 4) Order lines — four distinct trend profiles + light random noise
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
    SELECT 179 - (CURRENT_DATE - forecast_order.created_at::DATE) AS recency_index
) age
CROSS JOIN LATERAL (
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
