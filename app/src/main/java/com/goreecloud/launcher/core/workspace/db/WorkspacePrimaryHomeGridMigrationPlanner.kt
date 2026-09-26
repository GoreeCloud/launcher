package com.goreecloud.launcher.core.workspace.db

import com.goreecloud.launcher.core.workspace.WorkspaceGridPlacement
import com.goreecloud.launcher.core.workspace.WorkspaceWidgetKeyCodec

data class WorkspacePrimaryHomeGridMigrationPlan(
    val grid: WorkspaceGridPlacement.Grid,
    val sourceItems: List<WorkspaceItemEntity>,
    val migratedItems: List<WorkspaceItemEntity>,
)

sealed interface WorkspacePrimaryHomeGridMigrationPlanningResult {
    data class Planned(
        val plan: WorkspacePrimaryHomeGridMigrationPlan,
    ) : WorkspacePrimaryHomeGridMigrationPlanningResult

    data object Empty : WorkspacePrimaryHomeGridMigrationPlanningResult
    data object AlreadySpatial : WorkspacePrimaryHomeGridMigrationPlanningResult
    data object InvalidPrimaryPage : WorkspacePrimaryHomeGridMigrationPlanningResult
    data object InvalidPrimaryItems : WorkspacePrimaryHomeGridMigrationPlanningResult
}

/**
 * Pure planning contract for a future primary HOME compatibility-to-grid migration.
 *
 * This planner deliberately owns no persistence or runtime authority. The accepted primary
 * compatibility projection remains rank-zero `home:0` with null coordinates until a later,
 * separately reviewed migration transaction changes that contract. Planning only proves that the
 * current canonical Favorite rows can be mapped deterministically into the existing four-column
 * primary Home presentation without collisions or identity changes.
 */
object WorkspacePrimaryHomeGridMigrationPlanner {
    const val PRIMARY_HOME_COLUMNS = 4
    const val MIN_PRIMARY_HOME_COLUMNS = 4
    const val MAX_PRIMARY_HOME_COLUMNS = 6
    const val MIN_PRIMARY_HOME_ROWS = 4
    const val MAX_PRIMARY_HOME_ROWS = 7

