package com.goreecloud.launcher.ui

import android.appwidget.AppWidgetHostView
import android.content.pm.LauncherActivityInfo
import android.os.Process
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goreecloud.launcher.core.launcher.LauncherHomeLabelPolicy
import com.goreecloud.launcher.core.launcher.LauncherDockStyle
import com.goreecloud.launcher.core.launcher.LauncherHomePageTransition
import com.goreecloud.launcher.core.launcher.LauncherFolder
import com.goreecloud.launcher.core.workspace.WorkspaceMoveDirection
import com.goreecloud.launcher.core.workspace.db.WorkspaceHomeSpatialDirection
import com.goreecloud.launcher.core.workspace.db.WorkspaceLegacyImportMapper
import com.goreecloud.launcher.core.workspace.db.WorkspaceRenderedHomePage
import com.goreecloud.launcher.core.workspace.db.WorkspaceRenderedHomeWidget
import com.goreecloud.launcher.core.workspace.db.context
import com.goreecloud.launcher.core.workspace.workspaceKey
import com.goreecloud.launcher.ui.theme.GlazeMetrics
import kotlin.math.abs
import kotlinx.coroutines.withTimeoutOrNull

internal fun secondaryHomeShouldRenderPageIndicator(
    contentOnly: Boolean,
    requested: Boolean,
    pageCount: Int,
): Boolean = !contentOnly && requested && pageCount > 1

internal fun secondaryHomeShouldRenderDock(
    contentOnly: Boolean,
    dockAppCount: Int,
): Boolean = !contentOnly && dockAppCount > 0

internal fun homePageSwipeTargetIndex(
    currentIndex: Int,
    pageCount: Int,
    horizontalDistancePx: Float,
    verticalDistancePx: Float,
    minimumDistancePx: Float,
): Int? {
    if (
        pageCount <= 1 ||
        currentIndex !in 0 until pageCount ||
        abs(horizontalDistancePx) < minimumDistancePx ||
        abs(horizontalDistancePx) <= abs(verticalDistancePx) * 1.20f
    ) {
        return null
    }

    val target = if (horizontalDistancePx < 0f) {
        currentIndex + 1
    } else {
        currentIndex - 1
    }
    return target.takeIf { it in 0 until pageCount }
}

internal fun Modifier.homePageSwipeNavigation(
    enabled: Boolean,
    currentIndex: Int,
    pageCount: Int,
    onPageSelected: (Int) -> Unit,
): Modifier {
    if (!enabled || pageCount <= 1) return this

    return pointerInput(enabled, currentIndex, pageCount) {
        val minimumDistancePx = 44.dp.toPx()

        awaitEachGesture {
            val down = awaitFirstDown(
                requireUnconsumed = false,
                pass = PointerEventPass.Final,
            )
            var horizontalDistance = 0f
            var verticalDistance = 0f
            var pageSelected = false

            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Final)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                val delta = change.positionChange()
                horizontalDistance += delta.x
                verticalDistance += delta.y

                if (!pageSelected) {
                    homePageSwipeTargetIndex(
                        currentIndex = currentIndex,
                        pageCount = pageCount,
                        horizontalDistancePx = horizontalDistance,
                        verticalDistancePx = verticalDistance,
                        minimumDistancePx = minimumDistancePx,
                    )?.let { target ->
                        pageSelected = true
                        onPageSelected(target)
                    }
                }
                if (!change.pressed) break
            }
        }
    }
}

internal fun Modifier.homeVerticalGestureNavigation(
    enabled: Boolean,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
): Modifier {
    if (!enabled) return this
    return pointerInput(enabled, onSwipeUp, onSwipeDown) {
        val threshold = 42.dp.toPx()
        var distance = 0f
        var triggered = false
        detectVerticalDragGestures(
            onDragStart = {
                distance = 0f
                triggered = false
            },
            onDragCancel = {
                distance = 0f
                triggered = false
            },
            onDragEnd = {
                distance = 0f
                triggered = false
            },
            onVerticalDrag = { change, amount ->
                if (!triggered) {
                    distance += amount
                    when {
                        distance <= -threshold -> {
                            triggered = true
                            change.consume()
                            onSwipeUp()
                        }
                        distance >= threshold -> {
                            triggered = true
                            change.consume()
                            onSwipeDown()
                        }
                    }
                }
            },
        )
    }
}

internal fun Modifier.homeEditLongPress(
    enabled: Boolean,
    canStartAt: (Offset) -> Boolean = { true },
    onLongPress: () -> Unit,
): Modifier {
    if (!enabled) return this

    return pointerInput(enabled, canStartAt, onLongPress) {
        val timeoutMillis = viewConfiguration.longPressTimeoutMillis
        val touchSlop = viewConfiguration.touchSlop
        awaitEachGesture {
            val down = awaitFirstDown(
                requireUnconsumed = false,
                pass = PointerEventPass.Initial,
            )
            val start = down.position
            if (!canStartAt(start)) return@awaitEachGesture
            val interrupted = withTimeoutOrNull(timeoutMillis) {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id }
                        ?: return@withTimeoutOrNull true
                    if (!change.pressed) {
                        return@withTimeoutOrNull true
                    }
                    val delta = change.position - start
                    if (abs(delta.x) > touchSlop || abs(delta.y) > touchSlop) {
                        return@withTimeoutOrNull true
                    }
                }
            }
            if (interrupted == null) {
                onLongPress()
            }
        }
    }
}

internal fun canDeleteHomePage(
    page: WorkspaceRenderedHomePage,
    pages: List<WorkspaceRenderedHomePage>,
    layoutLocked: Boolean,
): Boolean =
    !layoutLocked &&
        pages.size > 1 &&
        pages.firstOrNull()?.pageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID &&
        page.pageId != WorkspaceLegacyImportMapper.HOME_PAGE_ID &&
        page.appKeys.isEmpty() &&
        page.widgetPlacements.isEmpty() &&
        page.folderPlacements.isEmpty() &&
        page.unsupportedItemCount == 0

internal fun homePageVisibleItemCount(page: WorkspaceRenderedHomePage): Int =
    page.appKeys.size +
        page.widgetPlacements.size +
        page.folderPlacements.size +
        page.unsupportedItemCount

internal fun homeMoveTargetPages(
    pages: List<WorkspaceRenderedHomePage>,
    currentPageId: String,
): List<WorkspaceRenderedHomePage> =
    pages.filterNot { it.pageId == currentPageId }

internal fun homePageSpatialSlotIndex(
    cellX: Int?,
    cellY: Int?,
    columns: Int,
    rows: Int,
): Int? {
    if (
        cellX == null ||
        cellY == null ||
        columns <= 0 ||
        rows <= 0 ||
        cellX !in 0 until columns ||
        cellY !in 0 until rows
    ) {
        return null
    }
    return cellY * columns + cellX
}

