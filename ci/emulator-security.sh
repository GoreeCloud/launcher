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
assert any(name.casefold()==n.attrib.get(k,"").casefold() for n in root.iter()
           for k in ("text","content-desc")), "Missing: "+name
print("PASS",name)
PY
}
tap() {
  python3 - "$EVIDENCE/$1.xml" "$2" "${3:-tap}" <<'PY'
import sys,re,subprocess,xml.etree.ElementTree as ET
root=ET.parse(sys.argv[1]).getroot()
for n in root.iter():
    if sys.argv[2] not in (n.attrib.get("text"),n.attrib.get("content-desc")): continue
    m=re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",n.attrib.get("bounds",""))
    if m:
        x1,y1,x2,y2=map(int,m.groups())
        x,y=str((x1+x2)//2),str((y1+y2)//2)
        cmd=(["adb","shell","input","swipe",x,y,x,y,"1100"]
             if sys.argv[3]=="long" else ["adb","shell","input","tap",x,y])
        subprocess.run(cmd,check=True)
        print("PASS",sys.argv[3],sys.argv[2]);break
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
adb shell am start -n com.goreecloud.launcher/.LauncherActivity
sleep 2
dump lock-home
tap lock-home "Phone" long
sleep 2
dump lock-menu
found lock-menu "Lock"
tap lock-menu "Lock"
sleep 2
dump lock-pin-required
found lock-pin-required "App Lock PIN"
# Exercise one disposable test-only PIN. Never use or request a real user PIN.
tap lock-pin-required "New PIN"
adb shell input text 2468
sleep 1
dump pin-first-filled
tap pin-first-filled "Confirm PIN"
adb shell input text 2468
adb shell input keyevent 4
sleep 1
dump pin-ready-to-save
tap pin-ready-to-save "SAVE PIN"
sleep 2
dump app-locked-home
found app-locked-home "Phone"
tap app-locked-home "Phone"
sleep 2
dump pin-unlock-required
found pin-unlock-required "Unlock app"
found pin-unlock-required "UNLOCK"
# Incorrect PIN must not launch the protected app.
tap pin-unlock-required "4–6 digit PIN"
adb shell input text 0000
adb shell input keyevent 4
sleep 1
dump pin-wrong-ready
tap pin-wrong-ready "UNLOCK"
sleep 2
dump pin-wrong-still-blocked
found pin-wrong-still-blocked "Unlock app"
# Correct PIN opens the selected app through the protected launcher path.
tap pin-wrong-still-blocked "4–6 digit PIN"
adb shell input text 2468
adb shell input keyevent 4
sleep 1
dump pin-correct-ready
tap pin-correct-ready "UNLOCK"
sleep 2
dump after-correct-unlock
python3 - "$EVIDENCE/after-correct-unlock.xml" <<'PY'
import sys,xml.etree.ElementTree as ET
nodes=list(ET.parse(sys.argv[1]).getroot().iter())
if any(n.attrib.get("text")=="Unlock app" for n in nodes):
    raise SystemExit("Correct PIN did not close the unlock gate")
print("PASS test-only PIN rejected incorrect entry and accepted correct entry")
PY
echo "PASS launcher-originated App Lock smoke with disposable test PIN"
# A locked app must also appear in the dedicated Settings manager.
adb shell am start -n com.goreecloud.launcher/.LauncherActivity
sleep 2
dump app-lock-manager-home
tap app-lock-manager-home "Edit Home screen"
sleep 2
dump app-lock-manager-editor
tap app-lock-manager-editor "Launcher Settings"
sleep 2
seek "Manage locked apps"
tap current "Manage locked apps"
sleep 2
dump app-lock-manager-dialog
found app-lock-manager-dialog "Manage locked apps"
python3 - "$EVIDENCE/app-lock-manager-dialog.xml" <<'PY'
import sys,xml.etree.ElementTree as ET
root=ET.parse(sys.argv[1]).getroot()
items=[n.attrib.get("text","") for n in root.iter()]
if not any(x.endswith(" — Unlock") for x in items):
    raise SystemExit("Locked app missing from App Lock manager")
print("PASS Settings lists locked app with an unlock action")
PY
