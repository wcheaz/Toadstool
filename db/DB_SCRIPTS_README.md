# Database Test Data Scripts

## Quick Start

```bash
psql -U postgres -d Toadstool -f db/restart_database.sql
```

This command drops all schemas, recreates the database structure, and inserts comprehensive test data (25 clients, 120 orders, ~96 fills).

---

## Overview

### Files Included

1. **restart_database.sql** - Complete SQL script for database reset and test data
   - Drops and recreates all schemas
   - Creates complete trading database schema
   - Inserts comprehensive test data (25 clients, 120 orders, ~96 fills)
   - Can be run with: `psql -U postgres -d Toadstool -f restart_database.sql`

2. **Flyway migrations** - The authoritative migration scripts live in
   `src/main/resources/db/migrations/` and run automatically when the Spring Boot
   application starts. They should not be duplicated in this folder.

## Data Summary

Each database reset provides:
- **25 Clients**: From "alice.johnson@example.com" to "yara.king@example.com"
- **25 Accounts**: One active account per client
- **14 Instruments**:
  - 10 Equities: AAPL, GOOGL, MSFT, AMZN, TSLA, META, NVDA, JPM, GLD, SPY
  - 3 Cryptocurrencies: BTC-USD, ETH-USD, XRP-USD
  - 1 FX ticker: FX:EURUSD
- **120 Orders**: Randomly distributed across clients, with varying quantities and sides
- **~96 Fills**: 80% of orders have fills, with realistic partial fills and some failed/pending states
- **~120 Trade Events**: Complete audit trail for all orders
- **~75 Login Events**: Security audit log (3 per client)

## How to Use

**Prerequisites:**
- PostgreSQL client tools installed (psql must be in your PATH)

See the Quick Start command at the top of this file to run the script.

### Flyway Migrations

Flyway uses `src/main/resources/db/migrations/` for version control and executes the
pending migrations on application startup. To change seeded test data, edit the
appropriate migration there; do not modify an already-applied migration in a shared
database.

## Customizing Test Data

To modify the test data, edit:
- **restart_database.sql** - For direct SQL execution
- **src/main/resources/db/migrations/** - For Flyway-managed migrations

### Examples:

**Change number of clients:**
```sql
-- In the client_data CTE, add/remove rows:
INSERT INTO trading.clients (email, display_name, status) VALUES
('your.email@example.com', 'Your Name', 'ACTIVE'),
-- Add more here...
```

**Change number of orders:**
```sql
-- In order_data CTE, change the LIMIT:
LIMIT 200  -- Instead of 120
```

**Change instruments:**
```sql
-- Modify the instruments INSERT:
INSERT INTO trading.instruments (symbol, name, asset_class, status) VALUES
('GOOG', 'Alphabet Inc. Class C', 'EQUITY', 'TRADABLE'),
-- Add more here...
```

**Change fill rate:**
```sql
-- In the order_subset CTE:
SELECT order_id, random() < 0.9 as should_fill  -- 90% instead of 80%
FROM trading.orders
```

## Verification

After running the script, verify the data was inserted:

```sql
-- Check row counts
SELECT 
    (SELECT COUNT(*) FROM trading.clients) as clients,
    (SELECT COUNT(*) FROM trading.orders) as orders,
    (SELECT COUNT(*) FROM trading.fills) as fills;

-- View a sample client
SELECT * FROM trading.clients LIMIT 1;

-- Check order distribution
SELECT side, COUNT(*) FROM trading.orders GROUP BY side;

-- View fill statistics
SELECT status, COUNT(*) FROM trading.fills GROUP BY status;
```

## Troubleshooting

**Error: "psql not found"**
- Install PostgreSQL client tools
- Ensure psql is in your PATH environment variable

**Error: "password authentication failed"**
- Configure a .pgpass file: `~/.pgpass` (Unix) or `%APPDATA%\postgresql\pgpass.conf` (Windows)
- Or set `PGPASSWORD` environment variable before running psql

**Error: "database 'Toadstool' does not exist"**
- Create the database first: `createdb -U postgres Toadstool`

**Constraints or Foreign Key Errors**
- Ensure the database is empty or the schema has been dropped first
- The script includes `DROP SCHEMA IF EXISTS trading CASCADE;`

## Development Workflow

1. **Start development**: Run the command from Quick Start (top of this file)
2. **Work on features**: Make code changes
3. **Need fresh data**: Run the script again
4. **Test specific scenario**: Manually insert additional test rows as needed

## Next Steps

Consider adding:
- **Seed data for analytics tables** (fact_daily_* tables)
- **Admin user test data** (trading.admin_users)
- **Performance test data** (10,000+ orders for load testing)
- **Edge case data** (max/min values, special characters, etc.)

---

## Daily OLTP -> OLAP Refresh Script

Use [sync_reporting_olap.py](./scripts/sync_reporting_olap.py) to copy OLTP data from `toadstool_db` into the existing analytics tables in `toadstool_reporting` after market hours.

### What It Does

- Reads source data from:
  - `trading.accounts`
  - `trading.admin_access_events`
  - `trading.cash_events`
  - `trading.instruments`
  - `trading.login_events`
  - `trading.orders`
- Upserts (insert/update) rows in existing reporting tables in the target OLAP database:
  - `analytics.accounts`
  - `analytics.admin_access_events`
  - `analytics.cash_movements`
  - `analytics.instruments`
  - `analytics.login_events`
  - `analytics.orders`
- Drops legacy fact tables if present:
  - `analytics.fact_daily_instrument_activity`
  - `analytics.fact_daily_platform_activity`
- Runs once (`--run-once`) or stays running with a weekday schedule (default 5:00 PM ET).

### Prerequisites

- Python 3.10+ (for `zoneinfo`)
- `psycopg2-binary` package

```bash
pip install psycopg2-binary
```

### Environment Variables

Set the source/target variables from [.env.example](../.env.example):

- `SOURCE_DB_*` for `toadstool_db` (OLTP)
- `TARGET_DB_*` for `toadstool_reporting` (OLAP)
- `MARKET_TIMEZONE` (default `America/New_York`)

### Usage

Run once immediately:

```bash
python db/scripts/sync_reporting_olap.py --run-once
```

Run once now, then keep running once per weekday after market hours:

```bash
python db/scripts/sync_reporting_olap.py --run-now-and-schedule
```

Run continuously on weekdays after market close (default 17:00 ET):

```bash
python db/scripts/sync_reporting_olap.py
```

Custom schedule time:

```bash
python db/scripts/sync_reporting_olap.py --run-hour 17 --run-minute 30
```

## Admin Access Outbox Processor

Use [process_admin_access_outbox.py](./scripts/process_admin_access_outbox.py) to process `AdminAccessEventCreated` rows from `toadstool_db.trading.outbox` into `toadstool_reporting.analytics.admin_access_events`.

### What It Does

- Reads unpublished outbox events where `event_type = 'AdminAccessEventCreated'`
- Upserts those events into `analytics.admin_access_events`
- Marks processed outbox rows by setting `published_at = now()`
- Uses `FOR UPDATE SKIP LOCKED` for safe parallel workers

### Usage

Run once (drains pending events and exits):

```bash
python db/scripts/process_admin_access_outbox.py --run-once
```

Run continuously (poll every 10 seconds by default):

```bash
python db/scripts/process_admin_access_outbox.py
```

Custom batch/poll tuning:

```bash
python db/scripts/process_admin_access_outbox.py --batch-size 500 --poll-interval-seconds 5
```
