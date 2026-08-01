-- V36: 30-day DELIVERED sales history for DSS testing (demand / price / inventory / what-if)
-- Idempotent via marker payment TXN_DSS30_SEED.

DO $$
DECLARE
    c1 BIGINT;
    c2 BIGINT;
    c3 BIGINT;
    buyer BIGINT;
    day_offset INT;
    o_id BIGINT;
    p_id BIGINT;
    s_id BIGINT;
    p_name TEXT;
    p_price NUMERIC(12, 2);
    qty INT;
    subtotal NUMERIC(12, 2);
    ship NUMERIC(12, 2) := 30000;
    txn TEXT;
    created_ts TIMESTAMP;
    n INT := 0;
BEGIN
    IF EXISTS (SELECT 1 FROM payments WHERE transaction_id = 'TXN_DSS30_SEED') THEN
        RAISE NOTICE 'V36 DSS 30-day sales already seeded — skip';
        RETURN;
    END IF;

    SELECT id INTO c1 FROM users WHERE email = 'customer01@gmail.com';
    SELECT id INTO c2 FROM users WHERE email = 'customer02@gmail.com';
    SELECT id INTO c3 FROM users WHERE email = 'customer03@gmail.com';

    IF c1 IS NULL OR c2 IS NULL OR c3 IS NULL THEN
        RAISE NOTICE 'V36 skip — demo customers missing (run V29 first)';
        RETURN;
    END IF;

    FOR day_offset IN 0..29 LOOP
        created_ts := (CURRENT_TIMESTAMP - make_interval(days => day_offset))
            - make_interval(hours => (day_offset % 5));

        FOR p_id, s_id, p_name, p_price IN
            SELECT p.id, p.seller_id, p.name, p.price
            FROM products p
            WHERE p.deleted_at IS NULL
              AND p.slug IN (
                  'iphone-15-pro-128gb',
                  'galaxy-tab-s9',
                  'dell-xps-15',
                  'google-pixel-9',
                  'oneplus-12',
                  'anker-65w-charger',
                  'men-slim-fit-blazer',
                  'oversized-hoodie',
                  'air-fryer-5l',
                  'adjustable-dumbbell-20kg',
                  'modern-sofa-3-seater',
                  'wireless-charging-pad'
              )
              AND MOD(p.id::int + day_offset, 3) = MOD(day_offset, 3)
            ORDER BY p.id
            LIMIT 4
        LOOP
            qty := 1 + MOD(day_offset + p_id::int, 3);
            subtotal := p_price * qty;
            buyer := CASE MOD(day_offset + p_id::int, 3)
                WHEN 0 THEN c1
                WHEN 1 THEN c2
                ELSE c3
            END;

            INSERT INTO orders (
                user_id, subtotal_amount, shipping_fee, discount_amount, total_amount,
                status, shipping_address, created_at, updated_at
            )
            VALUES (
                buyer, subtotal, ship, 0, subtotal + ship,
                'DELIVERED'::order_status,
                'DSS seed — lich su 30 ngay',
                created_ts, created_ts
            )
            RETURNING id INTO o_id;

            INSERT INTO order_items (
                order_id, product_id, seller_id,
                product_name_at_purchase, quantity, unit_price_at_purchase, subtotal
            )
            VALUES (o_id, p_id, s_id, p_name, qty, p_price, subtotal);

            txn := 'TXN_DSS30_D' || day_offset || '_P' || p_id || '_N' || n;
            INSERT INTO payments (
                order_id, payment_method, amount, status, transaction_id, paid_at, created_at, gateway_name
            )
            VALUES (
                o_id, 'VNPAY'::payment_method_enum, subtotal + ship,
                'SUCCESS'::payment_status, txn, created_ts, created_ts, 'VNPAY'
            );

            INSERT INTO order_tracking (order_id, event, updated_by, created_at) VALUES
                (o_id, 'CREATED'::order_tracking_event, buyer, created_ts),
                (o_id, 'PAYMENT_SUCCESS'::order_tracking_event, buyer, created_ts + interval '10 minutes'),
                (o_id, 'CONFIRMED'::order_tracking_event, s_id, created_ts + interval '2 hours'),
                (o_id, 'SHIPPED'::order_tracking_event, s_id, created_ts + interval '8 hours'),
                (o_id, 'DELIVERED'::order_tracking_event, s_id, created_ts + interval '1 day');

            n := n + 1;
        END LOOP;
    END LOOP;

    IF n = 0 THEN
        RAISE NOTICE 'V36 no matching products — nothing seeded';
        RETURN;
    END IF;

    -- Marker order (idempotency)
    SELECT p.id, p.seller_id, p.name INTO p_id, s_id, p_name
    FROM products p
    WHERE p.deleted_at IS NULL
    ORDER BY p.id
    LIMIT 1;

    INSERT INTO orders (
        user_id, subtotal_amount, shipping_fee, discount_amount, total_amount,
        status, shipping_address, created_at, updated_at
    )
    VALUES (
        c1, 1000, 0, 0, 1000,
        'DELIVERED'::order_status,
        'DSS seed marker',
        CURRENT_TIMESTAMP - interval '30 days',
        CURRENT_TIMESTAMP - interval '30 days'
    )
    RETURNING id INTO o_id;

    INSERT INTO order_items (
        order_id, product_id, seller_id,
        product_name_at_purchase, quantity, unit_price_at_purchase, subtotal
    )
    VALUES (o_id, p_id, s_id, COALESCE(p_name, 'Seed marker'), 1, 1000, 1000);

    INSERT INTO payments (
        order_id, payment_method, amount, status, transaction_id, paid_at, gateway_name
    )
    VALUES (
        o_id, 'COD'::payment_method_enum, 1000, 'SUCCESS'::payment_status,
        'TXN_DSS30_SEED', CURRENT_TIMESTAMP - interval '30 days', 'COD'
    );

    RAISE NOTICE 'V36 seeded % DSS sales line(s) across 30 days', n;
END $$;
