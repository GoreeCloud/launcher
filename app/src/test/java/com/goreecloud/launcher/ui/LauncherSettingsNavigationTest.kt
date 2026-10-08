package com.goreecloud.launcher.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherSettingsNavigationTest {
    @Test
    fun decoderRestoresKnownSettingsDestination() {
        assertEquals(
            LauncherSettingsDestination.THEME_MANAGER,
            LauncherSettingsNavigation.decode("THEME_MANAGER"),
        )
    }

    @Test
    fun decoderFailsClosedToSettingsRootForUnknownDestination() {
        assertEquals(
            LauncherSettingsDestination.ROOT,
            LauncherSettingsNavigation.decode("ICON_PACK_STORE"),
        )
        assertEquals(
            LauncherSettingsDestination.ROOT,
            LauncherSettingsNavigation.decode(null),
        )
    }

    @Test
    fun themeManagerOpensAndReturnsToSettingsRoot() {
        val themeManager = LauncherSettingsNavigation.openThemeManager()
        assertEquals(LauncherSettingsDestination.THEME_MANAGER, themeManager)
        assertEquals(
            LauncherSettingsDestination.ROOT,
            LauncherSettingsNavigation.backFrom(themeManager),
        )
    }
}
