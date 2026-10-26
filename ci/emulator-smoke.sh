#!/usr/bin/env bash
set -Eeuo pipefail
# Fresh GitHub-hosted emulator only; never execute role changes on physical/personal devices.
EVIDENCE="build/emulator-evidence"
mkdir -p "$EVIDENCE"
trap 'adb shell dumpsys activity activities > "$EVIDENCE/activity-on-error.txt" || true' ERR
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r qa-fixture/build/outputs/apk/debug/qa-fixture-debug.apk

# Cold-start UiAutomator occasionally produces no root at all on API 36.
# Retry only freshly created valid hierarchies; never read stale XML.
. ci/emulator-hierarchy-retry.sh

dump_screen() {
  local name="$1"
  capture_hierarchy_with_retries "$name" "$EVIDENCE/$name.xml"
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
# A transient Android input focus transition can swallow the first long press;
# retry only when the required real context menu is genuinely missing.
. ci/emulator-menu-retry.sh
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
assert_text home-role "Add app to dock"

# Verify an actual touchscreen-like swipe on the disposable emulator opens the
# drawer from Home and returns to Home. Do not replace this with a tab tap.
read -r W H < <(adb shell wm size | sed -n 's/.*: \([0-9]*\)x\([0-9]*\).*/\1 \2/p' | head -1)
test "$W" -gt 0 && test "$H" -gt 0
X=$((W/2))
adb shell input swipe "$X" "$((H*70/100))" "$X" "$((H*41/100))" 420
sleep 2
dump_screen swipe-up-opens-drawer
assert_text swipe-up-opens-drawer "All apps tab, selected"
assert_text swipe-up-opens-drawer "All applications"
adb shell input swipe "$X" "$((H*44/100))" "$X" "$((H*70/100))" 420
sleep 2
dump_screen swipe-down-closes-drawer
assert_text swipe-down-closes-drawer "Home tab, selected"
assert_text swipe-down-closes-drawer "Home workspace"

# Android may apply status-bar/window insets after the initial hierarchy capture.
# Always re-read the current control bounds and confirm the resulting state.
page_ready=0
for attempt in 1 2 3; do
  dump_screen "before-add-$attempt"
  if grep -Fq 'text="Page 2 / 2"' "$EVIDENCE/before-add-$attempt.xml"; then
    page_ready=1
    break
  fi
  tap_label "before-add-$attempt" "Add home page"
  sleep 2
  dump_screen page-two
  if grep -Fq 'text="Page 2 / 2"' "$EVIDENCE/page-two.xml"; then
    page_ready=1
    break
  fi
  echo "Page-add tap did not take effect; refreshing UI coordinates (attempt $attempt)" >&2
done
test "$page_ready" = 1
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
retry_longpress_menu place-menu "Launcher QA Fixture" "Add to current home page"
tap_label place-menu "Add to current home page"
sleep 1
dump_screen drawer-after-place
# A second activity provides a stable two-item layout for ordering checks.
retry_longpress_menu notes-menu "Launcher Fixture Notes" "Add to current home page"
tap_label notes-menu "Add to current home page"
sleep 1
dump_screen drawer-after-notes
tap_label drawer-after-notes "Home"
sleep 1
dump_screen home-shortcut
assert_text home-shortcut "Launcher QA Fixture"
assert_text home-shortcut "Launcher Fixture Notes"
python3 - "$EVIDENCE/home-shortcut.xml" <<'PY'
import sys, xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
fields = [n.attrib for n in root.iter() if n.attrib.get("class")=="android.widget.EditText"]
if len(fields)!=1 or fields[0].get("focused")=="true":
    raise SystemExit("Home search retained keyboard focus after switching from All apps")
print("PASS Home search is not focused")
PY

# Verify an accessible menu alternative to drag/drop for ordering shortcuts.
retry_longpress_menu order-menu "Launcher Fixture Notes" "Move earlier"
tap_label order-menu "Move earlier"
sleep 1
dump_screen reordered
assert_text reordered "Launcher Fixture Notes"
retry_longpress_menu order-reverse-menu "Launcher Fixture Notes" "Move later"
tap_label order-reverse-menu "Move later"
sleep 1
dump_screen order-restored
assert_text order-restored "Launcher Fixture Notes"

retry_longpress_menu pin-menu "Launcher QA Fixture" "Pin to dock"
tap_label pin-menu "Pin to dock"
sleep 1
dump_screen pinned
assert_shortcut_count pinned 2

# Move a populated shortcut between pre-existing pages, in both directions.
retry_longpress_menu cross-page-menu "Launcher QA Fixture" "Move to previous page"
tap_label cross-page-menu "Move to previous page"
sleep 1
dump_screen moved-page-one
assert_text moved-page-one "Page 1 / 2"
assert_shortcut_count moved-page-one 2
retry_longpress_menu return-menu "Launcher QA Fixture" "Move to next page"
tap_label return-menu "Move to next page"
sleep 1
dump_screen moved-page-two
assert_text moved-page-two "Page 2 / 2"
assert_shortcut_count moved-page-two 2
# The page reflows from one full-width tile to two columns after moving.
# Re-read settled bounds before launching instead of tapping a stale hit region.
sleep 3
dump_screen launch-ready
assert_shortcut_count launch-ready 2

# Verify the launcher opens the fixture, then Android HOME returns to our selected Home.
tap_label launch-ready "Launcher QA Fixture"
sleep 2
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
# New gesture and dock settings lengthen the native ScrollView. Scroll
# toward the document-picker controls without assuming one screen-height swipe.
backup_visible=0
for attempt in 1 2 3 4; do
  adb shell input swipe 420 1750 420 500 460
  sleep 1
  dump_screen settings-backup
  if grep -Fq 'text="Export backup"' "$EVIDENCE/settings-backup.xml" \
      && grep -Fq 'text="Import backup"' "$EVIDENCE/settings-backup.xml"; then
    backup_visible=1
    break
  fi
done
test "$backup_visible" = 1
assert_text settings-backup "Export backup"
assert_text settings-backup "Import backup"
# Open and dismiss the system document picker without exporting user data.
tap_label settings-backup "Export backup"
sleep 2
dump_screen export-picker-open
python3 - "$EVIDENCE/export-picker-open.xml" <<'PY'
import sys, xml.etree.ElementTree as ET
root=ET.parse(sys.argv[1]).getroot()
values=[n.attrib.get("text","") for n in root.iter()]
if "Export backup" in values:
    raise SystemExit("System create-document picker did not open")
print("PASS external create-document picker opened")
PY
adb shell input keyevent 4
sleep 1
dump_screen settings-after-picker
assert_text settings-after-picker "Export backup"
assert_text settings-after-picker "Import backup"
adb shell dumpsys package com.goreecloud.launcher > "$EVIDENCE/package.txt"
echo "PASS: HOME role, discovery/search, cross-app launch, shortcut/dock and settings persistence"
