package com.goreecloud.launcher.ui

import com.goreecloud.launcher.BuildConfig

/**
 * Process-local Development diagnostics for transition instrumentation.
 *
 * This state is never persisted, transmitted, exported, synchronized, or used as product authority.
 * Release builds ignore updates. Android instrumentation uses the value only to determine when an
 * existing Home <-> Apps transition has begun so FrameMetrics can be sampled around the real UI.
 */
internal object LauncherTransitionDiagnostics {
    @Volatile
    var currentSurfaceMode: LauncherSurfaceMode = LauncherSurfaceMode.HOME
        private set

    @Volatile
    var observationCount: Long = 0L
        private set

    fun recordSurfaceMode(mode: LauncherSurfaceMode) {
        if (BuildConfig.DEBUG) {
            currentSurfaceMode = mode
            observationCount += 1L
        }
    }
}
