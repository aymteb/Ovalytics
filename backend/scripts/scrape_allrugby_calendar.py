#!/usr/bin/env python3

import argparse
import csv
import re
import sys
import time
import urllib.request
from datetime import datetime
from pathlib import Path

from allrugby_foreign import COMPETITIONS, LABELS_BY_CODE
from scrape_nationale import (
    CSV_HEADERS,
    SCORE_RE,
    TIME_RE,
    clean_text,
    extract_club_label,
    fetch,
    parse_french_date,
)

USER_AGENT = "OvalyticsScraper/1.0"


def resolve_short(label: str, mapping: dict[str, str]) -> str | None:
    name = clean_text(label)
    if not name:
        return None
    if name in mapping:
        return mapping[name]
    folded = name.casefold()
    for key, short in mapping.items():
        if key.casefold() == folded:
            return short
    for key, short in mapping.items():
        key_fold = key.casefold()
        if key_fold in folded or folded in key_fold:
            return short
    return None


def parse_calendar(html_text: str, competition_code: str, mapping: dict[str, str]) -> list[dict]:
    rows: list[dict] = []
    current_day: datetime | None = None
    matchday = 0
    last_matchday_start: datetime | None = None

    tokens = re.split(
        r'(?is)(<li class="[^"]*sep_dat[^"]*">.*?</li>|<li class="clearfix">.*?</li>)',
        html_text,
    )
    for token in tokens:
        if "sep_dat" in token:
            current_day = parse_french_date(token)
            continue
        if 'class="clearfix"' not in token or 'class="mat"' not in token:
            continue
        if current_day is None:
            continue

        home_div = re.search(r'class="fl log txtright"[^>]*>(.*?)</div>', token, re.I | re.S)
        away_div = re.search(r'class="fl log txtleft"[^>]*>(.*?)</div>', token, re.I | re.S)
        res_match = re.search(r'class="fl res txtcenter"[^>]*>(.*?)</div>', token, re.I | re.S)
        if not home_div or not away_div or not res_match:
            continue

        home_label = extract_club_label(home_div.group(1))
        away_label = extract_club_label(away_div.group(1))
        home = resolve_short(home_label, mapping)
        away = resolve_short(away_label, mapping)
        if home is None or away is None:
            print(
                f"skip club inconnu ({competition_code}): {home_label} / {away_label}",
                file=sys.stderr,
            )
            continue

        res = clean_text(res_match.group(1))
        kickoff = current_day
        status = "SCHEDULED"
        home_score = ""
        away_score = ""

        score = SCORE_RE.match(res)
        time_match = TIME_RE.match(res)
        if score:
            status = "FINISHED"
            home_score = score.group(1)
            away_score = score.group(2)
        elif time_match:
            kickoff = current_day.replace(
                hour=int(time_match.group(1)),
                minute=int(time_match.group(2)),
            )

        if last_matchday_start is None or (current_day - last_matchday_start).days >= 5:
            matchday += 1
            last_matchday_start = current_day

        rows.append(
            {
                "competitionCode": competition_code,
                "homeShortName": home,
                "awayShortName": away,
                "matchday": matchday if matchday > 0 else 1,
                "kickoffAt": kickoff.strftime("%Y-%m-%dT%H:%M:%S"),
                "status": status,
                "homeScore": home_score,
                "awayScore": away_score,
                "homeTries": "",
                "awayTries": "",
            }
        )
    return rows


def write_csv(rows: list[dict], output: Path) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=CSV_HEADERS)
        writer.writeheader()
        for row in rows:
            writer.writerow({key: row.get(key, "") for key in CSV_HEADERS})


def scrape_one(competition_code: str, output: Path) -> int:
    meta = COMPETITIONS[competition_code]
    slug = meta["slug"]
    url = f"https://www.allrugby.com/competitions/{slug}/calendrier.html"
    mapping = LABELS_BY_CODE[competition_code]

    print(f"Fetch {url}", file=sys.stderr)
    html_text = fetch(url)
    time.sleep(0.2)
    rows = parse_calendar(html_text, competition_code, mapping)
    if not rows:
        print(f"Aucun match parse pour {competition_code}", file=sys.stderr)
        return 1

    write_csv(rows, output)
    finished = sum(1 for row in rows if row["status"] == "FINISHED")
    print(f"{competition_code}: {len(rows)} matchs ({finished} termines) -> {output}")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="Scrap calendriers AllRugby (coupe / etranger / tests)")
    parser.add_argument(
        "--competition",
        choices=list(COMPETITIONS.keys()) + ["ALL"],
        default="ALL",
    )
    parser.add_argument("--output", help="Fichier CSV (obligatoire si une seule competition)")
    parser.add_argument(
        "--output-dir",
        help="Dossier CSV (mode ALL). Sinon data/import relatif au repo.",
    )
    args = parser.parse_args()

    root = Path(__file__).resolve().parent.parent
    codes = list(COMPETITIONS.keys()) if args.competition == "ALL" else [args.competition]

    exit_code = 0
    for code in codes:
        if args.output and len(codes) == 1:
            output = Path(args.output)
        elif args.output_dir:
            output = Path(args.output_dir) / Path(COMPETITIONS[code]["csv"]).name
        else:
            output = root / COMPETITIONS[code]["csv"]
        if not output.is_absolute():
            output = (root / output).resolve()
        exit_code = scrape_one(code, output) or exit_code
    return exit_code


if __name__ == "__main__":
    raise SystemExit(main())
