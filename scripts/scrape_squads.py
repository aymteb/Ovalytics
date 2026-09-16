#!/usr/bin/env python3

import argparse
import csv
import html
import re
import sys
import time
import urllib.request
from pathlib import Path

from clubs_config import CLUBS

BASE_URL = "https://www.allrugby.com"
CSV_HEADERS = [
    "competitionCode",
    "teamShortName",
    "playerName",
    "position",
    "age",
    "heightCm",
    "weightKg",
    "nationality",
    "contractType",
    "jiffStatus",
    "contractEndDate",
]

POSITION_ALIASES = {
    "pilier": "Pilier",
    "talonneur": "Talonneur",
    "2ème ligne": "2ème ligne",
    "2eme ligne": "2ème ligne",
    "deuxième ligne": "2ème ligne",
    "deuxieme ligne": "2ème ligne",
    "3ème ligne": "3ème ligne",
    "3eme ligne": "3ème ligne",
    "troisième ligne": "3ème ligne",
    "troisieme ligne": "3ème ligne",
    "mêlée": "Mêlée",
    "melee": "Mêlée",
    "demi de mêlée": "Mêlée",
    "demi de melee": "Mêlée",
    "ouverture": "Ouverture",
    "demi d'ouverture": "Ouverture",
    "centre": "Centre",
    "ailier": "Ailier",
    "arrière": "Arrière",
    "arriere": "Arrière",
}


def fetch(url: str) -> str:
    request = urllib.request.Request(url, headers={"User-Agent": "OvalyticsScraper/1.0"})
    with urllib.request.urlopen(request, timeout=30) as response:
        return response.read().decode("utf-8")


def parse_int(value: str) -> str:
    match = re.search(r"\d+", value or "")
    return match.group(0) if match else ""


def parse_height_cm(value: str) -> str:
    match = re.search(r"(\d+)\s*(?:[.,]\s*(\d+))?\s*m", value or "")
    if match is None:
        return ""
    whole = int(match.group(1))
    fraction = match.group(2)
    if fraction:
        return str(whole * 100 + int(fraction))
    return str(whole * 100)


def parse_weight_kg(value: str) -> str:
    return parse_int(value)


def parse_contract_end(duree: str) -> str:
    match = re.search(r"(20\d{2})", duree or "")
    if match is None:
        return ""
    return f"{match.group(1)}-06-30"


def clean_name(raw: str) -> str:
    text = html.unescape(raw or "")
    text = re.sub(r"<[^>]+>", "", text)
    text = re.sub(r"\s*\(\d+\)", "", text)
    return re.sub(r"\s+", " ", text).strip()


def normalize_position(raw: str) -> str:
    cleaned = " ".join(raw.split())
    if not cleaned:
        return ""
    key = cleaned.lower().replace("é", "e").replace("è", "e")
    key = key.replace("ê", "e").replace("ô", "o")
    for alias, canonical in POSITION_ALIASES.items():
        if alias.replace("é", "e").replace("è", "e") == key:
            return canonical
    return cleaned


def mercato_class_to_status(css_class: str) -> tuple[str, str]:
    if css_class == "jiff":
        return "PRO", "JIFF"
    if css_class == "nonjiff":
        return "PRO", "NON_JIFF"
    if css_class == "espoir":
        return "ESPOIR", ""
    if css_class == "espoirnonjiff":
        return "ESPOIR", "ESPOIR_NON_JIFF"
    return "PRO", ""


def parse_effectif_rows(html_text: str) -> dict[str, dict]:
    players: dict[str, dict] = {}
    for row_html in re.findall(r'<tr data-pays="[^"]*"[^>]*>.*?</tr>', html_text, re.S):
        pays_match = re.search(r'data-pays="([^"]*)"', row_html)
        name_match = re.search(
            r'class="nom"[^>]*>\s*(?:<a[^>]+>)?\s*([^<]+)',
            row_html,
        )
        if name_match is None:
            continue
        name = clean_name(name_match.group(1))
        if not name:
            continue

        pos_match = re.search(
            r'class="nom"[^>]*>.*?</td>\s*<td class="txtcenter(?: pos)?">([^<]+)',
            row_html,
            re.S,
        )
        position = normalize_position(pos_match.group(1) if pos_match else "")

        age_match = re.search(r'class="txtcenter age">([^<]+)', row_html)
        age = parse_int(age_match.group(1) if age_match else "")

        tai_match = re.search(r'class="txtcenter tai">([^<]+)', row_html)
        height_cm = parse_height_cm(tai_match.group(1) if tai_match else "")

        poi_match = re.search(r'class="txtcenter poi">([^<]+)', row_html)
        weight_kg = parse_weight_kg(poi_match.group(1) if poi_match else "")

        con_match = re.search(r'class="txtcenter con">([^<]+)', row_html)
        contract_type = (con_match.group(1).strip() if con_match else "").upper()
        if contract_type not in {"PRO", "ESPOIR"}:
            contract_type = "PRO"

        dur_match = re.search(r'class="txtcenter dur">([^<]+)', row_html)
        contract_end = parse_contract_end(dur_match.group(1) if dur_match else "")

        nationality = pays_match.group(1).strip() if pays_match else ""

        players[name.casefold()] = {
            "playerName": name,
            "position": position,
            "age": age,
            "heightCm": height_cm,
            "weightKg": weight_kg,
            "nationality": nationality,
            "contractType": contract_type,
            "contractEndDate": contract_end,
        }
    return players


