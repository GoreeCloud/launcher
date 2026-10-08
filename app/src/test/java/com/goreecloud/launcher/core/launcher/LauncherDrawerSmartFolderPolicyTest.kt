package com.goreecloud.launcher.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherDrawerSmartFolderPolicyTest {
    private val now = 1_800_000_000_000L

    @Test
    fun buildsOnlyNonEmptyDeterministicFolders() {
        val freshness = mapOf(
            "a" to LauncherAppFreshness(
                firstInstallTimeMillis = now - 1_000L,
                lastUpdateTimeMillis = now - 1_000L,
            ),
            "b" to LauncherAppFreshness(
                firstInstallTimeMillis = now - 100_000L,
                lastUpdateTimeMillis = now - 1_000L,
            ),
            "c" to LauncherAppFreshness(
                firstInstallTimeMillis = now - LauncherDrawerDiscoveryPolicy.FRESHNESS_WINDOW_MILLIS * 2,
                lastUpdateTimeMillis = now - LauncherDrawerDiscoveryPolicy.FRESHNESS_WINDOW_MILLIS * 2,
            ),
        )

        val folders = LauncherDrawerSmartFolderPolicy.build(
            availableKeys = linkedSetOf("a", "b", "c"),
            pinnedKeys = setOf("c"),
            recentAppKeys = listOf("b", "a"),
            launchCounts = mapOf("a" to 1L, "b" to 8L),
            labelByKey = mapOf("a" to "Alpha", "b" to "Beta", "c" to "Gamma"),
            freshnessByKey = freshness,
            nowMillis = now,
            includeSuggested = true,
        )

        assertEquals(
            listOf(
                LauncherDrawerSmartFolderKind.PINNED,
                LauncherDrawerSmartFolderKind.SUGGESTED,
                LauncherDrawerSmartFolderKind.NEW,
                LauncherDrawerSmartFolderKind.UPDATED,
            ),
            folders.map { it.kind },
        )
        assertEquals(listOf("c"), folders.first { it.kind == LauncherDrawerSmartFolderKind.PINNED }.memberKeys)
        assertTrue("a" in folders.first { it.kind == LauncherDrawerSmartFolderKind.NEW }.memberKeys)
        assertTrue("b" in folders.first { it.kind == LauncherDrawerSmartFolderKind.UPDATED }.memberKeys)
    }

    @Test
    fun suggestedFolderRespectsUserControl() {
        val folders = LauncherDrawerSmartFolderPolicy.build(
            availableKeys = linkedSetOf("a", "b"),
            pinnedKeys = emptySet(),
            recentAppKeys = listOf("a"),
            launchCounts = mapOf("a" to 5L),
            labelByKey = mapOf("a" to "Alpha", "b" to "Beta"),
            freshnessByKey = emptyMap(),
            nowMillis = now,
            includeSuggested = false,
        )

        assertFalse(folders.any { it.kind == LauncherDrawerSmartFolderKind.SUGGESTED })
    }

    @Test
    fun exclusionsHideMembersButKeepRecoveryVisible() {
        val folders = LauncherDrawerSmartFolderPolicy.build(
            availableKeys = linkedSetOf("a", "b"),
            pinnedKeys = emptySet(),
            recentAppKeys = listOf("a", "b"),
            launchCounts = mapOf("a" to 5L, "b" to 3L),
            labelByKey = mapOf("a" to "Alpha", "b" to "Beta"),
            freshnessByKey = emptyMap(),
            nowMillis = now,
            includeSuggested = true,
            excludedKeysByKind = mapOf(
                LauncherDrawerSmartFolderKind.SUGGESTED to setOf("a", "b"),
            ),
        )

        val suggested = folders.single { it.kind == LauncherDrawerSmartFolderKind.SUGGESTED }
        assertTrue(suggested.memberKeys.isEmpty())
        assertEquals(2, suggested.excludedCount)
    }

    @Test
    fun exclusionCodecIsKindScopedReversibleAndNeverOverridesPinnedAuthority() {
        val excluded = LauncherDrawerSmartFolderExclusions.setExcluded(
            raw = emptySet(),
            kind = LauncherDrawerSmartFolderKind.NEW,
            appKey = "user:42:com.example/.Main",
            excluded = true,
        )
        val decoded = LauncherDrawerSmartFolderExclusions.decode(excluded)
        assertEquals(
            setOf("user:42:com.example/.Main"),
            decoded[LauncherDrawerSmartFolderKind.NEW],
        )
        assertFalse(LauncherDrawerSmartFolderKind.UPDATED in decoded)
        assertEquals(
            excluded,
            LauncherDrawerSmartFolderExclusions.setExcluded(
                raw = excluded,
                kind = LauncherDrawerSmartFolderKind.PINNED,
                appKey = "user:42:com.example/.Main",
                excluded = true,
            ),
        )
        assertTrue(
            LauncherDrawerSmartFolderExclusions.clearKind(
                excluded,
                LauncherDrawerSmartFolderKind.NEW,
            ).isEmpty(),
        )
    }

    @Test
    fun emptyInventoryProducesNoSmartFolders() {
        assertTrue(
            LauncherDrawerSmartFolderPolicy.build(
                availableKeys = emptySet(),
                pinnedKeys = emptySet(),
                recentAppKeys = emptyList(),
                launchCounts = emptyMap(),
                labelByKey = emptyMap(),
                freshnessByKey = emptyMap(),
                nowMillis = now,
                includeSuggested = true,
            ).isEmpty(),
        )
    }
}
