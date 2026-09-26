package com.goreecloud.launcher.core.workspace.db

import com.goreecloud.launcher.core.workspace.WorkspaceAuthority
import com.goreecloud.launcher.core.workspace.WorkspaceGridPlacement
import com.goreecloud.launcher.core.workspace.WorkspaceMoveDirection
import com.goreecloud.launcher.core.workspace.WorkspaceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

sealed interface WorkspaceProductionRuntimeResult {
    data object WaitingForInitialization : WorkspaceProductionRuntimeResult
    data object DataStoreReady : WorkspaceProductionRuntimeResult
    data object RoomReady : WorkspaceProductionRuntimeResult
    data class RecoveryRequired(
        val health: WorkspacePostCutoverHealthResult,
    ) : WorkspaceProductionRuntimeResult
}

/**
 * Owns the reviewed production workspace cutover boundary.
 *
 * Before terminal Room authority, DataStore remains usable while startup reconciliation establishes
 * verified Room evidence and the production promotion coordinator performs the guarded one-way
 * authority transaction. After terminal ROOM, startup health is checked before authoritative Room
 * placement is exposed. Placement and paged HOME writes always use authority-aware repositories.
 */
class WorkspaceProductionRuntimeCoordinator(
    private val authorityRepository: WorkspaceRepository,
    private val workspaceDaoProvider: () -> WorkspaceDao?,
) {
    private val refreshEpoch = MutableStateFlow(0L)
    private val startupReconciler = WorkspaceStartupReconciler(
        repository = authorityRepository,
        workspaceDaoProvider = workspaceDaoProvider,
    )
    private val promotionCoordinator = WorkspaceProductionPromotionCoordinator(
        repository = authorityRepository,
        workspaceDaoProvider = workspaceDaoProvider,
    )
    private val postCutoverStartupCoordinator = WorkspacePostCutoverStartupCoordinator(
        repository = authorityRepository,
        workspaceDaoProvider = workspaceDaoProvider,
    )
    private val placementObserver = WorkspaceAuthoritativePlacementObserver(
        authorityRepository = authorityRepository,
        workspaceDaoProvider = workspaceDaoProvider,
    )
    private val pagedHomeObserver = WorkspacePagedHomeObserver(
        authorityRepository = authorityRepository,
        workspaceDaoProvider = workspaceDaoProvider,
    )
    private val placementRepository = WorkspaceAuthoritativePlacementRepository(
        authorityRepository = authorityRepository,
        workspaceDaoProvider = workspaceDaoProvider,
    )
    private val pagedMutationRepository = WorkspacePagedRoomMutationRepository(
        authorityRepository = authorityRepository,
        workspaceDaoProvider = workspaceDaoProvider,
    )
    private val homeItemPageMover = WorkspaceHomeItemPageMover(
        authorityRepository = authorityRepository,
        workspaceDaoProvider = workspaceDaoProvider,
        mutationRepository = pagedMutationRepository,
    )
    private val primaryHomeSpatialRepository = WorkspacePrimaryHomeSpatialRepository(
        authorityRepository = authorityRepository,
        workspaceDaoProvider = workspaceDaoProvider,
    )
    private val widgetRepository = WorkspaceWidgetRepository(
        authorityRepository = authorityRepository,
        workspaceDaoProvider = workspaceDaoProvider,
    )
    private val folderRepository = WorkspaceFolderRepository(
        authorityRepository = authorityRepository,
        workspaceDaoProvider = workspaceDaoProvider,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observePlacement(): Flow<WorkspaceAuthoritativePlacementState> = combine(
        authorityRepository.state.map { it.authority }.distinctUntilChanged(),
        refreshEpoch,
    ) { authority, epoch -> authority to epoch }
        .flatMapLatest { (authority, _) ->
            if (authority == WorkspaceAuthority.ROOM) {
                observeTerminalRoomAfterHealthGate()
            } else {
                placementObserver.observe()
            }
        }
        .distinctUntilChanged()

    fun observeHomePages(): Flow<WorkspacePagedHomeState> = pagedHomeObserver.observe()

    suspend fun reconcileAndActivate(): WorkspaceProductionRuntimeResult {
        val starting = authorityRepository.state.first()
        if (!starting.initialized) {
            refresh()
            return WorkspaceProductionRuntimeResult.WaitingForInitialization
        }

        val result = if (starting.authority == WorkspaceAuthority.ROOM) {
            reconcileTerminalRoom()
        } else {
            startupReconciler.reconcile()
            val reconciled = authorityRepository.state.first()
            if (reconciled.authority == WorkspaceAuthority.ROOM_VERIFIED) {
                when (val promotion = promotionCoordinator.promote()) {
                    WorkspaceProductionPromotionResult.PromotedHealthy -> {
                        WorkspaceProductionRuntimeResult.RoomReady
                    }
                    is WorkspaceProductionPromotionResult.PromotedRecoveryRequired -> {
                        WorkspaceProductionRuntimeResult.RecoveryRequired(promotion.health)
                    }
                    WorkspaceProductionPromotionResult.AlreadyRoomAuthoritative -> {
                        reconcileTerminalRoom()
                    }
                    else -> WorkspaceProductionRuntimeResult.DataStoreReady
                }
            } else {
                WorkspaceProductionRuntimeResult.DataStoreReady
            }
        }

        refresh()
        return result
    }

    suspend fun toggleFavorite(
        key: String,
        homeColumns: Int? = null,
        homeRows: Int? = null,
    ): WorkspaceAuthoritativeWriteResult {
        val grid = if (homeColumns != null && homeRows != null) {
            runCatching { WorkspaceGridPlacement.Grid(homeColumns, homeRows) }.getOrNull()
                ?: return WorkspaceAuthoritativeWriteResult.Mismatch
        } else {
            null
        }
        return placementRepository.toggleFavorite(key, grid)
    }

    suspend fun ensurePrimaryHomeSpatialGrid(
        columns: Int,
        rows: Int,
    ): WorkspacePrimaryHomeSpatialResult {
        val result = primaryHomeSpatialRepository.ensureGrid(columns, rows)
        if (result is WorkspacePrimaryHomeSpatialResult.Ready && result.changed) refresh()
        return result
    }

    suspend fun movePrimaryHomeAppToCell(
        appKey: String,
        columns: Int,
        rows: Int,
        cellX: Int,
        cellY: Int,
    ): WorkspacePrimaryHomeSpatialResult {
        val result = primaryHomeSpatialRepository.moveAppToCell(
            appKey = appKey,
            columns = columns,
            rows = rows,
            cellX = cellX,
            cellY = cellY,
        )
        if (result is WorkspacePrimaryHomeSpatialResult.Moved) refresh()
        return result
    }

    suspend fun toggleDock(key: String): WorkspaceAuthoritativeWriteResult =
        placementRepository.toggleDock(key)

    suspend fun moveFavorite(
        key: String,
        direction: WorkspaceMoveDirection,
    ): WorkspaceAuthoritativeWriteResult = placementRepository.moveFavorite(key, direction)

    suspend fun moveDock(
        key: String,
        direction: WorkspaceMoveDirection,
    ): WorkspaceAuthoritativeWriteResult = placementRepository.moveDock(key, direction)

    suspend fun moveFavoriteToTarget(
        key: String,
        targetKey: String,
    ): WorkspaceAuthoritativeWriteResult = placementRepository.moveFavoriteToTarget(key, targetKey)

    suspend fun moveDockToTarget(
        key: String,
        targetKey: String,
    ): WorkspaceAuthoritativeWriteResult = placementRepository.moveDockToTarget(key, targetKey)

    suspend fun moveHomeToDock(
        key: String,
        targetDockKey: String?,
    ): WorkspaceAuthoritativeWriteResult =
        placementRepository.moveHomeToDock(key, targetDockKey)

    suspend fun moveDockToPrimaryHomeCell(
        key: String,
        columns: Int,
        rows: Int,
        cellX: Int,
        cellY: Int,
    ): WorkspaceAuthoritativeWriteResult {
        val grid = runCatching { WorkspaceGridPlacement.Grid(columns, rows) }.getOrNull()
            ?: return WorkspaceAuthoritativeWriteResult.Mismatch
        val write = placementRepository.moveDockToHome(key, grid)
        return positionPrimaryHomeAfterWrite(write, key, columns, rows, cellX, cellY)
    }

    suspend fun copyDrawerToPrimaryHomeCell(
        key: String,
        columns: Int,
        rows: Int,
        cellX: Int,
        cellY: Int,
    ): WorkspaceAuthoritativeWriteResult {
        val grid = runCatching { WorkspaceGridPlacement.Grid(columns, rows) }.getOrNull()
            ?: return WorkspaceAuthoritativeWriteResult.Mismatch
        val write = placementRepository.copyDrawerToHome(key, grid)
        return positionPrimaryHomeAfterWrite(write, key, columns, rows, cellX, cellY)
    }

    suspend fun copyDrawerToDock(
        key: String,
        targetDockKey: String?,
    ): WorkspaceAuthoritativeWriteResult =
        placementRepository.copyDrawerToDock(key, targetDockKey)

    suspend fun reorderDockByDrop(
        key: String,
        targetDockKey: String?,
    ): WorkspaceAuthoritativeWriteResult =
        placementRepository.reorderDockByDrop(key, targetDockKey)

    suspend fun addFolderToHome(
        itemId: String,
        folderId: String,
        columns: Int,
        rows: Int,
    ): WorkspaceFolderMutationResult {
        val spatial = ensurePrimaryHomeSpatialGrid(columns, rows)
        if (spatial !is WorkspacePrimaryHomeSpatialResult.Ready) {
            return when (spatial) {
                WorkspacePrimaryHomeSpatialResult.Reserved -> WorkspaceFolderMutationResult.Reserved
                WorkspacePrimaryHomeSpatialResult.Unavailable -> WorkspaceFolderMutationResult.Unavailable
                WorkspacePrimaryHomeSpatialResult.InvalidWorkspace ->
                    WorkspaceFolderMutationResult.InvalidWorkspace
                WorkspacePrimaryHomeSpatialResult.StoredWorkspaceChanged ->
                    WorkspaceFolderMutationResult.StoredWorkspaceChanged
                is WorkspacePrimaryHomeSpatialResult.Failed ->
                    WorkspaceFolderMutationResult.Failed(spatial.failureType)
                is WorkspacePrimaryHomeSpatialResult.Moved,
                is WorkspacePrimaryHomeSpatialResult.Ready,
                -> WorkspaceFolderMutationResult.InvalidWorkspace
            }
        }
        val result = folderRepository.addFolderToHome(
            itemId = itemId,
            folderId = folderId,
            columns = columns,
            rows = rows,
        )
        if (result is WorkspaceFolderMutationResult.Added) refresh()
        return result
    }

    suspend fun movePrimaryHomeFolderToCell(
        folderId: String,
        columns: Int,
        rows: Int,
        cellX: Int,
        cellY: Int,
    ): WorkspaceFolderMutationResult {
        val spatial = ensurePrimaryHomeSpatialGrid(columns, rows)
        if (spatial !is WorkspacePrimaryHomeSpatialResult.Ready) {
            return when (spatial) {
                WorkspacePrimaryHomeSpatialResult.Reserved -> WorkspaceFolderMutationResult.Reserved
                WorkspacePrimaryHomeSpatialResult.Unavailable -> WorkspaceFolderMutationResult.Unavailable
                WorkspacePrimaryHomeSpatialResult.InvalidWorkspace ->
                    WorkspaceFolderMutationResult.InvalidWorkspace
                WorkspacePrimaryHomeSpatialResult.StoredWorkspaceChanged ->
                    WorkspaceFolderMutationResult.StoredWorkspaceChanged
                is WorkspacePrimaryHomeSpatialResult.Failed ->
                    WorkspaceFolderMutationResult.Failed(spatial.failureType)
                is WorkspacePrimaryHomeSpatialResult.Moved,
                is WorkspacePrimaryHomeSpatialResult.Ready ->
                    WorkspaceFolderMutationResult.InvalidWorkspace
            }
        }
        val result = folderRepository.moveFolderToCell(folderId, columns, rows, cellX, cellY)
        if (result is WorkspaceFolderMutationResult.Moved) refresh()
        return result
    }

    suspend fun moveHomeFolderToPage(
        folderId: String,
        targetPageId: String,
        columns: Int,
        rows: Int,
    ): WorkspaceFolderMutationResult {
        val primaryReady = ensurePrimaryHomeSpatialGrid(columns, rows)
        if (primaryReady !is WorkspacePrimaryHomeSpatialResult.Ready) {
            return when (primaryReady) {
                WorkspacePrimaryHomeSpatialResult.Reserved -> WorkspaceFolderMutationResult.Reserved
                WorkspacePrimaryHomeSpatialResult.Unavailable -> WorkspaceFolderMutationResult.Unavailable
                WorkspacePrimaryHomeSpatialResult.InvalidWorkspace ->
                    WorkspaceFolderMutationResult.InvalidWorkspace
                WorkspacePrimaryHomeSpatialResult.StoredWorkspaceChanged ->
                    WorkspaceFolderMutationResult.StoredWorkspaceChanged
                is WorkspacePrimaryHomeSpatialResult.Failed ->
                    WorkspaceFolderMutationResult.Failed(primaryReady.failureType)
                is WorkspacePrimaryHomeSpatialResult.Moved,
                is WorkspacePrimaryHomeSpatialResult.Ready ->
                    WorkspaceFolderMutationResult.InvalidWorkspace
            }
        }
        val result = folderRepository.moveFolderToPage(folderId, targetPageId, columns, rows)
        if (result is WorkspaceFolderMutationResult.MovedToPage) refresh()
        return result
    }

    suspend fun removeFolderFromHome(folderId: String): WorkspaceFolderMutationResult {
        val result = folderRepository.removeFolderFromHome(folderId)
        if (result is WorkspaceFolderMutationResult.Removed) refresh()
        return result
    }

    suspend fun addBuiltInWidget(
        itemId: String,
        typeId: String,
        columns: Int,
        rows: Int,
    ): WorkspaceWidgetMutationResult {
        val spatial = ensurePrimaryHomeSpatialGrid(columns, rows)
        if (spatial !is WorkspacePrimaryHomeSpatialResult.Ready) {
            return spatial.toWidgetMutationResult()
        }
        val result = widgetRepository.addBuiltInWidget(
            itemId = itemId,
            typeId = typeId,
            columns = columns,
            rows = rows,
        )
        if (result is WorkspaceWidgetMutationResult.Added) refresh()
        return result
    }

    suspend fun addAndroidWidget(
        itemId: String,
        appWidgetId: Int,
        providerComponent: String,
        columns: Int,
        rows: Int,
        spanX: Int = 2,
        spanY: Int = 2,
    ): WorkspaceWidgetMutationResult {
        val spatial = ensurePrimaryHomeSpatialGrid(columns, rows)
        if (spatial !is WorkspacePrimaryHomeSpatialResult.Ready) {
            return spatial.toWidgetMutationResult()
        }
        val result = widgetRepository.addAndroidWidget(
            itemId = itemId,
            appWidgetId = appWidgetId,
            providerComponent = providerComponent,
            columns = columns,
            rows = rows,
            spanX = spanX,
            spanY = spanY,
        )
        if (result is WorkspaceWidgetMutationResult.Added) refresh()
        return result
    }

    suspend fun removeWidget(itemId: String): WorkspaceWidgetMutationResult {
        val result = widgetRepository.removeWidget(itemId)
        if (result is WorkspaceWidgetMutationResult.Removed) refresh()
        return result
    }

    suspend fun moveWidget(
        itemId: String,
        columns: Int,
        rows: Int,
        cellX: Int,
        cellY: Int,
    ): WorkspaceWidgetMutationResult {
        val spatial = ensurePrimaryHomeSpatialGrid(columns, rows)
        if (spatial !is WorkspacePrimaryHomeSpatialResult.Ready) {
            return spatial.toWidgetMutationResult()
        }
        val result = widgetRepository.moveWidget(itemId, columns, rows, cellX, cellY)
        if (result is WorkspaceWidgetMutationResult.Moved) refresh()
        return result
    }

    suspend fun resizeWidget(
        itemId: String,
        columns: Int,
        rows: Int,
        spanX: Int,
        spanY: Int,
    ): WorkspaceWidgetMutationResult {
        val spatial = ensurePrimaryHomeSpatialGrid(columns, rows)
        if (spatial !is WorkspacePrimaryHomeSpatialResult.Ready) {
            return spatial.toWidgetMutationResult()
        }
        val result = widgetRepository.resizeWidget(
            itemId = itemId,
            columns = columns,
            rows = rows,
            spanX = spanX,
            spanY = spanY,
        )
        if (result is WorkspaceWidgetMutationResult.Resized) refresh()
        return result
    }

    suspend fun createHomePage(pageId: String): WorkspacePagedRoomMutationResult {
        val result = pagedMutationRepository.createHomePage(pageId)
        if (result is WorkspacePagedRoomMutationResult.CreatedPage) {
            refresh()
        }
        return result
    }

    suspend fun deleteEmptyHomePage(pageId: String): WorkspacePagedRoomMutationResult {
        val result = pagedMutationRepository.deleteEmptyHomePage(pageId)
        if (result is WorkspacePagedRoomMutationResult.DeletedPage) {
            refresh()
        }
        return result
    }

    suspend fun moveHomePage(
        pageId: String,
        targetRank: Int,
    ): WorkspacePagedRoomMutationResult {
        val result = pagedMutationRepository.moveHomePage(pageId, targetRank)
        if (result is WorkspacePagedRoomMutationResult.Updated) {
            refresh()
        }
        return result
    }

    suspend fun moveHomeAppToPage(
        sourcePageId: String,
        appKey: String,
        targetPageId: String,
        primaryColumns: Int? = null,
        primaryRows: Int? = null,
    ): WorkspacePagedRoomMutationResult {
        val primaryBoundary =
            sourcePageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID ||
                targetPageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID
        val primaryGrid = if (primaryBoundary) {
            val columns = primaryColumns
                ?: return WorkspacePagedRoomMutationResult.PrimaryPageProtected
            val rows = primaryRows
                ?: return WorkspacePagedRoomMutationResult.PrimaryPageProtected
            when (val ready = ensurePrimaryHomeSpatialGrid(columns, rows)) {
                is WorkspacePrimaryHomeSpatialResult.Ready -> {
                    runCatching { WorkspaceGridPlacement.Grid(columns, rows) }.getOrNull()
                        ?: return WorkspacePagedRoomMutationResult.InvalidWorkspace
                }
                WorkspacePrimaryHomeSpatialResult.Reserved -> {
                    return WorkspacePagedRoomMutationResult.Reserved
                }
                WorkspacePrimaryHomeSpatialResult.Unavailable -> {
                    return WorkspacePagedRoomMutationResult.Unavailable
                }
                WorkspacePrimaryHomeSpatialResult.InvalidWorkspace -> {
                    return WorkspacePagedRoomMutationResult.InvalidWorkspace
                }
                WorkspacePrimaryHomeSpatialResult.StoredWorkspaceChanged -> {
                    return WorkspacePagedRoomMutationResult.StoredWorkspaceChanged
                }
                is WorkspacePrimaryHomeSpatialResult.Failed -> {
                    return WorkspacePagedRoomMutationResult.Failed(ready.failureType)
                }
                is WorkspacePrimaryHomeSpatialResult.Moved -> {
                    return WorkspacePagedRoomMutationResult.InvalidWorkspace
                }
            }
        } else {
            null
        }

        val result = homeItemPageMover.moveAppToPage(
            sourcePageId = sourcePageId,
            appKey = appKey,
            targetPageId = targetPageId,
            primaryGrid = primaryGrid,
        )
        if (result is WorkspacePagedRoomMutationResult.UpdatedItem) {
            refresh()
        }
        return result
    }

    suspend fun moveHomeAppWithinPage(
        pageId: String,
        appKey: String,
        direction: WorkspaceMoveDirection,
    ): WorkspacePagedRoomMutationResult {
        val result = homeItemPageMover.moveAppWithinPage(
            pageId = pageId,
            appKey = appKey,
            direction = direction,
        )
        if (result is WorkspacePagedRoomMutationResult.UpdatedItem) {
            refresh()
        }
        return result
    }

    suspend fun moveHomeAppOneCellWithinPage(
        pageId: String,
        appKey: String,
        direction: WorkspaceHomeSpatialDirection,
    ): WorkspacePagedRoomMutationResult {
        val result = homeItemPageMover.moveAppOneCellWithinPage(
            pageId = pageId,
            appKey = appKey,
            direction = direction,
        )
        if (result is WorkspacePagedRoomMutationResult.UpdatedItem) {
            refresh()
        }
        return result
    }

    private suspend fun positionPrimaryHomeAfterWrite(
        write: WorkspaceAuthoritativeWriteResult,
        key: String,
        columns: Int,
        rows: Int,
        cellX: Int,
        cellY: Int,
    ): WorkspaceAuthoritativeWriteResult {
        val written = write as? WorkspaceAuthoritativeWriteResult.Written ?: return write
        if (key !in written.snapshot.favoriteKeys) return written
        if (written.snapshot.source != WorkspacePlacementSource.ROOM) return written

        val placement = primaryHomeSpatialRepository.moveAppToCell(
            appKey = key,
            columns = columns,
            rows = rows,
            cellX = cellX,
            cellY = cellY,
        )
        return when (placement) {
            is WorkspacePrimaryHomeSpatialResult.Moved -> {
                refresh()
                written
            }
            WorkspacePrimaryHomeSpatialResult.Reserved ->
                WorkspaceAuthoritativeWriteResult.AuthorityChanged
            WorkspacePrimaryHomeSpatialResult.Unavailable ->
                WorkspaceAuthoritativeWriteResult.Unavailable
            WorkspacePrimaryHomeSpatialResult.InvalidWorkspace,
            WorkspacePrimaryHomeSpatialResult.StoredWorkspaceChanged ->
                WorkspaceAuthoritativeWriteResult.Mismatch
            is WorkspacePrimaryHomeSpatialResult.Failed ->
                WorkspaceAuthoritativeWriteResult.Failed(placement.failureType)
            is WorkspacePrimaryHomeSpatialResult.Ready ->
                WorkspaceAuthoritativeWriteResult.Mismatch
        }
    }

    private suspend fun reconcileTerminalRoom(): WorkspaceProductionRuntimeResult =
        when (val startup = postCutoverStartupCoordinator.reconcile()) {
            WorkspacePostCutoverStartupResult.Ready -> WorkspaceProductionRuntimeResult.RoomReady
            WorkspacePostCutoverStartupResult.NotRoomAuthoritative -> {
                WorkspaceProductionRuntimeResult.DataStoreReady
            }
            is WorkspacePostCutoverStartupResult.RecoveryRequired -> {
                WorkspaceProductionRuntimeResult.RecoveryRequired(startup.health)
            }
        }

    private fun observeTerminalRoomAfterHealthGate(): Flow<WorkspaceAuthoritativePlacementState> = flow {
        when (val startup = postCutoverStartupCoordinator.reconcile()) {
            WorkspacePostCutoverStartupResult.Ready -> emitAll(placementObserver.observe())
            WorkspacePostCutoverStartupResult.NotRoomAuthoritative -> emitAll(placementObserver.observe())
            is WorkspacePostCutoverStartupResult.RecoveryRequired -> emit(
                WorkspaceAuthoritativePlacementState.RecoveryRequired(
                    startup.health.toPlacementRecoveryReason()
                )
            )
        }
    }

    private fun refresh() {
        refreshEpoch.value += 1
    }
}

private fun WorkspacePrimaryHomeSpatialResult.toWidgetMutationResult():
    WorkspaceWidgetMutationResult = when (this) {
        WorkspacePrimaryHomeSpatialResult.Reserved -> WorkspaceWidgetMutationResult.Reserved
        WorkspacePrimaryHomeSpatialResult.Unavailable -> WorkspaceWidgetMutationResult.Unavailable
        WorkspacePrimaryHomeSpatialResult.InvalidWorkspace ->
            WorkspaceWidgetMutationResult.InvalidWorkspace
        WorkspacePrimaryHomeSpatialResult.StoredWorkspaceChanged ->
            WorkspaceWidgetMutationResult.StoredWorkspaceChanged
        is WorkspacePrimaryHomeSpatialResult.Failed ->
            WorkspaceWidgetMutationResult.Failed(failureType)
        is WorkspacePrimaryHomeSpatialResult.Ready,
        is WorkspacePrimaryHomeSpatialResult.Moved,
        -> WorkspaceWidgetMutationResult.InvalidWorkspace
    }

private fun WorkspacePostCutoverHealthResult.toPlacementRecoveryReason():
    WorkspaceAuthoritativePlacementRecoveryReason = when (this) {
        WorkspacePostCutoverHealthResult.Unavailable -> {
            WorkspaceAuthoritativePlacementRecoveryReason.Unavailable
        }
        WorkspacePostCutoverHealthResult.Mismatch,
        WorkspacePostCutoverHealthResult.AuthorityChanged,
        WorkspacePostCutoverHealthResult.NotRoomAuthoritative -> {
            WorkspaceAuthoritativePlacementRecoveryReason.Mismatch
        }
        is WorkspacePostCutoverHealthResult.Failed -> {
            WorkspaceAuthoritativePlacementRecoveryReason.Failed(failureType)
        }
        WorkspacePostCutoverHealthResult.Healthy -> {
            WorkspaceAuthoritativePlacementRecoveryReason.Mismatch
        }
    }
