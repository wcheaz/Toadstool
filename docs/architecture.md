# Architecture — Direct Trading Platform (LEAP-BRS-2026-014)

**Audience:** Programme Sponsor, Trading Desk Operations, Risk & Compliance, Finance.
**Purpose:** Section 12 item 1 — the candidate architecture, the technologies used, and why it meets
Sections 8 and 9. Delivery sequencing is in [`project-plan.md`](../project-plan.md); requirement-by-requirement
status is in [`requirements-status.md`](requirements-status.md); risks in [`risks.md`](risks.md); decisions and
changes in [`decision-log.md`](decision-log.md).

## 1. Shape of the platform

```
 Browser (desktop)                     Spring Boot API (one deployable)                    PostgreSQL
 ┌───────────────────┐   HTTPS/JWT    ┌──────────────────────────────────────────┐      ┌──────────────────────┐
 │ Client app        │ ─────────────▶ │ Auth & access   (sign-in, sessions)      │      │ trading schema       │
 │ (Angular)         │                │ Orders          (accept, validate)  ───────────▶ │  clients, accounts   │
 │  • order ticket   │ ◀── status ─── │ Execution worker(price + fill)   ────────────▶ │  orders, fills  ◀─ insert-only
 │  • holdings/blotter│               │ Holdings/cash   (positions, cash events)│      │  cash_events, trade_events
 │  • news, charts   │                │ Market data     (quotes, FX → USD)       │      │  holdings            │
 └───────────────────┘                │ News & calendar (hourly job)             │      ├──────────────────────┤
 ┌───────────────────┐                │ Admin/reporting (role ADMIN)  ◀──────────────── │ analytics schema     │
 │ Admin app         │ ─────────────▶ │ Analytics projector (every 10s) ──────────────▶ │  daily facts         │
 │ (same Angular,    │                └──────────────────────────────────────────┘      └──────────────────────┘
 │  admin routes)    │                        │                     │
 └───────────────────┘                  Fauxnance API          Finnhub API
                                     (quotes, candles, FX)   (news, earnings)
```

- **Front end:** Angular 22 single-page app. Clients and administrators use separate routes and sign-in
  flows; access is enforced by the API, never by the UI alone.
- **Back end:** one Spring Boot 3 service (Java 21 runtime) with MyBatis for SQL and Flyway for versioned
  schema migrations. Packaged as a Docker image; a Jenkins pipeline builds and smoke-tests it.
- **Database:** PostgreSQL 16. The `trading` schema is the system of record; the `analytics` schema holds
  reporting facts derived from it.
- **External data:** Fauxnance (educational, delayed market data: quotes, daily candles, FX) and Finnhub
  (company news and earnings calendar).

## 2. How an order flows (BR-04 to BR-09)

1. **Accept.** The client submits buy/sell, quantity, and optionally a limit price. The API checks the
   firm's rules — account active, instrument tradable, *its* market open, enough cash (BUY) or enough units
   after other pending sells (SELL) — and records the order. The order is now a firm commitment (`ACCEPTED`),
   written before any execution happens (BR-05, BR-06).
2. **Execute.** A server-side worker prices the order against the live quote — BUY at the ask, SELL at the
   bid, a LIMIT order only at its limit or better — and either fills or rejects it (BR-08). The client cannot
   influence the price.
3. **Settle atomically.** A fill moves cash, holdings and the fill record in one database transaction, under
   a lock on the client's account: all three change or none do (BR-09).
4. **Record.** Every step is appended to the trade record: submitted, accepted, priced (with the quote it was
   priced against), filled or rejected (with the reason) (BR-14, BR-15).
5. **Follow.** The client's screen shows the order as accepted, then filled or rejected, without refreshing
   (BR-07). Typical time from submission to fill is about one second.

If the service restarts mid-way, a recovery sweep finishes accepted orders, and rejects any that waited more
than two minutes rather than filling them at a stale price.

## 3. Why this meets Section 9

| Expectation | How the design meets it |
|---|---|
| **9.1 Trust and continuity** | Balances, holdings and fills change in one transaction. Each order can have only one outcome (a database constraint), so a trade can never be duplicated, even with several API instances. Orders, fills, cash events and trade events cannot be edited or deleted (database triggers). Recovery sweep after restarts. |
| **9.2 Responsiveness** | Acceptance is immediate; execution normally completes in about a second, and the client sees it happen. |
| **9.3 Security and confidentiality** | Passwords hashed with BCrypt. Short-lived signed tokens (15 minutes) backed by server-side sessions that are revoked on sign-out. Every account endpoint checks the caller owns the account; admin endpoints require the ADMIN role, and admin access to client records is itself logged. |
| **9.4 Retention and regulatory alignment** | The trade record is insert-only at the database level, carries the client, time and pricing evidence, and survives restarts and failed deployments. Schema changes are versioned migrations. |
| **9.5 Usability** | Single order ticket with a confirmation step and plain-language outcomes ("The market price ($231.40) was above your limit of $230.00"). |

## 4. Market data and pricing (BR-12, BR-13)

- **Instruments:** US equities; Indian equities (NSE); UK companies via their US listings; FX pairs; crypto.
- **One account currency.** Accounts hold USD. Quotes and charts in other currencies (INR, pence, the quote
  currency of an FX pair) are converted with live FX rates; the local price and rate are kept with each trade.
- **Market hours per market:** crypto 24/7; FX Sunday–Friday 17:00 New York; India 09:15–15:30 IST;
  UK 08:00–16:30 London; US 09:30–16:00 New York.
- **Indicative price** is shown on the order ticket and confirmation before submission (BR-13).
- **Limits of the data:** Fauxnance is educational and delayed; quotes are cached for up to 60 seconds and
  every trade records the quote's own timestamp. Fauxnance has no London market (see risks).

## 5. Reporting separated from trading (BR-16, BR-17)

- Reporting reads the `analytics` daily fact tables (activity by day and by instrument, buy/sell split, fees).
- A background projector folds new fills into those facts every ten seconds. A trade's transaction writes
  only trading data, so reporting load and reporting failures cannot slow or block trading.
- Reports trail live trading by up to ten seconds. Only small point-in-time counts (number of clients,
  accounts, total cash, open orders) read the live tables.
- **Next step if volumes grow:** point reporting at a read replica; the facts and queries move unchanged.

## 6. Extensibility (Objective 4)

- Versioned migrations (Flyway) let new capabilities add schema without rebuilding what exists.
- Market data, pricing, execution, news and reporting are separate services in the code, so a new instrument
  class, data provider or report changes one component.
- Example: adding Indian equities and FX needed a currency-conversion layer in the market-data service and a
  data migration — the order, execution and reporting code did not change.

## 7. Additional capability (BR-18)

Market context linked to the client's assets (news, earnings calendar). Justification and alternatives:
[`BR-18-differentiation.md`](BR-18-differentiation.md).
