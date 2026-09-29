package com.goreecloud.launcher.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherFileSearchRootIsolationTest {
    @Test
    fun failedRootDoesNotSuppressHealthyRoot() {
        val scanned = mutableListOf<String>()

        val result = LauncherFileSearchRootIsolation.collect(
            roots = listOf("revoked", "healthy"),
            maxEntries = 8,
        ) { root, _ ->
            scanned += root
            if (root == "revoked") error("grant revoked")
            listOf("document-a", "document-b")
        }

        assertEquals(listOf("revoked", "healthy"), scanned)
        assertEquals(listOf("document-a", "document-b"), result)
    }

    @Test
    fun aggregateNeverExceedsGlobalLimit() {
        val remainingBudgets = mutableListOf<Int>()

        val result = LauncherFileSearchRootIsolation.collect(
            roots = listOf("first", "second"),
            maxEntries = 3,
        ) { root, remaining ->
            remainingBudgets += remaining
            if (root == "first") listOf("a", "b") else listOf("c", "d", "e")
        }

        assertEquals(listOf(3, 1), remainingBudgets)
        assertEquals(listOf("a", "b", "c"), result)
    }

    @Test
    fun zeroLimitSkipsEveryRoot() {
        var scanned = false

        val result = LauncherFileSearchRootIsolation.collect(
            roots = listOf("root"),
            maxEntries = 0,
        ) { _, _ ->
            scanned = true
            listOf("unexpected")
        }

        assertTrue(result.isEmpty())
        assertTrue(!scanned)
    }
}
