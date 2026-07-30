-- V34: restore COD (pay on delivery / tại chỗ) for payment_method_enum
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_enum e
        JOIN pg_type t ON t.oid = e.enumtypid
        WHERE t.typname = 'payment_method_enum'
          AND e.enumlabel = 'COD'
    ) THEN
        ALTER TYPE payment_method_enum ADD VALUE 'COD';
    END IF;
END $$;
