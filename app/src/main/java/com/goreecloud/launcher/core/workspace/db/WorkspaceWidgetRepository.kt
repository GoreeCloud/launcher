package com.goreecloud.launcher.core.workspace.db

import com.goreecloud.launcher.core.workspace.WorkspaceAuthority
import com.goreecloud.launcher.core.workspace.WorkspaceGridPlacement
import com.goreecloud.launcher.core.workspace.WorkspaceWidgetCatalog
import com.goreecloud.launcher.core.workspace.WorkspaceWidgetDescriptor
import com.goreecloud.launcher.core.workspace.WorkspaceWidgetKeyCodec
import com.goreecloud.launcher.core.workspace.WorkspaceWidgetPlacementPolicy
import com.goreecloud.launcher.core.workspace.WorkspaceRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

sealed interface WorkspaceWidgetMutationResult {
    data object Reserved : WorkspaceWidgetMutationResult
    data object Unavailable : WorkspaceWidgetMutationResult
    data object InvalidWorkspace : WorkspaceWidgetMutationResult
    data object NoSpace : WorkspaceWidgetMutationResult
    data object NotFound : WorkspaceWidgetMutationResult
    data object StoredWorkspaceChanged : WorkspaceWidgetMutationResult
    data class Added(
        val itemId: String,
        val cellX: Int,
        val cellY: Int,
        val spanX: Int,
        val spanY: Int,
    ) : WorkspaceWidgetMutationResult
    data class Moved(
        val itemId: String,
        val cellX: Int,
        val cellY: Int,
    ) : WorkspaceWidgetMutationResult
    data class MovedToPage(
        val itemId: String,
        val sourcePageId: String,
        val targetPageId: String,
        val cellX: Int,
        val cellY: Int,
    ) : WorkspaceWidgetMutationResult
    data class Resized(
        val itemId: String,
        val spanX: Int,
        val spanY: Int,
    ) : WorkspaceWidgetMutationResult
    data class Removed(val itemId: String) : WorkspaceWidgetMutationResult
    data class Failed(val failureType: String) : WorkspaceWidgetMutationResult
}

