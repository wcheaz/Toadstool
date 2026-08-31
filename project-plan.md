# Direct Trading Platform — Delivery Plan

**Team size:** 5
**Duration:** 8 weeks
**Weekly rhythm:** 3 days of skills-building (self-paced, can be reordered to suit the team), 2 fixed
**Build Days** where the whole team works on the platform together.

This is a standalone delivery plan for building the platform described in the business
requirements specification (BR-01 through BR-18, plus the section 9 service expectations). It
targets a **Java/Spring Boot backend, an Angular front end, and a Postgres database**, with a
separate Node/NestJS auth service and Kafka as the event backbone — the stack the plan below
assumes throughout. Skills content can be consumed in whatever order and pace the team finds
useful — the Build Days below assume "enough to be dangerous," not mastery, and the plan expects
some rework as the team's own competence catches up with the design.

Two products come out of this plan, sharing one backend/stack and one database:

1. **Client Trading Platform** — the public, Joanna-facing product (registration, sign-in,
   order ticket, holdings, blotter).
2. **Admin/Reporting Dashboard** — an internal product for the David/Priya personas (trading
   activity, audit lookups, business insights), built on the *same* stack and reading the *same*
   database, but deployed to its own origin/URL, with its own access control, so that it can be
   iterated on, scaled and secured independently of the client-facing app.

## Operating principles

- Every backlog item traces to a specific BR reference (or a section 9 service expectation). No
  story exists that only restates a requirement title — each one carries acceptance criteria that
  can be demonstrated.
- Where the specification is ambiguous or silent, the team records the assumption it made and why,
  in the decision log, rather than guessing silently or blocking on it.
- A decision log and a risk list are living documents from Week 1, not artifacts produced once
  for a review.
- Pairs rotate weekly so no single member is the only one who understands a given area (see
  rotation guidance below). Keep a running contribution note per person, per week.
- The architecture is expected to change. When it does, record what changed and why rather than
  silently overwriting the previous decision.
- Treat the two products as genuinely separate deployables from the moment the backend exposes
  more than one consumer — not as an afterthought bolted on in the final week.

## Confirmed technology stack

- **Domain + trade API + admin/reporting API:** Java, Spring Boot, layered as domain → service →
  controller, with a contract-first, OpenAPI-documented REST API. The domain module has no Spring
  dependency at all.
- **Persistence:** Postgres, with MyBatis mappers for SQL access and Flyway for migrations,
  versioned in `/db/migrations` and applied automatically on service startup.
- **Auth service:** a separate Node/NestJS service issuing short-lived, revocable JWTs, carrying
  client ID and role, validated by the Spring Boot services via a shared JWT filter.
- **Event backbone:** Kafka, decoupling order acceptance (published by the trade API) from order
  execution (consumed by a dedicated executor), with an idempotency table guarding against
  replayed messages.
- **Two front ends:** two separate Angular applications — `client-ui` (the public trading app) and
  `admin-ui` (the internal dashboard) — each its own workspace/build/deployed origin, each
  generating its typed HTTP client from the same Spring Boot OpenAPI contract.
- **One database:** a single Postgres instance/cluster serving both APIs, with BR-16's reporting
  access pattern kept off the live trading path via a separate schema, materialised views, or a
  read replica — decide and record which in Week 1/Week 4.
- **Deployment:** Spring Boot services as containers; each Angular app built and deployed to its
  own private, CDN-fronted static origin.

## Requirements traceability summary

Which week is primarily responsible for first delivering each requirement. Several requirements
are touched again later (integration, UI, deployment) — this table marks where the *core* of the
capability is built, not every place it's exercised.

| Requirement | Summary | Priority | Primary week |
|---|---|---|---|
| BR-01 | Register and sign in securely | Must | Week 5 |
| BR-02 | A client reaches only their own data | Must | Week 5 |
| BR-03 | Time-limited, revocable session | Should | Week 5 |
| BR-04 | Submit a buy/sell order | Must | Week 3 |
| BR-05 | Orders checked against trading rules before acceptance | Must | Week 2 |
| BR-06 | Accepted order recorded as a commitment before execution | Must | Week 2 / Week 4 |
| BR-07 | Order status changes visible without manual refresh | Should | Week 4 / Week 6 |
| BR-08 | Priced against a current market quote at execution | Must | Week 4 |
| BR-09 | Holdings, cash and the trade record update atomically | Must | Week 3 / Week 4 |
| BR-10 | View current holdings and cash balance | Must | Week 3 / Week 6 |
| BR-11 | View chronological order/fill history | Must | Week 3 / Week 6 |
| BR-12 | Price across all supported instrument classes | Must | Week 4 |
| BR-13 | Indicative price before submitting an order | Should | Week 3 / Week 6 |
| BR-14 | Permanent, attributable record of every order/pricing/cash change | Must | Week 1 |
| BR-15 | Full lifecycle reconstruction for audit/dispute | Must | Week 1 / Week 6 |
| BR-16 | Analyse trading activity without competing with live trading | Must | Week 4 |
| BR-17 | Surface business insights to internal stakeholders | Should | Week 4 / Week 6 |
| BR-18 | One additional capability, proposed and justified | Should | Week 7 |

