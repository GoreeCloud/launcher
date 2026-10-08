package com.goreecloud.launcher.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherDockDragPagePolicyTest {
    @Test
    fun edgeHandoffRequiresInBoundsAvailableDirection() {
        assertEquals(
            LauncherDockDragPageDirection.PREVIOUS,
            launcherDockDragPageDirection(12f, 40f, 0f, 0f, 300f, 90f, 48f, true, true),
        )
        assertEquals(
            LauncherDockDragPageDirection.NEXT,
            launcherDockDragPageDirection(288f, 40f, 0f, 0f, 300f, 90f, 48f, true, true),
        )
        assertEquals(
            null,
            launcherDockDragPageDirection(288f, 120f, 0f, 0f, 300f, 90f, 48f, true, true),
        )
        assertEquals(
            null,
            launcherDockDragPageDirection(8f, 40f, 0f, 0f, 300f, 90f, 48f, false, true),
        )
    }

    @Test
    fun followingPageBoundaryPreservesFlatDockOrder() {
        val pages = listOf(
            listOf("a", "b", "c"),
            listOf("d", "e", "f"),
            listOf("g"),
        )
        assertEquals("d", launcherDockNextPageInsertionKey(pages, 0, "b"))
        assertEquals("e", launcherDockNextPageInsertionKey(pages, 0, "d"))
        assertEquals("g", launcherDockNextPageInsertionKey(pages, 1, "e"))
        assertEquals(null, launcherDockNextPageInsertionKey(pages, 2, "g"))
        assertEquals("a", launcherDockNextPageInsertionKey(pages, 2, "g", loop = true))
        assertEquals("b", launcherDockNextPageInsertionKey(pages, 2, "a", loop = true))
    }
}
