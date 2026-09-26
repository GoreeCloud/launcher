package com.goreecloud.launcher.core.workspace.db

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.goreecloud.launcher.core.workspace.WorkspaceGridPlacement
import com.goreecloud.launcher.core.workspace.WorkspacePagedPlacement
import com.goreecloud.launcher.core.workspace.WorkspacePortableSnapshot
import kotlinx.coroutines.flow.Flow

/**
 * Opaque rollback/recovery checkpoint for the bounded portable HOME placement writer.
 *
 * The constructor is internal so external callers cannot manufacture an arbitrary applied state;
 * plans are produced by [WorkspaceDao.planPortableHomePlacements] after current-state validation.
 */
class WorkspacePortableHomeRestoreCommit internal constructor(
    val previousPages: List<WorkspacePageEntity>,
    val previousItems: List<WorkspaceItemEntity>,
    val appliedPages: List<WorkspacePageEntity>,
    val appliedItems: List<WorkspaceItemEntity>,
)

data class WorkspacePortableHomeState(
    val pages: List<WorkspacePageEntity>,
    val items: List<WorkspaceItemEntity>,
)

@Dao
abstract class WorkspaceDao {
    @Query("SELECT * FROM workspace_pages ORDER BY containerType, rank")
    abstract fun observePages(): Flow<List<WorkspacePageEntity>>

    @Query("SELECT * FROM workspace_items ORDER BY pageId, rank")
    abstract fun observeItems(): Flow<List<WorkspaceItemEntity>>

    @Query("SELECT * FROM workspace_pages")
    abstract suspend fun readAllPages(): List<WorkspacePageEntity>

    @Query("SELECT * FROM workspace_pages WHERE pageId IN (:pageIds)")
    abstract suspend fun readPages(pageIds: List<String>): List<WorkspacePageEntity>

    @Query("SELECT * FROM workspace_pages WHERE containerType = :containerType ORDER BY rank")
    abstract suspend fun readPagesByContainer(containerType: String): List<WorkspacePageEntity>

    @Query("SELECT * FROM workspace_items WHERE pageId IN (:pageIds)")
    abstract suspend fun readItems(pageIds: List<String>): List<WorkspaceItemEntity>

    @Query(
        "SELECT workspace_items.* FROM workspace_items " +
            "INNER JOIN workspace_pages ON workspace_pages.pageId = workspace_items.pageId " +
            "WHERE workspace_pages.containerType = :containerType " +
            "ORDER BY workspace_pages.rank, workspace_items.rank, workspace_items.itemId"
    )
    abstract suspend fun readItemsByContainer(containerType: String): List<WorkspaceItemEntity>

    @Query("SELECT COUNT(*) FROM workspace_items")
    abstract suspend fun itemCount(): Int

    @Upsert
    abstract suspend fun upsertPages(pages: List<WorkspacePageEntity>)

    @Upsert
    abstract suspend fun upsertItems(items: List<WorkspaceItemEntity>)

    @Query("DELETE FROM workspace_pages WHERE pageId IN (:pageIds)")
    protected abstract suspend fun deletePages(pageIds: List<String>)

    @Query("DELETE FROM workspace_pages WHERE containerType = :containerType")
    protected abstract suspend fun deletePagesByContainer(containerType: String)

    @Query("DELETE FROM workspace_items WHERE pageId IN (:pageIds)")
    protected abstract suspend fun deleteItemsByPages(pageIds: List<String>)

    @Query("UPDATE workspace_pages SET rank = -(rank + 1) WHERE containerType = :containerType")
    protected abstract suspend fun stagePageRanks(containerType: String)

    @Transaction
    open suspend fun replaceLegacySnapshot(
        pages: List<WorkspacePageEntity>,
        items: List<WorkspaceItemEntity>,
    ) {
        if (pages.isEmpty()) return
        deletePages(pages.map { it.pageId })
        upsertPages(pages)
        if (items.isNotEmpty()) upsertItems(items)
    }

