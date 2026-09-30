#!/usr/bin/env python3

import argparse
import csv
import html
import json
import re
import sys
import time
import unicodedata
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

from clubs_config import ALL_RUGBY_LABEL_TO_SHORT, SHORT_TO_COMPETITION

USER_AGENT = "OvalyticsScraper/1.0"
CSV_HEADERS = ["competitionCode", "teamShortName", "playerName", "type", "note"]

SECTION_TYPES = {
    "suspension": "SUSPENDED",
    "infirmerie": "INJURED",
    "sélection": "INTERNATIONAL",
    "selection": "INTERNATIONAL",
}

SECTION_LABEL_RE = r"Suspension|Infirmerie|S[ée]lection"

TYPE_PRIORITY = {"SUSPENDED": 3, "INTERNATIONAL": 2, "INJURED": 1}

ALLRUGBY_SOURCES = [
    {
        "competition_code": "TOP14",
        "url": "https://www.allrugby.com/competitions/top-14/indisponibilites.html",
        "labels": ALL_RUGBY_LABEL_TO_SHORT,
    },
]

FFR_API = "https://api-presse.ffr.fr/wp-json/wp/v2"
FFR_SELECTION_FEEDS = [
    {
        "name": "XV France masculin",
        "category_id": 8,
        "note": "XV de France",
    },
]

FFR_LIST_SLUG_RE = re.compile(
    r"(liste.*joueur|le-groupe|groupe-de-\d+|groupe-pour|groupe$)",
    re.I,
)
FFR_LIST_EXCLUDE_RE = re.compile(
    r"(programme-presse|media-guide|composition|fan-experience|parten|billett)",
    re.I,
)

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
SELECTION_RE = re.compile(
    r"(?:"
    r"\ben s[ée]lections?\b"
    r"|\bretenu(?:e|s)? en s[ée]lections?\b"
    r"|\bretenu(?:e|s)? (?:avec |en )?(?:la |leur )?(?:s[ée]lection(?!neur)|equipe nationale)\b"
    r"|\b(?:a|à)(?:\s+la)?\s+dispos(?:ition)? de la s[ée]lection(?!neur)"
    r"|\bavec (?:la )?s[ée]lection(?!neur)"
    r"|\bequipe nationale\b"
    r"|\bparti(?:e|s)? en s[ée]lection(?!neur)"
    r"|\bretrouv[ée](?:r)? la s[ée]lection(?!neur)"
    r"|\brepart(?:i|is|ie|ies)? (?:dans|vers) (?:leurs |la )?s[ée]lections?"
    r"|\bmis[e]? [àa] disposition de France\s*7\b"
    r"|\bconvoqu[ée]s? (?:en |avec |par )?(?:la )?(?:s[ée]lection(?!neur)|equipe)"
    r")",
    re.I,
)


def fetch(url: str) -> str:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=30) as response:
        return response.read().decode("utf-8")


def fetch_json(url: str):
    request = urllib.request.Request(
        url,
        headers={"User-Agent": USER_AGENT, "Accept": "application/json"},
    )
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.loads(response.read().decode("utf-8"))


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


def resolve_club_competition(club_label: str) -> tuple[str, str] | None:
    short = resolve_team(club_label, ALL_RUGBY_LABEL_TO_SHORT)
    if short is None:
        short = resolve_team(club_label, PROD2_CLUB_LABELS)
    if short is None:
        return None
    code = SHORT_TO_COMPETITION.get(short)
    if code is None:
        return None
    return code, short


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


def parse_allrugby_sections(
    html_text: str,
    competition_code: str,
    labels: dict[str, str],
) -> list[dict]:
    chunks = re.split(r"(?is)<h2[^>]*>\s*(.*?)\s*</h2>", html_text)
    rows: list[dict] = []
    section_re = re.compile(
        rf'(?is)<span>\s*({SECTION_LABEL_RE})\s*</span>\s*:\s*<ul class="joueurs">(.*?)</ul>'
    )

    for i in range(1, len(chunks), 2):
        club_label = clean_text(chunks[i])
        short = resolve_team(club_label, labels)
        if short is None:
            continue
        body = chunks[i + 1]
        for match in section_re.finditer(body):
            absence_type = SECTION_TYPES.get(clean_text(match.group(1)).lower())
            if absence_type is None:
                continue
            for player_name in extract_players(match.group(2)):
                rows.append(
                    {
                        "competitionCode": competition_code,
                        "teamShortName": short,
                        "playerName": player_name,
                        "type": absence_type,
                        "note": "",
                    }
                )
    return dedupe_rows(rows)


def load_squad_names(squads_csv: Path, competition_code: str) -> dict[str, list[str]]:
    by_team: dict[str, list[str]] = {}
    if not squads_csv.exists():
        return by_team
    with squads_csv.open(encoding="utf-8", newline="") as file:
        for row in csv.DictReader(file):
            if row.get("competitionCode") != competition_code:
                continue
            short = (row.get("teamShortName") or "").strip()
            name = (row.get("playerName") or "").strip()
            if not short or not name:
                continue
            by_team.setdefault(short, []).append(name)
    return by_team


