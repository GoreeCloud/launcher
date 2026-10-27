#!/usr/bin/env bash
# Fresh-install QA handshake: onboarding is part of product, not a blocking dialog.
# Exercise all three pages and finish, retaining defaults for regression runs.
accept_onboarding_if_present() {
  local f=/tmp/goreecloud-onboarding.xml step label point
  for attempt in 1 2 3; do
    adb shell uiautomator dump /sdcard/goreecloud-onboarding.xml >/dev/null 2>&1 || true
    adb pull /sdcard/goreecloud-onboarding.xml "$f" >/dev/null 2>&1 || true
    if grep -Fq 'text="Setup 1 of 3"' "$f" 2>/dev/null; then break; fi
    sleep 1
  done
  if ! grep -Fq 'text="Setup 1 of 3"' "$f" 2>/dev/null; then return 0; fi
  grep -Fq 'text="Your Home, your way"' "$f"
  echo "PASS first-run onboarding opened"
  for step in 1 2 3; do
    adb shell uiautomator dump /sdcard/goreecloud-onboarding.xml >/dev/null 2>&1
    adb pull /sdcard/goreecloud-onboarding.xml "$f" >/dev/null 2>&1
    grep -Fq "text=\"Setup $step of 3\"" "$f"
    label=Next
    [ "$step" = 3 ] && label=Finish
    point=$(python3 - "$f" "$label" <<'PY'
import re,sys,xml.etree.ElementTree as ET
root=ET.parse(sys.argv[1]).getroot()
for n in root.iter():
    if n.attrib.get("text")==sys.argv[2]:
        m=re.fullmatch(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]',n.attrib.get("bounds",""))
        if m:
            a,b,c,d=map(int,m.groups())
            print((a+c)//2,(b+d)//2)
            break
else: raise SystemExit("Onboarding action unavailable: "+sys.argv[2])
PY
)
    adb shell input tap $point
    sleep 2
  done
  echo "PASS three-step first-run onboarding completed"
}