    /** Read the complete current HOME page/item state in one Room transaction. */
    @Transaction
    open suspend fun readPortableHomeState(): WorkspacePortableHomeState = WorkspacePortableHomeState(
        pages = readPagesByContainer(WorkspaceContainerType.HOME),
        items = readItemsByContainer(WorkspaceContainerType.HOME).canonicalItems(),
    )

    /**
     * Validate and plan the currently supported same-resolved-identity HOME placement restore
     * without mutating Room. A later apply rechecks this exact previous state before writing.
     */
    @Transaction
    open suspend fun planPortableHomePlacements(
        snapshot: WorkspacePortableSnapshot.Snapshot,
    ): WorkspacePortableHomeRestoreCommit = planPortableHomePlacementsUnsafe(snapshot)

    /**
     * Apply an already validated plan only if the complete HOME state still exactly matches the
     * plan's previous state. This closes the plan/journal/apply concurrency window fail closed.
     */
    @Transaction
    open suspend fun applyPortableHomeRestoreCommit(
        commit: WorkspacePortableHomeRestoreCommit,
    ) {
        applyPortableHomeRestoreCommitUnsafe(commit)
    }

    /**
     * Replace the currently supported portable HOME placement subset in one Room transaction.
     *
     * This preserves the original one-call behavior while reusing the same planning and exact-state
     * apply paths used by the crash-recovery-aware cross-store writer.
     */
    @Transaction
    open suspend fun replacePortableHomePlacements(
        snapshot: WorkspacePortableSnapshot.Snapshot,
    ): WorkspacePortableHomeRestoreCommit {
        val commit = planPortableHomePlacementsUnsafe(snapshot)
        applyPortableHomeRestoreCommitUnsafe(commit)
        return commit
    }

    /**
     * Compensate a failed cross-store restore only while the HOME state is still exactly the state
     * written by [applyPortableHomeRestoreCommit]. A concurrent workspace mutation makes rollback
     * fail closed rather than silently deleting newer user state.
     */
    @Transaction
    open suspend fun rollbackPortableHomePlacements(
        commit: WorkspacePortableHomeRestoreCommit,
    ) {
        val current = readPortableHomeStateUnsafe()
        check(current.pages == commit.appliedPages && current.items == commit.appliedItems) {
            "portable HOME rollback refused because workspace changed after restore apply"
        }

        replaceHomeStateUnsafe(commit.previousPages, commit.previousItems)
        val restored = readPortableHomeStateUnsafe()
        check(restored.pages == commit.previousPages) {
            "portable HOME rollback page verification failed"
        }
        check(restored.items == commit.previousItems) {
            "portable HOME rollback item verification failed"
        }
    }

    /**
     * Appends one empty page only when the caller's complete observed container-page snapshot is
     * still current. The page id must be globally unused and its rank must be exactly the next
     * contiguous rank. No child workspace item is created or moved by this transaction.
     */
    @Transaction
    open suspend fun appendPageIfSnapshotMatches(
        containerType: String,
        expectedPages: List<WorkspacePageEntity>,
        newPage: WorkspacePageEntity,
    ): Boolean {
        if (newPage.pageId.isBlank() || newPage.containerType != containerType) return false
        val currentPages = readPagesByContainer(containerType)
        if (currentPages != expectedPages) return false
        if (currentPages.map { it.rank } != currentPages.indices.toList()) return false
        if (newPage.rank != currentPages.size) return false
        if (readPages(listOf(newPage.pageId)).isNotEmpty()) return false

        upsertPages(listOf(newPage))
        return true
    }

