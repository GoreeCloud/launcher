package com.goreecloud.launcher.ui

import com.goreecloud.launcher.ui.theme.GlazeV16MotionMode

data class LauncherSurfaceTransitionProfile(
    val spatial: Boolean,
    val enterDurationMillis: Int,
    val exitDurationMillis: Int,
    val enterOffsetDivisor: Int,
    val exitOffsetDivisor: Int,
)

object LauncherSurfaceTransitionPolicy {
    fun resolve(
        initial: LauncherSurfaceMode,
        target: LauncherSurfaceMode,
        motionMode: GlazeV16MotionMode,
    ): LauncherSurfaceTransitionProfile {
        if (motionMode == GlazeV16MotionMode.MINIMAL) {
            return LauncherSurfaceTransitionProfile(
                spatial = false,
                enterDurationMillis = 70,
                exitDurationMillis = 55,
                enterOffsetDivisor = 1,
                exitOffsetDivisor = 1,
            )
        }

        val reduced = motionMode == GlazeV16MotionMode.REDUCED
        return when {
            initial == LauncherSurfaceMode.HOME && target == LauncherSurfaceMode.DRAWER ->
                LauncherSurfaceTransitionProfile(
                    spatial = true,
                    enterDurationMillis = if (reduced) 110 else 150,
                    exitDurationMillis = if (reduced) 85 else 105,
                    enterOffsetDivisor = if (reduced) 18 else 10,
                    exitOffsetDivisor = if (reduced) 28 else 18,
                )

            initial == LauncherSurfaceMode.DRAWER && target == LauncherSurfaceMode.HOME ->
                LauncherSurfaceTransitionProfile(
                    spatial = true,
                    enterDurationMillis = if (reduced) 95 else 125,
                    exitDurationMillis = if (reduced) 110 else 150,
                    enterOffsetDivisor = if (reduced) 28 else 18,
                    exitOffsetDivisor = if (reduced) 18 else 10,
                )

            else ->
                LauncherSurfaceTransitionProfile(
                    spatial = false,
                    enterDurationMillis = if (reduced) 100 else 140,
                    exitDurationMillis = if (reduced) 80 else 100,
                    enterOffsetDivisor = 1,
                    exitOffsetDivisor = 1,
                )
        }
    }
}
