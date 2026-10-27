#!/usr/bin/env bash
# Records anonymised MA API responses into core/ma-api/src/test/resources/fixtures/.
# Token never goes through chat: create a long-lived token in MA (Settings > Profile) or via auth/login,
# then:  MA_TOKEN=... bash scripts/record-fixtures.sh   (optional: MA_SERVER=http://host:8095 MA_SEARCH=beatles)
cd "$(dirname "$0")/.." || exit 1
: "${MA_TOKEN:?set MA_TOKEN in the environment}"
command -v node >/dev/null || { echo "Node 22+ required"; exit 1; }
exec node scripts/record-fixtures.mjs core/ma-api/src/test/resources/fixtures
