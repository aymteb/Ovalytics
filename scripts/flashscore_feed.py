import html
import json
import re
import sys
import urllib.error
import urllib.request
from datetime import datetime, timezone

FEED_BASE = "https://global.flashscore.ninja/2/x/feed"
FLASHSCORE_BASE = "https://www.flashscore.fr"
FLASHSCORE_TOP14_URL = f"{FLASHSCORE_BASE}/rugby/france/top-14/"
LIVE_UPDATE_FEED = "u_8_1"

FS_NAME_TO_SHORT = {
    "Stade Toulousain": "TOU",
    "Toulouse": "TOU",
    "Racing 92": "RAC",
    "Racing Metro 92": "RAC",
    "Stade Français": "SFP",
    "Stade Français Paris": "SFP",
    "RC Toulon": "TOL",
    "Toulon": "TOL",
    "Stade Rochelais": "LAR",
    "La Rochelle": "LAR",
    "Union Bordeaux-Bègles": "UBB",
    "Union Bordeaux Begles": "UBB",
    "Union Bordeaux-Begles": "UBB",
    "Bordeaux Bègles": "UBB",
    "Bordeaux Begles": "UBB",
    "ASM Clermont": "ASM",
    "Clermont": "ASM",
    "Lyon OU": "LOU",
    "LOU Rugby": "LOU",
    "Lyon": "LOU",
    "Montpellier HR": "MHR",
    "Montpellier": "MHR",
    "Montpellier Hérault Rugby": "MHR",
    "Castres Olympique": "CAS",
    "Castres": "CAS",
    "Section Paloise": "PAU",
    "Pau": "PAU",
    "Aviron Bayonnais": "BAY",
    "Bayonne": "BAY",
    "USA Perpignan": "USAP",
    "Perpignan": "USAP",
    "RC Vannes": "VAN",
    "Vannes": "VAN",
    "US Montauban": "MTB",
    "Montauban": "MTB",
}

FS_PROD2_NAME_TO_SHORT = {
    "Biarritz Olympique": "BIA",
    "Biarritz": "BIA",
    "Stade Niçois": "NIC",
    "Stade Nicois": "NIC",
    "Nissa": "NIC",
    "Nice": "NIC",
    "Soyaux Angoulême": "ANG",
    "Angoulême": "ANG",
    "Colomiers Rugby": "COL",
    "Colomiers": "COL",
    "AS Béziers Hérault": "BEZ",
    "AS Béziers": "BEZ",
    "Beziers": "BEZ",
    "Béziers": "BEZ",
    "US Oyonnax": "OYO",
    "Oyonnax Rugby": "OYO",
    "Oyonnax": "OYO",
    "US Dax": "DAX",
    "Dax": "DAX",
    "RC Narbonne": "NAR",
    "Narbonne": "NAR",
    "FC Grenoble": "GRE",
    "Grenoble": "GRE",
    "SA Aurillac": "AUR",
    "Stade Aurillacois": "AUR",
    "Aurillac": "AUR",
    "USON Nevers": "NEV",
    "USO Nevers Rugby": "NEV",
    "Nevers": "NEV",
    "US Montauban": "MTB",
    "Montauban": "MTB",
    "Provence Rugby": "AIX",
    "Aix-en-Provence": "AIX",
    "SU Agen": "AGE",
    "Agen": "AGE",
    "CA Brive": "BRI",
    "Brive": "BRI",
    "Valence Romans": "VAL",
    "Valence Romans Drôme Rugby": "VAL",
}

FS_DEMO_REMAP = {"MTB": "VAN"}


def fetch_page(url: str, user_agent: str) -> str:
    request = urllib.request.Request(url, headers={"User-Agent": user_agent})
    with urllib.request.urlopen(request, timeout=30) as response:
        return response.read().decode("utf-8")


def feed_sign(user_agent: str) -> str:
    page = fetch_page(FLASHSCORE_TOP14_URL, user_agent)
    match = re.search(r'"feed_sign":"([^"]+)"', page)
    if match is None:
        raise RuntimeError("feed_sign introuvable sur Flashscore")
    return match.group(1)


