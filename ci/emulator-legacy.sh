#!/usr/bin/env bash
set -Eeuo pipefail
. ci/launcher-gestures.sh
# Fresh GitHub-hosted emulator only; never execute role changes on physical/personal devices.
EVIDENCE="build/emulator-evidence"
mkdir -p "$EVIDENCE"
trap 'adb shell dumpsys activity activities > "$EVIDENCE/activity-on-error.txt" || true' ERR
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r qa-fixture/build/outputs/apk/debug/qa-fixture-debug.apk

dump_screen() {
  local name="$1"
  local xml="$EVIDENCE/$name.xml"
  local png="$EVIDENCE/$name.png"
  # Rotation can briefly disconnect ADB, and UIAutomator can return a null
  # root. Never accept stale local XML/screenshots from a previous attempt.
  for attempt in 1 2 3 4 5 6 7 8; do
    rm -f "$xml" "$png"
    if adb get-state >/dev/null 2>&1 \
      && adb shell rm -f /sdcard/launcher-hierarchy.xml /sdcard/launcher-screen.png >/dev/null 2>&1 \
      && adb shell uiautomator dump /sdcard/launcher-hierarchy.xml >/dev/null 2>&1 \
      && adb pull /sdcard/launcher-hierarchy.xml "$xml" >/dev/null 2>&1 \
      && adb shell screencap -p /sdcard/launcher-screen.png >/dev/null 2>&1 \
      && adb pull /sdcard/launcher-screen.png "$png" >/dev/null 2>&1 \
      && python3 - "$xml" "$png" <<'PYCHECK'
import struct,sys,xml.etree.ElementTree as ET
try:
    root=ET.parse(sys.argv[1]).getroot()
    if root.tag!="hierarchy": raise ValueError("Not an Android UI hierarchy")
    with open(sys.argv[2],"rb") as source: header=source.read(24)
    if len(header)!=24 or header[:8]!=b"\x89PNG\r\n\x1a\n":
        raise ValueError("Invalid screenshot")
    width,height=struct.unpack(">II",header[16:24])
    if width==0 or height==0: raise ValueError("Zero-sized screenshot")
except (OSError, ET.ParseError, ValueError):
    raise SystemExit(1)
PYCHECK
    then return 0; fi
    echo "Retry transient emulator hierarchy/screenshot capture ($name, $attempt)"
    sleep 3
  done
  echo "No fresh, valid emulator UI/screenshot for $name" >&2
  return 1
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
. ci/emulator-onboarding.sh
accept_onboarding_if_present
dump_screen legacy-home
assert_text legacy-home "Live Home clock and date"
assert_text legacy-home "Home page 1 of 1"
launcher_swipe_up
sleep 2
dump_screen legacy-drawer
assert_text legacy-drawer "Search apps"
# The fixture is alphabetically below the visible grid on API 28. Verify its
# discovery using the real local search instead of assuming every app fits.
python3 - "$EVIDENCE/legacy-drawer.xml" <<'PY'
import re,subprocess,sys,xml.etree.ElementTree as ET
root=ET.parse(sys.argv[1]).getroot()
for n in root.iter():
    a=n.attrib
    if a.get("class")!="android.widget.EditText": continue
    m=re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",a.get("bounds",""))
    if m:
        x1,y1,x2,y2=map(int,m.groups())
        subprocess.run(["adb","shell","input","tap",str((x1+x2)//2),str((y1+y2)//2)],check=True)
        sys.exit(0)
raise SystemExit("Native application search input missing")
PY
adb shell input text QA
adb shell input keyevent 4
sleep 2
# Older Android may deliver a delayed package/keyboard transition. Re-read
# live UI state a bounded number of times before declaring fixture missing.
for attempt in 1 2 3 4; do
  dump_screen legacy-filtered
  if grep -Fq 'content-desc="Launcher QA Fixture"' "$EVIDENCE/legacy-filtered.xml"; then
    break
  fi
  echo "Waiting for API 26/28 filtered fixture discovery ($attempt)"
  sleep 2
done
assert_text legacy-filtered "Launcher QA Fixture"
# Older AVDs may ignore the first long-press during app-grid focus settling.
# Refresh bounds and require the actual menu before accepting placement.
for attempt in 1 2 3 4; do
  dump_screen legacy-before-menu
  longpress_label legacy-before-menu "Launcher QA Fixture"
  sleep 2
  dump_screen legacy-menu
  if grep -Fq 'text="Add to current home page"' "$EVIDENCE/legacy-menu.xml"; then
    break
  fi
  echo "API 26/28 fixture menu not yet visible ($attempt)"
done
assert_text legacy-menu "Add to current home page"
tap_label legacy-menu "Add to current home page"
sleep 2
dump_screen legacy-drawer-after-add
launcher_swipe_down
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
assert_text legacy-restart "Live Home clock and date"
assert_text legacy-restart "Launcher QA Fixture"
tap_label legacy-restart "Edit Home screen"
sleep 1
dump_screen legacy-edit
tap_label legacy-edit "Settings"
sleep 2
# The new gesture, dock and drawer sections precede Android Home settings.
# Keep testing the real fallback control after scrolling, not its initial
# viewport position.
home_fallback_reached=false
for attempt in 1 2 3 4 5 6 7 8; do
  dump_screen legacy-settings
  if grep -Fq 'text="Choose a default Home app in Android settings"' "$EVIDENCE/legacy-settings.xml" \
    && grep -Fq 'text="Select default Home app"' "$EVIDENCE/legacy-settings.xml"; then
    home_fallback_reached=true
    break
  fi
  adb shell input swipe 430 1680 430 480 520
  sleep 1
done
test "$home_fallback_reached" = true
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
print("PASS legacy API opened platform Home selection settings")
PY
adb shell am start -n com.goreecloud.launcher/.LauncherActivity
sleep 2
dump_screen legacy-final
assert_text legacy-final "Live Home clock and date"
assert_text legacy-final "Launcher QA Fixture"
echo "PASS Android API $(adb shell getprop ro.build.version.sdk | tr -d '\r') native app discovery, launch, shortcut persistence and default Home fallback"
