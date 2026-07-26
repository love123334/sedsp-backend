-- V29: attributes, carts, orders, payments, tracking, reviews, wishlists

-- ═══════════════════════════════════════════════════════════
-- PRODUCT ATTRIBUTES
-- ═══════════════════════════════════════════════════════════

INSERT INTO product_attributes (product_id, attribute_name, attribute_value)
SELECT p.id, 'Brand',
       CASE
           WHEN p.name ILIKE '%iPhone%' OR p.name ILIKE '%MacBook%' OR p.name ILIKE '%iPad%' OR p.name ILIKE '%AirPods%' THEN 'Apple'
           WHEN p.name ILIKE '%Samsung%' OR p.name ILIKE '%Galaxy%' THEN 'Samsung'
           WHEN p.name ILIKE '%Xiaomi%' THEN 'Xiaomi'
           WHEN p.name ILIKE '%Dell%' THEN 'Dell'
           WHEN p.name ILIKE '%HP%' THEN 'HP'
           WHEN p.name ILIKE '%Google%' OR p.name ILIKE '%Pixel%' THEN 'Google'
           WHEN p.name ILIKE '%OnePlus%' THEN 'OnePlus'
           WHEN p.name ILIKE '%Logitech%' THEN 'Logitech'
           WHEN p.name ILIKE '%Anker%' THEN 'Anker'
           ELSE 'Generic'
       END
FROM products p
JOIN categories c ON c.id = p.category_id
WHERE c.slug IN ('phones', 'laptops', 'tablets')
  AND p.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM product_attributes pa
      WHERE pa.product_id = p.id AND pa.attribute_name = 'Brand' AND pa.deleted_at IS NULL
  );

INSERT INTO product_attributes (product_id, attribute_name, attribute_value)
SELECT p.id, 'Warranty', '12 Months Official Warranty'
FROM products p
JOIN categories c ON c.id = p.category_id
WHERE c.slug IN ('phones', 'laptops', 'tablets')
  AND p.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM product_attributes pa
      WHERE pa.product_id = p.id AND pa.attribute_name = 'Warranty' AND pa.deleted_at IS NULL
  );

INSERT INTO product_attributes (product_id, attribute_name, attribute_value)
SELECT p.id, 'Compatibility', 'Universal'
FROM products p
JOIN categories c ON c.id = p.category_id
WHERE c.slug = 'electronics-accessories'
  AND p.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM product_attributes pa
      WHERE pa.product_id = p.id AND pa.attribute_name = 'Compatibility' AND pa.deleted_at IS NULL
  );

INSERT INTO product_attributes (product_id, attribute_name, attribute_value)
SELECT p.id, 'Material', 'Cotton Blend'
FROM products p
JOIN categories c ON c.id = p.category_id
WHERE c.slug IN ('men-clothing', 'women-clothing', 'shoes')
  AND p.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM product_attributes pa
      WHERE pa.product_id = p.id AND pa.attribute_name = 'Material' AND pa.deleted_at IS NULL
  );

INSERT INTO product_attributes (product_id, attribute_name, attribute_value)
SELECT p.id, 'Size', 'S,M,L,XL'
FROM products p
JOIN categories c ON c.id = p.category_id
WHERE c.slug IN ('men-clothing', 'women-clothing')
  AND p.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM product_attributes pa
      WHERE pa.product_id = p.id AND pa.attribute_name = 'Size' AND pa.deleted_at IS NULL
  );

INSERT INTO product_attributes (product_id, attribute_name, attribute_value)
SELECT p.id, 'Skin Type', 'All Skin Types'
FROM products p
JOIN categories c ON c.id = p.category_id
WHERE c.slug IN ('skincare', 'makeup')
  AND p.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM product_attributes pa
      WHERE pa.product_id = p.id AND pa.attribute_name = 'Skin Type' AND pa.deleted_at IS NULL
  );

