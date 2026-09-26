package com.goreecloud.launcher.core.workspace.db

import com.goreecloud.launcher.core.workspace.MAX_DOCK_ITEMS
import com.goreecloud.launcher.core.workspace.WorkspaceAuthority
import com.goreecloud.launcher.core.workspace.WorkspaceGridPlacement
import com.goreecloud.launcher.core.workspace.WorkspaceRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

sealed interface WorkspaceRoomReadResult {
    data object Reserved : WorkspaceRoomReadResult
    data object Unavailable : WorkspaceRoomReadResult
    data class Loaded(val snapshot: WorkspaceRelationalSnapshot) : WorkspaceRoomReadResult
    data object Mismatch : WorkspaceRoomReadResult
    data class Failed(val failureType: String) : WorkspaceRoomReadResult
}

sealed interface WorkspaceRoomWriteResult {
    data object Reserved : WorkspaceRoomWriteResult
    data object Unavailable : WorkspaceRoomWriteResult
    data class Written(val snapshot: WorkspaceRelationalSnapshot) : WorkspaceRoomWriteResult
    data object Mismatch : WorkspaceRoomWriteResult
    data class Failed(val failureType: String) : WorkspaceRoomWriteResult
}

internal object WorkspaceRoomPlacementModel {
    fun normalize(
        favoriteKeys: List<String>,
        dockKeys: List<String>,
    ): WorkspaceRelationalSnapshot = WorkspaceRelationalSnapshot(
        favoriteKeys = favoriteKeys.distinct(),
        dockKeys = dockKeys.distinct().take(MAX_DOCK_ITEMS),
    )
}

/**
 * Reserved Room-backed placement I/O for the current Home/Dock compatibility containers.
 *
 * Production Home does not call this repository yet. Reads and writes are accepted only after the
 * durable workspace authority has already reached the terminal ROOM phase. Pre-cutover DATASTORE
 * and ROOM_VERIFIED states fail closed as [WorkspaceRoomReadResult.Reserved] or
 * [WorkspaceRoomWriteResult.Reserved].
 */
