package com.goreecloud.launcher.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherDrawerSortingPolicyTest {
    private data class Entry(val label: String, val stableKey: String)

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
