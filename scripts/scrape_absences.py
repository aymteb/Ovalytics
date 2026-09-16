#!/usr/bin/env python3

import argparse
import csv
import html
import re
import sys
import time
import unicodedata
import urllib.request
from pathlib import Path

from clubs_config import ALL_RUGBY_LABEL_TO_SHORT

USER_AGENT = "OvalyticsScraper/1.0"
TOP14_URL = "https://www.allrugby.com/competitions/top-14/indisponibilites.html"
PROD2_HUB_URL = "https://www.rugbyrama.fr/rugby/championnats-francais/pro-d2/"
CSV_HEADERS = ["competitionCode", "teamShortName", "playerName", "type", "note"]

SECTION_TYPES = {
    "suspension": "SUSPENDED",
    "infirmerie": "INJURED",
    "sélection": "INTERNATIONAL",
    "selection": "INTERNATIONAL",
}

TYPE_PRIORITY = {"SUSPENDED": 3, "INTERNATIONAL": 2, "INJURED": 1}

PROD2_CLUB_LABELS = {
    "Soyaux-Angoulême": "ANG",
    "Soyaux Angoulême": "ANG",
    "Angoulême": "ANG",
    "Montauban": "MTB",
    "Provence Rugby": "AIX",
    "Provence": "AIX",
    "Narbonne": "NAR",
    "Béziers": "BEZ",
    "Beziers": "BEZ",
    "Nevers": "NEV",
    "Brive": "BRI",
    "Nice": "NIC",
    "Nissa": "NIC",
    "Dax": "DAX",
    "Valence-Romans": "VAL",
    "Valence Romans": "VAL",
    "Oyonnax": "OYO",
    "Aurillac": "AUR",
    "Grenoble": "GRE",
    "Agen": "AGE",
    "Biarritz": "BIA",
    "Colomiers": "COL",
}

RETURN_RE = re.compile(
    r"(?:\bretour\b|\bapte\b|\bpostule|\br[ée]int[èe]gr|"
    r"\bop[ée]rationnel\b|chemin inverse|de retour|100\s*%|"
    r"\bont repris\b|\bretrouver (?:tr[èe]s )?vite\b|"
    r"\bd[ée]j[àa] de retour\b|\bsont d[ée]sormais\b|"
    r"\bse rapproche\b)",
    re.I,
)
FORCED_OUT_RE = re.compile(
    r"(?:pas aptes|ne (?:sont|sera|seront) pas|manquera|forfait|"
    r"toujours [àa] l['\u2019 ]?infirmerie|out pour|plusieurs mois|"
    r"plusieurs semaines|\bpr[ée]serv[ée]\b|"
    r"\b[àa] l['\u2019 ]?arr\w*t\b|"
    r"\blaiss[ée] au repos\b)",
    re.I,
)
NOT_AVAILABLE_RE = re.compile(
    r"(?:infirmerie|absent|indisponib|m[ée]nag[ée]|souffre|touch[ée]|"
    r"bless[ée]|op[ée]r[ée]|convalescence|carton)",
    re.I,
)
SUSPEND_RE = re.compile(r"(?:suspendu|carton rouge)", re.I)


def fetch(url: str) -> str:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=30) as response:
        return response.read().decode("utf-8")


def clean_text(raw: str) -> str:
    text = html.unescape(raw or "")
    text = re.sub(r"<[^>]+>", " ", text)
    text = text.replace("\xa0", " ")
    return re.sub(r"\s+", " ", text).strip(" ,\t\n")


def fold(value: str) -> str:
    normalized = unicodedata.normalize("NFD", value or "")
    without = "".join(ch for ch in normalized if unicodedata.category(ch) != "Mn")
    return without.lower()


def fix_name_word(word: str) -> str:
    core = word.replace("'", "").replace("-", "")
    if not core or not core.isalpha():
        return word
    if not word.isupper() and not core.isupper():
        return word
    if "'" in word:
        return "'".join(part.capitalize() if part else "" for part in word.split("'"))
    if "-" in word:
        return "-".join(part.capitalize() for part in word.split("-"))
    return word.capitalize()


def clean_player_name(raw: str) -> str:
    name = clean_text(raw)
    name = re.sub(r"^\d+\.\s*", "", name)
    if not name:
        return ""
    return " ".join(fix_name_word(part) for part in name.split())


def resolve_team(club_label: str, mapping: dict[str, str]) -> str | None:
    label = clean_text(club_label)
    if not label:
        return None
    if label in mapping:
        return mapping[label]
    folded = fold(label)
    for key, short in mapping.items():
        if fold(key) == folded:
            return short
    for key, short in mapping.items():
        if fold(key) in folded or folded in fold(key):
            return short
    return None


