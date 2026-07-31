-- Dev seed: users, catalog, inventory (password: 12345678)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO users (username, email, password, full_name, phone, status, role_id)
SELECT 'customer',
       'customer@sedsp.vn',
       crypt('12345678', gen_salt('bf', 10)),
       'Nguyễn Văn Khách',
       '0901234567',
       'ACTIVE'::user_status,
       r.id
FROM roles r
WHERE r.name = 'CUSTOMER'
  AND NOT EXISTS (SELECT 1 FROM users u WHERE u.email = 'customer@sedsp.vn');

INSERT INTO users (username, email, password, full_name, phone, status, role_id, store_name)
SELECT 'seller',
       'seller@sedsp.vn',
       crypt('12345678', gen_salt('bf', 10)),
       'Trần Thị Bán',
       '0912345678',
       'ACTIVE'::user_status,
       r.id,
       'SEDSP Official'
FROM roles r
WHERE r.name = 'SELLER'
  AND NOT EXISTS (SELECT 1 FROM users u WHERE u.email = 'seller@sedsp.vn');

INSERT INTO users (username, email, password, full_name, status, role_id)
SELECT 'manager',
       'manager@sedsp.vn',
       crypt('12345678', gen_salt('bf', 10)),
       'Lê Văn Quản',
       'ACTIVE'::user_status,
       r.id
FROM roles r
WHERE r.name = 'MANAGER'
  AND NOT EXISTS (SELECT 1 FROM users u WHERE u.email = 'manager@sedsp.vn');

INSERT INTO users (username, email, password, full_name, status, role_id)
SELECT 'admin',
       'admin@sedsp.vn',
       crypt('12345678', gen_salt('bf', 10)),
       'Phạm Admin',
       'ACTIVE'::user_status,
       r.id
FROM roles r
WHERE r.name = 'ADMIN'
  AND NOT EXISTS (SELECT 1 FROM users u WHERE u.email = 'admin@sedsp.vn');

INSERT INTO categories (name, slug)
SELECT 'Điện tử', 'dien-tu'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'dien-tu' AND deleted_at IS NULL);

INSERT INTO categories (name, slug)
SELECT 'Thể thao', 'the-thao'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'the-thao' AND deleted_at IS NULL);

INSERT INTO categories (name, slug)
SELECT 'Gia dụng', 'gia-dung'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'gia-dung' AND deleted_at IS NULL);

INSERT INTO products (seller_id, category_id, name, slug, description, price, cost_price, status)
SELECT u.id, c.id,
       'Tai nghe Bluetooth Pro ANC',
       'tai-nghe-bluetooth-pro-anc',
       'Tai nghe chống ồn chủ động, pin 30 giờ.',
       1890000, 1200000, 'ACTIVE'::product_status
FROM users u JOIN categories c ON c.slug = 'dien-tu'
WHERE u.email = 'seller@sedsp.vn'
  AND NOT EXISTS (SELECT 1 FROM products p WHERE p.slug = 'tai-nghe-bluetooth-pro-anc' AND p.deleted_at IS NULL);

INSERT INTO product_images (product_id, image_url, public_id, is_primary)
SELECT p.id, 'https://picsum.photos/seed/p1/400/400', 'secdsp/products/tai-nghe-bluetooth-pro-anc', TRUE
FROM products p WHERE p.slug = 'tai-nghe-bluetooth-pro-anc'
  AND NOT EXISTS (SELECT 1 FROM product_images pi WHERE pi.product_id = p.id AND pi.deleted_at IS NULL);

INSERT INTO inventory (product_id, available_quantity, reserved_quantity)
SELECT p.id, 45, 0 FROM products p WHERE p.slug = 'tai-nghe-bluetooth-pro-anc'
  AND NOT EXISTS (SELECT 1 FROM inventory i WHERE i.product_id = p.id);

INSERT INTO products (seller_id, category_id, name, slug, description, price, cost_price, status)
SELECT u.id, c.id,
       'Bàn phím cơ RGB KeyPro K87',
       'ban-phim-co-rgb-keypro-k87',
       'Switch đỏ, khung nhôm, RGB per-key.',
       2450000, 1800000, 'ACTIVE'::product_status
