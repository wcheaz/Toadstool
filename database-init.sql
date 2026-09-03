
BEGIN;
 
CREATE SCHEMA IF NOT EXISTS trading; -- OLTP: normalized operational tables
CREATE SCHEMA IF NOT EXISTS analytics; -- OLAP: denormalized star schema for reporting
 
CREATE TABLE IF NOT EXISTS trading.clients (
    client_id uuid PRIMARY KEY DEFAULT gen_random_uuid(), -- Unique identifier for the client
    identity_subject varchar(255) NOT NULL UNIQUE, -- External identity provider subject
    email varchar(320) NOT NULL UNIQUE, -- Client's email address
    display_name varchar(120) NOT NULL, -- Client's display name
    country_code char(2) NOT NULL, -- ISO 2-letter country code
    segment_code varchar(32) NOT NULL, -- Client segment classification
    status varchar(20) NOT NULL CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED')), -- Current client status
    created_at timestamptz NOT NULL DEFAULT now(), -- Record creation timestamp
    updated_at timestamptz NOT NULL DEFAULT now() -- Record last update timestamp
);
 
CREATE TABLE IF NOT EXISTS trading.sessions (
    session_id uuid PRIMARY KEY DEFAULT gen_random_uuid(), -- Unique session identifier
    client_id uuid NOT NULL REFERENCES trading.clients(client_id), -- Reference to client
    token_hash char(64) NOT NULL UNIQUE, -- Hash of session token
    created_at timestamptz NOT NULL DEFAULT now(), -- Session creation timestamp
    expires_at timestamptz NOT NULL, -- Session expiration timestamp
    last_seen_at timestamptz, -- Last activity timestamp
    revoked_at timestamptz, -- Revocation timestamp if applicable
    revoke_reason varchar(120), -- Reason for session revocation
    CHECK (expires_at > created_at)
);
 
CREATE TABLE IF NOT EXISTS trading.currencies (
    currency_code char(3) PRIMARY KEY, -- ISO 4217 currency code
    name varchar(80) NOT NULL, -- Full currency name
    decimal_places smallint NOT NULL CHECK (decimal_places BETWEEN 0 AND 18), -- Number of decimal places
    status varchar(20) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')) -- Currency trading status
);
 
CREATE TABLE IF NOT EXISTS trading.accounts (
    account_id uuid PRIMARY KEY DEFAULT gen_random_uuid(), -- Unique account identifier
    client_id uuid NOT NULL REFERENCES trading.clients(client_id), -- Reference to account owner
    account_number varchar(32) NOT NULL UNIQUE, -- Account number (external identifier)
    base_currency_code char(3) NOT NULL REFERENCES trading.currencies(currency_code), -- Account base currency
    status varchar(20) NOT NULL CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED')), -- Current account status
    opened_at timestamptz NOT NULL DEFAULT now(), -- Account opening timestamp
    closed_at timestamptz, -- Account closing timestamp if applicable
    UNIQUE (account_id, client_id),
    CHECK (closed_at IS NULL OR closed_at >= opened_at)
);
 
CREATE TABLE IF NOT EXISTS trading.instruments (
    instrument_id uuid PRIMARY KEY DEFAULT gen_random_uuid(), -- Unique instrument identifier
    asset_class varchar(16) NOT NULL CHECK (asset_class IN ('EQUITY', 'FX', 'CRYPTO')), -- Type of financial instrument
    symbol varchar(40) NOT NULL, -- Trading symbol
    venue_code varchar(20) NOT NULL, -- Trading venue identifier
    name varchar(160) NOT NULL, -- Full instrument name
    base_currency_code char(3) NOT NULL REFERENCES trading.currencies(currency_code), -- Base currency
    quote_currency_code char(3) NOT NULL REFERENCES trading.currencies(currency_code), -- Quote currency
    quantity_scale smallint NOT NULL CHECK (quantity_scale BETWEEN 0 AND 18), -- Decimal places for quantity
    price_scale smallint NOT NULL CHECK (price_scale BETWEEN 0 AND 18), -- Decimal places for price
    status varchar(20) NOT NULL CHECK (status IN ('TRADABLE', 'HALTED', 'INACTIVE')), -- Trading status
    created_at timestamptz NOT NULL DEFAULT now(), -- Record creation timestamp
    updated_at timestamptz NOT NULL DEFAULT now(), -- Record last update timestamp
    UNIQUE (asset_class, symbol, venue_code)
);
 
