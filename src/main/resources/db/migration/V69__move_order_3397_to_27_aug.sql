-- V69: Move live order #3397 onto 27 Aug 2026 daytime (VN wall clock).
-- Placed ~00:11 VN; TIMESTAMP + UTC session previously bucketed it onto the 26th
-- on DSS charts. No-op if the order is missing or already after 07:00 on the 27th.

DO $$
DECLARE
    old_ts TIMESTAMP;
    new_ts TIMESTAMP := TIMESTAMP '2026-08-27 10:11:49';
BEGIN
    SELECT created_at INTO old_ts FROM orders WHERE id = 3397;
    IF old_ts IS NULL THEN
        RETURN;
    END IF;
    IF old_ts >= TIMESTAMP '2026-08-27 07:00:00' THEN
        RETURN;
    END IF;

    UPDATE order_tracking
    SET created_at = new_ts + (created_at - old_ts)
    WHERE order_id = 3397;

    UPDATE payments
    SET created_at = new_ts + (created_at - old_ts),
        paid_at = CASE
            WHEN paid_at IS NULL THEN NULL
            ELSE new_ts + (paid_at - old_ts)
        END
    WHERE order_id = 3397;

    UPDATE orders
    SET created_at = new_ts,
        updated_at = new_ts
    WHERE id = 3397;
END $$;
