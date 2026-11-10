#!/usr/bin/env bash
set -Eeuo pipefail
# Refuse any personal or incorrectly provisioned device before a role
# request, install, screenshot, system dialog tap or display modification.
serial="emulator-5554"
adb_target() { command adb -s "$serial" "$@"; }
if [[ "$(adb_target get-state 2>/dev/null | tr -d '\r')" != "device" ]] ||
   [[ "$(adb_target shell getprop ro.kernel.qemu 2>/dev/null | tr -d '\r')" != "1" ]] ||
   [[ "$(adb_target shell getprop ro.build.version.sdk 2>/dev/null | tr -d '\r')" != "35" ]]; then
  echo "Refusing Home-role QA outside the verified disposable Android 35 emulator" >&2
  exit 2
fi
EVIDENCE="build/emulator-evidence"
mkdir -p "$EVIDENCE"
trap 'adb_target shell dumpsys activity activities > "$EVIDENCE/activity-on-error.txt" || true' ERR
adb_target install -r app/build/outputs/apk/debug/app-debug.apk
adb_target install -r qa-fixture/build/outputs/apk/debug/qa-fixture-debug.apk

dump_screen() {
  local name="$1"
  adb_target shell uiautomator dump /sdcard/launcher-hierarchy.xml
  adb_target pull /sdcard/launcher-hierarchy.xml "$EVIDENCE/$name.xml" >/dev/null
  adb_target shell screencap -p /sdcard/launcher-screen.png
  adb_target pull /sdcard/launcher-screen.png "$EVIDENCE/$name.png" >/dev/null
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
    cmd=["adb","-s","emulator-5554","shell","input"]
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
# Never infer Home-role placement from a single fixed scroll. Inspect a fresh
# accessibility hierarchy after each bounded swipe and refuse any unknown UI.
seek_home_role_controls() {
  local label="$1" status="$2" action="$3" attempt
  for attempt in 1 2 3 4 5 6 7 8; do
    dump_screen "$label"
    if python3 - "$EVIDENCE/$label.xml" "$status" "$action" <<'PYROLE'
import sys
import xml.etree.ElementTree as ET
try:
    root=ET.parse(sys.argv[1]).getroot()
    if root.tag!="hierarchy":
        raise ValueError("bad hierarchy")
    nodes=list(root.iter("node"))
    for expected in sys.argv[2:4]:
        found=any(
            n.attrib.get("package")=="com.goreecloud.launcher"
            and expected in (n.attrib.get("text"),n.attrib.get("content-desc"))
            and n.attrib.get("bounds","").startswith("[")
            for n in nodes)
        if not found:
            raise ValueError("target not visible")
except (ET.ParseError, OSError, ValueError):
    raise SystemExit(1)
PYROLE
    then
      echo "PASS Home-role Settings controls verified in fresh UI ($attempt/8)"
      return 0
    fi
    # Swipe upward on the disposable emulator to reveal lower app sections.
    adb_target shell input swipe 445 1780 445 460 460
    sleep 1
  done
  echo "Home-role Settings controls not visible after bounded real scrolls" >&2
  return 1
}
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
adb_target shell cmd role get-role-holders --user 0 android.app.role.HOME > "$EVIDENCE/role-before.txt"
if grep -Fq 'com.goreecloud.launcher' "$EVIDENCE/role-before.txt"; then
  echo "Unexpected existing GoreeCloud HOME role on fresh emulator"; exit 1
fi
adb_target shell am start -n com.goreecloud.launcher/.LauncherActivity
sleep 3
# A fresh Google APIs AVD can show an ANR from its stock Pixel Launcher
# even while GoreeCloud is visible beneath it. Dismiss that stock-app
# crash, never assign any role from the emulator shell.
for attempt in 1 2 3; do
  dump_screen "launcher-initial-$attempt"
  if point="$(python3 ci/pixel_launcher_anr.py "$EVIDENCE/launcher-initial-$attempt.xml")"; then
    if (( attempt == 3 )); then
      echo "Too many stock Pixel Launcher ANRs on the disposable emulator" >&2
      exit 1
    fi
    read -r point_x point_y <<< "$point"
    adb_target shell input tap "$point_x" "$point_y"
    sleep 2
    adb_target shell am start -n com.goreecloud.launcher/.LauncherActivity
    sleep 2
  else
    break
  fi
done
dump_screen home-before-consent
assert_text home-before-consent "Home workspace"
tap_label home-before-consent "Settings"
sleep 2
seek_home_role_controls settings-before-consent \
  "GoreeCloud is not the current default Home" "Select default Home app"
assert_text settings-before-consent "GoreeCloud is not the current default Home"
assert_text settings-before-consent "Select default Home app"
tap_label settings-before-consent "Select default Home app"
sleep 3
dump_screen home-role-consent
assert_text home-role-consent "GoreeCloud Launcher"
assert_text home-role-consent "Set as default"
# Cancel the system request. No HOME role mutation is authorized by cancellation.
assert_text home-role-consent "Cancel"
tap_label home-role-consent "Cancel"
sleep 2
adb_target shell cmd role get-role-holders --user 0 android.app.role.HOME > "$EVIDENCE/role-after-cancel.txt"
if grep -Fq 'com.goreecloud.launcher' "$EVIDENCE/role-after-cancel.txt"; then
  echo "Home role unexpectedly changed after Cancel"; exit 1
fi
seek_home_role_controls settings-after-cancel \
  "GoreeCloud is not the current default Home" "Select default Home app"
assert_text settings-after-cancel "GoreeCloud is not the current default Home"
assert_text settings-after-cancel "Select default Home app"
tap_label settings-after-cancel "Select default Home app"
sleep 2
dump_screen role-consent-retry
assert_text role-consent-retry "GoreeCloud Launcher"
# Android requires explicitly selecting the candidate radio row before enabling
# its affirmative button; a tap on disabled "Set as default" changes nothing.
tap_label role-consent-retry "GoreeCloud Launcher"
sleep 1
dump_screen role-selected
tap_label role-selected "Set as default"
sleep 2
role_assigned=0
for attempt in 1 2 3 4 5 6 7 8; do
  adb_target shell cmd role get-role-holders --user 0 android.app.role.HOME > "$EVIDENCE/role-after.txt"
  if grep -Fq 'com.goreecloud.launcher' "$EVIDENCE/role-after.txt"; then
    role_assigned=1
    break
  fi
  sleep 2
done
test "$role_assigned" = 1 || { echo "Android Home role was not granted after user consent"; exit 1; }
dump_screen after-role-consent
# Returning to our Settings component reports the actual RoleManager state.
adb_target shell input keyevent 3
sleep 2
dump_screen home-selected
assert_text home-selected "Home workspace"
tap_label home-selected "Settings"
sleep 2
seek_home_role_controls settings-home-held \
  "Current default Home: GoreeCloud" "Manage default Home apps"
assert_text settings-home-held "Current default Home: GoreeCloud"
assert_text settings-home-held "Manage default Home apps"
adb_target shell input keyevent 3
sleep 2
dump_screen home-after-consent
assert_text home-after-consent "Home workspace"
echo "PASS: cancelled Home selection unchanged, then consent granted role and Home returned"
