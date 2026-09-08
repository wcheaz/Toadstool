# Database Plan — Week 1

Use PostgreSQL. IDs are `uuid`, timestamps are UTC `timestamptz`, quantities are `numeric(28,10)`.

This is deliberately the smallest schema that satisfies Week 1's two DB stories (`W1-2`, `W1-3`
in `project-plan.md`) — not the platform's final data model. The full future shape is sketched
below and should grow one week at a time, in step with the plan, rather than being built upfront.

The schema itself lives in [`db/migrations/V1__init.sql`](db/migrations/V1__init.sql), a
Flyway-style migration, per the repo layout `project-plan.md` calls for. For now the script drops
and recreates both schemas on every run (`DROP SCHEMA ... CASCADE`), so anyone can re-run it to
pick up changes without hand-managing diffs — this goes away once Flyway (W1-4) takes over
versioned migrations that apply once each, in order.

## Week 1 tables

| Table | Columns | Story |
|---|---|---|
| `clients` | `client_id` PK, `email` UNIQUE, `display_name`, `status`, `created_at`, `updated_at` | FK target for orders/audit |
| `accounts` | `account_id` PK, `client_id` FK, `status`, `opened_at` | FK target for orders |
| `instruments` | `instrument_id` PK, `symbol` UNIQUE, `name`, `asset_class` (`EQUITY`/`FX`/`CRYPTO`), `status` | FK target for orders |
| `orders` | `order_id` PK, `client_id` FK, `account_id` FK, `instrument_id` FK, `side`, `quantity`, `idempotency_key`, `status`, `submitted_at`; UNIQUE (`client_id`, `idempotency_key`) | W1-3 (BR-06/BR-09) |
| `admin_users` | `admin_user_id` PK, `email` UNIQUE, `display_name`, `role` (`ADMIN`/`ANALYST`), `status`, `created_at`, `updated_at` | W5-4 (admin/client role split, enabled early) |
| `fills` | `fill_id` PK, `order_id` FK, `price`, `quantity`, `executed_at`; `UPDATE`/`DELETE` revoked from `PUBLIC` | Records the price a client traded at, for later up/down comparison |
| `trade_events` | `trade_event_id` PK, `client_id` FK, `entity_type` (`ORDER`/`FILL`), `entity_id`, `action`, `occurred_at`, `details` JSONB; `UPDATE`/`DELETE` revoked from `PUBLIC` | W1-2 (BR-14/BR-15) |
| `login_events` | `login_event_id` PK, `client_id` FK, `email_attempted`, `outcome` (`SUCCESS`/`FAILURE`), `occurred_at`, `details` JSONB; `UPDATE`/`DELETE` revoked from `PUBLIC` | W1-2 (BR-14), enabler for BR-03 |
| `analytics.fact_daily_instrument_activity` | `activity_date` PK, `instrument_id` PK/FK, `order_count`, `filled_quantity`, `gross_amount`, `updated_at` | BR-16 (reporting reads off the live path) |
| `analytics.fact_daily_platform_activity` | `activity_date` PK, `order_count`, `filled_quantity`, `gross_amount`, `updated_at` | BR-16/BR-17 (platform-wide business insight) |

## Immutability

Tables split into two kinds:

- **Current-state tables** (`clients`, `accounts`, `instruments`, `orders`, `admin_users`) are
  ordinary mutable OLTP rows — a client's `status`, an order's `status`, etc. genuinely change in
  place. Turning these into pure event logs would mean full event-sourcing (deriving current
  state from a replayed stream), which is a bigger redesign than Week 1 needs.
- **Log tables** (`fills`, `trade_events`, `login_events`) are insert-only by design: a row is
  written once and never changes, so each has `REVOKE UPDATE, DELETE ON ... FROM PUBLIC`
  enforced at the database level, not just by convention.

## Reasoning

- `clients`, `accounts`, and `instruments` exist only as minimal FK targets — `orders` needs
  something to reference. No sessions, currencies, segments, or asset-class distinctions yet.
- `orders` carries only what W1-3 needs to demonstrate: a unique idempotency key per client, and
  a basic status. No order type, pricing snapshot, rejection code, or optimistic-lock version
  until pricing/validation work (weeks 2–4) actually needs them.
