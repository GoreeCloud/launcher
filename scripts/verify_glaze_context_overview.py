#!/usr/bin/env python3
"""Contract guard for GoreeCloud Home edit and app contextual UI; static evidence only."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
def get(path):
    return (root / path).read_text(encoding="utf-8")
pager=get("lawnchair/src/app/lawnchair/views/EditModePageStrip.kt")
toolbar=get("lawnchair/src/app/lawnchair/views/GoreeCloudOverviewToolbar.kt")
launcher=get("lawnchair/src/app/lawnchair/LawnchairLauncher.kt")
popup=get("src/com/android/launcher3/popup/PopupContainerWithArrow.java")
menu=get("src/com/android/launcher3/popup/GoreeCloudContextSurfaces.java")
xml=get("lawnchair/res/layout/edit_mode_page_strip_item.xml")
assert "getItemCount(): Int = items.size + 1" in pager
assert "position == items.size" in pager and "addHomePage()" in pager
assert "getMovementFlags" in pager and "ws.reorderablePageGroupStarts.indexOf(pages[position])" in pager
assert "pageGroupStarts(ws)" in pager and "ws.defaultPage" in pager
assert "(0 until ws.childCount step ws.panelCount.coerceAtLeast(1))" in pager
assert "Preview Home page" in pager and "Default Home page" in pager
assert '// Add-page is an affordance, not the selected Home page.' in pager
assert 'holder.title.setTextColor(0x68FFFFFF)' in pager
assert "GoreeCloudOverviewToolbar.install(this)" in launcher
assert "Edit Home Screen" in toolbar and "Add apps" in toolbar and "Done" in toolbar
assert "host.removeItem(icon, item, true" in toolbar and "getCurrentPageLayout" in toolbar
assert 'setTitle("Remove from dock?")' in toolbar and 'setPositiveButton("Remove")' in toolbar
assert 'setTitle("Remove this Home page?")' in toolbar
assert 'Only empty secondary Home pages can be removed' in toolbar
assert 'ws.getScreenIdForPageIndex(ws.currentPage) == screenId' in toolbar
assert 'addOverlay(host, badge, dp(host, 48), dp(host, 48))' in toolbar
assert 'host.inflateAndBindItemWithAnimation(info)' in toolbar
assert 'LauncherAppState.getInstance(host).iconCache.getTitleAndIcon(' in toolbar
assert 'appInfo, chosen, DEFAULT_LOOKUP_FLAG)' in toolbar
assert toolbar.index('getTitleAndIcon(') < toolbar.index('appInfo.makeWorkspaceItem(host)')
assert "targetScreenId" in toolbar and "alreadyHere" in toolbar
assert 'SearchView(host)' in toolbar and "adapter.filter.filter" in toolbar
assert "showAddApps(host)" in toolbar and "AlertDialog.Builder(host)" in toolbar
assert "getScreenIdForPageIndex(index)" in toolbar and "page.isOccupied(x, y)" in toolbar
assert "ws.getPageAt(index) as? CellLayout" in toolbar
assert "if (item is FolderInfo)" in toolbar and "return@setOnClickListener" in toolbar
assert "alignGoreeCloudEditNavigation()" in launcher
assert "minOf(preferredTop, safeTop)" in launcher
frames=get("lawnchair/src/app/lawnchair/views/GoreeCloudOverviewFrames.kt")
assert "WeakHashMap<LawnchairLauncher, View>" in frames
assert "host.hotseat.getGlobalVisibleRect" in frames and "overlay.invalidate()" in frames
assert "index == workspace.currentPage" in frames
assert "GoreeCloudOverviewFrames.apply(host, editing)" in toolbar
assert "override fun onPageEndTransition()" in launcher and "GoreeCloudOverviewFrames.apply(this, true)" in launcher
assert all(label in launcher for label in ("Wallpapers","Widgets","Settings"))
assert "removeEmptyWorkspacePageByIndex" in toolbar and "ws.setDefaultPage(start)" in toolbar
assert "GoreeCloudContextSurfaces.actionBar" in popup
badges=get("src/com/android/launcher3/GoreeCloudProfileBadges.java")
assert "launcher.isInState(LauncherState.EDIT_MODE)" in badges
assert "setEditorVisible(@NonNull Launcher launcher, boolean visible)" in badges
assert "updateDockProfileBadgeVisibility(host, editing)" in toolbar
assert "children.getChildAt(index).invalidate()" in toolbar
assert "addAllShortcuts(deepShortcutCount, otherActions)" in popup
assert "APP SHORTCUTS" in popup and "notificationCard" in popup
assert "NotificationListener.getInstanceIfConnected()" in menu
assert "isKeyguardLocked()" in menu and "sbn.getUser()" in menu and "sbn.getPackageName()" in menu
assert all(label in menu for label in ('"Select"','"Remove"','"Edit"','"App info"'))
assert 'android:id="@+id/page_strip_title"' in xml
assert not (root / "prebuilts/libs/libGoogleFeed.jar").exists()
assert not (root / "lawnchair/aidl/com/google/android/libraries/launcherclient/ILauncherOverlay.aidl").exists()
assert not (root / "lawnchair/aidl/com/google/android/libraries/launcherclient/ILauncherOverlayCallback.aidl").exists()
print("PASS: Home edit navigation and real app context source contracts (device validation pending)")