class WorkspaceRoomPlacementRepository(
    private val authorityRepository: WorkspaceRepository,
    private val workspaceDaoProvider: () -> WorkspaceDao?,
) {
    suspend fun read(): WorkspaceRoomReadResult {
        if (!isRoomAuthoritative()) return WorkspaceRoomReadResult.Reserved
        val workspaceDao = workspaceDaoOrNull() ?: return WorkspaceRoomReadResult.Unavailable

        return try {
            val snapshot = WorkspaceCanonicalRoomPlacementReader.read(workspaceDao)
                ?: return WorkspaceRoomReadResult.Mismatch
            WorkspaceRoomReadResult.Loaded(snapshot)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            WorkspaceRoomReadResult.Failed(exception::class.java.simpleName)
        }
    }

    suspend fun replace(
        favoriteKeys: List<String>,
        dockKeys: List<String>,
        homeGrid: WorkspaceGridPlacement.Grid? = null,
    ): WorkspaceRoomWriteResult {
        if (!isRoomAuthoritative()) return WorkspaceRoomWriteResult.Reserved
        val workspaceDao = workspaceDaoOrNull() ?: return WorkspaceRoomWriteResult.Unavailable
        val normalized = WorkspaceRoomPlacementModel.normalize(favoriteKeys, dockKeys)

        return try {
            val currentPrimary = workspaceDao
                .readItems(listOf(WorkspaceLegacyImportMapper.HOME_PAGE_ID))
                .sortedBy { it.rank }
            val currentApps = currentPrimary.filter { it.itemType == WorkspaceItemType.APP }
            val currentExtras = currentPrimary.filter {
                it.itemType == WorkspaceItemType.WIDGET ||
                    it.itemType == WorkspaceItemType.FOLDER
            }
            if (currentApps.size + currentExtras.size != currentPrimary.size) {
                return WorkspaceRoomWriteResult.Mismatch
            }
            val currentCompatibility =
                currentExtras.isEmpty() && currentApps.all { it.cellX == null && it.cellY == null }
            val currentSpatial = currentPrimary.all { it.cellX != null && it.cellY != null }
            if (!currentCompatibility && !currentSpatial) {
                return WorkspaceRoomWriteResult.Mismatch
            }

            val expected = WorkspaceLegacyImportMapper.map(
                favoriteKeys = normalized.favoriteKeys,
                dockKeys = normalized.dockKeys,
            )
            val expectedDock = expected.items.filter {
                it.pageId == WorkspaceLegacyImportMapper.DOCK_PAGE_ID
            }
            val currentByKey = currentApps
                .mapNotNull { item -> item.appKey?.let { it to item } }
                .toMap()
            if (currentByKey.size != currentApps.size) {
                return WorkspaceRoomWriteResult.Mismatch
            }

            val primaryItems = if (currentPrimary.isEmpty() || currentCompatibility) {
                expected.items.filter { it.pageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID }
            } else {
                val requestedKeys = normalized.favoriteKeys
                val additions = requestedKeys.filterNot(currentByKey::containsKey)
                val grid = if (additions.isNotEmpty() || currentExtras.isNotEmpty()) {
                    homeGrid ?: WorkspaceGridPlacement.Grid(
                        WorkspacePrimaryHomeGridMigrationPlanner.MAX_PRIMARY_HOME_COLUMNS,
                        WorkspacePrimaryHomeGridMigrationPlanner.MAX_PRIMARY_HOME_ROWS,
                    )
                } else {
                    homeGrid
                }

                val occupiedPlacements = currentExtras.map { item ->
                    WorkspaceGridPlacement.Placement(
                        itemId = item.itemId,
                        cellX = item.cellX ?: return WorkspaceRoomWriteResult.Mismatch,
                        cellY = item.cellY ?: return WorkspaceRoomWriteResult.Mismatch,
                        spanX = item.spanX,
                        spanY = item.spanY,
                    )
                }.toMutableList()

                val retainedApps = requestedKeys.mapNotNull(currentByKey::get)
                retainedApps.forEach { item ->
                    occupiedPlacements += WorkspaceGridPlacement.Placement(
                        itemId = item.itemId,
                        cellX = item.cellX ?: return WorkspaceRoomWriteResult.Mismatch,
                        cellY = item.cellY ?: return WorkspaceRoomWriteResult.Mismatch,
                        spanX = item.spanX,
                        spanY = item.spanY,
                    )
                }
                if (
                    grid != null &&
                    WorkspaceGridPlacement.validate(grid, occupiedPlacements) !=
                        WorkspaceGridPlacement.Validation.Valid
                ) {
                    return WorkspaceRoomWriteResult.Mismatch
                }

                val nextApps = requestedKeys.mapIndexed { rank, appKey ->
                    currentByKey[appKey]?.copy(rank = rank)
                        ?: run {
                            val activeGrid = grid ?: return WorkspaceRoomWriteResult.Mismatch
                            val coordinate = firstFreeCell(
                                grid = activeGrid,
                                occupied = occupiedPlacements,
                            ) ?: return WorkspaceRoomWriteResult.Mismatch
                            val item = WorkspaceItemEntity(
                                itemId = "legacy:home:$appKey",
                                pageId = WorkspaceLegacyImportMapper.HOME_PAGE_ID,
                                itemType = WorkspaceItemType.APP,
                                appKey = appKey,
                                rank = rank,
                                cellX = coordinate.first,
                                cellY = coordinate.second,
                            )
                            occupiedPlacements += WorkspaceGridPlacement.Placement(
                                itemId = item.itemId,
                                cellX = coordinate.first,
                                cellY = coordinate.second,
                            )
                            item
                        }
                }
                val rerankedExtras = currentExtras.mapIndexed { index, item ->
                    item.copy(rank = nextApps.size + index)
                }
                (nextApps + rerankedExtras).also { spatialItems ->
                    if (grid != null) {
                        val placements = spatialItems.map { item ->
                            WorkspaceGridPlacement.Placement(
                                itemId = item.itemId,
                                cellX = item.cellX ?: return WorkspaceRoomWriteResult.Mismatch,
                                cellY = item.cellY ?: return WorkspaceRoomWriteResult.Mismatch,
                                spanX = item.spanX,
                                spanY = item.spanY,
                            )
                        }
                        if (
                            WorkspaceGridPlacement.validate(grid, placements) !=
                                WorkspaceGridPlacement.Validation.Valid
                        ) {
                            return WorkspaceRoomWriteResult.Mismatch
                        }
                    }
                }
            }

            workspaceDao.replaceLegacySnapshot(
                pages = expected.pages,
                items = primaryItems + expectedDock,
            )
            val actual = WorkspaceCanonicalRoomPlacementReader.read(workspaceDao)
                ?: return WorkspaceRoomWriteResult.Mismatch

            if (actual == normalized) {
                WorkspaceRoomWriteResult.Written(actual)
            } else {
                WorkspaceRoomWriteResult.Mismatch
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            WorkspaceRoomWriteResult.Failed(exception::class.java.simpleName)
        }
    }

    private fun firstFreeCell(
        grid: WorkspaceGridPlacement.Grid,
        occupied: List<WorkspaceGridPlacement.Placement>,
    ): Pair<Int, Int>? {
        for (cellY in 0 until grid.rows) {
            for (cellX in 0 until grid.columns) {
                val candidate = WorkspaceGridPlacement.Placement(
                    itemId = "candidate",
                    cellX = cellX,
                    cellY = cellY,
                )
                if (
                    WorkspaceGridPlacement.validate(grid, occupied + candidate) ==
                    WorkspaceGridPlacement.Validation.Valid
                ) return cellX to cellY
            }
        }
        return null
    }

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
