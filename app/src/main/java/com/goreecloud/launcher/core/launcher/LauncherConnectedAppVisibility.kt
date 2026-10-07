package com.goreecloud.launcher.core.launcher

/** Visibility never proves absence in an Android profile blocked by policy. */
internal enum class LauncherConnectedAppVisibility { USER, WORK, BOTH, NOT_VISIBLE }

internal fun <T, U> launcherConnectedAppVisibility(
    apps: Iterable<T>,
    targetPackage: String,
    primaryUser: U,
    packageOf: (T) -> String,
    userOf: (T) -> U,
): LauncherConnectedAppVisibility {
    var user = false
    var work = false
    apps.forEach { app ->
        if (packageOf(app) == targetPackage) {
            if (userOf(app) == primaryUser) user = true else work = true
        }
    }
    return when {
        user && work -> LauncherConnectedAppVisibility.BOTH
        user -> LauncherConnectedAppVisibility.USER
        work -> LauncherConnectedAppVisibility.WORK
        else -> LauncherConnectedAppVisibility.NOT_VISIBLE
    }
}
