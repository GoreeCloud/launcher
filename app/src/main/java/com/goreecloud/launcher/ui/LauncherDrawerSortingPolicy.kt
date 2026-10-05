package com.goreecloud.launcher.ui

import com.goreecloud.launcher.core.launcher.LauncherDrawerLayoutMode
import com.goreecloud.launcher.core.launcher.LauncherDrawerSpacing
import java.text.Normalizer
import java.util.Locale

internal enum class LauncherDrawerSortOrder(val displayName: String) {
    ALPHABETICAL("A–Z"),
    REVERSE_ALPHABETICAL("Z–A"),
    MOST_RECENT("Most recent"),
    RECENTLY_INSTALLED("Recently installed"),
    MOST_FREQUENT("Most frequent"),
    PINNED_FIRST("Pinned first"),
}

internal fun nextLauncherDrawerLayoutMode(
    current: LauncherDrawerLayoutMode,
): LauncherDrawerLayoutMode = when (current) {
    LauncherDrawerLayoutMode.GRID -> LauncherDrawerLayoutMode.COMPACT
    LauncherDrawerLayoutMode.COMPACT -> LauncherDrawerLayoutMode.LIST
    LauncherDrawerLayoutMode.LIST -> LauncherDrawerLayoutMode.CATEGORY
    LauncherDrawerLayoutMode.CATEGORY -> LauncherDrawerLayoutMode.GRID
}

internal fun launcherDrawerAlphabetBucket(label: String): String {
    val decomposed = Normalizer.normalize(label.trim(), Normalizer.Form.NFD)
    val base = decomposed.firstOrNull { character ->
        Character.getType(character) !in setOf(
            Character.NON_SPACING_MARK.toInt(),
            Character.COMBINING_SPACING_MARK.toInt(),
            Character.ENCLOSING_MARK.toInt(),
        )
    } ?: return "#"
    return if (base.isLetter()) {
        base.uppercaseChar().toString()
    } else {
        "#"
    }
}

internal fun <T> launcherDrawerAlphabetTargets(
    entries: List<T>,
    label: (T) -> String,
): List<Pair<String, Int>> {
    val firstIndexByBucket = linkedMapOf<String, Int>()
    entries.forEachIndexed { index, entry ->
        val bucket = launcherDrawerAlphabetBucket(label(entry))
        firstIndexByBucket.putIfAbsent(bucket, index)
    }
    return firstIndexByBucket.entries.map { (bucket, index) -> bucket to index }
}

internal object LauncherDrawerSortingPolicy {
    fun <T> order(
        entries: List<T>,
        label: (T) -> String,
        key: (T) -> String,
        sortOrder: LauncherDrawerSortOrder = LauncherDrawerSortOrder.ALPHABETICAL,
        recentRank: (T) -> Int? = { null },
        installTimeMillis: (T) -> Long? = { null },
        frequency: (T) -> Long? = { null },
        pinned: (T) -> Boolean = { false },
        pinnedRank: (T) -> Int? = { null },
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
            LauncherDrawerSortOrder.RECENTLY_INSTALLED -> {
                val leftInstallTime = installTimeMillis(left)
                val rightInstallTime = installTimeMillis(right)
                when {
                    leftInstallTime != null &&
                        rightInstallTime != null &&
                        leftInstallTime != rightInstallTime ->
                        rightInstallTime.compareTo(leftInstallTime)
                    leftInstallTime != null && rightInstallTime == null -> -1
                    leftInstallTime == null && rightInstallTime != null -> 1
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
                val leftRank = pinnedRank(left)
                val rightRank = pinnedRank(right)
                when {
                    leftPinned != rightPinned -> if (leftPinned) -1 else 1
                    leftPinned && rightPinned &&
                        leftRank != null &&
                        rightRank != null &&
                        leftRank != rightRank -> leftRank.compareTo(rightRank)
                    leftPinned && rightPinned && leftRank != null && rightRank == null -> -1
                    leftPinned && rightPinned && leftRank == null && rightRank != null -> 1
                    labelOrder != 0 -> labelOrder
                    else -> keyOrder
                }
            }
        }
    }

    private fun normalizedLabel(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFC).lowercase(Locale.ROOT)
}

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
            labelSlotHeightDp =
                if (compact) COMPACT_LABEL_SLOT_HEIGHT_DP else GRID_LABEL_SLOT_HEIGHT_DP,
        )
    }
}

internal object LauncherDrawerPageIndicatorPolicy {
    const val TOUCH_TARGET_DP = 48
    const val SELECTED_VISUAL_DP = 8
    const val IDLE_VISUAL_DP = 6

    fun visualSizeDp(selected: Boolean): Int =
        if (selected) SELECTED_VISUAL_DP else IDLE_VISUAL_DP
}
