#!/usr/bin/env bash
# One emulator, many parallel sessions: take the lock before installing/testing on emulator-5554.
# usage: bash scripts/emu-lock.sh acquire <SESSION_ID>   # waits up to 30 min, then fails
#        bash scripts/emu-lock.sh release <SESSION_ID>
#        bash scripts/emu-lock.sh status
# The lock is a directory (mkdir is atomic). A lock older than 60 min is treated as stale.
set -euo pipefail
LOCK="${SPINROOM_EMU_LOCK:-C:/Users/lukas/AppData/Local/Temp/spinroom-emu.lock}"
cmd="${1:-status}"; id="${2:-unknown}"
age_min() { echo $(( ( $(date +%s) - $(date -r "$LOCK" +%s) ) / 60 )); }
case "$cmd" in
  acquire)
    for _ in $(seq 1 180); do
      if mkdir "$LOCK" 2>/dev/null; then echo "$id" > "$LOCK/owner"; echo "locked by $id"; exit 0; fi
      if [ -d "$LOCK" ] && [ "$(age_min)" -ge 60 ]; then echo "stale lock ($(cat "$LOCK/owner" 2>/dev/null)), removing"; rm -rf "$LOCK"; continue; fi
      sleep 10
    done
    echo "emulator busy: $(cat "$LOCK/owner" 2>/dev/null)" >&2; exit 1 ;;
  release)
    if [ -d "$LOCK" ] && [ "$(cat "$LOCK/owner" 2>/dev/null)" = "$id" ]; then rm -rf "$LOCK"; echo "released"; else echo "not owner, nothing to release"; fi ;;
  status)
    if [ -d "$LOCK" ]; then echo "busy: $(cat "$LOCK/owner" 2>/dev/null), $(age_min) min"; else echo "free"; fi ;;
  *) echo "usage: $0 acquire|release|status <SESSION_ID>" >&2; exit 2 ;;
esac
