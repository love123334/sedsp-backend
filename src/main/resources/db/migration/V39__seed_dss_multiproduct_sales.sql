-- V39: denser multi-product DELIVERED baskets for DSS (demand / price / inventory / what-if).
-- Idempotent via marker payment TXN_DSS39_MULTI.

DO $$
DECLARE
    c1 BIGINT;
    c2 BIGINT;
    c3 BIGINT;
    buyer BIGINT;
    day_offset INT;
    o_id BIGINT;
    created_ts TIMESTAMP;
    ship NUMERIC(12, 2) := 25000;
    basket_subtotal NUMERIC(12, 2);
    item_rec RECORD;
    item_count INT;
    qty INT;
    line_total NUMERIC(12, 2);
    n INT := 0;
BEGIN
    IF EXISTS (SELECT 1 FROM payments WHERE transaction_id = 'TXN_DSS39_MULTI') THEN
        RAISE NOTICE 'V39 multi-product DSS sales already seeded — skip';
        RETURN;
    END IF;

    SELECT id INTO c1 FROM users WHERE email = 'customer01@gmail.com';
    SELECT id INTO c2 FROM users WHERE email = 'customer02@gmail.com';
    SELECT id INTO c3 FROM users WHERE email = 'customer03@gmail.com';

    IF c1 IS NULL OR c2 IS NULL OR c3 IS NULL THEN
        RAISE NOTICE 'V39 skip — demo customers missing';
        RETURN;
    END IF;

    FOR day_offset IN 0..44 LOOP
        created_ts := (CURRENT_TIMESTAMP - make_interval(days => day_offset))
            - make_interval(hours => (day_offset % 7));
        buyer := CASE MOD(day_offset, 3)
            WHEN 0 THEN c1
            WHEN 1 THEN c2
            ELSE c3
        END;

        INSERT INTO orders (
            user_id, subtotal_amount, shipping_fee, discount_amount, total_amount,
            status, shipping_address, created_at, updated_at
        )
        VALUES (
            buyer, 0, ship, 0, ship,
            'DELIVERED'::order_status,
            'DSS seed — gio hang nhieu mon V39',
            created_ts, created_ts
        )
        RETURNING id INTO o_id;

        basket_subtotal := 0;
        item_count := 0;

        FOR item_rec IN
            SELECT p.id AS product_id,
                   p.seller_id,
                   p.name AS product_name,
                   p.price
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
                  'blender-1000w',
                  'adjustable-dumbbell-20kg',
                  'modern-sofa-3-seater',
                  'wireless-charging-pad',
                  'tai-nghe-bluetooth-pro-anc',
                  'ban-phim-co-rgb-keypro-k87',
                  'giay-chay-bo-airflex-marathon',
                  'noi-chien-khong-dau-5l'
              )
              AND MOD(p.id::int + day_offset, 5) <> MOD(day_offset, 5)
            ORDER BY MOD(p.id::int + day_offset * 3, 97), p.id
            LIMIT (2 + MOD(day_offset, 3))
        LOOP
            qty := 1 + MOD(day_offset + item_rec.product_id::int, 2);
            line_total := item_rec.price * qty;

            INSERT INTO order_items (
                order_id, product_id, seller_id,
                product_name_at_purchase, quantity, unit_price_at_purchase, subtotal
            )
            VALUES (
                o_id,
                item_rec.product_id,
                item_rec.seller_id,
                item_rec.product_name,
                qty,
                item_rec.price,
                line_total
            );

            basket_subtotal := basket_subtotal + line_total;
            item_count := item_count + 1;
        END LOOP;

        IF item_count = 0 THEN
            DELETE FROM orders WHERE id = o_id;
            CONTINUE;
        END IF;

        UPDATE orders
        SET subtotal_amount = basket_subtotal,
            total_amount = basket_subtotal + ship,
            updated_at = created_ts
        WHERE id = o_id;

        INSERT INTO payments (
            order_id, payment_method, amount, status, transaction_id, paid_at, created_at, gateway_name
        )
        VALUES (
            o_id,
            'VNPAY'::payment_method_enum,
            basket_subtotal + ship,
            'SUCCESS'::payment_status,
            'TXN_DSS39_D' || day_offset || '_O' || o_id,
            created_ts,
            created_ts,
            'VNPAY'
        );

        INSERT INTO order_tracking (order_id, event, updated_by, created_at) VALUES
            (o_id, 'CREATED'::order_tracking_event, buyer, created_ts),
            (o_id, 'PAYMENT_SUCCESS'::order_tracking_event, buyer, created_ts + interval '15 minutes'),
            (o_id, 'CONFIRMED'::order_tracking_event, buyer, created_ts + interval '3 hours'),
            (o_id, 'SHIPPED'::order_tracking_event, buyer, created_ts + interval '10 hours'),
            (o_id, 'DELIVERED'::order_tracking_event, buyer, created_ts + interval '1 day');

        n := n + 1;
    END LOOP;

    IF n = 0 THEN
        RAISE NOTICE 'V39 no matching products — nothing seeded';
        RETURN;
    END IF;

    -- Marker payment on first V39 order (idempotency)
    UPDATE payments
    SET transaction_id = 'TXN_DSS39_MULTI'
    WHERE order_id = (
        SELECT id FROM orders
        WHERE shipping_address = 'DSS seed — gio hang nhieu mon V39'
        ORDER BY id
        LIMIT 1
    )
    AND transaction_id LIKE 'TXN_DSS39_D%';

    RAISE NOTICE 'V39 seeded % multi-item baskets for DSS', n;
END $$;
