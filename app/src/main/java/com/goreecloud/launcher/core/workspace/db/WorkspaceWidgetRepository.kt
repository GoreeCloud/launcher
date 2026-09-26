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
        val dao = workspaceDaoOrNull() ?: return WorkspaceWidgetMutationResult.Unavailable
        return try {
            val page = primaryPage(dao) ?: return WorkspaceWidgetMutationResult.InvalidWorkspace
            val items = primaryItems(dao)
            val widget = items.singleOrNull {
                it.itemId == itemId && it.itemType == WorkspaceItemType.WIDGET
            } ?: return WorkspaceWidgetMutationResult.NotFound
            if (WorkspaceWidgetKeyCodec.decode(widget.appKey) == null) {
                return WorkspaceWidgetMutationResult.InvalidWorkspace
            }
            val updated = items
                .filterNot { it.itemId == itemId }
                .mapIndexed { rank, item -> item.copy(rank = rank) }
            if (!dao.replacePrimaryHomeItemsIncludingIdentityChangesIfSnapshotMatches(
                    expectedPage = page,
                    expectedItems = items,
                    updatedItems = updated,
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
        if (columns <= 0 || rows <= 0) return WorkspaceWidgetMutationResult.InvalidWorkspace
        val dao = workspaceDaoOrNull() ?: return WorkspaceWidgetMutationResult.Unavailable
        return try {
            val page = primaryPage(dao) ?: return WorkspaceWidgetMutationResult.InvalidWorkspace
            val items = primaryItems(dao)
            val widget = items.singleOrNull {
                it.itemId == itemId && it.itemType == WorkspaceItemType.WIDGET
            } ?: return WorkspaceWidgetMutationResult.NotFound
            if (WorkspaceWidgetKeyCodec.decode(widget.appKey) == null) {
                return WorkspaceWidgetMutationResult.InvalidWorkspace
            }
            val placements = items.mapNotNull(WorkspaceItemEntity::toSpatialPlacement)
            if (placements.size != items.size) {
                return WorkspaceWidgetMutationResult.InvalidWorkspace
            }
            val moved = WorkspaceWidgetPlacementPolicy.move(
                grid = WorkspaceGridPlacement.Grid(columns, rows),
                existing = placements,
                itemId = itemId,
                cellX = cellX,
                cellY = cellY,
            ) ?: return WorkspaceWidgetMutationResult.NoSpace
            val updated = items.map { item ->
                if (item.itemId == itemId) {
                    item.copy(cellX = moved.cellX, cellY = moved.cellY)
                } else item
            }
            if (!dao.replacePrimaryHomeItemsIfSnapshotMatches(page, items, updated)) {
                return WorkspaceWidgetMutationResult.StoredWorkspaceChanged
            }
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
        if (columns <= 0 || rows <= 0) return WorkspaceWidgetMutationResult.InvalidWorkspace
        val dao = workspaceDaoOrNull() ?: return WorkspaceWidgetMutationResult.Unavailable
        return try {
            val page = primaryPage(dao) ?: return WorkspaceWidgetMutationResult.InvalidWorkspace
            val items = primaryItems(dao)
            val widget = items.singleOrNull {
                it.itemId == itemId && it.itemType == WorkspaceItemType.WIDGET
            } ?: return WorkspaceWidgetMutationResult.NotFound
            if (WorkspaceWidgetKeyCodec.decode(widget.appKey) == null) {
                return WorkspaceWidgetMutationResult.InvalidWorkspace
            }
            val placements = items.mapNotNull(WorkspaceItemEntity::toSpatialPlacement)
            if (placements.size != items.size) {
                return WorkspaceWidgetMutationResult.InvalidWorkspace
            }
            val resized = WorkspaceWidgetPlacementPolicy.resize(
                grid = WorkspaceGridPlacement.Grid(columns, rows),
                existing = placements,
                itemId = itemId,
                spanX = spanX,
                spanY = spanY,
            ) ?: return WorkspaceWidgetMutationResult.NoSpace

            val updated = items.map { item ->
                if (item.itemId == itemId) {
                    item.copy(spanX = resized.spanX, spanY = resized.spanY)
                } else {
                    item
                }
            }
            if (!dao.replacePrimaryHomeItemsIfSnapshotMatches(page, items, updated)) {
                return WorkspaceWidgetMutationResult.StoredWorkspaceChanged
            }
            WorkspaceWidgetMutationResult.Resized(itemId, resized.spanX, resized.spanY)
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