- `trade_events` and `login_events` replace the earlier single `audit_events` table, split by
  concern per your request: one covers order/fill activity, the other covers login attempts.
  Both stay append-only (`REVOKE UPDATE, DELETE`), satisfying W1-2 (BR-14) directly. `trade_events`
  also records order status transitions (`entity_type='ORDER'`, `details` holds
  `from_status`/`to_status`) rather than a separate `order_events` table — folding status
  transitions in still satisfies BR-15's lifecycle-reconstruction need while keeping the
  append-only guarantee in one place.
- A fill's two-part lifecycle — requested, then executed — is captured as two `trade_events` rows
  (`action='FILL_REQUESTED'` when pricing starts, `action='FILL_EXECUTED'` once the `fills` row is
  written) rather than mutable columns on `fills` itself. This keeps `fills` genuinely insert-only:
  a row only ever gets written once, when the trade has actually executed.
- `admin_users` is a bare identity table, separate from `clients`, so the admin/reporting API seam
  (W3-6) and the eventual role check (W5-4) have a real table to attach to. It stays in `trading`
  rather than its own schema: a schema was considered for it, but Postgres `GRANT`/`REVOKE` works
  per-table regardless of schema, and it's the only genuinely admin-owned table the plan calls
  for — everything else the admin dashboard reads (order lookup, business insights) is read-only
  access to existing `trading`/`analytics` data, not new admin-owned tables. `admin_users` isn't
  wired into `trade_events`/`login_events` yet — that attribution lands with the RBAC work.
- `fills` records only what a client actually paid (`price`, `quantity`, `executed_at`) — no
  pricing-decision snapshot, currency, gross/fee amounts, or external execution reference yet.
  This is enough to compare a client's cost basis against a current quote once `market_quotes`
  exists, without pulling in the full pricing engine ahead of Weeks 3–4.
- `login_events.client_id` is nullable because a failed login attempt (wrong email/unknown user)
  may never resolve to a real client; `email_attempted` is kept regardless so failed attempts are
  still reviewable.
- `instruments.asset_class` (`EQUITY`/`FX`/`CRYPTO`) is added now since it's a static, load-bearing
  fact about an instrument — it doesn't depend on any pricing/quantity-scale work still deferred.
- `analytics.fact_daily_instrument_activity` and `analytics.fact_daily_platform_activity` are the
  schema's first two tables, both scoped to "everyone" rather than any one client: the first is a
  daily rollup per instrument, summed across every client's activity on it; the second is a daily
  rollup with no breakdown at all, summed across every client and instrument. Neither has a
  per-client dimension — there's deliberately no `fact_daily_client_activity` — and neither uses
  surrogate-key dimension tables (`dim_instrument`, `dim_client`, `dim_date`). This satisfies
  BR-16 (reads land here, not on live trading tables) and BR-17 (a first, simplest business
  insight) without the full star-schema modelling, which is deferred to Week 4. There's no
  ETL/population job yet — that's Week 4 work once the outbox/Kafka path exists — this is schema
  only.
- Everything else from the original design is deferred, not discarded — see below.

## Deferred, and when to bring it back

Reintroduce each of these when its owning week starts, per `project-plan.md`'s traceability table:

- **Week 2** (BR-05 rule checks before acceptance): `order_validations`, `order_events` (a
  dedicated status-transition table may still be worth reintroducing here if `audit_events`
  proves too generic for replaying an order's state machine specifically).
- **Weeks 3–4** (BR-08/09 pricing, atomic settlement, holdings/cash): `currencies`,
  `market_quotes`, `pricing_decisions`, `positions`, `position_ledger`,
  `cash_balances`, `cash_ledger` (`fills` now exists in trimmed form, see above).
- **Week 4** (BR-16 reporting isolated from live trading): the full star-schema tables
  (`dim_date`, `dim_client`, `dim_instrument`, `fact_orders`, `fact_fills`) if per-client or
  per-order granularity turns out to be needed beyond the platform-wide daily summaries already
  in place, populated from an outbox once Kafka is wired up — not before.
- **Week 5** (BR-01/03 auth/sessions): decide then whether `sessions` lives here or entirely in
  the NestJS auth service's own store.

The previous full-platform draft (all of the above, already designed) is kept as
[`db/.bak/database-init.full.sql.bak`](db/.bak/database-init.full.sql.bak) /
[`db/.bak/database-plan.full.md.bak`](db/.bak/database-plan.full.md.bak) for reference when each
week arrives, so none of that thinking is lost — it's just not live schema until its story needs it.
