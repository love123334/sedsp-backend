-- V28: images, inventory, attributes, carts, orders, reviews, wishlists for V27 catalog

-- ═══════════════════════════════════════════════════════════
-- PRODUCT IMAGES (3 each, Unsplash — matched to product type)
-- ═══════════════════════════════════════════════════════════

INSERT INTO product_images (product_id, image_url, public_id, is_primary)
SELECT p.id, img.url, img.public_id, img.is_primary
FROM products p
JOIN (VALUES
    ('iphone-15-pro-128gb', 'https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=800&q=80', 'secdsp/products/iphone-15-pro-128gb-1', true),
    ('iphone-15-pro-128gb', 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=800&q=80', 'secdsp/products/iphone-15-pro-128gb-2', false),
    ('iphone-15-pro-128gb', 'https://images.unsplash.com/photo-1592899677977-9c10ca588bbd?w=800&q=80', 'secdsp/products/iphone-15-pro-128gb-3', false),
    ('samsung-galaxy-s24', 'https://images.unsplash.com/photo-1610945415295-d9bbf067e59c?w=800&q=80', 'secdsp/products/samsung-galaxy-s24-1', true),
    ('samsung-galaxy-s24', 'https://images.unsplash.com/photo-1598327105666-5b89351aff97?w=800&q=80', 'secdsp/products/samsung-galaxy-s24-2', false),
    ('samsung-galaxy-s24', 'https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?w=800&q=80', 'secdsp/products/samsung-galaxy-s24-3', false),
    ('macbook-air-m3', 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800&q=80', 'secdsp/products/macbook-air-m3-1', true),
    ('macbook-air-m3', 'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=800&q=80', 'secdsp/products/macbook-air-m3-2', false),
    ('macbook-air-m3', 'https://images.unsplash.com/photo-1541807084-5c52b6b3adef?w=800&q=80', 'secdsp/products/macbook-air-m3-3', false),
    ('airpods-pro-2', 'https://images.unsplash.com/photo-1600294037681-c80b4cb5b434?w=800&q=80', 'secdsp/products/airpods-pro-2-1', true),
    ('airpods-pro-2', 'https://images.unsplash.com/photo-1606220945770-b5b6c2c55bf1?w=800&q=80', 'secdsp/products/airpods-pro-2-2', false),
    ('airpods-pro-2', 'https://images.unsplash.com/photo-1572569511254-d8f925fe2cbb?w=800&q=80', 'secdsp/products/airpods-pro-2-3', false),
    ('ipad-air-6', 'https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=800&q=80', 'secdsp/products/ipad-air-6-1', true),
    ('ipad-air-6', 'https://images.unsplash.com/photo-1561154464-82e9adf32764?w=800&q=80', 'secdsp/products/ipad-air-6-2', false),
    ('ipad-air-6', 'https://images.unsplash.com/photo-1585790050230-5dd28404f065?w=800&q=80', 'secdsp/products/ipad-air-6-3', false),
    ('dell-xps-15', 'https://images.unsplash.com/photo-1593642632823-8f785ba67e45?w=800&q=80', 'secdsp/products/dell-xps-15-1', true),
    ('dell-xps-15', 'https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?w=800&q=80', 'secdsp/products/dell-xps-15-2', false),
    ('dell-xps-15', 'https://images.unsplash.com/photo-1525547719571-a2d4ac882e75?w=800&q=80', 'secdsp/products/dell-xps-15-3', false),
    ('hp-spectre-x360', 'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=800&q=80', 'secdsp/products/hp-spectre-x360-1', true),
    ('hp-spectre-x360', 'https://images.unsplash.com/photo-1484788984921-03950022c9ef?w=800&q=80', 'secdsp/products/hp-spectre-x360-2', false),
    ('hp-spectre-x360', 'https://images.unsplash.com/photo-1587614382346-4ec70e388b28?w=800&q=80', 'secdsp/products/hp-spectre-x360-3', false),
    ('xiaomi-14-ultra', 'https://images.unsplash.com/photo-1598327105666-5b89351aff97?w=800&q=80', 'secdsp/products/xiaomi-14-ultra-1', true),
    ('xiaomi-14-ultra', 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=800&q=80', 'secdsp/products/xiaomi-14-ultra-2', false),
    ('xiaomi-14-ultra', 'https://images.unsplash.com/photo-1580910051074-3eb694886505?w=800&q=80', 'secdsp/products/xiaomi-14-ultra-3', false),
    ('logitech-mx-master-3s', 'https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=800&q=80', 'secdsp/products/logitech-mx-master-3s-1', true),
    ('logitech-mx-master-3s', 'https://images.unsplash.com/photo-1615663245857-ac93bb7cde72?w=800&q=80', 'secdsp/products/logitech-mx-master-3s-2', false),
    ('logitech-mx-master-3s', 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=800&q=80', 'secdsp/products/logitech-mx-master-3s-3', false),
    ('samsung-t7-ssd-1tb', 'https://images.unsplash.com/photo-1597872200969-2b65d56bd16b?w=800&q=80', 'secdsp/products/samsung-t7-ssd-1tb-1', true),
    ('samsung-t7-ssd-1tb', 'https://images.unsplash.com/photo-1531492746076-161ca9bcad58?w=800&q=80', 'secdsp/products/samsung-t7-ssd-1tb-2', false),
    ('samsung-t7-ssd-1tb', 'https://images.unsplash.com/photo-1624823183493-ed5832f48f18?w=800&q=80', 'secdsp/products/samsung-t7-ssd-1tb-3', false),
    ('men-slim-fit-blazer', 'https://images.unsplash.com/photo-1594938298603-c8148c4dae35?w=800&q=80', 'secdsp/products/men-slim-fit-blazer-1', true),
    ('men-slim-fit-blazer', 'https://images.unsplash.com/photo-1507679799987-4db404882b8f?w=800&q=80', 'secdsp/products/men-slim-fit-blazer-2', false),
    ('men-slim-fit-blazer', 'https://images.unsplash.com/photo-1617127365659-c47fa864d8bc?w=800&q=80', 'secdsp/products/men-slim-fit-blazer-3', false),
    ('men-casual-shirt', 'https://images.unsplash.com/photo-1596755094514-f87e34085b85?w=800&q=80', 'secdsp/products/men-casual-shirt-1', true),
    ('men-casual-shirt', 'https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf?w=800&q=80', 'secdsp/products/men-casual-shirt-2', false),
    ('men-casual-shirt', 'https://images.unsplash.com/photo-1489987707025-afc232f7ea0f?w=800&q=80', 'secdsp/products/men-casual-shirt-3', false),
    ('women-floral-dress', 'https://images.unsplash.com/photo-1595777457583-95e059d581b8?w=800&q=80', 'secdsp/products/women-floral-dress-1', true),
    ('women-floral-dress', 'https://images.unsplash.com/photo-1572804013309-59a88b7e92f1?w=800&q=80', 'secdsp/products/women-floral-dress-2', false),
    ('women-floral-dress', 'https://images.unsplash.com/photo-1515372039744-b8f02a3ae446?w=800&q=80', 'secdsp/products/women-floral-dress-3', false),
    ('women-office-skirt', 'https://images.unsplash.com/photo-1583496661160-fb5886a0aaaa?w=800&q=80', 'secdsp/products/women-office-skirt-1', true),
    ('women-office-skirt', 'https://images.unsplash.com/photo-1551488831-00ddcb6c6bd3?w=800&q=80', 'secdsp/products/women-office-skirt-2', false),
    ('women-office-skirt', 'https://images.unsplash.com/photo-1564257631407-4deb1f99d992?w=800&q=80', 'secdsp/products/women-office-skirt-3', false),
    ('leather-oxford-shoes', 'https://images.unsplash.com/photo-1614252235816-8c852f74fa71?w=800&q=80', 'secdsp/products/leather-oxford-shoes-1', true),
    ('leather-oxford-shoes', 'https://images.unsplash.com/photo-1533867617858-e7b97e060509?w=800&q=80', 'secdsp/products/leather-oxford-shoes-2', false),
    ('leather-oxford-shoes', 'https://images.unsplash.com/photo-1449505278894-297fdb3edbc1?w=800&q=80', 'secdsp/products/leather-oxford-shoes-3', false),
    ('centella-facial-cleanser', 'https://images.unsplash.com/photo-1556228578-0d85b1a4d571?w=800&q=80', 'secdsp/products/centella-facial-cleanser-1', true),
    ('centella-facial-cleanser', 'https://images.unsplash.com/photo-1571781926291-c77df8098c7f?w=800&q=80', 'secdsp/products/centella-facial-cleanser-2', false),
    ('centella-facial-cleanser', 'https://images.unsplash.com/photo-1556228720-195a672e8a03?w=800&q=80', 'secdsp/products/centella-facial-cleanser-3', false),
    ('vitamin-c-serum-30ml', 'https://images.unsplash.com/photo-1620916569889-0f1dd397713b?w=800&q=80', 'secdsp/products/vitamin-c-serum-30ml-1', true),
    ('vitamin-c-serum-30ml', 'https://images.unsplash.com/photo-1608248543803-ba4f8c70ae0b?w=800&q=80', 'secdsp/products/vitamin-c-serum-30ml-2', false),
    ('vitamin-c-serum-30ml', 'https://images.unsplash.com/photo-1570194065650-d99fb4b38b17?w=800&q=80', 'secdsp/products/vitamin-c-serum-30ml-3', false),
    ('matte-lipstick-set', 'https://images.unsplash.com/photo-1586495777744-4413f2103256?w=800&q=80', 'secdsp/products/matte-lipstick-set-1', true),
    ('matte-lipstick-set', 'https://images.unsplash.com/photo-1631214524020-7e18db9a8f92?w=800&q=80', 'secdsp/products/matte-lipstick-set-2', false),
    ('matte-lipstick-set', 'https://images.unsplash.com/photo-1596462502278-27bfdc403348?w=800&q=80', 'secdsp/products/matte-lipstick-set-3', false),
    ('hyaluronic-acid-serum', 'https://images.unsplash.com/photo-1611930022073-b7a4ba5fcccd?w=800&q=80', 'secdsp/products/hyaluronic-acid-serum-1', true),
    ('hyaluronic-acid-serum', 'https://images.unsplash.com/photo-1608248543803-ba4f8c70ae0b?w=800&q=80', 'secdsp/products/hyaluronic-acid-serum-2', false),
    ('hyaluronic-acid-serum', 'https://images.unsplash.com/photo-1620916569889-0f1dd397713b?w=800&q=80', 'secdsp/products/hyaluronic-acid-serum-3', false),
    ('cushion-foundation', 'https://images.unsplash.com/photo-1522335789203-aabd1fc54bc9?w=800&q=80', 'secdsp/products/cushion-foundation-1', true),
    ('cushion-foundation', 'https://images.unsplash.com/photo-1512496015851-a90fb38ba796?w=800&q=80', 'secdsp/products/cushion-foundation-2', false),
    ('cushion-foundation', 'https://images.unsplash.com/photo-1596462502278-27bfdc403348?w=800&q=80', 'secdsp/products/cushion-foundation-3', false),
    ('modern-sofa-3-seater', 'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=800&q=80', 'secdsp/products/modern-sofa-3-seater-1', true),
    ('modern-sofa-3-seater', 'https://images.unsplash.com/photo-1493663284031-b7e3aefcae8e?w=800&q=80', 'secdsp/products/modern-sofa-3-seater-2', false),
    ('modern-sofa-3-seater', 'https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=800&q=80', 'secdsp/products/modern-sofa-3-seater-3', false),
    ('wooden-dining-table', 'https://images.unsplash.com/photo-1617806118233-18e1de247200?w=800&q=80', 'secdsp/products/wooden-dining-table-1', true),
    ('wooden-dining-table', 'https://images.unsplash.com/photo-1595428774223-ef52624120d2?w=800&q=80', 'secdsp/products/wooden-dining-table-2', false),
    ('wooden-dining-table', 'https://images.unsplash.com/photo-1604578762246-41134e37f9cc?w=800&q=80', 'secdsp/products/wooden-dining-table-3', false),
    ('wall-art-canvas', 'https://images.unsplash.com/photo-1513519245088-0e12902e35a6?w=800&q=80', 'secdsp/products/wall-art-canvas-1', true),
    ('wall-art-canvas', 'https://images.unsplash.com/photo-1579783902614-a3fb3927b6a5?w=800&q=80', 'secdsp/products/wall-art-canvas-2', false),
    ('wall-art-canvas', 'https://images.unsplash.com/photo-1549887534-1541e9326642?w=800&q=80', 'secdsp/products/wall-art-canvas-3', false),
    ('non-stick-cookware-set', 'https://images.unsplash.com/photo-1556910103-1c02745aae4d?w=800&q=80', 'secdsp/products/non-stick-cookware-set-1', true),
    ('non-stick-cookware-set', 'https://images.unsplash.com/photo-1584990347449-39b4c0c0c0c0?w=800&q=80', 'secdsp/products/non-stick-cookware-set-2', false),
    ('non-stick-cookware-set', 'https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?w=800&q=80', 'secdsp/products/non-stick-cookware-set-3', false),
    ('led-standing-lamp', 'https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=800&q=80', 'secdsp/products/led-standing-lamp-1', true),
    ('led-standing-lamp', 'https://images.unsplash.com/photo-1513506003901-1e6a229e69d8?w=800&q=80', 'secdsp/products/led-standing-lamp-2', false),
    ('led-standing-lamp', 'https://images.unsplash.com/photo-1540932239986-30128078f3c5?w=800&q=80', 'secdsp/products/led-standing-lamp-3', false),
    ('adjustable-dumbbell-20kg', 'https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=800&q=80', 'secdsp/products/adjustable-dumbbell-20kg-1', true),
    ('adjustable-dumbbell-20kg', 'https://images.unsplash.com/photo-1576678927484-cc907957088c?w=800&q=80', 'secdsp/products/adjustable-dumbbell-20kg-2', false),
    ('adjustable-dumbbell-20kg', 'https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800&q=80', 'secdsp/products/adjustable-dumbbell-20kg-3', false),
    ('yoga-mat-premium', 'https://images.unsplash.com/photo-1601925260368-ae2f83cf8b7f?w=800&q=80', 'secdsp/products/yoga-mat-premium-1', true),
    ('yoga-mat-premium', 'https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=800&q=80', 'secdsp/products/yoga-mat-premium-2', false),
    ('yoga-mat-premium', 'https://images.unsplash.com/photo-1599901860904-17e6ed7083a0?w=800&q=80', 'secdsp/products/yoga-mat-premium-3', false),
    ('treadmill-pro-x', 'https://images.unsplash.com/photo-1576678927484-cc907957088c?w=800&q=80', 'secdsp/products/treadmill-pro-x-1', true),
    ('treadmill-pro-x', 'https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=800&q=80', 'secdsp/products/treadmill-pro-x-2', false),
    ('treadmill-pro-x', 'https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=800&q=80', 'secdsp/products/treadmill-pro-x-3', false),
    ('camping-tent-4-person', 'https://images.unsplash.com/photo-1478131143081-80f7f84ca84d?w=800&q=80', 'secdsp/products/camping-tent-4-person-1', true),
    ('camping-tent-4-person', 'https://images.unsplash.com/photo-1504280390367-361c6d9f38f4?w=800&q=80', 'secdsp/products/camping-tent-4-person-2', false),
    ('camping-tent-4-person', 'https://images.unsplash.com/photo-1523987355523-c7b5b0dd90a7?w=800&q=80', 'secdsp/products/camping-tent-4-person-3', false),
    ('hiking-backpack-40l', 'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=800&q=80', 'secdsp/products/hiking-backpack-40l-1', true),
    ('hiking-backpack-40l', 'https://images.unsplash.com/photo-1622260614153-03223fb72032?w=800&q=80', 'secdsp/products/hiking-backpack-40l-2', false),
    ('hiking-backpack-40l', 'https://images.unsplash.com/photo-1501555088652-021faa106b9b?w=800&q=80', 'secdsp/products/hiking-backpack-40l-3', false),
    ('google-pixel-9', 'https://images.unsplash.com/photo-1598327105666-5b89351aff97?w=800&q=80', 'secdsp/products/google-pixel-9-1', true),
    ('google-pixel-9', 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=800&q=80', 'secdsp/products/google-pixel-9-2', false),
    ('google-pixel-9', 'https://images.unsplash.com/photo-1580910051074-3eb694886505?w=800&q=80', 'secdsp/products/google-pixel-9-3', false),
    ('galaxy-tab-s9', 'https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=800&q=80', 'secdsp/products/galaxy-tab-s9-1', true),
    ('galaxy-tab-s9', 'https://images.unsplash.com/photo-1561154464-82e9adf32764?w=800&q=80', 'secdsp/products/galaxy-tab-s9-2', false),
    ('galaxy-tab-s9', 'https://images.unsplash.com/photo-1585790050230-5dd28404f065?w=800&q=80', 'secdsp/products/galaxy-tab-s9-3', false),
    ('anker-65w-charger', 'https://images.unsplash.com/photo-1583863788434-e58a36330cf0?w=800&q=80', 'secdsp/products/anker-65w-charger-1', true),
    ('anker-65w-charger', 'https://images.unsplash.com/photo-1609091839311-d5365f9ff1c5?w=800&q=80', 'secdsp/products/anker-65w-charger-2', false),
    ('anker-65w-charger', 'https://images.unsplash.com/photo-1619642751034-765dfdf7c58e?w=800&q=80', 'secdsp/products/anker-65w-charger-3', false),
    ('oneplus-12', 'https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?w=800&q=80', 'secdsp/products/oneplus-12-1', true),
    ('oneplus-12', 'https://images.unsplash.com/photo-1592899677977-9c10ca588bbd?w=800&q=80', 'secdsp/products/oneplus-12-2', false),
    ('oneplus-12', 'https://images.unsplash.com/photo-1610945415295-d9bbf067e59c?w=800&q=80', 'secdsp/products/oneplus-12-3', false),
    ('wireless-charging-pad', 'https://images.unsplash.com/photo-1591290619762-c588f7cb0f81?w=800&q=80', 'secdsp/products/wireless-charging-pad-1', true),
    ('wireless-charging-pad', 'https://images.unsplash.com/photo-1609091839311-d5365f9ff1c5?w=800&q=80', 'secdsp/products/wireless-charging-pad-2', false),
    ('wireless-charging-pad', 'https://images.unsplash.com/photo-1583863788434-e58a36330cf0?w=800&q=80', 'secdsp/products/wireless-charging-pad-3', false),
    ('oversized-hoodie', 'https://images.unsplash.com/photo-1556821840-3a63f95609a7?w=800&q=80', 'secdsp/products/oversized-hoodie-1', true),
    ('oversized-hoodie', 'https://images.unsplash.com/photo-1578587018452-892bacefd3f2?w=800&q=80', 'secdsp/products/oversized-hoodie-2', false),
    ('oversized-hoodie', 'https://images.unsplash.com/photo-1620799140408-edc6dcb6d633?w=800&q=80', 'secdsp/products/oversized-hoodie-3', false),
    ('high-waist-jeans', 'https://images.unsplash.com/photo-1541099649105-f69ad21f3246?w=800&q=80', 'secdsp/products/high-waist-jeans-1', true),
    ('high-waist-jeans', 'https://images.unsplash.com/photo-1582418702059-97ebafb35d09?w=800&q=80', 'secdsp/products/high-waist-jeans-2', false),
    ('high-waist-jeans', 'https://images.unsplash.com/photo-1475178626620-a4d049757c3b?w=800&q=80', 'secdsp/products/high-waist-jeans-3', false),
    ('sneakers-street-pro', 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=800&q=80', 'secdsp/products/sneakers-street-pro-1', true),
    ('sneakers-street-pro', 'https://images.unsplash.com/photo-1460353581641-37baddab0fa2?w=800&q=80', 'secdsp/products/sneakers-street-pro-2', false),
    ('sneakers-street-pro', 'https://images.unsplash.com/photo-1606107557195-0e29a4b5b4aa?w=800&q=80', 'secdsp/products/sneakers-street-pro-3', false),
    ('graphic-tshirt', 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=800&q=80', 'secdsp/products/graphic-tshirt-1', true),
    ('graphic-tshirt', 'https://images.unsplash.com/photo-1583743814966-8936f5b7be1a?w=800&q=80', 'secdsp/products/graphic-tshirt-2', false),
    ('graphic-tshirt', 'https://images.unsplash.com/photo-1576566588028-4147f3842f27?w=800&q=80', 'secdsp/products/graphic-tshirt-3', false),
    ('crop-top-basic', 'https://images.unsplash.com/photo-1434389677669-e08b4cac3105?w=800&q=80', 'secdsp/products/crop-top-basic-1', true),
    ('crop-top-basic', 'https://images.unsplash.com/photo-1503342217505-b0a15ec3261c?w=800&q=80', 'secdsp/products/crop-top-basic-2', false),
    ('crop-top-basic', 'https://images.unsplash.com/photo-1489987707025-afc232f7ea0f?w=800&q=80', 'secdsp/products/crop-top-basic-3', false),
    ('air-fryer-5l', 'https://images.unsplash.com/photo-1585515320310-259814833e87?w=800&q=80', 'secdsp/products/air-fryer-5l-1', true),
    ('air-fryer-5l', 'https://images.unsplash.com/photo-1556910103-1c02745aae4d?w=800&q=80', 'secdsp/products/air-fryer-5l-2', false),
    ('air-fryer-5l', 'https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?w=800&q=80', 'secdsp/products/air-fryer-5l-3', false),
    ('blender-1000w', 'https://images.unsplash.com/photo-1570222094114-d054a817e56b?w=800&q=80', 'secdsp/products/blender-1000w-1', true),
    ('blender-1000w', 'https://images.unsplash.com/photo-1585515320310-259814833e87?w=800&q=80', 'secdsp/products/blender-1000w-2', false),
    ('blender-1000w', 'https://images.unsplash.com/photo-1574269909862-7e1d70bb8078?w=800&q=80', 'secdsp/products/blender-1000w-3', false),
    ('electric-kettle-18l', 'https://images.unsplash.com/photo-1565193566173-7a0ee3dbe261?w=800&q=80', 'secdsp/products/electric-kettle-18l-1', true),
    ('electric-kettle-18l', 'https://images.unsplash.com/photo-1574269909862-7e1d70bb8078?w=800&q=80', 'secdsp/products/electric-kettle-18l-2', false),
    ('electric-kettle-18l', 'https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?w=800&q=80', 'secdsp/products/electric-kettle-18l-3', false),
    ('ceramic-vase-decor', 'https://images.unsplash.com/photo-1578500494198-242dcfb7a7e2?w=800&q=80', 'secdsp/products/ceramic-vase-decor-1', true),
    ('ceramic-vase-decor', 'https://images.unsplash.com/photo-1490312278597-a0b29a4b4e5d?w=800&q=80', 'secdsp/products/ceramic-vase-decor-2', false),
    ('ceramic-vase-decor', 'https://images.unsplash.com/photo-1581783342308-f792dbdd27c5?w=800&q=80', 'secdsp/products/ceramic-vase-decor-3', false),
    ('wall-clock-modern', 'https://images.unsplash.com/photo-1563861826100-9cbac140d88b?w=800&q=80', 'secdsp/products/wall-clock-modern-1', true),
    ('wall-clock-modern', 'https://images.unsplash.com/photo-1509048191080-d2984bad6ae5?w=800&q=80', 'secdsp/products/wall-clock-modern-2', false),
    ('wall-clock-modern', 'https://images.unsplash.com/photo-1563861826100-9cbac140d88b?w=800&q=80', 'secdsp/products/wall-clock-modern-3', false),
    ('sleeping-bag-winter', 'https://images.unsplash.com/photo-1504280390367-361c6d9f38f4?w=800&q=80', 'secdsp/products/sleeping-bag-winter-1', true),
    ('sleeping-bag-winter', 'https://images.unsplash.com/photo-1478131143081-80f7f84ca84d?w=800&q=80', 'secdsp/products/sleeping-bag-winter-2', false),
    ('sleeping-bag-winter', 'https://images.unsplash.com/photo-1523987355523-c7b5b0dd90a7?w=800&q=80', 'secdsp/products/sleeping-bag-winter-3', false),
    ('portable-gas-stove', 'https://images.unsplash.com/photo-1504851149312-7a075b496cc7?w=800&q=80', 'secdsp/products/portable-gas-stove-1', true),
    ('portable-gas-stove', 'https://images.unsplash.com/photo-1478131143081-80f7f84ca84d?w=800&q=80', 'secdsp/products/portable-gas-stove-2', false),
    ('portable-gas-stove', 'https://images.unsplash.com/photo-1504280390367-361c6d9f38f4?w=800&q=80', 'secdsp/products/portable-gas-stove-3', false),
    ('resistance-band-set', 'https://images.unsplash.com/photo-1598289431512-b97b0917affc?w=800&q=80', 'secdsp/products/resistance-band-set-1', true),
    ('resistance-band-set', 'https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=800&q=80', 'secdsp/products/resistance-band-set-2', false),
    ('resistance-band-set', 'https://images.unsplash.com/photo-1576678927484-cc907957088c?w=800&q=80', 'secdsp/products/resistance-band-set-3', false),
    ('camping-table-foldable', 'https://images.unsplash.com/photo-1504851149312-7a075b496cc7?w=800&q=80', 'secdsp/products/camping-table-foldable-1', true),
    ('camping-table-foldable', 'https://images.unsplash.com/photo-1523987355523-c7b5b0dd90a7?w=800&q=80', 'secdsp/products/camping-table-foldable-2', false),
    ('camping-table-foldable', 'https://images.unsplash.com/photo-1478131143081-80f7f84ca84d?w=800&q=80', 'secdsp/products/camping-table-foldable-3', false),
    ('outdoor-flashlight-pro', 'https://images.unsplash.com/photo-1504851149312-7a075b496cc7?w=800&q=80', 'secdsp/products/outdoor-flashlight-pro-1', true),
    ('outdoor-flashlight-pro', 'https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=800&q=80', 'secdsp/products/outdoor-flashlight-pro-2', false),
    ('outdoor-flashlight-pro', 'https://images.unsplash.com/photo-1492691527719-9d1e07e534b4?w=800&q=80', 'secdsp/products/outdoor-flashlight-pro-3', false)
) AS img(slug, url, public_id, is_primary) ON img.slug = p.slug
WHERE p.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM product_images pi
      WHERE pi.product_id = p.id AND pi.public_id = img.public_id AND pi.deleted_at IS NULL
  );

-- Fix one broken cookware URL (placeholder) — use kitchen photo
UPDATE product_images
SET image_url = 'https://images.unsplash.com/photo-1556911220-bff31c812dba?w=800&q=80'
WHERE public_id = 'secdsp/products/non-stick-cookware-set-2';

-- ═══════════════════════════════════════════════════════════
-- INVENTORY + PRICE HISTORY
-- ═══════════════════════════════════════════════════════════

INSERT INTO inventory (product_id, available_quantity, reserved_quantity)
SELECT p.id,
       80 + ((ROW_NUMBER() OVER (ORDER BY p.id) - 1) % 5) * 20,
       0
FROM products p
WHERE p.slug IN (
    SELECT slug FROM (VALUES
        ('iphone-15-pro-128gb'),('samsung-galaxy-s24'),('macbook-air-m3'),('airpods-pro-2'),('ipad-air-6'),
        ('dell-xps-15'),('hp-spectre-x360'),('xiaomi-14-ultra'),('logitech-mx-master-3s'),('samsung-t7-ssd-1tb'),
        ('men-slim-fit-blazer'),('men-casual-shirt'),('women-floral-dress'),('women-office-skirt'),('leather-oxford-shoes'),
        ('centella-facial-cleanser'),('vitamin-c-serum-30ml'),('matte-lipstick-set'),('hyaluronic-acid-serum'),('cushion-foundation'),
        ('modern-sofa-3-seater'),('wooden-dining-table'),('wall-art-canvas'),('non-stick-cookware-set'),('led-standing-lamp'),
        ('adjustable-dumbbell-20kg'),('yoga-mat-premium'),('treadmill-pro-x'),('camping-tent-4-person'),('hiking-backpack-40l'),
        ('google-pixel-9'),('galaxy-tab-s9'),('anker-65w-charger'),('oneplus-12'),('wireless-charging-pad'),
        ('oversized-hoodie'),('high-waist-jeans'),('sneakers-street-pro'),('graphic-tshirt'),('crop-top-basic'),
        ('air-fryer-5l'),('blender-1000w'),('electric-kettle-18l'),('ceramic-vase-decor'),('wall-clock-modern'),
        ('sleeping-bag-winter'),('portable-gas-stove'),('resistance-band-set'),('camping-table-foldable'),('outdoor-flashlight-pro')
    ) s(slug)
)
  AND NOT EXISTS (SELECT 1 FROM inventory i WHERE i.product_id = p.id);

INSERT INTO price_history (product_id, old_price, new_price, changed_by)
SELECT p.id, NULL, p.price, a.id
FROM products p
CROSS JOIN LATERAL (
    SELECT id FROM users WHERE email = 'admin01@secdsp.com' LIMIT 1
) a
WHERE p.slug IN (
    SELECT slug FROM (VALUES
        ('iphone-15-pro-128gb'),('samsung-galaxy-s24'),('macbook-air-m3'),('airpods-pro-2'),('ipad-air-6'),
        ('dell-xps-15'),('hp-spectre-x360'),('xiaomi-14-ultra'),('logitech-mx-master-3s'),('samsung-t7-ssd-1tb'),
        ('men-slim-fit-blazer'),('men-casual-shirt'),('women-floral-dress'),('women-office-skirt'),('leather-oxford-shoes'),
        ('centella-facial-cleanser'),('vitamin-c-serum-30ml'),('matte-lipstick-set'),('hyaluronic-acid-serum'),('cushion-foundation'),
        ('modern-sofa-3-seater'),('wooden-dining-table'),('wall-art-canvas'),('non-stick-cookware-set'),('led-standing-lamp'),
        ('adjustable-dumbbell-20kg'),('yoga-mat-premium'),('treadmill-pro-x'),('camping-tent-4-person'),('hiking-backpack-40l'),
        ('google-pixel-9'),('galaxy-tab-s9'),('anker-65w-charger'),('oneplus-12'),('wireless-charging-pad'),
        ('oversized-hoodie'),('high-waist-jeans'),('sneakers-street-pro'),('graphic-tshirt'),('crop-top-basic'),
        ('air-fryer-5l'),('blender-1000w'),('electric-kettle-18l'),('ceramic-vase-decor'),('wall-clock-modern'),
        ('sleeping-bag-winter'),('portable-gas-stove'),('resistance-band-set'),('camping-table-foldable'),('outdoor-flashlight-pro')
    ) s(slug)
)
  AND NOT EXISTS (SELECT 1 FROM price_history ph WHERE ph.product_id = p.id AND ph.old_price IS NULL);
