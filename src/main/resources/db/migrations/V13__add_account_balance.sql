ALTER TABLE trading.accounts
    ADD COLUMN IF NOT EXISTS balance numeric(19, 2) NOT NULL DEFAULT 0.00;