def fetch_feed(feed_id: str, sign: str, user_agent: str) -> str:
    url = f"{FEED_BASE}/{feed_id}"
    request = urllib.request.Request(
        url,
        headers={
            "User-Agent": user_agent,
            "Referer": "https://www.flashscore.fr/",
            "x-fsign": sign,
        },
    )
    with urllib.request.urlopen(request, timeout=30) as response:
        return response.read().decode("utf-8")


def parse_embedded_events(page_html: str) -> list[dict]:
    events = []
    seen_ids = set()
    for chunk in page_html.split("¬~"):
        if "AA÷" not in chunk or "AE÷" not in chunk:
            continue
        fields = dict(re.findall(r"([A-Z]{2})÷([^¬]*)", chunk))
        event_id = fields.get("AA", "")
        if event_id and event_id in seen_ids:
            continue
        if event_id:
            seen_ids.add(event_id)
        events.append(fields)
    return events


def parse_events(raw: str) -> list[dict]:
    return parse_embedded_events(raw)


def season_list(page_html: str) -> list[dict]:
    match = re.search(r'"season_list":(\[.*?\])', page_html)
    if match is None:
        return []
    return json.loads(html.unescape(match.group(1)))


def principal_stage_id(page_html: str) -> str | None:
    match = re.search(r'"stages_group":(\{.*?\}),"flag_id"', page_html)
    if match is None:
        return None
    stages_group = json.loads(html.unescape(match.group(1)))
    for stage in stages_group.get("stages", []):
        if stage.get("name") == "Principal":
            return stage.get("id")
    return None


def tournament_prefix(pathname: str) -> str | None:
    parts = pathname.strip("/").split("/")
    if len(parts) < 2:
        return None
    return "/" + "/".join(parts[:2]) + "/"


def page_urls_for_season(season_label: str, user_agent: str) -> list[str]:
    fs_season = season_label.replace("-", "/")
    hub_url = f"{FLASHSCORE_BASE}/rugby/france/top-14/calendrier/"
    hub = fetch_page(hub_url, user_agent)

    seasons = season_list(hub)
    selected_match = re.search(r'"selected_season_id":(\d+)', hub)
    selected_id = int(selected_match.group(1)) if selected_match else 0

    target = None
    for entry in seasons:
        if entry.get("name") == fs_season:
            target = entry
            break
    if target is None:
        raise RuntimeError(f"Saison Flashscore introuvable: {season_label}")

    urls = []
    if target.get("id") == selected_id:
        urls.append(f"{FLASHSCORE_BASE}/rugby/france/top-14/calendrier/")
        urls.append(f"{FLASHSCORE_BASE}/rugby/france/top-14/resultats/")

    pathname = target.get("pathname", "")
    prefix = tournament_prefix(pathname)
    if prefix:
        try:
            season_page = fetch_page(FLASHSCORE_BASE + pathname, user_agent)
        except urllib.error.HTTPError:
            season_page = hub
        stage_id = principal_stage_id(season_page) or principal_stage_id(hub)
        if stage_id:
            urls.append(f"{FLASHSCORE_BASE}{prefix}{stage_id}/calendrier/")
            urls.append(f"{FLASHSCORE_BASE}{prefix}{stage_id}/resultats/")

    unique = []
    seen = set()
    for url in urls:
        if url not in seen:
            seen.add(url)
            unique.append(url)
    return unique


def team_short(name: str, demo_map: bool) -> str | None:
    clean = html.unescape(name).strip()
    if "7s" in clean.lower():
        return None
    short = _resolve_short(clean, FS_NAME_TO_SHORT)
    if short is None:
        short = _resolve_short(clean, FS_PROD2_NAME_TO_SHORT)
    if short is None:
        return None
    if demo_map and short in FS_DEMO_REMAP:
        return FS_DEMO_REMAP[short]
    return short


def map_competition_pair(home_name: str, away_name: str, demo_map: bool) -> tuple[str, str, str] | None:
    home_clean = html.unescape(home_name).strip()
    away_clean = html.unescape(away_name).strip()
    top14_home = _resolve_short(home_clean, FS_NAME_TO_SHORT)
    top14_away = _resolve_short(away_clean, FS_NAME_TO_SHORT)
    if top14_home and top14_away:
        if demo_map:
            top14_home = FS_DEMO_REMAP.get(top14_home, top14_home)
            top14_away = FS_DEMO_REMAP.get(top14_away, top14_away)
        return "TOP14", top14_home, top14_away
    prod2_home = _resolve_short(home_clean, FS_PROD2_NAME_TO_SHORT)
    prod2_away = _resolve_short(away_clean, FS_PROD2_NAME_TO_SHORT)
    if prod2_home and prod2_away:
        return "PROD2", prod2_home, prod2_away
    return None


