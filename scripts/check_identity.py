#!/usr/bin/env python3
import json
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]

CANONICAL_REPOSITORY = "GoreeCloud/goreecloud-branding-assets"
CANONICAL_PATH = "products/launcher/app-icon.svg"
CANONICAL_BLOB = "68ea82c4657253a1396376f715d3582f1417b9ac"
LOCAL_SOURCE = root / "branding/source/goreecloud-launcher-icon.svg"

required = [
    root / "branding/provenance.json",
    root / "branding/README.md",
    root / "docs/product-identity.md",
    root / "app/src/main/res/drawable/ic_launcher_background.xml",
    root / "app/src/main/res/drawable/ic_launcher_foreground.xml",
    root / "app/src/main/res/drawable/ic_launcher_monochrome.xml",
    root / "app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml",
    root / "app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml",
    root / "app/src/main/res/mipmap-anydpi-v33/ic_launcher.xml",
    root / "app/src/main/res/mipmap-anydpi-v33/ic_launcher_round.xml",
]
missing = [str(path.relative_to(root)) for path in required if not path.is_file()]
if missing:
    print("Missing Launcher identity/provenance files:", missing)
    sys.exit(1)

if LOCAL_SOURCE.exists():
    print(
        "Competing Launcher-local canonical artwork source is forbidden; "
        "use GoreeCloud/goreecloud-branding-assets instead."
    )
    sys.exit(1)

try:
    provenance = json.loads((root / "branding/provenance.json").read_text(encoding="utf-8"))
except (OSError, json.JSONDecodeError) as exc:
    print("Invalid Launcher branding provenance:", exc)
    sys.exit(1)

expected_provenance = {
    "canonical_repository": CANONICAL_REPOSITORY,
    "canonical_path": CANONICAL_PATH,
    "canonical_blob_sha": CANONICAL_BLOB,
    "consumer_repository": "GoreeCloud/goreecloud-launcher",
    "consumer_role": "platform-specific derivative only",
}
for key, expected in expected_provenance.items():
    if provenance.get(key) != expected:
        print(f"Launcher branding provenance mismatch for {key}: {provenance.get(key)!r}")
        sys.exit(1)

for path in required:
    if path.suffix == ".xml":
        try:
            ET.parse(path)
        except ET.ParseError as exc:
            print(f"Invalid XML in {path.relative_to(root)}: {exc}")
            sys.exit(1)

manifest = ET.parse(root / "app/src/main/AndroidManifest.xml").getroot()
application = manifest.find("application")
ns = "{http://schemas.android.com/apk/res/android}"
if application is None:
    print("Missing Android <application> element.")
    sys.exit(1)
if application.attrib.get(ns + "icon") != "@mipmap/ic_launcher":
    print("Android application icon is not wired to @mipmap/ic_launcher.")
    sys.exit(1)
if application.attrib.get(ns + "roundIcon") != "@mipmap/ic_launcher_round":
    print("Android round icon is not wired to @mipmap/ic_launcher_round.")
    sys.exit(1)

background = (root / "app/src/main/res/drawable/ic_launcher_background.xml").read_text(encoding="utf-8")
for required_token in ("#FF3B82F6", "#FF174EA6"):
    if required_token not in background:
        print("Launcher icon background lost canonical GoreeCloud color:", required_token)
        sys.exit(1)

foreground = (root / "app/src/main/res/drawable/ic_launcher_foreground.xml").read_text(encoding="utf-8")
if foreground.count("android:pathData=") != 5:
    print("Launcher foreground must preserve four tiles plus the center accent.")
    sys.exit(1)
for required_token in ("#F7FFFFFF", "#EEFFFFFF", "#E2FFFFFF", "#FF8EC5FF"):
    if required_token not in foreground:
        print("Launcher foreground lost canonical mobile icon token:", required_token)
        sys.exit(1)

monochrome = (root / "app/src/main/res/drawable/ic_launcher_monochrome.xml").read_text(encoding="utf-8")
if monochrome.count("android:pathData=") != 4:
    print("Launcher monochrome icon must preserve the four-tile identity geometry.")
    sys.exit(1)

for path in (
    root / "app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml",
    root / "app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml",
    root / "app/src/main/res/mipmap-anydpi-v33/ic_launcher.xml",
    root / "app/src/main/res/mipmap-anydpi-v33/ic_launcher_round.xml",
):
    text = path.read_text(encoding="utf-8")
    if '@drawable/ic_launcher_background' not in text or '@drawable/ic_launcher_foreground' not in text:
        print(f"{path.relative_to(root)} lost synchronized adaptive-icon wiring.")
        sys.exit(1)

for path in (
    root / "app/src/main/res/mipmap-anydpi-v33/ic_launcher.xml",
    root / "app/src/main/res/mipmap-anydpi-v33/ic_launcher_round.xml",
):
    text = path.read_text(encoding="utf-8")
    if "<monochrome" not in text or "@drawable/ic_launcher_monochrome" not in text:
        print(f"{path.relative_to(root)} is missing the themed/monochrome derivative.")
        sys.exit(1)

branding_doc = (root / "branding/README.md").read_text(encoding="utf-8")
identity_doc = (root / "docs/product-identity.md").read_text(encoding="utf-8")
for document_name, text in (("branding/README.md", branding_doc), ("docs/product-identity.md", identity_doc)):
    if CANONICAL_REPOSITORY not in text or CANONICAL_PATH not in text:
        print(f"{document_name} must preserve canonical branding authority/provenance.")
        sys.exit(1)

print("Launcher branding provenance and synchronized identity derivatives passed.")
