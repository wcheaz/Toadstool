BEGIN;

ALTER TABLE trading.auth_sessions
    ALTER COLUMN refresh_token_hash DROP NOT NULL,
    ALTER COLUMN refresh_token_expires_at DROP NOT NULL;

COMMIT;