CREATE TABLE IF NOT EXISTS trading.market_quotes (
    quote_id uuid PRIMARY KEY DEFAULT gen_random_uuid(), -- Unique quote identifier
    instrument_id uuid NOT NULL REFERENCES trading.instruments(instrument_id), -- Reference to instrument
    bid_price numeric(28,10) NOT NULL CHECK (bid_price >= 0), -- Bid price
    ask_price numeric(28,10) NOT NULL CHECK (ask_price >= 0), -- Ask price
    provider_code varchar(40) NOT NULL, -- Market data provider identifier
    provider_quote_ref varchar(120), -- Provider's reference for this quote
    observed_at timestamptz NOT NULL, -- When quote was observed in market
    received_at timestamptz NOT NULL DEFAULT now(), -- When quote was received
    expires_at timestamptz NOT NULL, -- Quote expiration timestamp
    CHECK (ask_price >= bid_price),
    CHECK (expires_at > observed_at)
);
 
CREATE TABLE IF NOT EXISTS trading.orders (
    order_id uuid PRIMARY KEY DEFAULT gen_random_uuid(), -- Unique order identifier
    client_id uuid NOT NULL REFERENCES trading.clients(client_id), -- Reference to client placing order
    account_id uuid NOT NULL, -- Reference to trading account
    instrument_id uuid NOT NULL REFERENCES trading.instruments(instrument_id), -- Reference to instrument
    side varchar(4) NOT NULL CHECK (side IN ('BUY', 'SELL')), -- Order side (buy or sell)
    order_type varchar(16) NOT NULL CHECK (order_type IN ('MARKET')), -- Type of order
    quantity numeric(28,10) NOT NULL CHECK (quantity > 0), -- Order quantity
    indicative_quote_id uuid REFERENCES trading.market_quotes(quote_id), -- Associated market quote
    idempotency_key varchar(80) NOT NULL, -- Idempotency key for duplicate detection
    submitted_at timestamptz NOT NULL DEFAULT now(), -- Order submission timestamp
    accepted_at timestamptz, -- Order acceptance timestamp
    current_status varchar(20) NOT NULL CHECK (
        current_status IN ('SUBMITTED', 'ACCEPTED', 'PENDING', 'FILLED', 'REJECTED')
    ), -- Current order status
    status_updated_at timestamptz NOT NULL DEFAULT now(), -- Last status update timestamp
    rejection_code varchar(40), -- Code explaining rejection if applicable
    version integer NOT NULL DEFAULT 1 CHECK (version > 0), -- Optimistic lock version
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
    validation_id uuid PRIMARY KEY DEFAULT gen_random_uuid(), -- Unique validation identifier
    order_id uuid NOT NULL REFERENCES trading.orders(order_id), -- Reference to order being validated
    rule_code varchar(50) NOT NULL, -- Validation rule code
    rule_version varchar(24) NOT NULL, -- Version of validation rule
    outcome varchar(8) NOT NULL CHECK (outcome IN ('PASS', 'FAIL')), -- Validation result
    reason text, -- Explanation of validation outcome
    facts jsonb NOT NULL DEFAULT '{}'::jsonb, -- Rule evaluation facts
    evaluated_at timestamptz NOT NULL DEFAULT now(), -- Validation evaluation timestamp
    UNIQUE (order_id, rule_code, rule_version)
);
 
CREATE TABLE IF NOT EXISTS trading.order_events (
    order_event_id uuid PRIMARY KEY DEFAULT gen_random_uuid(), -- Unique event identifier
    order_id uuid NOT NULL REFERENCES trading.orders(order_id), -- Reference to order
    sequence_no integer NOT NULL CHECK (sequence_no > 0), -- Event sequence number
    from_status varchar(20), -- Previous order status
    to_status varchar(20) NOT NULL, -- New order status
    reason_code varchar(40), -- Reason for status change
    occurred_at timestamptz NOT NULL DEFAULT now(), -- Event occurrence timestamp
    actor_type varchar(20) NOT NULL CHECK (actor_type IN ('CLIENT', 'SYSTEM', 'OPERATOR')), -- Type of actor causing event
    correlation_id uuid NOT NULL, -- Correlation identifier for tracing
    UNIQUE (order_id, sequence_no)
);
 
