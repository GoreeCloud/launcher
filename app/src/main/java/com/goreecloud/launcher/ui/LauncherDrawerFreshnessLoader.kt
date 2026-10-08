package com.goreecloud.launcher.ui

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import com.goreecloud.launcher.core.launcher.LauncherAppFreshness
import com.goreecloud.launcher.core.workspace.workspaceKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class LauncherProfilePackageKey(
    val profileId: Int,
    val packageName: String,
)

/**
 * Loads package freshness without borrowing primary-user metadata for a different Android profile.
 *
 * LauncherActivityInfo remains the profile-qualified source for first-install time. Android's
 * public launcher surface does not expose a profile-qualified last-update timestamp. Primary-profile
 * update time therefore comes from PackageManager, while other profiles fail closed to first install
 * so "New" stays truthful and "Updated" never borrows a same-package primary-profile timestamp.
 */
internal suspend fun loadLauncherDrawerFreshness(
    context: Context,
    apps: Collection<LauncherActivityInfo>,
): Map<String, LauncherAppFreshness> = withContext(Dispatchers.IO) {
    val appContext = context.applicationContext
    val primaryUser = Process.myUserHandle()
    val packageCache = mutableMapOf<LauncherProfilePackageKey, Pair<Long, Long>?>()

    buildMap {
        apps.forEach { app ->
            val packageName = app.componentName.packageName
            val firstInstall = app.firstInstallTime.takeIf { it > 0L } ?: 0L
            val cacheKey = LauncherProfilePackageKey(
                profileId = app.user.hashCode(),
                packageName = packageName,
            )

            val packageTimes = packageCache.getOrPut(cacheKey) {
                if (app.user != primaryUser) {
                    // Public LauncherApps exposes exact profile identity and first-install time,
                    // but it does not expose a profile-qualified last-update timestamp. Do not
                    // borrow the primary profile's PackageManager result for a same-package Work
                    // or managed-profile app; fail closed until Android exposes an authorized path.
                    null
                } else {
                    launcherPackageTimes(
                        packageManager = appContext.packageManager,
                        packageName = packageName,
                    )
                }
            }

            val installedAt = firstInstall.takeIf { it > 0L }
                ?: packageTimes?.first?.takeIf { it > 0L }
                ?: return@forEach
            val authoritativeUpdate = packageTimes?.second?.takeIf { it > 0L }
            val updatedAt = authoritativeUpdate ?: installedAt

            put(
                app.workspaceKey(),
                LauncherAppFreshness(
                    firstInstallTimeMillis = installedAt,
                    lastUpdateTimeMillis = updatedAt,
                    updateMetadataAvailable = authoritativeUpdate != null,
                ),
            )
        }
    }
}

@Suppress("DEPRECATION")
private fun launcherPackageTimes(
    packageManager: PackageManager,
    packageName: String,
): Pair<Long, Long>? = runCatching {
    val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(
            packageName,
            PackageManager.PackageInfoFlags.of(0L),
        )
    } else {
        packageManager.getPackageInfo(packageName, 0)
    }
    packageInfo.firstInstallTime to packageInfo.lastUpdateTime
}.getOrNull()
