#!/usr/bin/env bash
# Source after dump_screen is defined by the disposable Home-role QA driver.
# Find both real Settings controls in the SAME accessibility snapshot. A single
# unverified swipe can stop at the app dock instead of the Home-role section.
role_controls_visible() {
  python3 - "$EVIDENCE/$1.xml" "$2" "$3" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
path, status, action = sys.argv[1:4]
try:
    root = ET.parse(path).getroot()
except (ET.ParseError, OSError):
    raise SystemExit(1)
if root.tag != "hierarchy":
    raise SystemExit(1)
visible = set()
for node in root.iter():
    a = node.attrib
    text = a.get("text") or a.get("content-desc", "")
    if text not in (status, action):
        continue
    m = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", a.get("bounds", ""))
    if not m:
        continue
    x1, y1, x2, y2 = map(int, m.groups())
    if x2 > x1 and y2 > y1:
        visible.add(text)
raise SystemExit(0 if {status, action} <= visible else 1)
PY
}

find_settings_role_controls() {
  local snapshot="$1" status="$2" action="$3" attempt
  for attempt in 1 2 3 4 5 6 7 8; do
    dump_screen "$snapshot"
    if role_controls_visible "$snapshot" "$status" "$action"; then
      printf 'PASS Home-role Settings controls visible (%s; attempt %d)\n' "$snapshot" "$attempt"
      return 0
    fi
    if [[ "$attempt" -lt 8 ]]; then
      # Only the already verified disposable emulator receives this gesture.
      adb shell input swipe 445 1690 445 580 520
      sleep 1
    fi
  done
  printf 'Home-role Settings controls not both visible after bounded scrolling: %s\n' "$snapshot" >&2
  return 1
}
