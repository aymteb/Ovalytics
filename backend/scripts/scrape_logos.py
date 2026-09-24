#!/usr/bin/env python3

import argparse
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

from flashscore_feed import (
    fetch_page,
    map_competition_pair,
    parse_embedded_events,
)

USER_AGENT = "OvalyticsScraper/1.0"
IMAGE_BASE = "https://static.flashscore.com/res/image/data"
DEFAULT_OUTPUT = Path(__file__).resolve().parent.parent / "frontend" / "public" / "clubs"

CALENDAR_URLS = [
    "https://www.flashscore.fr/rugby/france/top-14/calendrier/",
    "https://www.flashscore.fr/rugby/france/top-14/resultats/",
    "https://www.flashscore.fr/rugby/france/pro-d2/calendrier/",
    "https://www.flashscore.fr/rugby/france/pro-d2/resultats/",
]

EXTRA_NAME_TO_SHORT = {
    "Nissa": "NIC",
    "Oyonnax Rugby": "OYO",
    "USO Nevers Rugby": "NEV",
    "Stade Aurillacois": "AUR",
    "AS Béziers": "BEZ",
}


def short_from_name(home: str, away: str, logo_side: str) -> str | None:
    mapped = map_competition_pair(home, away, False)
    if mapped is None:
        name = home if logo_side == "home" else away
        return EXTRA_NAME_TO_SHORT.get(name.strip())
    _comp, home_short, away_short = mapped
    return home_short if logo_side == "home" else away_short


def collect_logo_files() -> dict[str, str]:
    logos: dict[str, str] = {}
    for url in CALENDAR_URLS:
        try:
            page = fetch_page(url, USER_AGENT)
        except urllib.error.HTTPError as error:
            print(f"Page inaccessible: {url} ({error})", file=sys.stderr)
            continue
        for fields in parse_embedded_events(page):
            home = fields.get("AE", "")
            away = fields.get("AF", "")
            home_logo = fields.get("OA", "")
            away_logo = fields.get("OB", "")
            if home_logo:
                short = short_from_name(home, away, "home")
                if short:
                    logos[short] = home_logo
            if away_logo:
                short = short_from_name(home, away, "away")
                if short:
                    logos[short] = away_logo
    return logos


def download_logo(filename: str, output: Path) -> bool:
    url = f"{IMAGE_BASE}/{filename}"
    request = urllib.request.Request(
        url,
        headers={
            "User-Agent": USER_AGENT,
            "Referer": "https://www.flashscore.fr/",
        },
    )
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            data = response.read()
    except urllib.error.HTTPError as error:
        print(f"  echec {filename}: {error}", file=sys.stderr)
        return False
    output.write_bytes(data)
    return True


def main() -> int:
    parser = argparse.ArgumentParser(description="Telecharge les logos clubs depuis Flashscore")
    parser.add_argument(
        "--output",
        type=Path,
        default=DEFAULT_OUTPUT,
        help="Dossier de sortie (defaut: frontend/public/clubs)",
    )
    parser.add_argument(
        "--delay",
        type=float,
        default=0.2,
        help="Pause entre chaque telechargement (secondes)",
    )
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)

    logos = collect_logo_files()
    if not logos:
        print("Aucun logo trouve sur Flashscore.", file=sys.stderr)
        return 1

    print(f"{len(logos)} logo(s) repere(s)")
    ok = 0
    for short, filename in sorted(logos.items()):
        target = args.output / f"{short}.png"
        if download_logo(filename, target):
            ok += 1
            print(f"  {short} -> {target.name}")
        time.sleep(args.delay)

    print(f"Termine: {ok}/{len(logos)} fichiers ecrits dans {args.output}")
    return 0 if ok > 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
