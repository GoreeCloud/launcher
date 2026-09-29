package com.goreecloud.launcher.core.workspace.db

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkspaceRoomPlacementModelTest {
    @Test
    fun normalizationPreservesFavoriteOrderAndDeduplicates() {
        assertEquals(
            WorkspaceRelationalSnapshot(
                favoriteKeys = listOf("alpha", "beta", "gamma"),
                dockKeys = emptyList(),
            ),
            WorkspaceRoomPlacementModel.normalize(
                favoriteKeys = listOf("alpha", "beta", "alpha", "gamma"),
                dockKeys = emptyList(),
            ),
        )
    }

    @Test
    fun normalizationPreservesEveryDockItemWhileDeduplicating() {
        assertEquals(
            WorkspaceRelationalSnapshot(
                favoriteKeys = emptyList(),
                dockKeys = listOf("one", "two", "three", "four", "five", "six"),
            ),
            WorkspaceRoomPlacementModel.normalize(
                favoriteKeys = emptyList(),
                dockKeys = listOf("one", "two", "one", "three", "four", "five", "six"),
            ),
        )
    }
}
