/*
 * Copyright 2026 GoreeCloud
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package app.lawnchair.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Native Android adaptation of the stable Glaze 1.7.0 semantic contract.
 *
 * The launcher keeps Android's dynamic wallpaper colors, profile/launcher APIs,
 * system navigation and accessibility authority. This is a Development mapping,
 * not an assertion of Glaze consumer conformance or device acceptance.
 */
@Immutable
data class GlazeMetrics(
    val minimumTouchTarget: Dp = 48.dp,
    val compactRadius: Dp = 12.dp,
    val cardRadius: Dp = 20.dp,
    val sheetRadius: Dp = 28.dp,
    val contentSpacing: Dp = 16.dp,
)

val LocalGlazeMetrics = staticCompositionLocalOf { GlazeMetrics() }

object GlazeAndroidContract {
    const val stableVersion = "1.7.0"
    val metrics = GlazeMetrics()

    /**
     * Semantic container treatment: clear surfaces in light mode and
     * dimensional but readable elevation in dark mode.
     * Never replace dynamic colors with a hard-coded light-only palette.
     */
    fun preferenceSurface(scheme: ColorScheme, darkTheme: Boolean) =
        if (darkTheme) scheme.surfaceContainerHigh else scheme.surfaceContainerLow
}
