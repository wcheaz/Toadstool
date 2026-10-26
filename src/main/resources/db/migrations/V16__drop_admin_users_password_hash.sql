BEGIN;

-- Admin passwords now live in trading.admin_credentials (V15).
ALTER TABLE trading.admin_users
    DROP COLUMN IF EXISTS password_hash;

COMMIT;