class WorkspaceWidgetRepository(
    private val authorityRepository: WorkspaceRepository,
    private val workspaceDaoProvider: () -> WorkspaceDao?,
) {
    suspend fun addBuiltInWidget(
        itemId: String,
        typeId: String,
        columns: Int,
        rows: Int,
    ): WorkspaceWidgetMutationResult {
        val span = WorkspaceWidgetCatalog.defaultSpan(typeId)
            ?: return WorkspaceWidgetMutationResult.InvalidWorkspace
        return addWidget(
            itemId = itemId,
            descriptor = WorkspaceWidgetDescriptor.BuiltIn(typeId),
            columns = columns,
            rows = rows,
            spanX = span.first,
            spanY = span.second,
        )
    }

    suspend fun addAndroidWidget(
        itemId: String,
        appWidgetId: Int,
        providerComponent: String,
        columns: Int,
        rows: Int,
        spanX: Int = 2,
        spanY: Int = 2,
    ): WorkspaceWidgetMutationResult = addWidget(
        itemId = itemId,
        descriptor = WorkspaceWidgetDescriptor.Android(
            appWidgetId = appWidgetId,
            providerComponent = providerComponent,
        ),
        columns = columns,
        rows = rows,
        spanX = spanX.coerceIn(1, columns),
        spanY = spanY.coerceIn(1, rows),
    )

    suspend fun removeWidget(itemId: String): WorkspaceWidgetMutationResult {
        if (!isRoomAuthoritative()) return WorkspaceWidgetMutationResult.Reserved
        if (itemId.isBlank()) return WorkspaceWidgetMutationResult.InvalidWorkspace
        val dao = workspaceDaoOrNull() ?: return WorkspaceWidgetMutationResult.Unavailable
        return try {
            val snapshot = readHomeSnapshot(dao)
                ?: return WorkspaceWidgetMutationResult.InvalidWorkspace
            val candidates = snapshot.items.filter {
                it.itemId == itemId && it.itemType == WorkspaceItemType.WIDGET
            }
            if (candidates.isEmpty()) return WorkspaceWidgetMutationResult.NotFound
            if (candidates.size != 1) return WorkspaceWidgetMutationResult.InvalidWorkspace
            val widget = candidates.single()
            if (WorkspaceWidgetKeyCodec.decode(widget.appKey) == null) {
                return WorkspaceWidgetMutationResult.InvalidWorkspace
            }

            val sourceItems = snapshot.items
                .filter { it.pageId == widget.pageId }
                .sortedBy { it.rank }
            val updatedSource = sourceItems
                .filterNot { it.itemId == itemId }
                .mapIndexed { rank, item -> item.copy(rank = rank) }
                .associateBy { it.itemId }
            val updatedItems = snapshot.items.mapNotNull { item ->
                when {
                    item.itemId == itemId -> null
                    item.itemId in updatedSource -> checkNotNull(updatedSource[item.itemId])
                    else -> item
                }
            }
            if (!dao.replaceHomeItemsIncludingWidgetIdentityChangesIfSnapshotMatches(
                    expectedPages = snapshot.pages,
                    expectedItems = snapshot.items,
                    updatedItems = updatedItems,
                )
            ) {
                return WorkspaceWidgetMutationResult.StoredWorkspaceChanged
            }
            WorkspaceWidgetMutationResult.Removed(itemId)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            WorkspaceWidgetMutationResult.Failed(exception::class.java.simpleName)
        }
    }

    suspend fun moveWidget(
        itemId: String,
        columns: Int,
        rows: Int,
        cellX: Int,
        cellY: Int,
    ): WorkspaceWidgetMutationResult {
        if (!isRoomAuthoritative()) return WorkspaceWidgetMutationResult.Reserved
        if (
            itemId.isBlank() ||
            columns <= 0 ||
            rows <= 0 ||
            cellX !in 0 until columns ||
            cellY !in 0 until rows
        ) return WorkspaceWidgetMutationResult.InvalidWorkspace
        val dao = workspaceDaoOrNull() ?: return WorkspaceWidgetMutationResult.Unavailable
        return try {
            val snapshot = readHomeSnapshot(dao)
                ?: return WorkspaceWidgetMutationResult.InvalidWorkspace
            val candidates = snapshot.items.filter {
                it.itemId == itemId && it.itemType == WorkspaceItemType.WIDGET
            }
            if (candidates.isEmpty()) return WorkspaceWidgetMutationResult.NotFound
            if (candidates.size != 1) return WorkspaceWidgetMutationResult.InvalidWorkspace
            val widget = candidates.single()
            if (WorkspaceWidgetKeyCodec.decode(widget.appKey) == null) {
                return WorkspaceWidgetMutationResult.InvalidWorkspace
            }

            val pageItems = snapshot.items
                .filter { it.pageId == widget.pageId }
                .sortedBy { it.rank }
            val grid = WorkspaceGridPlacement.Grid(columns, rows)
            val placements = pageItems.mapNotNull(WorkspaceItemEntity::toSpatialPlacement)
            if (
                placements.size != pageItems.size ||
                WorkspaceGridPlacement.validate(grid, placements) !=
                    WorkspaceGridPlacement.Validation.Valid
            ) return WorkspaceWidgetMutationResult.InvalidWorkspace

            val moved = WorkspaceWidgetPlacementPolicy.move(
                grid = grid,
                existing = placements,
                itemId = itemId,
                cellX = cellX,
                cellY = cellY,
            ) ?: return WorkspaceWidgetMutationResult.NoSpace
            val updatedItems = snapshot.items.map { item ->
                if (item.itemId == itemId) {
                    item.copy(cellX = moved.cellX, cellY = moved.cellY)
                } else {
                    item
                }
            }
            if (!dao.replaceHomeItemsIfSnapshotMatches(
                    expectedPages = snapshot.pages,
                    expectedItems = snapshot.items,
                    updatedItems = updatedItems,
                )
            ) return WorkspaceWidgetMutationResult.StoredWorkspaceChanged
            WorkspaceWidgetMutationResult.Moved(itemId, moved.cellX, moved.cellY)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            WorkspaceWidgetMutationResult.Failed(exception::class.java.simpleName)
        }
    }

    suspend fun resizeWidget(
        itemId: String,
        columns: Int,
        rows: Int,
        spanX: Int,
        spanY: Int,
    ): WorkspaceWidgetMutationResult {
        if (!isRoomAuthoritative()) return WorkspaceWidgetMutationResult.Reserved
        if (
            itemId.isBlank() ||
            columns <= 0 ||
            rows <= 0 ||
            spanX <= 0 ||
            spanY <= 0
        ) return WorkspaceWidgetMutationResult.InvalidWorkspace
        val dao = workspaceDaoOrNull() ?: return WorkspaceWidgetMutationResult.Unavailable
        return try {
            val snapshot = readHomeSnapshot(dao)
                ?: return WorkspaceWidgetMutationResult.InvalidWorkspace
            val candidates = snapshot.items.filter {
                it.itemId == itemId && it.itemType == WorkspaceItemType.WIDGET
            }
            if (candidates.isEmpty()) return WorkspaceWidgetMutationResult.NotFound
            if (candidates.size != 1) return WorkspaceWidgetMutationResult.InvalidWorkspace
            val widget = candidates.single()
            if (WorkspaceWidgetKeyCodec.decode(widget.appKey) == null) {
                return WorkspaceWidgetMutationResult.InvalidWorkspace
            }

            val pageItems = snapshot.items
                .filter { it.pageId == widget.pageId }
                .sortedBy { it.rank }
            val grid = WorkspaceGridPlacement.Grid(columns, rows)
            val placements = pageItems.mapNotNull(WorkspaceItemEntity::toSpatialPlacement)
            if (
                placements.size != pageItems.size ||
                WorkspaceGridPlacement.validate(grid, placements) !=
                    WorkspaceGridPlacement.Validation.Valid
            ) return WorkspaceWidgetMutationResult.InvalidWorkspace

            val resized = WorkspaceWidgetPlacementPolicy.resize(
                grid = grid,
                existing = placements,
                itemId = itemId,
                spanX = spanX,
                spanY = spanY,
            ) ?: return WorkspaceWidgetMutationResult.NoSpace
            val updatedItems = snapshot.items.map { item ->
                if (item.itemId == itemId) {
                    item.copy(spanX = resized.spanX, spanY = resized.spanY)
                } else {
                    item
                }
            }
            if (!dao.replaceHomeItemsIfSnapshotMatches(
                    expectedPages = snapshot.pages,
                    expectedItems = snapshot.items,
                    updatedItems = updatedItems,
                )
            ) return WorkspaceWidgetMutationResult.StoredWorkspaceChanged
            WorkspaceWidgetMutationResult.Resized(itemId, resized.spanX, resized.spanY)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            WorkspaceWidgetMutationResult.Failed(exception::class.java.simpleName)
        }
    }

    suspend fun moveWidgetToPage(
        itemId: String,
        targetPageId: String,
        columns: Int,
        rows: Int,
        targetCellX: Int? = null,
        targetCellY: Int? = null,
    ): WorkspaceWidgetMutationResult {
        if (!isRoomAuthoritative()) return WorkspaceWidgetMutationResult.Reserved
        val hasExactTarget = targetCellX != null || targetCellY != null
        if (
            itemId.isBlank() ||
            targetPageId.isBlank() ||
            columns <= 0 ||
            rows <= 0 ||
            (hasExactTarget && (targetCellX == null || targetCellY == null))
        ) return WorkspaceWidgetMutationResult.InvalidWorkspace
        val dao = workspaceDaoOrNull() ?: return WorkspaceWidgetMutationResult.Unavailable
        return try {
            val snapshot = readHomeSnapshot(dao)
                ?: return WorkspaceWidgetMutationResult.InvalidWorkspace
            if (snapshot.pages.none { it.pageId == targetPageId }) {
                return WorkspaceWidgetMutationResult.InvalidWorkspace
            }
            val candidates = snapshot.items.filter {
                it.itemId == itemId && it.itemType == WorkspaceItemType.WIDGET
            }
            if (candidates.isEmpty()) return WorkspaceWidgetMutationResult.NotFound
            if (candidates.size != 1) return WorkspaceWidgetMutationResult.InvalidWorkspace
            val widget = candidates.single()
            if (
                WorkspaceWidgetKeyCodec.decode(widget.appKey) == null ||
                widget.pageId == targetPageId
            ) return WorkspaceWidgetMutationResult.InvalidWorkspace

            val grid = WorkspaceGridPlacement.Grid(columns, rows)
            val sourceItems = snapshot.items
                .filter { it.pageId == widget.pageId }
                .sortedBy { it.rank }
            val targetItems = snapshot.items
                .filter { it.pageId == targetPageId }
                .sortedBy { it.rank }
            val sourcePlacements = sourceItems.mapNotNull(WorkspaceItemEntity::toSpatialPlacement)
            val targetPlacements = targetItems.mapNotNull(WorkspaceItemEntity::toSpatialPlacement)
            if (
                sourcePlacements.size != sourceItems.size ||
                targetPlacements.size != targetItems.size ||
                WorkspaceGridPlacement.validate(grid, sourcePlacements) !=
                    WorkspaceGridPlacement.Validation.Valid ||
                WorkspaceGridPlacement.validate(grid, targetPlacements) !=
                    WorkspaceGridPlacement.Validation.Valid
            ) return WorkspaceWidgetMutationResult.InvalidWorkspace

            val requestedDestination = if (targetCellX != null && targetCellY != null) {
                WorkspaceGridPlacement.Placement(
                    itemId = widget.itemId,
                    cellX = targetCellX,
                    cellY = targetCellY,
                    spanX = widget.spanX,
                    spanY = widget.spanY,
                )
            } else {
                null
            }
            val destination = if (requestedDestination != null) {
                requestedDestination.takeIf { candidate ->
                    WorkspaceGridPlacement.validate(grid, targetPlacements + candidate) ==
                        WorkspaceGridPlacement.Validation.Valid
                }
            } else {
                WorkspaceWidgetPlacementPolicy.firstAvailable(
                    grid = grid,
                    existing = targetPlacements,
                    itemId = widget.itemId,
                    spanX = widget.spanX,
                    spanY = widget.spanY,
                )
            } ?: return WorkspaceWidgetMutationResult.NoSpace

            val updatedSource = sourceItems
                .filterNot { it.itemId == itemId }
                .mapIndexed { rank, item -> item.copy(rank = rank) }
                .associateBy { it.itemId }
            val movedWidget = widget.copy(
                pageId = targetPageId,
                rank = targetItems.size,
                cellX = destination.cellX,
                cellY = destination.cellY,
            )
            val updatedItems = snapshot.items.map { item ->
                when {
                    item.itemId == itemId -> movedWidget
                    item.itemId in updatedSource -> checkNotNull(updatedSource[item.itemId])
                    else -> item
                }
            }
            if (!dao.replaceHomeItemsIfSnapshotMatches(
                    expectedPages = snapshot.pages,
                    expectedItems = snapshot.items,
                    updatedItems = updatedItems,
                )
            ) return WorkspaceWidgetMutationResult.StoredWorkspaceChanged

            WorkspaceWidgetMutationResult.MovedToPage(
                itemId = itemId,
                sourcePageId = widget.pageId,
                targetPageId = targetPageId,
                cellX = destination.cellX,
                cellY = destination.cellY,
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            WorkspaceWidgetMutationResult.Failed(exception::class.java.simpleName)
        }
    }

    private suspend fun addWidget(
        itemId: String,
        descriptor: WorkspaceWidgetDescriptor,
        columns: Int,
        rows: Int,
        spanX: Int,
        spanY: Int,
    ): WorkspaceWidgetMutationResult {
        if (!isRoomAuthoritative()) return WorkspaceWidgetMutationResult.Reserved
        if (
            itemId.isBlank() ||
            columns <= 0 ||
            rows <= 0 ||
            spanX <= 0 ||
            spanY <= 0
        ) return WorkspaceWidgetMutationResult.InvalidWorkspace


        val dao = workspaceDaoOrNull() ?: return WorkspaceWidgetMutationResult.Unavailable
        return try {
            val page = primaryPage(dao) ?: return WorkspaceWidgetMutationResult.InvalidWorkspace
            val items = primaryItems(dao)
            if (items.any { it.itemId == itemId }) {
                return WorkspaceWidgetMutationResult.InvalidWorkspace
            }
            val placements = items.mapNotNull(WorkspaceItemEntity::toSpatialPlacement)
            if (placements.size != items.size) {
                return WorkspaceWidgetMutationResult.InvalidWorkspace
            }
            val placement = WorkspaceWidgetPlacementPolicy.firstAvailable(
                grid = WorkspaceGridPlacement.Grid(columns, rows),
                existing = placements,
                itemId = itemId,
                spanX = spanX,
                spanY = spanY,
            ) ?: return WorkspaceWidgetMutationResult.NoSpace
            val widget = WorkspaceItemEntity(
                itemId = itemId,
                pageId = WorkspaceLegacyImportMapper.HOME_PAGE_ID,
                itemType = WorkspaceItemType.WIDGET,
                appKey = WorkspaceWidgetKeyCodec.encode(descriptor),
                rank = items.size,
                cellX = placement.cellX,
                cellY = placement.cellY,
                spanX = placement.spanX,
                spanY = placement.spanY,
            )
            val updated = items + widget
            if (!dao.replacePrimaryHomeItemsIncludingIdentityChangesIfSnapshotMatches(
                    expectedPage = page,
                    expectedItems = items,
                    updatedItems = updated,
                )
            ) {
                return WorkspaceWidgetMutationResult.StoredWorkspaceChanged
            }
            WorkspaceWidgetMutationResult.Added(
                itemId = itemId,
                cellX = placement.cellX,
                cellY = placement.cellY,
                spanX = placement.spanX,
                spanY = placement.spanY,
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            WorkspaceWidgetMutationResult.Failed(exception::class.java.simpleName)
        }
    }

    private suspend fun readHomeSnapshot(dao: WorkspaceDao): HomeSnapshot? {
        val pages = dao.readPagesByContainer(WorkspaceContainerType.HOME)
        if (
            pages.isEmpty() ||
            pages.first().pageId != WorkspaceLegacyImportMapper.HOME_PAGE_ID ||
            pages.map { it.rank } != pages.indices.toList()
        ) return null
        val items = dao.readItems(pages.map { it.pageId })
        if (items.map { it.itemId }.distinct().size != items.size) return null
        if (
            items.groupBy { it.pageId }.values.any { pageItems ->
                pageItems.sortedBy { it.rank }.map { it.rank } != pageItems.indices.toList()
            }
        ) return null
        return HomeSnapshot(pages = pages, items = items)
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

    private data class HomeSnapshot(
        val pages: List<WorkspacePageEntity>,
        val items: List<WorkspaceItemEntity>,
    )

    private fun workspaceDaoOrNull(): WorkspaceDao? = try {
        workspaceDaoProvider()
    } catch (exception: CancellationException) {
        throw exception
    } catch (_: Exception) {
        null
    }
}

private fun WorkspaceItemEntity.toSpatialPlacement(): WorkspaceGridPlacement.Placement? {
    val x = cellX ?: return null
    val y = cellY ?: return null
    return WorkspaceGridPlacement.Placement(
        itemId = itemId,
        cellX = x,
        cellY = y,
        spanX = spanX,
        spanY = spanY,
    )
}

