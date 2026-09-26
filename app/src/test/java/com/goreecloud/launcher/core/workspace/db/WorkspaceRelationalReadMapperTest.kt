package com.goreecloud.launcher.core.workspace.db

import com.goreecloud.launcher.core.workspace.WorkspaceWidgetCatalog
import com.goreecloud.launcher.core.workspace.WorkspaceWidgetDescriptor
import com.goreecloud.launcher.core.workspace.WorkspaceWidgetKeyCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkspaceRelationalReadMapperTest {
    @Test
    fun spatialFoldersDoNotPolluteFavoriteProjection() {
        val pages = WorkspaceLegacyImportMapper.map(
            favoriteKeys = emptyList(),
            dockKeys = emptyList(),
        ).pages
        val items = listOf(
            WorkspaceItemEntity(
                itemId = "legacy:home:app-a",
                pageId = WorkspaceLegacyImportMapper.HOME_PAGE_ID,
                itemType = WorkspaceItemType.APP,
                appKey = "app-a",
                rank = 0,
                cellX = 0,
                cellY = 0,
            ),
            WorkspaceItemEntity(
                itemId = "folder:home:test",
                pageId = WorkspaceLegacyImportMapper.HOME_PAGE_ID,
                itemType = WorkspaceItemType.FOLDER,
                appKey = "folder-id",
                rank = 1,
                cellX = 1,
                cellY = 0,
            ),
        )

        assertEquals(
            WorkspaceRelationalSnapshot(
                favoriteKeys = listOf("app-a"),
                dockKeys = emptyList(),
            ),
            WorkspaceRelationalReadMapper.map(pages, items),
        )
    }

    @Test
    fun canonicalRowsReconstructFavoriteAndDockOrder() {
        val expected = WorkspaceLegacyImportMapper.map(
            favoriteKeys = listOf("profile:alpha", "profile:beta"),
            dockKeys = listOf("profile:dock"),
        )

        assertEquals(
            WorkspaceRelationalSnapshot(
                favoriteKeys = listOf("profile:alpha", "profile:beta"),
                dockKeys = listOf("profile:dock"),
            ),
            WorkspaceRelationalReadMapper.map(
                pages = expected.pages.reversed(),
                items = expected.items.reversed(),
            ),
        )
    }

    @Test
    fun spatialPrimaryRowsPreserveFavoriteRankOrder() {
        val expected = WorkspaceLegacyImportMapper.map(
            favoriteKeys = listOf("profile:alpha", "profile:beta"),
            dockKeys = listOf("profile:dock"),
        )
        val spatialItems = expected.items.map { item ->
            when (item.appKey) {
                "profile:alpha" -> item.copy(cellX = 3, cellY = 4)
                "profile:beta" -> item.copy(cellX = 0, cellY = 0)
                else -> item
            }
        }

        assertEquals(
            WorkspaceRelationalSnapshot(
                favoriteKeys = listOf("profile:alpha", "profile:beta"),
                dockKeys = listOf("profile:dock"),
            ),
            WorkspaceRelationalReadMapper.map(
                pages = expected.pages,
                items = spatialItems,
            ),
        )
    }

    @Test
    fun validSpatialWidgetDoesNotPolluteFavoriteCompatibilityProjection() {
        val expected = WorkspaceLegacyImportMapper.map(
            favoriteKeys = listOf("profile:alpha"),
            dockKeys = listOf("profile:dock"),
        )
        val spatialApps = expected.items.map { item ->
            if (item.pageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID) {
                item.copy(cellX = 0, cellY = 0)
            } else {
                item
            }
        }
        val widget = WorkspaceItemEntity(
            itemId = "widget:builtin:test",
            pageId = WorkspaceLegacyImportMapper.HOME_PAGE_ID,
            itemType = WorkspaceItemType.WIDGET,
            appKey = WorkspaceWidgetKeyCodec.encode(
                WorkspaceWidgetDescriptor.BuiltIn(WorkspaceWidgetCatalog.CLOCK),
            ),
            rank = 1,
            cellX = 1,
            cellY = 0,
            spanX = 2,
            spanY = 2,
        )

        assertEquals(
            WorkspaceRelationalSnapshot(
                favoriteKeys = listOf("profile:alpha"),
                dockKeys = listOf("profile:dock"),
            ),
            WorkspaceRelationalReadMapper.map(
                pages = expected.pages,
                items = spatialApps + widget,
            ),
        )
    }

    @Test
    fun spatialPrimaryRowsFailClosedOnCollisionOrUnsupportedBounds() {
        val expected = WorkspaceLegacyImportMapper.map(
            favoriteKeys = listOf("profile:alpha", "profile:beta"),
            dockKeys = emptyList(),
        )
        val collision = expected.items.map {
            if (it.pageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID) {
                it.copy(cellX = 1, cellY = 1)
            } else {
                it
            }
        }
        val outOfBounds = expected.items.map { item ->
            if (item.appKey == "profile:alpha") item.copy(cellX = 6, cellY = 0)
            else if (item.appKey == "profile:beta") item.copy(cellX = 0, cellY = 0)
            else item
        }

        assertNull(WorkspaceRelationalReadMapper.map(expected.pages, collision))
        assertNull(WorkspaceRelationalReadMapper.map(expected.pages, outOfBounds))
    }

    @Test
    fun nonCanonicalRankFailsClosed() {
        val expected = WorkspaceLegacyImportMapper.map(
            favoriteKeys = listOf("profile:alpha", "profile:beta"),
            dockKeys = emptyList(),
        )
        val malformedItems = expected.items.map { item ->
            if (item.appKey == "profile:beta") item.copy(rank = 4) else item
        }

        assertNull(
            WorkspaceRelationalReadMapper.map(
                pages = expected.pages,
                items = malformedItems,
            )
        )
    }

    @Test
    fun missingCompatibilityPageFailsClosed() {
        val expected = WorkspaceLegacyImportMapper.map(
            favoriteKeys = listOf("profile:alpha"),
            dockKeys = listOf("profile:dock"),
        )

        assertNull(
            WorkspaceRelationalReadMapper.map(
                pages = expected.pages.filterNot {
                    it.pageId == WorkspaceLegacyImportMapper.DOCK_PAGE_ID
                },
                items = expected.items,
            )
        )
    }

    @Test
    fun malformedApplicationRecordFailsClosed() {
        val expected = WorkspaceLegacyImportMapper.map(
            favoriteKeys = listOf("profile:alpha"),
            dockKeys = emptyList(),
        )
        val malformedItems = expected.items.map { it.copy(appKey = null) }

        assertNull(
            WorkspaceRelationalReadMapper.map(
                pages = expected.pages,
                items = malformedItems,
            )
        )
    }
}
