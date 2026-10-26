-- BR-16: reporting must not compete with live trading. The daily facts were maintained by a trigger
-- inside every fill transaction; they are now projected by a background job (AnalyticsProjectionJob)
-- that calls analytics.project_new_fills() every few seconds, so a trade's transaction only writes
-- trading data. Reports trail live trading by up to one projection interval.

-- Ledger of fills already counted in the facts: each fill is projected exactly once
CREATE TABLE IF NOT EXISTS analytics.projected_fill (
    fill_id uuid PRIMARY KEY,
    projected_at timestamptz NOT NULL DEFAULT now()
);

-- Every filled fill that exists now is already in the facts (rebuilt by V26, then kept by the trigger)
INSERT INTO analytics.projected_fill (fill_id)
SELECT fill_id FROM trading.fills WHERE status = 'Filled'
ON CONFLICT (fill_id) DO NOTHING;

DROP TRIGGER IF EXISTS trg_fills_record_activity ON trading.fills;
DROP FUNCTION IF EXISTS analytics.record_fill_activity();

-- Fold filled fills not yet projected into the daily facts; returns how many were projected.
-- An advisory lock keeps concurrent runs (e.g. several backend instances) from projecting the same fill.
CREATE OR REPLACE FUNCTION analytics.project_new_fills()
RETURNS integer AS $$
DECLARE
    v_count integer;
BEGIN
    PERFORM pg_advisory_xact_lock(hashtext('analytics.project_new_fills'));

    CREATE TEMP TABLE IF NOT EXISTS pg_temp.new_fills (
        fill_id uuid, activity_date date, instrument_id uuid, side varchar(4),
        quantity numeric, amount numeric, fee numeric
    ) ON COMMIT DROP;
    TRUNCATE pg_temp.new_fills;

    WITH claimed AS (
        INSERT INTO analytics.projected_fill (fill_id)
        SELECT f.fill_id
        FROM trading.fills f
        WHERE f.status = 'Filled'
          AND NOT EXISTS (SELECT 1 FROM analytics.projected_fill p WHERE p.fill_id = f.fill_id)
        ON CONFLICT (fill_id) DO NOTHING
        RETURNING fill_id
    )
    INSERT INTO pg_temp.new_fills
    SELECT f.fill_id, (f.executed_at AT TIME ZONE 'UTC')::date, o.instrument_id, o.side,
           f.quantity, f.price * f.quantity, COALESCE(f.fee, 0)
    FROM claimed c
    JOIN trading.fills f ON f.fill_id = c.fill_id
    JOIN trading.orders o ON o.order_id = f.order_id;

    GET DIAGNOSTICS v_count = ROW_COUNT;
    IF v_count = 0 THEN
        RETURN 0;
    END IF;

    INSERT INTO analytics.fact_daily_instrument_activity
        (activity_date, instrument_id, order_count, filled_quantity, gross_amount,
         buy_count, sell_count, buy_quantity, sell_quantity, buy_amount, sell_amount, fee_amount, updated_at)
    SELECT activity_date, instrument_id, COUNT(*), SUM(quantity), SUM(amount),
           COUNT(*) FILTER (WHERE side = 'BUY'), COUNT(*) FILTER (WHERE side = 'SELL'),
           COALESCE(SUM(quantity) FILTER (WHERE side = 'BUY'), 0), COALESCE(SUM(quantity) FILTER (WHERE side = 'SELL'), 0),
           COALESCE(SUM(amount) FILTER (WHERE side = 'BUY'), 0), COALESCE(SUM(amount) FILTER (WHERE side = 'SELL'), 0),
           SUM(fee), now()
    FROM pg_temp.new_fills
    GROUP BY activity_date, instrument_id
    ON CONFLICT (activity_date, instrument_id) DO UPDATE SET
        order_count = analytics.fact_daily_instrument_activity.order_count + EXCLUDED.order_count,
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
    SELECT activity_date, COUNT(*), SUM(quantity), SUM(amount),
           COUNT(*) FILTER (WHERE side = 'BUY'), COUNT(*) FILTER (WHERE side = 'SELL'),
           COALESCE(SUM(quantity) FILTER (WHERE side = 'BUY'), 0), COALESCE(SUM(quantity) FILTER (WHERE side = 'SELL'), 0),
           COALESCE(SUM(amount) FILTER (WHERE side = 'BUY'), 0), COALESCE(SUM(amount) FILTER (WHERE side = 'SELL'), 0),
           SUM(fee), now()
    FROM pg_temp.new_fills
    GROUP BY activity_date
    ON CONFLICT (activity_date) DO UPDATE SET
        order_count = analytics.fact_daily_platform_activity.order_count + EXCLUDED.order_count,
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

    RETURN v_count;
END;
$$ LANGUAGE plpgsql;
