#!/usr/bin/env python3

import argparse
import csv
import html
import re
import sys
import time
import urllib.error
import urllib.request
from datetime import date
from pathlib import Path

from clubs_config import CLUBS

BASE_URL = "https://www.allrugby.com"
CSV_HEADERS = [
    "competitionCode",
    "playerName",
    "type",
    "transferDate",
    "fromClub",
    "toClub",
    "contractLength",
]

SECTION_TYPES = {
    "Arrivées": "JOIN",
    "Départs": "LEAVE",
    "Partis en cours de saison": "LEAVE",
    "Prolongations": "EXTENSION",
    "En Fin de contrat": "CONTRACT_END",
}


def competition_shorts(competition_code: str) -> frozenset[str]:
    return frozenset(
        short for code, short, _club_slug, _mercato in CLUBS if code == competition_code
    )


def row_relevance_score(row: dict) -> int:
    shorts = competition_shorts(row["competitionCode"])
    transfer_type = row["type"]
    if transfer_type == "JOIN":
        club = row["toClub"]
    elif transfer_type in {"LEAVE", "CONTRACT_END"}:
        club = row["fromClub"]
    else:
        club = row["fromClub"]
    score = 0
    if club in shorts:
        score += 20
    if club and len(club) <= 5 and club.upper() == club:
        score += 10
    return score


def dedupe_rows(rows: list[dict]) -> list[dict]:
    best: dict[tuple[str, str, str, str], dict] = {}
    order: list[tuple[str, str, str, str]] = []
    for row in rows:
        key = (
            row["competitionCode"],
            row["playerName"],
            row["transferDate"],
            row["type"],
        )
        if key not in best:
            best[key] = row
            order.append(key)
            continue
        if row_relevance_score(row) > row_relevance_score(best[key]):
            best[key] = row
    return [best[key] for key in order]


def fetch(url: str) -> str:
    request = urllib.request.Request(url, headers={"User-Agent": "OvalyticsScraper/1.0"})
    with urllib.request.urlopen(request, timeout=30) as response:
        return response.read().decode("utf-8")


def parse_player_name(li_html: str) -> str:
    match = re.search(r"<a[^>]+><b>\s*(.*?)</b></a>", li_html, re.S)
    if match is None:
        return ""
    text = re.sub(r"<[^>]+>", "", match.group(1))
    text = html.unescape(text)
    text = re.sub(r"\s*\(\d+\)", "", text)
    return re.sub(r"\s+", " ", text).strip()


def parse_destination(li_html: str) -> str:
    text = re.sub(r"<[^>]+>", " ", li_html)
    text = " ".join(text.split())
    if "," not in text:
        return ""
    tail = text.split(",")[-1].strip()
    tail = re.sub(r"^\d+\s+ans\s*", "", tail, flags=re.I).strip()
    for noise in (
        "ESPOIR",
        "JIFF",
        "NON JIFF",
        "NON-JIFF",
        "ESPOIR NON JIFF",
        "Retour de prêt",
        "retour de prêt",
    ):
        if noise.lower() in tail.lower():
            return ""
    if re.fullmatch(r"20\d{2}", tail):
        return ""
    for prefix in ("Prêté à ", "prêté à ", "en provenance de ", "En provenance de "):
        if tail.lower().startswith(prefix.lower()):
            return tail[len(prefix) :].strip()
    return tail


def cut_section_body(body: str, transfer_type: str) -> str:
    cut = body
    stops = ["grid-2 transferts_club", "<h2"]
    if transfer_type != "CONTRACT_END":
        stops.insert(0, 'name="equ"')
    for stop in stops:
        index = cut.find(stop)
        if index >= 0:
            cut = cut[:index]
    return cut


def parse_contract_end_year(li_html: str) -> str:
    text = re.sub(r"<[^>]+>", " ", li_html)
    text = " ".join(text.split())
    match = re.search(r",\s*(20\d{2})\s*$", text)
    if match is None:
        match = re.search(r"\b(20\d{2})\b", text)
    return match.group(1) if match else ""