    /**
     * Deletes exactly one empty, non-protected page only if the complete page/item snapshot remains
     * unchanged inside this transaction. The emptiness check is repeated after the snapshot read so
     * a concurrent item insertion fails closed instead of being removed through the page FK cascade.
     * Remaining page ranks are compacted transactionally after the deletion.
     */
    @Transaction
    open suspend fun deleteEmptyPageIfSnapshotMatches(
        containerType: String,
        pageId: String,
        protectedPageId: String,
        expectedPages: List<WorkspacePageEntity>,
        expectedItems: List<WorkspaceItemEntity>,
    ): Boolean {
        if (pageId.isBlank() || pageId == protectedPageId) return false

        val currentPages = readPagesByContainer(containerType)
        if (currentPages != expectedPages || currentPages.size <= 1) return false
        if (currentPages.singleOrNull { it.pageId == pageId } == null) return false
        if (currentPages.map { it.rank } != currentPages.indices.toList()) return false

        val currentItems = readItems(currentPages.map { it.pageId })
        val currentById = currentItems.associateBy { it.itemId }
        val expectedById = expectedItems.associateBy { it.itemId }
        if (currentById.size != currentItems.size || expectedById.size != expectedItems.size) return false
        if (currentById != expectedById) return false
        if (currentItems.any { it.pageId == pageId }) return false

        val remaining = currentPages.filterNot { it.pageId == pageId }
        stagePageRanks(containerType)
        deletePages(listOf(pageId))
        upsertPages(
            remaining.mapIndexed { rank, page ->
                page.copy(rank = rank)
            }
        )
        return true
    }

    /**
     * Rewrites only page ranks for one container while preserving page rows and all child items.
     * The supplied page identity set must exactly match the current container identity set.
     */
    @Transaction
    open suspend fun replacePageOrder(
        containerType: String,
        orderedPageIds: List<String>,
    ): Boolean {
        if (orderedPageIds.isEmpty() || orderedPageIds.any { it.isBlank() }) return false
        if (orderedPageIds.size != orderedPageIds.distinct().size) return false

        val existing = readPagesByContainer(containerType)
        if (existing.size != orderedPageIds.size) return false
        if (existing.map { it.pageId }.toSet() != orderedPageIds.toSet()) return false

        stagePageRanks(containerType)
        val byId = existing.associateBy { it.pageId }
        upsertPages(
            orderedPageIds.mapIndexed { rank, pageId ->
                checkNotNull(byId[pageId]).copy(rank = rank)
            }
        )
        return true
    }

    /**
     * Replaces the complete primary HOME item set only when the primary page and item snapshot
     * still exactly match the caller's observed state. Item identity must be preserved.
     */
    @Transaction
    open suspend fun replacePrimaryHomeItemsIfSnapshotMatches(
        expectedPage: WorkspacePageEntity,
        expectedItems: List<WorkspaceItemEntity>,
        updatedItems: List<WorkspaceItemEntity>,
    ): Boolean {
        if (
            expectedPage.pageId != WorkspaceLegacyImportMapper.HOME_PAGE_ID ||
            expectedPage.containerType != WorkspaceContainerType.HOME ||
            expectedPage.rank != 0
        ) return false

        val currentPage = readPages(listOf(WorkspaceLegacyImportMapper.HOME_PAGE_ID)).singleOrNull()
            ?: return false
        if (currentPage != expectedPage) return false

        val currentItems = readItems(listOf(WorkspaceLegacyImportMapper.HOME_PAGE_ID))
        val currentById = currentItems.associateBy { it.itemId }
        val expectedById = expectedItems.associateBy { it.itemId }
        val updatedById = updatedItems.associateBy { it.itemId }
        if (
            currentById.size != currentItems.size ||
            expectedById.size != expectedItems.size ||
            updatedById.size != updatedItems.size ||
            currentById != expectedById ||
            updatedById.keys != expectedById.keys
        ) return false
        if (updatedItems.any { it.pageId != WorkspaceLegacyImportMapper.HOME_PAGE_ID }) return false

        if (updatedItems.isNotEmpty()) upsertItems(updatedItems)
        return true
    }