internal data class HomePageEdgeDropTarget(
    val pageId: String,
    val cellX: Int,
    val cellY: Int,
)

internal fun homePageEdgeDropTargetPageId(
    pages: List<WorkspaceRenderedHomePage>,
    currentPageId: String,
    dropX: Float,
    surfaceLeftPx: Float,
    surfaceRightPx: Float,
    edgeThresholdPx: Float,
): String? {
    val surfaceWidth = surfaceRightPx - surfaceLeftPx
    if (
        pages.size <= 1 ||
        pages.firstOrNull()?.pageId != WorkspaceLegacyImportMapper.HOME_PAGE_ID ||
        pages.map { it.rank } != pages.indices.toList() ||
        !dropX.isFinite() ||
        !surfaceLeftPx.isFinite() ||
        !surfaceRightPx.isFinite() ||
        !edgeThresholdPx.isFinite() ||
        surfaceWidth <= 0f ||
        edgeThresholdPx <= 0f ||
        edgeThresholdPx >= surfaceWidth / 2f
    ) {
        return null
    }

    val currentIndex = pages.indexOfFirst { it.pageId == currentPageId }
    if (currentIndex < 0) return null

    val targetIndex = when {
        dropX <= surfaceLeftPx + edgeThresholdPx -> currentIndex - 1
        dropX >= surfaceRightPx - edgeThresholdPx -> currentIndex + 1
        else -> return null
    }
    return pages.getOrNull(targetIndex)?.pageId
}

internal fun homePageCellAtPoint(
    point: Offset,
    surfaceBounds: Rect,
    columns: Int,
    rows: Int,
    horizontalSpacingPx: Float,
    verticalSpacingPx: Float,
    tileHeightPx: Float,
): Pair<Int, Int>? {
    if (
        columns <= 0 ||
        rows <= 0 ||
        !point.x.isFinite() ||
        !point.y.isFinite() ||
        !surfaceBounds.left.isFinite() ||
        !surfaceBounds.top.isFinite() ||
        !surfaceBounds.right.isFinite() ||
        !surfaceBounds.bottom.isFinite() ||
        surfaceBounds.width <= 0f ||
        surfaceBounds.height <= 0f ||
        horizontalSpacingPx < 0f ||
        verticalSpacingPx < 0f ||
        tileHeightPx <= 0f
    ) {
        return null
    }

    val usableWidth = surfaceBounds.width - horizontalSpacingPx * (columns - 1)
    val cellWidth = usableWidth / columns
    if (cellWidth <= 0f) return null
    val stepX = cellWidth + horizontalSpacingPx
    val stepY = tileHeightPx + verticalSpacingPx

    repeat(rows) { cellY ->
        repeat(columns) { cellX ->
            val left = surfaceBounds.left + stepX * cellX
            val top = surfaceBounds.top + stepY * cellY
            val cellBounds = Rect(
                left = left,
                top = top,
                right = left + cellWidth,
                bottom = top + tileHeightPx,
            )
            if (cellBounds.contains(point)) {
                return cellX to cellY
            }
        }
    }
    return null
}

