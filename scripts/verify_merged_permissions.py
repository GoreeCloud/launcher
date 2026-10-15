#!/usr/bin/env python3
"""Check packaged Launcher permissions at exact-head CI, not installed legacy APKs."""
import sys
from pathlib import Path
from tempfile import TemporaryDirectory
from xml.etree import ElementTree

ROOT = Path(__file__).resolve().parents[1]
ANDROID_NAME = "{http://schemas.android.com/apk/res/android}name"
FORBIDDEN = frozenset({
    "android.permission.READ_SMS",
    "android.permission.READ_CALL_LOG",
    "android.permission.ACCESS_FINE_LOCATION",
    "android.permission.ACCESS_COARSE_LOCATION",
    "android.permission.ACCESS_BACKGROUND_LOCATION",
})
PERMISSION_TAGS = {"uses-permission", "uses-permission-sdk-23", "uses-permission-sdk-m"}


def rejected_permissions(xml_path: Path) -> set[str]:
    root = ElementTree.parse(xml_path).getroot()
    if root.tag != "manifest":
        raise ValueError(f"Expected Android manifest: {xml_path}")
    declared = {
        elem.attrib[ANDROID_NAME]
        for elem in root.iter()
        if elem.tag in PERMISSION_TAGS and ANDROID_NAME in elem.attrib
    }
    return declared & FORBIDDEN


def self_test() -> None:
    with TemporaryDirectory() as d:
        manifest = Path(d) / "AndroidManifest.xml"
        head = '<manifest xmlns:android="http://schemas.android.com/apk/res/android">'
        manifest.write_text(head + '<uses-permission android:name="android.permission.READ_CONTACTS"/></manifest>')
        assert not rejected_permissions(manifest)
        for permission in sorted(FORBIDDEN):
            for tag in ("uses-permission", "uses-permission-sdk-23"):
                manifest.write_text(head + f'<{tag} android:name="{permission}"/></manifest>')
                assert rejected_permissions(manifest) == {permission}, (tag, permission)
    print("PASS: merged-manifest permission rejection regression fixtures")


def verify_build(variant: str = 'debug') -> None:
    intermediates = ROOT / "build/intermediates"
    manifest_paths = sorted({
        path for pattern in ("merged_manifest*/**/AndroidManifest.xml",
                             "packaged_manifests/**/AndroidManifest.xml")
        for path in intermediates.glob(pattern)
        if f"lawnwithquickstepgithub{variant}" in str(path).lower()
    })
    if not manifest_paths:
        raise RuntimeError(f"No exact launcher {variant} merged/packaged manifest found; cannot verify permissions")
    for manifest in manifest_paths:
        violations = rejected_permissions(manifest)
        if violations:
            raise RuntimeError(f"{manifest.relative_to(ROOT)} declares prohibited legacy permissions: {sorted(violations)}")
    print(f"PASS: {len(manifest_paths)} built Launcher {variant} manifest(s) omit legacy SMS, call-log and location permissions")
    print("This checks only CI build manifests; signing, user-granted permissions and device runtime remain separate gates")


if __name__ == "__main__":
    try:
        if sys.argv[1:] == ["--self-test"]:
            self_test()
        elif sys.argv[1:] == ['--qa']:
            verify_build('qa')
        elif not sys.argv[1:]:
            verify_build()
        else:
            raise ValueError("Usage: verify_merged_permissions.py [--self-test]")
    except Exception as exc:
        print(f"FAIL: {exc}", file=sys.stderr)
        sys.exit(1)
