package com.goreecloud.launcher.core.workspace.db

import com.goreecloud.launcher.core.workspace.WorkspaceAuthority
import com.goreecloud.launcher.core.workspace.WorkspaceGridPlacement
import com.goreecloud.launcher.core.workspace.WorkspaceMoveDirection
import com.goreecloud.launcher.core.workspace.WorkspaceRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

enum class WorkspaceHomeSpatialDirection {
    LEFT,
    RIGHT,
    UP,
    DOWN,
}

/**
 * Chooses deterministic placements for existing HOME applications while keeping Room authoritative.
 * Secondary-to-secondary writes delegate to [WorkspacePagedRoomMutationRepository.moveHomeItem].
 * Primary-boundary writes require an explicit accepted primary grid and atomically rewrite the
 * complete HOME item snapshot so primary ranks remain canonical. Exact-cell cross-page moves also
 * require the configured Home grid so out-of-bounds or colliding destinations fail closed.
 * Preflight reads never carry write
 * authority: every mutation repeats the complete relevant snapshot inside Room before writing.
 */
class WorkspaceHomeItemPageMover(
    private val authorityRepository: WorkspaceRepository,
    private val workspaceDaoProvider: () -> WorkspaceDao?,
    private val mutationRepository: WorkspacePagedRoomMutationRepository,
) {
    suspend fun moveAppToPage(
        sourcePageId: String,
        appKey: String,
        targetPageId: String,
        primaryGrid: WorkspaceGridPlacement.Grid? = null,
        targetCellX: Int? = null,
        targetCellY: Int? = null,
    ): WorkspacePagedRoomMutationResult {
        if (sourcePageId.isBlank() || appKey.isBlank() || targetPageId.isBlank()) {
            return WorkspacePagedRoomMutationResult.InvalidWorkspace
        }
        if (sourcePageId == targetPageId) {
            return WorkspacePagedRoomMutationResult.InvalidWorkspace
        }
        if ((targetCellX == null) != (targetCellY == null)) {
            return WorkspacePagedRoomMutationResult.InvalidWorkspace
        }
        val primaryBoundary =
            sourcePageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID ||
                targetPageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID
        if (primaryBoundary) {
            val grid = primaryGrid ?: return WorkspacePagedRoomMutationResult.PrimaryPageProtected
            return moveAppAcrossPrimaryBoundary(
                sourcePageId = sourcePageId,
                appKey = appKey,
                targetPageId = targetPageId,
                primaryGrid = grid,
                targetCellX = targetCellX,
                targetCellY = targetCellY,
            )
        }

        val context = when (val read = readMoveContext(sourcePageId, appKey)) {
            is MoveContextResult.Ready -> read.context
            is MoveContextResult.Failed -> return read.result
        }
        if (context.pages.none { it.pageId == targetPageId }) {
            return WorkspacePagedRoomMutationResult.PageNotFound
        }

        val grid = if (targetCellX != null && targetCellY != null) {
            primaryGrid ?: return WorkspacePagedRoomMutationResult.InvalidWorkspace
        } else {
            deriveGrid(context.items, context.source)
        }
        val targetPlacements = context.items
            .filter { it.pageId == targetPageId && it.itemId != context.source.itemId }
            .map(::toPlacement)
        val target = requestedOrFirstAvailablePlacement(
            grid = grid,
            occupied = targetPlacements,
            source = context.source,
            targetCellX = targetCellX,
            targetCellY = targetCellY,
        ) ?: return WorkspacePagedRoomMutationResult.InvalidWorkspace

        return mutationRepository.moveHomeItem(
            grid = grid,
            itemId = context.source.itemId,
            targetPageId = targetPageId,
            targetPlacement = target,
        )
    }

    suspend fun moveAppWithinPage(
        pageId: String,
        appKey: String,
        direction: WorkspaceMoveDirection,
    ): WorkspacePagedRoomMutationResult {
        if (pageId.isBlank() || appKey.isBlank()) {
            return WorkspacePagedRoomMutationResult.InvalidWorkspace
        }
        if (pageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID) {
            return WorkspacePagedRoomMutationResult.PrimaryPageProtected
        }
        val context = when (val read = readMoveContext(pageId, appKey)) {
            is MoveContextResult.Ready -> read.context
            is MoveContextResult.Failed -> return read.result
        }
        val grid = deriveGrid(context.items, context.source)
        val occupied = context.items
            .filter { it.pageId == pageId && it.itemId != context.source.itemId }
            .map(::toPlacement)
        val target = relativeAvailablePlacement(grid, occupied, context.source, direction)
            ?: return WorkspacePagedRoomMutationResult.InvalidWorkspace

        return mutationRepository.moveHomeItem(
            grid = grid,
            itemId = context.source.itemId,
            targetPageId = pageId,
            targetPlacement = target,
        )
    }

    /**
     * Moves one existing secondary HOME app exactly one grid cell in a requested spatial direction.
     * Occupied or out-of-bounds targets fail closed rather than swapping or displacing another
     * item. The authoritative mutation repository re-reads the complete snapshot before writing.
     */
    suspend fun moveAppOneCellWithinPage(
        pageId: String,
        appKey: String,
        direction: WorkspaceHomeSpatialDirection,
    ): WorkspacePagedRoomMutationResult {
        if (pageId.isBlank() || appKey.isBlank()) {
            return WorkspacePagedRoomMutationResult.InvalidWorkspace
        }
        if (pageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID) {
            return WorkspacePagedRoomMutationResult.PrimaryPageProtected
        }
        val context = when (val read = readMoveContext(pageId, appKey)) {
            is MoveContextResult.Ready -> read.context
            is MoveContextResult.Failed -> return read.result
        }
        val grid = deriveGrid(context.items, context.source)
        val occupied = context.items
            .filter { it.pageId == pageId && it.itemId != context.source.itemId }
            .map(::toPlacement)
        val sourceX = checkNotNull(context.source.cellX)
        val sourceY = checkNotNull(context.source.cellY)
        val targetX = sourceX + when (direction) {
            WorkspaceHomeSpatialDirection.LEFT -> -1
            WorkspaceHomeSpatialDirection.RIGHT -> 1
            WorkspaceHomeSpatialDirection.UP,
            WorkspaceHomeSpatialDirection.DOWN -> 0
        }
        val targetY = sourceY + when (direction) {
            WorkspaceHomeSpatialDirection.UP -> -1
            WorkspaceHomeSpatialDirection.DOWN -> 1
            WorkspaceHomeSpatialDirection.LEFT,
            WorkspaceHomeSpatialDirection.RIGHT -> 0
        }
        if (
            targetX < 0 || targetY < 0 ||
            targetX + context.source.spanX > grid.columns ||
            targetY + context.source.spanY > grid.rows
        ) {
            return WorkspacePagedRoomMutationResult.InvalidWorkspace
        }
        val target = placementAt(context.source, targetX, targetY)
        if (WorkspaceGridPlacement.validate(grid, occupied + target) != WorkspaceGridPlacement.Validation.Valid) {
            return WorkspacePagedRoomMutationResult.InvalidWorkspace
        }

        return mutationRepository.moveHomeItem(
            grid = grid,
            itemId = context.source.itemId,
            targetPageId = pageId,
            targetPlacement = target,
        )
    }

    /**
     * Moves one existing secondary HOME app to an exact cell in the configured Home grid.
     * The source page remains unchanged. Occupied or out-of-bounds targets fail closed, and
     * Room revalidates the complete page snapshot before the mutation is committed.
     */
    suspend fun moveAppToCellWithinPage(
        pageId: String,
        appKey: String,
        grid: WorkspaceGridPlacement.Grid,
        targetCellX: Int,
        targetCellY: Int,
    ): WorkspacePagedRoomMutationResult {
        if (pageId.isBlank() || appKey.isBlank()) {
            return WorkspacePagedRoomMutationResult.InvalidWorkspace
        }
        if (pageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID) {
            return WorkspacePagedRoomMutationResult.PrimaryPageProtected
        }
        val context = when (val read = readMoveContext(pageId, appKey)) {
            is MoveContextResult.Ready -> read.context
            is MoveContextResult.Failed -> return read.result
        }
        if (
            targetCellX < 0 ||
            targetCellY < 0 ||
            targetCellX + context.source.spanX > grid.columns ||
            targetCellY + context.source.spanY > grid.rows
        ) {
            return WorkspacePagedRoomMutationResult.InvalidWorkspace
        }
        val occupied = context.items
            .filter { it.pageId == pageId && it.itemId != context.source.itemId }
            .map(::toPlacement)
        val target = placementAt(context.source, targetCellX, targetCellY)
        if (
            WorkspaceGridPlacement.validate(grid, occupied + target) !=
                WorkspaceGridPlacement.Validation.Valid
        ) {
            return WorkspacePagedRoomMutationResult.InvalidWorkspace
        }

        return mutationRepository.moveHomeItem(
            grid = grid,
            itemId = context.source.itemId,
            targetPageId = pageId,
            targetPlacement = target,
        )
    }

    private suspend fun moveAppAcrossPrimaryBoundary(
        sourcePageId: String,
        appKey: String,
        targetPageId: String,
        primaryGrid: WorkspaceGridPlacement.Grid,
        targetCellX: Int?,
        targetCellY: Int?,
    ): WorkspacePagedRoomMutationResult {
        if (
            primaryGrid.columns !in WorkspacePrimaryHomeGridMigrationPlanner.MIN_PRIMARY_HOME_COLUMNS..
                WorkspacePrimaryHomeGridMigrationPlanner.MAX_PRIMARY_HOME_COLUMNS ||
            primaryGrid.rows !in WorkspacePrimaryHomeGridMigrationPlanner.MIN_PRIMARY_HOME_ROWS..
                WorkspacePrimaryHomeGridMigrationPlanner.MAX_PRIMARY_HOME_ROWS
        ) {
            return WorkspacePagedRoomMutationResult.InvalidWorkspace
        }
        val state = authorityRepository.state.first()
        if (!state.initialized || state.authority != WorkspaceAuthority.ROOM) {
            return WorkspacePagedRoomMutationResult.Reserved
        }
        val dao = workspaceDaoOrNull() ?: return WorkspacePagedRoomMutationResult.Unavailable

        return try {
            val pages = dao.readPagesByContainer(WorkspaceContainerType.HOME)
            if (
                pages.isEmpty() ||
                pages.map { it.rank } != pages.indices.toList() ||
                pages.firstOrNull()?.pageId != WorkspaceLegacyImportMapper.HOME_PAGE_ID
            ) {
                return WorkspacePagedRoomMutationResult.InvalidWorkspace
            }
            if (pages.none { it.pageId == sourcePageId } || pages.none { it.pageId == targetPageId }) {
                return WorkspacePagedRoomMutationResult.PageNotFound
            }

            val pageIds = pages.map { it.pageId }
            val items = dao.readItems(pageIds)
            if (items.map { it.itemId }.distinct().size != items.size) {
                return WorkspacePagedRoomMutationResult.InvalidWorkspace
            }
            val candidates = items.filter {
                it.pageId == sourcePageId &&
                    it.itemType == WorkspaceItemType.APP &&
                    it.appKey == appKey
            }
            if (candidates.isEmpty()) return WorkspacePagedRoomMutationResult.ItemNotFound
            if (candidates.size != 1) return WorkspacePagedRoomMutationResult.InvalidWorkspace
            val source = candidates.single()
            val targetItems = items.filter { it.pageId == targetPageId && it.itemId != source.itemId }
            if (targetItems.any { it.itemType == WorkspaceItemType.APP && it.appKey == appKey }) {
                return WorkspacePagedRoomMutationResult.InvalidWorkspace
            }

            val primaryPage = pages.first()
            val primaryItems = items
                .filter { it.pageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID }
                .sortedBy { it.rank }
            when (
                WorkspacePrimaryHomeGridMigrationPlanner.plan(
                    page = primaryPage,
                    items = primaryItems,
                    columns = primaryGrid.columns,
                    rows = primaryGrid.rows,
                )
            ) {
                WorkspacePrimaryHomeGridMigrationPlanningResult.Empty,
                WorkspacePrimaryHomeGridMigrationPlanningResult.AlreadySpatial -> Unit
                else -> return WorkspacePagedRoomMutationResult.InvalidWorkspace
            }

            val secondaryItems = items.filterNot {
                it.pageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID
            }
            if (secondaryItems.any { it.cellX == null || it.cellY == null }) {
                return WorkspacePagedRoomMutationResult.InvalidWorkspace
            }

            val targetGrid = if (
                targetPageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID ||
                (targetCellX != null && targetCellY != null)
            ) {
                primaryGrid
            } else {
                deriveGrid(
                    items = secondaryItems,
                    source = source,
                    minimumColumns = primaryGrid.columns,
                )
            }
            val targetPlacements = targetItems.map(::toPlacement)
            val targetPlacement = requestedOrFirstAvailablePlacement(
                grid = targetGrid,
                occupied = targetPlacements,
                source = source,
                targetCellX = targetCellX,
                targetCellY = targetCellY,
            ) ?: return WorkspacePagedRoomMutationResult.InvalidWorkspace
            val targetRank = if (targetPageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID) {
                primaryItems.size
            } else {
                val maxRank = targetItems.maxOfOrNull { it.rank }
                if (maxRank == Int.MAX_VALUE) {
                    return WorkspacePagedRoomMutationResult.Failed("TargetRankOverflow")
                }
                (maxRank ?: -1) + 1
            }
            val updatedSource = source.copy(
                pageId = targetPageId,
                rank = targetRank,
                cellX = targetPlacement.cellX,
                cellY = targetPlacement.cellY,
                spanX = targetPlacement.spanX,
                spanY = targetPlacement.spanY,
            )

            val compactedPrimaryById = if (sourcePageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID) {
                primaryItems
                    .filterNot { it.itemId == source.itemId }
                    .mapIndexed { rank, item -> item.copy(rank = rank) }
                    .associateBy { it.itemId }
            } else {
                emptyMap()
            }
            val updatedItems = items.map { item ->
                when {
                    item.itemId == source.itemId -> updatedSource
                    item.itemId in compactedPrimaryById -> checkNotNull(compactedPrimaryById[item.itemId])
                    else -> item
                }
            }

            val updatedPrimary = updatedItems
                .filter { it.pageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID }
                .sortedBy { it.rank }
            when (
                WorkspacePrimaryHomeGridMigrationPlanner.plan(
                    page = primaryPage,
                    items = updatedPrimary,
                    columns = primaryGrid.columns,
                    rows = primaryGrid.rows,
                )
            ) {
                WorkspacePrimaryHomeGridMigrationPlanningResult.Empty,
                WorkspacePrimaryHomeGridMigrationPlanningResult.AlreadySpatial -> Unit
                else -> return WorkspacePagedRoomMutationResult.InvalidWorkspace
            }

            val updatedSecondary = updatedItems.filterNot {
                it.pageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID
            }
            if (updatedSecondary.any { it.cellX == null || it.cellY == null }) {
                return WorkspacePagedRoomMutationResult.InvalidWorkspace
            }
            val secondaryValidationGrid = deriveValidationGrid(
                items = updatedSecondary,
                minimumColumns = primaryGrid.columns,
            )
            for (placements in updatedSecondary.groupBy { it.pageId }.values) {
                if (
                    WorkspaceGridPlacement.validate(
                        secondaryValidationGrid,
                        placements.map(::toPlacement),
                    ) != WorkspaceGridPlacement.Validation.Valid
                ) {
                    return WorkspacePagedRoomMutationResult.InvalidWorkspace
                }
            }

            if (!dao.replaceHomeItemsIfSnapshotMatches(
                    expectedPages = pages,
                    expectedItems = items,
                    updatedItems = updatedItems,
                )
            ) {
                return WorkspacePagedRoomMutationResult.StoredWorkspaceChanged
            }

            WorkspacePagedRoomMutationResult.UpdatedItem(
                itemId = updatedSource.itemId,
                pageId = updatedSource.pageId,
                cellX = checkNotNull(updatedSource.cellX),
                cellY = checkNotNull(updatedSource.cellY),
                spanX = updatedSource.spanX,
                spanY = updatedSource.spanY,
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            WorkspacePagedRoomMutationResult.Failed(exception::class.java.simpleName)
        }
    }

    private suspend fun readMoveContext(sourcePageId: String, appKey: String): MoveContextResult {
        val state = authorityRepository.state.first()
        if (!state.initialized || state.authority != WorkspaceAuthority.ROOM) {
            return MoveContextResult.Failed(WorkspacePagedRoomMutationResult.Reserved)
        }
        val dao = workspaceDaoOrNull()
            ?: return MoveContextResult.Failed(WorkspacePagedRoomMutationResult.Unavailable)

        return try {
            val pages = dao.readPagesByContainer(WorkspaceContainerType.HOME)
            if (pages.none { it.pageId == sourcePageId }) {
                return MoveContextResult.Failed(WorkspacePagedRoomMutationResult.PageNotFound)
            }
            if (pages.firstOrNull()?.pageId != WorkspaceLegacyImportMapper.HOME_PAGE_ID) {
                return MoveContextResult.Failed(WorkspacePagedRoomMutationResult.InvalidWorkspace)
            }
            val items = dao.readItems(pages.map { it.pageId })
            val spatialItems = items.filterNot {
                it.pageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID
            }
            if (spatialItems.any { it.cellX == null || it.cellY == null }) {
                return MoveContextResult.Failed(WorkspacePagedRoomMutationResult.InvalidWorkspace)
            }
            val candidates = spatialItems.filter {
                it.pageId == sourcePageId &&
                    it.itemType == WorkspaceItemType.APP &&
                    it.appKey == appKey
            }
            if (candidates.isEmpty()) {
                return MoveContextResult.Failed(WorkspacePagedRoomMutationResult.ItemNotFound)
            }
            if (candidates.size != 1) {
                return MoveContextResult.Failed(WorkspacePagedRoomMutationResult.InvalidWorkspace)
            }
            MoveContextResult.Ready(MoveContext(pages, spatialItems, candidates.single()))
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            MoveContextResult.Failed(
                WorkspacePagedRoomMutationResult.Failed(exception::class.java.simpleName)
            )
        }
    }

    private fun deriveGrid(
        items: List<WorkspaceItemEntity>,
        source: WorkspaceItemEntity,
        minimumColumns: Int = MIN_HOME_COLUMNS,
    ): WorkspaceGridPlacement.Grid {
        val existingColumns = items.maxOfOrNull { checkNotNull(it.cellX) + it.spanX } ?: 0
        val existingRows = items.maxOfOrNull { checkNotNull(it.cellY) + it.spanY } ?: 0
        val columns = maxOf(MIN_HOME_COLUMNS, minimumColumns, existingColumns, source.spanX)
        val rows = maxOf(1, existingRows + source.spanY)
        return WorkspaceGridPlacement.Grid(columns = columns, rows = rows)
    }

    private fun deriveValidationGrid(
        items: List<WorkspaceItemEntity>,
        minimumColumns: Int,
    ): WorkspaceGridPlacement.Grid {
        val existingColumns = items.maxOfOrNull { checkNotNull(it.cellX) + it.spanX } ?: 0
        val existingRows = items.maxOfOrNull { checkNotNull(it.cellY) + it.spanY } ?: 0
        return WorkspaceGridPlacement.Grid(
            columns = maxOf(MIN_HOME_COLUMNS, minimumColumns, existingColumns),
            rows = maxOf(1, existingRows),
        )
    }

    private fun requestedOrFirstAvailablePlacement(
        grid: WorkspaceGridPlacement.Grid,
        occupied: List<WorkspaceGridPlacement.Placement>,
        source: WorkspaceItemEntity,
        targetCellX: Int?,
        targetCellY: Int?,
    ): WorkspaceGridPlacement.Placement? {
        if (targetCellX == null && targetCellY == null) {
            return firstAvailablePlacement(grid, occupied, source)
        }
        val cellX = targetCellX ?: return null
        val cellY = targetCellY ?: return null
        val candidate = placementAt(source, cellX, cellY)
        return candidate.takeIf {
            WorkspaceGridPlacement.validate(grid, occupied + candidate) ==
                WorkspaceGridPlacement.Validation.Valid
        }
    }

    private fun firstAvailablePlacement(
        grid: WorkspaceGridPlacement.Grid,
        occupied: List<WorkspaceGridPlacement.Placement>,
        source: WorkspaceItemEntity,
    ): WorkspaceGridPlacement.Placement? {
        for (cellY in 0..grid.rows - source.spanY) {
            for (cellX in 0..grid.columns - source.spanX) {
                val candidate = placementAt(source, cellX, cellY)
                if (WorkspaceGridPlacement.validate(grid, occupied + candidate) == WorkspaceGridPlacement.Validation.Valid) {
                    return candidate
                }
            }
        }
        return null
    }

    private fun relativeAvailablePlacement(
        grid: WorkspaceGridPlacement.Grid,
        occupied: List<WorkspaceGridPlacement.Placement>,
        source: WorkspaceItemEntity,
        direction: WorkspaceMoveDirection,
    ): WorkspaceGridPlacement.Placement? {
        val sourceX = checkNotNull(source.cellX)
        val sourceY = checkNotNull(source.cellY)
        val sourceIndex = sourceY * grid.columns + sourceX
        val candidates = buildList {
            for (cellY in 0..grid.rows - source.spanY) {
                for (cellX in 0..grid.columns - source.spanX) {
                    val index = cellY * grid.columns + cellX
                    val inDirection = when (direction) {
                        WorkspaceMoveDirection.EARLIER -> index < sourceIndex
                        WorkspaceMoveDirection.LATER -> index > sourceIndex
                    }
                    if (!inDirection) continue
                    val candidate = placementAt(source, cellX, cellY)
                    if (WorkspaceGridPlacement.validate(grid, occupied + candidate) == WorkspaceGridPlacement.Validation.Valid) {
                        add(index to candidate)
                    }
                }
            }
        }
        return when (direction) {
            WorkspaceMoveDirection.EARLIER -> candidates.maxByOrNull { it.first }?.second
            WorkspaceMoveDirection.LATER -> candidates.minByOrNull { it.first }?.second
        }
    }

    private fun placementAt(source: WorkspaceItemEntity, cellX: Int, cellY: Int) =
        WorkspaceGridPlacement.Placement(
            itemId = source.itemId,
            cellX = cellX,
            cellY = cellY,
            spanX = source.spanX,
            spanY = source.spanY,
        )

    private fun toPlacement(item: WorkspaceItemEntity): WorkspaceGridPlacement.Placement =
        WorkspaceGridPlacement.Placement(
            itemId = item.itemId,
            cellX = checkNotNull(item.cellX),
            cellY = checkNotNull(item.cellY),
            spanX = item.spanX,
            spanY = item.spanY,
        )

    private fun workspaceDaoOrNull(): WorkspaceDao? = try {
        workspaceDaoProvider()
    } catch (exception: CancellationException) {
        throw exception
    } catch (_: Exception) {
        null
    }

    private sealed interface MoveContextResult {
        data class Ready(val context: MoveContext) : MoveContextResult
        data class Failed(val result: WorkspacePagedRoomMutationResult) : MoveContextResult
    }

    private data class MoveContext(
        val pages: List<WorkspacePageEntity>,
        val items: List<WorkspaceItemEntity>,
        val source: WorkspaceItemEntity,
    )

    private companion object {
        const val MIN_HOME_COLUMNS = 4
    }
}
