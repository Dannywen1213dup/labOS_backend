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

bootout_launch_agent() {
  local service="$1"
  local plist="$HOME/Library/LaunchAgents/homebrew.mxcl.${service}.plist"
  if [[ -f "$plist" ]]; then
    launchctl bootout "gui/$(id -u)" "$plist" >/dev/null 2>&1 || true
    launchctl bootout "user/$(id -u)" "$plist" >/dev/null 2>&1 || true
  fi
}

bootout_system_daemon() {
  local service="$1"
  local plist="/Library/LaunchDaemons/homebrew.mxcl.${service}.plist"
  if [[ ! -f "$plist" ]]; then
    return 0
  fi

  if [[ "$(id -u)" -eq 0 ]]; then
    launchctl bootout "system" "$plist" >/dev/null 2>&1 || true
  else
    # Non-interactive sudo fallback; if not permitted, skip silently.
    sudo -n launchctl bootout "system" "$plist" >/dev/null 2>&1 || true
  fi
}

kill_patterns_gracefully() {
  local patterns=("$@")
  local p pattern
  for pattern in "${patterns[@]}"; do
    if p="$(pgrep -f "$pattern" || true)" && [[ -n "$p" ]]; then
      echo "[INFO] Sending TERM to: $pattern"
      echo "$p" | xargs kill -TERM >/dev/null 2>&1 || true
    fi
  done
}

kill_patterns_force() {
  local patterns=("$@")
  local p pattern
  for pattern in "${patterns[@]}"; do
    if p="$(pgrep -f "$pattern" || true)" && [[ -n "$p" ]]; then
      echo "[WARN] Forcing KILL on: $pattern"
      echo "$p" | xargs kill -KILL >/dev/null 2>&1 || true
    fi
  done
}

any_process_alive() {
  local patterns=("$@")
  local pattern
  for pattern in "${patterns[@]}"; do
    if pgrep -f "$pattern" >/dev/null 2>&1; then
      return 0
    fi
  done
  return 1
}

require_cmd brew
require_cmd launchctl

MYSQL_SERVICE="${MYSQL_SERVICE:-}"
REDIS_SERVICE="${REDIS_SERVICE:-}"

if [[ -z "$MYSQL_SERVICE" ]]; then
  MYSQL_SERVICE="$(pick_installed_service mysql mysql@8.4 mysql@8.0 mariadb mariadb@10.11 || true)"
fi
if [[ -z "$REDIS_SERVICE" ]]; then
  REDIS_SERVICE="$(pick_installed_service redis redis-stack redis-stack-server || true)"
fi

if [[ -z "$MYSQL_SERVICE" ]]; then
  echo "[ERROR] MySQL service not found via Homebrew."
  exit 1
fi
if [[ -z "$REDIS_SERVICE" ]]; then
  echo "[ERROR] Redis service not found via Homebrew."
  exit 1
fi

echo "[INFO] Stopping services via Homebrew..."
brew services stop "$MYSQL_SERVICE" >/dev/null 2>&1 || true
brew services stop "$REDIS_SERVICE" >/dev/null 2>&1 || true
# Fallback for services that were started with sudo brew services.
sudo -n brew services stop "$MYSQL_SERVICE" >/dev/null 2>&1 || true
sudo -n brew services stop "$REDIS_SERVICE" >/dev/null 2>&1 || true

echo "[INFO] Unloading launchd agents to prevent auto-restart..."
bootout_launch_agent "$MYSQL_SERVICE"
bootout_launch_agent "$REDIS_SERVICE"
bootout_system_daemon "$MYSQL_SERVICE"
bootout_system_daemon "$REDIS_SERVICE"

MYSQL_PATTERNS=("mysqld" "mariadbd")
REDIS_PATTERNS=("redis-server")
ALL_PATTERNS=("${MYSQL_PATTERNS[@]}" "${REDIS_PATTERNS[@]}")

kill_patterns_gracefully "${ALL_PATTERNS[@]}"
sleep 2

if any_process_alive "${ALL_PATTERNS[@]}"; then
  kill_patterns_force "${ALL_PATTERNS[@]}"
fi

echo "[INFO] Running brew services cleanup..."
brew services cleanup >/dev/null 2>&1 || true

MYSQL_STATUS="$(service_status "$MYSQL_SERVICE")"
REDIS_STATUS="$(service_status "$REDIS_SERVICE")"

if [[ "$MYSQL_STATUS" == "started" || "$REDIS_STATUS" == "started" ]]; then
  echo "[ERROR] Service still in started state:"
  brew services list | awk -v m="$MYSQL_SERVICE" -v r="$REDIS_SERVICE" '$1 == m || $1 == r'
  exit 1
fi

if any_process_alive "${ALL_PATTERNS[@]}"; then
  echo "[ERROR] MySQL/Redis process is still alive after stop."
  pgrep -fl 'mysqld|mariadbd|redis-server' || true
  exit 1
fi

echo "[OK] MySQL and Redis are fully stopped."
brew services list | awk -v m="$MYSQL_SERVICE" -v r="$REDIS_SERVICE" '$1 == m || $1 == r'
