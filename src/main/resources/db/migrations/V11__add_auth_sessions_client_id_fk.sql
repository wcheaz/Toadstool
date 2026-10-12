BEGIN;

ALTER TABLE trading.auth_sessions
    DROP CONSTRAINT IF EXISTS auth_sessions_client_id_fkey;

ALTER TABLE trading.auth_sessions
    ADD CONSTRAINT auth_sessions_client_id_fkey
        FOREIGN KEY (client_id) REFERENCES trading.clients(client_id) ON DELETE CASCADE;

COMMIT;
