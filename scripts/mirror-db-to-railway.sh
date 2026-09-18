#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BACKUP_DIR="$ROOT/data/backups"
STAMP="$(date +%Y%m%d-%H%M%S)"
DUMP_FILE="$BACKUP_DIR/ovalytics-$STAMP.dump"
LATEST="$BACKUP_DIR/ovalytics-latest.dump"

LOCAL_DB="${LOCAL_DB:-ovalytics}"
LOCAL_USER="${LOCAL_USER:-aymteb}"
LOCAL_HOST="${LOCAL_HOST:-localhost}"
LOCAL_PORT="${LOCAL_PORT:-5432}"

mkdir -p "$BACKUP_DIR"

echo ">> Export Postgres local ($LOCAL_USER@$LOCAL_HOST:$LOCAL_PORT/$LOCAL_DB)"
pg_dump \
  --format=custom \
  --no-owner \
  --no-acl \
  --host="$LOCAL_HOST" \
  --port="$LOCAL_PORT" \
  --username="$LOCAL_USER" \
  --dbname="$LOCAL_DB" \
  --file="$DUMP_FILE"

cp "$DUMP_FILE" "$LATEST"
echo ">> Dump OK :"
echo "   $DUMP_FILE"
echo "   $LATEST"

if [[ "${1:-}" == "--restore-railway" ]]; then
  if [[ -z "${DATABASE_URL:-}" && -z "${RAILWAY_DATABASE_URL:-}" ]]; then
    echo "Erreur: définis DATABASE_URL (ou RAILWAY_DATABASE_URL) avant le restore."
    echo "Exemple:"
    echo "  export DATABASE_URL='postgresql://user:pass@host:port/railway'"
    echo "  ./scripts/mirror-db-to-railway.sh --restore-railway"
    exit 1
  fi
  TARGET_URL="${DATABASE_URL:-$RAILWAY_DATABASE_URL}"
  echo ">> Restore vers Railway (écrase le schéma public)"
  pg_restore \
    --clean \
    --if-exists \
    --no-owner \
    --no-acl \
    --dbname="$TARGET_URL" \
    "$LATEST"
  echo ">> Miroir local → Railway terminé."
else
  echo
  echo "Ensuite, pour pousser vers Railway :"
  echo "  1. Copie l'URL Postgres Railway (postgresql://...)"
  echo "  2. export DATABASE_URL='postgresql://...'"
  echo "  3. ./scripts/mirror-db-to-railway.sh --restore-railway"
fi
