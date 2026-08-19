-- V60: denser seller order history 06/08/2026 → CURRENT_DATE.
-- Tester asked for "nhiều nhiều tí" — many separate orders per seller per day.
-- Same event/P&L mix as V59 (8/8 sale, tựu trường, weekend). Does not skip
-- product-days already seeded. Leaves dss-forecast-* profiles alone.
-- Idempotent via [RECENT-VOL] marker.

UPDATE products p
SET cost_price = ROUND(p.price * 0.62, 2),
    updated_at = CURRENT_TIMESTAMP
FROM users seller
JOIN roles role ON role.id = seller.role_id
WHERE p.seller_id = seller.id
  AND p.deleted_at IS NULL
  AND p.status = 'ACTIVE'
  AND p.price > 0
  AND p.cost_price IS NULL
  AND seller.deleted_at IS NULL
  AND seller.status = 'ACTIVE'
  AND role.name = 'SELLER';

CREATE TEMP TABLE vol_buyers ON COMMIT DROP AS
SELECT u.id,
       ROW_NUMBER() OVER (ORDER BY u.id) AS rn
FROM users u
JOIN roles r ON r.id = u.role_id
WHERE r.name = 'CUSTOMER'
  AND u.status = 'ACTIVE'
  AND u.deleted_at IS NULL;

CREATE TEMP TABLE vol_catalog ON COMMIT DROP AS
SELECT
    p.id,
    p.seller_id,
    p.name,
    p.price,
    COALESCE(p.cost_price, ROUND(p.price * 0.62, 2)) AS cost_price,
    CASE
        WHEN COALESCE(c.slug, '') IN (
            'phones', 'laptops', 'tablets', 'electronics-accessories',
            'dss-demo-electronics'
        ) OR p.name ILIKE ANY (ARRAY['%laptop%', '%iphone%', '%samsung%', '%tai nghe%', '%chuột%', '%bàn phím%'])
            THEN 'ELECTRONICS'
        WHEN COALESCE(c.slug, '') IN ('kitchen', 'furniture', 'decor')
          OR p.name ILIKE ANY (ARRAY['%nồi%', '%air fryer%'])
            THEN 'HOME'
        WHEN COALESCE(c.slug, '') IN (
            'men-clothing', 'women-clothing', 'shoes', 'skincare', 'makeup'
        ) THEN 'FASHION'
        WHEN COALESCE(c.slug, '') IN ('fitness-equipment', 'outdoor-gear')
            THEN 'SPORTS'
        ELSE 'OTHER'
    END AS vertical,
    (
        p.name ILIKE ANY (ARRAY['%balo%', '%backpack%', '%laptop%'])
        OR COALESCE(c.slug, '') IN ('laptops', 'electronics-accessories', 'tablets')
    ) AS school_affinity
FROM products p
JOIN users seller ON seller.id = p.seller_id
JOIN roles role ON role.id = seller.role_id
LEFT JOIN categories c ON c.id = p.category_id
WHERE p.deleted_at IS NULL
  AND p.status = 'ACTIVE'
  AND p.price > 0
  AND seller.deleted_at IS NULL
  AND seller.status = 'ACTIVE'
  AND role.name = 'SELLER'
  AND p.slug NOT LIKE 'dss-forecast-%';

CREATE TEMP TABLE vol_days ON COMMIT DROP AS
SELECT d::DATE AS sale_date
FROM GENERATE_SERIES(
    DATE '2026-08-06',
    GREATEST(DATE '2026-08-06', CURRENT_DATE),
    INTERVAL '1 day'
) AS d
WHERE CURRENT_DATE >= DATE '2026-08-06';

CREATE TEMP TABLE vol_baskets ON COMMIT DROP AS
SELECT
    catalog.seller_id,
    day.sale_date,
    basket.basket_no,
    EXTRACT(ISODOW FROM day.sale_date)::INTEGER AS isodow,
    CASE
        WHEN day.sale_date BETWEEN DATE '2026-08-07' AND DATE '2026-08-09'
            THEN 'DOUBLE_88'
        WHEN day.sale_date BETWEEN DATE '2026-08-10' AND DATE '2026-08-25'
            THEN 'BACK_TO_SCHOOL'
        ELSE 'NONE'
    END AS event_code
