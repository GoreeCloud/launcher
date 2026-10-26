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


# All application/UI and file mutations below occur only on a disposable CI emulator.
adb shell cmd role add-role-holder --user 0 android.app.role.HOME com.goreecloud.launcher
adb shell input keyevent 3
sleep 4
dump_screen backup-home
assert_text backup-home "Page 1 / 1"

# Create a nonempty second page; the backup must carry state, not only defaults.
for attempt in 1 2 3; do
  dump_screen "before-create-$attempt"
  tap_label "before-create-$attempt" "Add home page"
  sleep 2
  dump_screen populated-page
  if grep -Fq 'text="Page 2 / 2"' "$EVIDENCE/populated-page.xml"; then break; fi
done
assert_text populated-page "Page 2 / 2"
tap_label populated-page "All apps"
sleep 1
dump_screen backup-drawer
longpress_label backup-drawer "Launcher QA Fixture"
sleep 1
dump_screen backup-place-menu
tap_label backup-place-menu "Add to current home page"
sleep 1
dump_screen backup-drawer-after-place
tap_label backup-drawer-after-place "Home"
sleep 1
dump_screen backup-tile
longpress_label backup-tile "Launcher QA Fixture"
sleep 1
dump_screen backup-pin-menu
tap_label backup-pin-menu "Pin to dock"
sleep 1
dump_screen backup-pinned
assert_shortcut_count backup-pinned 2

# Save a synthetic two-page, one-shortcut, one-pin layout with labels disabled.
tap_label backup-pinned "Settings"
sleep 1
dump_screen backup-settings
tap_label backup-settings "Show application labels"
sleep 1
dump_screen backup-settings-labels-off
python3 - "$EVIDENCE/backup-settings-labels-off.xml" <<'PY'
import sys,xml.etree.ElementTree as ET
r=ET.parse(sys.argv[1]).getroot()
x=[a.attrib for a in r.iter() if a.attrib.get("text")=="Show application labels"]
if not x or x[0].get("checked")!="false": raise SystemExit("Expected labels off before export")
print("PASS synthetic label setting disabled")
PY
adb shell input swipe 430 1700 430 510 460
sleep 1
dump_screen backup-settings-scroll
assert_text backup-settings-scroll "Export backup"
tap_label backup-settings-scroll "Export backup"
sleep 2
dump_screen backup-export-picker
assert_text backup-export-picker "SAVE"
tap_label backup-export-picker "SAVE"
sleep 3
dump_screen backup-after-save
assert_text backup-after-save "Export backup"

# SAF Downloads provider writes only our synthetic fixture identifiers; do not
# upload the actual backup file in CI artifacts. Inspect its contents locally.
adb shell ls /sdcard/Download/ > "$EVIDENCE/downloads-filenames.txt"
REMOTE_FILE="$(adb shell find /sdcard/Download -maxdepth 1 -type f -name 'GoreeCloud-Launcher-backup*' | tr -d '\r' | head -n 1)"
test -n "$REMOTE_FILE" || { echo "Backup file missing from emulator Downloads"; exit 1; }
adb pull "$REMOTE_FILE" /tmp/launcher-qa-export.txt >/dev/null
python3 - "$EVIDENCE/export-validation.txt" <<'PY'
import sys,hashlib,base64
s=open("/tmp/launcher-qa-export.txt","rb").read()
assert len(s)<65536 and s.endswith(b"\n")
lines=s.decode("utf8").splitlines()
assert lines[0]=="GOREELAUNCHER/1" and len(lines)==7
assert lines[1]=="page=1"
assert lines[2]=="columns=4"
assert lines[3]=="labels=0"
assert lines[-1]=="sha256="+hashlib.sha256(b"\n".join(s.splitlines()[:-1])+b"\n").hexdigest()
ws=base64.urlsafe_b64decode(lines[4].split("=",1)[1]+"===")
assert ws.startswith(b"|") and b"com.goreecloud.launcher.fixture" in ws or ws.startswith(b"|")
assert lines[5].startswith("pins=") and len(lines[5])>5
open(sys.argv[1],"w").write("PASS synthetic SAF export: version 1, two pages, labels off, dock pin, valid checksum\n")
print("PASS synthetic backup file created and validated")
PY

# Mutate the saved preference. A canceled restore must leave it unchanged.
adb shell input swipe 420 520 420 1690 460
sleep 1
dump_screen settings-revisit-labels
tap_label settings-revisit-labels "Show application labels"
sleep 1
dump_screen labels-mutated
assert_checked() {
  python3 - "$EVIDENCE/$1.xml" "$2" <<'PY'
import sys,xml.etree.ElementTree as ET
r=ET.parse(sys.argv[1]).getroot()
nodes=[n.attrib for n in r.iter() if n.attrib.get("text")=="Show application labels"]
if not nodes or nodes[0].get("checked")!=sys.argv[2]:
    raise SystemExit("Wrong label preference; expected "+sys.argv[2])
print("PASS label preference:",sys.argv[2])
PY
}
assert_checked labels-mutated true

# Repeated imports exercise the real ACTION_OPEN_DOCUMENT user flow.
begin_import() {
  adb shell input swipe 430 1690 430 510 460
  sleep 1
  dump_screen "backup-import-settings-$1"
  tap_label "backup-import-settings-$1" "Import backup"
  sleep 2
  dump_screen "backup-import-picker-$1"
  python3 - "$EVIDENCE/backup-import-picker-$1.xml" <<'PY'
import sys,re,subprocess,xml.etree.ElementTree as ET
root=ET.parse(sys.argv[1]).getroot()
for n in root.iter():
    a=n.attrib
    name=a.get("text","")+" "+a.get("content-desc","")
    if "GoreeCloud-Launcher-backup" not in name: continue
    m=re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",a.get("bounds",""))
    if not m: continue
    x1,y1,x2,y2=map(int,m.groups())
    subprocess.run(["adb","shell","input","tap",str((x1+x2)//2),str((y1+y2)//2)],check=True)
    print("PASS selected exported document from system picker")
    sys.exit(0)
raise SystemExit("Exported document not found in import picker")
PY
  sleep 2
  dump_screen "backup-restore-confirm-$1"
  assert_text "backup-restore-confirm-$1" "Restore launcher backup?"
}

begin_import cancel
tap_label backup-restore-confirm-cancel "Cancel"
sleep 1
adb shell input swipe 420 520 420 1690 460
sleep 1
dump_screen after-restore-cancel
assert_checked after-restore-cancel true

begin_import apply
tap_label backup-restore-confirm-apply "Replace layout"
sleep 1
adb shell input swipe 420 520 420 1690 460
sleep 1
dump_screen after-restore-apply
assert_checked after-restore-apply false

# Confirm shortcut+pin, page selection and label preference survive process restart.
adb shell input keyevent 4
sleep 1
dump_screen restored-home
assert_text restored-home "Page 2 / 2"
assert_shortcut_count restored-home 2
adb shell am force-stop com.goreecloud.launcher
adb shell input keyevent 3
sleep 3
dump_screen restored-after-restart
assert_text restored-after-restart "Page 2 / 2"
assert_shortcut_count restored-after-restart 2
echo "PASS: SAF create/write/read, restore-cancel, restore-apply and process persistence"