    /**
     * Replaces the complete primary HOME item set while allowing a deliberate item add/remove.
     *
     * The caller must supply the complete observed snapshot. Room rechecks that snapshot inside
     * this transaction before changing identities, preserving the same fail-closed concurrency
     * boundary used by existing placement writers.
     */
    @Transaction
    open suspend fun replacePrimaryHomeItemsIncludingIdentityChangesIfSnapshotMatches(
        expectedPage: WorkspacePageEntity,
        expectedItems: List<WorkspaceItemEntity>,
        updatedItems: List<WorkspaceItemEntity>,
    ): Boolean {
        if (
            expectedPage.pageId != WorkspaceLegacyImportMapper.HOME_PAGE_ID ||
            expectedPage.containerType != WorkspaceContainerType.HOME ||
            expectedPage.rank != 0
        ) return false

        val currentPage = readPages(listOf(WorkspaceLegacyImportMapper.HOME_PAGE_ID)).singleOrNull()
            ?: return false
        if (currentPage != expectedPage) return false

        val currentItems = readItems(listOf(WorkspaceLegacyImportMapper.HOME_PAGE_ID))
        val currentById = currentItems.associateBy { it.itemId }
        val expectedById = expectedItems.associateBy { it.itemId }
        if (
            currentById.size != currentItems.size ||
            expectedById.size != expectedItems.size ||
            currentById != expectedById
        ) return false

        if (
            updatedItems.any { it.pageId != WorkspaceLegacyImportMapper.HOME_PAGE_ID } ||
            updatedItems.map { it.itemId }.distinct().size != updatedItems.size ||
            updatedItems.map { it.rank }.sorted() != updatedItems.indices.toList()
        ) return false

        deleteItemsByPages(listOf(WorkspaceLegacyImportMapper.HOME_PAGE_ID))
        if (updatedItems.isNotEmpty()) upsertItems(updatedItems)

        val applied = readItems(listOf(WorkspaceLegacyImportMapper.HOME_PAGE_ID))
            .sortedBy { it.rank }
        check(applied == updatedItems.sortedBy { it.rank }) {
            "primary HOME identity-changing replacement readback verification failed"
        }
        return true
    }

    /**
     * Applies one item placement write only if the complete HOME page/item snapshot observed by
     * the caller is still current when this transaction executes. This prevents a validated
     * cross-page placement from overwriting a concurrent workspace mutation.
     */
    @Transaction
    open suspend fun replaceItemPlacementIfSnapshotMatches(
        containerType: String,
        expectedPages: List<WorkspacePageEntity>,
        expectedItems: List<WorkspaceItemEntity>,
        updatedItem: WorkspaceItemEntity,
    ): Boolean {
        if (updatedItem.itemId.isBlank() || updatedItem.pageId.isBlank()) return false

        val currentPages = readPagesByContainer(containerType)
        if (currentPages != expectedPages) return false
        val currentPageIds = currentPages.map { it.pageId }
        if (updatedItem.pageId !in currentPageIds) return false

        val currentItems = readItems(currentPageIds)
        val currentById = currentItems.associateBy { it.itemId }
        val expectedById = expectedItems.associateBy { it.itemId }
        if (currentById.size != currentItems.size || expectedById.size != expectedItems.size) return false
        if (currentById != expectedById) return false
        if (updatedItem.itemId !in currentById) return false

        upsertItems(listOf(updatedItem))
        return true
    }

    /**
     * Replaces the complete HOME item set without changing HOME pages, but only while the caller's
     * full observed page/item snapshot is still current. Item identities must be preserved. This is
     * used for mutations that need to move one item across the primary HOME boundary while also
     * compacting primary ranks atomically.
     */
    @Transaction
    open suspend fun replaceHomeItemsIfSnapshotMatches(
        expectedPages: List<WorkspacePageEntity>,
        expectedItems: List<WorkspaceItemEntity>,
        updatedItems: List<WorkspaceItemEntity>,
    ): Boolean {
        if (expectedPages.isEmpty()) return false
        val currentPages = readPagesByContainer(WorkspaceContainerType.HOME)
        if (currentPages != expectedPages) return false
        if (currentPages.map { it.rank } != currentPages.indices.toList()) return false

        val pageIds = currentPages.map { it.pageId }
        val currentItems = readItems(pageIds).canonicalItems()
        val expectedCanonical = expectedItems.canonicalItems()
        val updatedCanonical = updatedItems.canonicalItems()
        val currentById = currentItems.associateBy { it.itemId }
        val expectedById = expectedCanonical.associateBy { it.itemId }
        val updatedById = updatedCanonical.associateBy { it.itemId }
        if (
            currentById.size != currentItems.size ||
            expectedById.size != expectedCanonical.size ||
            updatedById.size != updatedCanonical.size ||
            currentItems != expectedCanonical ||
            updatedById.keys != expectedById.keys ||
            updatedCanonical.any { it.pageId !in pageIds }
        ) return false
        if (
            updatedCanonical.groupBy { it.pageId }.values.any { pageItems ->
                pageItems.map { it.rank }.distinct().size != pageItems.size
            }
        ) return false

        deleteItemsByPages(pageIds)
        if (updatedCanonical.isNotEmpty()) upsertItems(updatedCanonical)

        val applied = readItems(pageIds).canonicalItems()
        check(applied == updatedCanonical) {
            "HOME item replacement readback verification failed"
        }
        return true
    }

