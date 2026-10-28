#!/usr/bin/env bash
set -Eeuo pipefail
. ci/launcher-gestures.sh
# Fresh GitHub-hosted emulator only; never execute role changes on physical/personal devices.
EVIDENCE="build/emulator-evidence"
mkdir -p "$EVIDENCE"
trap 'adb shell dumpsys activity activities > "$EVIDENCE/activity-on-error.txt" || true' ERR
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r qa-fixture/build/outputs/apk/debug/qa-fixture-debug.apk

dump_screen_once() {
  local name="$1"
  adb shell uiautomator dump /sdcard/launcher-hierarchy.xml
  adb pull /sdcard/launcher-hierarchy.xml "$EVIDENCE/$name.xml" >/dev/null
  adb shell screencap -p /sdcard/launcher-screen.png
  adb pull /sdcard/launcher-screen.png "$EVIDENCE/$name.png" >/dev/null
}
dump_screen() {
  for attempt in 1 2 3 4 5; do
    rm -f "$EVIDENCE/$1.xml" "$EVIDENCE/$1.png"
    adb shell rm -f /sdcard/launcher-hierarchy.xml /sdcard/launcher-screen.png >/dev/null 2>&1 || true
    if dump_screen_once "$1" && grep -q "<hierarchy" "$EVIDENCE/$1.xml" \
       && file "$EVIDENCE/$1.png" | grep -q "PNG image data"; then return 0; fi
    sleep 2
  done
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
    # UiAutomator emits on-screen coordinates as [x1,y1][x2,y2].
    # Parse numeric bounds directly; never discard an otherwise clickable
    # node because of a brittle full-string formatting assumption.
    coords = re.findall(r"[0-9]+", n.attrib.get("bounds",""))
    if len(coords) != 4: continue
    x1,y1,x2,y2=map(int,coords)
    x,y=(x1+x2)//2,(y1+y2)//2
    if x<0 or y<0 or x2<=x1 or y2<=y1: continue
    cmd=["adb","shell","input"]
    cmd += (["swipe",str(x),str(y),str(x),str(y),"1000"] if mode=="long"
            else ["tap",str(x),str(y)])
    subprocess.run(cmd,check=True)
    print("PASS",mode,target,"at",x,y,"bounds",n.attrib.get("bounds"))
    sys.exit(0)
raise SystemExit("UI element not found: "+target + " (matching nodes: " + str(len(matching)) + ")")
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


. ci/emulator-menu-retry.sh

# Disposable emulator only: the shell role assignment here is bootstrapping
# dock QA, not evidence of end-user Home role selection (separately tested).
adb shell cmd role add-role-holder --user 0 android.app.role.HOME com.goreecloud.launcher
adb shell input keyevent 3
sleep 4
. ci/emulator-onboarding.sh
accept_onboarding_if_present
dump_screen dock-start
assert_text dock-start "Live Home clock and date"
# The redesigned launcher deliberately provides starter dock apps. Remove
# ONLY their pins through native accessible menus before testing five fixture
# slots and capacity, without editing app private storage or bypassing UI.
for target in Phone Dialer Messages Messaging Gmail Email Outlook Browser Chrome Firefox Internet Camera; do
  key=${target,,}
  dump_screen "starter-$key"
  if grep -Fq "content-desc=\"$target\"" "$EVIDENCE/starter-$key.xml"; then
    retry_longpress_menu "clear-starter-$key" "$target" "Remove from dock"
    tap_label "clear-starter-$key" "Remove from dock"
    sleep 1
  fi
done
launcher_swipe_up
# Fresh AVD startup can swallow the initial tap while the task regains focus.
# Require selected-tab semantics, retrying from the current hierarchy bounds.
for attempt in 1 2 3 4; do
  sleep 2
  dump_screen dock-drawer
  if grep -Fq 'content-desc="Launcher QA Fixture"' "$EVIDENCE/dock-drawer.xml"; then break; fi
  echo "All apps not yet active; retry $attempt"
  launcher_swipe_up
done
assert_text dock-drawer "Launcher QA Fixture"
assert_text dock-drawer "Launcher QA Fixture"
assert_text dock-drawer "Launcher Fixture Notes"
for app in "Launcher QA Fixture" "Launcher Fixture Notes" \
    "Launcher Fixture Alpha" "Launcher Fixture Beta" "Launcher Fixture Gamma"; do
  retry_longpress_menu pin-context "$app" "Pin to dock"
  tap_label pin-context "Pin to dock"
  sleep 1
done
dump_screen five-pinned-drawer
# Return Home: only the dock contains these fixture tiles.
launcher_swipe_down
sleep 1
dump_screen dock-at-capacity
python3 - "$EVIDENCE/dock-at-capacity.xml" <<'PY'
import sys,xml.etree.ElementTree as ET
root=ET.parse(sys.argv[1]).getroot()
expected=["Launcher QA Fixture","Launcher Fixture Notes","Launcher Fixture Alpha","Launcher Fixture Beta","Launcher Fixture Gamma"]
counts={v:sum(n.attrib.get("content-desc")==v for n in root.iter()) for v in expected}
if not all(counts[x]==1 for x in expected):
    raise SystemExit("Expected five unique pinned tiles: "+str(counts))
assert all(n.attrib.get("class")=="android.widget.Button" and n.attrib.get("clickable")=="true" for n in root.iter() if n.attrib.get("content-desc") in expected), "Dock tile missing Button role"
print("PASS five accessible Button dock pins",counts)
PY

# Attempt a sixth and ensure the full-dock guard refuses mutation.
launcher_swipe_up
sleep 1
retry_longpress_menu sixth-context "Launcher Fixture Delta" "Pin to dock"
tap_label sixth-context "Pin to dock"
sleep 1
dump_screen full-drawer
launcher_swipe_down
sleep 1
dump_screen after-full-attempt
python3 - "$EVIDENCE/after-full-attempt.xml" <<'PY'
import sys,xml.etree.ElementTree as ET
nodes=list(ET.parse(sys.argv[1]).getroot().iter())
expected=["Launcher QA Fixture","Launcher Fixture Notes","Launcher Fixture Alpha","Launcher Fixture Beta","Launcher Fixture Gamma"]
counts={v:sum(n.attrib.get("content-desc")==v for n in nodes) for v in expected}
extras=sum(n.attrib.get("content-desc")=="Launcher Fixture Delta" for n in nodes)
if any(v!=1 for v in counts.values()) or extras:
    raise SystemExit("Over-capacity dock mutation: "+str(counts)+" extra="+str(extras))
print("PASS sixth pin denied without altering five existing pins")
PY

# Remove existing pin via Home dock, then add previously denied app.
retry_longpress_menu unpin-menu "Launcher Fixture Alpha" "Remove from dock"
tap_label unpin-menu "Remove from dock"
sleep 1
dump_screen after-unpin
python3 - "$EVIDENCE/after-unpin.xml" <<'PY'
import sys,xml.etree.ElementTree as ET
nodes=list(ET.parse(sys.argv[1]).getroot().iter())
aliases=[n.attrib.get("content-desc") for n in nodes]
if aliases.count("Launcher Fixture Alpha")!=0:
    raise SystemExit("Unpin action did not remove Alpha")
if sum(aliases.count(n) for n in
       ["Launcher QA Fixture","Launcher Fixture Notes","Launcher Fixture Beta","Launcher Fixture Gamma"])!=4:
    raise SystemExit("Unpin disturbed other dock slots")
print("PASS unpin removed exactly one dock entry")
PY
launcher_swipe_up
sleep 1
retry_longpress_menu repin-menu "Launcher Fixture Delta" "Pin to dock"
tap_label repin-menu "Pin to dock"
sleep 1
dump_screen before-home-final
launcher_swipe_down
sleep 1
dump_screen dock-replaced
python3 - "$EVIDENCE/dock-replaced.xml" <<'PY'
import sys,xml.etree.ElementTree as ET
nodes=list(ET.parse(sys.argv[1]).getroot().iter())
names=["Launcher QA Fixture","Launcher Fixture Notes","Launcher Fixture Beta","Launcher Fixture Gamma","Launcher Fixture Delta"]
counts={v:sum(n.attrib.get("content-desc")==v for n in nodes) for v in names}
removed=sum(n.attrib.get("content-desc")=="Launcher Fixture Alpha" for n in nodes)
if any(v!=1 for v in counts.values()) or removed:
    raise SystemExit("After-replace dock mismatch: "+str(counts)+" removed="+str(removed))
print("PASS replacement pin uses freed slot")
PY

adb shell am force-stop com.goreecloud.launcher
adb shell input keyevent 3
sleep 3
dump_screen dock-after-restart
python3 - "$EVIDENCE/dock-after-restart.xml" <<'PY'
import sys,xml.etree.ElementTree as ET
nodes=list(ET.parse(sys.argv[1]).getroot().iter())
names=["Launcher QA Fixture","Launcher Fixture Notes","Launcher Fixture Beta","Launcher Fixture Gamma","Launcher Fixture Delta"]
counts={v:sum(n.attrib.get("content-desc")==v for n in nodes) for v in names}
removed=sum(n.attrib.get("content-desc")=="Launcher Fixture Alpha" for n in nodes)
if any(v!=1 for v in counts.values()) or removed:
    raise SystemExit("Dock restore/persistence mismatch "+str(counts)+" removed="+str(removed))
print("PASS dock capacity, unpin, replacement and persistence after restart")
PY

# Verify real on-screen dock order and accessible reorder actions after restart.
# The fixture labels identify five distinct app components, not screenshots of
# synthetic tiles. Inaccessible/missing pins must not affect visible order.
retry_longpress_menu move-dock-menu "Launcher Fixture Delta" "Move left in dock"
tap_label move-dock-menu "Move left in dock"
sleep 1
dump_screen dock-after-move
python3 - "$EVIDENCE/dock-after-move.xml" <<'PY'
import re,sys,xml.etree.ElementTree as ET
names=["Launcher QA Fixture","Launcher Fixture Notes","Launcher Fixture Beta",
       "Launcher Fixture Delta","Launcher Fixture Gamma"]
nodes=list(ET.parse(sys.argv[1]).getroot().iter())
tiles=[]
for node in nodes:
    name=node.attrib.get("content-desc")
    if name not in names: continue
    bounds=node.attrib.get("bounds","")
    m=re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",bounds)
    if m: tiles.append((int(m.group(1)),name))
ordered=[name for _,name in sorted(tiles)]
if ordered != names: raise SystemExit("Dock move incorrect: "+str(ordered))
print("PASS real dock tile order after moving Delta left",ordered)
PY
adb shell am force-stop com.goreecloud.launcher
adb shell input keyevent 3
sleep 3
dump_screen dock-after-move-restart
python3 - "$EVIDENCE/dock-after-move-restart.xml" <<'PY'
import re,sys,xml.etree.ElementTree as ET
expected=["Launcher QA Fixture","Launcher Fixture Notes","Launcher Fixture Beta",
          "Launcher Fixture Delta","Launcher Fixture Gamma"]
tiles=[]
for node in ET.parse(sys.argv[1]).getroot().iter():
    name=node.attrib.get("content-desc")
    if name not in expected: continue
    m=re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",node.attrib.get("bounds",""))
    if m: tiles.append((int(m.group(1)),name))
ordered=[name for _,name in sorted(tiles)]
if ordered != expected: raise SystemExit("Dock order not persisted: "+str(ordered))
print("PASS custom dock order survives process restart")
PY


# Glass Lens integration: actual Settings taps and Home screenshots, not only
# parsing a saved string. Every change must survive returning to Home.
open_dock_appearance() {
  local context="$1" attempt screenshot
  dump_screen "glass-home-before-$context"
  launcher_swipe_up
  sleep 1
  dump_screen "glass-drawer-$context"
  tap_label "glass-drawer-$context" "Launcher Settings"
  sleep 1
  for attempt in 1 2 3 4 5 6; do
    screenshot="glass-settings-$context-$attempt"
    dump_screen "$screenshot"
    if grep -Fq 'content-desc="Dock appearance:' "$EVIDENCE/$screenshot.xml"; then
      tap_label "$screenshot" "Dock appearance: $context"
      sleep 1
      dump_screen "glass-choices-$context"
      assert_text "glass-choices-$context" "Frosted Glass"
      assert_text "glass-choices-$context" "Clear Glass"
      assert_text "glass-choices-$context" "Solid"
      return 0
    fi
    adb shell input swipe 450 1640 450 610 380
    sleep 1
  done
  echo "Dock appearance setting unreachable on Android emulator" >&2
  return 1
}
choose_dock_material() {
  local before="$1" selected="$2"
  open_dock_appearance "$before"
  tap_label "glass-choices-$before" "$selected"
  sleep 1
  adb shell input keyevent 4
  sleep 1
  dump_screen "glass-return-$selected"
  assert_text "glass-return-$selected" "Search apps"
  launcher_swipe_down
  sleep 1
  dump_screen "glass-dock-$selected"
  assert_text "glass-dock-$selected" "Live Home clock and date"
  python3 - "$EVIDENCE/glass-dock-$selected.xml" <<'PYGLASS'
import sys, xml.etree.ElementTree as ET
expected={"Launcher QA Fixture", "Launcher Fixture Notes",
          "Launcher Fixture Beta", "Launcher Fixture Delta",
          "Launcher Fixture Gamma"}
visible={n.attrib.get("content-desc") for n in
         ET.parse(sys.argv[1]).getroot().iter()}
missing=expected-visible
if missing: raise SystemExit("Glass dock lost pinned fixtures: "+str(sorted(missing)))
print("PASS glass dock kept all five pinned launchable apps")
PYGLASS
}
choose_dock_material "Frosted Glass" "Clear Glass"
choose_dock_material "Clear Glass" "Solid"
choose_dock_material "Solid" "Frosted Glass"
echo "PASS Frosted/Clear/Solid selection, persistence and Home dock capture"

# Optional labels must remain legible on the default light glass material.
launcher_swipe_up
sleep 1
dump_screen glass-labels-drawer
tap_label glass-labels-drawer "Launcher Settings"
sleep 1
found_labels=false
for attempt in 1 2 3 4 5 6; do
  dump_screen "glass-labels-settings-$attempt"
  if grep -Fq 'text="Show dock icon labels"' "$EVIDENCE/glass-labels-settings-$attempt.xml"; then
    tap_label "glass-labels-settings-$attempt" "Show dock icon labels"
    found_labels=true
    break
  fi
  adb shell input swipe 450 1640 450 610 380
  sleep 1
done
if [ "$found_labels" != true ]; then
  echo "Dock icon label toggle unreachable" >&2
  exit 1
fi
adb shell input keyevent 4
sleep 1
launcher_swipe_down
sleep 1
dump_screen glass-frosted-with-labels
assert_text glass-frosted-with-labels "Launcher QA Fixture"
assert_text glass-frosted-with-labels "Launcher Fixture Notes"
echo "PASS native glass dock labels are visible with Frosted material"
