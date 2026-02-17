#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"
JAR_PATH="${JAR_PATH:-$PROJECT_ROOT/target/springboot-init-0.0.1-SNAPSHOT.jar}"
MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_DB="${MYSQL_DB:-my_db}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-123456}"
REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
REDIS_PORT="${REDIS_PORT:-6379}"
REDIS_PASSWORD="${REDIS_PASSWORD:-}"
AUTO_BUILD="${AUTO_BUILD:-1}"
AUTO_INIT_SCHEMA="${AUTO_INIT_SCHEMA:-1}"

require_cmd() {
  local cmd="$1"
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "[ERROR] Missing command: $cmd"
    exit 1
  fi
}

wait_for_tcp_port() {
  local host="$1"
  local port="$2"
  local label="$3"
  local max_wait="${4:-20}"
  local i

  for ((i = 1; i <= max_wait; i++)); do
    if command -v nc >/dev/null 2>&1 && nc -z "$host" "$port" >/dev/null 2>&1; then
      return 0
    fi
    if command -v lsof >/dev/null 2>&1 && lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done

  echo "[ERROR] $label TCP port not ready: ${host}:${port}"
  return 1
}

check_mysql_connectivity() {
  if ! command -v mysql >/dev/null 2>&1; then
    echo "[WARN] mysql client not found, skip SQL-level connectivity check."
    return 0
  fi

  if mysql \
    --protocol=tcp \
    -h"$MYSQL_HOST" \
    -P"$MYSQL_PORT" \
    -u"$MYSQL_USER" \
    -p"$MYSQL_PASSWORD" \
    --connect-timeout=5 \
    -e "SELECT 1;" >/dev/null 2>&1; then
    return 0
  fi

  echo "[ERROR] MySQL login check failed for ${MYSQL_USER}@${MYSQL_HOST}:${MYSQL_PORT}"
  echo "[INFO] You can test manually:"
  echo "       mysql --protocol=tcp -h$MYSQL_HOST -P$MYSQL_PORT -u$MYSQL_USER -p"
  return 1
}

ensure_mysql_database_and_schema() {
  local mysql_base_cmd=(
    mysql
    --protocol=tcp
    -h"$MYSQL_HOST"
    -P"$MYSQL_PORT"
    -u"$MYSQL_USER"
    -p"$MYSQL_PASSWORD"
    --connect-timeout=5
  )
  local schema_sql="$PROJECT_ROOT/sql/create_table.sql"
  local user_table_count tmp_sql

  if ! command -v mysql >/dev/null 2>&1; then
    echo "[WARN] mysql client not found, skip DB/schema auto-init."
    return 0
  fi

  echo "[INFO] Ensuring database exists: $MYSQL_DB"
  "${mysql_base_cmd[@]}" -e "CREATE DATABASE IF NOT EXISTS \`$MYSQL_DB\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" >/dev/null

  if [[ "$AUTO_INIT_SCHEMA" != "1" ]]; then
    echo "[INFO] AUTO_INIT_SCHEMA=0, skip schema initialization."
    return 0
  fi

  user_table_count="$("${mysql_base_cmd[@]}" -Nse \
    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${MYSQL_DB}' AND table_name='user';" 2>/dev/null || echo "0")"

  if [[ "$user_table_count" != "0" ]]; then
    echo "[INFO] Schema already initialized in database: $MYSQL_DB"
    return 0
  fi

  if [[ ! -f "$schema_sql" ]]; then
    echo "[WARN] Schema file not found, skip table initialization: $schema_sql"
    return 0
  fi

  echo "[INFO] Initializing schema from: $schema_sql"
  if [[ "$MYSQL_DB" == "my_db" ]]; then
    "${mysql_base_cmd[@]}" < "$schema_sql"
  else
    tmp_sql="$(mktemp)"
    sed "s/\\bmy_db\\b/${MYSQL_DB}/g" "$schema_sql" > "$tmp_sql"
    "${mysql_base_cmd[@]}" < "$tmp_sql"
    rm -f "$tmp_sql"
  fi
}