CREATE TABLE IF NOT EXISTS trading.pricing_decisions (
    pricing_decision_id uuid PRIMARY KEY DEFAULT gen_random_uuid(), -- Unique decision identifier
    order_id uuid NOT NULL REFERENCES trading.orders(order_id), -- Reference to order
    attempt_no smallint NOT NULL CHECK (attempt_no > 0), -- Attempt number for this order
    quote_id uuid NOT NULL REFERENCES trading.market_quotes(quote_id), -- Associated market quote
    bid_snapshot numeric(28,10) NOT NULL CHECK (bid_snapshot >= 0), -- Snapshot of bid price
    ask_snapshot numeric(28,10) NOT NULL CHECK (ask_snapshot >= 0), -- Snapshot of ask price
    selected_price numeric(28,10) CHECK (selected_price >= 0), -- Selected execution price
    decision varchar(12) NOT NULL CHECK (decision IN ('FILL', 'REJECT')), -- Pricing decision
    reason_code varchar(40), -- Reason for decision
    decided_at timestamptz NOT NULL DEFAULT now(), -- Decision timestamp
    UNIQUE (order_id, attempt_no),
    CHECK (
        (decision = 'FILL' AND selected_price IS NOT NULL)
        OR (decision = 'REJECT' AND selected_price IS NULL AND reason_code IS NOT NULL)
    )
);
 
CREATE TABLE IF NOT EXISTS trading.fills (
    fill_id uuid PRIMARY KEY DEFAULT gen_random_uuid(), -- Unique fill identifier
    order_id uuid NOT NULL REFERENCES trading.orders(order_id), -- Reference to order
    pricing_decision_id uuid NOT NULL REFERENCES trading.pricing_decisions(pricing_decision_id), -- Associated pricing decision
    external_execution_ref varchar(120) UNIQUE, -- External execution reference
    quantity numeric(28,10) NOT NULL CHECK (quantity > 0), -- Filled quantity
    price numeric(28,10) NOT NULL CHECK (price >= 0), -- Execution price
    currency_code char(3) NOT NULL REFERENCES trading.currencies(currency_code), -- Currency of fill
    gross_amount numeric(28,10) NOT NULL CHECK (gross_amount >= 0), -- Gross transaction amount
    fee_amount numeric(28,10) NOT NULL DEFAULT 0 CHECK (fee_amount >= 0), -- Transaction fee amount
    executed_at timestamptz NOT NULL, -- Execution timestamp
    recorded_at timestamptz NOT NULL DEFAULT now(), -- Recording timestamp
    UNIQUE (pricing_decision_id)
);
 
CREATE TABLE IF NOT EXISTS trading.positions (
    account_id uuid NOT NULL REFERENCES trading.accounts(account_id), -- Reference to account
    instrument_id uuid NOT NULL REFERENCES trading.instruments(instrument_id), -- Reference to instrument
    quantity numeric(28,10) NOT NULL CHECK (quantity >= 0), -- Current position quantity
    version integer NOT NULL DEFAULT 1 CHECK (version > 0), -- Optimistic lock version
    updated_at timestamptz NOT NULL DEFAULT now(), -- Last update timestamp
    PRIMARY KEY (account_id, instrument_id)
);
 
CREATE TABLE IF NOT EXISTS trading.position_ledger (
    position_movement_id uuid PRIMARY KEY DEFAULT gen_random_uuid(), -- Unique movement identifier
    account_id uuid NOT NULL REFERENCES trading.accounts(account_id), -- Reference to account
    instrument_id uuid NOT NULL REFERENCES trading.instruments(instrument_id), -- Reference to instrument
    fill_id uuid NOT NULL UNIQUE REFERENCES trading.fills(fill_id), -- Associated fill
    quantity_delta numeric(28,10) NOT NULL CHECK (quantity_delta <> 0), -- Change in quantity
    quantity_after numeric(28,10) NOT NULL CHECK (quantity_after >= 0), -- Position quantity after movement
    occurred_at timestamptz NOT NULL, -- Movement occurrence timestamp
    recorded_at timestamptz NOT NULL DEFAULT now() -- Recording timestamp
);
 
CREATE TABLE IF NOT EXISTS trading.cash_balances (
    account_id uuid NOT NULL REFERENCES trading.accounts(account_id), -- Reference to account
    currency_code char(3) NOT NULL REFERENCES trading.currencies(currency_code), -- Currency of balance
    amount numeric(28,10) NOT NULL CHECK (amount >= 0), -- Current cash balance
    version integer NOT NULL DEFAULT 1 CHECK (version > 0), -- Optimistic lock version
    updated_at timestamptz NOT NULL DEFAULT now(), -- Last update timestamp
    PRIMARY KEY (account_id, currency_code)
);
 
