-- Seller MoMo QR transfer (manual confirm) — separate from MoMo gateway

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_enum e
        JOIN pg_type t ON t.oid = e.enumtypid
        WHERE t.typname = 'payment_method_enum'
          AND e.enumlabel = 'MOMO_QR'
    ) THEN
        ALTER TYPE payment_method_enum ADD VALUE 'MOMO_QR';
    END IF;
END $$;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS momo_phone VARCHAR(20),
    ADD COLUMN IF NOT EXISTS momo_qr_url TEXT;

ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS transfer_note VARCHAR(120);
