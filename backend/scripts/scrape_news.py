#!/usr/bin/env python3

import argparse
import csv
import html as html_lib
import re
import sys
import time
import urllib.error
import urllib.request
import xml.etree.ElementTree as ET
from datetime import datetime, timedelta
from email.utils import parsedate_to_datetime
from pathlib import Path
from zoneinfo import ZoneInfo

USER_AGENT = "OvalyticsScraper/1.0 (+https://github.com/)"
MIN_BODY_CHARS = 700
FETCH_DELAY_SEC = 0.35
MAX_PER_SOURCE = 12
PARIS = ZoneInfo("Europe/Paris")
URL_DATE_RE = re.compile(r"/(\d{4})/(\d{2})/(\d{2})/")
COMPOSITION_TITLE_RE = re.compile(
    r"(?:"
    r"\bles compos?\s+(?:pour|de la|de\b|du\b)"
    r"|\bcompos(?:itions?)?\s+(?:officielles?|compl[eè]tes?|du match)"
    r"|\bfeuille de match\b"
    r")",
    re.I,
)

STOP_WORDS = {
    "dans",
    "avec",
    "pour",
    "plus",
    "mais",
    "comme",
    "cette",
    "sont",
    "être",
    "etre",
    "après",
    "apres",
    "avant",
    "encore",
    "aussi",
    "leur",
    "leurs",
    "tous",
    "tout",
    "toute",
    "toutes",
    "face",
    "contre",
    "selon",
    "sous",
    "chez",
    "vers",
    "dont",
    "sans",
    "très",
    "tres",
    "deux",
    "trois",
    "quatre",
    "cinq",
    "match",
    "matchs",
    "saison",
    "équipe",
    "equipe",
    "joueurs",
    "joueur",
    "rugby",
    "actu",
    "actualité",
    "actualite",
    "article",
}

CSV_HEADERS = [
    "title",
    "summary",
    "body",
    "sourceUrl",
    "publishedAt",
    "source",
    "competitionCode",
    "imageUrl",
]

SOURCES = [
    {
        "name": "Rugbyrama",
        "kind": "rss",
        "feed": "https://www.rugbyrama.fr/rss.xml",
        "body_classes": ("article-full__body", "article-full__body-content", "article-full"),
    },
    {
        "name": "Le Rugbynistère",
        "kind": "rss",
        "feed": "https://www.lerugbynistere.fr/rss",
        "body_classes": ("entry-content", "article-content", "post-content"),
    },
    {
        "name": "Quinze Mondial",
        "kind": "listing",
        "listing": "https://www.quinzemondial.com/actualites.html",
        "link_re": r'href="(https://www\.quinzemondial\.com/actualites/[^"]+-\d+)"',
        "body_classes": ("single-post-content", "kopa-article-content"),
    },
    {
        "name": "All Rugby",
        "kind": "listing",
        "listing": "https://www.allrugby.com/news/",
        "link_re": r'href="(/news/[^"]+\.html)"',
        "link_prefix": "https://www.allrugby.com",
        "body_classes": ("news_content",),
    },
    {
        "name": "L'Équipe",
        "kind": "rss",
        "feed": "https://dwh.lequipe.fr/api/edito/rss?path=/Rugby/",
        "body_classes": ("Article__paragraphs", "article__body", "article-body"),
    },
]

NOISE_CUT_MARKERS = (
    "bloc-comment",
    "react-comments",
    "comment--main",
    "comments-section",
    "article-full__footer",
    "article-full__tags",
)


def fetch(url: str) -> str:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=30) as response:
        return response.read().decode("utf-8", "replace")


def local_name(tag: str) -> str:
    if "}" in tag:
        return tag.rsplit("}", 1)[-1]
    return tag


def clean_html_text(text: str) -> str:
    if not text:
        return ""
    cleaned = re.sub(r"<[^>]+>", " ", text)
    return " ".join(html_lib.unescape(cleaned).split())


