#!/usr/bin/env bash
set -euo pipefail

require_cmd() {
  local cmd="$1"
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "[ERROR] Missing command: $cmd"
    exit 1
  fi
}

pick_installed_service() {
  local candidates=("$@")
  local name
  for name in "${candidates[@]}"; do
    if brew list --versions "$name" >/dev/null 2>&1; then
      echo "$name"
      return 0
    fi
  done
  return 1
}

service_status() {
  local service="$1"
  brew services list | awk -v s="$service" '$1 == s {print $2; exit}'
}

bootout_user_agent() {
  local service="$1"
  local uid plist
  uid="$(id -u)"
  plist="$HOME/Library/LaunchAgents/homebrew.mxcl.${service}.plist"

  if [[ -f "$plist" ]] && command -v launchctl >/dev/null 2>&1; then
    launchctl bootout "gui/${uid}" "$plist" >/dev/null 2>&1 || true
    launchctl bootout "user/${uid}" "$plist" >/dev/null 2>&1 || true
  fi
}

stop_service_forcefully() {
  local service="$1"

  brew services stop "$service" >/dev/null 2>&1 || true
  sudo -n brew services stop "$service" >/dev/null 2>&1 || true
  bootout_user_agent "$service"

  case "$service" in
    mysql*|mariadb*)
      pkill -TERM -f 'mysqld|mariadbd' >/dev/null 2>&1 || true
      sleep 1
      pkill -KILL -f 'mysqld|mariadbd' >/dev/null 2>&1 || true
      ;;
    redis*)
      pkill -TERM -f 'redis-server' >/dev/null 2>&1 || true
      sleep 1
      pkill -KILL -f 'redis-server' >/dev/null 2>&1 || true
      ;;
  esac

  brew services cleanup >/dev/null 2>&1 || true
}

service_process_alive() {
  local service="$1"
  case "$service" in
    mysql*|mariadb*)
      pgrep -f 'mysqld|mariadbd' >/dev/null 2>&1
      ;;
    redis*)
      pgrep -f 'redis-server' >/dev/null 2>&1
      ;;
    *)
      return 1
      ;;
  esac
}

wait_for_started() {
  local service="$1"
  local max_wait="${2:-20}"
  local i status
  for ((i = 1; i <= max_wait; i++)); do
    status="$(service_status "$service")"
    if [[ "$status" == "started" ]]; then
      return 0
    fi
    sleep 1
  done
  return 1
}

ensure_service_started() {
  local service="$1"
  local label="$2"
  local status output

  status="$(service_status "$service")"
  if [[ "$status" == "error" ]]; then
    echo "[WARN] $label service state is 'error'. Trying to repair launchd state..."
    stop_service_forcefully "$service"
    status="$(service_status "$service")"
  fi

  if [[ "$status" == "started" ]] || service_process_alive "$service"; then
    echo "[INFO] $label already running, skip start: $service"
    return 0
  fi

  echo "[INFO] Starting $label service: $service"
  if output="$(brew services start "$service" 2>&1)"; then
    :
  else
    echo "[WARN] brew services start returned non-zero for $service"
    echo "$output"
  fi

  if wait_for_started "$service" 20 || service_process_alive "$service"; then
    echo "[INFO] $label is running: $service"
    return 0
  fi

  echo "[ERROR] Failed to start $label service: $service"
  brew services list | awk -v s="$service" '$1 == s'
  return 1
}

require_cmd brew

MYSQL_SERVICE="${MYSQL_SERVICE:-}"
REDIS_SERVICE="${REDIS_SERVICE:-}"

if [[ -z "$MYSQL_SERVICE" ]]; then
  MYSQL_SERVICE="$(pick_installed_service mysql mysql@8.4 mysql@8.0 mariadb mariadb@10.11 || true)"
fi
if [[ -z "$REDIS_SERVICE" ]]; then
  REDIS_SERVICE="$(pick_installed_service redis redis-stack redis-stack-server || true)"
fi

if [[ -z "$MYSQL_SERVICE" ]]; then
  echo "[ERROR] MySQL service not found via Homebrew (tried: mysql mysql@8.4 mysql@8.0 mariadb mariadb@10.11)"
  exit 1
fi
if [[ -z "$REDIS_SERVICE" ]]; then
  echo "[ERROR] Redis service not found via Homebrew (tried: redis redis-stack redis-stack-server)"
  exit 1
fi

ensure_service_started "$MYSQL_SERVICE" "MySQL"
ensure_service_started "$REDIS_SERVICE" "Redis"

echo "[OK] Services running."
brew services list | awk -v m="$MYSQL_SERVICE" -v r="$REDIS_SERVICE" '$1 == m || $1 == r'