def _resolve_short(clean: str, mapping: dict[str, str]) -> str | None:
    short = mapping.get(clean)
    if short is not None:
        return short
    lower = clean.lower()
    for label, code in mapping.items():
        label_lower = label.lower()
        if label_lower in lower or lower in label_lower:
            return code
    return None


def kickoff_from_timestamp(value: str) -> str | None:
    if not value or not value.isdigit():
        return None
    dt = datetime.fromtimestamp(int(value), tz=timezone.utc)
    return dt.strftime("%Y-%m-%dT%H:%M:%S")


def event_to_row(fields: dict, demo_map: bool) -> dict | None:
    home_name = fields.get("AE", "")
    away_name = fields.get("AF", "")
    mapped = map_competition_pair(home_name, away_name, demo_map)
    if mapped is None:
        return None
    competition_code, home_short, away_short = mapped

    kickoff = kickoff_from_timestamp(fields.get("AD", ""))
    if kickoff is None:
        return None

    status_code = fields.get("AB", "")
    finished = status_code == "3"
    live = status_code in ("2", "12", "13")
    if finished:
        status = "FINISHED"
    elif live:
        status = "LIVE"
    else:
        status = "SCHEDULED"
    home_score = fields.get("AG", "")
    away_score = fields.get("AH", "")
    if not finished and not live:
        home_score = ""
        away_score = ""
    matchday = fields.get("CR", "") or ""

    return {
        "competitionCode": competition_code,
        "homeShortName": home_short,
        "awayShortName": away_short,
        "matchday": matchday,
        "kickoffAt": kickoff,
        "status": status,
        "homeScore": home_score,
        "awayScore": away_score,
        "homeTries": "",
        "awayTries": "",
        "flashscoreEventId": fields.get("AA", ""),
    }


def events_to_rows(events: list[dict], demo_map: bool) -> list[dict]:
    rows = []
    for fields in events:
        row = event_to_row(fields, demo_map)
        if row is not None:
            rows.append(row)
    return rows


def scrape_tournament_pages(
    user_agent: str,
    demo_map: bool,
    season_label: str,
) -> list[dict]:
    urls = page_urls_for_season(season_label, user_agent)
    merged_fields: dict[str, dict] = {}

    for url in urls:
        try:
            page = fetch_page(url, user_agent)
        except urllib.error.HTTPError as error:
            print(f"Flashscore page inaccessible: {url} ({error})", file=sys.stderr)
            continue

        for fields in parse_embedded_events(page):
            event_id = fields.get("AA", "")
            if not event_id:
                continue
            if event_id in merged_fields and merged_fields[event_id].get("AB") == "3":
                continue
            merged_fields[event_id] = fields

    rows = events_to_rows(list(merged_fields.values()), demo_map)
    if not rows:
        print(
            f"Flashscore: aucun match embarqué pour {season_label} "
            "(saison archivée ou chargée en JS). Utiliser LNR ou --fs-live-days.",
            file=sys.stderr,
        )
    return rows


def scrape_day_window(
    user_agent: str,
    demo_map: bool,
    from_day: int,
    to_day: int,
) -> list[dict]:
    sign = feed_sign(user_agent)
    rows = []
    seen = set()

    for day in range(from_day, to_day + 1):
        feed_id = f"f_8_{day}_3_fr_1"
        try:
            raw = fetch_feed(feed_id, sign, user_agent)
        except urllib.error.HTTPError:
            continue
        if len(raw) < 50:
            continue

        for fields in parse_events(raw):
            row = event_to_row(fields, demo_map)
            if row is None:
                continue
            key = (
                row["homeShortName"],
                row["awayShortName"],
                row["kickoffAt"][:10],
            )
            if key in seen:
                continue
            seen.add(key)
            rows.append(row)

    return rows


def scrape_flashscore(
    user_agent: str,
    demo_map: bool,
    season_label: str,
    live_from_day: int | None,
    live_to_day: int | None,
) -> list[dict]:
    rows = scrape_tournament_pages(user_agent, demo_map, season_label)
    if live_from_day is not None and live_to_day is not None:
        live_rows = scrape_day_window(user_agent, demo_map, live_from_day, live_to_day)
        rows = merge_rows(rows, live_rows)
    return rows


