package com.goreecloud.launcher.core.workspace.db

import com.goreecloud.launcher.core.workspace.WorkspaceAuthority
import com.goreecloud.launcher.core.workspace.WorkspaceGridPlacement
import com.goreecloud.launcher.core.workspace.WorkspacePagedPlacement
import com.goreecloud.launcher.core.workspace.WorkspaceRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

sealed interface WorkspacePagedRoomMutationResult {
    data object Reserved : WorkspacePagedRoomMutationResult
    data object Unavailable : WorkspacePagedRoomMutationResult
    data object PageNotFound : WorkspacePagedRoomMutationResult
    data object PageAlreadyExists : WorkspacePagedRoomMutationResult
    data object PrimaryPageProtected : WorkspacePagedRoomMutationResult
    data object LastHomePageProtected : WorkspacePagedRoomMutationResult
    data object PageNotEmpty : WorkspacePagedRoomMutationResult
    data object ItemNotFound : WorkspacePagedRoomMutationResult
    data object ItemIdentityMismatch : WorkspacePagedRoomMutationResult
    data object InvalidWorkspace : WorkspacePagedRoomMutationResult
    data object TargetRankOutOfRange : WorkspacePagedRoomMutationResult
    data object StoredPageSetChanged : WorkspacePagedRoomMutationResult
    data object StoredWorkspaceChanged : WorkspacePagedRoomMutationResult
    data class CreatedPage(
        val pageId: String,
        val rank: Int,
    ) : WorkspacePagedRoomMutationResult
    data class DeletedPage(
        val pageId: String,
        val orderedPageIds: List<String>,
    ) : WorkspacePagedRoomMutationResult
    data class Updated(val orderedPageIds: List<String>) : WorkspacePagedRoomMutationResult
    data class UpdatedItem(
        val itemId: String,
        val pageId: String,
        val cellX: Int,
        val cellY: Int,
        val spanX: Int,
        val spanY: Int,
    ) : WorkspacePagedRoomMutationResult
    data class Failed(val failureType: String) : WorkspacePagedRoomMutationResult
}

/**
 * Authoritative Room wiring for validated multi-page HOME mutations.
 *
 * Page creation, empty-page deletion, page ordering, and secondary-page item placement require
 * terminal Room authority. The protected primary HOME compatibility page remains rank zero and is
 * excluded from spatial item placement until a separately accepted primary-grid migration exists.
 * Creation appends an empty page through a complete page-snapshot comparison. Deletion is
 * intentionally limited to empty non-primary pages and repeats the full page/item snapshot and
 * emptiness checks inside the Room transaction so the page FK cascade can never remove a
 * concurrently inserted child. Item writes additionally compare the complete observed HOME
 * page/item snapshot so a validated secondary-page placement cannot overwrite a concurrent
 * workspace change.
 */