check_redis_connectivity() {
  if ! command -v redis-cli >/dev/null 2>&1; then
    echo "[WARN] redis-cli not found, skip Redis PING check."
    return 0
  fi

  local pong
  if [[ -n "$REDIS_PASSWORD" ]]; then
    pong="$(redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -a "$REDIS_PASSWORD" --no-auth-warning ping 2>/dev/null || true)"
  else
    pong="$(redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" ping 2>/dev/null || true)"
  fi
  if [[ "$pong" == "PONG" ]]; then
    return 0
  fi

  echo "[ERROR] Redis PING failed on ${REDIS_HOST}:${REDIS_PORT}"
  return 1
}

kill_project_processes() {
  local pids=()
  local pid

  while IFS= read -r pid; do
    [[ -z "$pid" ]] && continue
    [[ "$pid" == "$$" || "$pid" == "$PPID" ]] && continue
    pids+=("$pid")
  done < <(
    ps -eo pid=,command= | awk -v root="$PROJECT_ROOT" '
      index($0, root) > 0 &&
      ($0 ~ /java/ || $0 ~ /mvnw/ || $0 ~ /spring-boot:run/ || $0 ~ /springboot-init-0\.0\.1-SNAPSHOT\.jar/ || $0 ~ /MainApplication/) {
        print $1
      }'
  )

  if [[ "${#pids[@]}" -eq 0 ]]; then
    echo "[INFO] No old Spring process for this project."
    return 0
  fi

  echo "[INFO] Killing old project processes: ${pids[*]}"
  kill -TERM "${pids[@]}" >/dev/null 2>&1 || true
  sleep 2

  local alive=()
  for pid in "${pids[@]}"; do
    if kill -0 "$pid" >/dev/null 2>&1; then
      alive+=("$pid")
    fi
  done

  if [[ "${#alive[@]}" -gt 0 ]]; then
    echo "[WARN] Force killing remaining pids: ${alive[*]}"
    kill -KILL "${alive[@]}" >/dev/null 2>&1 || true
  fi
}

require_cmd java
require_cmd ps
require_cmd awk

cd "$PROJECT_ROOT"

if [[ ! -x "$PROJECT_ROOT/start_db.sh" ]]; then
  echo "[ERROR] start_db.sh not found or not executable: $PROJECT_ROOT/start_db.sh"
  exit 1
fi

kill_project_processes

echo "[INFO] Starting local MySQL and Redis via brew..."
"$PROJECT_ROOT/start_db.sh"

wait_for_tcp_port "$MYSQL_HOST" "$MYSQL_PORT" "MySQL" 20
wait_for_tcp_port "$REDIS_HOST" "$REDIS_PORT" "Redis" 20
check_mysql_connectivity
ensure_mysql_database_and_schema
check_redis_connectivity

if [[ ! -f "$JAR_PATH" ]]; then
  echo "[INFO] Jar not found, building..."
  ./mvnw -DskipTests package
fi

if [[ "$AUTO_BUILD" == "1" ]]; then
  echo "[INFO] Rebuilding jar to ensure latest dev config is included..."
  ./mvnw -DskipTests package
fi

JDBC_URL="jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}/${MYSQL_DB}?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
REDIS_URL="redis://${REDIS_HOST}:${REDIS_PORT}"

echo "[INFO] Effective dev runtime config:"
echo "       MySQL: ${MYSQL_HOST}:${MYSQL_PORT}/${MYSQL_DB} (user=${MYSQL_USER})"
echo "       Redis: ${REDIS_URL}"

echo "[INFO] Starting Spring Boot in dev profile (foreground, logs in current terminal)..."
if [[ -n "$REDIS_PASSWORD" ]]; then
  exec java -jar "$JAR_PATH" \
    --spring.profiles.active=dev \
    --spring.datasource.url="$JDBC_URL" \
    --spring.datasource.username="$MYSQL_USER" \
    --spring.datasource.password="$MYSQL_PASSWORD" \
    --spring.redis.url="$REDIS_URL" \
    --spring.redis.host="$REDIS_HOST" \
    --spring.redis.port="$REDIS_PORT" \
    --spring.redis.password="$REDIS_PASSWORD" \
    --spring.session.store-type=none
else
  exec java -jar "$JAR_PATH" \
    --spring.profiles.active=dev \
    --spring.datasource.url="$JDBC_URL" \
    --spring.datasource.username="$MYSQL_USER" \
    --spring.datasource.password="$MYSQL_PASSWORD" \
    --spring.redis.url="$REDIS_URL" \
    --spring.redis.host="$REDIS_HOST" \
    --spring.redis.port="$REDIS_PORT" \
    --spring.session.store-type=none
fi
