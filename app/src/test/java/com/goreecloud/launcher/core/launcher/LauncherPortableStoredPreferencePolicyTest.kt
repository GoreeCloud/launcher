package com.goreecloud.launcher.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherPortableStoredPreferencePolicyTest {
    @Test
    fun absentKeysResolveToCanonicalDefaults() {
        val result = LauncherPortableStoredPreferencePolicy.decode(
            LauncherPortableStoredPreferences(
                homeColumns = null,
                homeRows = null,
                drawerColumns = null,
                showLabels = null,
                iconScale = null,
                layoutLocked = null,
                universalSearchHomeMode = null,
            ),
        )

        assertTrue(result is LauncherPortableStoredPreferencePolicy.DecodeResult.Success)
        assertEquals(
            LauncherPreferences(),
            (result as LauncherPortableStoredPreferencePolicy.DecodeResult.Success).preferences,
        )
    }

    @Test
    fun canonicalStoredValuesRemainExact() {
        val result = LauncherPortableStoredPreferencePolicy.decode(
            LauncherPortableStoredPreferences(
                homeColumns = 6,
                homeRows = 7,
                drawerColumns = 4,
                showLabels = false,
                iconScale = 1.15f,
                layoutLocked = true,
                universalSearchHomeMode = LauncherUniversalSearchHomeMode.SWIPE_DOWN_ONLY.storageValue,
            ),
        )

        assertEquals(
            LauncherPreferences(
                homeColumns = 6,
                homeRows = 7,
                drawerColumns = 4,
                showLabels = false,
                iconScale = 1.15f,
                layoutLocked = true,
                universalSearchHomeMode = LauncherUniversalSearchHomeMode.SWIPE_DOWN_ONLY,
            ),
            (result as LauncherPortableStoredPreferencePolicy.DecodeResult.Success).preferences,
        )
    }

    @Test
    fun outOfRangeStoredValuesAreRejectedRatherThanSanitized() {
        val result = LauncherPortableStoredPreferencePolicy.decode(
            LauncherPortableStoredPreferences(
                homeColumns = 99,
                homeRows = 5,
                drawerColumns = 5,
                showLabels = true,
                iconScale = 1.0f,
                layoutLocked = false,
                universalSearchHomeMode = LauncherUniversalSearchHomeMode.PERMANENT.storageValue,
            ),
        )

        assertTrue(result is LauncherPortableStoredPreferencePolicy.DecodeResult.Invalid)
    }

    @Test
    fun invalidModeAndNonFiniteOrNonCanonicalScaleAreRejected() {
        val invalidMode = LauncherPortableStoredPreferencePolicy.decode(
            LauncherPortableStoredPreferences(
                homeColumns = 4,
                homeRows = 5,
                drawerColumns = 5,
                showLabels = true,
                iconScale = 1.0f,
                layoutLocked = false,
                universalSearchHomeMode = "unknown",
            ),
        )
        assertTrue(invalidMode is LauncherPortableStoredPreferencePolicy.DecodeResult.Invalid)

        val nonFinite = LauncherPortableStoredPreferencePolicy.decode(
            LauncherPortableStoredPreferences(
                homeColumns = 4,
                homeRows = 5,
                drawerColumns = 5,
                showLabels = true,
                iconScale = Float.NaN,
                layoutLocked = false,
                universalSearchHomeMode = null,
            ),
        )
        assertTrue(nonFinite is LauncherPortableStoredPreferencePolicy.DecodeResult.Invalid)

        val nonCanonical = LauncherPortableStoredPreferencePolicy.decode(
            LauncherPortableStoredPreferences(
                homeColumns = 4,
                homeRows = 5,
                drawerColumns = 5,
                showLabels = true,
                iconScale = 1.0005f,
                layoutLocked = false,
                universalSearchHomeMode = null,
            ),
        )
        assertTrue(nonCanonical is LauncherPortableStoredPreferencePolicy.DecodeResult.Invalid)
    }
}