FROM (SELECT DISTINCT seller_id FROM vol_catalog) catalog
CROSS JOIN vol_days day
CROSS JOIN GENERATE_SERIES(1, 8) AS basket(basket_no)
WHERE EXISTS (SELECT 1 FROM vol_buyers)
  AND (
      basket.basket_no <= 5
      OR (
          basket.basket_no IN (6, 7)
          AND (
              EXTRACT(ISODOW FROM day.sale_date) IN (5, 6, 7)
              OR day.sale_date BETWEEN DATE '2026-08-07' AND DATE '2026-08-09'
              OR day.sale_date BETWEEN DATE '2026-08-10' AND DATE '2026-08-25'
          )
      )
      OR (
          basket.basket_no = 8
          AND (
              day.sale_date = DATE '2026-08-08'
              OR EXTRACT(ISODOW FROM day.sale_date) = 7
          )
      )
  );

CREATE TEMP TABLE vol_lines ON COMMIT DROP AS
SELECT
    basket.seller_id,
    basket.sale_date,
    basket.basket_no,
    basket.event_code,
    product.id AS product_id,
    product.name AS product_name,
    GREATEST(
        1,
        (1 + MOD(product.id + basket.basket_no, 2))
        + CASE WHEN basket.isodow IN (6, 7) THEN 1 ELSE 0 END
        + CASE
            WHEN basket.event_code = 'DOUBLE_88'
                 AND product.vertical IN ('ELECTRONICS', 'HOME') THEN 1
            WHEN basket.event_code = 'BACK_TO_SCHOOL'
                 AND product.school_affinity THEN 1
            ELSE 0
          END
    )::INTEGER AS quantity,
    CASE
        WHEN basket.event_code = 'DOUBLE_88'
             AND MOD(product.id, 13) = 0
            THEN ROUND(product.cost_price * 0.95, 2)
        WHEN basket.event_code = 'DOUBLE_88'
             AND product.vertical IN ('ELECTRONICS', 'HOME')
            THEN ROUND(product.price * 0.88, 2)
        WHEN basket.event_code = 'DOUBLE_88'
            THEN ROUND(product.price * 0.92, 2)
        WHEN basket.event_code = 'BACK_TO_SCHOOL'
             AND product.school_affinity
            THEN ROUND(product.price * 0.95, 2)
        WHEN basket.isodow IN (6, 7)
             AND MOD(product.id, 5) = 0
            THEN ROUND(product.price * 0.97, 2)
        ELSE product.price
    END AS unit_price
FROM vol_baskets basket
JOIN vol_catalog product ON product.seller_id = basket.seller_id
WHERE MOD(
          product.id
          + EXTRACT(DOY FROM basket.sale_date)::INTEGER * 7
          + basket.basket_no * 19,
          3
      ) < 2;

