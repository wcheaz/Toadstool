BEGIN;

CREATE UNIQUE INDEX IF NOT EXISTS ux_client_credentials_username_lower
    ON trading.client_credentials (LOWER(username));

COMMIT;
