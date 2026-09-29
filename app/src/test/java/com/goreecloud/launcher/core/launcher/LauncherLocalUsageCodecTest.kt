package com.goreecloud.launcher.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherLocalUsageCodecTest {
    @Test
    fun roundTripPreservesOnlyPositiveCounts() {
        val encoded = LauncherLocalUsageCodec.encode(
            mapOf(
                "10:com.example.alpha/.MainActivity" to 7L,
                "10:com.example.beta/.MainActivity" to 2L,
                "ignored" to 0L,
            ),
        )

        assertEquals(
            mapOf(
                "10:com.example.alpha/.MainActivity" to 7L,
                "10:com.example.beta/.MainActivity" to 2L,
            ),
            LauncherLocalUsageCodec.decode(encoded),
        )
    }

    @Test
    fun recentKeyRoundTripPreservesOrderAndDeduplicates() {
        val encoded = LauncherLocalUsageCodec.encodeRecentKeys(
            listOf(
                "10:com.example.new/.MainActivity",
                "10:com.example.older/.MainActivity",
                "10:com.example.new/.MainActivity",
            ),
        )

        assertEquals(
            listOf(
                "10:com.example.new/.MainActivity",
                "10:com.example.older/.MainActivity",
            ),
            LauncherLocalUsageCodec.decodeRecentKeys(encoded),
        )
    }

    @Test
    fun malformedRecentKeysFailSoft() {
        val decoded = LauncherLocalUsageCodec.decodeRecentKeys("not-base64%%%")
        assertTrue(decoded.isEmpty())
    }

    @Test
    fun malformedRecordsFailSoftWithoutInventingUsage() {
        val decoded = LauncherLocalUsageCodec.decode(
            "not-base64\u001F4\u001Ealso-bad\u001Fnope",
        )

        assertTrue(decoded.isEmpty())
    }
}
