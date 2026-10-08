package com.goreecloud.launcher.core.launcher

/**
 * Stable, testable contract for handing uninstall requests to Android's package uninstaller.
 *
 * This does not remove packages itself. Android remains responsible for the user-facing
 * confirmation and the final uninstall decision.
 */
internal data class LauncherUninstallRequest(
    val action: String,
    val uriScheme: String,
    val packageName: String,
)

internal object LauncherUninstallRequestPolicy {
    const val ANDROID_UNINSTALL_ACTION = "android.intent.action.UNINSTALL_PACKAGE"
    const val PACKAGE_URI_SCHEME = "package"

    fun create(packageName: String): LauncherUninstallRequest? {
        val normalizedPackage = packageName.trim()
        if (normalizedPackage.isEmpty()) return null
        return LauncherUninstallRequest(
            action = ANDROID_UNINSTALL_ACTION,
            uriScheme = PACKAGE_URI_SCHEME,
            packageName = normalizedPackage,
        )
    }
}