def guess_competition(title: str, body: str = "", url: str = "") -> str:
    blobs = (
        title or "",
        url or "",
        (body or "")[:1200],
    )
    for blob in blobs:
        lower = blob.lower().replace("-", " ").replace("_", " ")
        if "top 14" in lower or "top14" in lower:
            return "TOP14"
        if "pro d2" in lower or "prod2" in lower:
            return "PROD2"
        if "nationale" in lower and "nationale 2" not in lower:
            return "NAT"
        if "champions cup" in lower or "coupe d europe" in lower:
            return "ERCC"
        if "challenge cup" in lower:
            return "ERCH"
        if "united rugby championship" in lower or " urc " in f" {lower} ":
            return "URC"
        if "premiership" in lower:
            return "PREM"
        if "test match" in lower or "tests internationaux" in lower:
            return "INT"
        if "supersevens" in lower or "super sevens" in lower:
            return "SEVENS"
        if re.search(r"\bsevens?\b", lower):
            return "SEVENS"
    return ""


def strong_words(title: str) -> set[str]:
    words = re.findall(r"[a-zàâäéèêëïîôùûüç0-9]{4,}", title.lower())
    return {w for w in words if w not in STOP_WORDS}


def is_duplicate(title: str, accepted: list[dict]) -> bool:
    words = strong_words(title)
    if len(words) < 3:
        return False
    for row in accepted:
        other = strong_words(row["title"])
        if len(words & other) >= 3:
            return True
    return False


def is_composition_recap(title: str) -> bool:
    return bool(COMPOSITION_TITLE_RE.search(title or ""))


def normalize_published(value: str) -> str | None:
    raw = (value or "").strip()
    if not raw:
        return None
    try:
        if raw.endswith("Z"):
            dt = datetime.fromisoformat(raw.replace("Z", "+00:00"))
        else:
            dt = datetime.fromisoformat(raw)
    except ValueError:
        try:
            dt = parsedate_to_datetime(raw)
        except (TypeError, ValueError, IndexError):
            return None
    if dt.tzinfo is not None:
        dt = dt.astimezone(PARIS).replace(tzinfo=None)
    return dt.strftime("%Y-%m-%dT%H:%M:%S")


def published_from_url(url: str) -> str | None:
    match = URL_DATE_RE.search(url or "")
    if not match:
        return None
    return f"{match.group(1)}-{match.group(2)}-{match.group(3)}T12:00:00"


def extract_published_from_html(page: str) -> str | None:
    patterns = (
        r'property=["\']article:published_time["\']\s+content=["\']([^"\']+)["\']',
        r'content=["\']([^"\']+)["\']\s+property=["\']article:published_time["\']',
        r'"datePublished"\s*:\s*"([^"]+)"',
        r'<time[^>]+datetime=["\']([^"\']+)["\']',
    )
    for pattern in patterns:
        match = re.search(pattern, page, re.I)
        if match:
            normalized = normalize_published(match.group(1))
            if normalized:
                return normalized
    return None


def is_fresh(published_at: str, max_age_days: int) -> bool:
    if max_age_days <= 0:
        return True
    normalized = normalize_published(published_at)
    if normalized is None:
        return False
    published_day = datetime.fromisoformat(normalized).date()
    today = datetime.now(PARIS).date()
    oldest = today - timedelta(days=max(0, max_age_days - 1))
    return published_day >= oldest


def looks_like_image_url(url: str) -> bool:
    if not url.startswith("http"):
        return False
    return bool(re.search(r"\.(jpe?g|png|webp|gif)(\?|$)", url, re.I))


def prefer_large_image(url: str) -> str:
    if not url:
        return url
    upgraded = re.sub(
        r"(/images/view/[^/]+/)standard/",
        r"\1large/",
        url,
        count=1,
        flags=re.I,
    )
    return upgraded


