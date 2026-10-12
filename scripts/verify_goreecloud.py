#!/usr/bin/env python3
"""Development source assertions, not device or security acceptance."""
from pathlib import Path
import subprocess
import sys
ROOT = Path(__file__).resolve().parents[1]
errors = []
def check(ok, msg):
    if not ok: errors.append(msg)
def read(p): return (ROOT/p).read_text()
build, settings = read("build.gradle"), read("settings.gradle")
tokens = read("lawnchair/src/app/lawnchair/ui/theme/GlazeContract.kt")
check('rootProject.name = "goreecloud-launcher"' in settings, "Gradle identity")
check("com.gradle.develocity" not in settings and "buildScan" not in settings, "Build scan uploader")
for package in ["com.goreecloud.launcher", "com.goreecloud.launcher.nightly", "com.goreecloud.launcher.play"]:
    check(package in build, "Application ID " + package)
check("applicationId 'app.lawnchair'" not in build, "Upstream package identity")
check("releaseSigning = signingConfigs.debug" not in build, "Release signing never falls back to debug")
check("} catch (FileNotFoundException ignored)" in build, "Missing signing only is recoverable")
check("signingConfig signingConfigs.debug" in build, "Debug builds use isolated debug signing")
check("signingConfig releaseSigning" in build.split("release {", 1)[1].split("}", 1)[0], "Release builds use release-only signing")
check('isDefault true' in build.split("github {",1)[1].split("}",1)[0], "Canonical GitHub flavor is default")
check('<string name="config_default_qsb_search_provider_id" translatable="false">app_search</string>' in read("lawnchair/res/values/config.xml"), "Local-only search default")
qsb=read("lawnchair/src/app/lawnchair/qsb/providers/QsbSearchProvider.kt")
check(".filterNot { it.sponsored }" in qsb, "Sponsored providers are filtered")
check("onSponsorDisclaimerClick" not in read("lawnchair/src/app/lawnchair/ui/preferences/destinations/SearchProviderPreferences.kt"), "No sponsored presentation")
clickable=read("lawnchair/src/app/lawnchair/ui/preferences/components/layout/ClickableIcon.kt")
check("glazeMetrics.minimumTouchTarget" in clickable, "Glaze actual touch target applied")
check("contentDescription = contentDescription" in clickable, "Accessibility label forwarded")
check("search_provider_install_action" in read("lawnchair/src/app/lawnchair/ui/preferences/destinations/SearchProviderPreferences.kt"), "Search install action labeled")
check("firstOrNull { LawnQsbLayout.resolveIntent" not in qsb, "No external provider auto-detection")
check("Invalid and unavailable defaults must fall back to local app search" in qsb, "Local-first search fail-closed fallback")
manifest=read("lawnchair/AndroidManifest.xml")
for obsolete in ("READ_HISTORY_BOOKMARKS", "WRITE_HISTORY_BOOKMARKS"):
    check(obsolete not in manifest, "Obsolete permission: "+obsolete)
check('derived_app_name", "Launcher"' in build, "Short installed label")
check('const val stableVersion = "1.7.0"' in tokens, "Glaze stable target")
check("minimumTouchTarget: Dp = 48.dp" in tokens, "Glaze touch target contract")
check("LocalGlazeMetrics provides GlazeAndroidContract.metrics" in read("lawnchair/src/app/lawnchair/ui/theme/Theme.kt"), "Glaze theme provider")
check("GlazeAndroidContract.preferenceSurface" in read("lawnchair/src/app/lawnchair/ui/theme/Color.kt"), "Glaze settings surface")
check((ROOT/"LICENSE.txt").exists(), "Upstream license retained")
check("4793ba3a238bb1a3e786a3471a6d84767ed05267" in read("docs/UPSTREAM_PROVENANCE.md"), "Upstream provenance pin")
check(sorted(p.name for p in (ROOT/".github/workflows").glob("*.yml")) == ["goreecloud-ci.yml"], "No unreviewed YAML workflows")
try:
    entry = subprocess.check_output(["git", "ls-files", "--stage", "platform_frameworks_libs_systemui"], cwd=ROOT, text=True)
    check("160000 7d9e92bda50359dc66a530248e15f9748343524e" in entry, "Submodule pin")
except Exception as e: errors.append("Submodule evidence unavailable: " + str(e))
if errors:
    for issue in errors: print("FAIL:", issue, file=sys.stderr)
    sys.exit(1)
print("PASS: identity, upstream pin, Glaze mapping, source-level privacy baseline")
print("Runtime, signing, device accessibility and full privacy audits remain pending.")