class WorkspacePagedRoomMutationRepository(
    private val authorityRepository: WorkspaceRepository,
    private val workspaceDaoProvider: () -> WorkspaceDao?,
) {
    suspend fun createHomePage(pageId: String): WorkspacePagedRoomMutationResult {
        if (!isRoomAuthoritative()) return WorkspacePagedRoomMutationResult.Reserved
        if (pageId.isBlank()) return WorkspacePagedRoomMutationResult.InvalidWorkspace
        val dao = workspaceDaoOrNull() ?: return WorkspacePagedRoomMutationResult.Unavailable

        return try {
            if (dao.readPages(listOf(pageId)).isNotEmpty()) {
                return WorkspacePagedRoomMutationResult.PageAlreadyExists
            }
            val storedPages = dao.readPagesByContainer(WorkspaceContainerType.HOME)
            if (
                storedPages.isEmpty() ||
                storedPages.map { it.rank } != storedPages.indices.toList() ||
                storedPages.firstOrNull()?.pageId != WorkspaceLegacyImportMapper.HOME_PAGE_ID
            ) {
                return WorkspacePagedRoomMutationResult.InvalidWorkspace
            }
            val newPage = WorkspacePageEntity(
                pageId = pageId,
                containerType = WorkspaceContainerType.HOME,
                rank = storedPages.size,
            )
            if (!dao.appendPageIfSnapshotMatches(
                    containerType = WorkspaceContainerType.HOME,
                    expectedPages = storedPages,
                    newPage = newPage,
                )
            ) {
                return WorkspacePagedRoomMutationResult.StoredPageSetChanged
            }
            WorkspacePagedRoomMutationResult.CreatedPage(newPage.pageId, newPage.rank)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            WorkspacePagedRoomMutationResult.Failed(exception::class.java.simpleName)
        }
    }

    suspend fun deleteEmptyHomePage(pageId: String): WorkspacePagedRoomMutationResult {
        if (!isRoomAuthoritative()) return WorkspacePagedRoomMutationResult.Reserved
        if (pageId.isBlank()) return WorkspacePagedRoomMutationResult.InvalidWorkspace
        if (pageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID) {
            return WorkspacePagedRoomMutationResult.PrimaryPageProtected
        }
        val dao = workspaceDaoOrNull() ?: return WorkspacePagedRoomMutationResult.Unavailable

        return try {
            val storedPages = dao.readPagesByContainer(WorkspaceContainerType.HOME)
            if (storedPages.none { it.pageId == pageId }) {
                return WorkspacePagedRoomMutationResult.PageNotFound
            }
            if (storedPages.size <= 1) {
                return WorkspacePagedRoomMutationResult.LastHomePageProtected
            }
            if (
                storedPages.map { it.rank } != storedPages.indices.toList() ||
                storedPages.firstOrNull()?.pageId != WorkspaceLegacyImportMapper.HOME_PAGE_ID
            ) {
                return WorkspacePagedRoomMutationResult.InvalidWorkspace
            }

            val storedItems = dao.readItems(storedPages.map { it.pageId })
            if (storedItems.any { it.pageId == pageId }) {
                return WorkspacePagedRoomMutationResult.PageNotEmpty
            }

            if (!dao.deleteEmptyPageIfSnapshotMatches(
                    containerType = WorkspaceContainerType.HOME,
                    pageId = pageId,
                    protectedPageId = WorkspaceLegacyImportMapper.HOME_PAGE_ID,
                    expectedPages = storedPages,
                    expectedItems = storedItems,
                )
            ) {
                return WorkspacePagedRoomMutationResult.StoredWorkspaceChanged
            }

            WorkspacePagedRoomMutationResult.DeletedPage(
                pageId = pageId,
                orderedPageIds = storedPages.filterNot { it.pageId == pageId }.map { it.pageId },
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            WorkspacePagedRoomMutationResult.Failed(exception::class.java.simpleName)
        }
    }

    suspend fun moveHomePage(
        pageId: String,
        targetRank: Int,
    ): WorkspacePagedRoomMutationResult {
        if (!isRoomAuthoritative()) return WorkspacePagedRoomMutationResult.Reserved
        val dao = workspaceDaoOrNull() ?: return WorkspacePagedRoomMutationResult.Unavailable

        return try {
            val storedPages = dao.readPagesByContainer(WorkspaceContainerType.HOME)
            if (storedPages.none { it.pageId == pageId }) {
                return WorkspacePagedRoomMutationResult.PageNotFound
            }
            if (targetRank !in storedPages.indices) {
                return WorkspacePagedRoomMutationResult.TargetRankOutOfRange
            }
            if (storedPages.firstOrNull()?.pageId != WorkspaceLegacyImportMapper.HOME_PAGE_ID) {
                return WorkspacePagedRoomMutationResult.InvalidWorkspace
            }
            if (
                (pageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID && targetRank != 0) ||
                (pageId != WorkspaceLegacyImportMapper.HOME_PAGE_ID && targetRank == 0)
            ) {
                return WorkspacePagedRoomMutationResult.PrimaryPageProtected
            }

            val domainPages = storedPages.map { page ->
                WorkspacePagedPlacement.Page(
                    pageId = page.pageId,
                    rank = page.rank,
                    placements = emptyList(),
                )
            }
            val mutation = WorkspacePagedPlacement.movePage(
                grid = WorkspaceGridPlacement.Grid(columns = 1, rows = 1),
                pages = domainPages,
                pageId = pageId,
                targetRank = targetRank,
            )
            val updated = mutation as? WorkspacePagedPlacement.Mutation.Updated
                ?: return when (mutation) {
                    is WorkspacePagedPlacement.Mutation.PageNotFound -> WorkspacePagedRoomMutationResult.PageNotFound
                    is WorkspacePagedPlacement.Mutation.TargetRankOutOfRange -> WorkspacePagedRoomMutationResult.TargetRankOutOfRange
                    else -> WorkspacePagedRoomMutationResult.StoredPageSetChanged
                }

            val orderedPageIds = updated.pages.map { it.pageId }
            if (orderedPageIds.firstOrNull() != WorkspaceLegacyImportMapper.HOME_PAGE_ID) {
                return WorkspacePagedRoomMutationResult.PrimaryPageProtected
            }
            if (!dao.replacePageOrder(WorkspaceContainerType.HOME, orderedPageIds)) {
                return WorkspacePagedRoomMutationResult.StoredPageSetChanged
            }
            WorkspacePagedRoomMutationResult.Updated(orderedPageIds)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            WorkspacePagedRoomMutationResult.Failed(exception::class.java.simpleName)
        }
    }

    suspend fun moveHomeItem(
        grid: WorkspaceGridPlacement.Grid,
        itemId: String,
        targetPageId: String,
        targetPlacement: WorkspaceGridPlacement.Placement,
    ): WorkspacePagedRoomMutationResult {
        if (!isRoomAuthoritative()) return WorkspacePagedRoomMutationResult.Reserved
        if (targetPageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID) {
            return WorkspacePagedRoomMutationResult.PrimaryPageProtected
        }
        val dao = workspaceDaoOrNull() ?: return WorkspacePagedRoomMutationResult.Unavailable

        return try {
            if (WorkspaceCanonicalRoomPlacementReader.read(dao) == null) {
                return WorkspacePagedRoomMutationResult.InvalidWorkspace
            }

            val storedPages = dao.readPagesByContainer(WorkspaceContainerType.HOME)
            if (storedPages.none { it.pageId == targetPageId }) {
                return WorkspacePagedRoomMutationResult.PageNotFound
            }
            if (storedPages.firstOrNull()?.pageId != WorkspaceLegacyImportMapper.HOME_PAGE_ID) {
                return WorkspacePagedRoomMutationResult.InvalidWorkspace
            }
            val pageIds = storedPages.map { it.pageId }
            val storedItems = dao.readItems(pageIds)
            val sourceItem = storedItems.singleOrNull { it.itemId == itemId }
                ?: return WorkspacePagedRoomMutationResult.ItemNotFound
            if (sourceItem.pageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID) {
                return WorkspacePagedRoomMutationResult.PrimaryPageProtected
            }
            if (targetPlacement.itemId != itemId) {
                return WorkspacePagedRoomMutationResult.ItemIdentityMismatch
            }

            val spatialPages = storedPages.filterNot {
                it.pageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID
            }
            val spatialPageIds = spatialPages.map { it.pageId }.toSet()
            val spatialItems = storedItems.filter { it.pageId in spatialPageIds }
            if (spatialItems.any { it.cellX == null || it.cellY == null }) {
                return WorkspacePagedRoomMutationResult.InvalidWorkspace
            }

            val itemsByPage = spatialItems.groupBy { it.pageId }
            val domainPages = spatialPages.map { page ->
                WorkspacePagedPlacement.Page(
                    pageId = page.pageId,
                    rank = page.rank,
                    placements = itemsByPage[page.pageId].orEmpty().map { item ->
                        WorkspaceGridPlacement.Placement(
                            itemId = item.itemId,
                            cellX = checkNotNull(item.cellX),
                            cellY = checkNotNull(item.cellY),
                            spanX = item.spanX,
                            spanY = item.spanY,
                        )
                    },
                )
            }

            val mutation = WorkspacePagedPlacement.moveItem(
                grid = grid,
                pages = domainPages,
                itemId = itemId,
                targetPageId = targetPageId,
                targetPlacement = targetPlacement,
            )
            val updated = mutation as? WorkspacePagedPlacement.Mutation.Updated
                ?: return when (mutation) {
                    is WorkspacePagedPlacement.Mutation.PageNotFound -> WorkspacePagedRoomMutationResult.PageNotFound
                    is WorkspacePagedPlacement.Mutation.ItemNotFound -> WorkspacePagedRoomMutationResult.ItemNotFound
                    is WorkspacePagedPlacement.Mutation.ItemIdentityMismatch -> WorkspacePagedRoomMutationResult.ItemIdentityMismatch
                    is WorkspacePagedPlacement.Mutation.InvalidWorkspace -> WorkspacePagedRoomMutationResult.InvalidWorkspace
                    else -> WorkspacePagedRoomMutationResult.InvalidWorkspace
                }

            val finalPlacement = updated.pages
                .firstOrNull { it.pageId == targetPageId }
                ?.placements
                ?.singleOrNull { it.itemId == itemId }
                ?: return WorkspacePagedRoomMutationResult.InvalidWorkspace

            val targetRank = if (sourceItem.pageId == targetPageId) {
                sourceItem.rank
            } else {
                val maxRank = storedItems
                    .asSequence()
                    .filter { it.pageId == targetPageId && it.itemId != itemId }
                    .maxOfOrNull { it.rank }
                if (maxRank == Int.MAX_VALUE) {
                    return WorkspacePagedRoomMutationResult.Failed("TargetRankOverflow")
                }
                (maxRank ?: -1) + 1
            }
            val updatedItem = sourceItem.copy(
                pageId = targetPageId,
                rank = targetRank,
                cellX = finalPlacement.cellX,
                cellY = finalPlacement.cellY,
                spanX = finalPlacement.spanX,
                spanY = finalPlacement.spanY,
            )

            if (!dao.replaceItemPlacementIfSnapshotMatches(
                    containerType = WorkspaceContainerType.HOME,
                    expectedPages = storedPages,
                    expectedItems = storedItems,
                    updatedItem = updatedItem,
                )
            ) {
                return WorkspacePagedRoomMutationResult.StoredWorkspaceChanged
            }

            WorkspacePagedRoomMutationResult.UpdatedItem(
                itemId = updatedItem.itemId,
                pageId = updatedItem.pageId,
                cellX = finalPlacement.cellX,
                cellY = finalPlacement.cellY,
                spanX = finalPlacement.spanX,
                spanY = finalPlacement.spanY,
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            WorkspacePagedRoomMutationResult.Failed(exception::class.java.simpleName)
        }
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
