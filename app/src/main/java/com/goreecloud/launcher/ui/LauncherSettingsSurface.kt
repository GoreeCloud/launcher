package com.goreecloud.launcher.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import com.goreecloud.launcher.ui.theme.GlazeThemeMode

/**
 * Saveable Settings composition that connects the validated destination model to the rendered host.
 *
 * The caller supplies the existing Settings root content and receives a bounded callback for opening
 * Theme Manager. Theme persistence remains outside this surface and continues through the supplied
 * [onSelectThemeMode] callback.
 */
@Composable
fun LauncherSettingsSurface(
    selectedThemeMode: GlazeThemeMode,
    onSelectThemeMode: (GlazeThemeMode) -> Unit,
    rootContent: @Composable (onOpenThemeManager: () -> Unit) -> Unit,
) {
    var persistedDestination by rememberSaveable {
        mutableStateOf(LauncherSettingsDestination.ROOT.name)
    }
    val destination = LauncherSettingsNavigation.decode(persistedDestination)

    LauncherSettingsDestinationHost(
        destination = destination,
        selectedThemeMode = selectedThemeMode,
        onSelectThemeMode = onSelectThemeMode,
        onBackFromThemeManager = {
            persistedDestination = LauncherSettingsNavigation.backFrom(destination).name
        },
        rootContent = {
            rootContent {
                persistedDestination = LauncherSettingsNavigation.openThemeManager().name
            }
        },
    )
}
