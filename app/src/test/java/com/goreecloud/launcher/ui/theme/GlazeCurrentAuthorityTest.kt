package com.goreecloud.launcher.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlazeCurrentAuthorityTest {
    @Test
    fun `current shared Glaze authority is V1_7 while implementation remains V1_6`() {
        assertEquals("1.7.0", GlazeCurrentAuthority.currentRequiredVersion)
        assertEquals(
            "1a5756daed2294155be2e9972b24f580f6222b7b",
            GlazeCurrentAuthority.currentReleaseIntegrationRevision,
        )
        assertEquals("7c4ded83d7a8725165bb6a55dfb175667cc9589e", GlazeCurrentAuthority.currentQualificationAnchor)
        assertEquals("1.6.0", GlazeCurrentAuthority.inheritedAcceptedRuntimeVersion)
        assertEquals(
            "a7180679ea851389e0f3004515f9a25f420e716d",
            GlazeCurrentAuthority.inheritedAcceptedRuntimeSourceRevision,
        )
    }

    @Test
    fun `V1_6 implementation requires V1_7 contract migration and consumer acceptance`() {
        assertEquals("1.6.0", GlazeCurrentAuthority.implementedBaselineVersion)
        assertEquals(
            "a7180679ea851389e0f3004515f9a25f420e716d",
            GlazeCurrentAuthority.implementedBaselineSourceRevision,
        )
        assertTrue(GlazeCurrentAuthority.sourceMigrationRequired())
        assertTrue(GlazeCurrentAuthority.consumerAcceptanceRequired())
        assertFalse(GlazeCurrentAuthority.currentConsumerConformanceEstablished)
        assertTrue(GlazeCurrentAuthority.migrationRequired())
    }
}