    fun plan(
        page: WorkspacePageEntity,
        items: List<WorkspaceItemEntity>,
        columns: Int = PRIMARY_HOME_COLUMNS,
        rows: Int? = null,
    ): WorkspacePrimaryHomeGridMigrationPlanningResult {
        if (
            columns !in MIN_PRIMARY_HOME_COLUMNS..MAX_PRIMARY_HOME_COLUMNS ||
            (rows != null && rows !in MIN_PRIMARY_HOME_ROWS..MAX_PRIMARY_HOME_ROWS)
        ) {
            return WorkspacePrimaryHomeGridMigrationPlanningResult.InvalidPrimaryItems
        }
        if (
            page.pageId != WorkspaceLegacyImportMapper.HOME_PAGE_ID ||
            page.containerType != WorkspaceContainerType.HOME ||
            page.rank != 0
        ) {
            return WorkspacePrimaryHomeGridMigrationPlanningResult.InvalidPrimaryPage
        }
        if (items.isEmpty()) {
            return WorkspacePrimaryHomeGridMigrationPlanningResult.Empty
        }

        val orderedItems = items.sortedBy { it.rank }
        if (orderedItems.map { it.rank } != orderedItems.indices.toList()) {
            return WorkspacePrimaryHomeGridMigrationPlanningResult.InvalidPrimaryItems
        }

        val itemIds = mutableSetOf<String>()
        val appKeys = mutableSetOf<String>()
        for (item in orderedItems) {
            if (
                item.pageId != WorkspaceLegacyImportMapper.HOME_PAGE_ID ||
                item.itemId.isBlank() ||
                item.spanX <= 0 ||
                item.spanY <= 0 ||
                !itemIds.add(item.itemId)
            ) {
                return WorkspacePrimaryHomeGridMigrationPlanningResult.InvalidPrimaryItems
            }

            when (item.itemType) {
                WorkspaceItemType.APP -> {
                    val appKey = item.appKey
                    if (
                        appKey == null ||
                        appKey.isBlank() ||
                        item.itemId != "legacy:home:$appKey" ||
                        item.spanX != 1 ||
                        item.spanY != 1 ||
                        !appKeys.add(appKey)
                    ) {
                        return WorkspacePrimaryHomeGridMigrationPlanningResult.InvalidPrimaryItems
                    }
                }
                WorkspaceItemType.WIDGET -> {
                    if (
                        WorkspaceWidgetKeyCodec.decode(item.appKey) == null ||
                        item.cellX == null ||
                        item.cellY == null
                    ) {
                        return WorkspacePrimaryHomeGridMigrationPlanningResult.InvalidPrimaryItems
                    }
                }
                WorkspaceItemType.FOLDER -> {
                    if (
                        item.appKey.isNullOrBlank() ||
                        item.cellX == null ||
                        item.cellY == null ||
                        item.spanX != 1 ||
                        item.spanY != 1
                    ) {
                        return WorkspacePrimaryHomeGridMigrationPlanningResult.InvalidPrimaryItems
                    }
                }
                else -> return WorkspacePrimaryHomeGridMigrationPlanningResult.InvalidPrimaryItems
            }
        }

        val allCompatibilityCoordinates = orderedItems.all {
            it.itemType == WorkspaceItemType.APP && it.cellX == null && it.cellY == null
        }
        val allSpatialCoordinates = orderedItems.all {
            it.cellX != null && it.cellY != null
        }
        if (!allCompatibilityCoordinates && !allSpatialCoordinates) {
            return WorkspacePrimaryHomeGridMigrationPlanningResult.InvalidPrimaryItems
        }

        val minimumRows = if (allSpatialCoordinates) {
            orderedItems.maxOf { item -> checkNotNull(item.cellY) + item.spanY }
        } else {
            maxOf(1, (orderedItems.size + columns - 1) / columns)
        }
        if (minimumRows > MAX_PRIMARY_HOME_ROWS) {
            return WorkspacePrimaryHomeGridMigrationPlanningResult.InvalidPrimaryItems
        }
        if (rows != null && minimumRows > rows) {
            return WorkspacePrimaryHomeGridMigrationPlanningResult.InvalidPrimaryItems
        }
        val gridRows = rows ?: minimumRows
        val grid = WorkspaceGridPlacement.Grid(
            columns = columns,
            rows = gridRows,
        )

        if (allSpatialCoordinates) {
            val currentPlacements = orderedItems.map { item ->
                WorkspaceGridPlacement.Placement(
                    itemId = item.itemId,
                    cellX = checkNotNull(item.cellX),
                    cellY = checkNotNull(item.cellY),
                    spanX = item.spanX,
                    spanY = item.spanY,
                )
            }
            return if (
                WorkspaceGridPlacement.validate(grid, currentPlacements) ==
                    WorkspaceGridPlacement.Validation.Valid
            ) {
                WorkspacePrimaryHomeGridMigrationPlanningResult.AlreadySpatial
            } else {
                WorkspacePrimaryHomeGridMigrationPlanningResult.InvalidPrimaryItems
            }
        }

        val migratedItems = orderedItems.map { item ->
            item.copy(
                cellX = item.rank % columns,
                cellY = item.rank / columns,
            )
        }
        val placements = migratedItems.map { item ->
            WorkspaceGridPlacement.Placement(
                itemId = item.itemId,
                cellX = checkNotNull(item.cellX),
                cellY = checkNotNull(item.cellY),
                spanX = item.spanX,
                spanY = item.spanY,
            )
        }
        if (WorkspaceGridPlacement.validate(grid, placements) != WorkspaceGridPlacement.Validation.Valid) {
            return WorkspacePrimaryHomeGridMigrationPlanningResult.InvalidPrimaryItems
        }

        return WorkspacePrimaryHomeGridMigrationPlanningResult.Planned(
            WorkspacePrimaryHomeGridMigrationPlan(
                grid = grid,
                sourceItems = orderedItems,
                migratedItems = migratedItems,
            )
        )
    }
}
