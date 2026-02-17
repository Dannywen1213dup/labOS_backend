#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"

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
    echo "[INFO] No running Spring process for this project."
    return 0
  fi

  echo "[INFO] Stopping Spring processes: ${pids[*]}"
  kill -TERM "${pids[@]}" >/dev/null 2>&1 || true
  sleep 2

  local alive=()
  for pid in "${pids[@]}"; do
    if kill -0 "$pid" >/dev/null 2>&1; then
      alive+=("$pid")
    fi
  done

  if [[ "${#alive[@]}" -gt 0 ]]; then
    echo "[WARN] Force killing remaining Spring pids: ${alive[*]}"
    kill -KILL "${alive[@]}" >/dev/null 2>&1 || true
  fi
}

cd "$PROJECT_ROOT"

kill_project_processes

if [[ -x "$PROJECT_ROOT/stop_db.sh" ]]; then
  echo "[INFO] Stopping local MySQL and Redis via brew..."
  "$PROJECT_ROOT/stop_db.sh"
else
  echo "[WARN] stop_db.sh not found or not executable, skip DB stop."
fi

echo "[OK] Local Spring + DB services stopped."

