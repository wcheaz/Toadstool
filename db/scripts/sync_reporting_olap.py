#!/usr/bin/env python3
"""Daily OLTP -> OLAP refresh for Toadstool reporting."""

from __future__ import annotations

import argparse
import logging
import os
import sys
import time
from dataclasses import dataclass
from datetime import datetime, timedelta
from typing import Sequence
from zoneinfo import ZoneInfo

try:
    import psycopg2
    from psycopg2.extras import Json
except ImportError as exc:  # pragma: no cover - handled at runtime only
    raise SystemExit(
        "Missing dependency: psycopg2. Install with `pip install psycopg2-binary`."
    ) from exc


REQUIRED_TARGET_TABLES = (
    "accounts",
    "admin_access_events",
    "cash_movements",
    "instruments",
    "login_events",
    "orders",
)

PRIMARY_KEY_BY_TABLE = {
    "instruments": ("instrument_id",),
    "accounts": ("account_id",),
    "orders": ("order_id",),
    "login_events": ("login_event_id",),
    "cash_movements": ("cash_event_id",),
    "admin_access_events": ("access_event_id",),
}

INSTRUMENTS_SQL = """
SELECT instrument_id, symbol, name, asset_class, status
FROM trading.instruments
ORDER BY symbol
"""

ACCOUNTS_SQL = """
SELECT account_id, client_id, status, opened_at, balance, account_name
FROM trading.accounts
ORDER BY opened_at, account_id
"""

ORDERS_SQL = """
SELECT
    o.order_id,
    o.account_id,
    o.instrument_id,
    o.side,
    o.quantity,
    o.idempotency_key,
    o.submitted_at,
    i.symbol AS instrument_symbol,
    i.name AS instrument_name,
    i.asset_class AS instrument_asset_class,
    i.status AS instrument_status,
    a.status AS account_status,
    a.account_name,
    a.opened_at AS account_opened_at,
    (o.submitted_at AT TIME ZONE 'UTC')::date AS order_date,
    EXTRACT(HOUR FROM o.submitted_at AT TIME ZONE 'UTC')::smallint AS order_hour,
    EXTRACT(WEEK FROM o.submitted_at AT TIME ZONE 'UTC')::smallint AS order_week,
    EXTRACT(MONTH FROM o.submitted_at AT TIME ZONE 'UTC')::smallint AS order_month,
    EXTRACT(QUARTER FROM o.submitted_at AT TIME ZONE 'UTC')::smallint AS order_quarter,
    EXTRACT(YEAR FROM o.submitted_at AT TIME ZONE 'UTC')::integer AS order_year,
    a.client_id
FROM trading.orders o
JOIN trading.accounts a ON a.account_id = o.account_id
JOIN trading.instruments i ON i.instrument_id = o.instrument_id
ORDER BY o.submitted_at, o.order_id
"""

LOGIN_EVENTS_SQL = """
SELECT
    le.login_event_id,
    le.client_id,
    le.email_attempted,
    le.outcome,
    le.occurred_at,
    le.details,
    (le.outcome = 'SUCCESS') AS is_success,
    (le.outcome = 'FAILURE') AS is_failure,
    (le.occurred_at AT TIME ZONE 'UTC')::date AS event_date,
    EXTRACT(HOUR FROM le.occurred_at AT TIME ZONE 'UTC')::smallint AS event_hour,
    EXTRACT(WEEK FROM le.occurred_at AT TIME ZONE 'UTC')::smallint AS event_week,
    EXTRACT(MONTH FROM le.occurred_at AT TIME ZONE 'UTC')::smallint AS event_month,
    EXTRACT(QUARTER FROM le.occurred_at AT TIME ZONE 'UTC')::smallint AS event_quarter,
    EXTRACT(YEAR FROM le.occurred_at AT TIME ZONE 'UTC')::integer AS event_year
FROM trading.login_events le
ORDER BY le.occurred_at, le.login_event_id
"""

