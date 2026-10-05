package com.goreecloud.launcher.core.launcher

internal const val LAUNCHER_DOCK_DEFAULT_PAGE_SIZE = 5
internal const val LAUNCHER_DOCK_MIN_PAGE_SIZE = 4
internal const val LAUNCHER_DOCK_MAX_PAGE_SIZE = 7

internal data class LauncherDockPagePlan(
    val configuredPageSize: Int,
    val effectivePageSize: Int,
    val pageCount: Int,
)

/**
 * Computes Dock paging without ever shrinking an interactive slot below the resolved accessibility
 * floor. A user's preferred page size is therefore a density target, not permission to compress
 * touch targets. Narrow layouts page earlier instead.
 */
internal fun launcherDockPagePlan(
    itemCount: Int,
    configuredPageSize: Int,
    availableAppWidthDp: Float,
    minimumInteractionTargetDp: Float,
): LauncherDockPagePlan {
    val configured = configuredPageSize.coerceIn(
        LAUNCHER_DOCK_MIN_PAGE_SIZE,
        LAUNCHER_DOCK_MAX_PAGE_SIZE,
    )
    val safeByWidth = if (availableAppWidthDp <= 0f || minimumInteractionTargetDp <= 0f) {
        1
    } else {
        (availableAppWidthDp / minimumInteractionTargetDp).toInt().coerceAtLeast(1)
    }
    val effective = configured.coerceAtMost(safeByWidth).coerceAtLeast(1)
    val count = itemCount.coerceAtLeast(0)
    val pages = if (count == 0) 1 else (count + effective - 1) / effective
    return LauncherDockPagePlan(
        configuredPageSize = configured,
        effectivePageSize = effective,
        pageCount = pages,
    )
}

internal fun launcherDockVirtualPageCount(
    logicalPageCount: Int,
    loop: Boolean,
): Int {
    val count = logicalPageCount.coerceAtLeast(1)
    return if (loop && count > 1) count + 2 else count
}

internal fun launcherDockInitialVirtualPage(
    logicalPageCount: Int,
    loop: Boolean,
): Int = if (loop && logicalPageCount > 1) 1 else 0

internal fun launcherDockLogicalPage(
    virtualPage: Int,
    logicalPageCount: Int,
    loop: Boolean,
): Int {
    val count = logicalPageCount.coerceAtLeast(1)
    if (!loop || count == 1) return virtualPage.coerceIn(0, count - 1)
    val virtualCount = count + 2
    return when {
        virtualPage <= 0 -> count - 1
        virtualPage >= virtualCount - 1 -> 0
        else -> virtualPage - 1
    }
}

internal fun launcherDockLoopBoundaryTarget(
    virtualPage: Int,
    logicalPageCount: Int,
    loop: Boolean,
): Int? {
    val count = logicalPageCount.coerceAtLeast(1)
    if (!loop || count <= 1) return null
    return when {
        virtualPage <= 0 -> count
        virtualPage >= count + 1 -> 1
        else -> null
    }
}


internal enum class LauncherDockDragPageDirection {
    PREVIOUS,
    NEXT,
}

internal fun launcherDockDragPageDirection(
    dragX: Float,
    dragY: Float,
    surfaceLeftPx: Float,
    surfaceTopPx: Float,
    surfaceRightPx: Float,
    surfaceBottomPx: Float,
    edgeThresholdPx: Float,
    previousPageAvailable: Boolean,
    nextPageAvailable: Boolean,
): LauncherDockDragPageDirection? {
    if (
        !dragX.isFinite() ||
        !dragY.isFinite() ||
        !surfaceLeftPx.isFinite() ||
        !surfaceTopPx.isFinite() ||
        !surfaceRightPx.isFinite() ||
        !surfaceBottomPx.isFinite() ||
        !edgeThresholdPx.isFinite() ||
        surfaceRightPx <= surfaceLeftPx ||
        surfaceBottomPx <= surfaceTopPx ||
        edgeThresholdPx <= 0f ||
        dragX < surfaceLeftPx ||
        dragX > surfaceRightPx ||
        dragY < surfaceTopPx ||
        dragY > surfaceBottomPx
    ) {
        return null
    }

    val safeThreshold = edgeThresholdPx.coerceAtMost(
        (surfaceRightPx - surfaceLeftPx) / 2f,
    )
    return when {
        previousPageAvailable && dragX <= surfaceLeftPx + safeThreshold ->
            LauncherDockDragPageDirection.PREVIOUS
        nextPageAvailable && dragX >= surfaceRightPx - safeThreshold ->
            LauncherDockDragPageDirection.NEXT
        else -> null
    }
}

internal fun launcherDockNextPageInsertionKey(
    pageKeys: List<List<String>>,
    logicalCurrentPage: Int,
    sourceKey: String?,
    loop: Boolean = false,
): String? {
    if (pageKeys.isEmpty()) return null
    val current = logicalCurrentPage.coerceIn(0, pageKeys.lastIndex)
    val nextIndex = when {
        current < pageKeys.lastIndex -> current + 1
        loop && pageKeys.size > 1 -> 0
        else -> return null
    }
    return pageKeys[nextIndex].firstOrNull { key -> key != sourceKey }
}
