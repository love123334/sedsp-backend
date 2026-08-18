-- V57: 180-day demand-forecast dataset with recognizable trend profiles.
--
-- Four isolated products are used so earlier demo orders cannot distort the
-- intended signals.
--
-- Idempotent via product slugs, [DSS-FORECAST-TREND] markers and NOT EXISTS.

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
            'dss-forecast-growing-demand',
            'Sản phẩm demo có nhu cầu tăng dần và hiệu ứng cuối tuần.',
            450000.00::NUMERIC,
            270000.00::NUMERIC
        ),
        (
            'DSS Forecast - Nhu cầu giảm',
            'dss-forecast-declining-demand',
            'Sản phẩm demo có nhu cầu giảm dần theo thời gian.',
            850000.00::NUMERIC,
            510000.00::NUMERIC
        ),
        (
            'DSS Forecast - Ổn định theo tuần',
            'dss-forecast-weekly-stable-demand',
            'Sản phẩm demo có nhu cầu ổn định và tăng vào cuối tuần.',
            1250000.00::NUMERIC,
            750000.00::NUMERIC
        ),
        (
            'DSS Forecast - Bán gián đoạn',
            'dss-forecast-intermittent-demand',
            'Sản phẩm demo có nhiều ngày không bán và xu hướng tăng nhẹ.',
            320000.00::NUMERIC,
            190000.00::NUMERIC
        )
) AS demo(name, slug, description, price, cost_price)
WHERE seller.email = 'seller.dss.demo@example.com'
  AND category.slug = 'dss-demo-electronics'
  AND NOT EXISTS (
      SELECT 1
      FROM products existing_product
      WHERE existing_product.slug = demo.slug
        AND existing_product.deleted_at IS NULL
  );

INSERT INTO inventory (product_id, available_quantity, reserved_quantity)
SELECT product.id, 1000, 0
FROM products product
WHERE product.slug LIKE 'dss-forecast-%'
  AND product.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM inventory existing_inventory
      WHERE existing_inventory.product_id = product.id
  );

CREATE TEMP TABLE dss_forecast_profiles ON COMMIT DROP AS
SELECT
    product.id,
    product.seller_id,
    product.name,
    product.price,
    profile.profile_name
FROM (
    VALUES
        ('dss-forecast-growing-demand', 'UPWARD'),
        ('dss-forecast-declining-demand', 'DOWNWARD'),
        ('dss-forecast-weekly-stable-demand', 'STABLE_SEASONAL'),
        ('dss-forecast-intermittent-demand', 'INTERMITTENT_UPWARD')
) AS profile(product_slug, profile_name)
JOIN products product ON product.slug = profile.product_slug
WHERE product.deleted_at IS NULL
  AND product.status = 'ACTIVE';

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
    '[DSS-FORECAST-TREND] ' || TO_CHAR(sale.sale_date, 'YYYY-MM-DD'),
    sale.sale_date + TIME '11:00:00',
    sale.sale_date + TIME '19:00:00'
FROM GENERATE_SERIES(
    CURRENT_DATE - 179,
    CURRENT_DATE,
    INTERVAL '1 day'
) AS sale(sale_date)
JOIN users customer
  ON customer.email = 'customer.dss.demo.'
      || (MOD((CURRENT_DATE - sale.sale_date::DATE), 12) + 1)
      || '@example.com'
WHERE EXISTS (SELECT 1 FROM dss_forecast_profiles)
  AND NOT EXISTS (
      SELECT 1
      FROM orders existing_order
      WHERE existing_order.shipping_address =
          '[DSS-FORECAST-TREND] ' || TO_CHAR(sale.sale_date, 'YYYY-MM-DD')
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
    forecast_order.id,
    product.id,
    product.seller_id,
    product.name,
    demand.quantity,
    product.price,
    ROUND(product.price * demand.quantity, 2)
FROM orders forecast_order
JOIN dss_forecast_profiles product ON TRUE
CROSS JOIN LATERAL (
    SELECT 179 - (CURRENT_DATE - forecast_order.created_at::DATE) AS recency_index
) age
CROSS JOIN LATERAL (
    SELECT CASE product.profile_name
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
    END::INTEGER AS quantity
) demand
WHERE forecast_order.shipping_address LIKE '[DSS-FORECAST-TREND] %'
  AND forecast_order.created_at::DATE BETWEEN CURRENT_DATE - 179 AND CURRENT_DATE
  -- Omit one day out of four to create real zero-demand observations. The
  -- forecast engine fills these missing dates with quantity zero.
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
    GROUP BY item.order_id
) totals
WHERE totals.order_id = forecast_order.id
  AND forecast_order.shipping_address LIKE '[DSS-FORECAST-TREND] %';

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
    'DSS_FORECAST_SEED',
    forecast_order.total_amount,
    'SUCCESS'::payment_status,
    'DSS-FORECAST-' || TO_CHAR(forecast_order.created_at, 'YYYYMMDD'),
    'VND',
    forecast_order.created_at + INTERVAL '10 minutes',
    forecast_order.created_at
FROM orders forecast_order
WHERE forecast_order.shipping_address LIKE '[DSS-FORECAST-TREND] %'
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
    'Delivered order generated for demand-forecast trend testing.',
    MIN(item.seller_id),
    forecast_order.created_at + INTERVAL '8 hours'
FROM orders forecast_order
JOIN order_items item ON item.order_id = forecast_order.id
WHERE forecast_order.shipping_address LIKE '[DSS-FORECAST-TREND] %'
  AND NOT EXISTS (
      SELECT 1
      FROM order_tracking existing_tracking
      WHERE existing_tracking.order_id = forecast_order.id
        AND existing_tracking.event = 'DELIVERED'
  )
GROUP BY forecast_order.id, forecast_order.created_at;