Section 9 service expectations (trust/continuity, responsiveness, security, retention, usability)
are cross-cutting and are called out inside the relevant week rather than owned by a single one.

## The 8-week plan

Each week lists a skills focus (for the 3 self-paced days), the user stories the Build Days exist
to satisfy, and what the 2 Build Days should produce. Skills focus is a guide, not a gate — a team
ahead of schedule should pull forward, a team behind should treat the Build Days as the priority
and catch up on skills afterwards.

Story IDs are `Wn-m` (week n, story m) so they can be copied straight into a backlog tool.

### Week 1 — Architecture, backlog and data foundations

**Skills focus:** agile backlog practice, relational data modelling, CI/CD basics.

**User stories:**

| ID | Story | Acceptance criteria | Requirement(s) | First steps |
|---|---|---|---|---|
| W1-1 | As the delivery team, we want the specification broken into a backlog with acceptance criteria per story, so scope and priority are explicit from day one. | Every BR-01..BR-18 item has at least one backlog story; every story has acceptance criteria, not a restated title. | All | Set up the backlog tool; create one story per BR item and tag it with its reference. |
| W1-2 | As Risk/Compliance, I want every order, pricing decision and balance change permanently recorded and attributable to a client and time, so a dispute or regulatory query never depends on reconstruction. | Schema has an append-only audit/record table keyed to client, instrument, time; a row cannot be updated or deleted by the application role. | BR-14 | Draft the audit table's columns (client, instrument, event type, timestamp, payload) and write the migration; revoke UPDATE/DELETE from the application DB role. |
| W1-3 | As the platform, I want a duplicate order submission rejected outright, so a client is never charged twice for one instruction. | Inserting the same idempotency key twice raises a database constraint violation, demonstrated against a running database. | BR-06, BR-09 | Add an idempotency-key column with a unique constraint on the orders table; write a script that inserts the same key twice and shows the second insert failing. |
| W1-4 | As the delivery team, we want a CI pipeline and container tooling in place before feature work starts, so every later week can build on the same foundation. | A commit triggers a build and test run; a service can be built and started as a container locally. | — | The repo already has a starter `pom.xml`, `Dockerfile` and `Jenkinsfile` — fix the Jenkinsfile so it actually runs tests (drop `-DskipTests`) instead of skipping them, and confirm the container still starts locally. |

**Starting point:** the repo is not empty. There is already a single-module `pom.xml` (artifact `team-skeleton`, groupId `com.neueda.leap`, plain `jar` packaging — no Spring Boot, no test dependency), a placeholder `Main.java` that just prints "Hello, World!", a `Dockerfile` that copies `target/team-skeleton.jar`, and a `Jenkinsfile` that builds with `mvn -B clean package -DskipTests` and calls a bare `docker run` its "smoke test". Week 1 modifies these existing files in place rather than creating a project from scratch.

**Technical first steps:**
- Turn the existing root `pom.xml` into a multi-module reactor: keep `com.neueda.leap` as the
  groupId, add `<modules>` for `domain` and `api`, and move the Spring Boot dependencies
  (`spring-boot-starter-parent`, `spring-boot-starter-web`) onto the new `api` module only —
  `domain` stays framework-free per W2-4.
- Retire the placeholder `Main.java` (or leave it under the `api` module temporarily) once a real
  Spring Boot application class exists for `/api`.
- Add the remaining repo layout the plan needs alongside the reactor: `/reporting-api` (or a
  package inside `/api`), `/client-ui`, `/admin-ui`, `/auth-service`, `/infra`, `/db/migrations`.
- Add a `docker-compose.yml` with a Postgres service and a named volume for local development.
- Add Flyway to the `api` module (`spring-boot-starter-flyway` or the Flyway Maven plugin) and
  write `V1__init.sql` creating `accounts`, `instruments`, `orders`, `positions`,
  `cash_movements`, `audit_log`, applied automatically on startup against `db/migrations`.
