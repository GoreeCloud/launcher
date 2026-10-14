#!/usr/bin/env python3
"""Source-only GoreeCloud Launcher first-run layout and Glaze contract checks.

This does not establish device, accessibility, migration, or release acceptance.
"""
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ATTR = "{http://schemas.android.com/apk/res-auto}"
WORKSPACE_ATTR = "{http://schemas.android.com/apk/res-auto/com.android.launcher3}"
failures = []


def check(value, message):
    if not value:
        failures.append(message)


grids = ET.parse(ROOT / "res/xml/device_profiles.xml").getroot().findall("grid-option")
by_name = {}
for item in grids:
    name = item.get(ATTR + "name")
    check(name not in by_name, "Duplicate grid name: " + str(name))
    by_name[name] = item

for name in ("5_by_5", "5_by_6", "4_by_4"):
    check(name in by_name, "Required fallback grid missing: " + name)

if "5_by_6" in by_name:
    grid = by_name["5_by_6"]
    for key, expected in {
        "numRows": "6",
        "numColumns": "5",
        "numHotseatIcons": "5",
        "dbFile": "launcher_5_by_6.db",
        "defaultLayoutId": "@xml/default_workspace_5x6",
    }.items():
        check(grid.get(ATTR + key) == expected, f"5x6 {key} must be {expected}")
    small = [x for x in grid.findall("display-option")
             if x.get(ATTR + "name") == "Shorter Stubby"]
    check(bool(small) and all(x.get(ATTR + "canBeDefault") == "false" for x in small),
          "Six-row grid must not auto-select on short phones")

if "5_by_5" in by_name:
    grid = by_name["5_by_5"]
    check(grid.get(ATTR + "dbFile") == "launcher.db", "Preserve original layout database")
    check(grid.get(ATTR + "defaultLayoutId") == "@xml/default_workspace_5x5",
          "Preserve existing 5x5 layout")
    check(any(x.get(ATTR + "canBeDefault") == "true" for x in grid.findall("display-option")),
          "Preserve 5x5 fallback for smaller screens")

for folder in ("res/xml", "lawnchair/res/xml"):
    root = ET.parse(ROOT / folder / "default_workspace_5x6.xml").getroot()
    dock = root.findall("resolve")
    check(len(dock) == 5, f"{folder} needs five role-based dock suggestions")
    check(sorted(x.get(WORKSPACE_ATTR + "screen") for x in dock) == list("01234"),
          f"{folder} needs exactly one item per dock slot")
    for entry in dock:
        check(entry.get(WORKSPACE_ATTR + "container") == "-101", f"{folder} default is dock only")
        check(len(entry.findall("favorite")) > 0, f"{folder} unresolved empty dock role")
        for fav in entry.findall("favorite"):
            uri = fav.get(WORKSPACE_ATTR + "uri", "")
            check(uri.startswith("#Intent;") or uri in ("tel:123", "sms:"),
                  f"{folder} must not place remote or vendor default shortcuts")
            check(fav.get(WORKSPACE_ATTR + "packageName") is None,
                  f"{folder} must not pin vendor packages")

for folder in ("res/xml", "lawnchair/res/xml"):
    legacy_home = (ROOT / folder / "default_workspace_5x5.xml").read_text()
    check("example.com" not in legacy_home, f"{folder} must not seed web fallbacks")
    check("APP_MARKET" not in legacy_home and "market://" not in legacy_home,
          f"{folder} must not pin an app store or market destination")

template = (ROOT / "lawnchair/src/app/lawnchair/ui/preferences/components/layout/PreferenceTemplate.kt").read_text()
check("defaultMinSize(minHeight = LocalGlazeMetrics.current.minimumTouchTarget)" in template,
      "Glaze touch-target minimum must be applied to settings rows")
group = (ROOT / "lawnchair/src/app/lawnchair/ui/preferences/components/layout/PreferenceGroup.kt").read_text()
check("RoundedCornerShape(LocalGlazeMetrics.current.cardRadius)" in group,
      "Glaze card radius must shape preference group containers")

