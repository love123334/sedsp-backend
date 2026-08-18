#!/bin/sh
# Railway entrypoint: diagnose DB env, prefer PUBLIC URL, then start Java.
# Do NOT pass JDBC URL on the java command line (passwords/&/? break shell).
set -eu

echo "==== SEDSP BOOT ===="
echo "PORT=${PORT:-unset} SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-unset}"

diag_key() {
  k="$1"
  if ! printenv "$k" >/dev/null 2>&1; then
    echo "[env] $k=UNSET"
    return
  fi
  v=$(printenv "$k")
  if [ -z "$v" ]; then
    echo "[env] $k=EMPTY (reference private URL rỗng — thêm DATABASE_PUBLIC_URL)"
  else
    echo "[env] $k=SET len=${#v}"
  fi
}

for k in \
  DATABASE_URL DATABASE_PUBLIC_URL DATABASE_PRIVATE_URL \
  POSTGRES_URL POSTGRES_PUBLIC_URL \
  PGHOST PGPORT PGUSER PGDATABASE PGPASSWORD \
  DATABASE_HOST DATABASE_PORT DATABASE_USER DATABASE_NAME \
  REDIS_URL REDIS_PUBLIC_URL SPRING_DATASOURCE_URL
do
  diag_key "$k"
done

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
    echo "[env] selected $k"
    break
  fi
done

# Scan entire environment for a postgres URL under any key name
if [ -z "$pick" ]; then
  echo "[env] scanning process environment for postgres:// URLs…"
  # env -0 is not portable; line-based is enough for Railway
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
        echo "[env] scan hit $name"
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
      # Leave postgres:// for Spring EnvironmentPostProcessor (handles encoding).
      # Also set a jdbc form when parsing is simple enough.
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
          echo "[env] WARN: could not split user@host in URL"
          ;;
      esac
      ;;
    *)
      echo "[env] WARN: unexpected DB URL prefix from $pick_name"
      return 1
      ;;
  esac
  return 0
}

if [ -n "$pick" ]; then
  if export_jdbc_from_url "$pick"; then
    echo "[env] JDBC ready from $pick_name"
  fi
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
  echo "[env] JDBC ready from PGHOST/DATABASE_HOST=$host"
else
  echo "[env] ERROR: No database URL in this container."
  echo "[env] Railway → sedsp-api → Variables → New Variable → Add Reference:"
  echo "[env]   Name: DATABASE_PUBLIC_URL"
  echo '[env]   Value: ${{Postgres.DATABASE_PUBLIC_URL}}  (đổi Postgres = tên service DB)'
  echo "[env] Private DATABASE_URL often EMPTY → healthcheck fail."
fi

redis=$(printenv REDIS_URL 2>/dev/null || printenv REDIS_PUBLIC_URL 2>/dev/null || true)
if [ -n "${redis:-}" ]; then
  export SPRING_DATA_REDIS_URL="$redis"
  echo "[env] Redis URL set"
fi

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"

echo "[env] binding server.port=${PORT:-8080} (0.0.0.0)"
if [ -f /proc/meminfo ]; then
  echo "[env] $(awk '/MemAvailable|MemTotal/ {printf "%s=%s kB ", $1, $2}' /proc/meminfo)"
fi
# Strip heap sizes from JAVA_OPTS — -Xmx512m OOMs a 512Mi Railway container
# once the fat JAR includes Gemini/ONNX. Container RAM percentage is safer.
JAVA_OPTS_SAFE=$(printf '%s' "${JAVA_OPTS:-}" | sed 's/-Xms[^ ]*//g; s/-Xmx[^ ]*//g')
# Only safe JVM flags here — datasource comes from env / EnvironmentPostProcessor
# shellcheck disable=SC2086
exec java ${JAVA_OPTS_SAFE} \
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage="${JAVA_MAX_RAM_PERCENTAGE:-65.0}" \
  -Dserver.port="${PORT:-8080}" \
  -Dserver.address=0.0.0.0 \
  -Duser.timezone=Asia/Ho_Chi_Minh \
  -jar /app/app.jar
