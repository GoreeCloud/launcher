package app.lawnchair

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import android.view.View
import app.lawnchair.preferences.PreferenceManager
import com.android.launcher3.LauncherSettings
import com.android.launcher3.model.data.AppInfo
import com.android.launcher3.model.data.ItemInfo

/**
 * On-device Home ranking. Only shortcuts created by this controller can be
 * removed by ranking changes. User-created shortcuts and the dock are retained.
 */
object GoreeCloudHomeOnboarding {
    private const val STORE = "goreecloud_home_onboarding"
    private const val HISTORY = "goreecloud_home_launch_history"
    private const val MAX_ROWS = 2

    fun recordLaunch(context: Context, packageName: String?) {
        if (packageName.isNullOrBlank() || packageName == context.packageName) return
        if (!context.getSharedPreferences(STORE, Context.MODE_PRIVATE)
                .getBoolean("onboarding_finished", false)) return
        val prefs = context.getSharedPreferences(HISTORY, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("count_$packageName", prefs.getInt("count_$packageName", 0) + 1)
            .putLong("last_$packageName", System.currentTimeMillis())
            .apply()
    }

    private fun usageAllowed(context: Context): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java) ?: return false
        return ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(), context.packageName) == AppOpsManager.MODE_ALLOWED
    }

    /**
     * One-time migration from the exact old seeded Google folder + Play Store
     * layout. Never delete a lone item or a manually moved/renamed shortcut.
     */
    private fun clearLegacyStarterPair(
        launcher: LawnchairLauncher,
        screen: com.android.launcher3.CellLayout,
    ): Boolean {
        val store = launcher.getSharedPreferences(STORE, Context.MODE_PRIVATE)
        if (store.getBoolean("legacy_seed_checked", false)) return false
        val widgets = screen.shortcutsAndWidgets
        var folderView: View? = null
        var folder: com.android.launcher3.model.data.FolderInfo? = null
        var storeView: View? = null
        var play: ItemInfo? = null
        for (i in 0 until widgets.childCount) {
            val view = widgets.getChildAt(i)
            val info = view.tag as? ItemInfo ?: continue
            if (info.screenId != 0 || info.cellY < 2) continue
            if (info is com.android.launcher3.model.data.FolderInfo &&
                info.cellX == 0 && info.title?.toString() == "Google") {
                folderView = view
                folder = info
            } else if (info.cellX == 3 &&
                info.targetComponent?.packageName == "com.android.vending") {
                storeView = view
                play = info
            }
        }
        store.edit().putBoolean("legacy_seed_checked", true).apply()
        if (folder == null || folderView == null || play == null || storeView == null ||
            folder!!.cellY != play!!.cellY) return false
        screen.removeView(folderView)
        screen.removeView(storeView)
        launcher.modelWriter.deleteCollectionAndContentsFromDatabase(folder)
        launcher.modelWriter.deleteItemFromDatabase(play, "Remove legacy Play Store seed")
        screen.postDelayed({ if (!launcher.isDestroyed) launcher.model.forceReload() }, 350L)
        return true
    }

    fun showIfNeeded(launcher: LawnchairLauncher) {
        val settings = launcher.getSharedPreferences(STORE, Context.MODE_PRIVATE)
        if (!settings.getBoolean("onboarding_finished", false)) return
        val screen = launcher.workspace.getScreenWithId(0) ?: return
        if (clearLegacyStarterPair(launcher, screen)) return
        val mode = settings.getInt("mode", 0)
        val history = launcher.getSharedPreferences(HISTORY, Context.MODE_PRIVATE)
        val apps = launcher.getSystemService(LauncherApps::class.java) ?: return
        val personal = Process.myUserHandle()
        val all = apps.getActivityList(null, personal)
            .filter { it.componentName.packageName != launcher.packageName }
            .distinctBy { it.componentName.packageName }
        val now = System.currentTimeMillis()
        val since = settings.getLong("ranking_started", now)
        val stats = if (mode != 0 && usageAllowed(launcher)) {
            launcher.getSystemService(UsageStatsManager::class.java)
                ?.queryAndAggregateUsageStats(since, now).orEmpty()
        } else emptyMap()
        val ranked = if (mode == 0) emptyList() else all.filter { app ->
            val id = app.componentName.packageName
            history.getInt("count_$id", 0) > 0 ||
                (stats[id]?.lastTimeUsed ?: 0L) >= since
        }.sortedWith(compareByDescending<android.content.pm.LauncherActivityInfo> { app ->
            val id = app.componentName.packageName
            if (mode == 1) {
                maxOf(history.getLong("last_$id", 0L), stats[id]?.lastTimeUsed ?: 0L)
            } else {
                history.getInt("count_$id", 0) * 60_000L +
                    (stats[id]?.totalTimeInForeground ?: 0L)
            }
        }.thenBy { it.label.toString() }).take(
            PreferenceManager.getInstance(launcher).workspaceColumns.get().coerceIn(4, 6) * MAX_ROWS
        )

        val container = screen.shortcutsAndWidgets
        val managed = settings.getStringSet("managed_ids", emptySet()).orEmpty()
        val placed = mutableListOf<Pair<View, ItemInfo>>()
        for (i in 0 until container.childCount) {
            val view = container.getChildAt(i)
            val item = view.tag as? ItemInfo ?: continue
            if (item.id.toString() in managed && item.cellY in 0 until MAX_ROWS &&
                item.screenId == 0) {
                placed.add(view to item)
            }
        }
        val desired = ranked.map { it.componentName.flattenToString() }
        val previous = settings.getString("last_ranked", null)
        if (desired.joinToString("|") == previous && managed.size == placed.size) return

        // Clear only the previous dynamic set; never touch manually pinned apps.
        for ((view, item) in placed) {
            screen.removeView(view)
            launcher.modelWriter.deleteItemFromDatabase(item, "Refresh GoreeCloud Home ranking")
        }
        val occupiedApps = mutableSetOf<String>()
        for (i in 0 until container.childCount) {
            val info = container.getChildAt(i).tag as? ItemInfo ?: continue
            info.targetComponent?.packageName?.let { occupiedApps.add(it) }
        }
        val columns = PreferenceManager.getInstance(launcher).workspaceColumns.get().coerceIn(4, 6)
        val newIds = mutableSetOf<String>()
        for (activity in ranked) {
            if (activity.componentName.packageName in occupiedApps) continue
            val position = (0 until columns * MAX_ROWS).firstOrNull {
                !screen.isOccupied(it % columns, it / columns)
            } ?: break
            val info = AppInfo(launcher, activity, personal).makeWorkspaceItem(launcher)
            launcher.modelWriter.addItemToDatabase(info,
                LauncherSettings.Favorites.CONTAINER_DESKTOP, 0,
                position % columns, position / columns)
            newIds.add(info.id.toString())
            occupiedApps.add(activity.componentName.packageName)
        }
        settings.edit().putStringSet("managed_ids", newIds)
            .putString("last_ranked", desired.joinToString("|"))
            .apply()
        if (placed.isNotEmpty() || newIds.isNotEmpty()) {
            // ModelWriter queues writes; rebind only after queued work settles.
            screen.postDelayed({ if (!launcher.isDestroyed) launcher.model.forceReload() }, 350L)
        }
    }
}
