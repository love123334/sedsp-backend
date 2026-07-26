-- Soft-delete tương thích: chỉ 1 dòng active / (cart, product)
ALTER TABLE cart_items DROP CONSTRAINT IF EXISTS uq_cart_product;

CREATE UNIQUE INDEX IF NOT EXISTS uq_cart_product_active
    ON cart_items (cart_id, product_id)
    WHERE deleted_at IS NULL;