def fetch_live_snapshot(user_agent: str) -> str:
    sign = feed_sign(user_agent)
    return fetch_feed(LIVE_UPDATE_FEED, sign, user_agent)


def fetch_h2h_feed(event_id: str, user_agent: str) -> str:
    sign = feed_sign(user_agent)
    return fetch_feed(f"df_hh_1_{event_id}", sign, user_agent)


def parse_h2h_section(raw: str) -> list[dict]:
    match = re.search(r"KB÷Head-to-head matches¬(.*?)(?=~KB÷|$)", raw, re.S)
    if match is None:
        return []
    section = match.group(1)
    rows: list[dict] = []
    for chunk in section.split("¬~"):
        if "KU÷" not in chunk and "KL÷" not in chunk:
            continue
        fields = dict(re.findall(r"([A-Z]{2,3})÷([^¬]*)", chunk))
        home_name = fields.get("FH") or fields.get("KK") or ""
        away_name = fields.get("FK") or fields.get("FL") or ""
        if not home_name or not away_name:
            continue
        competition_label = fields.get("KF", "")
        if "riendly" in competition_label.lower() or competition_label.upper() == "AMI":
            continue
        if fields.get("KL") and ":" in fields["KL"]:
            home_score, away_score = fields["KL"].split(":", 1)
        else:
            home_score = fields.get("KU", "")
            away_score = fields.get("KT", "")
        if not home_score.isdigit() or not away_score.isdigit():
            continue
        kickoff = kickoff_from_timestamp(fields.get("KC", ""))
        if kickoff is None:
            continue
        mapped = map_competition_pair(home_name, away_name, False)
        if mapped is None:
            continue
        competition_code, home_short, away_short = mapped
        day_key = kickoff[:10].replace("-", "")
        rows.append(
            {
                "competitionCode": competition_code,
                "homeShortName": home_short,
                "awayShortName": away_short,
                "matchday": day_key,
                "kickoffAt": kickoff,
                "status": "FINISHED",
                "homeScore": home_score,
                "awayScore": away_score,
                "homeTries": "",
                "awayTries": "",
                "flashscoreEventId": fields.get("KP", ""),
            }
        )
    return rows


def scrape_h2h_for_event(event_id: str, user_agent: str) -> list[dict]:
    raw = fetch_h2h_feed(event_id, user_agent)
    if not raw or raw.strip() == "0":
        return []
    return parse_h2h_section(raw)


def merge_rows(primary: list[dict], overlay: list[dict]) -> list[dict]:
    index: dict[tuple[str, str, str], dict] = {}
    for row in primary:
        key = (row["homeShortName"], row["awayShortName"], str(row["matchday"]))
        copy = {k: v for k, v in row.items() if not k.startswith("flashscore")}
        index[key] = copy

    for row in overlay:
        key = (row["homeShortName"], row["awayShortName"], str(row["matchday"]))
        if key in index:
            target = index[key]
            if row.get("status") in ("FINISHED", "LIVE") and row.get("homeScore") != "":
                target["status"] = row["status"]
                target["homeScore"] = row["homeScore"]
                target["awayScore"] = row["awayScore"]
            if row.get("kickoffAt"):
                target["kickoffAt"] = row["kickoffAt"]
        else:
            by_date = None
            for existing_key, existing in index.items():
                if (
                    existing_key[0] == row["homeShortName"]
                    and existing_key[1] == row["awayShortName"]
                    and existing.get("kickoffAt", "")[:10] == row.get("kickoffAt", "")[:10]
                ):
                    by_date = existing
                    break
            if by_date is not None:
                if row.get("status") in ("FINISHED", "LIVE") and row.get("homeScore") != "":
                    by_date["status"] = row["status"]
                    by_date["homeScore"] = row["homeScore"]
                    by_date["awayScore"] = row["awayScore"]
            else:
                copy = {k: v for k, v in row.items() if k in (
                    "competitionCode", "homeShortName", "awayShortName", "matchday",
                    "kickoffAt", "status", "homeScore", "awayScore", "homeTries", "awayTries",
                )}
                index[key] = copy

    return list(index.values())
