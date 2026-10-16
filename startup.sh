#!/usr/bin/env bash
# Once-per-worker prerequisites for serving + capturing this project.
# Idempotent: safe to re-run. Only tool checks and cache warm-up live here.
set -euo pipefail

# NOTE: /usr/bin/time needs real binaries, so shell builtins run via bash -c.
/usr/bin/time -p bash -c 'command -v python3'
/usr/bin/time -p bash -c 'command -v curl'
/usr/bin/time -p bash -c 'test -x /usr/bin/time'
/usr/bin/time -p bash -c 'command -v chromium || command -v chromium-browser || command -v google-chrome'

# Warm the headless-chromium first-run profile cache so later captures are fast.
if [ -z "${CHROME_WARMED:-}" ]; then
  /usr/bin/time -p chromium --headless=new --no-sandbox --disable-gpu --dump-dom about:blank >/dev/null
  export CHROME_WARMED=1
fi