def parse_mercato_squad(html_text: str) -> list[dict]:
    block_match = re.search(
        r'<div class="grid-2 transferts_club[^"]*">(.*)',
        html_text,
        re.S,
    )
    if block_match is None:
        return []

    block = block_match.group(1)
    players: list[dict] = []
    for column in re.finditer(
        r'<div class="h3-like">([^<]+)</div>\s*<ol>(.*?)</ol>',
        block,
        re.S,
    ):
        position = normalize_position(column.group(1))
        for li_match in re.finditer(r"<li>(.*?)</li>", column.group(2), re.S):
            inner = li_match.group(1)
            span_match = re.search(
                r'<span class="(jiff|nonjiff|espoir|espoirnonjiff)">(.*?)</span>',
                inner,
                re.S,
            )
            if span_match is None:
                continue
            name = clean_name(span_match.group(2))
            if not name:
                continue
            contract_type, jiff_status = mercato_class_to_status(span_match.group(1))
            age_match = re.search(r"(\d+)\s*ans", inner)
            pays_match = re.search(r'alt="([^"]*)"', inner)
            players.append(
                {
                    "playerName": name,
                    "position": position,
                    "age": age_match.group(1) if age_match else "",
                    "nationality": pays_match.group(1).strip() if pays_match else "",
                    "contractType": contract_type,
                    "jiffStatus": jiff_status,
                }
            )
    return players


def scrape_club(
    competition_code: str,
    short_name: str,
    club_slug: str,
    mercato_slug: str,
) -> list[dict]:
    effectif_map: dict[str, dict] = {}
    try:
        effectif_html = fetch(f"{BASE_URL}/clubs/{club_slug}/effectif")
        effectif_map = parse_effectif_rows(effectif_html)
    except urllib.error.URLError as error:
        print(f"effectif ignoré {club_slug}: {error}", file=sys.stderr)

    mercato_players: list[dict] = []
    try:
        mercato_html = fetch(f"{BASE_URL}/dossiers/{mercato_slug}.html")
        mercato_players = parse_mercato_squad(mercato_html)
    except urllib.error.URLError as error:
        print(f"mercato ignoré {mercato_slug}: {error}", file=sys.stderr)

    source = mercato_players if mercato_players else [
        effectif_map[key] for key in sorted(effectif_map.keys(), key=lambda k: effectif_map[k]["playerName"])
    ]

    rows: list[dict] = []
    seen: set[str] = set()
    for player in source:
        key = player["playerName"].casefold()
        if key in seen:
            continue
        seen.add(key)
        extra = effectif_map.get(key, {})
        merged = {**extra, **player}
        if not merged.get("position") and extra.get("position"):
            merged["position"] = extra["position"]
        if not merged.get("age") and extra.get("age"):
            merged["age"] = extra["age"]
        if not merged.get("nationality") and extra.get("nationality"):
            merged["nationality"] = extra["nationality"]
        if not merged.get("contractEndDate") and extra.get("contractEndDate"):
            merged["contractEndDate"] = extra["contractEndDate"]
        rows.append(
            {
                "competitionCode": competition_code,
                "teamShortName": short_name,
                "playerName": merged["playerName"],
                "position": merged.get("position", ""),
                "age": merged.get("age", ""),
                "heightCm": merged.get("heightCm", ""),
                "weightKg": merged.get("weightKg", ""),
                "nationality": merged.get("nationality", ""),
                "contractType": merged.get("contractType", "PRO"),
                "jiffStatus": merged.get("jiffStatus", ""),
                "contractEndDate": merged.get("contractEndDate", ""),
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


def main() -> int:
    parser = argparse.ArgumentParser(description="Scrap les effectifs All Rugby")
    parser.add_argument("--output", default="data/import/squads.csv")
    parser.add_argument(
        "--teams",
        default="",
        help="Codes clubs séparés par des virgules (ex: BRI,VAL)",
    )
    parser.add_argument("--delay", type=float, default=0.2)
    args = parser.parse_args()

    team_filter = {
        code.strip().upper()
        for code in args.teams.split(",")
        if code.strip()
    }
    clubs = CLUBS if not team_filter else [
        club for club in CLUBS if club[1] in team_filter
    ]

    root = Path(__file__).resolve().parent.parent
    output = (root / args.output).resolve()
    rows = []
    for competition_code, short_name, club_slug, mercato_slug in clubs:
        print(f"  {short_name} ({competition_code})...", file=sys.stderr)
        try:
            rows.extend(scrape_club(competition_code, short_name, club_slug, mercato_slug))
        except urllib.error.URLError as error:
            print(f"ignoré {club_slug}: {error}", file=sys.stderr)
        if args.delay > 0:
            time.sleep(args.delay)

    write_csv(rows, output)
    print(f"{len(rows)} lignes -> {output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
