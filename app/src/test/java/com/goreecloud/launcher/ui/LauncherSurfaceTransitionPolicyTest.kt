package com.goreecloud.launcher.ui

import com.goreecloud.launcher.ui.theme.GlazeV16MotionMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherSurfaceTransitionPolicyTest {
    @Test
    fun `standard Home to Apps keeps bounded spatial continuity`() {
        val profile = LauncherSurfaceTransitionPolicy.resolve(
            initial = LauncherSurfaceMode.HOME,
            target = LauncherSurfaceMode.DRAWER,
            motionMode = GlazeV16MotionMode.STANDARD,
        )

        assertTrue(profile.spatial)
        assertTrue(profile.enterDurationMillis <= 150)
        assertTrue(profile.exitDurationMillis <= 105)
    }

    @Test
    fun `reduced motion profile lowers spatial travel and duration`() {
        val standard = LauncherSurfaceTransitionPolicy.resolve(
            LauncherSurfaceMode.HOME,
            LauncherSurfaceMode.DRAWER,
            GlazeV16MotionMode.STANDARD,
        )
        val reduced = LauncherSurfaceTransitionPolicy.resolve(
            LauncherSurfaceMode.HOME,
            LauncherSurfaceMode.DRAWER,
            GlazeV16MotionMode.REDUCED,
        )

        assertTrue(reduced.spatial)
        assertTrue(reduced.enterDurationMillis < standard.enterDurationMillis)
        assertTrue(reduced.exitDurationMillis < standard.exitDurationMillis)
        assertTrue(reduced.enterOffsetDivisor > standard.enterOffsetDivisor)
        assertTrue(reduced.exitOffsetDivisor > standard.exitOffsetDivisor)
    }

    @Test
    fun `minimal motion removes spatial translation`() {
        val profile = LauncherSurfaceTransitionPolicy.resolve(
            LauncherSurfaceMode.DRAWER,
            LauncherSurfaceMode.HOME,
            GlazeV16MotionMode.MINIMAL,
        )

        assertFalse(profile.spatial)
        assertTrue(profile.enterDurationMillis <= 70)
        assertTrue(profile.exitDurationMillis <= 55)
    }
}
