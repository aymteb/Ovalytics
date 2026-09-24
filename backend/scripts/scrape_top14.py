#!/usr/bin/env python3

import argparse
import csv
import html
import json
import re
import sys
import time
import urllib.error
import urllib.request
from datetime import date
from pathlib import Path

from flashscore_feed import merge_rows, scrape_flashscore

USER_AGENT = "OvalyticsScraper/1.0 (+https://github.com/)"

LNR_SLUG_TO_SHORT = {
    "toulouse": "TOU",
    "racing-92": "RAC",
    "paris": "SFP",
    "toulon": "TOL",
    "la-rochelle": "LAR",
    "bordeaux-begles": "UBB",
    "clermont": "ASM",
    "lyon": "LOU",
    "montpellier": "MHR",
    "castres": "CAS",
    "pau": "PAU",
    "bayonne": "BAY",
    "perpignan": "USAP",
    "vannes": "VAN",
    "montauban": "MTB",
}

PROD2_SLUG_TO_SHORT = {
    "biarritz": "BIA",
    "nice": "NIC",
    "angouleme": "ANG",
    "colomiers": "COL",
    "beziers": "BEZ",
    "oyonnax": "OYO",
    "dax": "DAX",
    "narbonne": "NAR",
    "grenoble": "GRE",
    "aurillac": "AUR",
    "nevers": "NEV",
    "montauban": "MTB",
    "provence-rugby": "AIX",
    "agen": "AGE",
    "brive": "BRI",
    "valence-romans": "VAL",
}

COMPETITION_CONFIG = {
    "TOP14": {
        "code": "TOP14",
        "calendar_base": "https://top14.lnr.fr/calendrier-et-resultats",
        "match_base": "https://top14.lnr.fr/feuille-de-match",
        "slugs": LNR_SLUG_TO_SHORT,
        "default_output": "data/import/top14-matches.csv",
    },
    "PROD2": {
        "code": "PROD2",
        "calendar_base": "https://prod2.lnr.fr/calendrier-et-resultats",
        "match_base": "https://prod2.lnr.fr/feuille-de-match",
        "slugs": PROD2_SLUG_TO_SHORT,
        "default_output": "data/import/prod2-matches.csv",
    },
}

DEMO_SLUG_REMAP = {"montauban": "vannes"}

FRENCH_MONTHS = {
    "janvier": 1,
    "fevrier": 2,
    "février": 2,
    "mars": 3,
    "avril": 4,
    "mai": 5,
    "juin": 6,
    "juillet": 7,
    "aout": 8,
    "août": 8,
    "septembre": 9,
    "octobre": 10,
    "novembre": 11,
    "decembre": 12,
    "décembre": 12,
}

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


def default_season() -> str:
    today = date.today()
    if today.month >= 8:
        return f"{today.year}-{today.year + 1}"
    return f"{today.year - 1}-{today.year}"


def season_start_year(season: str) -> int:
    return int(season.split("-")[0])


def parse_french_fixture_date(text: str, season: str) -> tuple[int, int, int] | None:
    if "–" in text:
        text = text.split("–", 1)[0]
    if " - " in text:
        text = text.split(" - ", 1)[0]
    cleaned = " ".join(text.lower().split())
    match = re.search(
        r"(\d{1,2})\s+"
        r"(janvier|février|fevrier|mars|avril|mai|juin|juillet|août|aout|"
        r"septembre|octobre|novembre|décembre|decembre)",
        cleaned,
    )
    if match is None:
        return None
    day = int(match.group(1))
    month = FRENCH_MONTHS[match.group(2)]
    start_year = season_start_year(season)
    year = start_year if month >= 8 else start_year + 1
    return year, month, day


def parse_match_time(chunk: str) -> tuple[str, str] | None:
    time_match = re.search(r"match-line__time\">(\d{2})h(\d{2})", chunk)
    if time_match:
        return time_match.group(1), time_match.group(2)
    time_text = re.search(r"match-line__time\">([^<]+)", chunk)
    if time_text is None:
        return None
    label = time_text.group(1).strip()
    if label in {"", "-"}:
        return "16", "35"
    return None


