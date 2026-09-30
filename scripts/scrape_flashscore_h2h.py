#!/usr/bin/env python3

import argparse
import csv
import sys
import time
from pathlib import Path

from flashscore_feed import (
    scrape_h2h_for_event,
    scrape_tournament_pages,
)

USER_AGENT = "OvalyticsScraper/1.0"
CSV_HEADERS = [
    "competitionCode",
    "homeShortName",
    "awayShortName",
    "matchday",
    "kickoffAt",
    "status",
    "homeScore",
    "awayScore",
    "homeTries",
    "awayTries",
]

OUTPUT_CODES = ("TOP14", "PROD2", "NAT", "ERCC", "ERCH", "URC", "PREM")


def write_csv(rows: list[dict], output: Path) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=CSV_HEADERS)
        writer.writeheader()
        for row in rows:
            writer.writerow({key: row.get(key, "") for key in CSV_HEADERS})


def load_event_ids(path: Path) -> list[str]:
    ids: list[str] = []
    seen: set[str] = set()
    for line in path.read_text(encoding="utf-8").splitlines():
        event_id = line.strip()
        if not event_id or event_id.startswith("#") or event_id in seen:
            continue
        seen.add(event_id)
        ids.append(event_id)
    return ids


def collect_event_ids_from_calendars(season: str) -> list[str]:
    rows = scrape_tournament_pages(USER_AGENT, False, season)
    ids: list[str] = []
    seen: set[str] = set()
    for row in rows:
        if row.get("status") != "SCHEDULED":
            continue
        event_id = row.get("flashscoreEventId") or ""
        if not event_id or event_id in seen:
            continue
        seen.add(event_id)
        ids.append(event_id)
    return ids


def scrape_events(event_ids: list[str], delay: float) -> dict[str, list[dict]]:
    buckets: dict[str, list[dict]] = {code: [] for code in OUTPUT_CODES}
    seen: set[tuple[str, str, str, str]] = set()

    for index, event_id in enumerate(event_ids, start=1):
        print(f"  [{index}/{len(event_ids)}] {event_id}...", file=sys.stderr)
        try:
            rows = scrape_h2h_for_event(event_id, USER_AGENT)
        except Exception as error:
            print(f"    ignore: {error}", file=sys.stderr)
            rows = []
        for row in rows:
            code = row["competitionCode"]
            if code not in buckets:
                continue
            dedupe = (
                code,
                row["homeShortName"],
                row["awayShortName"],
                row["kickoffAt"][:10],
            )
            if dedupe in seen:
                continue
            seen.add(dedupe)
            buckets[code].append(row)
        if delay > 0:
            time.sleep(delay)
    return buckets


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Scrap Flashscore H2H a partir d'event ids (multi-competition)"
    )
    parser.add_argument("--event-ids-file", default="")
    parser.add_argument("--season", default="2026-2027")
    parser.add_argument("--delay", type=float, default=0.12)
    parser.add_argument("--output-top14", default="data/import/top14-h2h.csv")
    parser.add_argument("--output-prod2", default="data/import/prod2-h2h.csv")
    parser.add_argument("--output-nationale", default="data/import/nationale-h2h.csv")
    parser.add_argument("--output-ercc", default="data/import/champions-cup-h2h.csv")
    parser.add_argument("--output-erch", default="data/import/challenge-cup-h2h.csv")
    parser.add_argument("--output-urc", default="data/import/urc-h2h.csv")
    parser.add_argument("--output-prem", default="data/import/premiership-h2h.csv")
    args = parser.parse_args()

    root = Path(__file__).resolve().parent.parent

    if args.event_ids_file:
        ids_path = Path(args.event_ids_file)
        if not ids_path.is_absolute():
            ids_path = (root / ids_path).resolve()
        event_ids = load_event_ids(ids_path)
        print(f"{len(event_ids)} event ids depuis {ids_path}", file=sys.stderr)
    else:
        print("Collecte event ids depuis calendriers Flashscore...", file=sys.stderr)
        event_ids = collect_event_ids_from_calendars(args.season)
        print(f"{len(event_ids)} matchs SCHEDULED", file=sys.stderr)

    buckets = scrape_events(event_ids, args.delay)

    outputs = {
        "TOP14": args.output_top14,
        "PROD2": args.output_prod2,
        "NAT": args.output_nationale,
        "ERCC": args.output_ercc,
        "ERCH": args.output_erch,
        "URC": args.output_urc,
        "PREM": args.output_prem,
    }
    for code, relative in outputs.items():
        output = Path(relative)
        if not output.is_absolute():
            output = (root / output).resolve()
        rows = buckets[code]
        write_csv(rows, output)
        print(f"{len(rows)} H2H {code} -> {output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