# GoreeCloud first-run five-column workspace, dock, and app drawer defaults.
# Existing stored preferences remain independent and are not rewritten.
drawer_pref = (ROOT / "lawnchair/src/app/lawnchair/preferences/PreferenceManager.kt").read_text()
drawer_pref2 = (ROOT / "lawnchair/src/app/lawnchair/preferences2/PreferenceManager2.kt").read_text()
drawer_xml = ET.parse(ROOT / "lawnchair/res/values/config.xml").getroot()
drawer_icon_scale = next(
    (item.text for item in drawer_xml.findall("item")
     if item.get("name") == "config_default_drawer_icon_size_factor"), None
)
check('val drawerOpacity = FloatPref("pref_drawerOpacity", 1f, recreate)' in drawer_pref,
      "Drawer must default to fully opaque")
for pref_key in ('drawer_columns', 'drawer_columns_unfolded'):
    prefix = 'key = intPreferencesKey(name = "' + pref_key + '"),'
    section = drawer_pref2.split(prefix, 1)
    check(len(section) == 2 and 'defaultSelector = { 5 }' in section[1].split(')', 1)[0],
          f"{pref_key} must default to exactly five columns")
check(drawer_icon_scale == "0.8", "Drawer icon size must default to 80 percent")
for entry in (
    'isPhone -> LayoutConfig(5, 5, 6)',
    'isTablet -> LayoutConfig(5, 5, 5)',
    'isFoldable -> LayoutConfig(5, 5, 6, 5)',
    'isDesktop -> LayoutConfig(5, 5, 5)',
    'else -> LayoutConfig(5, 5, 7)',
):
    check(entry in drawer_pref, f"Device type grid default should be five columns: {entry}")
check('val workspaceColumns = IntPref("pref_workspaceColumns", calculatedGridSpec.workspaceColumns)' in drawer_pref,
      "Home grid preferences must remain independent from app drawer choices")
check('val hotseatColumns = IntPref("pref_hotseatColumns", calculatedGridSpec.hotseatColumns, reloadGrid)' in drawer_pref,
      "Hotseat preferences must remain independent from app drawer choices")
for grid_name in ('5_by_5', '5_by_6'):
    grid = by_name.get(grid_name)
    if grid is not None:
        check(grid.get(ATTR + 'numColumns') == '5', f"{grid_name} needs five workspace columns")
        check(grid.get(ATTR + 'numHotseatIcons') == '5', f"{grid_name} needs five hotseat icons")

# GoreeCloud first-run Home and wallpaper catalog invariants.
config = ET.parse(ROOT / "lawnchair/res/values/config.xml").getroot()
check(any(v.get("name") == "config_default_enable_smartspace" and v.text == "false"
          for v in config.findall("bool")), "Home must not seed Smartspace")
check(any(v.get("name") == "config_default_hotseat_mode" and v.text == "disabled"
          for v in config.findall("string")), "Dock must not show search")
root_view = (ROOT / "lawnchair/src/app/lawnchair/LawnchairLauncher.kt").read_text()
wallpaper_menu = (ROOT / "src/com/android/launcher3/views/OptionsPopupView.java").read_text()
check("class GoreeCloudWallpaperView" in root_view and
      "GoreeCloudWallpapers.showPicker" in wallpaper_menu,
      "Wallpaper catalog must use GoreeCloud assets and picker")
check("Intent.ACTION_SET_WALLPAPER)" not in wallpaper_menu,
      "Launcher must not launch the external wallpaper catalog")
check('showWallpaperCarousel = false' in root_view,
      "Wallpaper history carousel must remain disabled")
