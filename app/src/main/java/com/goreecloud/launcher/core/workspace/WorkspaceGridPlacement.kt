package com.goreecloud.launcher.core.workspace

/**
 * Pure placement contract for future multi-page launcher grids.
 *
 * The model is deliberately Android-framework independent so placement validity can be
 * enforced consistently before Room persistence, drag/drop rendering, or widget binding.
 */
object WorkspaceGridPlacement {
    data class Grid(val columns: Int, val rows: Int) {
        init {
            require(columns > 0) { "columns must be positive" }
            require(rows > 0) { "rows must be positive" }
        }
    }

    data class Placement(
        val itemId: String,
        val cellX: Int,
        val cellY: Int,
        val spanX: Int = 1,
        val spanY: Int = 1,
    ) {
        init {
            require(itemId.isNotBlank()) { "itemId must not be blank" }
            require(cellX >= 0 && cellY >= 0) { "cell coordinates must be non-negative" }
            require(spanX > 0 && spanY > 0) { "spans must be positive" }
        }
    }

    sealed interface Validation {
        data object Valid : Validation
        data class OutOfBounds(val itemId: String) : Validation
        data class Collision(val firstItemId: String, val secondItemId: String) : Validation
        data class DuplicateItem(val itemId: String) : Validation
    }

    fun validate(grid: Grid, placements: List<Placement>): Validation {
        val seen = mutableSetOf<String>()
        for (placement in placements) {
            if (!seen.add(placement.itemId)) return Validation.DuplicateItem(placement.itemId)
            if (placement.cellX + placement.spanX > grid.columns ||
                placement.cellY + placement.spanY > grid.rows
            ) {
                return Validation.OutOfBounds(placement.itemId)
            }
        }

        placements.forEachIndexed { index, first ->
            for (second in placements.drop(index + 1)) {
                if (overlaps(first, second)) {
                    return Validation.Collision(first.itemId, second.itemId)
                }
            }
        }
        return Validation.Valid
    }

    private fun overlaps(first: Placement, second: Placement): Boolean =
        first.cellX < second.cellX + second.spanX &&
            first.cellX + first.spanX > second.cellX &&
            first.cellY < second.cellY + second.spanY &&
            first.cellY + first.spanY > second.cellY
}
