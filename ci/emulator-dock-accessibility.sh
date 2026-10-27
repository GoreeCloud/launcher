#!/usr/bin/env bash
set -Eeuo pipefail
# Disposable CI Android emulator only, after emulator-dock.sh has pinned/reordered fixtures.
EVIDENCE="build/emulator-evidence"
mkdir -p "$EVIDENCE"
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
result="$(adb shell am instrument -w -r \
  com.goreecloud.launcher.test/com.goreecloud.launcher.test.DockActionProbe)"
printf '%s\n' "$result" | tee "$EVIDENCE/dock-a11y-action-result.txt"
printf '%s\n' "$result" | grep -Fq 'INSTRUMENTATION_CODE: -1'
printf '%s\n' "$result" | grep -Fq 'PASS custom accessibility actions in both directions'
# The probe deliberately leaves the dock in a different order.
adb shell am force-stop com.goreecloud.launcher
adb shell input keyevent 3
sleep 3
. ci/emulator-onboarding.sh
accept_onboarding_if_present
. ci/emulator-hierarchy-retry.sh
capture_hierarchy_with_retries dock-a11y-restart "$EVIDENCE/dock-a11y-restart.xml"
python3 - "$EVIDENCE/dock-a11y-restart.xml" <<'PY'
import re,sys,xml.etree.ElementTree as ET
expected=["Launcher QA Fixture","Launcher Fixture Notes","Launcher Fixture Beta",
          "Launcher Fixture Gamma","Launcher Fixture Delta"]
tiles=[]
for node in ET.parse(sys.argv[1]).getroot().iter():
    label=node.attrib.get("content-desc")
    if label not in expected: continue
    m=re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",node.attrib.get("bounds",""))
    if m: tiles.append((int(m.group(1)),label))
order=[label for _,label in sorted(tiles)]
if order!=expected: raise SystemExit("Custom a11y dock action not persisted: "+str(order))
print("PASS direct accessibility-node movement survives Launcher process restart")
PY
