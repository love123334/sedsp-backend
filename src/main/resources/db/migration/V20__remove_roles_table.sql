-- =====================================================
-- V20__remove_roles_table.sql
-- Remove legacy role system
-- =====================================================

-- =====================================================
-- 1. REMOVE FK users -> roles
-- =====================================================

ALTER TABLE users
DROP CONSTRAINT IF EXISTS users_role_id_fkey;

-- =====================================================
-- 2. DROP role_id COLUMN
-- =====================================================

ALTER TABLE users
DROP COLUMN IF EXISTS role_id;

-- =====================================================
-- 3. DROP INDEX (if exists)
-- =====================================================

DROP INDEX IF EXISTS idx_users_role;

-- =====================================================
-- 4. DROP ROLES TABLE
-- =====================================================

DROP TABLE IF EXISTS roles CASCADE;
