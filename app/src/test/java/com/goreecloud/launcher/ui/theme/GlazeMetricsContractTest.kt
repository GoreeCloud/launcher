package com.goreecloud.launcher.ui.theme

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlazeMetricsContractTest {
    @Test
    fun `launcher source mapping targets exact GLAZE UI V1_6 Stable release`() {
        assertEquals("1.6.0", GlazeMetrics.targetVersion)
        assertEquals(
            "a7180679ea851389e0f3004515f9a25f420e716d",
            GlazeMetrics.sourceRevision,
        )
    }

    @Test
    fun `launcher retains stricter interaction targets than inherited V1_6 coarse floor`() {
        assertEquals(44.dp, GlazeMetrics.inheritedCoarseTargetFloor)
        assertEquals(32.dp, GlazeMetrics.inheritedPointerCompactFloor)
        assertEquals(48.dp, GlazeMetrics.minimumTarget)
        assertEquals(56.dp, GlazeMetrics.touchAssistanceTarget)
        assertTrue(GlazeMetrics.minimumTarget > GlazeMetrics.inheritedCoarseTargetFloor)
        assertEquals(48.dp, GlazeMetrics.interactionTarget(touchAssistance = false))
        assertEquals(56.dp, GlazeMetrics.interactionTarget(touchAssistance = true))
    }

    @Test
    fun `launcher maps inherited V1_6 Stable spacing subset without relabeling conveniences`() {
        assertEquals(4.dp, GlazeMetrics.space1)
        assertEquals(8.dp, GlazeMetrics.space2)
        assertEquals(12.dp, GlazeMetrics.space3)
        assertEquals(16.dp, GlazeMetrics.space4)
        assertEquals(24.dp, GlazeMetrics.space6)
        assertEquals(32.dp, GlazeMetrics.space8)
        assertEquals(48.dp, GlazeMetrics.space12)
        assertEquals(64.dp, GlazeMetrics.space16)

        assertEquals(20.dp, GlazeMetrics.space5)
        assertEquals(40.dp, GlazeMetrics.space10)
    }

    @Test
    fun `retained Launcher geometry remains stable during semantic migration`() {
        assertEquals(12.dp, GlazeMetrics.radiusSmall)
        assertEquals(20.dp, GlazeMetrics.radiusMedium)
        assertEquals(12.dp, GlazeMetrics.radiusControl)
        assertEquals(20.dp, GlazeMetrics.radiusLarge)
        assertEquals(28.dp, GlazeMetrics.radiusExtraLarge)
        assertEquals(28.dp, GlazeMetrics.radius2ExtraLarge)
        assertEquals(999.dp, GlazeMetrics.radiusPill)

        assertEquals(8.dp, GlazeMetrics.opticalMicro)
        assertEquals(16.dp, GlazeMetrics.opticalControl)
        assertEquals(24.dp, GlazeMetrics.opticalContainer)
        assertEquals(32.dp, GlazeMetrics.opticalHero)
        assertEquals(999.dp, GlazeMetrics.opticalCapsule)
    }
}