def parse_calendar_kickoffs(page: str, season: str) -> dict[str, str]:
    kickoffs: dict[str, str] = {}
    current_ymd: tuple[int, int, int] | None = None
    chunks = re.split(
        r"(?=calendar-results__fixture-date|match-calendar-line__match)",
        page,
    )
    for chunk in chunks:
        if "calendar-results__fixture-date" in chunk[:60]:
            date_match = re.search(r"fixture-date[^>]*>\s*([^<]+)", chunk)
            if date_match:
                current_ymd = parse_french_fixture_date(date_match.group(1), season)
        elif "match-calendar-line__match" in chunk[:60]:
            if current_ymd is None:
                continue
            time_parts = parse_match_time(chunk)
            path_match = re.search(
                r"feuille-de-match/(" + re.escape(season) + r"/[^\"']+)",
                chunk,
            )
            if time_parts is None or path_match is None:
                continue
            year, month, day = current_ymd
            hour, minute = time_parts
            kickoffs[path_match.group(1)] = (
                f"{year}-{month:02d}-{day:02d}T{hour}:{minute}:00"
            )
    return kickoffs


def fetch(url: str) -> str:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=30) as response:
        return response.read().decode("utf-8")


def season_rounds(season: str, calendar_base: str) -> list[tuple[str, int]]:
    page = fetch(f"{calendar_base}/{season}")
    start = page.find(":filter-list='") + len(":filter-list='")
    end = page.find("' :current-week=", start)
    if start < len(":filter-list='") or end < 0:
        raise RuntimeError("Impossible de lire la liste des journées sur LNR")

    data = json.loads(html.unescape(page[start:end]))
    season_id = None
    for item in data.get("seasons", []):
        if item.get("name") == season:
            season_id = str(item["id"])
            break
    if season_id is None:
        raise RuntimeError(f"Saison introuvable sur LNR: {season}")

    rounds = []
    for week in data.get("weeks", {}).get(season_id, []):
        slug = week.get("slug", "")
        number = week.get("number")
        if slug and number is not None:
            rounds.append((slug, int(number)))
    return rounds


def select_rounds(
    rounds: list[tuple[str, int]],
    from_week: str,
    to_week: str,
    include_playoffs: bool,
) -> list[tuple[str, int]]:
    start_num = int(from_week[1:])
    end_num = int(to_week[1:])
    selected = []
    for slug, matchday in rounds:
        if start_num <= matchday <= end_num:
            selected.append((slug, matchday))
        elif include_playoffs and matchday > end_num:
            selected.append((slug, matchday))
    return selected


def split_lnr_clubs(club_part: str, slug_map: dict[str, str]) -> tuple[str, str] | None:
    slugs = sorted(slug_map.keys(), key=len, reverse=True)
    for home in slugs:
        prefix = home + "-"
        if club_part.startswith(prefix):
            away = club_part[len(prefix):]
            if away in slug_map:
                return home, away
    return None


def parse_week_page(
    season: str,
    week_slug: str,
    matchday: int,
    calendar_base: str,
    slug_map: dict[str, str],
) -> list[dict]:
    page = fetch(f"{calendar_base}/{season}/{week_slug}")
    kickoff_by_path = parse_calendar_kickoffs(page, season)
    matches = []
    seen = set()

    for path in re.findall(
        r"feuille-de-match/(" + re.escape(season) + r"/" + re.escape(week_slug) + r"/\d+-[^\"'\s]+)",
        page,
    ):
        if path in seen:
            continue
        seen.add(path)

        tail = path.rsplit("/", 1)[-1]
        club_part = tail.split("-", 1)[1]
        clubs = split_lnr_clubs(club_part, slug_map)
        if clubs is None:
            continue

        home_slug, away_slug = clubs
        snippet_start = max(0, page.find(path) - 800)
        snippet = page[snippet_start:page.find(path) + 200]
        score_match = re.search(r"match-line__score[^>]*>\s*(\d+)\s*-\s*(\d+)", snippet)
        finished = score_match is not None
        home_score = score_match.group(1) if score_match else ""
        away_score = score_match.group(2) if score_match else ""

        matches.append(
            {
                "matchday": matchday,
                "path": path,
                "home_slug": home_slug,
                "away_slug": away_slug,
                "finished": finished,
                "home_score": home_score,
                "away_score": away_score,
                "kickoff": kickoff_by_path.get(path),
            }
        )

    return matches


