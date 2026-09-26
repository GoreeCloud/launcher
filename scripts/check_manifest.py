#!/usr/bin/env python3
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

m = Path(__file__).resolve().parents[1] / "app/src/main/AndroidManifest.xml"
root = ET.parse(m).getroot()
ns = "{http://schemas.android.com/apk/res/android}"

permissions = {x.attrib.get(ns + "name") for x in root.findall("uses-permission")}
allowed_permissions = {
    "android.permission.SET_WALLPAPER",
    "android.permission.REQUEST_DELETE_PACKAGES",
    "android.permission.READ_CONTACTS",
    "android.permission.READ_CALL_LOG",
    "android.permission.READ_SMS",
}
unexpected_permissions = permissions - allowed_permissions
if unexpected_permissions:
    print("Unexpected permissions:", sorted(unexpected_permissions))
    sys.exit(1)

if "android.permission.REQUEST_DELETE_PACKAGES" not in permissions:
    print("Missing REQUEST_DELETE_PACKAGES required for the user-confirmed Android uninstall flow.")
    sys.exit(1)

telephony_feature = next(
    (
        feature
        for feature in root.findall("uses-feature")
        if feature.attrib.get(ns + "name") == "android.hardware.telephony"
    ),
    None,
)
if {
    "android.permission.READ_CALL_LOG",
    "android.permission.READ_SMS",
} & permissions:
    if telephony_feature is None or telephony_feature.attrib.get(ns + "required") != "false":
        print("Telephony-backed Search permissions require optional telephony hardware declaration.")
        sys.exit(1)

text = m.read_text(encoding="utf-8")
for required in (
    "android.intent.category.HOME",
    "android.intent.category.DEFAULT",
):
    if required not in text:
        print("Missing:", required)
        sys.exit(1)

queries = root.find("queries")
if queries is None:
    print("Missing <queries> package-visibility declaration for launchable apps.")
    sys.exit(1)

has_launcher_query = False
has_index_search_query = False
for intent in queries.findall("intent"):
    action_names = {
        action.attrib.get(ns + "name")
        for action in intent.findall("action")
    }
    category_names = {
        category.attrib.get(ns + "name")
        for category in intent.findall("category")
    }
    if (
        "android.intent.action.MAIN" in action_names
        and "android.intent.category.LAUNCHER" in category_names
    ):
        has_launcher_query = True
    if "com.goreecloud.index.action.SEARCH" in action_names:
        has_index_search_query = True

allowed_visible_packages = {
    "com.google.android.apps.docs",
    "com.dropbox.android",
}
visible_packages = {
    package.attrib.get(ns + "name")
    for package in queries.findall("package")
}
unexpected_visible_packages = visible_packages - allowed_visible_packages
if unexpected_visible_packages:
    print("Unexpected package visibility:", sorted(unexpected_visible_packages))
    sys.exit(1)

if not has_launcher_query:
    print("Missing MAIN/LAUNCHER visibility query required for complete app discovery.")
    sys.exit(1)

if has_index_search_query:
    print("Legacy GoreeCloud Index search visibility query must not be required by core Launcher search.")
    sys.exit(1)

if "android.permission.QUERY_ALL_PACKAGES" in text:
    print("Broad QUERY_ALL_PACKAGES visibility is not permitted.")
    sys.exit(1)

ui = Path(__file__).resolve().parents[1] / "app/src/main/java/com/goreecloud/launcher/ui/LauncherBetaRoot.kt"
ui_text = ui.read_text(encoding="utf-8")
for forbidden_label in (
    "Open GoreeCloud Search",
    "Open GoreeCloud Index",
):
    if forbidden_label in ui_text:
        print("Legacy external-search authority label is not permitted:", forbidden_label)
        sys.exit(1)

print("Manifest and Launcher search-authority guards passed.")
