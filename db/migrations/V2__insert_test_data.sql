-- Test data insertion script
-- Populates the database with realistic test data for development and testing
-- Includes: clients, accounts, instruments, orders, fills, admin users, and analytics data

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
-- This uses a simpler approach to reliably generate orders across clients
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
INSERT INTO trading.orders (account_id, instrument_id, side, quantity, idempotency_key, submitted_at) VALUES
-- Large BUY order for Apple
((SELECT account_id FROM trading.accounts LIMIT 1), (SELECT instrument_id FROM trading.instruments WHERE symbol = 'AAPL'), 'BUY', 1000, 'TEST-BUY-AAPL-1000', now() - interval '5 days'),
-- Large SELL order for Microsoft
((SELECT account_id FROM trading.accounts LIMIT 1 OFFSET 1), (SELECT instrument_id FROM trading.instruments WHERE symbol = 'MSFT'), 'SELL', 500, 'TEST-SELL-MSFT-500', now() - interval '3 days'),
-- Crypto BUY order for Bitcoin
((SELECT account_id FROM trading.accounts LIMIT 1 OFFSET 2), (SELECT instrument_id FROM trading.instruments WHERE symbol = 'BTC/USD'), 'BUY', 0.5, 'TEST-BUY-BTC-0.5', now() - interval '2 days'),
-- FX order for EUR/USD
((SELECT account_id FROM trading.accounts LIMIT 1 OFFSET 3), (SELECT instrument_id FROM trading.instruments WHERE symbol = 'EUR/USD'), 'BUY', 100000, 'TEST-BUY-EUR-100K', now() - interval '1 day'),
-- Small order for ETF
((SELECT account_id FROM trading.accounts LIMIT 1 OFFSET 4), (SELECT instrument_id FROM trading.instruments WHERE symbol = 'SPY'), 'BUY', 10, 'TEST-BUY-SPY-10', now() - interval '12 hours');

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
