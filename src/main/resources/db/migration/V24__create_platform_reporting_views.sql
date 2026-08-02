CREATE SCHEMA IF NOT EXISTS reporting;

CREATE VIEW reporting.platform_daily_revenue AS
WITH date_bounds AS (
    SELECT COALESCE(
               LEAST(
                   (SELECT MIN(CAST(created_at AS DATE)) FROM orders),
                   (SELECT MIN(CAST(paid_at AS DATE))
                    FROM payments
                    WHERE paid_at IS NOT NULL)
               ),
               (SELECT MIN(CAST(created_at AS DATE)) FROM orders),
               (SELECT MIN(CAST(paid_at AS DATE))
                FROM payments
                WHERE paid_at IS NOT NULL),
               CURRENT_DATE
           ) AS start_date
),
calendar AS (
    SELECT CAST(day_value AS DATE) AS report_date
    FROM date_bounds,
         GENERATE_SERIES(
             start_date,
             CURRENT_DATE,
             INTERVAL '1 day'
         ) AS day_value
),
order_metrics AS (
    SELECT CAST(o.created_at AS DATE) AS report_date,
           COUNT(*) AS total_orders,
           COUNT(*) FILTER (WHERE o.status = 'PENDING') AS pending_orders,
           COUNT(*) FILTER (WHERE o.status = 'PAID') AS paid_orders,
           COUNT(*) FILTER (WHERE o.status = 'PROCESSING') AS processing_orders,
           COUNT(*) FILTER (WHERE o.status = 'SHIPPING') AS shipping_orders,
           COUNT(*) FILTER (WHERE o.status = 'DELIVERED') AS delivered_orders,
           COUNT(*) FILTER (WHERE o.status = 'CANCELLED') AS cancelled_orders,
           COUNT(*) FILTER (WHERE o.status = 'REFUNDED') AS refunded_orders,
           COALESCE(
               SUM(o.subtotal_amount) FILTER (WHERE o.status = 'DELIVERED'),
               0
           ) AS gross_merchandise_value,
           COALESCE(
               SUM(o.total_amount) FILTER (WHERE o.status = 'DELIVERED'),
               0
           ) AS delivered_order_value,
           COALESCE(
               SUM(o.discount_amount) FILTER (WHERE o.status = 'DELIVERED'),
               0
           ) AS total_discount_amount,
           COALESCE(
               SUM(o.shipping_fee) FILTER (WHERE o.status = 'DELIVERED'),
               0
           ) AS total_shipping_fee,
           COUNT(DISTINCT o.user_id)
               FILTER (WHERE o.status = 'DELIVERED') AS active_customer_count
    FROM orders o
    GROUP BY CAST(o.created_at AS DATE)
),
item_metrics AS (
    SELECT CAST(o.created_at AS DATE) AS report_date,
           COALESCE(SUM(oi.quantity), 0) AS units_sold,
           COUNT(DISTINCT oi.seller_id) AS active_seller_count
    FROM orders o
    JOIN order_items oi ON oi.order_id = o.id
    WHERE o.status = 'DELIVERED'
    GROUP BY CAST(o.created_at AS DATE)
),
payment_metrics AS (
    SELECT CAST(p.paid_at AS DATE) AS report_date,
           COALESCE(SUM(p.amount), 0) AS successful_payment_amount
    FROM payments p
    WHERE p.status = 'SUCCESS'
      AND p.paid_at IS NOT NULL
    GROUP BY CAST(p.paid_at AS DATE)
)
SELECT c.report_date,
       COALESCE(om.gross_merchandise_value, 0)::NUMERIC(18, 2)
           AS gross_merchandise_value,
       COALESCE(om.delivered_order_value, 0)::NUMERIC(18, 2)
           AS delivered_order_value,
       COALESCE(pm.successful_payment_amount, 0)::NUMERIC(18, 2)
           AS successful_payment_amount,
       COALESCE(om.total_discount_amount, 0)::NUMERIC(18, 2)
           AS total_discount_amount,
       COALESCE(om.total_shipping_fee, 0)::NUMERIC(18, 2)
           AS total_shipping_fee,
       COALESCE(om.total_orders, 0)::BIGINT AS total_orders,
       COALESCE(om.pending_orders, 0)::BIGINT AS pending_orders,
       COALESCE(om.paid_orders, 0)::BIGINT AS paid_orders,
       COALESCE(om.processing_orders, 0)::BIGINT AS processing_orders,
       COALESCE(om.shipping_orders, 0)::BIGINT AS shipping_orders,
       COALESCE(om.delivered_orders, 0)::BIGINT AS delivered_orders,
       COALESCE(om.cancelled_orders, 0)::BIGINT AS cancelled_orders,
       COALESCE(om.refunded_orders, 0)::BIGINT AS refunded_orders,
       COALESCE(
           ROUND(
               om.delivered_order_value
                   / NULLIF(om.delivered_orders, 0),
               2
           ),
           0
       )::NUMERIC(18, 2) AS average_order_value,
       COALESCE(im.units_sold, 0)::BIGINT AS units_sold,
       COALESCE(im.active_seller_count, 0)::BIGINT AS active_seller_count,
       COALESCE(om.active_customer_count, 0)::BIGINT AS active_customer_count
