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



# The fixture and HOME assignment are disposable-emulator-only bootstrapping,
# NOT acceptance evidence for user-selected HOME (covered in another suite).
adb shell cmd role add-role-holder --user 0 android.app.role.HOME com.goreecloud.launcher
adb shell input keyevent 3
sleep 4
dump_screen responsive-initial
assert_text responsive-initial "Home workspace"
assert_text responsive-initial "Home tab, selected"
assert_text responsive-initial "All apps tab"
tap_label responsive-initial "All apps"
sleep 1
dump_screen responsive-drawer
assert_text responsive-drawer "All apps tab, selected"
assert_text responsive-drawer "Home tab"
longpress_label responsive-drawer "Launcher QA Fixture"
sleep 1
dump_screen responsive-menu
assert_text responsive-menu "Add to current home page"
tap_label responsive-menu "Add to current home page"
sleep 1
dump_screen responsive-after-add
tap_label responsive-after-add "Home"
sleep 1
dump_screen responsive-home
assert_text responsive-home "Launcher QA Fixture"
assert_text responsive-home "Home tab, selected"

assert_png_orientation() {
  python3 - "$EVIDENCE/$1.png" "$2" <<'PY'
import sys,struct
path,expected=sys.argv[1:]
b=open(path,'rb').read(24)
if b[:8]!=b'\x89PNG\r\n\x1a\n': raise SystemExit("Not a PNG")
w,h=struct.unpack('>II',b[16:24])
observed="landscape" if w>h else "portrait"
if observed!=expected: raise SystemExit("Expected "+expected+", got "+str((w,h)))
print("PASS rendered screenshot orientation",observed,w,h)
PY
}
assert_tile_label_bounds() {
  python3 - "$EVIDENCE/$1.xml" "$2" <<'PY'
import re,subprocess,sys,xml.etree.ElementTree as ET
path,scale=sys.argv[1],float(sys.argv[2])
root=ET.parse(path).getroot()
tile=None
for node in root.iter():
    if node.attrib.get("content-desc")=="Launcher QA Fixture":
        for sub in node.iter():
            if sub.attrib.get("text")=="Launcher QA Fixture":
                tile=node
                break
    if tile is not None: break
if tile is None: raise SystemExit("Fixture Home tile or label not reachable")
def rect(node):
    m=re.fullmatch(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]',node.attrib.get("bounds",""))
    if not m: raise SystemExit("No bounds")
    return tuple(map(int,m.groups()))
outer=rect(tile)
txt=next(x for x in tile.iter() if x.attrib.get("text")=="Launcher QA Fixture")
label=rect(txt)
density=int(re.search(r'\d+',subprocess.check_output(["adb","shell","wm","density"]).decode()).group())/160
expected_label=min(3,max(1,scale))*40*density
if scale>=1.75 and outer[2]-outer[0]<140*density:
    raise SystemExit("Large text Home tile too narrow for readable labels: "+str(outer))
if label[3]-label[1] < expected_label*0.8:
    raise SystemExit("Label height too small for accessibility scale: "+str((label,expected_label)))
if label[1]<outer[1] or label[3]>outer[3]:
    raise SystemExit("Text label out of tile bounds: "+str((label,outer)))
print("PASS scaled label fits tile",label,"tile",outer)
PY
}
# Do not alter users' devices. All configuration changes occur on the CI AVD.
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1
adb shell settings put system font_scale 1.45
adb shell am force-stop com.goreecloud.launcher
adb shell input keyevent 3
sleep 5
dump_screen responsive-landscape-top
assert_png_orientation responsive-landscape-top landscape
assert_text responsive-landscape-top "Home workspace"
# Landscape has vertically scrolling Home surface; the dock must be reachable.
adb shell input swipe 680 770 680 175 560
sleep 2
dump_screen responsive-landscape-bottom
assert_text responsive-landscape-bottom "Pinned apps"
# A hierarchy entry can be partly hidden behind Android's bottom system bar.
# Scroll farther, then require both the heading and empty dock hint fully inside
# the visible display instead of trusting UIAutomator text presence alone.
adb shell input swipe 680 780 680 145 600
sleep 2
dump_screen responsive-landscape-dock
assert_text responsive-landscape-dock "Pinned apps"
assert_text responsive-landscape-dock "Long-press an app to pin it here"
python3 - "$EVIDENCE/responsive-landscape-dock.xml" "$EVIDENCE/responsive-landscape-dock.png" <<'PY'
import re,struct,sys,xml.etree.ElementTree as ET
root=ET.parse(sys.argv[1]).getroot()
img=open(sys.argv[2],"rb").read(24)
width,height=struct.unpack(">II",img[16:24])
for name in ("Pinned apps","Long-press an app to pin it here"):
    matches=[n.attrib for n in root.iter() if n.attrib.get("text")==name]
    if len(matches)!=1: raise SystemExit("Cannot find unique dock element "+name)
    m=re.fullmatch(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]',matches[0].get("bounds",""))
    if not m: raise SystemExit("Missing dock element bounds")
    x1,y1,x2,y2=map(int,m.groups())
    if x1<0 or x2>width or y1 < height*0.06 or y2 > height*0.88:
        raise SystemExit("Dock element clipped by status/navigation bars: "+name+" "+str((x1,y1,x2,y2)))
    print("PASS landscape dock safely visible",name,(x1,y1,x2,y2))
PY
# Two-line tile labels must be visible/reachable after scrolling.
python3 - "$EVIDENCE/responsive-landscape-top.xml" "$EVIDENCE/responsive-landscape-bottom.xml" <<'PY'
import sys,xml.etree.ElementTree as ET
all_nodes=[]
for p in sys.argv[1:]: all_nodes+=list(ET.parse(p).getroot().iter())
if not any(n.attrib.get("content-desc")=="Launcher QA Fixture" for n in all_nodes):
    raise SystemExit("Landscape fixture shortcut not reachable in scroll hierarchy")
print("PASS landscape shortcut reachable")
PY
adb shell settings put system user_rotation 0
adb shell settings put system font_scale 2.0
adb shell am force-stop com.goreecloud.launcher
adb shell input keyevent 3
sleep 5
dump_screen responsive-large-top
assert_png_orientation responsive-large-top portrait
assert_text responsive-large-top "Home workspace"
adb shell input swipe 630 1670 630 410 600
sleep 2
dump_screen responsive-large-content
# Home is scrollable at 200% font. Label and dock must be reachable.
assert_text responsive-large-content "Launcher QA Fixture"
assert_tile_label_bounds responsive-large-content 2.0
adb shell input swipe 630 1670 630 380 600
sleep 1
dump_screen responsive-large-dock
assert_text responsive-large-dock "Pinned apps"
# Return default configuration; the fixture shortcut must survive.
adb shell settings put system user_rotation 0
adb shell settings put system font_scale 1.0
adb shell am force-stop com.goreecloud.launcher
adb shell input keyevent 3
sleep 3
dump_screen responsive-restored
assert_text responsive-restored "Home workspace"
assert_text responsive-restored "Launcher QA Fixture"
echo "PASS: landscape, 145%- and 200%-text Home access, tile geometry, persistence"
