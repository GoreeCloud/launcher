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
adb shell am force-stop "$pkg" || true
adb shell am start -W -n "$pkg/app.lawnchair.LawnchairLauncher" > "$out/home-launch.txt"
sleep 12
# Refuse a false-green screenshot if Android terminated the Launcher process.
adb shell pidof "$pkg" > "$out/launcher-pid-home.txt"
adb shell screencap -p /sdcard/goreecloud-home-qa.png
adb pull /sdcard/goreecloud-home-qa.png "$out/home.png"
# Long-press intentionally targets the empty center of the synthetic Home page.
adb shell input swipe 540 1090 540 1090 1450
sleep 5
adb shell screencap -p /sdcard/goreecloud-edit-qa.png
adb pull /sdcard/goreecloud-edit-qa.png "$out/edit.png"
adb shell uiautomator dump /sdcard/goreecloud-edit.xml >/dev/null || true
adb pull /sdcard/goreecloud-edit.xml "$out/edit-hierarchy.xml" || true
adb logcat -d -t 1500 > "$out/logcat.txt"
adb shell wm size > "$out/device-size.txt"
python3 scripts/check_launcher_visual_edges.py "$out"
