-- V27: Capstone demo catalog (users, categories, 50 products, orders, reviews)
-- Idempotent via email/slug guards. Passwords: Admin@123 / Manager@123 / Seller@123 / Customer@123
-- Images: Unsplash (product-type matched). Lookups by slug/email — no hardcoded IDs.

-- BCrypt hashes (Spring-compatible $2b$). Plain passwords:
-- Admin@123 | Manager@123 | Seller@123 | Customer@123
-- (pgcrypto crypt() hashes are not reliably verified by Spring BCryptPasswordEncoder)

-- ═══════════════════════════════════════════════════════════
-- 1) USERS
-- ═══════════════════════════════════════════════════════════

INSERT INTO users (username, email, password, full_name, phone, status, role_id)
SELECT v.username, v.email, v.pw_hash, v.full_name, v.phone,
       'ACTIVE'::user_status, r.id
FROM (VALUES
    ('admin01', 'admin01@secdsp.com', '$2b$10$.6CytaAUMzHCV8qIzrQJHeL1uq2xZSWIz3sl7lt4H0YdUu0lqbXzq', 'System Admin 01', '0901000001'),
    ('admin02', 'admin02@secdsp.com', '$2b$10$.6CytaAUMzHCV8qIzrQJHeL1uq2xZSWIz3sl7lt4H0YdUu0lqbXzq', 'System Admin 02', '0901000002'),
    ('admin03', 'admin03@secdsp.com', '$2b$10$.6CytaAUMzHCV8qIzrQJHeL1uq2xZSWIz3sl7lt4H0YdUu0lqbXzq', 'System Admin 03', '0901000003'),
    ('admin04', 'admin04@secdsp.com', '$2b$10$.6CytaAUMzHCV8qIzrQJHeL1uq2xZSWIz3sl7lt4H0YdUu0lqbXzq', 'System Admin 04', '0901000004')
) AS v(username, email, pw_hash, full_name, phone)
JOIN roles r ON r.name = 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.email = v.email);

INSERT INTO users (username, email, password, full_name, phone, status, role_id)
SELECT v.username, v.email, v.pw_hash, v.full_name, v.phone,
       'ACTIVE'::user_status, r.id
FROM (VALUES
    ('manager01', 'manager01@secdsp.com', '$2b$10$VYxA7/aejRwwabARc8oWNuM0FK90DP/GwCLS5UUUiCzYshx11RF7C', 'Business Manager 01', '0902000001'),
    ('manager02', 'manager02@secdsp.com', '$2b$10$VYxA7/aejRwwabARc8oWNuM0FK90DP/GwCLS5UUUiCzYshx11RF7C', 'Business Manager 02', '0902000002'),
    ('manager03', 'manager03@secdsp.com', '$2b$10$VYxA7/aejRwwabARc8oWNuM0FK90DP/GwCLS5UUUiCzYshx11RF7C', 'Business Manager 03', '0902000003')
) AS v(username, email, pw_hash, full_name, phone)
JOIN roles r ON r.name = 'MANAGER'
WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.email = v.email);

INSERT INTO users (username, email, password, full_name, phone, status, role_id,
                   store_name, business_email, business_phone, seller_description)
SELECT v.username, v.email, v.pw_hash, v.full_name, v.phone,
       'ACTIVE'::user_status, r.id,
       v.store_name, v.business_email, v.business_phone, v.seller_description
