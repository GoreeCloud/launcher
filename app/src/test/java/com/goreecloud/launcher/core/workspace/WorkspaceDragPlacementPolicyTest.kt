package com.goreecloud.launcher.core.workspace

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkspaceDragPlacementPolicyTest {
    @Test
    fun homeToDockMovesInsteadOfCopiesAndHonorsInsertionTarget() {
        val result = WorkspaceDragPlacementPolicy.moveHomeToDock(
            favoriteKeys = listOf("home-a", "home-b"),
            dockKeys = listOf("dock-a", "dock-b"),
            key = "home-a",
            targetDockKey = "dock-b",
        )

        assertEquals(listOf("home-b"), result.favoriteKeys)
        assertEquals(listOf("dock-a", "home-a", "dock-b"), result.dockKeys)
    }

    @Test
    fun homeToDockGrowsBeyondFiveItemsAndPreservesInsertionTarget() {
        val result = WorkspaceDragPlacementPolicy.moveHomeToDock(
            favoriteKeys = listOf("home"),
            dockKeys = listOf("a", "b", "c", "d", "e"),
            key = "home",
            targetDockKey = "c",
        )

        assertEquals(emptyList<String>(), result.favoriteKeys)
        assertEquals(listOf("a", "b", "home", "c", "d", "e"), result.dockKeys)
    }

    @Test
    fun dockToHomeMovesAndHomeCapacityFailsClosed() {
        val moved = WorkspaceDragPlacementPolicy.moveDockToHome(
            favoriteKeys = listOf("home-a"),
            dockKeys = listOf("dock-a", "dock-b"),
            key = "dock-a",
            homeLimit = 4,
        )
        assertEquals(listOf("home-a", "dock-a"), moved.favoriteKeys)
        assertEquals(listOf("dock-b"), moved.dockKeys)

        val full = WorkspaceDragPlacementPolicy.moveDockToHome(
            favoriteKeys = listOf("h1", "h2"),
            dockKeys = listOf("dock-a"),
            key = "dock-a",
            homeLimit = 2,
        )
        assertEquals(listOf("h1", "h2"), full.favoriteKeys)
        assertEquals(listOf("dock-a"), full.dockKeys)
    }

    @Test
    fun drawerCopiesRemainInInventoryAndDoNotRemoveOtherPlacement() {
        val home = WorkspaceDragPlacementPolicy.copyDrawerToHome(
            favoriteKeys = listOf("home-a"),
            dockKeys = listOf("dock-a"),
            key = "drawer-app",
            homeLimit = 4,
        )
        assertEquals(listOf("home-a", "drawer-app"), home.favoriteKeys)
        assertEquals(listOf("dock-a"), home.dockKeys)

        val dock = WorkspaceDragPlacementPolicy.copyDrawerToDock(
            favoriteKeys = home.favoriteKeys,
            dockKeys = home.dockKeys,
            key = "drawer-app",
            targetDockKey = "dock-a",
        )
        assertEquals(home.favoriteKeys, dock.favoriteKeys)
        assertEquals(listOf("drawer-app", "dock-a"), dock.dockKeys)
    }

    @Test
    fun dockReorderSupportsAppendByDroppingAfterLastSlot() {
        val result = WorkspaceDragPlacementPolicy.reorderDock(
            favoriteKeys = emptyList(),
            dockKeys = listOf("a", "b", "c"),
            key = "a",
            targetDockKey = null,
        )

        assertEquals(listOf("b", "c", "a"), result.dockKeys)
    }
}
