#!/usr/bin/env bash
# No-device regression for fresh, bounded screenshot capture on ephemeral CI AVD.
set -Eeuo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
EVIDENCE="$(mktemp -d)"
trap 'rm -rf "$EVIDENCE"' EXIT
scenario=transient
screens=0
pulls=0
remotes=()
adb() {
  if [[ "$1" == shell && "$2" == screencap && "$3" == -p && "$4" == /sdcard/gc-* ]]; then
    screens=$((screens + 1))
    remotes+=("$4")
    if [[ "$scenario" == never || ( "$scenario" == transient && "$screens" -lt 3 ) ]]; then
      return 1
    fi
    return 0
  fi
  if [[ "$1" == pull && "$2" == /sdcard/gc-* && "$#" -eq 3 ]]; then
    pulls=$((pulls + 1))
    python3 - "$3" "$scenario" <<'PY'
from pathlib import Path
import struct
import sys
valid = b"\x89PNG\r\n\x1a\n" + b"\x00\x00\x00\x0dIHDR" + struct.pack(">II", 1080, 2400) + b"\x08\x06\x00\x00\x00" + b"\x00\x00\x00\x00"
Path(sys.argv[1]).write_bytes(valid if sys.argv[2] != "bad_png" else b"not-an-image")
PY
    return 0
  fi
  echo "Unexpected synthetic ADB command: $*" >&2
  return 99
}
sleep() { :; }
source ci/emulator-screenshot-retry.sh

capture_emulator_screenshot_with_retries availability-recovery
[[ "$screens" == 3 && "$pulls" == 1 ]] || {
  echo "Expected two failures and one fresh screenshot" >&2; exit 1;
}
[[ "${remotes[0]}" != "${remotes[1]}" && "${remotes[1]}" != "${remotes[2]}" ]] || {
  echo "Retried remote path was stale" >&2; exit 1;
}
[[ -s "$EVIDENCE/availability-recovery.png" ]] || exit 1

scenario=never; screens=0; pulls=0
if capture_emulator_screenshot_with_retries availability-recovery; then
  echo "Repeated offline ADB incorrectly accepted" >&2; exit 1
fi
[[ "$screens" == 4 && "$pulls" == 0 ]] || exit 1
[[ ! -e "$EVIDENCE/availability-recovery.png" ]] || {
  echo "Stale screenshot was reused after failure" >&2; exit 1;
}

scenario=bad_png; screens=0; pulls=0
if capture_emulator_screenshot_with_retries corrupted; then
  echo "Invalid screenshot image was accepted" >&2; exit 1
fi
[[ "$screens" == 4 && "$pulls" == 4 ]] || exit 1
[[ ! -e "$EVIDENCE/corrupted.png" ]] || exit 1
if capture_emulator_screenshot_with_retries "../escape"; then
  echo "Screenshot path traversal accepted" >&2; exit 1
fi
echo "PASS: fresh emulator screenshots, bounded offline retries, no stale or malformed images"
