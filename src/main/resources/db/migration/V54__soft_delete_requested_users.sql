-- Soft-delete accounts requested for removal (capstone cleanup)
UPDATE users
SET deleted_at = NOW(),
    updated_at = NOW()
WHERE deleted_at IS NULL
  AND LOWER(email) IN (
    LOWER('kzorel2408@gmail.com'),
    LOWER('minhpndse180043@fpt.edu.vn')
  );
