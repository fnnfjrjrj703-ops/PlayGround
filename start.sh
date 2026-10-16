#!/usr/bin/env bash
# Karnet preview server: serve the built static directory in the foreground.
# Writes worker metadata (deployment-output.json) under OPENCODE_WEB_DIR only;
# project source and built output stay inside the project directory.
set -euo pipefail

# NOTE: cd is a shell builtin (/usr/bin/time needs real binaries), so it is untimed.
cd "$(dirname "$0")"
/usr/bin/time -p test -f dist/index.html

# Install dependencies and build only when the project needs it.
if /usr/bin/time -p test -f package.json; then
  /usr/bin/time -p npm install --no-audit --no-fund
  /usr/bin/time -p npm run build
  /usr/bin/time -p test -f dist/index.html
fi

: "${OPENCODE_WEB_DIR:=/home/runner/work/_temp/omgithub-web}"
/usr/bin/time -p mkdir -p "$OPENCODE_WEB_DIR"
/usr/bin/time -p python3 - "$OPENCODE_WEB_DIR/deployment-output.json" "$(pwd)/dist" "$(pwd)" <<'EOF'
import json, sys
out, directory, project = sys.argv[1], sys.argv[2], sys.argv[3]
with open(out, "w", encoding="utf-8") as f:
    json.dump({"project": project, "directory": directory}, f)
print("wrote " + out)
EOF

PORT="${PORT:-3000}"
/usr/bin/time -p echo "Serving $(pwd)/dist on port $PORT"
/usr/bin/time -p python3 -m http.server "$PORT" --directory dist --bind 0.0.0.0