- Add a `UNIQUE` constraint on `orders(idempotency_key)` in that same migration.
- Fix the existing `Jenkinsfile`: remove `-DskipTests` so `mvn -B clean package` actually runs the
  test suite (add a JUnit dependency to `domain` first, since none of the current modules has one),
  then run Flyway against a throwaway database as part of the same pipeline.
- Update the existing `Dockerfile` to build from whichever module produces the runnable Boot jar
  (e.g. `api/target/api.jar`) once the reactor split lands, rather than `team-skeleton.jar`.

**Build Days:**
- Break the platform into capabilities (order processing, positions/cash, the audit trail,
  reporting) and decide, capability by capability, what storage access pattern it needs before
  drawing any diagram.
- Design and migrate the initial schema for the transactional store: accounts, instruments,
  orders, positions, cash movements, the permanent audit record.
- Prove the database — not application code — rejects a duplicate order under one idempotency
  key.
- Stand up the repo, CI skeleton and container tooling the rest of the plan will build on.

**Deliverables:** backlog v1 traced to BR references, ER diagram + applied migrations, risk list
v1, decision log started, a running CI pipeline shell.

### Week 2 — Domain engine

**Skills focus:** OOP, SOLID, TDD.

**User stories:**

| ID | Story | Acceptance criteria | Requirement(s) | First steps |
|---|---|---|---|---|
| W2-1 | As the platform, I want every order checked against the firm's trading rules before it is accepted, so an order that shouldn't proceed never does. | Insufficient cash, insufficient holding, and an untradable instrument are each rejected, each with a distinct, tested reason; the rule sequence is documented. | BR-05 | Write a failing unit test per rejection reason first; list the rule sequence in the decision log before implementing it. |
| W2-2 | As the platform, I want an accepted order recorded as a firm commitment before execution is attempted, so the record of intent never depends on execution succeeding. | Domain model distinguishes "accepted" from "filled"/"rejected" as separate, tested states; an execution failure cannot erase or skip the accepted record. | BR-06 | Draw the order lifecycle as a state diagram and agree which states are terminal before writing the enum/class. |
| W2-3 | As the platform, I want monetary values to survive many small operations without drifting, so a client's balance is always exactly correct. | The chosen monetary type is unit-tested across a long sequence of small credits/debits with an exact expected result. | Section 9.1 | Pick a candidate monetary type (e.g. integer minor units or a fixed-point decimal type) and write a test applying hundreds of small operations to it. |
| W2-4 | As a future caller (API, event consumer, or anything else), I want to ask the domain "is this order allowed?" in isolation, so new consumers don't have to reimplement business rules. | Domain module has no database, HTTP or framework dependency; it is unit-tested standalone. | BR-05, BR-06 | Create the domain as its own module/package with zero framework dependencies in its build file; add a unit test that runs with no database or server started. |

**Technical first steps:**
- Create `/domain` as its own Maven/Gradle module with no Spring Boot dependency at all — only
  the JDK standard library and JUnit — so it can never accidentally pick up an HTTP or DB import.
- Add `OrderStatus` as a Java enum with the agreed states, and an `Order` class that only
  transitions between them through methods, not by direct field assignment.
- Add a `Money` class wrapping a `long` of minor units (e.g. cents) rather than `double` or
  unscaled `BigDecimal`, with `add`/`subtract` methods, unit-tested for exactness.
- Add a `TradingRules` class implementing the BR-05 checks as an ordered list of predicates/checks,
  each with its own JUnit test.
- Add `/domain` as a Maven/Gradle module dependency of `/api` (an empty call site is fine this
  week) so the seam exists before Week 3 needs it.

**Build Days:**
- Model the trading domain as objects, not tables: entities, the order lifecycle state machine,
  and which states are genuinely terminal.
- Implement the BR-05 rule chain (checked against trading rules before acceptance) and the BR-06
  rule (recorded as a commitment before execution) as an explicit, ordered sequence living in the
  domain, not a caller.
- Choose and pressure-test a monetary type across repeated small operations.
- Write tests before implementation and keep the domain free of any database, HTTP or framework
  dependency.

**Deliverables:** domain library with no infrastructure dependencies, class/sequence diagrams,
a test suite proving behaviour rather than just running green, decision log update.

### Week 3 — Trade API and the dashboard's backend seam

**Skills focus:** REST API design, contract-first/OpenAPI, Spring Boot and MyBatis, JWT
validation concepts, persistence with parameterised queries.

**User stories:**

