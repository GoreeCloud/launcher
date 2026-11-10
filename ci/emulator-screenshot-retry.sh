#!/usr/bin/env bash
# CI-only disposable-emulator screenshot recovery. Source only after the caller
# has verified the pinned Android emulator; this helper does not admit devices.
# Every attempt has a fresh remote name and locally validated image bytes.
capture_emulator_screenshot_with_retries() {
  local name="$1" output attempt remote
  [[ "$name" =~ ^[a-z0-9-]{1,64}$ ]] || {
    printf 'Invalid emulator screenshot label\n' >&2
    return 2
  }
  [[ -n "${EVIDENCE:-}" ]] || return 2
  output="$EVIDENCE/$name.png"
  GC_SCREENSHOT_CAPTURE_ID=$(("${GC_SCREENSHOT_CAPTURE_ID:-0}" + 1))
  for attempt in 1 2 3 4; do
    remote="/sdcard/gc-$name-$GC_SCREENSHOT_CAPTURE_ID-$attempt.png"
    rm -f -- "$output"
    if adb shell screencap -p "$remote" \
        && adb pull "$remote" "$output" >/dev/null \
        && python3 - "$output" <<'PY'
from pathlib import Path
import struct
import sys

path = Path(sys.argv[1])
try:
    with path.open("rb") as fd:
        header = fd.read(24)
        size = fd.seek(0, 2)
    if size < 33 or size > 16 * 1024 * 1024:
        raise ValueError("image size outside QA bounds")
    if header[:8] != b"\x89PNG\r\n\x1a\n" or header[12:16] != b"IHDR":
        raise ValueError("invalid PNG signature or IHDR")
    width, height = struct.unpack(">II", header[16:24])
    if not (0 < width <= 8192 and 0 < height <= 8192):
        raise ValueError("invalid screenshot dimensions")
except (OSError, ValueError, struct.error):
    raise SystemExit(1)
PY
    then
      printf 'PASS fresh emulator screenshot after %d attempt(s): %s\n' "$attempt" "$name"
      return 0
    fi
    if [[ "$attempt" != 4 ]]; then
      printf 'Transient emulator screenshot failure (attempt %d of 4)\n' "$attempt" >&2
      sleep 2
    fi
  done
  rm -f -- "$output"
  printf 'FAIL no verified emulator screenshot after four attempts: %s\n' "$name" >&2
  return 1
}
