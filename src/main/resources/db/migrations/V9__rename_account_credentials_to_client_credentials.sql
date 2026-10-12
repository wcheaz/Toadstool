BEGIN;

ALTER TABLE trading.account_credentials RENAME TO client_credentials;
ALTER TABLE trading.client_credentials RENAME COLUMN account_id TO client_id;

ALTER TABLE trading.client_credentials
    DROP CONSTRAINT IF EXISTS account_credentials_account_id_fkey;

UPDATE trading.client_credentials cc
SET client_id = a.client_id
FROM trading.accounts a
WHERE cc.client_id = a.account_id;

ALTER TABLE trading.client_credentials
    ADD CONSTRAINT client_credentials_client_id_fkey
        FOREIGN KEY (client_id) REFERENCES trading.clients(client_id) ON DELETE CASCADE;

ALTER INDEX IF EXISTS trading.ix_account_credentials_account RENAME TO ix_client_credentials_client;
ALTER INDEX IF EXISTS trading.ix_account_credentials_username RENAME TO ix_client_credentials_username;

COMMIT;
