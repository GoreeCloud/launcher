package com.goreecloud.launcher.core.launcher

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherLabelSortKeyTest {
    @Test
    fun sortKeyDoesNotDependOnDeviceDefaultLocale() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))

            assertEquals("i", launcherLabelSortKey("I"))
            assertEquals("calendar", launcherLabelSortKey("CALENDAR"))
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun canonicallyEquivalentLabelsShareOneSortKey() {
        assertEquals(
            launcherLabelSortKey("Café"),
            launcherLabelSortKey("Cafe\u0301"),
        )
    }

}
