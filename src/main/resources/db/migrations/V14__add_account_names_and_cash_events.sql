ALTER TABLE trading.accounts
    ADD COLUMN IF NOT EXISTS account_name varchar(80) NOT NULL DEFAULT 'Main Account';

CREATE TABLE IF NOT EXISTS trading.cash_events (
    cash_event_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id uuid NOT NULL REFERENCES trading.accounts(account_id),
    order_id uuid REFERENCES trading.orders(order_id),
    event_type varchar(20) NOT NULL CHECK (event_type IN ('DEPOSIT', 'WITHDRAWAL', 'BUY', 'SELL')),
    amount numeric(19, 2) NOT NULL CHECK (amount <> 0),
    occurred_at timestamptz NOT NULL DEFAULT now(),
    details jsonb NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX ix_cash_events_account_occurred
    ON trading.cash_events (account_id, occurred_at DESC);

CREATE OR REPLACE FUNCTION trading.prevent_cash_event_mutation()
RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'cash_events are immutable';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS cash_events_immutable ON trading.cash_events;

CREATE TRIGGER cash_events_immutable
    BEFORE UPDATE OR DELETE ON trading.cash_events
    FOR EACH ROW EXECUTE FUNCTION trading.prevent_cash_event_mutation();

REVOKE UPDATE, DELETE ON trading.cash_events FROM PUBLIC;