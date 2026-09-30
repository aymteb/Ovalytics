#!/usr/bin/env python3

import argparse
import html as html_lib
import re
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.request
from pathlib import Path

from allrugby_foreign import LABELS_BY_CODE
from scrape_nationale import LABEL_TO_SHORT

USER_AGENT = "OvalyticsScraper/1.0"
BASE = "https://www.allrugby.com"
DEFAULT_OUTPUT = Path(__file__).resolve().parent.parent / "frontend" / "public" / "clubs"

TOP14_LABELS = {
    "Toulouse": "TOU",
    "Bordeaux": "UBB",
    "Racing 92": "RAC",
    "Racing": "RAC",
    "Paris": "SFP",
    "Stade Français": "SFP",
    "Stade Francais": "SFP",
    "Toulon": "TOL",
    "La Rochelle": "LAR",
    "Clermont": "ASM",
    "Lyon": "LOU",
    "Montpellier": "MHR",
    "Castres": "CAS",
    "Pau": "PAU",
    "Bayonne": "BAY",
    "Perpignan": "USAP",
    "Vannes": "VAN",
}

PROD2_LABELS = {
    "Béziers": "BEZ",
    "Beziers": "BEZ",
    "Oyonnax": "OYO",
    "Colomiers": "COL",
    "Nevers": "NEV",
    "Provence": "AIX",
    "Aix": "AIX",
    "Grenoble": "GRE",
    "Biarritz": "BIA",
    "Agen": "AGE",
    "Brive": "BRI",
    "Nice": "NIC",
    "Nissa": "NIC",
    "Angoulême": "ANG",
    "Angouleme": "ANG",
    "Soyaux-Angoulême": "ANG",
    "Dax": "DAX",
    "Narbonne": "NAR",
    "Aurillac": "AUR",
    "Montauban": "MTB",
    "Valence-Romans": "VAL",
    "Valence Romans": "VAL",
}

SOURCES = [
    ("top-14", TOP14_LABELS, "classement"),
    ("pro-d2", PROD2_LABELS, "classement"),
    ("nationale", LABEL_TO_SHORT, "classement"),
    ("champions-cup", LABELS_BY_CODE["ERCC"], "classement"),
    ("challenge-cup", LABELS_BY_CODE["ERCH"], "classement"),
    ("urc", LABELS_BY_CODE["URC"], "classement"),
    ("premiership", LABELS_BY_CODE["PREM"], "classement"),
    ("test-matchs", LABELS_BY_CODE["INT"], "calendrier"),
]


def fetch(url: str) -> str:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=30) as response:
        return response.read().decode("utf-8", "replace")


def resolve_short(label: str, mapping: dict[str, str]) -> str | None:
    name = html_lib.unescape(label or "").strip()
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


def prefer_size40(path: str) -> str:
    return path.replace("/img/logo/clubs/20/", "/img/logo/clubs/40/")


def parse_classement(html_text: str, mapping: dict[str, str]) -> dict[str, str]:
    logos: dict[str, str] = {}
    for name, path in re.findall(
        r'alt="Classement ([^"]+)"\s+src="(/img/logo/clubs/\d+/[^"]+)"',
        html_text,
    ):
        short = resolve_short(name, mapping)
        if short:
            logos[short] = prefer_size40(path)
    return logos


def parse_calendar_logos(html_text: str, mapping: dict[str, str]) -> dict[str, str]:
    logos: dict[str, str] = {}
    for name, path in re.findall(
        r'alt="([^"]+)"[^>]*src="(/img/logo/clubs/\d+/[^"]+)"',
        html_text,
    ):
        short = resolve_short(name, mapping)
        if short:
            logos[short] = prefer_size40(path)
    return logos


def download_bytes(path: str) -> bytes | None:
    url = f"{BASE}{path}"
    request = urllib.request.Request(
        url,
        headers={"User-Agent": USER_AGENT, "Referer": f"{BASE}/"},
    )
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            return response.read()
    except urllib.error.HTTPError:
        if "/40/" in path:
            return download_bytes(path.replace("/img/logo/clubs/40/", "/img/logo/clubs/20/"))
        return None


def write_png(data: bytes, source_path: str, target: Path) -> bool:
    if source_path.lower().endswith(".webp"):
        with tempfile.NamedTemporaryFile(suffix=".webp", delete=False) as tmp:
            tmp.write(data)
            tmp_path = Path(tmp.name)
        try:
            result = subprocess.run(
                ["sips", "-s", "format", "png", str(tmp_path), "--out", str(target)],
                capture_output=True,
                text=True,
            )
            return result.returncode == 0 and target.exists()
        finally:
            tmp_path.unlink(missing_ok=True)
    target.write_bytes(data)
    return True


def collect_logos() -> dict[str, str]:
    logos: dict[str, str] = {}
    for slug, mapping, kind in SOURCES:
        url = f"{BASE}/competitions/{slug}/{kind}.html"
        try:
            html_text = fetch(url)
        except urllib.error.HTTPError as error:
            print(f"Page inaccessible: {url} ({error})", file=sys.stderr)
            continue
        parsed = (
            parse_classement(html_text, mapping)
            if kind == "classement"
            else parse_calendar_logos(html_text, mapping)
        )
        print(f"{slug}: {len(parsed)} logos")
        logos.update(parsed)
        time.sleep(0.2)
    return logos


def main() -> int:
    parser = argparse.ArgumentParser(description="Telecharge les logos clubs depuis AllRugby")
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--delay", type=float, default=0.15)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)

    logos = collect_logos()
    if not logos:
        print("Aucun logo trouve.", file=sys.stderr)
        return 1

    print(f"{len(logos)} logo(s) uniques")
    ok = 0
    for short, path in sorted(logos.items()):
        data = download_bytes(path)
        if data is None:
            print(f"  echec {short}: {path}", file=sys.stderr)
            continue
        target = args.output / f"{short}.png"
        if write_png(data, path, target):
            ok += 1
            print(f"  {short} <- {path.split('/')[-1]}")
        else:
            print(f"  echec conversion {short}", file=sys.stderr)
        time.sleep(args.delay)

    print(f"Termine: {ok}/{len(logos)} fichiers dans {args.output}")
    return 0 if ok > 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