FROM calendar c
LEFT JOIN order_metrics om ON om.report_date = c.report_date
LEFT JOIN item_metrics im ON im.report_date = c.report_date
LEFT JOIN payment_metrics pm ON pm.report_date = c.report_date;

CREATE VIEW reporting.platform_order_status_daily AS
WITH date_bounds AS (
    SELECT COALESCE(
               MIN(CAST(created_at AS DATE)),
               CURRENT_DATE
           ) AS start_date
    FROM orders
),
calendar AS (
    SELECT CAST(day_value AS DATE) AS report_date
    FROM date_bounds,
         GENERATE_SERIES(
             start_date,
             CURRENT_DATE,
             INTERVAL '1 day'
         ) AS day_value
),
statuses AS (
    SELECT UNNEST(ENUM_RANGE(NULL::order_status)) AS order_status
),
status_metrics AS (
    SELECT CAST(o.created_at AS DATE) AS report_date,
           o.status AS order_status,
           COUNT(*) AS order_count,
           COALESCE(SUM(o.total_amount), 0) AS total_order_value
    FROM orders o
    GROUP BY CAST(o.created_at AS DATE), o.status
)
SELECT c.report_date,
       CAST(s.order_status AS TEXT) AS order_status,
       COALESCE(sm.order_count, 0)::BIGINT AS order_count,
       COALESCE(sm.total_order_value, 0)::NUMERIC(18, 2)
           AS total_order_value
FROM calendar c
CROSS JOIN statuses s
LEFT JOIN status_metrics sm
       ON sm.report_date = c.report_date
      AND sm.order_status = s.order_status;

CREATE VIEW reporting.platform_payment_method_daily AS
WITH date_bounds AS (
    SELECT COALESCE(
               LEAST(
                   MIN(CAST(created_at AS DATE)),
                   MIN(CAST(paid_at AS DATE))
               ),
               MIN(CAST(created_at AS DATE)),
               MIN(CAST(paid_at AS DATE)),
               CURRENT_DATE
           ) AS start_date
    FROM payments
),
calendar AS (
    SELECT CAST(day_value AS DATE) AS report_date
    FROM date_bounds,
         GENERATE_SERIES(
             start_date,
             CURRENT_DATE,
             INTERVAL '1 day'
         ) AS day_value
),
methods AS (
    SELECT UNNEST(ENUM_RANGE(NULL::payment_method_enum)) AS payment_method
),
created_metrics AS (
    SELECT CAST(p.created_at AS DATE) AS report_date,
           p.payment_method,
           COUNT(*) AS total_payment_count,
           COUNT(*) FILTER (WHERE p.status = 'SUCCESS')
               AS successful_payment_count,
           COUNT(*) FILTER (WHERE p.status = 'PENDING')
               AS pending_payment_count,
           COUNT(*) FILTER (WHERE p.status = 'FAILED')
               AS failed_payment_count,
           COALESCE(
               SUM(p.amount) FILTER (WHERE p.status = 'SUCCESS'),
               0
           ) AS successful_amount_by_created_date
    FROM payments p
    GROUP BY CAST(p.created_at AS DATE), p.payment_method
),
settled_metrics AS (
    SELECT CAST(p.paid_at AS DATE) AS report_date,
           p.payment_method,
           COUNT(*) AS settled_payment_count,
           COALESCE(SUM(p.amount), 0) AS settled_successful_amount
    FROM payments p
    WHERE p.status = 'SUCCESS'
      AND p.paid_at IS NOT NULL
    GROUP BY CAST(p.paid_at AS DATE), p.payment_method
)
SELECT c.report_date,
       CAST(m.payment_method AS TEXT) AS payment_method,
       COALESCE(cm.total_payment_count, 0)::BIGINT AS total_payment_count,
       COALESCE(cm.successful_payment_count, 0)::BIGINT
           AS successful_payment_count,
       COALESCE(cm.pending_payment_count, 0)::BIGINT
           AS pending_payment_count,
       COALESCE(cm.failed_payment_count, 0)::BIGINT
           AS failed_payment_count,
       COALESCE(cm.successful_amount_by_created_date, 0)::NUMERIC(18, 2)
           AS successful_amount_by_created_date,
       COALESCE(sm.settled_payment_count, 0)::BIGINT
           AS settled_payment_count,
       COALESCE(sm.settled_successful_amount, 0)::NUMERIC(18, 2)
           AS settled_successful_amount
