-- Read-only Looker Studio user for Railway production (schema reporting only).
-- Manual equivalent: scripts/looker_isp_reader_railway.sql (database name railway).
-- Idempotent: safe to re-run on redeploy.

DO $$
DECLARE
    dbname text := current_database();
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'looker_isp_reader_railway') THEN
        CREATE USER looker_isp_reader_railway WITH PASSWORD 'sedsp';
    ELSE
        ALTER USER looker_isp_reader_railway WITH PASSWORD 'sedsp';
    END IF;

    EXECUTE format('GRANT CONNECT ON DATABASE %I TO looker_isp_reader_railway', dbname);
END
$$;

GRANT USAGE ON SCHEMA reporting TO looker_isp_reader_railway;

GRANT SELECT ON ALL TABLES IN SCHEMA reporting TO looker_isp_reader_railway;

ALTER DEFAULT PRIVILEGES IN SCHEMA reporting
    GRANT SELECT ON TABLES TO looker_isp_reader_railway;
