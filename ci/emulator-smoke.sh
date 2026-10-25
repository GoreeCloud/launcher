#!/usr/bin/env bash
set -Eeuo pipefail
# Runs only on a fresh GitHub-hosted Android emulator: no personal data, accounts or credentials.
EVIDENCE="build/emulator-evidence"
mkdir -p "$EVIDENCE"
APK="app/build/outputs/apk/debug/app-debug.apk"
adb install -r "$APK"
adb shell input keyevent 82
adb shell am start -W -n com.goreecloud.launcher/.LauncherActivity | tee "$EVIDENCE/launch.txt"

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
values = [n.attrib.get("text","") for n in root.iter()]
if target not in values:
    raise SystemExit("Missing visible UI text: " + target)
print("PASS visible text:", target)
PY
}

tap_label() {
  python3 - "$EVIDENCE/$1.xml" "$2" <<'PY'
import re, subprocess, sys, xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
target = sys.argv[2]
for n in root.iter():
    a = n.attrib
    if a.get("text") == target or a.get("content-desc") == target:
        m = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", a.get("bounds",""))
        if m:
            x1,y1,x2,y2 = map(int,m.groups())
            subprocess.run(["adb","shell","input","tap",str((x1+x2)//2),str((y1+y2)//2)],check=True)
            print("TAP:",target)
            sys.exit(0)
raise SystemExit("Clickable element not found: "+target)
PY
}

sleep 3 # Allow Android system bars/insets and initial layout to settle before coordinate taps.
dump_screen "initial"
assert_text "initial" "Home workspace"
assert_text "initial" "Page 1 / 1"
tap_label "initial" "Add home page"
sleep 1
dump_screen "added-page"
assert_text "added-page" "Page 2 / 2"
tap_label "added-page" "All apps"
sleep 1
dump_screen "app-drawer"
assert_text "app-drawer" "All applications"
tap_label "app-drawer" "Home"
sleep 1
dump_screen "back-home"
assert_text "back-home" "Page 2 / 2"
adb shell am force-stop com.goreecloud.launcher
adb shell am start -W -n com.goreecloud.launcher/.LauncherActivity | tee "$EVIDENCE/relaunch.txt"
dump_screen "after-restart"
assert_text "after-restart" "Page 2 / 2"

# Evidence only: does not claim selection as the device's default Home application.
adb shell dumpsys package com.goreecloud.launcher > "$EVIDENCE/package.txt"
echo "PASS: emulator installation, app start, drawer, page creation and persistence smoke"
