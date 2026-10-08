package com.goreecloud.launcher.core.workspace

/**
 * Multi-page placement contract layered on [WorkspaceGridPlacement].
 *
 * This remains Android-framework independent so page identity, rank ordering, global item
 * uniqueness, per-page grid validity, and deterministic page/item mutations can be enforced
 * before Room persistence or rendering.
 */
object WorkspacePagedPlacement {
    data class Page(
        val pageId: String,
        val rank: Int,
        val placements: List<WorkspaceGridPlacement.Placement>,
    ) {
        init {
            require(pageId.isNotBlank()) { "pageId must not be blank" }
            require(rank >= 0) { "page rank must be non-negative" }
        }
    }

    sealed interface Validation {
        data object Valid : Validation
        data class DuplicatePageId(val pageId: String) : Validation
        data class DuplicatePageRank(val rank: Int) : Validation
        data class DuplicateItemAcrossPages(
            val itemId: String,
            val firstPageId: String,
            val secondPageId: String,
        ) : Validation
        data class InvalidPage(
            val pageId: String,
            val reason: WorkspaceGridPlacement.Validation,
        ) : Validation
    }

    sealed interface Mutation {
        data class Updated(val pages: List<Page>) : Mutation
        data class InvalidWorkspace(val reason: Validation) : Mutation
        data class TargetRankOutOfRange(val targetRank: Int) : Mutation
        data class PageNotFound(val pageId: String) : Mutation
        data class ItemNotFound(val itemId: String) : Mutation
        data class ItemIdentityMismatch(val expected: String, val actual: String) : Mutation
    }

    fun validate(
        grid: WorkspaceGridPlacement.Grid,
        pages: List<Page>,
    ): Validation {
        val pageIds = mutableSetOf<String>()
        val pageRanks = mutableSetOf<Int>()
        val itemPages = mutableMapOf<String, String>()

        for (page in pages) {
            if (!pageIds.add(page.pageId)) return Validation.DuplicatePageId(page.pageId)
            if (!pageRanks.add(page.rank)) return Validation.DuplicatePageRank(page.rank)

            val pageValidation = WorkspaceGridPlacement.validate(grid, page.placements)
            if (pageValidation != WorkspaceGridPlacement.Validation.Valid) {
                return Validation.InvalidPage(page.pageId, pageValidation)
            }

            for (placement in page.placements) {
                val firstPageId = itemPages.putIfAbsent(placement.itemId, page.pageId)
                if (firstPageId != null) {
                    return Validation.DuplicateItemAcrossPages(
                        itemId = placement.itemId,
                        firstPageId = firstPageId,
                        secondPageId = page.pageId,
                    )
                }
            }
        }

        return Validation.Valid
    }

    fun ordered(pages: List<Page>): List<Page> = pages.sortedBy { it.rank }

    /**
     * Moves a page to a zero-based ordered position and rewrites all page ranks to a contiguous
     * 0..n-1 sequence. Invalid input is never normalized silently; it is rejected first.
     */
    fun movePage(
        grid: WorkspaceGridPlacement.Grid,
        pages: List<Page>,
        pageId: String,
        targetRank: Int,
    ): Mutation {
        val currentValidation = validate(grid, pages)
        if (currentValidation != Validation.Valid) {
            return Mutation.InvalidWorkspace(currentValidation)
        }
        if (targetRank !in pages.indices) {
            return Mutation.TargetRankOutOfRange(targetRank)
        }

        val orderedPages = ordered(pages).toMutableList()
        val sourceIndex = orderedPages.indexOfFirst { it.pageId == pageId }
        if (sourceIndex < 0) return Mutation.PageNotFound(pageId)

        val moved = orderedPages.removeAt(sourceIndex)
        orderedPages.add(targetRank, moved)
        val reranked = orderedPages.mapIndexed { index, page -> page.copy(rank = index) }

        val finalValidation = validate(grid, reranked)
        return if (finalValidation == Validation.Valid) {
            Mutation.Updated(reranked)
        } else {
            Mutation.InvalidWorkspace(finalValidation)
        }
    }

    /**
     * Moves an existing item to a new placement on any existing page. The requested placement
     * must preserve the same item identity. Collision/out-of-bounds/global-uniqueness checks are
     * applied to the full resulting workspace before it can be returned as updated state.
     */
    fun moveItem(
        grid: WorkspaceGridPlacement.Grid,
        pages: List<Page>,
        itemId: String,
        targetPageId: String,
        targetPlacement: WorkspaceGridPlacement.Placement,
    ): Mutation {
        val currentValidation = validate(grid, pages)
        if (currentValidation != Validation.Valid) {
            return Mutation.InvalidWorkspace(currentValidation)
        }
        if (targetPlacement.itemId != itemId) {
            return Mutation.ItemIdentityMismatch(itemId, targetPlacement.itemId)
        }
        if (pages.none { it.pageId == targetPageId }) {
            return Mutation.PageNotFound(targetPageId)
        }
        if (pages.none { page -> page.placements.any { it.itemId == itemId } }) {
            return Mutation.ItemNotFound(itemId)
        }

        val updated = pages.map { page ->
            val withoutItem = page.placements.filterNot { it.itemId == itemId }
            if (page.pageId == targetPageId) {
                page.copy(placements = withoutItem + targetPlacement)
            } else if (withoutItem.size != page.placements.size) {
                page.copy(placements = withoutItem)
            } else {
                page
            }
        }

        val finalValidation = validate(grid, updated)
        return if (finalValidation == Validation.Valid) {
            Mutation.Updated(ordered(updated))
        } else {
            Mutation.InvalidWorkspace(finalValidation)
        }
    }
}
