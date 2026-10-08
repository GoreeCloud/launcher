package com.goreecloud.launcher.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Launcher-owned decorative atmospheric primitives retained under the GLAZE UI V1.6 semantic
 * mapping. These are non-semantic application presentation values, not canonical V1.6 status or
 * authority colors.
 *
 * They never establish protection, privacy, identity, recovery, availability, focus, selection,
 * authorization, or other authoritative state.
 */
object GlazeAtmosphere {
    val canvasBlack = Color(0xFF081016)
    val deepGraphite = Color(0xFF101A20)
    val slateGraphite = Color(0xFF18252B)
    val deepTeal = Color(0xFF0F6B6F)
    val mineralTeal = Color(0xFF1C8A8D)
    val softAqua = Color(0xFF8FD6D2)
    val softAmber = Color(0xFFD9A35F)
    val champagneGold = Color(0xFFE7C78A)
    val warmGlow = Color(0xFFF2D7A6)

    const val lightTealAuraMaxAlpha = 0.08f
    const val lightAmberAuraMaxAlpha = 0.04f
    const val darkTealAuraMaxAlpha = 0.12f
    const val darkAmberAuraMaxAlpha = 0.06f
    const val deepDarkTealAuraMaxAlpha = 0.16f
    const val deepDarkAmberAuraMaxAlpha = 0.08f

    const val defaultAuraFieldsMax = 2
    const val defaultTealFieldsMax = 1
    const val defaultAmberFieldsMax = 1
}
