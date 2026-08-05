-- V45: Restore teammate DSS demo credentials (seller.dss.demo@example.com / password).
-- V44 incorrectly overwrote their account password to the shared 12345678 demo secret.
-- Hash is Spring BCrypt-compatible for plaintext "password".

UPDATE users
SET password = '$2a$10$MDes8qRTuKmeopk7NxNZv.gZV5kBFMP7cQ2SlVMMfXT6aXqqHnukK',
    updated_at = CURRENT_TIMESTAMP
WHERE email = 'seller.dss.demo@example.com'
   OR email LIKE 'seller.dss.demo.%@example.com'
   OR email LIKE 'customer.dss.demo.%@example.com';
