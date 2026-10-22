-- BR-16: business insights are read from the analytics fact tables instead of aggregating the live
-- trading tables on every request. The facts gain the buy/sell split and fees the insights need.

ALTER TABLE analytics.fact_daily_instrument_activity
    ADD COLUMN IF NOT EXISTS buy_count integer NOT NULL DEFAULT 0 CHECK (buy_count >= 0),
    ADD COLUMN IF NOT EXISTS sell_count integer NOT NULL DEFAULT 0 CHECK (sell_count >= 0),
    ADD COLUMN IF NOT EXISTS buy_quantity numeric(28,10) NOT NULL DEFAULT 0 CHECK (buy_quantity >= 0),
    ADD COLUMN IF NOT EXISTS sell_quantity numeric(28,10) NOT NULL DEFAULT 0 CHECK (sell_quantity >= 0),
    ADD COLUMN IF NOT EXISTS buy_amount numeric(28,10) NOT NULL DEFAULT 0 CHECK (buy_amount >= 0),
    ADD COLUMN IF NOT EXISTS sell_amount numeric(28,10) NOT NULL DEFAULT 0 CHECK (sell_amount >= 0),
    ADD COLUMN IF NOT EXISTS fee_amount numeric(28,10) NOT NULL DEFAULT 0 CHECK (fee_amount >= 0);

ALTER TABLE analytics.fact_daily_platform_activity
    ADD COLUMN IF NOT EXISTS buy_count integer NOT NULL DEFAULT 0 CHECK (buy_count >= 0),
    ADD COLUMN IF NOT EXISTS sell_count integer NOT NULL DEFAULT 0 CHECK (sell_count >= 0),
    ADD COLUMN IF NOT EXISTS buy_quantity numeric(28,10) NOT NULL DEFAULT 0 CHECK (buy_quantity >= 0),
    ADD COLUMN IF NOT EXISTS sell_quantity numeric(28,10) NOT NULL DEFAULT 0 CHECK (sell_quantity >= 0),
    ADD COLUMN IF NOT EXISTS buy_amount numeric(28,10) NOT NULL DEFAULT 0 CHECK (buy_amount >= 0),
    ADD COLUMN IF NOT EXISTS sell_amount numeric(28,10) NOT NULL DEFAULT 0 CHECK (sell_amount >= 0),
    ADD COLUMN IF NOT EXISTS fee_amount numeric(28,10) NOT NULL DEFAULT 0 CHECK (fee_amount >= 0);

-- Keep the facts current as fills are written (Filled only; rejections are not trading activity)
CREATE OR REPLACE FUNCTION analytics.record_fill_activity()
RETURNS trigger AS $$
DECLARE
    v_instrument_id uuid;
    v_side varchar(4);
    v_day date := (NEW.executed_at AT TIME ZONE 'UTC')::date;
    v_amount numeric := NEW.price * NEW.quantity;
    v_buy int;
    v_sell int;
