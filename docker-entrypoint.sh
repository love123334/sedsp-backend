#!/bin/sh
set -e

# Render injects DATABASE_URL=postgres://user:pass@host:5432/dbname
if [ -z "${DATABASE_URL:-}" ]; then
  echo "[entrypoint] ERROR: DATABASE_URL is missing."
  echo "[entrypoint] On Render: link Postgres → Environment → DATABASE_URL (Internal Database URL)."
  exit 1
fi

REST="${DATABASE_URL#*://}"
USERPASS="${REST%%@*}"
HOSTDB="${REST#*@}"
USER="${USERPASS%%:*}"
PASS="${USERPASS#*:}"
export SPRING_DATASOURCE_USERNAME="$USER"
export SPRING_DATASOURCE_PASSWORD="$PASS"

JDBC="jdbc:postgresql://${HOSTDB}"
case "$JDBC" in
  *\?*) JDBC="${JDBC}&sslmode=require" ;;
  *)    JDBC="${JDBC}?sslmode=require" ;;
esac
export SPRING_DATASOURCE_URL="$JDBC"
echo "[entrypoint] Datasource configured (sslmode=require) host=${HOSTDB%%/*}"

# Render Key Value: REDIS_URL=redis://...
if [ -n "${REDIS_URL:-}" ]; then
  export SPRING_DATA_REDIS_URL="$REDIS_URL"
  echo "[entrypoint] Redis URL configured"
fi

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-render}"
export SERVER_PORT="${PORT:-8080}"

echo "[entrypoint] Starting SEDSP profile=${SPRING_PROFILES_ACTIVE} port=${SERVER_PORT}"
# shellcheck disable=SC2086
exec java ${JAVA_OPTS:-} \
  -Dserver.port="${SERVER_PORT}" \
  -Dspring.datasource.url="${SPRING_DATASOURCE_URL}" \
  -Dspring.datasource.username="${SPRING_DATASOURCE_USERNAME}" \
  -Dspring.datasource.password="${SPRING_DATASOURCE_PASSWORD}" \
  -jar /app/app.jar
