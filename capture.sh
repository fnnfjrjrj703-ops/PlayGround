#!/usr/bin/env bash
# Capture desktop + mobile screenshots of CAPTURE_URL into CAPTURE_DIR.
# Exit 75: temporary navigation / browser infrastructure failure.
# Exit 1:  script misuse or rendering defect.
set -euo pipefail

# --- script inputs (missing inputs are a script defect) ---
if [[ -z "${CAPTURE_URL:-}" || -z "${CAPTURE_DIR:-}" ]]; then
  echo "CAPTURE_URL and CAPTURE_DIR must be set." >&2
  exit 1
fi
URL="$CAPTURE_URL"
OUT="$CAPTURE_DIR"

/usr/bin/time -p mkdir -p "$OUT"

CHROME="$(command -v chromium || command -v chromium-browser || command -v google-chrome || true)"
if [[ -z "$CHROME" ]]; then
  echo "No chromium browser binary found (infrastructure)." >&2
  exit 75
fi

# --- reachability precheck: classify transient vs permanent failures ---
/usr/bin/time -p curl --silent --show-error --location --max-time 25 \
  --output /dev/null --write-out 'http:%{http_code}\n' "$URL" > "$OUT/http-status.txt" || curl_rc=$?
curl_rc="${curl_rc:-0}"
/usr/bin/time -p cat "$OUT/http-status.txt"
http_code="$(sed 's/^http://' "$OUT/http-status.txt" || true)"
if [[ "${curl_rc}" -ne 0 ]]; then
  # 6=resolve, 7=connect, 28=timeout: temporary infrastructure problems
  echo "curl failed (rc=$curl_rc): temporary infrastructure failure." >&2
  exit 75
fi
case "$http_code" in
  2*)
    /usr/bin/time -p echo "Preview reachable: HTTP $http_code" ;;
  408|429|500|502|503|504)
    echo "Preview temporarily unavailable: HTTP $http_code." >&2
    exit 75 ;;
  *)
    echo "Preview rejected the request: HTTP $http_code." >&2
    exit 1 ;;
esac

MOBILE_UA="Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

shot() { # name width height [extra chromium args...]
  local name="$1" width="$2" height="$3"
  shift 3
  local profile="$OUT/.chrome-$name"
  /usr/bin/time -p rm -rf "$profile"
  if /usr/bin/time -p "$CHROME" --headless=new --no-sandbox --disable-gpu \
      --hide-scrollbars --virtual-time-budget=8000 \
      --window-size="$width,$height" --user-data-dir="$profile" \
      "$@" --screenshot="$OUT/final-$name.png" "$URL"; then
    /usr/bin/time -p echo "captured final-$name.png"
  else
    echo "Chromium failed for $name (infrastructure)." >&2
    return 75
  fi
}

/usr/bin/time -p echo "Capturing desktop view…"
shot desktop 1440 900 || exit 75
/usr/bin/time -p echo "Capturing mobile view…"
shot mobile 390 844 --user-agent="$MOBILE_UA" || exit 75

# --- validate rendered output (defects here are rendering failures) ---
for name in desktop mobile; do
  f="$OUT/final-$name.png"
  /usr/bin/time -p test -f "$f" || { echo "Missing $f." >&2; exit 1; }
  /usr/bin/time -p file "$f"
  if ! /usr/bin/time -p python3 - "$f" <<'EOF'
import struct, sys
p = sys.argv[1]
with open(p, "rb") as fh:
    head = fh.read(33)
assert len(head) >= 33 and head[:8] == b"\x89PNG\r\n\x1a\n", "not a PNG"
w, h = struct.unpack(">II", head[16:24])
print(f"{p}: PNG {w}x{h}")
assert w >= 300 and h >= 500, "unexpected dimensions"
import os
assert os.path.getsize(p) > 10240, "suspiciously small file"
EOF
  then
    echo "Invalid screenshot: $f." >&2
    exit 1
  fi
done

/usr/bin/time -p ls -lh "$OUT/final-desktop.png" "$OUT/final-mobile.png"
/usr/bin/time -p echo "Capture complete; app server left running."
