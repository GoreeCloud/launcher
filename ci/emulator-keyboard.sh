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


# All actions are confined to a disposable GitHub-hosted Android emulator.
# Use the shell only to establish a repeatable HOME role, not to fake keyboard input.
adb shell cmd role add-role-holder --user 0 android.app.role.HOME com.goreecloud.launcher
adb shell input keyevent KEYCODE_HOME
sleep 4
dump_screen keyboard-initial
assert_text keyboard-initial "Home workspace"
tap_label keyboard-initial "All apps"
sleep 2
dump_screen keyboard-drawer
assert_text keyboard-drawer "All applications"
assert_text keyboard-drawer "Launcher QA Fixture"

# Navigate with hardware keyboard Tab events, never a touch long-press.
# UIAutomator reports the currently focused view; seek the fixture tile.
found=0
for attempt in $(seq 1 60); do
  adb shell input keyevent KEYCODE_TAB
  sleep 0.20
  adb shell uiautomator dump /sdcard/launcher-hierarchy.xml >/dev/null
  adb pull /sdcard/launcher-hierarchy.xml "$EVIDENCE/tab-traversal.xml" >/dev/null
  if python3 - "$EVIDENCE/tab-traversal.xml" <<'PY'
import sys,xml.etree.ElementTree as ET
root=ET.parse(sys.argv[1]).getroot()
matches=[n.attrib for n in root.iter()
    if n.attrib.get("content-desc")=="Launcher QA Fixture" and n.attrib.get("focused")=="true"]
if not matches: sys.exit(1)
print("PASS keyboard Tab focused fixture shortcut")
PY
  then
    found=1
    break
  fi
done
test "$found" -eq 1 || { echo "Unable to focus launcher tile with Tab"; exit 1; }
dump_screen keyboard-tile-focused

# Android's physical Menu key must expose the native contextual actions.
adb shell input keyevent KEYCODE_MENU
sleep 1
dump_screen keyboard-menu
assert_text keyboard-menu "Add to current home page"
assert_text keyboard-menu "Pin to dock"
# Accessibility keyboard workflow: act on the first menu item with ENTER.
adb shell input keyevent KEYCODE_DPAD_CENTER
sleep 2
dump_screen keyboard-after-action
# Menu may remain open if keyboard focus was elsewhere. Treat that as failure.
python3 - "$EVIDENCE/keyboard-after-action.xml" <<'PY'
import sys,xml.etree.ElementTree as ET
root=ET.parse(sys.argv[1]).getroot()
visible=[n.attrib.get("text","") for n in root.iter()]
if "Add to current home page" in visible:
    raise SystemExit("Keyboard selection did not dismiss shortcut menu")
print("PASS keyboard activated contextual action")
PY

# Return to Home via a conventional keyboard event (not a touch long-press).
adb shell input keyevent KEYCODE_HOME
sleep 2
dump_screen keyboard-home
assert_text keyboard-home "Home workspace"
assert_text keyboard-home "Launcher QA Fixture"
echo "PASS: hardware Tab, Menu and Enter shortcut placement"