FROM calendar c
CROSS JOIN methods m
LEFT JOIN created_metrics cm
       ON cm.report_date = c.report_date
      AND cm.payment_method = m.payment_method
LEFT JOIN settled_metrics sm
       ON sm.report_date = c.report_date
      AND sm.payment_method = m.payment_method;

CREATE VIEW reporting.platform_seller_daily AS
WITH seller_metrics AS (
    SELECT CAST(o.created_at AS DATE) AS report_date,
           oi.seller_id,
           COALESCE(
               NULLIF(u.store_name, ''),
               NULLIF(u.full_name, ''),
               u.username
           ) AS seller_name,
           COALESCE(SUM(oi.subtotal), 0) AS gross_merchandise_value,
           COUNT(DISTINCT oi.order_id) AS delivered_orders,
           COALESCE(SUM(oi.quantity), 0) AS units_sold
    FROM orders o
    JOIN order_items oi ON oi.order_id = o.id
    JOIN users u ON u.id = oi.seller_id
    WHERE o.status = 'DELIVERED'
    GROUP BY CAST(o.created_at AS DATE),
             oi.seller_id,
             u.store_name,
             u.full_name,
             u.username
)
SELECT sm.report_date,
       sm.seller_id,
       sm.seller_name,
       sm.gross_merchandise_value::NUMERIC(18, 2)
           AS gross_merchandise_value,
       sm.delivered_orders::BIGINT AS delivered_orders,
       sm.units_sold::BIGINT AS units_sold,
       COALESCE(
           ROUND(
               sm.gross_merchandise_value * 100.0
                   / NULLIF(
                       SUM(sm.gross_merchandise_value)
                           OVER (PARTITION BY sm.report_date),
                       0
                   ),
               2
           ),
           0
       )::NUMERIC(7, 2) AS daily_market_share_percentage
FROM seller_metrics sm;

CREATE VIEW reporting.platform_product_daily AS
WITH product_metrics AS (
    SELECT CAST(o.created_at AS DATE) AS report_date,
           oi.product_id,
           p.name AS product_name,
           oi.seller_id,
           COALESCE(
               NULLIF(u.store_name, ''),
               NULLIF(u.full_name, ''),
               u.username
           ) AS seller_name,
           COALESCE(SUM(oi.subtotal), 0) AS gross_merchandise_value,
           COUNT(DISTINCT oi.order_id) AS delivered_orders,
           COALESCE(SUM(oi.quantity), 0) AS units_sold
    FROM orders o
    JOIN order_items oi ON oi.order_id = o.id
    JOIN products p ON p.id = oi.product_id
    JOIN users u ON u.id = oi.seller_id
    WHERE o.status = 'DELIVERED'
    GROUP BY CAST(o.created_at AS DATE),
             oi.product_id,
             p.name,
             oi.seller_id,
             u.store_name,
             u.full_name,
             u.username
)
SELECT pm.report_date,
       pm.product_id,
       pm.product_name,
       pm.seller_id,
       pm.seller_name,
       pm.gross_merchandise_value::NUMERIC(18, 2)
           AS gross_merchandise_value,
       pm.delivered_orders::BIGINT AS delivered_orders,
       pm.units_sold::BIGINT AS units_sold,
       COALESCE(
           ROUND(
               pm.gross_merchandise_value * 100.0
                   / NULLIF(
                       SUM(pm.gross_merchandise_value)
                           OVER (PARTITION BY pm.report_date),
                       0
                   ),
               2
           ),
           0
       )::NUMERIC(7, 2) AS daily_market_share_percentage
