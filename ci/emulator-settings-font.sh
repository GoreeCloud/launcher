#!/usr/bin/env bash
set -Eeuo pipefail
# Operate only on a disposable CI Android emulator.
EVIDENCE="build/emulator-evidence"
mkdir -p "$EVIDENCE"
trap 'adb shell dumpsys activity activities > "$EVIDENCE/settings-error.txt" || true' ERR
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell settings put system accelerometer_rotation 0
adb shell settings put system font_scale 2.0
adb shell am start -W -n com.goreecloud.launcher/.LauncherActivity
sleep 3
. ci/emulator-onboarding.sh
accept_onboarding_if_present

dump_screen() {
  local name="$1"
  local path="$EVIDENCE/$name.xml"
  for attempt in 1 2 3 4 5 6; do
    if adb shell uiautomator dump /sdcard/gc-settings.xml >/dev/null 2>&1 \
       && adb pull /sdcard/gc-settings.xml "$path" >/dev/null 2>&1 \
       && python3 - "$path" <<'PY'
import sys, xml.etree.ElementTree as ET
try:
    assert ET.parse(sys.argv[1]).getroot().tag == "hierarchy"
except (OSError, ET.ParseError, AssertionError):
    raise SystemExit(1)
PY
    then break; fi
    if [ "$attempt" = 6 ]; then echo "Invalid settings UI hierarchy" >&2; return 1; fi
    sleep 2
  done
  adb shell screencap -p /sdcard/gc-settings.png
  adb pull /sdcard/gc-settings.png "$EVIDENCE/$name.png" >/dev/null
}
press_label() {
  python3 - "$EVIDENCE/$1.xml" "$2" <<'PY'
import re,subprocess,sys,xml.etree.ElementTree as ET
target=sys.argv[2]
for n in ET.parse(sys.argv[1]).getroot().iter():
    if n.attrib.get("text")!=target and n.attrib.get("content-desc")!=target: continue
    m=re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",n.attrib.get("bounds",""))
    if not m: continue
    x1,y1,x2,y2=map(int,m.groups())
    if x2<=x1 or y2<=y1: continue
    subprocess.run(["adb","shell","input","tap",str((x1+x2)//2),str((y1+y2)//2)],check=True)
    print("PASS tapped",target)
    sys.exit(0)
raise SystemExit("Missing Settings control: "+target)
PY
}
check_header() {
  python3 - "$EVIDENCE/$1.xml" <<'PY'
import re,sys,xml.etree.ElementTree as ET
nodes=list(ET.parse(sys.argv[1]).getroot().iter())
def bounds(text):
    hits=[n for n in nodes if n.attrib.get("text")==text or n.attrib.get("content-desc")==text]
    if not hits: raise SystemExit("Missing settings text: "+text)
    m=re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",hits[0].attrib.get("bounds",""))
    if not m: raise SystemExit("No text bounds: "+text)
    return tuple(map(int,m.groups()))
back,title,subtitle=(bounds(s) for s in
    ("Back to Home","Home settings","Personalize your workspace"))
if any(v[3]<=v[1] for v in (back,title,subtitle)):
    raise SystemExit("Settings title, subtitle or back control invisible")
if title[3]>subtitle[1]+3:
    raise SystemExit("Settings heading overlaps subtitle: "+str((title,subtitle)))
print("PASS settings header legible at 200% font")
PY
}
# API35 fresh emulator can still be finishing the system Home transition
# after am start reports success. Re-open our explicit activity and locate the
# real accessible Settings button from a NEW hierarchy; never invent a tap.
# At 200% font the dashboard actions are intentionally absent.
# The editor route is tested by Android 16 smoke; here validate native
# Settings directly so no hidden Home control is falsely required.
dump_screen settings-large-home
press_label settings-large-home "Edit Home screen"
sleep 1
dump_screen settings-large-editor
press_label settings-large-editor "Launcher Settings"
sleep 2
dump_screen settings-large-portrait
check_header settings-large-portrait
python3 - "$EVIDENCE/settings-large-portrait.xml" <<'PYROLE'
import sys,xml.etree.ElementTree as ET
matches=[n.attrib for n in ET.parse(sys.argv[1]).getroot().iter()
         if n.attrib.get("content-desc")=="Back to Home"]
if not any(n.get("class")=="android.widget.Button"
           and n.get("clickable")=="true" for n in matches):
    raise SystemExit("Settings Back does not expose accessible button role")
print("PASS Settings Back accessible button semantics")
PYROLE
adb shell settings put system user_rotation 1
sleep 3
dump_screen settings-large-landscape
check_header settings-large-landscape
adb shell settings put system user_rotation 0
sleep 3
dump_screen settings-large-restored
check_header settings-large-restored
# Test that both backup controls remain reachable by scrolling, without activating either.
reached=false
for attempt in $(seq 1 16); do
  adb shell input swipe 650 1650 650 430 550
  sleep 1
  dump_screen settings-large-backup
  if python3 - "$EVIDENCE/settings-large-backup.xml" <<'PY'
import sys,xml.etree.ElementTree as ET
texts={n.attrib.get("text","") for n in ET.parse(sys.argv[1]).getroot().iter()}
assert {"Export backup","Import backup"}.issubset(texts)
print("PASS Settings backup controls reachable with 200% font")
PY
  then reached=true; break; fi
done
if [ "$reached" != true ]; then echo "Settings backup controls unreachable" >&2; exit 1; fi
adb shell settings put system font_scale 1.0
adb shell settings put system user_rotation 0
echo "PASS: Settings at 200% font in portrait and landscape"
