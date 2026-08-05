CREATE SCHEMA IF NOT EXISTS reporting;

CREATE OR REPLACE VIEW reporting.platform_customer_purchase_daily AS
WITH order_item_metrics AS (
    SELECT oi.order_id,
           COALESCE(SUM(oi.quantity), 0) AS units_purchased
    FROM order_items oi
    GROUP BY oi.order_id
)
SELECT CAST(o.created_at AS DATE) AS report_date,
       o.user_id AS customer_id,
       COALESCE(
           NULLIF(u.full_name, ''),
           NULLIF(u.username, ''),
           'Customer ' || CAST(u.id AS TEXT)
       ) AS customer_name,
       COUNT(*)::BIGINT AS delivered_orders,
       COALESCE(SUM(oim.units_purchased), 0)::BIGINT
           AS units_purchased,
       COALESCE(SUM(o.subtotal_amount), 0)::NUMERIC(18, 2)
           AS gross_merchandise_value,
       COALESCE(SUM(o.total_amount), 0)::NUMERIC(18, 2)
           AS delivered_order_value,
       COALESCE(SUM(o.discount_amount), 0)::NUMERIC(18, 2)
           AS total_discount_amount,
       COALESCE(SUM(o.shipping_fee), 0)::NUMERIC(18, 2)
           AS total_shipping_fee
FROM orders o
JOIN users u ON u.id = o.user_id
JOIN order_item_metrics oim ON oim.order_id = o.id
WHERE o.status = 'DELIVERED'
GROUP BY CAST(o.created_at AS DATE),
         o.user_id,
         u.id,
         u.full_name,
         u.username;

COMMENT ON VIEW reporting.platform_customer_purchase_daily IS
    'Daily delivered-order metrics at customer grain for exact distinct customer counts across arbitrary date ranges.';