| ID | Story | Acceptance criteria | Requirement(s) | First steps |
|---|---|---|---|---|
| W3-1 | As Joanna, I want to submit an order to buy or sell a supported instrument, so I can act on a decision myself. | POST endpoint accepts a valid order and returns an accepted/rejected outcome; invalid input returns a consistent error shape. | BR-04 | Draft the OpenAPI request/response schema for "place order" first; wire the controller to call the existing domain, not a new copy of its rules. |
| W3-2 | As Joanna, I want to see my current holdings and cash balance, so I know where I stand as of my last executed trade. | GET endpoint returns holdings and cash correct as of the last fill, covered by an integration test. | BR-10 | Add the read query against the Week 1 schema; write an integration test that seeds a fill and asserts the returned balance. |
| W3-3 | As Joanna, I want to see a chronological history of my own orders and fills, so I can review what happened. | GET endpoint returns a client's own orders/fills in time order; another client's data is never returned. | BR-11 | Add the query scoped by client ID; write a test asserting client A's request never returns client B's rows. |
| W3-4 | As Joanna, I want an indicative price before I submit an order, so I know roughly what I'm agreeing to. | GET/preview endpoint returns a current indicative quote for a candidate order without placing it. | BR-13 | Stub a quote source returning a fixed/test price and expose it read-only; swap in the real source in Week 4. |
| W3-5 | As the platform, I want an order fill, its cash movement and its trade record to commit as one unit, so none of the three can ever update without the others. | A forced failure mid-fill leaves holdings, cash and the record all unchanged (all rolled back), demonstrated. | BR-09 | Wrap the fill/cash/record writes in a single database transaction; write a test that forces an exception after the first write and asserts a full rollback. |
| W3-6 | As David/Priya, I want a read-only reporting API surface separate from the trade API, so dashboard queries never share a path with live order placement. | A distinct API module/route group exists for reporting reads, deployable independently of the trade endpoints. | BR-16 (enabler) | Create a separate controller/module and base route (e.g. `/admin/...`) now, even with a single placeholder endpoint, so it exists as its own seam. |

**Technical first steps:**
- Scaffold `/api` as a Spring Boot project (via `start.spring.io` or your IDE) with
  `spring-boot-starter-web`, `springdoc-openapi-starter-webmvc-ui` for OpenAPI, and a
  Dockerfile; run it in `docker-compose.yml` alongside Postgres.
- Write `openapi.yaml` (or use `springdoc` annotations to generate it) for `POST /orders`,
  `GET /accounts/{id}/holdings`, `GET /accounts/{id}/orders`, `GET /orders/quote-preview` before
  writing controller bodies.
- Add MyBatis (`mybatis-spring-boot-starter`) mappers pointed at the Week 1 Flyway-managed
  schema, and inject the `/domain` module into the service layer rather than reimplementing rule
  checks there.
- Wrap order placement in a single `@Transactional` service method covering the order row, cash
  movement and position update together.
- Create a second controller/package namespaced under `/admin` (e.g. `AdminReportingController`
  in `com.leap.trading.admin`) in the same Spring Boot service, with one placeholder
  `GET /admin/health` endpoint, so the seam is real even before Week 4's data exists.

**Build Days:**
- Design the API contract (consistent response and error shapes) before writing a controller,
  then expose the Week 2 domain over HTTP: place an order, view holdings/cash/history (BR-04,
  BR-07, BR-08, BR-09, BR-10, BR-11, BR-13).
- Make BR-09 concrete: something enforces that the fill, the cash movement and the trade record
  commit together or not at all — not just a hope that they do.
- Containerise the service and get it running reproducibly in the team's own environment.
- Stand up a second, thin API surface — same backend project or a clearly separated module —
  for read-only reporting/admin queries against the same database. It doesn't need real data yet;
  what matters is that it exists as its own seam from day one, so Week 4 onward and eventual
  separate deployment don't require retrofitting a boundary that was never there.

**Deliverables:** published OpenAPI contract, running containerised trade API, an early
admin/reporting API seam, updated backlog and risk list.

### Week 4 — Event backbone and the dashboard's analytics pipeline

**Skills focus:** event-driven architecture and a message broker, ETL/data-pipeline design,
basic exploratory analysis.

**User stories:**

