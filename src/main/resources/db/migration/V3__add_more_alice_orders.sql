-- Additional test orders for alice.johnson to support pagination testing
-- This migration adds 2 more orders to bring alice.johnson from 3 to 5 orders total

BEGIN;

-- Get alice.johnson's account ID for reference
-- SELL order for Google - alice.johnson account (fourth order for alice)
INSERT INTO trading.orders (account_id, instrument_id, side, quantity, idempotency_key, submitted_at) VALUES
((SELECT account_id FROM trading.accounts WHERE client_id = (SELECT client_id FROM trading.clients WHERE email = 'alice.johnson@example.com')), (SELECT instrument_id FROM trading.instruments WHERE symbol = 'GOOGL'), 'SELL', 50, 'TEST-SELL-GOOGL-50', now() - interval '6 hours');

-- BUY order for Tesla - alice.johnson account (fifth order for alice)
INSERT INTO trading.orders (account_id, instrument_id, side, quantity, idempotency_key, submitted_at) VALUES
((SELECT account_id FROM trading.accounts WHERE client_id = (SELECT client_id FROM trading.clients WHERE email = 'alice.johnson@example.com')), (SELECT instrument_id FROM trading.instruments WHERE symbol = 'TSLA'), 'BUY', 25, 'TEST-BUY-TSLA-25', now() - interval '2 hours');

-- Create fills for these new orders (deterministic for testing)
INSERT INTO trading.fills (order_id, price, quantity, status, executed_at)
SELECT
  o.order_id,
  2500.00::numeric(28,10) as price,
  o.quantity * 0.75 as quantity,
  'Filled'::varchar as status,
  o.submitted_at + interval '30 minutes' as executed_at
FROM trading.orders o
WHERE o.idempotency_key IN ('TEST-SELL-GOOGL-50', 'TEST-BUY-TSLA-25');

-- Create trade_events for these new orders
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
JOIN trading.accounts a ON o.account_id = a.account_id
WHERE o.idempotency_key IN ('TEST-SELL-GOOGL-50', 'TEST-BUY-TSLA-25');

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
JOIN trading.accounts a ON o.account_id = a.account_id
WHERE o.idempotency_key IN ('TEST-SELL-GOOGL-50', 'TEST-BUY-TSLA-25');

COMMIT;
