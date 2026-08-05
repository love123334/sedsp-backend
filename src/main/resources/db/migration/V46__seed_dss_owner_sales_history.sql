-- V46: Attach 121-day DELIVERED sales history to products owned by DSS demo sellers.
-- Source logic: feature/platform-revenue-management db/seed/dss_demo_data.sql
-- Why: V43 seeded global [DSS-DEMO] baskets preferred older products (ORDER BY id LIMIT 20),
-- so seller.dss.demo@example.com products had no sales → DSS demand/price/what-if fail.
-- Idempotent via shipping_address marker [DSS-OWNER-DEMO] YYYY-MM-DD.

CREATE TEMP TABLE dss_owner_products ON COMMIT DROP AS
SELECT p.id,
       p.seller_id,
       p.name,
       p.price,
       COALESCE(p.cost_price, ROUND(p.price * 0.6, 2)) AS cost_price
FROM products p
JOIN users seller ON seller.id = p.seller_id
WHERE p.deleted_at IS NULL
  AND p.status = 'ACTIVE'
  AND p.price > 0
  AND seller.deleted_at IS NULL
  AND seller.status = 'ACTIVE'
  AND (
      seller.email = 'seller.dss.demo@example.com'
      OR seller.email LIKE 'seller.dss.demo.%@example.com'
  )
ORDER BY p.id;

UPDATE products p
SET cost_price = ROUND(p.price * 0.6, 2),
    updated_at = CURRENT_TIMESTAMP
FROM dss_owner_products owned
WHERE p.id = owned.id
  AND p.cost_price IS NULL;

INSERT INTO price_history (
    product_id,
    old_price,
    new_price,
    changed_by,
    changed_at
)
SELECT
    p.id,
    ROUND(p.price * regime.old_factor, 2),
    ROUND(p.price * regime.new_factor, 2),
    p.seller_id,
    (CURRENT_DATE + regime.day_offset) + TIME '09:00:00'
FROM dss_owner_products p
CROSS JOIN (
    VALUES
        (-90, 0.90::NUMERIC, 0.95::NUMERIC),
        (-60, 0.95::NUMERIC, 1.05::NUMERIC),
        (-30, 1.05::NUMERIC, 1.00::NUMERIC)
) AS regime(day_offset, old_factor, new_factor)
WHERE NOT EXISTS (
    SELECT 1
    FROM price_history history
    WHERE history.product_id = p.id
      AND history.changed_at =
          (CURRENT_DATE + regime.day_offset) + TIME '09:00:00'
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
    customer.id,
    0.00,
    15000.00,
    0.00,
    15000.00,
    'DELIVERED'::order_status,
    '[DSS-OWNER-DEMO] ' || TO_CHAR(sale.sale_date, 'YYYY-MM-DD'),
    sale.sale_date + TIME '10:00:00',
    sale.sale_date + TIME '18:00:00'
FROM GENERATE_SERIES(
    CURRENT_DATE - 120,
    CURRENT_DATE,
    INTERVAL '1 day'
) AS sale(sale_date)
JOIN users customer
  ON customer.email = 'customer.dss.demo.'
      || (MOD((CURRENT_DATE - sale.sale_date::DATE), 12) + 1)
      || '@example.com'
WHERE EXISTS (SELECT 1 FROM dss_owner_products)
  AND NOT EXISTS (
    SELECT 1
    FROM orders existing_order
    WHERE existing_order.shipping_address =
        '[DSS-OWNER-DEMO] ' || TO_CHAR(sale.sale_date, 'YYYY-MM-DD')
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
    product.id,
    product.seller_id,
    product.name,
    quantity.value,
    price.value,
    ROUND(price.value * quantity.value, 2)
FROM orders demo_order
JOIN dss_owner_products product ON TRUE
CROSS JOIN LATERAL (
    SELECT CASE
        WHEN demo_order.created_at::DATE <= CURRENT_DATE - 91
            THEN ROUND(product.price * 0.90, 2)
        WHEN demo_order.created_at::DATE <= CURRENT_DATE - 61
            THEN ROUND(product.price * 0.95, 2)
        WHEN demo_order.created_at::DATE <= CURRENT_DATE - 31
            THEN ROUND(product.price * 1.05, 2)
        ELSE product.price
    END AS value
) price
CROSS JOIN LATERAL (
    SELECT GREATEST(
        1,
        ROUND(
            (
                8
                + MOD(product.id, 4)
                + CASE
                    WHEN EXTRACT(ISODOW FROM demo_order.created_at) IN (6, 7)
                        THEN 2
                    ELSE 0
                  END
            )
            * CASE
                WHEN demo_order.created_at::DATE <= CURRENT_DATE - 91 THEN 1.25
                WHEN demo_order.created_at::DATE <= CURRENT_DATE - 61 THEN 1.12
                WHEN demo_order.created_at::DATE <= CURRENT_DATE - 31 THEN 0.82
                ELSE 1.00
              END
        )::INTEGER
    ) AS value
) quantity
WHERE demo_order.shipping_address LIKE '[DSS-OWNER-DEMO] %'
  AND demo_order.created_at::DATE BETWEEN CURRENT_DATE - 120 AND CURRENT_DATE
  AND NOT EXISTS (
      SELECT 1
      FROM order_items existing_item
      WHERE existing_item.order_id = demo_order.id
        AND existing_item.product_id = product.id
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
  AND demo_order.shipping_address LIKE '[DSS-OWNER-DEMO] %';

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
    'TXN_DSS_OWNER_' || demo_order.id,
    'VND',
    demo_order.created_at + INTERVAL '15 minutes',
    demo_order.created_at
FROM orders demo_order
WHERE demo_order.shipping_address LIKE '[DSS-OWNER-DEMO] %'
  AND NOT EXISTS (
      SELECT 1
      FROM payments existing_payment
      WHERE existing_payment.order_id = demo_order.id
  );

INSERT INTO order_tracking (order_id, event, updated_by, created_at)
SELECT
    demo_order.id,
    tracking.event,
    CASE
        WHEN tracking.event IN (
            'CREATED'::order_tracking_event,
            'PAYMENT_SUCCESS'::order_tracking_event
        ) THEN demo_order.user_id
        ELSE (
            SELECT seller_id
            FROM order_items oi
            WHERE oi.order_id = demo_order.id
            ORDER BY oi.id
            LIMIT 1
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
WHERE demo_order.shipping_address LIKE '[DSS-OWNER-DEMO] %'
  AND NOT EXISTS (
      SELECT 1
      FROM order_tracking existing_tracking
      WHERE existing_tracking.order_id = demo_order.id
        AND existing_tracking.event = tracking.event
  );
