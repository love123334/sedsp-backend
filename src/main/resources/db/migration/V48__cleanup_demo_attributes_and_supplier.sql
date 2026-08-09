-- V48: Cleanup demo data, Việt hóa thuộc tính SP, xóa URL nhà cung cấp sai

UPDATE product_attributes SET attribute_name = 'Thương hiệu', updated_at = CURRENT_TIMESTAMP
WHERE attribute_name = 'Brand' AND deleted_at IS NULL;

UPDATE product_attributes SET attribute_name = 'Xuất xứ', updated_at = CURRENT_TIMESTAMP
WHERE attribute_name = 'Origin' AND deleted_at IS NULL;

UPDATE product_attributes SET attribute_name = 'Bảo hành', updated_at = CURRENT_TIMESTAMP
WHERE attribute_name = 'Warranty' AND deleted_at IS NULL;

UPDATE product_attributes SET attribute_name = 'Tương thích', updated_at = CURRENT_TIMESTAMP
WHERE attribute_name = 'Compatibility' AND deleted_at IS NULL;

UPDATE product_attributes SET attribute_name = 'Chất liệu', updated_at = CURRENT_TIMESTAMP
WHERE attribute_name = 'Material' AND deleted_at IS NULL;

UPDATE product_attributes SET attribute_name = 'Kích cỡ', updated_at = CURRENT_TIMESTAMP
WHERE attribute_name = 'Size' AND deleted_at IS NULL;

UPDATE product_attributes SET attribute_name = 'Loại da', updated_at = CURRENT_TIMESTAMP
WHERE attribute_name = 'Skin Type' AND deleted_at IS NULL;

UPDATE product_attributes SET attribute_name = 'Công dụng', updated_at = CURRENT_TIMESTAMP
WHERE attribute_name = 'Usage' AND deleted_at IS NULL;

UPDATE product_attributes
SET deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
WHERE deleted_at IS NULL
  AND (
    attribute_value ILIKE '%vnshop%'
    OR attribute_value ILIKE '%example.com%'
    OR (attribute_value ILIKE 'http%' AND (
      attribute_name ILIKE '%supplier%'
      OR attribute_name ILIKE '%cung cấp%'
      OR attribute_name = 'Origin'
    ))
  );

UPDATE product_attributes pa
SET attribute_value = COALESCE(u.store_name, u.full_name, 'Cửa hàng SEDSP'),
    attribute_name = 'Nhà cung cấp',
    updated_at = CURRENT_TIMESTAMP
FROM products p
JOIN users u ON u.id = p.seller_id
WHERE pa.product_id = p.id
  AND pa.deleted_at IS NULL
  AND (
    pa.attribute_name ILIKE '%supplier%'
    OR pa.attribute_name ILIKE '%cung cấp%'
  )
  AND pa.attribute_value ILIKE 'http%';

UPDATE categories SET name = 'Điện tử DSS', updated_at = CURRENT_TIMESTAMP
WHERE slug = 'dss-demo-electronics' AND deleted_at IS NULL
  AND name ILIKE '%DSS Demo%';

UPDATE categories SET name = 'Máy tính xách tay', updated_at = CURRENT_TIMESTAMP
WHERE slug = 'laptops' AND deleted_at IS NULL AND name = 'Laptop';
