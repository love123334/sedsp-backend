-- V33: migrate legacy COD/BANK payment rows after VNPAY enum is committed

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_enum e
        JOIN pg_type t ON t.oid = e.enumtypid
        WHERE t.typname = 'payment_method_enum'
          AND e.enumlabel = 'COD'
    ) THEN
        UPDATE payments
        SET payment_method = 'MOMO'::payment_method_enum,
            gateway_name = COALESCE(gateway_name, 'MOMO')
        WHERE payment_method::text = 'COD';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM pg_enum e
        JOIN pg_type t ON t.oid = e.enumtypid
        WHERE t.typname = 'payment_method_enum'
          AND e.enumlabel = 'BANK'
    ) THEN
        UPDATE payments
        SET payment_method = 'VNPAY'::payment_method_enum,
            gateway_name = COALESCE(gateway_name, 'VNPAY')
        WHERE payment_method::text = 'BANK';
    END IF;
END $$;

UPDATE payments
SET gateway_name = payment_method::text
WHERE gateway_name IS NULL;
