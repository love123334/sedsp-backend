-- DSS demo dataset for FR01, FR03 and FR07.
-- PostgreSQL only. Run manually in a development database.
--
-- Primary demo seller login:
--   email:    seller.dss.demo@example.com
--   password: password
--
-- The seed creates base sales plus dedicated 180-day demand-forecast profiles,
-- realistic order-status distributions and three historical price changes.
-- It uses at most 20 active products owned by
-- active SELLER users. Existing products are preferred; four demo sellers,
-- twelve customers and ten products are added so an empty development database
-- is also immediately usable for DSS and platform-performance reporting.

BEGIN;

-- ---------------------------------------------------------------------------
-- Demo identities and catalogue fallback
-- ---------------------------------------------------------------------------

INSERT INTO users (
    username,
    email,
    password,
    full_name,
    status,
    role_id,
    store_name,
    business_email,
    created_at,
    updated_at
)
SELECT
    'seller.dss.demo',
    'seller.dss.demo@example.com',
    '$2a$10$MDes8qRTuKmeopk7NxNZv.gZV5kBFMP7cQ2SlVMMfXT6aXqqHnukK',
    'DSS Demo Seller',
    'ACTIVE'::user_status,
    r.id,
    'DSS Demo Store',
    'seller.dss.demo@example.com',
    CURRENT_TIMESTAMP - INTERVAL '180 days',
    CURRENT_TIMESTAMP - INTERVAL '180 days'
FROM roles r
WHERE r.name = 'SELLER'
  AND NOT EXISTS (
      SELECT 1
      FROM users u
      WHERE u.email = 'seller.dss.demo@example.com'
  );

INSERT INTO users (
    username,
    email,
    password,
    full_name,
    status,
    role_id,
    store_name,
    business_email,
    created_at,
    updated_at
)
SELECT
    demo.username,
    demo.email,
    '$2a$10$MDes8qRTuKmeopk7NxNZv.gZV5kBFMP7cQ2SlVMMfXT6aXqqHnukK',
    demo.full_name,
    'ACTIVE'::user_status,
    r.id,
    demo.store_name,
    demo.email,
    CURRENT_TIMESTAMP - INTERVAL '180 days',
    CURRENT_TIMESTAMP - INTERVAL '180 days'
FROM roles r
CROSS JOIN (
    VALUES
        ('seller.dss.demo.2', 'seller.dss.demo.2@example.com', 'DSS Fashion Seller', 'DSS Fashion Store'),
        ('seller.dss.demo.3', 'seller.dss.demo.3@example.com', 'DSS Sports Seller', 'DSS Sports Store'),
        ('seller.dss.demo.4', 'seller.dss.demo.4@example.com', 'DSS Home Seller', 'DSS Home Store')
) AS demo(username, email, full_name, store_name)
WHERE r.name = 'SELLER'
  AND NOT EXISTS (
      SELECT 1
      FROM users u
      WHERE u.email = demo.email
  );

INSERT INTO users (
    username,
    email,
    password,
    full_name,
    status,
    role_id,
    created_at,
    updated_at
)
SELECT
    demo.username,
    demo.email,
    '$2a$10$MDes8qRTuKmeopk7NxNZv.gZV5kBFMP7cQ2SlVMMfXT6aXqqHnukK',
    demo.full_name,
    'ACTIVE'::user_status,
    r.id,
    CURRENT_TIMESTAMP - INTERVAL '180 days',
    CURRENT_TIMESTAMP - INTERVAL '180 days'
FROM roles r
CROSS JOIN LATERAL (
    SELECT 'customer.dss.demo.' || sequence_number AS username,
           'customer.dss.demo.' || sequence_number || '@example.com' AS email,
           'DSS Customer ' || LPAD(sequence_number::TEXT, 2, '0') AS full_name
    FROM GENERATE_SERIES(1, 12) AS generated(sequence_number)
) AS demo
WHERE r.name = 'CUSTOMER'
  AND NOT EXISTS (
      SELECT 1
      FROM users u
      WHERE u.email = demo.email
  );

-- Keep the documented demo credential deterministic when the seed is rerun.
UPDATE users
SET password = '$2a$10$MDes8qRTuKmeopk7NxNZv.gZV5kBFMP7cQ2SlVMMfXT6aXqqHnukK',
    updated_at = CURRENT_TIMESTAMP
