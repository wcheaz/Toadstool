BEGIN;

-- Drop and recreate on every run so the schema is still easy to reset while it's changing daily;
-- revisit once Flyway (W1-4) takes over versioned migrations instead of a single re-run script.
DROP SCHEMA IF EXISTS trading CASCADE;
DROP SCHEMA IF EXISTS analytics CASCADE;

CREATE SCHEMA IF NOT EXISTS trading; -- OLTP: normalized operational tables
CREATE SCHEMA IF NOT EXISTS analytics; -- OLAP skeleton for reporting; populated from Week 4 onward

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
    asset_class varchar(16) NOT NULL CHECK (asset_class IN ('EQUITY', 'FX', 'CRYPTO')), -- Type of financial instrument
    status varchar(20) NOT NULL CHECK (status IN ('TRADABLE', 'HALTED', 'INACTIVE')) -- Trading status
);

-- Client orders; the central record of trading intent (immutable once created)
CREATE TABLE IF NOT EXISTS trading.orders (
    order_id uuid PRIMARY KEY DEFAULT gen_random_uuid(), -- Unique order identifier
    account_id uuid NOT NULL, -- Reference to trading account
    instrument_id uuid NOT NULL REFERENCES trading.instruments(instrument_id), -- Reference to instrument
    side varchar(4) NOT NULL CHECK (side IN ('BUY', 'SELL')), -- Order side (buy or sell)
    quantity numeric(28,10) NOT NULL CHECK (quantity > 0), -- Order quantity
    idempotency_key varchar(80) NOT NULL, -- Idempotency key for duplicate detection
    submitted_at timestamptz NOT NULL DEFAULT now(), -- Order submission timestamp
    UNIQUE (account_id, idempotency_key), -- rejects a duplicate submission outright (BR-06/BR-09)
    FOREIGN KEY (account_id) REFERENCES trading.accounts(account_id)
);

-- Internal users (David/Priya personas) for the admin/reporting dashboard, distinct from clients (W5-4)
CREATE TABLE IF NOT EXISTS trading.admin_users (
    admin_user_id uuid PRIMARY KEY DEFAULT gen_random_uuid(), -- Unique admin user identifier
    email varchar(320) NOT NULL UNIQUE, -- Admin user's email address
    display_name varchar(120) NOT NULL, -- Admin user's display name
    role varchar(20) NOT NULL CHECK (role IN ('ADMIN', 'ANALYST')), -- Internal role
    status varchar(20) NOT NULL CHECK (status IN ('ACTIVE', 'SUSPENDED')), -- Current admin user status
    created_at timestamptz NOT NULL DEFAULT now(), -- Record creation timestamp
    updated_at timestamptz NOT NULL DEFAULT now() -- Record last update timestamp
);

-- Executed trades; the price a client bought/sold at, for later up/down comparison against a quote.
-- Insert-only: a row is written once, when execution completes; the request itself is a trade_events row.
-- Status tracks the outcome: Filled (successful execution), Failed (execution failed), or Pending (awaiting fill).
CREATE TABLE IF NOT EXISTS trading.fills (
    fill_id uuid PRIMARY KEY DEFAULT gen_random_uuid(), -- Unique fill identifier
    order_id uuid NOT NULL REFERENCES trading.orders(order_id), -- Reference to order
    price numeric(28,10) NOT NULL CHECK (price >= 0), -- Execution price
    quantity numeric(28,10) NOT NULL CHECK (quantity > 0), -- Filled quantity
    status varchar(20) NOT NULL CHECK (status IN ('Filled', 'Failed', 'Pending')), -- Fill status: Filled/Failed/Pending
    executed_at timestamptz NOT NULL DEFAULT now() -- Execution timestamp
);

-- Append-only trail of order/fill activity, attributable to client and time (BR-14/BR-15);
-- covers order status transitions and both halves of a fill's lifecycle
-- (action='FILL_REQUESTED' when pricing starts, action='FILL_EXECUTED' once trading.fills is written)
CREATE TABLE IF NOT EXISTS trading.trade_events (
    trade_event_id uuid PRIMARY KEY DEFAULT gen_random_uuid(), -- Unique event identifier
    client_id uuid REFERENCES trading.clients(client_id), -- Associated client if applicable
    entity_type varchar(40) NOT NULL CHECK (entity_type IN ('ORDER', 'FILL')), -- Type of entity affected
    entity_id uuid NOT NULL, -- Identifier of affected entity (order_id or fill_id)
    action varchar(80) NOT NULL, -- Action performed, e.g. SUBMITTED, ACCEPTED, REJECTED, FILL_REQUESTED, FILL_EXECUTED
    occurred_at timestamptz NOT NULL DEFAULT now(), -- Event occurrence timestamp
    details jsonb NOT NULL DEFAULT '{}'::jsonb -- Additional event details
);

