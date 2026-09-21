-- Complete Database Restart Script (SQL-only version)
-- This script can be run directly with: psql -U postgres -d Toadstool -f restart_database.sql
-- It will drop all data and recreate the schema with test data in one transaction

-- Drop existing schemas
DROP SCHEMA IF EXISTS trading CASCADE;
DROP SCHEMA IF EXISTS analytics CASCADE;

-- Recreate schema from V1__init.sql
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

-- ============================================================================
-- TEST DATA INSERTION
-- ============================================================================

BEGIN;

-- Insert test instruments first (referenced by orders)
INSERT INTO trading.instruments (symbol, name, asset_class, status) VALUES
('AAPL', 'Apple Inc.', 'EQUITY', 'TRADABLE'),
('GOOGL', 'Alphabet Inc.', 'EQUITY', 'TRADABLE'),
('MSFT', 'Microsoft Corporation', 'EQUITY', 'TRADABLE'),
('AMZN', 'Amazon.com Inc.', 'EQUITY', 'TRADABLE'),
('TSLA', 'Tesla Inc.', 'EQUITY', 'TRADABLE'),
('META', 'Meta Platforms Inc.', 'EQUITY', 'TRADABLE'),
('NVDA', 'NVIDIA Corporation', 'EQUITY', 'TRADABLE'),
('JPM', 'JPMorgan Chase & Co.', 'EQUITY', 'TRADABLE'),
('GLD', 'SPDR Gold Shares', 'EQUITY', 'TRADABLE'),
('SPY', 'SPDR S&P 500 ETF Trust', 'EQUITY', 'TRADABLE'),
('BTC/USD', 'Bitcoin to US Dollar', 'CRYPTO', 'TRADABLE'),
('ETH/USD', 'Ethereum to US Dollar', 'CRYPTO', 'TRADABLE'),
('XRP/USD', 'XRP to US Dollar', 'CRYPTO', 'TRADABLE'),
('EUR/USD', 'Euro to US Dollar', 'FX', 'TRADABLE'),
('GBP/USD', 'British Pound to US Dollar', 'FX', 'TRADABLE'),
('JPY/USD', 'Japanese Yen to US Dollar', 'FX', 'TRADABLE');

-- Create 25 test clients with accounts
WITH client_data AS (
  INSERT INTO trading.clients (email, display_name, status) VALUES
  ('alice.johnson@example.com', 'Alice Johnson', 'ACTIVE'),
  ('bob.smith@example.com', 'Bob Smith', 'ACTIVE'),
  ('carol.white@example.com', 'Carol White', 'ACTIVE'),
  ('david.brown@example.com', 'David Brown', 'ACTIVE'),
  ('emma.davis@example.com', 'Emma Davis', 'ACTIVE'),
  ('frank.miller@example.com', 'Frank Miller', 'ACTIVE'),
  ('grace.wilson@example.com', 'Grace Wilson', 'ACTIVE'),
  ('henry.moore@example.com', 'Henry Moore', 'ACTIVE'),
  ('isabelle.taylor@example.com', 'Isabelle Taylor', 'ACTIVE'),
  ('jack.anderson@example.com', 'Jack Anderson', 'ACTIVE'),
  ('kate.thomas@example.com', 'Kate Thomas', 'ACTIVE'),
  ('leo.jackson@example.com', 'Leo Jackson', 'ACTIVE'),
  ('mia.martin@example.com', 'Mia Martin', 'ACTIVE'),
  ('noah.perez@example.com', 'Noah Perez', 'ACTIVE'),
  ('olivia.garcia@example.com', 'Olivia Garcia', 'ACTIVE'),
  ('paul.robinson@example.com', 'Paul Robinson', 'ACTIVE'),
  ('quinn.clark@example.com', 'Quinn Clark', 'ACTIVE'),
  ('rachel.rodriguez@example.com', 'Rachel Rodriguez', 'ACTIVE'),
  ('sam.lewis@example.com', 'Sam Lewis', 'ACTIVE'),
  ('tina.lee@example.com', 'Tina Lee', 'ACTIVE'),
  ('ursula.walker@example.com', 'Ursula Walker', 'ACTIVE'),
  ('victor.hall@example.com', 'Victor Hall', 'ACTIVE'),
  ('wendy.allen@example.com', 'Wendy Allen', 'ACTIVE'),
  ('xander.young@example.com', 'Xander Young', 'ACTIVE'),
  ('yara.king@example.com', 'Yara King', 'ACTIVE')
  RETURNING client_id
)
INSERT INTO trading.accounts (client_id, status)
SELECT client_id, 'ACTIVE' FROM client_data;

