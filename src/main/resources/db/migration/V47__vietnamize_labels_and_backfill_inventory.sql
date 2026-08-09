-- V47: Vietnamese labels + backfill inventory (fix false "hết hàng")
-- Categories / shop names → tiếng Việt. Mọi SP thiếu inventory → tồn mặc định 80.

-- ── Categories ─────────────────────────────────────────────
UPDATE categories SET name = 'Máy tính xách tay', updated_at = CURRENT_TIMESTAMP
WHERE slug = 'laptops' AND deleted_at IS NULL AND name IS DISTINCT FROM 'Máy tính xách tay';

UPDATE categories SET name = 'Điện tử', updated_at = CURRENT_TIMESTAMP
WHERE slug IN ('electronics', 'dien-tu') AND deleted_at IS NULL AND name IS DISTINCT FROM 'Điện tử';

UPDATE categories SET name = 'Điện tử DSS', updated_at = CURRENT_TIMESTAMP
WHERE slug = 'dss-demo-electronics' AND deleted_at IS NULL AND name IS DISTINCT FROM 'Điện tử DSS';

UPDATE categories SET name = 'Thời trang', updated_at = CURRENT_TIMESTAMP
WHERE slug IN ('fashion', 'thoi-trang') AND deleted_at IS NULL AND name IS DISTINCT FROM 'Thời trang';

UPDATE categories SET name = 'Nhà bếp', updated_at = CURRENT_TIMESTAMP
WHERE slug = 'kitchen' AND deleted_at IS NULL AND name IS DISTINCT FROM 'Nhà bếp';

UPDATE categories SET name = 'Đồ dã ngoại', updated_at = CURRENT_TIMESTAMP
WHERE slug = 'outdoor-gear' AND deleted_at IS NULL AND name IS DISTINCT FROM 'Đồ dã ngoại';

UPDATE categories SET name = 'Điện thoại', updated_at = CURRENT_TIMESTAMP
WHERE slug = 'phones' AND deleted_at IS NULL AND name IS DISTINCT FROM 'Điện thoại';

UPDATE categories SET name = 'Máy tính bảng', updated_at = CURRENT_TIMESTAMP
WHERE slug = 'tablets' AND deleted_at IS NULL AND name IS DISTINCT FROM 'Máy tính bảng';

UPDATE categories SET name = 'Phụ kiện', updated_at = CURRENT_TIMESTAMP
WHERE slug IN ('electronics-accessories', 'phu-kien', 'accessories') AND deleted_at IS NULL
  AND name IS DISTINCT FROM 'Phụ kiện';

UPDATE categories SET name = 'Giày dép', updated_at = CURRENT_TIMESTAMP
WHERE slug = 'shoes' AND deleted_at IS NULL AND name IS DISTINCT FROM 'Giày dép';

UPDATE categories SET name = 'Chăm sóc da', updated_at = CURRENT_TIMESTAMP
WHERE slug = 'skincare' AND deleted_at IS NULL AND name IS DISTINCT FROM 'Chăm sóc da';

UPDATE categories SET name = 'Trang điểm', updated_at = CURRENT_TIMESTAMP
WHERE slug = 'makeup' AND deleted_at IS NULL AND name IS DISTINCT FROM 'Trang điểm';

UPDATE categories SET name = 'Gia dụng', updated_at = CURRENT_TIMESTAMP
WHERE slug IN ('home-living', 'gia-dung', 'home') AND deleted_at IS NULL AND name IS DISTINCT FROM 'Gia dụng';

UPDATE categories SET name = 'Thể thao', updated_at = CURRENT_TIMESTAMP
WHERE slug IN ('sports', 'the-thao') AND deleted_at IS NULL AND name IS DISTINCT FROM 'Thể thao';

UPDATE categories SET name = 'Sách', updated_at = CURRENT_TIMESTAMP
WHERE slug IN ('books', 'sach') AND deleted_at IS NULL AND name IS DISTINCT FROM 'Sách';