-- Append-only trail of every login attempt (success or failure), for security review
CREATE TABLE IF NOT EXISTS trading.login_events (
    login_event_id uuid PRIMARY KEY DEFAULT gen_random_uuid(), -- Unique login event identifier
    client_id uuid REFERENCES trading.clients(client_id), -- Matched client, if the attempted email resolved to one
    email_attempted varchar(320) NOT NULL, -- Email address used in the attempt
    outcome varchar(10) NOT NULL CHECK (outcome IN ('SUCCESS', 'FAILURE')), -- Attempt outcome
    occurred_at timestamptz NOT NULL DEFAULT now(), -- Event occurrence timestamp
    details jsonb NOT NULL DEFAULT '{}'::jsonb -- Additional event details, e.g. failure reason
);

-- Daily trading activity per instrument, summed across every client; read by the admin dashboard
-- instead of live trading tables (BR-16)
CREATE TABLE IF NOT EXISTS analytics.fact_daily_instrument_activity (
    activity_date date NOT NULL, -- Day the activity occurred
    instrument_id uuid NOT NULL REFERENCES trading.instruments(instrument_id), -- Reference to instrument
    order_count integer NOT NULL DEFAULT 0 CHECK (order_count >= 0), -- Orders submitted that day, across all clients
    filled_quantity numeric(28,10) NOT NULL DEFAULT 0 CHECK (filled_quantity >= 0), -- Total quantity filled that day
    gross_amount numeric(28,10) NOT NULL DEFAULT 0 CHECK (gross_amount >= 0), -- Total price * quantity filled that day
    updated_at timestamptz NOT NULL DEFAULT now(), -- Last rollup update timestamp
    PRIMARY KEY (activity_date, instrument_id)
);

-- Daily platform-wide trading activity, summed across every client and instrument; read by the
-- admin dashboard for business insights (BR-16/BR-17)
CREATE TABLE IF NOT EXISTS analytics.fact_daily_platform_activity (
    activity_date date PRIMARY KEY, -- Day the activity occurred
    order_count integer NOT NULL DEFAULT 0 CHECK (order_count >= 0), -- Orders submitted that day, across all clients
    filled_quantity numeric(28,10) NOT NULL DEFAULT 0 CHECK (filled_quantity >= 0), -- Total quantity filled that day
    gross_amount numeric(28,10) NOT NULL DEFAULT 0 CHECK (gross_amount >= 0), -- Total price * quantity filled that day
    updated_at timestamptz NOT NULL DEFAULT now() -- Last rollup update timestamp
);

CREATE INDEX IF NOT EXISTS ix_accounts_client
    ON trading.accounts (client_id); -- speeds up looking up a client's accounts

CREATE INDEX IF NOT EXISTS ix_fills_order
    ON trading.fills (order_id, executed_at); -- list an order's fills in execution order
CREATE INDEX IF NOT EXISTS ix_trade_events_entity
    ON trading.trade_events (entity_type, entity_id, occurred_at); -- audit trail lookup for a specific order or fill
CREATE INDEX IF NOT EXISTS ix_login_events_client
    ON trading.login_events (client_id, occurred_at); -- a client's login history in recency order
CREATE INDEX IF NOT EXISTS ix_fact_daily_instrument_activity_instrument
    ON analytics.fact_daily_instrument_activity (instrument_id, activity_date); -- an instrument's activity history over time

-- Audit rows must never be changed or removed once written (BR-14)
-- Orders are also immutable once submitted; only fills track status changes
REVOKE UPDATE, DELETE ON trading.orders FROM PUBLIC;
REVOKE UPDATE, DELETE ON trading.fills FROM PUBLIC;
REVOKE UPDATE, DELETE ON trading.trade_events FROM PUBLIC;
REVOKE UPDATE, DELETE ON trading.login_events FROM PUBLIC;

COMMIT;
