#!/usr/bin/env bash
set -Eeuo pipefail
# Fresh GitHub-hosted emulator only; never execute role changes on physical/personal devices.
EVIDENCE="build/emulator-evidence"
mkdir -p "$EVIDENCE"
trap 'adb shell dumpsys activity activities > "$EVIDENCE/activity-on-error.txt" || true' ERR
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r qa-fixture/build/outputs/apk/debug/qa-fixture-debug.apk

dump_screen() {
  local name="$1"
  adb shell uiautomator dump /sdcard/launcher-hierarchy.xml
  adb pull /sdcard/launcher-hierarchy.xml "$EVIDENCE/$name.xml" >/dev/null
  adb shell screencap -p /sdcard/launcher-screen.png
  adb pull /sdcard/launcher-screen.png "$EVIDENCE/$name.png" >/dev/null
}
assert_text() {
  python3 - "$EVIDENCE/$1.xml" "$2" <<'PY'
import sys, xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
target = sys.argv[2]
if not any(target in (n.attrib.get("text",""),n.attrib.get("content-desc","")) for n in root.iter()):
    raise SystemExit("Missing UI text/description: "+target)
print("PASS visible:",target)
PY
}
act_on() {
  python3 - "$EVIDENCE/$1.xml" "$2" "$3" <<'PY'
import re, subprocess, sys, xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
target, mode = sys.argv[2:4]
matching = [n for n in root.iter()
    if n.attrib.get("text") == target or n.attrib.get("content-desc") == target]
matching.sort(key=lambda n: n.attrib.get("clickable")!="true")
for n in matching:
    m = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", n.attrib.get("bounds",""))
    if not m: continue
    x1,y1,x2,y2=map(int,m.groups())
    x,y=(x1+x2)//2,(y1+y2)//2
    if x<0 or y<0 or x2<=x1 or y2<=y1: continue
    cmd=["adb","shell","input"]
    cmd += (["swipe",str(x),str(y),str(x),str(y),"1000"] if mode=="long"
            else ["tap",str(x),str(y)])
    subprocess.run(cmd,check=True)
    print("PASS",mode,target,"at",x,y,"bounds",n.attrib.get("bounds"))
    sys.exit(0)
raise SystemExit("UI element not found: "+target)
PY
}
tap_label() { act_on "$1" "$2" tap; }
longpress_label() { act_on "$1" "$2" long; }
assert_shortcut_count() {
  python3 - "$EVIDENCE/$1.xml" "$2" <<'PY'
import sys, xml.etree.ElementTree as ET
root=ET.parse(sys.argv[1]).getroot()
count=sum(n.attrib.get("content-desc")=="Launcher QA Fixture" for n in root.iter())
if count<int(sys.argv[2]):
    raise SystemExit("Expected fixture shortcuts >= "+sys.argv[2]+", found "+str(count))
print("PASS fixture icon count",count)
PY
}



# Only a clean GitHub Actions Android emulator: never disable user applications.
adb shell cmd role add-role-holder --user 0 android.app.role.HOME com.goreecloud.launcher
adb shell input keyevent 3
sleep 4
dump_screen availability-start
assert_text availability-start "Home workspace"

tap_label availability-start "All apps"
sleep 2
dump_screen availability-drawer
assert_text availability-drawer "Launcher QA Fixture"
longpress_label availability-drawer "Launcher QA Fixture"
sleep 1
dump_screen availability-pin-menu
assert_text availability-pin-menu "Pin to dock"
tap_label availability-pin-menu "Pin to dock"
sleep 1
dump_screen availability-after-pin
longpress_label availability-after-pin "Launcher QA Fixture"
sleep 1
dump_screen availability-place-menu
assert_text availability-place-menu "Add to current home page"
tap_label availability-place-menu "Add to current home page"
sleep 1
dump_screen availability-after-place
tap_label availability-after-place "Home"
sleep 1
dump_screen availability-initial
assert_shortcut_count availability-initial 2

# System-level temporary package unavailability should hide, not delete,
# saved launcher configuration. Do not use adb on a user's own phone.
adb shell pm disable-user --user 0 com.goreecloud.launcher.fixture > "$EVIDENCE/disabled-synthetic-app.txt"
adb shell am force-stop com.goreecloud.launcher
adb shell input keyevent 3
sleep 3
dump_screen availability-disabled
python3 - "$EVIDENCE/availability-disabled.xml" <<'PY'
import sys, xml.etree.ElementTree as ET
root=ET.parse(sys.argv[1]).getroot()
n=sum(e.attrib.get("content-desc")=="Launcher QA Fixture" for e in root.iter())
if n: raise SystemExit("Unavailable package still exposed as launchable shortcut: "+str(n))
print("PASS fixture hidden while its package is disabled")
PY

adb shell pm enable --user 0 com.goreecloud.launcher.fixture > "$EVIDENCE/reenabled-synthetic-app.txt"
adb shell am force-stop com.goreecloud.launcher
adb shell input keyevent 3
sleep 3
dump_screen availability-reenabled
assert_text availability-reenabled "Home workspace"
assert_shortcut_count availability-reenabled 2
echo "PASS: saved Home and dock shortcuts recover when temporarily disabled app is enabled"