FROM product_metrics pm;

CREATE VIEW reporting.platform_category_daily AS
WITH category_metrics AS (
    SELECT CAST(o.created_at AS DATE) AS report_date,
           c.id AS category_id,
           COALESCE(CAST(c.id AS TEXT), 'UNCATEGORIZED') AS category_key,
           COALESCE(c.name, 'Uncategorized') AS category_name,
           COALESCE(SUM(oi.subtotal), 0) AS gross_merchandise_value,
           COUNT(DISTINCT oi.order_id) AS delivered_orders,
           COALESCE(SUM(oi.quantity), 0) AS units_sold
    FROM orders o
    JOIN order_items oi ON oi.order_id = o.id
    JOIN products p ON p.id = oi.product_id
    LEFT JOIN categories c ON c.id = p.category_id
    WHERE o.status = 'DELIVERED'
    GROUP BY CAST(o.created_at AS DATE), c.id, c.name
)
SELECT cm.report_date,
       cm.category_id,
       cm.category_key,
       cm.category_name,
       cm.gross_merchandise_value::NUMERIC(18, 2)
           AS gross_merchandise_value,
       cm.delivered_orders::BIGINT AS delivered_orders,
       cm.units_sold::BIGINT AS units_sold,
       COALESCE(
           ROUND(
               cm.gross_merchandise_value * 100.0
                   / NULLIF(
                       SUM(cm.gross_merchandise_value)
                           OVER (PARTITION BY cm.report_date),
                       0
                   ),
               2
           ),
           0
       )::NUMERIC(7, 2) AS daily_market_share_percentage
FROM category_metrics cm;

CREATE VIEW reporting.platform_activity_daily AS
WITH date_bounds AS (
    SELECT COALESCE(
               LEAST(
                   (SELECT MIN(CAST(created_at AS DATE)) FROM users),
                   (SELECT MIN(CAST(created_at AS DATE)) FROM products),
                   (SELECT MIN(CAST(created_at AS DATE)) FROM categories)
               ),
               (SELECT MIN(CAST(created_at AS DATE)) FROM users),
               (SELECT MIN(CAST(created_at AS DATE)) FROM products),
               (SELECT MIN(CAST(created_at AS DATE)) FROM categories),
               CURRENT_DATE
           ) AS start_date
),
calendar AS (
    SELECT CAST(day_value AS DATE) AS report_date
    FROM date_bounds,
         GENERATE_SERIES(
             start_date,
             CURRENT_DATE,
             INTERVAL '1 day'
         ) AS day_value
),
user_activity AS (
    SELECT CAST(u.created_at AS DATE) AS report_date,
           COUNT(*) FILTER (WHERE r.name = 'SELLER') AS new_sellers,
           COUNT(*) FILTER (WHERE r.name = 'CUSTOMER') AS new_customers
    FROM users u
    JOIN roles r ON r.id = u.role_id
    GROUP BY CAST(u.created_at AS DATE)
),
product_activity AS (
    SELECT CAST(p.created_at AS DATE) AS report_date,
           COUNT(*) AS new_products
    FROM products p
    GROUP BY CAST(p.created_at AS DATE)
),
category_activity AS (
    SELECT CAST(c.created_at AS DATE) AS report_date,
           COUNT(*) AS new_categories
    FROM categories c
    GROUP BY CAST(c.created_at AS DATE)
)
SELECT c.report_date,
       COALESCE(ua.new_sellers, 0)::BIGINT AS new_sellers,
       COALESCE(ua.new_customers, 0)::BIGINT AS new_customers,
       COALESCE(pa.new_products, 0)::BIGINT AS new_products,
       COALESCE(ca.new_categories, 0)::BIGINT AS new_categories
FROM calendar c
LEFT JOIN user_activity ua ON ua.report_date = c.report_date
LEFT JOIN product_activity pa ON pa.report_date = c.report_date
LEFT JOIN category_activity ca ON ca.report_date = c.report_date;

