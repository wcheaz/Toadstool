# Recommended Database Schema
 
Use PostgreSQL. IDs are `uuid`, timestamps are UTC `timestamptz`, and money/quantities are `numeric(28,10)`.
 
## Transactional Tables
 
| Table | Columns |
|---|---|
| `clients` | `client_id` PK, `identity_subject` UNIQUE, `email` UNIQUE, `display_name`, `country_code`, `segment_code`, `status`, `created_at`, `updated_at` |
| `sessions` | `session_id` PK, `client_id` FK, `token_hash` UNIQUE, `created_at`, `expires_at`, `last_seen_at`, `revoked_at`, `revoke_reason` |
| `currencies` | `currency_code` PK, `name`, `decimal_places`, `status` |
| `accounts` | `account_id` PK, `client_id` FK, `account_number` UNIQUE, `base_currency_code` FK, `status`, `opened_at`, `closed_at` |
| `instruments` | `instrument_id` PK, `asset_class`, `symbol`, `venue_code`, `name`, `base_currency_code` FK, `quote_currency_code` FK, `quantity_scale`, `price_scale`, `status`, `created_at`, `updated_at`; UNIQUE (`asset_class`, `symbol`, `venue_code`) |
| `market_quotes` | `quote_id` PK, `instrument_id` FK, `bid_price`, `ask_price`, `provider_code`, `provider_quote_ref`, `observed_at`, `received_at`, `expires_at` |
| `orders` | `order_id` PK, `client_id` FK, `account_id` FK, `instrument_id` FK, `side`, `order_type`, `quantity`, `indicative_quote_id` FK, `idempotency_key`, `submitted_at`, `accepted_at`, `current_status`, `status_updated_at`, `rejection_code`, `version`; UNIQUE (`client_id`, `idempotency_key`) |
| `order_validations` | `validation_id` PK, `order_id` FK, `rule_code`, `rule_version`, `outcome`, `reason`, `facts` JSONB, `evaluated_at` |
| `order_events` | `order_event_id` PK, `order_id` FK, `sequence_no`, `from_status`, `to_status`, `reason_code`, `occurred_at`, `actor_type`, `correlation_id`; UNIQUE (`order_id`, `sequence_no`) |
| `pricing_decisions` | `pricing_decision_id` PK, `order_id` FK, `attempt_no`, `quote_id` FK, `bid_snapshot`, `ask_snapshot`, `selected_price`, `decision`, `reason_code`, `decided_at`; UNIQUE (`order_id`, `attempt_no`) |
| `fills` | `fill_id` PK, `order_id` FK, `pricing_decision_id` FK, `external_execution_ref` UNIQUE, `quantity`, `price`, `currency_code` FK, `gross_amount`, `fee_amount`, `executed_at`, `recorded_at` |
| `positions` | `account_id` PK/FK, `instrument_id` PK/FK, `quantity`, `version`, `updated_at` |
| `position_ledger` | `position_movement_id` PK, `account_id` FK, `instrument_id` FK, `fill_id` FK UNIQUE, `quantity_delta`, `quantity_after`, `occurred_at`, `recorded_at` |
| `cash_balances` | `account_id` PK/FK, `currency_code` PK/FK, `amount`, `version`, `updated_at` |
| `cash_ledger` | `cash_movement_id` PK, `account_id` FK, `currency_code` FK, `fill_id` FK, `movement_type`, `amount_delta`, `balance_after`, `occurred_at`, `recorded_at`, `reason` |
| `audit_events` | `audit_event_id` PK, `actor_type`, `actor_id`, `client_id` FK, `action`, `entity_type`, `entity_id`, `occurred_at`, `correlation_id`, `details` JSONB |
| `outbox_events` | `event_id` PK, `aggregate_type`, `aggregate_id`, `event_type`, `payload` JSONB, `occurred_at`, `created_at`, `published_at`, `attempt_count`, `last_error` |
 
## Reporting Tables
 
Keep these in a separate `analytics` schema or database, populated from `outbox_events`.
 
| Table | Columns |
|---|---|
| `dim_date` | `date_key` PK, `calendar_date` UNIQUE, `month_no`, `quarter_no`, `year_no` |
| `dim_client` | `client_key` PK, `client_id`, `segment_code`, `country_code`, `effective_from`, `effective_to`, `is_current` |
| `dim_instrument` | `instrument_key` PK, `instrument_id`, `symbol`, `name`, `asset_class`, `venue_code`, `quote_currency_code` |
| `fact_orders` | `order_id` PK, `date_key` FK, `client_key` FK, `instrument_key` FK, `submitted_at`, `side`, `quantity`, `final_status`, `acceptance_ms`, `completion_ms` |
| `fact_fills` | `fill_id` PK, `order_id`, `date_key` FK, `client_key` FK, `instrument_key` FK, `executed_at`, `quantity`, `price`, `gross_amount`, `fee_amount`, `currency_code` |
 
## Reasoning
 
- Keep the clear core from response one: clients, accounts, instruments, quotes, orders, fills, positions, cash, and audit history.
- Keep orders separate from fills: an order is an instruction; a fill is an execution. One order may eventually have multiple fills.
- Use `order_events` instead of a simple status-history table, and retain validation and pricing evidence so every trade can be reconstructed.
- Keep current `positions` and `cash_balances` for fast reads, backed by append-only ledgers for reconciliation and recovery.
- Keep idempotency keys, version columns, and the outbox so retries and asynchronous processing cannot duplicate trades or lose events.
- Use a managed identity provider; do not store password hashes here unless authentication is intentionally built in-house.
- Keep reporting isolated from live trading tables, but start with this small read model rather than a full data warehouse.
- Defer watchlists and price alerts until the required trade lifecycle is complete. Avoid a generic `transactions` table because explicit cash and position ledgers are easier to validate and audit.