def discover_prod2_article_url() -> str:
    return discover_rugbyrama_article("pro d2 infirmeries", "pro-d2")


def discover_top14_article_url() -> str:
    return discover_rugbyrama_article("top 14 infirmeries", "top-14")


def discover_rugbyrama_article(query: str, slug_token: str) -> str:
    search_url = "https://www.rugbyrama.fr/recherche/?" + urllib.parse.urlencode(
        {"q": query}
    )
    page = fetch(search_url)
    pattern = (
        r'href="((?:https://www\.rugbyrama\.fr)?/\d{4}/\d{2}/\d{2}/[^"]*'
        + re.escape(slug_token)
        + r'[^"]*infirmerie[^"]*)"'
    )
    candidates = re.findall(pattern, page, re.I)
    scored: list[tuple[str, str]] = []
    for raw in candidates:
        path = raw
        if path.startswith("http"):
            path = re.sub(r"^https://www\.rugbyrama\.fr", "", path)
        if "amicaux" in path.lower():
            continue
        date_match = re.match(r"/(\d{4}/\d{2}/\d{2})/", path)
        if not date_match:
            continue
        scored.append((date_match.group(1), path))
    if not scored:
        raise RuntimeError(f"Article infirmeries introuvable pour: {query}")
    scored.sort(reverse=True)
    return "https://www.rugbyrama.fr" + scored[0][1]


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
    selection_near = text[max(0, start - 70) : min(len(text), end + 130)]
    if SUSPEND_RE.search(after):
        return "SUSPENDED"
    if SELECTION_RE.search(selection_near):
        return "INTERNATIONAL"
    if FORCED_OUT_RE.search(near):
        return "INJURED"
    if RETURN_RE.search(local):
        return None
    if NOT_AVAILABLE_RE.search(forward):
        return "INJURED"
    return None


def parse_article_squad_mentions(
    html_text: str,
    competition_code: str,
    labels: dict[str, str],
    squad_by_team: dict[str, list[str]],
) -> list[dict]:
    chunks = re.split(r"(?is)<h2[^>]*>\s*(.*?)\s*</h2>", html_text)
    rows: list[dict] = []
    for i in range(1, len(chunks), 2):
        club_label = clean_text(chunks[i])
        short = resolve_team(club_label, labels)
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
                    "competitionCode": competition_code,
                    "teamShortName": short,
                    "playerName": clean_player_name(player_name),
                    "type": absence_type,
                    "note": "",
                }
            )
    return dedupe_rows(rows)


def parse_post_date(value: str) -> datetime | None:
    if not value:
        return None
    try:
        return datetime.fromisoformat(value.replace("Z", "+00:00"))
    except ValueError:
        return None


def is_ffr_group_post(slug: str, title: str) -> bool:
    blob = f"{slug} {title}"
    if FFR_LIST_EXCLUDE_RE.search(blob):
        return False
    return bool(FFR_LIST_SLUG_RE.search(slug) or FFR_LIST_SLUG_RE.search(fold(title).replace(" ", "-")))


def parse_ffr_player_tables(content_html: str, note: str) -> list[dict]:
    rows: list[dict] = []
    for tr in re.findall(r"(?is)<tr[^>]*>(.*?)</tr>", content_html):
        cells = [
            clean_text(cell)
            for cell in re.findall(r"(?is)<t[hd][^>]*>(.*?)</t[hd]>", tr)
        ]
        cells = [cell for cell in cells if cell]
        if len(cells) < 7:
            continue
        if cells[0].upper().startswith("CLUB") or cells[1].upper().startswith("CLUB"):
            continue
        first, last = cells[0], cells[1]
        club = cells[6]
        if not first or not last or not club:
            continue
        if fold(last) in {"ans", "selection", "selections"}:
            continue
        resolved = resolve_club_competition(club)
        if resolved is None:
            continue
        competition_code, short = resolved
        player_name = clean_player_name(f"{first} {last}")
        if not player_name:
            continue
        rows.append(
            {
                "competitionCode": competition_code,
                "teamShortName": short,
                "playerName": player_name,
                "type": "INTERNATIONAL",
                "note": note,
            }
        )
    return rows


def discover_ffr_group_posts(category_id: int, max_age_days: int) -> list[dict]:
    query = urllib.parse.urlencode(
        {
            "categories": category_id,
            "per_page": 20,
            "_fields": "id,date,slug,title,content",
        }
    )
    posts = fetch_json(f"{FFR_API}/posts?{query}")
    now = datetime.now(timezone.utc)
    chosen: list[dict] = []
    for post in posts:
        slug = post.get("slug") or ""
        title_raw = post.get("title") or {}
        title = clean_text(title_raw.get("rendered") if isinstance(title_raw, dict) else str(title_raw))
        if not is_ffr_group_post(slug, title):
            continue
        posted_at = parse_post_date(post.get("date") or "")
        if posted_at is None:
            continue
        age_days = (now - posted_at.astimezone(timezone.utc)).days
        if age_days > max_age_days:
            continue
        content_raw = post.get("content") or {}
        content = content_raw.get("rendered") if isinstance(content_raw, dict) else ""
        if not content or "<table" not in content.lower():
            continue
        chosen.append(
            {
                "slug": slug,
                "title": title,
                "date": posted_at,
                "age_days": age_days,
                "content": content,
            }
        )
    chosen.sort(key=lambda item: item["date"], reverse=True)
    return chosen


