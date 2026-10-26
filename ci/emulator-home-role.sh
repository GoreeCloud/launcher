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


# Test the real, consent-based Android system UI. Do NOT shell-grant HOME.
adb shell cmd role get-role-holders --user 0 android.app.role.HOME > "$EVIDENCE/role-before.txt"
if grep -Fq 'com.goreecloud.launcher' "$EVIDENCE/role-before.txt"; then
  echo "Unexpected existing GoreeCloud HOME role on fresh emulator"; exit 1
fi
adb shell am start -n com.goreecloud.launcher/.LauncherActivity
sleep 3
# A fresh Google APIs AVD can show an ANR from its stock Pixel Launcher
# even while GoreeCloud is visible beneath it. Dismiss that stock-app
# crash, never assign any role from the emulator shell.
for attempt in 1 2 3; do
  dump_screen "launcher-initial-$attempt"
  if grep -Eq "isn.t responding" "$EVIDENCE/launcher-initial-$attempt.xml"; then
    tap_label "launcher-initial-$attempt" "Close app"
    sleep 2
    adb shell am start -n com.goreecloud.launcher/.LauncherActivity
    sleep 2
  else
    break
  fi
done
dump_screen home-before-consent
assert_text home-before-consent "Home workspace"
tap_label home-before-consent "Settings"
sleep 2
adb shell input swipe 445 1680 445 640 520
sleep 2
dump_screen settings-before-consent
assert_text settings-before-consent "GoreeCloud is not the current default Home"
assert_text settings-before-consent "Select default Home app"
tap_label settings-before-consent "Select default Home app"
sleep 3
dump_screen home-role-consent
assert_text home-role-consent "GoreeCloud Launcher"
assert_text home-role-consent "Set as default"
# Android requires explicitly selecting the candidate radio row before enabling
# its affirmative button; a tap on disabled "Set as default" changes nothing.
tap_label home-role-consent "GoreeCloud Launcher"
sleep 1
dump_screen role-selected
tap_label role-selected "Set as default"
sleep 2
role_assigned=0
for attempt in 1 2 3 4 5 6 7 8; do
  adb shell cmd role get-role-holders --user 0 android.app.role.HOME > "$EVIDENCE/role-after.txt"
  if grep -Fq 'com.goreecloud.launcher' "$EVIDENCE/role-after.txt"; then
    role_assigned=1
    break
  fi
  sleep 2
done
test "$role_assigned" = 1 || { echo "Android Home role was not granted after user consent"; exit 1; }
dump_screen after-role-consent
# Returning to our Settings component reports the actual RoleManager state.
adb shell input keyevent 3
sleep 2
dump_screen home-selected
assert_text home-selected "Home workspace"
tap_label home-selected "Settings"
sleep 2
adb shell input swipe 445 1680 445 640 520
sleep 1
dump_screen settings-home-held
assert_text settings-home-held "Current default Home: GoreeCloud"
adb shell input keyevent 3
sleep 2
dump_screen home-after-consent
assert_text home-after-consent "Home workspace"
echo "PASS: user-facing RoleManager HOME dialog, consent, role ownership and Home return"
