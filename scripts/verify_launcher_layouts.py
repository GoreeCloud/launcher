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

# Device feedback regression: drawer defaults must not change the Home grid.
drawer_pref = (ROOT / "lawnchair/src/app/lawnchair/preferences/PreferenceManager.kt").read_text()
drawer_pref2 = (ROOT / "lawnchair/src/app/lawnchair/preferences2/PreferenceManager2.kt").read_text()
drawer_xml = ET.parse(ROOT / "lawnchair/res/values/config.xml").getroot()
drawer_icon_scale = next(
    (item.text for item in drawer_xml.findall("item")
     if item.get("name") == "config_default_drawer_icon_size_factor"), None
)
check('val drawerOpacity = FloatPref("pref_drawerOpacity", 1f, recreate)' in drawer_pref,
      "Drawer must default to fully opaque")
check('defaultSelector = { numAllAppsColumns.coerceAtLeast(5) }' in drawer_pref2,
      "Drawer default must support at least five columns")
check(drawer_icon_scale == "0.8", "Drawer icon size must default to 80 percent")
check('val workspaceColumns = IntPref("pref_workspaceColumns", calculatedGridSpec.workspaceColumns)' in drawer_pref,
      "Home grid must remain independent of drawer columns")

if failures:
    for failure in failures:
        print("FAIL:", failure)
    raise SystemExit(1)
print("PASS: new-install grids, local role-only dock, Glaze settings touch floor")
print("Device/upgrade and visual/assistive-technology QA remain pending")
