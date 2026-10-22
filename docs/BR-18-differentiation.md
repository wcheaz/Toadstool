# BR-18 — Additional Capability: Market Context Linked to the Client's Assets

**Requirement (BR-18, Should):** identify and build one additional capability that extends the platform's
value to clients or the business, with a stated justification for why it was chosen over other options.

**Status:** Draft for Programme Sponsor review.

## The capability

Clients see the market news and upcoming company events that matter to *the assets they hold or are
looking at*, in the same place they trade:

- **News linked to assets.** Live headlines (Finnhub) are tagged by instrument. Selecting an asset on the
  dashboard or trading desk shows its latest news; the news page ranks assets by how much they are in the news.
- **Earnings calendar.** Upcoming earnings for tradable equities, marked HIGH impact within 3 days and
  MEDIUM within a week, so a client knows before trading that a price may move sharply.
- **Honest "nothing new" signal.** When there is no recent news, the platform says so rather than showing
  stale headlines as if they were current.

## Why this capability

**It serves Joanna directly.** The persona "checks positions in the evening and around news events" and
"wants to place a trade in under a minute and trust what she sees." Today she would leave the platform to
find out *why* an asset moved, then come back to trade. Bringing that context onto the trading screen
shortens the journey and supports the decision she is about to make, without adding steps to the order flow
(Section 9.5).

**It does not touch the trading path.** News and earnings are collected by an hourly background job and
read from their own tables. If the news provider is slow or unavailable, ordering, pricing and the trade
record are unaffected (Section 9.1); the news panels show a clear message instead.

**It is low risk and already proven.** It uses a provider the team has integrated, is read-only, and
holds no client data, so it adds no new obligations under Section 9.3/9.4.

**It gives the business a signal too.** Which assets clients look at around news is a natural input to the
activity reporting in BR-16/BR-17.

## Options considered

| Option | Value | Why not chosen now |
|---|---|---|
| **Market context linked to assets (chosen)** | High for Joanna; supports decisions at the moment of trading | — |
| Price alerts | High; brings clients back at the right time | Needs a notification channel (email/SMS) that is out of scope for this phase |
| Stop-loss / stop orders | High; teaches risk management | Requires resting orders and a matching engine beyond BR-06/BR-08; better as a follow-on to server-side execution |
| Position P&L and cost basis | High; answers "am I up or down?" | Partly covered by the portfolio performance chart; full cost-basis/tax-lot reporting needs Finance input |
| Customisable dashboard | Medium; personal layout | Delivered as a usability improvement, but not a differentiator on its own |
| Leaderboard / competitions | Medium; engagement | Weak fit for a firm's retail clients; reputational risk |
| Options trading | Medium for advanced users | Not suited to Joanna; significant regulatory and risk scope |

## How success would be measured

- Share of trades placed within an hour of the client viewing that asset's news (decision support in use).
- Time from opening the trading desk to order confirmation stays under a minute (BR usability goal).
- No trading incidents attributable to the news feed (isolation holds).

## Known limitations

- **Coverage:** company news and earnings come from Finnhub's free tier, which is strongest for US equities.
  Indian and UK equities and FX/crypto get general market news rather than company-specific stories.
- **Economic calendar:** central-bank and macro events need a paid Finnhub plan; the calendar currently
  shows company earnings only.
- **Freshness:** headlines refresh hourly; the platform states when nothing new has been published in 24 hours.
