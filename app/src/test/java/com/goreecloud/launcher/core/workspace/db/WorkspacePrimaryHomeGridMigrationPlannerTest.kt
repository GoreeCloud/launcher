package com.goreecloud.launcher.core.workspace.db

import com.goreecloud.launcher.core.workspace.WorkspaceWidgetCatalog
import com.goreecloud.launcher.core.workspace.WorkspaceWidgetDescriptor
import com.goreecloud.launcher.core.workspace.WorkspaceWidgetKeyCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspacePrimaryHomeGridMigrationPlannerTest {
    @Test
    fun plansCanonicalFavoritesIntoFourColumnRowMajorGrid() {
        val result = WorkspacePrimaryHomeGridMigrationPlanner.plan(
            page = primaryPage(),
            items = listOf(
                primaryItem(rank = 4, appKey = "app-e"),
                primaryItem(rank = 1, appKey = "app-b"),
                primaryItem(rank = 0, appKey = "app-a"),
                primaryItem(rank = 3, appKey = "app-d"),
                primaryItem(rank = 2, appKey = "app-c"),
            ),
        )

        assertTrue(result is WorkspacePrimaryHomeGridMigrationPlanningResult.Planned)
        val plan = (result as WorkspacePrimaryHomeGridMigrationPlanningResult.Planned).plan
        assertEquals(4, plan.grid.columns)
        assertEquals(2, plan.grid.rows)
        assertEquals((0..4).toList(), plan.sourceItems.map { it.rank })
        assertEquals(
            listOf(
                0 to 0,
                1 to 0,
                2 to 0,
                3 to 0,
                0 to 1,
            ),
            plan.migratedItems.map { checkNotNull(it.cellX) to checkNotNull(it.cellY) },
        )
        assertEquals(plan.sourceItems.map { it.itemId }, plan.migratedItems.map { it.itemId })
        assertEquals(plan.sourceItems.map { it.appKey }, plan.migratedItems.map { it.appKey })
        assertEquals(plan.sourceItems.map { it.rank }, plan.migratedItems.map { it.rank })
    }

    @Test
    fun plansAgainstRequestedColumnsAndRows() {
        val result = WorkspacePrimaryHomeGridMigrationPlanner.plan(
            page = primaryPage(),
            items = (0..5).map { rank -> primaryItem(rank, "app-$rank") },
            columns = 5,
            rows = 4,
        )
        assertTrue(result is WorkspacePrimaryHomeGridMigrationPlanningResult.Planned)
        val plan = (result as WorkspacePrimaryHomeGridMigrationPlanningResult.Planned).plan
        assertEquals(5, plan.grid.columns)
        assertEquals(4, plan.grid.rows)
        assertEquals(
            0 to 1,
            checkNotNull(plan.migratedItems.last().cellX) to
                checkNotNull(plan.migratedItems.last().cellY),
        )
    }

    @Test
    fun requestedGridFailsClosedWhenFavoritesExceedCapacity() {
        assertEquals(
            WorkspacePrimaryHomeGridMigrationPlanningResult.InvalidPrimaryItems,
            WorkspacePrimaryHomeGridMigrationPlanner.plan(
                page = primaryPage(),
                items = (0..16).map { rank -> primaryItem(rank, "app-$rank") },
                columns = 4,
                rows = 4,
            ),
        )
    }

    @Test
    fun emptyPrimaryPageRequiresNoMigrationPlan() {
        assertEquals(
            WorkspacePrimaryHomeGridMigrationPlanningResult.Empty,
            WorkspacePrimaryHomeGridMigrationPlanner.plan(primaryPage(), emptyList()),
        )
    }

    @Test
    fun rejectsWrongPrimaryPageIdentityContainerOrRank() {
        val item = primaryItem(rank = 0, appKey = "app-a")
        listOf(
            primaryPage().copy(pageId = "home:1"),
            primaryPage().copy(containerType = WorkspaceContainerType.DOCK),
            primaryPage().copy(rank = 1),
        ).forEach { page ->
            assertEquals(
                WorkspacePrimaryHomeGridMigrationPlanningResult.InvalidPrimaryPage,
                WorkspacePrimaryHomeGridMigrationPlanner.plan(page, listOf(item)),
            )
        }
    }

    @Test
    fun rejectsMixedOrHalfNullCoordinateState() {
        val mixed = listOf(
            primaryItem(rank = 0, appKey = "app-a"),
            primaryItem(rank = 1, appKey = "app-b").copy(cellX = 1, cellY = 0),
        )
        val halfNull = listOf(
            primaryItem(rank = 0, appKey = "app-a").copy(cellX = 0, cellY = null),
        )

        assertEquals(
            WorkspacePrimaryHomeGridMigrationPlanningResult.InvalidPrimaryItems,
            WorkspacePrimaryHomeGridMigrationPlanner.plan(primaryPage(), mixed),
        )
        assertEquals(
            WorkspacePrimaryHomeGridMigrationPlanningResult.InvalidPrimaryItems,
            WorkspacePrimaryHomeGridMigrationPlanner.plan(primaryPage(), halfNull),
        )
    }

    @Test
    fun rejectsNonCanonicalPrimaryItemShapeAndRanks() {
        val invalidVariants = listOf(
            listOf(primaryItem(rank = 1, appKey = "app-a")),
            listOf(primaryItem(rank = 0, appKey = "app-a").copy(pageId = "home:1")),
            listOf(primaryItem(rank = 0, appKey = "app-a").copy(itemType = WorkspaceItemType.FOLDER)),
            listOf(primaryItem(rank = 0, appKey = "app-a").copy(appKey = null)),
            listOf(primaryItem(rank = 0, appKey = "app-a").copy(itemId = "native:item:a")),
            listOf(primaryItem(rank = 0, appKey = "app-a").copy(spanX = 2)),
            listOf(
                primaryItem(rank = 0, appKey = "app-a"),
                primaryItem(rank = 1, appKey = "app-a"),
            ),
        )

        invalidVariants.forEach { items ->
            assertEquals(
                WorkspacePrimaryHomeGridMigrationPlanningResult.InvalidPrimaryItems,
                WorkspacePrimaryHomeGridMigrationPlanner.plan(primaryPage(), items),
            )
        }
    }

    @Test
    fun acceptsAlreadySpatialAppsAndWidgetsWhenSpansDoNotCollide() {
        val items = listOf(
            primaryItem(rank = 0, appKey = "app-a").copy(cellX = 0, cellY = 0),
            WorkspaceItemEntity(
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
            ),
        )

        assertEquals(
            WorkspacePrimaryHomeGridMigrationPlanningResult.AlreadySpatial,
            WorkspacePrimaryHomeGridMigrationPlanner.plan(
                page = primaryPage(),
                items = items,
                columns = 4,
                rows = 4,
            ),
        )
    }

    @Test
    fun acceptsAlreadySpatialFoldersAlongsideApps() {
        val items = listOf(
            primaryItem(rank = 0, appKey = "app-a").copy(cellX = 0, cellY = 0),
            WorkspaceItemEntity(
                itemId = "folder:home:test",
                pageId = WorkspaceLegacyImportMapper.HOME_PAGE_ID,
                itemType = WorkspaceItemType.FOLDER,
                appKey = "folder-test",
                rank = 1,
                cellX = 1,
                cellY = 0,
                spanX = 1,
                spanY = 1,
            ),
        )

        assertEquals(
            WorkspacePrimaryHomeGridMigrationPlanningResult.AlreadySpatial,
            WorkspacePrimaryHomeGridMigrationPlanner.plan(
                page = primaryPage(),
                items = items,
                columns = 4,
                rows = 4,
            ),
        )
    }

    @Test
    fun acceptsAlreadySpatialStateWhenItIsValidInsideRequestedGrid() {
        val alreadySpatial = listOf(
            primaryItem(rank = 0, appKey = "app-a").copy(cellX = 0, cellY = 0),
            primaryItem(rank = 1, appKey = "app-b").copy(cellX = 1, cellY = 0),
            primaryItem(rank = 2, appKey = "app-c").copy(cellX = 2, cellY = 0),
            primaryItem(rank = 3, appKey = "app-d").copy(cellX = 3, cellY = 0),
            primaryItem(rank = 4, appKey = "app-e").copy(cellX = 0, cellY = 1),
        )
        assertEquals(
            WorkspacePrimaryHomeGridMigrationPlanningResult.AlreadySpatial,
            WorkspacePrimaryHomeGridMigrationPlanner.plan(primaryPage(), alreadySpatial),
        )

        val rearranged = alreadySpatial.toMutableList().also {
            it[0] = it[0].copy(cellX = 1, cellY = 0)
            it[1] = it[1].copy(cellX = 0, cellY = 0)
        }
        assertEquals(
            WorkspacePrimaryHomeGridMigrationPlanningResult.AlreadySpatial,
            WorkspacePrimaryHomeGridMigrationPlanner.plan(primaryPage(), rearranged),
        )

        val collision = rearranged.toMutableList().also {
            it[1] = it[1].copy(cellX = 1, cellY = 0)
        }
        assertEquals(
            WorkspacePrimaryHomeGridMigrationPlanningResult.InvalidPrimaryItems,
            WorkspacePrimaryHomeGridMigrationPlanner.plan(primaryPage(), collision),
        )
    }

    private fun primaryPage() = WorkspacePageEntity(
        pageId = WorkspaceLegacyImportMapper.HOME_PAGE_ID,
        containerType = WorkspaceContainerType.HOME,
        rank = 0,
    )

    private fun primaryItem(rank: Int, appKey: String) = WorkspaceItemEntity(
        itemId = "legacy:home:$appKey",
        pageId = WorkspaceLegacyImportMapper.HOME_PAGE_ID,
        itemType = WorkspaceItemType.APP,
        appKey = appKey,
        rank = rank,
        cellX = null,
        cellY = null,
    )
}
