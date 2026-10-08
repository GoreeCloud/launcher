package com.goreecloud.launcher.core.launcher

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class LauncherPortablePreferencesTest {
    @Test
    fun roundTripIsDeterministicAndPreservesSupportedPreferences() {
        val preferences = LauncherPreferences(
            homeColumns = 6,
            homeRows = 7,
            drawerColumns = 4,
            showLabels = false,
            iconScale = 1.15f,
            layoutLocked = true,
            universalSearchHomeMode = LauncherUniversalSearchHomeMode.SWIPE_DOWN_ONLY,
        )

        val first = LauncherPortablePreferences.encode(preferences)
        val second = LauncherPortablePreferences.encode(preferences)
        assertEquals(first, second)
        assertTrue(first.startsWith("format=${LauncherPortablePreferences.FORMAT}\n"))
        assertTrue(first.contains("icon_scale_milli=1150\n"))

        val decoded = LauncherPortablePreferences.decode(first)
        assertTrue(decoded is LauncherPortablePreferences.DecodeResult.Success)
        assertEquals(
            preferences,
            (decoded as LauncherPortablePreferences.DecodeResult.Success).preferences,
        )
    }

    @Test
    fun tamperingFailsBeforePreferenceMaterialization() {
        val encoded = LauncherPortablePreferences.encode(LauncherPreferences())
        val tampered = encoded.replace("show_labels=true", "show_labels=false")

        val decoded = LauncherPortablePreferences.decode(tampered)
        assertTrue(decoded is LauncherPortablePreferences.DecodeResult.Invalid)
        assertEquals(
            "snapshot integrity check failed",
            (decoded as LauncherPortablePreferences.DecodeResult.Invalid).reason,
        )
    }

    @Test
    fun correctlyChecksummedUnknownRecordIsRejected() {
        val payload = listOf(
            "format=${LauncherPortablePreferences.FORMAT}",
            "version=${LauncherPortablePreferences.VERSION}",
            "home_columns=4",
            "home_rows=5",
            "drawer_columns=5",
            "show_labels=true",
            "icon_scale_milli=1000",
            "layout_locked=false",
            "index_home_mode=permanent",
            "hidden_apps=com.example.private",
        ).joinToString("\n")
        val encoded = "$payload\nchecksum=${sha256Hex(payload)}\n"

        val decoded = LauncherPortablePreferences.decode(encoded)
        assertTrue(decoded is LauncherPortablePreferences.DecodeResult.Invalid)
        assertEquals(
            "snapshot must contain exactly the supported records",
            (decoded as LauncherPortablePreferences.DecodeResult.Invalid).reason,
        )
    }

    @Test
    fun outOfRangeValuesAreRejectedWithoutSanitizing() {
        val payload = listOf(
            "format=${LauncherPortablePreferences.FORMAT}",
            "version=${LauncherPortablePreferences.VERSION}",
            "home_columns=99",
            "home_rows=5",
            "drawer_columns=5",
            "show_labels=true",
            "icon_scale_milli=1000",
            "layout_locked=false",
            "index_home_mode=permanent",
        ).joinToString("\n")
        val encoded = "$payload\nchecksum=${sha256Hex(payload)}\n"

        val decoded = LauncherPortablePreferences.decode(encoded)
        assertTrue(decoded is LauncherPortablePreferences.DecodeResult.Invalid)
        assertEquals(
            "home columns are outside the supported range",
            (decoded as LauncherPortablePreferences.DecodeResult.Invalid).reason,
        )
    }

    @Test
    fun encodeRejectsNonCanonicalIconScale() {
        try {
            LauncherPortablePreferences.encode(LauncherPreferences(iconScale = 1.0015f))
            fail("non-millistep icon scale must be rejected")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message.orEmpty().contains("icon scale is not canonical"))
        }
    }

    @Test
    fun oversizedInputIsRejectedBeforeParsing() {
        val decoded = LauncherPortablePreferences.decode("x".repeat(4097))
        assertTrue(decoded is LauncherPortablePreferences.DecodeResult.Invalid)
        assertEquals(
            "snapshot exceeds the bounded size limit",
            (decoded as LauncherPortablePreferences.DecodeResult.Invalid).reason,
        )
    }

    private fun sha256Hex(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }
}
