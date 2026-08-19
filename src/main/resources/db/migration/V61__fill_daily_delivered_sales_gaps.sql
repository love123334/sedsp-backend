-- V61: fill missing DELIVERED daily sales so DSS charts have no zero-gaps.
--
-- V43/V46 seeded one DELIVERED basket per day (qty ≈ 8+mod(id,4)+weekend),
-- using CURRENT_DATE at migrate time — later days were empty. V59/V60 scattered
-- random baskets so many SKUs (e.g. Tai nghe Bluetooth Pro ANC) dropped to 0
-- on 06–17/08. This continues the same daily formula for every catalog product
-- that has zero DELIVERED qty that day. Revenue wobbles ~1% so days are not clones.
-- Idempotent: [DAILY-GAP] s{sellerId} YYYY-MM-DD. Skips dss-forecast-* profiles.

CREATE TEMP TABLE gap_buyers ON COMMIT DROP AS
SELECT u.id,
       ROW_NUMBER() OVER (ORDER BY u.id) AS rn
FROM users u
JOIN roles r ON r.id = u.role_id
WHERE r.name = 'CUSTOMER'
  AND u.status = 'ACTIVE'
  AND u.deleted_at IS NULL;

CREATE TEMP TABLE gap_days ON COMMIT DROP AS
SELECT d::DATE AS sale_date
FROM GENERATE_SERIES(
    DATE '2026-08-06',
    GREATEST(DATE '2026-08-06', CURRENT_DATE),
    INTERVAL '1 day'
) AS d
WHERE CURRENT_DATE >= DATE '2026-08-06';

CREATE TEMP TABLE gap_catalog ON COMMIT DROP AS
SELECT
    p.id,
    p.seller_id,
    p.name,
    p.price
FROM products p
JOIN users seller ON seller.id = p.seller_id
JOIN roles role ON role.id = seller.role_id
WHERE p.deleted_at IS NULL
  AND p.status = 'ACTIVE'
  AND p.price > 0
  AND seller.deleted_at IS NULL
  AND seller.status = 'ACTIVE'
  AND role.name = 'SELLER'
  AND p.slug NOT LIKE 'dss-forecast-%';

CREATE TEMP TABLE gap_needed ON COMMIT DROP AS
SELECT
    catalog.seller_id,
    day.sale_date,
    catalog.id AS product_id,
    catalog.name AS product_name,
    GREATEST(
        1,
        (
            8
            + MOD(catalog.id, 4)
            + CASE
                WHEN EXTRACT(ISODOW FROM day.sale_date) IN (6, 7) THEN 2
                ELSE 0
              END
            + CASE
                WHEN MOD(EXTRACT(DAY FROM day.sale_date)::INTEGER, 7) = 0 THEN 1
                ELSE 0
              END
            + CASE
                WHEN day.sale_date BETWEEN DATE '2026-08-07' AND DATE '2026-08-09'
                    THEN 1
                ELSE 0
              END
        )
    )::INTEGER AS quantity,
    ROUND(
        catalog.price * (
            1
            + ((MOD(catalog.id + EXTRACT(DOY FROM day.sale_date)::INTEGER, 3) - 1) * 0.01)
        ),
        2
    ) AS unit_price
FROM gap_catalog catalog
CROSS JOIN gap_days day
WHERE EXISTS (SELECT 1 FROM gap_buyers)
  AND NOT EXISTS (
      SELECT 1
      FROM order_items existing_item
      JOIN orders existing_order ON existing_order.id = existing_item.order_id
      WHERE existing_item.product_id = catalog.id
        AND existing_order.status = 'DELIVERED'::order_status
        AND existing_order.created_at::DATE = day.sale_date
  );

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
    buyer.id,
    0.00,
    15000.00,
    0.00,
    15000.00,
    'DELIVERED'::order_status,
    '[DAILY-GAP] s'
        || basket.seller_id
        || ' '
        || TO_CHAR(basket.sale_date, 'YYYY-MM-DD'),
    basket.sale_date + TIME '10:00:00',
    basket.sale_date + TIME '18:00:00'
