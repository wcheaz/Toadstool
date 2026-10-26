BEGIN;

ALTER TABLE trading.auth_sessions RENAME COLUMN account_id TO client_id;

ALTER TABLE trading.auth_sessions
    DROP CONSTRAINT IF EXISTS auth_sessions_account_id_fkey;

UPDATE trading.auth_sessions s
SET client_id = a.client_id
FROM trading.accounts a
WHERE s.client_id = a.account_id;

ALTER TABLE trading.auth_sessions
    ADD CONSTRAINT auth_sessions_client_id_fkey
        FOREIGN KEY (client_id) REFERENCES trading.clients(client_id) ON DELETE CASCADE;

ALTER INDEX IF EXISTS trading.ix_auth_sessions_account RENAME TO ix_auth_sessions_client;

COMMIT;