def extract_players(block: str) -> list[str]:
    names: list[str] = []
    for match in re.finditer(r'<a href="/joueurs/[^"]+"[^>]*>([^<]+)</a>', block, re.I):
        name = clean_player_name(match.group(1))
        if name:
            names.append(name)
    return names


def prefer_type(existing: str | None, candidate: str) -> str:
    if existing is None:
        return candidate
    if TYPE_PRIORITY.get(candidate, 0) > TYPE_PRIORITY.get(existing, 0):
        return candidate
    return existing


def dedupe_rows(rows: list[dict]) -> list[dict]:
    best: dict[tuple[str, str, str], dict] = {}
    for row in rows:
        key = (row["competitionCode"], row["teamShortName"], fold(row["playerName"]))
        current = best.get(key)
        if current is None:
            best[key] = row
            continue
        chosen_type = prefer_type(current["type"], row["type"])
        if chosen_type == row["type"] and chosen_type != current["type"]:
            best[key] = row
        elif not current.get("note") and row.get("note"):
            current["note"] = row["note"]
    return list(best.values())


def parse_top14(html_text: str) -> list[dict]:
    chunks = re.split(r"(?is)<h2[^>]*>\s*(.*?)\s*</h2>", html_text)
    rows: list[dict] = []

    for i in range(1, len(chunks), 2):
        club_label = clean_text(chunks[i])
        short = resolve_team(club_label, ALL_RUGBY_LABEL_TO_SHORT)
        if short is None:
            continue
        body = chunks[i + 1]
        for match in re.finditer(
            r'(?is)<span>\s*(Suspension|Infirmerie|S[ée]lection)\s*</span>\s*:\s*<ul class="joueurs">(.*?)</ul>',
            body,
        ):
            absence_type = SECTION_TYPES.get(clean_text(match.group(1)).lower())
            if absence_type is None:
                continue
            for player_name in extract_players(match.group(2)):
                rows.append(
                    {
                        "competitionCode": "TOP14",
                        "teamShortName": short,
                        "playerName": player_name,
                        "type": absence_type,
                        "note": "",
                    }
                )
    return dedupe_rows(rows)


def load_squad_names(squads_csv: Path) -> dict[str, list[str]]:
    by_team: dict[str, list[str]] = {}
    if not squads_csv.exists():
        return by_team
    with squads_csv.open(encoding="utf-8", newline="") as file:
        for row in csv.DictReader(file):
            if row.get("competitionCode") != "PROD2":
                continue
            short = (row.get("teamShortName") or "").strip()
            name = (row.get("playerName") or "").strip()
            if not short or not name:
                continue
            by_team.setdefault(short, []).append(name)
    return by_team


def discover_prod2_article_url() -> str:
    hub = fetch(PROD2_HUB_URL)
    candidates = re.findall(
        r'href="((?:https://www\.rugbyrama\.fr)?/[^"]*pro-d2[^"]*infirmerie[^"]*)"',
        hub,
        re.I,
    )
    if not candidates:
        raise RuntimeError("Article infirmeries Pro D2 introuvable sur Rugbyrama")
    path = candidates[0]
    if path.startswith("http"):
        return path
    return "https://www.rugbyrama.fr" + path


def context_around(text: str, start: int, end: int, radius: int = 220) -> str:
    left = max(0, start - radius)
    right = min(len(text), end + radius)
    return text[left:right].strip()


def name_search_keys(name: str, all_names: list[str]) -> list[str]:
    keys = [name]
    parts = name.split()
    if len(parts) >= 2:
        keys.append(" ".join(parts[-2:]))
    if parts:
        last = parts[-1]
        if len(fold(last)) >= 5:
            same_last = [
                other
                for other in all_names
                if fold(other).split() and fold(other).split()[-1] == fold(last)
            ]
            if len(same_last) == 1:
                keys.append(last)
    unique: list[str] = []
    seen: set[str] = set()
    for key in keys:
        folded = fold(key)
        if folded in seen or len(folded) < 4:
            continue
        seen.add(folded)
        unique.append(key)
    return unique


