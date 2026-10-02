-- Add holdings table to cache current quantity per instrument per account
-- Holdings are derived from filled trades but cached for performance on portfolio reads
-- This migration is deterministic: no random() functions, quantities computed from filled trade data

BEGIN;

-- Create holdings table to cache current positions per instrument per account
-- Mutable table (unlike append-only trade_events): updated whenever a fill completes
CREATE TABLE IF NOT EXISTS trading.holdings (
    holding_id uuid PRIMARY KEY DEFAULT gen_random_uuid(), -- Unique holding identifier
    account_id uuid NOT NULL REFERENCES trading.accounts(account_id), -- Reference to owning account
    instrument_id uuid NOT NULL REFERENCES trading.instruments(instrument_id), -- Reference to instrument
    quantity numeric(28,10) NOT NULL DEFAULT 0 CHECK (quantity >= 0), -- Current quantity held (can be 0 if liquidated)
    updated_at timestamptz NOT NULL DEFAULT now(), -- Last update timestamp
    UNIQUE (account_id, instrument_id) -- One row per account-instrument pair
);

-- Populate holdings from filled trades: for each account-instrument pair, sum all filled quantities
-- For BUY orders: add to quantity; for SELL orders: subtract from quantity
-- Only include 'Filled' status fills for deterministic population
INSERT INTO trading.holdings (account_id, instrument_id, quantity, updated_at)
SELECT
  a.account_id,
  o.instrument_id,
  COALESCE(
    SUM(
      CASE
        WHEN o.side = 'BUY' THEN f.quantity
        WHEN o.side = 'SELL' THEN -f.quantity
        ELSE 0
      END
    ),
    0
  )::numeric(28,10) as quantity,
  CURRENT_TIMESTAMP as updated_at
FROM trading.accounts a
CROSS JOIN trading.instruments i
LEFT JOIN trading.orders o ON a.account_id = o.account_id AND i.instrument_id = o.instrument_id
LEFT JOIN trading.fills f ON o.order_id = f.order_id AND f.status = 'Filled'
GROUP BY a.account_id, i.instrument_id
-- Only insert rows for account-instrument pairs that have some activity or should start at 0
ON CONFLICT (account_id, instrument_id) DO NOTHING;

-- Index for efficient lookups by account (e.g., user portfolio view)
CREATE INDEX IF NOT EXISTS ix_holdings_account
    ON trading.holdings (account_id);

-- Index for efficient lookups by instrument (e.g., which accounts hold this instrument)
CREATE INDEX IF NOT EXISTS ix_holdings_instrument
    ON trading.holdings (instrument_id);

-- Index for efficient lookups by account and instrument (primary access path)
CREATE INDEX IF NOT EXISTS ix_holdings_account_instrument
    ON trading.holdings (account_id, instrument_id);

COMMIT;