WHERE email = 'seller.dss.demo@example.com'
   OR email LIKE 'seller.dss.demo.%@example.com'
   OR email LIKE 'customer.dss.demo.%@example.com';

INSERT INTO categories (name, slug, created_at, updated_at)
SELECT
    'DSS Demo Electronics',
    'dss-demo-electronics',
    CURRENT_TIMESTAMP - INTERVAL '180 days',
    CURRENT_TIMESTAMP - INTERVAL '180 days'
WHERE NOT EXISTS (
    SELECT 1
    FROM categories c
    WHERE c.slug = 'dss-demo-electronics'
      AND c.deleted_at IS NULL
);

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
    'DSS demonstration product with simulated sales and price history.',
    demo.price,
    demo.cost_price,
    'ACTIVE'::product_status,
    CURRENT_TIMESTAMP - INTERVAL '150 days',
    CURRENT_TIMESTAMP
FROM users seller
CROSS JOIN categories category
CROSS JOIN (
    VALUES
        ('Wireless Mouse Pro', 'dss-demo-wireless-mouse-pro', 350000.00::NUMERIC, 210000.00::NUMERIC),
        ('Mechanical Keyboard K87', 'dss-demo-mechanical-keyboard-k87', 1290000.00::NUMERIC, 780000.00::NUMERIC),
        ('Noise Cancelling Headphones', 'dss-demo-noise-cancelling-headphones', 1890000.00::NUMERIC, 1150000.00::NUMERIC),
        ('USB-C Hub 8-in-1', 'dss-demo-usb-c-hub-8-in-1', 990000.00::NUMERIC, 590000.00::NUMERIC)
) AS demo(name, slug, price, cost_price)
WHERE seller.email = 'seller.dss.demo@example.com'
  AND category.slug = 'dss-demo-electronics'
  AND NOT EXISTS (
      SELECT 1
      FROM products p
      WHERE p.slug = demo.slug
        AND p.deleted_at IS NULL
  );

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
    'Platform performance demonstration product with simulated sales history.',
    demo.price,
    demo.cost_price,
    'ACTIVE'::product_status,
    CURRENT_TIMESTAMP - INTERVAL '150 days',
    CURRENT_TIMESTAMP
FROM users seller
CROSS JOIN categories category
JOIN (
    VALUES
        ('seller.dss.demo.2@example.com', 'Urban Travel Backpack', 'dss-demo-urban-travel-backpack', 750000.00::NUMERIC, 450000.00::NUMERIC),
        ('seller.dss.demo.2@example.com', 'Premium Cotton Jacket', 'dss-demo-premium-cotton-jacket', 1450000.00::NUMERIC, 870000.00::NUMERIC),
        ('seller.dss.demo.3@example.com', 'Running Shoes X1', 'dss-demo-running-shoes-x1', 1650000.00::NUMERIC, 990000.00::NUMERIC),
        ('seller.dss.demo.3@example.com', 'Fitness Smart Watch', 'dss-demo-fitness-smart-watch', 2490000.00::NUMERIC, 1540000.00::NUMERIC),
        ('seller.dss.demo.4@example.com', 'Digital Coffee Maker', 'dss-demo-digital-coffee-maker', 1750000.00::NUMERIC, 1050000.00::NUMERIC),
        ('seller.dss.demo.4@example.com', 'Smart Air Fryer', 'dss-demo-smart-air-fryer', 2200000.00::NUMERIC, 1350000.00::NUMERIC)
) AS demo(seller_email, name, slug, price, cost_price)
  ON demo.seller_email = seller.email
WHERE category.slug = 'dss-demo-electronics'
  AND NOT EXISTS (
      SELECT 1
      FROM products p
      WHERE p.slug = demo.slug
        AND p.deleted_at IS NULL
  );

-- A cost is required by FR03 and FR07. For incomplete development records,
-- estimate cost at 65% of the current selling price.
UPDATE products p
SET cost_price = ROUND(p.price * 0.65, 2),
    updated_at = CURRENT_TIMESTAMP
FROM users seller
JOIN roles role ON role.id = seller.role_id
WHERE p.seller_id = seller.id
  AND role.name = 'SELLER'
  AND seller.status = 'ACTIVE'
  AND p.status = 'ACTIVE'
  AND p.deleted_at IS NULL
  AND p.price > 0
  AND p.cost_price IS NULL;