internal fun homePageEdgeDropTarget(
    pages: List<WorkspaceRenderedHomePage>,
    currentPageId: String,
    dropX: Float,
    dropY: Float,
    surfaceLeftPx: Float,
    surfaceTopPx: Float,
    surfaceRightPx: Float,
    surfaceBottomPx: Float,
    edgeThresholdPx: Float,
    columns: Int,
    rows: Int,
    spanX: Int = 1,
    spanY: Int = 1,
): HomePageEdgeDropTarget? {
    if (
        columns <= 0 ||
        rows <= 0 ||
        spanX <= 0 ||
        spanY <= 0 ||
        spanX > columns ||
        spanY > rows ||
        !dropY.isFinite() ||
        !surfaceTopPx.isFinite() ||
        !surfaceBottomPx.isFinite() ||
        surfaceBottomPx <= surfaceTopPx ||
        dropY < surfaceTopPx ||
        dropY >= surfaceBottomPx
    ) {
        return null
    }
    val pageId = homePageEdgeDropTargetPageId(
        pages = pages,
        currentPageId = currentPageId,
        dropX = dropX,
        surfaceLeftPx = surfaceLeftPx,
        surfaceRightPx = surfaceRightPx,
        edgeThresholdPx = edgeThresholdPx,
    ) ?: return null
    val currentIndex = pages.indexOfFirst { it.pageId == currentPageId }
    val targetIndex = pages.indexOfFirst { it.pageId == pageId }
    if (currentIndex < 0 || targetIndex < 0 || currentIndex == targetIndex) return null

    val normalizedY = ((dropY - surfaceTopPx) / (surfaceBottomPx - surfaceTopPx))
        .coerceIn(0f, 0.999999f)
    val targetRow = (normalizedY * rows).toInt().coerceIn(0, rows - spanY)
    val targetColumn = if (targetIndex < currentIndex) columns - spanX else 0
    return HomePageEdgeDropTarget(
        pageId = pageId,
        cellX = targetColumn,
        cellY = targetRow,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePageManagerSheet(
    pages: List<WorkspaceRenderedHomePage>,
    selectedPageId: String,
    homeColumns: Int,
    homeRows: Int,
    onSelectPage: (String) -> Unit,
    onMovePage: (String, Int) -> Unit,
    onCreatePage: () -> Unit,
    onDeletePage: (String) -> Unit,
    onDismiss: () -> Unit,
    layoutLocked: Boolean = false,
) {
    if (pages.isEmpty()) return

    var pendingDeletePageId by remember(pages) { mutableStateOf<String?>(null) }
    val pendingDelete = pages.firstOrNull { it.pageId == pendingDeletePageId }
    val selectedIndex = pages.indexOfFirst { it.pageId == selectedPageId }.coerceAtLeast(0)
    val pagerState = rememberPagerState(
        initialPage = selectedIndex,
        pageCount = { pages.size },
    )

    LaunchedEffect(selectedPageId, pages.size) {
        val target = pages.indexOfFirst { it.pageId == selectedPageId }
        if (target >= 0 && target != pagerState.currentPage) {
            pagerState.animateScrollToPage(target)
        }
    }
    LaunchedEffect(pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            pages.getOrNull(pagerState.currentPage)?.let { page ->
                if (page.pageId != selectedPageId) onSelectPage(page.pageId)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = 0.dp,
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(top = GlazeMetrics.space2)
                    .width(42.dp)
                    .height(4.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f),
            ) {}
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    start = GlazeMetrics.space3,
                    end = GlazeMetrics.space3,
                    bottom = GlazeMetrics.space4,
                ),
            verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Home pages",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        pages.size.toString() +
                            if (pages.size == 1) " page · Swipe or tap the preview"
                            else " pages · Swipe between real page previews",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = onCreatePage,
                        enabled = !layoutLocked,
                        modifier = Modifier.testTag("launcher-home-page-add"),
                    ) {
                        Text("+ Add")
                    }
                    TextButton(onClick = onDismiss) { Text("Done") }
                }
            }

            if (layoutLocked) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.54f),
                ) {
                    Text(
                        "Home layout is locked. Page switching remains available; unlock the layout to add, reorder, or delete pages.",
                        modifier = Modifier.padding(GlazeMetrics.space3),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(490.dp)
                    .testTag("launcher-home-page-carousel"),
                contentPadding = PaddingValues(horizontal = 42.dp),
                pageSpacing = GlazeMetrics.space3,
                userScrollEnabled = pages.size > 1,
            ) { index ->
                val page = pages[index]
                val primaryRankHealthy =
                    pages.firstOrNull()?.pageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    HomePageManagerCard(
                        page = page,
                        pageNumber = index + 1,
                        pageCount = pages.size,
                        selected = page.pageId == selectedPageId,
                        primaryRankHealthy = primaryRankHealthy,
                        homeColumns = homeColumns,
                        homeRows = homeRows,
                        layoutLocked = layoutLocked,
                        canDelete = canDeleteHomePage(page, pages, layoutLocked),
                        onSelect = { onSelectPage(page.pageId) },
                        onMoveEarlier = {
                            onMovePage(page.pageId, index - 1)
                        },
                        onMoveLater = {
                            onMovePage(page.pageId, index + 1)
                        },
                        onDelete = {
                            pendingDeletePageId = page.pageId
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Text(
                "The centered preview is the active page. Adjacent page edges stay visible so horizontal navigation remains discoverable.",
                modifier = Modifier.padding(horizontal = GlazeMetrics.space2),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDeletePageId = null },
            shape = RoundedCornerShape(GlazeMetrics.radiusExtraLarge),
            title = { Text("Delete empty Home page?") },
            text = {
                Text(
                    "This page is empty, so deleting it will not remove apps, widgets, or folders. The primary Home page is always protected.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val pageId = pendingDelete.pageId
                        pendingDeletePageId = null
                        onDeletePage(pageId)
                    },
                ) {
                    Text(
                        "Delete page",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeletePageId = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun HomePageManagerCard(
    page: WorkspaceRenderedHomePage,
    pageNumber: Int,
    pageCount: Int,
    selected: Boolean,
    primaryRankHealthy: Boolean,
    homeColumns: Int,
    homeRows: Int,
    layoutLocked: Boolean,
    canDelete: Boolean,
    onSelect: () -> Unit,
    onMoveEarlier: () -> Unit,
    onMoveLater: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = page.pageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID
    val canMoveEarlier = !layoutLocked && primaryRankHealthy && !primary && pageNumber > 2
    val canMoveLater = !layoutLocked && primaryRankHealthy && !primary && pageNumber < pageCount
    val itemCount = homePageVisibleItemCount(page)

    Surface(
        modifier = modifier
            .testTag("launcher-home-page-card-" + page.pageId),
        onClick = onSelect,
        shape = RoundedCornerShape(GlazeMetrics.radiusExtraLarge),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.44f)
        },
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(GlazeMetrics.space3),
            verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "Page " + pageNumber,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        if (selected) "Current page" else itemCount.toString() +
                            if (itemCount == 1) " item" else " items",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (primary) {
                    Surface(
                        shape = RoundedCornerShape(GlazeMetrics.radiusPill),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    ) {
                        Text(
                            "Home",
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            HomePageMiniPreview(
                page = page,
                homeColumns = homeColumns,
                homeRows = homeRows,
            )

            if (primary) {
                Text(
                    "Primary Home · protected",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space1),
                ) {
                    OutlinedButton(
                        onClick = onMoveEarlier,
                        enabled = canMoveEarlier,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 6.dp),
                    ) { Text("Earlier", style = MaterialTheme.typography.labelSmall) }
                    OutlinedButton(
                        onClick = onMoveLater,
                        enabled = canMoveLater,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 6.dp),
                    ) { Text("Later", style = MaterialTheme.typography.labelSmall) }
                }

                if (canDelete) {
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "Delete empty page",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                } else if (!layoutLocked && itemCount > 0) {
                    Text(
                        "Move its items before deleting.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
internal fun HomePageMiniPreview(
    page: WorkspaceRenderedHomePage,
    homeColumns: Int,
    homeRows: Int,
    modifier: Modifier = Modifier,
    preservePhoneAspectRatio: Boolean = true,
) {
    val columns = homeColumns.coerceIn(4, 6)
    val rows = homeRows.coerceIn(4, 7)
    val appColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f)
    val widgetColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    val folderColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.62f)
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (preservePhoneAspectRatio) Modifier.aspectRatio(0.72f) else Modifier,
            ),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.56f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f),
        ),
    ) {
        Box(Modifier.fillMaxSize()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
            ) {
                val cellWidth = size.width / columns
                val cellHeight = size.height / rows

                for (column in 1 until columns) {
                    val x = cellWidth * column
                    drawLine(
                        color = gridColor,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1f,
                    )
                }
                for (row in 1 until rows) {
                    val y = cellHeight * row
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f,
                    )
                }

                page.widgetPlacements.forEach { widget ->
                    val left = widget.cellX.coerceIn(0, columns - 1) * cellWidth + 3f
                    val top = widget.cellY.coerceIn(0, rows - 1) * cellHeight + 3f
                    val width = widget.spanX.coerceAtLeast(1).coerceAtMost(columns) * cellWidth - 6f
                    val height = widget.spanY.coerceAtLeast(1).coerceAtMost(rows) * cellHeight - 6f
                    drawRoundRect(
                        color = widgetColor,
                        topLeft = Offset(left, top),
                        size = Size(width.coerceAtLeast(6f), height.coerceAtLeast(6f)),
                        cornerRadius = CornerRadius(10f, 10f),
                    )
                }

                page.folderPlacements.forEach { folder ->
                    val centerX = (folder.cellX.coerceIn(0, columns - 1) + 0.5f) * cellWidth
                    val centerY = (folder.cellY.coerceIn(0, rows - 1) + 0.5f) * cellHeight
                    val edge = minOf(cellWidth, cellHeight) * 0.50f
                    drawRoundRect(
                        color = folderColor,
                        topLeft = Offset(centerX - edge / 2f, centerY - edge / 2f),
                        size = Size(edge, edge),
                        cornerRadius = CornerRadius(edge * 0.25f, edge * 0.25f),
                    )
                }

                val positionedApps = page.appPlacements.filter {
                    it.cellX != null && it.cellY != null
                }
                positionedApps.forEach { app ->
                    val centerX = (checkNotNull(app.cellX).coerceIn(0, columns - 1) + 0.5f) * cellWidth
                    val centerY = (checkNotNull(app.cellY).coerceIn(0, rows - 1) + 0.5f) * cellHeight
                    drawCircle(
                        color = appColor,
                        radius = minOf(cellWidth, cellHeight) * 0.19f,
                        center = Offset(centerX, centerY),
                    )
                }

                if (positionedApps.isEmpty() && page.appKeys.isNotEmpty()) {
                    page.appKeys.take(columns * rows).forEachIndexed { index, _ ->
                        val column = index % columns
                        val row = index / columns
                        val centerX = (column + 0.5f) * cellWidth
                        val centerY = (row + 0.5f) * cellHeight
                        drawCircle(
                            color = appColor,
                            radius = minOf(cellWidth, cellHeight) * 0.19f,
                            center = Offset(centerX, centerY),
                        )
                    }
                }
            }

        }
    }
}

@Composable
fun HomePageSwitcher(
    pages: List<WorkspaceRenderedHomePage>,
    selectedPageId: String,
    onSelectPage: (String) -> Unit,
    onMovePage: (String, Int) -> Unit,
    onCreatePage: () -> Unit,
    onDeletePage: (String) -> Unit,
    layoutLocked: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (pages.isEmpty()) return

    val selectedIndex = pages.indexOfFirst { it.pageId == selectedPageId }
    val selectedPage = pages.getOrNull(selectedIndex)
    val primaryRankHealthy = pages.firstOrNull()?.pageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID
    val pageListState = rememberLazyListState()
    var pageMenuExpanded by remember(selectedPageId, layoutLocked) { mutableStateOf(false) }

    val canDelete = selectedPage?.let { page ->
        canDeleteHomePage(page, pages, layoutLocked)
    } ?: false
    val canMoveEarlier = !layoutLocked && primaryRankHealthy &&
        selectedPage != null &&
        selectedPage.pageId != WorkspaceLegacyImportMapper.HOME_PAGE_ID &&
        selectedIndex > 1
    val canMoveLater = !layoutLocked && primaryRankHealthy &&
        selectedPage != null &&
        selectedPage.pageId != WorkspaceLegacyImportMapper.HOME_PAGE_ID &&
        selectedIndex in 1 until pages.lastIndex

    LaunchedEffect(selectedPageId, selectedIndex, pages.size) {
        if (selectedIndex >= 0) pageListState.animateScrollToItem(selectedIndex)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(GlazeMetrics.radiusControl),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = GlazeMetrics.space2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space1),
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                state = pageListState,
                horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space1),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                itemsIndexed(pages, key = { _, page -> page.pageId }) { index, page ->
                    val selected = page.pageId == selectedPageId
                    val pageContext = page.context()
                    Surface(
                        modifier = Modifier
                            .semantics(mergeDescendants = true) {
                                contentDescription = pageContext.accessibilityLabel(index + 1, selected)
                            }
                            .clickable { onSelectPage(page.pageId) },
                        shape = RoundedCornerShape(GlazeMetrics.radiusControl),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                        },
                    ) {
                        Text(
                            text = if (selected) "Page ${index + 1}" else "${index + 1}",
                            modifier = Modifier.padding(
                                horizontal = GlazeMetrics.space3,
                                vertical = GlazeMetrics.space2,
                            ),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }

            TextButton(
                onClick = onCreatePage,
                enabled = !layoutLocked,
            ) { Text("Add") }

            if (layoutLocked) {
                Text(
                    "Locked",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (canMoveEarlier || canMoveLater || canDelete) {
                Box {
                    TextButton(onClick = { pageMenuExpanded = true }) { Text("More") }
                    DropdownMenu(
                        expanded = pageMenuExpanded,
                        onDismissRequest = { pageMenuExpanded = false },
                    ) {
                        if (canMoveEarlier) {
                            DropdownMenuItem(
                                text = { Text("Move page earlier") },
                                onClick = {
                                    pageMenuExpanded = false
                                    onMovePage(selectedPageId, selectedIndex - 1)
                                },
                            )
                        }
                        if (canMoveLater) {
                            DropdownMenuItem(
                                text = { Text("Move page later") },
                                onClick = {
                                    pageMenuExpanded = false
                                    onMovePage(selectedPageId, selectedIndex + 1)
                                },
                            )
                        }
                        if (canDelete) {
                            DropdownMenuItem(
                                text = { Text("Delete empty page") },
                                onClick = {
                                    pageMenuExpanded = false
                                    onDeletePage(selectedPageId)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
@Suppress("UNUSED_PARAMETER")
fun HomePageDots(
    pages: List<WorkspaceRenderedHomePage>,
    selectedPageId: String,
    onSelectPage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (pages.size <= 1) return

    val selectedIndex = pages.indexOfFirst { it.pageId == selectedPageId }
        .takeIf { it >= 0 }
        ?: 0
    val selectedPage = pages[selectedIndex]

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .testTag("launcher-home-page-indicator")
                .semantics {
                    contentDescription = selectedPage.context()
                        .accessibilityLabel(selectedIndex + 1, selected = true)
                },
            shape = RoundedCornerShape(GlazeMetrics.radiusPill),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.34f),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                pages.forEach { page ->
                    val selected = page.pageId == selectedPageId
                    Surface(
                        modifier = Modifier
                            .width(if (selected) 14.dp else 5.dp)
                            .height(5.dp),
                        shape = RoundedCornerShape(GlazeMetrics.radiusPill),
                        color = if (selected) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.90f)
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f)
                        },
                    ) {}
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReadOnlyPagedHomeSurface(
    apps: List<LauncherActivityInfo>,
    folders: List<LauncherFolder>,
    page: WorkspaceRenderedHomePage,
    pages: List<WorkspaceRenderedHomePage>,
    homeColumns: Int,
    homeRows: Int,
    showLabels: Boolean,
    iconScale: Float,
    homeLabelOverrides: Map<String, String>,
    onLaunchApp: (LauncherActivityInfo) -> Unit,
    onSetHomeLabelOverride: (LauncherActivityInfo, String?) -> Unit,
    onRequestUninstall: (LauncherActivityInfo) -> Unit,
    onMoveAppToPage: (LauncherActivityInfo, String) -> Unit,
    onMoveAppToPageCell: (LauncherActivityInfo, String, Int, Int) -> Unit,
    onMoveAppToCell: (LauncherActivityInfo, Int, Int) -> Unit,
    onMoveAppWithinPage: (LauncherActivityInfo, WorkspaceMoveDirection) -> Unit,
    onMoveAppOneCell: (LauncherActivityInfo, WorkspaceHomeSpatialDirection) -> Unit,
    onRenameFolder: (String, String) -> Unit,
    onDeleteFolder: (LauncherFolder) -> Unit,
    onAddAppToFolder: (String, LauncherActivityInfo) -> Unit,
    onRemoveAppFromFolder: (String, LauncherActivityInfo) -> Unit,
    onRemoveFolderFromHome: (LauncherFolder) -> Unit,
    onMoveFolderToPage: (LauncherFolder, String) -> Unit,
    onMoveFolderToPageCell: (LauncherFolder, String, Int, Int) -> Unit,
    onMoveFolderToCell: (LauncherFolder, Int, Int) -> Unit,
    onCreateAndroidWidgetView: (Int) -> AppWidgetHostView?,
    onRemoveWidget: (WorkspaceRenderedHomeWidget) -> Unit,
    onResizeWidget: (WorkspaceRenderedHomeWidget, Int, Int) -> Unit,
    onMoveWidgetToCell: (WorkspaceRenderedHomeWidget, Int, Int) -> Unit,
    onMoveWidgetToPage: (WorkspaceRenderedHomeWidget, String) -> Unit,
    onMoveWidgetToPageCell: (WorkspaceRenderedHomeWidget, String, Int, Int) -> Unit,
    onOpenWidgetSearch: () -> Unit,
    onOpenWidgetApps: () -> Unit,
    onOpenWidgetEditor: () -> Unit,
    onOpenHomeEditor: () -> Unit,
    onOpenWidgetSettings: () -> Unit,
    dockApps: List<LauncherActivityInfo> = emptyList(),
    dockStyle: LauncherDockStyle = LauncherDockStyle.GLASS,
    pageTransition: LauncherHomePageTransition = LauncherHomePageTransition.SLIDE,
    showPageIndicator: Boolean = true,
    onSelectPage: (String) -> Unit = {},
    layoutLocked: Boolean = false,
    onGridBoundsChanged: (Rect?) -> Unit = {},
    contentOnly: Boolean = false,
) {
    val appsByKey = remember(apps) { apps.associateBy { it.workspaceKey() } }
    val pageApps = remember(appsByKey, page.appKeys) {
        page.appKeys.mapNotNull(appsByKey::get)
    }
    val personalApps = remember(apps) {
        apps.filter { it.user == Process.myUserHandle() }
    }
    val personalAppsByKey = remember(personalApps) {
        personalApps.associateBy { it.workspaceKey() }
    }
    val foldersById = remember(folders) { folders.associateBy { it.id } }
    val pageFolders = remember(foldersById, page.folderPlacements) {
        page.folderPlacements.mapNotNull { placement ->
            foldersById[placement.folderId]
        }
    }
    val columns = homeColumns.coerceIn(4, 6)
    val rows = homeRows.coerceIn(4, 7)
    val pageWidgets = remember(page.widgetPlacements, columns, rows) {
        page.widgetPlacements.filter { widget ->
            widget.cellX in 0 until columns &&
                widget.cellY in 0 until rows &&
                widget.spanX > 0 &&
                widget.spanY > 0 &&
                widget.cellX + widget.spanX <= columns &&
                widget.cellY + widget.spanY <= rows
        }
    }
    val folderPlacements = remember(page.folderPlacements, columns, rows) {
        page.folderPlacements.associateBy { it.folderId }.filterValues { placement ->
            placement.cellX in 0 until columns && placement.cellY in 0 until rows
        }
    }
    val storedAppPlacements = remember(page.appPlacements) {
        page.appPlacements.associateBy { it.appKey }
    }
    val resolvedAppPlacements = remember(
        pageApps,
        storedAppPlacements,
        folderPlacements,
        pageWidgets,
        columns,
        rows,
    ) {
        val occupied = mutableSetOf<Pair<Int, Int>>()
        folderPlacements.values.forEach { placement ->
            occupied += placement.cellX to placement.cellY
        }
        pageWidgets.forEach { widget ->
            for (cellY in widget.cellY until widget.cellY + widget.spanY) {
                for (cellX in widget.cellX until widget.cellX + widget.spanX) {
                    occupied += cellX to cellY
                }
            }
        }

        val resolved = linkedMapOf<String, Pair<Int, Int>>()
        pageApps.forEach { app ->
            val placement = storedAppPlacements[app.workspaceKey()]
            val coordinate = if (
                placement?.cellX != null &&
                placement.cellY != null &&
                placement.spanX == 1 &&
                placement.spanY == 1 &&
                placement.cellX in 0 until columns &&
                placement.cellY in 0 until rows
            ) {
                placement.cellX to placement.cellY
            } else {
                null
            }
            if (coordinate != null && coordinate !in occupied) {
                resolved[app.workspaceKey()] = coordinate
                occupied += coordinate
            }
        }

        pageApps.forEach { app ->
            val key = app.workspaceKey()
            if (key in resolved) return@forEach
            val fallback = sequence {
                for (cellY in 0 until rows) {
                    for (cellX in 0 until columns) {
                        yield(cellX to cellY)
                    }
                }
            }.firstOrNull { it !in occupied }
            if (fallback != null) {
                resolved[key] = fallback
                occupied += fallback
            }
        }
        resolved
    }
    var openedFolderId by remember(page.pageId) { mutableStateOf<String?>(null) }
    var pickingFolderId by remember(page.pageId) { mutableStateOf<String?>(null) }
    var selectedWidget by remember(page.pageId) {
        mutableStateOf<WorkspaceRenderedHomeWidget?>(null)
    }
    val targetPages = remember(pages, page.pageId) {
        homeMoveTargetPages(
            pages = pages,
            currentPageId = page.pageId,
        )
    }
    var gridBounds by remember(page.pageId) { mutableStateOf<Rect?>(null) }
    DisposableEffect(page.pageId) {
        onDispose { onGridBoundsChanged(null) }
    }
    val cellBounds = remember(page.pageId, homeColumns, homeRows) {
        mutableMapOf<Pair<Int, Int>, Rect>()
    }
    val crossPageEdgeThresholdPx = with(LocalDensity.current) { 36.dp.toPx() }
    val occupiedHomeCells = remember(
        resolvedAppPlacements,
        folderPlacements,
        pageWidgets,
    ) {
        buildSet {
            addAll(resolvedAppPlacements.values)
            folderPlacements.values.forEach { placement ->
                add(placement.cellX to placement.cellY)
            }
            pageWidgets.forEach { widget ->
                for (cellY in widget.cellY until widget.cellY + widget.spanY) {
                    for (cellX in widget.cellX until widget.cellX + widget.spanX) {
                        add(cellX to cellY)
                    }
                }
            }
        }
    }

    // The activity window asks Android to draw the system wallpaper underneath Launcher.
    // This surface remains translucent and does not read wallpaper files directly.
    Box(
        Modifier
            .fillMaxSize()
            .testTag("launcher-home-page-" + page.pageId)
            .homeEditLongPress(
                enabled = true,
                canStartAt = { point ->
                    occupiedHomeCells.none { cell ->
                        cellBounds[cell]?.contains(point) == true
                    }
                },
                onLongPress = onOpenHomeEditor,
            ),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.08f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .then(if (contentOnly) Modifier else Modifier.navigationBarsPadding())
                .padding(horizontal = GlazeMetrics.space4),
        ) {
            Spacer(Modifier.height(72.dp))

            if (page.unsupportedItemCount > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = GlazeMetrics.space2),
                    shape = RoundedCornerShape(GlazeMetrics.radiusExtraLarge),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.92f)
                    ),
                ) {
                    Text(
                        "${page.unsupportedItemCount} item${if (page.unsupportedItemCount == 1) "" else "s"} on this page still need rendering or recovery support.",
                        modifier = Modifier.padding(GlazeMetrics.space3),
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            val hasVisibleItems =
                pageApps.isNotEmpty() || pageFolders.isNotEmpty() || pageWidgets.isNotEmpty()
            // Empty/new Home pages remain visually silent, but they still measure the same
            // transparent destination grid so a held cross-page drag can land on them.
            val horizontalSpacing = GlazeMetrics.space1
            val verticalSpacing = GlazeMetrics.space3
            val tileHeight = 96.dp
            val gridHeight = tileHeight * rows + verticalSpacing * (rows - 1)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .launcherHomePageEntryTransition(
                        transition = pageTransition,
                        transitionKey = page.pageId,
                    )
                    .then(
                        if (!hasVisibleItems) {
                            Modifier.testTag("launcher-home-empty-page")
                        } else {
                            Modifier
                        },
                    )
                    .verticalScroll(rememberScrollState()),
            ) {
                    Spacer(Modifier.height(GlazeMetrics.space4))
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(gridHeight)
                            .onGloballyPositioned {
                                val bounds = it.boundsInRoot()
                                gridBounds = bounds
                                onGridBoundsChanged(bounds)
                            },
                    ) {
                        val cellWidth =
                            (maxWidth - horizontalSpacing * (columns - 1)) / columns
                        val stepX = cellWidth + horizontalSpacing
                        val stepY = tileHeight + verticalSpacing

                        repeat(rows) { cellY ->
                            repeat(columns) { cellX ->
                                val coordinate = cellX to cellY
                                Box(
                                    modifier = Modifier
                                        .offset(
                                            x = stepX * cellX,
                                            y = stepY * cellY,
                                        )
                                        .size(width = cellWidth, height = tileHeight)
                                        .testTag(
                                            "launcher-secondary-home-cell-$cellX-$cellY",
                                        )
                                        .combinedClickable(
                                            onClick = {},
                                            onLongClick = onOpenHomeEditor,
                                        )
                                        .onGloballyPositioned {
                                            cellBounds[coordinate] = it.boundsInRoot()
                                        },
                                )
                            }
                        }

                        pageApps.forEach { app ->
                            val coordinate =
                                resolvedAppPlacements[app.workspaceKey()] ?: return@forEach
                            Box(
                                modifier = Modifier
                                    .offset(
                                        x = stepX * coordinate.first,
                                        y = stepY * coordinate.second,
                                    )
                                    .size(width = cellWidth, height = tileHeight),
                                contentAlignment = Alignment.TopCenter,
                            ) {
                                PagedAppTile(
                                    app = app,
                                    displayLabel = homeLabelOverrides[app.workspaceKey()]
                                        ?: app.label.toString(),
                                    homeLabelOverride = homeLabelOverrides[app.workspaceKey()],
                                    showLabel = showLabels,
                                    iconScale = iconScale,
                                    sourcePageId = page.pageId,
                                    targetPages = targetPages,
                                    layoutLocked = layoutLocked,
                                    onLaunchApp = onLaunchApp,
                                    onSetHomeLabelOverride = { label ->
                                        onSetHomeLabelOverride(app, label)
                                    },
                                    onRequestUninstall = { onRequestUninstall(app) },
                                    onMoveAppToPage = onMoveAppToPage,
                                    onMoveAppToPageCell = onMoveAppToPageCell,
                                    onMoveAppToCell = onMoveAppToCell,
                                    edgeTarget = { dropPoint ->
                                        gridBounds?.let { bounds ->
                                            homePageEdgeDropTarget(
                                                pages = pages,
                                                currentPageId = page.pageId,
                                                dropX = dropPoint.x,
                                                dropY = dropPoint.y,
                                                surfaceLeftPx = bounds.left,
                                                surfaceTopPx = bounds.top,
                                                surfaceRightPx = bounds.right,
                                                surfaceBottomPx = bounds.bottom,
                                                edgeThresholdPx = crossPageEdgeThresholdPx,
                                                columns = columns,
                                                rows = rows,
                                            )
                                        }
                                    },
                                    cellTarget = { dropPoint ->
                                        cellBounds.entries.firstOrNull { (_, bounds) ->
                                            bounds.contains(dropPoint)
                                        }?.key
                                    },
                                    onMoveAppWithinPage = onMoveAppWithinPage,
                                    onMoveAppOneCell = onMoveAppOneCell,
                                )
                            }
                        }

                        pageFolders.forEach { folder ->
                            val placement = folderPlacements[folder.id] ?: return@forEach
                            HomeFolderTile(
                                folder = folder,
                                allApps = personalApps,
                                showLabel = showLabels,
                                editMode = false,
                                layoutLocked = layoutLocked,
                                onOpen = { openedFolderId = folder.id },
                                onDrop = { selected, dropPoint ->
                                    val edgeTarget = gridBounds?.let { bounds ->
                                        homePageEdgeDropTarget(
                                            pages = pages,
                                            currentPageId = page.pageId,
                                            dropX = dropPoint.x,
                                            dropY = dropPoint.y,
                                            surfaceLeftPx = bounds.left,
                                            surfaceTopPx = bounds.top,
                                            surfaceRightPx = bounds.right,
                                            surfaceBottomPx = bounds.bottom,
                                            edgeThresholdPx = crossPageEdgeThresholdPx,
                                            columns = columns,
                                            rows = rows,
                                        )
                                    }
                                    if (edgeTarget != null) {
                                        onMoveFolderToPageCell(
                                            selected,
                                            edgeTarget.pageId,
                                            edgeTarget.cellX,
                                            edgeTarget.cellY,
                                        )
                                    } else {
                                        cellBounds.entries.firstOrNull { (_, bounds) ->
                                            bounds.contains(dropPoint)
                                        }?.key?.let { (cellX, cellY) ->
                                            onMoveFolderToCell(selected, cellX, cellY)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .offset(
                                        x = stepX * placement.cellX,
                                        y = stepY * placement.cellY,
                                    )
                                    .size(width = cellWidth, height = tileHeight),
                            )
                        }

                        pageWidgets.forEach { widget ->
                            HomeWidgetTile(
                                widget = widget,
                                editMode = false,
                                layoutLocked = layoutLocked,
                                onCreateAndroidWidgetView = onCreateAndroidWidgetView,
                                onManageWidget = { selectedWidget = it },
                                onOpenSearch = onOpenWidgetSearch,
                                onOpenApps = onOpenWidgetApps,
                                onOpenHomeEditor = onOpenHomeEditor,
                                onOpenSettings = onOpenWidgetSettings,
                                onDropWidget = { candidate, dropPoint ->
                                    val edgeTarget = gridBounds?.let { bounds ->
                                        homePageEdgeDropTarget(
                                            pages = pages,
                                            currentPageId = page.pageId,
                                            dropX = dropPoint.x,
                                            dropY = dropPoint.y,
                                            surfaceLeftPx = bounds.left,
                                            surfaceTopPx = bounds.top,
                                            surfaceRightPx = bounds.right,
                                            surfaceBottomPx = bounds.bottom,
                                            edgeThresholdPx = crossPageEdgeThresholdPx,
                                            columns = columns,
                                            rows = rows,
                                            spanX = candidate.spanX,
                                            spanY = candidate.spanY,
                                        )
                                    }
                                    if (edgeTarget != null) {
                                        onMoveWidgetToPageCell(
                                            candidate,
                                            edgeTarget.pageId,
                                            edgeTarget.cellX,
                                            edgeTarget.cellY,
                                        )
                                    } else {
                                        cellBounds.entries.firstOrNull { (_, bounds) ->
                                            bounds.contains(dropPoint)
                                        }?.key?.let { (cellX, cellY) ->
                                            onMoveWidgetToCell(candidate, cellX, cellY)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .offset(
                                        x = stepX * widget.cellX,
                                        y = stepY * widget.cellY,
                                    )
                                    .size(
                                        width = cellWidth * widget.spanX +
                                            horizontalSpacing * (widget.spanX - 1),
                                        height = tileHeight * widget.spanY +
                                            verticalSpacing * (widget.spanY - 1),
                                    ),
                            )
                        }
                    }
                    Spacer(
                        Modifier.height(
                            if (pages.size > 1) GlazeMetrics.space1 else GlazeMetrics.space4,
                        ),
                    )
                }

            if (
                secondaryHomeShouldRenderPageIndicator(
                    contentOnly = contentOnly,
                    requested = showPageIndicator,
                    pageCount = pages.size,
                )
            ) {
                HomePageDots(
                    pages = pages,
                    selectedPageId = page.pageId,
                    onSelectPage = onSelectPage,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .testTag("launcher-secondary-home-page-indicator"),
                )
            }

            if (
                secondaryHomeShouldRenderDock(
                    contentOnly = contentOnly,
                    dockAppCount = dockApps.size,
                )
            ) {
                PersistentHomeDock(
                    apps = dockApps,
                    iconScale = iconScale,
                    style = dockStyle,
                    onLaunchApp = onLaunchApp,
                )
                Spacer(Modifier.height(2.dp))
            }
        }
        openedFolderId?.let { id -> pageFolders.firstOrNull { it.id == id } }
            ?.let { folder ->
                LauncherFolderContentsSheet(
                    folder = folder,
                    appsByKey = personalAppsByKey,
                    isOnHome = true,
                    onLaunchApp = onLaunchApp,
                    onRemoveApp = { app -> onRemoveAppFromFolder(folder.id, app) },
                    onAddApps = {
                        openedFolderId = null
                        pickingFolderId = folder.id
                    },
                    onRename = { name -> onRenameFolder(folder.id, name) },
                    onAddToHome = {},
                    onRemoveFromHome = {
                        openedFolderId = null
                        onRemoveFolderFromHome(folder)
                    },
                    moveTargets = pages.filter { candidate ->
                        candidate.folderPlacements.none { it.folderId == folder.id }
                    },
                    onMoveToPage = { destination ->
                        openedFolderId = null
                        onMoveFolderToPage(folder, destination)
                    },
                    onDelete = {
                        openedFolderId = null
                        onDeleteFolder(folder)
                    },
                    onDismiss = { openedFolderId = null },
                )
            }
        pickingFolderId?.let { id -> pageFolders.firstOrNull { it.id == id } }
            ?.let { folder ->
                LauncherFolderAppPickerSheet(
                    folder = folder,
                    availableApps = personalApps,
                    onAddApp = { app -> onAddAppToFolder(folder.id, app) },
                    onDismiss = {
                        pickingFolderId = null
                        openedFolderId = folder.id
                    },
                )
            }
        selectedWidget?.let { widget ->
            LauncherWidgetManagementDialog(
                widget = widget,
                columns = columns,
                rows = rows,
                layoutLocked = layoutLocked,
                onResize = { spanX, spanY ->
                    onResizeWidget(widget, spanX, spanY)
                    selectedWidget = null
                },
                onRemove = {
                    onRemoveWidget(widget)
                    selectedWidget = null
                },
                onMove = { cellX, cellY ->
                    onMoveWidgetToCell(widget, cellX, cellY)
                    selectedWidget = null
                },
                moveTargets = targetPages,
                onMoveToPage = { targetPageId ->
                    onMoveWidgetToPage(widget, targetPageId)
                    selectedWidget = null
                },
                onClose = { selectedWidget = null },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PagedAppTile(
    app: LauncherActivityInfo,
    displayLabel: String,
    homeLabelOverride: String?,
    showLabel: Boolean,
    iconScale: Float,
    sourcePageId: String,
    targetPages: List<WorkspaceRenderedHomePage>,
    layoutLocked: Boolean,
    onLaunchApp: (LauncherActivityInfo) -> Unit,
    onSetHomeLabelOverride: (String?) -> Unit,
    onRequestUninstall: () -> Unit,
    onMoveAppToPage: (LauncherActivityInfo, String) -> Unit,
    onMoveAppToPageCell: (LauncherActivityInfo, String, Int, Int) -> Unit,
    onMoveAppToCell: (LauncherActivityInfo, Int, Int) -> Unit,
    edgeTarget: (Offset) -> HomePageEdgeDropTarget?,
    cellTarget: (Offset) -> Pair<Int, Int>?,
    onMoveAppWithinPage: (LauncherActivityInfo, WorkspaceMoveDirection) -> Unit,
    onMoveAppOneCell: (LauncherActivityInfo, WorkspaceHomeSpatialDirection) -> Unit,
) {
    val icon = rememberLauncherAppIcon(app)
    var manageOpen by remember(app.componentName, app.user) { mutableStateOf(false) }
    var dragging by remember(app.componentName, app.user) { mutableStateOf(false) }
    var tileBounds by remember(app.componentName, app.user) { mutableStateOf<Rect?>(null) }
    val dragData = remember(app.componentName, app.user, sourcePageId, layoutLocked) {
        if (layoutLocked) {
            null
        } else {
            LauncherAppDragData(
                appKey = app.workspaceKey(),
                origin = LauncherAppDragOrigin.HOME,
                sourcePageId = sourcePageId,
            )
        }
    }

    val interactionModifier = if (dragData == null) {
        Modifier.combinedClickable(
            onClick = { onLaunchApp(app) },
            onLongClick = { manageOpen = true },
        )
    } else {
        Modifier
            .dragAndDropSource { _ ->
                dragData.toTransferData()
            }
            .clickable(enabled = !dragging) { onLaunchApp(app) }
    }

    Column(
        modifier = Modifier
            .onGloballyPositioned { tileBounds = it.boundsInRoot() }
            .graphicsLayer {
                alpha = if (dragging) 0.86f else 1f
            }
            .then(interactionModifier)
            .padding(GlazeMetrics.space1)
            .testTag("launcher-secondary-home-app-" + app.workspaceKey()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = displayLabel,
                modifier = Modifier
                    .size((54f * iconScale.coerceIn(0.85f, 1.15f)).dp)
                    .launcherIconMask(),
            )
        } else {
            Surface(
                modifier = Modifier.size((54f * iconScale.coerceIn(0.85f, 1.15f)).dp),
                shape = RoundedCornerShape(GlazeMetrics.radiusExtraLarge),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(displayLabel.take(1).uppercase(), fontWeight = FontWeight.Bold)
                }
            }
        }
        if (showLabel) {
            Spacer(Modifier.height(GlazeMetrics.space1))
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f),
                shape = RoundedCornerShape(GlazeMetrics.radiusControl),
            ) {
                Text(
                    text = displayLabel,
                    modifier = Modifier.padding(horizontal = GlazeMetrics.space1),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    if (manageOpen) {
        PagedAppManagementDialog(
            app = app,
            displayLabel = displayLabel,
            homeLabelOverride = homeLabelOverride,
            targetPages = targetPages,
            layoutLocked = layoutLocked,
            onSetHomeLabelOverride = onSetHomeLabelOverride,
            onRequestUninstall = onRequestUninstall,
            onMoveAppToPage = onMoveAppToPage,
            onMoveAppWithinPage = onMoveAppWithinPage,
            onMoveAppOneCell = onMoveAppOneCell,
            onClose = { manageOpen = false },
        )
    }
}

@Composable
private fun PagedAppManagementDialog(
    app: LauncherActivityInfo,
    displayLabel: String,
    homeLabelOverride: String?,
    targetPages: List<WorkspaceRenderedHomePage>,
    layoutLocked: Boolean,
    onSetHomeLabelOverride: (String?) -> Unit,
    onRequestUninstall: () -> Unit,
    onMoveAppToPage: (LauncherActivityInfo, String) -> Unit,
    onMoveAppWithinPage: (LauncherActivityInfo, WorkspaceMoveDirection) -> Unit,
    onMoveAppOneCell: (LauncherActivityInfo, WorkspaceHomeSpatialDirection) -> Unit,
    onClose: () -> Unit,
) {
    val originalLabel = app.label.toString()
    var labelDraft by remember(app.workspaceKey(), homeLabelOverride) {
        mutableStateOf(homeLabelOverride ?: originalLabel)
    }

    AlertDialog(
        onDismissRequest = onClose,
        shape = RoundedCornerShape(GlazeMetrics.radiusExtraLarge),
        title = { Text(displayLabel) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space3),
            ) {
                Text(
                    if (layoutLocked) {
                        "Home screen layout is locked. Unlock it in Launcher settings or hold the Home lock control for 5 seconds before changing placement."
                    } else {
                        "Manage this app on the current Home page."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Text("Order on page", fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                ) {
                    OutlinedButton(
                        onClick = { onMoveAppWithinPage(app, WorkspaceMoveDirection.EARLIER) },
                        enabled = !layoutLocked,
                        modifier = Modifier.weight(1f),
                    ) { Text("Earlier") }
                    OutlinedButton(
                        onClick = { onMoveAppWithinPage(app, WorkspaceMoveDirection.LATER) },
                        enabled = !layoutLocked,
                        modifier = Modifier.weight(1f),
                    ) { Text("Later") }
                }

                Text("Move one cell", fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                ) {
                    OutlinedButton(
                        onClick = { onMoveAppOneCell(app, WorkspaceHomeSpatialDirection.LEFT) },
                        enabled = !layoutLocked,
                        modifier = Modifier.weight(1f),
                    ) { Text("Left") }
                    OutlinedButton(
                        onClick = { onMoveAppOneCell(app, WorkspaceHomeSpatialDirection.RIGHT) },
                        enabled = !layoutLocked,
                        modifier = Modifier.weight(1f),
                    ) { Text("Right") }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                ) {
                    OutlinedButton(
                        onClick = { onMoveAppOneCell(app, WorkspaceHomeSpatialDirection.UP) },
                        enabled = !layoutLocked,
                        modifier = Modifier.weight(1f),
                    ) { Text("Up") }
                    OutlinedButton(
                        onClick = { onMoveAppOneCell(app, WorkspaceHomeSpatialDirection.DOWN) },
                        enabled = !layoutLocked,
                        modifier = Modifier.weight(1f),
                    ) { Text("Down") }
                }

                Text("Home label", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = labelDraft,
                    onValueChange = { labelDraft = it.take(LauncherHomeLabelPolicy.MAX_LABEL_LENGTH) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = { Text("Rename this label on Home only.") },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                ) {
                    FilledTonalButton(
                        onClick = {
                            val normalized = LauncherHomeLabelPolicy.normalize(labelDraft)
                            val original = LauncherHomeLabelPolicy.normalize(originalLabel)
                            onSetHomeLabelOverride(normalized?.takeUnless { it == original })
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("Save label") }
                    TextButton(
                        onClick = {
                            labelDraft = originalLabel
                            onSetHomeLabelOverride(null)
                        },
                    ) { Text("Reset") }
                }

                OutlinedButton(
                    onClick = {
                        onRequestUninstall()
                        onClose()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Uninstall app") }
                Text(
                    "Android will show its system uninstall confirmation before anything is removed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (targetPages.isNotEmpty()) {
                    Text("Move to another Home page", fontWeight = FontWeight.SemiBold)
                    targetPages.forEach { target ->
                        FilledTonalButton(
                            onClick = {
                                onMoveAppToPage(app, target.pageId)
                                onClose()
                            },
                            enabled = !layoutLocked,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                target.context().moveTargetLabel(
                                    pageNumber = target.rank + 1,
                                    primary = target.pageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID,
                                ),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Done") } },
    )
}
