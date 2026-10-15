#!/usr/bin/env bash
set -euo pipefail
out="${GITHUB_WORKSPACE:-$(pwd)}/visual-qa"
mkdir -p "$out"
apk="$(find build/outputs/apk -name '*.apk' -type f | head -n 1)"
test -n "$apk"
pkg='com.goreecloud.launcher.debug'
adb wait-for-device
adb shell wm size 1080x2400
adb shell wm density 420
adb shell settings put system screen_off_timeout 2147483647
adb shell cmd overlay enable --user 0 com.android.internal.systemui.navbar.gestural || true
adb install -r "$apk"
# Debug-only synthetic preferences; never use or copy real user settings.
cat > "$out/goreecloud_home_onboarding.xml" <<'EOF'
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map><boolean name="onboarding_finished" value="true" /></map>
EOF
adb push "$out/goreecloud_home_onboarding.xml" /data/local/tmp/goreecloud_home_onboarding.xml
adb shell chmod 644 /data/local/tmp/goreecloud_home_onboarding.xml
adb shell run-as "$pkg" mkdir -p shared_prefs
adb shell run-as "$pkg" cp /data/local/tmp/goreecloud_home_onboarding.xml shared_prefs/goreecloud_home_onboarding.xml
# The stock Pixel Launcher can show an ANR dialog over the app under test.
# Select GoreeCloud as HOME on this disposable emulator before capture.
adb shell cmd role add-role-holder android.app.role.HOME "$pkg" 0
adb shell cmd role get-role-holders android.app.role.HOME > "$out/home-role.txt"
grep -F "$pkg" "$out/home-role.txt"
adb shell am force-stop com.google.android.apps.nexuslauncher || true
adb shell input keyevent KEYCODE_BACK || true
adb shell am force-stop "$pkg" || true
adb shell am start -W -n "$pkg/app.lawnchair.LawnchairLauncher" > "$out/home-launch.txt"
sleep 12
# Refuse a false-green screenshot if Android terminated the Launcher process.
adb shell pidof "$pkg" > "$out/launcher-pid-home.txt"
adb shell screencap -p /sdcard/goreecloud-home-qa.png
adb pull /sdcard/goreecloud-home-qa.png "$out/home.png"
adb shell uiautomator dump /sdcard/goreecloud-home.xml >/dev/null || true
adb pull /sdcard/goreecloud-home.xml "$out/home-hierarchy.xml" || true
# Long-press intentionally targets the empty center of the synthetic Home page.
adb shell input swipe 540 1090 540 1090 1450
sleep 5
adb shell screencap -p /sdcard/goreecloud-edit-qa.png
adb pull /sdcard/goreecloud-edit-qa.png "$out/edit.png"
adb shell uiautomator dump /sdcard/goreecloud-edit.xml >/dev/null || true
adb pull /sdcard/goreecloud-edit.xml "$out/edit-hierarchy.xml"
# Exercise Add page in the disposable emulator, then capture adjacent pages.
add_coords="$(python3 - "$out/edit-hierarchy.xml" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
for node in root.iter("node"):
    if node.attrib.get("content-desc") == "Add a Home screen":
        x1, y1, x2, y2 = map(int, re.findall(r"[0-9]+", node.attrib["bounds"]))
        print((x1 + x2) // 2, (y1 + y2) // 2)
        break
else:
    raise SystemExit("Add-page control is not exposed in the UI hierarchy")
PY
)"
read -r add_x add_y <<< "$add_coords"
adb shell logcat -c
adb shell input tap "$add_x" "$add_y"
sleep 4
adb shell logcat -d -s GoreeEditQA:I > "$out/add-page-events.txt"
if ! grep -q "add-click" "$out/add-page-events.txt"; then
  echo "Add-page tap was not received by GoreeCloud Launcher"
  exit 1
fi
python3 - "$out/add-page-events.txt" <<'PY'
import re, sys
log = open(sys.argv[1]).read()
before = re.search(r"add-click pages=(\d+)", log)
after = re.search(r"add-result pages=(\d+)", log)
if not before or not after or int(after.group(1)) <= int(before.group(1)):
    raise SystemExit("Add-page handler did not increase the Workspace page count")
PY
adb shell screencap -p /sdcard/goreecloud-edit-added-qa.png
adb pull /sdcard/goreecloud-edit-added-qa.png "$out/edit-added.png"
adb shell uiautomator dump /sdcard/goreecloud-edit-added.xml >/dev/null
adb pull /sdcard/goreecloud-edit-added.xml "$out/edit-added-hierarchy.xml"
adb logcat -d -t 1500 > "$out/logcat.txt"
adb shell wm size > "$out/device-size.txt"
python3 scripts/check_launcher_visual_edges.py "$out"

