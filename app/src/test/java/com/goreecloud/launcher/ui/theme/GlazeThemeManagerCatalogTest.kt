package com.goreecloud.launcher.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class GlazeThemeManagerCatalogTest {
    @Test
    fun catalogExposesEachPersistedAppearanceExactlyOnce() {
        assertEquals(
            listOf(
                GlazeThemeMode.SYSTEM,
                GlazeThemeMode.LIGHT,
                GlazeThemeMode.DARK,
                GlazeThemeMode.DEEP_DARK,
            ),
            GlazeThemeManagerCatalog.choices.map { it.mode },
        )
    }

    @Test
    fun choiceLookupReturnsStableUserFacingAndAccessibilityMetadata() {
        val deepDark = GlazeThemeManagerCatalog.choiceFor(GlazeThemeMode.DEEP_DARK)

        assertEquals("Deep Dark", deepDark.title)
        assertEquals(GlazeThemeMode.DEEP_DARK, deepDark.mode)
        assertEquals("Deep Dark appearance preview", deepDark.previewAccessibilityLabel)
        assertEquals("Deep Dark appearance selected", deepDark.selectedAccessibilityState)
    }

    @Test
    fun persistedModeDecoderAcceptsDeepDarkAndFallsBackForUnknownValues() {
        assertEquals(GlazeThemeMode.LIGHT, GlazeThemeModeCodec.decode("LIGHT"))
        assertEquals(GlazeThemeMode.DEEP_DARK, GlazeThemeModeCodec.decode("DEEP_DARK"))
        assertEquals(GlazeThemeMode.SYSTEM, GlazeThemeModeCodec.decode("VIVID"))
        assertEquals(GlazeThemeMode.DARK, GlazeThemeModeCodec.decode(null, GlazeThemeMode.DARK))
    }
}
