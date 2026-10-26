#!/usr/bin/env bash
# Disposable emulator CI helper: accept only a freshly captured valid UI tree.
capture_hierarchy_with_retries() {
  local snapshot="$1" output="$2" attempt remote
  for attempt in 1 2 3; do
    remote="/sdcard/gc-${snapshot}-${attempt}.xml"
    if adb shell uiautomator dump "$remote" \
        && adb pull "$remote" "$output" >/dev/null \
        && python3 - "$output" <<'PY'
import sys
import xml.etree.ElementTree as ET
try:
    root = ET.parse(sys.argv[1]).getroot()
    assert root.tag == "hierarchy"
    assert any(n.tag == "node" for n in root.iter())
except (OSError, ET.ParseError, AssertionError):
    sys.exit(1)
PY
    then
      echo "PASS fresh UiAutomator hierarchy after $attempt attempts"
      return 0
    fi
    echo "UiAutomator hierarchy unavailable (attempt $attempt of 3)" >&2
    sleep 2
  done
  echo "FAIL no valid UiAutomator hierarchy after 3 attempts" >&2
  return 1
}