def parse_sections(html_text: str) -> list[tuple[str, list[str]]]:
    parts = re.split(r'<div class="h3-like">([^<]+)</div>', html_text)
    sections = []
    for index in range(1, len(parts), 2):
        label = parts[index].strip()
        body = parts[index + 1] if index + 1 < len(parts) else ""
        if label not in SECTION_TYPES:
            continue
        transfer_type = SECTION_TYPES[label]
        body = cut_section_body(body, transfer_type)
        items = re.findall(r"<li[^>]*>.*?</li>", body, re.S)
        sections.append((label, items))
    return sections


def mercato_path_suffix(today: date | None = None) -> str:
    today = today or date.today()
    if today.month >= 8:
        return f"-{today.year + 1}"
    return f"-{today.year}"


def dossier_url(dossier: str, today: date | None = None) -> str:
    suffix = mercato_path_suffix(today)
    return f"{BASE_URL}/dossiers/{dossier}{suffix}.html"


def scrape_club(
    competition_code: str,
    short_name: str,
    dossier: str,
    transfer_date: str,
) -> list[dict]:
    url = dossier_url(dossier)
    html_text = fetch(url)
    rows = []
    for label, items in parse_sections(html_text):
        transfer_type = SECTION_TYPES[label]
        for item in items:
            player = parse_player_name(item)
            if not player:
                continue
            other_club = parse_destination(item)
            from_club = ""
            to_club = ""
            if transfer_type == "JOIN":
                from_club = other_club
                to_club = short_name
            elif transfer_type == "LEAVE":
                from_club = short_name
                to_club = other_club
            elif transfer_type == "EXTENSION":
                from_club = short_name
                to_club = short_name
            elif transfer_type == "CONTRACT_END":
                from_club = short_name
                to_club = parse_contract_end_year(item) or other_club
            else:
                continue
            rows.append(
                {
                    "competitionCode": competition_code,
                    "playerName": player,
                    "type": transfer_type,
                    "transferDate": transfer_date,
                    "fromClub": from_club,
                    "toClub": to_club,
                    "contractLength": "",
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


def selected_clubs(team_filter: set[str]) -> list[tuple[str, str, str, str]]:
    clubs = [
        club
        for club in CLUBS
        if club[3]
    ]
    if not team_filter:
        return clubs
    return [club for club in clubs if club[1] in team_filter]


def main() -> int:
    parser = argparse.ArgumentParser(description="Scrap les mercatos All Rugby (saison suivante)")
    parser.add_argument("--output", default="data/import/transfers.csv")
    parser.add_argument("--delay", type=float, default=0.2)
    parser.add_argument(
        "--teams",
        default="",
        help="Codes clubs à scraper (ex: BAT,ALB). Vide = tous.",
    )
    args = parser.parse_args()

    root = Path(__file__).resolve().parent.parent
    output = (root / args.output).resolve()
    transfer_date = date.today().isoformat()
    suffix = mercato_path_suffix()
    team_filter = {
        code.strip().upper()
        for code in args.teams.split(",")
        if code.strip()
    }
    clubs = selected_clubs(team_filter)
    if not clubs:
        print("Aucun club correspondant.", file=sys.stderr)
        return 1

    print(f"Mercato AllRugby suffix={suffix or '(aucun)'} clubs={len(clubs)}", file=sys.stderr)
    rows = []
    for competition_code, short_name, _club_slug, dossier in clubs:
        print(f"  {short_name} ({competition_code})...", file=sys.stderr)
        try:
            rows.extend(scrape_club(competition_code, short_name, dossier, transfer_date))
        except urllib.error.HTTPError as error:
            print(f"ignoré {dossier}: HTTP {error.code}", file=sys.stderr)
        except urllib.error.URLError as error:
            print(f"ignoré {dossier}: {error}", file=sys.stderr)
        if args.delay > 0:
            time.sleep(args.delay)

    rows = dedupe_rows(rows)
    write_csv(rows, output)
    by_comp: dict[str, int] = {}
    for row in rows:
        by_comp[row["competitionCode"]] = by_comp.get(row["competitionCode"], 0) + 1
    print(f"{len(rows)} lignes -> {output}")
    for code in sorted(by_comp):
        print(f"  {code}: {by_comp[code]}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
