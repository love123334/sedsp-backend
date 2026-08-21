-- V62: empty leftover demo carts + copy MoMo wallet to every active seller.
-- Testers saw ghost cart lines (e.g. KeyPro) without adding them — clear all
-- cart_items. MoMo QR checkout needs every shop configured like SEDSP Official.

DELETE FROM cart_items;

UPDATE users AS seller
SET
    momo_phone = COALESCE(
        NULLIF(TRIM(seller.momo_phone), ''),
        NULLIF(TRIM(src.momo_phone), ''),
        NULLIF(TRIM(seller.phone), ''),
        NULLIF(TRIM(seller.business_phone), ''),
        '0912345678'
    ),
    momo_qr_url = COALESCE(
        NULLIF(TRIM(seller.momo_qr_url), ''),
        NULLIF(TRIM(src.momo_qr_url), '')
    ),
    updated_at = CURRENT_TIMESTAMP
FROM users AS src
CROSS JOIN roles AS seller_role
WHERE src.email = 'seller@sedsp.vn'
  AND seller_role.id = seller.role_id
  AND seller_role.name = 'SELLER'
  AND seller.deleted_at IS NULL
  AND seller.status = 'ACTIVE';

UPDATE users AS seller
SET
    momo_phone = COALESCE(
        NULLIF(TRIM(seller.momo_phone), ''),
        NULLIF(TRIM(seller.phone), ''),
        NULLIF(TRIM(seller.business_phone), ''),
        '0912345678'
    ),
    updated_at = CURRENT_TIMESTAMP
FROM roles AS seller_role
WHERE seller_role.id = seller.role_id
  AND seller_role.name = 'SELLER'
  AND seller.deleted_at IS NULL
  AND seller.status = 'ACTIVE'
  AND (
      seller.momo_phone IS NULL
      OR TRIM(seller.momo_phone) = ''
  );

-- Keep demo vouchers usable after heavy tester traffic.
UPDATE vouchers
SET
    is_active = TRUE,
    used_count = LEAST(used_count, 10),
    usage_limit = GREATEST(COALESCE(usage_limit, 0), 500),
    ends_at = GREATEST(ends_at, NOW() + INTERVAL '180 days')
WHERE UPPER(code) IN ('SEDSP10', 'SHOP50K');
