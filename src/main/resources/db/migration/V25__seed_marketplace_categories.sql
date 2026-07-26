-- Bổ sung danh mục marketplace còn thiếu so với FE
INSERT INTO categories (name, slug, parent_id, created_at, updated_at)
SELECT v.name, v.slug, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (VALUES
    ('Thời trang', 'thoi-trang'),
    ('Sách', 'sach'),
    ('Phụ kiện', 'phu-kien')
) AS v(name, slug)
WHERE NOT EXISTS (
    SELECT 1 FROM categories c
    WHERE c.slug = v.slug AND c.deleted_at IS NULL
);
