package com.goreecloud.launcher.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LauncherDrawerAndDockVisualDefaultsTest {
    @Test
    fun alphabetNavigationDefaultsToOff() {
        assertFalse(LauncherVisualPreferences().showDrawerAlphabetIndex)
    }

    @Test
    fun newDockDefaultsToGlassWithoutOverridingExplicitClear() {
        assertEquals(LauncherDockStyle.GLASS, LauncherExperiencePreferences().dockStyle)
        assertEquals(LauncherDockStyle.GLASS, LauncherDockStyle.fromStorage(null))
        assertEquals(LauncherDockStyle.CLEAR, LauncherDockStyle.fromStorage("clear"))
    }
}