INSERT INTO inventory (product_id, available_quantity, reserved_quantity)
SELECT p.id, 10000, 0
FROM products p
WHERE p.slug LIKE 'dss-demo-%'
  AND p.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM inventory i
      WHERE i.product_id = p.id
  );

CREATE TEMP TABLE dss_seed_products ON COMMIT DROP AS
SELECT p.id,
       p.seller_id,
       p.name,
       p.price,
       p.cost_price
FROM products p
JOIN users seller ON seller.id = p.seller_id
JOIN roles role ON role.id = seller.role_id
WHERE p.deleted_at IS NULL
  AND p.status = 'ACTIVE'
  AND p.price > 0
  AND p.cost_price IS NOT NULL
  AND seller.deleted_at IS NULL
  AND seller.status = 'ACTIVE'
  AND role.name = 'SELLER'
ORDER BY p.id
LIMIT 20;

-- ---------------------------------------------------------------------------
-- Price regimes
--
-- Day -120..-91: 90% of current price, relatively high demand
-- Day  -90..-61: 95% of current price
-- Day  -60..-31: 105% of current price, relatively low demand
-- Day  -30..today: current price
-- ---------------------------------------------------------------------------

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
FROM dss_seed_products p
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

-- ---------------------------------------------------------------------------
-- 121 daily delivered orders. Each order contains every seeded product.
-- The customer rotates daily and quantity changes inversely with price.
-- ---------------------------------------------------------------------------

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
    '[DSS-DEMO] ' || TO_CHAR(sale.sale_date, 'YYYY-MM-DD'),
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
WHERE NOT EXISTS (
    SELECT 1
    FROM orders existing_order
    WHERE existing_order.shipping_address =
        '[DSS-DEMO] ' || TO_CHAR(sale.sale_date, 'YYYY-MM-DD')
);

-- Redistribute previously seeded daily orders when this expanded dataset is
-- applied over the original three-customer version.
UPDATE orders demo_order
SET user_id = customer.id
FROM users customer
WHERE demo_order.shipping_address LIKE '[DSS-DEMO] %'
  AND customer.email = 'customer.dss.demo.'
      || (MOD((CURRENT_DATE - demo_order.created_at::DATE), 12) + 1)
      || '@example.com';

-- Add realistic non-delivered order outcomes. Their cadence produces a stable
-- mix suitable for completion/cancellation-rate reporting without altering the
-- delivered sales history used by DSS calculations.
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
    outcome.order_status,
    '[DSS-DEMO-' || outcome.order_status::TEXT || '] '
        || TO_CHAR(sale.sale_date, 'YYYY-MM-DD'),
    sale.sale_date + TIME '14:00:00',
    sale.sale_date + TIME '16:00:00'
FROM (
    VALUES
        ('CANCELLED'::order_status, 8, 1),
        ('PROCESSING'::order_status, 10, 3),
        ('PENDING'::order_status, 14, 5),
        ('SHIPPING'::order_status, 18, 7)
) AS outcome(order_status, every_days, customer_offset)
CROSS JOIN LATERAL GENERATE_SERIES(
    CURRENT_DATE - 120,
    CURRENT_DATE,
    outcome.every_days * INTERVAL '1 day'
) AS sale(sale_date)
JOIN users customer
  ON customer.email = 'customer.dss.demo.'
      || (
          MOD(
              CURRENT_DATE - sale.sale_date::DATE
                  + outcome.customer_offset,
              12
          ) + 1
      )
      || '@example.com'