def extract_image_from_rss_item(item: ET.Element) -> str:
    for enc in item.findall("enclosure"):
        url = (enc.get("url") or "").strip()
        typ = (enc.get("type") or "").lower()
        if url and (typ.startswith("image") or looks_like_image_url(url)):
            return url
    for el in item.iter():
        name = local_name(el.tag).lower()
        if name not in ("content", "thumbnail", "image"):
            continue
        url = (el.get("url") or (el.text or "")).strip()
        if looks_like_image_url(url) or (url.startswith("http") and name in ("thumbnail", "image")):
            return url
    desc_el = item.find("description")
    if desc_el is not None and desc_el.text:
        match = re.search(r'<img[^>]+src=["\']([^"\']+)["\']', desc_el.text, re.I)
        if match:
            return match.group(1).strip()
    return ""


def extract_image_from_html(page: str) -> str:
    match = re.search(
        r'<meta[^>]+property=["\']og:image["\'][^>]+content=["\']([^"\']+)["\']',
        page,
        re.I,
    )
    if match:
        return match.group(1).strip()
    match = re.search(
        r'<meta[^>]+content=["\']([^"\']+)["\'][^>]+property=["\']og:image["\']',
        page,
        re.I,
    )
    if match:
        return match.group(1).strip()
    return ""


def extract_title_from_html(page: str) -> str:
    match = re.search(r'<meta[^>]+property=["\']og:title["\'][^>]+content=["\']([^"\']+)["\']', page, re.I)
    if match:
        return html_lib.unescape(match.group(1).strip())
    match = re.search(r"<h1[^>]*>(.*?)</h1>", page, re.S | re.I)
    if match:
        return clean_html_text(match.group(1))
    match = re.search(r"<title[^>]*>(.*?)</title>", page, re.S | re.I)
    if match:
        return clean_html_text(match.group(1))
    return ""


def cut_after_noise(chunk: str) -> str:
    earliest = len(chunk)
    for marker in NOISE_CUT_MARKERS:
        match = re.search(
            rf'<[^>]+(?:class|id)=["\'][^"\']*{re.escape(marker)}[^"\']*["\']',
            chunk,
            re.I,
        )
        if match and match.start() < earliest:
            earliest = match.start()
    return chunk[:earliest]


def is_junk_paragraph(text: str) -> bool:
    lower = text.lower()
    if len(text) < 60:
        return True
    markers = (
        "commencer à taper",
        "programme tv",
        "carte bancaire",
        "votre abonnement",
        "créer un compte",
        "creez un compte",
        "mentions légales",
        "données personnelles",
        "lien copié",
        "cursor-pointer",
        "copied = false",
        "urlcopied",
        "connectez pour consulter",
        "souhaitez-vous recevoir",
        "le rugbynistere est un site",
        "le rugbynistère est un site",
        "cliquez ici pour la mettre à jour",
        "paiement a échoué",
        "paiement a echoue",
        "sans action de votre part",
        "article rédigé par",
        "copyright",
        "newsletter",
        "nous suivre",
        "lire l'article",
        "lire l’article",
        "signaler",
        "forcerment les clubs",
        "ok je sors",
        "var slmadshb",
        "display(\"quinzemondial",
        "ajouter aux sources préférées",
        "partager :",
        "publié le ",
        "mis à jour le ",
        "dans votre boite mail",
        "dans votre boîte mail",
        "retrouvez tous les soirs",
    )
    if any(marker in lower for marker in markers):
        return True
    if text.count("http") > 2:
        return True
    nav_hits = sum(
        1
        for word in (
            "actualités",
            "classement",
            "transferts",
            "champions cup",
            "pro d2",
            "top 14",
            "xv de france",
        )
        if word in lower
    )
    if nav_hits >= 3 and len(text) < 400:
        return True
    if nav_hits >= 4:
        return True
    if lower.count("remplaçants :") >= 2 and "le xv de départ" in lower:
        return False
    return False