FROM (VALUES
    ('seller01', 'seller01@secdsp.com', '$2b$10$aqSXDEnhGzZYNf3ibiPHeeKAeqP/F6h/qEGg.XED/B4jd7dtxDEvi', 'Nguyen Tech', '0903000001',
     'NT Tech Store', 'contact@nttech.vn', '0903888001', 'Chuyên thiết bị điện tử chính hãng'),
    ('seller02', 'seller02@secdsp.com', '$2b$10$aqSXDEnhGzZYNf3ibiPHeeKAeqP/F6h/qEGg.XED/B4jd7dtxDEvi', 'Minh Electronics', '0903000002',
     'Minh Electronics', 'support@minhelec.vn', '0903888002', 'Laptop và phụ kiện cao cấp'),
    ('seller03', 'seller03@secdsp.com', '$2b$10$aqSXDEnhGzZYNf3ibiPHeeKAeqP/F6h/qEGg.XED/B4jd7dtxDEvi', 'Lan Fashion', '0903000003',
     'Lan Fashion House', 'contact@lanfashion.vn', '0903888003', 'Thời trang nam nữ hiện đại'),
    ('seller04', 'seller04@secdsp.com', '$2b$10$aqSXDEnhGzZYNf3ibiPHeeKAeqP/F6h/qEGg.XED/B4jd7dtxDEvi', 'Beauty Hub', '0903000004',
     'Beauty Hub', 'care@beautyhub.vn', '0903888004', 'Mỹ phẩm chính hãng Hàn Quốc'),
    ('seller05', 'seller05@secdsp.com', '$2b$10$aqSXDEnhGzZYNf3ibiPHeeKAeqP/F6h/qEGg.XED/B4jd7dtxDEvi', 'HomeStyle', '0903000005',
     'HomeStyle Living', 'hello@homestyle.vn', '0903888005', 'Nội thất và trang trí nhà cửa'),
    ('seller06', 'seller06@secdsp.com', '$2b$10$aqSXDEnhGzZYNf3ibiPHeeKAeqP/F6h/qEGg.XED/B4jd7dtxDEvi', 'Sport Max', '0903000006',
     'Sport Max Store', 'contact@sportmax.vn', '0903888006', 'Dụng cụ thể thao chuyên nghiệp'),
    ('seller07', 'seller07@secdsp.com', '$2b$10$aqSXDEnhGzZYNf3ibiPHeeKAeqP/F6h/qEGg.XED/B4jd7dtxDEvi', 'Digital World', '0903000007',
     'Digital World', 'support@digitalworld.vn', '0903888007', 'Điện thoại và tablet mới nhất'),
    ('seller08', 'seller08@secdsp.com', '$2b$10$aqSXDEnhGzZYNf3ibiPHeeKAeqP/F6h/qEGg.XED/B4jd7dtxDEvi', 'Urban Wear', '0903000008',
     'Urban Wear', 'contact@urbanwear.vn', '0903888008', 'Thời trang street style'),
    ('seller09', 'seller09@secdsp.com', '$2b$10$aqSXDEnhGzZYNf3ibiPHeeKAeqP/F6h/qEGg.XED/B4jd7dtxDEvi', 'Kitchen Pro', '0903000009',
     'Kitchen Pro', 'support@kitchenpro.vn', '0903888009', 'Thiết bị nhà bếp cao cấp'),
    ('seller10', 'seller10@secdsp.com', '$2b$10$aqSXDEnhGzZYNf3ibiPHeeKAeqP/F6h/qEGg.XED/B4jd7dtxDEvi', 'Outdoor Life', '0903000010',
     'Outdoor Life', 'hello@outdoorlife.vn', '0903888010', 'Dụng cụ dã ngoại và outdoor gear')
) AS v(username, email, pw_hash, full_name, phone, store_name, business_email, business_phone, seller_description)
JOIN roles r ON r.name = 'SELLER'
WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.email = v.email);

INSERT INTO users (username, email, password, full_name, phone, status, role_id)
SELECT v.username, v.email, v.pw_hash, v.full_name, v.phone,
       'ACTIVE'::user_status, r.id
FROM (VALUES
    ('customer01', 'customer01@gmail.com', '$2b$10$8k1kz.R67FCQv4uAiXGJZeSBBDR8qkh5CMbo9eUY63zcF9CMXqP/q', 'Tran Van A', '0910000001'),
    ('customer02', 'customer02@gmail.com', '$2b$10$8k1kz.R67FCQv4uAiXGJZeSBBDR8qkh5CMbo9eUY63zcF9CMXqP/q', 'Le Thi B', '0910000002'),
    ('customer03', 'customer03@gmail.com', '$2b$10$8k1kz.R67FCQv4uAiXGJZeSBBDR8qkh5CMbo9eUY63zcF9CMXqP/q', 'Pham Van C', '0910000003'),
    ('customer04', 'customer04@gmail.com', '$2b$10$8k1kz.R67FCQv4uAiXGJZeSBBDR8qkh5CMbo9eUY63zcF9CMXqP/q', 'Nguyen Thi D', '0910000004'),
    ('customer05', 'customer05@gmail.com', '$2b$10$8k1kz.R67FCQv4uAiXGJZeSBBDR8qkh5CMbo9eUY63zcF9CMXqP/q', 'Hoang Van E', '0910000005'),
    ('customer06', 'customer06@gmail.com', '$2b$10$8k1kz.R67FCQv4uAiXGJZeSBBDR8qkh5CMbo9eUY63zcF9CMXqP/q', 'Vu Thi F', '0910000006'),
    ('customer07', 'customer07@gmail.com', '$2b$10$8k1kz.R67FCQv4uAiXGJZeSBBDR8qkh5CMbo9eUY63zcF9CMXqP/q', 'Dang Van G', '0910000007'),
    ('customer08', 'customer08@gmail.com', '$2b$10$8k1kz.R67FCQv4uAiXGJZeSBBDR8qkh5CMbo9eUY63zcF9CMXqP/q', 'Bui Thi H', '0910000008'),
    ('customer09', 'customer09@gmail.com', '$2b$10$8k1kz.R67FCQv4uAiXGJZeSBBDR8qkh5CMbo9eUY63zcF9CMXqP/q', 'Ly Van I', '0910000009'),
    ('customer10', 'customer10@gmail.com', '$2b$10$8k1kz.R67FCQv4uAiXGJZeSBBDR8qkh5CMbo9eUY63zcF9CMXqP/q', 'Do Thi K', '0910000010')
) AS v(username, email, pw_hash, full_name, phone)
JOIN roles r ON r.name = 'CUSTOMER'
WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.email = v.email);