for folder in ("res/xml", "lawnchair/res/xml"):
    for name in ("default_workspace_5x5.xml", "default_workspace_5x6.xml"):
        layout = ET.parse(ROOT / folder / name).getroot()
        check(not any(item.get(WORKSPACE_ATTR + "container") == "-100" for item in layout),
              f"{folder}/{name} must seed no Home app shortcuts")
        dock = [item for item in layout if item.get(WORKSPACE_ATTR + "container") == "-101"]
        check(len(dock) == 5, f"{folder}/{name} must seed five dock icons")

# Glaze dock should be a light rounded capsule behind only the icon band,
# rather than a full-screen-area hotseat background. Existing custom settings remain.
config = ET.parse(ROOT / "lawnchair/res/values/config.xml").getroot()
config_strings = {v.get("name"): v.text for v in config.findall("string")}
check(config_strings.get("config_default_hotseat_bg_color") == "custom|#FFF1F7FF",
      "GoreeCloud dock should default to a pale Glaze surface")
check(config_strings.get("config_default_icon_shape") == "roundedSquare",
      "GoreeCloud icons must default to rounded-square masks")
prefs = (ROOT / "lawnchair/src/app/lawnchair/preferences/PreferenceManager.kt").read_text()
check('val hotseatBG = BoolPref("pref_hotseatBG", true, recreate)' in prefs,
      "New installs must show the Glaze dock capsule")
check('val hotseatBGAlpha = IntPref("pref_hotseatBGTransparency", 86, recreate)' in prefs,
      "Dock surface must retain intended frost opacity")
hotseat = (ROOT / "src/com/android/launcher3/Hotseat.java").read_text()
check("mIconsContainer.setBackground(bg);" in hotseat,
      "Dock surface must render behind icon band, not the QSB/navigation area")
check("if (grid.isVerticalBarLayout())" in hotseat and
      "mIconsContainer.setBackground(null);" in hotseat,
      "Vertical hotseat must not inherit the phone dock capsule")
check("background.setStroke(strokeWidth" in hotseat,
      "Dock capsule must retain the subtle Glaze highlight edge")
check("bgColor instanceof ColorOption.Default" in hotseat,
      "Only the inherited default color must use the Glaze fallback; honor transparent custom colors")
check("Color.alpha(baseColor) == 0" not in hotseat,
      "Never override an explicitly selected transparent custom dock color")

config = ET.parse(ROOT / "lawnchair/res/values/config.xml").getroot()
dimensions = {node.get("name"): (node.text or "").strip() for node in config.findall("item")}
check(dimensions.get("config_default_home_icon_size_factor") == "0.78", "Compact icons should be default")
check(dimensions.get("config_default_hotseat_bottom_factor") == "0.70", "Compact dock height should be default")
for folder in ("res/xml", "lawnchair/res/xml"):
    workspace = ET.parse(ROOT / folder / "default_workspace_5x5.xml").getroot()
    check(len(workspace.findall("resolve")) == 5 and not workspace.findall("favorite"), "5x5 fallback must contain only dock")
setup = (ROOT / "lawnchair/src/app/lawnchair/GoreeCloudHomeOnboarding.kt").read_text()
setup_ui = (ROOT / "lawnchair/src/app/lawnchair/GoreeCloudSetupActivity.kt").read_text()
for label in ("No Apps", "10 Recently Used Apps", "10 Most Used Apps"):
    check(label in setup_ui, "Missing onboarding choice: " + label)
check("hasUsageAccess(launcher)" in setup, "Usage based selection must honor user permission")
check("reserved.add(position)" in setup, "Onboarding must reserve each placement")
dashboard = (ROOT / "lawnchair/src/app/lawnchair/ui/preferences/destinations/PreferencesDashboard.kt").read_text()
check("        AnnouncementPreference()" not in dashboard, "Dashboard must not show inherited announcements")

if failures:
    for failure in failures:
        print("FAIL:", failure)
    raise SystemExit(1)
print("PASS: new-install grids, local role-only dock, Glaze settings touch floor")
print("Device/upgrade and visual/assistive-technology QA remain pending")