    /**
     * Folder-only add/remove variant of the full HOME compare-and-swap.
     *
     * The regular HOME placement method requires an identical set of item IDs and is
     * deliberately not valid for adding or deleting a folder. This narrow variant
     * permits exactly one added OR removed folder identity, never an app/widget,
     * while comparing every page and item inside the same Room transaction.
     */
    @Transaction
    open suspend fun replaceHomeItemsIncludingFolderIdentityChangesIfSnapshotMatches(
        expectedPages: List<WorkspacePageEntity>,
        expectedItems: List<WorkspaceItemEntity>,
        updatedItems: List<WorkspaceItemEntity>,
    ): Boolean {
        if (expectedPages.isEmpty()) return false
        val currentPages = readPagesByContainer(WorkspaceContainerType.HOME)
        if (currentPages != expectedPages ||
            currentPages.first().pageId != WorkspaceLegacyImportMapper.HOME_PAGE_ID ||
            currentPages.map { it.rank } != currentPages.indices.toList()
        ) return false

        val pageIds = currentPages.map { it.pageId }
        val currentItems = readItems(pageIds).canonicalItems()
        val expectedCanonical = expectedItems.canonicalItems()
        val updatedCanonical = updatedItems.canonicalItems()
        val currentById = currentItems.associateBy { it.itemId }
        val expectedById = expectedCanonical.associateBy { it.itemId }
        val updatedById = updatedCanonical.associateBy { it.itemId }
        if (
            currentById.size != currentItems.size ||
            expectedById.size != expectedCanonical.size ||
            updatedById.size != updatedCanonical.size ||
            currentItems != expectedCanonical ||
            updatedCanonical.any { it.pageId !in pageIds }
        ) return false

        val added = updatedById.keys - expectedById.keys
        val removed = expectedById.keys - updatedById.keys
        if (
            !((added.size == 1 && removed.isEmpty()) ||
                (removed.size == 1 && added.isEmpty())) ||
            added.any { updatedById[it]?.itemType != WorkspaceItemType.FOLDER } ||
            removed.any { expectedById[it]?.itemType != WorkspaceItemType.FOLDER }
        ) return false
        if (
            updatedCanonical.groupBy { it.pageId }.values.any { pageItems ->
                pageItems.map { it.rank }.sorted() != pageItems.indices.toList()
            }
        ) return false

        deleteItemsByPages(pageIds)
        if (updatedCanonical.isNotEmpty()) upsertItems(updatedCanonical)
        check(readItems(pageIds).canonicalItems() == updatedCanonical) {
            "HOME folder identity-changing replacement readback verification failed"
        }
        return true
    }