-- Fix passwords if users were seeded earlier with pgcrypto-incompatible hashes
UPDATE users SET password = '$2b$10$.6CytaAUMzHCV8qIzrQJHeL1uq2xZSWIz3sl7lt4H0YdUu0lqbXzq'
WHERE email LIKE 'admin0%@secdsp.com';
UPDATE users SET password = '$2b$10$VYxA7/aejRwwabARc8oWNuM0FK90DP/GwCLS5UUUiCzYshx11RF7C'
WHERE email LIKE 'manager0%@secdsp.com';
UPDATE users SET password = '$2b$10$aqSXDEnhGzZYNf3ibiPHeeKAeqP/F6h/qEGg.XED/B4jd7dtxDEvi'
WHERE email LIKE 'seller%@secdsp.com' AND email ~ '^seller[0-9]+@secdsp\.com$';
UPDATE users SET password = '$2b$10$8k1kz.R67FCQv4uAiXGJZeSBBDR8qkh5CMbo9eUY63zcF9CMXqP/q'
WHERE email LIKE 'customer%@gmail.com' AND email ~ '^customer[0-9]+@gmail\.com$';

-- ═══════════════════════════════════════════════════════════
-- 2) CATEGORIES (multi-level)
-- ═══════════════════════════════════════════════════════════

INSERT INTO categories (name, slug)
SELECT v.name, v.slug
FROM (VALUES
    ('Electronics', 'electronics'),
    ('Fashion', 'fashion'),
    ('Beauty', 'beauty'),
    ('Home & Living', 'home-living'),
    ('Sports', 'sports')
) AS v(name, slug)
WHERE NOT EXISTS (SELECT 1 FROM categories c WHERE c.slug = v.slug AND c.deleted_at IS NULL);

INSERT INTO categories (name, slug, parent_id)
SELECT v.name, v.slug, p.id
FROM (VALUES
    ('Phones', 'phones', 'electronics'),
    ('Laptops', 'laptops', 'electronics'),
    ('Tablets', 'tablets', 'electronics'),
    ('Accessories', 'electronics-accessories', 'electronics'),
    ('Men Clothing', 'men-clothing', 'fashion'),
    ('Women Clothing', 'women-clothing', 'fashion'),
    ('Shoes', 'shoes', 'fashion'),
    ('Skincare', 'skincare', 'beauty'),
    ('Makeup', 'makeup', 'beauty'),
    ('Kitchen', 'kitchen', 'home-living'),
    ('Furniture', 'furniture', 'home-living'),
    ('Decor', 'decor', 'home-living'),
    ('Fitness Equipment', 'fitness-equipment', 'sports'),
    ('Outdoor Gear', 'outdoor-gear', 'sports')
) AS v(name, slug, parent_slug)
JOIN categories p ON p.slug = v.parent_slug AND p.deleted_at IS NULL
WHERE NOT EXISTS (SELECT 1 FROM categories c WHERE c.slug = v.slug AND c.deleted_at IS NULL);

-- ═══════════════════════════════════════════════════════════
-- 3) PRODUCTS (50)
-- ═══════════════════════════════════════════════════════════