def paragraphs_from_html(chunk: str) -> list[str]:
    cleaned = re.sub(r"<script[^>]*>.*?</script>", "", chunk, flags=re.S | re.I)
    cleaned = re.sub(r"<style[^>]*>.*?</style>", "", cleaned, flags=re.S | re.I)
    cleaned = re.sub(r"<noscript[^>]*>.*?</noscript>", "", cleaned, flags=re.S | re.I)
    cleaned = re.sub(r"<nav[^>]*>.*?</nav>", "", cleaned, flags=re.S | re.I)
    cleaned = re.sub(r"<footer[^>]*>.*?</footer>", "", cleaned, flags=re.S | re.I)
    cleaned = re.sub(r"<header[^>]*>.*?</header>", "", cleaned, flags=re.S | re.I)

    paragraphs = []
    for raw in re.findall(r"<p[^>]*>(.*?)</p>", cleaned, re.S | re.I):
        text = clean_html_text(raw)
        if is_junk_paragraph(text):
            continue
        paragraphs.append(text)
    return paragraphs


def body_from_chunk(chunk: str) -> str:
    paragraphs = paragraphs_from_html(cut_after_noise(chunk))
    if not paragraphs:
        return ""
    return "\n\n".join(paragraphs)


def extract_body(page: str, body_classes: tuple[str, ...]) -> str:
    for cls in body_classes:
        marker = re.search(rf'class="[^"]*{re.escape(cls)}[^"]*"', page, re.I)
        if not marker:
            continue
        start = page.find(">", marker.end())
        if start < 0:
            continue
        joined = body_from_chunk(page[start + 1 : start + 1 + 80_000])
        if len(joined) >= MIN_BODY_CHARS:
            return joined

    article = re.search(r"<article[^>]*>(.*?)</article>", page, re.S | re.I)
    if article:
        joined = body_from_chunk(article.group(1))
        if len(joined) >= MIN_BODY_CHARS:
            return joined

    return body_from_chunk(page)


def parse_rss_items(feed_url: str, limit: int, max_age_days: int) -> list[dict]:
    root = ET.fromstring(fetch(feed_url))
    channel = root.find("channel")
    nodes = channel.findall("item") if channel is not None else root.findall(".//item")
    rows = []
    for item in nodes:
        title_el = item.find("title")
        link_el = item.find("link")
        title = (title_el.text or "").strip() if title_el is not None else ""
        link = (link_el.text or "").strip() if link_el is not None else ""
        if not link:
            guid = item.find("guid")
            link = (guid.text or "").strip() if guid is not None else ""
        link = link.split("#")[0].strip()
        if not title or not link or not link.startswith("http"):
            continue
        if "/actu-en-direct/" in link or "/collection/" in link:
            continue
        if is_composition_recap(title):
            print(f"skip compos: {title[:80]}", file=sys.stderr)
            continue

        pub_el = item.find("pubDate")
        published = None
        if pub_el is not None and pub_el.text:
            published = normalize_published(pub_el.text)
        if published is None:
            published = published_from_url(link)
        if published is None:
            published = datetime.now(PARIS).strftime("%Y-%m-%dT%H:%M:%S")
        if not is_fresh(published, max_age_days):
            continue

        rows.append(
            {
                "title": title,
                "sourceUrl": link,
                "publishedAt": published,
                "imageUrl": extract_image_from_rss_item(item),
            }
        )
        if len(rows) >= limit:
            break
    return rows


def parse_listing_items(source: dict, limit: int, max_age_days: int) -> list[dict]:
    page = fetch(source["listing"])
    links = re.findall(source["link_re"], page)
    prefix = source.get("link_prefix", "")
    seen = set()
    rows = []
    for href in links:
        url = href if href.startswith("http") else prefix + href
        url = url.split("#")[0].strip()
        if url in seen:
            continue
        seen.add(url)
        published = published_from_url(url)
        if published is not None and not is_fresh(published, max_age_days):
            continue
        rows.append(
            {
                "title": "",
                "sourceUrl": url,
                "publishedAt": published or "",
                "imageUrl": "",
            }
        )
        if len(rows) >= limit * 2:
            break
    return rows


