package com.goreecloud.launcher.core.workspace.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkspaceLegacyImportMapperTest {
    @Test
    fun mapCreatesHomeAndDockPagesWithStableRanks() {
        val result = WorkspaceLegacyImportMapper.map(
            favoriteKeys = listOf("profile:one", "profile:two"),
            dockKeys = listOf("profile:two", "profile:three"),
        )

        assertEquals(
            listOf(
                WorkspacePageEntity(WorkspaceLegacyImportMapper.HOME_PAGE_ID, WorkspaceContainerType.HOME, 0),
                WorkspacePageEntity(WorkspaceLegacyImportMapper.DOCK_PAGE_ID, WorkspaceContainerType.DOCK, 0),
            ),
            result.pages,
        )
        assertEquals(
            listOf("profile:one", "profile:two"),
            result.items.filter { it.pageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID }.map { it.appKey },
        )
        assertEquals(
            listOf("profile:two", "profile:three"),
            result.items.filter { it.pageId == WorkspaceLegacyImportMapper.DOCK_PAGE_ID }.map { it.appKey },
        )
        assertEquals(
            listOf(0, 1),
            result.items.filter { it.pageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID }.map { it.rank },
        )
    }

    @Test
    fun mapDeduplicatesWithinContainerButAllowsAppInHomeAndDock() {
        val result = WorkspaceLegacyImportMapper.map(
            favoriteKeys = listOf("same", "same"),
            dockKeys = listOf("same", "same"),
        )

        assertEquals(2, result.items.size)
        assertEquals(2, result.items.map { it.itemId }.distinct().size)
        assertEquals(listOf(WorkspaceItemType.APP, WorkspaceItemType.APP), result.items.map { it.itemType })
        result.items.forEach {
            assertNull(it.cellX)
            assertNull(it.cellY)
            assertEquals(1, it.spanX)
            assertEquals(1, it.spanY)
        }
    }

    @Test
    fun mapPreservesEmptyContainersAsFuturePageAnchors() {
        val result = WorkspaceLegacyImportMapper.map(emptyList(), emptyList())

        assertEquals(2, result.pages.size)
        assertEquals(emptyList<WorkspaceItemEntity>(), result.items)
    }
}
