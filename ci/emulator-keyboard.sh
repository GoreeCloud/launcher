#!/usr/bin/env bash
set -Eeuo pipefail
. ci/launcher-gestures.sh
# Fresh GitHub-hosted emulator only; never execute role changes on physical/personal devices.
EVIDENCE="build/emulator-evidence"
mkdir -p "$EVIDENCE"
trap 'adb shell dumpsys activity activities > "$EVIDENCE/activity-on-error.txt" || true' ERR
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r qa-fixture/build/outputs/apk/debug/qa-fixture-debug.apk

# Android's accessibility bridge occasionally returns a null hierarchy
# immediately after emulator startup. Retry only the snapshot, and never use
# an XML file left over from a previous iteration.
dump_hierarchy() {
  local name="$1" remote=/sdcard/launcher-hierarchy.xml attempt
  for attempt in 1 2 3 4 5 6; do
    adb shell rm -f "$remote" >/dev/null 2>&1 || true
    if adb shell uiautomator dump "$remote" >/dev/null 2>&1 \
        && adb shell test -s "$remote" \
        && adb pull "$remote" "$EVIDENCE/$name.xml" >/dev/null 2>&1 \
        && python3 - "$EVIDENCE/$name.xml" <<'PY'
import sys, xml.etree.ElementTree as ET
try:
    root = ET.parse(sys.argv[1]).getroot()
    if root.tag != 'hierarchy':
        raise ValueError('Unexpected UI hierarchy root')
except (ET.ParseError, ValueError):
    sys.exit(1)
PY
    then
      return 0
    fi
    echo "UI hierarchy unavailable ($name, attempt $attempt/6)" >&2
    sleep 2
  done
  echo "Unable to obtain a valid UI hierarchy: $name" >&2
  return 1
}
dump_screen() {
  local name="$1"
  dump_hierarchy "$name"
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
assert_text keyboard-initial "Live Home clock and date"
# Button-looking text actions must expose the actual native button role.
python3 - "$EVIDENCE/keyboard-initial.xml" <<'PYROLE'
import sys,xml.etree.ElementTree as ET
nodes=[n.attrib for n in ET.parse(sys.argv[1]).getroot().iter()]
for title in ("Edit Home screen","Add Android widget","Add app to dock"):
    matches=[n for n in nodes if n.get("text")==title or n.get("content-desc")==title]
    if not any(n.get("class")=="android.widget.Button"
               and n.get("clickable")=="true" and n.get("focusable")=="true"
               for n in matches):
        raise SystemExit("Not an accessible button: "+title+" "+str(matches))
    print("PASS accessible button semantics:",title)
PYROLE
launcher_swipe_up
# A fresh Home can consume the first tap; package discovery may also lag
# initial fixture APK installation. Require BOTH selected tab and actual tile.
for attempt in 1 2 3 4 5 6; do
  sleep 2
  dump_screen keyboard-drawer
  if grep -Fq 'text="Search apps"' "$EVIDENCE/keyboard-drawer.xml"; then
    if grep -Fq 'content-desc="Launcher QA Fixture"' "$EVIDENCE/keyboard-drawer.xml"; then
      break
    fi
    echo "Fixture tile not indexed yet on attempt $attempt" >&2
    if (( attempt % 2 == 0 )); then
      # Re-query package inventory through normal Home activity startup.
      # This is a disposable emulator, never a personal Home device.
      adb shell am force-stop com.goreecloud.launcher
      adb shell input keyevent KEYCODE_HOME
      sleep 2
      dump_screen keyboard-home-refresh
      assert_text keyboard-home-refresh "Live Home clock and date"
      launcher_swipe_up
    fi
  else
    echo "All apps selection not yet visible; retry $attempt" >&2
    launcher_swipe_up
  fi
done
assert_text keyboard-drawer "Launcher QA Fixture"
assert_text keyboard-drawer "Launcher QA Fixture"

# Navigate with hardware keyboard Tab events, never a touch long-press.
# UIAutomator reports the currently focused view; seek the fixture tile.
found=0
# A full drawer can contain more than 60 focusable Android/system nodes.
for attempt in $(seq 1 100); do
  adb shell input keyevent KEYCODE_TAB
  sleep 0.20
  dump_hierarchy tab-traversal
  if python3 - "$EVIDENCE/tab-traversal.xml" <<'PY'
import sys,xml.etree.ElementTree as ET
root=ET.parse(sys.argv[1]).getroot()
matches=[n.attrib for n in root.iter()
    if n.attrib.get("content-desc")=="Launcher QA Fixture" and n.attrib.get("focused")=="true"]
if not matches: sys.exit(1)
assert all(n.get("class")=="android.widget.Button" and n.get("clickable")=="true" for n in matches), "Expected native Button role"
print("PASS keyboard Tab focused accessible Button fixture shortcut")
PY
  then
    found=1
    break
  fi
  if (( attempt % 15 == 0 )); then
    python3 - "$EVIDENCE/tab-traversal.xml" "$attempt" <<'PY'
import sys, xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
focused = [(n.attrib.get("content-desc") or n.attrib.get("text") or n.attrib.get("class"))
           for n in root.iter() if n.attrib.get("focused") == "true"]
print("Tab step", sys.argv[2], "focused:", focused)
PY
  fi
done
test "$found" -eq 1 || { echo "Unable to focus launcher tile with Tab after 100 events"; exit 1; }
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
assert_text keyboard-home "Live Home clock and date"
assert_text keyboard-home "Launcher QA Fixture"
echo "PASS: hardware Tab, Menu and Enter shortcut placement"