def scrape_ffr_selections(max_age_days: int, force_url: str = "") -> list[dict]:
    rows: list[dict] = []

    if force_url.strip():
        html_text = fetch(force_url.strip())
        parsed = parse_ffr_player_tables(html_text, "XV de France")
        rows.extend(parsed)
        print(f"FFR force URL: {len(parsed)} joueurs ({force_url.strip()})")
        return dedupe_rows(rows)

    for feed in FFR_SELECTION_FEEDS:
        posts = discover_ffr_group_posts(feed["category_id"], max_age_days)
        if not posts:
            print(
                f"FFR {feed['name']}: aucune liste recente"
                f" (max {max_age_days} j)"
            )
            continue
        latest = posts[0]
        parsed = parse_ffr_player_tables(latest["content"], feed["note"])
        rows.extend(parsed)
        print(
            f"FFR {feed['name']}: {len(parsed)} joueurs"
            f" via {latest['slug']} ({latest['age_days']} j)"
        )
        time.sleep(0.2)

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
    by_type: dict[str, int] = {}
    for row in rows:
        team_counts = by_comp.setdefault(row["competitionCode"], {})
        team_counts[row["teamShortName"]] = team_counts.get(row["teamShortName"], 0) + 1
        by_type[row["type"]] = by_type.get(row["type"], 0) + 1
    print(f"{len(rows)} absences")
    print("  types: " + ", ".join(f"{k}={v}" for k, v in sorted(by_type.items())))
    for code in sorted(by_comp):
        print(f"  {code}: {sum(by_comp[code].values())}")
        for short in sorted(by_comp[code]):
            print(f"    {short}: {by_comp[code][short]}")


def main() -> int:
    parser = argparse.ArgumentParser(description="Scrap absences (multi-competitions)")
    parser.add_argument("--output", default="data/import/absences.csv")
    parser.add_argument(
        "--squads",
        default="data/import/squads.csv",
        help="Effectifs pour matcher les noms (articles type Pro D2)",
    )
    parser.add_argument("--prod2-url", default="", help="URL article Rugbyrama Pro D2 (sinon auto)")
    parser.add_argument("--top14-url", default="", help="URL article Rugbyrama Top 14 (sinon auto)")
    parser.add_argument(
        "--ffr-max-age-days",
        type=int,
        default=28,
        help="Ignorer une liste FFR plus vieille (joueurs liberes / fenetre close)",
    )
    parser.add_argument(
        "--ffr-force-url",
        default="",
        help="Forcer une URL presse FFR (test / reprise manuelle)",
    )
    args = parser.parse_args()

    root = Path(__file__).resolve().parent.parent
    output = (root / args.output).resolve()
    squads_csv = (root / args.squads).resolve()

    rows: list[dict] = []

    for source in ALLRUGBY_SOURCES:
        code = source["competition_code"]
        page = fetch(source["url"])
        parsed = parse_allrugby_sections(page, code, source["labels"])
        rows.extend(parsed)
        print(f"{code} AllRugby: {len(parsed)} apres dedupe")
        time.sleep(0.2)

    top14_squads = load_squad_names(squads_csv, "TOP14")
    try:
        top14_url = args.top14_url.strip() or discover_top14_article_url()
        print(f"Top 14 article: {top14_url}")
        top14_html = fetch(top14_url)
        time.sleep(0.3)
        top14_article_rows = parse_article_squad_mentions(
            top14_html,
            "TOP14",
            ALL_RUGBY_LABEL_TO_SHORT,
            top14_squads,
        )
        rows.extend(top14_article_rows)
        print(f"TOP14 article: {len(top14_article_rows)}")
    except Exception as ex:
        print(f"Top 14 article ignore: {ex}")

    squad_by_team = load_squad_names(squads_csv, "PROD2")
    prod2_url = args.prod2_url.strip() or discover_prod2_article_url()
    print(f"Pro D2 article: {prod2_url}")
    prod2_html = fetch(prod2_url)
    time.sleep(0.3)
    prod2_rows = parse_article_squad_mentions(
        prod2_html,
        "PROD2",
        PROD2_CLUB_LABELS,
        squad_by_team,
    )
    rows.extend(prod2_rows)
    print(f"PROD2 article: {len(prod2_rows)}")

    ffr_rows = scrape_ffr_selections(args.ffr_max_age_days, args.ffr_force_url)
    rows.extend(ffr_rows)
    print(f"FFR selections: {len(ffr_rows)}")

    if not rows:
        print("Aucune absence parse", file=sys.stderr)
        return 1

    write_csv(dedupe_rows(rows), output)
    print(f"-> {output}")
    print_summary(dedupe_rows(rows))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
