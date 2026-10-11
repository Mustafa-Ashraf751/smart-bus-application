#!/usr/bin/env bash
# One-time: copy the Supabase "public" schema (tables, data, sequences, Flyway history) into the local db container.
# Run on the VPS from $DEPLOY_PATH after the first deploy with the local database:
#   bash migrate-from-supabase.sh
# It REPLACES everything in the local "public" schema. Supabase itself is only read, never modified.
set -euo pipefail

cd "$(dirname "$0")"

SUPABASE_HOST=${SUPABASE_HOST:-aws-1-eu-west-1.pooler.supabase.com}
SUPABASE_PORT=${SUPABASE_PORT:-5432}
SUPABASE_DB=${SUPABASE_DB:-postgres}
SUPABASE_USER=${SUPABASE_USER:-postgres.dunhgmpegsmvwdakrvkw}

DB_CONTAINER=verysmartbus-db
APP_CONTAINER=verysmartbus-app

env_value() { grep -E "^$1=" .env | tail -1 | cut -d= -f2- || true; }
LOCAL_DB=$(env_value POSTGRES_DB); LOCAL_DB=${LOCAL_DB:-smart_bus_db}
LOCAL_USER=$(env_value POSTGRES_USER); LOCAL_USER=${LOCAL_USER:-postgres}

# Exact row count of every table in "public", used to compare source and target
COUNT_SQL="SELECT table_name || ' ' || (xpath('/row/c/text()', query_to_xml(format('SELECT count(*) AS c FROM public.%I', table_name), false, true, '')))[1]::text
FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE' ORDER BY table_name;"

remote_psql() { docker exec -e PGPASSWORD "$DB_CONTAINER" psql -h "$SUPABASE_HOST" -p "$SUPABASE_PORT" -U "$SUPABASE_USER" -d "$SUPABASE_DB" -v ON_ERROR_STOP=1 -tA "$@"; }
local_psql() { docker exec -i "$DB_CONTAINER" psql -U "$LOCAL_USER" -d "$LOCAL_DB" -v ON_ERROR_STOP=1 -tA "$@"; }

if ! docker ps --format '{{.Names}}' | grep -qx "$DB_CONTAINER"; then
  echo "$DB_CONTAINER is not running. Deploy first (or: docker compose -f docker-compose.prod.yml up -d db)." >&2
  exit 1
fi

read -rsp "Supabase database password for $SUPABASE_USER: " PGPASSWORD; echo
export PGPASSWORD

echo "==> Checking Supabase connection (one attempt)"
remote_psql -c 'SELECT 1' >/dev/null

mkdir -p backups
DUMP="backups/supabase-$(date -u +%Y%m%dT%H%M%SZ).dump"
echo "==> Dumping Supabase public schema to $DUMP"
docker exec -e PGPASSWORD "$DB_CONTAINER" pg_dump \
  -h "$SUPABASE_HOST" -p "$SUPABASE_PORT" -U "$SUPABASE_USER" -d "$SUPABASE_DB" \
  --schema=public --format=custom --no-owner --no-privileges > "$DUMP"
echo "    $(du -h "$DUMP" | cut -f1)"

echo "==> Counting rows in Supabase"
remote_psql -c "$COUNT_SQL" > backups/rowcounts-supabase.txt
unset PGPASSWORD

echo "==> Stopping the app"
docker stop "$APP_CONTAINER" >/dev/null 2>&1 || true

echo "==> Resetting local public schema"
local_psql <<'SQL'
DROP SCHEMA IF EXISTS public CASCADE;
CREATE SCHEMA public;
CREATE SCHEMA IF NOT EXISTS extensions;
CREATE EXTENSION IF NOT EXISTS postgis WITH SCHEMA extensions;
SQL

echo "==> Restoring into local database"
# Supabase-only objects (policies/grants for anon, authenticated, ...) can fail to restore; those errors are harmless.
docker exec -i "$DB_CONTAINER" pg_restore -U "$LOCAL_USER" -d "$LOCAL_DB" --no-owner --no-privileges \
  < "$DUMP" 2> backups/restore-errors.log || true
if [ -s backups/restore-errors.log ]; then
  echo "    pg_restore reported issues (see backups/restore-errors.log):"
  grep -E '^pg_restore: error' backups/restore-errors.log | head -20 | sed 's/^/    /'
fi

echo "==> Comparing row counts"
local_psql -c "$COUNT_SQL" > backups/rowcounts-local.txt
if diff backups/rowcounts-supabase.txt backups/rowcounts-local.txt; then
  echo "    All $(wc -l < backups/rowcounts-local.txt) tables match."
else
  echo "Row counts differ (left: Supabase, right: local). App NOT started; check backups/restore-errors.log." >&2
  exit 1
fi

echo "==> Starting the app"
docker start "$APP_CONTAINER" >/dev/null
APP_PORT=$(env_value APP_PORT); APP_PORT=${APP_PORT:-8080}
for _ in $(seq 1 36); do
  if curl -fsS "http://localhost:${APP_PORT}/actuator/health" 2>/dev/null | grep -q '"status":"UP"'; then
    echo "Done. The app is running on the local database."
    exit 0
  fi
  sleep 5
done
echo "App did not become healthy; check: docker logs --tail 100 $APP_CONTAINER" >&2
exit 1
