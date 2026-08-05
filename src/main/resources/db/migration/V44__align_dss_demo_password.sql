-- V44: Align DSS demo passwords with the shared backend demo password (12345678).
-- Spring-compatible BCrypt ($2b$). Idempotent UPDATE by email pattern.

UPDATE users
SET password = '$2b$10$gFbkZBWE1FRG/GZuQ/MI8e9OPSQ10K4YW3WRPuHnBhB8yELbcucpi',
    updated_at = CURRENT_TIMESTAMP
WHERE email = 'seller.dss.demo@example.com'
   OR email LIKE 'seller.dss.demo.%@example.com'
   OR email LIKE 'customer.dss.demo.%@example.com';
