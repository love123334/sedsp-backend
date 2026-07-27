-- V31: localize category display names to Vietnamese (keep slugs)

-- Hide empty / duplicate English roots (children keep working via parent_id)
UPDATE categories
SET deleted_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE deleted_at IS NULL
  AND slug IN (
    'electronics',
    'fashion',
    'beauty',
    'home-living',
    'sports',
    'thoi-trang',
    'sach',
    'phu-kien'
  );

-- Translate remaining English category names
UPDATE categories SET name = 'Điện thoại', updated_at = CURRENT_TIMESTAMP
WHERE slug = 'phones' AND deleted_at IS NULL AND name IS DISTINCT FROM 'Điện thoại';

UPDATE categories SET name = 'Laptop', updated_at = CURRENT_TIMESTAMP
WHERE slug = 'laptops' AND deleted_at IS NULL AND name IS DISTINCT FROM 'Laptop';

UPDATE categories SET name = 'Máy tính bảng', updated_at = CURRENT_TIMESTAMP
WHERE slug = 'tablets' AND deleted_at IS NULL AND name IS DISTINCT FROM 'Máy tính bảng';

UPDATE categories SET name = 'Phụ kiện', updated_at = CURRENT_TIMESTAMP
WHERE slug = 'electronics-accessories' AND deleted_at IS NULL AND name IS DISTINCT FROM 'Phụ kiện';

UPDATE categories SET name = 'Thời trang nam', updated_at = CURRENT_TIMESTAMP
WHERE slug = 'men-clothing' AND deleted_at IS NULL AND name IS DISTINCT FROM 'Thời trang nam';

UPDATE categories SET name = 'Thời trang nữ', updated_at = CURRENT_TIMESTAMP
WHERE slug = 'women-clothing' AND deleted_at IS NULL AND name IS DISTINCT FROM 'Thời trang nữ';

UPDATE categories SET name = 'Giày dép', updated_at = CURRENT_TIMESTAMP
WHERE slug = 'shoes' AND deleted_at IS NULL AND name IS DISTINCT FROM 'Giày dép';

UPDATE categories SET name = 'Chăm sóc da', updated_at = CURRENT_TIMESTAMP
WHERE slug = 'skincare' AND deleted_at IS NULL AND name IS DISTINCT FROM 'Chăm sóc da';

UPDATE categories SET name = 'Trang điểm', updated_at = CURRENT_TIMESTAMP
WHERE slug = 'makeup' AND deleted_at IS NULL AND name IS DISTINCT FROM 'Trang điểm';

UPDATE categories SET name = 'Nhà bếp', updated_at = CURRENT_TIMESTAMP
WHERE slug = 'kitchen' AND deleted_at IS NULL AND name IS DISTINCT FROM 'Nhà bếp';

UPDATE categories SET name = 'Nội thất', updated_at = CURRENT_TIMESTAMP
WHERE slug = 'furniture' AND deleted_at IS NULL AND name IS DISTINCT FROM 'Nội thất';

UPDATE categories SET name = 'Trang trí', updated_at = CURRENT_TIMESTAMP
WHERE slug = 'decor' AND deleted_at IS NULL AND name IS DISTINCT FROM 'Trang trí';

UPDATE categories SET name = 'Thiết bị thể hình', updated_at = CURRENT_TIMESTAMP
WHERE slug = 'fitness-equipment' AND deleted_at IS NULL AND name IS DISTINCT FROM 'Thiết bị thể hình';

UPDATE categories SET name = 'Đồ dã ngoại', updated_at = CURRENT_TIMESTAMP
WHERE slug = 'outdoor-gear' AND deleted_at IS NULL AND name IS DISTINCT FROM 'Đồ dã ngoại';
