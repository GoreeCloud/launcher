package com.goreecloud.launcher.ui

import com.goreecloud.launcher.core.launcher.LauncherDrawerLayoutMode
import com.goreecloud.launcher.core.launcher.LauncherDrawerSpacing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherDrawerSortingPolicyTest {
    private data class Entry(val label: String, val stableKey: String)

    @Test
    fun quickLayoutCycleVisitsEveryDrawerModeAndReturnsToGrid() {
        var mode = LauncherDrawerLayoutMode.GRID
        val visited = mutableListOf(mode)
        repeat(4) {
            mode = nextLauncherDrawerLayoutMode(mode)
            visited += mode
        }
        assertEquals(
            listOf(
                LauncherDrawerLayoutMode.GRID,
                LauncherDrawerLayoutMode.COMPACT,
                LauncherDrawerLayoutMode.LIST,
                LauncherDrawerLayoutMode.CATEGORY,
                LauncherDrawerLayoutMode.GRID,
            ),
            visited,
        )
    }

    @Test
    fun appsAndFoldersInterleaveAlphabeticallyWithoutEmptyGridSlots() {
        val entries = listOf(
            Entry("Camera", "app:camera"),
            Entry("Banking", "folder:banking"),
            Entry("Zebra", "app:zebra"),
            Entry("Calculator", "app:calculator"),
            Entry("Media", "folder:media"),
        )
        val sorted = LauncherDrawerSortingPolicy.order(entries, { it.label }, { it.stableKey })
        assertEquals(
            listOf("folder:banking", "app:calculator", "app:camera", "folder:media", "app:zebra"),
            sorted.map { it.stableKey },
        )
        assertEquals(entries.size, sorted.size)
    }

    @Test
    fun caseFoldAndStableKeysMakeTiesDeterministic() {
        val entries = listOf(
            Entry("Camera", "folder:camera"),
            Entry("camera", "app:camera"),
            Entry("Banking", "folder:banking"),
            Entry("CAMERA", "app:camera-2"),
        )
        val sorted = LauncherDrawerSortingPolicy.order(entries, { it.label }, { it.stableKey })
        assertEquals(
            listOf("folder:banking", "app:camera", "app:camera-2", "folder:camera"),
            sorted.map { it.stableKey },
        )
    }

    @Test
    fun canonicallyEquivalentUnicodeLabelsShareStableAppFolderTieBreaks() {
        val entries = listOf(
            Entry("Caf\u00e9", "folder:cafe"),
            Entry("Camera", "app:camera"),
            Entry("Cafe\u0301", "app:cafe"),
        )
        val expected = listOf("app:cafe", "folder:cafe", "app:camera")
        val labelsFirst = LauncherDrawerSortingPolicy.order(entries, { it.label }, { it.stableKey })
        val keysFirst = LauncherDrawerSortingPolicy.order(entries.reversed(), { it.label }, { it.stableKey })
        assertEquals(expected, labelsFirst.map { it.stableKey })
        assertEquals(expected, keysFirst.map { it.stableKey })
    }

    @Test
    fun alphabetTargetsExposeFirstStableIndexForFastNavigation() {
        val entries = listOf(
            Entry("1Password", "app:one-password"),
            Entry("Alpha", "app:alpha"),
            Entry("Alarm", "app:alarm"),
            Entry("Browser", "app:browser"),
            Entry("Camera", "app:camera"),
        )

        assertEquals(
            listOf("#" to 0, "A" to 1, "B" to 3, "C" to 4),
            launcherDrawerAlphabetTargets(entries) { it.label },
        )
        assertEquals("A", launcherDrawerAlphabetBucket("  alpha "))
    }

    @Test
    fun reverseAlphabeticalKeepsEquivalentLabelTieBreaksStable() {
        val entries = listOf(
            Entry("Alpha", "folder:alpha"),
            Entry("Zulu", "app:zulu"),
            Entry("alpha", "app:alpha"),
            Entry("Media", "folder:media"),
        )

        val sorted = LauncherDrawerSortingPolicy.order(
            entries = entries,
            label = { it.label },
            key = { it.stableKey },
            sortOrder = LauncherDrawerSortOrder.REVERSE_ALPHABETICAL,
        )

        assertEquals(
            listOf("app:zulu", "folder:media", "app:alpha", "folder:alpha"),
            sorted.map { it.stableKey },
        )
    }

    @Test
    fun mostRecentPlacesKnownUsageFirstAndKeepsUnknownEntriesAlphabetical() {
        val entries = listOf(
            Entry("Camera", "app:camera"),
            Entry("Banking", "folder:banking"),
            Entry("Maps", "app:maps"),
            Entry("Alarm", "app:alarm"),
        )
        val recent = mapOf(
            "app:maps" to 0,
            "app:camera" to 1,
        )

        val sorted = LauncherDrawerSortingPolicy.order(
            entries = entries,
            label = { it.label },
            key = { it.stableKey },
            sortOrder = LauncherDrawerSortOrder.MOST_RECENT,
            recentRank = { recent[it.stableKey] },
        )

        assertEquals(
            listOf("app:maps", "app:camera", "app:alarm", "folder:banking"),
            sorted.map { it.stableKey },
        )
    }

    @Test
    fun recentlyInstalledSortsByProfileQualifiedInstallTimeThenAlphabeticalUnknowns() {
        val entries = listOf(
            Entry("Camera", "app:user:0:camera"),
            Entry("Banking", "folder:banking"),
            Entry("Maps", "app:user:10:maps"),
            Entry("Alarm", "app:user:0:alarm"),
        )
        val installTimes = mapOf(
            "app:user:10:maps" to 400L,
            "app:user:0:camera" to 200L,
            "app:user:0:alarm" to 300L,
        )

        val sorted = LauncherDrawerSortingPolicy.order(
            entries = entries,
            label = { it.label },
            key = { it.stableKey },
            sortOrder = LauncherDrawerSortOrder.RECENTLY_INSTALLED,
            installTimeMillis = { installTimes[it.stableKey] },
        )

        assertEquals(
            listOf(
                "app:user:10:maps",
                "app:user:0:alarm",
                "app:user:0:camera",
                "folder:banking",
            ),
            sorted.map { it.stableKey },
        )
    }

    @Test
    fun mostFrequentSortsDescendingThenUsesAlphabeticalTieBreaks() {
        val entries = listOf(
            Entry("Camera", "app:camera"),
            Entry("Banking", "folder:banking"),
            Entry("Maps", "app:maps"),
            Entry("Alarm", "app:alarm"),
        )
        val frequency = mapOf(
            "app:camera" to 4L,
            "app:maps" to 9L,
            "app:alarm" to 4L,
        )

        val sorted = LauncherDrawerSortingPolicy.order(
            entries = entries,
            label = { it.label },
            key = { it.stableKey },
            sortOrder = LauncherDrawerSortOrder.MOST_FREQUENT,
            frequency = { frequency[it.stableKey] },
        )

        assertEquals(
            listOf("app:maps", "app:alarm", "app:camera", "folder:banking"),
            sorted.map { it.stableKey },
        )
    }

    @Test
    fun pinnedFirstKeepsPinnedAppsAheadOfUnpinnedAppsAndFolders() {
        val entries = listOf(
            Entry("Camera", "app:0:camera"),
            Entry("Banking", "folder:banking"),
            Entry("Maps", "app:10:maps"),
            Entry("Alarm", "app:0:alarm"),
            Entry("Browser", "app:10:browser"),
        )
        val pinned = setOf("app:10:maps", "app:0:alarm")

        val sorted = LauncherDrawerSortingPolicy.order(
            entries = entries,
            label = { it.label },
            key = { it.stableKey },
            sortOrder = LauncherDrawerSortOrder.PINNED_FIRST,
            pinned = { it.stableKey in pinned },
        )

        assertEquals(
            listOf(
                "app:0:alarm",
                "app:10:maps",
                "folder:banking",
                "app:10:browser",
                "app:0:camera",
            ),
            sorted.map { it.stableKey },
        )
    }

    @Test
    fun pinnedFirstUsesExplicitPinnedRankBeforeAlphabeticalFallback() {
        val entries = listOf(
            Entry("Alpha", "app:alpha"),
            Entry("Bravo", "app:bravo"),
            Entry("Charlie", "app:charlie"),
            Entry("Folder", "folder:folder"),
        )
        val pinned = setOf("app:alpha", "app:bravo", "app:charlie")
        val ranks = mapOf(
            "app:charlie" to 0,
            "app:alpha" to 1,
            "app:bravo" to 2,
        )

        val sorted = LauncherDrawerSortingPolicy.order(
            entries = entries,
            label = { it.label },
            key = { it.stableKey },
            sortOrder = LauncherDrawerSortOrder.PINNED_FIRST,
            pinned = { it.stableKey in pinned },
            pinnedRank = { ranks[it.stableKey] },
        )

        assertEquals(
            listOf("app:charlie", "app:alpha", "app:bravo", "folder:folder"),
            sorted.map { it.stableKey },
        )
    }

    @Test
    fun gridGeometryKeepsIconAndLabelSlotsFixedAcrossSpacingModes() {
        val grid = LauncherDrawerSpacing.entries.map { spacing ->
            LauncherDrawerGridPolicy.geometry(compact = false, spacing = spacing)
        }
        val compact = LauncherDrawerSpacing.entries.map { spacing ->
            LauncherDrawerGridPolicy.geometry(compact = true, spacing = spacing)
        }

        assertEquals(
            setOf(LauncherDrawerGridPolicy.ICON_SLOT_HEIGHT_DP),
            (grid + compact).map { it.iconSlotHeightDp }.toSet(),
        )
        assertEquals(
            setOf(LauncherDrawerGridPolicy.GRID_LABEL_SLOT_HEIGHT_DP),
            grid.map { it.labelSlotHeightDp }.toSet(),
        )
        assertEquals(
            setOf(LauncherDrawerGridPolicy.COMPACT_LABEL_SLOT_HEIGHT_DP),
            compact.map { it.labelSlotHeightDp }.toSet(),
        )
    }

    @Test
    fun gridTileHeightAlwaysFitsReservedIconAndLabelSlots() {
        LauncherDrawerSpacing.entries.forEach { spacing ->
            listOf(false, true).forEach { compact ->
                val geometry = LauncherDrawerGridPolicy.geometry(compact, spacing)
                val labelGapDp = if (compact) 3 else 4
                val verticalPaddingDp = 4
                assertTrue(
                    geometry.tileHeightDp >=
                        geometry.iconSlotHeightDp +
                        geometry.labelSlotHeightDp +
                        labelGapDp +
                        verticalPaddingDp,
                )
            }
        }
    }

    @Test
    fun drawerPageIndicatorKeepsAccessibleTargetWithSmallVisualDots() {
        assertTrue(
            LauncherDrawerPageIndicatorPolicy.TOUCH_TARGET_DP >= 48,
        )
        assertEquals(
            8,
            LauncherDrawerPageIndicatorPolicy.visualSizeDp(selected = true),
        )
        assertEquals(
            6,
            LauncherDrawerPageIndicatorPolicy.visualSizeDp(selected = false),
        )
        assertTrue(
            LauncherDrawerPageIndicatorPolicy.SELECTED_VISUAL_DP <
                LauncherDrawerPageIndicatorPolicy.TOUCH_TARGET_DP,
        )
        assertTrue(
            LauncherDrawerPageIndicatorPolicy.IDLE_VISUAL_DP <
                LauncherDrawerPageIndicatorPolicy.TOUCH_TARGET_DP,
        )
    }

    @Test
    fun emptyAndSingleEntryListsDoNotRequireScaffolding() {
        assertEquals(
            emptyList<Entry>(),
            LauncherDrawerSortingPolicy.order(emptyList<Entry>(), { it.label }, { it.stableKey }),
        )
        val lone = Entry("Banking", "folder:banking")
        assertEquals(
            listOf(lone),
            LauncherDrawerSortingPolicy.order(listOf(lone), { it.label }, { it.stableKey }),
        )
    }
}