def parse_kickoff(page: str) -> str | None:
    header = re.search(r"match-header__season-day\">\s*([^<]+)", page)
    if header is None:
        return None
    text = " ".join(header.group(1).split())
    date_match = re.search(r"(\d{2})/(\d{2})/(\d{4})\s*-\s*(\d{2})h(\d{2})", text)
    if date_match is None:
        return None
    day, month, year, hour, minute = date_match.groups()
    return f"{year}-{month}-{day}T{hour}:{minute}:00"


def parse_tries(page: str) -> tuple[str, str]:
    marker = "game-facts='"
    start = page.find(marker)
    if start < 0:
        return "", ""
    start += len(marker)
    end = start
    while end < len(page):
        if page[end] == "'" and page[end - 1] == "]":
            break
        end += 1
    chunk = page[start:end - 1]
    home = len(re.findall(r'"slugSubType":"essai"[^}]*"club":"home"', chunk))
    away = len(re.findall(r'"slugSubType":"essai"[^}]*"club":"away"', chunk))
    if home == 0 and away == 0:
        return "", ""
    return str(home), str(away)


def to_short(slug: str, demo_map: bool, slug_map: dict[str, str]) -> str | None:
    if demo_map and slug in DEMO_SLUG_REMAP:
        slug = DEMO_SLUG_REMAP[slug]
    short = slug_map.get(slug)
    return short


def scrape_lnr(
    config: dict,
    season: str,
    from_week: str,
    to_week: str,
    include_playoffs: bool,
    demo_map: bool,
    delay_seconds: float,
) -> list[dict]:
    calendar_base = config["calendar_base"]
    match_base = config["match_base"]
    slug_map = config["slugs"]
    competition_code = config["code"]

    rounds = season_rounds(season, calendar_base)
    selected = select_rounds(rounds, from_week, to_week, include_playoffs)
    print(
        f"LNR: {len(selected)} phases ({from_week}–{to_week}"
        + (" + phases finales" if include_playoffs else "")
        + ")",
        file=sys.stderr,
    )

    rows = []
    skipped = 0

    for week_slug, matchday in selected:
        print(f"  {week_slug} (n°{matchday})...", file=sys.stderr)
        week_matches = parse_week_page(season, week_slug, matchday, calendar_base, slug_map)
        for match in week_matches:
            home_short = to_short(match["home_slug"], demo_map, slug_map)
            away_short = to_short(match["away_slug"], demo_map, slug_map)
            if home_short is None or away_short is None:
                skipped += 1
                print(
                    f"ignoré {week_slug}: {match['home_slug']} vs {match['away_slug']} (club hors base)",
                    file=sys.stderr,
                )
                continue

            kickoff = match.get("kickoff")
            home_tries = ""
            away_tries = ""
            if delay_seconds > 0:
                time.sleep(delay_seconds)

            try:
                sheet = fetch(f"{match_base}/{match['path']}")
                sheet_kickoff = parse_kickoff(sheet)
                if sheet_kickoff is not None:
                    kickoff = sheet_kickoff
                if match["finished"]:
                    home_tries, away_tries = parse_tries(sheet)
            except urllib.error.URLError as error:
                print(f"feuille de match inaccessible: {match['path']} ({error})", file=sys.stderr)

            if kickoff is None:
                print(f"date introuvable: {match['path']}", file=sys.stderr)
                continue

            status = "FINISHED" if match["finished"] else "SCHEDULED"
            home_score = match["home_score"] if match["finished"] else ""
            away_score = match["away_score"] if match["finished"] else ""

            rows.append(
                {
                    "competitionCode": competition_code,
                    "homeShortName": home_short,
                    "awayShortName": away_short,
                    "matchday": match["matchday"],
                    "kickoffAt": kickoff,
                    "status": status,
                    "homeScore": home_score,
                    "awayScore": away_score,
                    "homeTries": home_tries if status == "FINISHED" else "",
                    "awayTries": away_tries if status == "FINISHED" else "",
                }
            )

    if skipped:
        print(f"{skipped} match(s) ignoré(s)", file=sys.stderr)

    return rows


