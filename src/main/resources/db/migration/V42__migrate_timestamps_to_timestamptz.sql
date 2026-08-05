-- V42: intentionally NO-OP on Railway.
--
-- develop's timestamptz ALTER cannot run safely against an already-seeded DB:
-- reporting views (V37/V38) depend on orders.created_at / payments.paid_at, so
-- ALTER TYPE aborts the Flyway transaction and drops the JDBC connection
-- (symptoms: "Connection is closed" / unable to restore autocommit).
--
-- Hibernate OffsetDateTime + spring.jpa hibernate.jdbc.time_zone works with
-- existing TIMESTAMP WITHOUT TIME ZONE columns. Keep applied V1–V41 checksums.
SELECT 1;
