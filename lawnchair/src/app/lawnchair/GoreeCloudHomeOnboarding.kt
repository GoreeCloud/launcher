package app.lawnchair

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Process
import android.widget.Toast
import com.android.launcher3.LauncherSettings
import com.android.launcher3.model.data.AppInfo

/**
 * Owner-chosen local Home setup. Dock content is never changed here.
 * History is read only after Android grants this package Usage Access.
 */
object GoreeCloudHomeOnboarding {
    private const val PREFERENCES = "goreecloud_home_onboarding"
    private const val MODE = "mode"
    private const val COMPLETED = "completed"

    fun showIfNeeded(launcher: LawnchairLauncher) {
        val prefs = launcher.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        // The full-screen setup is launched by LawnchairLauncher before this
        // runs. Never create blocking first-run dialogs from Home.
        if (!prefs.getBoolean("onboarding_finished", false)) return
        val mode = prefs.getInt(MODE, 0)
        if (mode != 0 && !prefs.getBoolean(COMPLETED, false) && hasUsageAccess(launcher)) {
            populate(launcher, mode)
        }
    }

    private fun hasUsageAccess(context: Context): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java) ?: return false
        return ops.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        ) == AppOpsManager.MODE_ALLOWED
    }

    private fun populate(launcher: LawnchairLauncher, mode: Int) {
        val prefs = launcher.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        if (prefs.getBoolean(COMPLETED, false) || !hasUsageAccess(launcher)) return
        val screen = launcher.workspace.getScreenWithId(0) ?: return
        val apps = launcher.getSystemService(LauncherApps::class.java) ?: return
        val manager = launcher.getSystemService(UsageStatsManager::class.java) ?: return
        val now = System.currentTimeMillis()
        val usage = manager.queryAndAggregateUsageStats(now - 30L * 86400_000L, now)
        val candidates = apps.getActivityList(null, Process.myUserHandle())
            .filter { it.componentName.packageName != launcher.packageName }
            .distinctBy { it.componentName.packageName }
            .filter { (usage[it.componentName.packageName]?.lastTimeUsed ?: 0L) > 0L }
        val ranked = when (mode) {
            1 -> candidates.sortedByDescending { usage[it.componentName.packageName]?.lastTimeUsed ?: 0L }
            2 -> candidates.sortedByDescending { usage[it.componentName.packageName]?.totalTimeInForeground ?: 0L }
            else -> emptyList()
        }.take(10)

        var count = 0
        val reserved = mutableSetOf<Int>()
        for (activity in ranked) {
            val position = (0 until 10).firstOrNull {
                it !in reserved && !screen.isOccupied(it % 5, it / 5)
            } ?: break
            reserved.add(position)
            val info = AppInfo(launcher, activity, Process.myUserHandle())
            info.title = activity.label
            launcher.modelWriter.addItemToDatabase(
                info.makeWorkspaceItem(launcher),
                LauncherSettings.Favorites.CONTAINER_DESKTOP,
                0,
                position % 5,
                position / 5,
            )
            count++
        }
        prefs.edit().putBoolean(COMPLETED, true).apply()
        if (count == 0) Toast.makeText(launcher, "No usage history found; Home remains empty.", Toast.LENGTH_LONG).show()
    }
}
