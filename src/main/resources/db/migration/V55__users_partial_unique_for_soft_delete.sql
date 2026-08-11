-- Soft-delete: email/username/phone chỉ unique trên user còn active
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_username_key;
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_email_key;
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_phone_key;

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_username_active
    ON users (username)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_email_active
    ON users (email)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_phone_active
    ON users (phone)
    WHERE deleted_at IS NULL AND phone IS NOT NULL;
