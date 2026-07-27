-- V30: ensure every product has at least 3 images (pad with type-matched placeholders)

-- Helper: insert missing slots (2nd / 3rd) for products that already have a primary
INSERT INTO product_images (product_id, image_url, public_id, is_primary)
SELECT p.id,
       CASE
           WHEN c.slug IN ('phones', 'tablets') OR p.name ILIKE '%iphone%' OR p.name ILIKE '%samsung%' OR p.name ILIKE '%xiaomi%' OR p.name ILIKE '%ipad%'
               THEN CASE n.slot
                        WHEN 2 THEN 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=800&q=80'
                        ELSE 'https://images.unsplash.com/photo-1592899677977-9c10ca588bbd?w=800&q=80'
                    END
           WHEN c.slug IN ('laptops') OR p.name ILIKE '%macbook%' OR p.name ILIKE '%laptop%' OR p.name ILIKE '%dell%' OR p.name ILIKE '%hp %'
               THEN CASE n.slot
                        WHEN 2 THEN 'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=800&q=80'
                        ELSE 'https://images.unsplash.com/photo-1541807084-5c52b6b3adef?w=800&q=80'
                    END
           WHEN p.name ILIKE '%tai nghe%' OR p.name ILIKE '%airpods%' OR p.name ILIKE '%headphone%'
               THEN CASE n.slot
                        WHEN 2 THEN 'https://images.unsplash.com/photo-1606220945770-b5b6c2c55bf1?w=800&q=80'
                        ELSE 'https://images.unsplash.com/photo-1572569511254-d8f925fe2cbb?w=800&q=80'
                    END
           WHEN p.name ILIKE '%bàn phím%' OR p.name ILIKE '%keyboard%' OR p.name ILIKE '%chuột%' OR p.name ILIKE '%mouse%'
               THEN CASE n.slot
                        WHEN 2 THEN 'https://images.unsplash.com/photo-1615663245857-ac93bb7cde72?w=800&q=80'
                        ELSE 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=800&q=80'
                    END
           WHEN p.name ILIKE '%giày%' OR p.name ILIKE '%shoe%' OR p.name ILIKE '%oxford%'
               THEN CASE n.slot
                        WHEN 2 THEN 'https://images.unsplash.com/photo-1533867617858-e7b97e060509?w=800&q=80'
                        ELSE 'https://images.unsplash.com/photo-1449505278894-297fdb3edbc1?w=800&q=80'
                    END
           WHEN p.name ILIKE '%nồi%' OR p.name ILIKE '%chiên%' OR p.name ILIKE '%cook%'
               THEN CASE n.slot
                        WHEN 2 THEN 'https://images.unsplash.com/photo-1556911220-bff31c5750ea?w=800&q=80'
                        ELSE 'https://images.unsplash.com/photo-1585515320310-259814833e62?w=800&q=80'
                    END
           WHEN c.slug IN ('mens-fashion', 'womens-fashion') OR p.name ILIKE '%blazer%' OR p.name ILIKE '%dress%' OR p.name ILIKE '%shirt%'
               THEN CASE n.slot
                        WHEN 2 THEN 'https://images.unsplash.com/photo-1489987707025-afc232f7ea0f?w=800&q=80'
                        ELSE 'https://images.unsplash.com/photo-1515372039744-b8f02a3ae446?w=800&q=80'
                    END
           ELSE CASE n.slot
                    WHEN 2 THEN 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800&q=80'
                    ELSE 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800&q=80'
                END
       END AS url,
       'secdsp/products/pad-' || p.id || '-' || n.slot AS public_id,
       false AS is_primary
FROM products p
LEFT JOIN categories c ON c.id = p.category_id
CROSS JOIN (VALUES (2), (3)) AS n(slot)
WHERE p.deleted_at IS NULL
  AND (
      SELECT COUNT(*) FROM product_images pi
      WHERE pi.product_id = p.id AND pi.deleted_at IS NULL
  ) < n.slot
  AND NOT EXISTS (
      SELECT 1 FROM product_images pi
      WHERE pi.product_id = p.id
        AND pi.public_id = 'secdsp/products/pad-' || p.id || '-' || n.slot
        AND pi.deleted_at IS NULL
  );

-- Products with ZERO images: add 3 placeholders (first is primary)
INSERT INTO product_images (product_id, image_url, public_id, is_primary)
SELECT p.id,
       CASE n.slot
           WHEN 1 THEN 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800&q=80'
           WHEN 2 THEN 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800&q=80'
           ELSE 'https://images.unsplash.com/photo-1572635196237-14b3f281503f?w=800&q=80'
       END,
       'secdsp/products/empty-' || p.id || '-' || n.slot,
       (n.slot = 1)
FROM products p
CROSS JOIN (VALUES (1), (2), (3)) AS n(slot)
WHERE p.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM product_images pi
      WHERE pi.product_id = p.id AND pi.deleted_at IS NULL
  )
  AND NOT EXISTS (
      SELECT 1 FROM product_images pi
      WHERE pi.product_id = p.id
        AND pi.public_id = 'secdsp/products/empty-' || p.id || '-' || n.slot
        AND pi.deleted_at IS NULL
  );
