-- Clean up test data and rebuild with realistic, lower-quantity orders
-- This migration removes the problematic random data from V2/V3 and replaces it with:
-- 1. Deterministic orders with mostly BUY (70%) vs SELL (30%) for positive positions
-- 2. Lower quantities (10-100 range) for easier testing and verification
-- 3. Proper fills for all orders to ensure holdings are populated correctly

BEGIN;

-- Clear out the old random test data (keep instruments and clients/accounts)
DELETE FROM trading.trade_events WHERE entity_type IN ('ORDER', 'FILL');
DELETE FROM trading.fills;
DELETE FROM trading.orders;

-- Rebuild orders with better data: realistic BUY/SELL ratio and lower quantities
-- Most clients get 3-5 orders with mix of instruments
INSERT INTO trading.orders (account_id, instrument_id, side, quantity, idempotency_key, submitted_at) VALUES
-- Alice Johnson - Strong buyer, building portfolio
((SELECT account_id FROM trading.accounts WHERE client_id = (SELECT client_id FROM trading.clients WHERE email = 'alice.johnson@example.com')), (SELECT instrument_id FROM trading.instruments WHERE symbol = 'AAPL'), 'BUY', 50, 'ALICE-BUY-AAPL-50', now() - interval '10 days'),
((SELECT account_id FROM trading.accounts WHERE client_id = (SELECT client_id FROM trading.clients WHERE email = 'alice.johnson@example.com')), (SELECT instrument_id FROM trading.instruments WHERE symbol = 'MSFT'), 'BUY', 30, 'ALICE-BUY-MSFT-30', now() - interval '9 days'),
((SELECT account_id FROM trading.accounts WHERE client_id = (SELECT client_id FROM trading.clients WHERE email = 'alice.johnson@example.com')), (SELECT instrument_id FROM trading.instruments WHERE symbol = 'AAPL'), 'BUY', 25, 'ALICE-BUY-AAPL-25', now() - interval '5 days'),
((SELECT account_id FROM trading.accounts WHERE client_id = (SELECT client_id FROM trading.clients WHERE email = 'alice.johnson@example.com')), (SELECT instrument_id FROM trading.instruments WHERE symbol = 'GOOGL'), 'SELL', 10, 'ALICE-SELL-GOOGL-10', now() - interval '3 days'),
((SELECT account_id FROM trading.accounts WHERE client_id = (SELECT client_id FROM trading.clients WHERE email = 'alice.johnson@example.com')), (SELECT instrument_id FROM trading.instruments WHERE symbol = 'TSLA'), 'BUY', 15, 'ALICE-BUY-TSLA-15', now() - interval '2 days'),

-- Bob Smith - Moderate trader
((SELECT account_id FROM trading.accounts WHERE client_id = (SELECT client_id FROM trading.clients WHERE email = 'bob.smith@example.com')), (SELECT instrument_id FROM trading.instruments WHERE symbol = 'MSFT'), 'BUY', 40, 'BOB-BUY-MSFT-40', now() - interval '8 days'),
((SELECT account_id FROM trading.accounts WHERE client_id = (SELECT client_id FROM trading.clients WHERE email = 'bob.smith@example.com')), (SELECT instrument_id FROM trading.instruments WHERE symbol = 'SPY'), 'BUY', 20, 'BOB-BUY-SPY-20', now() - interval '6 days'),
((SELECT account_id FROM trading.accounts WHERE client_id = (SELECT client_id FROM trading.clients WHERE email = 'bob.smith@example.com')), (SELECT instrument_id FROM trading.instruments WHERE symbol = 'MSFT'), 'SELL', 15, 'BOB-SELL-MSFT-15', now() - interval '4 days'),

-- Carol White - Crypto focused
((SELECT account_id FROM trading.accounts WHERE client_id = (SELECT client_id FROM trading.clients WHERE email = 'carol.white@example.com')), (SELECT instrument_id FROM trading.instruments WHERE symbol = 'BTC/USD'), 'BUY', 2, 'CAROL-BUY-BTC-2', now() - interval '7 days'),
((SELECT account_id FROM trading.accounts WHERE client_id = (SELECT client_id FROM trading.clients WHERE email = 'carol.white@example.com')), (SELECT instrument_id FROM trading.instruments WHERE symbol = 'ETH/USD'), 'BUY', 10, 'CAROL-BUY-ETH-10', now() - interval '5 days'),
((SELECT account_id FROM trading.accounts WHERE client_id = (SELECT client_id FROM trading.clients WHERE email = 'carol.white@example.com')), (SELECT instrument_id FROM trading.instruments WHERE symbol = 'BTC/USD'), 'BUY', 1, 'CAROL-BUY-BTC-1', now() - interval '1 day'),