CREATE TABLE IF NOT EXISTS trading.cash_ledger (
    cash_movement_id uuid PRIMARY KEY DEFAULT gen_random_uuid(), -- Unique movement identifier
    account_id uuid NOT NULL REFERENCES trading.accounts(account_id), -- Reference to account
    currency_code char(3) NOT NULL REFERENCES trading.currencies(currency_code), -- Currency of movement
    fill_id uuid REFERENCES trading.fills(fill_id), -- Associated fill if applicable
    movement_type varchar(20) NOT NULL CHECK (
        movement_type IN ('TRADE', 'FEE', 'DEPOSIT', 'WITHDRAWAL', 'ADJUSTMENT')
    ), -- Type of cash movement
    amount_delta numeric(28,10) NOT NULL CHECK (amount_delta <> 0), -- Change in cash amount
    balance_after numeric(28,10) NOT NULL CHECK (balance_after >= 0), -- Cash balance after movement
    occurred_at timestamptz NOT NULL, -- Movement occurrence timestamp
    recorded_at timestamptz NOT NULL DEFAULT now(), -- Recording timestamp
    reason varchar(200), -- Reason for movement
    CHECK (movement_type <> 'ADJUSTMENT' OR reason IS NOT NULL)
);
 
CREATE UNIQUE INDEX IF NOT EXISTS uq_cash_ledger_fill_type
    ON trading.cash_ledger (fill_id, movement_type)
    WHERE fill_id IS NOT NULL;
 
CREATE TABLE IF NOT EXISTS trading.audit_events (
    audit_event_id uuid PRIMARY KEY DEFAULT gen_random_uuid(), -- Unique audit event identifier
    actor_type varchar(20) NOT NULL CHECK (actor_type IN ('CLIENT', 'SYSTEM', 'OPERATOR')), -- Type of actor performing action
    actor_id varchar(255) NOT NULL, -- Identifier of actor
    client_id uuid REFERENCES trading.clients(client_id), -- Associated client if applicable
    action varchar(80) NOT NULL, -- Action performed
    entity_type varchar(40) NOT NULL, -- Type of entity affected
    entity_id uuid, -- Identifier of affected entity
    occurred_at timestamptz NOT NULL DEFAULT now(), -- Event occurrence timestamp
    correlation_id uuid NOT NULL, -- Correlation identifier for tracing
    details jsonb NOT NULL DEFAULT '{}'::jsonb -- Additional event details
);
 
CREATE TABLE IF NOT EXISTS trading.outbox_events (
    event_id uuid PRIMARY KEY DEFAULT gen_random_uuid(), -- Unique event identifier
    aggregate_type varchar(40) NOT NULL, -- Type of aggregate root
    aggregate_id uuid NOT NULL, -- Identifier of aggregate root
    event_type varchar(80) NOT NULL, -- Type of domain event
    payload jsonb NOT NULL, -- Event payload data
    occurred_at timestamptz NOT NULL, -- Event occurrence timestamp
    created_at timestamptz NOT NULL DEFAULT now(), -- Record creation timestamp
    published_at timestamptz, -- Event publication timestamp
    attempt_count integer NOT NULL DEFAULT 0 CHECK (attempt_count >= 0), -- Number of publication attempts
    last_error text -- Last publication error message
);
 
CREATE TABLE IF NOT EXISTS analytics.dim_date (
    date_key integer PRIMARY KEY, -- Date surrogate key
    calendar_date date NOT NULL UNIQUE, -- Calendar date
    month_no smallint NOT NULL CHECK (month_no BETWEEN 1 AND 12), -- Month number
    quarter_no smallint NOT NULL CHECK (quarter_no BETWEEN 1 AND 4), -- Quarter number
    year_no smallint NOT NULL -- Year number
);
 
CREATE TABLE IF NOT EXISTS analytics.dim_client (
    client_key bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY, -- Client surrogate key
    client_id uuid NOT NULL, -- Reference to actual client
    segment_code varchar(32) NOT NULL, -- Client segment code
    country_code char(2) NOT NULL, -- Client country code
    effective_from timestamptz NOT NULL, -- Valid from timestamp
    effective_to timestamptz, -- Valid to timestamp for SCD Type 2
    is_current boolean NOT NULL, -- Flag for current dimension record
    CHECK (effective_to IS NULL OR effective_to > effective_from)
);
 
CREATE TABLE IF NOT EXISTS analytics.dim_instrument (
    instrument_key bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY, -- Instrument surrogate key
    instrument_id uuid NOT NULL UNIQUE, -- Reference to actual instrument
    symbol varchar(40) NOT NULL, -- Trading symbol
    name varchar(160) NOT NULL, -- Instrument name
    asset_class varchar(16) NOT NULL, -- Asset class
    venue_code varchar(20) NOT NULL, -- Trading venue
    quote_currency_code char(3) NOT NULL -- Quote currency
);
 
