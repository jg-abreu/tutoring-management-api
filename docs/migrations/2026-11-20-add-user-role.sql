-- #12: user roles. Brings an existing database (Neon) to the 01-schema.sql of v2.0.
-- New databases created from docker/postgres/init/ already have this; run it only on existing ones.
-- Safe to run more than once.

BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_role') THEN
        CREATE TYPE user_role AS ENUM ('ADMIN', 'USER');
    END IF;
END $$;

ALTER TABLE users ADD COLUMN IF NOT EXISTS role user_role NOT NULL DEFAULT 'USER';

UPDATE users SET role = 'ADMIN' WHERE id = '2c53ba13-53cc-4c79-a4f2-31cfbcceb898';

COMMIT;
