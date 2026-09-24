#!/usr/bin/env python3

import argparse
import csv
import html
import re
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path
from urllib.parse import quote, urlsplit, urlunsplit

from clubs_config import ALL_RUGBY_LABEL_TO_SHORT, CLUBS, PARCOURS_KEYWORDS

BASE_URL = "https://www.allrugby.com"
USER_AGENT = "OvalyticsScraper/1.0"
DEFAULT_OUTPUT = Path(__file__).resolve().parent.parent / "data" / "import" / "player-profiles.csv"

DEFAULT_APPEARANCES_OUTPUT = (
    Path(__file__).resolve().parent.parent / "data" / "import" / "player-appearances.csv"
)

CSV_HEADERS = [
    "competitionCode",
    "teamShortName",
    "playerName",
    "profileUrl",
    "seasonMatches",
    "seasonStarts",
    "seasonMinutes",
    "seasonTries",
    "seasonYellowCards",
    "seasonRedCards",
    "contractEndDate",
    "careerHistory",
]

APPEARANCE_HEADERS = [
    "competitionCode",
    "teamShortName",
    "playerName",
    "matchday",
    "homeShortName",
    "awayShortName",
    "jerseyNumber",
    "minutesPlayed",
    "tries",
    "yellowCards",
    "redCards",
]


def fetch(url: str) -> str:
    parts = urlsplit(url)
    safe_path = quote(parts.path, safe="/:%")
    safe_url = urlunsplit((parts.scheme, parts.netloc, safe_path, parts.query, parts.fragment))
    request = urllib.request.Request(safe_url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=30) as response:
        return response.read().decode("utf-8")


def clean_name(raw: str) -> str:
    text = html.unescape(raw or "")
    text = re.sub(r"<[^>]+>", "", text)
    text = re.sub(r"\s*\(\d+\)", "", text)
    return re.sub(r"\s+", " ", text).strip()


def parse_int(value: str) -> str:
    match = re.search(r"\d+", value or "")
    return match.group(0) if match else ""


def parse_minutes(value: str) -> str:
    if not value:
        return ""
    match = re.search(r"(\d+)", value.replace("'", ""))
    return match.group(1) if match else ""


def parse_effectif_links(html_text: str) -> list[dict]:
    players: list[dict] = []
    for row_html in re.findall(r'<tr data-pays="[^"]*"[^>]*>.*?</tr>', html_text, re.S):
        link_match = re.search(r'href="(/joueurs/[^"]+\.html)"', row_html)
        name_match = re.search(
            r'class="nom"[^>]*>\s*(?:<a[^>]+>)?\s*([^<]+)',
            row_html,
        )
        if link_match is None or name_match is None:
            continue
        name = clean_name(name_match.group(1))
        if not name:
            continue
        players.append(
            {
                "playerName": name,
                "profileUrl": BASE_URL + link_match.group(1),
            }
        )
    return players


def cell_text(cell_html: str) -> str:
    text = re.sub(r"<[^>]+>", " ", cell_html)
    return re.sub(r"\s+", " ", text).strip()


def parse_cartons(cell_html: str) -> tuple[int, int]:
    yellow = len(re.findall(r'class="[^"]*jaune[^"]*"', cell_html, re.I))
    red = len(re.findall(r'class="[^"]*rouge[^"]*"', cell_html, re.I))
    if yellow == 0 and red == 0:
        for css_class in re.findall(r'class="([^"]+)"', cell_html):
            clean = css_class.strip().lower()
            if not clean or clean in {"cartons", ""}:
                continue
            if "rouge" in clean:
                red += 1
            elif "jaune" in clean or "orange" in clean:
                yellow += 1
    return yellow, red


def parse_current_season_row(html_text: str) -> dict[str, str]:
    row_match = re.search(
        r'<table class="rtable JOverall"[^>]*>.*?<tbody>\s*<tr[^>]*class="[^"]*sepSaison[^"]*"[^>]*>(.*?)</tr>',
        html_text,
        re.S,
    )
    if row_match is None:
        return {}

    cells = re.findall(r"<td[^>]*>(.*?)</td>", row_match.group(1), re.S)
    if len(cells) < 14:
        return {}

    yellow, red = parse_cartons(cells[12])
    return {
        "seasonMatches": parse_int(cell_text(cells[4])),
        "seasonStarts": parse_int(cell_text(cells[6])),
        "seasonTries": parse_int(cell_text(cells[7])),
        "seasonMinutes": parse_minutes(cell_text(cells[13])),
        "seasonYellowCards": str(yellow) if yellow else "",
        "seasonRedCards": str(red) if red else "",
    }


def parse_full_parcours(html_text: str) -> str:
    block = re.search(
        r'<div class="h2-like">Parcours</div>\s*<ul>(.*?)</ul>',
        html_text,
        re.S,
    )
    if block is None:
        return ""

    entries: list[str] = []
    for item_html in re.findall(r"<li>(.*?)</li>", block.group(1), re.S):
        item = clean_name(item_html)
        if item:
            entries.append(item)
    return " | ".join(entries)


def resolve_team_short(label: str) -> str:
    clean = clean_name(label)
    if not clean:
        return ""
    lower = clean.lower()
    for name, short in sorted(
        ALL_RUGBY_LABEL_TO_SHORT.items(),
        key=lambda entry: len(entry[0]),
        reverse=True,
    ):
        if name.lower() in lower or lower in name.lower():
            return short
    return ""


def parse_matchday(label: str) -> str:
    match = re.search(r"J(\d+)", label or "", re.I)
    return match.group(1) if match else ""