CASH_MOVEMENTS_SQL = """
SELECT
    ce.cash_event_id,
    ce.account_id,
    ce.order_id,
    ce.event_type,
    ce.amount,
    ce.occurred_at,
    ce.details,
    o.instrument_id,
    i.symbol AS instrument_symbol,
    i.name AS instrument_name,
    i.asset_class AS instrument_asset_class,
    a.status AS account_status,
    a.account_name,
    CASE
        WHEN ce.event_type IN ('BUY', 'WITHDRAWAL') THEN -ABS(ce.amount)
        ELSE ABS(ce.amount)
    END AS signed_amount,
    (ce.order_id IS NOT NULL OR ce.event_type IN ('BUY', 'SELL')) AS is_trade_related,
    (ce.occurred_at AT TIME ZONE 'UTC')::date AS event_date,
    EXTRACT(HOUR FROM ce.occurred_at AT TIME ZONE 'UTC')::smallint AS event_hour,
    EXTRACT(WEEK FROM ce.occurred_at AT TIME ZONE 'UTC')::smallint AS event_week,
    EXTRACT(MONTH FROM ce.occurred_at AT TIME ZONE 'UTC')::smallint AS event_month,
    EXTRACT(QUARTER FROM ce.occurred_at AT TIME ZONE 'UTC')::smallint AS event_quarter,
    EXTRACT(YEAR FROM ce.occurred_at AT TIME ZONE 'UTC')::integer AS event_year,
    a.client_id
FROM trading.cash_events ce
JOIN trading.accounts a ON a.account_id = ce.account_id
LEFT JOIN trading.orders o ON o.order_id = ce.order_id
LEFT JOIN trading.instruments i ON i.instrument_id = o.instrument_id
ORDER BY ce.occurred_at, ce.cash_event_id
"""

ADMIN_ACCESS_EVENTS_SQL = """
SELECT
    aae.access_event_id,
    aae.admin_user_id,
    aae.admin_email,
    aae.client_id,
    aae.action,
    aae.occurred_at,
    aae.details,
    (aae.occurred_at AT TIME ZONE 'UTC')::date AS event_date,
    EXTRACT(HOUR FROM aae.occurred_at AT TIME ZONE 'UTC')::smallint AS event_hour,
    EXTRACT(WEEK FROM aae.occurred_at AT TIME ZONE 'UTC')::smallint AS event_week,
    EXTRACT(MONTH FROM aae.occurred_at AT TIME ZONE 'UTC')::smallint AS event_month,
    EXTRACT(QUARTER FROM aae.occurred_at AT TIME ZONE 'UTC')::smallint AS event_quarter,
    EXTRACT(YEAR FROM aae.occurred_at AT TIME ZONE 'UTC')::integer AS event_year
FROM trading.admin_access_events aae
ORDER BY aae.occurred_at, aae.access_event_id
"""


@dataclass(frozen=True)
class DbConfig:
    host: str
    port: int
    dbname: str
    user: str
    password: str

    def dsn(self) -> str:
        return (
            f"host={self.host} port={self.port} dbname={self.dbname} "
            f"user={self.user} password={self.password}"
        )


def env(name: str, default: str | None = None, required: bool = False) -> str:
    value = os.getenv(name, default)
    if required and (value is None or value == ""):
        raise RuntimeError(f"Environment variable {name} is required.")
    if value is None:
        raise RuntimeError(f"Environment variable {name} is required.")
    return value


def load_configs() -> tuple[DbConfig, DbConfig]:
    source = DbConfig(
        host=env("SOURCE_DB_HOST", env("DB_HOST", "localhost")),
        port=int(env("SOURCE_DB_PORT", env("DB_PORT", "5432"))),
        dbname=env("SOURCE_DB_NAME", env("DB_NAME", "toadstool_db")),
        user=env("SOURCE_DB_USERNAME", env("DB_USERNAME", required=True)),
        password=env("SOURCE_DB_PASSWORD", env("DB_PASSWORD", required=True)),
    )
    target = DbConfig(
        host=env("TARGET_DB_HOST", source.host),
        port=int(env("TARGET_DB_PORT", str(source.port))),
        dbname=env("TARGET_DB_NAME", "toadstool_reporting"),
        user=env("TARGET_DB_USERNAME", source.user),
        password=env("TARGET_DB_PASSWORD", source.password),
    )
    return source, target


def fetch_rows(source_cfg: DbConfig, query: str) -> list[tuple]:
    with psycopg2.connect(source_cfg.dsn()) as source_conn:
        with source_conn.cursor() as source_cur:
            source_cur.execute(query)
            return source_cur.fetchall()


