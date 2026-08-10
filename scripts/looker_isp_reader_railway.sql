-- Run on Railway Postgres (database: railway) if Flyway V53 has not applied yet.
-- Looker Studio: read-only on schema reporting only.

CREATE USER looker_isp_reader_railway
WITH PASSWORD 'sedsp';

GRANT CONNECT ON DATABASE "railway"
TO looker_isp_reader_railway;

GRANT USAGE ON SCHEMA reporting
TO looker_isp_reader_railway;

GRANT SELECT ON ALL TABLES IN SCHEMA reporting
TO looker_isp_reader_railway;

ALTER DEFAULT PRIVILEGES IN SCHEMA reporting
GRANT SELECT ON TABLES TO looker_isp_reader_railway;
