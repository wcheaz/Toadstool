BEGIN;

CREATE SCHEMA IF NOT EXISTS trading; -- OLTP: normalized operational tables

-- Registered clients; the root identity all trading activity hangs off of
CREATE TABLE IF NOT EXISTS trading.clients (
    client_id uuid PRIMARY KEY DEFAULT gen_random_uuid(), -- Unique identifier for the client
    email varchar(320) NOT NULL UNIQUE, -- Client's email address
    display_name varchar(120) NOT NULL, -- Client's display name
    status varchar(20) NOT NULL CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED')), -- Current client status
    created_at timestamptz NOT NULL DEFAULT now(), -- Record creation timestamp
    updated_at timestamptz NOT NULL DEFAULT now() -- Record last update timestamp
);

-- Trading accounts owned by clients; one per client for now
CREATE TABLE IF NOT EXISTS trading.accounts (
    account_id uuid PRIMARY KEY DEFAULT gen_random_uuid(), -- Unique account identifier
    client_id uuid NOT NULL REFERENCES trading.clients(client_id), -- Reference to account owner
    status varchar(20) NOT NULL CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED')), -- Current account status
    opened_at timestamptz NOT NULL DEFAULT now(), -- Account opening timestamp
    UNIQUE (account_id, client_id)
);

-- Tradable instruments; minimal reference data until pricing/asset-class rules exist
CREATE TABLE IF NOT EXISTS trading.instruments (
    instrument_id uuid PRIMARY KEY DEFAULT gen_random_uuid(), -- Unique instrument identifier
    symbol varchar(40) NOT NULL UNIQUE, -- Trading symbol
    name varchar(160) NOT NULL, -- Full instrument name
    status varchar(20) NOT NULL CHECK (status IN ('TRADABLE', 'HALTED', 'INACTIVE')) -- Trading status
);

-- Client orders; the central record of trading intent
CREATE TABLE IF NOT EXISTS trading.orders (
    order_id uuid PRIMARY KEY DEFAULT gen_random_uuid(), -- Unique order identifier
    client_id uuid NOT NULL REFERENCES trading.clients(client_id), -- Reference to client placing order
    account_id uuid NOT NULL, -- Reference to trading account
    instrument_id uuid NOT NULL REFERENCES trading.instruments(instrument_id), -- Reference to instrument
    side varchar(4) NOT NULL CHECK (side IN ('BUY', 'SELL')), -- Order side (buy or sell)
    quantity numeric(28,10) NOT NULL CHECK (quantity > 0), -- Order quantity
    idempotency_key varchar(80) NOT NULL, -- Idempotency key for duplicate detection
    status varchar(20) NOT NULL DEFAULT 'SUBMITTED' CHECK (status IN ('SUBMITTED', 'ACCEPTED', 'REJECTED', 'FILLED')), -- Current order status
    submitted_at timestamptz NOT NULL DEFAULT now(), -- Order submission timestamp
    UNIQUE (client_id, idempotency_key), -- rejects a duplicate submission outright (BR-06/BR-09)
    FOREIGN KEY (account_id, client_id) REFERENCES trading.accounts(account_id, client_id)
);

-- Append-only audit trail of every order/pricing/cash change, attributable to client and time (BR-14/BR-15)
CREATE TABLE IF NOT EXISTS trading.audit_events (
    audit_event_id uuid PRIMARY KEY DEFAULT gen_random_uuid(), -- Unique audit event identifier
    client_id uuid REFERENCES trading.clients(client_id), -- Associated client if applicable
    entity_type varchar(40) NOT NULL, -- Type of entity affected (e.g. ORDER)
    entity_id uuid NOT NULL, -- Identifier of affected entity
    action varchar(80) NOT NULL, -- Action performed
    occurred_at timestamptz NOT NULL DEFAULT now(), -- Event occurrence timestamp
    details jsonb NOT NULL DEFAULT '{}'::jsonb -- Additional event details
);

CREATE INDEX IF NOT EXISTS ix_accounts_client
    ON trading.accounts (client_id); -- speeds up looking up a client's accounts
CREATE INDEX IF NOT EXISTS ix_orders_client_submitted
    ON trading.orders (client_id, submitted_at DESC); -- fetch a client's order history in recency order
CREATE INDEX IF NOT EXISTS ix_audit_events_entity
    ON trading.audit_events (entity_type, entity_id, occurred_at); -- audit trail lookup for a specific entity

-- Audit rows must never be changed or removed once written (BR-14)
REVOKE UPDATE, DELETE ON trading.audit_events FROM PUBLIC;

COMMIT;
