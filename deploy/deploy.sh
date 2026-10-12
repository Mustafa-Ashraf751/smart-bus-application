#!/usr/bin/env bash
# Runs on the VPS (copied to $DEPLOY_PATH by the deploy workflow).
# Usage: APP_IMAGE=ghcr.io/<owner>/<repo>:<tag> bash deploy.sh
# Pulls and starts the image, waits for /actuator/health, rolls back to the previous image on failure.
set -euo pipefail

cd "$(dirname "$0")"
: "${APP_IMAGE:?APP_IMAGE is required}"

if [ ! -f .env ]; then
  echo "Missing $(pwd)/.env — create it from .env.example first." >&2
  exit 1
fi

COMPOSE="docker compose -f docker-compose.prod.yml"
APP_PORT=$(grep -E '^APP_PORT=' .env | cut -d= -f2 || true)
APP_PORT=${APP_PORT:-8080}
PREVIOUS_IMAGE=$(cat .current-image 2>/dev/null || true)

start() {
  APP_IMAGE="$1" $COMPOSE pull app
  APP_IMAGE="$1" $COMPOSE up -d --remove-orphans app
}

wait_healthy() {
  # Up to 3 minutes: startup includes Flyway migrations
  for _ in $(seq 1 36); do
    if curl -fsS "http://localhost:${APP_PORT}/actuator/health" 2>/dev/null | grep -q '"status":"UP"'; then
      return 0
    fi
    sleep 5
  done
  return 1
}

echo "Deploying $APP_IMAGE"
start "$APP_IMAGE"

if wait_healthy; then
  echo "$APP_IMAGE" > .current-image
  docker image prune -af --filter "until=168h" >/dev/null || true
  echo "Deployed $APP_IMAGE"
  exit 0
fi

echo "Health check failed for $APP_IMAGE" >&2
docker inspect verysmartbus-app \
  --format 'Status={{.State.Status}} ExitCode={{.State.ExitCode}} OOMKilled={{.State.OOMKilled}} Restarts={{.RestartCount}}' >&2 || true
free -h >&2 || true
docker logs --tail 200 verysmartbus-app || true

if [ -n "$PREVIOUS_IMAGE" ] && [ "$PREVIOUS_IMAGE" != "$APP_IMAGE" ]; then
  echo "Rolling back to $PREVIOUS_IMAGE" >&2
  start "$PREVIOUS_IMAGE"
  wait_healthy && echo "Rollback healthy" >&2 || echo "Rollback also unhealthy" >&2
fi
exit 1
