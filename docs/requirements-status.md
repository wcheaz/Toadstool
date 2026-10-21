# Requirements Status — LEAP-BRS-2026-014

**Audience:** Programme Sponsor and stakeholders. Status of each requirement in Sections 8 and 9, with where
it is met. "Met" means built and verified; open items are listed at the end.

## Section 8

| Ref | Priority | Status | How it is met |
|---|---|---|---|
| BR-01 Register and sign in | Must | Met | Registration and sign-in; BCrypt-hashed passwords. |
| BR-02 Own data only | Must | Met | Every account, order, fill and cash endpoint checks the caller owns the account. |
| BR-03 Time-limited, revocable sessions | Should | Met | 15-minute signed tokens backed by server-side sessions, revoked on sign-out. |
| BR-04 Submit buy/sell orders | Must | Met | Order ticket; market and limit orders. |
| BR-05 Trading rules before acceptance | Must | Met | Account active, instrument tradable, its market open, cash for buys, units for sells (net of pending sells). |
| BR-06 Commitment recorded before execution | Must | Met | Order recorded as ACCEPTED, then executed separately by the server; recovery sweep after restarts. |
| BR-07 Status changes without refresh | Should | Met | Order ticket and blotter follow ACCEPTED → FILLED / REJECTED automatically. |
| BR-08 Priced against a current quote at execution | Must | Met | Server prices at execution (buy at ask, sell at bid, limits enforced); clients cannot set the price. |
| BR-09 Holdings, cash and record update together | Must | Met | One transaction under an account lock; one outcome per order enforced by the database. |
| BR-10 Holdings and cash | Must | Met | Dashboard holdings and cash, correct as of the last fill. |
| BR-11 Order and fill history | Must | Met | Activity blotter, chronological, with rejection reasons. |
| BR-12 Equities (UK/US/India), FX, crypto | Must | **Partly met** | US, Indian (NSE), FX and crypto tradable, priced in USD via live FX. **UK: the market-data provider has no London market**; UK companies are tradable through their US listings, LSE listings are loaded but inactive. See open items. |
| BR-13 Indicative price before submitting | Should | Met | Live quote on the ticket and confirmation. |
| BR-14 Permanent, attributable record | Must | Met | Orders, fills, cash events and trade events are insert-only at the database level, with client and time. |
| BR-15 Reconstruct a trade's lifecycle | Must | Met | Trade events: submitted → accepted → priced (quote snapshot, FX rate) → filled or rejected (reason). |
| BR-16 Analysis without competing with trading | Must | Met | Reporting reads analytics facts maintained by a background projector, off the trading transaction. |
| BR-17 Business insights | Should | Met | Admin insights: volumes, most active instruments, buy/sell balance, client activity. |
| BR-18 Additional capability, justified | Should | **Awaiting sign-off** | Market context linked to assets; see [`BR-18-differentiation.md`](BR-18-differentiation.md). |

## Section 9

| Expectation | Status | Notes |
|---|---|---|
| 9.1 Trust and continuity | Met | Atomic fills, one outcome per order, immutable records, restart recovery. |
| 9.2 Responsiveness | Met | Fill typically ~1 second after submission, shown live. |
| 9.3 Security | Met | Role-based admin access; admin access to client data is logged. |
| 9.4 Retention | Met | Insert-only trade record; versioned schema. |
| 9.5 Usability | Met, to be confirmed | Plain-language outcomes; recommend a usability check with first-time users. |

## Open items

1. **UK market data (BR-12).** Fauxnance provides no London market. Options for the Sponsor: accept US
   listings of UK companies for this phase, or fund a UK data source. If Fauxnance adds London, an admin
   switches the inactive LSE instruments on; a different provider needs a small market-data adapter.
2. **BR-18 sign-off.** Confirm the chosen capability and its justification.
3. **Usability check (9.5).** Observe two or three first-time users placing an order unaided.
4. **Market depth panel (9.1).** The trade popup's depth ladder is generated, not live data; label, remove or
   replace it before the showcase (decision D13, risk R13).