| ID | Story | Acceptance criteria | Requirement(s) | First steps |
|---|---|---|---|---|
| W4-1 | As the platform, I want order acceptance and order execution to be genuinely separate steps connected by a message, so execution can fail or be delayed without losing the record of intent. | Accepting an order publishes a message; a stopped consumer still leaves the accepted order recorded; restarting the consumer resumes execution. | BR-06 | Stand up the broker locally; define the "order accepted" message schema and have the Week 3 API publish it instead of executing inline. |
| W4-2 | As the platform, I want a replayed execution message to never be applied twice, so an account is never double-debited. | The same message delivered twice results in exactly one fill, demonstrated live. | BR-06, BR-09 | Add a processed-message/idempotency table keyed by message ID, checked before applying a fill; write a test that publishes the same message twice. |
| W4-3 | As Joanna, I want my order filled or rejected against a current market quote, so the price reflects the real market at the moment of execution. | Execution calls a live/stand-in market data source per instrument class and records the quote used; a source outage results in a defined rejection, not a hang. | BR-08, BR-12 | Pick the market data source and confirm its request limits; wrap the call with a timeout and a defined rejection path for failure. |
| W4-4 | As Priya, I want trading activity analysed by instrument, period and client segment, so I can report on it without slowing down live trading. | The analytics path reads from a reporting-isolated source (replica/materialised view/separate schema), not the live transactional path used by order placement. | BR-16 | Decide replica vs. materialised view vs. separate schema and record the reason; create the read path pointed at it, not the live table. |
| W4-5 | As an internal stakeholder, I want a small set of business insights surfaced automatically, so I don't have to write ad hoc queries to see what's happening. | At least three named insights are computed by a repeatable ETL run and available to the reporting API. | BR-17 | Agree the three insights as a team and write them down; build the extract/transform/load run as a scheduled or triggerable script, tested against sample data. |

**Technical first steps:**
- Add a Kafka service to `docker-compose.yml` (Kafka + a schema/topic bootstrap step) and create
  the `order-accepted` topic.
- Add `spring-kafka` to `/api`, and publish to `order-accepted` from the order-placement service
  method after the transactional commit from Week 3, instead of executing inline.
- Create an `/executor` Spring Boot service (or a `@KafkaListener` module inside `/api`) that
  consumes `order-accepted`, and add a `processed_messages(message_id TEXT PRIMARY KEY)` Flyway
  migration, checked before applying any fill, inside the same transaction as the fill itself.
- Wire a market data client (real provider or the provided stand-in) behind a small Java interface,
  with a timeout and a defined error path for a failed/slow call.
