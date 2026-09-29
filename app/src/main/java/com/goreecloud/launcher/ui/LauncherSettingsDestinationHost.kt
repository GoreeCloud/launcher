package com.goreecloud.launcher.ui

import androidx.compose.runtime.Composable
import com.goreecloud.launcher.ui.theme.GlazeThemeMode
import com.goreecloud.launcher.ui.theme.ThemeManagerSurface

/**
 * Rendered Settings sub-surface host for saveable LauncherSettingsDestination state.
 *
 * Root settings content remains owned by LauncherBetaRoot. Theme persistence remains
 * owned by GlazeThemeRepository; this host only selects which presentation is visible.
 */
@Composable
fun LauncherSettingsDestinationHost(
    destination: LauncherSettingsDestination,
    selectedThemeMode: GlazeThemeMode,
    onSelectThemeMode: (GlazeThemeMode) -> Unit,
    onBackFromThemeManager: () -> Unit,
    rootContent: @Composable () -> Unit,
) {
    when (destination) {
        LauncherSettingsDestination.ROOT -> rootContent()
        LauncherSettingsDestination.THEME_MANAGER -> ThemeManagerSurface(
            selectedMode = selectedThemeMode,
            onSelectMode = onSelectThemeMode,
            onBack = onBackFromThemeManager,
        )
    }
}