-- Catch-all: remaining Latin-only category names → keep slug, prefix VI hint if still English-looking
UPDATE categories
SET name = CASE slug
    WHEN 'men-clothing' THEN 'Thời trang nam'
    WHEN 'women-clothing' THEN 'Thời trang nữ'
    WHEN 'furniture' THEN 'Nội thất'
    WHEN 'decor' THEN 'Trang trí'
    WHEN 'fitness-equipment' THEN 'Thiết bị thể hình'
    ELSE name
END,
updated_at = CURRENT_TIMESTAMP
WHERE deleted_at IS NULL
  AND slug IN (
    'men-clothing', 'women-clothing', 'furniture', 'decor', 'fitness-equipment'
  );

-- ── Seller store names (UI shop tag) ───────────────────────
UPDATE users SET store_name = 'NT Tech', full_name = 'Nguyễn Tech', updated_at = CURRENT_TIMESTAMP
WHERE email = 'seller01@secdsp.com';

UPDATE users SET store_name = 'Minh Điện tử', full_name = 'Minh Điện tử', updated_at = CURRENT_TIMESTAMP
WHERE email = 'seller02@secdsp.com';

UPDATE users SET store_name = 'Lan Thời trang', full_name = 'Lan Thời trang', updated_at = CURRENT_TIMESTAMP
WHERE email = 'seller03@secdsp.com';

UPDATE users SET store_name = 'Beauty Hub VN', full_name = 'Beauty Hub', updated_at = CURRENT_TIMESTAMP
WHERE email = 'seller04@secdsp.com';

UPDATE users SET store_name = 'Nhà đẹp HomeStyle', full_name = 'HomeStyle', updated_at = CURRENT_TIMESTAMP
WHERE email = 'seller05@secdsp.com';

UPDATE users SET store_name = 'Sport Max', full_name = 'Sport Max', updated_at = CURRENT_TIMESTAMP
WHERE email = 'seller06@secdsp.com';

UPDATE users SET store_name = 'Thế giới số', full_name = 'Thế giới số', updated_at = CURRENT_TIMESTAMP
WHERE email = 'seller07@secdsp.com';

UPDATE users SET store_name = 'Urban Wear', full_name = 'Urban Wear', updated_at = CURRENT_TIMESTAMP
WHERE email = 'seller08@secdsp.com';

UPDATE users SET store_name = 'Bếp Pro', full_name = 'Bếp Pro', updated_at = CURRENT_TIMESTAMP
WHERE email = 'seller09@secdsp.com';

UPDATE users SET store_name = 'Đời sống ngoài trời', full_name = 'Đời sống ngoài trời', updated_at = CURRENT_TIMESTAMP
WHERE email = 'seller10@secdsp.com';

UPDATE users SET store_name = 'Cửa hàng DSS', full_name = 'Người bán DSS', updated_at = CURRENT_TIMESTAMP
WHERE email = 'seller.dss.demo@example.com';

UPDATE users SET store_name = 'Thời trang DSS', full_name = 'Người bán thời trang DSS', updated_at = CURRENT_TIMESTAMP
WHERE email = 'seller.dss.demo.2@example.com';

-- ── Inventory backfill (root cause of fake out-of-stock) ───
INSERT INTO inventory (product_id, available_quantity, reserved_quantity, updated_at)
SELECT p.id, 80, 0, CURRENT_TIMESTAMP
FROM products p
WHERE p.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM inventory i WHERE i.product_id = p.id
  );

-- Products that already have inventory row but qty = 0 (never sold out intentionally for demo)
UPDATE inventory i
SET available_quantity = 80,
    updated_at = CURRENT_TIMESTAMP
WHERE available_quantity = 0
  AND reserved_quantity = 0
  AND EXISTS (
      SELECT 1 FROM products p
      WHERE p.id = i.product_id
        AND p.deleted_at IS NULL
        AND p.status = 'ACTIVE'
  );