def ensure_required_target_tables_exist(target_cursor: "psycopg2.extensions.cursor") -> None:
    target_cursor.execute(
        """
        SELECT table_name
        FROM information_schema.tables
        WHERE table_schema = 'analytics'
          AND table_type = 'BASE TABLE'
          AND table_name = ANY(%s)
        """,
        (list(REQUIRED_TARGET_TABLES),),
    )
    existing = {row[0] for row in target_cursor.fetchall()}
    missing = [name for name in REQUIRED_TARGET_TABLES if name not in existing]
    if missing:
        raise RuntimeError(
            "Missing required target analytics tables: " + ", ".join(missing)
        )


def drop_legacy_fact_tables(target_cursor: "psycopg2.extensions.cursor") -> None:
    target_cursor.execute("DROP TABLE IF EXISTS analytics.fact_daily_instrument_activity")
    target_cursor.execute("DROP TABLE IF EXISTS analytics.fact_daily_platform_activity")


def upsert_rows(
    target_cursor: "psycopg2.extensions.cursor",
    table_name: str,
    columns: Sequence[str],
    rows: Sequence[tuple],
) -> None:
    if not rows:
        return

    key_columns = PRIMARY_KEY_BY_TABLE.get(table_name)
    if key_columns is None:
        raise RuntimeError(f"No primary key mapping configured for table: {table_name}")

    update_columns = [column for column in columns if column not in key_columns]
    if not update_columns:
        raise RuntimeError(f"No updatable columns configured for table: {table_name}")

    column_sql = ", ".join(columns)
    placeholders = ", ".join(["%s"] * len(columns))
    conflict_sql = ", ".join(key_columns)
    update_sql = ", ".join(
        [f"{column} = EXCLUDED.{column}" for column in update_columns]
    )
    target_cursor.executemany(
        (
            f"INSERT INTO analytics.{table_name} ({column_sql}) VALUES ({placeholders}) "
            f"ON CONFLICT ({conflict_sql}) DO UPDATE SET {update_sql}"
        ),
        rows,
    )


def adapt_json_columns(rows: Sequence[tuple], json_indexes: Sequence[int]) -> list[tuple]:
    if not rows:
        return []
    converted: list[tuple] = []
    for row in rows:
        mutable_row = list(row)
        for json_index in json_indexes:
            value = mutable_row[json_index]
            if value is not None:
                mutable_row[json_index] = Json(value)
        converted.append(tuple(mutable_row))
    return converted


