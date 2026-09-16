#!/usr/bin/env python3

import argparse
import csv
import sys
import time
from pathlib import Path

from flashscore_feed import (
    event_to_row,
    feed_sign,
    fetch_feed,
    parse_events,
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


def pair_key(home: str, away: str) -> tuple[str, str]:
    return tuple(sorted((home, away)))


def collect_event_ids(season: str, competition_filter: str | None) -> dict[tuple[str, str], str]:
    rows = scrape_tournament_pages(USER_AGENT, False, season)
    sign = feed_sign(USER_AGENT)
    for day in range(-30, 40):
        try:
            raw = fetch_feed(f"f_8_{day}_3_fr_1", sign, USER_AGENT)
        except Exception:
            continue
        if len(raw) < 50:
            continue
        for fields in parse_events(raw):
            row = event_to_row(fields, False)
            if row is None:
                continue
            rows.append(row)

    by_pair: dict[tuple[str, str], str] = {}
    for row in rows:
        if competition_filter and row["competitionCode"] != competition_filter:
            continue
        event_id = row.get("flashscoreEventId") or ""
        if not event_id:
            continue
        key = pair_key(row["homeShortName"], row["awayShortName"])
        if key not in by_pair:
            by_pair[key] = event_id
    return by_pair


def write_csv(rows: list[dict], output: Path) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=CSV_HEADERS)
        writer.writeheader()
        for row in rows:
            writer.writerow({key: row.get(key, "") for key in CSV_HEADERS})


def main() -> int:
    parser = argparse.ArgumentParser(description="Scrap Flashscore H2H (confrontations directes)")
    parser.add_argument("--season", default="2026-2027")
    parser.add_argument("--competition", choices=["TOP14", "PROD2", ""], default="")
    parser.add_argument("--delay", type=float, default=0.12)
    parser.add_argument(
        "--output-top14",
        default="data/import/top14-h2h.csv",
    )
    parser.add_argument(
        "--output-prod2",
        default="data/import/prod2-h2h.csv",
    )
    args = parser.parse_args()

    root = Path(__file__).resolve().parent.parent
    competition_filter = args.competition or None

    print("Collecte des event ids Flashscore...", file=sys.stderr)
    by_pair = collect_event_ids(args.season, competition_filter)
    print(f"{len(by_pair)} paires d'équipes", file=sys.stderr)

    top14: list[dict] = []
    prod2: list[dict] = []
    seen: set[tuple[str, str, str, str]] = set()

    for index, (key, event_id) in enumerate(sorted(by_pair.items()), start=1):
        print(f"  [{index}/{len(by_pair)}] {key[0]}-{key[1]} ({event_id})...", file=sys.stderr)
        try:
            rows = scrape_h2h_for_event(event_id, USER_AGENT)
        except Exception as error:
            print(f"    ignoré: {error}", file=sys.stderr)
            rows = []
        for row in rows:
            dedupe = (
                row["competitionCode"],
                row["homeShortName"],
                row["awayShortName"],
                row["kickoffAt"][:10],
            )
            if dedupe in seen:
                continue
            seen.add(dedupe)
            if row["competitionCode"] == "TOP14":
                top14.append(row)
            elif row["competitionCode"] == "PROD2":
                prod2.append(row)
        if args.delay > 0:
            time.sleep(args.delay)

    out_top14 = (root / args.output_top14).resolve()
    out_prod2 = (root / args.output_prod2).resolve()
    write_csv(top14, out_top14)
    write_csv(prod2, out_prod2)
    print(f"{len(top14)} H2H Top 14 -> {out_top14}")
    print(f"{len(prod2)} H2H Pro D2 -> {out_prod2}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