CREATE TEMP TABLE vol_lines_capped ON COMMIT DROP AS
SELECT *
FROM (
    SELECT
        vol_lines.*,
        ROW_NUMBER() OVER (
            PARTITION BY seller_id, sale_date, basket_no
            ORDER BY product_id
        ) AS line_no
    FROM vol_lines
) ranked
WHERE ranked.line_no <= 5;

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
    CASE
        WHEN basket.event_code = 'DOUBLE_88' THEN 19000.00
        WHEN basket.isodow IN (6, 7) THEN 22000.00
        ELSE 25000.00
    END,
    0.00,
    CASE
        WHEN basket.event_code = 'DOUBLE_88' THEN 19000.00
        WHEN basket.isodow IN (6, 7) THEN 22000.00
        ELSE 25000.00
    END,
    CASE
        WHEN basket.sale_date = CURRENT_DATE THEN
            CASE MOD(basket.seller_id + basket.basket_no, 4)
                WHEN 0 THEN 'PENDING'::order_status
                WHEN 1 THEN 'PAID'::order_status
                WHEN 2 THEN 'PROCESSING'::order_status
                ELSE 'SHIPPING'::order_status
            END
        WHEN basket.sale_date = CURRENT_DATE - 1 THEN
            CASE MOD(basket.basket_no, 2)
                WHEN 0 THEN 'SHIPPING'::order_status
                ELSE 'DELIVERED'::order_status
            END
        WHEN basket.sale_date <= CURRENT_DATE - 3
             AND MOD(
                 basket.seller_id
                 + EXTRACT(DOY FROM basket.sale_date)::INTEGER
                 + basket.basket_no,
                 16
             ) = 0
            THEN 'CANCELLED'::order_status
        WHEN basket.sale_date BETWEEN DATE '2026-08-09' AND DATE '2026-08-11'
             AND MOD(basket.seller_id + basket.basket_no, 19) = 0
            THEN 'REFUNDED'::order_status
        ELSE 'DELIVERED'::order_status
    END,
    '[RECENT-VOL] s'
        || basket.seller_id
        || ' '
        || TO_CHAR(basket.sale_date, 'YYYY-MM-DD')
        || ' #'
        || basket.basket_no,
    basket.sale_date
        + TIME '08:00:00'
        + ((basket.basket_no - 1) * INTERVAL '90 minutes'),
    basket.sale_date
        + TIME '08:00:00'
        + ((basket.basket_no - 1) * INTERVAL '90 minutes')
        + INTERVAL '5 hours'
FROM (
    SELECT DISTINCT seller_id, sale_date, basket_no, event_code, isodow
    FROM vol_lines_capped
) basket
JOIN vol_buyers buyer
  ON buyer.rn = (
      MOD(
          basket.seller_id
          + EXTRACT(DOY FROM basket.sale_date)::INTEGER
          + basket.basket_no * 3
          - 1,
          (SELECT COUNT(*)::INTEGER FROM vol_buyers)
      ) + 1
  )
