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
    for retry in 1 2 3 4 5 6 7 8; do
      if grep -Fq "text=\"Setup $step of 3\"" "$f"; then break; fi
      sleep 2
      adb shell uiautomator dump /sdcard/goreecloud-onboarding.xml >/dev/null 2>&1 || true
      adb pull /sdcard/goreecloud-onboarding.xml "$f" >/dev/null 2>&1 || true
    done
    grep -Fq "text=\"Setup $step of 3\"" "$f" || {
      echo "Onboarding step $step did not appear" >&2
      return 1
    }
    # Preserve actual emulator screenshots of every onboarding page for
    # manual GC UI audit, not only XML assertions.
    mkdir -p build/emulator-evidence
    adb shell screencap -p /sdcard/goreecloud-onboarding-step.png
    adb pull /sdcard/goreecloud-onboarding-step.png "build/emulator-evidence/onboarding-step-$step.png" >/dev/null
    cp "$f" "build/emulator-evidence/onboarding-step-$step.xml"
    # On a 200%-font emulator, the pinned action row must not cover
    # scrollable instructions; then verify the last note remains reachable.
    local font_scale
    font_scale=$(adb shell settings get system font_scale | tr -d '\r')
    if [ "$step" = 3 ] && python3 - "$font_scale" <<'PYSCALE'
import sys
try:
    enabled = float(sys.argv[1]) >= 1.6
except ValueError:
    enabled = False
sys.exit(0 if enabled else 1)
PYSCALE
    then
      python3 - "$f" <<'PYBOUNDS'
import re,sys,xml.etree.ElementTree as ET
nodes=list(ET.parse(sys.argv[1]).getroot().iter())
def bounds(node):
    m=re.fullmatch(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]',node.attrib.get("bounds",""))
    if not m: raise SystemExit("Missing onboarding UI bounds")
    return tuple(map(int,m.groups()))
scrollers=[n for n in nodes if n.attrib.get("class")=="android.widget.ScrollView"]
buttons=[n for n in nodes if n.attrib.get("text") in ("Back","Skip","Finish")]
if len(scrollers)!=1 or len(buttons)!=3:
    raise SystemExit("Missing pinned onboarding navigation")
visible_bottom=bounds(scrollers[0])[3]
button_top=min(bounds(n)[1] for n in buttons)
if visible_bottom>button_top:
    raise SystemExit(f"Onboarding text viewport overlaps action footer: {visible_bottom}>{button_top}")
print("PASS onboarding scroll viewport ends before pinned footer")
PYBOUNDS
      adb shell input swipe 540 1580 540 680 450
      sleep 1
      adb shell uiautomator dump /sdcard/goreecloud-onboarding-scroll.xml >/dev/null 2>&1
      adb pull /sdcard/goreecloud-onboarding-scroll.xml \
        build/emulator-evidence/onboarding-step-3-scrolled.xml >/dev/null
      adb shell screencap -p /sdcard/goreecloud-onboarding-scrolled.png
      adb pull /sdcard/goreecloud-onboarding-scrolled.png \
        build/emulator-evidence/onboarding-step-3-scrolled.png >/dev/null
      python3 - build/emulator-evidence/onboarding-step-3-scrolled.xml <<'PYNOTE'
import re,sys,xml.etree.ElementTree as ET
nodes=list(ET.parse(sys.argv[1]).getroot().iter())
def rect(n):
    m=re.fullmatch(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]',n.attrib.get("bounds",""))
    if not m: raise SystemExit("Unreadable onboarding bounds")
    return tuple(map(int,m.groups()))
scroll=next(n for n in nodes if n.attrib.get("class")=="android.widget.ScrollView")
note=next((n for n in nodes if n.attrib.get("text","").startswith("Your setup choices stay on device.")),None)
finish=next((n for n in nodes if n.attrib.get("text")=="Finish"),None)
if note is None or finish is None: raise SystemExit("Onboarding final note or Finish disappeared")
if rect(note)[3]>rect(scroll)[3] or rect(note)[1]<rect(scroll)[1]:
    raise SystemExit("Final onboarding privacy note not fully reachable above footer")
if rect(finish)[1]<rect(scroll)[3]:
    raise SystemExit("Finish overlays the scroll viewport")
print("PASS onboarding final note readable with pinned Finish at 200% text")
PYNOTE
      # Use the freshly captured hierarchy for the Finish tap after scrolling.
      cp build/emulator-evidence/onboarding-step-3-scrolled.xml "$f"
    fi
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
