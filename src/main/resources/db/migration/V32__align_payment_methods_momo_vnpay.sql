-- V32: additive schema for MoMo/VNPay (safe on already-migrated DBs)

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_enum e
        JOIN pg_type t ON t.oid = e.enumtypid
        WHERE t.typname = 'payment_method_enum'
          AND e.enumlabel = 'VNPAY'
    ) THEN
        ALTER TYPE payment_method_enum ADD VALUE 'VNPAY';
    END IF;
END $$;

ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS gateway_name VARCHAR(50);
