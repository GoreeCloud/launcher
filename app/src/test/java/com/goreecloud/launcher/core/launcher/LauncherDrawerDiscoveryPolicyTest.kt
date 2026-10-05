package com.goreecloud.launcher.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherDrawerDiscoveryPolicyTest {
    private val now = 2_000_000_000_000L
    private val day = 86_400_000L

    @Test
    fun freshnessSeparatesInstallsFromUpdates() {
        val install = LauncherAppFreshness(now - 2 * day, now - 2 * day + 10_000)
        val update = LauncherAppFreshness(now - 90 * day, now - day)

        assertTrue(LauncherDrawerDiscoveryPolicy.recentlyInstalled(install, now))
        assertFalse(LauncherDrawerDiscoveryPolicy.recentlyUpdated(install, now))
        assertFalse(LauncherDrawerDiscoveryPolicy.recentlyInstalled(update, now))
        assertTrue(LauncherDrawerDiscoveryPolicy.recentlyUpdated(update, now))
    }

    @Test
    fun futureOrInvalidFreshnessFailsClosed() {
        assertFalse(
            LauncherDrawerDiscoveryPolicy.recentlyInstalled(
                LauncherAppFreshness(now + day, now + day),
                now,
            ),
        )
        assertFalse(
            LauncherDrawerDiscoveryPolicy.recentlyUpdated(
                LauncherAppFreshness(0L, now - day),
                now,
            ),
        )
        assertFalse(
            LauncherDrawerDiscoveryPolicy.recentlyUpdated(
                LauncherAppFreshness(
                    firstInstallTimeMillis = now - 90 * day,
                    lastUpdateTimeMillis = now - day,
                    updateMetadataAvailable = false,
                ),
                now,
            ),
        )
    }

    @Test
    fun suggestionsUseLocalUsageThenStableLabels() {
        val suggested = LauncherDrawerDiscoveryPolicy.suggestedKeys(
            availableKeys = setOf("camera", "mail", "browser", "notes"),
            recentAppKeys = listOf("notes", "browser"),
            launchCounts = mapOf("camera" to 20L, "mail" to 5L),
            labelByKey = mapOf(
                "camera" to "Camera",
                "mail" to "Mail",
                "browser" to "Browser",
                "notes" to "Notes",
            ),
            limit = 4,
        )

        assertEquals(listOf("notes", "browser", "camera", "mail"), suggested.toList())
    }

    @Test
    fun firstUseSuggestionsStayDeterministicInsteadOfFabricatingHistory() {
        val suggested = LauncherDrawerDiscoveryPolicy.suggestedKeys(
            availableKeys = setOf("z", "a", "m"),
            recentAppKeys = emptyList(),
            launchCounts = emptyMap(),
            labelByKey = mapOf("z" to "Zulu", "a" to "Alpha", "m" to "Mail"),
            limit = 3,
        )

        assertEquals(listOf("a", "m", "z"), suggested.toList())
    }

    @Test
    fun newAndUpdatedFiltersUseProfileQualifiedKeysOnly() {
        val freshness = mapOf(
            "user:0:mail" to LauncherAppFreshness(now - day, now - day),
            "user:10:mail" to LauncherAppFreshness(now - 90 * day, now - day),
        )
        val available = freshness.keys

        assertEquals(
            setOf("user:0:mail"),
            LauncherDrawerDiscoveryPolicy.filterKeys(
                filter = LauncherDrawerDiscoveryFilter.NEW,
                availableKeys = available,
                pinnedKeys = emptySet(),
                recentAppKeys = emptyList(),
                launchCounts = emptyMap(),
                labelByKey = emptyMap(),
                freshnessByKey = freshness,
                nowMillis = now,
            ),
        )
        assertEquals(
            setOf("user:10:mail"),
            LauncherDrawerDiscoveryPolicy.filterKeys(
                filter = LauncherDrawerDiscoveryFilter.UPDATED,
                availableKeys = available,
                pinnedKeys = emptySet(),
                recentAppKeys = emptyList(),
                launchCounts = emptyMap(),
                labelByKey = emptyMap(),
                freshnessByKey = freshness,
                nowMillis = now,
            ),
        )
    }
}
