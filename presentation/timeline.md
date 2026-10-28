# Toadstool Development Timeline

A rough, week-by-week overview of major features and milestones, based primarily
on commit titles and commit dates in the history reachable from the current
checkout, through October 26, 2026. Weeks run Monday-Sunday; dates refer to the
feature commits, not necessarily when their branches were merged.

Work is grouped into **Database**, **Backend**, **Frontend**, **Authorization**
(including authentication), and **APIs** (REST endpoints and external
integrations). Categories without a notable milestone in a given week are
omitted. Representative commit hashes are included for reference; routine fixes,
merge commits, and duplicate/cherry-picked work are generally omitted.

## Week 1: August 17-23 - Project foundations

- **Backend:** Established the project skeleton and confirmed an initial Docker
  build worked (`5cfb446`, `3d9671e`, August 19).
- **Project setup:** Added the project description, team roster, and branching
  strategy.

## Week 2: August 24-30 - Gap in recorded activity

No commits are recorded for this week in the reviewed history.

## Week 3: August 31-September 6 - Planning and database design

- **Database:** Created the database plan and initialization SQL, then simplified
  the schema. Added admin users, audit events, and a fills table to track filled
  orders (`22722e5`, `4970d8f`, `13607f6`, `e253443`, September 3-4).
- **Project planning:** Added the sprint plan and project requirements
  (`68e96db`, `73286c2`, August 31-September 3).

## Week 4: September 7-13 - Audit-oriented database behavior

- **Database:** Made fills insert-only, strengthened table immutability, and
  revised the order schema (`0ead5f6`, `a21405e`, September 8-11).

## Week 5: September 14-20 - Application models and frontend skeleton

- **Database:** Added seed-data and database restart scripts, along with usage
  documentation (`2fa7b74`, September 14).
- **Backend:** Introduced the instrument model and classes corresponding to
  database tables. Added JUnit, instrument tests, and shared enums
  (`48eb66b`, `8f223c0`, `181d9f5`, `da02df5`, September 16-17).
- **Frontend:** Created the initial frontend skeleton (`f5952ef`, September 16).
- **Presentation:** Added architecture diagrams and presentation documents
  (`8a11f40`, September 18).

## Week 6: September 21-27 - Database connectivity, REST, and landing page

- **Database:** Added PostgreSQL support and improved test-data generation.
  Established Flyway migration setup for database tests
  (`44cd1e3`, `b338067`, `1311cdd`, `2edc940`, September 21-22).
- **Backend:** Containerized the Spring Boot application and introduced
  environment-based configuration (`10cd693`, `5047057`, September 24).
- **Frontend:** Added the homepage/landing page (`48d8dcd`, September 24).
- **APIs:** Added an API specification, its REST implementation, controller unit
  tests, and Swagger connectivity to Docker
  (`0d0a3b9`, `2fa6792`, `bea388d`, `488964f`, September 22-25).

## Week 7: September 28-October 4 - Trading and live market data

- **Database:** Added a realistic-holdings migration and associated test coverage
  (`29aa438`, October 2).
- **Backend:** Implemented order creation, total-price calculation, market
  validation, and market-status checks
  (`b17ded8`, `9984b98`, September 28-29).
- **Frontend:** Connected the frontend to database-backed data. Added the trade
  popup and buy UI, integrated market charts, and changed the chart visualization
  to candlesticks (`02a2f6c`, `c1c6f2e`, `c418874`, `2bfed17`,
  September 29-October 2).
- **Authorization:** Introduced working authentication with protected endpoints
  (`84401fb`, September 29).
- **APIs:** Integrated Fauxnance, wired live market-depth data, and added batch
  quotes with five-second auto-refresh
  (`ce74ce9`, `94a565d`, `77deb87`, September 29-October 2).

## Week 8: October 5-11 - Order submission, authentication, and news

- **Database:** Imported database migrations from the authentication branch
  (`8719b38`, October 5).
- **Backend:** Added order submission to the backend, initially with balance
  checks stubbed. Implemented sell orders and repaired the buy-order flow
  (`0188b12`, `1abf94f`, `9914593`, October 5-8).
- **Frontend:** Added news, activity, and profile UI, plus a deposit button.
  Refactored pages into components
  (`0c7d58f`, `09d9ca6`, `0403d6a`, October 7-8).
- **Authorization:** Reached the "part 1 auth" milestone and improved registration,
  password-field behavior, and post-registration navigation
  (`93f82c1`, `dd17b96`, October 7-8).
- **APIs:** Added the Finnhub-backed news integration and improved batch calls
  (`719b9bf`, `1abf94f`, October 7-8).

## Week 9: October 12-18 - Roles, administration, and trading safeguards

- **Database:** Applied authentication-related database fixes
  (`02dbfe2`, October 12).
- **Backend:** Enforced buy-order balance checks and balance deductions, and
  improved sell-order validation (`b4401d6`, `2621e55`, October 14).
- **Frontend:** Added the admin dashboard, account switching, trading tabs, and
  paginated past-order activity. Added light mode, terms and conditions, and a
  privacy policy (`e33be0a`, `0814f66`, `0a642c1`, `dd7b4fe`,
  `5a83c10`, `12fc292`, October 13-16).
- **Authorization:** Added admin and analyst roles, authentication-event logging,
  access revocation, and user-based access control for fills. Improved page
  protection, session storage, and logout behavior
  (`d329199`, `09f2b7c`, `572bd20`, `9281c3a`, `62cafc1`, `dcdd935`,
  October 12-16).
- **APIs:** Extended admin order retrieval across accounts, batched portfolio
  valuation requests, and changed Fauxnance caching to operate per ticker
  (`e59501d`, `cf181f5`, `42707bc`, October 13-16).

## Week 10: October 19-25 - Expanded assets, cash movement, and deployment

- **Database:** Added 1,000 instruments and connected admin access events to the
  database and frontend. Reporting separation was also recorded in this week's
  work (`bbad551`, `3f4840b`, `e8e2620`, October 20-22).
- **Backend:** Added withdrawal and transfer features, admin dashboard metrics,
  server-side execution, and backend Docker deployment configuration
  (`7642955`, `49157cd`, `e8e2620`, `de7b6c2`, October 20-23).
- **Frontend:** Added account management, deposits/withdrawals activity views,
  small-screen layouts, movable/resizable dashboard cards, and a portfolio
  chart. Added frontend Docker/Nginx deployment with health checks
  (`c5ed64f`, `6a0b007`, `04f0d44`, `865c99d`, `b1320fe`,
  October 19-23).
- **Authorization:** Made usernames case-insensitive and surfaced admin access
  events through the application (`13f6325`, `3f4840b`, October 19-22).
- **APIs:** Added cash-flow audit endpoints, corrected Fauxnance handling for
  some instruments, and recorded multi-market pricing support. Limited Finnhub
  to 20 specific instruments while expanding the instrument catalog
  (`660d6d5`, `6fd2d80`, `e8e2620`, `bbad551`, October 19-21).

## Week 11: October 26-November 1 - Cleanup so far

Only October 26 is covered by this snapshot.

- **Backend:** Removed an unused holdings-service dependency from the order
  controller and related tests (`dcc7f4e`, October 26).
- **Deployment documentation:** Corrected the documented Docker container route
  (`a924668`, October 26).

## Overall progression

**Project scaffolding -> database design -> models and connectivity -> REST and
frontend foundations -> live trading data -> order flows and authentication ->
roles and administration -> broader assets, cash movement, and deployment.**