WHERE NOT EXISTS (
    SELECT 1
    FROM orders existing_order
    WHERE existing_order.shipping_address =
        '[DSS-DEMO-' || outcome.order_status::TEXT || '] '
            || TO_CHAR(sale.sale_date, 'YYYY-MM-DD')
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
JOIN dss_seed_products product ON TRUE
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
                + CASE
                    WHEN MOD(EXTRACT(DAY FROM demo_order.created_at)::INTEGER, 7) = 0
                        THEN 1
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
WHERE demo_order.shipping_address LIKE '[DSS-DEMO] %'
  AND demo_order.created_at::DATE BETWEEN CURRENT_DATE - 120 AND CURRENT_DATE
  AND NOT EXISTS (
      SELECT 1
      FROM order_items existing_item
      WHERE existing_item.order_id = demo_order.id
        AND existing_item.product_id = product.id
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
    status_order.id,
    product.id,
    product.seller_id,
    product.name,
    quantity.value,
    product.price,
    ROUND(product.price * quantity.value, 2)
FROM orders status_order
JOIN LATERAL (
    SELECT seeded_product.*
    FROM dss_seed_products seeded_product
    ORDER BY seeded_product.id
    OFFSET MOD(
        status_order.id::INTEGER,
        (SELECT COUNT(*)::INTEGER FROM dss_seed_products)
    )
    LIMIT 1
) product ON TRUE
CROSS JOIN LATERAL (
    SELECT (1 + MOD(status_order.id::INTEGER, 3)) AS value
) quantity
WHERE status_order.shipping_address LIKE '[DSS-DEMO-%] %'
  AND status_order.created_at::DATE
      BETWEEN CURRENT_DATE - 120 AND CURRENT_DATE
  AND NOT EXISTS (
      SELECT 1
      FROM order_items existing_item
      WHERE existing_item.order_id = status_order.id
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
  AND demo_order.shipping_address LIKE '[DSS-DEMO] %';

UPDATE orders status_order
SET subtotal_amount = totals.subtotal,
    total_amount = totals.subtotal + status_order.shipping_fee
        - status_order.discount_amount,
    updated_at = status_order.created_at + INTERVAL '2 hours'
FROM (
    SELECT item.order_id, ROUND(SUM(item.subtotal), 2) AS subtotal
    FROM order_items item
    GROUP BY item.order_id
) totals
WHERE totals.order_id = status_order.id
  AND status_order.shipping_address LIKE '[DSS-DEMO-%] %';

-- ---------------------------------------------------------------------------
-- Dedicated 180-day demand-forecast profiles
--
-- These four profiles exercise trend, momentum, weekly seasonality and
-- zero-filled dates with lookback windows from 7 through 180 days.
-- ---------------------------------------------------------------------------

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
            2 + (age.recency_index / 22)
            + CASE WHEN EXTRACT(ISODOW FROM forecast_order.created_at) IN (6, 7)
                THEN 2 ELSE 0 END
        WHEN 'DOWNWARD' THEN
            GREATEST(
                2,
                12 - (age.recency_index / 20)
                + CASE WHEN EXTRACT(ISODOW FROM forecast_order.created_at) IN (6, 7)
                    THEN 1 ELSE 0 END
            )
        WHEN 'STABLE_SEASONAL' THEN
            6
            + CASE WHEN EXTRACT(ISODOW FROM forecast_order.created_at) IN (6, 7)
                THEN 3 ELSE 0 END
            + CASE WHEN MOD(EXTRACT(DAY FROM forecast_order.created_at)::INTEGER, 15) = 0
                THEN 2 ELSE 0 END
        ELSE
            3 + (age.recency_index / 45)
            + CASE WHEN EXTRACT(ISODOW FROM forecast_order.created_at) = 7
                THEN 2 ELSE 0 END
    END::INTEGER AS quantity
) demand
WHERE forecast_order.shipping_address LIKE '[DSS-FORECAST-TREND] %'
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
        WHEN MOD(demo_order.created_at::DATE - DATE '2000-01-01', 2) = 0
            THEN 'MOMO'::payment_method_enum
        ELSE 'VNPAY'::payment_method_enum
    END,
    'DSS_DEMO_GATEWAY',
    demo_order.total_amount,
    'SUCCESS'::payment_status,
    'DSS-DEMO-' || TO_CHAR(demo_order.created_at, 'YYYYMMDD'),
    'VND',
    demo_order.created_at + INTERVAL '5 minutes',
    demo_order.created_at + INTERVAL '5 minutes'
FROM orders demo_order
WHERE demo_order.shipping_address LIKE '[DSS-DEMO] %'
  AND NOT EXISTS (
      SELECT 1
      FROM payments payment
      WHERE payment.order_id = demo_order.id
  );

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
    status_order.id,
    CASE
        WHEN MOD(status_order.id, 2) = 0
            THEN 'MOMO'::payment_method_enum
        ELSE 'VNPAY'::payment_method_enum
    END,
    'DSS_DEMO_GATEWAY',
    status_order.total_amount,
    CASE
        WHEN status_order.status = 'CANCELLED'
            THEN 'FAILED'::payment_status
        WHEN status_order.status = 'PENDING'
            THEN 'PENDING'::payment_status
        ELSE 'SUCCESS'::payment_status
    END,
    'DSS-DEMO-STATUS-' || status_order.id,
    'VND',
    CASE
        WHEN status_order.status IN ('PROCESSING', 'SHIPPING')
            THEN status_order.created_at + INTERVAL '5 minutes'
        ELSE NULL
    END,
    status_order.created_at + INTERVAL '5 minutes'
FROM orders status_order
WHERE status_order.shipping_address LIKE '[DSS-DEMO-%] %'
  AND NOT EXISTS (
      SELECT 1
      FROM payments payment
      WHERE payment.order_id = status_order.id
  );

INSERT INTO order_tracking (
    order_id,
    event,
    note,
    updated_by,
    created_at
)
SELECT
    demo_order.id,
    'DELIVERED'::order_tracking_event,
    'Delivered order generated by DSS demo seed.',
    MIN(item.seller_id),
    demo_order.created_at + INTERVAL '8 hours'
FROM orders demo_order
JOIN order_items item ON item.order_id = demo_order.id
WHERE demo_order.shipping_address LIKE '[DSS-DEMO] %'
  AND NOT EXISTS (
      SELECT 1
      FROM order_tracking tracking
      WHERE tracking.order_id = demo_order.id
        AND tracking.event = 'DELIVERED'
  )
GROUP BY demo_order.id, demo_order.created_at;

INSERT INTO order_tracking (
    order_id,
    event,
    note,
    updated_by,
    created_at
)
SELECT
    status_order.id,
    CASE
        WHEN status_order.status = 'CANCELLED'
            THEN 'CANCELLED_BY_USER'::order_tracking_event
        WHEN status_order.status = 'PROCESSING'
            THEN 'CONFIRMED'::order_tracking_event
        WHEN status_order.status = 'SHIPPING'
            THEN 'SHIPPED'::order_tracking_event
        ELSE 'CREATED'::order_tracking_event
    END,
    'Order outcome generated by platform performance demo seed.',
    item.seller_id,
    status_order.created_at + INTERVAL '2 hours'
FROM orders status_order
JOIN order_items item ON item.order_id = status_order.id
WHERE status_order.shipping_address LIKE '[DSS-DEMO-%] %'
  AND NOT EXISTS (
      SELECT 1
      FROM order_tracking tracking
      WHERE tracking.order_id = status_order.id
  );

COMMIT;

-- Quick verification result set. Aggregates are separated to avoid multiplying
-- sales rows when a product has multiple price-history records.
WITH seeded_products AS (
    SELECT seeded_product.id
    FROM products seeded_product
    JOIN users seeded_seller ON seeded_seller.id = seeded_product.seller_id
    JOIN roles seeded_role ON seeded_role.id = seeded_seller.role_id
    WHERE seeded_product.deleted_at IS NULL
      AND seeded_product.status = 'ACTIVE'
      AND seeded_seller.status = 'ACTIVE'
      AND seeded_role.name = 'SELLER'
    ORDER BY seeded_product.id
    LIMIT 20
), sales_summary AS (
    SELECT item.product_id,
           COUNT(DISTINCT completed_order.created_at::DATE) AS sale_days,
           SUM(item.quantity) AS total_quantity
    FROM order_items item
    JOIN orders completed_order ON completed_order.id = item.order_id
    WHERE completed_order.status = 'DELIVERED'
      AND completed_order.created_at >= CURRENT_DATE - 120
      AND completed_order.created_at < CURRENT_DATE + 1
      AND item.product_id IN (SELECT id FROM seeded_products)
    GROUP BY item.product_id
), history_summary AS (
    SELECT history.product_id, COUNT(*) AS price_changes
    FROM price_history history
    WHERE history.changed_at >= CURRENT_DATE - 120
      AND history.product_id IN (SELECT id FROM seeded_products)
    GROUP BY history.product_id
)
SELECT
    product.id AS product_id,
    product.name AS product_name,
    seller.email AS seller_email,
    product.price AS current_price,
    product.cost_price,
    sales.sale_days,
    sales.total_quantity,
    COALESCE(history.price_changes, 0) AS price_changes
FROM seeded_products seeded
JOIN products product ON product.id = seeded.id
JOIN users seller ON seller.id = product.seller_id
JOIN sales_summary sales ON sales.product_id = product.id
LEFT JOIN history_summary history ON history.product_id = product.id
ORDER BY product.id;
