#!/usr/bin/env bash
# Build + unit tests, printing only errors and the final result (keeps Claude sessions cheap).
# Usage: bash scripts/check.sh [extra gradle tasks]
cd "$(dirname "$0")/.." || exit 1
log=$(mktemp)
./gradlew assembleDebug testDebugUnitTest "$@" --console=plain >"$log" 2>&1
code=$?
grep -E "^e: |^w: .*(deprecated|unused)|error:|FAILED|What went wrong|BUILD (SUCCESSFUL|FAILED)" -A3 "$log" | head -60
[ $code -ne 0 ] && echo "Full log: $log"
exit $code