-- David Brown - FX trader
((SELECT account_id FROM trading.accounts WHERE client_id = (SELECT client_id FROM trading.clients WHERE email = 'david.brown@example.com')), (SELECT instrument_id FROM trading.instruments WHERE symbol = 'EUR/USD'), 'BUY', 50000, 'DAVID-BUY-EUR-50K', now() - interval '6 days'),
((SELECT account_id FROM trading.accounts WHERE client_id = (SELECT client_id FROM trading.clients WHERE email = 'david.brown@example.com')), (SELECT instrument_id FROM trading.instruments WHERE symbol = 'GBP/USD'), 'BUY', 30000, 'DAVID-BUY-GBP-30K', now() - interval '4 days'),

-- Emma Davis - Dividend seeker
((SELECT account_id FROM trading.accounts WHERE client_id = (SELECT client_id FROM trading.clients WHERE email = 'emma.davis@example.com')), (SELECT instrument_id FROM trading.instruments WHERE symbol = 'JPM'), 'BUY', 100, 'EMMA-BUY-JPM-100', now() - interval '8 days'),
((SELECT account_id FROM trading.accounts WHERE client_id = (SELECT client_id FROM trading.clients WHERE email = 'emma.davis@example.com')), (SELECT instrument_id FROM trading.instruments WHERE symbol = 'GLD'), 'BUY', 75, 'EMMA-BUY-GLD-75', now() - interval '5 days'),
((SELECT account_id FROM trading.accounts WHERE client_id = (SELECT client_id FROM trading.clients WHERE email = 'emma.davis@example.com')), (SELECT instrument_id FROM trading.instruments WHERE symbol = 'JPM'), 'BUY', 50, 'EMMA-BUY-JPM-50', now() - interval '2 days'),

-- Frank Miller - Tech enthusiast
((SELECT account_id FROM trading.accounts WHERE client_id = (SELECT client_id FROM trading.clients WHERE email = 'frank.miller@example.com')), (SELECT instrument_id FROM trading.instruments WHERE symbol = 'NVDA'), 'BUY', 35, 'FRANK-BUY-NVDA-35', now() - interval '7 days'),
((SELECT account_id FROM trading.accounts WHERE client_id = (SELECT client_id FROM trading.clients WHERE email = 'frank.miller@example.com')), (SELECT instrument_id FROM trading.instruments WHERE symbol = 'TSLA'), 'BUY', 20, 'FRANK-BUY-TSLA-20', now() - interval '4 days'),
((SELECT account_id FROM trading.accounts WHERE client_id = (SELECT client_id FROM trading.clients WHERE email = 'frank.miller@example.com')), (SELECT instrument_id FROM trading.instruments WHERE symbol = 'NVDA'), 'SELL', 10, 'FRANK-SELL-NVDA-10', now() - interval '1 day');

-- Create fills for all orders (100% fill rate for clean test data)
INSERT INTO trading.fills (order_id, price, quantity, status, executed_at)
SELECT
  o.order_id,
  -- Realistic prices: use ranges per instrument class
  CASE 
    WHEN i.asset_class = 'EQUITY' THEN (random() * 300 + 50)::numeric(28,10)
    WHEN i.asset_class = 'CRYPTO' THEN (random() * 50000 + 20000)::numeric(28,10)
    WHEN i.asset_class = 'FX' THEN (random() * 2 + 1)::numeric(28,10)
    ELSE 100::numeric(28,10)
  END as price,
  o.quantity, -- Full quantity fills
  'Filled'::text as status,
  o.submitted_at + (random() * interval '30 minutes') as executed_at
FROM trading.orders o
JOIN trading.instruments i ON o.instrument_id = i.instrument_id;

-- Create corresponding trade events for all orders and fills
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

INSERT INTO trading.trade_events (client_id, entity_type, entity_id, action, occurred_at, details)
SELECT
  a.client_id,
  'FILL',
  f.fill_id,
  'FILL_EXECUTED',
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

-- Rebuild holdings from fresh data (truncate and repopulate)
TRUNCATE trading.holdings;

INSERT INTO trading.holdings (account_id, instrument_id, quantity, updated_at)
SELECT
  a.account_id,
  i.instrument_id,
  GREATEST(
    0,
    COALESCE(
      SUM(
        CASE
          WHEN o.side = 'BUY' THEN f.quantity
          WHEN o.side = 'SELL' THEN -f.quantity
          ELSE 0
        END
      ),
      0
    )
  )::numeric(28,10) as quantity,
  CURRENT_TIMESTAMP as updated_at
FROM trading.accounts a
CROSS JOIN trading.instruments i
LEFT JOIN trading.orders o ON a.account_id = o.account_id AND i.instrument_id = o.instrument_id
LEFT JOIN trading.fills f ON o.order_id = f.order_id AND f.status = 'Filled'
GROUP BY a.account_id, i.instrument_id
HAVING GREATEST(0, COALESCE(SUM(CASE WHEN o.side = 'BUY' THEN f.quantity WHEN o.side = 'SELL' THEN -f.quantity ELSE 0 END), 0)) > 0;

COMMIT;