INSERT INTO product_attributes (product_id, attribute_name, attribute_value)
SELECT p.id, 'Origin', 'Korea'
FROM products p
JOIN categories c ON c.id = p.category_id
WHERE c.slug IN ('skincare', 'makeup')
  AND p.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM product_attributes pa
      WHERE pa.product_id = p.id AND pa.attribute_name = 'Origin' AND pa.deleted_at IS NULL
  );

INSERT INTO product_attributes (product_id, attribute_name, attribute_value)
SELECT p.id, 'Material', 'Premium Wood'
FROM products p
JOIN categories c ON c.id = p.category_id
WHERE c.slug IN ('kitchen', 'furniture', 'decor')
  AND p.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM product_attributes pa
      WHERE pa.product_id = p.id AND pa.attribute_name = 'Material' AND pa.deleted_at IS NULL
  );

INSERT INTO product_attributes (product_id, attribute_name, attribute_value)
SELECT p.id, 'Usage', 'Indoor & Outdoor'
FROM products p
JOIN categories c ON c.id = p.category_id
WHERE c.slug IN ('fitness-equipment', 'outdoor-gear')
  AND p.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM product_attributes pa
      WHERE pa.product_id = p.id AND pa.attribute_name = 'Usage' AND pa.deleted_at IS NULL
  );

-- ═══════════════════════════════════════════════════════════
-- CARTS
-- ═══════════════════════════════════════════════════════════

INSERT INTO carts (user_id)
SELECT u.id FROM users u
WHERE u.email IN ('customer01@gmail.com', 'customer02@gmail.com', 'customer03@gmail.com')
  AND NOT EXISTS (SELECT 1 FROM carts c WHERE c.user_id = u.id AND c.deleted_at IS NULL);

INSERT INTO cart_items (cart_id, product_id, quantity)
SELECT c.id, p.id, v.qty
FROM (VALUES
    ('customer01@gmail.com', 'iphone-15-pro-128gb', 1),
    ('customer01@gmail.com', 'men-slim-fit-blazer', 2),
    ('customer02@gmail.com', 'dell-xps-15', 1),
    ('customer02@gmail.com', 'modern-sofa-3-seater', 1),
    ('customer03@gmail.com', 'adjustable-dumbbell-20kg', 1),
    ('customer03@gmail.com', 'google-pixel-9', 1)
) AS v(email, slug, qty)
JOIN users u ON u.email = v.email
JOIN carts c ON c.user_id = u.id AND c.deleted_at IS NULL
JOIN products p ON p.slug = v.slug AND p.deleted_at IS NULL
WHERE NOT EXISTS (
    SELECT 1 FROM cart_items ci
    WHERE ci.cart_id = c.id AND ci.product_id = p.id AND ci.deleted_at IS NULL
);

-- ═══════════════════════════════════════════════════════════
-- ORDERS (guard: skip if demo order TXN0001 already exists)
-- ═══════════════════════════════════════════════════════════

DO $$
DECLARE
    c1 BIGINT; c2 BIGINT; c3 BIGINT;
    s_nt BIGINT; s_minh BIGINT; s_lan BIGINT; s_home BIGINT; s_sport BIGINT; s_dig BIGINT; s_urban BIGINT; s_kit BIGINT;
    p_iphone BIGINT; p_blazer BIGINT; p_dell BIGINT; p_sofa BIGINT; p_dumb BIGINT; p_pixel BIGINT;
    p_fryer BIGINT; p_blend BIGINT; p_hoodie BIGINT;
    o1 BIGINT; o2 BIGINT; o3 BIGINT; o4 BIGINT; o5 BIGINT; o6 BIGINT;
    prev_qty INT;