def load_json(path: Path) -> list[dict]:
    data = json.loads(path.read_text(encoding="utf-8"))
    matches = data.get("matches", data)
    if not isinstance(matches, list):
        raise ValueError("Le JSON doit contenir une liste 'matches'")
    return matches


def write_csv(rows: list[dict], output: Path) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=CSV_HEADERS)
        writer.writeheader()
        for row in rows:
            writer.writerow({key: row.get(key, "") for key in CSV_HEADERS})


def main() -> int:
    parser = argparse.ArgumentParser(description="Produit un CSV matchs LNR pour Spring Batch")
    parser.add_argument(
        "--competition",
        choices=sorted(COMPETITION_CONFIG.keys()),
        default="TOP14",
    )
    parser.add_argument(
        "--source",
        choices=["lnr", "flashscore", "merge", "json"],
        default="lnr",
    )
    parser.add_argument("--season", default=default_season())
    parser.add_argument("--from-week", default="j1")
    parser.add_argument("--to-week", default="j26")
    parser.add_argument(
        "--no-playoffs",
        action="store_true",
        help="Ignorer barrages, demi-finales et finale",
    )
    parser.add_argument(
        "--input",
        default="data/import/top14-source.json",
        help="Fichier JSON si --source json",
    )
    parser.add_argument(
        "--output",
        default="",
    )
    parser.add_argument(
        "--demo-map",
        action=argparse.BooleanOptionalAction,
        default=False,
        help="Remap Montauban LNR -> Vannes (uniquement si le seed démo utilise VAN)",
    )
    parser.add_argument(
        "--delay",
        type=float,
        default=0.15,
        help="Pause entre chaque feuille de match LNR (secondes)",
    )
    parser.add_argument(
        "--fs-from-day",
        type=int,
        default=-7,
        help="Merge : offset jour Flashscore live (négatif = passé)",
    )
    parser.add_argument(
        "--fs-to-day",
        type=int,
        default=3,
        help="Merge : offset jour Flashscore live (positif = à venir)",
    )
    args = parser.parse_args()

    root = Path(__file__).resolve().parent.parent
    config = COMPETITION_CONFIG[args.competition]
    output_path = args.output or config["default_output"]
    output = (root / output_path).resolve()

    include_playoffs = not args.no_playoffs

    if args.source == "json":
        input_path = (root / args.input).resolve()
        rows = load_json(input_path)
    elif args.source == "flashscore":
        rows = scrape_flashscore(
            USER_AGENT,
            args.demo_map,
            args.season,
            None,
            None,
        )
    elif args.source == "merge":
        lnr_rows = scrape_lnr(
            config,
            args.season,
            args.from_week,
            args.to_week,
            include_playoffs,
            args.demo_map,
            args.delay,
        )
        fs_rows = scrape_flashscore(
            USER_AGENT,
            args.demo_map,
            args.season,
            args.fs_from_day,
            args.fs_to_day,
        )
        rows = merge_rows(lnr_rows, fs_rows)
        print(f"merge: {len(lnr_rows)} LNR + {len(fs_rows)} Flashscore -> {len(rows)}", file=sys.stderr)
    else:
        rows = scrape_lnr(
            config,
            args.season,
            args.from_week,
            args.to_week,
            include_playoffs,
            args.demo_map,
            args.delay,
        )

    write_csv(rows, output)
    print(f"{len(rows)} lignes -> {output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