# Exercise the new functional Add apps picker only on the disposable emulator,
# after preserving the Home/edit/add-page evidence above.
picker_coords="$(python3 - "$out/edit-added-hierarchy.xml" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
nodes = ET.parse(sys.argv[1]).getroot().iter("node")
for node in nodes:
    if node.get("content-desc") == "Add apps" and node.get("clickable") == "true":
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds", "")))
        print((x1 + x2) // 2, (y1 + y2) // 2)
        break
else:
    raise SystemExit("Edit Home Add apps button is not actionable")
PY
)"
read -r picker_x picker_y <<< "$picker_coords"
adb shell input tap "$picker_x" "$picker_y"
sleep 2
adb shell screencap -p /sdcard/goreecloud-edit-picker-qa.png
adb pull /sdcard/goreecloud-edit-picker-qa.png "$out/edit-app-picker.png"
adb shell uiautomator dump /sdcard/goreecloud-edit-picker.xml >/dev/null
adb pull /sdcard/goreecloud-edit-picker.xml "$out/edit-app-picker-hierarchy.xml"
python3 - "$out/edit-app-picker-hierarchy.xml" <<'PY'
import sys
import xml.etree.ElementTree as ET
nodes = ET.parse(sys.argv[1]).getroot().iter("node")
visible = " ".join((n.get("text", "") + " " + n.get("content-desc", "")) for n in nodes)
if "Add apps to Home" not in visible:
    raise SystemExit("Add apps picker did not open in the emulator")
PY

# Verify that choosing an installed app actually inserts a Home shortcut.
# This is synthetic emulator workspace data, never a real user's Home.
app_choice="$(python3 - "$out/edit-app-picker-hierarchy.xml" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
for node in ET.parse(sys.argv[1]).getroot().iter("node"):
    if node.get("text") == "Calendar":
        coords = list(map(int, re.findall(r"\d+", node.get("bounds", ""))))
        if len(coords) == 4:
            a, b, c, d = coords
            print((a + c) // 2, (b + d) // 2)
            break
else:
    raise SystemExit("Disposable emulator lacks the expected Calendar app")
PY
)"
read -r calendar_x calendar_y <<< "$app_choice"
adb shell input tap "$calendar_x" "$calendar_y"
sleep 4
adb shell uiautomator dump /sdcard/goreecloud-edit-app-added.xml >/dev/null
adb pull /sdcard/goreecloud-edit-app-added.xml "$out/edit-app-added-hierarchy.xml"
python3 - "$out/edit-app-added-hierarchy.xml" <<'PY'
import sys
import xml.etree.ElementTree as ET
nodes = list(ET.parse(sys.argv[1]).getroot().iter("node"))
if any(n.get("text") == "Add apps to Home" for n in nodes):
    raise SystemExit("Add apps dialog did not close after selecting an app")
if not any("Calendar" in (n.get("content-desc", "") + " " + n.get("text", ""))
           for n in nodes):
    raise SystemExit("Selecting Calendar did not create a visible Home shortcut")
PY

# The owner's reference shows neighboring previews on both sides. Confirm
# that three real pages can be created and page two can be centered.
third_add="$(python3 - "$out/edit-added-hierarchy.xml" <<'PY'
import re, sys
import xml.etree.ElementTree as ET
for n in ET.parse(sys.argv[1]).getroot().iter("node"):
    if n.get("content-desc") == "Add a Home screen" and n.get("clickable") == "true":
        a, b, c, d = map(int, re.findall(r"\d+", n.get("bounds", "")))
        print((a+c)//2, (b+d)//2)
        break
else:
    raise SystemExit("Add-page control missing after first page creation")
PY
)"
read -r third_x third_y <<< "$third_add"
adb shell input tap "$third_x" "$third_y"
sleep 3
adb shell uiautomator dump /sdcard/goreecloud-edit-three.xml >/dev/null
adb pull /sdcard/goreecloud-edit-three.xml "$out/edit-three-hierarchy.xml"
middle_page="$(python3 - "$out/edit-three-hierarchy.xml" <<'PY'
import re, sys
import xml.etree.ElementTree as ET
nodes = {n.get("content-desc", ""): n for n in ET.parse(sys.argv[1]).getroot().iter("node")}
if "Preview Home page 3" not in nodes:
    raise SystemExit("Third Home page was not created")
node = nodes.get("Preview Home page 2")
if node is None or node.get("clickable") != "true":
    raise SystemExit("Page two is not navigable")
a,b,c,d = map(int, re.findall(r"\d+", node.get("bounds", "")))
print((a+c)//2, (b+d)//2)
PY
)"
read -r middle_x middle_y <<< "$middle_page"
adb shell input tap "$middle_x" "$middle_y"
sleep 3
adb shell screencap -p /sdcard/goreecloud-edit-three-center.png
adb pull /sdcard/goreecloud-edit-three-center.png "$out/edit-three-center.png"
adb shell uiautomator dump /sdcard/goreecloud-edit-three-center.xml >/dev/null
adb pull /sdcard/goreecloud-edit-three-center.xml "$out/edit-three-center-hierarchy.xml"
python3 - "$out/edit-three-center-hierarchy.xml" <<'PY'
import sys
import xml.etree.ElementTree as ET
nodes = ET.parse(sys.argv[1]).getroot().iter("node")
if not any(n.get("content-desc") == "Preview Home page 2" and
           n.get("selected") == "true" for n in nodes):
    raise SystemExit("Page two failed to center as the selected preview")
PY
