#!/usr/bin/env python3
"""GoreeCloud Universal Search source-level regression contracts.

This does not replace Android compilation, physical-device gesture verification,
accessibility checks, or privacy/network observation.
"""
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
failures = []
def file(path):
    return (ROOT / path).read_text(encoding="utf-8")
def check(condition, detail):
    if not condition:
        failures.append(detail)

prefs = file("lawnchair/src/app/lawnchair/preferences2/PreferenceManager2.kt")
gesture = file("lawnchair/src/app/lawnchair/gestures/VerticalSwipeTouchController.kt")
handler = file("lawnchair/src/app/lawnchair/gestures/handlers/OpenAppSearchGestureHandler.kt")
dashboard = file("lawnchair/src/app/lawnchair/ui/preferences/destinations/PreferencesDashboard.kt")
routes = file("lawnchair/src/app/lawnchair/ui/preferences/navigation/PreferenceRoutes.kt")
drawer = file("lawnchair/src/app/lawnchair/ui/preferences/components/search/DrawerSearchPreferences.kt")
search = file("lawnchair/src/app/lawnchair/ui/preferences/destinations/SearchPreferences.kt")
strings = file("lawnchair/res/values/strings.xml")

start = prefs.index("val swipeDownGestureHandler")
check("defaultValue = GestureHandlerConfig.OpenAppSearch" in prefs[start:start+240],
      "Swipe down must default to Universal Search")
check('name = "universal_search_enabled"' in prefs and
      'name = "universal_search_keyboard_on_open"' in prefs,
      "Universal Search must have independently configurable enable/keyboard preferences")
check("combine(prefs.universalSearchEnabled.get())" in gesture,
      "Gesture interception must honor enable control")
check("action != GestureHandlerConfig.OpenNotifications" in gesture,
      "Native notifications must not be intercepted")
check("(enabled || action != GestureHandlerConfig.OpenAppSearch)" in gesture,
      "Disabled search must not intercept default swipe")
check("isTouchOnHomeItem(ev)" in gesture, "Interactive Home icons and widgets must retain gestures")
check("launcher.isInState(LauncherState.NORMAL)" in gesture,
      "Home gesture must not start from unrelated launcher states")
check("it.isTouchOnIconWithSwipeGesture" in gesture,
      "Per-icon swipe gestures must keep priority")
check("if (!prefs.universalSearchEnabled.firstCached()) return" in handler,
      "Disabled search must not trigger")
overlay = file("lawnchair/src/app/lawnchair/GoreeCloudUniversalSearchActivity.kt")
manifest = file("lawnchair/AndroidManifest.xml")
check("GoreeCloudUniversalSearchActivity::class.java" in handler and
      "animateToAllApps" not in handler,
      "Default swipe must open Glaze overlay, not the app drawer")
check(all(tab in overlay for tab in ('"FREQUENT"', '"RECENT"', '"NEW/UPDATED"')),
      "Pull-down panel must provide the three requested tabs")
check("SOFT_INPUT_ADJUST_RESIZE" in overlay and "showSoftInput(input" in overlay and
      "universalSearchKeyboardOnOpen.firstCached()" in overlay,
      "Pull-down keyboard preference must control keyboard focus")
check("queryAndAggregateUsageStats" in overlay and "hasUsageAccess()" in overlay,
      "Recent/frequent rankings must require authorized usage data")
check(all(provider in overlay for provider in ("CalculatorSearchProvider", "ContactsSearchProvider", "FileSearchProvider")),
      "Pull-down query must reuse on-device quick answer, contacts and file providers")
check("LauncherApps.ShortcutQuery" in overlay and "startShortcut(" in overlay,
      "Pull-down query must support authorized app shortcuts")
check("prefs.hiddenApps.firstCached()" in overlay and "hiddenAppsInSearch.firstCached()" in overlay,
      "Overlay must enforce existing hidden-app search preferences")
check('android:name="app.lawnchair.GoreeCloudUniversalSearchActivity"' in manifest and
      'android:exported="false"' in manifest,
      "Overlay must be declared as non-exported Android activity")
check("prefs2.universalSearchEnabled.getAdapter()" in drawer and
      "prefs2.universalSearchKeyboardOnOpen.getAdapter()" in drawer,
      "Settings must expose both Universal Search controls")
check("label = stringResource(R.string.universal_search_label)" in dashboard,
      "Dashboard must present Universal Search")
check("SearchRoute.DRAWER_SEARCH" in routes, "Search settings default tab must be drawer search")
check("label = stringResource(id = R.string.universal_search_label)" in search,
      "Search settings title must be Universal Search")
for key in ("universal_search_label", "universal_search_description",
            "universal_search_enabled_label", "universal_search_enabled_description",
            "universal_search_keyboard_label"):
    check(strings.count('name="'+key+'"') == 1, "Missing/duplicate string: "+key)

if failures:
    for issue in failures:
        print("FAIL:", issue)
    sys.exit(1)
print("PASS: universal swipe gesture, settings, focus, keyboard and local controls (source-level)")
