#!/usr/bin/env bash
set -Eeuo pipefail
# Refuse a personal Android phone before ANY install or display change.
# All subsequent ADB calls explicitly target the hosted disposable emulator.
serial="emulator-5554"
adb_target() { command adb -s "$serial" "$@"; }
if [[ "$(adb_target get-state 2>/dev/null | tr -d '\r')" != "device" ]] ||
   [[ "$(adb_target shell getprop ro.kernel.qemu 2>/dev/null | tr -d '\r')" != "1" ]] ||
   [[ "$(adb_target shell getprop ro.build.version.sdk 2>/dev/null | tr -d '\r')" != "35" ]]; then
  echo "Refusing Launcher Settings QA outside the verified Android 35 emulator" >&2
  exit 2
fi
EVIDENCE="build/emulator-evidence"
mkdir -p "$EVIDENCE"
trap 'adb_target shell dumpsys activity activities > "$EVIDENCE/settings-error.txt" || true' ERR
adb_target install -r app/build/outputs/apk/debug/app-debug.apk
adb_target shell settings put system accelerometer_rotation 0
adb_target shell settings put system font_scale 2.0
adb_target shell am start -W -n com.goreecloud.launcher/.LauncherActivity
sleep 3

dump_screen() {
  local name="$1"
  local path="$EVIDENCE/$name.xml"
  for attempt in 1 2 3 4 5 6; do
    if adb_target shell uiautomator dump /sdcard/gc-settings.xml >/dev/null 2>&1 \
       && adb_target pull /sdcard/gc-settings.xml "$path" >/dev/null 2>&1 \
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
  adb_target shell screencap -p /sdcard/gc-settings.png
  adb_target pull /sdcard/gc-settings.png "$EVIDENCE/$name.png" >/dev/null
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
    subprocess.run(["adb","-s","emulator-5554","shell","input","tap",str((x1+x2)//2),str((y1+y2)//2)],check=True)
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
opened=false
anr_recoveries=0
for attempt in 1 2 3 4 5 6; do
  dump_screen settings-large-home
  if grep -Fq 'content-desc="Settings"' "$EVIDENCE/settings-large-home.xml"; then
    press_label settings-large-home "Settings"
    opened=true
    break
  fi
  # Recover ONLY a verified Android system "Pixel Launcher isn't responding"
  # dialog on the disposable emulator; never dismiss our app's own ANR or
  # any unknown system prompt. At most two recoveries, then fail closed.
  if (( anr_recoveries < 2 )); then
    if tap="$(python3 ci/pixel_launcher_anr.py "$EVIDENCE/settings-large-home.xml")"; then
      read -r tap_x tap_y <<< "$tap"
      adb_target shell input tap "$tap_x" "$tap_y"
      anr_recoveries=$((anr_recoveries + 1))
      printf 'Recovered verified disposable Pixel Launcher ANR (%s/2)\n' "$anr_recoveries"
      sleep 2
    fi
  fi
  adb_target shell am start -W -n com.goreecloud.launcher/.LauncherActivity >/dev/null
  sleep 2
done
if [ "$opened" != true ]; then echo "Launcher Settings action inaccessible" >&2; exit 1; fi
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
adb_target shell settings put system user_rotation 1
sleep 3
dump_screen settings-large-landscape
check_header settings-large-landscape
adb_target shell settings put system user_rotation 0
sleep 3
dump_screen settings-large-restored
check_header settings-large-restored
# Test that both backup controls remain reachable by scrolling, without activating either.
reached=false
for attempt in 1 2 3 4 5 6; do
  adb_target shell input swipe 650 1650 650 430 550
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
adb_target shell settings put system font_scale 1.0
adb_target shell settings put system user_rotation 0
echo "PASS: Settings at 200% font in portrait and landscape"
