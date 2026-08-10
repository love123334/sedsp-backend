-- Idempotent demo vouchers for checkout / manager / seller flows

INSERT INTO vouchers (
    code, name, description, discount_type, discount_value, scope, seller_id,
    applies_to, minimum_order_amount, maximum_discount_amount, usage_limit,
    used_count, starts_at, ends_at, is_active, created_by
)
SELECT
    'SEDSP10',
    'Giảm 10% toàn sàn',
    'Voucher demo nền tảng — giảm 10%, tối đa 100.000đ',
    'PERCENTAGE',
    10,
    'PLATFORM',
    NULL,
    'ALL_PRODUCTS',
    200000,
    100000,
    500,
    0,
    NOW() - INTERVAL '1 day',
    NOW() + INTERVAL '365 days',
    TRUE,
    u.id
FROM users u
WHERE u.email = 'manager@sedsp.vn'
  AND NOT EXISTS (
      SELECT 1 FROM vouchers v
      WHERE v.scope = 'PLATFORM' AND UPPER(v.code) = 'SEDSP10'
  );

INSERT INTO vouchers (
    code, name, description, discount_type, discount_value, scope, seller_id,
    applies_to, minimum_order_amount, maximum_discount_amount, usage_limit,
    used_count, starts_at, ends_at, is_active, created_by
)
SELECT
    'SHOP50K',
    'Shop giảm 50K',
    'Voucher demo shop SEDSP Official',
    'FIXED',
    50000,
    'SHOP',
    s.id,
    'ALL_PRODUCTS',
    300000,
    NULL,
    200,
    0,
    NOW() - INTERVAL '1 day',
    NOW() + INTERVAL '180 days',
    TRUE,
    m.id
FROM users s
CROSS JOIN users m
WHERE s.email = 'seller@sedsp.vn'
  AND m.email = 'manager@sedsp.vn'
  AND NOT EXISTS (
      SELECT 1 FROM vouchers v
      WHERE v.scope = 'SHOP'
        AND v.seller_id = s.id
        AND UPPER(v.code) = 'SHOP50K'
  );
