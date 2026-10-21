# Risk Register — Direct Trading Platform

**Audience:** Programme Sponsor, Risk & Compliance, delivery team. Section 12 item 3. A living document:
update the status column as risks change; add new risks at the bottom.

Likelihood / impact: H = high, M = medium, L = low.

| # | Risk | L | I | Mitigation | Status |
|---|---|---|---|---|---|
| R1 | **Market-data provider coverage.** Fauxnance has no London market, so UK equities (BR-12) cannot be priced directly. | H | M | UK companies offered through US listings; LSE instruments loaded inactive, ready to switch on. Decision needed on a UK data source. | Open — Sponsor decision |
| R2 | **Market-data quota.** Fauxnance allows 2,000 requests per day per key; heavy use could exhaust it and stop pricing. | M | H | Quotes cached 60 s; batch quote calls (25 symbols per request); FX rates cached. If the quota is hit, orders are rejected ("no current market price") rather than mispriced. Monitor `/usage`. | Mitigated, monitor |
| R3 | **Delayed / educational prices.** Quotes may be minutes old; not suitable for real money. | H | M | Each trade records the quote's own timestamp and source. A production data feed is required before live client money. | Accepted for this phase |
| R4 | **Currency conversion.** Non-USD instruments depend on a live FX rate; a missing or wrong rate would misprice trades. | M | H | No rate means no quote, so the order is rejected, never priced in the wrong currency. Local price, currency and rate stored with every trade for audit. | Mitigated |
| R5 | **Execution after an outage.** Accepted orders left unexecuted by a restart could fill at a stale time. | M | H | Recovery sweep finishes orders within two minutes; older orders are rejected as expired with a recorded reason. | Mitigated |
| R6 | **Double execution / overspend** with several API instances. | L | H | Database allows one outcome per order; execution locks the account row; cash debit is conditional on sufficient balance. | Mitigated |
| R7 | **Reporting load affecting trading** (BR-16). | M | M | Reporting reads analytics facts updated by a background projector, not the trading transaction. Next step at scale: read replica. | Mitigated |
| R8 | **Third-party news provider** (Finnhub free tier) is US-centric and rate-limited. | M | L | News is off the trading path, refreshed hourly, and the UI says when nothing recent is available. | Accepted |
| R9 | **Credential compromise.** | L | H | BCrypt hashing, 15-minute tokens, server-side session revocation, HTTPS enforcement switch for production, admin access logging. Production signing keys must be configured (development uses generated keys). | Mitigated — production key setup required |
| R10 | **Single-region, single-database deployment** affects availability during trading hours (9.1). | M | H | Containerised deployment with versioned migrations; recommend managed Postgres with automated backups and a standby before launch. | Open — infrastructure decision |
| R11 | **Market hours and holidays.** Sessions are enforced per market but exchange holidays are not modelled. | M | L | Orders on a holiday are priced at the last available quote; add exchange holiday calendars before launch. | Open |
| R12 | **Team knowledge concentration** in execution, pricing and FX conversion. | M | M | Decision log and architecture notes; pair rotation per the delivery plan; unit tests around pricing, limits and market sessions. | Mitigated |
| R13 | **Market depth panel shows generated levels**, not real data, in the trade popup. Undermines "trust what she sees on screen" (Joanna persona, 9.1). | H | M | Label it as simulated, remove it, or replace it with the platform's own order book (decision D13). | Open — decision needed before showcase |
