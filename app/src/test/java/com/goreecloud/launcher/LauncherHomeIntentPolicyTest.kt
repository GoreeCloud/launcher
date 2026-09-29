package com.goreecloud.launcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherHomeIntentPolicyTest {
    @Test
    fun mainHomeIntentResetsPrimaryHome() {
        assertTrue(
            LauncherHomeIntentPolicy.shouldResetToPrimaryHome(
                action = "android.intent.action.MAIN",
                categories = setOf("android.intent.category.HOME"),
            ),
        )
    }

    @Test
    fun launcherIconIntentDoesNotMasqueradeAsHomeRoleIntent() {
        assertFalse(
            LauncherHomeIntentPolicy.shouldResetToPrimaryHome(
                action = "android.intent.action.MAIN",
                categories = setOf("android.intent.category.LAUNCHER"),
            ),
        )
    }

    @Test
    fun missingOrUnrelatedIntentDoesNotResetHome() {
        assertFalse(LauncherHomeIntentPolicy.shouldResetToPrimaryHome(null, null))
        assertFalse(
            LauncherHomeIntentPolicy.shouldResetToPrimaryHome(
                action = "android.intent.action.VIEW",
                categories = setOf("android.intent.category.HOME"),
            ),
        )
    }
}
