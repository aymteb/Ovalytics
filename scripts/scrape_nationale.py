#!/usr/bin/env python3

import argparse
import csv
import html
import re
import sys
import time
import urllib.request
from datetime import datetime
from pathlib import Path

USER_AGENT = "OvalyticsScraper/1.0"
CALENDAR_URL = "https://www.allrugby.com/competitions/nationale/calendrier.html"

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

LABEL_TO_SHORT = {
    "Massy": "MAS",
    "Carcassonne": "CAR",
    "Albi": "ALB",
    "Mont-de-Marsan": "MDM",
    "Mt.Marsan": "MDM",
    "Mt Marsan": "MDM",
    "Chambéry": "CHA",
    "Chambery": "CHA",
    "Rouen": "ROU",
    "Suresnes": "SUR",
    "Bourgoin": "BOU",
    "Orléans": "ORL",
    "Orleans": "ORL",
    "Périgueux": "PER",
    "Perigueux": "PER",
    "Rennes": "REN",
    "Vienne": "VIE",
    "Bourg-en-Bresse": "USB",
    "Bg-en-Bresse": "USB",
    "Bg.en-Bresse": "USB",
    "Marcq-en-Baroeul": "MAR",
    "Marcq-en-B": "MAR",
    "Marcq-en-B.": "MAR",
    "Marcq": "MAR",
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

SCORE_RE = re.compile(r"^(\d{1,3})\s*[-–]\s*(\d{1,3})$")
TIME_RE = re.compile(r"^(\d{1,2})h(\d{2})$", re.I)
DATE_RE = re.compile(
    r"(?:lundi|mardi|mercredi|jeudi|vendredi|samedi|dimanche)\s+"
    r"(\d{1,2})\s+([a-zéûô]+)\s+(\d{4})",
    re.I,
)


def fetch(url: str) -> str:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=30) as response:
        return response.read().decode("utf-8", "replace")


def clean_text(raw: str) -> str:
    text = html.unescape(raw or "")
    text = re.sub(r"<[^>]+>", " ", text)
    return re.sub(r"\s+", " ", text).strip()


def resolve_short(label: str) -> str | None:
    name = clean_text(label)
    if not name:
        return None
    if name in LABEL_TO_SHORT:
        return LABEL_TO_SHORT[name]
    folded = name.casefold()
    for key, short in LABEL_TO_SHORT.items():
        if key.casefold() == folded:
            return short
    for key, short in LABEL_TO_SHORT.items():
        if key.casefold() in folded or folded in key.casefold():
            return short
    return None


def parse_french_date(text: str) -> datetime | None:
    match = DATE_RE.search(clean_text(text))
    if not match:
        return None
    day = int(match.group(1))
    month = FRENCH_MONTHS.get(match.group(2).casefold())
    year = int(match.group(3))
    if month is None:
        return None
    return datetime(year, month, day, 20, 0, 0)


def extract_club_label(div_html: str) -> str:
    bold = re.search(r"<b>([^<]+)</b>", div_html, re.I)
    if bold:
        return clean_text(bold.group(1))
    alt = re.search(r'alt="([^"]+)"', div_html, re.I)
    if alt:
        return clean_text(alt.group(1))
    return clean_text(re.sub(r"<img[^>]*>", " ", div_html, flags=re.I))


def parse_calendar(html_text: str) -> list[dict]:
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

        href_match = re.search(r'href="(/saison-[^"]+/matchs/[^"]+)"', token)
        home_div = re.search(r'class="fl log txtright"[^>]*>(.*?)</div>', token, re.I | re.S)
        away_div = re.search(r'class="fl log txtleft"[^>]*>(.*?)</div>', token, re.I | re.S)
        res_match = re.search(r'class="fl res txtcenter"[^>]*>(.*?)</div>', token, re.I | re.S)
        if not home_div or not away_div or not res_match:
            continue

        home_label = extract_club_label(home_div.group(1))
        away_label = extract_club_label(away_div.group(1))
        home = resolve_short(home_label)
        away = resolve_short(away_label)
        if home is None or away is None:
            print(
                f"skip club inconnu: {home_label} / {away_label}",
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
                "competitionCode": "NAT",
                "homeShortName": home,
                "awayShortName": away,
                "matchday": matchday if matchday > 0 else 1,
                "kickoffAt": kickoff.strftime("%Y-%m-%dT%H:%M:%S"),
                "status": status,
                "homeScore": home_score,
                "awayScore": away_score,
                "homeTries": "",
                "awayTries": "",
                "_href": href_match.group(1) if href_match else "",
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
    parser = argparse.ArgumentParser(description="Scrap calendrier Nationale (AllRugby)")
    parser.add_argument("--output", default="data/import/nationale-matches.csv")
    parser.add_argument("--url", default=CALENDAR_URL)
    args = parser.parse_args()

    root = Path(__file__).resolve().parent.parent
    output = Path(args.output)
    if not output.is_absolute():
        output = (root / output).resolve()

    print(f"Fetch {args.url}", file=sys.stderr)
    html_text = fetch(args.url)
    time.sleep(0.2)
    rows = parse_calendar(html_text)
    if not rows:
        print("Aucun match parse", file=sys.stderr)
        return 1

    write_csv(rows, output)
    finished = sum(1 for row in rows if row["status"] == "FINISHED")
    print(f"{len(rows)} matchs ({finished} termines) -> {output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