def refresh_reporting(source_cfg: DbConfig, target_cfg: DbConfig) -> None:
    logging.info("Starting OLTP -> OLAP refresh")

    instruments = fetch_rows(source_cfg, INSTRUMENTS_SQL)
    accounts = fetch_rows(source_cfg, ACCOUNTS_SQL)
    orders = fetch_rows(source_cfg, ORDERS_SQL)
    login_events = fetch_rows(source_cfg, LOGIN_EVENTS_SQL)
    cash_movements = fetch_rows(source_cfg, CASH_MOVEMENTS_SQL)
    admin_access_events = fetch_rows(source_cfg, ADMIN_ACCESS_EVENTS_SQL)
    login_events = adapt_json_columns(login_events, (5,))
    cash_movements = adapt_json_columns(cash_movements, (6,))
    admin_access_events = adapt_json_columns(admin_access_events, (6,))

    with psycopg2.connect(target_cfg.dsn()) as target_conn:
        with target_conn.cursor() as target_cur:
            ensure_required_target_tables_exist(target_cur)
            drop_legacy_fact_tables(target_cur)

            upsert_rows(
                target_cur,
                "instruments",
                ("instrument_id", "symbol", "name", "asset_class", "status"),
                instruments,
            )
            upsert_rows(
                target_cur,
                "accounts",
                ("account_id", "client_id", "status", "opened_at", "balance", "account_name"),
                accounts,
            )
            upsert_rows(
                target_cur,
                "orders",
                (
                    "order_id",
                    "account_id",
                    "instrument_id",
                    "side",
                    "quantity",
                    "idempotency_key",
                    "submitted_at",
                    "instrument_symbol",
                    "instrument_name",
                    "instrument_asset_class",
                    "instrument_status",
                    "account_status",
                    "account_name",
                    "account_opened_at",
                    "order_date",
                    "order_hour",
                    "order_week",
                    "order_month",
                    "order_quarter",
                    "order_year",
                    "client_id",
                ),
                orders,
            )
            upsert_rows(
                target_cur,
                "login_events",
                (
                    "login_event_id",
                    "client_id",
                    "email_attempted",
                    "outcome",
                    "occurred_at",
                    "details",
                    "is_success",
                    "is_failure",
                    "event_date",
                    "event_hour",
                    "event_week",
                    "event_month",
                    "event_quarter",
                    "event_year",
                ),
                login_events,
            )
            upsert_rows(
                target_cur,
                "cash_movements",
                (
                    "cash_event_id",
                    "account_id",
                    "order_id",
                    "event_type",
                    "amount",
                    "occurred_at",
                    "details",
                    "instrument_id",
                    "instrument_symbol",
                    "instrument_name",
                    "instrument_asset_class",
                    "account_status",
                    "account_name",
                    "signed_amount",
                    "is_trade_related",
                    "event_date",
                    "event_hour",
                    "event_week",
                    "event_month",
                    "event_quarter",
                    "event_year",
                    "client_id",
                ),
                cash_movements,
            )
            upsert_rows(
                target_cur,
                "admin_access_events",
                (
                    "access_event_id",
                    "admin_user_id",
                    "admin_email",
                    "client_id",
                    "action",
                    "occurred_at",
                    "details",
                    "event_date",
                    "event_hour",
                    "event_week",
                    "event_month",
                    "event_quarter",
                    "event_year",
                ),
                admin_access_events,
            )
        target_conn.commit()

    logging.info(
        (
            "Refresh complete. Rows loaded -> instruments=%s, accounts=%s, "
            "orders=%s, login_events=%s, cash_movements=%s, admin_access_events=%s"
        ),
        len(instruments),
        len(accounts),
        len(orders),
        len(login_events),
        len(cash_movements),
        len(admin_access_events),
    )


def next_market_close_run(now: datetime, run_hour: int, run_minute: int) -> datetime:
    candidate = now.replace(hour=run_hour, minute=run_minute, second=0, microsecond=0)
    if now >= candidate:
        candidate = candidate + timedelta(days=1)
    while candidate.weekday() >= 5:
        candidate = candidate + timedelta(days=1)
    return candidate


def run_scheduler(
    source_cfg: DbConfig, target_cfg: DbConfig, run_hour: int, run_minute: int
) -> None:
    timezone_name = env("MARKET_TIMEZONE", "America/New_York")
    market_tz = ZoneInfo(timezone_name)

    while True:
        now = datetime.now(market_tz)
        next_run = next_market_close_run(now, run_hour, run_minute)
        wait_seconds = (next_run - now).total_seconds()
        logging.info(
            "Next refresh scheduled for %s (%s).",
            next_run.isoformat(),
            timezone_name,
        )
        time.sleep(max(1, int(wait_seconds)))
        refresh_reporting(source_cfg, target_cfg)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Refresh OLAP database (toadstool_reporting) from OLTP database "
            "(toadstool_db trading tables)."
        )
    )
    parser.add_argument(
        "--run-once",
        action="store_true",
        help="Run the refresh immediately and exit.",
    )
    parser.add_argument(
        "--run-now-and-schedule",
        action="store_true",
        help="Run immediately, then continue with weekday scheduled refreshes.",
    )
    parser.add_argument(
        "--run-hour",
        type=int,
        default=17,
        help="24-hour ET schedule hour (default: 17).",
    )
    parser.add_argument(
        "--run-minute",
        type=int,
        default=0,
        help="ET schedule minute (default: 0).",
    )
    return parser.parse_args()


def main() -> int:
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s [%(levelname)s] %(message)s",
    )
    args = parse_args()

    if not (0 <= args.run_hour <= 23 and 0 <= args.run_minute <= 59):
        raise RuntimeError("--run-hour must be 0-23 and --run-minute must be 0-59.")

    source_cfg, target_cfg = load_configs()

    if args.run_once:
        refresh_reporting(source_cfg, target_cfg)
        return 0

    if args.run_now_and_schedule:
        refresh_reporting(source_cfg, target_cfg)

    run_scheduler(source_cfg, target_cfg, args.run_hour, args.run_minute)
    return 0


if __name__ == "__main__":
    sys.exit(main())