def enrich_with_body(candidate: dict, source: dict, max_age_days: int) -> dict | None:
    url = candidate["sourceUrl"]
    try:
        page = fetch(url)
    except (urllib.error.HTTPError, urllib.error.URLError, TimeoutError) as exc:
        print(f"skip {source['name']}: {url} ({exc})", file=sys.stderr)
        return None

    body = extract_body(page, source["body_classes"])
    if len(body) < MIN_BODY_CHARS:
        print(f"skip short {source['name']}: {url} ({len(body)} chars)", file=sys.stderr)
        return None

    title = candidate["title"] or extract_title_from_html(page)
    title = re.sub(r"\s+", " ", html_lib.unescape(title or "")).strip()
    if not title:
        return None
    if is_composition_recap(title):
        print(f"skip compos: {title[:80]}", file=sys.stderr)
        return None

    published = normalize_published(candidate.get("publishedAt") or "")
    if published is None:
        published = published_from_url(url)
    if published is None:
        published = extract_published_from_html(page)
    if published is None:
        published = datetime.now(PARIS).strftime("%Y-%m-%dT%H:%M:%S")
    if not is_fresh(published, max_age_days):
        print(f"skip old {source['name']}: {title[:80]} ({published[:10]})", file=sys.stderr)
        return None

    image = candidate.get("imageUrl") or extract_image_from_html(page)
    image = prefer_large_image(image)
    summary = body.split("\n\n", 1)[0][:400]

    return {
        "title": title[:300],
        "summary": summary,
        "body": body,
        "sourceUrl": url[:500],
        "publishedAt": published,
        "source": source["name"],
        "competitionCode": guess_competition(title, body, url),
        "imageUrl": image[:500] if image else "",
    }


def scrape_all(limit_per_source: int, max_age_days: int) -> list[dict]:
    accepted: list[dict] = []
    seen_urls: set[str] = set()

    for source in SOURCES:
        print(f"source {source['name']}…", file=sys.stderr)
        try:
            if source["kind"] == "rss":
                candidates = parse_rss_items(source["feed"], limit_per_source, max_age_days)
            else:
                candidates = parse_listing_items(source, limit_per_source, max_age_days)
        except (urllib.error.HTTPError, urllib.error.URLError, ET.ParseError) as exc:
            print(f"feed fail {source['name']}: {exc}", file=sys.stderr)
            continue

        kept = 0
        for candidate in candidates:
            if kept >= limit_per_source:
                break
            url = candidate["sourceUrl"]
            if url in seen_urls:
                continue
            time.sleep(FETCH_DELAY_SEC)
            row = enrich_with_body(candidate, source, max_age_days)
            if row is None:
                continue
            if is_duplicate(row["title"], accepted):
                print(f"skip dup {source['name']}: {row['title'][:80]}", file=sys.stderr)
                continue
            seen_urls.add(url)
            accepted.append(row)
            kept += 1

    return accepted


def write_csv(rows: list[dict], output: Path) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(
            file,
            fieldnames=CSV_HEADERS,
            quoting=csv.QUOTE_MINIMAL,
            lineterminator="\n",
        )
        writer.writeheader()
        for row in rows:
            payload = dict(row)
            payload["body"] = row["body"].replace("\n", "\\n")
            writer.writerow({key: payload.get(key, "") for key in CSV_HEADERS})


def main() -> int:
    parser = argparse.ArgumentParser(description="Scrap actu rugby multi-sources (corps complet)")
    parser.add_argument("--limit", type=int, default=MAX_PER_SOURCE)
    parser.add_argument("--output", default="data/import/news.csv")
    parser.add_argument(
        "--max-age-days",
        type=int,
        default=1,
        help="Ne garder que les articles publies sur les N derniers jours calendaires (Paris)",
    )
    args = parser.parse_args()

    root = Path(__file__).resolve().parent.parent
    output_arg = Path(args.output)
    output = output_arg if output_arg.is_absolute() else (root / output_arg).resolve()

    rows = scrape_all(args.limit, args.max_age_days)
    write_csv(rows, output)
    print(f"{len(rows)} lignes -> {output} (max-age-days={args.max_age_days})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