BEGIN
    IF EXISTS (SELECT 1 FROM payments WHERE transaction_id = 'TXN0001') THEN
        RAISE NOTICE 'V29 demo orders already seeded — skip';
        RETURN;
    END IF;

    SELECT id INTO c1 FROM users WHERE email = 'customer01@gmail.com';
    SELECT id INTO c2 FROM users WHERE email = 'customer02@gmail.com';
    SELECT id INTO c3 FROM users WHERE email = 'customer03@gmail.com';
    SELECT id INTO s_nt FROM users WHERE email = 'seller01@secdsp.com';
    SELECT id INTO s_minh FROM users WHERE email = 'seller02@secdsp.com';
    SELECT id INTO s_lan FROM users WHERE email = 'seller03@secdsp.com';
    SELECT id INTO s_home FROM users WHERE email = 'seller05@secdsp.com';
    SELECT id INTO s_sport FROM users WHERE email = 'seller06@secdsp.com';
    SELECT id INTO s_dig FROM users WHERE email = 'seller07@secdsp.com';
    SELECT id INTO s_urban FROM users WHERE email = 'seller08@secdsp.com';
    SELECT id INTO s_kit FROM users WHERE email = 'seller09@secdsp.com';

    SELECT id INTO p_iphone FROM products WHERE slug = 'iphone-15-pro-128gb';
    SELECT id INTO p_blazer FROM products WHERE slug = 'men-slim-fit-blazer';
    SELECT id INTO p_dell FROM products WHERE slug = 'dell-xps-15';
    SELECT id INTO p_sofa FROM products WHERE slug = 'modern-sofa-3-seater';
    SELECT id INTO p_dumb FROM products WHERE slug = 'adjustable-dumbbell-20kg';
    SELECT id INTO p_pixel FROM products WHERE slug = 'google-pixel-9';
    SELECT id INTO p_fryer FROM products WHERE slug = 'air-fryer-5l';
    SELECT id INTO p_blend FROM products WHERE slug = 'blender-1000w';
    SELECT id INTO p_hoodie FROM products WHERE slug = 'oversized-hoodie';

    -- ORDER 1 DELIVERED
    INSERT INTO orders (user_id, subtotal_amount, shipping_fee, discount_amount, total_amount, status, shipping_address)
    VALUES (c1, 32570000, 50000, 0, 32620000, 'DELIVERED'::order_status, '123 Nguyen Trai, Ho Chi Minh City')
    RETURNING id INTO o1;

    INSERT INTO order_items (order_id, product_id, seller_id, product_name_at_purchase, quantity, unit_price_at_purchase, subtotal)
    VALUES
        (o1, p_iphone, s_nt, 'iPhone 15 Pro 128GB', 1, 29990000, 29990000),
        (o1, p_blazer, s_lan, 'Men Slim Fit Blazer', 2, 1290000, 2580000);

    INSERT INTO payments (order_id, payment_method, amount, status, transaction_id, paid_at)
    VALUES (o1, 'MOMO'::payment_method_enum, 32620000, 'SUCCESS'::payment_status, 'TXN0001', NOW());

    INSERT INTO order_tracking (order_id, event, updated_by) VALUES
        (o1, 'CREATED'::order_tracking_event, c1),
        (o1, 'PAYMENT_SUCCESS'::order_tracking_event, c1),
        (o1, 'CONFIRMED'::order_tracking_event, s_nt),
        (o1, 'SHIPPED'::order_tracking_event, s_nt),
        (o1, 'DELIVERED'::order_tracking_event, s_nt);

    SELECT available_quantity INTO prev_qty FROM inventory WHERE product_id = p_iphone;
    UPDATE inventory SET available_quantity = available_quantity - 1, updated_at = NOW() WHERE product_id = p_iphone;
    INSERT INTO inventory_logs (product_id, change_amount, previous_quantity, current_quantity, reason, updated_by)
    VALUES (p_iphone, -1, prev_qty, prev_qty - 1, 'ORDER'::inventory_log_reason, c1);

    SELECT available_quantity INTO prev_qty FROM inventory WHERE product_id = p_blazer;
    UPDATE inventory SET available_quantity = available_quantity - 2, updated_at = NOW() WHERE product_id = p_blazer;
    INSERT INTO inventory_logs (product_id, change_amount, previous_quantity, current_quantity, reason, updated_by)
    VALUES (p_blazer, -2, prev_qty, prev_qty - 2, 'ORDER'::inventory_log_reason, c1);

    -- ORDER 2 SHIPPING
    INSERT INTO orders (user_id, subtotal_amount, shipping_fee, discount_amount, total_amount, status, shipping_address)
    VALUES (c2, 15990000, 200000, 0, 16190000, 'SHIPPING'::order_status, '456 Le Loi, Ha Noi')
    RETURNING id INTO o2;

    INSERT INTO order_items (order_id, product_id, seller_id, product_name_at_purchase, quantity, unit_price_at_purchase, subtotal)
    VALUES (o2, p_sofa, s_home, 'Modern Sofa 3 Seater', 1, 15990000, 15990000);

    INSERT INTO payments (order_id, payment_method, amount, status, transaction_id, paid_at)
    VALUES (o2, 'BANK'::payment_method_enum, 16190000, 'SUCCESS'::payment_status, 'TXN0002', NOW());

    INSERT INTO order_tracking (order_id, event, updated_by) VALUES
        (o2, 'CREATED'::order_tracking_event, c2),
        (o2, 'PAYMENT_SUCCESS'::order_tracking_event, c2),
        (o2, 'CONFIRMED'::order_tracking_event, s_home),
        (o2, 'SHIPPED'::order_tracking_event, s_home);

    SELECT available_quantity INTO prev_qty FROM inventory WHERE product_id = p_sofa;
    UPDATE inventory SET available_quantity = available_quantity - 1, updated_at = NOW() WHERE product_id = p_sofa;
    INSERT INTO inventory_logs (product_id, change_amount, previous_quantity, current_quantity, reason, updated_by)
    VALUES (p_sofa, -1, prev_qty, prev_qty - 1, 'ORDER'::inventory_log_reason, c2);

    -- ORDER 3 PROCESSING
    INSERT INTO orders (user_id, subtotal_amount, shipping_fee, discount_amount, total_amount, status, shipping_address)
    VALUES (c3, 24489000, 50000, 500000, 24039000, 'PROCESSING'::order_status, '789 Tran Hung Dao, Da Nang')
    RETURNING id INTO o3;

    INSERT INTO order_items (order_id, product_id, seller_id, product_name_at_purchase, quantity, unit_price_at_purchase, subtotal)
    VALUES
        (o3, p_dumb, s_sport, 'Adjustable Dumbbell 20kg', 1, 1499000, 1499000),
        (o3, p_pixel, s_dig, 'Google Pixel 9', 1, 22990000, 22990000);

    INSERT INTO payments (order_id, payment_method, amount, status, transaction_id, paid_at)
    VALUES (o3, 'MOMO'::payment_method_enum, 24039000, 'SUCCESS'::payment_status, 'TXN0003', NOW());

    INSERT INTO order_tracking (order_id, event, updated_by) VALUES
        (o3, 'CREATED'::order_tracking_event, c3),
        (o3, 'PAYMENT_SUCCESS'::order_tracking_event, c3),
        (o3, 'CONFIRMED'::order_tracking_event, s_sport);

    SELECT available_quantity INTO prev_qty FROM inventory WHERE product_id = p_dumb;
    UPDATE inventory SET available_quantity = available_quantity - 1, updated_at = NOW() WHERE product_id = p_dumb;
    INSERT INTO inventory_logs (product_id, change_amount, previous_quantity, current_quantity, reason, updated_by)
    VALUES (p_dumb, -1, prev_qty, prev_qty - 1, 'ORDER'::inventory_log_reason, c3);

    SELECT available_quantity INTO prev_qty FROM inventory WHERE product_id = p_pixel;
    UPDATE inventory SET available_quantity = available_quantity - 1, updated_at = NOW() WHERE product_id = p_pixel;
    INSERT INTO inventory_logs (product_id, change_amount, previous_quantity, current_quantity, reason, updated_by)
    VALUES (p_pixel, -1, prev_qty, prev_qty - 1, 'ORDER'::inventory_log_reason, c3);

    -- ORDER 4 CANCELLED (no inventory deduct)
    INSERT INTO orders (user_id, subtotal_amount, shipping_fee, discount_amount, total_amount, status, shipping_address)
    VALUES (c1, 38990000, 50000, 0, 39040000, 'CANCELLED'::order_status, '123 Nguyen Trai, Ho Chi Minh City')
    RETURNING id INTO o4;

    INSERT INTO order_items (order_id, product_id, seller_id, product_name_at_purchase, quantity, unit_price_at_purchase, subtotal)
    VALUES (o4, p_dell, s_minh, 'Dell XPS 15', 1, 38990000, 38990000);

    INSERT INTO payments (order_id, payment_method, amount, status, transaction_id)
    VALUES (o4, 'BANK'::payment_method_enum, 39040000, 'FAILED'::payment_status, 'TXN0004');

    INSERT INTO order_tracking (order_id, event, updated_by) VALUES
        (o4, 'CREATED'::order_tracking_event, c1),
        (o4, 'PAYMENT_FAILED'::order_tracking_event, c1),
        (o4, 'CANCELLED_BY_USER'::order_tracking_event, c1);

    -- ORDER 5 DELIVERED
    INSERT INTO orders (user_id, subtotal_amount, shipping_fee, discount_amount, total_amount, status, shipping_address)
    VALUES (c2, 4098000, 30000, 0, 4128000, 'DELIVERED'::order_status, '456 Le Loi, Ha Noi')
    RETURNING id INTO o5;

    INSERT INTO order_items (order_id, product_id, seller_id, product_name_at_purchase, quantity, unit_price_at_purchase, subtotal)
    VALUES
        (o5, p_fryer, s_kit, 'Air Fryer 5L', 1, 2499000, 2499000),
        (o5, p_blend, s_kit, 'Blender 1000W', 1, 1599000, 1599000);

    INSERT INTO payments (order_id, payment_method, amount, status, transaction_id, paid_at)
    VALUES (o5, 'COD'::payment_method_enum, 4128000, 'SUCCESS'::payment_status, 'TXN0005', NOW());

    INSERT INTO order_tracking (order_id, event, updated_by) VALUES
        (o5, 'CREATED'::order_tracking_event, c2),
        (o5, 'PAYMENT_SUCCESS'::order_tracking_event, c2),
        (o5, 'CONFIRMED'::order_tracking_event, s_kit),
        (o5, 'SHIPPED'::order_tracking_event, s_kit),
        (o5, 'DELIVERED'::order_tracking_event, s_kit);

    SELECT available_quantity INTO prev_qty FROM inventory WHERE product_id = p_fryer;
    UPDATE inventory SET available_quantity = available_quantity - 1, updated_at = NOW() WHERE product_id = p_fryer;
    INSERT INTO inventory_logs (product_id, change_amount, previous_quantity, current_quantity, reason, updated_by)
    VALUES (p_fryer, -1, prev_qty, prev_qty - 1, 'ORDER'::inventory_log_reason, c2);

    SELECT available_quantity INTO prev_qty FROM inventory WHERE product_id = p_blend;
    UPDATE inventory SET available_quantity = available_quantity - 1, updated_at = NOW() WHERE product_id = p_blend;
    INSERT INTO inventory_logs (product_id, change_amount, previous_quantity, current_quantity, reason, updated_by)
    VALUES (p_blend, -1, prev_qty, prev_qty - 1, 'ORDER'::inventory_log_reason, c2);

    -- ORDER 6 PAID
    INSERT INTO orders (user_id, subtotal_amount, shipping_fee, discount_amount, total_amount, status, shipping_address)
    VALUES (c3, 699000, 30000, 0, 729000, 'PAID'::order_status, '789 Tran Hung Dao, Da Nang')
    RETURNING id INTO o6;

    INSERT INTO order_items (order_id, product_id, seller_id, product_name_at_purchase, quantity, unit_price_at_purchase, subtotal)
    VALUES (o6, p_hoodie, s_urban, 'Oversized Hoodie', 1, 699000, 699000);

    INSERT INTO payments (order_id, payment_method, amount, status, transaction_id, paid_at)
    VALUES (o6, 'MOMO'::payment_method_enum, 729000, 'SUCCESS'::payment_status, 'TXN0006', NOW());

    INSERT INTO order_tracking (order_id, event, updated_by) VALUES
        (o6, 'CREATED'::order_tracking_event, c3),
        (o6, 'PAYMENT_SUCCESS'::order_tracking_event, c3);

    SELECT available_quantity INTO prev_qty FROM inventory WHERE product_id = p_hoodie;
    UPDATE inventory SET available_quantity = available_quantity - 1, updated_at = NOW() WHERE product_id = p_hoodie;
    INSERT INTO inventory_logs (product_id, change_amount, previous_quantity, current_quantity, reason, updated_by)
    VALUES (p_hoodie, -1, prev_qty, prev_qty - 1, 'ORDER'::inventory_log_reason, c3);
