#!/usr/bin/env python3
"""Source contracts for the 2026-10-14 Launcher screenshot regressions."""
from pathlib import Path
import xml.etree.ElementTree as ET

r = Path(__file__).resolve().parents[1]
read = lambda name: (r / name).read_text()
cfg = ET.parse(r / "lawnchair/res/values/config.xml").getroot()
settings = {e.get("name"): (e.text or "").strip() for e in cfg}
assert settings["config_default_hide_app_drawer_search_bar"] == "true"
assert settings["config_default_show_suggested_apps_at_drawer_top"] == "false"

launcher = read("lawnchair/src/app/lawnchair/LawnchairLauncher.kt")
assert "applyGoreeCloudSystemBarWallpaper()" in launcher
assert "rootView.addView(\n            GoreeCloudWallpaperView" in launcher
assert "targetWindow.statusBarColor = android.graphics.Color.TRANSPARENT" in launcher
assert "targetWindow.navigationBarColor = android.graphics.Color.TRANSPARENT" in launcher
assert "targetWindow.isNavigationBarContrastEnforced = false" in launcher

setup = read("lawnchair/src/app/lawnchair/GoreeCloudSetupActivity.kt")
assert "WindowInsetsCompat.Type.systemBars()" in setup
assert "Recently Used" in setup and "Most Used" in setup and "No Apps" in setup

home = read("lawnchair/src/app/lawnchair/GoreeCloudHomeOnboarding.kt")
assert "MAX_ROWS = 2" in home and "managed_ids" in home
assert "reserved.add(position)" in home
assert "recordLaunch(" in home and "ranking_started" in setup
assert "clearLegacyStarterPair" in home and 'info.cellX == 3' in home
assert "deleteCollectionAndContentsFromDatabase" in home

ns = "{http://schemas.android.com/apk/res-auto/com.android.launcher3}"
for path in (r / "lawnchair/res/xml").glob("default_workspace*.xml"):
    tree = ET.parse(path).getroot()
    assert all(node.get(ns + "container") == "-101" for node in tree), path

search = read("lawnchair/src/app/lawnchair/GoreeCloudUniversalSearchActivity.kt")
assert search.count(".take(columns * 2)") == 3
assert "setMargins(dp(14), dp(74), dp(14), 0)" in search
assert "profileBadgePosition.get()" not in search
assert "R.drawable.goreecloud_ic_search" in search
assert "dp(207)" in search
assert "this.background = shape(Color.rgb(239, 243, 248), 30)" in search
assert "clearButton.visibility = if (query.isEmpty()) View.GONE" in search
assert ET.parse(r / "lawnchair/res/drawable/goreecloud_ic_search.xml").getroot().tag == "vector"
assert "migrateGoreeCloudDrawerDefaults()" in launcher
assert "drawer_search_hidden_v3" in launcher
assert "hideAppDrawerSearchBar.set(true)" in launcher
assert "showSuggestedAppsInDrawer.set(false)" in launcher
edit = read("lawnchair/src/app/lawnchair/views/GoreeCloudOverviewToolbar.kt")
assert "Set current page as Home" in edit and "Remove current empty Home screen" in edit
assert "host.rootView.addView" in edit
assert "canvas.drawRoundRect" in read("lawnchair/src/app/lawnchair/views/GoreeCloudOverviewFrames.kt")
assert "trashLeft" in read("lawnchair/src/app/lawnchair/views/GoreeCloudOverviewFrames.kt")
assert "params.bottomMargin = dp(94)" in launcher
assert "InsettableFrameLayout.LayoutParams" in launcher
assert "ignoreInsets = true" in launcher
assert "getEditModeWorkspaceScaleAndTranslation(launcher).scale" in read("src/com/android/launcher3/states/EditModeState.kt")
assert "host.hotseat.getGlobalVisibleRect" in read("lawnchair/src/app/lawnchair/views/GoreeCloudOverviewFrames.kt")
assert "WeakHashMap<LawnchairLauncher, View>" in read("lawnchair/src/app/lawnchair/views/GoreeCloudOverviewFrames.kt")
assert "ignoreInsets = true" in edit

assert all(("R.drawable.goreecloud_edit_" + n) in launcher for n in ("wallpaper", "widgets", "settings"))
assert 'if (item.isDefault) "⌂" else "●"' in read("lawnchair/src/app/lawnchair/views/EditModePageStrip.kt")
assert "GoreeCloudOverviewToolbar.INSTANCE.refresh(host)" in read("src/com/android/launcher3/Workspace.java")
badge = read("src/com/android/launcher3/GoreeCloudProfileBadges.java")
assert "icon.mDisplay != BubbleTextView.DISPLAY_WORKSPACE" in badge
assert 'drawText(work ? "W" : "P"' not in badge
assert "canvas.drawRoundRect" in badge

surface = ET.parse(r / "res/drawable/popup_background.xml").getroot()
assert surface.find("stroke") is not None
assert ET.parse(r / "res/drawable/goreecloud_launcher_foreground.xml").getroot().find("group") is not None
print("PASS: screenshot UX source contracts; real-device validation still required")
