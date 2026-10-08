BEGIN;

CREATE TABLE IF NOT EXISTS trading.account_credentials (
    credential_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id uuid NOT NULL REFERENCES trading.accounts(account_id) ON DELETE CASCADE,
    username varchar(80) NOT NULL UNIQUE,
    password_hash varchar(100) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    last_login_at timestamptz
);

CREATE TABLE IF NOT EXISTS trading.auth_sessions (
    session_key_id uuid PRIMARY KEY,
    credential_id uuid NOT NULL REFERENCES trading.account_credentials(credential_id) ON DELETE CASCADE,
    account_id uuid NOT NULL REFERENCES trading.accounts(account_id) ON DELETE CASCADE,
    refresh_token_hash varchar(128) NOT NULL UNIQUE,
    issued_at timestamptz NOT NULL DEFAULT now(),
    access_token_expires_at timestamptz NOT NULL,
    refresh_token_expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    last_used_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_account_credentials_account
    ON trading.account_credentials (account_id);

CREATE INDEX IF NOT EXISTS ix_account_credentials_username
    ON trading.account_credentials (username);

CREATE INDEX IF NOT EXISTS ix_auth_sessions_account
    ON trading.auth_sessions (account_id, revoked_at);

CREATE INDEX IF NOT EXISTS ix_auth_sessions_refresh_hash
    ON trading.auth_sessions (refresh_token_hash);

CREATE INDEX IF NOT EXISTS ix_auth_sessions_active
    ON trading.auth_sessions (session_key_id, revoked_at, access_token_expires_at);

COMMIT;
