package com.goreecloud.launcher.ui

enum class LauncherSettingsDestination {
    ROOT,
    THEME_MANAGER,
}

/**
 * Saveable, deterministic navigation model for Launcher Settings sub-surfaces.
 *
 * This layer owns presentation destination only. Theme persistence remains with
 * GlazeThemeRepository and Theme Manager does not gain workspace or launcher-role
 * authority through this model.
 */
object LauncherSettingsNavigation {
    fun decode(
        rawDestination: String?,
        fallback: LauncherSettingsDestination = LauncherSettingsDestination.ROOT,
    ): LauncherSettingsDestination = LauncherSettingsDestination.entries
        .firstOrNull { it.name == rawDestination }
        ?: fallback

    fun openThemeManager(): LauncherSettingsDestination = LauncherSettingsDestination.THEME_MANAGER

    fun backFrom(destination: LauncherSettingsDestination): LauncherSettingsDestination = when (destination) {
        LauncherSettingsDestination.THEME_MANAGER -> LauncherSettingsDestination.ROOT
        LauncherSettingsDestination.ROOT -> LauncherSettingsDestination.ROOT
    }
}