CREATE TABLE IF NOT EXISTS analytics.fact_orders (
    order_id uuid PRIMARY KEY, -- Reference to order
    date_key integer NOT NULL REFERENCES analytics.dim_date(date_key), -- Date dimension key
    client_key bigint NOT NULL REFERENCES analytics.dim_client(client_key), -- Client dimension key
    instrument_key bigint NOT NULL REFERENCES analytics.dim_instrument(instrument_key), -- Instrument dimension key
    submitted_at timestamptz NOT NULL, -- Order submission timestamp
    side varchar(4) NOT NULL CHECK (side IN ('BUY', 'SELL')), -- Order side
    quantity numeric(28,10) NOT NULL CHECK (quantity > 0), -- Order quantity
    final_status varchar(20) NOT NULL CHECK (final_status IN ('FILLED', 'REJECTED')), -- Final order status
    acceptance_ms integer CHECK (acceptance_ms >= 0), -- Milliseconds to acceptance
    completion_ms integer CHECK (completion_ms >= 0) -- Milliseconds to completion
);
 
CREATE TABLE IF NOT EXISTS analytics.fact_fills (
    fill_id uuid PRIMARY KEY, -- Reference to fill
    order_id uuid NOT NULL, -- Reference to order
    date_key integer NOT NULL REFERENCES analytics.dim_date(date_key), -- Date dimension key
    client_key bigint NOT NULL REFERENCES analytics.dim_client(client_key), -- Client dimension key
    instrument_key bigint NOT NULL REFERENCES analytics.dim_instrument(instrument_key), -- Instrument dimension key
    executed_at timestamptz NOT NULL, -- Execution timestamp
    quantity numeric(28,10) NOT NULL CHECK (quantity > 0), -- Filled quantity
    price numeric(28,10) NOT NULL CHECK (price >= 0), -- Execution price
    gross_amount numeric(28,10) NOT NULL CHECK (gross_amount >= 0), -- Gross transaction amount
    fee_amount numeric(28,10) NOT NULL DEFAULT 0 CHECK (fee_amount >= 0), -- Fee amount
    currency_code char(3) NOT NULL -- Currency of fill
);
 
CREATE INDEX IF NOT EXISTS ix_sessions_client
    ON trading.sessions (client_id); -- speeds up looking up a client's sessions
CREATE INDEX IF NOT EXISTS ix_accounts_client
    ON trading.accounts (client_id); -- speeds up looking up a client's accounts
CREATE INDEX IF NOT EXISTS ix_quotes_instrument_observed
    ON trading.market_quotes (instrument_id, observed_at DESC); -- fetch latest quotes per instrument
CREATE INDEX IF NOT EXISTS ix_orders_client_submitted
    ON trading.orders (client_id, submitted_at DESC); -- fetch a client's order history in recency order
CREATE INDEX IF NOT EXISTS ix_order_events_order_sequence
    ON trading.order_events (order_id, sequence_no); -- replay an order's event history in order
CREATE INDEX IF NOT EXISTS ix_fills_order_executed
    ON trading.fills (order_id, executed_at); -- list an order's fills in execution order
CREATE INDEX IF NOT EXISTS ix_position_ledger_account_occurred
    ON trading.position_ledger (account_id, occurred_at); -- reconstruct an account's position history over time
CREATE INDEX IF NOT EXISTS ix_cash_ledger_account_occurred
    ON trading.cash_ledger (account_id, occurred_at); -- reconstruct an account's cash movement history over time
CREATE INDEX IF NOT EXISTS ix_audit_events_entity
    ON trading.audit_events (entity_type, entity_id, occurred_at); -- audit trail lookup for a specific entity
CREATE INDEX IF NOT EXISTS ix_outbox_unpublished
    ON trading.outbox_events (created_at)
    WHERE published_at IS NULL; -- partial index for the publisher's polling query on pending events
CREATE INDEX IF NOT EXISTS ix_dim_client_lookup
    ON analytics.dim_client (client_id, effective_from, effective_to); -- resolve the SCD Type 2 record valid at a point in time
CREATE INDEX IF NOT EXISTS ix_fact_orders_date
    ON analytics.fact_orders (date_key, client_key, instrument_key); -- support BI aggregation/drill-down by date, client, instrument
CREATE INDEX IF NOT EXISTS ix_fact_fills_date
    ON analytics.fact_fills (date_key, client_key, instrument_key); -- support BI aggregation/drill-down by date, client, instrument
 
COMMIT;