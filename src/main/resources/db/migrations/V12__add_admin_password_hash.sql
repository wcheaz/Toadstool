BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE trading.admin_users
    ADD COLUMN IF NOT EXISTS password_hash varchar(100);

UPDATE trading.admin_users
SET password_hash = crypt('Admin123!', gen_salt('bf', 10))
WHERE password_hash IS NULL;

ALTER TABLE trading.admin_users
    ALTER COLUMN password_hash SET DEFAULT crypt('Admin123!', gen_salt('bf', 10));

ALTER TABLE trading.admin_users
    ALTER COLUMN password_hash SET NOT NULL;

COMMIT;