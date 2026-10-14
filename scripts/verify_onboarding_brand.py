#!/usr/bin/env python3
"""Source contracts for GoreeCloud first-run onboarding and app branding."""
import xml.etree.ElementTree as ET
import hashlib
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
app = (ROOT / "lawnchair/src/app/lawnchair/GoreeCloudSetupActivity.kt").read_text()
launcher = (ROOT / "lawnchair/src/app/lawnchair/LawnchairLauncher.kt").read_text()
legacy = (ROOT / "lawnchair/src/app/lawnchair/GoreeCloudHomeOnboarding.kt").read_text()
res = (ROOT / "res/values/strings.xml").read_text()
assert "class GoreeCloudSetupActivity : ComponentActivity()" in app
assert '"Skip"' in app and '"Back"' in app and '"Continue"' in app and '"Finish"' in app
assert all(word in app for word in ("No Apps","10 Recently Used Apps","10 Most Used Apps"))
assert all(word in app for word in ("Universal Search","Privacy & finish","Appearance"))
assert "onSaveInstanceState" in app and '"onboarding_finished"' in app
assert "lifecycleScope.launch" in app and "finish()" in app
assert "android.app.AlertDialog" not in legacy and "setCancelable(false)" not in legacy
assert "GoreeCloudSetupActivity::class.java" in launcher
manifest = (ROOT / "lawnchair/AndroidManifest.xml").read_text()
assert 'android:name="app.lawnchair.GoreeCloudSetupActivity"' in manifest
assert 'android:exported="false"' in manifest
assert "<string name=\"app_name\">GoreeCloud Launcher</string>" in res
ns = "{http://schemas.android.com/apk/res/android}"
for item in ("res/drawable/ic_launcher_home.xml", "res/mipmap-anydpi-v26/ic_launcher_home.xml", "res/mipmap-anydpi-v26/ic_launcher_home_round.xml"):
    root = ET.parse(ROOT / item).getroot()
    assert root.tag == "adaptive-icon"
    assert root.find("foreground").get(ns+"drawable") == "@drawable/goreecloud_launcher_foreground"
    assert root.find("background").get(ns+"drawable") == "@drawable/goreecloud_launcher_background"
    assert root.find("monochrome").get(ns+"drawable") == "@drawable/goreecloud_launcher_monochrome"
for item in ("goreecloud_launcher_background","goreecloud_launcher_foreground","goreecloud_launcher_monochrome"):
    ET.parse(ROOT / "res/drawable" / (item+".xml"))
assert b"GoreeCloud Launcher" in (ROOT / "res/raw/goreecloud_launcher_canonical.svg").read_bytes()
print("PASS: official GoreeCloud Launcher icon and onboarding")
