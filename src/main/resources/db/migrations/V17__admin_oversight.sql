BEGIN;

-- Fee charged on each execution, so fee and net-cash figures come from stored data.
ALTER TABLE trading.fills
    ADD COLUMN IF NOT EXISTS fee numeric(28,10) NOT NULL DEFAULT 0 CHECK (fee >= 0);

-- Records which admin viewed which client, backing the "admin access is audited" promise.
CREATE TABLE IF NOT EXISTS trading.admin_access_events (
    access_event_id uuid PRIMARY KEY DEFAULT gen_random_uuid(), -- Unique access event identifier
    admin_user_id uuid NOT NULL REFERENCES trading.admin_users(admin_user_id), -- Admin who performed the access
    admin_email varchar(320) NOT NULL, -- Admin email at the time of access
    client_id uuid REFERENCES trading.clients(client_id), -- Client whose data was viewed, if any
    action varchar(80) NOT NULL, -- What was accessed, e.g. VIEW_CLIENT
    occurred_at timestamptz NOT NULL DEFAULT now(), -- When the access happened
    details jsonb NOT NULL DEFAULT '{}'::jsonb -- Extra context
);

CREATE INDEX IF NOT EXISTS ix_admin_access_events_occurred
    ON trading.admin_access_events (occurred_at DESC);

CREATE INDEX IF NOT EXISTS ix_admin_access_events_client
    ON trading.admin_access_events (client_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS ix_trade_events_occurred
    ON trading.trade_events (occurred_at DESC);

CREATE INDEX IF NOT EXISTS ix_login_events_occurred
    ON trading.login_events (occurred_at DESC);

-- Keep the daily analytics facts current: every successful fill rolls into its day's totals.
CREATE OR REPLACE FUNCTION analytics.record_fill_activity()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    v_instrument_id uuid;
    v_day date := (NEW.executed_at AT TIME ZONE 'UTC')::date;
BEGIN
    IF NEW.status <> 'Filled' THEN
        RETURN NEW;
    END IF;

    SELECT instrument_id INTO v_instrument_id FROM trading.orders WHERE order_id = NEW.order_id;

    INSERT INTO analytics.fact_daily_instrument_activity
        (activity_date, instrument_id, order_count, filled_quantity, gross_amount, updated_at)
    VALUES (v_day, v_instrument_id, 1, NEW.quantity, NEW.price * NEW.quantity, now())
    ON CONFLICT (activity_date, instrument_id) DO UPDATE SET
        order_count = analytics.fact_daily_instrument_activity.order_count + 1,
        filled_quantity = analytics.fact_daily_instrument_activity.filled_quantity + EXCLUDED.filled_quantity,
        gross_amount = analytics.fact_daily_instrument_activity.gross_amount + EXCLUDED.gross_amount,
        updated_at = now();

    INSERT INTO analytics.fact_daily_platform_activity
        (activity_date, order_count, filled_quantity, gross_amount, updated_at)
    VALUES (v_day, 1, NEW.quantity, NEW.price * NEW.quantity, now())
    ON CONFLICT (activity_date) DO UPDATE SET
        order_count = analytics.fact_daily_platform_activity.order_count + 1,
        filled_quantity = analytics.fact_daily_platform_activity.filled_quantity + EXCLUDED.filled_quantity,
        gross_amount = analytics.fact_daily_platform_activity.gross_amount + EXCLUDED.gross_amount,
        updated_at = now();

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_fills_record_activity ON trading.fills;
CREATE TRIGGER trg_fills_record_activity
    AFTER INSERT ON trading.fills
    FOR EACH ROW EXECUTE FUNCTION analytics.record_fill_activity();

-- Rebuild the daily analytics facts from actual fills instead of the synthetic seed rows;
-- the trigger above keeps them current from here on.
DELETE FROM analytics.fact_daily_instrument_activity;
DELETE FROM analytics.fact_daily_platform_activity;

INSERT INTO analytics.fact_daily_instrument_activity
    (activity_date, instrument_id, order_count, filled_quantity, gross_amount, updated_at)
SELECT (f.executed_at AT TIME ZONE 'UTC')::date, o.instrument_id,
       COUNT(DISTINCT o.order_id), SUM(f.quantity), SUM(f.price * f.quantity), now()
FROM trading.fills f
JOIN trading.orders o ON o.order_id = f.order_id
WHERE f.status = 'Filled'
GROUP BY 1, 2;

INSERT INTO analytics.fact_daily_platform_activity
    (activity_date, order_count, filled_quantity, gross_amount, updated_at)
SELECT (f.executed_at AT TIME ZONE 'UTC')::date,
       COUNT(DISTINCT o.order_id), SUM(f.quantity), SUM(f.price * f.quantity), now()
FROM trading.fills f
JOIN trading.orders o ON o.order_id = f.order_id
WHERE f.status = 'Filled'
GROUP BY 1;

COMMIT;