WHERE NOT EXISTS (
    SELECT 1
    FROM orders existing_order
    WHERE existing_order.shipping_address =
        '[RECENT-VOL] s'
        || basket.seller_id
        || ' '
        || TO_CHAR(basket.sale_date, 'YYYY-MM-DD')
        || ' #'
        || basket.basket_no
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
JOIN vol_lines_capped line
  ON demo_order.shipping_address =
        '[RECENT-VOL] s'
        || line.seller_id
        || ' '
        || TO_CHAR(line.sale_date, 'YYYY-MM-DD')
        || ' #'
        || line.basket_no
WHERE demo_order.shipping_address LIKE '[RECENT-VOL] %'
  AND NOT EXISTS (
      SELECT 1
      FROM order_items existing_item
      WHERE existing_item.order_id = demo_order.id
        AND existing_item.product_id = line.product_id
  );

DELETE FROM orders orphan
WHERE orphan.shipping_address LIKE '[RECENT-VOL] %'
  AND NOT EXISTS (
      SELECT 1
      FROM order_items item
      WHERE item.order_id = orphan.id
  );

UPDATE orders demo_order
SET subtotal_amount = totals.subtotal,
    total_amount = GREATEST(
        0,
        totals.subtotal + demo_order.shipping_fee - demo_order.discount_amount
    ),
    updated_at = demo_order.created_at + INTERVAL '4 hours'
FROM (
    SELECT item.order_id, ROUND(SUM(item.subtotal), 2) AS subtotal
    FROM order_items item
    GROUP BY item.order_id
) totals
WHERE totals.order_id = demo_order.id
  AND demo_order.shipping_address LIKE '[RECENT-VOL] %';

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
    CASE
        WHEN MOD(demo_order.id, 2) = 0 THEN 'VNPAY'::payment_method_enum
        ELSE 'MOMO'::payment_method_enum
    END,
    CASE WHEN MOD(demo_order.id, 2) = 0 THEN 'VNPAY' ELSE 'MOMO' END,
    demo_order.total_amount,
    CASE demo_order.status
        WHEN 'PENDING'::order_status THEN 'PENDING'::payment_status
        WHEN 'CANCELLED'::order_status THEN 'FAILED'::payment_status
        ELSE 'SUCCESS'::payment_status
    END,
    'TXN_RECENTVOL_' || demo_order.id,
    'VND',
    CASE
        WHEN demo_order.status IN (
            'PENDING'::order_status,
            'CANCELLED'::order_status
        ) THEN NULL
        ELSE demo_order.created_at + INTERVAL '9 minutes'
    END,
    demo_order.created_at
FROM orders demo_order
WHERE demo_order.shipping_address LIKE '[RECENT-VOL] %'
  AND NOT EXISTS (
      SELECT 1
      FROM payments existing_payment
      WHERE existing_payment.order_id = demo_order.id
  );

INSERT INTO order_tracking (order_id, event, note, updated_by, created_at)
SELECT
    demo_order.id,
    tracking.event,
    tracking.note,
    CASE
        WHEN tracking.event IN (
            'CREATED'::order_tracking_event,
            'PAYMENT_SUCCESS'::order_tracking_event,
            'PAYMENT_FAILED'::order_tracking_event,
            'CANCELLED_BY_USER'::order_tracking_event
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
JOIN (
    VALUES
        (
            'CREATED'::order_tracking_event,
            INTERVAL '0 minutes',
            'Đơn được tạo.',
            ARRAY[
                'PENDING', 'PAID', 'PROCESSING', 'SHIPPING',
                'DELIVERED', 'CANCELLED', 'REFUNDED'
            ]::order_status[]
        ),
        (
            'PAYMENT_SUCCESS'::order_tracking_event,
            INTERVAL '9 minutes',
            'Thanh toán thành công.',
            ARRAY[
                'PAID', 'PROCESSING', 'SHIPPING', 'DELIVERED', 'REFUNDED'
            ]::order_status[]
        ),
        (
            'PAYMENT_FAILED'::order_tracking_event,
            INTERVAL '6 minutes',
            'Thanh toán không thành công — đơn hủy.',
            ARRAY['CANCELLED']::order_status[]
        ),
        (
            'CONFIRMED'::order_tracking_event,
            INTERVAL '90 minutes',
            'Người bán đã xác nhận đơn.',
            ARRAY[
                'PROCESSING', 'SHIPPING', 'DELIVERED', 'REFUNDED'
            ]::order_status[]
        ),
        (
            'SHIPPED'::order_tracking_event,
            INTERVAL '6 hours',
            'Đơn đã giao cho đơn vị vận chuyển.',
            ARRAY['SHIPPING', 'DELIVERED', 'REFUNDED']::order_status[]
        ),
        (
            'DELIVERED'::order_tracking_event,
            INTERVAL '26 hours',
            'Giao hàng thành công.',
            ARRAY['DELIVERED', 'REFUNDED']::order_status[]
        ),
        (
            'CANCELLED_BY_USER'::order_tracking_event,
            INTERVAL '25 minutes',
            'Khách hủy đơn.',
            ARRAY['CANCELLED']::order_status[]
        ),
        (
            'CANCELLED_BY_ADMIN'::order_tracking_event,
            INTERVAL '2 days',
            'Hoàn sau sale 8/8 — khách đổi ý.',
            ARRAY['REFUNDED']::order_status[]
        )
) AS tracking(event, delay, note, statuses)
  ON demo_order.status = ANY (tracking.statuses)
WHERE demo_order.shipping_address LIKE '[RECENT-VOL] %'
  AND NOT EXISTS (
      SELECT 1
      FROM order_tracking existing_tracking
      WHERE existing_tracking.order_id = demo_order.id
        AND existing_tracking.event = tracking.event
  );
