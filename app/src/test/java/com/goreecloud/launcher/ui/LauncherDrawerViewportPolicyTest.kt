package com.goreecloud.launcher.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherDrawerViewportPolicyTest {
    @Test
    fun representativeProfileInventoriesStayComposedWhileScrolling() {
        assertTrue(launcherDrawerUsesEagerGrid(0))
        assertTrue(launcherDrawerUsesEagerGrid(76))
        assertTrue(launcherDrawerUsesEagerGrid(320))
        assertFalse(launcherDrawerUsesEagerGrid(321))
    }

    @Test
    fun stableRowsNeverDropEntries() {
        val input = (1..76).toList()
        val rows = launcherDrawerStableRows(input, columns = 5)

        assertTrue(rows.all { it.size in 1..5 })
        assertTrue(rows.flatten() == input)
    }
}
