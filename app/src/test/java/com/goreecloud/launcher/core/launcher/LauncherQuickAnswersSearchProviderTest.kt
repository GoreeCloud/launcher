package com.goreecloud.launcher.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherQuickAnswersSearchProviderTest {
    private val provider = LauncherQuickAnswersSearchProvider()

    @Test
    fun calculatorHonorsPrecedenceAndParentheses() {
        assertAnswer("2 + 3 * 4", "14", "Calculator")
        assertAnswer("(2 + 3) * 4", "20", "Calculator")
        assertAnswer("-5 × (2 + 1)", "-15", "Calculator")
    }

    @Test
    fun calculatorRejectsPlainNumbersMalformedInputAndDivisionByZero() {
        assertTrue(provider.search("42").isEmpty())
        assertTrue(provider.search("2 +").isEmpty())
        assertTrue(provider.search("10 / 0").isEmpty())
        assertTrue(provider.search("sqrt(9)").isEmpty())
    }

    @Test
    fun convertsCommonLengthMassAndTimeUnits() {
        assertAnswer("10 km to mi", "6.21371192237 mi", "Unit conversion")
        assertAnswer("5 lb in kg", "2.26796185 kg", "Unit conversion")
        assertAnswer("90 minutes to hours", "1.5 h", "Unit conversion")
    }

    @Test
    fun convertsTemperatureAndRejectsImpossibleOrCrossDimensionConversions() {
        assertAnswer("32 f to c", "0 °C", "Unit conversion")
        assertAnswer("100 c to f", "212 °F", "Unit conversion")
        assertTrue(provider.search("-1 k to c").isEmpty())
        assertTrue(provider.search("1 kg to m").isEmpty())
    }

    @Test
    fun resultIsLocalActionWithExplicitCopyPayload() {
        val result = provider.search("2 + 2").single()

        assertEquals(LauncherQuickAnswersSearchProvider.PROVIDER_ID, result.providerId)
        assertEquals(LauncherSearchCategory.ACTION, result.category)
        assertEquals("4", result.title)
        assertEquals("4", (result.action as LauncherCopyTextSearchAction).text)
    }

    @Test
    fun blankOrOversizedQueriesDoNotProduceAnswers() {
        assertTrue(provider.search("   ").isEmpty())
        assertTrue(provider.search("1 + " + "1".repeat(200)).isEmpty())
    }

    private fun assertAnswer(query: String, expected: String, detailPrefix: String) {
        val result = provider.search(query).single()
        assertEquals(expected, result.title)
        assertTrue(result.subtitle.orEmpty().startsWith(detailPrefix))
        assertEquals(expected, (result.action as LauncherCopyTextSearchAction).text)
    }
}