BEGIN
    IF NEW.status <> 'Filled' THEN
        RETURN NEW;
    END IF;

    SELECT instrument_id, side INTO v_instrument_id, v_side FROM trading.orders WHERE order_id = NEW.order_id;
    v_buy := CASE WHEN v_side = 'BUY' THEN 1 ELSE 0 END;
    v_sell := CASE WHEN v_side = 'SELL' THEN 1 ELSE 0 END;

    INSERT INTO analytics.fact_daily_instrument_activity
        (activity_date, instrument_id, order_count, filled_quantity, gross_amount,
         buy_count, sell_count, buy_quantity, sell_quantity, buy_amount, sell_amount, fee_amount, updated_at)
    VALUES (v_day, v_instrument_id, 1, NEW.quantity, v_amount,
            v_buy, v_sell, NEW.quantity * v_buy, NEW.quantity * v_sell, v_amount * v_buy, v_amount * v_sell,
            COALESCE(NEW.fee, 0), now())
    ON CONFLICT (activity_date, instrument_id) DO UPDATE SET
        order_count = analytics.fact_daily_instrument_activity.order_count + 1,
        filled_quantity = analytics.fact_daily_instrument_activity.filled_quantity + EXCLUDED.filled_quantity,
        gross_amount = analytics.fact_daily_instrument_activity.gross_amount + EXCLUDED.gross_amount,
        buy_count = analytics.fact_daily_instrument_activity.buy_count + EXCLUDED.buy_count,
        sell_count = analytics.fact_daily_instrument_activity.sell_count + EXCLUDED.sell_count,
        buy_quantity = analytics.fact_daily_instrument_activity.buy_quantity + EXCLUDED.buy_quantity,
        sell_quantity = analytics.fact_daily_instrument_activity.sell_quantity + EXCLUDED.sell_quantity,
        buy_amount = analytics.fact_daily_instrument_activity.buy_amount + EXCLUDED.buy_amount,
        sell_amount = analytics.fact_daily_instrument_activity.sell_amount + EXCLUDED.sell_amount,
        fee_amount = analytics.fact_daily_instrument_activity.fee_amount + EXCLUDED.fee_amount,
        updated_at = now();

    INSERT INTO analytics.fact_daily_platform_activity
        (activity_date, order_count, filled_quantity, gross_amount,
         buy_count, sell_count, buy_quantity, sell_quantity, buy_amount, sell_amount, fee_amount, updated_at)
    VALUES (v_day, 1, NEW.quantity, v_amount,
            v_buy, v_sell, NEW.quantity * v_buy, NEW.quantity * v_sell, v_amount * v_buy, v_amount * v_sell,
            COALESCE(NEW.fee, 0), now())
    ON CONFLICT (activity_date) DO UPDATE SET
        order_count = analytics.fact_daily_platform_activity.order_count + 1,
        filled_quantity = analytics.fact_daily_platform_activity.filled_quantity + EXCLUDED.filled_quantity,
        gross_amount = analytics.fact_daily_platform_activity.gross_amount + EXCLUDED.gross_amount,
        buy_count = analytics.fact_daily_platform_activity.buy_count + EXCLUDED.buy_count,
        sell_count = analytics.fact_daily_platform_activity.sell_count + EXCLUDED.sell_count,
        buy_quantity = analytics.fact_daily_platform_activity.buy_quantity + EXCLUDED.buy_quantity,
        sell_quantity = analytics.fact_daily_platform_activity.sell_quantity + EXCLUDED.sell_quantity,
        buy_amount = analytics.fact_daily_platform_activity.buy_amount + EXCLUDED.buy_amount,
        sell_amount = analytics.fact_daily_platform_activity.sell_amount + EXCLUDED.sell_amount,
        fee_amount = analytics.fact_daily_platform_activity.fee_amount + EXCLUDED.fee_amount,
        updated_at = now();

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Rebuild the facts from the trade record so existing history has the new breakdown.
-- The facts are derived data; trading.fills remains the source of truth.
TRUNCATE analytics.fact_daily_instrument_activity, analytics.fact_daily_platform_activity;

INSERT INTO analytics.fact_daily_instrument_activity
    (activity_date, instrument_id, order_count, filled_quantity, gross_amount,
     buy_count, sell_count, buy_quantity, sell_quantity, buy_amount, sell_amount, fee_amount, updated_at)
SELECT (f.executed_at AT TIME ZONE 'UTC')::date, o.instrument_id,
       COUNT(*), SUM(f.quantity), SUM(f.price * f.quantity),
       COUNT(*) FILTER (WHERE o.side = 'BUY'), COUNT(*) FILTER (WHERE o.side = 'SELL'),
       COALESCE(SUM(f.quantity) FILTER (WHERE o.side = 'BUY'), 0), COALESCE(SUM(f.quantity) FILTER (WHERE o.side = 'SELL'), 0),
       COALESCE(SUM(f.price * f.quantity) FILTER (WHERE o.side = 'BUY'), 0), COALESCE(SUM(f.price * f.quantity) FILTER (WHERE o.side = 'SELL'), 0),
       COALESCE(SUM(f.fee), 0), now()
FROM trading.fills f
JOIN trading.orders o ON o.order_id = f.order_id
WHERE f.status = 'Filled'
GROUP BY 1, 2;

INSERT INTO analytics.fact_daily_platform_activity
    (activity_date, order_count, filled_quantity, gross_amount,
     buy_count, sell_count, buy_quantity, sell_quantity, buy_amount, sell_amount, fee_amount, updated_at)
SELECT activity_date, SUM(order_count), SUM(filled_quantity), SUM(gross_amount),
       SUM(buy_count), SUM(sell_count), SUM(buy_quantity), SUM(sell_quantity), SUM(buy_amount), SUM(sell_amount),
       SUM(fee_amount), now()
FROM analytics.fact_daily_instrument_activity
GROUP BY activity_date;
