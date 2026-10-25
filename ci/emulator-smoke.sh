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
    print("PASS",mode,target)
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

# Role manipulation is confined to this disposable emulator, not user hardware.
adb shell cmd role add-role-holder --user 0 android.app.role.HOME com.goreecloud.launcher
adb shell cmd role get-role-holders --user 0 android.app.role.HOME | tee "$EVIDENCE/home-role.txt"
grep -F 'com.goreecloud.launcher' "$EVIDENCE/home-role.txt"
adb shell input keyevent 3
sleep 4
dump_screen home-role
assert_text home-role "Home workspace"
assert_text home-role "Page 1 / 1"

tap_label home-role "Add home page"
sleep 1
dump_screen page-two
assert_text page-two "Page 2 / 2"
tap_label page-two "All apps"
sleep 1
dump_screen drawer
assert_text drawer "All applications"
assert_text drawer "Launcher QA Fixture"

# Exercise the actual search field before using filtered results for placement.
python3 - "$EVIDENCE/drawer.xml" <<'PY'
import re, subprocess, sys, xml.etree.ElementTree as ET
root=ET.parse(sys.argv[1]).getroot()
for node in root.iter():
    a=node.attrib
    if a.get("class") != "android.widget.EditText": continue
    m=re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",a.get("bounds",""))
    if not m: continue
    x1,y1,x2,y2=map(int,m.groups())
    subprocess.run(["adb","shell","input","tap",str((x1+x2)//2),str((y1+y2)//2)],check=True)
    sys.exit(0)
raise SystemExit("Search input missing")
PY
adb shell input text Fixture
adb shell input keyevent 4
sleep 1
dump_screen searched
assert_text searched "Launcher QA Fixture"
python3 - "$EVIDENCE/searched.xml" <<'PY'
import sys, xml.etree.ElementTree as ET
root=ET.parse(sys.argv[1]).getroot()
values=[n.attrib.get("text","") for n in root.iter()]
if "Calendar" in values: raise SystemExit("Unfiltered Calendar app remains in search results")
print("PASS local fixture search filters unrelated apps")
PY

# Exercise contextual placement using an actual independently installed package.
longpress_label searched "Launcher QA Fixture"
sleep 1
dump_screen place-menu
assert_text place-menu "Add to current home page"
tap_label place-menu "Add to current home page"
sleep 1
dump_screen drawer-after-place
tap_label drawer-after-place "Home"
sleep 1
dump_screen home-shortcut
assert_text home-shortcut "Launcher QA Fixture"

longpress_label home-shortcut "Launcher QA Fixture"
sleep 1
dump_screen pin-menu
assert_text pin-menu "Pin to dock"
tap_label pin-menu "Pin to dock"
sleep 1
dump_screen pinned
assert_shortcut_count pinned 2

# Verify the launcher opens the fixture, then Android HOME returns to our selected Home.
tap_label pinned "Launcher QA Fixture"
sleep 1
dump_screen launched-fixture
assert_text launched-fixture "QA target running"
adb shell input keyevent 3
sleep 2
dump_screen home-after-launch
assert_text home-after-launch "Page 2 / 2"
assert_shortcut_count home-after-launch 2

# Verify the native setting changes on device and persists after activity recreation.
tap_label home-after-launch "Settings"
sleep 1
dump_screen settings-before
assert_text settings-before "Home settings"
assert_text settings-before "Show application labels"
tap_label settings-before "Show application labels"
sleep 1
dump_screen settings-after
python3 - "$EVIDENCE/settings-after.xml" <<'PY'
import sys, xml.etree.ElementTree as ET
root=ET.parse(sys.argv[1]).getroot()
matching=[n for n in root.iter() if n.attrib.get("text")=="Show application labels"]
if not matching or matching[0].attrib.get("checked")!="false":
    raise SystemExit("Application label switch was not disabled")
print("PASS labels setting disabled")
PY
adb shell input keyevent 4
sleep 1
dump_screen home-hidden-labels
assert_shortcut_count home-hidden-labels 2

adb shell am force-stop com.goreecloud.launcher
adb shell input keyevent 3
sleep 3
dump_screen after-restart
assert_text after-restart "Page 2 / 2"
assert_shortcut_count after-restart 2
tap_label after-restart "Settings"
sleep 1
dump_screen settings-restored
python3 - "$EVIDENCE/settings-restored.xml" <<'PY'
import sys, xml.etree.ElementTree as ET
root=ET.parse(sys.argv[1]).getroot()
matching=[n for n in root.iter() if n.attrib.get("text")=="Show application labels"]
if not matching or matching[0].attrib.get("checked")!="false":
    raise SystemExit("Labels preference did not survive app force-stop/relaunch")
print("PASS saved label preference persisted")
PY
adb shell dumpsys package com.goreecloud.launcher > "$EVIDENCE/package.txt"
echo "PASS: HOME role, discovery/search, cross-app launch, shortcut/dock and settings persistence"