    private suspend fun planPortableHomePlacementsUnsafe(
        snapshot: WorkspacePortableSnapshot.Snapshot,
    ): WorkspacePortableHomeRestoreCommit {
        // Reuse the codec as the defensive structural/geometry validation authority even for direct
        // callers that did not arrive through LauncherPortableRestoreImport.
        WorkspacePortableSnapshot.encode(snapshot)

        val previousPages = readPagesByContainer(WorkspaceContainerType.HOME)
        val previousItems = readItemsByContainer(WorkspaceContainerType.HOME).canonicalItems()
        val currentById = previousItems.associateBy { it.itemId }
        check(currentById.size == previousItems.size) {
            "current HOME workspace contains duplicate item identities"
        }

        val orderedPages = WorkspacePagedPlacement.ordered(snapshot.pages)
        val targetPlacements = orderedPages.flatMap { page ->
            page.placements.map { placement -> page to placement }
        }
        val targetItemIds = targetPlacements.map { (_, placement) -> placement.itemId }
        check(targetItemIds.size == targetItemIds.distinct().size) {
            "portable HOME snapshot contains duplicate item identities"
        }
        check(targetItemIds.toSet() == currentById.keys) {
            "portable HOME snapshot cannot be rebound: item identities must exactly match the current HOME app set"
        }
        check(
            previousItems.all {
                it.itemType == WorkspaceItemType.APP && !it.appKey.isNullOrBlank()
            }
        ) {
            "portable HOME restore does not yet support shortcut, folder, widget, or unresolved app rebinding"
        }

        val nonHomePageIds = readAllPages()
            .asSequence()
            .filter { it.containerType != WorkspaceContainerType.HOME }
            .map { it.pageId }
            .toSet()
        check(orderedPages.none { it.pageId in nonHomePageIds }) {
            "portable HOME snapshot page identity collides with non-HOME state"
        }

        val appliedPages = orderedPages.map { page ->
            WorkspacePageEntity(
                pageId = page.pageId,
                containerType = WorkspaceContainerType.HOME,
                rank = page.rank,
            )
        }
        val appliedItems = orderedPages.flatMap { page ->
            page.placements
                .sortedWith(portablePlacementOrder)
                .mapIndexed { rank, placement ->
                    checkNotNull(currentById[placement.itemId]).copy(
                        pageId = page.pageId,
                        rank = rank,
                        cellX = placement.cellX,
                        cellY = placement.cellY,
                        spanX = placement.spanX,
                        spanY = placement.spanY,
                    )
                }
        }.canonicalItems()

        return WorkspacePortableHomeRestoreCommit(
            previousPages = previousPages,
            previousItems = previousItems,
            appliedPages = appliedPages,
            appliedItems = appliedItems,
        )
    }

    private suspend fun applyPortableHomeRestoreCommitUnsafe(
        commit: WorkspacePortableHomeRestoreCommit,
    ) {
        val current = readPortableHomeStateUnsafe()
        check(current.pages == commit.previousPages && current.items == commit.previousItems) {
            "portable HOME apply refused because workspace changed after restore planning"
        }

        val nonHomePageIds = readAllPages()
            .asSequence()
            .filter { it.containerType != WorkspaceContainerType.HOME }
            .map { it.pageId }
            .toSet()
        check(commit.appliedPages.none { it.pageId in nonHomePageIds }) {
            "portable HOME apply refused because a target page identity now collides with non-HOME state"
        }

        replaceHomeStateUnsafe(commit.appliedPages, commit.appliedItems)
        val applied = readPortableHomeStateUnsafe()
        check(applied.pages == commit.appliedPages) {
            "portable HOME page readback verification failed"
        }
        check(applied.items == commit.appliedItems) {
            "portable HOME item readback verification failed"
        }
    }

    private suspend fun readPortableHomeStateUnsafe(): WorkspacePortableHomeState = WorkspacePortableHomeState(
        pages = readPagesByContainer(WorkspaceContainerType.HOME),
        items = readItemsByContainer(WorkspaceContainerType.HOME).canonicalItems(),
    )

    private suspend fun replaceHomeStateUnsafe(
        pages: List<WorkspacePageEntity>,
        items: List<WorkspaceItemEntity>,
    ) {
        deletePagesByContainer(WorkspaceContainerType.HOME)
        if (pages.isNotEmpty()) upsertPages(pages)
        if (items.isNotEmpty()) upsertItems(items)
    }

    private fun List<WorkspaceItemEntity>.canonicalItems(): List<WorkspaceItemEntity> =
        sortedWith(compareBy({ it.pageId }, { it.rank }, { it.itemId }))

    private companion object {
        val portablePlacementOrder = compareBy<WorkspaceGridPlacement.Placement>(
            { it.cellY },
            { it.cellX },
            { it.spanY },
            { it.spanX },
            { it.itemId },
        )
    }
}
