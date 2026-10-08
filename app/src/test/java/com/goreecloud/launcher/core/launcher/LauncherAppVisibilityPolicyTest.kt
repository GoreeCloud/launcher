package com.goreecloud.launcher.core.launcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherAppVisibilityPolicyTest {
    @Test
    fun hiddenIdentityIsExcludedWithoutHidingSameComponentInAnotherProfile() {
        val personal = "0:com.example/.Main"
        val work = "10:com.example/.Main"
        val hidden = setOf(personal)

        assertFalse(LauncherAppVisibilityPolicy.isDiscoverable(personal, hidden))
        assertTrue(LauncherAppVisibilityPolicy.isDiscoverable(work, hidden))
    }

    @Test
    fun visibleIdentityRemainsDiscoverable() {
        assertTrue(
            LauncherAppVisibilityPolicy.isDiscoverable(
                appKey = "0:com.example/.Visible",
                hiddenAppKeys = emptySet(),
            ),
        )
    }
}