def find_name_hits(text: str, names: list[str]) -> list[tuple[str, int, int]]:
    folded_text = fold(text)
    candidates: list[tuple[str, str]] = []
    for name in set(names):
        for key in name_search_keys(name, names):
            candidates.append((name, key))
    candidates.sort(key=lambda item: len(fold(item[1])), reverse=True)

    used: list[tuple[int, int]] = []
    hits: list[tuple[str, int, int]] = []
    claimed: set[str] = set()
    for canonical, key in candidates:
        if fold(canonical) in claimed:
            continue
        needle = fold(key)
        start = 0
        while True:
            idx = folded_text.find(needle, start)
            if idx < 0:
                break
            end = idx + len(needle)
            overlaps = any(idx < used_end and end > used_start for used_start, used_end in used)
            if overlaps:
                start = idx + 1
                continue
            used.append((idx, end))
            hits.append((canonical, idx, end))
            claimed.add(fold(canonical))
            break
    return hits


def classify_mention(text: str, start: int, end: int) -> str | None:
    after = text[end : end + 55]
    near = text[start : min(len(text), end + 75)]
    local = text[max(0, start - 80) : min(len(text), end + 170)]
    forward = text[start : min(len(text), end + 320)]
    if SUSPEND_RE.search(after):
        return "SUSPENDED"
    if FORCED_OUT_RE.search(near):
        return "INJURED"
    if RETURN_RE.search(local):
        return None
    if NOT_AVAILABLE_RE.search(forward):
        return "INJURED"
    return None


def parse_prod2(html_text: str, squad_by_team: dict[str, list[str]]) -> list[dict]:
    chunks = re.split(r"(?is)<h2[^>]*>\s*(.*?)\s*</h2>", html_text)
    rows: list[dict] = []
    for i in range(1, len(chunks), 2):
        club_label = clean_text(chunks[i])
        short = resolve_team(club_label, PROD2_CLUB_LABELS)
        if short is None:
            continue
        body_html = chunks[i + 1]
        next_aside = re.split(r"(?is)<h2\b", body_html, maxsplit=1)[0]
        text = clean_text(next_aside)
        names = squad_by_team.get(short, [])
        if not names:
            continue
        for player_name, start, end in find_name_hits(text, names):
            absence_type = classify_mention(text, start, end)
            if absence_type is None:
                continue
            rows.append(
                {
                    "competitionCode": "PROD2",
                    "teamShortName": short,
                    "playerName": clean_player_name(player_name),
                    "type": absence_type,
                    "note": "",
                }
            )
    return dedupe_rows(rows)


def write_csv(rows: list[dict], output: Path) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    ordered = sorted(
        rows,
        key=lambda row: (row["competitionCode"], row["teamShortName"], row["playerName"]),
    )
    with output.open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=CSV_HEADERS)
        writer.writeheader()
        for row in ordered:
            writer.writerow({key: row.get(key, "") for key in CSV_HEADERS})


def print_summary(rows: list[dict]) -> None:
    by_comp: dict[str, dict[str, int]] = {}
    for row in rows:
        team_counts = by_comp.setdefault(row["competitionCode"], {})
        team_counts[row["teamShortName"]] = team_counts.get(row["teamShortName"], 0) + 1
    print(f"{len(rows)} absences")
    for code in sorted(by_comp):
        print(f"  {code}: {sum(by_comp[code].values())}")
        for short in sorted(by_comp[code]):
            print(f"    {short}: {by_comp[code][short]}")


def main() -> int:
    parser = argparse.ArgumentParser(description="Scrap absences Top 14 + Pro D2")
    parser.add_argument("--output", default="data/import/absences.csv")
    parser.add_argument(
        "--squads",
        default="data/import/squads.csv",
        help="Effectifs pour matcher les noms Pro D2",
    )
    parser.add_argument("--prod2-url", default="", help="URL article Rugbyrama (sinon auto)")
    args = parser.parse_args()

    root = Path(__file__).resolve().parent.parent
    output = (root / args.output).resolve()
    squads_csv = (root / args.squads).resolve()

    rows: list[dict] = []

    top14_html = fetch(TOP14_URL)
    top14_rows = parse_top14(top14_html)
    rows.extend(top14_rows)
    print(f"Top 14: {len(top14_rows)} apres dedupe")

    squad_by_team = load_squad_names(squads_csv)
    prod2_url = args.prod2_url.strip() or discover_prod2_article_url()
    print(f"Pro D2 article: {prod2_url}")
    prod2_html = fetch(prod2_url)
    time.sleep(0.3)
    prod2_rows = parse_prod2(prod2_html, squad_by_team)
    rows.extend(prod2_rows)
    print(f"Pro D2: {len(prod2_rows)}")

    if not rows:
        print("Aucune absence parse", file=sys.stderr)
        return 1

    write_csv(rows, output)
    print(f"-> {output}")
    print_summary(rows)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
