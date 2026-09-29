package com.goreecloud.launcher.ui.theme

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlazeV16PresentationPolicyTest {
    @Test
    fun `policy pins exact Stable V1_6 release source`() {
        assertEquals("1.6.0", GlazeV16PresentationPolicy.stableVersion)
        assertEquals(
            "a7180679ea851389e0f3004515f9a25f420e716d",
            GlazeV16PresentationPolicy.stableSourceRevision,
        )
        assertEquals(44.dp, GlazeV16PresentationPolicy.inheritedCoarseTargetFloor)
        assertEquals(32.dp, GlazeV16PresentationPolicy.inheritedPointerCompactFloor)
        assertEquals(2.dp, GlazeV16PresentationPolicy.inheritedFocusRingWidth)
        assertEquals(2.dp, GlazeV16PresentationPolicy.inheritedFocusRingOffset)
    }

    @Test
    fun `reduced transparency resolves glass to solid without changing task truth`() {
        val resolved = GlazeV16PresentationPolicy.resolve(
            requestedMaterial = GlazeV16MaterialRole.FUNCTIONAL_GLASS,
            context = GlazeV16PresentationContext(reducedTransparency = true),
        )

        assertEquals(GlazeV16MaterialRole.SOLID, resolved.materialRole)
        assertEquals(GlazeV16MotionMode.STANDARD, resolved.motionMode)
        assertEquals(48.dp, resolved.minimumInteractionTarget)
    }

    @Test
    fun `essential performance fails visual cost down to solid and minimal motion`() {
        val resolved = GlazeV16PresentationPolicy.resolve(
            requestedMaterial = GlazeV16MaterialRole.CLEAR_GLASS,
            context = GlazeV16PresentationContext(
                performanceLevel = GlazeV16PerformanceLevel.ESSENTIAL,
            ),
        )

        assertEquals(GlazeV16MaterialRole.SOLID, resolved.materialRole)
        assertEquals(GlazeV16MotionMode.MINIMAL, resolved.motionMode)
    }

    @Test
    fun `reduced motion is presentation-only and preserves target size`() {
        val resolved = GlazeV16PresentationPolicy.resolve(
            requestedMaterial = GlazeV16MaterialRole.RAISED,
            context = GlazeV16PresentationContext(
                reducedMotion = true,
                touchAssistance = true,
            ),
        )

        assertEquals(GlazeV16MaterialRole.RAISED, resolved.materialRole)
        assertEquals(GlazeV16MotionMode.MINIMAL, resolved.motionMode)
        assertEquals(56.dp, resolved.minimumInteractionTarget)
    }

    @Test
    fun `large text yields density while strong input modes require visible focus`() {
        val resolved = GlazeV16PresentationPolicy.resolve(
            requestedMaterial = GlazeV16MaterialRole.SOLID,
            context = GlazeV16PresentationContext(
                largeText = true,
                keyboardFirst = true,
            ),
        )

        assertTrue(resolved.densityMayYieldToReflow)
        assertTrue(resolved.strongVisibleFocusRequired)
    }

    @Test
    fun `Android presentation signals map only authoritative runtime state`() {
        val resolved = GlazeV16AndroidPresentationContext.resolve(
            GlazeV16AndroidPresentationSignals(
                fontScale = 1.65f,
                animationsEnabled = false,
                touchExplorationEnabled = true,
            ),
        )

        assertTrue(resolved.reducedMotion)
        assertTrue(resolved.largeText)
        assertTrue(resolved.extraLargeText)
        assertTrue(resolved.touchAssistance)
        assertTrue(resolved.screenReaderOptimized)
        assertFalse(resolved.reducedTransparency)
        assertEquals(GlazeV16PerformanceLevel.FULL, resolved.performanceLevel)
    }

    @Test
    fun `neutral context does not manufacture accessibility state`() {
        val context = GlazeV16PresentationContext()
        assertFalse(context.reducedMotion)
        assertFalse(context.reducedTransparency)
        assertFalse(context.increasedContrast)
        assertFalse(context.touchAssistance)
        assertEquals(GlazeV16PerformanceLevel.FULL, context.performanceLevel)
    }
}
