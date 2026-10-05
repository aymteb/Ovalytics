#!/usr/bin/env python3

import argparse
import csv
import html
import re
import sys
import time
import unicodedata
import urllib.error
import urllib.request
from pathlib import Path
from urllib.parse import quote, urlsplit, urlunsplit

from clubs_config import ALL_RUGBY_LABEL_TO_SHORT, CLUBS, PARCOURS_KEYWORDS

BASE_URL = "https://www.allrugby.com"
RUGBYRAMA_BASE = "https://www.rugbyrama.fr"
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
    "photoUrl",
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
    return fetch_with_final_url(url)[0]


def fetch_with_final_url(url: str) -> tuple[str, str]:
    parts = urlsplit(url)
    safe_path = quote(parts.path, safe="/:%")
    safe_url = urlunsplit((parts.scheme, parts.netloc, safe_path, parts.query, parts.fragment))
    request = urllib.request.Request(safe_url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=30) as response:
        return response.read().decode("utf-8"), response.geturl()


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
    seen: set[str] = set()
    for table_match in re.finditer(r"<table\b([^>]*)>(.*?)</table>", html_text, re.S | re.I):
        attrs = table_match.group(1)
        body = table_match.group(2)
        if "eff" not in attrs:
            continue
        if "espoir" in attrs.lower():
            continue
        for row_html in re.findall(r"<tr[^>]*>.*?</tr>", body, re.S):
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
            key = name.casefold()
            if key in seen:
                continue
            seen.add(key)
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


def parse_photo_url(html_text: str, profile_url: str = "") -> str:
    photos = re.findall(r'src="(/photos/[^"]+)"', html_text, re.I)
    if not photos:
        return ""
    slug = profile_url.rstrip("/").split("/")[-1].removesuffix(".html")
    slug = re.sub(r"-\d+$", "", slug).lower()
    slug_compact = slug.replace("-", "")
    for photo in photos:
        photo_lower = photo.lower()
        if slug and (slug in photo_lower or slug_compact in photo_lower.replace("-", "")):
            return BASE_URL + photo
    return ""


def fold_name(value: str) -> str:
    text = html.unescape(value or "")
    text = unicodedata.normalize("NFKD", text)
    text = "".join(char for char in text if not unicodedata.combining(char))
    return re.sub(r"[^a-z0-9]+", "", text.lower())


def rugbyrama_slug(player_name: str) -> str:
    text = unicodedata.normalize("NFKD", player_name or "")
    text = "".join(char for char in text if not unicodedata.combining(char))
    text = text.lower().strip()
    return re.sub(r"[^a-z0-9]+", "-", text).strip("-")


def names_match(player_name: str, candidate: str) -> bool:
    player_key = fold_name(player_name)
    candidate_key = fold_name(candidate)
    if not player_key or not candidate_key:
        return False
    if player_key == candidate_key:
        return True
    tokens = [token for token in re.split(r"\s+", player_name.strip()) if token]
    return bool(tokens) and all(fold_name(token) in candidate_key for token in tokens)


def parse_rugbyrama_photo(html_text: str, player_name: str) -> str:
    player_key = fold_name(player_name)
    if not player_key:
        return ""
    for tag in re.findall(r"<img\b[^>]*>", html_text, re.I):
        alt_match = re.search(r'alt="([^"]*)"', tag, re.I)
        src_match = re.search(
            r'(?:src|data-src)="(https://images\.rugbyrama\.fr[^"]+)"',
            tag,
            re.I,
        )
        if alt_match is None or src_match is None:
            continue
        alt = html.unescape(alt_match.group(1)).strip()
        if not alt or len(alt) > 80:
            continue
        alt_key = fold_name(alt)
        if alt_key == player_key or alt_key.startswith(player_key):
            return src_match.group(1).replace("&amp;", "&")
    return ""


def fetch_rugbyrama_photo(player_name: str) -> str:
    slug = rugbyrama_slug(player_name)
    if not slug:
        return ""
    url = f"{RUGBYRAMA_BASE}/joueur/{slug}"
    try:
        page, final_url = fetch_with_final_url(url)
    except urllib.error.URLError:
        return ""
    if "/joueur/" not in final_url:
        return ""
    heading_match = re.search(r"<h1[^>]*>(.*?)</h1>", page, re.S | re.I)
    heading = clean_name(heading_match.group(1)) if heading_match else ""
    if not names_match(player_name, heading):
        return ""
    return parse_rugbyrama_photo(page, player_name)


def enrich_missing_photos(csv_path: Path, delay: float) -> int:
    with csv_path.open(encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle))
    if not rows:
        print("CSV vide.", file=sys.stderr)
        return 0

    missing = [
        index
        for index, row in enumerate(rows)
        if not (row.get("photoUrl") or "").strip() and (row.get("playerName") or "").strip()
    ]
    print(f"photos manquantes: {len(missing)}/{len(rows)}", file=sys.stderr)
    filled = 0
    for offset, index in enumerate(missing, start=1):
        name = rows[index]["playerName"]
        photo = fetch_rugbyrama_photo(name)
        if photo:
            rows[index]["photoUrl"] = photo
            filled += 1
            print(f"  + {name}", file=sys.stderr)
        if offset % 200 == 0:
            print(f"  {offset}/{len(missing)} (filled {filled})", file=sys.stderr)
        if delay > 0:
            time.sleep(delay)

    fieldnames = list(rows[0].keys())
    with csv_path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)
    print(f"enrichies: {filled}/{len(missing)} -> {csv_path}")
    return filled


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


