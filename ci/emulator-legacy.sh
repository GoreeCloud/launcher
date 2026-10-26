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


# Older Android fallback: API 28 does not provide RoleManager.
# Open the Activity explicitly and never alter another launcher on this AVD.
adb shell am start -n com.goreecloud.launcher/.LauncherActivity
sleep 4
dump_screen legacy-home
assert_text legacy-home "Home workspace"
assert_text legacy-home "Page 1 / 1"
tap_label legacy-home "All apps"
sleep 2
dump_screen legacy-drawer
assert_text legacy-drawer "All applications"
assert_text legacy-drawer "Launcher QA Fixture"
longpress_label legacy-drawer "Launcher QA Fixture"
sleep 1
dump_screen legacy-menu
assert_text legacy-menu "Add to current home page"
tap_label legacy-menu "Add to current home page"
sleep 2
dump_screen legacy-drawer-after-add
tap_label legacy-drawer-after-add "Home"
sleep 2
dump_screen legacy-shortcut
assert_text legacy-shortcut "Launcher QA Fixture"
tap_label legacy-shortcut "Launcher QA Fixture"
sleep 2
dump_screen legacy-target
assert_text legacy-target "QA target running"
adb shell am force-stop com.goreecloud.launcher
adb shell am start -n com.goreecloud.launcher/.LauncherActivity
sleep 3
dump_screen legacy-restart
assert_text legacy-restart "Home workspace"
assert_text legacy-restart "Launcher QA Fixture"
tap_label legacy-restart "Settings"
sleep 2
dump_screen legacy-settings
assert_text legacy-settings "Choose a default Home app in Android settings"
assert_text legacy-settings "Select default Home app"
tap_label legacy-settings "Select default Home app"
sleep 2
dump_screen legacy-picker
python3 - "$EVIDENCE/legacy-picker.xml" <<'PY'
import sys,xml.etree.ElementTree as ET
root=ET.parse(sys.argv[1]).getroot()
packages={n.attrib.get("package","") for n in root.iter()}
if "com.android.settings" not in packages:
    raise SystemExit("Legacy Home fallback did not open Android Settings: "+str(packages))
print("PASS Android 9 opened platform Home selection settings")
PY
adb shell am start -n com.goreecloud.launcher/.LauncherActivity
sleep 2
dump_screen legacy-final
assert_text legacy-final "Home workspace"
assert_text legacy-final "Launcher QA Fixture"
echo "PASS Android 9 native app discovery, launch, shortcut persistence and default Home fallback"