INSERT INTO products (seller_id, category_id, name, slug, description, price, cost_price, status)
SELECT s.id, c.id, v.name, v.slug, v.description, v.price, v.cost_price, 'ACTIVE'::product_status
FROM (VALUES
    -- seller01 NT Tech
    ('seller01@secdsp.com', 'phones', 'iPhone 15 Pro 128GB', 'iphone-15-pro-128gb', 'Latest Apple smartphone with A17 chip', 29990000::numeric, 25000000::numeric),
    ('seller01@secdsp.com', 'phones', 'Samsung Galaxy S24', 'samsung-galaxy-s24', 'Flagship Samsung phone 2026 edition', 24990000, 21000000),
    ('seller01@secdsp.com', 'laptops', 'MacBook Air M3', 'macbook-air-m3', 'Lightweight laptop with Apple M3 chip', 32990000, 28000000),
    ('seller01@secdsp.com', 'electronics-accessories', 'AirPods Pro 2', 'airpods-pro-2', 'Wireless earbuds with noise cancelling', 5990000, 4500000),
    ('seller01@secdsp.com', 'tablets', 'iPad Air 6', 'ipad-air-6', 'Apple tablet for productivity and entertainment', 18990000, 15000000),
    -- seller02 Minh Electronics
    ('seller02@secdsp.com', 'laptops', 'Dell XPS 15', 'dell-xps-15', 'Premium laptop for professionals', 38990000, 33000000),
    ('seller02@secdsp.com', 'laptops', 'HP Spectre x360', 'hp-spectre-x360', 'Convertible ultrabook high performance', 35990000, 30000000),
    ('seller02@secdsp.com', 'phones', 'Xiaomi 14 Ultra', 'xiaomi-14-ultra', 'High-end Xiaomi flagship phone', 21990000, 18000000),
    ('seller02@secdsp.com', 'electronics-accessories', 'Logitech MX Master 3S', 'logitech-mx-master-3s', 'Advanced wireless mouse', 2990000, 2200000),
    ('seller02@secdsp.com', 'electronics-accessories', 'Samsung T7 SSD 1TB', 'samsung-t7-ssd-1tb', 'Portable high speed SSD', 3490000, 2600000),
    -- seller03 Lan Fashion
    ('seller03@secdsp.com', 'men-clothing', 'Men Slim Fit Blazer', 'men-slim-fit-blazer', 'Elegant slim fit blazer for men', 1290000, 900000),
    ('seller03@secdsp.com', 'men-clothing', 'Men Casual Shirt', 'men-casual-shirt', 'Comfortable cotton casual shirt', 499000, 320000),
    ('seller03@secdsp.com', 'women-clothing', 'Women Floral Dress', 'women-floral-dress', 'Summer floral dress', 899000, 600000),
    ('seller03@secdsp.com', 'women-clothing', 'Women Office Skirt', 'women-office-skirt', 'Professional office skirt', 699000, 450000),
    ('seller03@secdsp.com', 'shoes', 'Leather Oxford Shoes', 'leather-oxford-shoes', 'Classic leather shoes for men', 1590000, 1100000),
    -- seller04 Beauty Hub
    ('seller04@secdsp.com', 'skincare', 'Centella Facial Cleanser', 'centella-facial-cleanser', 'Gentle facial cleanser', 299000, 180000),
    ('seller04@secdsp.com', 'skincare', 'Vitamin C Serum 30ml', 'vitamin-c-serum-30ml', 'Brightening vitamin C serum', 459000, 300000),
    ('seller04@secdsp.com', 'makeup', 'Matte Lipstick Set', 'matte-lipstick-set', 'Long lasting matte lipstick', 399000, 250000),
    ('seller04@secdsp.com', 'skincare', 'Hyaluronic Acid Serum', 'hyaluronic-acid-serum', 'Hydrating serum for all skin types', 499000, 320000),
    ('seller04@secdsp.com', 'makeup', 'Cushion Foundation', 'cushion-foundation', 'Lightweight cushion foundation', 529000, 350000),
    -- seller05 HomeStyle
    ('seller05@secdsp.com', 'furniture', 'Modern Sofa 3 Seater', 'modern-sofa-3-seater', 'Comfortable modern sofa', 15990000, 12000000),
    ('seller05@secdsp.com', 'furniture', 'Wooden Dining Table', 'wooden-dining-table', 'Solid wood dining table', 10990000, 8000000),
    ('seller05@secdsp.com', 'decor', 'Wall Art Canvas', 'wall-art-canvas', 'Minimalist wall decoration', 799000, 500000),
    ('seller05@secdsp.com', 'kitchen', 'Non-stick Cookware Set', 'non-stick-cookware-set', 'Premium kitchen cookware', 2499000, 1800000),
    ('seller05@secdsp.com', 'decor', 'LED Standing Lamp', 'led-standing-lamp', 'Decorative LED lamp', 1299000, 900000),
    -- seller06 Sport Max
    ('seller06@secdsp.com', 'fitness-equipment', 'Adjustable Dumbbell 20kg', 'adjustable-dumbbell-20kg', 'Fitness dumbbell set', 1499000, 1100000),
    ('seller06@secdsp.com', 'fitness-equipment', 'Yoga Mat Premium', 'yoga-mat-premium', 'Non-slip yoga mat', 399000, 250000),
    ('seller06@secdsp.com', 'fitness-equipment', 'Treadmill Pro X', 'treadmill-pro-x', 'Electric treadmill for home use', 12990000, 10000000),
    ('seller06@secdsp.com', 'outdoor-gear', 'Camping Tent 4 Person', 'camping-tent-4-person', 'Waterproof outdoor tent', 2999000, 2200000),
    ('seller06@secdsp.com', 'outdoor-gear', 'Hiking Backpack 40L', 'hiking-backpack-40l', 'Durable hiking backpack', 999000, 700000),
    -- seller07 Digital World
    ('seller07@secdsp.com', 'phones', 'Google Pixel 9', 'google-pixel-9', 'Pure Android flagship phone', 22990000, 19000000),
    ('seller07@secdsp.com', 'tablets', 'Samsung Galaxy Tab S9', 'galaxy-tab-s9', 'High-end Android tablet', 19990000, 16000000),
    ('seller07@secdsp.com', 'electronics-accessories', 'Anker 65W Charger', 'anker-65w-charger', 'Fast charging adapter', 899000, 600000),
    ('seller07@secdsp.com', 'phones', 'OnePlus 12', 'oneplus-12', 'Flagship killer smartphone', 17990000, 14500000),
    ('seller07@secdsp.com', 'electronics-accessories', 'Wireless Charging Pad', 'wireless-charging-pad', 'Qi wireless charger', 599000, 400000),
    -- seller08 Urban Wear
    ('seller08@secdsp.com', 'men-clothing', 'Oversized Hoodie', 'oversized-hoodie', 'Street style hoodie', 699000, 450000),
    ('seller08@secdsp.com', 'women-clothing', 'High Waist Jeans', 'high-waist-jeans', 'Trendy women jeans', 899000, 600000),
    ('seller08@secdsp.com', 'shoes', 'Sneakers Street Pro', 'sneakers-street-pro', 'Comfortable street sneakers', 1299000, 900000),
    ('seller08@secdsp.com', 'men-clothing', 'Graphic T-Shirt', 'graphic-tshirt', 'Cotton printed t-shirt', 399000, 250000),
    ('seller08@secdsp.com', 'women-clothing', 'Crop Top Basic', 'crop-top-basic', 'Basic crop top', 299000, 180000),
    -- seller09 Kitchen Pro
    ('seller09@secdsp.com', 'kitchen', 'Air Fryer 5L', 'air-fryer-5l', 'Healthy oil-free cooking', 2499000, 1900000),
    ('seller09@secdsp.com', 'kitchen', 'Blender 1000W', 'blender-1000w', 'High power kitchen blender', 1599000, 1200000),
    ('seller09@secdsp.com', 'kitchen', 'Electric Kettle 1.8L', 'electric-kettle-18l', 'Fast boiling kettle', 499000, 350000),
    ('seller09@secdsp.com', 'decor', 'Ceramic Vase Decor', 'ceramic-vase-decor', 'Minimalist decor vase', 599000, 400000),
    ('seller09@secdsp.com', 'decor', 'Wall Clock Modern', 'wall-clock-modern', 'Modern style wall clock', 699000, 500000),
    -- seller10 Outdoor Life
    ('seller10@secdsp.com', 'outdoor-gear', 'Sleeping Bag Winter', 'sleeping-bag-winter', 'Warm sleeping bag for camping', 1299000, 950000),
    ('seller10@secdsp.com', 'outdoor-gear', 'Portable Gas Stove', 'portable-gas-stove', 'Compact outdoor stove', 899000, 650000),
    ('seller10@secdsp.com', 'fitness-equipment', 'Resistance Band Set', 'resistance-band-set', 'Fitness resistance bands', 299000, 180000),
    ('seller10@secdsp.com', 'outdoor-gear', 'Camping Table Foldable', 'camping-table-foldable', 'Foldable outdoor table', 1499000, 1100000),
    ('seller10@secdsp.com', 'outdoor-gear', 'Outdoor Flashlight Pro', 'outdoor-flashlight-pro', 'High brightness flashlight', 499000, 300000)
) AS v(seller_email, cat_slug, name, slug, description, price, cost_price)
JOIN users s ON s.email = v.seller_email
JOIN categories c ON c.slug = v.cat_slug AND c.deleted_at IS NULL
WHERE NOT EXISTS (SELECT 1 FROM products p WHERE p.slug = v.slug AND p.deleted_at IS NULL);
