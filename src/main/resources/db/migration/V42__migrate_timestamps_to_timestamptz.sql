-- V42: Convert TIMESTAMP → TIMESTAMPTZ for OffsetDateTime entities (develop #36)
-- Does NOT rewrite V1–V22 (already applied on Railway) — additive only.

DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT c.table_name, c.column_name
        FROM information_schema.columns c
        JOIN information_schema.tables t
          ON t.table_schema = c.table_schema
         AND t.table_name = c.table_name
        WHERE c.table_schema = 'public'
          AND t.table_type = 'BASE TABLE'
          AND c.data_type = 'timestamp without time zone'
          AND c.column_name IN (
              'created_at', 'updated_at', 'deleted_at',
              'paid_at', 'changed_at', 'expires_at', 'verified_at',
              'sent_at', 'used_at'
          )
    LOOP
        EXECUTE format(
            'ALTER TABLE %I ALTER COLUMN %I TYPE TIMESTAMPTZ USING %I AT TIME ZONE ''Asia/Ho_Chi_Minh''',
            r.table_name,
            r.column_name,
            r.column_name
        );
    END LOOP;
END $$;