-- Create 120 orders and associated fills to generate realistic trade data
INSERT INTO trading.orders (account_id, instrument_id, side, quantity, idempotency_key, submitted_at)
SELECT
  a.account_id,
  i.instrument_id,
  CASE WHEN random() < 0.5 THEN 'BUY' ELSE 'SELL' END as side,
  (random() * 10000 + 10)::numeric(28,10) as quantity,
  'IDEM-' || gen_random_uuid()::text as idempotency_key,
  now() - (random() * interval '30 days') as submitted_at
FROM (
  SELECT account_id FROM trading.accounts, generate_series(1, 5)
  LIMIT 120
) AS account_expansion(account_id)
JOIN trading.accounts a ON account_expansion.account_id = a.account_id
CROSS JOIN LATERAL (SELECT instrument_id FROM trading.instruments ORDER BY random() LIMIT 1) i;

-- Add specific test orders with known properties for testing
-- Explicitly get alice.johnson's account to ensure proper linkage for tests
INSERT INTO trading.orders (account_id, instrument_id, side, quantity, idempotency_key, submitted_at) VALUES
-- Large BUY order for Apple - alice.johnson account
((SELECT account_id FROM trading.accounts WHERE client_id = (SELECT client_id FROM trading.clients WHERE email = 'alice.johnson@example.com')), (SELECT instrument_id FROM trading.instruments WHERE symbol = 'AAPL'), 'BUY', 1000, 'TEST-BUY-AAPL-1000', now() - interval '5 days'),
-- Large SELL order for Microsoft - bob.smith account
((SELECT account_id FROM trading.accounts WHERE client_id = (SELECT client_id FROM trading.clients WHERE email = 'bob.smith@example.com')), (SELECT instrument_id FROM trading.instruments WHERE symbol = 'MSFT'), 'SELL', 500, 'TEST-SELL-MSFT-500', now() - interval '3 days'),
-- Crypto BUY order for Bitcoin - carol.white account
((SELECT account_id FROM trading.accounts WHERE client_id = (SELECT client_id FROM trading.clients WHERE email = 'carol.white@example.com')), (SELECT instrument_id FROM trading.instruments WHERE symbol = 'BTC/USD'), 'BUY', 0.5, 'TEST-BUY-BTC-0.5', now() - interval '2 days'),
-- FX order for EUR/USD - alice.johnson account (second order for alice)
((SELECT account_id FROM trading.accounts WHERE client_id = (SELECT client_id FROM trading.clients WHERE email = 'alice.johnson@example.com')), (SELECT instrument_id FROM trading.instruments WHERE symbol = 'EUR/USD'), 'BUY', 100000, 'TEST-BUY-EUR-100K', now() - interval '1 day'),
-- Small order for ETF - alice.johnson account (third order for alice)
((SELECT account_id FROM trading.accounts WHERE client_id = (SELECT client_id FROM trading.clients WHERE email = 'alice.johnson@example.com')), (SELECT instrument_id FROM trading.instruments WHERE symbol = 'SPY'), 'BUY', 10, 'TEST-BUY-SPY-10', now() - interval '12 hours');

-- Create fills for the orders (80% fill rate to have some open orders)
WITH order_subset AS (
  SELECT order_id, random() < 0.8 as should_fill
  FROM trading.orders
)
INSERT INTO trading.fills (order_id, price, quantity, status, executed_at)
SELECT
  o.order_id,
  (random() * 5000 + 10)::numeric(28,10) as price,
  o.quantity * (0.5 + random() * 0.5) as quantity, -- Partial fills possible
  CASE
    WHEN random() < 0.05 THEN 'Failed'
    WHEN random() < 0.10 THEN 'Pending'
    ELSE 'Filled'
  END as status,
  o.submitted_at + (random() * interval '1 hour') as executed_at
FROM trading.orders o
JOIN order_subset os ON o.order_id = os.order_id
WHERE os.should_fill = true;

-- Create corresponding trade_events for audit trail
INSERT INTO trading.trade_events (client_id, entity_type, entity_id, action, occurred_at, details)
SELECT
  a.client_id,
  'ORDER',
  o.order_id,
  'SUBMITTED',
  o.submitted_at,
  jsonb_build_object(
    'side', o.side,
    'quantity', o.quantity,
    'instrument_id', o.instrument_id
  )
