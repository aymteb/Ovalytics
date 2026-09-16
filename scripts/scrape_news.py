#!/usr/bin/env python3

import argparse
import csv
import re
import sys
import urllib.request
import xml.etree.ElementTree as ET
from datetime import datetime
from email.utils import parsedate_to_datetime
from pathlib import Path

RSS_URL = "https://www.rugbyrama.fr/rss.xml"
CSV_HEADERS = ["title", "summary", "sourceUrl", "publishedAt", "source", "competitionCode"]


def fetch(url: str) -> bytes:
    request = urllib.request.Request(url, headers={"User-Agent": "OvalyticsScraper/1.0"})
    with urllib.request.urlopen(request, timeout=30) as response:
        return response.read()


def guess_competition(title: str) -> str:
    lower = title.lower()
    if "top 14" in lower or "top14" in lower:
        return "TOP14"
    if "pro d2" in lower or "prod2" in lower:
        return "PROD2"
    return ""


def clean_html(text: str) -> str:
    if not text:
        return ""
    cleaned = re.sub(r"<[^>]+>", " ", text)
    return " ".join(cleaned.split())


def scrape_rugbyrama(limit: int) -> list[dict]:
    root = ET.fromstring(fetch(RSS_URL))
    channel = root.find("channel")
    if channel is None:
        raise RuntimeError("Flux RSS invalide")

    rows = []
    for item in channel.findall("item"):
        title_el = item.find("title")
        link_el = item.find("link")
        if title_el is None or link_el is None:
            continue
        title = (title_el.text or "").strip()
        link = (link_el.text or "").strip()
        if not title or not link:
            continue

        pub_el = item.find("pubDate")
        if pub_el is not None and pub_el.text:
            published = parsedate_to_datetime(pub_el.text).strftime("%Y-%m-%dT%H:%M:%S")
        else:
            published = datetime.now().strftime("%Y-%m-%dT%H:%M:%S")

        desc_el = item.find("description")
        summary = clean_html(desc_el.text if desc_el is not None else "")

        rows.append(
            {
                "title": title,
                "summary": summary[:500],
                "sourceUrl": link,
                "publishedAt": published,
                "source": "Rugbyrama",
                "competitionCode": guess_competition(title),
            }
        )
        if len(rows) >= limit:
            break
    return rows


def write_csv(rows: list[dict], output: Path) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=CSV_HEADERS)
        writer.writeheader()
        for row in rows:
            writer.writerow({key: row.get(key, "") for key in CSV_HEADERS})


def main() -> int:
    parser = argparse.ArgumentParser(description="Scrap le fil d'actualités rugby (RSS Rugbyrama)")
    parser.add_argument("--limit", type=int, default=30)
    parser.add_argument("--output", default="data/import/news.csv")
    args = parser.parse_args()

    root = Path(__file__).resolve().parent.parent
    output = (root / args.output).resolve()
    rows = scrape_rugbyrama(args.limit)
    write_csv(rows, output)
    print(f"{len(rows)} lignes -> {output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
