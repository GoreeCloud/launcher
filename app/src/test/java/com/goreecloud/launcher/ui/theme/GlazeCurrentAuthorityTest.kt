package com.goreecloud.launcher.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlazeCurrentAuthorityTest {
    @Test
    fun `current shared Glaze authority is exact V1_6 Stable release`() {
        assertEquals("1.6.0", GlazeCurrentAuthority.currentRequiredVersion)
        assertEquals(
            "a7180679ea851389e0f3004515f9a25f420e716d",
            GlazeCurrentAuthority.currentStableSourceRevision,
        )
    }

    @Test
    fun `V1_6 source migration can be complete while consumer acceptance remains open`() {
        assertEquals("1.6.0", GlazeCurrentAuthority.implementedBaselineVersion)
        assertEquals(
            "a7180679ea851389e0f3004515f9a25f420e716d",
            GlazeCurrentAuthority.implementedBaselineSourceRevision,
        )
        assertFalse(GlazeCurrentAuthority.sourceMigrationRequired())
        assertTrue(GlazeCurrentAuthority.consumerAcceptanceRequired())
        assertFalse(GlazeCurrentAuthority.currentConsumerConformanceEstablished)
        assertTrue(GlazeCurrentAuthority.migrationRequired())
    }
}