def season_bounds(label: str) -> tuple[int, int] | None:
    match = re.match(r"^(\d{2})/(\d{2})$", (label or "").strip())
    if match is None:
        return None
    start = int(match.group(1))
    end = int(match.group(2))
    start += 2000 if start < 100 else 0
    end += 2000 if end < 100 else 0
    if end < start:
        end += 100
    return start, end


def club_merge_key(name: str) -> str:
    short = resolve_team_short(name)
    if short:
        return short
    key = name.casefold()
    for noise in (
        " rugby",
        " stade",
        " fc",
        " olympique",
        " club",
        " union",
        " sporting",
    ):
        key = key.replace(noise, " ")
    return re.sub(r"[^a-z0-9]+", "", key)


def parse_parcours_spans(html_text: str) -> list[tuple[str, int, int]]:
    spans: list[tuple[str, int, int]] = []
    for entry in parse_full_parcours(html_text).split("|"):
        item = entry.strip()
        if not item:
            continue
        years = re.search(r"^(.*?)\s*\((\d{4})\s*-\s*(\d{4})\)$", item)
        if years is None:
            continue
        club = years.group(1).strip()
        if club:
            spans.append((club, int(years.group(2)), int(years.group(3))))
    return spans


def parse_stats_spans(html_text: str) -> list[tuple[str, int, int]]:
    table = re.search(
        r'<table class="rtable JOverall"[^>]*>(.*?)</table>',
        html_text,
        re.S,
    )
    if table is None:
        return []

    seasons: list[tuple[int, int, str]] = []
    for row_html in re.findall(r"<tr[^>]*>(.*?)</tr>", table.group(1), re.S):
        cells = re.findall(r"<td[^>]*>(.*?)</td>", row_html, re.S)
        if len(cells) < 4:
            continue
        bounds = season_bounds(cell_text(cells[0]))
        club = cell_text(cells[2])
        if bounds is None or not club:
            continue
        seasons.append((bounds[0], bounds[1], club))

    if not seasons:
        return []

    seasons.sort(key=lambda item: (item[0], item[1], item[2]))
    spans: list[tuple[str, int, int]] = []
    for start, end, club in seasons:
        if spans and club_merge_key(spans[-1][0]) == club_merge_key(club) and start <= spans[-1][2] + 1:
            prev_club, prev_start, prev_end = spans[-1]
            spans[-1] = (prev_club, prev_start, max(prev_end, end))
        else:
            spans.append((club, start, end))
    return spans


def merge_career_spans(spans: list[tuple[str, int, int]]) -> str:
    if not spans:
        return ""

    merged: dict[str, tuple[str, int, int]] = {}
    order: list[str] = []
    for club, start, end in sorted(spans, key=lambda item: (item[1], item[2], item[0])):
        key = club_merge_key(club) or club.casefold()
        if key not in merged:
            merged[key] = (club, start, end)
            order.append(key)
            continue
        prev_club, prev_start, prev_end = merged[key]
        display = prev_club if len(prev_club) >= len(club) else club
        merged[key] = (display, min(prev_start, start), max(prev_end, end))

    ordered = sorted((merged[key] for key in order), key=lambda item: (item[1], item[2], item[0]))
    collapsed: list[tuple[str, int, int]] = []
    for club, start, end in ordered:
        if collapsed and club_merge_key(collapsed[-1][0]) == club_merge_key(club) and start <= collapsed[-1][2] + 1:
            prev_club, prev_start, prev_end = collapsed[-1]
            display = prev_club if len(prev_club) >= len(club) else club
            collapsed[-1] = (display, prev_start, max(prev_end, end))
        else:
            collapsed.append((club, start, end))

    return " | ".join(f"{club} ({start} - {end})" for club, start, end in collapsed)


def parse_career_history(html_text: str) -> str:
    spans = parse_stats_spans(html_text) + parse_parcours_spans(html_text)
    return merge_career_spans(spans)


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
            club_short = resolve_team_short(texts[2])
            opponent_short = resolve_team_short(texts[3])
            if not club_short or not opponent_short:
                continue
            venue = (texts[4] or "").strip().upper()
            if venue.startswith("EXT"):
                home_short = opponent_short
                away_short = club_short
            else:
                home_short = club_short
                away_short = opponent_short
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
    career_history = parse_career_history(page)
    photo_url = parse_photo_url(page, profile_url)
    if not photo_url:
        photo_url = fetch_rugbyrama_photo(player_name)
    appearances = parse_match_appearances(page, competition_code, short_name, player_name)
    profile = {
        "competitionCode": competition_code,
        "teamShortName": short_name,
        "playerName": player_name,
        "profileUrl": profile_url,
        "photoUrl": photo_url,
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
    parser.add_argument(
        "--enrich-missing-photos",
        action="store_true",
        help="Complete photoUrl vides via Rugbyrama (CSV existant, sans re-scrap AllRugby).",
    )
    args = parser.parse_args()

    if args.enrich_missing_photos:
        enrich_missing_photos(args.output.resolve(), args.delay)
        return 0

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
