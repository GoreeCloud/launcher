#!/usr/bin/env bash
set -Eeuo pipefail
# Disposable emulator. No API keys, PIN writes, or changes to personal devices.
EVIDENCE=build/security-evidence
mkdir -p "$EVIDENCE"
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.goreecloud.launcher/.LauncherActivity
sleep 3
. ci/emulator-hierarchy-retry.sh
dump() {
  local name="$1"
  capture_hierarchy_with_retries "$name" "$EVIDENCE/$name.xml"
}
found() {
  python3 - "$EVIDENCE/$1.xml" "$2" <<'PY'
import sys,xml.etree.ElementTree as ET
root=ET.parse(sys.argv[1]).getroot()
name=sys.argv[2]
assert any(name==n.attrib.get(k) for n in root.iter()
           for k in ("text","content-desc")), "Missing: "+name
print("PASS",name)
PY
}
tap() {
  python3 - "$EVIDENCE/$1.xml" "$2" <<'PY'
import sys,re,subprocess,xml.etree.ElementTree as ET
root=ET.parse(sys.argv[1]).getroot()
for n in root.iter():
    if sys.argv[2] not in (n.attrib.get("text"),n.attrib.get("content-desc")): continue
    m=re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",n.attrib.get("bounds",""))
    if m:
        x1,y1,x2,y2=map(int,m.groups())
        subprocess.run(["adb","shell","input","tap",str((x1+x2)//2),str((y1+y2)//2)],check=True)
        print("PASS tapped",sys.argv[2]);break
else: raise SystemExit("Missing control: "+sys.argv[2])
PY
}
seek() {
  local label="$1"
  for i in 1 2 3 4 5 6 7 8; do
    dump current
    if grep -Fq "text=\"$label\"" "$EVIDENCE/current.xml"; then return; fi
    adb shell input swipe 470 1680 470 550 450
    sleep 1
  done
  echo "Missing settings control $label" >&2; exit 1
}
# Access private Settings through the exported launcher, never via adb.
. ci/emulator-onboarding.sh
accept_onboarding_if_present
dump launcher-home
tap launcher-home "Edit Home screen"
sleep 2
dump home-editor
tap home-editor "Launcher Settings"
sleep 2
seek "Configure AI quick answers"
tap current "Configure AI quick answers"
sleep 2
dump quick-providers
found quick-providers "‹   Quick answers"
found quick-providers "GoreeCloud Search quick answers are unavailable until a supported service API is published. No endpoint or credential has been assumed."
adb shell input keyevent 4
sleep 2
# Settings scroll position may be retained or reset across Activity navigation.
seek "Set or change App Lock PIN"
tap current "Set or change App Lock PIN"
sleep 2
dump pin-dialog
found pin-dialog "App Lock PIN"
found pin-dialog "Save PIN"
found pin-dialog "Cancel"
adb shell input keyevent 4
echo "PASS provider Settings and PIN setup affordances; no credentials were created"