- Create an `/etl` folder with separate `extract.py`/`transform.py`/`load.py` scripts (Python, per
  the curriculum's ETL teaching), reading from Postgres via a read-only role and writing to a
  reporting schema/materialised view; add a test with malformed sample input.

**Build Days:**
- Separate order acceptance from order execution with a message broker: what's published on
  acceptance, what consumes it, how BR-09 stays true from the execution side.
- Make the consumer idempotent and demonstrate — not just describe — that replaying a message
  doesn't double-debit an account.
- Wire a real market-data source (or a provided stand-in) into execution for BR-08 pricing,
  respecting its request limits by design.
- Agree the small set of business insights the admin dashboard needs (BR-16, BR-17) and build a
  repeatable extract/transform/load path from the transactional store into whatever the reporting
  side of the schema needs, isolated so it cannot compete with live trading for capacity.

**Deliverables:** event flow diagram, live duplicate-message demonstration, a working
extract/transform/load path feeding the admin backend, three named business insights with the
data behind them.

### Week 5 — Authentication and access control

**Skills focus:** identity/zero trust concepts, NestJS, password hashing, JWT issuing.

**User stories:**

| ID | Story | Acceptance criteria | Requirement(s) | First steps |
|---|---|---|---|---|
| W5-1 | As a prospective client, I want to register and sign in securely, so I can access my own account on return visits. | Registration and login endpoints work end to end; passwords are hashed with a deliberately chosen algorithm and cost, never logged. | BR-01 | Pick the hashing algorithm and cost parameter and write it in the decision log; build register/login endpoints against a test user before wiring in the real client app. |
| W5-2 | As a client, I want to be certain no other client can ever see or act on my positions, cash or history, so my data stays mine. | A valid token for client A is rejected on any endpoint scoped to client B's data, demonstrated. | BR-02 | Put the client-ID check in a shared guard/middleware used by every scoped endpoint, not repeated per controller; write the cross-client rejection test first. |
| W5-3 | As a client, I want my session to be time-limited and revocable, so a compromised credential has bounded, controllable exposure. | Tokens expire after a defined window; a documented mechanism can invalidate a session before natural expiry. | BR-03 | Set a short token expiry and add a token/session denylist or store lookup that can be flipped before expiry; test that a revoked token is rejected immediately. |
| W5-4 | As an internal user (David/Priya persona), I want an internal/admin role distinct from a client role, so the reporting API can be restricted to people who genuinely need it. | A client-role token is rejected on every admin/reporting endpoint; an admin-role token is rejected on client-only actions. | BR-02 (dashboard equivalent) | Add a role claim to the token; guard the Week 3 admin/reporting routes with a role check and write the rejection test both ways. |

**Technical first steps:**
- Scaffold `/auth-service` as a NestJS project (`nest new auth-service`) with a Flyway (or
  NestJS-migration) `users` table including a hashed password column and a `role` column
  (`client` / `admin`).
- Add `argon2` or `bcrypt` via npm and write the cost/parameter choice into the decision log next
  to the code that sets it.
- Add `POST /auth/register`, `POST /auth/login`, `POST /auth/refresh` and `POST /auth/logout`
  endpoints (NestJS controllers + `@nestjs/jwt`), issuing short-lived JWTs carrying client ID and
  role.
- Add a session/token store (a `sessions` table, or Redis via `docker-compose.yml`) so
  `logout`/revoke can invalidate a token before its natural expiry.
- Add a shared `JwtAuthFilter`/`OncePerRequestFilter` in the Spring Boot services (both `/api`
  and the admin routes) that validates the JWT signature and extracts client ID/role, used by
  every scoped controller rather than re-implemented per controller.

**Build Days:**
- Build a single shared auth service used by both products: registration and secure sign-in
  (BR-01), a time-limited and revocable session (BR-03), and roles that distinguish a retail
  client from an internal/admin user.
- Enforce BR-02 (and its dashboard-side equivalent): a client reaches only their own data, and an
  internal role reaches only what it genuinely needs — as a check the API itself makes, not
  something either front end merely chooses not to show.
- Hash passwords deliberately (algorithm and cost chosen and recorded, not copied from the first
  example found), and confirm nothing secret ever reaches a log.
- Migrate the trade API off whatever stood in for authentication earlier, and wire the
  admin/reporting API to require an internal role.

**Deliverables:** auth flow diagram, a documented role model covering both products, a concrete
answer to "how would we revoke a session right now," updated risk list.

### Week 6 — Client UI and admin dashboard UI

**Skills focus:** Angular, component/service/DI patterns, HTTP client generation
from the API contract, end-to-end UI testing.

**User stories:**

| ID | Story | Acceptance criteria | Requirement(s) | First steps |
|---|---|---|---|---|
| W6-1 | As Joanna, I want to sign in, view my holdings/cash, place an order and view my order history through a real UI, so I don't need training or a guide to use the platform. | A first-time user completes sign-in, a trade and a history check unaided, observed against the acceptance criteria from Weeks 3 and 5. | BR-10, BR-11, BR-04 | Scaffold the client app and generate/write a typed HTTP client from the Week 3 OpenAPI contract before building screens against it. |
| W6-2 | As Joanna, I want an order that's accepted but not yet filled to read as still working, so I never wonder whether my action registered. | The UI shows a distinct, non-alarming state for accepted-not-yet-filled and updates automatically once resolved. | BR-07 | Add an explicit "pending" status in the UI's order model and a polling/refresh call for it; design its look before wiring the data. |
| W6-3 | As Joanna, I want every error the platform can return to reach me as something I can act on, so a failure doesn't leave me confused. | Every backend error code maps to a distinct, plain-language message in the UI; none render raw/internal text. | Section 9.5 | List every error code the backend defines and draft a plain-language message for each before writing the mapping code. |
| W6-4 | As David, I want to look up the full lifecycle of a specific client's order from the dashboard, so I can answer a dispute without asking engineering to dig through logs. | The admin dashboard can retrieve, for one order, its full history (placed, priced, filled/rejected) from a single view. | BR-15 | Scaffold the separate admin app; build the single order-lookup screen against the Week 3 admin API seam first. |
| W6-5 | As Priya, I want a dashboard view of trading activity and the named business insights, so I can report to the executive committee without writing queries myself. | The admin dashboard renders the Week 4 insights and lets activity be filtered by instrument, period and client segment. | BR-16, BR-17 | Wire the admin app to the Week 4 reporting endpoints; build one insight end to end before adding the rest. |

**Technical first steps:**
- Scaffold two separate Angular workspaces (`ng new client-ui` and `ng new admin-ui`), each with
  its own `package.json`, build output and `environment.ts`/`environment.prod.ts`
  (`apiBaseUrl`, distinct per app).
- Generate a typed HTTP client from the Week 3 `openapi.yaml` for each app (e.g.
  `openapi-generator-cli generate -g typescript-angular`) rather than hand-writing `HttpClient`
  calls.
- Add Angular route guards (`CanActivateFn`) in both apps that check for a valid token and the
  expected role before rendering a protected route, redirecting to `/sign-in` otherwise.
- Build the sign-in, holdings, order-ticket and blotter components in `client-ui` first; build
  the order-lookup and insights components in `admin-ui` against the Week 3/4 admin endpoints.
- Add a Playwright project (`npm init playwright@latest`) with one end-to-end test per app:
  sign-in through to a core action.

**Build Days:**
- Build the client-facing app end to end against the real backend: sign-in, holdings/cash view,
  order ticket, blotter (BR-10, BR-11, BR-13), designed for the Joanna persona without a guide.
- Treat an order that's accepted-but-not-yet-filled as still working, not broken, and turn every
  backend error into something a non-technical user can act on.
- Build the admin dashboard as its **own separate application** — its own project, its own build
  output, its own eventual origin — even though it can share styling/component conventions with
  the client app. It should cover the David/Priya needs: trading activity, client activity
  trends, audit/trace lookup for a disputed order.
- Wire both apps to the shared auth service with route guards appropriate to their role, and keep
  every secret and API key out of both built bundles.

**Deliverables:** two working front ends against real services, meaningful end-to-end test
coverage on both, updated backlog.

### Week 7 — Integration, gap-closing and the BR-18 extension

**Skills focus:** none scheduled — a full project week, used as buffer and integration time.

**User stories:**

| ID | Story | Acceptance criteria | Requirement(s) | First steps |
|---|---|---|---|---|
| W7-1 | As the delivery team, I want a clear status against every BR item, so gap-closing effort goes where it matters most. | A single audit document marks each of BR-01..BR-18 and each section 9 expectation as done/partial/not-started, checked against the running system, not the backlog. | All | Copy the traceability summary table into a working document and mark each row against the live system, not the backlog board. |
| W7-2 | As Joanna, I want to place an order through the real UI and see it reach the permanent record and the dashboard, so the whole platform demonstrably works end to end, not just its parts. | One order is traced live from the UI through the API, the event backbone, the committed rows and the dashboard's reporting view. | BR-04, BR-06, BR-09, BR-15, BR-16 | Place one order through the deployed-locally client UI and follow it step by step (API log, broker, DB row, dashboard view), noting where it breaks. |
| W7-3 | As the business, I want one additional capability that extends the platform's value, with a stated reason it was chosen over the alternatives, so the platform's growth path is demonstrated, not just claimed. | A BR-18 capability is built (if time allows) with a short written justification and what was rejected. | BR-18 | List 2-3 candidate capabilities and their trade-offs; pick one and write the one-paragraph justification before starting to build it. |

**Technical first steps:**
- Create `/docs/br-audit.md` with one row per BR-01..BR-18 and section 9 item, a status, and a link/reference to the code or test that proves it.
- Add or extend an `/e2e` test suite that drives the deployed-locally stack: register, sign in, place an order, confirm it reaches the database and the admin dashboard's reporting view.
- Re-run the Week 1 duplicate-order test and the Week 4 duplicate-message test against the full stack as regression checks, not just their original modules.
- If building BR-18, scaffold it as its own module/component from the start rather than bolting it onto an existing one, so it's clear what was added and why.

**Build Days (both, plus any slack from earlier weeks):**
- Audit BR-01 through BR-18 and the section 9 service expectations against what is actually
  running, marked done/partial/not-started, not what a backlog card claims.
- Prioritise Must before Should before Could and close the highest-value gaps first.
- Integration-test the whole platform together: place an order through the real client UI, watch
  it move through the domain, the event backbone, and land in the admin dashboard's reporting
  view.
- Run a continuity pass on every earlier decision: does the schema, the domain and the API still
  hang together now that four more weeks are built on top of them? Record any drift found and
  fix it deliberately rather than patching around it.
- If the core platform is solid with time to spare, build the BR-18 extension capability and
  write down why it was chosen over the alternatives considered.

**Deliverables:** a BR traceability audit, an updated risk list and decision log, the BR-18
capability and its justification if built.

### Week 8 — Deployment and showcase

**Skills focus:** cloud fundamentals — networking, static hosting behind a CDN, container
deployment, managed secrets.

**User stories:**

| ID | Story | Acceptance criteria | Requirement(s) | First steps |
|---|---|---|---|---|
| W8-1 | As Joanna, I want to reach the trading platform at a real URL over HTTPS, so I can use it as a real client would. | The client app resolves at a public HTTPS URL and functions identically to the local build. | Section 9.1 | Create the private static origin and CDN distribution for the client app; do a manual first deploy before scripting it. |
| W8-2 | As David/Priya, I want the admin dashboard reachable at its own, separate URL, so it can be secured and iterated on independently of the client platform. | The admin dashboard resolves at a second, distinct HTTPS URL/origin, restricted to internal roles, backed by the same database. | Admin dashboard requirement | Repeat the Week 8 client-app hosting pattern with a second origin/distribution for the admin app; confirm it's unreachable without an internal-role session. |
| W8-3 | As the delivery team, I want deployment to be one repeatable command per app, so redeploying never depends on anyone's memory of the steps. | Running the deploy command twice for either app leaves an identical result to running it once. | Section 9.1 | Script the manual deploy steps from W8-1/W8-2 into one command per app; run each command twice and diff the result. |
| W8-4 | As Risk/Compliance, I want the backend to accept requests only from the platform's own deployed origins, so an arbitrary site can never call it on a client's behalf. | CORS/origin configuration is a considered allow-list covering exactly the two deployed origins, not a wildcard. | Section 9.3 | Replace any wildcard CORS config with an explicit allow-list of the two deployed origins; test a request from a third, disallowed origin is rejected. |

**Technical first steps:**
- Add an `/infra` folder with IaC (e.g. Terraform or a CDK app) defining: two private S3 buckets
  (`client-ui`, `admin-ui`), two CloudFront distributions in front of them, and the ECS/container
  service definitions for the Spring Boot and NestJS services.
- Write `deploy-client.sh` and `deploy-admin.sh` doing `ng build --configuration production` →
  `aws s3 sync` → CloudFront invalidation for their own app; run each twice locally and diff the
  resulting bucket contents.
- Create a scoped IAM policy/role for the deploying credential limited to the specific bucket/
  distribution/ECS service it needs, and store it outside the repository (a secrets manager or
  CI secret store, not `.env` in git).
- Update the Spring Boot CORS configuration (`WebMvcConfigurer#addCorsMappings`) to an explicit
  allow-list of the two deployed CloudFront origins, and add a test/manual check that a third
  origin is rejected.
- Document teardown commands (`terraform destroy` or equivalent) for every resource created, and
  confirm with whoever owns the account before the showcase.

**Build Days:**
- Deploy the client app to its own URL over HTTPS: a private static origin behind a CDN, never
  addressed directly.
- Deploy the admin dashboard to a **second, separate** URL/origin, on the same pattern, pointed
  at the same backend and database but reachable only by internal roles.
- Configure the backend's origin/CORS rules deliberately for both deployed origins — "allow
  everything" is not an acceptable answer for a service holding customer positions and internal
  reporting alike.
- Make deployment one repeatable command per app; prove running it twice leaves the same result
  as running it once.
- Scope every deployment credential to only what it needs, confirm no long-lived key is anywhere
  in the repository, and confirm teardown responsibilities before the showcase.
- Rehearse the showcase: the platform end to end, how the architecture changed since the first
  pitch, the decisions the team would defend, the BR-18 capability if built, and where the two
  products' shared stack paid off versus where it added friction.

**Deliverables:** two live, HTTPS-reachable URLs on separate origins, a deploy script per app,
a teardown record, the final showcase.

## Team-of-five rotation guidance

Rotate pairs so that by Week 8 every member has touched the domain, the API, the event backbone,
at least one front end and the deployment pipeline at least once. A simple pattern:

- Two pairs plus one rotating "floater" each week, the floater picking whichever area is thinnest
  on coverage that week.
- Swap who partners with whom every week rather than letting pairs calcify.
- Whoever builds the admin dashboard in Week 6 should not be the same two people who built the
  client UI — cross-pollinate so both products get reviewed by someone who didn't write them.
- Keep the per-person, per-week contribution note going the whole way through; it's the only
  artifact that shows the rotation actually happened rather than being planned and ignored.

## Risk callouts specific to this plan

- **Two deployables from one backend is real added surface area.** Treat the admin/reporting API
  seam (Week 3) and the separate deployment (Week 8) as first-class work, not a footnote — teams
  that leave it until the last week tend to discover the backend was never actually separable.
- **Shared database, two front doors.** Role-based access has to be enforced at the API, not the
  UI. Neither front end should be trusted to be the only thing standing between a user and data
  they shouldn't see.
- **5-day weeks with 3 learning / 2 build is tight.** If a team is behind on the underlying skill
  for a given week, prioritise the Build Day outcomes above and treat the skill gap as a risk-list
  item with a plan to close it, rather than letting the build slip silently.
- **Reporting must not compete with live trading (BR-16).** Load-test or at least reason about
  this explicitly once the dashboard's queries exist; don't assume isolation that was never
  designed in.
