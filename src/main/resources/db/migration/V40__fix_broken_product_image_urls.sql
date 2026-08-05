-- V40: replace dead Unsplash / picsum product image URLs with working ones.
-- (Many seeded photo IDs now return 404 on images.unsplash.com.)

UPDATE product_images
SET image_url = 'https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf?w=800&q=80',
    updated_at = NOW()
WHERE deleted_at IS NULL
  AND image_url LIKE '%photo-1596755094514-f87e34085b85%';

UPDATE product_images
SET image_url = 'https://images.unsplash.com/photo-1460353581641-37baddab0fa2?w=800&q=80',
    updated_at = NOW()
WHERE deleted_at IS NULL
  AND image_url LIKE '%photo-1614252235816-8c852f74fa71%';

UPDATE product_images
SET image_url = 'https://images.unsplash.com/photo-1556228720-195a672e8a03?w=800&q=80',
    updated_at = NOW()
WHERE deleted_at IS NULL
  AND image_url LIKE '%photo-1620916569889-0f1dd397713b%';

UPDATE product_images
SET image_url = 'https://images.unsplash.com/photo-1596462502278-27bfdc403348?w=800&q=80',
    updated_at = NOW()
WHERE deleted_at IS NULL
  AND image_url LIKE '%photo-1586495777744-4413f2103256%';

UPDATE product_images
SET image_url = 'https://images.unsplash.com/photo-1583863788434-e58a36330cf0?w=800&q=80',
    updated_at = NOW()
WHERE deleted_at IS NULL
  AND image_url LIKE '%photo-1591290619762-c588f7cb0f81%';

UPDATE product_images
SET image_url = 'https://images.unsplash.com/photo-1610701596007-11502861dcfa?w=800&q=80',
    updated_at = NOW()
WHERE deleted_at IS NULL
  AND image_url LIKE '%photo-1578500494198-242dcfb7a7e2%';

UPDATE product_images
SET image_url = 'https://images.unsplash.com/photo-1556910103-1c02745aae4d?w=800&q=80',
    updated_at = NOW()
WHERE deleted_at IS NULL
  AND (
    image_url LIKE '%photo-1585515320310-259814833e87%'
    OR image_url LIKE '%photo-1584990347449-39b4c0c0c0c0%'
  );

UPDATE product_images
SET image_url = 'https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?w=800&q=80',
    updated_at = NOW()
WHERE deleted_at IS NULL
  AND image_url LIKE '%photo-1556911220-bff31c5750ea%';

UPDATE product_images
SET image_url = 'https://images.unsplash.com/photo-1579783902614-a3fb3927b6a5?w=800&q=80',
    updated_at = NOW()
WHERE deleted_at IS NULL
  AND image_url LIKE '%photo-1513519245088-0e12902e35a6%';

UPDATE product_images
SET image_url = 'https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=800&q=80',
    updated_at = NOW()
WHERE deleted_at IS NULL
  AND image_url LIKE '%photo-1563861826100-9cbac140d88b%';

UPDATE product_images
SET image_url = 'https://images.unsplash.com/photo-1556228720-195a672e8a03?w=800&q=80',
    updated_at = NOW()
WHERE deleted_at IS NULL
  AND image_url LIKE '%photo-1571781926291-c77df8098c7f%';

-- Legacy V24 picsum seeds → stable Unsplash
UPDATE product_images
SET image_url = 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800&q=80',
    updated_at = NOW()
WHERE deleted_at IS NULL
  AND image_url LIKE '%picsum.photos/seed/p1%';

UPDATE product_images
SET image_url = 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=800&q=80',
    updated_at = NOW()
WHERE deleted_at IS NULL
  AND image_url LIKE '%picsum.photos/seed/p2%';

UPDATE product_images
SET image_url = 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=800&q=80',
    updated_at = NOW()
WHERE deleted_at IS NULL
  AND image_url LIKE '%picsum.photos/seed/p4%';

UPDATE product_images
SET image_url = 'https://images.unsplash.com/photo-1556910103-1c02745aae4d?w=800&q=80',
    updated_at = NOW()
WHERE deleted_at IS NULL
  AND image_url LIKE '%picsum.photos/seed/p9%';
