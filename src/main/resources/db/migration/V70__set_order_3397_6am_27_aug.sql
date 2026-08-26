-- V70: Set order #3397 to 06:00 on 27 Aug 2026 (VN wall clock).
-- Follow-up to V69 (10:11). No-op if the order is missing or already at 06:00.

DO $$
DECLARE
    old_ts TIMESTAMP;
    new_ts TIMESTAMP := TIMESTAMP '2026-08-27 06:00:00';
BEGIN
    SELECT created_at INTO old_ts FROM orders WHERE id = 3397;
    IF old_ts IS NULL THEN
        RETURN;
    END IF;
    IF old_ts = new_ts THEN
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