FROM (
    SELECT DISTINCT seller_id, sale_date
    FROM gap_needed
) basket
JOIN gap_buyers buyer
  ON buyer.rn = (
      MOD(
          basket.seller_id + EXTRACT(DOY FROM basket.sale_date)::INTEGER - 1,
          (SELECT COUNT(*)::INTEGER FROM gap_buyers)
      ) + 1
  )
WHERE NOT EXISTS (
    SELECT 1
    FROM orders existing_order
    WHERE existing_order.shipping_address =
        '[DAILY-GAP] s'
        || basket.seller_id
        || ' '
        || TO_CHAR(basket.sale_date, 'YYYY-MM-DD')
);

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
    demo_order.id,
    line.product_id,
    line.seller_id,
    line.product_name,
    line.quantity,
    line.unit_price,
    ROUND(line.unit_price * line.quantity, 2)
FROM orders demo_order
JOIN gap_needed line
  ON demo_order.shipping_address =
        '[DAILY-GAP] s'
        || line.seller_id
        || ' '
        || TO_CHAR(line.sale_date, 'YYYY-MM-DD')
WHERE demo_order.shipping_address LIKE '[DAILY-GAP] %'
  AND NOT EXISTS (
      SELECT 1
      FROM order_items existing_item
      WHERE existing_item.order_id = demo_order.id
        AND existing_item.product_id = line.product_id
  );

DELETE FROM orders orphan
WHERE orphan.shipping_address LIKE '[DAILY-GAP] %'
  AND NOT EXISTS (
      SELECT 1
      FROM order_items item
      WHERE item.order_id = orphan.id
  );

UPDATE orders demo_order
SET subtotal_amount = totals.subtotal,
    total_amount = totals.subtotal + demo_order.shipping_fee
        - demo_order.discount_amount,
    updated_at = demo_order.created_at + INTERVAL '8 hours'
FROM (
    SELECT item.order_id, ROUND(SUM(item.subtotal), 2) AS subtotal
    FROM order_items item
    GROUP BY item.order_id
) totals
WHERE totals.order_id = demo_order.id
  AND demo_order.shipping_address LIKE '[DAILY-GAP] %';

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
    demo_order.id,
    'VNPAY'::payment_method_enum,
    'VNPAY',
    demo_order.total_amount,
    'SUCCESS'::payment_status,
    'TXN_DAILYGAP_' || demo_order.id,
    'VND',
    demo_order.created_at + INTERVAL '15 minutes',
    demo_order.created_at
FROM orders demo_order
WHERE demo_order.shipping_address LIKE '[DAILY-GAP] %'
  AND NOT EXISTS (
      SELECT 1
      FROM payments existing_payment
      WHERE existing_payment.order_id = demo_order.id
  );

INSERT INTO order_tracking (order_id, event, note, updated_by, created_at)
SELECT
    demo_order.id,
    tracking.event,
    'Bổ sung lịch sử bán theo ngày — cùng nhịp seed DSS.',
    CASE
        WHEN tracking.event IN (
            'CREATED'::order_tracking_event,
            'PAYMENT_SUCCESS'::order_tracking_event
        ) THEN demo_order.user_id
        ELSE COALESCE(
            (
                SELECT item.seller_id
                FROM order_items item
                WHERE item.order_id = demo_order.id
                ORDER BY item.id
                LIMIT 1
            ),
            demo_order.user_id
        )
    END,
    demo_order.created_at + tracking.delay
FROM orders demo_order
CROSS JOIN (
    VALUES
        ('CREATED'::order_tracking_event, INTERVAL '0 minutes'),
        ('PAYMENT_SUCCESS'::order_tracking_event, INTERVAL '15 minutes'),
        ('CONFIRMED'::order_tracking_event, INTERVAL '2 hours'),
        ('SHIPPED'::order_tracking_event, INTERVAL '8 hours'),
        ('DELIVERED'::order_tracking_event, INTERVAL '1 day')
) AS tracking(event, delay)
WHERE demo_order.shipping_address LIKE '[DAILY-GAP] %'
  AND NOT EXISTS (
      SELECT 1
      FROM order_tracking existing_tracking
      WHERE existing_tracking.order_id = demo_order.id
        AND existing_tracking.event = tracking.event
  );
