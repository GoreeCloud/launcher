#!/usr/bin/env bash
set -Eeuo pipefail
# Operate only on disposable GitHub-hosted emulator fixture packages.
EVIDENCE="build/emulator-evidence"
mkdir -p "$EVIDENCE"
trap 'adb shell dumpsys activity activities > "$EVIDENCE/icon-activity-on-error.txt" || true' ERR
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r /tmp/goree-fixture-red.apk
adb shell cmd role add-role-holder --user 0 android.app.role.HOME com.goreecloud.launcher
adb shell input keyevent KEYCODE_HOME
sleep 4

dump_screen() {
  local name="$1" xml="$EVIDENCE/$1.xml" png="$EVIDENCE/$1.png"
  for attempt in 1 2 3 4 5 6 7 8; do
    rm -f "$xml" "$png"
    if adb get-state >/dev/null 2>&1 \
      && adb shell rm -f /sdcard/gc-icon-hierarchy.xml /sdcard/gc-icon-screen.png >/dev/null 2>&1 \
      && adb shell uiautomator dump /sdcard/gc-icon-hierarchy.xml >/dev/null 2>&1 \
      && adb pull /sdcard/gc-icon-hierarchy.xml "$xml" >/dev/null 2>&1 \
      && adb shell screencap -p /sdcard/gc-icon-screen.png >/dev/null 2>&1 \
      && adb pull /sdcard/gc-icon-screen.png "$png" >/dev/null 2>&1 \
      && python3 - "$xml" "$png" <<'PY'
import sys,xml.etree.ElementTree as ET
from pathlib import Path
try:
    assert ET.parse(sys.argv[1]).getroot().tag == "hierarchy"
    assert Path(sys.argv[2]).read_bytes()[:8] == b"\x89PNG\r\n\x1a\n"
except (OSError, ET.ParseError, AssertionError):
    raise SystemExit(1)
PY
    then return 0; fi
    echo "Retry icon QA emulator capture $name ($attempt)"
    sleep 2
  done
  echo "Icon QA snapshot unavailable: $name" >&2
  return 1
}
act_on() {
  python3 - "$EVIDENCE/$1.xml" "$2" "$3" <<'PY'
import re,subprocess,sys,xml.etree.ElementTree as ET
target,mode=sys.argv[2:4]
nodes=list(ET.parse(sys.argv[1]).getroot().iter())
matches=[n.attrib for n in nodes if n.attrib.get("text")==target or n.attrib.get("content-desc")==target]
matches.sort(key=lambda a: a.get("clickable")!="true")
for a in matches:
    m=re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",a.get("bounds",""))
    if not m: continue
    x1,y1,x2,y2=map(int,m.groups())
    if x2<=x1 or y2<=y1: continue
    x,y=(x1+x2)//2,(y1+y2)//2
    args=["swipe",str(x),str(y),str(x),str(y),"1000"] if mode=="long" else ["tap",str(x),str(y)]
    subprocess.run(["adb","shell","input"]+args,check=True)
    print("PASS",mode,target)
    sys.exit(0)
raise SystemExit("Missing icon QA action: "+target)
PY
}
# Create a stable Home shortcut to observe one app icon before and after update.
dump_screen icon-home
act_on icon-home "All apps" tap
for attempt in 1 2 3 4 5; do
  sleep 2
  dump_screen icon-all-apps
  if grep -Fq 'content-desc="All apps tab, selected"' "$EVIDENCE/icon-all-apps.xml" \
     && grep -Fq 'content-desc="Launcher QA Fixture"' "$EVIDENCE/icon-all-apps.xml"; then
    break
  fi
  echo "Waiting for fixture in All apps ($attempt)"
done
grep -Fq 'content-desc="Launcher QA Fixture"' "$EVIDENCE/icon-all-apps.xml"
for attempt in 1 2 3 4; do
  dump_screen icon-before-menu
  act_on icon-before-menu "Launcher QA Fixture" long
  sleep 2
  dump_screen icon-shortcut-menu
  if grep -Fq 'text="Add to current home page"' "$EVIDENCE/icon-shortcut-menu.xml"; then break; fi
done
grep -Fq 'text="Add to current home page"' "$EVIDENCE/icon-shortcut-menu.xml"
act_on icon-shortcut-menu "Add to current home page" tap
sleep 1
dump_screen icon-after-placement
act_on icon-after-placement "Home" tap
sleep 2
dump_screen icon-before-upgrade
python3 ci/check-fixture-icon.py "$EVIDENCE/icon-before-upgrade.xml" "$EVIDENCE/icon-before-upgrade.png" red
pid_before="$(adb shell pidof com.goreecloud.launcher | tr -d '\r')"
test -n "$pid_before" || { echo "Launcher process missing before fixture update"; exit 1; }

# Replace ONLY the disposable QA fixture package; label, activity component
# identifiers, and launcher process remain unchanged. No Home restart or relaunch.
adb install -r /tmp/goree-fixture-green.apk
confirmed=false
for attempt in 1 2 3 4 5 6 7 8; do
  sleep 2
  dump_screen icon-after-upgrade
  if python3 ci/check-fixture-icon.py "$EVIDENCE/icon-after-upgrade.xml" "$EVIDENCE/icon-after-upgrade.png" green; then
    confirmed=true
    break
  fi
  echo "Waiting for live PACKAGE_REPLACED icon redraw ($attempt)"
done
test "$confirmed" = true || { echo "Live package asset refresh not observed"; exit 1; }
pid_after="$(adb shell pidof com.goreecloud.launcher | tr -d '\r')"
test "$pid_before" = "$pid_after" || {
  echo "Launcher was restarted during icon replacement; live-refresh proof invalid"
  exit 1
}
echo "PASS fixture activity/label unchanged; visible icon changed red->green in the same live Launcher process"
