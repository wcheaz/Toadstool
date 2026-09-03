
BEGIN;
 
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE SCHEMA IF NOT EXISTS trading;
CREATE SCHEMA IF NOT EXISTS analytics;
 
CREATE TABLE IF NOT EXISTS trading.clients (
    client_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    identity_subject varchar(255) NOT NULL UNIQUE,
    email varchar(320) NOT NULL UNIQUE,
    display_name varchar(120) NOT NULL,
    country_code char(2) NOT NULL,
    segment_code varchar(32) NOT NULL,
    status varchar(20) NOT NULL CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED')),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
 
CREATE TABLE IF NOT EXISTS trading.sessions (
    session_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES trading.clients(client_id),
    token_hash char(64) NOT NULL UNIQUE,
    created_at timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz NOT NULL,
    last_seen_at timestamptz,
    revoked_at timestamptz,
    revoke_reason varchar(120),
    CHECK (expires_at > created_at)
);
 
CREATE TABLE IF NOT EXISTS trading.currencies (
    currency_code char(3) PRIMARY KEY,
    name varchar(80) NOT NULL,
    decimal_places smallint NOT NULL CHECK (decimal_places BETWEEN 0 AND 18),
    status varchar(20) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE'))
);
 
CREATE TABLE IF NOT EXISTS trading.accounts (
    account_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES trading.clients(client_id),
    account_number varchar(32) NOT NULL UNIQUE,
    base_currency_code char(3) NOT NULL REFERENCES trading.currencies(currency_code),
    status varchar(20) NOT NULL CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED')),
    opened_at timestamptz NOT NULL DEFAULT now(),
    closed_at timestamptz,
    UNIQUE (account_id, client_id),
    CHECK (closed_at IS NULL OR closed_at >= opened_at)
);
 
CREATE TABLE IF NOT EXISTS trading.instruments (
    instrument_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_class varchar(16) NOT NULL CHECK (asset_class IN ('EQUITY', 'FX', 'CRYPTO')),
    symbol varchar(40) NOT NULL,
    venue_code varchar(20) NOT NULL,
    name varchar(160) NOT NULL,
    base_currency_code char(3) NOT NULL REFERENCES trading.currencies(currency_code),
    quote_currency_code char(3) NOT NULL REFERENCES trading.currencies(currency_code),
    quantity_scale smallint NOT NULL CHECK (quantity_scale BETWEEN 0 AND 18),
    price_scale smallint NOT NULL CHECK (price_scale BETWEEN 0 AND 18),
    status varchar(20) NOT NULL CHECK (status IN ('TRADABLE', 'HALTED', 'INACTIVE')),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (asset_class, symbol, venue_code)
);
 
CREATE TABLE IF NOT EXISTS trading.market_quotes (
    quote_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    instrument_id uuid NOT NULL REFERENCES trading.instruments(instrument_id),
    bid_price numeric(28,10) NOT NULL CHECK (bid_price >= 0),
    ask_price numeric(28,10) NOT NULL CHECK (ask_price >= 0),
    provider_code varchar(40) NOT NULL,
    provider_quote_ref varchar(120),
    observed_at timestamptz NOT NULL,
    received_at timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz NOT NULL,
    CHECK (ask_price >= bid_price),
    CHECK (expires_at > observed_at)
);
 
CREATE TABLE IF NOT EXISTS trading.orders (
    order_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES trading.clients(client_id),
    account_id uuid NOT NULL,
    instrument_id uuid NOT NULL REFERENCES trading.instruments(instrument_id),
    side varchar(4) NOT NULL CHECK (side IN ('BUY', 'SELL')),
    order_type varchar(16) NOT NULL CHECK (order_type IN ('MARKET')),
    quantity numeric(28,10) NOT NULL CHECK (quantity > 0),
    indicative_quote_id uuid REFERENCES trading.market_quotes(quote_id),
    idempotency_key varchar(80) NOT NULL,
    submitted_at timestamptz NOT NULL DEFAULT now(),
    accepted_at timestamptz,
    current_status varchar(20) NOT NULL CHECK (
        current_status IN ('SUBMITTED', 'ACCEPTED', 'PENDING', 'FILLED', 'REJECTED')
    ),
    status_updated_at timestamptz NOT NULL DEFAULT now(),
    rejection_code varchar(40),
    version integer NOT NULL DEFAULT 1 CHECK (version > 0),
    UNIQUE (client_id, idempotency_key),
    FOREIGN KEY (account_id, client_id)
        REFERENCES trading.accounts(account_id, client_id),
    CHECK (accepted_at IS NULL OR accepted_at >= submitted_at),
    CHECK (
        (current_status = 'REJECTED' AND rejection_code IS NOT NULL)
        OR current_status <> 'REJECTED'
    )
);
 
CREATE TABLE IF NOT EXISTS trading.order_validations (
    validation_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id uuid NOT NULL REFERENCES trading.orders(order_id),
    rule_code varchar(50) NOT NULL,
    rule_version varchar(24) NOT NULL,
    outcome varchar(8) NOT NULL CHECK (outcome IN ('PASS', 'FAIL')),
    reason text,
    facts jsonb NOT NULL DEFAULT '{}'::jsonb,
    evaluated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (order_id, rule_code, rule_version)
);
 
CREATE TABLE IF NOT EXISTS trading.order_events (
    order_event_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id uuid NOT NULL REFERENCES trading.orders(order_id),
    sequence_no integer NOT NULL CHECK (sequence_no > 0),
    from_status varchar(20),
    to_status varchar(20) NOT NULL,
    reason_code varchar(40),
    occurred_at timestamptz NOT NULL DEFAULT now(),
    actor_type varchar(20) NOT NULL CHECK (actor_type IN ('CLIENT', 'SYSTEM', 'OPERATOR')),
    correlation_id uuid NOT NULL,
    UNIQUE (order_id, sequence_no)
);
 
CREATE TABLE IF NOT EXISTS trading.pricing_decisions (
    pricing_decision_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id uuid NOT NULL REFERENCES trading.orders(order_id),
    attempt_no smallint NOT NULL CHECK (attempt_no > 0),
    quote_id uuid NOT NULL REFERENCES trading.market_quotes(quote_id),
    bid_snapshot numeric(28,10) NOT NULL CHECK (bid_snapshot >= 0),
    ask_snapshot numeric(28,10) NOT NULL CHECK (ask_snapshot >= 0),
    selected_price numeric(28,10) CHECK (selected_price >= 0),
    decision varchar(12) NOT NULL CHECK (decision IN ('FILL', 'REJECT')),
    reason_code varchar(40),
    decided_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (order_id, attempt_no),
    CHECK (
        (decision = 'FILL' AND selected_price IS NOT NULL)
        OR (decision = 'REJECT' AND selected_price IS NULL AND reason_code IS NOT NULL)
    )
);
 
CREATE TABLE IF NOT EXISTS trading.fills (
    fill_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id uuid NOT NULL REFERENCES trading.orders(order_id),
    pricing_decision_id uuid NOT NULL REFERENCES trading.pricing_decisions(pricing_decision_id),
    external_execution_ref varchar(120) UNIQUE,
    quantity numeric(28,10) NOT NULL CHECK (quantity > 0),
    price numeric(28,10) NOT NULL CHECK (price >= 0),
    currency_code char(3) NOT NULL REFERENCES trading.currencies(currency_code),
    gross_amount numeric(28,10) NOT NULL CHECK (gross_amount >= 0),
    fee_amount numeric(28,10) NOT NULL DEFAULT 0 CHECK (fee_amount >= 0),
    executed_at timestamptz NOT NULL,
    recorded_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (pricing_decision_id)
);
 
CREATE TABLE IF NOT EXISTS trading.positions (
    account_id uuid NOT NULL REFERENCES trading.accounts(account_id),
    instrument_id uuid NOT NULL REFERENCES trading.instruments(instrument_id),
    quantity numeric(28,10) NOT NULL CHECK (quantity >= 0),
    version integer NOT NULL DEFAULT 1 CHECK (version > 0),
    updated_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (account_id, instrument_id)
);
 
CREATE TABLE IF NOT EXISTS trading.position_ledger (
    position_movement_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id uuid NOT NULL REFERENCES trading.accounts(account_id),
    instrument_id uuid NOT NULL REFERENCES trading.instruments(instrument_id),
    fill_id uuid NOT NULL UNIQUE REFERENCES trading.fills(fill_id),
    quantity_delta numeric(28,10) NOT NULL CHECK (quantity_delta <> 0),
    quantity_after numeric(28,10) NOT NULL CHECK (quantity_after >= 0),
    occurred_at timestamptz NOT NULL,
    recorded_at timestamptz NOT NULL DEFAULT now()
);
 
CREATE TABLE IF NOT EXISTS trading.cash_balances (
    account_id uuid NOT NULL REFERENCES trading.accounts(account_id),
    currency_code char(3) NOT NULL REFERENCES trading.currencies(currency_code),
    amount numeric(28,10) NOT NULL CHECK (amount >= 0),
    version integer NOT NULL DEFAULT 1 CHECK (version > 0),
    updated_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (account_id, currency_code)
);
 
CREATE TABLE IF NOT EXISTS trading.cash_ledger (
    cash_movement_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id uuid NOT NULL REFERENCES trading.accounts(account_id),
    currency_code char(3) NOT NULL REFERENCES trading.currencies(currency_code),
    fill_id uuid REFERENCES trading.fills(fill_id),
    movement_type varchar(20) NOT NULL CHECK (
        movement_type IN ('TRADE', 'FEE', 'DEPOSIT', 'WITHDRAWAL', 'ADJUSTMENT')
    ),
    amount_delta numeric(28,10) NOT NULL CHECK (amount_delta <> 0),
    balance_after numeric(28,10) NOT NULL CHECK (balance_after >= 0),
    occurred_at timestamptz NOT NULL,
    recorded_at timestamptz NOT NULL DEFAULT now(),
    reason varchar(200),
    CHECK (movement_type <> 'ADJUSTMENT' OR reason IS NOT NULL)
);
 
CREATE UNIQUE INDEX IF NOT EXISTS uq_cash_ledger_fill_type
    ON trading.cash_ledger (fill_id, movement_type)
    WHERE fill_id IS NOT NULL;
 
CREATE TABLE IF NOT EXISTS trading.audit_events (
    audit_event_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_type varchar(20) NOT NULL CHECK (actor_type IN ('CLIENT', 'SYSTEM', 'OPERATOR')),
    actor_id varchar(255) NOT NULL,
    client_id uuid REFERENCES trading.clients(client_id),
    action varchar(80) NOT NULL,
    entity_type varchar(40) NOT NULL,
    entity_id uuid,
    occurred_at timestamptz NOT NULL DEFAULT now(),
    correlation_id uuid NOT NULL,
    details jsonb NOT NULL DEFAULT '{}'::jsonb
);
 
CREATE TABLE IF NOT EXISTS trading.outbox_events (
    event_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type varchar(40) NOT NULL,
    aggregate_id uuid NOT NULL,
    event_type varchar(80) NOT NULL,
    payload jsonb NOT NULL,
    occurred_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    published_at timestamptz,
    attempt_count integer NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    last_error text
);
 
CREATE TABLE IF NOT EXISTS analytics.dim_date (
    date_key integer PRIMARY KEY,
    calendar_date date NOT NULL UNIQUE,
    month_no smallint NOT NULL CHECK (month_no BETWEEN 1 AND 12),
    quarter_no smallint NOT NULL CHECK (quarter_no BETWEEN 1 AND 4),
    year_no smallint NOT NULL
);
 
CREATE TABLE IF NOT EXISTS analytics.dim_client (
    client_key bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    client_id uuid NOT NULL,
    segment_code varchar(32) NOT NULL,
    country_code char(2) NOT NULL,
    effective_from timestamptz NOT NULL,
    effective_to timestamptz,
    is_current boolean NOT NULL,
    CHECK (effective_to IS NULL OR effective_to > effective_from)
);
 
CREATE TABLE IF NOT EXISTS analytics.dim_instrument (
    instrument_key bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    instrument_id uuid NOT NULL UNIQUE,
    symbol varchar(40) NOT NULL,
    name varchar(160) NOT NULL,
    asset_class varchar(16) NOT NULL,
    venue_code varchar(20) NOT NULL,
    quote_currency_code char(3) NOT NULL
);
 
CREATE TABLE IF NOT EXISTS analytics.fact_orders (
    order_id uuid PRIMARY KEY,
    date_key integer NOT NULL REFERENCES analytics.dim_date(date_key),
    client_key bigint NOT NULL REFERENCES analytics.dim_client(client_key),
    instrument_key bigint NOT NULL REFERENCES analytics.dim_instrument(instrument_key),
    submitted_at timestamptz NOT NULL,
    side varchar(4) NOT NULL CHECK (side IN ('BUY', 'SELL')),
    quantity numeric(28,10) NOT NULL CHECK (quantity > 0),
    final_status varchar(20) NOT NULL CHECK (final_status IN ('FILLED', 'REJECTED')),
    acceptance_ms integer CHECK (acceptance_ms >= 0),
    completion_ms integer CHECK (completion_ms >= 0)
);
 
CREATE TABLE IF NOT EXISTS analytics.fact_fills (
    fill_id uuid PRIMARY KEY,
    order_id uuid NOT NULL,
    date_key integer NOT NULL REFERENCES analytics.dim_date(date_key),
    client_key bigint NOT NULL REFERENCES analytics.dim_client(client_key),
    instrument_key bigint NOT NULL REFERENCES analytics.dim_instrument(instrument_key),
    executed_at timestamptz NOT NULL,
    quantity numeric(28,10) NOT NULL CHECK (quantity > 0),
    price numeric(28,10) NOT NULL CHECK (price >= 0),
    gross_amount numeric(28,10) NOT NULL CHECK (gross_amount >= 0),
    fee_amount numeric(28,10) NOT NULL DEFAULT 0 CHECK (fee_amount >= 0),
    currency_code char(3) NOT NULL
);
 
CREATE INDEX IF NOT EXISTS ix_sessions_client
    ON trading.sessions (client_id);
CREATE INDEX IF NOT EXISTS ix_accounts_client
    ON trading.accounts (client_id);
CREATE INDEX IF NOT EXISTS ix_quotes_instrument_observed
    ON trading.market_quotes (instrument_id, observed_at DESC);
CREATE INDEX IF NOT EXISTS ix_orders_client_submitted
    ON trading.orders (client_id, submitted_at DESC);
CREATE INDEX IF NOT EXISTS ix_order_events_order_sequence
    ON trading.order_events (order_id, sequence_no);
CREATE INDEX IF NOT EXISTS ix_fills_order_executed
    ON trading.fills (order_id, executed_at);
CREATE INDEX IF NOT EXISTS ix_position_ledger_account_occurred
    ON trading.position_ledger (account_id, occurred_at);
CREATE INDEX IF NOT EXISTS ix_cash_ledger_account_occurred
    ON trading.cash_ledger (account_id, occurred_at);
CREATE INDEX IF NOT EXISTS ix_audit_events_entity
    ON trading.audit_events (entity_type, entity_id, occurred_at);
CREATE INDEX IF NOT EXISTS ix_outbox_unpublished
    ON trading.outbox_events (created_at)
    WHERE published_at IS NULL;
CREATE INDEX IF NOT EXISTS ix_dim_client_lookup
    ON analytics.dim_client (client_id, effective_from, effective_to);
CREATE INDEX IF NOT EXISTS ix_fact_orders_date
    ON analytics.fact_orders (date_key, client_key, instrument_key);
CREATE INDEX IF NOT EXISTS ix_fact_fills_date
    ON analytics.fact_fills (date_key, client_key, instrument_key);
 
COMMIT;