END $$;

-- ═══════════════════════════════════════════════════════════
-- REVIEWS + WISHLISTS
-- ═══════════════════════════════════════════════════════════

INSERT INTO product_reviews (product_id, user_id, rating, comment)
SELECT p.id, u.id, v.rating, v.comment
FROM (VALUES
    ('customer01@gmail.com', 'iphone-15-pro-128gb', 5, 'Excellent performance and camera quality'),
    ('customer01@gmail.com', 'men-slim-fit-blazer', 4, 'Good fabric but size runs slightly large'),
    ('customer02@gmail.com', 'modern-sofa-3-seater', 5, 'Very comfortable sofa, delivery was smooth'),
    ('customer02@gmail.com', 'air-fryer-5l', 5, 'Air fryer works perfectly'),
    ('customer03@gmail.com', 'adjustable-dumbbell-20kg', 4, 'Solid dumbbell set for home workout'),
    ('customer03@gmail.com', 'google-pixel-9', 5, 'Pixel phone camera is amazing'),
    ('customer03@gmail.com', 'oversized-hoodie', 4, 'Nice hoodie, comfortable to wear')
) AS v(email, slug, rating, comment)
JOIN users u ON u.email = v.email
JOIN products p ON p.slug = v.slug AND p.deleted_at IS NULL
WHERE NOT EXISTS (
    SELECT 1 FROM product_reviews pr
    WHERE pr.user_id = u.id AND pr.product_id = p.id AND pr.deleted_at IS NULL
);

INSERT INTO wishlists (user_id)
SELECT u.id FROM users u
WHERE u.email IN ('customer01@gmail.com', 'customer02@gmail.com')
  AND NOT EXISTS (SELECT 1 FROM wishlists w WHERE w.user_id = u.id);

INSERT INTO wishlist_items (wishlist_id, product_id)
SELECT w.id, p.id
FROM (VALUES
    ('customer01@gmail.com', 'dell-xps-15'),
    ('customer01@gmail.com', 'google-pixel-9'),
    ('customer02@gmail.com', 'iphone-15-pro-128gb'),
    ('customer02@gmail.com', 'adjustable-dumbbell-20kg')
) AS v(email, slug)
JOIN users u ON u.email = v.email
JOIN wishlists w ON w.user_id = u.id
JOIN products p ON p.slug = v.slug AND p.deleted_at IS NULL
WHERE NOT EXISTS (
    SELECT 1 FROM wishlist_items wi WHERE wi.wishlist_id = w.id AND wi.product_id = p.id
);
