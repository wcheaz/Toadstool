#!/usr/bin/env python3
"""Process admin access outbox events into analytics.admin_access_events."""

from __future__ import annotations

import argparse
import logging
import os
import sys
import time
import uuid
from dataclasses import dataclass
from datetime import UTC, datetime
from typing import Any

try:
    import psycopg2
    from psycopg2.extras import Json, register_uuid
except ImportError as exc:  # pragma: no cover - runtime dependency check
    raise SystemExit(
        "Missing dependency: psycopg2. Install with `pip install psycopg2-binary`."
    ) from exc

# Let psycopg2 pass uuid.UUID values as Postgres uuid.
register_uuid()


EVENT_TYPE = "AdminAccessEventCreated"


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


def parse_uuid(value: Any, field_name: str) -> uuid.UUID | None:
    if value is None:
        return None
    try:
        return uuid.UUID(str(value))
    except ValueError as exc:
        raise RuntimeError(f"Invalid UUID in payload field {field_name}: {value}") from exc


def parse_occurred_at(value: Any) -> datetime:
    if value is None:
        raise RuntimeError("Payload field occurred_at is required.")
    if isinstance(value, datetime):
        dt = value
    else:
        text = str(value)
        dt = datetime.fromisoformat(text.replace("Z", "+00:00"))
    if dt.tzinfo is None:
        dt = dt.replace(tzinfo=UTC)
    return dt


def ensure_target_table(target_cursor: "psycopg2.extensions.cursor") -> None:
    target_cursor.execute(
        """
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'analytics'
          AND table_name = 'admin_access_events'
        """
    )
    if target_cursor.fetchone() is None:
        raise RuntimeError(
            "Missing target table analytics.admin_access_events in toadstool_reporting."
        )


def map_payload_to_row(payload: dict[str, Any]) -> tuple:
    required = ("access_event_id", "admin_user_id", "admin_email", "action", "occurred_at")
    for field_name in required:
        if payload.get(field_name) is None:
            raise RuntimeError(f"Payload field {field_name} is required.")

    occurred_at = parse_occurred_at(payload["occurred_at"])
    utc_time = occurred_at.astimezone(UTC)

    return (
        parse_uuid(payload["access_event_id"], "access_event_id"),
        parse_uuid(payload["admin_user_id"], "admin_user_id"),
        str(payload["admin_email"]),
        parse_uuid(payload.get("client_id"), "client_id"),
        str(payload["action"]),
        occurred_at,
        Json(payload.get("details", {})),
        utc_time.date(),
        utc_time.hour,
        utc_time.isocalendar().week,
        utc_time.month,
        ((utc_time.month - 1) // 3) + 1,
        utc_time.year,
    )


def fetch_outbox_batch(
    source_cursor: "psycopg2.extensions.cursor", batch_size: int
) -> list[tuple[uuid.UUID, dict[str, Any]]]:
    source_cursor.execute(
        """
        SELECT outbox_id, payload
        FROM trading.outbox
        WHERE published_at IS NULL
          AND event_type = %s
        ORDER BY created_at, outbox_id
        FOR UPDATE SKIP LOCKED
        LIMIT %s
        """,
        (EVENT_TYPE, batch_size),
    )
    return source_cursor.fetchall()


def process_batch(source_cfg: DbConfig, target_cfg: DbConfig, batch_size: int) -> int:
    with psycopg2.connect(source_cfg.dsn()) as source_conn, psycopg2.connect(
        target_cfg.dsn()
    ) as target_conn:
        source_conn.autocommit = False
        target_conn.autocommit = False

        with source_conn.cursor() as source_cur:
            outbox_rows = fetch_outbox_batch(source_cur, batch_size)
            if not outbox_rows:
                source_conn.commit()
                return 0

            outbox_ids: list[uuid.UUID] = []
            target_rows: list[tuple] = []
            for outbox_id, payload in outbox_rows:
                if not isinstance(payload, dict):
                    raise RuntimeError(
                        f"Outbox payload for row {outbox_id} must be a JSON object."
                    )
                outbox_ids.append(outbox_id)
                target_rows.append(map_payload_to_row(payload))

            try:
                with target_conn.cursor() as target_cur:
                    ensure_target_table(target_cur)
                    target_cur.executemany(
                        """
                        INSERT INTO analytics.admin_access_events (
                            access_event_id,
                            admin_user_id,
                            admin_email,
                            client_id,
                            action,
                            occurred_at,
                            details,
                            event_date,
                            event_hour,
                            event_week,
                            event_month,
                            event_quarter,
                            event_year
                        ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                        ON CONFLICT (access_event_id) DO UPDATE SET
                            admin_user_id = EXCLUDED.admin_user_id,
                            admin_email = EXCLUDED.admin_email,
                            client_id = EXCLUDED.client_id,
                            action = EXCLUDED.action,
                            occurred_at = EXCLUDED.occurred_at,
                            details = EXCLUDED.details,
                            event_date = EXCLUDED.event_date,
                            event_hour = EXCLUDED.event_hour,
                            event_week = EXCLUDED.event_week,
                            event_month = EXCLUDED.event_month,
                            event_quarter = EXCLUDED.event_quarter,
                            event_year = EXCLUDED.event_year
                        """,
                        target_rows,
                    )
                target_conn.commit()
            except Exception:
                target_conn.rollback()
                source_conn.rollback()
                raise

            source_cur.execute(
                """
                UPDATE trading.outbox
                SET published_at = now()
                WHERE outbox_id = ANY(%s)
                """,
                (outbox_ids,),
            )
            source_conn.commit()
            return len(outbox_rows)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Process AdminAccessEventCreated rows from trading.outbox into "
            "analytics.admin_access_events."
        )
    )
    parser.add_argument(
        "--run-once",
        action="store_true",
        help="Process until no more events are pending, then exit.",
    )
    parser.add_argument(
        "--batch-size",
        type=int,
        default=200,
        help="Number of outbox rows to process per batch (default: 200).",
    )
    parser.add_argument(
        "--poll-interval-seconds",
        type=int,
        default=10,
        help="Sleep time between polling attempts when not using --run-once (default: 10).",
    )
    return parser.parse_args()


def main() -> int:
    logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
    args = parse_args()
    if args.batch_size <= 0:
        raise RuntimeError("--batch-size must be > 0.")
    if args.poll_interval_seconds <= 0:
        raise RuntimeError("--poll-interval-seconds must be > 0.")

    source_cfg, target_cfg = load_configs()

    if args.run_once:
        total = 0
        while True:
            processed = process_batch(source_cfg, target_cfg, args.batch_size)
            total += processed
            if processed == 0:
                break
        logging.info("Completed one-time run. Processed %s outbox events.", total)
        return 0

    while True:
        processed = process_batch(source_cfg, target_cfg, args.batch_size)
        if processed > 0:
            logging.info("Processed %s outbox events.", processed)
        else:
            logging.debug("No outbox rows ready for event type %s.", EVENT_TYPE)
        time.sleep(args.poll_interval_seconds)


if __name__ == "__main__":
    sys.exit(main())