CREATE VIEW reporting.platform_current_summary AS
WITH user_summary AS (
    SELECT COUNT(*) AS total_users,
           COUNT(*) FILTER (WHERE r.name = 'SELLER') AS total_sellers,
           COUNT(*) FILTER (
               WHERE r.name = 'SELLER' AND u.status = 'ACTIVE'
           ) AS active_seller_accounts,
           COUNT(*) FILTER (
               WHERE r.name = 'SELLER' AND u.status = 'INACTIVE'
           ) AS inactive_seller_accounts,
           COUNT(*) FILTER (
               WHERE r.name = 'SELLER' AND u.status = 'PENDING'
           ) AS pending_seller_accounts,
           COUNT(*) FILTER (
               WHERE r.name = 'SELLER' AND u.status = 'BLOCKED'
           ) AS blocked_seller_accounts,
           COUNT(*) FILTER (WHERE r.name = 'CUSTOMER') AS total_customers,
           COUNT(*) FILTER (
               WHERE r.name = 'CUSTOMER' AND u.status = 'ACTIVE'
           ) AS active_customer_accounts,
           COUNT(*) FILTER (
               WHERE r.name = 'CUSTOMER' AND u.status = 'INACTIVE'
           ) AS inactive_customer_accounts,
           COUNT(*) FILTER (
               WHERE r.name = 'CUSTOMER' AND u.status = 'PENDING'
           ) AS pending_customer_accounts,
           COUNT(*) FILTER (
               WHERE r.name = 'CUSTOMER' AND u.status = 'BLOCKED'
           ) AS blocked_customer_accounts
    FROM users u
    JOIN roles r ON r.id = u.role_id
    WHERE u.deleted_at IS NULL
      AND r.deleted_at IS NULL
),
product_summary AS (
    SELECT COUNT(*) AS total_products,
           COUNT(*) FILTER (WHERE p.status = 'ACTIVE') AS active_products,
           COUNT(*) FILTER (WHERE p.status = 'INACTIVE') AS inactive_products,
           COUNT(*) FILTER (WHERE p.status = 'OUT_OF_STOCK')
               AS out_of_stock_products,
           COUNT(*) FILTER (WHERE p.category_id IS NULL)
               AS uncategorized_products,
           COUNT(DISTINCT p.seller_id) AS sellers_with_products
    FROM products p
    WHERE p.deleted_at IS NULL
),
category_summary AS (
    SELECT COUNT(*) AS total_categories,
           COUNT(*) FILTER (WHERE c.parent_id IS NULL) AS root_categories
    FROM categories c
    WHERE c.deleted_at IS NULL
)
SELECT CURRENT_DATE AS snapshot_date,
       CURRENT_TIMESTAMP AS generated_at,
       us.total_users::BIGINT,
       us.total_sellers::BIGINT,
       us.active_seller_accounts::BIGINT,
       us.inactive_seller_accounts::BIGINT,
       us.pending_seller_accounts::BIGINT,
       us.blocked_seller_accounts::BIGINT,
       us.total_customers::BIGINT,
       us.active_customer_accounts::BIGINT,
       us.inactive_customer_accounts::BIGINT,
       us.pending_customer_accounts::BIGINT,
       us.blocked_customer_accounts::BIGINT,
       ps.total_products::BIGINT,
       ps.active_products::BIGINT,
       ps.inactive_products::BIGINT,
       ps.out_of_stock_products::BIGINT,
       ps.uncategorized_products::BIGINT,
       ps.sellers_with_products::BIGINT,
       cs.total_categories::BIGINT,
       cs.root_categories::BIGINT
FROM user_summary us
CROSS JOIN product_summary ps
CROSS JOIN category_summary cs;

COMMENT ON SCHEMA reporting IS
    'Read-only analytical views for platform reporting tools.';

COMMENT ON VIEW reporting.platform_daily_revenue IS
    'Daily platform GMV, delivered order value, payment and order KPIs.';

COMMENT ON VIEW reporting.platform_order_status_daily IS
    'Daily order counts and values for every order status.';

COMMENT ON VIEW reporting.platform_payment_method_daily IS
    'Daily payment attempts and settled amounts by payment method.';

COMMENT ON VIEW reporting.platform_seller_daily IS
    'Daily delivered merchandise performance by seller.';

COMMENT ON VIEW reporting.platform_product_daily IS
    'Daily delivered merchandise performance by product.';

COMMENT ON VIEW reporting.platform_category_daily IS
    'Daily delivered merchandise performance by current product category.';

COMMENT ON VIEW reporting.platform_activity_daily IS
    'Daily seller, customer, product and category creation activity.';

COMMENT ON VIEW reporting.platform_current_summary IS
    'Current non-deleted account, product and category snapshot.';
