package com.goreecloud.launcher.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import java.net.InetAddress
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherDirectApiSourcesTest {
    @Test
    fun officialApiConnectionsRemainOffUntilExplicitConfiguration() {
        val sources = LauncherDirectApiCatalog.templates()
        assertEquals(
            setOf(
                LauncherConnectedSearchProviderRegistry.CHATGPT_PROVIDER_ID,
                LauncherConnectedSearchProviderRegistry.CLAUDE_PROVIDER_ID,
                LauncherConnectedSearchProviderRegistry.GEMINI_PROVIDER_ID,
                LauncherConnectedSearchProviderRegistry.PERPLEXITY_PROVIDER_ID,
            ),
            sources.map { it.id }.toSet(),
        )
        assertTrue(sources.all { !it.enabled && it.secret.isEmpty() })
    }

    @Test
    fun officialDestinationCannotBeReplacedBySavedPhishingEndpoint() {
        val official = LauncherDirectApiCatalog.templates().first()
        val altered = official.copy(
            endpoint = "https://unrelated.example/v1/chat/completions",
            title = "Misleading title",
        )
        val resolved = LauncherDirectApiCatalog.resolveSaved(altered)
        assertEquals(official.endpoint, resolved.endpoint)
        assertEquals(official.title, resolved.title)
        assertEquals(official.kind, resolved.kind)
    }

    @Test
    fun customApisRequireHttpsAndRejectUrlCredentialsAndFragments() {
        assertTrue(launcherDirectApiValidHttpsEndpoint("https://api.example.com/v1/chat/completions"))
        assertTrue(launcherDirectApiValidHttpsEndpoint("https://search.example.com/query"))
        assertFalse(launcherDirectApiValidHttpsEndpoint("http://api.example.com/chat"))
        assertFalse(launcherDirectApiValidHttpsEndpoint("https://user:pass@api.example.com/chat"))
        assertFalse(launcherDirectApiValidHttpsEndpoint("https://api.example.com/chat#fragment"))
        assertFalse(launcherDirectApiValidHttpsEndpoint("https://api.example.com/chat?q=secret"))
        assertFalse(launcherDirectApiValidHttpsEndpoint("file:///etc/passwd"))
        assertFalse(launcherDirectApiValidHttpsEndpoint("https://"))
    }

    @Test
    fun customEndpointAndResolvedAddressRejectLocalTargets() {
        for (endpoint in listOf(
            "https://localhost/api",
            "https://device.local/search",
            "https://metadata.google.internal/latest",
            "https://127.0.0.1/api",
            "https://192.168.1.40/api",
            "https://[::1]/api",
            "https://public.example.org:8080/api",
        )) {
            assertFalse(endpoint, launcherDirectApiValidHttpsEndpoint(endpoint))
        }

        fun inet(vararg octets: Int): InetAddress =
            InetAddress.getByAddress(octets.map { it.toByte() }.toByteArray())

        for (address in listOf(
            inet(0, 0, 0, 0),
            inet(10, 1, 2, 3),
            inet(127, 0, 0, 1),
            inet(169, 254, 169, 254),
            inet(172, 16, 0, 4),
            inet(192, 168, 1, 1),
            inet(100, 100, 1, 2),
            inet(198, 18, 0, 1),
            inet(224, 0, 0, 1),
        )) {
            assertFalse(launcherDirectApiIsPublicAddress(address))
        }
        assertTrue(launcherDirectApiIsPublicAddress(inet(8, 8, 8, 8)))
    }

    @Test
    fun customHeadersAndJsonPathsRejectControlAndSeparatorCharacters() {
        assertTrue(launcherDirectApiValidFieldName("X-API-Key"))
        assertTrue(launcherDirectApiValidFieldName("Authorization"))
        assertFalse(launcherDirectApiValidFieldName("X-API-Key\r\nHost"))
        assertFalse(launcherDirectApiValidFieldName(""))
        assertTrue(launcherDirectApiValidResponsePath("results.0.snippet"))
        assertTrue(launcherDirectApiValidResponsePath("answer"))
        assertFalse(launcherDirectApiValidResponsePath("results..snippet"))
        assertFalse(launcherDirectApiValidResponsePath("answers[0].text"))
    }
}
