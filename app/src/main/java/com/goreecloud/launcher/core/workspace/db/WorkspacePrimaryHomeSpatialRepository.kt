package com.goreecloud.launcher.core.workspace.db

import com.goreecloud.launcher.core.workspace.WorkspaceAuthority
import com.goreecloud.launcher.core.workspace.WorkspaceGridPlacement
import com.goreecloud.launcher.core.workspace.WorkspaceRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

sealed interface WorkspacePrimaryHomeSpatialResult {
    data object Reserved : WorkspacePrimaryHomeSpatialResult
    data object Unavailable : WorkspacePrimaryHomeSpatialResult
    data object InvalidWorkspace : WorkspacePrimaryHomeSpatialResult
    data object StoredWorkspaceChanged : WorkspacePrimaryHomeSpatialResult
    data class Ready(val changed: Boolean, val columns: Int, val rows: Int) : WorkspacePrimaryHomeSpatialResult
    data class Moved(
        val appKey: String,
        val cellX: Int,
        val cellY: Int,
        val swappedAppKey: String?,
    ) : WorkspacePrimaryHomeSpatialResult
    data class Failed(val failureType: String) : WorkspacePrimaryHomeSpatialResult
}

class WorkspacePrimaryHomeSpatialRepository(
    private val authorityRepository: WorkspaceRepository,
    private val workspaceDaoProvider: () -> WorkspaceDao?,
) {
    suspend fun ensureGrid(columns: Int, rows: Int): WorkspacePrimaryHomeSpatialResult {
        if (!isRoomAuthoritative()) return WorkspacePrimaryHomeSpatialResult.Reserved
        if (
            columns !in WorkspacePrimaryHomeGridMigrationPlanner.MIN_PRIMARY_HOME_COLUMNS..
                WorkspacePrimaryHomeGridMigrationPlanner.MAX_PRIMARY_HOME_COLUMNS ||
            rows !in WorkspacePrimaryHomeGridMigrationPlanner.MIN_PRIMARY_HOME_ROWS..
                WorkspacePrimaryHomeGridMigrationPlanner.MAX_PRIMARY_HOME_ROWS
        ) {
            return WorkspacePrimaryHomeSpatialResult.InvalidWorkspace
        }
        val dao = workspaceDaoOrNull() ?: return WorkspacePrimaryHomeSpatialResult.Unavailable
        return try {
            val page = primaryPage(dao) ?: return WorkspacePrimaryHomeSpatialResult.InvalidWorkspace
            val items = primaryItems(dao)
            when (val planned = WorkspacePrimaryHomeGridMigrationPlanner.plan(page, items, columns, rows)) {
                WorkspacePrimaryHomeGridMigrationPlanningResult.Empty,
                WorkspacePrimaryHomeGridMigrationPlanningResult.AlreadySpatial ->
                    WorkspacePrimaryHomeSpatialResult.Ready(false, columns, rows)
                is WorkspacePrimaryHomeGridMigrationPlanningResult.Planned -> {
                    if (!dao.replacePrimaryHomeItemsIfSnapshotMatches(page, items, planned.plan.migratedItems)) {
                        WorkspacePrimaryHomeSpatialResult.StoredWorkspaceChanged
                    } else {
                        WorkspacePrimaryHomeSpatialResult.Ready(true, columns, rows)
                    }
                }
                WorkspacePrimaryHomeGridMigrationPlanningResult.InvalidPrimaryPage ->
                    WorkspacePrimaryHomeSpatialResult.InvalidWorkspace
                WorkspacePrimaryHomeGridMigrationPlanningResult.InvalidPrimaryItems -> {
                    val supported = WorkspacePrimaryHomeGridMigrationPlanner.plan(
                        page = page,
                        items = items,
                        columns = WorkspacePrimaryHomeGridMigrationPlanner.MAX_PRIMARY_HOME_COLUMNS,
                        rows = WorkspacePrimaryHomeGridMigrationPlanner.MAX_PRIMARY_HOME_ROWS,
                    )
                    if (supported != WorkspacePrimaryHomeGridMigrationPlanningResult.AlreadySpatial) {
                        return WorkspacePrimaryHomeSpatialResult.InvalidWorkspace
                    }
                    val reflowed = reflowIntoGrid(items, columns, rows)
                        ?: return WorkspacePrimaryHomeSpatialResult.InvalidWorkspace
                    if (!dao.replacePrimaryHomeItemsIfSnapshotMatches(page, items, reflowed)) {
                        WorkspacePrimaryHomeSpatialResult.StoredWorkspaceChanged
                    } else {
                        WorkspacePrimaryHomeSpatialResult.Ready(true, columns, rows)
                    }
                }
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            WorkspacePrimaryHomeSpatialResult.Failed(exception::class.java.simpleName)
        }
    }

    suspend fun moveAppToCell(
        appKey: String,
        columns: Int,
        rows: Int,
        cellX: Int,
        cellY: Int,
    ): WorkspacePrimaryHomeSpatialResult {
        if (!isRoomAuthoritative()) return WorkspacePrimaryHomeSpatialResult.Reserved
        if (
            appKey.isBlank() || columns <= 0 || rows <= 0 ||
            cellX !in 0 until columns || cellY !in 0 until rows
        ) return WorkspacePrimaryHomeSpatialResult.InvalidWorkspace

        when (val ready = ensureGrid(columns, rows)) {
            is WorkspacePrimaryHomeSpatialResult.Ready -> Unit
            else -> return ready
        }

        val dao = workspaceDaoOrNull() ?: return WorkspacePrimaryHomeSpatialResult.Unavailable
        return try {
            val page = primaryPage(dao) ?: return WorkspacePrimaryHomeSpatialResult.InvalidWorkspace
            val items = primaryItems(dao)
            if (
                WorkspacePrimaryHomeGridMigrationPlanner.plan(page, items, columns, rows) !=
                    WorkspacePrimaryHomeGridMigrationPlanningResult.AlreadySpatial
            ) return WorkspacePrimaryHomeSpatialResult.InvalidWorkspace

            val source = items.singleOrNull { it.itemType == WorkspaceItemType.APP && it.appKey == appKey }
                ?: return WorkspacePrimaryHomeSpatialResult.InvalidWorkspace
            val sourceX = checkNotNull(source.cellX)
            val sourceY = checkNotNull(source.cellY)
            if (sourceX == cellX && sourceY == cellY) {
                return WorkspacePrimaryHomeSpatialResult.Moved(appKey, cellX, cellY, null)
            }

            val target = items.singleOrNull {
                it.itemId != source.itemId && it.cellX == cellX && it.cellY == cellY
            }
            val updated = items.map { item ->
                when (item.itemId) {
                    source.itemId -> item.copy(cellX = cellX, cellY = cellY)
                    target?.itemId -> item.copy(cellX = sourceX, cellY = sourceY)
                    else -> item
                }
            }
            val placements = updated.map { item ->
                WorkspaceGridPlacement.Placement(
                    itemId = item.itemId,
                    cellX = checkNotNull(item.cellX),
                    cellY = checkNotNull(item.cellY),
                    spanX = item.spanX,
                    spanY = item.spanY,
                )
            }
            if (
                WorkspaceGridPlacement.validate(
                    WorkspaceGridPlacement.Grid(columns, rows),
                    placements,
                ) != WorkspaceGridPlacement.Validation.Valid
            ) return WorkspacePrimaryHomeSpatialResult.InvalidWorkspace

            if (!dao.replacePrimaryHomeItemsIfSnapshotMatches(page, items, updated)) {
                return WorkspacePrimaryHomeSpatialResult.StoredWorkspaceChanged
            }
            WorkspacePrimaryHomeSpatialResult.Moved(
                appKey = appKey,
                cellX = cellX,
                cellY = cellY,
                swappedAppKey = target?.appKey,
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            WorkspacePrimaryHomeSpatialResult.Failed(exception::class.java.simpleName)
        }
    }

    private fun reflowIntoGrid(
        items: List<WorkspaceItemEntity>,
        columns: Int,
        rows: Int,
    ): List<WorkspaceItemEntity>? {
        if (items.size > columns * rows) return null
        val occupied = mutableSetOf<Pair<Int, Int>>()
        val freeCells = buildList {
            for (cellY in 0 until rows) {
                for (cellX in 0 until columns) add(cellX to cellY)
            }
        }.toMutableList()

        return items.sortedBy { it.rank }.map { item ->
            val currentX = item.cellX ?: return null
            val currentY = item.cellY ?: return null
            val current = currentX to currentY
            val coordinate = if (
                currentX in 0 until columns &&
                currentY in 0 until rows &&
                current !in occupied
            ) {
                freeCells.remove(current)
                current
            } else {
                freeCells.firstOrNull { it !in occupied } ?: return null
            }
            occupied.add(coordinate)
            item.copy(cellX = coordinate.first, cellY = coordinate.second)
        }
    }

    private suspend fun primaryPage(dao: WorkspaceDao): WorkspacePageEntity? =
        dao.readPages(listOf(WorkspaceLegacyImportMapper.HOME_PAGE_ID)).singleOrNull()?.takeIf {
            it.containerType == WorkspaceContainerType.HOME && it.rank == 0
        }

    private suspend fun primaryItems(dao: WorkspaceDao): List<WorkspaceItemEntity> =
        dao.readItems(listOf(WorkspaceLegacyImportMapper.HOME_PAGE_ID)).sortedBy { it.rank }

    private suspend fun isRoomAuthoritative(): Boolean {
        val state = authorityRepository.state.first()
        return state.initialized && state.authority == WorkspaceAuthority.ROOM
    }

    private fun workspaceDaoOrNull(): WorkspaceDao? = try {
        workspaceDaoProvider()
    } catch (exception: CancellationException) {
        throw exception
    } catch (_: Exception) {
        null
    }
}
