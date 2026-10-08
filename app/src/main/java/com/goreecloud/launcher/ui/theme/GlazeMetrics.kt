package com.goreecloud.launcher.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Native Android geometry mapping used by GoreeCloud Launcher under the exact GLAZE UI V1.6
 * Stable release source.
 *
 * Exact current source authority:
 * - product identity: GLAZE UI V1.6
 * - machine version: 1.6.0
 * - Stable release source revision: a7180679ea851389e0f3004515f9a25f420e716d
 *
 * V1.6 inherits the Stable layout contract containing 4/8/12/16/24/32/48/64 spacing and a
 * 44 dp-equivalent coarse interaction floor. Launcher deliberately retains a stricter 48 dp
 * touch floor and 56 dp accessibility-oriented target. Those stricter product choices are not
 * relabeled as canonical Glaze tokens.
 *
 * `space5` (20 dp), `space10` (40 dp), the retained radius tiers, and the optical geometry
 * aliases remain Launcher-owned composition conveniences. They are kept for source continuity
 * while V1.6 semantic material/state policy is applied separately.
 */
object GlazeMetrics {
    const val targetVersion = "1.6.0"
    const val sourceRevision = "a7180679ea851389e0f3004515f9a25f420e716d"

    val space1: Dp = 4.dp
    val space2: Dp = 8.dp
    val space3: Dp = 12.dp
    val space4: Dp = 16.dp
    val space5: Dp = 20.dp // Launcher-owned convenience.
    val space6: Dp = 24.dp
    val space8: Dp = 32.dp
    val space10: Dp = 40.dp // Launcher-owned convenience.
    val space12: Dp = 48.dp
    val space16: Dp = 64.dp

    // Launcher-owned retained geometry; not claimed as exact V1.6 token values.
    val radiusSmall: Dp = 12.dp
    val radiusMedium: Dp = 20.dp
    val radiusControl: Dp = 12.dp
    val radiusLarge: Dp = 20.dp
    val radiusExtraLarge: Dp = 28.dp
    val radius2ExtraLarge: Dp = 28.dp
    val radiusPill: Dp = 999.dp

    val opticalMicro: Dp = 8.dp
    val opticalControl: Dp = 16.dp
    val opticalContainer: Dp = 24.dp
    val opticalHero: Dp = 32.dp
    val opticalCapsule: Dp = 999.dp

    val inheritedCoarseTargetFloor: Dp = 44.dp
    val inheritedPointerCompactFloor: Dp = 32.dp

    // Launcher stays more conservative than the inherited coarse floor.
    val minimumTarget: Dp = 48.dp
    val comfortableTarget: Dp = 48.dp
    val touchAssistanceTarget: Dp = 56.dp

    fun interactionTarget(touchAssistance: Boolean): Dp =
        if (touchAssistance) touchAssistanceTarget else minimumTarget
}
