-- Server-side order execution (BR-06, BR-08, BR-14, BR-15, section 9.1/9.4)

-- Limit orders keep their limit with the order, so execution can enforce it against the live quote.
-- Existing orders were all executed at market.
ALTER TABLE trading.orders
    ADD COLUMN IF NOT EXISTS order_type varchar(10) NOT NULL DEFAULT 'MARKET'
        CHECK (order_type IN ('MARKET', 'LIMIT')),
    ADD COLUMN IF NOT EXISTS limit_price numeric(28,10)
        CHECK (limit_price IS NULL OR limit_price > 0);

ALTER TABLE trading.orders DROP CONSTRAINT IF EXISTS orders_limit_price_matches_type;
ALTER TABLE trading.orders ADD CONSTRAINT orders_limit_price_matches_type
    CHECK ((order_type = 'LIMIT') = (limit_price IS NOT NULL));

-- A rejected execution records why, as part of the permanent trade record
ALTER TABLE trading.fills ADD COLUMN IF NOT EXISTS failure_reason text;

-- Each order has exactly one execution outcome (Filled or Failed): it can never be filled twice
CREATE UNIQUE INDEX IF NOT EXISTS ux_fills_order ON trading.fills (order_id);

-- Orders, fills and trade events are the firm's legal record: insert-only, like cash_events
CREATE OR REPLACE FUNCTION trading.prevent_trade_record_mutation()
RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION '% records are immutable', TG_TABLE_NAME;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS orders_immutable ON trading.orders;
CREATE TRIGGER orders_immutable
    BEFORE UPDATE OR DELETE ON trading.orders
    FOR EACH ROW EXECUTE FUNCTION trading.prevent_trade_record_mutation();

DROP TRIGGER IF EXISTS fills_immutable ON trading.fills;
CREATE TRIGGER fills_immutable
    BEFORE UPDATE OR DELETE ON trading.fills
    FOR EACH ROW EXECUTE FUNCTION trading.prevent_trade_record_mutation();

DROP TRIGGER IF EXISTS trade_events_immutable ON trading.trade_events;
CREATE TRIGGER trade_events_immutable
    BEFORE UPDATE OR DELETE ON trading.trade_events
    FOR EACH ROW EXECUTE FUNCTION trading.prevent_trade_record_mutation();

REVOKE UPDATE, DELETE ON trading.orders, trading.fills, trading.trade_events FROM PUBLIC;
