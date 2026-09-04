# Database Plan — Week 1

Use PostgreSQL. IDs are `uuid`, timestamps are UTC `timestamptz`, quantities are `numeric(28,10)`.

This is deliberately the smallest schema that satisfies Week 1's two DB stories (`W1-2`, `W1-3`
in `project-plan.md`) — not the platform's final data model. The full future shape is sketched
below and should grow one week at a time, in step with the plan, rather than being built upfront.

The schema itself lives in [`db/migrations/V1__init.sql`](db/migrations/V1__init.sql), a
Flyway-style migration, per the repo layout `project-plan.md` calls for.

## Week 1 tables

| Table | Columns | Story |
|---|---|---|
| `clients` | `client_id` PK, `email` UNIQUE, `display_name`, `status`, `created_at`, `updated_at` | FK target for orders/audit |
| `accounts` | `account_id` PK, `client_id` FK, `status`, `opened_at` | FK target for orders |
| `instruments` | `instrument_id` PK, `symbol` UNIQUE, `name`, `asset_class` (`EQUITY`/`FX`/`CRYPTO`), `status` | FK target for orders |
| `orders` | `order_id` PK, `client_id` FK, `account_id` FK, `instrument_id` FK, `side`, `quantity`, `idempotency_key`, `status`, `submitted_at`; UNIQUE (`client_id`, `idempotency_key`) | W1-3 (BR-06/BR-09) |
| `audit_events` | `audit_event_id` PK, `client_id` FK, `entity_type`, `entity_id`, `action`, `occurred_at`, `details` JSONB; `UPDATE`/`DELETE` revoked from `PUBLIC` | W1-2 (BR-14) |
| `admin_users` | `admin_user_id` PK, `email` UNIQUE, `display_name`, `role` (`ADMIN`/`ANALYST`), `status`, `created_at`, `updated_at` | W5-4 (admin/client role split, enabled early) |
| `fills` | `fill_id` PK, `order_id` FK, `price`, `quantity`, `executed_at` | Records the price a client traded at, for later up/down comparison |

## Reasoning

- `clients`, `accounts`, and `instruments` exist only as minimal FK targets — `orders` needs
  something to reference. No sessions, currencies, segments, or asset-class distinctions yet.
- `orders` carries only what W1-3 needs to demonstrate: a unique idempotency key per client, and
  a basic status. No order type, pricing snapshot, rejection code, or optimistic-lock version
  until pricing/validation work (weeks 2–4) actually needs them.
- `audit_events` is append-only from day one (`REVOKE UPDATE, DELETE`), satisfying W1-2 directly.
  It also records order status transitions (`entity_type='ORDER'`, `details` holds
  `from_status`/`to_status`) rather than a separate `order_events` table — the two were
  redundant (both "record an action against an entity, attributably"), and folding status
  transitions into `audit_events` still satisfies BR-15's lifecycle-reconstruction need while
  keeping the append-only guarantee in one place.
- `admin_users` is added early as a bare identity table, separate from `clients`, so the
  admin/reporting API seam (W3-6) and the eventual role check (W5-4) have a real table to attach
  to. It isn't wired into `audit_events` yet — that attribution lands with the RBAC work.
- `fills` records only what a client actually paid (`price`, `quantity`, `executed_at`) — no
  pricing-decision snapshot, currency, gross/fee amounts, or external execution reference yet.
  This is enough to compare a client's cost basis against a current quote once `market_quotes`
  exists, without pulling in the full pricing engine ahead of Weeks 3–4.
- `instruments.asset_class` (`EQUITY`/`FX`/`CRYPTO`) is added now since it's a static, load-bearing
  fact about an instrument — it doesn't depend on any pricing/quantity-scale work still deferred.
- `analytics` exists only as an empty schema for now (`CREATE SCHEMA`), not yet any tables.
  `dim_date` was considered but dropped: Postgres can derive month/quarter/year from a
  `timestamptz` with `date_trunc()`/`EXTRACT()` at query time, and a date dimension only earns
  its keep once a fact table exists to join it to and/or the business needs a non-standard
  calendar (fiscal quarters, trading days). Revisit in Week 4 if that need materialises.
- Everything else from the original design is deferred, not discarded — see below.

## Deferred, and when to bring it back

Reintroduce each of these when its owning week starts, per `project-plan.md`'s traceability table:

- **Week 2** (BR-05 rule checks before acceptance): `order_validations`, `order_events` (a
  dedicated status-transition table may still be worth reintroducing here if `audit_events`
  proves too generic for replaying an order's state machine specifically).
- **Weeks 3–4** (BR-08/09 pricing, atomic settlement, holdings/cash): `currencies`,
  `market_quotes`, `pricing_decisions`, `positions`, `position_ledger`,
  `cash_balances`, `cash_ledger` (`fills` now exists in trimmed form, see above).
- **Week 4** (BR-16 reporting isolated from live trading): the `analytics` schema's tables
  (`dim_date`, `dim_client`, `dim_instrument`, `fact_orders`, `fact_fills`), populated from an
  outbox once Kafka is wired up — not before.
- **Week 5** (BR-01/03 auth/sessions): decide then whether `sessions` lives here or entirely in
  the NestJS auth service's own store.

The previous full-platform draft (all of the above, already designed) is kept as
[`db/.bak/database-init.full.sql.bak`](db/.bak/database-init.full.sql.bak) /
[`db/.bak/database-plan.full.md.bak`](db/.bak/database-plan.full.md.bak) for reference when each
week arrives, so none of that thinking is lost — it's just not live schema until its story needs it.
