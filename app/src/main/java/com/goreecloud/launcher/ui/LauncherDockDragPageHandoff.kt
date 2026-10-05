package com.goreecloud.launcher.ui

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.goreecloud.launcher.core.launcher.LauncherDockDragPageDirection
import com.goreecloud.launcher.core.launcher.launcherDockDragPageDirection
import kotlinx.coroutines.delay

@Composable
internal fun LauncherDockDragPageHandoff(
    activeDrag: LauncherAppDragData?,
    dragPoint: Offset?,
    pagerBounds: Rect?,
    pagerState: PagerState,
    previousPageAvailable: Boolean,
    nextPageAvailable: Boolean,
): LauncherDockDragPageDirection? {
    val edgeThresholdPx = with(LocalDensity.current) { 48.dp.toPx() }
    val direction = if (activeDrag != null && dragPoint != null && pagerBounds != null) {
        launcherDockDragPageDirection(
            dragX = dragPoint.x,
            dragY = dragPoint.y,
            surfaceLeftPx = pagerBounds.left,
            surfaceTopPx = pagerBounds.top,
            surfaceRightPx = pagerBounds.right,
            surfaceBottomPx = pagerBounds.bottom,
            edgeThresholdPx = edgeThresholdPx,
            previousPageAvailable = previousPageAvailable,
            nextPageAvailable = nextPageAvailable,
        )
    } else {
        null
    }

    LaunchedEffect(
        activeDrag?.appKey,
        direction,
        pagerState.currentPage,
        pagerState.isScrollInProgress,
        pagerState.pageCount,
    ) {
        if (activeDrag == null || direction == null || pagerState.isScrollInProgress) {
            return@LaunchedEffect
        }

        delay(450)
        val delta = when (direction) {
            LauncherDockDragPageDirection.PREVIOUS -> -1
            LauncherDockDragPageDirection.NEXT -> 1
        }
        val target = (pagerState.currentPage + delta).coerceIn(0, pagerState.pageCount - 1)
        if (target != pagerState.currentPage) {
            pagerState.animateScrollToPage(target)
        }
    }
    return direction
}
