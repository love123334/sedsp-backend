#!/bin/sh
# Always start Java. Print DB env diagnostics for Railway Deploy Logs.
echo "==== SEDSP BOOT ===="
echo "PORT=${PORT:-unset} SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-unset}"

for k in \
  DATABASE_URL DATABASE_PUBLIC_URL DATABASE_PRIVATE_URL \
  POSTGRES_URL POSTGRES_PUBLIC_URL \
  PGHOST PGPORT PGUSER PGDATABASE \
  DATABASE_HOST DATABASE_PORT DATABASE_USER DATABASE_NAME \
  REDIS_URL SPRING_DATASOURCE_URL
do
  v=$(printenv "$k" 2>/dev/null || true)
  if [ -n "$v" ]; then
    echo "[env] $k=SET len=${#v}"
  else
    echo "[env] $k=UNSET"
  fi
done

pick=""
pick_name=""
for k in SPRING_DATASOURCE_URL DATABASE_PUBLIC_URL DATABASE_URL DATABASE_PRIVATE_URL POSTGRES_PUBLIC_URL POSTGRES_URL; do
  v=$(printenv "$k" 2>/dev/null || true)
  if [ -n "$v" ]; then
    pick="$v"
    pick_name="$k"
    echo "[env] selected $k"
    break
  fi
done

if [ -n "$pick" ]; then
  case "$pick" in
    jdbc:postgresql://*)
      export SPRING_DATASOURCE_URL="$pick"
      ;;
    postgres://*|postgresql://*)
      rest="${pick#*://}"
      userpass="${rest%%@*}"
      hostdb="${rest#*@}"
      user="${userpass%%:*}"
      pass="${userpass#*:}"
      if [ "$pass" = "$userpass" ]; then
        pass=""
      fi
      jdbc="jdbc:postgresql://${hostdb}"
      case "$hostdb" in
        *railway.internal*)
          case "$jdbc" in *\?*) jdbc="${jdbc}&sslmode=prefer" ;; *) jdbc="${jdbc}?sslmode=prefer" ;; esac
          ;;
        *)
          case "$jdbc" in *\?*) jdbc="${jdbc}&sslmode=require" ;; *) jdbc="${jdbc}?sslmode=require" ;; esac
          ;;
      esac
      export SPRING_DATASOURCE_URL="$jdbc"
      export SPRING_DATASOURCE_USERNAME="$user"
      export SPRING_DATASOURCE_PASSWORD="$pass"
      ;;
    *)
      echo "[env] WARN: $pick_name has unexpected prefix"
      ;;
  esac
  echo "[env] JDBC ready from $pick_name"
elif [ -n "$(printenv PGHOST 2>/dev/null)$(printenv DATABASE_HOST 2>/dev/null)" ]; then
  host=$(printenv PGHOST 2>/dev/null || printenv DATABASE_HOST)
  port=$(printenv PGPORT 2>/dev/null || printenv DATABASE_PORT || echo 5432)
  [ -z "$port" ] && port=5432
  db=$(printenv PGDATABASE 2>/dev/null || printenv DATABASE_NAME || echo railway)
  [ -z "$db" ] && db=railway
  user=$(printenv PGUSER 2>/dev/null || printenv DATABASE_USER || echo postgres)
  pass=$(printenv PGPASSWORD 2>/dev/null || printenv DATABASE_PASSWORD || true)
  case "$host" in
    *railway.internal*) ssl=prefer ;;
    *) ssl=require ;;
  esac
  export SPRING_DATASOURCE_URL="jdbc:postgresql://${host}:${port}/${db}?sslmode=${ssl}"
  export SPRING_DATASOURCE_USERNAME="$user"
  export SPRING_DATASOURCE_PASSWORD="$pass"
  echo "[env] JDBC ready from PGHOST/DATABASE_HOST=$host"
else
  echo "[env] ERROR: No database URL in this container."
  echo "[env] Open Railway → sedsp-api → Variables → Add Reference →"
  echo "[env]   DATABASE_PUBLIC_URL  =  (Postgres service) DATABASE_PUBLIC_URL"
fi

redis=$(printenv REDIS_URL 2>/dev/null || printenv REDIS_PUBLIC_URL 2>/dev/null || true)
if [ -n "$redis" ]; then
  export SPRING_DATA_REDIS_URL="$redis"
  echo "[env] Redis URL set"
fi

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"

JAVA_ARGS="-Dserver.port=${PORT:-8080}"
if [ -n "${SPRING_DATASOURCE_URL:-}" ]; then
  JAVA_ARGS="$JAVA_ARGS -Dspring.datasource.url=${SPRING_DATASOURCE_URL}"
fi
if [ -n "${SPRING_DATASOURCE_USERNAME:-}" ]; then
  JAVA_ARGS="$JAVA_ARGS -Dspring.datasource.username=${SPRING_DATASOURCE_USERNAME}"
fi
if [ -n "${SPRING_DATASOURCE_PASSWORD:-}" ]; then
  JAVA_ARGS="$JAVA_ARGS -Dspring.datasource.password=${SPRING_DATASOURCE_PASSWORD}"
fi

# shellcheck disable=SC2086
exec java ${JAVA_OPTS:-} $JAVA_ARGS -jar /app/app.jar
