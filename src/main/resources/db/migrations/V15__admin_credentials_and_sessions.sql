BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Admin login credentials. Read-only to the application: there is no admin registration,
-- rows are provisioned manually (see the provisioning snippet at the bottom of this file).
CREATE TABLE IF NOT EXISTS trading.admin_credentials (
    credential_id uuid PRIMARY KEY DEFAULT gen_random_uuid(), -- Unique credential identifier
    admin_user_id uuid NOT NULL UNIQUE REFERENCES trading.admin_users(admin_user_id) ON DELETE RESTRICT, -- Owning admin user
    username varchar(320) NOT NULL UNIQUE, -- Login identifier (the admin's email)
    password_hash varchar(100) NOT NULL, -- BCrypt hash
    created_at timestamptz NOT NULL DEFAULT now(), -- Record creation timestamp
    updated_at timestamptz NOT NULL DEFAULT now() -- Record last update timestamp
);

CREATE INDEX IF NOT EXISTS ix_admin_credentials_username
    ON trading.admin_credentials (username);

-- Block every write unless the session explicitly opts in with:
--   SET LOCAL toadstool.admin_provisioning = 'on';
CREATE OR REPLACE FUNCTION trading.admin_credentials_read_only()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF coalesce(current_setting('toadstool.admin_provisioning', true), 'off') <> 'on' THEN
        RAISE EXCEPTION 'trading.admin_credentials is read-only; admins must be provisioned manually'
            USING ERRCODE = 'insufficient_privilege';
    END IF;
    IF TG_OP = 'UPDATE' THEN
        NEW.updated_at := now();
    END IF;
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_admin_credentials_read_only ON trading.admin_credentials;
CREATE TRIGGER trg_admin_credentials_read_only
    BEFORE INSERT OR UPDATE OR DELETE ON trading.admin_credentials
    FOR EACH ROW EXECUTE FUNCTION trading.admin_credentials_read_only();

DROP TRIGGER IF EXISTS trg_admin_credentials_no_truncate ON trading.admin_credentials;
CREATE TRIGGER trg_admin_credentials_no_truncate
    BEFORE TRUNCATE ON trading.admin_credentials
    FOR EACH STATEMENT EXECUTE FUNCTION trading.admin_credentials_read_only();

-- Carry existing admins over with their current password hashes (seeded as Admin123! by V12).
SET LOCAL toadstool.admin_provisioning = 'on';

INSERT INTO trading.admin_credentials (admin_user_id, username, password_hash)
SELECT admin_user_id, email, password_hash
FROM trading.admin_users
ON CONFLICT DO NOTHING;

SET LOCAL toadstool.admin_provisioning = 'off';

-- Admin sessions; mirrors trading.auth_sessions, keyed to admin credentials/users instead of clients.
CREATE TABLE IF NOT EXISTS trading.admin_auth_sessions (
    session_key_id uuid PRIMARY KEY,
    credential_id uuid NOT NULL REFERENCES trading.admin_credentials(credential_id) ON DELETE CASCADE,
    admin_user_id uuid NOT NULL REFERENCES trading.admin_users(admin_user_id) ON DELETE CASCADE,
    refresh_token_hash varchar(128) UNIQUE,
    issued_at timestamptz NOT NULL DEFAULT now(),
    access_token_expires_at timestamptz NOT NULL,
    refresh_token_expires_at timestamptz,
    revoked_at timestamptz,
    last_used_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_admin_auth_sessions_admin_user
    ON trading.admin_auth_sessions (admin_user_id, revoked_at);

CREATE INDEX IF NOT EXISTS ix_admin_auth_sessions_refresh_hash
    ON trading.admin_auth_sessions (refresh_token_hash);

CREATE INDEX IF NOT EXISTS ix_admin_auth_sessions_active
    ON trading.admin_auth_sessions (session_key_id, revoked_at, access_token_expires_at);

COMMIT;

-- Manual admin provisioning (run by hand, not part of the migration):
--
-- BEGIN;
-- SET LOCAL toadstool.admin_provisioning = 'on';
-- INSERT INTO trading.admin_credentials (admin_user_id, username, password_hash)
-- SELECT admin_user_id, email, crypt('<password>', gen_salt('bf', 10))
-- FROM trading.admin_users
-- WHERE email = '<admin email>';
-- COMMIT;