def parse_match_appearances(
    html_text: str,
    competition_code: str,
    team_short_name: str,
    player_name: str,
) -> list[dict]:
    rows: list[dict] = []
    for table_html in re.findall(
        r'<table class="rtable JSaison fdmj fdmjwithpub".*?</table>',
        html_text,
        re.S,
    ):
        for row_html in re.findall(r"<tr[^>]*>(.*?)</tr>", table_html, re.S):
            cells = re.findall(r"<td[^>]*>(.*?)</td>", row_html, re.S)
            if len(cells) < 10:
                continue
            texts = [cell_text(cell) for cell in cells]
            if not texts[2] or not texts[3]:
                continue
            matchday = parse_matchday(texts[1])
            if not matchday:
                continue
            home_short = resolve_team_short(texts[2])
            away_short = resolve_team_short(texts[3])
            if not home_short or not away_short:
                continue
            minutes = parse_minutes(texts[-1])
            rows.append(
                {
                    "competitionCode": competition_code,
                    "teamShortName": team_short_name,
                    "playerName": player_name,
                    "matchday": matchday,
                    "homeShortName": home_short,
                    "awayShortName": away_short,
                    "jerseyNumber": parse_int(texts[7]),
                    "minutesPlayed": minutes,
                    "tries": parse_int(texts[8]) if len(texts) > 8 else "",
                    "yellowCards": "",
                    "redCards": "",
                }
            )
    return rows


def parse_parcours_contract(html_text: str, short_name: str) -> str:
    block = re.search(
        r'<div class="h2-like">Parcours</div>\s*<ul>(.*?)</ul>',
        html_text,
        re.S,
    )
    if block is None:
        return ""

    keywords = PARCOURS_KEYWORDS.get(short_name, [])
    for item_html in re.findall(r"<li>(.*?)</li>", block.group(1), re.S):
        item = clean_name(item_html)
        if not item:
            continue
        if keywords and not any(keyword.lower() in item.lower() for keyword in keywords):
            continue
        years = re.search(r"\((\d{4})\s*-\s*(\d{4})\)", item)
        if years is None:
            continue
        return f"{years.group(2)}-06-30"
    return ""


def scrape_profile(
    competition_code: str,
    short_name: str,
    player_name: str,
    profile_url: str,
) -> tuple[dict, list[dict]]:
    try:
        page = fetch(profile_url)
    except urllib.error.URLError as error:
        print(f"  profil ignoré {player_name}: {error}", file=sys.stderr)
        return (
            {
                "competitionCode": competition_code,
                "teamShortName": short_name,
                "playerName": player_name,
                "profileUrl": profile_url,
            },
            [],
        )

    stats = parse_current_season_row(page)
    contract_end = parse_parcours_contract(page, short_name)
    career_history = parse_full_parcours(page)
    appearances = parse_match_appearances(page, competition_code, short_name, player_name)
    profile = {
        "competitionCode": competition_code,
        "teamShortName": short_name,
        "playerName": player_name,
        "profileUrl": profile_url,
        "contractEndDate": contract_end,
        "careerHistory": career_history,
        **stats,
    }
    return profile, appearances


def scrape_club(
    competition_code: str,
    short_name: str,
    club_slug: str,
    delay: float,
) -> tuple[list[dict], list[dict]]:
    effectif_url = f"{BASE_URL}/clubs/{club_slug}/effectif"
    try:
        effectif_html = fetch(effectif_url)
    except urllib.error.URLError as error:
        print(f"effectif ignoré {club_slug}: {error}", file=sys.stderr)
        return [], []

    profiles: list[dict] = []
    appearances: list[dict] = []
    for entry in parse_effectif_links(effectif_html):
        profile, player_appearances = scrape_profile(
            competition_code,
            short_name,
            entry["playerName"],
            entry["profileUrl"],
        )
        profiles.append(profile)
        appearances.extend(player_appearances)
        if delay > 0:
            time.sleep(delay)
    return profiles, appearances


def write_csv(rows: list[dict], output: Path, headers: list[str]) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=headers)
        writer.writeheader()
        for row in rows:
            writer.writerow({key: row.get(key, "") for key in headers})


def filter_clubs(team_filter: set[str]) -> list[tuple[str, str, str, str]]:
    if not team_filter:
        return list(CLUBS)
    return [
        club
        for club in CLUBS
        if club[1] in team_filter
    ]


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Scrap fiches joueurs All Rugby (stats, parcours, feuilles de match)"
    )
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument(
        "--appearances-output",
        type=Path,
        default=DEFAULT_APPEARANCES_OUTPUT,
    )
    parser.add_argument(
        "--teams",
        default="",
        help="Codes clubs à enrichir (ex: BRI,VAL). Fiches joueurs uniquement, pas les scores match.",
    )
    parser.add_argument("--delay", type=float, default=0.15)
    args = parser.parse_args()

    team_filter = {
        code.strip().upper()
        for code in args.teams.split(",")
        if code.strip()
    }
    selected = filter_clubs(team_filter)
    if not selected:
        print("Aucun club correspondant.", file=sys.stderr)
        return 1

    profiles: list[dict] = []
    appearances: list[dict] = []
    for competition_code, short_name, club_slug, _mercato_slug in selected:
        print(f"  {short_name} ({competition_code})...", file=sys.stderr)
        club_profiles, club_appearances = scrape_club(
            competition_code,
            short_name,
            club_slug,
            args.delay,
        )
        profiles.extend(club_profiles)
        appearances.extend(club_appearances)

    profiles_output = args.output.resolve()
    appearances_output = args.appearances_output.resolve()
    write_csv(profiles, profiles_output, CSV_HEADERS)
    write_csv(appearances, appearances_output, APPEARANCE_HEADERS)
    print(f"{len(profiles)} fiches -> {profiles_output}")
    print(f"{len(appearances)} feuilles -> {appearances_output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
