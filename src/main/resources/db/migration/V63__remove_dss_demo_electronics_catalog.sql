-- V63: Remove DSS "Điện tử DSS" fake catalog + related demo shops.
-- Keep real marketplace seeds (phones, fashion, home, …) unchanged.
-- Idempotent soft-delete — safe if already cleaned.

CREATE TEMP TABLE tmp_dss_electronics_products ON COMMIT DROP AS
SELECT DISTINCT p.id AS product_id
FROM products p
LEFT JOIN categories c ON c.id = p.category_id
WHERE p.deleted_at IS NULL
  AND (
      COALESCE(c.slug, '') = 'dss-demo-electronics'
      OR p.slug LIKE 'dss-demo-%'
      OR p.slug LIKE 'dss-forecast-%'
      OR EXISTS (
          SELECT 1
          FROM users s
          WHERE s.id = p.seller_id
            AND (
                s.email = 'seller.dss.demo@example.com'
                OR s.email LIKE 'seller.dss.demo.%@example.com'
            )
      )
  );

-- Cart lines pointing at removed SKUs
DELETE FROM cart_items
WHERE product_id IN (SELECT product_id FROM tmp_dss_electronics_products);

-- Soft-delete related product media / attributes / reviews
UPDATE product_images
SET deleted_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE deleted_at IS NULL
  AND product_id IN (SELECT product_id FROM tmp_dss_electronics_products);

UPDATE product_attributes
SET deleted_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE deleted_at IS NULL
  AND product_id IN (SELECT product_id FROM tmp_dss_electronics_products);

UPDATE product_reviews
SET deleted_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE deleted_at IS NULL
  AND product_id IN (SELECT product_id FROM tmp_dss_electronics_products);

-- Zero stock so DSS/forms don't show ghost inventory
UPDATE inventory
SET available_quantity = 0,
    reserved_quantity = 0,
    updated_at = CURRENT_TIMESTAMP
WHERE product_id IN (SELECT product_id FROM tmp_dss_electronics_products);

UPDATE products
SET status = 'INACTIVE'::product_status,
    deleted_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE deleted_at IS NULL
  AND id IN (SELECT product_id FROM tmp_dss_electronics_products);

UPDATE categories
SET deleted_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE deleted_at IS NULL
  AND slug = 'dss-demo-electronics';

-- Soft-delete DSS demo shops tied to that fake electronics catalog
UPDATE users
SET status = 'INACTIVE'::user_status,
    deleted_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE deleted_at IS NULL
  AND (
      email = 'seller.dss.demo@example.com'
      OR email LIKE 'seller.dss.demo.%@example.com'
  );
