-- Ensure every product has an inventory row (for seller stock adjustments)
INSERT INTO inventory (product_id, available_quantity, reserved_quantity, updated_at)
SELECT p.id, 0, 0, CURRENT_TIMESTAMP
FROM products p
WHERE p.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM inventory i WHERE i.product_id = p.id);