FROM trading.orders o
JOIN trading.accounts a ON o.account_id = a.account_id;

-- Create fill events
INSERT INTO trading.trade_events (client_id, entity_type, entity_id, action, occurred_at, details)
SELECT
  a.client_id,
  'FILL',
  f.fill_id,
  CASE f.status
    WHEN 'Filled' THEN 'FILL_EXECUTED'
    WHEN 'Pending' THEN 'FILL_REQUESTED'
    ELSE 'FILL_FAILED'
  END,
  f.executed_at,
  jsonb_build_object(
    'price', f.price,
    'quantity', f.quantity,
    'status', f.status,
    'order_id', f.order_id
  )
FROM trading.fills f
JOIN trading.orders o ON f.order_id = o.order_id
JOIN trading.accounts a ON o.account_id = a.account_id;

-- Add some test login events for security audit trail
INSERT INTO trading.login_events (client_id, email_attempted, outcome, details)
SELECT
  c.client_id,
  c.email,
  CASE WHEN random() < 0.95 THEN 'SUCCESS' ELSE 'FAILURE' END,
  CASE
    WHEN random() < 0.95 THEN '{}'::jsonb
    ELSE jsonb_build_object('failure_reason', 'Invalid password')
  END
FROM trading.clients c
CROSS JOIN generate_series(1, 3) -- 3 login events per client
ORDER BY random();

-- Add admin users (internal staff for dashboard and reporting)
INSERT INTO trading.admin_users (email, display_name, role, status) VALUES
('david.admin@neueda.com', 'David Kumar', 'ADMIN', 'ACTIVE'),
('priya.analyst@neueda.com', 'Priya Sharma', 'ANALYST', 'ACTIVE'),
('john.super@neueda.com', 'John Supervisor', 'ADMIN', 'ACTIVE'),
('sarah.analyst@neueda.com', 'Sarah Johnson', 'ANALYST', 'ACTIVE'),
('michael.admin@neueda.com', 'Michael Chen', 'ADMIN', 'ACTIVE');

-- Populate analytics.fact_daily_instrument_activity - rollup of daily trading by instrument
INSERT INTO analytics.fact_daily_instrument_activity (activity_date, instrument_id, order_count, filled_quantity, gross_amount, updated_at)
SELECT
  date_trunc('day', o.submitted_at)::date as activity_date,
  o.instrument_id,
  COUNT(DISTINCT o.order_id)::integer as order_count,
  COALESCE(SUM(f.quantity), 0)::numeric(28,10) as filled_quantity,
  COALESCE(SUM(f.price * f.quantity), 0)::numeric(28,10) as gross_amount,
  now()::timestamptz as updated_at
FROM trading.orders o
LEFT JOIN trading.fills f ON o.order_id = f.order_id AND f.status = 'Filled'
GROUP BY activity_date, o.instrument_id
ORDER BY activity_date DESC;

-- Populate analytics.fact_daily_platform_activity - rollup of daily trading across entire platform
INSERT INTO analytics.fact_daily_platform_activity (activity_date, order_count, filled_quantity, gross_amount, updated_at)
SELECT
  date_trunc('day', o.submitted_at)::date as activity_date,
  COUNT(DISTINCT o.order_id)::integer as order_count,
  COALESCE(SUM(f.quantity), 0)::numeric(28,10) as filled_quantity,
  COALESCE(SUM(f.price * f.quantity), 0)::numeric(28,10) as gross_amount,
  now()::timestamptz as updated_at
FROM trading.orders o
LEFT JOIN trading.fills f ON o.order_id = f.order_id AND f.status = 'Filled'
GROUP BY activity_date
ORDER BY activity_date DESC;

COMMIT;

-- Verification query
SELECT 
    (SELECT COUNT(*) FROM trading.clients) as client_count,
    (SELECT COUNT(*) FROM trading.accounts) as account_count,
    (SELECT COUNT(*) FROM trading.instruments) as instrument_count,
    (SELECT COUNT(*) FROM trading.orders) as order_count,
    (SELECT COUNT(*) FROM trading.fills) as fill_count,
    (SELECT COUNT(*) FROM trading.trade_events) as trade_event_count,
    (SELECT COUNT(*) FROM trading.login_events) as login_event_count;
