#!/bin/sh
# Railway entrypoint: resolve DB URL then start Java (no password on argv).
set -eu

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"

# Prefer non-empty values: explicit JDBC → private network → public proxy
pick=""
pick_name=""
for k in \
  SPRING_DATASOURCE_URL \
  DATABASE_URL \
  DATABASE_PRIVATE_URL \
  POSTGRES_URL \
  POSTGRES_PRIVATE_URL \
  DATABASE_PUBLIC_URL \
  POSTGRES_PUBLIC_URL \
  JDBC_DATABASE_URL
do
  v=$(printenv "$k" 2>/dev/null || true)
  if [ -n "$v" ]; then
    pick="$v"
    pick_name="$k"
    break
  fi
done

if [ -z "$pick" ]; then
  while IFS= read -r line || [ -n "$line" ]; do
    name=${line%%=*}
    val=${line#*=}
    case "$val" in
      postgres://*|postgresql://*|jdbc:postgresql://*)
        case "$name" in
          *REDIS*|*redis*) continue ;;
        esac
        pick="$val"
        pick_name="$name"
        break
        ;;
    esac
  done <<EOF
$(env)
EOF
fi

export_jdbc_from_url() {
  raw="$1"
  case "$raw" in
    jdbc:postgresql://*)
      export SPRING_DATASOURCE_URL="$raw"
      ;;
    postgres://*|postgresql://*)
      export DATABASE_URL="$raw"
      rest="${raw#*://}"
      case "$rest" in
        *@*)
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
          echo "[secdsp] WARN: could not split user@host in database URL" >&2
          ;;
      esac
      ;;
    *)
      echo "[secdsp] WARN: unexpected database URL prefix from $pick_name" >&2
      return 1
      ;;
  esac
  return 0
}

if [ -n "$pick" ]; then
  export_jdbc_from_url "$pick" || true
elif [ -n "$(printenv PGHOST 2>/dev/null || true)$(printenv DATABASE_HOST 2>/dev/null || true)" ]; then
  host=$(printenv PGHOST 2>/dev/null || printenv DATABASE_HOST)
  port=$(printenv PGPORT 2>/dev/null || printenv DATABASE_PORT || true)
  [ -z "${port:-}" ] && port=5432
  db=$(printenv PGDATABASE 2>/dev/null || printenv DATABASE_NAME || true)
  [ -z "${db:-}" ] && db=railway
  user=$(printenv PGUSER 2>/dev/null || printenv DATABASE_USER || true)
  [ -z "${user:-}" ] && user=postgres
  pass=$(printenv PGPASSWORD 2>/dev/null || printenv DATABASE_PASSWORD || true)
  case "$host" in
    *railway.internal*) ssl=prefer ;;
    *) ssl=require ;;
  esac
  export SPRING_DATASOURCE_URL="jdbc:postgresql://${host}:${port}/${db}?sslmode=${ssl}"
  export SPRING_DATASOURCE_USERNAME="$user"
  export SPRING_DATASOURCE_PASSWORD="$pass"
  pick_name="PGHOST"
else
  echo "[secdsp] ERROR: No database URL in this container." >&2
  echo "[secdsp] Railway → Variables → Add Reference DATABASE_PUBLIC_URL = \${{Postgres.DATABASE_PUBLIC_URL}}" >&2
fi

redis=$(printenv REDIS_URL 2>/dev/null || printenv REDIS_PUBLIC_URL 2>/dev/null || true)
if [ -n "${redis:-}" ]; then
  export SPRING_DATA_REDIS_URL="$redis"
fi

echo "[secdsp] boot profile=${SPRING_PROFILES_ACTIVE} port=${PORT:-8080}${pick_name:+ db=${pick_name}}"

# Cap heap explicitly. Railway's /proc/meminfo is the host (~hundreds of GB);
# MaxRAMPercentage without a cgroup limit will OOM-kill the container.
# shellcheck disable=SC2086
exec java ${JAVA_OPTS:--Xms128m -Xmx512m} \
  -XX:+UseContainerSupport \
  -Dserver.port="${PORT:-8080}" \
  -Dserver.address=0.0.0.0 \
  -Duser.timezone=Asia/Ho_Chi_Minh \
  -jar /app/app.jar
