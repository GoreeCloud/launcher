package com.goreecloud.launcher.ui

import com.goreecloud.launcher.core.launcher.LauncherDrawerSpacing
import java.text.Normalizer
import java.util.Locale

/**
 * Treat apps and folders as peers in the app drawer. Sorting must remain stable regardless
 * of a provider's item-list order or the work profile's separate application enumeration.
 * Canonically equivalent Unicode labels must sort together, like the installed-app inventory.
 * Only presentation order changes: folder membership and persisted Home positions are untouched.
 */
internal enum class LauncherDrawerSortOrder(val displayName: String) {
    ALPHABETICAL("A–Z"),
    REVERSE_ALPHABETICAL("Z–A"),
    MOST_RECENT("Most recent"),
    MOST_FREQUENT("Most frequent"),
    PINNED_FIRST("Pinned first"),
}

internal object LauncherDrawerSortingPolicy {
    fun <T> order(
        entries: List<T>,
        label: (T) -> String,
        key: (T) -> String,
        sortOrder: LauncherDrawerSortOrder = LauncherDrawerSortOrder.ALPHABETICAL,
        recentRank: (T) -> Int? = { null },
        frequency: (T) -> Long? = { null },
        pinned: (T) -> Boolean = { false },
    ): List<T> = entries.sortedWith { left, right ->
        val leftLabel = normalizedLabel(label(left))
        val rightLabel = normalizedLabel(label(right))
        val labelOrder = leftLabel.compareTo(rightLabel)
        val keyOrder = key(left).compareTo(key(right))

        when (sortOrder) {
            LauncherDrawerSortOrder.ALPHABETICAL ->
                labelOrder.takeIf { it != 0 } ?: keyOrder
            LauncherDrawerSortOrder.REVERSE_ALPHABETICAL ->
                (-labelOrder).takeIf { it != 0 } ?: keyOrder
            LauncherDrawerSortOrder.MOST_RECENT -> {
                val leftRank = recentRank(left)
                val rightRank = recentRank(right)
                when {
                    leftRank != null && rightRank != null && leftRank != rightRank ->
                        leftRank.compareTo(rightRank)
                    leftRank != null && rightRank == null -> -1
                    leftRank == null && rightRank != null -> 1
                    labelOrder != 0 -> labelOrder
                    else -> keyOrder
                }
            }
            LauncherDrawerSortOrder.MOST_FREQUENT -> {
                val leftFrequency = frequency(left) ?: 0L
                val rightFrequency = frequency(right) ?: 0L
                when {
                    leftFrequency != rightFrequency ->
                        rightFrequency.compareTo(leftFrequency)
                    labelOrder != 0 -> labelOrder
                    else -> keyOrder
                }
            }
            LauncherDrawerSortOrder.PINNED_FIRST -> {
                val leftPinned = pinned(left)
                val rightPinned = pinned(right)
                when {
                    leftPinned != rightPinned -> if (leftPinned) -1 else 1
                    labelOrder != 0 -> labelOrder
                    else -> keyOrder
                }
            }
        }
    }

    private fun normalizedLabel(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFC).lowercase(Locale.ROOT)
}


/** Fixed cell geometry for a uniform app-drawer grid. */
internal data class LauncherDrawerGridGeometry(
    val tileHeightDp: Int,
    val iconSlotHeightDp: Int,
    val labelSlotHeightDp: Int,
)

internal object LauncherDrawerGridPolicy {
    const val ICON_SLOT_HEIGHT_DP = 60
    const val GRID_LABEL_SLOT_HEIGHT_DP = 34
    const val COMPACT_LABEL_SLOT_HEIGHT_DP = 20

    fun geometry(
        compact: Boolean,
        spacing: LauncherDrawerSpacing,
    ): LauncherDrawerGridGeometry {
        val tileHeight = if (compact) {
            when (spacing) {
                LauncherDrawerSpacing.TIGHT -> 88
                LauncherDrawerSpacing.STANDARD -> 94
                LauncherDrawerSpacing.RELAXED -> 100
            }
        } else {
            when (spacing) {
                LauncherDrawerSpacing.TIGHT -> 104
                LauncherDrawerSpacing.STANDARD -> 110
                LauncherDrawerSpacing.RELAXED -> 116
            }
        }
        return LauncherDrawerGridGeometry(
            tileHeightDp = tileHeight,
            iconSlotHeightDp = ICON_SLOT_HEIGHT_DP,
            labelSlotHeightDp = if (compact) COMPACT_LABEL_SLOT_HEIGHT_DP else GRID_LABEL_SLOT_HEIGHT_DP,
        )
    }
}


/**
 * Drawer page indicators keep a restrained visual dot while preserving the Glaze interaction
 * floor. The visual size is never used as the touch target.
 */
internal object LauncherDrawerPageIndicatorPolicy {
    const val TOUCH_TARGET_DP = 48
    const val SELECTED_VISUAL_DP = 8
    const val IDLE_VISUAL_DP = 6

    fun visualSizeDp(selected: Boolean): Int =
        if (selected) SELECTED_VISUAL_DP else IDLE_VISUAL_DP
}