FROM users u JOIN categories c ON c.slug = 'dien-tu'
WHERE u.email = 'seller@sedsp.vn'
  AND NOT EXISTS (SELECT 1 FROM products p WHERE p.slug = 'ban-phim-co-rgb-keypro-k87' AND p.deleted_at IS NULL);

INSERT INTO product_images (product_id, image_url, public_id, is_primary)
SELECT p.id, 'https://picsum.photos/seed/p2/400/400', 'secdsp/products/ban-phim-co-rgb-keypro-k87', TRUE
FROM products p WHERE p.slug = 'ban-phim-co-rgb-keypro-k87'
  AND NOT EXISTS (SELECT 1 FROM product_images pi WHERE pi.product_id = p.id AND pi.deleted_at IS NULL);

INSERT INTO inventory (product_id, available_quantity, reserved_quantity)
SELECT p.id, 22, 0 FROM products p WHERE p.slug = 'ban-phim-co-rgb-keypro-k87'
  AND NOT EXISTS (SELECT 1 FROM inventory i WHERE i.product_id = p.id);

INSERT INTO products (seller_id, category_id, name, slug, description, price, cost_price, status)
SELECT u.id, c.id,
       'Giày chạy bộ AirFlex Marathon',
       'giay-chay-bo-airflex-marathon',
       'Đế foam nhẹ, thoáng khí.',
       1490000, 950000, 'ACTIVE'::product_status
FROM users u JOIN categories c ON c.slug = 'the-thao'
WHERE u.email = 'seller@sedsp.vn'
  AND NOT EXISTS (SELECT 1 FROM products p WHERE p.slug = 'giay-chay-bo-airflex-marathon' AND p.deleted_at IS NULL);

INSERT INTO product_images (product_id, image_url, public_id, is_primary)
SELECT p.id, 'https://picsum.photos/seed/p4/400/400', 'secdsp/products/giay-chay-bo-airflex-marathon', TRUE
FROM products p WHERE p.slug = 'giay-chay-bo-airflex-marathon'
  AND NOT EXISTS (SELECT 1 FROM product_images pi WHERE pi.product_id = p.id AND pi.deleted_at IS NULL);

INSERT INTO inventory (product_id, available_quantity, reserved_quantity)
SELECT p.id, 30, 0 FROM products p WHERE p.slug = 'giay-chay-bo-airflex-marathon'
  AND NOT EXISTS (SELECT 1 FROM inventory i WHERE i.product_id = p.id);

INSERT INTO products (seller_id, category_id, name, slug, description, price, cost_price, status)
SELECT u.id, c.id,
       'Nồi chiên không dầu 5L',
       'noi-chien-khong-dau-5l',
       'Công nghệ Rapid Air, 8 chế độ nấu.',
       1290000, 820000, 'ACTIVE'::product_status
FROM users u JOIN categories c ON c.slug = 'gia-dung'
WHERE u.email = 'seller@sedsp.vn'
  AND NOT EXISTS (SELECT 1 FROM products p WHERE p.slug = 'noi-chien-khong-dau-5l' AND p.deleted_at IS NULL);

INSERT INTO product_images (product_id, image_url, public_id, is_primary)
SELECT p.id, 'https://picsum.photos/seed/p9/400/400', 'secdsp/products/noi-chien-khong-dau-5l', TRUE
FROM products p WHERE p.slug = 'noi-chien-khong-dau-5l'
  AND NOT EXISTS (SELECT 1 FROM product_images pi WHERE pi.product_id = p.id AND pi.deleted_at IS NULL);

INSERT INTO inventory (product_id, available_quantity, reserved_quantity)
SELECT p.id, 28, 0 FROM products p WHERE p.slug = 'noi-chien-khong-dau-5l'
  AND NOT EXISTS (SELECT 1 FROM inventory i WHERE i.product_id = p.id);
