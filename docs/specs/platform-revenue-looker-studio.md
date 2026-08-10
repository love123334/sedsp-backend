# Platform Revenue Reporting for Looker Studio

## Purpose

Flyway migration `V37__create_platform_reporting_views.sql` creates a dedicated
`reporting` schema. The views expose read-only, ASCII-named, tabular datasets for
the Looker Studio PostgreSQL connector.

The reporting layer intentionally separates datasets by grain. Do not join all
views into one wide dataset: joining daily platform totals to seller or product
rows would duplicate platform revenue.

## Available views

| View | One row represents | Recommended use |
| --- | --- | --- |
| `reporting.platform_daily_revenue` | One calendar day | Scorecards and revenue time series |
| `reporting.platform_order_status_daily` | One day and one order status | Order status distribution |
| `reporting.platform_payment_method_daily` | One day and one payment method | Payment activity and settlement distribution |
| `reporting.platform_seller_daily` | One day and one seller with delivered sales | Seller ranking |
| `reporting.platform_product_daily` | One day and one product with delivered sales | Product ranking |
| `reporting.platform_category_daily` | One day and one current product category | Category ranking |
| `reporting.platform_activity_daily` | One calendar day | New seller, customer, product and category trends |
| `reporting.platform_current_summary` | Current platform snapshot | Current account/product/category scorecards |
| `reporting.platform_customer_purchase_daily` | One day and one customer with delivered orders | Exact distinct customer metrics (V38) |

Daily platform, order-status, payment-method and activity views include zero
rows for calendar dates without activity. Ranking views only contain rows with
delivered sales.

Flyway `V38__create_customer_purchase_reporting_view.sql` adds the customer-grain
view used for accurate distinct-customer counts across arbitrary date ranges.

## Metric definitions

- `gross_merchandise_value`: sum of `orders.subtotal_amount` for orders whose
  current status is `DELIVERED`, grouped by `orders.created_at` date.
- `delivered_order_value`: sum of `orders.total_amount` for the same orders.
- `successful_payment_amount`: successful payment volume grouped by `paid_at`.
- `average_order_value`: delivered order value divided by delivered order count.
- `active_seller_count`: distinct sellers in delivered order items for the day.
- `active_customer_count`: distinct customers with delivered orders for the day.
- `successful_amount_by_created_date`: successful amount among payments created
  on the day.
- `settled_successful_amount`: successful amount that was settled on the day.
- `daily_market_share_percentage`: entity GMV divided by total GMV of its day.

The schema has no commission, platform fee or refund amount. These fields are
gross transaction metrics, not platform net revenue or profit.

The schema also has no reliable `delivered_at`. Delivered-order metrics therefore
represent orders created on the report date whose current status is `DELIVERED`.
The category view uses the Product's current category because OrderItem does not
store a category snapshot.

## Run the migration

Start PostgreSQL, then start the Spring Boot application or run the normal
Flyway-enabled application startup. Verify migration state:

```sql
SELECT version, description, success
FROM flyway_schema_history
WHERE version = '37';
```

List reporting views:

```sql
SELECT table_schema, table_name
FROM information_schema.views
WHERE table_schema = 'reporting'
ORDER BY table_name;
```

Smoke-test the datasets:

```sql
SELECT *
FROM reporting.platform_daily_revenue
ORDER BY report_date DESC
LIMIT 30;
```

```sql
SELECT *
FROM reporting.platform_current_summary;
```

## Create a read-only Looker Studio user

Create the credential outside Flyway so that its password can be managed per
environment. Never use the application or PostgreSQL administrator account.

```sql
CREATE USER looker_isp_reader_railway WITH PASSWORD '<strong-environment-password>';

GRANT CONNECT ON DATABASE sedsp TO looker_isp_reader_railway;
GRANT USAGE ON SCHEMA reporting TO looker_isp_reader_railway;
GRANT SELECT ON ALL TABLES IN SCHEMA reporting TO looker_isp_reader_railway;

ALTER DEFAULT PRIVILEGES IN SCHEMA reporting
GRANT SELECT ON TABLES TO looker_isp_reader_railway;
```

Railway production applies the same grants via Flyway `V53__looker_isp_reader_railway.sql`
(uses `current_database()` so it works when the DB is named `railway` or `sedsp`).

For local/dev you may still use `looker_reader` with a separate password:

```sql
CREATE USER looker_reader WITH PASSWORD '<strong-environment-password>';
```

Do not grant access to business tables unless another reporting requirement
explicitly needs it.

## Connect Looker Studio

Create a PostgreSQL data source and configure the public database host, port,
database, `looker_reader` credentials and SSL. The local Docker hostname and
`localhost` are not reachable from Looker Studio.

The PostgreSQL connector cannot select a table outside `public` through the
table picker. Select `CUSTOM QUERY` and create one reusable data source per view.

```sql
SELECT * FROM reporting.platform_daily_revenue
```

```sql
SELECT * FROM reporting.platform_order_status_daily
```

```sql
SELECT * FROM reporting.platform_payment_method_daily
```

```sql
SELECT * FROM reporting.platform_seller_daily
```

```sql
SELECT * FROM reporting.platform_product_daily
```

```sql
SELECT * FROM reporting.platform_category_daily
```

```sql
SELECT * FROM reporting.platform_activity_daily
```

```sql
SELECT * FROM reporting.platform_current_summary
```

## Looker Studio field configuration

- Set `report_date` and `snapshot_date` to Date.
- Set additive money fields to Currency (VND) with aggregation `SUM`.
- Set count and quantity fields to Number with aggregation `SUM`.
- Set IDs, names, statuses and payment methods to Dimensions.
- Set `daily_market_share_percentage` to Percent only after confirming whether
  the data source expects `14.50` or `0.145`; the view returns percentage points
  such as `14.50`.
- Use `report_date` as the Date Range Dimension for every daily data source.
- Sort ranking charts by `SUM(gross_merchandise_value)` descending and apply the
  desired row limit in the chart.

Do not sum `average_order_value` across dates. For a selected date range, create
the weighted calculated field:

```text
SUM(delivered_order_value) / SUM(delivered_orders)
```

Use Looker Studio's comparison date range on the GMV scorecard for period growth.
For monthly charts, keep `report_date` as the source dimension and change the
chart date granularity to Year Month; a separate monthly database view is not
required.

For an arbitrary multi-day date range, calculate market share in Looker Studio
from aggregated GMV instead of summing `daily_market_share_percentage`:

```text
SUM(entity gross_merchandise_value) / SUM(platform gross merchandise value)
```

Likewise, do not sum daily distinct `active_seller_count` or
`active_customer_count` and interpret the result as a distinct count for the
whole period. Use these fields for daily time series. A true distinct count for
an arbitrary period needs the seller/customer-grain dataset or a period-specific
query.

Fields from `platform_current_summary` are a one-row snapshot. Use `MAX` as their
default aggregation when adding them to scorecards.

## Recommended report mapping

- Revenue overview and time series: `platform_daily_revenue`.
- Order status donut/table: `platform_order_status_daily`.
- Payment method chart/table: `platform_payment_method_daily`.
- Top seller/product/category tables: their corresponding ranking views.
- Registration/product activity trend: `platform_activity_daily`.
- Current platform snapshot cards: `platform_current_summary`.

Use a report-level date range control. Looker Studio will apply the selected date
range to every daily source whose Date Range Dimension is `report_date`.
