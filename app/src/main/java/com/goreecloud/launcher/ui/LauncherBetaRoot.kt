package com.goreecloud.launcher.ui

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.appwidget.AppWidgetHostView
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.BatteryManager
import android.os.Process
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import com.goreecloud.launcher.core.launcher.LauncherAppIconCache
import com.goreecloud.launcher.core.launcher.LauncherAppVisibilityPolicy
import com.goreecloud.launcher.core.launcher.LauncherAppFreshness
import com.goreecloud.launcher.core.launcher.LauncherUniversalSearchHomeMode
import com.goreecloud.launcher.core.launcher.LaunchApplicationSearchAction
import com.goreecloud.launcher.core.launcher.LauncherLaunchShortcutSearchAction
import com.goreecloud.launcher.core.launcher.LauncherDockDragPageDirection
import com.goreecloud.launcher.core.launcher.LauncherDockStyle
import com.goreecloud.launcher.core.launcher.launcherDockInitialVirtualPage
import com.goreecloud.launcher.core.launcher.launcherDockLogicalPage
import com.goreecloud.launcher.core.launcher.launcherDockLoopBoundaryTarget
import com.goreecloud.launcher.core.launcher.launcherDockNextPageInsertionKey
import com.goreecloud.launcher.core.launcher.launcherDockPagePlan
import com.goreecloud.launcher.core.launcher.launcherDockVirtualPageCount
import com.goreecloud.launcher.core.launcher.LauncherDrawerBackdrop
import com.goreecloud.launcher.core.launcher.LauncherDrawerDiscoveryFilter
import com.goreecloud.launcher.core.launcher.LauncherDrawerDiscoveryPolicy
import com.goreecloud.launcher.core.launcher.LauncherDrawerSmartFolder
import com.goreecloud.launcher.core.launcher.LauncherDrawerSmartFolderKind
import com.goreecloud.launcher.core.launcher.LauncherDrawerSmartFolderPolicy
import com.goreecloud.launcher.core.launcher.LauncherDrawerEntryMode
import com.goreecloud.launcher.core.launcher.LauncherDrawerHeaderPresentation
import com.goreecloud.launcher.core.launcher.LauncherDrawerLayoutMode
import com.goreecloud.launcher.core.launcher.LauncherDrawerNavigation
import com.goreecloud.launcher.core.launcher.LauncherDrawerProfileKind
import com.goreecloud.launcher.core.launcher.LauncherDrawerSearchPlacement
import com.goreecloud.launcher.core.launcher.LauncherDrawerSpacing
import com.goreecloud.launcher.core.launcher.LauncherDrawerTab
import com.goreecloud.launcher.core.launcher.LauncherExperiencePreferences
import com.goreecloud.launcher.core.launcher.LauncherHomeCardStyle
import com.goreecloud.launcher.core.launcher.LauncherHomeLabelPolicy
import com.goreecloud.launcher.core.launcher.LauncherHomeAppMode
import com.goreecloud.launcher.core.launcher.LauncherHomeSuggestionsPolicy
import com.goreecloud.launcher.core.launcher.LauncherHomeGlanceAlignment
import com.goreecloud.launcher.core.launcher.LauncherHomeSearchPlacement
import com.goreecloud.launcher.core.launcher.LauncherHomeSearchStyle
import com.goreecloud.launcher.core.launcher.LauncherHomeSearchSurface
import com.goreecloud.launcher.core.launcher.launcherHomeSearchSurface
import com.goreecloud.launcher.core.launcher.LauncherHomeSpacing
import com.goreecloud.launcher.core.launcher.LauncherHomePageTransition
import com.goreecloud.launcher.core.launcher.LauncherGestureAction
import com.goreecloud.launcher.core.launcher.LauncherGestureSensitivity
import com.goreecloud.launcher.core.launcher.LauncherGestureActionType
import com.goreecloud.launcher.core.launcher.LauncherFolder
import com.goreecloud.launcher.core.launcher.LauncherFolderProfilePolicy
import com.goreecloud.launcher.core.launcher.LauncherHomeGesture
import com.goreecloud.launcher.core.launcher.LauncherIconPackDescriptor
import com.goreecloud.launcher.core.launcher.LauncherIconShape
import com.goreecloud.launcher.core.launcher.LauncherBuiltInSearchProviderRegistry
import com.goreecloud.launcher.core.launcher.LauncherNavigateSearchAction
import com.goreecloud.launcher.core.launcher.LauncherSearchCategory
import com.goreecloud.launcher.core.launcher.LauncherSearchDestination
import com.goreecloud.launcher.core.launcher.LauncherSearchResult
import com.goreecloud.launcher.core.launcher.LauncherPreferences
import com.goreecloud.launcher.core.launcher.LauncherUniversalSearch
import com.goreecloud.launcher.core.launcher.LauncherWallpaperShade
import com.goreecloud.launcher.core.launcher.LauncherVisualPreferences
import com.goreecloud.launcher.core.launcher.LauncherVisualPreferencesRepository
import com.goreecloud.launcher.core.launcher.LauncherWidgetProviderDescriptor
import com.goreecloud.launcher.core.launcher.LauncherWeather
import com.goreecloud.launcher.core.launcher.LauncherWeatherSnapshot
import com.goreecloud.launcher.core.launcher.LauncherWeatherVisualKind
import com.goreecloud.launcher.core.launcher.launcherWeatherVisualKind
import com.goreecloud.launcher.core.launcher.launcherDrawerProfilePages
import com.goreecloud.launcher.core.workspace.MAX_DOCK_ITEMS
import com.goreecloud.launcher.core.workspace.WorkspaceMoveDirection
import com.goreecloud.launcher.core.workspace.WorkspaceState
import com.goreecloud.launcher.core.workspace.WorkspaceWidgetCatalog
import com.goreecloud.launcher.core.workspace.WorkspaceWidgetDescriptor
import com.goreecloud.launcher.core.workspace.db.WorkspaceRenderedHomeFolder
import com.goreecloud.launcher.core.workspace.db.WorkspaceLegacyImportMapper
import com.goreecloud.launcher.core.workspace.db.WorkspaceRenderedHomePage
import com.goreecloud.launcher.core.workspace.db.WorkspaceRenderedHomeWidget
import com.goreecloud.launcher.core.workspace.db.context
import com.goreecloud.launcher.core.workspace.workspaceKey
import com.goreecloud.launcher.ui.theme.GlazeAtmosphere
import com.goreecloud.launcher.ui.theme.GlazeMetrics
import com.goreecloud.launcher.ui.theme.GlazeThemeMode
import com.goreecloud.launcher.ui.theme.GlazeV16MaterialRole
import com.goreecloud.launcher.ui.theme.GlazeV16PresentationPolicy
import com.goreecloud.launcher.ui.theme.LocalGlazeV16PresentationContext
import com.goreecloud.launcher.ui.theme.ThemeManagerSurface
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

enum class LauncherSurfaceMode { HOME, SEARCH, DRAWER, SETTINGS, THEME_MANAGER }

internal enum class LauncherAppDragOrigin { HOME, DOCK, DRAWER }

internal enum class LauncherAppContextOrigin { HOME, DOCK, DRAWER }

internal data class LauncherAppDragData(
    val appKey: String,
    val origin: LauncherAppDragOrigin,
    val sourcePageId: String? = null,
)

internal fun launcherFolderGridColumns(
    availableWidthDp: Float,
    largeText: Boolean,
): Int = when {
    availableWidthDp <= 0f -> 3
    largeText -> 3
    availableWidthDp >= 336f -> 4
    else -> 3
}

internal fun launcherFolderPanelWidthDp(
    availableWidthDp: Float,
): Float {
    if (availableWidthDp <= 0f) return 520f
    val maxWidthDp = when {
        availableWidthDp >= 720f -> 600f
        availableWidthDp >= 600f -> 560f
        else -> 520f
    }
    return minOf(availableWidthDp * 0.92f, maxWidthDp)
}

private const val LAUNCHER_FOLDER_ROWS_PER_PAGE = 3

internal fun launcherFolderCellMinHeightDp(
    largeText: Boolean,
    extraLargeText: Boolean,
): Float = when {
    extraLargeText -> 118f
    largeText -> 108f
    else -> 98f
}

internal fun launcherFolderPageCount(
    itemCount: Int,
    columns: Int,
    includeAddTile: Boolean,
): Int {
    val safeColumns = columns.coerceAtLeast(1)
    val capacity = safeColumns * LAUNCHER_FOLDER_ROWS_PER_PAGE
    val totalItems = itemCount.coerceAtLeast(0) + if (includeAddTile) 1 else 0
    return ((totalItems + capacity - 1) / capacity).coerceAtLeast(1)
}

internal fun launcherFolderPickerMinCellWidthDp(
    largeText: Boolean,
    extraLargeText: Boolean,
): Float = when {
    extraLargeText -> 116f
    largeText -> 104f
    else -> 82f
}

internal fun launcherHomeTileIconSlotHeightDp(
    availableHeightDp: Float,
    showLabel: Boolean,
): Float {
    if (availableHeightDp <= 0f) return 44f
    val reservedLabelHeight = if (showLabel) 21f else 0f
    return (availableHeightDp - reservedLabelHeight).coerceIn(44f, 60f)
}

internal fun launcherHomeSearchMaterialRole(
    style: LauncherHomeSearchStyle,
): GlazeV16MaterialRole = when (style) {
    LauncherHomeSearchStyle.GLASS -> GlazeV16MaterialRole.FUNCTIONAL_GLASS
    LauncherHomeSearchStyle.CLEAR -> GlazeV16MaterialRole.CLEAR_GLASS
    LauncherHomeSearchStyle.SOLID -> GlazeV16MaterialRole.SOLID
}

internal fun launcherDockMaterialRole(
    style: LauncherDockStyle,
): GlazeV16MaterialRole = when (style) {
    LauncherDockStyle.CLEAR -> GlazeV16MaterialRole.CLEAR_GLASS
    LauncherDockStyle.GLASS,
    LauncherDockStyle.EDGE -> GlazeV16MaterialRole.FUNCTIONAL_GLASS
    LauncherDockStyle.SOLID -> GlazeV16MaterialRole.SOLID
    LauncherDockStyle.RAISED -> GlazeV16MaterialRole.RAISED
}

internal fun launcherDockWidthFraction(
    appCount: Int,
    showSearch: Boolean,
): Float = when {
    showSearch -> 0.82f
    appCount.coerceAtLeast(0) <= 1 -> 0.32f
    appCount == 2 -> 0.40f
    appCount == 3 -> 0.50f
    appCount == 4 -> 0.62f
    appCount == 5 -> 0.72f
    else -> 0.82f
}

internal fun launcherHomeSearchHeightDp(
    style: LauncherHomeSearchStyle,
    largeText: Boolean,
    extraLargeText: Boolean,
): Float = when {
    extraLargeText -> 64f
    largeText -> 60f
    style == LauncherHomeSearchStyle.CLEAR -> 50f
    else -> 54f
}

internal fun launcherSearchFieldHeightDp(
    largeText: Boolean,
    extraLargeText: Boolean,
): Float = when {
    extraLargeText -> 64f
    largeText -> 60f
    else -> 54f
}

internal fun launcherUsesWallpaperGlass(
    materialRole: GlazeV16MaterialRole,
): Boolean = materialRole != GlazeV16MaterialRole.SOLID &&
    materialRole != GlazeV16MaterialRole.RAISED

internal fun launcherWidgetDragMoved(
    delta: Offset,
    thresholdPx: Float,
): Boolean =
    thresholdPx > 0f &&
        (abs(delta.x) >= thresholdPx || abs(delta.y) >= thresholdPx)

internal fun launcherUsesDarkSystemBarIcons(
    surfaceMode: LauncherSurfaceMode,
    startupWizardCompleted: Boolean,
    homeEditorVisible: Boolean,
    darkTheme: Boolean,
): Boolean {
    if (darkTheme) return false
    if (!startupWizardCompleted || homeEditorVisible) return true
    return surfaceMode == LauncherSurfaceMode.SETTINGS ||
        surfaceMode == LauncherSurfaceMode.THEME_MANAGER
}

internal fun LauncherAppDragData.toTransferData(): DragAndDropTransferData =
    DragAndDropTransferData(
        clipData = ClipData.newPlainText("GoreeCloud Launcher app", appKey),
        localState = this,
    )

internal fun DragAndDropEvent.launcherAppDragData(): LauncherAppDragData? =
    toAndroidDragEvent().localState as? LauncherAppDragData

internal fun DragAndDropEvent.rootDropPoint(): Offset =
    toAndroidDragEvent().let { event -> Offset(event.x, event.y) }

private fun nearestHomeCell(
    point: Offset,
    bounds: Map<Pair<Int, Int>, Rect>,
): Pair<Int, Int>? =
    bounds.entries.minByOrNull { entry ->
        val dx = entry.value.center.x - point.x
        val dy = entry.value.center.y - point.y
        dx * dx + dy * dy
    }?.key

private fun dockInsertionTarget(
    dropX: Float,
    sourceKey: String,
    bounds: Map<String, Rect>,
): String? =
    bounds.entries
        .asSequence()
        .filter { (key, rect) ->
            key != sourceKey && rect.center.x.isFinite()
        }
        .sortedBy { it.value.center.x }
        .firstOrNull { it.value.center.x > dropX }
        ?.key

internal fun primaryHomeShouldRenderFixedSearch(
    contentOnly: Boolean,
    requested: Boolean,
): Boolean = !contentOnly && requested

internal fun primaryHomeShouldReservePageIndicator(
    contentOnly: Boolean,
    pageCount: Int,
    requested: Boolean,
): Boolean = !contentOnly && requested && pageCount > 1

internal fun primaryHomeShouldRenderDock(
    contentOnly: Boolean,
    dockAppCount: Int,
    activeDrag: Boolean,
    persistentAffordance: Boolean = false,
    dockHostedExternally: Boolean = false,
): Boolean = !contentOnly &&
    !dockHostedExternally &&
    (dockAppCount > 0 || activeDrag || persistentAffordance)

internal fun primaryHomeShouldOwnBottomInset(
    contentOnly: Boolean,
    dockHostedExternally: Boolean,
): Boolean = !contentOnly && !dockHostedExternally

internal fun primaryHomeShouldHandleHorizontalPaging(
    contentOnly: Boolean,
    pagingHostedExternally: Boolean = false,
): Boolean = !contentOnly && !pagingHostedExternally

internal fun resolvedHomePageId(
    selectedHomePageId: String?,
    pages: List<WorkspaceRenderedHomePage>,
): String =
    pages.firstOrNull { it.pageId == selectedHomePageId }?.pageId
        ?: WorkspaceLegacyImportMapper.HOME_PAGE_ID

internal fun launcherHomePagerSelectedIndex(
    selectedHomePageId: String?,
    pages: List<WorkspaceRenderedHomePage>,
): Int {
    val resolvedPageId = resolvedHomePageId(
        selectedHomePageId = selectedHomePageId,
        pages = pages,
    )
    val resolvedIndex = pages.indexOfFirst { it.pageId == resolvedPageId }
    if (resolvedIndex >= 0) return resolvedIndex

    val primaryIndex = pages.indexOfFirst {
        it.pageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID
    }
    return primaryIndex.takeIf { it >= 0 } ?: 0
}

internal fun launcherHomeBeyondViewportPageCount(
    pageCount: Int,
): Int = if (pageCount > 1) 1 else 0

internal enum class LauncherHomePagerBoundarySwipe {
    LEFT,
    RIGHT,
}

internal fun launcherHomePagerBoundarySwipe(
    startPageIndex: Int,
    pageCount: Int,
    horizontalDistancePx: Float,
    verticalDistancePx: Float,
    minimumDistancePx: Float,
): LauncherHomePagerBoundarySwipe? {
    if (
        pageCount <= 1 ||
        startPageIndex !in 0 until pageCount ||
        abs(horizontalDistancePx) < minimumDistancePx ||
        abs(horizontalDistancePx) <= abs(verticalDistancePx) * 1.20f
    ) {
        return null
    }

    return when {
        startPageIndex == 0 && horizontalDistancePx > 0f ->
            LauncherHomePagerBoundarySwipe.RIGHT
        startPageIndex == pageCount - 1 && horizontalDistancePx < 0f ->
            LauncherHomePagerBoundarySwipe.LEFT
        else -> null
    }
}

internal fun Modifier.launcherHomePagerBoundaryGestureNavigation(
    enabled: Boolean,
    currentPageIndex: () -> Int,
    pageCount: Int,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
): Modifier {
    if (!enabled || pageCount <= 1) return this

    return pointerInput(enabled, pageCount, currentPageIndex, onSwipeLeft, onSwipeRight) {
        val minimumDistancePx = 56.dp.toPx()

        awaitEachGesture {
            val down = awaitFirstDown(
                requireUnconsumed = false,
                pass = PointerEventPass.Final,
            )
            val startPageIndex = currentPageIndex()
            var horizontalDistance = 0f
            var verticalDistance = 0f
            var triggered = false

            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Final)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                // The pager consumes horizontal position changes before the Final pass. Boundary
                // actions still need the raw pointer travel so outward swipes on the first/last
                // page remain observable without competing with ordinary pager navigation.
                val delta = change.position - change.previousPosition
                horizontalDistance += delta.x
                verticalDistance += delta.y

                if (!triggered) {
                    when (
                        launcherHomePagerBoundarySwipe(
                            startPageIndex = startPageIndex,
                            pageCount = pageCount,
                            horizontalDistancePx = horizontalDistance,
                            verticalDistancePx = verticalDistance,
                            minimumDistancePx = minimumDistancePx,
                        )
                    ) {
                        LauncherHomePagerBoundarySwipe.LEFT -> {
                            triggered = true
                            onSwipeLeft()
                        }
                        LauncherHomePagerBoundarySwipe.RIGHT -> {
                            triggered = true
                            onSwipeRight()
                        }
                        null -> Unit
                    }
                }

                if (!change.pressed) break
            }
        }
    }
}

internal enum class LauncherHomePagerVerticalSwipe {
    UP,
    DOWN,
}

internal fun launcherHomePagerVerticalSwipe(
    horizontalDistancePx: Float,
    verticalDistancePx: Float,
    minimumDistancePx: Float,
): LauncherHomePagerVerticalSwipe? {
    if (
        abs(verticalDistancePx) < minimumDistancePx ||
        abs(verticalDistancePx) <= abs(horizontalDistancePx) * 1.20f
    ) {
        return null
    }

    return if (verticalDistancePx < 0f) {
        LauncherHomePagerVerticalSwipe.UP
    } else {
        LauncherHomePagerVerticalSwipe.DOWN
    }
}

internal fun Modifier.launcherHomePagerVerticalGestureNavigation(
    enabled: Boolean,
    sensitivity: LauncherGestureSensitivity = LauncherGestureSensitivity.STANDARD,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
): Modifier {
    if (!enabled) return this

    return pointerInput(enabled, sensitivity, onSwipeUp, onSwipeDown) {
        val minimumDistancePx = sensitivity.activationDistancePx(56.dp.toPx())

        // Observe before the HorizontalPager/child gesture stack arbitrates the stream.
        // This observer never consumes changes; it only dispatches a configured Home action
        // once vertical travel is dominant and above threshold.
        awaitEachGesture {
            val down = awaitFirstDown(
                requireUnconsumed = false,
                pass = PointerEventPass.Initial,
            )
            var horizontalDistance = 0f
            var verticalDistance = 0f
            var triggered = false

            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                val delta = change.position - change.previousPosition
                horizontalDistance += delta.x
                verticalDistance += delta.y

                if (!triggered) {
                    when (
                        launcherHomePagerVerticalSwipe(
                            horizontalDistancePx = horizontalDistance,
                            verticalDistancePx = verticalDistance,
                            minimumDistancePx = minimumDistancePx,
                        )
                    ) {
                        LauncherHomePagerVerticalSwipe.UP -> {
                            triggered = true
                            onSwipeUp()
                        }
                        LauncherHomePagerVerticalSwipe.DOWN -> {
                            triggered = true
                            onSwipeDown()
                        }
                        null -> Unit
                    }
                }

                if (!change.pressed) break
            }
        }
    }
}

internal fun primaryHomeTransitionKey(
    selectedHomePageId: String?,
    pages: List<WorkspaceRenderedHomePage>,
    pagingHostedExternally: Boolean,
): String =
    if (pagingHostedExternally) {
        WorkspaceLegacyImportMapper.HOME_PAGE_ID
    } else {
        resolvedHomePageId(
            selectedHomePageId = selectedHomePageId,
            pages = pages,
        )
    }

internal fun selectedSecondaryHomePage(
    selectedHomePageId: String?,
    pages: List<WorkspaceRenderedHomePage>,
): WorkspaceRenderedHomePage? {
    val resolvedPageId = resolvedHomePageId(
        selectedHomePageId = selectedHomePageId,
        pages = pages,
    )
    return pages.firstOrNull { page ->
        page.pageId == resolvedPageId &&
            page.pageId != WorkspaceLegacyImportMapper.HOME_PAGE_ID
    }
}

internal fun dispatchLauncherHomeGestureAction(
    action: LauncherGestureAction,
    appsByKey: Map<String, LauncherActivityInfo>,
    onOpenApps: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenHomeEditor: () -> Unit,
    onOpenWallpaperPicker: () -> Unit,
    onOpenThemeManager: () -> Unit,
    onLaunchApp: (LauncherActivityInfo) -> Unit,
) {
    when (action.type) {
        LauncherGestureActionType.NONE -> Unit
        LauncherGestureActionType.APPS -> onOpenApps()
        LauncherGestureActionType.UNIVERSAL_SEARCH -> onOpenSearch()
        LauncherGestureActionType.LAUNCHER_SETTINGS,
        LauncherGestureActionType.HOME_EDITOR -> onOpenHomeEditor()
        LauncherGestureActionType.WALLPAPER -> onOpenWallpaperPicker()
        LauncherGestureActionType.THEME_MANAGER -> onOpenThemeManager()
        LauncherGestureActionType.OPEN_APP -> {
            action.appKey
                ?.let(appsByKey::get)
                ?.let(onLaunchApp)
        }
    }
}

@Composable
internal fun EditableHomeDock(
    apps: List<LauncherActivityInfo>,
    iconScale: Float,
    style: LauncherDockStyle,
    pageSize: Int,
    loopPages: Boolean,
    showLabels: Boolean,
    showSearch: Boolean,
    layoutLocked: Boolean,
    editMode: Boolean,
    activeDrag: LauncherAppDragData?,
    dragPoint: Offset?,
    onDockBoundsChanged: (Rect) -> Unit,
    dockItemBounds: MutableMap<String, Rect>,
    onBeginLocalDrag: (LauncherAppDragData, Offset) -> Unit,
    onUpdateLocalDrag: (Offset) -> Unit,
    onEndLocalDrag: (LauncherAppDragData, Offset) -> Unit,
    onCancelLocalDrag: () -> Unit,
    onLaunchApp: (LauncherActivityInfo) -> Unit,
    onManageApp: (LauncherActivityInfo, Rect?) -> Unit,
    onOpenSearch: () -> Unit,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
) {
    GlazeDock(
        apps = apps,
        iconScale = iconScale,
        style = style,
        pageSize = pageSize,
        loopPages = loopPages,
        showLabels = showLabels,
        showSearch = showSearch,
        layoutLocked = layoutLocked,
        editMode = editMode,
        activeDrag = activeDrag,
        dragPoint = dragPoint,
        onDockBoundsChanged = onDockBoundsChanged,
        dockItemBounds = dockItemBounds,
        onBeginLocalDrag = onBeginLocalDrag,
        onUpdateLocalDrag = onUpdateLocalDrag,
        onEndLocalDrag = onEndLocalDrag,
        onCancelLocalDrag = onCancelLocalDrag,
        onLaunchApp = onLaunchApp,
        onManageApp = onManageApp,
        onOpenSearch = onOpenSearch,
        onSwipeUp = onSwipeUp,
        onSwipeDown = onSwipeDown,
    )
}

@Composable
fun LauncherBetaRoot(
    apps: List<LauncherActivityInfo>,
    workspace: WorkspaceState,
    preferences: LauncherPreferences,
    drawerLayoutMode: LauncherDrawerLayoutMode,
    experiencePreferences: LauncherExperiencePreferences,
    homePageTransition: LauncherHomePageTransition = LauncherHomePageTransition.SLIDE,
    recentAppKeys: List<String>,
    localLaunchCounts: Map<String, Long>,
    hiddenHomeSuggestionKeys: Set<String>,
    hiddenAppKeys: Set<String>,
    lockedAppKeys: Set<String>,
    drawerPinnedAppKeys: Set<String>,
    drawerPinnedAppOrder: List<String>,
    drawerSortOrderName: String?,
    drawerTabs: List<LauncherDrawerTab>,
    searchProviderPreferences: com.goreecloud.launcher.core.launcher.LauncherSearchProviderPreferenceDecodeResult?,
    fileSearchRoots: List<Uri>,
    homePageCount: Int,
    selectedHomePageId: String = WorkspaceLegacyImportMapper.HOME_PAGE_ID,
    homeResetSequence: Long,
    requestedSurfaceMode: LauncherSurfaceMode = LauncherSurfaceMode.HOME,
    externalHomeEditorRequestSequence: Long = 0L,
    homeEditorInitialPageId: String? = null,
    homeLabelOverrides: Map<String, String>,
    folders: List<LauncherFolder>,
    primaryHomePage: WorkspaceRenderedHomePage?,
    homePages: List<WorkspaceRenderedHomePage>,
    onMoveFolderToPage: (LauncherFolder, String) -> Unit,
    onMoveFolderToPageCell: (LauncherFolder, String, Int, Int) -> Unit,
    onCreateFolder: (String, Boolean, LauncherActivityInfo?, Int) -> Unit,
    onRenameFolder: (String, String) -> Unit,
    onDeleteFolder: (LauncherFolder) -> Unit,
    onAddAppToFolder: (String, LauncherActivityInfo) -> Unit,
    onRemoveAppFromFolder: (String, LauncherActivityInfo) -> Unit,
    onAddFolderToHome: (LauncherFolder) -> Unit,
    onRemoveFolderFromHome: (LauncherFolder) -> Unit,
    onMoveHomeFolderToCell: (LauncherFolder, Int, Int) -> Unit,
    onManageHomePages: () -> Unit,
    onCreateHomePage: () -> Unit,
    onSelectHomePage: (String) -> Unit,
    onDeleteHomePage: (String) -> Unit,
    onSwipeHomePageLeft: () -> Boolean,
    onSwipeHomePageRight: () -> Boolean,
    isDefaultHome: Boolean,
    onRequestHomeRole: () -> Unit,
    onLaunchApp: (LauncherActivityInfo) -> Unit,
    onOpenAppInfo: (LauncherActivityInfo) -> Unit,
    onAddBuiltInWidget: (String) -> Unit,
    onSetManagedHomeSearchEnabled: (Boolean) -> Unit,
    availableAndroidWidgets: List<LauncherWidgetProviderDescriptor>,
    availableIconPacks: List<LauncherIconPackDescriptor>,
    onPickInstalledAndroidWidget: (LauncherWidgetProviderDescriptor) -> Unit,
    onPickAndroidWidget: () -> Unit,
    onCreateAndroidWidgetView: (Int) -> AppWidgetHostView?,
    onRemoveWidget: (WorkspaceRenderedHomeWidget) -> Unit,
    onResizeWidget: (WorkspaceRenderedHomeWidget, Int, Int) -> Unit,
    onMoveWidget: (WorkspaceRenderedHomeWidget, Int, Int) -> Unit,
    onMoveWidgetToPage: (WorkspaceRenderedHomeWidget, String) -> Unit,
    onMoveWidgetToPageCell: (WorkspaceRenderedHomeWidget, String, Int, Int) -> Unit,
    onToggleFavorite: (LauncherActivityInfo) -> Unit,
    onToggleDock: (LauncherActivityInfo) -> Unit,
    onMoveFavorite: (LauncherActivityInfo, WorkspaceMoveDirection) -> Unit,
    onMoveFavoriteToPage: (LauncherActivityInfo, String) -> Unit,
    onMoveFavoriteToPageCell: (LauncherActivityInfo, String, Int, Int) -> Unit,
    onMoveFavoriteToCell: (LauncherActivityInfo, Int, Int) -> Unit,
    onMoveFavoriteToDock: (LauncherActivityInfo, String?) -> Unit,
    onMoveDockToHomeCell: (LauncherActivityInfo, Int, Int) -> Unit,
    onCopyDrawerToHomeCell: (LauncherActivityInfo, Int, Int) -> Unit,
    onCopyDrawerToDock: (LauncherActivityInfo, String?) -> Unit,
    onReorderDockByDrop: (LauncherActivityInfo, String?) -> Unit,
    onMoveDock: (LauncherActivityInfo, WorkspaceMoveDirection) -> Unit,
    onSetHomeLabelOverride: (LauncherActivityInfo, String?) -> Unit,
    onSetHomeSuggestionHidden: (String, Boolean) -> Unit,
    onSetAppHidden: (String, Boolean) -> Unit,
    onSetAppLocked: (String, Boolean) -> Unit,
    onSetDrawerAppPinned: (String, Boolean) -> Unit,
    onMoveDrawerPinnedApp: (String, Int) -> Unit,
    onSetDrawerPinnedAppOrder: (List<String>) -> Unit,
    onSetDrawerSortOrderName: (String?) -> Unit,
    onCreateDrawerTab: (String) -> Unit,
    onRenameDrawerTab: (String, String) -> Unit,
    onDeleteDrawerTab: (String) -> Unit,
    onSetDrawerTabMembership: (String, String, Boolean) -> Unit,
    onRequestUninstall: (LauncherActivityInfo) -> Unit,
    themeMode: GlazeThemeMode,
    onSetThemeMode: (GlazeThemeMode) -> Unit,
    onSetHomeGrid: (Int, Int) -> Unit,
    onSetDrawerColumns: (Int) -> Unit,
    onSetDrawerLayoutMode: (LauncherDrawerLayoutMode) -> Unit,
    onSetShowLabels: (Boolean) -> Unit,
    onSetIconScale: (Float) -> Unit,
    onSetIconShape: (LauncherIconShape) -> Unit,
    onSetIconPackPackage: (String?) -> Unit,
    onSetLayoutLocked: (Boolean) -> Unit,
    onSetUniversalSearchHomeMode: (LauncherUniversalSearchHomeMode) -> Unit,
    onSetSearchProviderPreferences:
        (com.goreecloud.launcher.core.launcher.LauncherSearchProviderPreferenceSnapshot) -> Unit,
    onSetSearchProviderEnabled:
        (com.goreecloud.launcher.core.launcher.LauncherSearchProviderControlState, String, Boolean) -> Unit,
    onLaunchSearchShortcut:
        (com.goreecloud.launcher.core.launcher.LauncherLaunchShortcutSearchAction) -> Unit,
    onOpenSearchUri:
        (com.goreecloud.launcher.core.launcher.LauncherOpenUriSearchAction) -> Unit,
    onChooseFileSearchRoot: () -> Unit,
    onRemoveFileSearchRoot: (Uri) -> Unit,
    onOpenDocument:
        (com.goreecloud.launcher.core.launcher.LauncherOpenDocumentSearchAction) -> Unit,
    onSearchWithConnectedProvider: (String, String) -> Unit,
    onResetSearchProviderPreferences: () -> Unit,
    onSetHomeCardStyle: (LauncherHomeCardStyle) -> Unit,
    onSetShowHomeQuickActions: (Boolean) -> Unit,
    onSetShowHomePageIndicator: (Boolean) -> Unit,
    onSetShowHomeLabels: (Boolean) -> Unit,
    onSetShowDrawerLabels: (Boolean) -> Unit,
    onSetShowDrawerPageIndicator: (Boolean) -> Unit,
    onSetHomeAppMode: (LauncherHomeAppMode) -> Unit,
    onSetAddNewAppsToHome: (Boolean) -> Unit,
    onClearLocalUsage: () -> Unit,
    onSetHintsEnabled: (Boolean) -> Unit,
    onReplayStartupWizard: () -> Unit,
    onSetDrawerBackdrop: (LauncherDrawerBackdrop) -> Unit,
    onSetDrawerSearchPlacement: (LauncherDrawerSearchPlacement) -> Unit,
    onSetDrawerNavigation: (LauncherDrawerNavigation) -> Unit,
    onSetDrawerEntryMode: (LauncherDrawerEntryMode) -> Unit,
    onSetDrawerSpacing: (LauncherDrawerSpacing) -> Unit,
    onSetDrawerPageRows: (Int) -> Unit,
    onSetShowDrawerAppCount: (Boolean) -> Unit,
    onSetShowDrawerSuggestions: (Boolean) -> Unit,
    onSetHomeGlanceAlignment: (LauncherHomeGlanceAlignment) -> Unit,
    onSetHomeSearchPlacement: (LauncherHomeSearchPlacement) -> Unit,
    onSetHomeSearchStyle: (LauncherHomeSearchStyle) -> Unit,
    onSetHomeSpacing: (LauncherHomeSpacing) -> Unit,
    onSetDockStyle: (LauncherDockStyle) -> Unit,
    onSetDockPageSize: (Int) -> Unit,
    onSetDockLoopPages: (Boolean) -> Unit,
    onSetShowDockLabels: (Boolean) -> Unit,
    onSetShowDockSearch: (Boolean) -> Unit,
    onSetWallpaperShade: (LauncherWallpaperShade) -> Unit,
    onSetGestureSensitivity: (LauncherGestureSensitivity) -> Unit = {},
    onSetGestureAction: (LauncherHomeGesture, LauncherGestureAction) -> Unit,
    onOpenWallpaperPicker: () -> Unit,
    onSurfaceModeChanged: (LauncherSurfaceMode) -> Unit,
    onHomeEditorVisibilityChanged: (Boolean) -> Unit = {},
    onPrimaryHomeGridBoundsChanged: (Rect?) -> Unit = {},
    homeContentOnly: Boolean = false,
    secondaryHomeContent: @Composable (WorkspaceRenderedHomePage) -> Unit = {},
) {
    var surfaceModeName by rememberSaveable { mutableStateOf(requestedSurfaceMode.name) }
    val surfaceMode = runCatching { LauncherSurfaceMode.valueOf(surfaceModeName) }
        .getOrDefault(LauncherSurfaceMode.HOME)
    var selectedApp by remember { mutableStateOf<LauncherActivityInfo?>(null) }
    var selectedAppAnchor by remember { mutableStateOf<Rect?>(null) }
    var selectedAppContextOrigin by remember { mutableStateOf(LauncherAppContextOrigin.DRAWER) }
    var selectedWidget by remember { mutableStateOf<WorkspaceRenderedHomeWidget?>(null) }
    var appWidgetChoices by remember { mutableStateOf<List<LauncherWidgetProviderDescriptor>>(emptyList()) }
    var appWidgetChoiceTitle by remember { mutableStateOf<String?>(null) }
    var selectedFolderId by rememberSaveable { mutableStateOf<String?>(null) }
    var folderAppPickerId by rememberSaveable { mutableStateOf<String?>(null) }
    var showFolderManager by rememberSaveable { mutableStateOf(false) }
    var showHiddenAppsManager by rememberSaveable { mutableStateOf(false) }
    var showAppLockManager by rememberSaveable { mutableStateOf(false) }
    var folderManagerAddToHome by rememberSaveable { mutableStateOf(false) }
    val primaryFolderProfileId = remember { Process.myUserHandle().hashCode() }
    var folderManagerProfileId by rememberSaveable {
        mutableStateOf(primaryFolderProfileId)
    }
    var folderAssignmentAppKey by rememberSaveable { mutableStateOf<String?>(null) }
    var drawerTabMembershipAppKey by rememberSaveable { mutableStateOf<String?>(null) }
    val rootAppsByKey = remember(apps) {
        apps.associateBy { it.workspaceKey() }
    }
    val discoverableApps = remember(apps, hiddenAppKeys) {
        apps.filter { app ->
            LauncherAppVisibilityPolicy.isDiscoverable(
                appKey = app.workspaceKey(),
                hiddenAppKeys = hiddenAppKeys,
            )
        }
    }
    val homeFolderIds = remember(homePages) {
        homePages.flatMap { page -> page.folderPlacements.map { it.folderId } }.toSet()
    }
    var drawerSearchRequested by rememberSaveable { mutableStateOf(false) }
    var homeEditorRequestSequence by remember { mutableStateOf(0L) }
    val homeCellBounds = remember { mutableStateMapOf<Pair<Int, Int>, Rect>() }
    val dockItemBounds = remember { mutableStateMapOf<String, Rect>() }
    var dockBounds by remember { mutableStateOf<Rect?>(null) }
    var activeDrag by remember { mutableStateOf<LauncherAppDragData?>(null) }
    var dragPoint by remember { mutableStateOf<Offset?>(null) }
    var launcherBounds by remember { mutableStateOf<Rect?>(null) }
    var primaryHomeGridBounds by remember { mutableStateOf<Rect?>(null) }
    var homeEditMode by rememberSaveable { mutableStateOf(false) }
    val rootDockApps = remember(rootAppsByKey, workspace.dockKeys) {
        workspace.dockKeys.mapNotNull(rootAppsByKey::get).take(MAX_DOCK_ITEMS)
    }

    LaunchedEffect(rootDockApps.map { it.workspaceKey() }) {
        val visibleDockKeys = rootDockApps.map { it.workspaceKey() }.toSet()
        dockItemBounds.keys
            .filterNot(visibleDockKeys::contains)
            .forEach(dockItemBounds::remove)
    }

    DisposableEffect(Unit) {
        onDispose { onPrimaryHomeGridBoundsChanged(null) }
    }
    val crossPageEdgeThresholdPx = with(LocalDensity.current) { 36.dp.toPx() }

    LaunchedEffect(requestedSurfaceMode) {
        if (surfaceModeName != requestedSurfaceMode.name) {
            surfaceModeName = requestedSurfaceMode.name
        }
    }
    LaunchedEffect(externalHomeEditorRequestSequence) {
        if (externalHomeEditorRequestSequence > 0L) {
            surfaceModeName = LauncherSurfaceMode.HOME.name
            homeEditorRequestSequence += 1L
        }
    }

    val currentApps by rememberUpdatedState(apps)
    val currentMoveFavoriteToPageCell by rememberUpdatedState(onMoveFavoriteToPageCell)
    val currentMoveFavoriteToCell by rememberUpdatedState(onMoveFavoriteToCell)
    val currentMoveFavoriteToDock by rememberUpdatedState(onMoveFavoriteToDock)
    val currentMoveDockToHomeCell by rememberUpdatedState(onMoveDockToHomeCell)
    val currentCopyDrawerToHomeCell by rememberUpdatedState(onCopyDrawerToHomeCell)
    val currentCopyDrawerToDock by rememberUpdatedState(onCopyDrawerToDock)
    val currentReorderDockByDrop by rememberUpdatedState(onReorderDockByDrop)

    val routeAppDrop: (LauncherAppDragData, Offset) -> Boolean = { drag, point ->
        val app = currentApps.firstOrNull { it.workspaceKey() == drag.appKey }
        if (app == null) {
            false
        } else {
            val dock = dockBounds
            if (dock != null && dock.contains(point)) {
                val targetDockKey = dockInsertionTarget(
                    dropX = point.x,
                    sourceKey = drag.appKey,
                    bounds = dockItemBounds,
                )
                when (drag.origin) {
                    LauncherAppDragOrigin.HOME ->
                        currentMoveFavoriteToDock(app, targetDockKey)
                    LauncherAppDragOrigin.DOCK ->
                        currentReorderDockByDrop(app, targetDockKey)
                    LauncherAppDragOrigin.DRAWER ->
                        currentCopyDrawerToDock(app, targetDockKey)
                }
                true
            } else {
                val edgeTarget = if (drag.origin == LauncherAppDragOrigin.HOME) {
                    (primaryHomeGridBounds ?: launcherBounds)?.let { bounds ->
                        homePageEdgeDropTarget(
                            pages = homePages,
                            currentPageId = WorkspaceLegacyImportMapper.HOME_PAGE_ID,
                            dropX = point.x,
                            dropY = point.y,
                            surfaceLeftPx = bounds.left,
                            surfaceTopPx = bounds.top,
                            surfaceRightPx = bounds.right,
                            surfaceBottomPx = bounds.bottom,
                            edgeThresholdPx = crossPageEdgeThresholdPx,
                            columns = preferences.homeColumns,
                            rows = preferences.homeRows,
                        )
                    }
                } else {
                    null
                }
                if (edgeTarget != null) {
                    currentMoveFavoriteToPageCell(
                        app,
                        edgeTarget.pageId,
                        edgeTarget.cellX,
                        edgeTarget.cellY,
                    )
                    true
                } else {
                    val targetCell = nearestHomeCell(point, homeCellBounds)
                    if (targetCell == null) {
                        false
                    } else {
                        when (drag.origin) {
                            LauncherAppDragOrigin.HOME ->
                                currentMoveFavoriteToCell(app, targetCell.first, targetCell.second)
                            LauncherAppDragOrigin.DOCK ->
                                currentMoveDockToHomeCell(app, targetCell.first, targetCell.second)
                            LauncherAppDragOrigin.DRAWER ->
                                currentCopyDrawerToHomeCell(app, targetCell.first, targetCell.second)
                        }
                        true
                    }
                }
            }
        }
    }

    val launcherDragTarget = remember {
        object : DragAndDropTarget {
            private fun accepts(drag: LauncherAppDragData): Boolean =
                drag.origin != LauncherAppDragOrigin.HOME ||
                    drag.sourcePageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID

            override fun onStarted(event: DragAndDropEvent) {
                val drag = event.launcherAppDragData() ?: return
                if (!accepts(drag)) return
                selectedApp = null
                selectedAppAnchor = null
                homeEditMode = true
                activeDrag = drag
                dragPoint = null
                if (drag.origin == LauncherAppDragOrigin.DRAWER) {
                    drawerSearchRequested = false
                    surfaceModeName = LauncherSurfaceMode.HOME.name
                }
            }

            override fun onMoved(event: DragAndDropEvent) {
                val drag = event.launcherAppDragData() ?: return
                if (accepts(drag) && activeDrag?.appKey == drag.appKey) {
                    dragPoint = event.rootDropPoint()
                }
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                val drag = activeDrag ?: event.launcherAppDragData() ?: return false
                if (!accepts(drag)) return false
                val point = event.rootDropPoint()
                dragPoint = point
                return routeAppDrop(drag, point)
            }

            override fun onEnded(event: DragAndDropEvent) {
                val drag = event.launcherAppDragData()
                if (drag == null || accepts(drag)) {
                    activeDrag = null
                    dragPoint = null
                }
            }
        }
    }

    val beginLocalDrag: (LauncherAppDragData, Offset) -> Unit = { drag, point ->
        selectedApp = null
        selectedAppAnchor = null
        homeEditMode = true
        activeDrag = drag
        dragPoint = point
    }
    val updateLocalDrag: (Offset) -> Unit = { point ->
        dragPoint = point
    }
    val endLocalDrag: (LauncherAppDragData, Offset) -> Unit = { drag, point ->
        dragPoint = point
        routeAppDrop(drag, point)
        activeDrag = null
        dragPoint = null
    }
    val cancelLocalDrag: () -> Unit = {
        activeDrag = null
        dragPoint = null
    }

    LaunchedEffect(homeResetSequence) {
        drawerSearchRequested = false
        selectedApp = null
        selectedAppAnchor = null
        selectedWidget = null
        selectedFolderId = null
        folderAppPickerId = null
        showFolderManager = false
        folderAssignmentAppKey = null
        homeEditMode = false
        activeDrag = null
        dragPoint = null
        surfaceModeName = LauncherSurfaceMode.HOME.name
    }

    LaunchedEffect(surfaceMode) { onSurfaceModeChanged(surfaceMode) }

    val presentationContext = LocalGlazeV16PresentationContext.current
    val surfaceTransitionMotionMode = remember(presentationContext) {
        GlazeV16PresentationPolicy.resolve(
            requestedMaterial = GlazeV16MaterialRole.RAISED,
            context = presentationContext,
        ).motionMode
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { launcherBounds = it.boundsInRoot() }
            .dragAndDropTarget(
                shouldStartDragAndDrop = { event ->
                    event.launcherAppDragData()?.let { drag ->
                        drag.origin != LauncherAppDragOrigin.HOME ||
                            drag.sourcePageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID
                    } == true
                },
                target = launcherDragTarget,
            ),
    ) {
        AnimatedContent(
            targetState = surfaceMode,
            transitionSpec = {
            val profile = LauncherSurfaceTransitionPolicy.resolve(
                initial = initialState,
                target = targetState,
                motionMode = surfaceTransitionMotionMode,
            )
            when {
                !profile.spatial ->
                    fadeIn(animationSpec = tween(durationMillis = profile.enterDurationMillis)) togetherWith
                        fadeOut(animationSpec = tween(durationMillis = profile.exitDurationMillis))

                initialState == LauncherSurfaceMode.HOME &&
                    targetState == LauncherSurfaceMode.DRAWER ->
                    (
                        slideInVertically(
                            animationSpec = tween(durationMillis = profile.enterDurationMillis),
                            initialOffsetY = { height -> height / profile.enterOffsetDivisor },
                        ) + fadeIn(animationSpec = tween(durationMillis = profile.enterDurationMillis))
                    ) togetherWith (
                        slideOutVertically(
                            animationSpec = tween(durationMillis = profile.exitDurationMillis),
                            targetOffsetY = { height -> -height / profile.exitOffsetDivisor },
                        ) + fadeOut(animationSpec = tween(durationMillis = profile.exitDurationMillis))
                    )

                initialState == LauncherSurfaceMode.DRAWER &&
                    targetState == LauncherSurfaceMode.HOME ->
                    (
                        slideInVertically(
                            animationSpec = tween(durationMillis = profile.enterDurationMillis),
                            initialOffsetY = { height -> -height / profile.enterOffsetDivisor },
                        ) + fadeIn(animationSpec = tween(durationMillis = profile.enterDurationMillis))
                    ) togetherWith (
                        slideOutVertically(
                            animationSpec = tween(durationMillis = profile.exitDurationMillis),
                            targetOffsetY = { height -> height / profile.exitOffsetDivisor },
                        ) + fadeOut(animationSpec = tween(durationMillis = profile.exitDurationMillis))
                    )

                else ->
                    fadeIn(animationSpec = tween(durationMillis = profile.enterDurationMillis)) togetherWith
                        fadeOut(animationSpec = tween(durationMillis = profile.exitDurationMillis))
            }
        },
    ) { targetSurfaceMode ->
        when (targetSurfaceMode) {
            LauncherSurfaceMode.HOME -> {
                val selectedSecondaryPage = selectedSecondaryHomePage(
                    selectedHomePageId = selectedHomePageId,
                    pages = homePages,
                )
                val pagerPages = homePages
                val unifiedPagerEnabled = pagerPages.size > 1
                val dispatchPagerBoundaryGesture: (LauncherGestureAction) -> Unit = { action ->
                    dispatchLauncherHomeGestureAction(
                        action = action,
                        appsByKey = rootAppsByKey,
                        onOpenApps = {
                            drawerSearchRequested =
                                experiencePreferences.drawerSearchPlacement !=
                                    LauncherDrawerSearchPlacement.OFF &&
                                    experiencePreferences.drawerEntryMode ==
                                        LauncherDrawerEntryMode.SEARCH_FIRST
                            surfaceModeName = LauncherSurfaceMode.DRAWER.name
                        },
                        onOpenSearch = {
                            drawerSearchRequested = false
                            surfaceModeName = LauncherSurfaceMode.SEARCH.name
                        },
                        onOpenHomeEditor = {
                            if (selectedSecondaryPage != null) {
                                onSelectHomePage(WorkspaceLegacyImportMapper.HOME_PAGE_ID)
                            }
                            homeEditorRequestSequence += 1L
                        },
                        onOpenWallpaperPicker = onOpenWallpaperPicker,
                        onOpenThemeManager = {
                            surfaceModeName = LauncherSurfaceMode.THEME_MANAGER.name
                        },
                        onLaunchApp = onLaunchApp,
                    )
                }
                val primaryHomeContent: @Composable (Boolean) -> Unit = { pagingHostedExternally ->
                    HomeSurface(
                        apps = apps,
                        workspace = workspace,
                        preferences = preferences,
                        experiencePreferences = experiencePreferences,
                        homePageTransition = homePageTransition,
                        recentAppKeys = recentAppKeys,
                        localLaunchCounts = localLaunchCounts,
                        hiddenHomeSuggestionKeys = hiddenHomeSuggestionKeys,
                        homePageCount = homePageCount,
                        selectedHomePageId = selectedHomePageId,
                        homeResetSequence = homeResetSequence,
                        homeEditorRequestSequence = homeEditorRequestSequence,
                        homeEditorInitialPageId = homeEditorInitialPageId,
                        homeLabelOverrides = homeLabelOverrides,
                        folders = folders,
                        primaryHomePage = primaryHomePage,
                        homePages = homePages,
                        editMode = homeEditMode,
                        activeDrag = activeDrag,
                        dragPoint = dragPoint,
                        homeCellBounds = homeCellBounds,
                        dockItemBounds = dockItemBounds,
                        onHomeGridBoundsChanged = {
                            primaryHomeGridBounds = it
                            onPrimaryHomeGridBoundsChanged(it)
                        },
                        onDockBoundsChanged = { dockBounds = it },
                        onBeginLocalDrag = beginLocalDrag,
                        onUpdateLocalDrag = updateLocalDrag,
                        onEndLocalDrag = endLocalDrag,
                        onCancelLocalDrag = cancelLocalDrag,
                        onExitEditMode = {
                            homeEditMode = false
                            selectedApp = null
                            selectedAppAnchor = null
                            selectedWidget = null
                            activeDrag = null
                            dragPoint = null
                        },
                        onManageHomePages = onManageHomePages,
                        onCreateHomePage = onCreateHomePage,
                        onSelectHomePage = onSelectHomePage,
                        onDeleteHomePage = onDeleteHomePage,
                        onSwipeHomePageLeft = onSwipeHomePageLeft,
                        onSwipeHomePageRight = onSwipeHomePageRight,
                        onManageFolders = {
                            folderManagerProfileId = primaryFolderProfileId
                            folderManagerAddToHome = true
                            showFolderManager = true
                        },
                        onOpenFolder = { folder -> selectedFolderId = folder.id },
                        onMoveFavoriteToCell = onMoveFavoriteToCell,
                        onMoveWidget = onMoveWidget,
                        onMoveWidgetToPageCell = onMoveWidgetToPageCell,
                        onMoveHomeFolderToCell = onMoveHomeFolderToCell,
                        onMoveHomeFolderToPageCell = onMoveFolderToPageCell,
                        onLaunchApp = onLaunchApp,
                        onAddBuiltInWidget = onAddBuiltInWidget,
                        onSetManagedHomeSearchEnabled = onSetManagedHomeSearchEnabled,
                        availableAndroidWidgets = availableAndroidWidgets,
                        onPickInstalledAndroidWidget = onPickInstalledAndroidWidget,
                        onPickAndroidWidget = onPickAndroidWidget,
                        onCreateAndroidWidgetView = onCreateAndroidWidgetView,
                        onManageWidget = {
                            homeEditMode = true
                            selectedApp = null
                            selectedAppAnchor = null
                            selectedWidget = it
                        },
                        onOpenLauncherSearch = {
                            drawerSearchRequested = false
                            surfaceModeName = LauncherSurfaceMode.SEARCH.name
                        },
                        onManageApp = { app, anchor, origin ->
                            homeEditMode = true
                            selectedWidget = null
                            selectedApp = app
                            selectedAppAnchor = anchor
                            selectedAppContextOrigin = origin
                        },
                        onOpenDrawer = {
                            drawerSearchRequested =
                                experiencePreferences.drawerSearchPlacement !=
                                    LauncherDrawerSearchPlacement.OFF &&
                                    experiencePreferences.drawerEntryMode ==
                                        LauncherDrawerEntryMode.SEARCH_FIRST
                            surfaceModeName = LauncherSurfaceMode.DRAWER.name
                        },
                        onOpenSettings = { surfaceModeName = LauncherSurfaceMode.SETTINGS.name },
                        onOpenThemeManager = { surfaceModeName = LauncherSurfaceMode.THEME_MANAGER.name },
                        onOpenWallpaperPicker = onOpenWallpaperPicker,
                        onSetHomeCardStyle = onSetHomeCardStyle,
                        onHomeEditorVisibilityChanged = onHomeEditorVisibilityChanged,
                        contentOnly = homeContentOnly,
                        dockHostedExternally = true,
                        horizontalPagingHostedExternally = pagingHostedExternally,
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                ) {
                    if (unifiedPagerEnabled) {
                        val pagerState = rememberPagerState(
                            initialPage = launcherHomePagerSelectedIndex(
                                selectedHomePageId = selectedHomePageId,
                                pages = pagerPages,
                            ),
                            pageCount = { pagerPages.size },
                        )

                        LaunchedEffect(selectedHomePageId, pagerPages.map { it.pageId }) {
                            val targetIndex = launcherHomePagerSelectedIndex(
                                selectedHomePageId = selectedHomePageId,
                                pages = pagerPages,
                            )
                            if (targetIndex != pagerState.currentPage) {
                                pagerState.scrollToPage(targetIndex)
                            }
                        }
                        LaunchedEffect(pagerState.currentPage, pagerPages, selectedHomePageId) {
                            val targetPageId = pagerPages.getOrNull(pagerState.currentPage)?.pageId
                            if (targetPageId != null && targetPageId != selectedHomePageId) {
                                onSelectHomePage(targetPageId)
                            }
                        }

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .testTag("launcher-home-unified-pager")
                                .launcherHomePagerBoundaryGestureNavigation(
                                    enabled = activeDrag == null,
                                    currentPageIndex = { pagerState.currentPage },
                                    pageCount = pagerPages.size,
                                    onSwipeLeft = {
                                        dispatchPagerBoundaryGesture(
                                            experiencePreferences.swipeLeftAction,
                                        )
                                    },
                                    onSwipeRight = {
                                        dispatchPagerBoundaryGesture(
                                            experiencePreferences.swipeRightAction,
                                        )
                                    },
                                )
                                .launcherHomePagerVerticalGestureNavigation(
                                    enabled = activeDrag == null,
                                    sensitivity = experiencePreferences.gestureSensitivity,
                                    onSwipeUp = {
                                        dispatchPagerBoundaryGesture(
                                            experiencePreferences.swipeUpAction,
                                        )
                                    },
                                    onSwipeDown = {
                                        dispatchPagerBoundaryGesture(
                                            experiencePreferences.swipeDownAction,
                                        )
                                    },
                                ),
                            userScrollEnabled = activeDrag == null,
                            beyondViewportPageCount =
                                launcherHomeBeyondViewportPageCount(pagerPages.size),
                        ) { pageIndex ->
                            val page = pagerPages[pageIndex]
                            if (page.pageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID) {
                                primaryHomeContent(true)
                            } else {
                                secondaryHomeContent(page)
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                        ) {
                            primaryHomeContent(false)
                        }
                    }

                    if (
                        rootDockApps.isNotEmpty() ||
                        activeDrag != null ||
                        experiencePreferences.showDockSearch
                    ) {
                        EditableHomeDock(
                            apps = rootDockApps,
                            iconScale = preferences.iconScale,
                            style = experiencePreferences.dockStyle,
                            pageSize = experiencePreferences.dockPageSize,
                            loopPages = experiencePreferences.dockLoopPages,
                            showLabels = experiencePreferences.showDockLabels,
                            showSearch = experiencePreferences.showDockSearch,
                            layoutLocked = preferences.layoutLocked,
                            editMode = homeEditMode,
                            activeDrag = activeDrag,
                            dragPoint = dragPoint,
                            onDockBoundsChanged = { dockBounds = it },
                            dockItemBounds = dockItemBounds,
                            onBeginLocalDrag = beginLocalDrag,
                            onUpdateLocalDrag = updateLocalDrag,
                            onEndLocalDrag = endLocalDrag,
                            onCancelLocalDrag = cancelLocalDrag,
                            onLaunchApp = onLaunchApp,
                            onManageApp = { app, anchor ->
                                homeEditMode = true
                                selectedWidget = null
                                selectedApp = app
                                selectedAppAnchor = anchor
                                selectedAppContextOrigin = LauncherAppContextOrigin.DOCK
                            },
                            onOpenSearch = {
                                drawerSearchRequested = false
                                surfaceModeName = LauncherSurfaceMode.SEARCH.name
                            },
                            onSwipeUp = {
                                dispatchLauncherHomeGestureAction(
                                    action = experiencePreferences.swipeUpAction,
                                    appsByKey = rootAppsByKey,
                                    onOpenApps = {
                                        drawerSearchRequested =
                                            experiencePreferences.drawerSearchPlacement !=
                                                LauncherDrawerSearchPlacement.OFF &&
                                                experiencePreferences.drawerEntryMode ==
                                                LauncherDrawerEntryMode.SEARCH_FIRST
                                        surfaceModeName = LauncherSurfaceMode.DRAWER.name
                                    },
                                    onOpenSearch = {
                                        drawerSearchRequested = false
                                        surfaceModeName = LauncherSurfaceMode.SEARCH.name
                                    },
                                    onOpenHomeEditor = {
                                        if (selectedSecondaryPage != null) {
                                            onSelectHomePage(WorkspaceLegacyImportMapper.HOME_PAGE_ID)
                                        }
                                        homeEditorRequestSequence += 1L
                                    },
                                    onOpenWallpaperPicker = onOpenWallpaperPicker,
                                    onOpenThemeManager = {
                                        surfaceModeName = LauncherSurfaceMode.THEME_MANAGER.name
                                    },
                                    onLaunchApp = onLaunchApp,
                                )
                            },
                            onSwipeDown = {
                                dispatchLauncherHomeGestureAction(
                                    action = experiencePreferences.swipeDownAction,
                                    appsByKey = rootAppsByKey,
                                    onOpenApps = {
                                        drawerSearchRequested =
                                            experiencePreferences.drawerSearchPlacement !=
                                                LauncherDrawerSearchPlacement.OFF &&
                                                experiencePreferences.drawerEntryMode ==
                                                LauncherDrawerEntryMode.SEARCH_FIRST
                                        surfaceModeName = LauncherSurfaceMode.DRAWER.name
                                    },
                                    onOpenSearch = {
                                        drawerSearchRequested = false
                                        surfaceModeName = LauncherSurfaceMode.SEARCH.name
                                    },
                                    onOpenHomeEditor = {
                                        if (selectedSecondaryPage != null) {
                                            onSelectHomePage(WorkspaceLegacyImportMapper.HOME_PAGE_ID)
                                        }
                                        homeEditorRequestSequence += 1L
                                    },
                                    onOpenWallpaperPicker = onOpenWallpaperPicker,
                                    onOpenThemeManager = {
                                        surfaceModeName = LauncherSurfaceMode.THEME_MANAGER.name
                                    },
                                    onLaunchApp = onLaunchApp,
                                )
                            },
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                }
            }
            LauncherSurfaceMode.SEARCH -> LauncherProviderControlledSearchSurface(
                apps = discoverableApps,
                recentAppKeys = recentAppKeys,
                localLaunchCounts = localLaunchCounts,
                searchProviderPreferences = searchProviderPreferences,
                fileSearchRoots = fileSearchRoots,
                onSetSearchProviderPreferences = onSetSearchProviderPreferences,
                onSetSearchProviderEnabled = onSetSearchProviderEnabled,
                onChooseFileSearchRoot = onChooseFileSearchRoot,
                onRemoveFileSearchRoot = onRemoveFileSearchRoot,
                onResetSearchProviderPreferences = onResetSearchProviderPreferences,
                onLaunchApp = onLaunchApp,
                onLaunchShortcut = onLaunchSearchShortcut,
                onOpenSearchUri = onOpenSearchUri,
                onOpenDocument = onOpenDocument,
                onSearchWithConnectedProvider = onSearchWithConnectedProvider,
                onNavigate = { destination ->
                    when (destination) {
                        LauncherSearchDestination.HOME -> {
                            drawerSearchRequested = false
                            surfaceModeName = LauncherSurfaceMode.HOME.name
                        }
                        LauncherSearchDestination.APPS -> {
                            drawerSearchRequested = false
                            surfaceModeName = LauncherSurfaceMode.DRAWER.name
                        }
                        LauncherSearchDestination.SETTINGS -> {
                            homeEditorRequestSequence += 1L
                            surfaceModeName = LauncherSurfaceMode.HOME.name
                        }
                        LauncherSearchDestination.HOME_EDITOR -> {
                            homeEditorRequestSequence += 1L
                            surfaceModeName = LauncherSurfaceMode.HOME.name
                        }
                        LauncherSearchDestination.WALLPAPER -> {
                            surfaceModeName = LauncherSurfaceMode.HOME.name
                            onOpenWallpaperPicker()
                        }
                        LauncherSearchDestination.THEME_MANAGER -> {
                            surfaceModeName = LauncherSurfaceMode.THEME_MANAGER.name
                        }
                    }
                },
                onBack = {
                    surfaceModeName = LauncherSurfaceMode.HOME.name
                },
            )
            LauncherSurfaceMode.DRAWER -> AppDrawerSurface(
                apps = discoverableApps,
                folders = folders,
                recentAppKeys = recentAppKeys,
                localLaunchCounts = localLaunchCounts,
                pinnedAppKeys = drawerPinnedAppKeys,
                pinnedAppOrder = drawerPinnedAppOrder,
                lockedAppKeys = lockedAppKeys,
                sortOrderName = drawerSortOrderName,
                drawerTabs = drawerTabs,
                preferences = preferences,
                drawerLayoutMode = drawerLayoutMode,
                experiencePreferences = experiencePreferences,
                focusSearch = drawerSearchRequested,
                onLaunchApp = onLaunchApp,
                onManageApp = { app, anchor ->
                    selectedApp = app
                    selectedAppAnchor = anchor
                    selectedAppContextOrigin = LauncherAppContextOrigin.DRAWER
                },
                onOpenFolder = { folder -> selectedFolderId = folder.id },
                onManageFolders = { profileId ->
                    folderManagerProfileId = profileId
                    folderManagerAddToHome = profileId == primaryFolderProfileId
                    showFolderManager = true
                },
                onSetSortOrderName = onSetDrawerSortOrderName,
                onSetDrawerLayoutMode = onSetDrawerLayoutMode,
                onOpenSettings = {
                    drawerSearchRequested = false
                    surfaceModeName = LauncherSurfaceMode.SETTINGS.name
                },
                onCreateDrawerTab = onCreateDrawerTab,
                onRenameDrawerTab = onRenameDrawerTab,
                onDeleteDrawerTab = onDeleteDrawerTab,
                onHome = {
                    drawerSearchRequested = false
                    surfaceModeName = LauncherSurfaceMode.HOME.name
                },
            )
            LauncherSurfaceMode.SETTINGS -> LauncherSettingsSurface(
                selectedThemeMode = themeMode,
                onSelectThemeMode = onSetThemeMode,
                rootContent = { onOpenThemeManager ->
                    LauncherSettingsRootSurface(
                        apps = apps,
                        preferences = preferences,
                        drawerLayoutMode = drawerLayoutMode,
                        drawerSortOrderName = drawerSortOrderName,
                        experiencePreferences = experiencePreferences,
                        availableIconPacks = availableIconPacks,
                        themeMode = themeMode,
                        isDefaultHome = isDefaultHome,
                        onRequestHomeRole = onRequestHomeRole,
                        onManageFolders = {
                            folderManagerProfileId = primaryFolderProfileId
                            folderManagerAddToHome = false
                            showFolderManager = true
                        },
                        hiddenAppCount = hiddenAppKeys.count(rootAppsByKey::containsKey),
                        onManageHiddenApps = { showHiddenAppsManager = true },
                        lockedAppCount = lockedAppKeys.count(rootAppsByKey::containsKey),
                        onManageAppLock = { showAppLockManager = true },
                        onSetHomeGrid = onSetHomeGrid,
                        onSetDrawerColumns = onSetDrawerColumns,
                        onSetDrawerLayoutMode = onSetDrawerLayoutMode,
                        onSetDrawerSortOrderName = onSetDrawerSortOrderName,
                        onSetShowLabels = onSetShowLabels,
                        onSetIconScale = onSetIconScale,
                        onSetIconShape = onSetIconShape,
                        onSetIconPackPackage = onSetIconPackPackage,
                        onSetLayoutLocked = onSetLayoutLocked,
                        onSetUniversalSearchHomeMode = onSetUniversalSearchHomeMode,
                        onSetHomeCardStyle = onSetHomeCardStyle,
                        onSetShowHomeQuickActions = onSetShowHomeQuickActions,
                        onSetShowHomePageIndicator = onSetShowHomePageIndicator,
                        onSetShowHomeLabels = onSetShowHomeLabels,
                        onSetShowDrawerLabels = onSetShowDrawerLabels,
                        onSetShowDrawerPageIndicator = onSetShowDrawerPageIndicator,
                        onSetHomeAppMode = onSetHomeAppMode,
                        onSetAddNewAppsToHome = onSetAddNewAppsToHome,
                        onClearLocalUsage = onClearLocalUsage,
                        onSetHintsEnabled = onSetHintsEnabled,
                        onReplayStartupWizard = onReplayStartupWizard,
                        onSetDrawerBackdrop = onSetDrawerBackdrop,
                        onSetDrawerSearchPlacement = onSetDrawerSearchPlacement,
                        onSetDrawerNavigation = onSetDrawerNavigation,
                        onSetDrawerEntryMode = onSetDrawerEntryMode,
                        onSetDrawerSpacing = onSetDrawerSpacing,
                        onSetDrawerPageRows = onSetDrawerPageRows,
                        onSetShowDrawerAppCount = onSetShowDrawerAppCount,
                        onSetShowDrawerSuggestions = onSetShowDrawerSuggestions,
                        onSetHomeGlanceAlignment = onSetHomeGlanceAlignment,
                        onSetHomeSearchPlacement = onSetHomeSearchPlacement,
                        onSetHomeSearchStyle = onSetHomeSearchStyle,
                        onSetHomeSpacing = onSetHomeSpacing,
                        onSetDockStyle = onSetDockStyle,
                        onSetDockPageSize = onSetDockPageSize,
                        onSetDockLoopPages = onSetDockLoopPages,
                        onSetShowDockLabels = onSetShowDockLabels,
                        onSetShowDockSearch = onSetShowDockSearch,
                        onSetWallpaperShade = onSetWallpaperShade,
                        onSetGestureSensitivity = onSetGestureSensitivity,
                        onSetGestureAction = onSetGestureAction,
                        onOpenThemeManager = onOpenThemeManager,
                        onBack = { surfaceModeName = LauncherSurfaceMode.HOME.name },
                    )
                },
            )
            LauncherSurfaceMode.THEME_MANAGER -> ThemeManagerSurface(
                selectedMode = themeMode,
                onSelectMode = onSetThemeMode,
                onBack = { surfaceModeName = LauncherSurfaceMode.HOME.name },
            )
        }
    }

    }

    if (activeDrag == null) selectedApp?.let { app ->
        selectedAppAnchor?.let { anchor ->
            val appKey = app.workspaceKey()
            val pinnedIndex = drawerPinnedAppOrder.indexOf(appKey)
            val dockIndex = workspace.dockKeys.indexOf(appKey)
            AppContextPopup(
                app = app,
                anchor = anchor,
                contextOrigin = selectedAppContextOrigin,
                workspace = workspace,
                layoutLocked = preferences.layoutLocked,
                drawerPinned = appKey in drawerPinnedAppKeys,
                canMoveDrawerPinnedEarlier =
                    selectedAppContextOrigin == LauncherAppContextOrigin.DRAWER && pinnedIndex > 0,
                canMoveDrawerPinnedLater =
                    selectedAppContextOrigin == LauncherAppContextOrigin.DRAWER &&
                        pinnedIndex >= 0 &&
                        pinnedIndex < drawerPinnedAppOrder.lastIndex,
                canResetDrawerPinnedOrder =
                    selectedAppContextOrigin == LauncherAppContextOrigin.DRAWER &&
                        drawerPinnedAppKeys.size > 1,
                hasDrawerTabs = drawerTabs.isNotEmpty(),
                canMoveDockEarlier =
                    selectedAppContextOrigin == LauncherAppContextOrigin.DOCK && dockIndex > 0,
                canMoveDockLater =
                    selectedAppContextOrigin == LauncherAppContextOrigin.DOCK &&
                        dockIndex >= 0 &&
                        dockIndex < workspace.dockKeys.lastIndex,
                hiddenFromLauncher = appKey in hiddenAppKeys,
                lockedByLauncher = appKey in lockedAppKeys,
                availableAndroidWidgets = availableAndroidWidgets,
                onHomeAction = {
                    if (
                        selectedAppContextOrigin == LauncherAppContextOrigin.HOME &&
                        appKey !in workspace.favoriteKeys
                    ) {
                        onSetHomeSuggestionHidden(appKey, true)
                    } else {
                        onToggleFavorite(app)
                    }
                    selectedApp = null
                    selectedAppAnchor = null
                },
                onToggleDock = {
                    onToggleDock(app)
                    selectedApp = null
                    selectedAppAnchor = null
                },
                onOpenAppInfo = {
                    onOpenAppInfo(app)
                    selectedApp = null
                    selectedAppAnchor = null
                },
                onRequestUninstall = {
                    onRequestUninstall(app)
                    selectedApp = null
                    selectedAppAnchor = null
                },
                onToggleDrawerPinned = {
                    onSetDrawerAppPinned(
                        appKey,
                        appKey !in drawerPinnedAppKeys,
                    )
                    selectedApp = null
                    selectedAppAnchor = null
                },
                onMoveDrawerPinnedEarlier = {
                    onMoveDrawerPinnedApp(appKey, -1)
                    selectedApp = null
                    selectedAppAnchor = null
                },
                onMoveDrawerPinnedLater = {
                    onMoveDrawerPinnedApp(appKey, 1)
                    selectedApp = null
                    selectedAppAnchor = null
                },
                onMoveDockEarlier = {
                    onMoveDock(app, WorkspaceMoveDirection.EARLIER)
                    selectedApp = null
                    selectedAppAnchor = null
                },
                onMoveDockLater = {
                    onMoveDock(app, WorkspaceMoveDirection.LATER)
                    selectedApp = null
                    selectedAppAnchor = null
                },
                onResetDrawerPinnedOrder = {
                    val alphabeticalOrder = drawerPinnedAppKeys
                        .mapNotNull { key ->
                            rootAppsByKey[key]?.let { pinnedApp ->
                                key to pinnedApp.label.toString()
                            }
                        }
                        .sortedWith(
                            compareBy<Pair<String, String>> {
                                it.second.lowercase(java.util.Locale.ROOT)
                            }.thenBy { it.first },
                        )
                        .map { it.first }
                    onSetDrawerPinnedAppOrder(alphabeticalOrder)
                    selectedApp = null
                    selectedAppAnchor = null
                },
                onManageDrawerTabs = {
                    drawerTabMembershipAppKey = appKey
                    selectedApp = null
                    selectedAppAnchor = null
                },
                onToggleHidden = {
                    onSetAppHidden(appKey, appKey !in hiddenAppKeys)
                    selectedApp = null
                    selectedAppAnchor = null
                },
                onToggleLocked = {
                    onSetAppLocked(appKey, appKey !in lockedAppKeys)
                    selectedApp = null
                    selectedAppAnchor = null
                },
                onAddToFolder = {
                    selectedApp = null
                    selectedAppAnchor = null
                    val appProfileId = app.user.hashCode()
                    val compatibleFolders = folders.filter { folder ->
                        LauncherFolderProfilePolicy.belongsToProfile(
                            folder = folder,
                            profileId = appProfileId,
                            primaryProfileId = primaryFolderProfileId,
                        )
                    }
                    folderAssignmentAppKey = appKey
                    if (compatibleFolders.isEmpty()) {
                        folderManagerProfileId = appProfileId
                        folderManagerAddToHome = appProfileId == primaryFolderProfileId
                        showFolderManager = true
                    }
                },
                onOpenWidgets = { providers ->
                    selectedApp = null
                    selectedAppAnchor = null
                    appWidgetChoices = providers
                    appWidgetChoiceTitle = app.label.toString()
                },
                onLaunchShortcut = { action ->
                    onLaunchSearchShortcut(action)
                    selectedApp = null
                    selectedAppAnchor = null
                },
                onClose = {
                    selectedApp = null
                    selectedAppAnchor = null
                },
            )
        }
    }

    if (activeDrag == null) {
        drawerTabMembershipAppKey
            ?.let(rootAppsByKey::get)
            ?.let { app ->
                LauncherDrawerTabMembershipDialog(
                    app = app,
                    tabs = drawerTabs,
                    onSetMembership = { tabId, enabled ->
                        onSetDrawerTabMembership(tabId, app.workspaceKey(), enabled)
                    },
                    onClose = { drawerTabMembershipAppKey = null },
                )
            }
    }

    if (activeDrag == null && appWidgetChoices.isNotEmpty()) {
        LauncherAppWidgetChoicesDialog(
            appLabel = appWidgetChoiceTitle.orEmpty(),
            providers = appWidgetChoices,
            apps = apps,
            onChoose = { descriptor ->
                appWidgetChoices = emptyList()
                appWidgetChoiceTitle = null
                onPickInstalledAndroidWidget(descriptor)
            },
            onClose = {
                appWidgetChoices = emptyList()
                appWidgetChoiceTitle = null
            },
        )
    }

    if (activeDrag == null) selectedWidget?.let { widget ->
        LauncherWidgetManagementDialog(
            widget = widget,
            columns = preferences.homeColumns,
            rows = preferences.homeRows,
            layoutLocked = preferences.layoutLocked,
            onResize = { spanX, spanY ->
                onResizeWidget(widget, spanX, spanY)
                selectedWidget = null
            },
            onRemove = {
                onRemoveWidget(widget)
                selectedWidget = null
            },
            onMove = { cellX, cellY ->
                onMoveWidget(widget, cellX, cellY)
                selectedWidget = null
            },
            moveTargets = homeMoveTargetPages(
                pages = homePages,
                currentPageId = WorkspaceLegacyImportMapper.HOME_PAGE_ID,
            ),
            onMoveToPage = { targetPageId ->
                onMoveWidgetToPage(widget, targetPageId)
                selectedWidget = null
            },
            onClose = { selectedWidget = null },
        )
    }

    selectedFolderId
        ?.let { id -> folders.firstOrNull { it.id == id } }
        ?.let { folder ->
            val folderProfileId = folder.profileId ?: primaryFolderProfileId
            val allowHomePlacement = folderProfileId == primaryFolderProfileId
            LauncherFolderContentsSheet(
                folder = folder,
                appsByKey = rootAppsByKey,
                isOnHome = allowHomePlacement && folder.id in homeFolderIds,
                allowHomePlacement = allowHomePlacement,
                onLaunchApp = onLaunchApp,
                onRemoveApp = { app -> onRemoveAppFromFolder(folder.id, app) },
                onAddApps = {
                    selectedFolderId = null
                    folderAppPickerId = folder.id
                },
                onRename = { name -> onRenameFolder(folder.id, name) },
                onAddToHome = { onAddFolderToHome(folder) },
                onRemoveFromHome = { onRemoveFolderFromHome(folder) },
                moveTargets = if (allowHomePlacement) {
                    homePages.filter { page ->
                        page.folderPlacements.none { placement -> placement.folderId == folder.id }
                    }
                } else {
                    emptyList()
                },
                onMoveToPage = { target ->
                    onMoveFolderToPage(folder, target)
                    selectedFolderId = null
                },
                onDelete = {
                    selectedFolderId = null
                    onDeleteFolder(folder)
                },
                onDismiss = { selectedFolderId = null },
            )
        }

    folderAppPickerId
        ?.let { id -> folders.firstOrNull { it.id == id } }
        ?.let { folder ->
            val folderProfileId = folder.profileId ?: primaryFolderProfileId
            LauncherFolderAppPickerSheet(
                folder = folder,
                availableApps = rootAppsByKey.values.filter { app ->
                    app.user.hashCode() == folderProfileId
                },
                onAddApp = { app -> onAddAppToFolder(folder.id, app) },
                onDismiss = {
                    folderAppPickerId = null
                    selectedFolderId = folder.id
                },
            )
        }

    if (showHiddenAppsManager) {
        LauncherHiddenAppsManagerSheet(
            hiddenApps = hiddenAppKeys
                .asSequence()
                .mapNotNull(rootAppsByKey::get)
                .sortedBy { app -> app.label.toString().lowercase(Locale.getDefault()) }
                .toList(),
            onRestore = { app -> onSetAppHidden(app.workspaceKey(), false) },
            onDismiss = { showHiddenAppsManager = false },
        )
    }

    if (showAppLockManager) {
        LauncherAppLockManagerSheet(
            apps = rootAppsByKey.values
                .sortedBy { app -> app.label.toString().lowercase(Locale.getDefault()) },
            lockedAppKeys = lockedAppKeys,
            onSetLocked = { app, locked -> onSetAppLocked(app.workspaceKey(), locked) },
            onDismiss = { showAppLockManager = false },
        )
    }

    if (showFolderManager) {
        val managedFolders = folders.filter { folder ->
            LauncherFolderProfilePolicy.belongsToProfile(
                folder = folder,
                profileId = folderManagerProfileId,
                primaryProfileId = primaryFolderProfileId,
            )
        }
        LauncherFolderManagerSheet(
            folders = managedFolders,
            appsByKey = rootAppsByKey,
            homeFolderIds = homeFolderIds,
            defaultAddToHome = folderManagerAddToHome,
            allowHomePlacement = folderManagerProfileId == primaryFolderProfileId,
            onCreate = { name, addToHome ->
                val initialApp = folderAssignmentAppKey?.let(rootAppsByKey::get)
                onCreateFolder(
                    name,
                    addToHome && folderManagerProfileId == primaryFolderProfileId,
                    initialApp,
                    folderManagerProfileId,
                )
                folderAssignmentAppKey = null
            },
            onOpen = { folder ->
                showFolderManager = false
                selectedFolderId = folder.id
            },
            onAddToHome = onAddFolderToHome,
            onRemoveFromHome = onRemoveFolderFromHome,
            onDismiss = {
                showFolderManager = false
                folderAssignmentAppKey = null
            },
        )
    }

    if (!showFolderManager) folderAssignmentAppKey
        ?.let(rootAppsByKey::get)
        ?.let { app ->
            val appProfileId = app.user.hashCode()
            val compatibleFolders = folders.filter { folder ->
                LauncherFolderProfilePolicy.belongsToProfile(
                    folder = folder,
                    profileId = appProfileId,
                    primaryProfileId = primaryFolderProfileId,
                )
            }
            LauncherFolderAssignmentSheet(
                app = app,
                folders = compatibleFolders,
                onAssign = { folder ->
                    onAddAppToFolder(folder.id, app)
                    folderAssignmentAppKey = null
                },
                onCreateFolder = {
                    folderManagerProfileId = appProfileId
                    folderManagerAddToHome = appProfileId == primaryFolderProfileId
                    showFolderManager = true
                },
                onDismiss = { folderAssignmentAppKey = null },
            )
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeSurface(
    apps: List<LauncherActivityInfo>,
    workspace: WorkspaceState,
    preferences: LauncherPreferences,
    experiencePreferences: LauncherExperiencePreferences,
    homePageTransition: LauncherHomePageTransition,
    recentAppKeys: List<String>,
    localLaunchCounts: Map<String, Long>,
    hiddenHomeSuggestionKeys: Set<String>,
    homePageCount: Int,
    selectedHomePageId: String,
    homeResetSequence: Long,
    homeEditorRequestSequence: Long,
    homeEditorInitialPageId: String?,
    homeLabelOverrides: Map<String, String>,
    folders: List<LauncherFolder>,
    primaryHomePage: WorkspaceRenderedHomePage?,
    homePages: List<WorkspaceRenderedHomePage>,
    editMode: Boolean,
    activeDrag: LauncherAppDragData?,
    dragPoint: Offset?,
    homeCellBounds: MutableMap<Pair<Int, Int>, Rect>,
    dockItemBounds: MutableMap<String, Rect>,
    onHomeGridBoundsChanged: (Rect) -> Unit,
    onDockBoundsChanged: (Rect) -> Unit,
    onBeginLocalDrag: (LauncherAppDragData, Offset) -> Unit,
    onUpdateLocalDrag: (Offset) -> Unit,
    onEndLocalDrag: (LauncherAppDragData, Offset) -> Unit,
    onCancelLocalDrag: () -> Unit,
    onExitEditMode: () -> Unit,
    onManageHomePages: () -> Unit,
    onCreateHomePage: () -> Unit,
    onSelectHomePage: (String) -> Unit,
    onDeleteHomePage: (String) -> Unit,
    onSwipeHomePageLeft: () -> Boolean,
    onSwipeHomePageRight: () -> Boolean,
    onManageFolders: () -> Unit,
    onOpenFolder: (LauncherFolder) -> Unit,
    onMoveFavoriteToCell: (LauncherActivityInfo, Int, Int) -> Unit,
    onMoveWidget: (WorkspaceRenderedHomeWidget, Int, Int) -> Unit,
    onMoveWidgetToPageCell: (WorkspaceRenderedHomeWidget, String, Int, Int) -> Unit,
    onMoveHomeFolderToCell: (LauncherFolder, Int, Int) -> Unit,
    onMoveHomeFolderToPageCell: (LauncherFolder, String, Int, Int) -> Unit,
    onLaunchApp: (LauncherActivityInfo) -> Unit,
    onAddBuiltInWidget: (String) -> Unit,
    onSetManagedHomeSearchEnabled: (Boolean) -> Unit,
    availableAndroidWidgets: List<LauncherWidgetProviderDescriptor>,
    onPickInstalledAndroidWidget: (LauncherWidgetProviderDescriptor) -> Unit,
    onPickAndroidWidget: () -> Unit,
    onCreateAndroidWidgetView: (Int) -> AppWidgetHostView?,
    onManageWidget: (WorkspaceRenderedHomeWidget) -> Unit,
    onOpenLauncherSearch: () -> Unit,
    onManageApp: (LauncherActivityInfo, Rect?, LauncherAppContextOrigin) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenThemeManager: () -> Unit,
    onOpenWallpaperPicker: () -> Unit,
    onSetHomeCardStyle: (LauncherHomeCardStyle) -> Unit,
    onHomeEditorVisibilityChanged: (Boolean) -> Unit,
    contentOnly: Boolean = false,
    dockHostedExternally: Boolean = false,
    horizontalPagingHostedExternally: Boolean = false,
) {
    val appsByKey = remember(apps) { apps.associateBy { it.workspaceKey() } }
    val personalApps = remember(apps) {
        val personalUser = Process.myUserHandle()
        apps.filter { it.user == personalUser }
    }
    val favoriteApps = remember(appsByKey, workspace.favoriteKeys, preferences.homeCapacity) {
        workspace.favoriteKeys.mapNotNull(appsByKey::get).take(preferences.homeCapacity)
    }
    val dockApps = remember(appsByKey, workspace.dockKeys) {
        workspace.dockKeys.mapNotNull(appsByKey::get).take(MAX_DOCK_ITEMS)
    }
    val suggestedApps = remember(
        appsByKey,
        recentAppKeys,
        localLaunchCounts,
        workspace.favoriteKeys,
        workspace.dockKeys,
        experiencePreferences.homeAppMode,
        hiddenHomeSuggestionKeys,
    ) {
        LauncherHomeSuggestionsPolicy.selectKeys(
            mode = experiencePreferences.homeAppMode,
            recentAppKeys = recentAppKeys,
            launchCounts = localLaunchCounts,
            availableAppKeys = appsByKey.keys,
            favoriteKeys = workspace.favoriteKeys.toSet(),
            dockKeys = workspace.dockKeys.toSet(),
        )
            .filterNot(hiddenHomeSuggestionKeys::contains)
            .mapNotNull(appsByKey::get)
    }

    LaunchedEffect(preferences.homeColumns, preferences.homeRows) {
        homeCellBounds.keys
            .filter { (cellX, cellY) ->
                cellX !in 0 until preferences.homeColumns ||
                    cellY !in 0 until preferences.homeRows
            }
            .forEach(homeCellBounds::remove)
    }
    LaunchedEffect(dockApps.map { it.workspaceKey() }) {
        val visibleDockKeys = dockApps.map { it.workspaceKey() }.toSet()
        dockItemBounds.keys
            .filterNot(visibleDockKeys::contains)
            .forEach(dockItemBounds::remove)
    }
    LaunchedEffect(contentOnly) {
        if (contentOnly) {
            dockItemBounds.clear()
            onDockBoundsChanged(Rect.Zero)
        }
    }

    val swipeThreshold = with(LocalDensity.current) { 56.dp.toPx() }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var homeOverlay by rememberSaveable { mutableStateOf(LauncherHomeOverlay.NONE) }
    var movableGlanceMigrationRequested by rememberSaveable { mutableStateOf(false) }
    val hasMovableGlance = remember(primaryHomePage) {
        primaryHomePage?.widgetPlacements?.any { placement ->
            val descriptor = placement.descriptor as? WorkspaceWidgetDescriptor.BuiltIn
            descriptor?.typeId == WorkspaceWidgetCatalog.GLANCE
        } == true
    }

    LaunchedEffect(hasMovableGlance, movableGlanceMigrationRequested) {
        if (movableGlanceMigrationRequested && hasMovableGlance) {
            onSetHomeCardStyle(LauncherHomeCardStyle.OFF)
            movableGlanceMigrationRequested = false
        } else if (movableGlanceMigrationRequested) {
            delay(1_500)
            movableGlanceMigrationRequested = false
        }
    }

    // HOME re-entry invalidates any editor/picker that was opened under an older reset
    // generation immediately, instead of waiting an extra composition for the cleanup effect.
    val homeOverlayResetGeneration = remember(homeOverlay) { homeResetSequence }
    val effectiveHomeOverlay =
        if (homeOverlayResetGeneration == homeResetSequence) {
            homeOverlay
        } else {
            LauncherHomeOverlay.NONE
        }
    val effectiveShowHomeEditor = effectiveHomeOverlay == LauncherHomeOverlay.EDITOR
    val effectiveShowWidgetPicker = effectiveHomeOverlay == LauncherHomeOverlay.WIDGET_PICKER

    LaunchedEffect(effectiveShowHomeEditor) {
        onHomeEditorVisibilityChanged(effectiveShowHomeEditor)
    }

    LaunchedEffect(homeResetSequence) {
        if (homeResetSequence > 0L) {
            homeOverlay = LauncherHomeOverlay.NONE
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            onHomeEditorVisibilityChanged(false)
        }
    }

    LaunchedEffect(homeEditorRequestSequence) {
        if (homeEditorRequestSequence > 0L) {
            homeOverlay = LauncherHomeOverlay.EDITOR
        }
    }

    val executeGestureAction: (LauncherGestureAction) -> Unit = { action ->
        dispatchLauncherHomeGestureAction(
            action = action,
            appsByKey = appsByKey,
            onOpenApps = onOpenDrawer,
            onOpenSearch = onOpenLauncherSearch,
            onOpenHomeEditor = { homeOverlay = LauncherHomeOverlay.EDITOR },
            onOpenWallpaperPicker = onOpenWallpaperPicker,
            onOpenThemeManager = onOpenThemeManager,
            onLaunchApp = onLaunchApp,
        )
    }
    val currentGesturePreferences by rememberUpdatedState(experiencePreferences)
    val currentExecuteGestureAction by rememberUpdatedState(executeGestureAction)
    val currentSwipeHomePageLeft by rememberUpdatedState(onSwipeHomePageLeft)
    val currentSwipeHomePageRight by rememberUpdatedState(onSwipeHomePageRight)

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = LocalDateTime.now()
        }
    }

    val wallpaperShadeAlpha = when (experiencePreferences.wallpaperShade) {
        LauncherWallpaperShade.OFF -> 0f
        LauncherWallpaperShade.SOFT -> 0.18f
        LauncherWallpaperShade.STRONG -> 0.34f
    }
    val homeSearchSurface = launcherHomeSearchSurface(
        mode = preferences.universalSearchHomeMode,
        placement = experiencePreferences.homeSearchPlacement,
    )
    val hasMovableSearch = remember(homePages) {
        homePages.any { page ->
            page.widgetPlacements.any { placement ->
                val descriptor = placement.descriptor as? WorkspaceWidgetDescriptor.BuiltIn
                descriptor?.typeId == WorkspaceWidgetCatalog.SEARCH
            }
        }
    }
    LaunchedEffect(homeSearchSurface, hasMovableSearch, primaryHomePage) {
        when {
            homeSearchSurface == LauncherHomeSearchSurface.MOVABLE && !hasMovableSearch ->
                onSetManagedHomeSearchEnabled(true)
            homeSearchSurface != LauncherHomeSearchSurface.MOVABLE ->
                onSetManagedHomeSearchEnabled(false)
        }
    }
    val showFixedSearchAtTop = homeSearchSurface == LauncherHomeSearchSurface.FIXED_TOP
    val showFixedSearchAtBottom =
        homeSearchSurface == LauncherHomeSearchSurface.FIXED_BOTTOM ||
            (homeSearchSurface == LauncherHomeSearchSurface.MOVABLE && !hasMovableSearch)
    val openSearch = onOpenLauncherSearch

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag(
                "launcher-home-swipe-up-" +
                    experiencePreferences.swipeUpAction.storageValue,
            )
            .launcherHomePagerVerticalGestureNavigation(
                enabled = !horizontalPagingHostedExternally,
                sensitivity = experiencePreferences.gestureSensitivity,
                onSwipeUp = {
                    currentExecuteGestureAction(
                        currentGesturePreferences.swipeUpAction,
                    )
                },
                onSwipeDown = {
                    currentExecuteGestureAction(
                        currentGesturePreferences.swipeDownAction,
                    )
                },
            ),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .testTag("launcher-home-empty-space-actions")
                .semantics {
                    onLongClick(label = "Edit Home") {
                        homeOverlay = LauncherHomeOverlay.EDITOR
                        true
                    }
                }
                .pointerInput(swipeThreshold) {
                    detectTapGestures(
                        onDoubleTap = {
                            currentExecuteGestureAction(
                                currentGesturePreferences.doubleTapAction,
                            )
                        },
                        onLongPress = {
                            homeOverlay = LauncherHomeOverlay.EDITOR
                        },
                    )
                }
                .then(
                    if (
                        primaryHomeShouldHandleHorizontalPaging(
                            contentOnly = contentOnly,
                            pagingHostedExternally = horizontalPagingHostedExternally,
                        )
                    ) {
                        Modifier.pointerInput(swipeThreshold) {
                            var drag = 0f
                            var triggered = false
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    drag = 0f
                                    triggered = false
                                },
                                onDragCancel = {
                                    drag = 0f
                                    triggered = false
                                },
                                onDragEnd = {
                                    drag = 0f
                                    triggered = false
                                },
                                onHorizontalDrag = { change, amount ->
                                    change.consume()
                                    if (!triggered) {
                                        drag += amount
                                        when {
                                            drag >= swipeThreshold -> {
                                                triggered = true
                                                if (!currentSwipeHomePageRight()) {
                                                    currentExecuteGestureAction(
                                                        currentGesturePreferences.swipeRightAction,
                                                    )
                                                }
                                            }
                                            drag <= -swipeThreshold -> {
                                                triggered = true
                                                if (!currentSwipeHomePageLeft()) {
                                                    currentExecuteGestureAction(
                                                        currentGesturePreferences.swipeLeftAction,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                },
                            )
                        }
                    } else {
                        Modifier
                    },
                ),
        )

        if (wallpaperShadeAlpha > 0f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                GlazeAtmosphere.canvasBlack.copy(alpha = wallpaperShadeAlpha * 0.35f),
                                Color.Transparent,
                                GlazeAtmosphere.canvasBlack.copy(alpha = wallpaperShadeAlpha),
                            ),
                        ),
                    ),
            )
        }

        val homeVerticalSpacing = when (experiencePreferences.homeSpacing) {
            LauncherHomeSpacing.COMPACT -> GlazeMetrics.space1
            LauncherHomeSpacing.BALANCED -> GlazeMetrics.space2
            LauncherHomeSpacing.AIRY -> GlazeMetrics.space3
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .then(
                    if (
                        primaryHomeShouldOwnBottomInset(
                            contentOnly = contentOnly,
                            dockHostedExternally = dockHostedExternally,
                        )
                    ) {
                        Modifier.navigationBarsPadding()
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = GlazeMetrics.space4, vertical = GlazeMetrics.space2),
            verticalArrangement = Arrangement.spacedBy(homeVerticalSpacing),
        ) {
            if (editMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("launcher-home-edit-mode"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            if (activeDrag != null) "Move app" else "Edit Home",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            if (activeDrag != null) {
                                "Drop on a Home cell, in the Dock, or at a page edge"
                            } else {
                                "Drag apps, or long-press one for more options"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.76f),
                        )
                    }
                    FilledTonalButton(
                        onClick = onExitEditMode,
                        enabled = activeDrag == null,
                    ) {
                        Text("Done")
                    }
                }
            }

            if (experiencePreferences.homeCardStyle != LauncherHomeCardStyle.OFF) {
                HomeAtAGlance(
                    now = now,
                    compact = experiencePreferences.homeCardStyle == LauncherHomeCardStyle.COMPACT,
                    alignment = experiencePreferences.homeGlanceAlignment,
                    canPromote = !preferences.layoutLocked,
                    onRequestMovableGlance = {
                        if (!movableGlanceMigrationRequested && !hasMovableGlance) {
                            movableGlanceMigrationRequested = true
                            onAddBuiltInWidget(WorkspaceWidgetCatalog.GLANCE)
                        }
                    },
                )
            }

            if (primaryHomeShouldRenderFixedSearch(contentOnly, showFixedSearchAtTop)) {
                GlazeSearchCapsule(
                    value = "Search with GoreeCloud…",
                    style = experiencePreferences.homeSearchStyle,
                    onClick = openSearch,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (experiencePreferences.showHomeQuickActions) {
                HomeQuickActions(
                    onOpenApps = onOpenDrawer,
                    onOpenSearch = openSearch,
                )
            }

            Spacer(Modifier.weight(1f))

            if (
                favoriteApps.isEmpty() &&
                suggestedApps.isEmpty() &&
                dockApps.isEmpty() &&
                primaryHomePage?.widgetPlacements.isNullOrEmpty() &&
                primaryHomePage?.folderPlacements.isNullOrEmpty() &&
                activeDrag == null
            ) {
                EmptyWorkspaceCard(
                    onOpenApps = onOpenDrawer,
                )
            }
            if (
                favoriteApps.isNotEmpty() ||
                suggestedApps.isNotEmpty() ||
                !primaryHomePage?.widgetPlacements.isNullOrEmpty() ||
                !primaryHomePage?.folderPlacements.isNullOrEmpty() ||
                activeDrag != null
            ) {
                HomeFavoritesGrid(
                    apps = favoriteApps,
                    suggestedApps = suggestedApps,
                    allApps = personalApps,
                    folders = folders,
                    columns = preferences.homeColumns,
                    rows = preferences.homeRows,
                    primaryHomePage = primaryHomePage,
                    homePages = homePages,
                    iconScale = preferences.iconScale,
                    showLabels = experiencePreferences.showHomeLabels,
                    spacing = experiencePreferences.homeSpacing,
                    layoutLocked = preferences.layoutLocked,
                    homeLabelOverrides = homeLabelOverrides,
                    cellBounds = homeCellBounds,
                    onGridBoundsChanged = onHomeGridBoundsChanged,
                    editMode = editMode,
                    activeDrag = activeDrag,
                    dragPoint = dragPoint,
                    onBeginLocalDrag = onBeginLocalDrag,
                    onUpdateLocalDrag = onUpdateLocalDrag,
                    onEndLocalDrag = onEndLocalDrag,
                    onCancelLocalDrag = onCancelLocalDrag,
                    onLaunchApp = onLaunchApp,
                    onManageApp = { app, anchor ->
                        onManageApp(app, anchor, LauncherAppContextOrigin.HOME)
                    },
                    onCreateAndroidWidgetView = onCreateAndroidWidgetView,
                    onManageWidget = onManageWidget,
                    onMoveWidget = onMoveWidget,
                    onMoveWidgetToPageCell = onMoveWidgetToPageCell,
                    onMoveHomeFolderToCell = onMoveHomeFolderToCell,
                    onMoveHomeFolderToPageCell = onMoveHomeFolderToPageCell,
                    onOpenFolder = onOpenFolder,
                    onOpenWidgetSearch = openSearch,
                    onOpenWidgetApps = onOpenDrawer,
                    onOpenWidgetEditor = { homeOverlay = LauncherHomeOverlay.EDITOR },
                    onOpenWidgetSettings = onOpenSettings,
                    onSwipeUp = {
                        executeGestureAction(experiencePreferences.swipeUpAction)
                    },
                    onSwipeDown = {
                        executeGestureAction(experiencePreferences.swipeDownAction)
                    },
                    modifier = Modifier.launcherHomePageEntryTransition(
                        transition = homePageTransition,
                        transitionKey = primaryHomeTransitionKey(
                            selectedHomePageId = selectedHomePageId,
                            pages = homePages,
                            pagingHostedExternally = horizontalPagingHostedExternally,
                        ),
                    ),
                )
            }

            if (primaryHomeShouldRenderFixedSearch(contentOnly, showFixedSearchAtBottom)) {
                GlazeSearchCapsule(
                    value = "Search with GoreeCloud…",
                    style = experiencePreferences.homeSearchStyle,
                    onClick = openSearch,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (
                primaryHomeShouldReservePageIndicator(
                    contentOnly = contentOnly,
                    pageCount = homePageCount,
                    requested = experiencePreferences.showHomePageIndicator,
                )
            ) {
                // MainActivity renders the page dots as an overlay so they stay clickable while
                // Home content changes. Reserve a real strip in the Home layout so the final app
                // row/labels can never be painted under that indicator.
                Spacer(
                    Modifier
                        .height(20.dp)
                        .testTag("launcher-home-page-indicator-reserve"),
                )
            }

            if (
                primaryHomeShouldRenderDock(
                    contentOnly = contentOnly,
                    dockAppCount = dockApps.size,
                    activeDrag = activeDrag != null,
                    persistentAffordance = experiencePreferences.showDockSearch,
                    dockHostedExternally = dockHostedExternally,
                )
            ) {
                EditableHomeDock(
                    apps = dockApps,
                    iconScale = preferences.iconScale,
                    style = experiencePreferences.dockStyle,
                    pageSize = experiencePreferences.dockPageSize,
                    loopPages = experiencePreferences.dockLoopPages,
                    showLabels = experiencePreferences.showDockLabels,
                    showSearch = experiencePreferences.showDockSearch,
                    layoutLocked = preferences.layoutLocked,
                    editMode = editMode,
                    activeDrag = activeDrag,
                    dragPoint = dragPoint,
                    onDockBoundsChanged = onDockBoundsChanged,
                    dockItemBounds = dockItemBounds,
                    onBeginLocalDrag = onBeginLocalDrag,
                    onUpdateLocalDrag = onUpdateLocalDrag,
                    onEndLocalDrag = onEndLocalDrag,
                    onCancelLocalDrag = onCancelLocalDrag,
                    onLaunchApp = onLaunchApp,
                    onManageApp = { app, anchor ->
                        onManageApp(app, anchor, LauncherAppContextOrigin.DOCK)
                    },
                    onOpenSearch = openSearch,
                    onSwipeUp = {
                        executeGestureAction(experiencePreferences.swipeUpAction)
                    },
                    onSwipeDown = {
                        executeGestureAction(experiencePreferences.swipeDownAction)
                    },
                )
            }

            if (!contentOnly && !dockHostedExternally) {
                Spacer(Modifier.height(2.dp))
            }
        }

        if (effectiveShowHomeEditor || effectiveShowWidgetPicker) {
            // Keep editor -> widget-gallery navigation in one Compose dialog window. Replacing a
            // Dialog with a Material bottom-sheet window in the same interaction proved racy on
            // Android 16 and could leave the picker uncomposed. A single window also makes Back
            // dismissal and accessibility focus transfer deterministic.
            Dialog(
                onDismissRequest = {
                    if (effectiveShowWidgetPicker) {
                        homeOverlay = LauncherHomeOverlay.NONE
                    } else {
                        homeOverlay = LauncherHomeOverlay.NONE
                    }
                },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false,
                ),
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (effectiveShowHomeEditor) {
                                Modifier.testTag("launcher-home-editor-fullscreen")
                            } else {
                                Modifier.testTag("launcher-widget-picker-fullscreen")
                            },
                        ),
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = 0.dp,
                ) {
                    if (effectiveShowHomeEditor) {
                        HomeEditorSurface(
                            dockApps = dockApps,
                            preferences = preferences,
                            homePages = homePages,
                            initialPageId = homeEditorInitialPageId,
                            onSelectPage = onSelectHomePage,
                            onCreatePage = onCreateHomePage,
                            onDeletePage = onDeleteHomePage,
                            onDone = { homeOverlay = LauncherHomeOverlay.NONE },
                            onWallpaper = {
                                homeOverlay = LauncherHomeOverlay.NONE
                                onOpenWallpaperPicker()
                            },
                            onWidgets = {
                                homeOverlay = LauncherHomeOverlay.WIDGET_PICKER
                            },
                            onFolders = {
                                homeOverlay = LauncherHomeOverlay.NONE
                                onManageFolders()
                            },
                            onApps = {
                                homeOverlay = LauncherHomeOverlay.NONE
                                onOpenDrawer()
                            },
                            onSettings = {
                                homeOverlay = LauncherHomeOverlay.NONE
                                onOpenSettings()
                            },
                        )
                    } else {
                        LauncherWidgetPickerSheet(
                            apps = apps,
                            availableAndroidWidgets = availableAndroidWidgets,
                            onDismiss = {
                                homeOverlay = LauncherHomeOverlay.NONE
                            },
                            onAddBuiltInWidget = { typeId ->
                                homeOverlay = LauncherHomeOverlay.NONE
                                onAddBuiltInWidget(typeId)
                            },
                            onPickInstalledAndroidWidget = { descriptor ->
                                homeOverlay = LauncherHomeOverlay.NONE
                                onPickInstalledAndroidWidget(descriptor)
                            },
                            onPickAndroidWidget = {
                                homeOverlay = LauncherHomeOverlay.NONE
                                onPickAndroidWidget()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LauncherWidgetPickerSheet(
    apps: List<LauncherActivityInfo>,
    availableAndroidWidgets: List<LauncherWidgetProviderDescriptor>,
    onDismiss: () -> Unit,
    onAddBuiltInWidget: (String) -> Unit,
    onPickInstalledAndroidWidget: (LauncherWidgetProviderDescriptor) -> Unit,
    onPickAndroidWidget: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val builtIns = remember { WorkspaceWidgetCatalog.builtInTypeIds.toList() }
    val filteredBuiltIns = remember(builtIns, query) {
        builtIns.filter { typeId -> WorkspaceWidgetCatalog.matchesQuery(typeId, query) }
    }
    val filteredAndroidWidgets = remember(availableAndroidWidgets, query) {
        val needle = query.trim()
        if (needle.isBlank()) {
            availableAndroidWidgets
        } else {
            availableAndroidWidgets.filter { descriptor ->
                descriptor.label.contains(needle, ignoreCase = true) ||
                    descriptor.packageName.contains(needle, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("launcher-widget-picker-sheet")
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = GlazeMetrics.space4, vertical = GlazeMetrics.space3),
        verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space3),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Choose widget",
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                LauncherHeaderGlyphAction(
                    contentDescription = "Close widget picker",
                    symbol = GlazePopupActionSymbol.CLOSE,
                    onClick = onDismiss,
                    modifier = Modifier.testTag("launcher-widget-picker-close"),
                )
            }
            Text(
                "GoreeCloud widgets and installed Android widgets in one Launcher gallery.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LauncherSettingsSearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Search widgets",
            inputTestTag = "launcher-widget-search-field",
        )

        Text(
            "GoreeCloud",
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (filteredBuiltIns.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
            ) {
                Text(
                    "No GoreeCloud widgets match “$query”.",
                    modifier = Modifier.padding(GlazeMetrics.space3),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val columns = if (maxWidth >= 720.dp) 2 else 1
                Column(verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space2)) {
                    filteredBuiltIns.chunked(columns).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                        ) {
                            row.forEach { typeId ->
                                WidgetPickerBuiltInCard(
                                    typeId = typeId,
                                    onClick = { onAddBuiltInWidget(typeId) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }

        Text(
            "Installed apps",
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        if (filteredAndroidWidgets.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
            ) {
                Text(
                    if (query.isBlank()) {
                        "No installed third-party widgets were discovered for this profile."
                    } else {
                        "No installed Android widgets match “$query”."
                    },
                    modifier = Modifier.padding(GlazeMetrics.space3),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            filteredAndroidWidgets.forEach { descriptor ->
                InstalledWidgetPickerRow(
                    descriptor = descriptor,
                    apps = apps,
                    onClick = { onPickInstalledAndroidWidget(descriptor) },
                )
            }
        }

        Surface(
            onClick = onPickAndroidWidget,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.065f),
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space3),
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(GlazeMetrics.radiusMedium),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        GlazePopupActionGlyph(
                            symbol = GlazePopupActionSymbol.WIDGET,
                            color = MaterialTheme.colorScheme.primary,
                            iconSize = 20.dp,
                        )
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Android widget picker",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Browse every widget exposed by Android",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LauncherHomeOpenGlyph(
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Text(
            "Android may still show a system authorization or configuration screen after you choose a third-party widget.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(GlazeMetrics.space2))
    }
}

@Composable
private fun WidgetPickerBuiltInCard(
    typeId: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val span = WorkspaceWidgetCatalog.defaultSpan(typeId) ?: (1 to 1)
    val now = remember { LocalDateTime.now() }
    val previewColor = MaterialTheme.colorScheme.primary
    Surface(
        modifier = modifier.testTag("launcher-widget-built-in-$typeId"),
        onClick = onClick,
        shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.065f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier.size(58.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clearAndSetSemantics { },
                    contentAlignment = Alignment.Center,
                ) {
                    when (typeId) {
                        WorkspaceWidgetCatalog.CALENDAR -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(0.dp),
                            ) {
                                Text(
                                    now.format(
                                        DateTimeFormatter.ofPattern(
                                            "MMM",
                                            Locale.getDefault(),
                                        ),
                                    ).uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = previewColor.copy(alpha = 0.72f),
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    now.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Light,
                                    color = previewColor,
                                )
                            }
                        }
                        WorkspaceWidgetCatalog.WEATHER -> {
                            LauncherWeatherIcon(
                                kind = LauncherWeatherVisualKind.UNKNOWN,
                                isDay = true,
                                color = previewColor,
                                size = 30.dp,
                            )
                        }
                        WorkspaceWidgetCatalog.GLANCE -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(1.dp),
                            ) {
                                Text(
                                    now.format(
                                        DateTimeFormatter.ofPattern(
                                            "h:mm",
                                            Locale.getDefault(),
                                        ),
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Light,
                                    color = previewColor,
                                )
                                Box(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .height(2.dp)
                                        .background(
                                            previewColor.copy(alpha = 0.72f),
                                            RoundedCornerShape(GlazeMetrics.radiusPill),
                                        ),
                                )
                            }
                        }
                        WorkspaceWidgetCatalog.CLOCK,
                        WorkspaceWidgetCatalog.COMPACT_CLOCK,
                        -> Text(
                            now.format(
                                DateTimeFormatter.ofPattern(
                                    "h:mm",
                                    Locale.getDefault(),
                                ),
                            ),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Light,
                            color = previewColor,
                        )
                        WorkspaceWidgetCatalog.ANALOG_CLOCK,
                        WorkspaceWidgetCatalog.QUICK_ACTIONS,
                        WorkspaceWidgetCatalog.BATTERY,
                        -> LauncherWidgetPreviewGlyph(
                            typeId = typeId,
                            tint = previewColor,
                            modifier = Modifier.size(30.dp),
                        )
                        WorkspaceWidgetCatalog.DATE -> Text(
                            now.dayOfMonth.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Light,
                            color = previewColor,
                        )
                        WorkspaceWidgetCatalog.MONTH -> Text(
                            now.format(
                                DateTimeFormatter.ofPattern(
                                    "MMM",
                                    Locale.getDefault(),
                                ),
                            ).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = previewColor,
                        )
                        WorkspaceWidgetCatalog.SEARCH ->
                            LauncherSearchMagnifier(previewColor)
                        WorkspaceWidgetCatalog.LAUNCHER_STATUS -> Text(
                            "GC",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = previewColor,
                        )
                        else -> Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(previewColor, CircleShape),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    WorkspaceWidgetCatalog.displayName(typeId),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    WorkspaceWidgetCatalog.description(typeId),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Surface(
                shape = RoundedCornerShape(GlazeMetrics.radiusPill),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.64f),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                ),
            ) {
                Text(
                    "${span.first} × ${span.second}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun InstalledWidgetPickerRow(
    descriptor: LauncherWidgetProviderDescriptor,
    apps: List<LauncherActivityInfo>,
    onClick: () -> Unit,
) {
    val sourceApp = remember(apps, descriptor.packageName) {
        apps.firstOrNull { app -> app.componentName.packageName == descriptor.packageName }
    }
    val icon = sourceApp?.let { rememberLauncherAppIcon(it) }
    val dimensionSummary = if (descriptor.minWidth > 0 && descriptor.minHeight > 0) {
        "Minimum ${descriptor.minWidth} × ${descriptor.minHeight}"
    } else {
        "Android widget"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GlazeMetrics.space3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space3),
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (icon != null) {
                        Image(
                            bitmap = icon,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(36.dp).launcherIconMask(),
                        )
                    } else {
                        GlazePopupActionGlyph(
                            symbol = GlazePopupActionSymbol.WIDGET,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    descriptor.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    dimensionSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Surface(
                modifier = Modifier
                    .size(44.dp)
                    .semantics { contentDescription = "Add " + descriptor.label + " widget" },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                ),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    GlazePopupActionGlyph(
                        symbol = GlazePopupActionSymbol.ADD,
                        color = MaterialTheme.colorScheme.primary,
                        iconSize = 18.dp,
                    )
                }
            }
        }
    }
}

@Composable
internal fun LauncherWidgetManagementDialog(
    widget: WorkspaceRenderedHomeWidget,
    columns: Int,
    rows: Int,
    layoutLocked: Boolean,
    onResize: (Int, Int) -> Unit,
    onRemove: () -> Unit,
    onMove: (Int, Int) -> Unit,
    moveTargets: List<WorkspaceRenderedHomePage> = emptyList(),
    onMoveToPage: (String) -> Unit = {},
    onClose: () -> Unit,
) {
    var choosingCell by remember(widget.itemId) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Widget options") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
            ) {
                Text(
                    when (val descriptor = widget.descriptor) {
                        is WorkspaceWidgetDescriptor.BuiltIn ->
                            WorkspaceWidgetCatalog.displayName(descriptor.typeId)
                        is WorkspaceWidgetDescriptor.Android -> "Android widget"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "${widget.spanX} × ${widget.spanY} Home cells",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (choosingCell && !layoutLocked) {
                    Text(
                        "Choose the top-left cell for this widget. Occupied destinations are rejected without changing the layout.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    repeat(rows) { cellY ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(columns) { cellX ->
                                Surface(
                                    onClick = { onMove(cellX, cellY) },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(GlazeMetrics.radiusSmall),
                                    color = if (cellX == widget.cellX && cellY == widget.cellY) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("${cellX + 1},${cellY + 1}", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                    TextButton(onClick = { choosingCell = false }) { Text("Back to options") }
                } else if (layoutLocked) {
                    Text(
                        "Unlock the Home layout to move, resize or remove widgets.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    OutlinedButton(
                        onClick = { choosingCell = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Move to another cell") }
                    if (moveTargets.isNotEmpty()) {
                        Text(
                            "Move to another Home page",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        moveTargets.forEach { target ->
                            OutlinedButton(
                                onClick = { onMoveToPage(target.pageId) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    target.context().moveTargetLabel(
                                        pageNumber = target.rank + 1,
                                        primary =
                                            target.pageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID,
                                    ),
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                    ) {
                        OutlinedButton(
                            onClick = { onResize(widget.spanX - 1, widget.spanY) },
                            enabled = widget.spanX > 1,
                            modifier = Modifier.weight(1f),
                        ) { Text("Narrower") }
                        OutlinedButton(
                            onClick = { onResize(widget.spanX + 1, widget.spanY) },
                            enabled = widget.spanX < columns,
                            modifier = Modifier.weight(1f),
                        ) { Text("Wider") }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                    ) {
                        OutlinedButton(
                            onClick = { onResize(widget.spanX, widget.spanY - 1) },
                            enabled = widget.spanY > 1,
                            modifier = Modifier.weight(1f),
                        ) { Text("Shorter") }
                        OutlinedButton(
                            onClick = { onResize(widget.spanX, widget.spanY + 1) },
                            enabled = widget.spanY < rows,
                            modifier = Modifier.weight(1f),
                        ) { Text("Taller") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) { Text("Done") }
        },
        dismissButton = {
            TextButton(
                onClick = onRemove,
                enabled = !layoutLocked,
            ) {
                Text("Remove")
            }
        },
    )
}


@Composable
private fun HomeEditorSurface(
    dockApps: List<LauncherActivityInfo>,
    preferences: LauncherPreferences,
    homePages: List<WorkspaceRenderedHomePage>,
    initialPageId: String? = null,
    onSelectPage: (String) -> Unit,
    onDeletePage: (String) -> Unit,
    onDone: () -> Unit,
    onWallpaper: () -> Unit,
    onCreatePage: () -> Unit,
    onWidgets: () -> Unit,
    onFolders: () -> Unit,
    onApps: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = GlazeMetrics.space3, vertical = GlazeMetrics.space2),
        verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    "Edit Home",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Swipe pages, then choose what to customize.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            LauncherHeaderGlyphAction(
                contentDescription = "Done editing Home",
                symbol = GlazePopupActionSymbol.CHECK,
                onClick = onDone,
                modifier = Modifier.testTag("launcher-home-editor-done"),
            )
        }

        HomeEditorPageOverview(
            pages = homePages,
            dockApps = dockApps,
            homeColumns = preferences.homeColumns,
            homeRows = preferences.homeRows,
            layoutLocked = preferences.layoutLocked,
            initialPageId = initialPageId,
            onSelectPage = onSelectPage,
            onCreatePage = onCreatePage,
            onDeletePage = onDeletePage,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("launcher-home-editor-actions"),
            shape = RoundedCornerShape(GlazeMetrics.radiusExtraLarge),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
            ),
        ) {
            Row(
                modifier = Modifier.padding(GlazeMetrics.space1),
                horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space1),
            ) {
                HomeEditorAction(
                    "Wallpaper",
                    GlazePopupActionSymbol.WALLPAPER,
                    onWallpaper,
                    Modifier.weight(1f),
                )
                HomeEditorAction(
                    "Widgets",
                    GlazePopupActionSymbol.WIDGET,
                    onWidgets,
                    Modifier.weight(1f),
                )
                HomeEditorAction(
                    "Apps",
                    GlazePopupActionSymbol.APPS,
                    onApps,
                    Modifier.weight(1f),
                )
                HomeEditorAction(
                    "Folders",
                    GlazePopupActionSymbol.FOLDER,
                    onFolders,
                    Modifier.weight(1f),
                )
                HomeEditorAction(
                    "Settings",
                    GlazePopupActionSymbol.SETTINGS,
                    onSettings,
                    Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun HomeEditorPageOverview(
    pages: List<WorkspaceRenderedHomePage>,
    dockApps: List<LauncherActivityInfo>,
    homeColumns: Int,
    homeRows: Int,
    layoutLocked: Boolean,
    initialPageId: String? = null,
    onSelectPage: (String) -> Unit,
    onCreatePage: () -> Unit,
    onDeletePage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visiblePages = pages
    if (visiblePages.isEmpty()) return

    var pendingDeletePageId by remember(visiblePages) { mutableStateOf<String?>(null) }
    val pendingDeletePage = visiblePages.firstOrNull { it.pageId == pendingDeletePageId }
    val initialPageIndex = remember(visiblePages, initialPageId) {
        visiblePages.indexOfFirst { it.pageId == initialPageId }
            .takeIf { it >= 0 }
            ?: 0
    }
    val pagerState = rememberPagerState(
        initialPage = initialPageIndex,
        pageCount = { visiblePages.size + 1 },
    )

    LaunchedEffect(initialPageIndex, visiblePages.size) {
        if (pagerState.currentPage != initialPageIndex) {
            pagerState.scrollToPage(initialPageIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("launcher-home-editor-page-overview"),
        verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Home pages",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    if (visiblePages.size == 1) {
                        "One page · Swipe to + Add Page"
                    } else {
                        visiblePages.size.toString() + " pages · Swipe between previews or to + Add Page"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("launcher-home-editor-page-carousel"),
            contentPadding = PaddingValues(horizontal = 42.dp),
            pageSpacing = GlazeMetrics.space3,
            userScrollEnabled = true,
        ) { index ->
            if (index == visiblePages.size) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("launcher-home-editor-add-page"),
                    onClick = onCreatePage,
                    enabled = !layoutLocked,
                    shape = RoundedCornerShape(GlazeMetrics.radiusExtraLarge),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f),
                    border = BorderStroke(
                        1.dp,
                        if (layoutLocked) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)
                        },
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(GlazeMetrics.space4),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        GlazePopupActionGlyph(
                            symbol = GlazePopupActionSymbol.ADD,
                            color = if (layoutLocked) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            iconSize = 34.dp,
                        )
                        Text(
                            "Add Page",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            if (layoutLocked) {
                                "Unlock Home layout to add a page"
                            } else {
                                "Create a new blank Home page"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                return@HorizontalPager
            }

            val page = visiblePages[index]
            val primary = page.pageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID
            val itemCount = homePageVisibleItemCount(page)
            val canDelete = canDeleteHomePage(page, visiblePages, layoutLocked)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("launcher-home-editor-page-" + page.pageId),
                onClick = { onSelectPage(page.pageId) },
                shape = RoundedCornerShape(GlazeMetrics.radiusExtraLarge),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
                border = BorderStroke(
                    if (index == pagerState.currentPage) 2.dp else 1.dp,
                    if (index == pagerState.currentPage) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.58f)
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    },
                ),
            ) {
                Column(
                    modifier = Modifier.padding(GlazeMetrics.space2),
                    verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                ) {
                    HomePageMiniPreview(
                        page = page,
                        homeColumns = homeColumns,
                        homeRows = homeRows,
                        modifier = Modifier.weight(1f),
                        preservePhoneAspectRatio = false,
                    )

                    if (dockApps.isNotEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(GlazeMetrics.radiusPill),
                            color = GlazeAtmosphere.canvasBlack.copy(alpha = 0.20f),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                dockApps.take(7).forEach { app ->
                                    HomeEditorPreviewIcon(app)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                if (primary) "Home" else "Page " + (index + 1),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (index == pagerState.currentPage) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                            Text(
                                itemCount.toString() + if (itemCount == 1) " item" else " items",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (canDelete) {
                            HomeEditorDeleteGlyphAction(
                                contentDescription = "Delete empty " +
                                    (if (primary) "Home page" else "page " + (index + 1)),
                                onClick = { pendingDeletePageId = page.pageId },
                                modifier = Modifier.testTag(
                                    "launcher-home-editor-delete-page-" + page.pageId,
                                ),
                            )
                        } else if (primary) {
                            Text(
                                "Protected",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

    }

    if (pendingDeletePage != null) {
        AlertDialog(
            onDismissRequest = { pendingDeletePageId = null },
            shape = RoundedCornerShape(GlazeMetrics.radiusExtraLarge),
            title = { Text("Delete empty Home page?") },
            text = {
                Text(
                    "This page is empty. Deleting it will not remove apps, widgets, or folders.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val pageId = pendingDeletePage.pageId
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
private fun HomeEditorPreviewIcon(app: LauncherActivityInfo) {
    val icon = rememberLauncherAppIcon(app)
    if (icon != null) {
        Image(
            bitmap = icon,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(34.dp).launcherIconMask(),
        )
    } else {
        Surface(
            modifier = Modifier.size(34.dp).launcherIconMask(),
            shape = RoundedCornerShape(10.dp),
            color = Color.White.copy(alpha = 0.14f),
        ) {}
    }
}

@Composable
private fun HomeEditorDeleteGlyphAction(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .semantics { this.contentDescription = contentDescription },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.52f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            GlazePopupActionGlyph(
                symbol = GlazePopupActionSymbol.DELETE,
                color = MaterialTheme.colorScheme.error,
                iconSize = 19.dp,
            )
        }
    }
}

@Composable
private fun HomeEditorAction(
    label: String,
    symbol: GlazePopupActionSymbol,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.testTag("launcher-home-editor-action-" + label.lowercase()),
        onClick = onClick,
        shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            GlazePopupActionGlyph(
                symbol = symbol,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun LauncherWeatherStatusChip(
    foreground: Color,
    compact: Boolean = false,
) {
    val context = LocalContext.current
    var permissionRevision by remember { mutableIntStateOf(0) }
    var weather by remember { mutableStateOf(LauncherWeather.cachedSnapshot()) }
    var loading by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val hasLocationPermission = remember(permissionRevision, context) {
        LauncherWeather.hasLocationPermission(context)
    }
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract =
            androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        permissionRevision += 1
    }

    LaunchedEffect(hasLocationPermission, permissionRevision) {
        if (!hasLocationPermission) {
            weather = null
            loading = false
            failed = false
        } else {
            val hadCachedWeather = weather != null
            loading = !hadCachedWeather
            failed = false
            val refreshed = LauncherWeather.load(
                context = context.applicationContext,
                forceRefresh = permissionRevision > 0,
            )
            if (refreshed != null) weather = refreshed
            failed = weather == null
            loading = false
        }
    }

    val snapshot = weather.takeIf { hasLocationPermission }
    val primaryLabel = when {
        !hasLocationPermission -> "Weather"
        loading -> "Weather"
        snapshot != null -> snapshot.temperature.toString() + snapshot.unit
        failed -> "Weather"
        else -> "Weather"
    }
    val secondaryLabel = when {
        !hasLocationPermission -> "Tap to allow location"
        loading -> "Updating local weather"
        snapshot != null -> snapshot.condition
        failed -> "Weather unavailable"
        else -> "Local conditions"
    }
    val detailLabel = snapshot?.takeIf {
        it.condition == "High winds" || it.condition == "Windy"
    }?.let {
        "Gusts " + maxOf(it.windSpeed, it.windGust) + " " + it.windUnit
    }
    val visualKind = snapshot?.let {
        launcherWeatherVisualKind(
            code = it.weatherCode,
            windSpeed = it.windSpeed,
            windGust = it.windGust,
            windUnit = it.windUnit,
        )
    } ?: LauncherWeatherVisualKind.UNKNOWN

    Row(
        modifier = Modifier
            .widthIn(min = if (compact) 112.dp else 154.dp)
            .heightIn(min = if (compact) 56.dp else 68.dp)
            .semantics {
                contentDescription = when {
                    !hasLocationPermission ->
                        "Weather. Allow location access to show local conditions."
                    snapshot != null ->
                        "Weather. " + snapshot.temperature + snapshot.unit + ", " +
                            snapshot.condition
                    else -> "$primaryLabel. $secondaryLabel."
                }
            }
            .clickable {
                if (!hasLocationPermission) {
                    permissionLauncher.launch(
                        arrayOf(
                            android.Manifest.permission.ACCESS_COARSE_LOCATION,
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                        ),
                    )
                } else {
                    permissionRevision += 1
                }
            }
            .padding(horizontal = 4.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LauncherWeatherIcon(
            kind = visualKind,
            isDay = snapshot?.isDay ?: true,
            color = MaterialTheme.colorScheme.primary,
            size = if (compact) 36.dp else 44.dp,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                primaryLabel,
                style = if (compact) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.headlineSmall
                },
                fontWeight = FontWeight.Medium,
                color = foreground,
                maxLines = 1,
            )
            Text(
                secondaryLabel,
                style = if (compact) {
                    MaterialTheme.typography.labelSmall
                } else {
                    MaterialTheme.typography.bodySmall
                },
                color = foreground.copy(alpha = 0.76f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (detailLabel != null) {
                Text(
                    detailLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = foreground.copy(alpha = 0.60f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun HomeAtAGlance(
    now: LocalDateTime,
    compact: Boolean,
    alignment: LauncherHomeGlanceAlignment,
    canPromote: Boolean,
    onRequestMovableGlance: () -> Unit,
) {
    val locale = Locale.getDefault()
    val time = remember(now.minute, locale) {
        now.format(DateTimeFormatter.ofPattern("h:mm", locale))
    }
    val date = remember(now.dayOfYear, locale) {
        now.format(DateTimeFormatter.ofPattern("EEE, MMM d", locale))
    }
    val foreground = MaterialTheme.colorScheme.onSurface
    val horizontalAlignment = if (alignment == LauncherHomeGlanceAlignment.CENTER) {
        Alignment.CenterHorizontally
    } else {
        Alignment.Start
    }
    val textAlign = if (alignment == LauncherHomeGlanceAlignment.CENTER) {
        TextAlign.Center
    } else {
        TextAlign.Start
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("launcher-home-at-a-glance")
            .combinedClickable(
                enabled = canPromote,
                onClick = {},
                onLongClick = onRequestMovableGlance,
            )
            .semantics {
                contentDescription = if (canPromote) {
                    "Time, date and weather. Long press to make this a movable Glance widget."
                } else {
                    "Time, date and weather. Unlock Home layout to make this movable."
                }
            }
            .padding(horizontal = 4.dp, vertical = if (compact) 2.dp else 4.dp),
        horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space3),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                time,
                modifier = if (alignment == LauncherHomeGlanceAlignment.CENTER) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier
                },
                style = if (compact) {
                    MaterialTheme.typography.displaySmall
                } else {
                    MaterialTheme.typography.displayLarge
                },
                color = foreground,
                fontWeight = FontWeight.Light,
                textAlign = textAlign,
                maxLines = 1,
            )
            Text(
                date,
                modifier = if (alignment == LauncherHomeGlanceAlignment.CENTER) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier
                },
                style = if (compact) {
                    MaterialTheme.typography.bodyMedium
                } else {
                    MaterialTheme.typography.titleLarge
                },
                color = foreground.copy(alpha = 0.90f),
                textAlign = textAlign,
                maxLines = 1,
            )
            if (!compact) {
                Spacer(Modifier.height(5.dp))
                Text(
                    "A calmer, more private you",
                    modifier = if (alignment == LauncherHomeGlanceAlignment.CENTER) {
                        Modifier.fillMaxWidth()
                    } else {
                        Modifier
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = foreground.copy(alpha = 0.72f),
                    textAlign = textAlign,
                    maxLines = 1,
                )
                Spacer(Modifier.height(5.dp))
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(2.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.82f),
                            RoundedCornerShape(GlazeMetrics.radiusPill),
                        ),
                )
            }
        }
        LauncherWeatherStatusChip(
            foreground = foreground,
            compact = compact,
        )
    }
}

@Composable
private fun HomeFavoritesGrid(
    apps: List<LauncherActivityInfo>,
    suggestedApps: List<LauncherActivityInfo>,
    allApps: List<LauncherActivityInfo>,
    folders: List<LauncherFolder>,
    columns: Int,
    rows: Int,
    primaryHomePage: WorkspaceRenderedHomePage?,
    homePages: List<WorkspaceRenderedHomePage>,
    iconScale: Float,
    showLabels: Boolean,
    spacing: LauncherHomeSpacing,
    layoutLocked: Boolean,
    homeLabelOverrides: Map<String, String>,
    cellBounds: MutableMap<Pair<Int, Int>, Rect>,
    onGridBoundsChanged: (Rect) -> Unit,
    editMode: Boolean,
    activeDrag: LauncherAppDragData?,
    dragPoint: Offset?,
    onBeginLocalDrag: (LauncherAppDragData, Offset) -> Unit,
    onUpdateLocalDrag: (Offset) -> Unit,
    onEndLocalDrag: (LauncherAppDragData, Offset) -> Unit,
    onCancelLocalDrag: () -> Unit,
    onLaunchApp: (LauncherActivityInfo) -> Unit,
    onManageApp: (LauncherActivityInfo, Rect?) -> Unit,
    onCreateAndroidWidgetView: (Int) -> AppWidgetHostView?,
    onManageWidget: (WorkspaceRenderedHomeWidget) -> Unit,
    onMoveWidget: (WorkspaceRenderedHomeWidget, Int, Int) -> Unit,
    onMoveWidgetToPageCell: (WorkspaceRenderedHomeWidget, String, Int, Int) -> Unit,
    onMoveHomeFolderToCell: (LauncherFolder, Int, Int) -> Unit,
    onMoveHomeFolderToPageCell: (LauncherFolder, String, Int, Int) -> Unit,
    onOpenFolder: (LauncherFolder) -> Unit,
    onOpenWidgetSearch: () -> Unit,
    onOpenWidgetApps: () -> Unit,
    onOpenWidgetEditor: () -> Unit,
    onOpenWidgetSettings: () -> Unit,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridSpacing = when (spacing) {
        LauncherHomeSpacing.COMPACT -> 2.dp
        LauncherHomeSpacing.BALANCED -> GlazeMetrics.space1
        LauncherHomeSpacing.AIRY -> GlazeMetrics.space2
    }
    val tileHeight = when (spacing) {
        LauncherHomeSpacing.COMPACT -> 72.dp
        LauncherHomeSpacing.BALANCED -> 78.dp
        LauncherHomeSpacing.AIRY -> 86.dp
    }
    val storedPlacements = remember(primaryHomePage) {
        primaryHomePage?.appPlacements?.associateBy { it.appKey }.orEmpty()
    }
    val widgets = remember(primaryHomePage, columns, rows) {
        primaryHomePage?.widgetPlacements.orEmpty().filter { widget ->
            widget.cellX in 0 until columns &&
                widget.cellY in 0 until rows &&
                widget.spanX > 0 &&
                widget.spanY > 0 &&
                widget.cellX + widget.spanX <= columns &&
                widget.cellY + widget.spanY <= rows
        }
    }
    val foldersById = remember(folders) { folders.associateBy { it.id } }
    val homeFolders = remember(primaryHomePage, foldersById, columns, rows) {
        primaryHomePage?.folderPlacements.orEmpty().mapNotNull { placement ->
            val folder = foldersById[placement.folderId] ?: return@mapNotNull null
            if (
                placement.cellX !in 0 until columns ||
                placement.cellY !in 0 until rows
            ) return@mapNotNull null
            placement to folder
        }
    }
    // Every cell is measured for widget drops, including occupied cells. Room rejects collisions.
    val widgetTargetBounds = remember { mutableStateMapOf<Pair<Int, Int>, Rect>() }
    var folderGridBounds by remember(columns, rows) { mutableStateOf<Rect?>(null) }
    val crossPageFolderEdgeThresholdPx = with(LocalDensity.current) { 36.dp.toPx() }
    LaunchedEffect(columns, rows) {
        widgetTargetBounds.keys.filter { (x, y) -> x !in 0 until columns || y !in 0 until rows }
            .forEach(widgetTargetBounds::remove)
    }
    val blockedCells = remember(widgets, homeFolders) {
        buildSet {
            widgets.forEach { widget ->
                for (cellY in widget.cellY until widget.cellY + widget.spanY) {
                    for (cellX in widget.cellX until widget.cellX + widget.spanX) {
                        add(cellX to cellY)
                    }
                }
            }
            homeFolders.forEach { (placement, _) ->
                add(placement.cellX to placement.cellY)
            }
        }
    }
    val useSpatialPlacement = remember(apps, storedPlacements, columns, rows) {
        apps.isNotEmpty() && apps.all { app ->
            val placement = storedPlacements[app.workspaceKey()]
            placement?.cellX != null &&
                placement.cellY != null &&
                placement.cellX in 0 until columns &&
                placement.cellY in 0 until rows
        }
    }
    val appPlacements = remember(apps, storedPlacements, useSpatialPlacement, columns, rows) {
        buildMap<String, Pair<Int, Int>> {
            if (useSpatialPlacement) {
                apps.forEach { app ->
                    val placement = storedPlacements[app.workspaceKey()] ?: return@forEach
                    val cellX = placement.cellX ?: return@forEach
                    val cellY = placement.cellY ?: return@forEach
                    put(app.workspaceKey(), cellX to cellY)
                }
            } else {
                apps.take(columns * rows).forEachIndexed { index, app ->
                    put(app.workspaceKey(), (index % columns) to (index / columns))
                }
            }
        }
    }

    val suggestedPlacements = remember(
        suggestedApps,
        appPlacements,
        blockedCells,
        columns,
        rows,
    ) {
        val occupied = blockedCells + appPlacements.values.toSet()
        val available = buildList {
            for (cellY in (rows - 1) downTo 0) {
                for (cellX in 0 until columns) {
                    val coordinate = cellX to cellY
                    if (coordinate !in occupied) add(coordinate)
                }
            }
        }
        suggestedApps.zip(available)
            .associate { (app, coordinate) -> app.workspaceKey() to coordinate }
    }

    LaunchedEffect(blockedCells) {
        blockedCells.forEach { coordinate -> cellBounds.remove(coordinate) }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(tileHeight * rows + gridSpacing * (rows - 1))
            .onGloballyPositioned {
                val bounds = it.boundsInRoot()
                folderGridBounds = bounds
                onGridBoundsChanged(bounds)
            },
    ) {
        val cellWidth = (maxWidth - gridSpacing * (columns - 1)) / columns
        val stepX = cellWidth + gridSpacing
        val stepY = tileHeight + gridSpacing

        repeat(rows) { cellY ->
            repeat(columns) { cellX ->
                val coordinate = cellX to cellY
                val blockedByPlacedItem = coordinate in blockedCells
                val cellHovered = !blockedByPlacedItem &&
                    activeDrag != null &&
                    dragPoint?.let { point -> cellBounds[coordinate]?.contains(point) } == true
                Box(
                    modifier = Modifier
                        .offset(x = stepX * cellX, y = stepY * cellY)
                        .size(width = cellWidth, height = tileHeight)
                        .testTag("launcher-home-cell-$cellX-$cellY")
                        .onGloballyPositioned {
                            val bounds = it.boundsInRoot()
                            widgetTargetBounds[coordinate] = bounds
                            if (blockedByPlacedItem) {
                                cellBounds.remove(coordinate)
                            } else {
                                cellBounds[coordinate] = bounds
                            }
                        }
                        .background(
                            when {
                                cellHovered -> Color.White.copy(alpha = 0.16f)
                                editMode -> Color.White.copy(alpha = 0.045f)
                                else -> Color.Transparent
                            },
                            RoundedCornerShape(GlazeMetrics.radiusLarge),
                        )
                        .then(
                            if (editMode) {
                                Modifier.border(
                                    1.dp,
                                    Color.White.copy(alpha = 0.12f),
                                    RoundedCornerShape(GlazeMetrics.radiusLarge),
                                )
                            } else {
                                Modifier
                            },
                        ),
                )
            }
        }

        apps.forEach { app ->
            val coordinate = appPlacements[app.workspaceKey()] ?: return@forEach
            val appKey = app.workspaceKey()
            HomeFavoriteTile(
                app = app,
                displayLabel = homeLabelOverrides[appKey] ?: app.label.toString(),
                iconScale = iconScale,
                showLabel = showLabels,
                layoutLocked = layoutLocked,
                editMode = editMode,
                dragData = if (layoutLocked) null else {
                    LauncherAppDragData(
                        appKey = appKey,
                        origin = LauncherAppDragOrigin.HOME,
                        sourcePageId = WorkspaceLegacyImportMapper.HOME_PAGE_ID,
                    )
                },
                onBeginLocalDrag = onBeginLocalDrag,
                onUpdateLocalDrag = onUpdateLocalDrag,
                onEndLocalDrag = onEndLocalDrag,
                onCancelLocalDrag = onCancelLocalDrag,
                onLaunchApp = onLaunchApp,
                onManageApp = onManageApp,
                onSwipeUp = onSwipeUp,
                onSwipeDown = onSwipeDown,
                modifier = Modifier
                    .offset(x = stepX * coordinate.first, y = stepY * coordinate.second)
                    .size(width = cellWidth, height = tileHeight)
                    .testTag("launcher-home-app-$appKey"),
            )
        }

        suggestedApps.forEach { app ->
            val coordinate = suggestedPlacements[app.workspaceKey()] ?: return@forEach
            val appKey = app.workspaceKey()
            HomeFavoriteTile(
                app = app,
                displayLabel = app.label.toString(),
                iconScale = iconScale,
                showLabel = showLabels,
                layoutLocked = true,
                editMode = false,
                dragData = null,
                onBeginLocalDrag = onBeginLocalDrag,
                onUpdateLocalDrag = onUpdateLocalDrag,
                onEndLocalDrag = onEndLocalDrag,
                onCancelLocalDrag = onCancelLocalDrag,
                onLaunchApp = onLaunchApp,
                onManageApp = onManageApp,
                onSwipeUp = onSwipeUp,
                onSwipeDown = onSwipeDown,
                modifier = Modifier
                    .offset(x = stepX * coordinate.first, y = stepY * coordinate.second)
                    .size(width = cellWidth, height = tileHeight)
                    .testTag("launcher-home-suggested-$appKey"),
            )
        }

        homeFolders.forEach { (placement, folder) ->
            key(placement.itemId) {
                HomeFolderTile(
                    folder = folder,
                    allApps = allApps,
                    showLabel = showLabels,
                    editMode = editMode,
                    layoutLocked = layoutLocked,
                    onOpen = { onOpenFolder(folder) },
                    onDrop = { selected, point ->
                        val edgeTarget = folderGridBounds?.let { bounds ->
                            homePageEdgeDropTarget(
                                pages = homePages,
                                currentPageId = WorkspaceLegacyImportMapper.HOME_PAGE_ID,
                                dropX = point.x,
                                dropY = point.y,
                                surfaceLeftPx = bounds.left,
                                surfaceTopPx = bounds.top,
                                surfaceRightPx = bounds.right,
                                surfaceBottomPx = bounds.bottom,
                                edgeThresholdPx = crossPageFolderEdgeThresholdPx,
                                columns = columns,
                                rows = rows,
                            )
                        }
                        if (edgeTarget != null) {
                            onMoveHomeFolderToPageCell(
                                selected,
                                edgeTarget.pageId,
                                edgeTarget.cellX,
                                edgeTarget.cellY,
                            )
                        } else {
                            widgetTargetBounds.entries.firstOrNull { (_, bounds) ->
                                bounds.contains(point)
                            }?.key?.let { (cellX, cellY) ->
                                onMoveHomeFolderToCell(selected, cellX, cellY)
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
        }

        widgets.forEach { widget ->
            key(widget.itemId) {
                HomeWidgetTile(
                    widget = widget,
                    editMode = editMode,
                    onCreateAndroidWidgetView = onCreateAndroidWidgetView,
                    onManageWidget = onManageWidget,
                    layoutLocked = layoutLocked,
                    onOpenSearch = onOpenWidgetSearch,
                    onOpenApps = onOpenWidgetApps,
                    onOpenHomeEditor = onOpenWidgetEditor,
                    onOpenSettings = onOpenWidgetSettings,
                    onDropWidget = { candidate, point ->
                        val edgeTarget = folderGridBounds?.let { bounds ->
                            homePageEdgeDropTarget(
                                pages = homePages,
                                currentPageId = WorkspaceLegacyImportMapper.HOME_PAGE_ID,
                                dropX = point.x,
                                dropY = point.y,
                                surfaceLeftPx = bounds.left,
                                surfaceTopPx = bounds.top,
                                surfaceRightPx = bounds.right,
                                surfaceBottomPx = bounds.bottom,
                                edgeThresholdPx = crossPageFolderEdgeThresholdPx,
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
                            widgetTargetBounds.entries.firstOrNull { (_, bounds) ->
                                bounds.contains(point)
                            }?.key?.let { (cellX, cellY) ->
                                onMoveWidget(candidate, cellX, cellY)
                            }
                        }
                    },
                    modifier = Modifier
                        .offset(
                            x = stepX * widget.cellX,
                            y = stepY * widget.cellY,
                        )
                        .size(
                            width = cellWidth * widget.spanX + gridSpacing * (widget.spanX - 1),
                            height = tileHeight * widget.spanY + gridSpacing * (widget.spanY - 1),
                        ),
                )
            }
        }
    }
}

@Composable
internal fun HomeFolderTile(
    folder: LauncherFolder,
    allApps: List<LauncherActivityInfo>,
    showLabel: Boolean,
    editMode: Boolean,
    onOpen: () -> Unit,
    labelOnWallpaper: Boolean = true,
    layoutLocked: Boolean = true,
    fixedGridGeometry: Boolean = false,
    compact: Boolean = false,
    iconScale: Float = 1f,
    onDrop: ((LauncherFolder, Offset) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val inheritedShape = LocalLauncherIconAppearance.current.shape
    val previewLayout by com.goreecloud.launcher.core.launcher.LauncherFolderAppearance.layout
        .collectAsState()
    val previewShape by com.goreecloud.launcher.core.launcher.LauncherFolderAppearance.shape
        .collectAsState()
    val previewSize by com.goreecloud.launcher.core.launcher.LauncherFolderAppearance.size
        .collectAsState()
    val previewSurface by com.goreecloud.launcher.core.launcher.LauncherFolderAppearance.surface
        .collectAsState()
    val previewOutline by com.goreecloud.launcher.core.launcher.LauncherFolderAppearance.outline
        .collectAsState()
    val folderShape = when (previewShape) {
        com.goreecloud.launcher.core.launcher.LauncherFolderPreviewShape.FOLLOW_ICONS ->
            if (inheritedShape == LauncherIconShape.ORIGINAL) RoundedCornerShape(16.dp)
            else inheritedShape.toLauncherComposeShape()
        com.goreecloud.launcher.core.launcher.LauncherFolderPreviewShape.ROUND ->
            CircleShape
        com.goreecloud.launcher.core.launcher.LauncherFolderPreviewShape.SQUIRCLE ->
            RoundedCornerShape(21.dp)
        com.goreecloud.launcher.core.launcher.LauncherFolderPreviewShape.ROUNDED_SQUARE ->
            RoundedCornerShape(13.dp)
    }
    val folderIconSize = when (previewSize) {
        com.goreecloud.launcher.core.launcher.LauncherFolderPreviewSize.SMALL -> 44.dp
        com.goreecloud.launcher.core.launcher.LauncherFolderPreviewSize.MEDIUM -> 52.dp
        com.goreecloud.launcher.core.launcher.LauncherFolderPreviewSize.LARGE -> 59.dp
    }
    val folderBackground = when (previewSurface) {
        com.goreecloud.launcher.core.launcher.LauncherFolderPreviewSurface.GLASS ->
            if (labelOnWallpaper) GlazeAtmosphere.canvasBlack.copy(alpha = 0.38f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
        com.goreecloud.launcher.core.launcher.LauncherFolderPreviewSurface.SOLID ->
            MaterialTheme.colorScheme.surfaceVariant
        com.goreecloud.launcher.core.launcher.LauncherFolderPreviewSurface.ACCENT ->
            MaterialTheme.colorScheme.primaryContainer
    }

    val appsByKey = remember(allApps) { allApps.associateBy { it.workspaceKey() } }
    val folderApps = remember(folder, appsByKey) {
        folder.appKeys.mapNotNull(appsByKey::get)
    }
    val previewApps = remember(folderApps) { folderApps.take(4) }
    val dragThreshold = with(LocalDensity.current) { 12.dp.toPx() }
    var tileBounds by remember(folder.id) { mutableStateOf<Rect?>(null) }
    var dragStart by remember(folder.id) { mutableStateOf<Offset?>(null) }
    var dragOffset by remember(folder.id) { mutableStateOf(Offset.Zero) }
    var dragging by remember(folder.id) { mutableStateOf(false) }
    val moveGesture = if (layoutLocked || onDrop == null) Modifier else Modifier
        .pointerInput(folder.id, dragThreshold) {
            detectDragGesturesAfterLongPress(
                onDragStart = {
                    dragStart = tileBounds?.center
                    dragOffset = Offset.Zero
                    dragging = true
                },
                onDrag = { event, amount ->
                    event.consume()
                    dragOffset += amount
                },
                onDragEnd = {
                    val origin = dragStart
                    if (origin != null && (
                            kotlin.math.abs(dragOffset.x) >= dragThreshold ||
                                kotlin.math.abs(dragOffset.y) >= dragThreshold
                        )
                    ) onDrop(folder, origin + dragOffset)
                    dragging = false
                    dragOffset = Offset.Zero
                    dragStart = null
                },
                onDragCancel = {
                    dragging = false
                    dragOffset = Offset.Zero
                    dragStart = null
                },
            )
        }
    BoxWithConstraints(
        modifier = modifier
            .padding(horizontal = 2.dp, vertical = 2.dp)
            .onGloballyPositioned { tileBounds = it.boundsInRoot() }
            .then(moveGesture)
            .graphicsLayer {
                translationX = if (dragging) dragOffset.x else 0f
                translationY = if (dragging) dragOffset.y else 0f
                alpha = if (dragging) 0.76f else 1f
            }
            .clickable(onClick = onOpen)
            .testTag("launcher-home-folder-" + folder.id),
    ) {
        val useFixedGridGeometry = fixedGridGeometry && showLabel
        val iconSlotHeight = if (useFixedGridGeometry) {
            LauncherDrawerGridPolicy.ICON_SLOT_HEIGHT_DP.dp
        } else {
            launcherHomeTileIconSlotHeightDp(
                availableHeightDp = maxHeight.value,
                showLabel = showLabel,
            ).dp
        }
        val scaledFolderIconSize = if (fixedGridGeometry) {
            (folderIconSize.value * iconScale.coerceIn(0.85f, 1.15f)).dp
        } else {
            folderIconSize
        }
        val renderedFolderIconSize = scaledFolderIconSize.coerceAtMost(iconSlotHeight)
        val previewScale = (renderedFolderIconSize.value / 52f).coerceIn(0.82f, 1.12f)

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (useFixedGridGeometry) Arrangement.Top else Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(iconSlotHeight),
                contentAlignment = Alignment.Center,
            ) {
                Box(modifier = Modifier.size(renderedFolderIconSize)) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = folderShape,
                        color = folderBackground,
                        border = if (previewOutline) BorderStroke(
                            1.dp,
                            if (labelOnWallpaper) {
                                Color.White.copy(alpha = if (editMode) 0.38f else 0.24f)
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                        ) else null,
                    ) {
                        if (previewApps.isEmpty()) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "＋",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = if (labelOnWallpaper) {
                                        Color.White.copy(alpha = 0.78f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                val spots = when (previewLayout) {
                                    com.goreecloud.launcher.core.launcher
                                        .LauncherFolderPreviewLayout.GRID ->
                                        listOf(
                                            (-11f * previewScale).dp to
                                                (-11f * previewScale).dp,
                                            (11f * previewScale).dp to
                                                (-11f * previewScale).dp,
                                            (-11f * previewScale).dp to
                                                (11f * previewScale).dp,
                                            (11f * previewScale).dp to
                                                (11f * previewScale).dp,
                                        )
                                    com.goreecloud.launcher.core.launcher
                                        .LauncherFolderPreviewLayout.RADIAL ->
                                        listOf(
                                            0.dp to (-13f * previewScale).dp,
                                            (13f * previewScale).dp to 0.dp,
                                            0.dp to (13f * previewScale).dp,
                                            (-13f * previewScale).dp to 0.dp,
                                        )
                                    com.goreecloud.launcher.core.launcher
                                        .LauncherFolderPreviewLayout.STACK ->
                                        listOf(
                                            (-9f * previewScale).dp to
                                                (-9f * previewScale).dp,
                                            (-3f * previewScale).dp to
                                                (-3f * previewScale).dp,
                                            (3f * previewScale).dp to
                                                (3f * previewScale).dp,
                                            (9f * previewScale).dp to
                                                (9f * previewScale).dp,
                                        )
                                    com.goreecloud.launcher.core.launcher
                                        .LauncherFolderPreviewLayout.FAN ->
                                        listOf(
                                            (-15f * previewScale).dp to
                                                (7f * previewScale).dp,
                                            (-5f * previewScale).dp to
                                                (-6f * previewScale).dp,
                                            (5f * previewScale).dp to
                                                (-6f * previewScale).dp,
                                            (15f * previewScale).dp to
                                                (7f * previewScale).dp,
                                        )
                                    com.goreecloud.launcher.core.launcher
                                        .LauncherFolderPreviewLayout.LINE ->
                                        listOf(
                                            (-17f * previewScale).dp to 0.dp,
                                            (-6f * previewScale).dp to 0.dp,
                                            (6f * previewScale).dp to 0.dp,
                                            (17f * previewScale).dp to 0.dp,
                                        )
                                }
                                val previewIconSize = when (previewLayout) {
                                    com.goreecloud.launcher.core.launcher
                                        .LauncherFolderPreviewLayout.STACK ->
                                        (20f * previewScale).dp
                                    com.goreecloud.launcher.core.launcher
                                        .LauncherFolderPreviewLayout.LINE ->
                                        (13f * previewScale).dp
                                    else ->
                                        (17f * previewScale).dp
                                }
                                previewApps.forEachIndexed { index, app ->
                                    key(app.workspaceKey()) {
                                        val icon = rememberLauncherAppIcon(app)
                                        val position = spots[index]
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .offset(
                                                    x = position.first,
                                                    y = position.second,
                                                )
                                                .size(previewIconSize),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            if (icon != null) {
                                                Image(
                                                    bitmap = icon,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .launcherIconMask(),
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(
                                                            MaterialTheme.colorScheme
                                                                .onSurface
                                                                .copy(alpha = 0.12f),
                                                            RoundedCornerShape(5.dp),
                                                        ),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    LauncherFolderBadgeMark(
                        folderApps,
                        modifier = launcherBadgePositionModifier(),
                    )
                }
            }

            if (showLabel) {
                Spacer(Modifier.height(if (compact) 3.dp else 4.dp))
                Text(
                    folder.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (useFixedGridGeometry) {
                                Modifier.height(
                                    if (compact) {
                                        LauncherDrawerGridPolicy.COMPACT_LABEL_SLOT_HEIGHT_DP.dp
                                    } else {
                                        LauncherDrawerGridPolicy.GRID_LABEL_SLOT_HEIGHT_DP.dp
                                    },
                                )
                            } else {
                                Modifier.heightIn(min = 14.dp)
                            },
                        ),
                    style = if (labelOnWallpaper) {
                        MaterialTheme.typography.labelSmall.copy(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.60f),
                                offset = Offset(0f, 1.5f),
                                blurRadius = 5f,
                            ),
                        )
                    } else MaterialTheme.typography.labelSmall,
                    color = if (labelOnWallpaper) Color.White else Color.Unspecified,
                    textAlign = TextAlign.Center,
                    maxLines = if (compact) 1 else if (useFixedGridGeometry) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private enum class LauncherHomeOverlay {
    NONE,
    EDITOR,
    WIDGET_PICKER,
}

private fun Modifier.observeLongPressWithoutConsuming(
    onLongPress: () -> Unit,
): Modifier = pointerInput(onLongPress) {
    awaitEachGesture {
        val down = awaitFirstDown(
            requireUnconsumed = false,
            pass = PointerEventPass.Initial,
        )
        val endedBeforeTimeout = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id }
                    ?: return@withTimeoutOrNull true
                if (!change.pressed) return@withTimeoutOrNull true
                if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                    return@withTimeoutOrNull true
                }
            }
        }
        if (endedBeforeTimeout == null) {
            onLongPress()
        }
    }
}

@Composable
internal fun HomeWidgetTile(
    widget: WorkspaceRenderedHomeWidget,
    editMode: Boolean,
    layoutLocked: Boolean,
    onCreateAndroidWidgetView: (Int) -> AppWidgetHostView?,
    onManageWidget: (WorkspaceRenderedHomeWidget) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenApps: () -> Unit,
    onOpenHomeEditor: () -> Unit,
    onOpenSettings: () -> Unit,
    onDropWidget: (WorkspaceRenderedHomeWidget, Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    var tileBounds by remember(widget.itemId) { mutableStateOf<Rect?>(null) }
    var startRoot by remember(widget.itemId) { mutableStateOf<Offset?>(null) }
    var dragDelta by remember(widget.itemId) { mutableStateOf(Offset.Zero) }
    val dragThreshold = with(LocalDensity.current) { 12.dp.toPx() }
    Box(
        modifier = modifier
            .testTag("launcher-home-widget-" + widget.itemId)
            .graphicsLayer {
                translationX = dragDelta.x
                translationY = dragDelta.y
                alpha = if (startRoot != null) 0.80f else 1f
            }
            .onGloballyPositioned { tileBounds = it.boundsInRoot() }
            .padding(2.dp)
            .then(
                when {
                    !editMode && !layoutLocked -> Modifier.pointerInput(widget.itemId, dragThreshold) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { pointer ->
                                startRoot = tileBounds?.topLeft?.plus(pointer)
                                dragDelta = Offset.Zero
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                dragDelta += amount
                            },
                            onDragEnd = {
                                val origin = startRoot
                                if (
                                    origin != null &&
                                    launcherWidgetDragMoved(dragDelta, dragThreshold)
                                ) {
                                    onDropWidget(widget, origin + dragDelta)
                                } else {
                                    onManageWidget(widget)
                                }
                                startRoot = null
                                dragDelta = Offset.Zero
                            },
                            onDragCancel = {
                                startRoot = null
                                dragDelta = Offset.Zero
                            },
                        )
                    }
                    !editMode -> Modifier.observeLongPressWithoutConsuming {
                        onManageWidget(widget)
                    }
                    else -> Modifier
                }
            )
            .then(
                if (editMode) {
                    Modifier.border(
                        1.dp,
                        Color.White.copy(alpha = 0.24f),
                        RoundedCornerShape(GlazeMetrics.radiusExtraLarge),
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        when (val descriptor = widget.descriptor) {
            is WorkspaceWidgetDescriptor.BuiltIn -> {
                LauncherBuiltInWidget(
                    typeId = descriptor.typeId,
                    onOpenSearch = onOpenSearch,
                    onOpenApps = onOpenApps,
                    onOpenHomeEditor = onOpenHomeEditor,
                    onOpenSettings = onOpenSettings,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            is WorkspaceWidgetDescriptor.Android -> {
                val hostView = remember(widget.itemId, descriptor.appWidgetId) {
                    onCreateAndroidWidgetView(descriptor.appWidgetId)
                }
                if (hostView != null) {
                    AndroidView(
                        factory = { hostView },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    LauncherUnavailableWidgetSurface(
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        if (editMode && !layoutLocked) {
            // An edit-only touch layer owns widget movement; live Android widget taps are untouched
            // outside edit mode and platform widget binding/authorization is unchanged.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(widget.itemId) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { pointer ->
                                startRoot = tileBounds?.topLeft?.plus(pointer)
                                dragDelta = Offset.Zero
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                dragDelta += amount
                            },
                            onDragEnd = {
                                val origin = startRoot
                                if (
                                    origin != null &&
                                    launcherWidgetDragMoved(dragDelta, dragThreshold)
                                ) {
                                    onDropWidget(widget, origin + dragDelta)
                                }
                                startRoot = null
                                dragDelta = Offset.Zero
                            },
                            onDragCancel = {
                                startRoot = null
                                dragDelta = Offset.Zero
                            },
                        )
                    }
                    .clickable { onManageWidget(widget) },
            )
        }
        if (editMode) {
            FilledTonalButton(
                onClick = { onManageWidget(widget) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text("Edit", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun LauncherUnavailableWidgetSurface(
    modifier: Modifier = Modifier,
) {
    val presentationContext = LocalGlazeV16PresentationContext.current
    val resolvedPresentation = remember(presentationContext) {
        GlazeV16PresentationPolicy.resolve(
            requestedMaterial = GlazeV16MaterialRole.FUNCTIONAL_GLASS,
            context = presentationContext,
        )
    }
    val usesWallpaperGlass = launcherUsesWallpaperGlass(
        resolvedPresentation.materialRole,
    )
    val foreground = if (usesWallpaperGlass) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val background = when (resolvedPresentation.materialRole) {
        GlazeV16MaterialRole.SOLID -> MaterialTheme.colorScheme.surface
        GlazeV16MaterialRole.RAISED -> MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)
        else -> GlazeAtmosphere.canvasBlack.copy(alpha = 0.34f)
    }
    val outline = if (usesWallpaperGlass) {
        Color.White.copy(alpha = 0.10f)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(GlazeMetrics.radiusExtraLarge),
        color = background,
        border = BorderStroke(1.dp, outline),
        shadowElevation = when (resolvedPresentation.materialRole) {
            GlazeV16MaterialRole.SOLID -> 1.dp
            GlazeV16MaterialRole.RAISED -> 2.dp
            else -> 1.dp
        },
    ) {
        Box(
            modifier = Modifier.padding(GlazeMetrics.space3),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Widget unavailable",
                color = foreground.copy(alpha = 0.78f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

private data class LauncherBatterySnapshot(
    val percent: Int?,
    val charging: Boolean,
)

private fun launcherBatterySnapshot(intent: Intent?): LauncherBatterySnapshot {
    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val percent = if (level >= 0 && scale > 0) {
        ((level.toFloat() / scale.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
    } else {
        null
    }
    val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    return LauncherBatterySnapshot(
        percent = percent,
        charging =
            status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL,
    )
}

@Composable
private fun rememberLauncherBatterySnapshot(): LauncherBatterySnapshot {
    val context = LocalContext.current.applicationContext
    var snapshot by remember(context) {
        mutableStateOf(LauncherBatterySnapshot(percent = null, charging = false))
    }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                snapshot = launcherBatterySnapshot(intent)
            }
        }
        // ACTION_BATTERY_CHANGED is a protected system broadcast. Register only for that
        // explicit action and unregister with the widget lifecycle; no polling or permission.
        val sticky = ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_EXPORTED,
        )
        snapshot = launcherBatterySnapshot(sticky)
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }
    return snapshot
}

@Composable
private fun LauncherBuiltInWidget(
    typeId: String,
    onOpenSearch: () -> Unit,
    onOpenApps: () -> Unit,
    onOpenHomeEditor: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(typeId) {
        while (true) {
            delay(30_000)
            now = LocalDateTime.now()
        }
    }

    val presentationContext = LocalGlazeV16PresentationContext.current
    val resolvedPresentation = remember(presentationContext) {
        GlazeV16PresentationPolicy.resolve(
            requestedMaterial = GlazeV16MaterialRole.FUNCTIONAL_GLASS,
            context = presentationContext,
        )
    }
    val usesWallpaperGlass = launcherUsesWallpaperGlass(
        resolvedPresentation.materialRole,
    )
    val isOpenGlance = typeId == WorkspaceWidgetCatalog.GLANCE
    val foreground = if (isOpenGlance) {
        MaterialTheme.colorScheme.onSurface
    } else if (usesWallpaperGlass) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val background = if (isOpenGlance) {
        Color.Transparent
    } else when (resolvedPresentation.materialRole) {
        GlazeV16MaterialRole.SOLID -> MaterialTheme.colorScheme.surface
        GlazeV16MaterialRole.RAISED -> MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.76f)
    }
    val outline = if (isOpenGlance) {
        Color.Transparent
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    }
    val widgetGradientColors = if (isOpenGlance) {
        listOf(Color.Transparent, Color.Transparent)
    } else if (usesWallpaperGlass) {
        listOf(
            MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
        )
    } else {
        listOf(background, background)
    }
    val insetFill = if (usesWallpaperGlass) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)
    } else {
        foreground.copy(alpha = 0.08f)
    }
    val insetOutline = if (usesWallpaperGlass) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(GlazeMetrics.radiusExtraLarge),
        color = background,
        border = if (isOpenGlance) null else BorderStroke(1.dp, outline),
        shadowElevation = if (isOpenGlance) {
            0.dp
        } else {
            when (resolvedPresentation.materialRole) {
                GlazeV16MaterialRole.SOLID -> 1.dp
                GlazeV16MaterialRole.RAISED -> 2.dp
                else -> 1.dp
            }
        },
    ) {
        when (typeId) {
            WorkspaceWidgetCatalog.GLANCE -> {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space3),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                    ) {
                        Text(
                            now.format(
                                DateTimeFormatter.ofPattern(
                                    "h:mm",
                                    Locale.getDefault(),
                                ),
                            ),
                            style = MaterialTheme.typography.displayLarge,
                            color = foreground,
                            fontWeight = FontWeight.Light,
                            maxLines = 1,
                        )
                        Text(
                            now.format(
                                DateTimeFormatter.ofPattern(
                                    "EEE, MMM d",
                                    Locale.getDefault(),
                                ),
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = foreground.copy(alpha = 0.88f),
                            maxLines = 1,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "A calmer, more private you",
                            style = MaterialTheme.typography.bodySmall,
                            color = foreground.copy(alpha = 0.68f),
                            maxLines = 1,
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(2.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.82f),
                                    RoundedCornerShape(GlazeMetrics.radiusPill),
                                ),
                        )
                    }
                    LauncherWeatherStatusChip(
                        foreground = foreground,
                        compact = true,
                    )
                }
            }
            WorkspaceWidgetCatalog.CALENDAR -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(widgetGradientColors),
                            RoundedCornerShape(GlazeMetrics.opticalHero),
                        )
                        .padding(GlazeMetrics.space3),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            now.format(
                                DateTimeFormatter.ofPattern(
                                    "MMM yyyy",
                                    Locale.getDefault(),
                                ),
                            ).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = foreground.copy(alpha = 0.68f),
                            fontWeight = FontWeight.SemiBold,
                        )
                        Surface(
                            shape = RoundedCornerShape(GlazeMetrics.radiusPill),
                            color = insetFill,
                            border = BorderStroke(1.dp, insetOutline),
                        ) {
                            Text(
                                "Today",
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = foreground.copy(alpha = 0.86f),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    Text(
                        now.dayOfMonth.toString(),
                        style = MaterialTheme.typography.displayMedium,
                        color = foreground,
                        fontWeight = FontWeight.Light,
                    )
                    Text(
                        now.format(
                            DateTimeFormatter.ofPattern(
                                "EEEE",
                                Locale.getDefault(),
                            ),
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        color = foreground,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            WorkspaceWidgetCatalog.WEATHER -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(widgetGradientColors),
                            RoundedCornerShape(GlazeMetrics.opticalHero),
                        )
                        .padding(GlazeMetrics.space3),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "LOCAL WEATHER",
                            style = MaterialTheme.typography.labelSmall,
                            color = foreground.copy(alpha = 0.70f),
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            now.format(
                                DateTimeFormatter.ofPattern(
                                    "h:mm",
                                    Locale.getDefault(),
                                ),
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = foreground.copy(alpha = 0.82f),
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    LauncherWeatherStatusChip(
                        foreground = foreground,
                        compact = true,
                    )
                }
            }
            WorkspaceWidgetCatalog.CLOCK -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(GlazeMetrics.space3),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "CLOCK",
                            style = MaterialTheme.typography.labelSmall,
                            color = foreground.copy(alpha = 0.58f),
                            fontWeight = FontWeight.SemiBold,
                        )
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.90f),
                                    CircleShape,
                                ),
                        )
                    }
                    Text(
                        now.format(DateTimeFormatter.ofPattern("h:mm", Locale.getDefault())),
                        style = MaterialTheme.typography.displayMedium,
                        color = foreground,
                        fontWeight = FontWeight.Light,
                        maxLines = 1,
                    )
                    Surface(
                        shape = RoundedCornerShape(GlazeMetrics.radiusPill),
                        color = foreground.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, foreground.copy(alpha = 0.08f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                now.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault())),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                now.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())),
                                style = MaterialTheme.typography.labelMedium,
                                color = foreground.copy(alpha = 0.80f),
                            )
                        }
                    }
                }
            }
            WorkspaceWidgetCatalog.COMPACT_CLOCK -> {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = GlazeMetrics.space3),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        now.format(DateTimeFormatter.ofPattern("h:mm", Locale.getDefault())),
                        style = MaterialTheme.typography.headlineMedium,
                        color = foreground,
                        fontWeight = FontWeight.Light,
                    )
                    Text(
                        now.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault())),
                        style = MaterialTheme.typography.labelLarge,
                        color = foreground.copy(alpha = 0.72f),
                    )
                }
            }
            WorkspaceWidgetCatalog.ANALOG_CLOCK -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(GlazeMetrics.space3),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val radius = size.minDimension * 0.38f
                        val minuteRadians = Math.toRadians(now.minute * 6.0 - 90.0)
                        val hourRadians = Math.toRadians(
                            (now.hour % 12) * 30.0 + now.minute * 0.5 - 90.0,
                        )
                        drawCircle(
                            color = foreground.copy(alpha = 0.12f),
                            radius = radius,
                            center = center,
                        )
                        drawCircle(
                            color = foreground.copy(alpha = 0.82f),
                            radius = 3.dp.toPx(),
                            center = center,
                        )
                        drawLine(
                            color = foreground.copy(alpha = 0.92f),
                            start = center,
                            end = Offset(
                                x = center.x + cos(hourRadians).toFloat() * radius * 0.52f,
                                y = center.y + sin(hourRadians).toFloat() * radius * 0.52f,
                            ),
                            strokeWidth = 4.dp.toPx(),
                        )
                        drawLine(
                            color = foreground.copy(alpha = 0.84f),
                            start = center,
                            end = Offset(
                                x = center.x + cos(minuteRadians).toFloat() * radius * 0.76f,
                                y = center.y + sin(minuteRadians).toFloat() * radius * 0.76f,
                            ),
                            strokeWidth = 2.dp.toPx(),
                        )
                    }
                }
            }
            WorkspaceWidgetCatalog.DATE -> {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = GlazeMetrics.space3),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space3),
                ) {
                    Text(
                        now.dayOfMonth.toString(),
                        style = MaterialTheme.typography.displaySmall,
                        color = foreground,
                        fontWeight = FontWeight.Light,
                    )
                    Column {
                        Text(
                            now.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault())),
                            style = MaterialTheme.typography.titleSmall,
                            color = foreground,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            now.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                            style = MaterialTheme.typography.bodySmall,
                            color = foreground.copy(alpha = 0.72f),
                        )
                    }
                }
            }
            WorkspaceWidgetCatalog.MONTH -> {
                val today = now.toLocalDate()
                val monthStart = today.withDayOfMonth(1)
                val leadingBlankCount = monthStart.dayOfWeek.value % 7
                val monthLength = monthStart.lengthOfMonth()
                val locale = Locale.getDefault()
                val weekdayLabels = remember(locale) {
                    (0L..6L).map { offset ->
                        java.time.DayOfWeek.SUNDAY
                            .plus(offset)
                            .getDisplayName(
                                java.time.format.TextStyle.NARROW_STANDALONE,
                                locale,
                            )
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = GlazeMetrics.space3, vertical = GlazeMetrics.space2)
                        .semantics {
                            contentDescription =
                                today.format(
                                    DateTimeFormatter.ofPattern("MMMM d, yyyy", locale),
                                ) + ". Local month overview; no event data is accessed."
                        },
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            today.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)),
                            style = MaterialTheme.typography.titleSmall,
                            color = foreground,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                        Text(
                            "LOCAL · NO EVENTS",
                            style = MaterialTheme.typography.labelSmall,
                            color = foreground.copy(alpha = 0.58f),
                            maxLines = 1,
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        weekdayLabels.forEach { label ->
                            Text(
                                label,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelSmall,
                                color = foreground.copy(alpha = 0.58f),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                            )
                        }
                    }
                    repeat(6) { weekIndex ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            repeat(7) { weekdayIndex ->
                                val gridIndex = weekIndex * 7 + weekdayIndex
                                val dayOfMonth = gridIndex - leadingBlankCount + 1
                                val isInMonth = dayOfMonth in 1..monthLength
                                val isToday = isInMonth && dayOfMonth == today.dayOfMonth
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (isInMonth) {
                                        Surface(
                                            modifier = Modifier.size(22.dp),
                                            shape = CircleShape,
                                            color = if (isToday) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                Color.Transparent
                                            },
                                        ) {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(
                                                    dayOfMonth.toString(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isToday) {
                                                        MaterialTheme.colorScheme.onPrimary
                                                    } else {
                                                        foreground.copy(alpha = 0.86f)
                                                    },
                                                    fontWeight = if (isToday) {
                                                        FontWeight.SemiBold
                                                    } else {
                                                        FontWeight.Normal
                                                    },
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            WorkspaceWidgetCatalog.SEARCH -> {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(onClick = onOpenSearch),
                    color = Color.Transparent,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = GlazeMetrics.space3),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                    ) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = RoundedCornerShape(GlazeMetrics.radiusPill),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                LauncherSearchMagnifier(MaterialTheme.colorScheme.primary)
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Search GoreeCloud",
                                style = MaterialTheme.typography.titleSmall,
                                color = foreground,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                            Text(
                                "Apps, contacts, files & actions",
                                style = MaterialTheme.typography.bodySmall,
                                color = foreground.copy(alpha = 0.70f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            WorkspaceWidgetCatalog.QUICK_ACTIONS -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(GlazeMetrics.space2),
                    verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                    ) {
                        GlazeActionChip(
                            title = "Apps",
                            symbol = LauncherQuickActionSymbol.APPS,
                            onClick = onOpenApps,
                            modifier = Modifier.weight(1f),
                        )
                        GlazeActionChip(
                            title = "Search",
                            symbol = LauncherQuickActionSymbol.SEARCH,
                            onClick = onOpenSearch,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                    ) {
                        GlazeActionChip(
                            title = "Edit Home",
                            symbol = LauncherQuickActionSymbol.EDIT_HOME,
                            onClick = onOpenHomeEditor,
                            modifier = Modifier.weight(1f),
                        )
                        GlazeActionChip(
                            title = "Settings",
                            symbol = LauncherQuickActionSymbol.SETTINGS,
                            onClick = onOpenSettings,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            WorkspaceWidgetCatalog.BATTERY -> {
                val battery = rememberLauncherBatterySnapshot()
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = GlazeMetrics.space3),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            battery.percent?.let { "$it%" } ?: "—",
                            style = MaterialTheme.typography.headlineMedium,
                            color = foreground,
                            fontWeight = FontWeight.Light,
                        )
                        Text(
                            if (battery.charging) "Charging" else "Battery",
                            style = MaterialTheme.typography.bodySmall,
                            color = foreground.copy(alpha = 0.70f),
                        )
                    }
                    LauncherBatteryGlyph(
                        percent = battery.percent,
                        charging = battery.charging,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            WorkspaceWidgetCatalog.LAUNCHER_STATUS -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(GlazeMetrics.space3),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        "GoreeCloud Launcher",
                        style = MaterialTheme.typography.titleMedium,
                        color = foreground,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Home ready · local-first",
                        style = MaterialTheme.typography.bodySmall,
                        color = foreground.copy(alpha = 0.76f),
                    )
                }
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(GlazeMetrics.space3),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Unknown GoreeCloud widget",
                        color = foreground.copy(alpha = 0.78f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeFavoriteTile(
    app: LauncherActivityInfo,
    displayLabel: String,
    iconScale: Float,
    showLabel: Boolean,
    layoutLocked: Boolean,
    editMode: Boolean,
    dragData: LauncherAppDragData?,
    onBeginLocalDrag: (LauncherAppDragData, Offset) -> Unit,
    onUpdateLocalDrag: (Offset) -> Unit,
    onEndLocalDrag: (LauncherAppDragData, Offset) -> Unit,
    onCancelLocalDrag: () -> Unit,
    onLaunchApp: (LauncherActivityInfo) -> Unit,
    onManageApp: (LauncherActivityInfo, Rect?) -> Unit,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
    modifier: Modifier = Modifier,
    labelColor: Color = Color.White,
) {
    val icon = rememberLauncherAppIcon(app)
    val iconSize = (50f * iconScale.coerceIn(0.85f, 1.15f)).dp
    val moveThreshold = with(LocalDensity.current) { 14.dp.toPx() }
    val swipeThreshold = with(LocalDensity.current) { 42.dp.toPx() }
    var dragging by remember(app.componentName, app.user) { mutableStateOf(false) }
    var dragOffset by remember(app.componentName, app.user) { mutableStateOf(Offset.Zero) }
    var tileBounds by remember(app.componentName, app.user) { mutableStateOf<Rect?>(null) }
    var dragStartCenter by remember(app.componentName, app.user) { mutableStateOf<Offset?>(null) }

    val swipeModifier = Modifier.pointerInput(onSwipeUp, onSwipeDown, swipeThreshold) {
        var drag = 0f
        var triggered = false
        detectVerticalDragGestures(
            onDragStart = {
                drag = 0f
                triggered = false
            },
            onDragCancel = {
                drag = 0f
                triggered = false
            },
            onDragEnd = {
                drag = 0f
                triggered = false
            },
            onVerticalDrag = { change, amount ->
                if (!dragging) {
                    change.consume()
                    if (!triggered) {
                        drag += amount
                        when {
                            drag <= -swipeThreshold -> {
                                triggered = true
                                onSwipeUp()
                            }
                            drag >= swipeThreshold -> {
                                triggered = true
                                onSwipeDown()
                            }
                        }
                    }
                }
            },
        )
    }

    val gestureModifier = if (layoutLocked || dragData == null) {
        swipeModifier.combinedClickable(
            onClick = { onLaunchApp(app) },
            onLongClick = { onManageApp(app, tileBounds) },
        )
    } else {
        Modifier
            .pointerInput(app.componentName, app.user, moveThreshold) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        dragging = true
                        dragOffset = Offset.Zero
                        dragStartCenter = tileBounds?.center
                        onManageApp(app, tileBounds)
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        val start = dragStartCenter
                        val wasStationary = dragOffset == Offset.Zero
                        dragOffset += amount
                        if (start != null) {
                            if (wasStationary) {
                                onBeginLocalDrag(dragData, start)
                            }
                            onUpdateLocalDrag(start + dragOffset)
                        }
                    },
                    onDragEnd = {
                        val moved =
                            kotlin.math.abs(dragOffset.x) >= moveThreshold ||
                                kotlin.math.abs(dragOffset.y) >= moveThreshold
                        val start = dragStartCenter ?: tileBounds?.center
                        if (moved && start != null) {
                            onEndLocalDrag(dragData, start + dragOffset)
                        } else {
                            onCancelLocalDrag()
                        }
                        dragging = false
                        dragOffset = Offset.Zero
                        dragStartCenter = null
                    },
                    onDragCancel = {
                        onCancelLocalDrag()
                        dragging = false
                        dragOffset = Offset.Zero
                        dragStartCenter = null
                    },
                )
            }
            .then(swipeModifier)
            .clickable(enabled = !dragging) { onLaunchApp(app) }
    }

    BoxWithConstraints(
        modifier = modifier
            .onGloballyPositioned { tileBounds = it.boundsInRoot() }
            .graphicsLayer {
                scaleX = if (editMode && !dragging) 0.96f else 1f
                scaleY = if (editMode && !dragging) 0.96f else 1f
                alpha = if (dragging) 0.88f else 1f
                translationX = dragOffset.x
                translationY = dragOffset.y
            }
            .then(gestureModifier)
            .background(
                if (editMode && !dragging) Color.White.copy(alpha = 0.06f)
                else Color.Transparent,
                RoundedCornerShape(GlazeMetrics.radiusLarge),
            )
            .padding(horizontal = 2.dp, vertical = 2.dp),
    ) {
        val iconSlotHeight = launcherHomeTileIconSlotHeightDp(
            availableHeightDp = maxHeight.value,
            showLabel = showLabel,
        ).dp
        val renderedIconSize = iconSize.coerceAtMost(iconSlotHeight)

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(iconSlotHeight),
                contentAlignment = Alignment.Center,
            ) {
                Box(modifier = Modifier.size(renderedIconSize)) {
                    if (icon != null) {
                        Image(
                            bitmap = icon,
                            contentDescription = displayLabel,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().launcherIconMask(),
                        )
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxSize().launcherIconMask(),
                            shape = RoundedCornerShape(15.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    displayLabel.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                    LauncherAppBadgeMark(
                        app,
                        modifier = launcherBadgePositionModifier(),
                    )
                }
            }

            if (showLabel) {
                Spacer(Modifier.height(3.dp))
                Text(
                    displayLabel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 14.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.60f),
                            offset = Offset(0f, 1.5f),
                            blurRadius = 5f,
                        ),
                    ),
                    color = labelColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun HomeQuickActions(
    onOpenApps: () -> Unit,
    onOpenSearch: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
    ) {
        GlazeActionChip(
            title = "Apps",
            symbol = LauncherQuickActionSymbol.APPS,
            onClick = onOpenApps,
            modifier = Modifier.weight(1f),
        )
        GlazeActionChip(
            title = "Search",
            symbol = LauncherQuickActionSymbol.SEARCH,
            onClick = onOpenSearch,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun GlazeActionChip(
    title: String,
    symbol: LauncherQuickActionSymbol,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val presentationContext = LocalGlazeV16PresentationContext.current
    val resolvedPresentation = remember(presentationContext) {
        GlazeV16PresentationPolicy.resolve(
            requestedMaterial = GlazeV16MaterialRole.FUNCTIONAL_GLASS,
            context = presentationContext,
        )
    }
    val solidResolved = resolvedPresentation.materialRole in setOf(
        GlazeV16MaterialRole.SOLID,
        GlazeV16MaterialRole.RAISED,
    )
    val foreground = MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = modifier.heightIn(min = resolvedPresentation.minimumInteractionTarget),
        onClick = onClick,
        shape = RoundedCornerShape(GlazeMetrics.radiusMedium),
        color = when (resolvedPresentation.materialRole) {
            GlazeV16MaterialRole.SOLID -> MaterialTheme.colorScheme.surface
            GlazeV16MaterialRole.RAISED -> MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)
            else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.52f)
        },
        border = BorderStroke(
            1.dp,
            if (solidResolved) {
                MaterialTheme.colorScheme.outlineVariant
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
            },
        ),
        shadowElevation = if (solidResolved) 1.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Surface(
                modifier = Modifier.size(28.dp),
                shape = RoundedCornerShape(GlazeMetrics.radiusPill),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    LauncherQuickActionGlyph(
                        symbol = symbol,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = foreground.copy(alpha = 0.94f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private enum class LauncherQuickActionSymbol {
    APPS,
    SEARCH,
    EDIT_HOME,
    SETTINGS,
}

@Composable
private fun LauncherQuickActionGlyph(
    symbol: LauncherQuickActionSymbol,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(18.dp)) {
        val u = size.minDimension
        val stroke = u * 0.09f
        val cap = androidx.compose.ui.graphics.StrokeCap.Round
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
            drawLine(
                tint,
                Offset(u * x1, u * y1),
                Offset(u * x2, u * y2),
                stroke,
                cap = cap,
            )
        }
        when (symbol) {
            LauncherQuickActionSymbol.APPS -> {
                listOf(
                    0.16f to 0.16f,
                    0.57f to 0.16f,
                    0.16f to 0.57f,
                    0.57f to 0.57f,
                ).forEach { (x, y) ->
                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(u * x, u * y),
                        size = Size(u * 0.27f, u * 0.27f),
                        cornerRadius = CornerRadius(u * 0.06f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
                    )
                }
            }
            LauncherQuickActionSymbol.SEARCH -> {
                drawCircle(
                    color = tint,
                    radius = u * 0.24f,
                    center = Offset(u * 0.42f, u * 0.42f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
                )
                line(0.60f, 0.60f, 0.84f, 0.84f)
            }
            LauncherQuickActionSymbol.EDIT_HOME -> {
                line(0.18f, 0.52f, 0.50f, 0.22f)
                line(0.50f, 0.22f, 0.82f, 0.52f)
                line(0.28f, 0.46f, 0.28f, 0.82f)
                line(0.72f, 0.46f, 0.72f, 0.82f)
                line(0.28f, 0.82f, 0.72f, 0.82f)
                drawCircle(
                    color = tint,
                    radius = u * 0.055f,
                    center = Offset(u * 0.72f, u * 0.24f),
                )
            }
            LauncherQuickActionSymbol.SETTINGS -> {
                drawCircle(
                    color = tint,
                    radius = u * 0.24f,
                    center = Offset(u * 0.50f, u * 0.50f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
                )
                drawCircle(
                    color = tint,
                    radius = u * 0.07f,
                    center = Offset(u * 0.50f, u * 0.50f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
                )
                listOf(
                    0.50f to 0.12f,
                    0.50f to 0.88f,
                    0.12f to 0.50f,
                    0.88f to 0.50f,
                ).forEach { (x, y) -> line(0.50f, 0.50f, x, y) }
            }
        }
    }
}

@Composable
private fun LauncherBatteryGlyph(
    percent: Int?,
    charging: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val contrast = MaterialTheme.colorScheme.surface
    Canvas(modifier.size(30.dp)) {
        val u = size.minDimension
        val stroke = u * 0.065f
        val level = ((percent ?: 55).coerceIn(0, 100) / 100f)
        drawRoundRect(
            color = tint,
            topLeft = Offset(u * 0.10f, u * 0.27f),
            size = Size(u * 0.70f, u * 0.46f),
            cornerRadius = CornerRadius(u * 0.08f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
        )
        drawRoundRect(
            color = tint.copy(alpha = 0.80f),
            topLeft = Offset(u * 0.15f, u * 0.32f),
            size = Size(u * 0.60f * level.coerceAtLeast(0.06f), u * 0.36f),
            cornerRadius = CornerRadius(u * 0.05f),
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(u * 0.82f, u * 0.40f),
            size = Size(u * 0.08f, u * 0.20f),
            cornerRadius = CornerRadius(u * 0.025f),
        )
        if (charging) {
            val bolt = Path().apply {
                moveTo(u * 0.52f, u * 0.18f)
                lineTo(u * 0.37f, u * 0.49f)
                lineTo(u * 0.51f, u * 0.49f)
                lineTo(u * 0.43f, u * 0.82f)
                lineTo(u * 0.67f, u * 0.43f)
                lineTo(u * 0.54f, u * 0.43f)
                close()
            }
            drawPath(bolt, color = contrast)
        }
    }
}

@Composable
private fun LauncherHomeOpenGlyph(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(18.dp)) {
        val u = size.minDimension
        val stroke = u * 0.10f
        val cap = androidx.compose.ui.graphics.StrokeCap.Round
        drawLine(tint, Offset(u * 0.28f, u * 0.50f), Offset(u * 0.72f, u * 0.50f), stroke, cap = cap)
        drawLine(tint, Offset(u * 0.55f, u * 0.32f), Offset(u * 0.72f, u * 0.50f), stroke, cap = cap)
        drawLine(tint, Offset(u * 0.72f, u * 0.50f), Offset(u * 0.55f, u * 0.68f), stroke, cap = cap)
    }
}

@Composable
private fun LauncherWidgetPreviewGlyph(
    typeId: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(34.dp)) {
        val u = size.minDimension
        val stroke = u * 0.065f
        when (typeId) {
            WorkspaceWidgetCatalog.ANALOG_CLOCK -> {
                drawCircle(
                    color = tint,
                    radius = u * 0.34f,
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
                )
                drawLine(
                    tint,
                    center,
                    Offset(u * 0.50f, u * 0.29f),
                    stroke,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
                drawLine(
                    tint,
                    center,
                    Offset(u * 0.68f, u * 0.58f),
                    stroke,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
            }
            WorkspaceWidgetCatalog.QUICK_ACTIONS -> {
                listOf(
                    0.14f to 0.14f,
                    0.55f to 0.14f,
                    0.14f to 0.55f,
                    0.55f to 0.55f,
                ).forEach { (x, y) ->
                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(u * x, u * y),
                        size = Size(u * 0.31f, u * 0.31f),
                        cornerRadius = CornerRadius(u * 0.08f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
                    )
                }
            }
            WorkspaceWidgetCatalog.BATTERY -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(u * 0.12f, u * 0.30f),
                    size = Size(u * 0.66f, u * 0.40f),
                    cornerRadius = CornerRadius(u * 0.07f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(u * 0.18f, u * 0.36f),
                    size = Size(u * 0.38f, u * 0.28f),
                    cornerRadius = CornerRadius(u * 0.04f),
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(u * 0.80f, u * 0.42f),
                    size = Size(u * 0.08f, u * 0.16f),
                    cornerRadius = CornerRadius(u * 0.02f),
                )
            }
        }
    }
}

@Composable
private fun EmptyWorkspaceCard(
    onOpenApps: () -> Unit,
) {
    Surface(
        onClick = onOpenApps,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .semantics {
                contentDescription = "Open Apps. Hold Home to customize."
            },
        shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.60f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.055f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                modifier = Modifier.size(34.dp),
                shape = RoundedCornerShape(GlazeMetrics.radiusPill),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    LauncherQuickActionGlyph(
                        symbol = LauncherQuickActionSymbol.APPS,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "Open Apps",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Hold Home to customize",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LauncherHomeOpenGlyph(
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun LauncherUniversalSearchSurface(
    apps: List<LauncherActivityInfo>,
    onLaunchApp: (LauncherActivityInfo) -> Unit,
    onNavigate: (LauncherSearchDestination) -> Unit,
    onBack: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val providers = remember(apps) {
        LauncherBuiltInSearchProviderRegistry.providers(apps)
    }
    val executionPolicy = remember {
        com.goreecloud.launcher.core.launcher.LauncherSearchExecutionPolicy.cancellationOnly()
    }
    var results by remember(providers, query) {
        mutableStateOf<List<LauncherSearchResult>>(emptyList())
    }
    var searchCompleted by remember(providers, query) { mutableStateOf(false) }

    LaunchedEffect(providers, query) {
        results = LauncherUniversalSearch.searchAsync(
            rawQuery = query,
            providers = providers,
            policy = executionPolicy,
        )
        searchCompleted = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        GlazeAtmosphere.softAqua.copy(alpha = 0.07f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = GlazeMetrics.space4, vertical = GlazeMetrics.space3),
            verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space3),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Universal Search",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Launcher-owned local search and actions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                GlazeTextAction("Done", onBack)
            }

            GlazeAppSearchField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                requestFocus = true,
                placeholder = "Search apps, settings and actions",
                inputTestTag = "launcher-universal-search-field",
            )

            if (results.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (searchCompleted) {
                            "No Launcher results match “" + query.trim() + "”"
                        } else {
                            "Searching…"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                    contentPadding = PaddingValues(bottom = GlazeMetrics.space3),
                ) {
                    lazyItems(
                        items = results,
                        key = { result -> result.providerId + ":" + result.resultId },
                    ) { result ->
                        LauncherUniversalSearchResultRow(
                            result = result,
                            onClick = {
                                when (val action = result.action) {
                                    is LaunchApplicationSearchAction -> onLaunchApp(action.app)
                                    is LauncherNavigateSearchAction -> onNavigate(action.destination)
                                    null -> Unit
                                    else -> Unit
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LauncherUniversalSearchResultRow(
    result: LauncherSearchResult,
    onClick: () -> Unit,
) {
    val categoryLabel = when (result.category) {
        LauncherSearchCategory.APPLICATION -> "App"
        LauncherSearchCategory.SHORTCUT -> "Shortcut"
        LauncherSearchCategory.CONTACT -> "Contact"
        LauncherSearchCategory.CALL_HISTORY -> "Call"
        LauncherSearchCategory.MESSAGE -> "Message"
        LauncherSearchCategory.FILE -> "File"
        LauncherSearchCategory.CONNECTED_SOURCE -> "Connected"
        LauncherSearchCategory.SETTING -> "Setting"
        LauncherSearchCategory.ACTION -> "Action"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(GlazeMetrics.radiusMedium),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.055f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                modifier = Modifier.size(34.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    LauncherLegacySearchCategoryGlyph(
                        category = result.category,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    result.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                result.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                categoryLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun LauncherLegacySearchCategoryGlyph(
    category: LauncherSearchCategory,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(18.dp)) {
        val u = size.minDimension
        val stroke = u * 0.09f
        val cap = androidx.compose.ui.graphics.StrokeCap.Round
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
            drawLine(tint, Offset(u * x1, u * y1), Offset(u * x2, u * y2), stroke, cap = cap)
        }
        when (category) {
            LauncherSearchCategory.APPLICATION -> {
                listOf(
                    0.15f to 0.15f,
                    0.57f to 0.15f,
                    0.15f to 0.57f,
                    0.57f to 0.57f,
                ).forEach { (x, y) ->
                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(u * x, u * y),
                        size = Size(u * 0.28f, u * 0.28f),
                        cornerRadius = CornerRadius(u * 0.06f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
                    )
                }
            }
            LauncherSearchCategory.SHORTCUT,
            LauncherSearchCategory.ACTION,
            -> {
                line(0.22f, 0.70f, 0.72f, 0.26f)
                line(0.48f, 0.26f, 0.72f, 0.26f)
                line(0.72f, 0.26f, 0.72f, 0.50f)
            }
            LauncherSearchCategory.CONTACT -> {
                drawCircle(
                    color = tint,
                    radius = u * 0.16f,
                    center = Offset(u * 0.50f, u * 0.34f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
                )
                drawArc(
                    color = tint,
                    startAngle = 205f,
                    sweepAngle = 130f,
                    useCenter = false,
                    topLeft = Offset(u * 0.24f, u * 0.50f),
                    size = Size(u * 0.52f, u * 0.30f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
                )
            }
            LauncherSearchCategory.CALL_HISTORY -> {
                line(0.28f, 0.22f, 0.72f, 0.78f)
                line(0.22f, 0.22f, 0.36f, 0.18f)
                line(0.64f, 0.82f, 0.78f, 0.76f)
            }
            LauncherSearchCategory.MESSAGE -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(u * 0.14f, u * 0.22f),
                    size = Size(u * 0.72f, u * 0.48f),
                    cornerRadius = CornerRadius(u * 0.14f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
                )
                line(0.34f, 0.70f, 0.26f, 0.84f)
            }
            LauncherSearchCategory.FILE -> {
                val path = Path().apply {
                    moveTo(u * 0.18f, u * 0.18f)
                    lineTo(u * 0.58f, u * 0.18f)
                    lineTo(u * 0.82f, u * 0.42f)
                    lineTo(u * 0.82f, u * 0.82f)
                    lineTo(u * 0.18f, u * 0.82f)
                    close()
                }
                drawPath(path, tint, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
                line(0.58f, 0.18f, 0.58f, 0.42f)
                line(0.58f, 0.42f, 0.82f, 0.42f)
            }
            LauncherSearchCategory.CONNECTED_SOURCE -> {
                drawCircle(
                    color = tint,
                    radius = u * 0.23f,
                    center = Offset(u * 0.42f, u * 0.42f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
                )
                line(0.59f, 0.59f, 0.84f, 0.84f)
            }
            LauncherSearchCategory.SETTING -> {
                drawCircle(
                    color = tint,
                    radius = u * 0.24f,
                    center = Offset(u * 0.50f, u * 0.50f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
                )
                drawCircle(
                    color = tint,
                    radius = u * 0.07f,
                    center = Offset(u * 0.50f, u * 0.50f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
                )
                listOf(
                    0.50f to 0.12f,
                    0.50f to 0.88f,
                    0.12f to 0.50f,
                    0.88f to 0.50f,
                ).forEach { (x, y) -> line(0.50f, 0.50f, x, y) }
            }
        }
    }
}

private sealed interface LauncherDrawerVisualEntry {
    val label: String
    val stableKey: String

    data class Application(
        val app: LauncherActivityInfo,
        val pinned: Boolean,
    ) : LauncherDrawerVisualEntry {
        override val label: String = app.label.toString()
        override val stableKey: String = "app:" + app.workspaceKey()
    }

    data class Folder(val folder: LauncherFolder) : LauncherDrawerVisualEntry {
        override val label: String = folder.name
        override val stableKey: String = "folder:" + folder.id
    }
}

/** Folder entries share app ordering and grid cells without changing persisted folder membership. */
private fun orderedDrawerVisualEntries(
    apps: List<LauncherActivityInfo>,
    folders: List<LauncherFolder>,
    sortOrder: LauncherDrawerSortOrder,
    recentAppKeys: List<String>,
    localLaunchCounts: Map<String, Long>,
    pinnedAppKeys: Set<String>,
    pinnedAppOrder: List<String>,
    freshnessByAppKey: Map<String, LauncherAppFreshness>,
): List<LauncherDrawerVisualEntry> {
    val recentRanks = recentAppKeys.withIndex().associate { (index, key) -> key to index }
    val pinnedRanks = pinnedAppOrder.withIndex().associate { (index, key) -> key to index }
    return buildList {
        apps.forEach { app ->
            add(
                LauncherDrawerVisualEntry.Application(
                    app = app,
                    pinned = app.workspaceKey() in pinnedAppKeys,
                )
            )
        }
        folders.forEach { add(LauncherDrawerVisualEntry.Folder(it)) }
    }.let { entries ->
        LauncherDrawerSortingPolicy.order(
            entries = entries,
            label = { it.label },
            key = { it.stableKey },
            sortOrder = sortOrder,
            recentRank = { entry ->
                (entry as? LauncherDrawerVisualEntry.Application)
                    ?.app
                    ?.workspaceKey()
                    ?.let(recentRanks::get)
            },
            installTimeMillis = { entry ->
                (entry as? LauncherDrawerVisualEntry.Application)
                    ?.app
                    ?.firstInstallTime
                    ?.takeIf { timestamp -> timestamp > 0L }
            },
            updateTimeMillis = { entry ->
                (entry as? LauncherDrawerVisualEntry.Application)
                    ?.app
                    ?.workspaceKey()
                    ?.let(freshnessByAppKey::get)
                    ?.takeIf { freshness ->
                        freshness.updateMetadataAvailable &&
                            freshness.lastUpdateTimeMillis -
                            freshness.firstInstallTimeMillis >
                            LauncherDrawerDiscoveryPolicy.UPDATE_SEPARATION_MILLIS
                    }
                    ?.lastUpdateTimeMillis
            },
            frequency = { entry ->
                (entry as? LauncherDrawerVisualEntry.Application)
                    ?.app
                    ?.workspaceKey()
                    ?.let(localLaunchCounts::get)
            },
            pinned = { entry ->
                (entry as? LauncherDrawerVisualEntry.Application)?.pinned ?: false
            },
            pinnedRank = { entry ->
                (entry as? LauncherDrawerVisualEntry.Application)
                    ?.app
                    ?.workspaceKey()
                    ?.let(pinnedRanks::get)
            },
        )
    }
}

@Composable
private fun LauncherDrawerVisualTile(
    entry: LauncherDrawerVisualEntry,
    allApps: List<LauncherActivityInfo>,
    lockedAppKeys: Set<String>,
    iconScale: Float,
    showLabel: Boolean,
    compact: Boolean,
    layoutLocked: Boolean,
    onLaunchApp: (LauncherActivityInfo) -> Unit,
    onManageApp: (LauncherActivityInfo, Rect?) -> Unit,
    onOpenFolder: (LauncherFolder) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (entry) {
        is LauncherDrawerVisualEntry.Application -> LauncherAppTile(
            app = entry.app,
            iconScale = iconScale,
            showLabel = showLabel,
            compact = compact,
            fixedGridGeometry = true,
            pinnedInDrawer = entry.pinned,
            lockedByLauncher = entry.app.workspaceKey() in lockedAppKeys,
            onClick = { onLaunchApp(entry.app) },
            onLongClick = { anchor -> onManageApp(entry.app, anchor) },
            dragData = if (layoutLocked) null else LauncherAppDragData(
                appKey = entry.app.workspaceKey(),
                origin = LauncherAppDragOrigin.DRAWER,
            ),
            modifier = modifier,
        )
        is LauncherDrawerVisualEntry.Folder -> HomeFolderTile(
            folder = entry.folder,
            allApps = allApps,
            showLabel = showLabel,
            editMode = false,
            labelOnWallpaper = false,
            fixedGridGeometry = true,
            compact = compact,
            iconScale = iconScale,
            onOpen = { onOpenFolder(entry.folder) },
            modifier = modifier.testTag("launcher-drawer-inline-folder-" + entry.folder.id),
        )
    }
}

@Composable
private fun LauncherDrawerSmartFolderTile(
    folder: LauncherDrawerSmartFolder,
    allApps: List<LauncherActivityInfo>,
    iconScale: Float,
    showLabel: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visualFolder = remember(folder) {
        LauncherFolder(
            id = folder.id,
            name = folder.name,
            appKeys = folder.memberKeys,
        )
    }
    HomeFolderTile(
        folder = visualFolder,
        allApps = allApps,
        showLabel = showLabel,
        editMode = false,
        labelOnWallpaper = false,
        fixedGridGeometry = true,
        compact = false,
        iconScale = iconScale,
        onOpen = onOpen,
        modifier = modifier
            .testTag("launcher-drawer-smart-folder-" + folder.kind.name.lowercase())
            .semantics {
                contentDescription =
                    "Smart folder " + folder.name + ". " + folder.kind.explanation
            },
    )
}

@Composable
private fun LauncherDrawerSmartFolderSheet(
    folder: LauncherDrawerSmartFolder,
    apps: List<LauncherActivityInfo>,
    lockedAppKeys: Set<String>,
    onLaunchApp: (LauncherActivityInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(folder.name)
                Text(
                    "Smart folder",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space1),
            ) {
                Text(
                    folder.kind.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Dynamic and local-only. This view does not change manual folders, tabs, Home, or Dock placement.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(GlazeMetrics.space1))
                apps.forEach { app ->
                    val icon = rememberLauncherAppIcon(app)
                    Surface(
                        onClick = { onLaunchApp(app) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .semantics {
                                contentDescription = buildString {
                                    append(app.label.toString())
                                    if (app.workspaceKey() in lockedAppKeys) append(", App Lock")
                                }
                            },
                        shape = RoundedCornerShape(GlazeMetrics.radiusMedium),
                        color = Color.Transparent,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = GlazeMetrics.space2, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                        ) {
                            if (icon != null) {
                                Image(
                                    bitmap = icon,
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.size(36.dp).launcherIconMask(),
                                )
                            } else {
                                Spacer(Modifier.size(36.dp))
                            }
                            Text(
                                app.label.toString(),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (app.workspaceKey() in lockedAppKeys) {
                                GlazePopupActionGlyph(
                                    symbol = GlazePopupActionSymbol.LOCK,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    iconSize = 17.dp,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun DrawerAlphabetIndex(
    targets: List<Pair<String, Int>>,
    secondaryColor: Color,
    onJumpToIndex: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (targets.size <= 1) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .testTag("launcher-drawer-alphabet-index"),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        targets.forEach { (bucket, index) ->
            Surface(
                onClick = { onJumpToIndex(index) },
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = "Jump to " + bucket },
                shape = CircleShape,
                color = Color.Transparent,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        bucket,
                        style = MaterialTheme.typography.labelMedium,
                        color = secondaryColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun StableDrawerVerticalGrid(
    entries: List<LauncherDrawerVisualEntry>,
    allApps: List<LauncherActivityInfo>,
    lockedAppKeys: Set<String>,
    columns: Int,
    iconScale: Float,
    showLabel: Boolean,
    compact: Boolean,
    layoutLocked: Boolean,
    spacing: Dp,
    tileHeight: Dp,
    onLaunchApp: (LauncherActivityInfo) -> Unit,
    onManageApp: (LauncherActivityInfo, Rect?) -> Unit,
    onOpenFolder: (LauncherFolder) -> Unit,
    onDismiss: () -> Unit,
    alphabetJumpRequest: Pair<Int, Int>? = null,
    modifier: Modifier = Modifier,
) {
    val columnCount = columns.coerceAtLeast(1)
    val rows = remember(entries, columnCount) {
        launcherDrawerStableRows(entries, columnCount)
    }
    if (launcherDrawerUsesEagerGrid(entries.size)) {
        // Keep ordinary profile inventories composed for the lifetime of this drawer surface.
        // This intentionally trades a small bounded composition cost for stable icon/label
        // ownership on OEM builds where recycled lazy cells intermittently disappear.
        val scrollState = rememberScrollState()
        val rowStridePx = with(LocalDensity.current) { (tileHeight + spacing).roundToPx() }
        LaunchedEffect(alphabetJumpRequest, columnCount, rowStridePx) {
            alphabetJumpRequest?.first?.let { itemIndex ->
                scrollState.animateScrollTo((itemIndex / columnCount) * rowStridePx)
            }
        }
        val dismissConnection = rememberDrawerDismissNestedScrollConnection(
            canScrollBackward = { scrollState.value > 0 },
            onDismiss = onDismiss,
        )
        Column(
            modifier = modifier
                .fillMaxWidth()
                .nestedScroll(dismissConnection)
                .verticalScroll(scrollState)
                .padding(vertical = spacing),
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    verticalAlignment = Alignment.Top,
                ) {
                    row.forEach { entry ->
                        key(entry.stableKey) {
                            LauncherDrawerVisualTile(
                                entry = entry,
                                allApps = allApps,
                                lockedAppKeys = lockedAppKeys,
                                iconScale = iconScale,
                                showLabel = showLabel,
                                compact = compact,
                                layoutLocked = layoutLocked,
                                onLaunchApp = onLaunchApp,
                                onManageApp = onManageApp,
                                onOpenFolder = onOpenFolder,
                                modifier = Modifier.weight(1f).height(tileHeight),
                            )
                        }
                    }
                    repeat(columnCount - row.size) {
                        Spacer(Modifier.weight(1f).height(tileHeight))
                    }
                }
            }
        }
    } else {
        val listState = rememberLazyListState()
        LaunchedEffect(alphabetJumpRequest, columnCount) {
            alphabetJumpRequest?.first?.let { itemIndex ->
                listState.animateScrollToItem(itemIndex / columnCount)
            }
        }
        val dismissConnection = rememberDrawerDismissNestedScrollConnection(
            canScrollBackward = { listState.canScrollBackward },
            onDismiss = onDismiss,
        )
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxWidth().nestedScroll(dismissConnection),
            contentPadding = PaddingValues(vertical = spacing),
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            items(
                items = rows,
                key = { row -> "drawer-row:" + row.joinToString("|") { it.stableKey } },
                contentType = { "drawer-row" },
            ) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    verticalAlignment = Alignment.Top,
                ) {
                    row.forEach { entry ->
                        key(entry.stableKey) {
                            LauncherDrawerVisualTile(
                                entry = entry,
                                allApps = allApps,
                                lockedAppKeys = lockedAppKeys,
                                iconScale = iconScale,
                                showLabel = showLabel,
                                compact = compact,
                                layoutLocked = layoutLocked,
                                onLaunchApp = onLaunchApp,
                                onManageApp = onManageApp,
                                onOpenFolder = onOpenFolder,
                                modifier = Modifier.weight(1f).height(tileHeight),
                            )
                        }
                    }
                    repeat(columnCount - row.size) {
                        Spacer(Modifier.weight(1f).height(tileHeight))
                    }
                }
            }
        }
    }
}
@Composable
private fun AppDrawerSurface(
    apps: List<LauncherActivityInfo>,
    folders: List<LauncherFolder>,
    recentAppKeys: List<String>,
    localLaunchCounts: Map<String, Long>,
    pinnedAppKeys: Set<String>,
    pinnedAppOrder: List<String>,
    lockedAppKeys: Set<String>,
    sortOrderName: String?,
    drawerTabs: List<LauncherDrawerTab>,
    preferences: LauncherPreferences,
    drawerLayoutMode: LauncherDrawerLayoutMode,
    experiencePreferences: LauncherExperiencePreferences,
    focusSearch: Boolean,
    onLaunchApp: (LauncherActivityInfo) -> Unit,
    onManageApp: (LauncherActivityInfo, Rect?) -> Unit,
    onOpenFolder: (LauncherFolder) -> Unit,
    onManageFolders: (Int) -> Unit,
    onSetSortOrderName: (String?) -> Unit,
    onSetDrawerLayoutMode: (LauncherDrawerLayoutMode) -> Unit,
    onOpenSettings: () -> Unit,
    onCreateDrawerTab: (String) -> Unit,
    onRenameDrawerTab: (String, String) -> Unit,
    onDeleteDrawerTab: (String) -> Unit,
    onHome: () -> Unit,
) {
    var drawerQuery by rememberSaveable { mutableStateOf("") }
    val drawerSortOrder = runCatching {
        LauncherDrawerSortOrder.valueOf(sortOrderName.orEmpty())
    }.getOrDefault(LauncherDrawerSortOrder.ALPHABETICAL)
    var showDrawerSortMenu by remember { mutableStateOf(false) }
    var discoveryFilterName by rememberSaveable {
        mutableStateOf(LauncherDrawerDiscoveryFilter.ALL.name)
    }
    val discoveryFilter = remember(discoveryFilterName) {
        runCatching { LauncherDrawerDiscoveryFilter.valueOf(discoveryFilterName) }
            .getOrDefault(LauncherDrawerDiscoveryFilter.ALL)
    }
    var selectedDrawerTabId by rememberSaveable { mutableStateOf<String?>(null) }
    var showCreateDrawerTabDialog by rememberSaveable { mutableStateOf(false) }
    var editingDrawerTabId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSmartFolderKindName by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedDrawerTab = drawerTabs.firstOrNull { it.id == selectedDrawerTabId }
    LaunchedEffect(drawerTabs.map { it.id }) {
        if (selectedDrawerTabId != null && drawerTabs.none { it.id == selectedDrawerTabId }) {
            selectedDrawerTabId = null
        }
    }
    val primaryUser = remember { Process.myUserHandle() }
    val primaryProfileId = remember(primaryUser) { primaryUser.hashCode() }
    val profilePages = remember(apps, primaryUser) {
        launcherDrawerProfilePages(
            items = apps,
            primaryUser = primaryUser,
            userOf = { app -> app.user },
        )
    }
    var selectedProfileName by rememberSaveable {
        mutableStateOf(LauncherDrawerProfileKind.USER.name)
    }
    val profilePager = rememberPagerState(
        initialPage = profilePages.indexOfFirst { it.kind.name == selectedProfileName }
            .coerceAtLeast(0),
        pageCount = { profilePages.size },
    )
    val profilePagerScope = rememberCoroutineScope()
    val selectedPage = profilePages[profilePager.currentPage.coerceIn(profilePages.indices)]
    val selectedPageProfileIds = remember(
        selectedPage.kind,
        selectedPage.items,
        primaryProfileId,
    ) {
        if (selectedPage.kind == LauncherDrawerProfileKind.USER) {
            listOf(primaryProfileId)
        } else {
            selectedPage.items.map { it.user.hashCode() }.distinct()
        }
    }
    val folderCreationProfileId = if (
        selectedPage.kind == LauncherDrawerProfileKind.USER
    ) {
        primaryProfileId
    } else {
        selectedPageProfileIds.singleOrNull()
    }
    LaunchedEffect(profilePager.currentPage, profilePages.map { it.kind }) {
        selectedProfileName = selectedPage.kind.name
    }
    val drawerContext = LocalContext.current.applicationContext
    var drawerFreshnessByAppKey by remember(apps) {
        mutableStateOf<Map<String, LauncherAppFreshness>>(emptyMap())
    }
    var drawerFreshnessLoaded by remember(apps) { mutableStateOf(false) }
    LaunchedEffect(apps) {
        drawerFreshnessLoaded = false
        drawerFreshnessByAppKey = loadLauncherDrawerFreshness(drawerContext, apps)
        drawerFreshnessLoaded = true
    }
    val drawerFreshnessNowMillis = remember(apps, drawerFreshnessByAppKey) {
        System.currentTimeMillis()
    }
    val selectedProfileUpdateMetadataAvailable = remember(
        selectedPage.items,
        drawerFreshnessByAppKey,
    ) {
        selectedPage.items.any { app ->
            drawerFreshnessByAppKey[app.workspaceKey()]?.updateMetadataAvailable == true
        }
    }
    val selectedProfileSmartFolders = remember(
        selectedPage.items,
        pinnedAppKeys,
        recentAppKeys,
        localLaunchCounts,
        drawerFreshnessByAppKey,
        drawerFreshnessNowMillis,
        experiencePreferences.useLocalUsageForSuggestions,
    ) {
        val availableKeys = selectedPage.items.mapTo(linkedSetOf()) { app -> app.workspaceKey() }
        LauncherDrawerSmartFolderPolicy.build(
            availableKeys = availableKeys,
            pinnedKeys = pinnedAppKeys,
            recentAppKeys = recentAppKeys,
            launchCounts = localLaunchCounts,
            labelByKey = selectedPage.items.associate { app ->
                app.workspaceKey() to app.label.toString()
            },
            freshnessByKey = drawerFreshnessByAppKey,
            nowMillis = drawerFreshnessNowMillis,
            includeSuggested = experiencePreferences.showDrawerSuggestions,
        )
    }
    val selectedSmartFolder = remember(
        selectedSmartFolderKindName,
        selectedProfileSmartFolders,
    ) {
        selectedSmartFolderKindName
            ?.let { raw -> runCatching { LauncherDrawerSmartFolderKind.valueOf(raw) }.getOrNull() }
            ?.let { kind -> selectedProfileSmartFolders.firstOrNull { it.kind == kind } }
    }
    val selectedSmartFolderApps = remember(selectedSmartFolder, selectedPage.items) {
        val byKey = selectedPage.items.associateBy { app -> app.workspaceKey() }
        selectedSmartFolder?.memberKeys?.mapNotNull(byKey::get).orEmpty()
    }
    val drawerVisualPreferencesRepository = remember(drawerContext) {
        LauncherVisualPreferencesRepository(drawerContext)
    }
    val drawerVisualPreferences by drawerVisualPreferencesRepository.preferences.collectAsState(
        initial = LauncherVisualPreferences(),
    )
    LaunchedEffect(
        selectedPage.kind,
        selectedPage.items.map { it.workspaceKey() },
    ) {
        // Warm the entire visible profile page when the drawer opens/switches profiles. The shared
        // cache still bounds memory and de-duplicates in-flight decodes, while this removes the
        // random "letters first / icon never catches up" path seen on representative OEM builds.
        LauncherAppIconCache.preload(
            apps = selectedPage.items,
            packageManager = drawerContext.packageManager,
            maxCount = selectedPage.items.size.coerceAtMost(192),
        )
    }

    val dismissThreshold = with(LocalDensity.current) { 56.dp.toPx() }
    val glass = experiencePreferences.drawerBackdrop == LauncherDrawerBackdrop.GLASS
    val drawerSurfaceColor = if (glass) {
        GlazeAtmosphere.canvasBlack.copy(alpha = 0.76f)
    } else {
        MaterialTheme.colorScheme.background
    }
    val drawerSecondaryColor = if (glass) {
        Color.White.copy(alpha = 0.84f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val layoutDescription = when (drawerLayoutMode) {
        LauncherDrawerLayoutMode.GRID -> "Grid · ${preferences.drawerColumns} columns"
        LauncherDrawerLayoutMode.COMPACT -> "Compact · ${preferences.drawerColumns} columns"
        LauncherDrawerLayoutMode.LIST -> "Alphabetical list"
        LauncherDrawerLayoutMode.CATEGORY -> "Grouped by app category"
    }
    val searchEnabled =
        experiencePreferences.drawerSearchPlacement != LauncherDrawerSearchPlacement.OFF
    val searchAtTop =
        experiencePreferences.drawerSearchPlacement == LauncherDrawerSearchPlacement.TOP

    LaunchedEffect(searchEnabled) {
        if (!searchEnabled && drawerQuery.isNotEmpty()) {
            drawerQuery = ""
        }
    }
    val selectedFilteredCount = remember(
        selectedPage.items,
        selectedPageProfileIds,
        folders,
        drawerQuery,
        pinnedAppKeys,
        selectedDrawerTab?.id,
        selectedDrawerTab?.memberKeys,
        discoveryFilter,
        recentAppKeys,
        localLaunchCounts,
        drawerFreshnessByAppKey,
        drawerFreshnessNowMillis,
        primaryProfileId,
    ) {
        val tabApps = selectedPage.items.filter { app ->
            selectedDrawerTab == null || app.workspaceKey() in selectedDrawerTab.memberKeys
        }
        val availableKeys = tabApps.mapTo(linkedSetOf()) { it.workspaceKey() }
        val labelByKey = tabApps.associate { it.workspaceKey() to it.label.toString() }
        val discoveryKeys = LauncherDrawerDiscoveryPolicy.filterKeys(
            filter = discoveryFilter,
            availableKeys = availableKeys,
            pinnedKeys = pinnedAppKeys,
            recentAppKeys = recentAppKeys,
            launchCounts = localLaunchCounts,
            labelByKey = labelByKey,
            freshnessByKey = drawerFreshnessByAppKey,
            nowMillis = drawerFreshnessNowMillis,
        )
        val matchingApps = tabApps.count { app ->
            app.workspaceKey() in discoveryKeys &&
                LauncherLocalAppSearch.matches(
                    label = app.label.toString(),
                    packageName = app.componentName.packageName,
                    rawQuery = drawerQuery,
                )
        }
        val matchingFolders = if (
            discoveryFilter != LauncherDrawerDiscoveryFilter.ALL ||
            selectedDrawerTab != null
        ) {
            0
        } else {
            folders.count { folder ->
                selectedPageProfileIds.any { profileId ->
                    LauncherFolderProfilePolicy.belongsToProfile(
                        folder = folder,
                        profileId = profileId,
                        primaryProfileId = primaryProfileId,
                    )
                } &&
                    (
                        drawerQuery.isBlank() ||
                            LauncherLocalAppSearch.matches(
                                label = folder.name,
                                packageName = "",
                                rawQuery = drawerQuery,
                            )
                    )
            }
        }
        matchingApps + matchingFolders
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        GlazeAtmosphere.canvasBlack.copy(alpha = if (glass) 0.08f else 0.02f),
                        GlazeAtmosphere.canvasBlack.copy(alpha = if (glass) 0.42f else 0.18f),
                    ),
                ),
            ),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = GlazeMetrics.space2)
                .testTag("launcher-app-drawer"),
            shape = RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp),
            color = drawerSurfaceColor,
            contentColor = if (glass) Color.White else MaterialTheme.colorScheme.onBackground,
            border = BorderStroke(
                1.dp,
                if (glass) Color.White.copy(alpha = 0.10f)
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = GlazeMetrics.space4, vertical = GlazeMetrics.space2)
                    .testTag("launcher-app-drawer-gesture-surface")
                    .pointerInput(onHome, dismissThreshold) {
                        var drag = 0f
                        var triggered = false
                        detectVerticalDragGestures(
                            onDragStart = {
                                drag = 0f
                                triggered = false
                            },
                            onDragCancel = {
                                drag = 0f
                                triggered = false
                            },
                            onDragEnd = {
                                drag = 0f
                                triggered = false
                            },
                            onVerticalDrag = { _, amount ->
                                if (!triggered) {
                                    drag += amount
                                    if (drag >= dismissThreshold) {
                                        triggered = true
                                        onHome()
                                    }
                                }
                            },
                        )
                    },
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Surface(
                        modifier = Modifier.width(36.dp).height(4.dp),
                        shape = CircleShape,
                        color = if (glass) Color.White.copy(alpha = 0.28f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
                    ) {}
                }
                Spacer(Modifier.height(GlazeMetrics.space3))

                if (searchAtTop) {
                    DrawerSearchField(
                        value = drawerQuery,
                        onValueChange = { drawerQuery = it },
                        darkSurface = glass,
                        requestFocus = focusSearch,
                    )
                    Spacer(Modifier.height(GlazeMetrics.space2))
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space1),
                ) {
                    Column {
                        Text(
                            if (selectedPage.kind == LauncherDrawerProfileKind.USER) {
                                "Apps"
                            } else {
                                selectedPage.kind.displayName
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            buildString {
                                if (experiencePreferences.showDrawerAppCount || drawerQuery.isNotBlank()) {
                                    append(selectedFilteredCount)
                                    append(if (drawerQuery.isBlank()) " installed · " else " shown · ")
                                }
                                append(layoutDescription)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = drawerSecondaryColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("launcher-drawer-header-actions"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("launcher-drawer-settings")
                                .semantics { contentDescription = "Launcher settings" },
                            shape = CircleShape,
                            color = Color.Transparent,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                LauncherDrawerSettingsIcon(color = drawerSecondaryColor)
                            }
                        }
                        Box {
                            Surface(
                                onClick = { showDrawerSortMenu = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("launcher-drawer-sort-order")
                                    .semantics {
                                        contentDescription =
                                            "Sort apps. Current " + drawerSortOrder.displayName
                                    },
                                shape = CircleShape,
                                color = Color.Transparent,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    LauncherDrawerSortIcon(
                                        ascending =
                                            drawerSortOrder !=
                                                LauncherDrawerSortOrder.REVERSE_ALPHABETICAL,
                                        color = drawerSecondaryColor,
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = showDrawerSortMenu,
                                onDismissRequest = { showDrawerSortMenu = false },
                                shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
                                containerColor = if (glass) {
                                    GlazeAtmosphere.slateGraphite.copy(alpha = 0.98f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                tonalElevation = 0.dp,
                                shadowElevation = 8.dp,
                            ) {
                                LauncherDrawerSortOrder.entries.forEach { order ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                order.displayName,
                                                color = if (glass) {
                                                    Color.White.copy(alpha = 0.94f)
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                },
                                            )
                                        },
                                        onClick = {
                                            onSetSortOrderName(order.name)
                                            showDrawerSortMenu = false
                                        },
                                        trailingIcon = if (order == drawerSortOrder) {
                                            {
                                                GlazePopupActionGlyph(
                                                    symbol = GlazePopupActionSymbol.CHECK,
                                                    color = if (glass) {
                                                        Color.White.copy(alpha = 0.94f)
                                                    } else {
                                                        MaterialTheme.colorScheme.primary
                                                    },
                                                    iconSize = 15.dp,
                                                )
                                            }
                                        } else {
                                            null
                                        },
                                    )
                                }
                            }
                        }
                        LauncherDrawerDiscoveryFiltersRow(
                            selectedFilter = discoveryFilter,
                            pinnedAvailable = pinnedAppKeys.isNotEmpty(),
                            suggestionsEnabled = experiencePreferences.showDrawerSuggestions,
                            secondaryColor = drawerSecondaryColor,
                            chooseFilter = { filter -> discoveryFilterName = filter.name },
                            modifier = Modifier.size(48.dp),
                        )
                        val newFolderEnabled =
                            drawerQuery.isBlank() && folderCreationProfileId != null
                        Surface(
                            onClick = {
                                folderCreationProfileId?.let(onManageFolders)
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("launcher-drawer-new-folder")
                                .semantics {
                                    contentDescription = "New folder"
                                    stateDescription =
                                        if (newFolderEnabled) "Available" else "Unavailable"
                                },
                            enabled = newFolderEnabled,
                            shape = CircleShape,
                            color = Color.Transparent,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                LauncherDrawerNewFolderIcon(
                                    color = drawerSecondaryColor.copy(
                                        alpha = if (newFolderEnabled) 1f else 0.40f,
                                    ),
                                )
                            }
                        }
                        Surface(
                            onClick = {
                                onSetDrawerLayoutMode(
                                    nextLauncherDrawerLayoutMode(drawerLayoutMode),
                                )
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("launcher-drawer-layout-mode")
                                .semantics {
                                    contentDescription = "Change Apps layout"
                                    stateDescription = layoutDescription
                                },
                            shape = CircleShape,
                            color = Color.Transparent,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                LauncherDrawerLayoutIcon(
                                    mode = drawerLayoutMode,
                                    color = drawerSecondaryColor,
                                )
                            }
                        }
                    }
                }
                if (profilePages.size > 1) {
                    Spacer(Modifier.height(GlazeMetrics.space2))
                    DrawerProfileTabs(
                        pages = profilePages.map { page -> page.kind to page.items.size },
                        selected = selectedPage.kind,
                        onSelect = { kind ->
                            val index = profilePages.indexOfFirst { it.kind == kind }
                            if (index >= 0) profilePagerScope.launch {
                                profilePager.animateScrollToPage(index)
                            }
                        },
                        secondaryColor = drawerSecondaryColor,
                    )
                }
                Spacer(Modifier.height(GlazeMetrics.space2))
                DrawerCustomTabsRow(
                    tabs = drawerTabs,
                    selectedTabId = selectedDrawerTabId,
                    onSelectTab = { tabId ->
                        selectedDrawerTabId = tabId
                        discoveryFilterName = LauncherDrawerDiscoveryFilter.ALL.name
                    },
                    onCreateTab = {
                        if (drawerTabs.size < 8) showCreateDrawerTabDialog = true
                    },
                    onEditTab = { tabId -> editingDrawerTabId = tabId },
                    secondaryColor = drawerSecondaryColor,
                )
                Spacer(Modifier.height(GlazeMetrics.space1))
                LaunchedEffect(
                    experiencePreferences.showDrawerSuggestions,
                    discoveryFilter,
                ) {
                    if (
                        !experiencePreferences.showDrawerSuggestions &&
                        discoveryFilter == LauncherDrawerDiscoveryFilter.SUGGESTED
                    ) {
                        discoveryFilterName = LauncherDrawerDiscoveryFilter.ALL.name
                    }
                }
                if (
                    discoveryFilter == LauncherDrawerDiscoveryFilter.SUGGESTED &&
                    recentAppKeys.isEmpty() &&
                    localLaunchCounts.values.none { count -> count > 0L }
                ) {
                    Text(
                        "Suggestions stay local. No launch history yet — using a deterministic A–Z fallback.",
                        modifier = Modifier.padding(horizontal = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = drawerSecondaryColor.copy(alpha = 0.78f),
                    )
                }
                if (
                    discoveryFilter == LauncherDrawerDiscoveryFilter.UPDATED &&
                    drawerFreshnessLoaded &&
                    selectedPage.items.isNotEmpty() &&
                    !selectedProfileUpdateMetadataAvailable
                ) {
                    Text(
                        "Update metadata is unavailable for the current profile.",
                        modifier = Modifier.padding(horizontal = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = drawerSecondaryColor.copy(alpha = 0.78f),
                    )
                }
                Spacer(Modifier.height(GlazeMetrics.space2))
                HorizontalPager(
                    state = profilePager,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                        .testTag("launcher-drawer-profile-pager"),
                    userScrollEnabled = profilePages.size > 1,
                    beyondViewportPageCount = if (profilePages.size > 1) 1 else 0,
                    key = { index -> profilePages[index].kind.name },
                ) { index ->
                    val page = profilePages[index]
                    val pageApps = remember(
                        page.items,
                        drawerQuery,
                        pinnedAppKeys,
                        selectedDrawerTab?.id,
                        selectedDrawerTab?.memberKeys,
                        discoveryFilter,
                        recentAppKeys,
                        localLaunchCounts,
                        drawerFreshnessByAppKey,
                        drawerFreshnessNowMillis,
                    ) {
                        val tabApps = page.items.filter { app ->
                            selectedDrawerTab == null ||
                                app.workspaceKey() in selectedDrawerTab.memberKeys
                        }
                        val availableKeys = tabApps.mapTo(linkedSetOf()) { it.workspaceKey() }
                        val labelByKey = tabApps.associate {
                            it.workspaceKey() to it.label.toString()
                        }
                        val discoveryKeys = LauncherDrawerDiscoveryPolicy.filterKeys(
                            filter = discoveryFilter,
                            availableKeys = availableKeys,
                            pinnedKeys = pinnedAppKeys,
                            recentAppKeys = recentAppKeys,
                            launchCounts = localLaunchCounts,
                            labelByKey = labelByKey,
                            freshnessByKey = drawerFreshnessByAppKey,
                            nowMillis = drawerFreshnessNowMillis,
                        )
                        tabApps.filter { app ->
                            app.workspaceKey() in discoveryKeys &&
                                (
                                    drawerQuery.isBlank() ||
                                        LauncherLocalAppSearch.matches(
                                            label = app.label.toString(),
                                            packageName = app.componentName.packageName,
                                            rawQuery = drawerQuery,
                                        )
                                )
                        }
                    }
                    val pageProfileIds = remember(page.kind, page.items, primaryProfileId) {
                        if (page.kind == LauncherDrawerProfileKind.USER) {
                            listOf(primaryProfileId)
                        } else {
                            page.items.map { it.user.hashCode() }.distinct()
                        }
                    }
                    val pageFolders = remember(
                        pageProfileIds,
                        folders,
                        drawerQuery,
                        discoveryFilter,
                        selectedDrawerTab?.id,
                        primaryProfileId,
                    ) {
                        if (
                            discoveryFilter != LauncherDrawerDiscoveryFilter.ALL ||
                            selectedDrawerTab != null
                        ) {
                            emptyList()
                        } else folders.filter { folder ->
                            pageProfileIds.any { profileId ->
                                LauncherFolderProfilePolicy.belongsToProfile(
                                    folder = folder,
                                    profileId = profileId,
                                    primaryProfileId = primaryProfileId,
                                )
                            } &&
                                (
                                    drawerQuery.isBlank() ||
                                        LauncherLocalAppSearch.matches(
                                            label = folder.name,
                                            packageName = "",
                                            rawQuery = drawerQuery,
                                        )
                                )
                        }
                    }
                    if (pageApps.isEmpty() && pageFolders.isEmpty() && drawerQuery.isBlank()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                when {
                                    selectedDrawerTab != null ->
                                        "No apps in " + selectedDrawerTab.name + " for " +
                                            page.kind.displayName + "."
                                    discoveryFilter == LauncherDrawerDiscoveryFilter.PINNED ->
                                        "No pinned apps in " + page.kind.displayName + "."
                                    discoveryFilter == LauncherDrawerDiscoveryFilter.NEW ->
                                        "No recently installed apps in " + page.kind.displayName + "."
                                    discoveryFilter == LauncherDrawerDiscoveryFilter.UPDATED ->
                                        "No recently updated apps in " + page.kind.displayName + "."
                                    discoveryFilter == LauncherDrawerDiscoveryFilter.SUGGESTED ->
                                        "No suggested apps in " + page.kind.displayName + "."
                                    else ->
                                        "No apps are available in " + page.kind.displayName + "."
                                },
                                color = drawerSecondaryColor,
                            )
                        }
                    } else {
                        DrawerAppsContent(
                            apps = pageApps,
                            folders = pageFolders,
                            recentAppKeys = recentAppKeys,
                            localLaunchCounts = localLaunchCounts,
                            pinnedAppKeys = pinnedAppKeys,
                            pinnedAppOrder = pinnedAppOrder,
                            lockedAppKeys = lockedAppKeys,
                            freshnessByAppKey = drawerFreshnessByAppKey,
                            query = drawerQuery,
                            preferences = preferences,
                            drawerLayoutMode = drawerLayoutMode,
                            experiencePreferences = experiencePreferences,
                            sortOrder = drawerSortOrder,
                            smartFolders = if (
                                drawerQuery.isBlank() &&
                                discoveryFilter == LauncherDrawerDiscoveryFilter.ALL &&
                                selectedDrawerTab == null
                            ) {
                                val availableKeys = pageApps.mapTo(linkedSetOf()) { app -> app.workspaceKey() }
                                LauncherDrawerSmartFolderPolicy.build(
                                    availableKeys = availableKeys,
                                    pinnedKeys = pinnedAppKeys,
                                    recentAppKeys = recentAppKeys,
                                    launchCounts = localLaunchCounts,
                                    labelByKey = pageApps.associate { app ->
                                        app.workspaceKey() to app.label.toString()
                                    },
                                    freshnessByKey = drawerFreshnessByAppKey,
                                    nowMillis = drawerFreshnessNowMillis,
                                    includeSuggested = experiencePreferences.showDrawerSuggestions,
                                )
                            } else {
                                emptyList()
                            },
                            onLaunchApp = onLaunchApp,
                            onManageApp = onManageApp,
                            onOpenFolder = onOpenFolder,
                            onOpenSmartFolder = { kind -> selectedSmartFolderKindName = kind.name },
                            onDismiss = onHome,
                            secondaryColor = drawerSecondaryColor,
                            showAlphabetIndex = drawerVisualPreferences.showDrawerAlphabetIndex,
                            allowHorizontalPaging = profilePages.size == 1,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                if (searchEnabled && !searchAtTop) {
                    Spacer(Modifier.height(GlazeMetrics.space2))
                    DrawerSearchField(
                        value = drawerQuery,
                        onValueChange = { drawerQuery = it },
                        darkSurface = glass,
                        requestFocus = focusSearch,
                    )
                }
            }
        }
    }

    selectedSmartFolder?.let { smartFolder ->
        LauncherDrawerSmartFolderSheet(
            folder = smartFolder,
            apps = selectedSmartFolderApps,
            lockedAppKeys = lockedAppKeys,
            onLaunchApp = { app ->
                selectedSmartFolderKindName = null
                onLaunchApp(app)
            },
            onDismiss = { selectedSmartFolderKindName = null },
        )
    }

    if (showCreateDrawerTabDialog) {
        DrawerTabNameDialog(
            title = "New app tab",
            initialName = "",
            confirmLabel = "Create",
            onConfirm = { name ->
                onCreateDrawerTab(name)
                showCreateDrawerTabDialog = false
            },
            onDismiss = { showCreateDrawerTabDialog = false },
        )
    }

    editingDrawerTabId
        ?.let { id -> drawerTabs.firstOrNull { it.id == id } }
        ?.let { tab ->
            DrawerTabNameDialog(
                title = "Edit app tab",
                initialName = tab.name,
                confirmLabel = "Save",
                onConfirm = { name ->
                    onRenameDrawerTab(tab.id, name)
                    editingDrawerTabId = null
                },
                onDelete = {
                    onDeleteDrawerTab(tab.id)
                    if (selectedDrawerTabId == tab.id) selectedDrawerTabId = null
                    editingDrawerTabId = null
                },
                onDismiss = { editingDrawerTabId = null },
            )
        }
}

@Composable
private fun DrawerSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    darkSurface: Boolean,
    requestFocus: Boolean,
) {
    GlazeAppSearchField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("launcher-drawer-search"),
        darkSurface = darkSurface,
        requestFocus = requestFocus,
        placeholder = "Search apps",
        inputTestTag = "launcher-drawer-search-input",
        trailingContent = if (value.isBlank()) {
            null
        } else {
            {
                Surface(
                    onClick = { onValueChange("") },
                    modifier = Modifier
                        .size(40.dp)
                        .semantics { contentDescription = "Clear app search" },
                    shape = CircleShape,
                    color = Color.Transparent,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        GlazePopupActionGlyph(
                            symbol = GlazePopupActionSymbol.CLOSE,
                            color = if (darkSurface) {
                                Color.White.copy(alpha = 0.78f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            iconSize = 17.dp,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun DrawerCustomTabsRow(
    tabs: List<LauncherDrawerTab>,
    selectedTabId: String?,
    onSelectTab: (String?) -> Unit,
    onCreateTab: () -> Unit,
    onEditTab: (String) -> Unit,
    secondaryColor: Color,
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .testTag("launcher-drawer-custom-tabs"),
        horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DrawerTabChip(
            label = "All",
            selected = selectedTabId == null,
            onClick = { onSelectTab(null) },
            secondaryColor = secondaryColor,
            testTag = "launcher-drawer-tab-all",
        )
        tabs.forEach { tab ->
            DrawerTabChip(
                label = tab.name,
                selected = selectedTabId == tab.id,
                onClick = { onSelectTab(tab.id) },
                secondaryColor = secondaryColor,
                testTag = "launcher-drawer-tab-" + tab.id,
            )
        }
        if (tabs.size < 8) {
            Surface(
                onClick = onCreateTab,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("launcher-drawer-tab-create")
                    .semantics { contentDescription = "Create app tab" },
                shape = CircleShape,
                color = Color.Transparent,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    GlazePopupActionGlyph(
                        symbol = GlazePopupActionSymbol.ADD,
                        color = secondaryColor,
                        iconSize = 19.dp,
                    )
                }
            }
        }
        selectedTabId?.let { id ->
            Surface(
                onClick = { onEditTab(id) },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("launcher-drawer-tab-edit")
                    .semantics { contentDescription = "Edit selected app tab" },
                shape = CircleShape,
                color = Color.Transparent,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    LauncherDrawerEditIcon(
                        color = secondaryColor,
                        modifier = Modifier.size(21.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerTabChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    secondaryColor: Color,
    testTag: String,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag(testTag)
            .semantics {
                contentDescription = "App tab " + label
                this.selected = selected
            },
        shape = RoundedCornerShape(GlazeMetrics.radiusPill),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        } else {
            Color.Transparent
        },
        border = BorderStroke(
            1.dp,
            if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.34f)
            } else {
                secondaryColor.copy(alpha = 0.22f)
            },
        ),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = GlazeMetrics.space3, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.primary else secondaryColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LauncherDrawerEditIcon(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val u = size.minDimension
        val stroke = 1.9.dp.toPx()
        val cap = StrokeCap.Round
        drawLine(
            color = color,
            start = Offset(u * .27f, u * .73f),
            end = Offset(u * .70f, u * .30f),
            strokeWidth = stroke,
            cap = cap,
        )
        drawLine(
            color = color,
            start = Offset(u * .63f, u * .23f),
            end = Offset(u * .77f, u * .37f),
            strokeWidth = stroke,
            cap = cap,
        )
        drawLine(
            color = color,
            start = Offset(u * .27f, u * .73f),
            end = Offset(u * .23f, u * .78f),
            strokeWidth = stroke,
            cap = cap,
        )
        drawLine(
            color = color,
            start = Offset(u * .23f, u * .78f),
            end = Offset(u * .36f, u * .75f),
            strokeWidth = stroke,
            cap = cap,
        )
    }
}

@Composable
private fun DrawerTabNameDialog(
    title: String,
    initialName: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    val normalized = name.trim().replace(Regex("\\s+"), " ").take(32)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(64) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Tab name") },
                supportingText = { Text(normalized.length.toString() + "/32") },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(normalized) },
                enabled = normalized.isNotBlank(),
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space1)) {
                onDelete?.let { delete ->
                    TextButton(onClick = delete) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@Composable
private fun LauncherDrawerTabMembershipDialog(
    app: LauncherActivityInfo,
    tabs: List<LauncherDrawerTab>,
    onSetMembership: (String, Boolean) -> Unit,
    onClose: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("App drawer tabs") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space1),
            ) {
                Text(
                    app.label.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                tabs.forEach { tab ->
                    val checked = app.workspaceKey() in tab.memberKeys
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                    ) {
                        Text(
                            tab.name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Switch(
                            checked = checked,
                            onCheckedChange = { enabled ->
                                onSetMembership(tab.id, enabled)
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) { Text("Done") }
        },
    )
}

@Composable
private fun DrawerProfileTabs(
    pages: List<Pair<LauncherDrawerProfileKind, Int>>,
    selected: LauncherDrawerProfileKind,
    onSelect: (LauncherDrawerProfileKind) -> Unit,
    secondaryColor: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("launcher-drawer-profile-tabs"),
        horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
    ) {
        pages.forEach { (kind, count) ->
            val isSelected = kind == selected
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .testTag(
                        if (kind == LauncherDrawerProfileKind.USER) {
                            "launcher-drawer-profile-user"
                        } else {
                            "launcher-drawer-profile-work"
                        },
                    ),
                onClick = { onSelect(kind) },
                shape = RoundedCornerShape(GlazeMetrics.radiusPill),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                } else {
                    Color.Transparent
                },
                border = BorderStroke(
                    1.dp,
                    if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.46f)
                    } else {
                        secondaryColor.copy(alpha = 0.20f)
                    },
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        kind.displayName + " · " + count,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            secondaryColor
                        },
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrawerAppsContent(
    apps: List<LauncherActivityInfo>,
    folders: List<LauncherFolder>,
    recentAppKeys: List<String>,
    localLaunchCounts: Map<String, Long>,
    pinnedAppKeys: Set<String>,
    pinnedAppOrder: List<String>,
    lockedAppKeys: Set<String>,
    freshnessByAppKey: Map<String, LauncherAppFreshness>,
    query: String,
    preferences: LauncherPreferences,
    drawerLayoutMode: LauncherDrawerLayoutMode,
    experiencePreferences: LauncherExperiencePreferences,
    sortOrder: LauncherDrawerSortOrder,
    smartFolders: List<LauncherDrawerSmartFolder>,
    onLaunchApp: (LauncherActivityInfo) -> Unit,
    onManageApp: (LauncherActivityInfo, Rect?) -> Unit,
    onOpenFolder: (LauncherFolder) -> Unit,
    onOpenSmartFolder: (LauncherDrawerSmartFolderKind) -> Unit,
    onDismiss: () -> Unit,
    secondaryColor: Color,
    showAlphabetIndex: Boolean = false,
    allowHorizontalPaging: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val entries = remember(
        apps,
        folders,
        recentAppKeys,
        localLaunchCounts,
        pinnedAppKeys,
        pinnedAppOrder,
        freshnessByAppKey,
        sortOrder,
    ) {
        orderedDrawerVisualEntries(
            apps = apps,
            folders = folders,
            sortOrder = sortOrder,
            recentAppKeys = recentAppKeys,
            localLaunchCounts = localLaunchCounts,
            pinnedAppKeys = pinnedAppKeys,
            pinnedAppOrder = pinnedAppOrder,
            freshnessByAppKey = freshnessByAppKey,
        )
    }
    if (entries.isEmpty() && query.isNotBlank()) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "No installed apps match “" + query.trim() + "”",
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryColor,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    val pagedGrid = allowHorizontalPaging &&
        experiencePreferences.drawerNavigation == LauncherDrawerNavigation.PAGES &&
        drawerLayoutMode != LauncherDrawerLayoutMode.LIST &&
        drawerLayoutMode != LauncherDrawerLayoutMode.CATEGORY
    val standardSpacing = when (experiencePreferences.drawerSpacing) {
        LauncherDrawerSpacing.TIGHT -> GlazeMetrics.space1
        LauncherDrawerSpacing.STANDARD -> GlazeMetrics.space2
        LauncherDrawerSpacing.RELAXED -> GlazeMetrics.space3
    }
    val compactSpacing = when (experiencePreferences.drawerSpacing) {
        LauncherDrawerSpacing.TIGHT -> 2.dp
        LauncherDrawerSpacing.STANDARD -> GlazeMetrics.space1
        LauncherDrawerSpacing.RELAXED -> GlazeMetrics.space2
    }
    // Grid rows reserve fixed icon and label space so wrapped names never move neighboring icons.
    val gridGeometry = LauncherDrawerGridPolicy.geometry(
        compact = false,
        spacing = experiencePreferences.drawerSpacing,
    )
    val compactGeometry = LauncherDrawerGridPolicy.geometry(
        compact = true,
        spacing = experiencePreferences.drawerSpacing,
    )
    val gridTileHeight = gridGeometry.tileHeightDp.dp
    val compactTileHeight = compactGeometry.tileHeightDp.dp

    if (pagedGrid) {
        val pageSize = (
            preferences.drawerColumns * experiencePreferences.drawerPageRows.coerceIn(4, 6)
        ).coerceAtLeast(1)
        val pageCount = ((entries.size + pageSize - 1) / pageSize).coerceAtLeast(1)
        val pagerState = rememberPagerState(pageCount = { pageCount })
        val pagerScope = rememberCoroutineScope()

        LaunchedEffect(query, pageCount) {
            if (pagerState.currentPage >= pageCount || query.isNotBlank()) {
                pagerState.scrollToPage(0)
            }
        }

        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                pageSpacing = GlazeMetrics.space3,
            ) { page ->
                val pageItems = entries.drop(page * pageSize).take(pageSize)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(preferences.drawerColumns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = GlazeMetrics.space2),
                    horizontalArrangement = Arrangement.spacedBy(
                        if (drawerLayoutMode == LauncherDrawerLayoutMode.COMPACT) {
                            compactSpacing
                        } else {
                            standardSpacing
                        },
                    ),
                    verticalArrangement = Arrangement.spacedBy(
                        if (drawerLayoutMode == LauncherDrawerLayoutMode.COMPACT) {
                            compactSpacing
                        } else {
                            standardSpacing
                        },
                    ),
                    // Fallback vertical scrolling keeps all rows reachable on short screens,
                    // in landscape, and when accessibility display scaling is enabled.
                    userScrollEnabled = true,
                ) {
                    items(
                        items = pageItems,
                        key = { entry -> entry.stableKey },
                        contentType = { entry ->
                            when (entry) {
                                is LauncherDrawerVisualEntry.Application -> "app"
                                is LauncherDrawerVisualEntry.Folder -> "folder"
                            }
                        },
                    ) { entry ->
                        LauncherDrawerVisualTile(
                            entry = entry,
                            allApps = apps,
                            lockedAppKeys = lockedAppKeys,
                            iconScale = preferences.iconScale,
                            showLabel = experiencePreferences.showDrawerLabels,
                            compact = drawerLayoutMode == LauncherDrawerLayoutMode.COMPACT,
                            layoutLocked = preferences.layoutLocked,
                            onLaunchApp = onLaunchApp,
                            onManageApp = onManageApp,
                            onOpenFolder = onOpenFolder,
                            modifier = Modifier.height(
                                if (drawerLayoutMode == LauncherDrawerLayoutMode.COMPACT) compactTileHeight
                                else gridTileHeight,
                            ),
                        )
                    }
                }
            }

            if (pageCount > 1 && experiencePreferences.showDrawerPageIndicator) {
                DrawerPageDots(
                    pageCount = pageCount,
                    currentPage = pagerState.currentPage,
                    onSelectPage = { target ->
                        if (target != pagerState.currentPage) {
                            pagerScope.launch {
                                pagerState.animateScrollToPage(target)
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
        return
    }

    when (drawerLayoutMode) {
        LauncherDrawerLayoutMode.GRID -> {
            val alphabetTargets = remember(entries, sortOrder, query) {
                if (sortOrder == LauncherDrawerSortOrder.ALPHABETICAL && query.isBlank()) {
                    launcherDrawerAlphabetTargets(entries) { it.label }
                } else {
                    emptyList()
                }
            }
            var alphabetJumpRequest by remember {
                mutableStateOf<Pair<Int, Int>?>(null)
            }
            Column(modifier = modifier.fillMaxWidth()) {
                DrawerAlphabetIndex(
                    targets = if (showAlphabetIndex) alphabetTargets else emptyList(),
                    secondaryColor = secondaryColor,
                    onJumpToIndex = { index ->
                        alphabetJumpRequest = index to ((alphabetJumpRequest?.second ?: 0) + 1)
                    },
                )
                StableDrawerVerticalGrid(
                    entries = entries,
                    allApps = apps,
                    lockedAppKeys = lockedAppKeys,
                    columns = preferences.drawerColumns,
                    iconScale = preferences.iconScale,
                    showLabel = experiencePreferences.showDrawerLabels,
                    compact = false,
                    layoutLocked = preferences.layoutLocked,
                    spacing = standardSpacing,
                    tileHeight = gridTileHeight,
                    onLaunchApp = onLaunchApp,
                    onManageApp = onManageApp,
                    onOpenFolder = onOpenFolder,
                    onDismiss = onDismiss,
                    alphabetJumpRequest = alphabetJumpRequest,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        LauncherDrawerLayoutMode.COMPACT -> {
            val gridState = rememberLazyGridState()
            val dismissConnection = rememberDrawerDismissNestedScrollConnection(
                canScrollBackward = { gridState.canScrollBackward },
                onDismiss = onDismiss,
            )
            val alphabetTargets = remember(entries, sortOrder, query) {
                if (sortOrder == LauncherDrawerSortOrder.ALPHABETICAL && query.isBlank()) {
                    launcherDrawerAlphabetTargets(entries) { it.label }
                } else {
                    emptyList()
                }
            }
            val alphabetScope = rememberCoroutineScope()
            Column(modifier = modifier.fillMaxWidth()) {
                DrawerAlphabetIndex(
                    targets = if (showAlphabetIndex) alphabetTargets else emptyList(),
                    secondaryColor = secondaryColor,
                    onJumpToIndex = { index ->
                        alphabetScope.launch { gridState.animateScrollToItem(index) }
                    },
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(preferences.drawerColumns),
                    state = gridState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .nestedScroll(dismissConnection),
                    contentPadding = PaddingValues(vertical = compactSpacing),
                    horizontalArrangement = Arrangement.spacedBy(compactSpacing),
                    verticalArrangement = Arrangement.spacedBy(compactSpacing),
                ) {
                    items(
                        items = entries,
                        key = { entry -> entry.stableKey },
                        contentType = { entry ->
                            when (entry) {
                                is LauncherDrawerVisualEntry.Application -> "app"
                                is LauncherDrawerVisualEntry.Folder -> "folder"
                            }
                        },
                    ) { entry ->
                        LauncherDrawerVisualTile(
                            entry = entry,
                            allApps = apps,
                            lockedAppKeys = lockedAppKeys,
                            iconScale = preferences.iconScale,
                            showLabel = experiencePreferences.showDrawerLabels,
                            compact = true,
                            layoutLocked = preferences.layoutLocked,
                            onLaunchApp = onLaunchApp,
                            onManageApp = onManageApp,
                            onOpenFolder = onOpenFolder,
                            modifier = Modifier.height(compactTileHeight),
                        )
                    }
                }
            }
        }
        LauncherDrawerLayoutMode.LIST -> {
            val listState = rememberLazyListState()
            val dismissConnection = rememberDrawerDismissNestedScrollConnection(
                canScrollBackward = { listState.canScrollBackward },
                onDismiss = onDismiss,
            )
            val alphabetTargets = remember(entries, sortOrder, query) {
                if (
                    sortOrder == LauncherDrawerSortOrder.ALPHABETICAL &&
                    query.isBlank()
                ) {
                    launcherDrawerAlphabetTargets(entries) { it.label }
                } else {
                    emptyList()
                }
            }
            val alphabetScope = rememberCoroutineScope()

            Column(modifier = modifier.fillMaxWidth()) {
                DrawerAlphabetIndex(
                    targets = if (showAlphabetIndex) alphabetTargets else emptyList(),
                    secondaryColor = secondaryColor,
                    onJumpToIndex = { index ->
                        alphabetScope.launch { listState.animateScrollToItem(index) }
                    },
                )

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .nestedScroll(dismissConnection),
                    contentPadding = PaddingValues(vertical = standardSpacing),
                    verticalArrangement = Arrangement.spacedBy(compactSpacing),
                ) {
                    lazyItems(entries, key = { it.stableKey }) { entry ->
                        when (entry) {
                            is LauncherDrawerVisualEntry.Application -> LauncherAppListRow(
                                app = entry.app,
                                iconScale = preferences.iconScale,
                                pinnedInDrawer = entry.pinned,
                                lockedByLauncher = entry.app.workspaceKey() in lockedAppKeys,
                                onClick = { onLaunchApp(entry.app) },
                                onLongClick = { anchor -> onManageApp(entry.app, anchor) },
                                dragData = if (preferences.layoutLocked) null else LauncherAppDragData(
                                    appKey = entry.app.workspaceKey(),
                                    origin = LauncherAppDragOrigin.DRAWER,
                                ),
                            )
                            is LauncherDrawerVisualEntry.Folder -> LauncherDrawerVisualTile(
                                entry = entry,
                                allApps = apps,
                                lockedAppKeys = lockedAppKeys,
                                iconScale = preferences.iconScale,
                                showLabel = true,
                                compact = true,
                                layoutLocked = preferences.layoutLocked,
                                onLaunchApp = onLaunchApp,
                                onManageApp = onManageApp,
                                onOpenFolder = onOpenFolder,
                                modifier = Modifier.fillMaxWidth().height(78.dp),
                            )
                        }
                    }
                }
            }
        }
        LauncherDrawerLayoutMode.CATEGORY -> {
            val categoryGroups = remember(apps) {
                apps.groupBy(::drawerCategoryLabel)
                    .toList()
                    .sortedWith(
                        compareBy<Pair<String, List<LauncherActivityInfo>>>(
                            { drawerCategoryRank(it.first) },
                            { it.first },
                        ),
                    )
            }
            val listState = rememberLazyListState()
            val dismissConnection = rememberDrawerDismissNestedScrollConnection(
                canScrollBackward = { listState.canScrollBackward },
                onDismiss = onDismiss,
            )
            LazyColumn(
                state = listState,
                modifier = modifier
                    .fillMaxWidth()
                    .nestedScroll(dismissConnection),
                contentPadding = PaddingValues(vertical = standardSpacing),
                verticalArrangement = Arrangement.spacedBy(compactSpacing),
            ) {
                if (smartFolders.isNotEmpty()) {
                    item(key = "category:smart-folders") {
                        DrawerCategoryHeader(
                            label = "Smart folders",
                            count = smartFolders.size,
                            secondaryColor = secondaryColor,
                        )
                    }
                    val smartFolderRows = smartFolders.chunked(
                        preferences.drawerColumns.coerceAtLeast(1),
                    )
                    lazyItems(
                        smartFolderRows,
                        key = { row -> "category:smart:" + row.first().id },
                    ) { rowSmartFolders ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(standardSpacing),
                        ) {
                            rowSmartFolders.forEach { smartFolder ->
                                LauncherDrawerSmartFolderTile(
                                    folder = smartFolder,
                                    allApps = apps,
                                    iconScale = preferences.iconScale,
                                    showLabel = experiencePreferences.showDrawerLabels,
                                    onOpen = { onOpenSmartFolder(smartFolder.kind) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(gridTileHeight),
                                )
                            }
                            repeat(preferences.drawerColumns - rowSmartFolders.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }

                if (folders.isNotEmpty()) {
                    item(key = "category:folders") {
                        DrawerCategoryHeader(
                            label = "Folders",
                            count = folders.size,
                            secondaryColor = secondaryColor,
                        )
                    }
                    val folderRows = folders.chunked(preferences.drawerColumns.coerceAtLeast(1))
                    lazyItems(
                        folderRows,
                        key = { row -> "category:folders:" + row.first().id },
                    ) { rowFolders ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(standardSpacing),
                        ) {
                            rowFolders.forEach { folder ->
                                LauncherDrawerVisualTile(
                                    entry = LauncherDrawerVisualEntry.Folder(folder),
                                    allApps = apps,
                                    lockedAppKeys = lockedAppKeys,
                                    iconScale = preferences.iconScale,
                                    showLabel = experiencePreferences.showDrawerLabels,
                                    compact = false,
                                    layoutLocked = preferences.layoutLocked,
                                    onLaunchApp = onLaunchApp,
                                    onManageApp = onManageApp,
                                    onOpenFolder = onOpenFolder,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(gridTileHeight),
                                )
                            }
                            repeat(preferences.drawerColumns - rowFolders.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }

                categoryGroups.forEach { (category, categoryApps) ->
                    item(key = "category:$category") {
                        DrawerCategoryHeader(
                            label = category,
                            count = categoryApps.size,
                            secondaryColor = secondaryColor,
                        )
                    }
                    val rows = categoryApps.chunked(preferences.drawerColumns.coerceAtLeast(1))
                    lazyItems(
                        rows,
                        key = { row -> "category:$category:" + row.first().workspaceKey() },
                    ) { rowApps ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(standardSpacing),
                        ) {
                            rowApps.forEach { app ->
                                LauncherAppTile(
                                    app = app,
                                    iconScale = preferences.iconScale,
                                    showLabel = experiencePreferences.showDrawerLabels,
                                    compact = false,
                                    fixedGridGeometry = true,
                                    pinnedInDrawer = app.workspaceKey() in pinnedAppKeys,
                                    lockedByLauncher = app.workspaceKey() in lockedAppKeys,
                                    onClick = { onLaunchApp(app) },
                                    onLongClick = { anchor -> onManageApp(app, anchor) },
                                    dragData = if (preferences.layoutLocked) null else {
                                        LauncherAppDragData(
                                            appKey = app.workspaceKey(),
                                            origin = LauncherAppDragOrigin.DRAWER,
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(gridTileHeight),
                                )
                            }
                            repeat(preferences.drawerColumns - rowApps.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

        }
    }
}

@Composable
private fun rememberDrawerDismissNestedScrollConnection(
    canScrollBackward: () -> Boolean,
    onDismiss: () -> Unit,
): NestedScrollConnection {
    val dismissThreshold = with(LocalDensity.current) { 56.dp.toPx() }
    val currentCanScrollBackward by rememberUpdatedState(canScrollBackward)
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    return remember(dismissThreshold) {
        object : NestedScrollConnection {
            private var downwardDrag = 0f
            private var triggered = false

            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (source != NestedScrollSource.UserInput) {
                    return Offset.Zero
                }

                if (available.y <= 0f || currentCanScrollBackward()) {
                    downwardDrag = 0f
                    triggered = false
                    return Offset.Zero
                }

                if (!triggered) {
                    downwardDrag += available.y
                    if (downwardDrag >= dismissThreshold) {
                        triggered = true
                        currentOnDismiss()
                    }
                }

                return if (triggered) Offset(0f, available.y) else Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                downwardDrag = 0f
                triggered = false
                return Velocity.Zero
            }
        }
    }
}

private fun drawerCategoryLabel(app: LauncherActivityInfo): String =
    when (app.applicationInfo.category) {
        ApplicationInfo.CATEGORY_GAME -> "Games"
        ApplicationInfo.CATEGORY_AUDIO,
        ApplicationInfo.CATEGORY_VIDEO,
        ApplicationInfo.CATEGORY_IMAGE,
        -> "Media"
        ApplicationInfo.CATEGORY_SOCIAL -> "Social"
        ApplicationInfo.CATEGORY_NEWS -> "News"
        ApplicationInfo.CATEGORY_MAPS -> "Travel & maps"
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Productivity"
        else -> "Other"
    }

private fun drawerCategoryRank(label: String): Int =
    when (label) {
        "Productivity" -> 0
        "Social" -> 1
        "Media" -> 2
        "Games" -> 3
        "Travel & maps" -> 4
        "News" -> 5
        else -> 6
    }

@Composable
private fun DrawerCategoryHeader(
    label: String,
    count: Int,
    secondaryColor: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = GlazeMetrics.space2, bottom = GlazeMetrics.space1),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Surface(
            shape = RoundedCornerShape(GlazeMetrics.radiusPill),
            color = secondaryColor.copy(alpha = 0.10f),
            border = BorderStroke(1.dp, secondaryColor.copy(alpha = 0.18f)),
        ) {
            Text(
                count.toString(),
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = secondaryColor,
            )
        }
    }
}

@Composable
private fun DrawerPageDots(
    pageCount: Int,
    currentPage: Int,
    onSelectPage: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { page ->
            val selected = page == currentPage
            Surface(
                onClick = { onSelectPage(page) },
                modifier = Modifier
                    .size(LauncherDrawerPageIndicatorPolicy.TOUCH_TARGET_DP.dp)
                    .testTag("launcher-drawer-page-" + (page + 1))
                    .semantics {
                        this.selected = selected
                        contentDescription =
                            "App drawer page " + (page + 1) + " of " + pageCount
                    },
                shape = CircleShape,
                color = Color.Transparent,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        modifier = Modifier.size(
                            LauncherDrawerPageIndicatorPolicy.visualSizeDp(selected).dp,
                        ),
                        shape = CircleShape,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
                        },
                    ) {}
                }
            }
        }
    }
}


private enum class LauncherSettingsCategory(
    val title: String,
    val summary: String,
    val keywords: String,
) {
    HOME(
        "Home",
        "Grid, pages, labels, Glance and workspace behavior",
        "home grid pages default page looping indicators labels padding new apps lock layout wallpaper glance quick actions hints",
    ),
    DRAWER(
        "App Drawer",
        "Layout, organization, profiles and local discovery",
        "apps drawer grid compact list category density icon size sorting pinned tabs folders smart hidden work profile recent updated suggested search",
    ),
    DOCK(
        "Dock",
        "Favorites, Dock pages, Search and Glaze presentation",
        "dock favorites apps pages overflow icon size spacing search glaze clear solid raised edge loop labels",
    ),
    FOLDERS(
        "Folders",
        "Home and App Drawer folder organization and presentation",
        "folders folder grid ordering preview background opacity corner radius large smart create rename membership",
    ),
    SEARCH(
        "Universal Search",
        "Search sources, connected providers, history and Home entry",
        "universal search sources providers files contacts calls messages shortcuts online connected history recent frequent suggestions categories home dock",
    ),
    WIDGETS(
        "Widgets & Glaze Cards",
        "Android widgets and Launcher-owned information surfaces",
        "widgets glaze cards calendar weather glance search quick actions battery date month clock status tasks contacts media",
    ),
    GESTURES(
        "Gestures & Actions",
        "Assign Home gestures to Launcher actions or installed apps",
        "gestures actions sensitivity responsive deliberate swipe up down left right double tap hold pinch two finger home button notifications quick settings search app shortcut editor lock",
    ),
    APPEARANCE(
        "Appearance",
        "Glaze theme, icons, typography and visual effects",
        "appearance theme glaze system light dark scheduled wallpaper colors accent icons shape adaptive mask themed typography drawer dock folder search effects blur translucency gradients shadows motion reduced",
    ),
    BADGES(
        "Notifications & Badges",
        "Optional Launcher-owned notification indicators",
        "notifications badges unread dots numeric notification listener access privacy style size corner",
    ),
    PRIVACY(
        "Privacy & Permissions",
        "Local data, permissions, providers, hidden apps and App Lock",
        "privacy permissions app lock hidden apps local history suggestions contacts calls messages files providers diagnostics network advertising tracking",
    ),
    BACKUP(
        "Backup & Restore",
        "Versioned local Launcher configuration recovery",
        "backup restore export import local workspace preferences search theme validation reset recovery portable",
    ),
    ADVANCED(
        "Advanced",
        "Default Home, onboarding, compatibility and diagnostics",
        "advanced default home role setup onboarding startup wizard compatibility diagnostics import export developer experimental",
    ),
    ABOUT(
        "About",
        "Build, lifecycle, license, privacy and product information",
        "about version build development release lifecycle license open source privacy security documentation",
    ),
}

@Composable
private fun LauncherSettingsCategoryIcon(
    category: LauncherSettingsCategory,
    color: Color,
) {
    val glyph = when (category) {
        LauncherSettingsCategory.HOME -> LauncherOutlineGlyph.HOME
        LauncherSettingsCategory.DRAWER -> LauncherOutlineGlyph.APPS
        LauncherSettingsCategory.DOCK -> LauncherOutlineGlyph.DOCK
        LauncherSettingsCategory.FOLDERS -> LauncherOutlineGlyph.FOLDER
        LauncherSettingsCategory.SEARCH -> LauncherOutlineGlyph.SEARCH
        LauncherSettingsCategory.WIDGETS -> LauncherOutlineGlyph.WIDGETS
        LauncherSettingsCategory.GESTURES -> LauncherOutlineGlyph.GESTURE
        LauncherSettingsCategory.APPEARANCE -> LauncherOutlineGlyph.APPEARANCE
        LauncherSettingsCategory.BADGES -> LauncherOutlineGlyph.BELL
        LauncherSettingsCategory.PRIVACY -> LauncherOutlineGlyph.SHIELD
        LauncherSettingsCategory.BACKUP -> LauncherOutlineGlyph.BACKUP
        LauncherSettingsCategory.ADVANCED -> LauncherOutlineGlyph.SLIDERS
        LauncherSettingsCategory.ABOUT -> LauncherOutlineGlyph.INFO
    }
    LauncherOutlineGlyph(
        glyph = glyph,
        color = color,
        modifier = Modifier.size(26.dp),
    )
}

@Composable
private fun LauncherSettingsSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "Search Launcher settings",
    inputTestTag: String? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(GlazeMetrics.radiusPill),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.48f),
        ),
    ) {
        val searchIconColor = MaterialTheme.colorScheme.onSurfaceVariant
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .padding(start = GlazeMetrics.space3, end = GlazeMetrics.space2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
        ) {
            LauncherOutlineGlyph(
                glyph = LauncherOutlineGlyph.SEARCH,
                color = searchIconColor,
                modifier = Modifier.size(22.dp),
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (inputTestTag != null) {
                            Modifier.testTag(inputTestTag)
                        } else {
                            Modifier
                        },
                    ),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                decorationBox = { inner ->
                    if (value.isBlank()) {
                        Text(
                            placeholder,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    inner()
                },
            )
            if (value.isNotBlank()) {
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .semantics { contentDescription = "Clear search" },
                    onClick = { onValueChange("") },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        GlazePopupActionGlyph(
                            symbol = GlazePopupActionSymbol.CLOSE,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            iconSize = 16.dp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LauncherSettingsOverviewRow(
    category: LauncherSettingsCategory,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        color = Color.Transparent,
        shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(horizontal = GlazeMetrics.space3, vertical = GlazeMetrics.space2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space3),
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = RoundedCornerShape(15.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    LauncherSettingsCategoryIcon(
                        category = category,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    category.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    category.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
@Suppress("UNUSED_PARAMETER")
private fun LauncherSettingsRootSurface(
    apps: List<LauncherActivityInfo>,
    preferences: LauncherPreferences,
    drawerLayoutMode: LauncherDrawerLayoutMode,
    drawerSortOrderName: String?,
    experiencePreferences: LauncherExperiencePreferences,
    availableIconPacks: List<LauncherIconPackDescriptor>,
    themeMode: GlazeThemeMode,
    isDefaultHome: Boolean,
    onRequestHomeRole: () -> Unit,
    onManageFolders: () -> Unit,
    hiddenAppCount: Int,
    onManageHiddenApps: () -> Unit,
    lockedAppCount: Int,
    onManageAppLock: () -> Unit,
    onSetHomeGrid: (Int, Int) -> Unit,
    onSetDrawerColumns: (Int) -> Unit,
    onSetDrawerLayoutMode: (LauncherDrawerLayoutMode) -> Unit,
    onSetDrawerSortOrderName: (String?) -> Unit,
    onSetShowLabels: (Boolean) -> Unit,
    onSetIconScale: (Float) -> Unit,
    onSetIconShape: (LauncherIconShape) -> Unit,
    onSetIconPackPackage: (String?) -> Unit,
    onSetLayoutLocked: (Boolean) -> Unit,
    onSetUniversalSearchHomeMode: (LauncherUniversalSearchHomeMode) -> Unit,
    onSetHomeCardStyle: (LauncherHomeCardStyle) -> Unit,
    onSetShowHomeQuickActions: (Boolean) -> Unit,
    onSetShowHomePageIndicator: (Boolean) -> Unit,
    onSetShowHomeLabels: (Boolean) -> Unit,
    onSetShowDrawerLabels: (Boolean) -> Unit,
    onSetShowDrawerPageIndicator: (Boolean) -> Unit,
    onSetHomeAppMode: (LauncherHomeAppMode) -> Unit,
    onSetAddNewAppsToHome: (Boolean) -> Unit,
    onClearLocalUsage: () -> Unit,
    onSetHintsEnabled: (Boolean) -> Unit,
    onReplayStartupWizard: () -> Unit,
    onSetDrawerBackdrop: (LauncherDrawerBackdrop) -> Unit,
    onSetDrawerSearchPlacement: (LauncherDrawerSearchPlacement) -> Unit,
    onSetDrawerNavigation: (LauncherDrawerNavigation) -> Unit,
    onSetDrawerEntryMode: (LauncherDrawerEntryMode) -> Unit,
    onSetDrawerSpacing: (LauncherDrawerSpacing) -> Unit,
    onSetDrawerPageRows: (Int) -> Unit,
    onSetShowDrawerAppCount: (Boolean) -> Unit,
    onSetShowDrawerSuggestions: (Boolean) -> Unit,
    onSetHomeGlanceAlignment: (LauncherHomeGlanceAlignment) -> Unit,
    onSetHomeSearchPlacement: (LauncherHomeSearchPlacement) -> Unit,
    onSetHomeSearchStyle: (LauncherHomeSearchStyle) -> Unit,
    onSetHomeSpacing: (LauncherHomeSpacing) -> Unit,
    onSetDockStyle: (LauncherDockStyle) -> Unit,
    onSetDockPageSize: (Int) -> Unit,
    onSetDockLoopPages: (Boolean) -> Unit,
    onSetShowDockLabels: (Boolean) -> Unit,
    onSetShowDockSearch: (Boolean) -> Unit,
    onSetWallpaperShade: (LauncherWallpaperShade) -> Unit,
    onSetGestureSensitivity: (LauncherGestureSensitivity) -> Unit,
    onSetGestureAction: (LauncherHomeGesture, LauncherGestureAction) -> Unit,
    onOpenThemeManager: () -> Unit,
    onBack: () -> Unit,
) {
    var gestureToConfigure by remember { mutableStateOf<LauncherHomeGesture?>(null) }
    var showIconPackPicker by rememberSaveable { mutableStateOf(false) }
    val badgeContext = androidx.compose.ui.platform.LocalContext.current
    val settingsVisualPreferencesRepository = remember(badgeContext.applicationContext) {
        LauncherVisualPreferencesRepository(badgeContext.applicationContext)
    }
    val settingsVisualPreferences by
        settingsVisualPreferencesRepository.preferences.collectAsState(
            initial = LauncherVisualPreferences(),
        )
    val badgesEnabled by com.goreecloud.launcher.core.launcher.LauncherNotificationBadges.enabled
        .collectAsState()
    val badgeAccess by com.goreecloud.launcher.core.launcher.LauncherNotificationBadges.accessGranted
        .collectAsState()
    val badgeStyle by com.goreecloud.launcher.core.launcher.LauncherNotificationBadges.style
        .collectAsState()
    val badgeSize by com.goreecloud.launcher.core.launcher.LauncherNotificationBadges.size
        .collectAsState()
    val badgeCorner by com.goreecloud.launcher.core.launcher.LauncherNotificationBadges.corner
        .collectAsState()
    val folderPreviewLayout by com.goreecloud.launcher.core.launcher.LauncherFolderAppearance.layout
        .collectAsState()
    val folderPreviewShape by com.goreecloud.launcher.core.launcher.LauncherFolderAppearance.shape
        .collectAsState()
    val folderPreviewSize by com.goreecloud.launcher.core.launcher.LauncherFolderAppearance.size
        .collectAsState()
    val folderPreviewSurface by com.goreecloud.launcher.core.launcher.LauncherFolderAppearance.surface
        .collectAsState()
    val folderPreviewOutline by com.goreecloud.launcher.core.launcher.LauncherFolderAppearance.outline
        .collectAsState()

    val appsByKey = remember(apps) { apps.associateBy { it.workspaceKey() } }
    var selectedSettingsCategoryName by rememberSaveable { mutableStateOf<String?>(null) }
    var settingsQuery by rememberSaveable { mutableStateOf("") }
    val selectedSettingsCategory = LauncherSettingsCategory.entries.firstOrNull {
        it.name == selectedSettingsCategoryName
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        GlazeAtmosphere.softAqua.copy(alpha = 0.05f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = GlazeMetrics.space4, vertical = GlazeMetrics.space3),
            verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space3),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        selectedSettingsCategory?.title ?: "Launcher settings",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        selectedSettingsCategory?.summary
                            ?: "Home, App Drawer, Dock, Search, appearance, privacy and recovery in one place.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (selectedSettingsCategory == null) {
                    LauncherHeaderGlyphAction(
                        contentDescription = "Done",
                        symbol = GlazePopupActionSymbol.CHECK,
                        onClick = onBack,
                    )
                } else {
                    LauncherHeaderGlyphAction(
                        contentDescription = "Back",
                        symbol = GlazePopupActionSymbol.BACK,
                        onClick = { selectedSettingsCategoryName = null },
                    )
                }
            }

            if (selectedSettingsCategory == null) {
                LauncherSettingsSearchField(
                    value = settingsQuery,
                    onValueChange = { settingsQuery = it },
                )

                if (!isDefaultHome) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onRequestHomeRole,
                        shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.error.copy(alpha = 0.50f),
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(GlazeMetrics.space3),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space3),
                        ) {
                            Surface(
                                modifier = Modifier.size(42.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    GlazePopupActionGlyph(
                                        symbol = GlazePopupActionSymbol.INFO,
                                        color = MaterialTheme.colorScheme.error,
                                        iconSize = 22.dp,
                                    )
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Not set as default",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    "Tap to use GoreeCloud Launcher for the Home gesture.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                val normalizedQuery = settingsQuery.trim().lowercase(Locale.getDefault())
                val visibleCategories = LauncherSettingsCategory.entries.filter { category ->
                    normalizedQuery.isBlank() ||
                        category.title.lowercase(Locale.getDefault()).contains(normalizedQuery) ||
                        category.summary.lowercase(Locale.getDefault()).contains(normalizedQuery) ||
                        category.keywords.lowercase(Locale.getDefault()).contains(normalizedQuery)
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(GlazeMetrics.radiusExtraLarge),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f),
                    ),
                    tonalElevation = 0.dp,
                    shadowElevation = 1.dp,
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        visibleCategories.forEachIndexed { index, category ->
                            LauncherSettingsOverviewRow(
                                category = category,
                                onClick = {
                                    selectedSettingsCategoryName = category.name
                                    settingsQuery = ""
                                },
                            )
                            if (index != visibleCategories.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = GlazeMetrics.space3),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f),
                                )
                            }
                        }
                        if (visibleCategories.isEmpty()) {
                            Text(
                                "No settings categories match your search.",
                                modifier = Modifier.padding(GlazeMetrics.space4),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            SettingsSection(
                "Home",
                "Workspace layout, pages, labels and Glance",
                visible = selectedSettingsCategory == LauncherSettingsCategory.HOME,
            ) {
                Text(
                    "Clock & date",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                ChoiceRow(
                    choices = listOf("Clock", "Compact", "Off"),
                    selected = when (experiencePreferences.homeCardStyle) {
                        LauncherHomeCardStyle.CLOCK -> "Clock"
                        LauncherHomeCardStyle.COMPACT -> "Compact"
                        LauncherHomeCardStyle.OFF -> "Off"
                    },
                    onChoice = {
                        onSetHomeCardStyle(
                            when (it) {
                                "Compact" -> LauncherHomeCardStyle.COMPACT
                                "Off" -> LauncherHomeCardStyle.OFF
                                else -> LauncherHomeCardStyle.CLOCK
                            },
                        )
                    },
                )
                Text(
                    "Home grid",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                ChoiceRow(
                    choices = listOf("4×5", "5×6", "6×7"),
                    selected = preferences.homeColumns.toString() + "×" + preferences.homeRows.toString(),
                    onChoice = {
                        val parts = it.split("×")
                        onSetHomeGrid(parts[0].toInt(), parts[1].toInt())
                    },
                )
                Text(
                    "Home spacing",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                ChoiceRow(
                    choices = listOf("Compact", "Balanced", "Airy"),
                    selected = when (experiencePreferences.homeSpacing) {
                        LauncherHomeSpacing.COMPACT -> "Compact"
                        LauncherHomeSpacing.BALANCED -> "Balanced"
                        LauncherHomeSpacing.AIRY -> "Airy"
                    },
                    onChoice = {
                        onSetHomeSpacing(
                            when (it) {
                                "Compact" -> LauncherHomeSpacing.COMPACT
                                "Airy" -> LauncherHomeSpacing.AIRY
                                else -> LauncherHomeSpacing.BALANCED
                            },
                        )
                    },
                )
                Text(
                    "Clock alignment",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                ChoiceRow(
                    choices = listOf("Left", "Center"),
                    selected = if (
                        experiencePreferences.homeGlanceAlignment == LauncherHomeGlanceAlignment.CENTER
                    ) "Center" else "Left",
                    onChoice = {
                        onSetHomeGlanceAlignment(
                            if (it == "Center") LauncherHomeGlanceAlignment.CENTER
                            else LauncherHomeGlanceAlignment.LEFT,
                        )
                    },
                )
                SettingSwitch(
                    "Quick actions",
                    experiencePreferences.showHomeQuickActions,
                    onSetShowHomeQuickActions,
                )
                SettingSwitch(
                    "Show Home app labels",
                    experiencePreferences.showHomeLabels,
                    onSetShowHomeLabels,
                )
                SettingSwitch(
                    "Home page indicator",
                    experiencePreferences.showHomePageIndicator,
                    onSetShowHomePageIndicator,
                )
                Text(
                    "Page transition",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                ChoiceRow(
                    choices = listOf("Slide", "Fade", "Zoom", "Minimal"),
                    selected = settingsVisualPreferences.homePageTransition.displayName,
                    onChoice = { choice ->
                        settingsVisualPreferencesRepository.setHomePageTransition(
                            when (choice) {
                                "Fade" -> LauncherHomePageTransition.FADE
                                "Zoom" -> LauncherHomePageTransition.ZOOM
                                "Minimal" -> LauncherHomePageTransition.NONE
                                else -> LauncherHomePageTransition.SLIDE
                            },
                        )
                    },
                )
                Text(
                    "Automatic Home apps",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                ChoiceRow(
                    choices = listOf(
                        "No automatic apps",
                        "10 most recent apps",
                        "10 most used apps",
                    ),
                    selected = when (experiencePreferences.homeAppMode) {
                        LauncherHomeAppMode.NONE -> "No automatic apps"
                        LauncherHomeAppMode.RECENT -> "10 most recent apps"
                        LauncherHomeAppMode.MOST_USED -> "10 most used apps"
                    },
                    onChoice = {
                        onSetHomeAppMode(
                            when (it) {
                                "10 most recent apps" -> LauncherHomeAppMode.RECENT
                                "10 most used apps" -> LauncherHomeAppMode.MOST_USED
                                else -> LauncherHomeAppMode.NONE
                            },
                        )
                    },
                )
                SettingSwitch(
                    "Launcher hints",
                    !experiencePreferences.homeHintsDismissed,
                    onSetHintsEnabled,
                )
                SettingSwitch(
                    "Add new apps to Home",
                    experiencePreferences.addNewAppsToHome,
                    onSetAddNewAppsToHome,
                )
                SettingSwitch("Lock layout", preferences.layoutLocked, onSetLayoutLocked)
            }

            SettingsSection(
                "Dock",
                "Persistent favorites, pages, Search and material",
                visible = selectedSettingsCategory == LauncherSettingsCategory.DOCK,
            ) {
                Text(
                    "Dock style",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                ChoiceRow(
                    choices = listOf("Glaze", "Clear", "Solid", "Raised", "Edge"),
                    selected = when (experiencePreferences.dockStyle) {
                        LauncherDockStyle.GLASS -> "Glaze"
                        LauncherDockStyle.CLEAR -> "Clear"
                        LauncherDockStyle.SOLID -> "Solid"
                        LauncherDockStyle.RAISED -> "Raised"
                        LauncherDockStyle.EDGE -> "Edge"
                    },
                    onChoice = {
                        onSetDockStyle(
                            when (it) {
                                "Clear" -> LauncherDockStyle.CLEAR
                                "Solid" -> LauncherDockStyle.SOLID
                                "Raised" -> LauncherDockStyle.RAISED
                                "Edge" -> LauncherDockStyle.EDGE
                                else -> LauncherDockStyle.GLASS
                            },
                        )
                    },
                )
                Text(
                    "Glaze, Solid, Raised, Edge and background-free Clear treatments follow accessibility and reduced-transparency presentation policy.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Items per page",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                ChoiceRow(
                    choices = listOf("4", "5", "6", "7"),
                    selected = experiencePreferences.dockPageSize.toString(),
                    onChoice = { value -> value.toIntOrNull()?.let(onSetDockPageSize) },
                )
                SettingSwitch(
                    "Loop Dock pages",
                    experiencePreferences.dockLoopPages,
                    onSetDockLoopPages,
                )
                SettingSwitch(
                    "Dock labels",
                    experiencePreferences.showDockLabels,
                    onSetShowDockLabels,
                )
                SettingSwitch(
                    "Universal Search in Dock",
                    experiencePreferences.showDockSearch,
                    onSetShowDockSearch,
                )
                SettingsReadOnlyRow("Capacity", "Multiple pages · 48 dp minimum targets")
                SettingsReadOnlyRow("Navigate", "Swipe the Dock independently of Home pages")
                SettingsReadOnlyRow("Edit", "Long-press or drag an app")
            }

            SettingsSection(
                "Universal Search",
                "Home access, local sources and connected-provider boundaries",
                visible = selectedSettingsCategory == LauncherSettingsCategory.SEARCH,
            ) {
                ChoiceRow(
                    choices = listOf("Swipe down", "Movable", "Top", "Bottom"),
                    selected = when (
                        launcherHomeSearchSurface(
                            mode = preferences.universalSearchHomeMode,
                            placement = experiencePreferences.homeSearchPlacement,
                        )
                    ) {
                        LauncherHomeSearchSurface.SWIPE_DOWN_ONLY -> "Swipe down"
                        LauncherHomeSearchSurface.MOVABLE -> "Movable"
                        LauncherHomeSearchSurface.FIXED_TOP -> "Top"
                        LauncherHomeSearchSurface.FIXED_BOTTOM -> "Bottom"
                    },
                    onChoice = { choice ->
                        if (choice == "Swipe down") {
                            onSetUniversalSearchHomeMode(
                                LauncherUniversalSearchHomeMode.SWIPE_DOWN_ONLY,
                            )
                        } else {
                            onSetHomeSearchPlacement(
                                when (choice) {
                                    "Movable" -> LauncherHomeSearchPlacement.MOVABLE
                                    "Top" -> LauncherHomeSearchPlacement.TOP
                                    else -> LauncherHomeSearchPlacement.BOTTOM
                                },
                            )
                            onSetUniversalSearchHomeMode(
                                LauncherUniversalSearchHomeMode.PERMANENT,
                            )
                        }
                    },
                )
                if (preferences.universalSearchHomeMode == LauncherUniversalSearchHomeMode.PERMANENT) {
                    Text(
                        if (
                            experiencePreferences.homeSearchPlacement ==
                                LauncherHomeSearchPlacement.MOVABLE
                        ) {
                            "Movable Search uses the Home grid and can be dragged between pages. " +
                                "If there is no free 4 × 1 area, Launcher keeps the bottom bar visible."
                        } else {
                            "Fixed Search stays outside the Home grid."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (
                        experiencePreferences.homeSearchPlacement !=
                            LauncherHomeSearchPlacement.MOVABLE
                    ) {
                        Text(
                            "Home bar style",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        ChoiceRow(
                            choices = listOf("Glass", "Clear", "Solid"),
                            selected = when (experiencePreferences.homeSearchStyle) {
                                LauncherHomeSearchStyle.GLASS -> "Glass"
                                LauncherHomeSearchStyle.CLEAR -> "Clear"
                                LauncherHomeSearchStyle.SOLID -> "Solid"
                            },
                            onChoice = {
                                onSetHomeSearchStyle(
                                    when (it) {
                                        "Clear" -> LauncherHomeSearchStyle.CLEAR
                                        "Solid" -> LauncherHomeSearchStyle.SOLID
                                        else -> LauncherHomeSearchStyle.GLASS
                                    },
                                )
                            },
                        )
                    }
                }
                SettingsReadOnlyRow("Home gestures", "Configured in Gestures & Actions")
                SettingsReadOnlyRow("Core provider", "Installed apps · Launcher")
                SettingsReadOnlyRow(
                    "Sensitive local sources",
                    "Opt-in and permission-gated",
                )
                SettingsReadOnlyRow(
                    "Connected providers",
                    "Explicit enablement and authorization only",
                )
            }

            SettingsSection(
                "Widgets & Glaze Cards",
                "Android widgets and Launcher-owned information surfaces",
                visible = selectedSettingsCategory == LauncherSettingsCategory.WIDGETS,
            ) {
                SettingsReadOnlyRow("Android widgets", "Supported through Android AppWidgetHost")
                SettingsReadOnlyRow(
                    "GoreeCloud widgets",
                    WorkspaceWidgetCatalog.builtInTypeIds.size.toString() + " built-in types",
                )
                SettingsReadOnlyRow(
                    "Current utilities",
                    "Glance · Search · Quick actions · Battery · Calendar/time · Status",
                )
                Text(
                    "Add and manage current widgets through Edit Home → Widgets. " +
                        "Third-party widgets remain governed by Android widget-hosting contracts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SettingsReadOnlyRow("Glaze Cards", "Planned expansion · optional")
                Text(
                    "Glaze Cards are reserved for Launcher-owned, local-first information surfaces. " +
                        "They must never become advertising, sponsorship, affiliate-placement, or paid-ranking surfaces.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SettingsSection(
                "App Drawer",
                "Profiles, layout, density, organization and local discovery",
                visible = selectedSettingsCategory == LauncherSettingsCategory.DRAWER,
            ) {
                SettingsReadOnlyRow("Profiles", "User Apps · Work Apps when available")
                Text(
                    "Layout",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                ChoiceRow(
                    choices = listOf("Grid", "Compact", "List", "Category"),
                    selected = drawerLayoutMode.name.lowercase().replaceFirstChar { it.uppercase() },
                    onChoice = {
                        onSetDrawerLayoutMode(
                            when (it) {
                                "Compact" -> LauncherDrawerLayoutMode.COMPACT
                                "List" -> LauncherDrawerLayoutMode.LIST
                                "Category" -> LauncherDrawerLayoutMode.CATEGORY
                                else -> LauncherDrawerLayoutMode.GRID
                            },
                        )
                    },
                )
                Text(
                    "Sorting",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                val settingsDrawerSortOrder = remember(drawerSortOrderName) {
                    runCatching {
                        drawerSortOrderName
                            ?.let(LauncherDrawerSortOrder::valueOf)
                            ?: LauncherDrawerSortOrder.ALPHABETICAL
                    }.getOrDefault(LauncherDrawerSortOrder.ALPHABETICAL)
                }
                ChoiceRow(
                    choices = LauncherDrawerSortOrder.entries.map { it.displayName },
                    selected = settingsDrawerSortOrder.displayName,
                    onChoice = { choice ->
                        LauncherDrawerSortOrder.entries
                            .firstOrNull { it.displayName == choice }
                            ?.let { onSetDrawerSortOrderName(it.name) }
                    },
                )
                SettingSwitch(
                    "Alphabet navigation",
                    settingsVisualPreferences.showDrawerAlphabetIndex,
                    settingsVisualPreferencesRepository::setDrawerAlphabetIndex,
                )
                Text(
                    "Columns",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                ChoiceRow(
                    choices = listOf("4", "5", "6"),
                    selected = preferences.drawerColumns.toString(),
                    onChoice = { onSetDrawerColumns(it.toInt()) },
                )
                Text(
                    "Spacing",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                ChoiceRow(
                    choices = listOf("Tight", "Standard", "Relaxed"),
                    selected = when (experiencePreferences.drawerSpacing) {
                        LauncherDrawerSpacing.TIGHT -> "Tight"
                        LauncherDrawerSpacing.STANDARD -> "Standard"
                        LauncherDrawerSpacing.RELAXED -> "Relaxed"
                    },
                    onChoice = {
                        onSetDrawerSpacing(
                            when (it) {
                                "Tight" -> LauncherDrawerSpacing.TIGHT
                                "Relaxed" -> LauncherDrawerSpacing.RELAXED
                                else -> LauncherDrawerSpacing.STANDARD
                            },
                        )
                    },
                )
                Text(
                    "Background",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                ChoiceRow(
                    choices = listOf("Glass", "Solid"),
                    selected = if (experiencePreferences.drawerBackdrop == LauncherDrawerBackdrop.GLASS) "Glass" else "Solid",
                    onChoice = {
                        onSetDrawerBackdrop(
                            if (it == "Solid") LauncherDrawerBackdrop.SOLID
                            else LauncherDrawerBackdrop.GLASS,
                        )
                    },
                )
                Text(
                    "Search bar",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                ChoiceRow(
                    choices = listOf("Off", "Top", "Bottom"),
                    selected = when (experiencePreferences.drawerSearchPlacement) {
                        LauncherDrawerSearchPlacement.OFF -> "Off"
                        LauncherDrawerSearchPlacement.TOP -> "Top"
                        LauncherDrawerSearchPlacement.BOTTOM -> "Bottom"
                    },
                    onChoice = {
                        val placement = when (it) {
                            "Top" -> LauncherDrawerSearchPlacement.TOP
                            "Bottom" -> LauncherDrawerSearchPlacement.BOTTOM
                            else -> LauncherDrawerSearchPlacement.OFF
                        }
                        onSetDrawerSearchPlacement(placement)
                        if (placement == LauncherDrawerSearchPlacement.OFF) {
                            onSetDrawerEntryMode(LauncherDrawerEntryMode.BROWSE)
                        }
                    },
                )
                if (
                    experiencePreferences.drawerSearchPlacement !=
                        LauncherDrawerSearchPlacement.OFF
                ) {
                    Text(
                        "Open Apps to",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    ChoiceRow(
                        choices = listOf("Browse", "Search ready"),
                        selected = if (
                            experiencePreferences.drawerEntryMode ==
                                LauncherDrawerEntryMode.SEARCH_FIRST
                        ) "Search ready" else "Browse",
                        onChoice = {
                            onSetDrawerEntryMode(
                                if (it == "Search ready") {
                                    LauncherDrawerEntryMode.SEARCH_FIRST
                                } else {
                                    LauncherDrawerEntryMode.BROWSE
                                },
                            )
                        },
                    )
                }
                Text(
                    "Navigation",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                ChoiceRow(
                    choices = listOf("Pages", "Scroll"),
                    selected = if (
                        experiencePreferences.drawerNavigation == LauncherDrawerNavigation.PAGES
                    ) "Pages" else "Scroll",
                    onChoice = {
                        onSetDrawerNavigation(
                            if (it == "Scroll") LauncherDrawerNavigation.SCROLL
                            else LauncherDrawerNavigation.PAGES,
                        )
                    },
                )
                if (
                    experiencePreferences.drawerNavigation == LauncherDrawerNavigation.PAGES &&
                    drawerLayoutMode != LauncherDrawerLayoutMode.LIST
                ) {
                    Text(
                        "Rows per page",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    ChoiceRow(
                        choices = listOf("4", "5", "6"),
                        selected = experiencePreferences.drawerPageRows.toString(),
                        onChoice = { onSetDrawerPageRows(it.toInt()) },
                    )
                }
                SettingSwitch(
                    "Show app labels",
                    experiencePreferences.showDrawerLabels,
                    onSetShowDrawerLabels,
                )
                SettingSwitch(
                    "Page indicator",
                    experiencePreferences.showDrawerPageIndicator,
                    onSetShowDrawerPageIndicator,
                )
                SettingSwitch(
                    "Show app count",
                    experiencePreferences.showDrawerAppCount,
                    onSetShowDrawerAppCount,
                )
                SettingSwitch(
                    "Suggested apps",
                    experiencePreferences.showDrawerSuggestions,
                    onSetShowDrawerSuggestions,
                )
                Text(
                    "Suggested apps use only Launcher-local recent/frequent launch signals. " +
                        "They are off by default, require no Android Usage Access, and fall back " +
                        "to deterministic A–Z when there is no truthful local history.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SettingsReadOnlyRow(
                    "Discovery",
                    "Pinned · New · Updated" +
                        if (experiencePreferences.showDrawerSuggestions) " · Suggested" else "",
                )
                GlazeSettingsAction(
                    title = "Hidden apps",
                    summary = "Hide apps from App Drawer and Universal Search. Home, Dock and folders stay unchanged.",
                    value = if (hiddenAppCount == 0) "None" else hiddenAppCount.toString(),
                    onClick = onManageHiddenApps,
                    modifier = Modifier.testTag("launcher-settings-hidden-apps"),
                )
            }

            SettingsSection(
                "Folders",
                "Create, organize and customize",
                visible = selectedSettingsCategory == LauncherSettingsCategory.FOLDERS,
            ) {
                GlazeSettingsAction(
                    title = "Manage folders",
                    summary = "Create, rename or add folders without leaving Settings.",
                    value = "Open",
                    onClick = onManageFolders,
                )
                Text(
                    "Folder icon layout",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                ChoiceRow(
                    choices = listOf("Grid", "Radial", "Stack", "Fan", "Line"),
                    selected = folderPreviewLayout.name.lowercase().replaceFirstChar { it.uppercase() },
                    onChoice = { selection ->
                        com.goreecloud.launcher.core.launcher.LauncherFolderAppearance.setLayout(
                            badgeContext,
                            com.goreecloud.launcher.core.launcher.LauncherFolderPreviewLayout.entries
                                .first { it.name.equals(selection, ignoreCase = true) },
                        )
                    },
                )
                Text(
                    "Folder background shape",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                ChoiceRow(
                    choices = listOf("Follow icons", "Round", "Squircle", "Rounded square"),
                    selected = when (folderPreviewShape) {
                        com.goreecloud.launcher.core.launcher.LauncherFolderPreviewShape.FOLLOW_ICONS ->
                            "Follow icons"
                        com.goreecloud.launcher.core.launcher.LauncherFolderPreviewShape.ROUND ->
                            "Round"
                        com.goreecloud.launcher.core.launcher.LauncherFolderPreviewShape.SQUIRCLE ->
                            "Squircle"
                        com.goreecloud.launcher.core.launcher.LauncherFolderPreviewShape.ROUNDED_SQUARE ->
                            "Rounded square"
                    },
                    onChoice = { selection ->
                        val next = when (selection) {
                            "Round" ->
                                com.goreecloud.launcher.core.launcher.LauncherFolderPreviewShape.ROUND
                            "Squircle" ->
                                com.goreecloud.launcher.core.launcher.LauncherFolderPreviewShape.SQUIRCLE
                            "Rounded square" ->
                                com.goreecloud.launcher.core.launcher.LauncherFolderPreviewShape.ROUNDED_SQUARE
                            else ->
                                com.goreecloud.launcher.core.launcher.LauncherFolderPreviewShape.FOLLOW_ICONS
                        }
                        com.goreecloud.launcher.core.launcher.LauncherFolderAppearance.setShape(
                            badgeContext, next,
                        )
                    },
                )
                Text(
                    "Folder icon size",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                ChoiceRow(
                    choices = listOf("Small", "Medium", "Large"),
                    selected = folderPreviewSize.name.lowercase().replaceFirstChar { it.uppercase() },
                    onChoice = { selection ->
                        com.goreecloud.launcher.core.launcher.LauncherFolderAppearance.setSize(
                            badgeContext,
                            com.goreecloud.launcher.core.launcher.LauncherFolderPreviewSize.entries
                                .first { it.name.equals(selection, ignoreCase = true) },
                        )
                    },
                )
                Text(
                    "Folder background",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                ChoiceRow(
                    choices = listOf("Glass", "Solid", "Accent"),
                    selected = folderPreviewSurface.name.lowercase().replaceFirstChar {
                        it.uppercase()
                    },
                    onChoice = { selection ->
                        com.goreecloud.launcher.core.launcher.LauncherFolderAppearance.setSurface(
                            badgeContext,
                            com.goreecloud.launcher.core.launcher.LauncherFolderPreviewSurface.entries
                                .first { it.name.equals(selection, ignoreCase = true) },
                        )
                    },
                )
                SettingSwitch(
                    "Folder icon outline",
                    folderPreviewOutline,
                    { enabled ->
                        com.goreecloud.launcher.core.launcher.LauncherFolderAppearance.setOutline(
                            badgeContext, enabled,
                        )
                    },
                )
                Text(
                    "Folder icon appearance is stored on this device. Preview choices change " +
                        "only the visual presentation, never folder membership or placement.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Folders appear alphabetically with your personal apps. " +
                        "Long-press a Home folder to drag it to another free cell, or release " +
                        "it at a page edge to move it to the adjacent Home page.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SettingsSection(
                "Icons",
                "Shape, size and icon packs",
                visible = selectedSettingsCategory == LauncherSettingsCategory.APPEARANCE,
            ) {
                Text(
                    "Icon shape",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                IconShapeChoiceGrid(
                    selected = experiencePreferences.iconShape,
                    onSelect = onSetIconShape,
                )
                Text(
                    "Icon size",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                ChoiceRow(
                    choices = listOf("Small", "Medium", "Large"),
                    selected = when {
                        preferences.iconScale < 0.95f -> "Small"
                        preferences.iconScale > 1.05f -> "Large"
                        else -> "Medium"
                    },
                    onChoice = {
                        onSetIconScale(
                            when (it) {
                                "Small" -> 0.85f
                                "Large" -> 1.15f
                                else -> 1f
                            },
                        )
                    },
                )
                val selectedPackLabel = experiencePreferences.iconPackPackage
                    ?.let { selectedPackage ->
                        availableIconPacks.firstOrNull { it.packageName == selectedPackage }?.label
                            ?: selectedPackage.substringAfterLast('.')
                    }
                    ?: "Original icons"
                GlazeSettingsAction(
                    title = "Icon pack",
                    summary = if (availableIconPacks.isEmpty()) {
                        "Install a compatible icon pack or keep original app artwork."
                    } else {
                        "Use original artwork or an installed compatible icon pack."
                    },
                    value = selectedPackLabel,
                    onClick = { showIconPackPicker = true },
                )
                Text(
                    "Rounded square is the default. Original leaves Android-provided icon artwork unmasked.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SettingsSection(
                "Notifications & Badges",
                "Optional local unread indicators and Android notification access",
                visible = selectedSettingsCategory == LauncherSettingsCategory.BADGES,
            ) {
                Text(
                    "Android notification-listener access can expose notification details " +
                        "from other apps and permitted profiles. GoreeCloud Launcher uses " +
                        "it only for temporary per-app/profile counts; it does not store " +
                        "notification content. Access is optional and can be revoked in Android Settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SettingSwitch(
                    "Show badges",
                    badgesEnabled,
                    { enabled ->
                        com.goreecloud.launcher.core.launcher.LauncherNotificationBadges.setEnabled(
                            badgeContext, enabled,
                        )
                        if (enabled && !badgeAccess) {
                            runCatching {
                                badgeContext.startActivity(
                                    android.content.Intent(
                                        android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS,
                                    ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            }
                        }
                    },
                )
                if (badgesEnabled) {
                    Text(
                        if (badgeAccess) {
                            "Android notification access is enabled."
                        } else {
                            "Notification access is still off. Development builds installed from " +
                                "outside a trusted app store can also be blocked by Android Restricted Settings."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!badgeAccess) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("launcher-badge-access-required"),
                            shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.36f),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                            ),
                        ) {
                            Column(
                                modifier = Modifier.padding(GlazeMetrics.space3),
                                verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                            ) {
                                Text(
                                    "Android access required",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    "The side-by-side Dev APK cannot bypass Android’s Restricted Settings gate. " +
                                        "If the notification-access switch is greyed out, first open App info and " +
                                        "choose the top-right menu → Allow restricted settings. Then return and grant " +
                                        "notification access.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                                ) {
                                    FilledTonalButton(
                                        onClick = {
                                            runCatching {
                                                badgeContext.startActivity(
                                                    android.content.Intent(
                                                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                        Uri.parse("package:" + badgeContext.packageName),
                                                    ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                                                )
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                    ) { Text("1 · App info") }
                                    FilledTonalButton(
                                        onClick = {
                                            runCatching {
                                                badgeContext.startActivity(
                                                    android.content.Intent(
                                                        android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS,
                                                    ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                                                )
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                    ) { Text("2 · Access") }
                                }
                                TextButton(
                                    onClick = {
                                        com.goreecloud.launcher.core.launcher.LauncherNotificationBadges
                                            .refreshAccess(badgeContext)
                                    },
                                    modifier = Modifier.align(Alignment.End),
                                ) {
                                    Text("Check again")
                                }
                            }
                        }
                    }
                    Text(
                        "Badge style",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    ChoiceRow(
                        choices = listOf("Count", "Dot"),
                        selected = if (badgeStyle ==
                            com.goreecloud.launcher.core.launcher.LauncherBadgeStyle.DOT
                        ) "Dot" else "Count",
                        onChoice = { choice ->
                            com.goreecloud.launcher.core.launcher.LauncherNotificationBadges.setStyle(
                                badgeContext,
                                if (choice == "Dot")
                                    com.goreecloud.launcher.core.launcher.LauncherBadgeStyle.DOT
                                else com.goreecloud.launcher.core.launcher.LauncherBadgeStyle.COUNT,
                            )
                        },
                    )
                    Text(
                        "Badge size",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    ChoiceRow(
                        choices = listOf("Small", "Medium", "Large"),
                        selected = badgeSize.name.lowercase()
                            .replaceFirstChar { it.uppercase() },
                        onChoice = { choice ->
                            com.goreecloud.launcher.core.launcher.LauncherNotificationBadges.setSize(
                                badgeContext,
                                com.goreecloud.launcher.core.launcher.LauncherBadgeSize.entries
                                    .first { it.name.equals(choice, ignoreCase = true) },
                            )
                        },
                    )
                    Text(
                        "Badge position",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    ChoiceRow(
                        choices = listOf("Top left", "Top right", "Bottom left", "Bottom right"),
                        selected = when (badgeCorner) {
                            com.goreecloud.launcher.core.launcher.LauncherBadgeCorner.TOP_START ->
                                "Top left"
                            com.goreecloud.launcher.core.launcher.LauncherBadgeCorner.TOP_END ->
                                "Top right"
                            com.goreecloud.launcher.core.launcher.LauncherBadgeCorner.BOTTOM_START ->
                                "Bottom left"
                            com.goreecloud.launcher.core.launcher.LauncherBadgeCorner.BOTTOM_END ->
                                "Bottom right"
                        },
                        onChoice = { choice ->
                            val corner = when (choice) {
                                "Top left" ->
                                    com.goreecloud.launcher.core.launcher.LauncherBadgeCorner.TOP_START
                                "Bottom left" ->
                                    com.goreecloud.launcher.core.launcher.LauncherBadgeCorner.BOTTOM_START
                                "Bottom right" ->
                                    com.goreecloud.launcher.core.launcher.LauncherBadgeCorner.BOTTOM_END
                                else ->
                                    com.goreecloud.launcher.core.launcher.LauncherBadgeCorner.TOP_END
                            }
                            com.goreecloud.launcher.core.launcher.LauncherNotificationBadges.setCorner(
                                badgeContext, corner,
                            )
                        },
                    )
                }
                Text(
                    "Notification access is optional and off by default. Android grants broad " +
                        "notification visibility; Launcher only retains package/profile counts " +
                        "in memory. It does not keep or expose notification text or send data.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SettingsSection(
                "Appearance",
                "Glaze theme and wallpaper treatment",
                visible = selectedSettingsCategory == LauncherSettingsCategory.APPEARANCE,
            ) {
                GlazeSettingsAction(
                    title = "Theme Manager",
                    summary = "System, Light, Dark and Deep Dark",
                    value = themeMode.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() },
                    onClick = onOpenThemeManager,
                )
                Text(
                    "Wallpaper shade",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                ChoiceRow(
                    choices = listOf("Off", "Soft", "Strong"),
                    selected = when (experiencePreferences.wallpaperShade) {
                        LauncherWallpaperShade.OFF -> "Off"
                        LauncherWallpaperShade.SOFT -> "Soft"
                        LauncherWallpaperShade.STRONG -> "Strong"
                    },
                    onChoice = {
                        onSetWallpaperShade(
                            when (it) {
                                "Off" -> LauncherWallpaperShade.OFF
                                "Strong" -> LauncherWallpaperShade.STRONG
                                else -> LauncherWallpaperShade.SOFT
                            },
                        )
                    },
                )
            }

            SettingsSection(
                "Gestures & Actions",
                "Assign Home gestures to Launcher actions or installed apps",
                visible = selectedSettingsCategory == LauncherSettingsCategory.GESTURES,
            ) {
                Text(
                    "Vertical swipe sensitivity",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                ChoiceRow(
                    choices = LauncherGestureSensitivity.entries.map { it.displayName },
                    selected = experiencePreferences.gestureSensitivity.displayName,
                    onChoice = { selected ->
                        onSetGestureSensitivity(
                            LauncherGestureSensitivity.entries.firstOrNull {
                                it.displayName == selected
                            } ?: LauncherGestureSensitivity.STANDARD,
                        )
                    },
                )
                Text(
                    "Responsive activates swipe up/down with less travel; Deliberate requires more. Standard preserves the current Home thresholds.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LauncherHomeGesture.entries
                    .filterNot { it == LauncherHomeGesture.TAP_AND_HOLD }
                    .forEach { gesture ->
                    val action = when (gesture) {
                        LauncherHomeGesture.SWIPE_UP -> experiencePreferences.swipeUpAction
                        LauncherHomeGesture.SWIPE_DOWN -> experiencePreferences.swipeDownAction
                        LauncherHomeGesture.SWIPE_LEFT -> experiencePreferences.swipeLeftAction
                        LauncherHomeGesture.SWIPE_RIGHT -> experiencePreferences.swipeRightAction
                        LauncherHomeGesture.DOUBLE_TAP -> experiencePreferences.doubleTapAction
                        LauncherHomeGesture.TAP_AND_HOLD -> experiencePreferences.tapAndHoldAction
                    }
                    GestureAssignmentRow(
                        gesture = gesture,
                        actionLabel = gestureActionLabel(action, appsByKey),
                        onClick = { gestureToConfigure = gesture },
                    )
                }
                SettingsReadOnlyRow("Tap and hold", "Edit Home")
                Text(
                    "Tap and hold on empty Home space is reserved for Edit Home so Wallpaper, Widgets, Pages, Apps, and Settings remain consistently reachable. Swipe left/right and Double-tap remain configurable empty-space gestures.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (showIconPackPicker) {
                LauncherIconPackPickerSheet(
                    packs = availableIconPacks,
                    selectedPackage = experiencePreferences.iconPackPackage,
                    onSelect = { packageName ->
                        onSetIconPackPackage(packageName)
                        showIconPackPicker = false
                    },
                    onDismiss = { showIconPackPicker = false },
                )
            }

            SettingsSection(
                "Privacy & Permissions",
                "Local data, permissions and protected Launcher actions",
                visible = selectedSettingsCategory == LauncherSettingsCategory.PRIVACY,
            ) {
                GlazeSettingsAction(
                    title = "App Lock",
                    summary = "Require Android device authentication before Launcher opens selected apps.",
                    value = if (lockedAppCount == 0) "None" else "$lockedAppCount locked",
                    onClick = onManageAppLock,
                    modifier = Modifier.testTag("launcher-settings-app-lock"),
                )
                Text(
                    "App Lock protects launches that begin inside GoreeCloud Launcher. Android Settings, " +
                        "notifications, deep links, other launchers, and other apps remain outside Launcher authority.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                GlazeSettingsAction(
                    title = "Hidden apps",
                    summary = "Manage apps hidden from Apps and Universal Search.",
                    value = if (hiddenAppCount == 0) "None" else hiddenAppCount.toString(),
                    onClick = onManageHiddenApps,
                )
                GlazeSettingsAction(
                    title = "Local app activity",
                    summary = "Clear Launcher-local launch counts and bounded recency used by local suggestions and usage-based ordering.",
                    value = "Clear",
                    onClick = onClearLocalUsage,
                )
                SettingsReadOnlyRow(
                    "Suggested apps",
                    if (experiencePreferences.showDrawerSuggestions) "Enabled · local only" else "Off",
                )
                SettingsReadOnlyRow("Core operation", "No account or network required")
                Text(
                    "GoreeCloud Launcher does not use advertising networks, sponsored application placement, " +
                        "affiliate ranking, behavioral tracking, or remote analytics for core Launcher behavior.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SettingsSection(
                "Backup & Restore",
                "Versioned local Launcher recovery foundations",
                visible = selectedSettingsCategory == LauncherSettingsCategory.BACKUP,
            ) {
                SettingsReadOnlyRow("Preference format", "goreecloud-launcher-preferences/1")
                SettingsReadOnlyRow("Validation", "Strict · fail closed")
                SettingsReadOnlyRow("Startup recovery", "Journaled local reconciliation")
                Text(
                    "Current Development source contains versioned preference/workspace portability and " +
                        "restore-recovery foundations. A complete end-user create/export/import workflow for " +
                        "all newer Dock, Drawer, folder, widget, Search and Glaze state remains acceptance-gated.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SettingsReadOnlyRow("Full configuration export/import", "Development · not yet complete")
            }

            SettingsSection(
                "Advanced",
                "Default Home, onboarding, compatibility and Development tools",
                visible = selectedSettingsCategory == LauncherSettingsCategory.ADVANCED,
            ) {
                if (!isDefaultHome) {
                    GlazeSettingsAction(
                        title = "Default Home app",
                        summary = "Use GoreeCloud Launcher for the Android Home gesture.",
                        value = "Set Home",
                        onClick = onRequestHomeRole,
                    )
                } else {
                    SettingsReadOnlyRow("Default Home app", "GoreeCloud Launcher")
                }
                GlazeSettingsAction(
                    title = "Review Launcher setup",
                    summary = "Replay first-use guidance without clearing Home, Search, appearance or privacy choices.",
                    value = "Open",
                    onClick = onReplayStartupWizard,
                )
                SettingsReadOnlyRow("Diagnostics", "Local-first Development diagnostics")
                Text(
                    "Experimental or compatibility controls must remain clearly identified and do not imply Stable support.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SettingsSection(
                "About",
                "GoreeCloud Launcher product and lifecycle information",
                visible = selectedSettingsCategory == LauncherSettingsCategory.ABOUT,
            ) {
                SettingsReadOnlyRow("Product", "GoreeCloud Launcher")
                SettingsReadOnlyRow("Build channel", "Development")
                SettingsReadOnlyRow("License", "GPL-3.0-only")
                SettingsReadOnlyRow("Privacy", "GoreeCloud Privacy Shield")
                SettingsReadOnlyRow("Security", "Wardveil Security by GoreeCloud")
                Text(
                    "Development status does not imply Release Candidate, Production, Stable, Seal or Anchor acceptance.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    gestureToConfigure?.let { gesture ->
        val currentAction = when (gesture) {
            LauncherHomeGesture.SWIPE_UP -> experiencePreferences.swipeUpAction
            LauncherHomeGesture.SWIPE_DOWN -> experiencePreferences.swipeDownAction
            LauncherHomeGesture.SWIPE_LEFT -> experiencePreferences.swipeLeftAction
            LauncherHomeGesture.SWIPE_RIGHT -> experiencePreferences.swipeRightAction
            LauncherHomeGesture.DOUBLE_TAP -> experiencePreferences.doubleTapAction
            LauncherHomeGesture.TAP_AND_HOLD -> experiencePreferences.tapAndHoldAction
        }
        GestureActionPickerDialog(
            gesture = gesture,
            currentAction = currentAction,
            apps = apps,
            onSelect = { action ->
                onSetGestureAction(gesture, action)
                gestureToConfigure = null
            },
            onDismiss = { gestureToConfigure = null },
        )
    }
}

private fun gestureActionLabel(
    action: LauncherGestureAction,
    appsByKey: Map<String, LauncherActivityInfo>,
): String = when (action.type) {
    LauncherGestureActionType.LAUNCHER_SETTINGS -> "Home editor"
    LauncherGestureActionType.OPEN_APP ->
        action.appKey
            ?.let(appsByKey::get)
            ?.label
            ?.toString()
            ?.let { "Open $it" }
            ?: "Unavailable app"
    else -> action.type.displayName
}

@Composable
private fun GestureAssignmentRow(
    gesture: LauncherHomeGesture,
    actionLabel: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.54f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GlazeMetrics.space3, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                gesture.displayName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                actionLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun GestureActionPickerDialog(
    gesture: LauncherHomeGesture,
    currentAction: LauncherGestureAction,
    apps: List<LauncherActivityInfo>,
    onSelect: (LauncherGestureAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val builtInActions = remember {
        listOf(
            LauncherGestureActionType.NONE,
            LauncherGestureActionType.APPS,
            LauncherGestureActionType.UNIVERSAL_SEARCH,
            LauncherGestureActionType.HOME_EDITOR,
            LauncherGestureActionType.WALLPAPER,
            LauncherGestureActionType.THEME_MANAGER,
        )
    }
    val sortedApps = remember(apps) {
        apps.sortedWith(
            compareBy<LauncherActivityInfo> { it.label.toString().lowercase(Locale.getDefault()) }
                .thenBy { it.componentName.packageName },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(gesture.displayName) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item {
                    Text(
                        "Launcher actions",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                lazyItems(builtInActions, key = { it.storageValue }) { type ->
                    val selected = currentAction.type == type
                    TextButton(
                        onClick = {
                            onSelect(LauncherGestureAction.builtIn(type))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(type.displayName)
                            if (selected) {
                                Text(
                                    "Selected",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(GlazeMetrics.space2))
                    Text(
                        "Open app",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                lazyItems(
                    items = sortedApps,
                    key = { app -> app.workspaceKey() },
                ) { app ->
                    val appKey = app.workspaceKey()
                    val selected =
                        currentAction.type == LauncherGestureActionType.OPEN_APP &&
                            currentAction.appKey == appKey
                    TextButton(
                        onClick = {
                            onSelect(LauncherGestureAction.openApp(appKey))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(app.label.toString())
                                Text(
                                    app.componentName.packageName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (selected) {
                                Text(
                                    "Selected",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun SettingsReadOnlyRow(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    summary: String,
    visible: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!visible) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(GlazeMetrics.radiusExtraLarge),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(GlazeMetrics.space4),
            verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space3),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
        }
    }
}

@Composable
private fun ChoiceRow(
    choices: List<String>,
    selected: String,
    onChoice: (String) -> Unit,
) {
    // Fixed equal-width rows wrap long settings choices instead of overflowing compact displays.
    Column(verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space2)) {
        val choicesPerRow = if (choices.size == 3) 3 else 2
        choices.chunked(choicesPerRow).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
            ) {
                row.forEach { label ->
                    val isSelected = label == selected
                    Surface(
                        modifier = Modifier.weight(1f),
                        onClick = { onChoice(label) },
                        shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.68f),
                        border = BorderStroke(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                        ),
                    ) {
                        Box(
                            modifier = Modifier.heightIn(min = 48.dp).padding(horizontal = 10.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                label,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.labelLarge,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun IconShapeChoiceGrid(
    selected: LauncherIconShape,
    onSelect: (LauncherIconShape) -> Unit,
) {
    val options = LauncherIconShape.entries
    options.chunked(3).forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
        ) {
            row.forEach { shape ->
                val isSelected = selected == shape
                Surface(
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(shape) },
                    shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.44f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.56f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.88f),
                                    shape.toLauncherComposeShape(),
                                ),
                        )
                        Text(
                            shape.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
            }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LauncherIconPackPickerSheet(
    packs: List<LauncherIconPackDescriptor>,
    selectedPackage: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = GlazeMetrics.space4, vertical = GlazeMetrics.space3),
            verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space3),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Icon pack",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Choose original app artwork or an installed icon pack. Missing mappings fall back to the original icon.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
            ) {
                item {
                    IconPackPickerRow(
                        title = "Original icons",
                        summary = "Use Android-provided app artwork",
                        selected = selectedPackage == null,
                        onClick = { onSelect(null) },
                    )
                }
                lazyItems(
                    items = packs,
                    key = { it.packageName },
                ) { pack ->
                    IconPackPickerRow(
                        title = pack.label,
                        summary = pack.packageName,
                        selected = selectedPackage == pack.packageName,
                        onClick = { onSelect(pack.packageName) },
                    )
                }
            }

            Text(
                "Compatible packs can expose the GoreeCloud icon-pack action or common Android launcher theme actions and appfilter.xml mappings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(GlazeMetrics.space2))
        }
    }
}

@Composable
private fun IconPackPickerRow(
    title: String,
    summary: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.40f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f)
        },
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.50f)
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GlazeMetrics.space3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space3),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        RoundedCornerShape(13.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    title.take(1).uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (selected) {
                Text(
                    "Selected",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun GlazeSettingsAction(
    title: String,
    summary: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.54f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(GlazeMetrics.space3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space3),
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                value,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun LauncherHeaderGlyphAction(
    contentDescription: String,
    symbol: GlazePopupActionSymbol,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .semantics { this.contentDescription = contentDescription },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.48f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            GlazePopupActionGlyph(
                symbol = symbol,
                color = MaterialTheme.colorScheme.onSurface,
                iconSize = 20.dp,
            )
        }
    }
}

@Composable
internal fun GlazeTextAction(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(GlazeMetrics.radiusPill),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.46f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun GlazeRoundAction(
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(44.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.52f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun LauncherSearchMagnifier(color: Color) {
    Canvas(Modifier.size(20.dp)) {
        val centerPoint = Offset(size.width * 0.42f, size.height * 0.42f)
        drawCircle(
            color = color.copy(alpha = 0.95f),
            radius = size.minDimension * 0.25f,
            center = centerPoint,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = size.minDimension * 0.095f,
            ),
        )
        drawLine(
            color = color.copy(alpha = 0.95f),
            start = Offset(size.width * 0.60f, size.height * 0.60f),
            end = Offset(size.width * 0.88f, size.height * 0.88f),
            strokeWidth = size.minDimension * 0.095f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
    }
}

@Composable
private fun GlazeSearchCapsule(
    value: String,
    style: LauncherHomeSearchStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val presentationContext = LocalGlazeV16PresentationContext.current
    val resolvedPresentation = remember(style, presentationContext) {
        GlazeV16PresentationPolicy.resolve(
            requestedMaterial = launcherHomeSearchMaterialRole(style),
            context = presentationContext,
        )
    }
    val solidResolved = resolvedPresentation.materialRole in setOf(
        GlazeV16MaterialRole.SOLID,
        GlazeV16MaterialRole.RAISED,
    )
    val foreground = MaterialTheme.colorScheme.onSurface
    val background = when (resolvedPresentation.materialRole) {
        GlazeV16MaterialRole.SOLID -> MaterialTheme.colorScheme.surface
        GlazeV16MaterialRole.RAISED -> MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        GlazeV16MaterialRole.CLEAR_GLASS -> MaterialTheme.colorScheme.surface.copy(alpha = 0.56f)
        GlazeV16MaterialRole.FUNCTIONAL_GLASS -> MaterialTheme.colorScheme.surface.copy(alpha = 0.76f)
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.70f)
    }
    val outline = MaterialTheme.colorScheme.onSurface.copy(
        alpha = if (style == LauncherHomeSearchStyle.CLEAR) 0.05f else 0.08f,
    )
    val leadingFill = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    val height = launcherHomeSearchHeightDp(
        style = style,
        largeText = presentationContext.largeText,
        extraLargeText = presentationContext.extraLargeText,
    ).dp
    val shadowElevation = when (resolvedPresentation.materialRole) {
        GlazeV16MaterialRole.CLEAR_GLASS -> 0.dp
        GlazeV16MaterialRole.SOLID -> 1.dp
        else -> 3.dp
    }

    Surface(
        modifier = modifier
            .heightIn(min = resolvedPresentation.minimumInteractionTarget)
            .testTag("launcher-home-search-capsule"),
        onClick = onClick,
        shape = RoundedCornerShape(GlazeMetrics.radiusPill),
        color = background,
        border = BorderStroke(1.dp, outline),
        shadowElevation = shadowElevation,
    ) {
        Row(
            modifier = Modifier
                .height(height)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier.size(
                    if (style == LauncherHomeSearchStyle.CLEAR) 32.dp else 36.dp,
                ),
                shape = CircleShape,
                color = leadingFill,
                border = if (
                    resolvedPresentation.materialRole == GlazeV16MaterialRole.CLEAR_GLASS
                ) {
                    null
                } else {
                    BorderStroke(1.dp, outline.copy(alpha = 0.72f))
                },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    LauncherSearchMagnifier(foreground)
                }
            }
            Text(
                value,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = foreground.copy(alpha = if (solidResolved) 0.86f else 0.90f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Surface(
                modifier = Modifier.size(34.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val overflowTint = MaterialTheme.colorScheme.primary
                    Canvas(Modifier.size(18.dp)) {
                        val radius = size.minDimension * 0.075f
                        listOf(0.25f, 0.50f, 0.75f).forEach { x ->
                            drawCircle(
                                color = overflowTint,
                                radius = radius,
                                center = Offset(size.width * x, size.height * 0.50f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun GlazeAppSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    darkSurface: Boolean = false,
    requestFocus: Boolean = false,
    placeholder: String = "Search apps",
    inputTestTag: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val presentationContext = LocalGlazeV16PresentationContext.current
    val resolvedPresentation = remember(presentationContext) {
        GlazeV16PresentationPolicy.resolve(
            requestedMaterial = GlazeV16MaterialRole.SOLID,
            context = presentationContext,
        )
    }
    var focused by remember { mutableStateOf(false) }
    val requestedHeight = launcherSearchFieldHeightDp(
        largeText = presentationContext.largeText,
        extraLargeText = presentationContext.extraLargeText,
    ).dp
    val fieldHeight = if (
        requestedHeight < resolvedPresentation.minimumInteractionTarget
    ) {
        resolvedPresentation.minimumInteractionTarget
    } else {
        requestedHeight
    }
    val focusEmphasis = focused && resolvedPresentation.strongVisibleFocusRequired

    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Surface(
        modifier = modifier.heightIn(min = resolvedPresentation.minimumInteractionTarget),
        shape = RoundedCornerShape(GlazeMetrics.radiusExtraLarge),
        color = if (darkSurface) Color.White.copy(alpha = 0.10f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
        border = BorderStroke(
            if (focusEmphasis) resolvedPresentation.focusRingWidth else 1.dp,
            when {
                focusEmphasis && darkSurface -> Color.White.copy(alpha = 0.82f)
                focusEmphasis -> MaterialTheme.colorScheme.primary
                darkSurface -> Color.White.copy(alpha = 0.12f)
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            },
        ),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = if (darkSurface) Color.White else MaterialTheme.colorScheme.onSurface,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(fieldHeight)
                .focusRequester(focusRequester)
                .onFocusChanged { focused = it.isFocused }
                .then(
                    inputTestTag?.let { tag -> Modifier.testTag(tag) } ?: Modifier,
                )
                .padding(horizontal = GlazeMetrics.space4),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                ) {
                    LauncherSearchMagnifier(
                        if (darkSurface) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Box(Modifier.weight(1f)) {
                        if (value.isBlank()) {
                            Text(
                                placeholder,
                                color = if (darkSurface) Color.White.copy(alpha = 0.62f)
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        innerTextField()
                    }
                    trailingContent?.invoke()
                }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun GlazeDock(
    apps: List<LauncherActivityInfo>,
    iconScale: Float,
    style: LauncherDockStyle,
    pageSize: Int,
    loopPages: Boolean,
    showLabels: Boolean,
    showSearch: Boolean,
    layoutLocked: Boolean,
    editMode: Boolean,
    activeDrag: LauncherAppDragData?,
    dragPoint: Offset?,
    onDockBoundsChanged: (Rect) -> Unit,
    dockItemBounds: MutableMap<String, Rect>,
    onBeginLocalDrag: (LauncherAppDragData, Offset) -> Unit,
    onUpdateLocalDrag: (Offset) -> Unit,
    onEndLocalDrag: (LauncherAppDragData, Offset) -> Unit,
    onCancelLocalDrag: () -> Unit,
    onLaunchApp: (LauncherActivityInfo) -> Unit,
    onManageApp: (LauncherActivityInfo, Rect?) -> Unit,
    onOpenSearch: () -> Unit,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
) {
    val shape = if (style == LauncherDockStyle.EDGE) {
        RoundedCornerShape(
            topStart = 30.dp,
            topEnd = 30.dp,
            bottomStart = 12.dp,
            bottomEnd = 12.dp,
        )
    } else {
        RoundedCornerShape(GlazeMetrics.opticalHero)
    }
    val presentationContext = LocalGlazeV16PresentationContext.current
    val resolvedPresentation = remember(style, presentationContext) {
        GlazeV16PresentationPolicy.resolve(
            requestedMaterial = launcherDockMaterialRole(style),
            context = presentationContext,
        )
    }
    val solidResolved = resolvedPresentation.materialRole in setOf(
        GlazeV16MaterialRole.SOLID,
        GlazeV16MaterialRole.RAISED,
    )
    val color = when (resolvedPresentation.materialRole) {
        GlazeV16MaterialRole.SOLID -> MaterialTheme.colorScheme.surface
        GlazeV16MaterialRole.RAISED -> MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)
        GlazeV16MaterialRole.CLEAR_GLASS -> MaterialTheme.colorScheme.surface.copy(alpha = 0.44f)
        GlazeV16MaterialRole.FUNCTIONAL_GLASS -> when (style) {
            LauncherDockStyle.EDGE -> MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
            else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.64f)
        }
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.60f)
    }
    val dockForeground = MaterialTheme.colorScheme.onSurface
    var measuredBounds by remember { mutableStateOf<Rect?>(null) }
    var pagerBounds by remember { mutableStateOf<Rect?>(null) }
    val dockHovered = activeDrag != null &&
        dragPoint?.let { point -> measuredBounds?.contains(point) } == true
    // Clear may float directly on wallpaper in the normal case, but reduced-transparency or
    // other accessibility policy can resolve it to a solid/raised material. Honor that fallback.
    val dockHasSurface = style != LauncherDockStyle.CLEAR || solidResolved
    val border = when {
        dockHovered -> BorderStroke(
            2.dp,
            if (solidResolved) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.70f)
            } else {
                Color.White.copy(alpha = 0.58f)
            },
        )
        style == LauncherDockStyle.CLEAR -> null
        resolvedPresentation.materialRole == GlazeV16MaterialRole.CLEAR_GLASS -> null
        solidResolved -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        else -> BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.onSurface.copy(
                alpha = if (style == LauncherDockStyle.EDGE) 0.10f else 0.07f,
            ),
        )
    }

    val compactWidthFraction = launcherDockWidthFraction(
        appCount = apps.size,
        showSearch = showSearch,
    )

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(compactWidthFraction)
                .widthIn(max = 560.dp)
                .testTag("launcher-home-dock")
                .onGloballyPositioned {
                    val bounds = it.boundsInRoot()
                    measuredBounds = bounds
                    onDockBoundsChanged(bounds)
                },
            shape = shape,
            color = if (dockHasSurface || dockHovered) color else Color.Transparent,
            border = if (dockHasSurface || dockHovered) border else null,
            shadowElevation = when {
                style == LauncherDockStyle.RAISED -> 3.dp
                style == LauncherDockStyle.SOLID -> 1.dp
                dockHovered && resolvedPresentation.materialRole == GlazeV16MaterialRole.RAISED -> 2.dp
                else -> 0.dp
            },
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        when {
                            showLabels -> 96.dp
                            style == LauncherDockStyle.EDGE -> 80.dp
                            else -> 72.dp
                        },
                    ),
            ) {
                val minimumSlot = resolvedPresentation.minimumInteractionTarget.coerceAtLeast(48.dp)
            val preferredSlot = if (minimumSlot > 60.dp) minimumSlot else 60.dp
            val horizontalPadding = GlazeMetrics.space3
            val searchReservation = if (showSearch) minimumSlot + GlazeMetrics.space1 else 0.dp
            val availableAppWidth = (
                maxWidth - horizontalPadding * 2 - searchReservation
            ).coerceAtLeast(minimumSlot)
            val pagePlan = launcherDockPagePlan(
                itemCount = apps.size,
                configuredPageSize = pageSize,
                availableAppWidthDp = availableAppWidth.value,
                minimumInteractionTargetDp = minimumSlot.value,
            )
            val dockPages = remember(apps, pagePlan.effectivePageSize) {
                if (apps.isEmpty()) {
                    listOf(emptyList<LauncherActivityInfo>())
                } else {
                    apps.chunked(pagePlan.effectivePageSize)
                }
            }
            val loopingDockPages = loopPages && dockPages.size > 1
            val pagerPageCount = launcherDockVirtualPageCount(
                logicalPageCount = dockPages.size,
                loop = loopingDockPages,
            )
            val pagerState = rememberPagerState(
                initialPage = launcherDockInitialVirtualPage(
                    logicalPageCount = dockPages.size,
                    loop = loopingDockPages,
                ),
                pageCount = { pagerPageCount },
            )
            LaunchedEffect(loopingDockPages, dockPages.size) {
                pagerState.scrollToPage(
                    launcherDockInitialVirtualPage(
                        logicalPageCount = dockPages.size,
                        loop = loopingDockPages,
                    ),
                )
            }
            LaunchedEffect(
                pagerState.currentPage,
                pagerState.isScrollInProgress,
                loopingDockPages,
                dockPages.size,
            ) {
                if (!pagerState.isScrollInProgress) {
                    launcherDockLoopBoundaryTarget(
                        virtualPage = pagerState.currentPage,
                        logicalPageCount = dockPages.size,
                        loop = loopingDockPages,
                    )?.let { pagerState.scrollToPage(it) }
                }
            }
            val logicalCurrentPage = launcherDockLogicalPage(
                virtualPage = pagerState.currentPage,
                logicalPageCount = dockPages.size,
                loop = loopingDockPages,
            )
            val visibleApps = dockPages.getOrElse(logicalCurrentPage) { emptyList() }
            val visibleKeys = remember(visibleApps) { visibleApps.map { it.workspaceKey() }.toSet() }
            val dockPageKeys = remember(dockPages) { dockPages.map { page -> page.map { it.workspaceKey() } } }
            val nextPageInsertionKey = if (activeDrag == null) {
                null
            } else {
                launcherDockNextPageInsertionKey(
                    dockPageKeys,
                    logicalCurrentPage,
                    activeDrag.appKey,
                    loop = loopingDockPages,
                )
            }
            LaunchedEffect(visibleKeys, nextPageInsertionKey, measuredBounds) {
                val retainedKeys = visibleKeys + listOfNotNull(nextPageInsertionKey)
                dockItemBounds.keys
                    .filterNot(retainedKeys::contains)
                    .toList()
                    .forEach(dockItemBounds::remove)
                val dock = measuredBounds
                if (nextPageInsertionKey != null && dock != null) {
                    dockItemBounds[nextPageInsertionKey] = Rect(
                        dock.right + 1f,
                        dock.top,
                        dock.right + 2f,
                        dock.bottom,
                    )
                }
            }

            val slotSize = (
                availableAppWidth / pagePlan.effectivePageSize.toFloat()
            ).coerceIn(minimumSlot, preferredSlot)
            val adaptiveIconScale = (
                iconScale * (slotSize.value / preferredSlot.value)
            ).coerceIn(0.85f, 1.15f)
            val dockPagerScope = rememberCoroutineScope()
            val previousDockPageAvailable = loopingDockPages || logicalCurrentPage > 0
            val nextDockPageAvailable =
                loopingDockPages || logicalCurrentPage < dockPages.lastIndex
            val dragPageDirection = LauncherDockDragPageHandoff(activeDrag, dragPoint, pagerBounds, pagerState, previousDockPageAvailable, nextDockPageAvailable)

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .onGloballyPositioned { pagerBounds = it.boundsInRoot() }
                        .semantics {
                            stateDescription =
                                "Dock page ${logicalCurrentPage + 1} of ${dockPages.size}"
                            customActions = buildList {
                                if (previousDockPageAvailable) {
                                    add(
                                        CustomAccessibilityAction("Previous Dock page") {
                                            dockPagerScope.launch {
                                                pagerState.animateScrollToPage(
                                                    (pagerState.currentPage - 1).coerceAtLeast(0),
                                                )
                                            }
                                            true
                                        },
                                    )
                                }
                                if (nextDockPageAvailable) {
                                    add(
                                        CustomAccessibilityAction("Next Dock page") {
                                            dockPagerScope.launch {
                                                pagerState.animateScrollToPage(
                                                    (pagerState.currentPage + 1).coerceAtMost(
                                                        pagerPageCount - 1,
                                                    ),
                                                )
                                            }
                                            true
                                        },
                                    )
                                }
                            }
                        },
                    userScrollEnabled = activeDrag == null && dockPages.size > 1,
                ) { pageIndex ->
                    val pageApps = dockPages[
                        launcherDockLogicalPage(
                            virtualPage = pageIndex,
                            logicalPageCount = dockPages.size,
                            loop = loopingDockPages,
                        )
                    ]
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (pageApps.isEmpty() && activeDrag != null) {
                            Text(
                                "Drop in Dock",
                                style = MaterialTheme.typography.labelMedium,
                                color = dockForeground.copy(alpha = 0.76f),
                            )
                        } else {
                            pageApps.forEach { app ->
                                val appKey = app.workspaceKey()
                                val tileHeight = if (showLabels) slotSize + 22.dp else slotSize
                                Box(
                                    modifier = Modifier
                                        .width(slotSize)
                                        .height(tileHeight)
                                        .onGloballyPositioned {
                                            if (pageIndex == pagerState.currentPage) {
                                                dockItemBounds[appKey] = it.boundsInRoot()
                                            }
                                        },
                                ) {
                                    HomeFavoriteTile(
                                        app = app,
                                        displayLabel = app.label.toString(),
                                        iconScale = adaptiveIconScale,
                                        showLabel = showLabels,
                                        layoutLocked = layoutLocked,
                                        editMode = editMode,
                                        dragData = if (layoutLocked) null else LauncherAppDragData(
                                            appKey = appKey,
                                            origin = LauncherAppDragOrigin.DOCK,
                                        ),
                                        onBeginLocalDrag = onBeginLocalDrag,
                                        onUpdateLocalDrag = onUpdateLocalDrag,
                                        onEndLocalDrag = onEndLocalDrag,
                                        onCancelLocalDrag = onCancelLocalDrag,
                                        onLaunchApp = onLaunchApp,
                                        onManageApp = onManageApp,
                                        onSwipeUp = onSwipeUp,
                                        onSwipeDown = onSwipeDown,
                                        modifier = Modifier.fillMaxSize(),
                                        labelColor = dockForeground,
                                    )
                                }
                            }
                        }
                    }
                }

                if (showSearch) {
                    Spacer(Modifier.width(GlazeMetrics.space1))
                    Surface(
                        modifier = Modifier
                            .size(minimumSlot)
                            .testTag("launcher-dock-search")
                            .semantics { contentDescription = "Universal Search" }
                            .clickable(onClick = onOpenSearch),
                        shape = CircleShape,
                        color = if (style == LauncherDockStyle.CLEAR && !solidResolved) {
                            Color.Transparent
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.30f)
                        },
                        border = if (style == LauncherDockStyle.CLEAR && !solidResolved) null else BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        ),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            LauncherSearchMagnifier(dockForeground)
                        }
                    }
                }
            }

            if (activeDrag != null && dragPageDirection != null) {
                val previous = dragPageDirection == LauncherDockDragPageDirection.PREVIOUS
                Box(
                    modifier = Modifier
                        .align(if (previous) Alignment.CenterStart else Alignment.CenterEnd)
                        .padding(
                            start = if (previous) horizontalPadding else 0.dp,
                            end = if (previous) 0.dp else horizontalPadding + searchReservation,
                        )
                        .width(5.dp)
                        .height(34.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                            CircleShape,
                        )
                        .testTag("launcher-dock-drag-page-edge"),
                )
            }

            if (dockPages.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 3.dp)
                        .testTag("launcher-dock-page-indicator"),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(dockPages.size) { index ->
                        val selected = index == logicalCurrentPage
                        Box(
                            modifier = Modifier
                                .size(
                                    width = if (selected) 12.dp else 5.dp,
                                    height = 5.dp,
                                )
                                .background(
                                    dockForeground.copy(alpha = if (selected) 0.82f else 0.34f),
                                    CircleShape,
                                ),
                        )
                    }
                }
            }
        }
    }
}
}

@Suppress("DEPRECATION")
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LauncherAppTile(
    app: LauncherActivityInfo,
    iconScale: Float,
    showLabel: Boolean,
    compact: Boolean,
    fixedGridGeometry: Boolean = false,
    pinnedInDrawer: Boolean = false,
    lockedByLauncher: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (Rect?) -> Unit,
    modifier: Modifier,
    dragData: LauncherAppDragData? = null,
    onBoundsChanged: ((Rect) -> Unit)? = null,
    onSwipeUp: (() -> Unit)? = null,
    onSwipeDown: (() -> Unit)? = null,
    labelOnWallpaper: Boolean = false,
) {
    val icon = rememberLauncherAppIcon(app)
    val base = if (compact) 50f else 52f
    val iconSize = (base * iconScale.coerceIn(0.85f, 1.15f)).dp
    val useFixedGridGeometry = fixedGridGeometry && showLabel
    val swipeThreshold = with(LocalDensity.current) { 42.dp.toPx() }
    var tileBounds by remember(app.componentName, app.user) { mutableStateOf<Rect?>(null) }
    val dragModifier = if (dragData != null) {
        Modifier.dragAndDropSource(block = {
            var transferStarted = false
            detectDragGesturesAfterLongPress(
                onDragStart = { transferStarted = false },
                onDrag = { change, _ ->
                    change.consume()
                    if (!transferStarted) {
                        transferStarted = true
                        startTransfer(dragData.toTransferData())
                    }
                },
                onDragEnd = {
                    if (!transferStarted) onLongClick(tileBounds)
                },
                onDragCancel = { transferStarted = false },
            )
        })
    } else {
        Modifier
    }
    val gestureModifier = if (onSwipeUp != null || onSwipeDown != null) {
        Modifier.pointerInput(onSwipeUp, onSwipeDown, swipeThreshold) {
            var drag = 0f
            var triggered = false
            detectVerticalDragGestures(
                onDragStart = {
                    drag = 0f
                    triggered = false
                },
                onDragCancel = {
                    drag = 0f
                    triggered = false
                },
                onDragEnd = {
                    drag = 0f
                    triggered = false
                },
                onVerticalDrag = { change, amount ->
                    change.consume()
                    if (!triggered) {
                        drag += amount
                        when {
                            drag <= -swipeThreshold -> {
                                triggered = true
                                onSwipeUp?.invoke()
                            }
                            drag >= swipeThreshold -> {
                                triggered = true
                                onSwipeDown?.invoke()
                            }
                        }
                    }
                },
            )
        }
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .then(dragModifier)
            .then(gestureModifier)
            .onGloballyPositioned {
                tileBounds = it.boundsInRoot()
                onBoundsChanged?.invoke(tileBounds!!)
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = { onLongClick(tileBounds) },
            )
            .semantics {
                when {
                    pinnedInDrawer && lockedByLauncher ->
                        stateDescription = "Pinned in Apps. App Lock enabled"
                    pinnedInDrawer ->
                        stateDescription = "Pinned in Apps"
                    lockedByLauncher ->
                        stateDescription = "App Lock enabled"
                }
            }
            .padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (useFixedGridGeometry) Arrangement.Top else Arrangement.Center,
    ) {
        Box(modifier = Modifier.size(iconSize)) {
            if (icon != null) {
                Image(
                    bitmap = icon,
                    contentDescription = app.label.toString(),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().launcherIconMask(),
                )
            } else {
                Surface(
                    modifier = Modifier.fillMaxSize().launcherIconMask(),
                    shape = RoundedCornerShape(15.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(app.label.toString().take(1).uppercase(), fontWeight = FontWeight.Bold)
                    }
                }
            }
            LauncherAppBadgeMark(
                app,
                modifier = launcherBadgePositionModifier(),
            )
            if (pinnedInDrawer) {
                DrawerPinnedMark(
                    modifier = Modifier.align(Alignment.TopStart),
                )
            }
            if (lockedByLauncher) {
                DrawerLockedMark(
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }

        if (showLabel) {
            Spacer(Modifier.height(if (compact) 3.dp else 4.dp))
            Text(
                app.label.toString(),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (useFixedGridGeometry) {
                            Modifier.height(
                                if (compact) {
                                    LauncherDrawerGridPolicy.COMPACT_LABEL_SLOT_HEIGHT_DP.dp
                                } else {
                                    LauncherDrawerGridPolicy.GRID_LABEL_SLOT_HEIGHT_DP.dp
                                },
                            )
                        } else {
                            Modifier
                        },
                    ),
                style = if (labelOnWallpaper) {
                    MaterialTheme.typography.labelSmall.copy(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.60f),
                            offset = Offset(0f, 1.5f),
                            blurRadius = 5f,
                        ),
                    )
                } else {
                    MaterialTheme.typography.labelSmall
                },
                color = if (labelOnWallpaper) Color.White else Color.Unspecified,
                textAlign = TextAlign.Center,
                maxLines = if (compact || fixedGridGeometry) 1 else 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DrawerPinnedMark(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .size(18.dp)
            .semantics { contentDescription = "Pinned in Apps" },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.42f),
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            GlazePopupActionGlyph(
                symbol = GlazePopupActionSymbol.PIN,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                iconSize = 11.dp,
            )
        }
    }
}

@Composable
private fun DrawerLockedMark(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .width(20.dp)
            .height(16.dp)
            .semantics { contentDescription = "App Lock enabled" },
        shape = RoundedCornerShape(GlazeMetrics.radiusPill),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.42f),
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            GlazePopupActionGlyph(
                symbol = GlazePopupActionSymbol.LOCK,
                color = MaterialTheme.colorScheme.primary,
                iconSize = 10.dp,
            )
        }
    }
}

@Suppress("DEPRECATION")
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LauncherAppListRow(
    app: LauncherActivityInfo,
    iconScale: Float,
    pinnedInDrawer: Boolean = false,
    lockedByLauncher: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (Rect?) -> Unit,
    dragData: LauncherAppDragData? = null,
) {
    val icon = rememberLauncherAppIcon(app)
    val iconSize = (44f * iconScale.coerceIn(0.85f, 1.15f)).dp
    var rowBounds by remember(app.componentName, app.user) { mutableStateOf<Rect?>(null) }
    val dragModifier = if (dragData != null) {
        Modifier.dragAndDropSource(block = {
            var transferStarted = false
            detectDragGesturesAfterLongPress(
                onDragStart = { transferStarted = false },
                onDrag = { change, _ ->
                    change.consume()
                    if (!transferStarted) {
                        transferStarted = true
                        startTransfer(dragData.toTransferData())
                    }
                },
                onDragEnd = {
                    if (!transferStarted) onLongClick(rowBounds)
                },
                onDragCancel = { transferStarted = false },
            )
        })
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(dragModifier)
            .onGloballyPositioned { rowBounds = it.boundsInRoot() }
            .combinedClickable(
                onClick = onClick,
                onLongClick = { onLongClick(rowBounds) },
            )
            .semantics {
                when {
                    pinnedInDrawer && lockedByLauncher ->
                        stateDescription = "Pinned in Apps. App Lock enabled"
                    pinnedInDrawer ->
                        stateDescription = "Pinned in Apps"
                    lockedByLauncher ->
                        stateDescription = "App Lock enabled"
                }
            }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(modifier = Modifier.size(iconSize)) {
            if (icon != null) {
                Image(
                    bitmap = icon,
                    contentDescription = app.label.toString(),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().launcherIconMask(),
                )
            } else {
                Surface(
                    modifier = Modifier.fillMaxSize().launcherIconMask(),
                    shape = RoundedCornerShape(13.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(app.label.toString().take(1).uppercase(), fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (pinnedInDrawer) {
                DrawerPinnedMark(
                    modifier = Modifier.align(Alignment.TopStart),
                )
            }
            if (lockedByLauncher) {
                DrawerLockedMark(
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                app.label.toString(),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                app.componentName.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LauncherAppLockManagerSheet(
    apps: List<LauncherActivityInfo>,
    lockedAppKeys: Set<String>,
    onSetLocked: (LauncherActivityInfo, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var showLockedOnly by rememberSaveable { mutableStateOf(false) }
    val visibleApps = remember(apps, lockedAppKeys, query, showLockedOnly) {
        apps.filter { app ->
            (!showLockedOnly || app.workspaceKey() in lockedAppKeys) &&
                (
                    query.isBlank() ||
                        LauncherLocalAppSearch.matches(
                            label = app.label.toString(),
                            packageName = app.componentName.packageName,
                            rawQuery = query,
                        )
                )
        }
    }
    val lockedCount = lockedAppKeys.count { key -> apps.any { it.workspaceKey() == key } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("launcher-app-lock-manager"),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = GlazeMetrics.space4, vertical = GlazeMetrics.space3),
            verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space3),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "App Lock",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "$lockedCount locked · " + apps.size + " available",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("launcher-app-lock-close")
                        .semantics { contentDescription = "Close App Lock" },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        GlazePopupActionGlyph(
                            symbol = GlazePopupActionSymbol.CLOSE,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            iconSize = 18.dp,
                        )
                    }
                }
            }

            GlazeAppSearchField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("launcher-app-lock-search"),
                placeholder = "Search apps",
                inputTestTag = "launcher-app-lock-search-input",
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
            ) {
                listOf(false to "All", true to "Locked").forEach { (lockedOnly, label) ->
                    val selected = showLockedOnly == lockedOnly
                    Surface(
                        onClick = { showLockedOnly = lockedOnly },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag(
                                if (lockedOnly) "launcher-app-lock-filter-locked"
                                else "launcher-app-lock-filter-all",
                            ),
                        shape = RoundedCornerShape(GlazeMetrics.radiusPill),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.68f)
                        } else {
                            Color.Transparent
                        },
                        border = BorderStroke(
                            1.dp,
                            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.46f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.68f),
                        ),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 11.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                label + " · " + if (lockedOnly) lockedCount else apps.size,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(GlazeMetrics.radiusMedium),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
            ) {
                Text(
                    "Launcher-only boundary: direct launches from notifications, Android Settings, " +
                        "deep links, other launchers, or another app are not intercepted.",
                    modifier = Modifier.padding(GlazeMetrics.space3),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space1),
            ) {
                if (visibleApps.isEmpty()) {
                    item(key = "app-lock-empty") {
                        Text(
                            if (showLockedOnly && query.isBlank()) {
                                "No apps are locked."
                            } else {
                                "No apps match this search."
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = GlazeMetrics.space4),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                lazyItems(
                    items = visibleApps,
                    key = { app -> app.workspaceKey() },
                ) { app ->
                    val appKey = app.workspaceKey()
                    val locked = appKey in lockedAppKeys
                    val icon = rememberLauncherAppIcon(app)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("launcher-app-lock-" + appKey),
                        shape = RoundedCornerShape(GlazeMetrics.radiusMedium),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 60.dp)
                                .padding(horizontal = GlazeMetrics.space3, vertical = GlazeMetrics.space1),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space3),
                        ) {
                            if (icon != null) {
                                Image(
                                    bitmap = icon,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(40.dp).launcherIconMask(),
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    app.label.toString(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    (if (app.user == Process.myUserHandle()) "Personal" else "Work") +
                                        " · " + app.componentName.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = locked,
                                onCheckedChange = { onSetLocked(app, it) },
                                modifier = Modifier.testTag("launcher-app-lock-toggle-" + appKey),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LauncherHiddenAppsManagerSheet(
    hiddenApps: List<LauncherActivityInfo>,
    onRestore: (LauncherActivityInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("launcher-hidden-apps-manager"),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = GlazeMetrics.space4, vertical = GlazeMetrics.space3),
            verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space3),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Hidden apps",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Hidden apps stay installed. Existing Home, Dock and folder placements are unchanged.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text("Done") }
            }

            if (hiddenApps.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                ) {
                    Text(
                        "No hidden apps.",
                        modifier = Modifier.padding(GlazeMetrics.space3),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                ) {
                    lazyItems(
                        items = hiddenApps,
                        key = { app -> app.workspaceKey() },
                    ) { app ->
                        val icon = rememberLauncherAppIcon(app)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("launcher-hidden-app-" + app.workspaceKey()),
                            shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f),
                            ),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(GlazeMetrics.space3),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space3),
                            ) {
                                if (icon != null) {
                                    Image(
                                        bitmap = icon,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(42.dp).launcherIconMask(),
                                    )
                                } else {
                                    Surface(
                                        modifier = Modifier.size(42.dp),
                                        shape = RoundedCornerShape(13.dp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                    ) {}
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        app.label.toString(),
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        (if (app.user == Process.myUserHandle()) "User app" else "Work app") +
                                            " · " + app.componentName.packageName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                TextButton(
                                    onClick = { onRestore(app) },
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("launcher-show-hidden-app-" + app.workspaceKey()),
                                ) {
                                    Text("Show")
                                }
                            }
                        }
                    }
                }
            }

            Text(
                "Hidden apps are excluded from App Drawer and Universal Search discovery only. " +
                    "They can still remain on Home, in the Dock, or inside folders until you remove those placements.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(GlazeMetrics.space2))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LauncherFolderManagerSheet(
    folders: List<LauncherFolder>,
    appsByKey: Map<String, LauncherActivityInfo>,
    homeFolderIds: Set<String>,
    defaultAddToHome: Boolean,
    allowHomePlacement: Boolean,
    onCreate: (String, Boolean) -> Unit,
    onOpen: (LauncherFolder) -> Unit,
    onAddToHome: (LauncherFolder) -> Unit,
    onRemoveFromHome: (LauncherFolder) -> Unit,
    onDismiss: () -> Unit,
) {
    var nameDraft by rememberSaveable { mutableStateOf("") }
    var addToHome by rememberSaveable(defaultAddToHome, allowHomePlacement) {
        mutableStateOf(defaultAddToHome && allowHomePlacement)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = GlazeMetrics.space4, vertical = GlazeMetrics.space2),
            verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space3),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
            ) {
                Surface(
                    modifier = Modifier.size(53.dp),
                    shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        GlazePopupActionGlyph(
                            symbol = GlazePopupActionSymbol.FOLDER,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "New folder",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Choose a name; then add your apps.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Close")
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(GlazeMetrics.radiusExtraLarge),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    modifier = Modifier.padding(GlazeMetrics.space3),
                    verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                ) {
                    OutlinedTextField(
                        value = nameDraft,
                        onValueChange = { nameDraft = it.take(40) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Folder name") },
                        placeholder = { Text("e.g. Banking, Work or Media") },
                        leadingIcon = {
                            GlazePopupActionGlyph(
                                symbol = GlazePopupActionSymbol.FOLDER,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                    if (allowHomePlacement) {
                        Row(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Place on Home",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    "You can drag it later; it always remains in the app drawer.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(checked = addToHome, onCheckedChange = { addToHome = it })
                        }
                    } else {
                        Text(
                            "This folder stays in its Android profile's App Drawer.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(
                        onClick = {
                            nameDraft.trim().takeIf(String::isNotBlank)?.let { name ->
                                onCreate(name, addToHome)
                                nameDraft = ""
                            }
                        },
                        enabled = nameDraft.trim().isNotBlank(),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
                    ) { Text("Create folder") }
                }
            }

            if (folders.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    "Your folders",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 230.dp),
                    verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                ) {
                    lazyItems(
                        items = folders,
                        key = { it.id },
                    ) { folder ->
                        val availableCount = folder.appKeys.count(appsByKey::containsKey)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onOpen(folder) },
                            shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f),
                            ),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(GlazeMetrics.space3),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                            ) {
                                Surface(
                                    modifier = Modifier.size(42.dp),
                                    shape = RoundedCornerShape(13.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        GlazePopupActionGlyph(
                                            symbol = GlazePopupActionSymbol.FOLDER,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        folder.name,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        "$availableCount apps",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (allowHomePlacement) {
                                    TextButton(
                                        onClick = {
                                            if (folder.id in homeFolderIds) {
                                                onRemoveFromHome(folder)
                                            } else {
                                                onAddToHome(folder)
                                            }
                                        },
                                    ) {
                                        Text(
                                            if (folder.id in homeFolderIds) {
                                                "Remove Home"
                                            } else {
                                                "Add Home"
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(GlazeMetrics.space2))
        }
    }
}

/** Glaze floating folder: a spacious wallpaper-layered app surface with compact management chrome. */
@Composable
internal fun LauncherFolderContentsSheet(
    folder: LauncherFolder,
    appsByKey: Map<String, LauncherActivityInfo>,
    isOnHome: Boolean,
    allowHomePlacement: Boolean = true,
    onLaunchApp: (LauncherActivityInfo) -> Unit,
    onRemoveApp: (LauncherActivityInfo) -> Unit,
    onAddApps: () -> Unit,
    onRename: (String) -> Unit,
    onAddToHome: () -> Unit,
    onRemoveFromHome: () -> Unit,
    moveTargets: List<WorkspaceRenderedHomePage> = emptyList(),
    onMoveToPage: (String) -> Unit = {},
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var nameDraft by remember(folder.id, folder.name) { mutableStateOf(folder.name) }
    var editName by remember(folder.id) { mutableStateOf(false) }
    var alphabetical by remember(folder.id) { mutableStateOf(false) }
    var selectMode by remember(folder.id) { mutableStateOf(false) }
    var selectedKeys by remember(folder.id) { mutableStateOf(setOf<String>()) }
    var showActions by remember(folder.id) { mutableStateOf(false) }
    var confirmRemove by remember(folder.id) { mutableStateOf(false) }
    var showDeleteConfirmation by remember(folder.id) { mutableStateOf(false) }
    var showMoveTargets by remember(folder.id) { mutableStateOf(false) }
    var panelVisible by remember(folder.id) { mutableStateOf(false) }
    var dismissing by remember(folder.id) { mutableStateOf(false) }

    val presentationContext = LocalGlazeV16PresentationContext.current
    val resolvedPresentation = remember(presentationContext) {
        GlazeV16PresentationPolicy.resolve(
            requestedMaterial = GlazeV16MaterialRole.FUNCTIONAL_GLASS,
            context = presentationContext,
        )
    }
    val enterMillis = when (resolvedPresentation.motionMode) {
        com.goreecloud.launcher.ui.theme.GlazeV16MotionMode.STANDARD -> 170
        com.goreecloud.launcher.ui.theme.GlazeV16MotionMode.REDUCED -> 105
        com.goreecloud.launcher.ui.theme.GlazeV16MotionMode.MINIMAL -> 60
    }
    val exitMillis = when (resolvedPresentation.motionMode) {
        com.goreecloud.launcher.ui.theme.GlazeV16MotionMode.STANDARD -> 125
        com.goreecloud.launcher.ui.theme.GlazeV16MotionMode.REDUCED -> 85
        com.goreecloud.launcher.ui.theme.GlazeV16MotionMode.MINIMAL -> 45
    }
    val scope = rememberCoroutineScope()
    val members = remember(folder, appsByKey, alphabetical) {
        folder.appKeys.mapNotNull(appsByKey::get).let { apps ->
            if (alphabetical) apps.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) {
                it.label.toString()
            }) else apps
        }
    }
    val panelColor = when (resolvedPresentation.materialRole) {
        GlazeV16MaterialRole.SOLID ->
            MaterialTheme.colorScheme.surface
        GlazeV16MaterialRole.RAISED ->
            MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
        else ->
            MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    }
    val panelBorder = when (resolvedPresentation.materialRole) {
        GlazeV16MaterialRole.SOLID,
        GlazeV16MaterialRole.RAISED ->
            MaterialTheme.colorScheme.outlineVariant
        else ->
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    }
    val folderControlTarget = resolvedPresentation.minimumInteractionTarget
    val folderCellMinHeight = launcherFolderCellMinHeightDp(
        largeText = presentationContext.largeText,
        extraLargeText = presentationContext.extraLargeText,
    ).dp

    LaunchedEffect(folder.id) {
        panelVisible = true
    }

    fun dismissThen(afterDismiss: (() -> Unit)? = null) {
        if (dismissing) return
        dismissing = true
        panelVisible = false
        scope.launch {
            if (exitMillis > 0) delay(exitMillis.toLong())
            onDismiss()
            afterDismiss?.invoke()
        }
    }

    Dialog(
        onDismissRequest = { dismissThen() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            val panelWidth = launcherFolderPanelWidthDp(maxWidth.value).dp
            AnimatedVisibility(
                visible = panelVisible,
            enter = if (
                resolvedPresentation.motionMode ==
                    com.goreecloud.launcher.ui.theme.GlazeV16MotionMode.MINIMAL
            ) {
                fadeIn(animationSpec = tween(enterMillis))
            } else {
                fadeIn(animationSpec = tween(enterMillis)) +
                    scaleIn(
                        initialScale = 0.965f,
                        animationSpec = tween(enterMillis),
                    )
            },
            exit = if (
                resolvedPresentation.motionMode ==
                    com.goreecloud.launcher.ui.theme.GlazeV16MotionMode.MINIMAL
            ) {
                fadeOut(animationSpec = tween(exitMillis))
            } else {
                fadeOut(animationSpec = tween(exitMillis)) +
                    scaleOut(
                        targetScale = 0.975f,
                        animationSpec = tween(exitMillis),
                    )
            },
            ) {
                Surface(
                    modifier = Modifier
                        .width(panelWidth)
                        .testTag("launcher-glaze-folder-popup"),
                shape = RoundedCornerShape(GlazeMetrics.opticalHero),
                color = panelColor,
                border = BorderStroke(1.dp, panelBorder),
                shadowElevation = if (
                    resolvedPresentation.materialRole == GlazeV16MaterialRole.SOLID
                ) 12.dp else 24.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = GlazeMetrics.space4,
                            vertical = GlazeMetrics.space4,
                        ),
                    verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space3),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                folder.name,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = if (
                                    presentationContext.largeText ||
                                    presentationContext.extraLargeText
                                ) 2 else 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "${members.size} apps" +
                                    if (alphabetical) " · A–Z view" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        if (selectMode) {
                            TextButton(
                                onClick = {
                                    selectMode = false
                                    selectedKeys = emptySet()
                                },
                                modifier = Modifier.heightIn(min = folderControlTarget),
                            ) {
                                Text("Done")
                            }
                        }

                        Box {
                            Surface(
                                modifier = Modifier
                                    .size(folderControlTarget)
                                    .semantics { contentDescription = "Folder actions" },
                                onClick = { showActions = true },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                                ),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        "⋮",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = showActions,
                                onDismissRequest = { showActions = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Add apps") },
                                    onClick = {
                                        showActions = false
                                        dismissThen(onAddApps)
                                    },
                                )
                                if (allowHomePlacement) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (isOnHome) {
                                                    "Remove from Home"
                                                } else {
                                                    "Add to Home"
                                                },
                                            )
                                        },
                                        onClick = {
                                            showActions = false
                                            if (isOnHome) onRemoveFromHome() else onAddToHome()
                                        },
                                    )
                                }
                                if (allowHomePlacement && isOnHome && moveTargets.isNotEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Move to another Home page") },
                                        onClick = {
                                            showActions = false
                                            showMoveTargets = true
                                        },
                                    )
                                }
                                DropdownMenuItem(
                                    text = {
                                        Text(if (selectMode) "Finish selecting" else "Select apps")
                                    },
                                    onClick = {
                                        selectMode = !selectMode
                                        selectedKeys = emptySet()
                                        showActions = false
                                    },
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(if (alphabetical) "Original order" else "Sort A–Z")
                                    },
                                    onClick = {
                                        alphabetical = !alphabetical
                                        showActions = false
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Edit folder name") },
                                    onClick = {
                                        editName = true
                                        showActions = false
                                    },
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Delete folder",
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    },
                                    onClick = {
                                        showActions = false
                                        showDeleteConfirmation = true
                                    },
                                )
                            }
                        }
                    }

                    if (editName) {
                        OutlinedTextField(
                            value = nameDraft,
                            onValueChange = { nameDraft = it.take(40) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Folder name") },
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(
                                onClick = {
                                    nameDraft = folder.name
                                    editName = false
                                },
                            ) {
                                Text("Cancel")
                            }
                            TextButton(
                                enabled = nameDraft.trim().isNotBlank(),
                                onClick = {
                                    onRename(nameDraft.trim())
                                    editName = false
                                },
                            ) {
                                Text("Save")
                            }
                        }
                    }

                    if (members.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = GlazeMetrics.space4),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space3),
                        ) {
                            Text(
                                "This folder is empty.",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                "Add apps to build this collection.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            FilledTonalButton(
                                onClick = { dismissThen(onAddApps) },
                                modifier = Modifier.heightIn(min = folderControlTarget),
                            ) {
                                Text("Add apps")
                            }
                        }
                    } else {
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val columnCount = launcherFolderGridColumns(
                                availableWidthDp = maxWidth.value,
                                largeText = presentationContext.largeText ||
                                    presentationContext.extraLargeText,
                            )
                            val pageCapacity =
                                columnCount * LAUNCHER_FOLDER_ROWS_PER_PAGE
                            val pageCount = launcherFolderPageCount(
                                itemCount = members.size,
                                columns = columnCount,
                                includeAddTile = !selectMode,
                            )
                            val pagerState = rememberPagerState(
                                pageCount = { pageCount },
                            )
                            val pagerHeight =
                                folderCellMinHeight * LAUNCHER_FOLDER_ROWS_PER_PAGE.toFloat() +
                                    GlazeMetrics.space2 *
                                    (LAUNCHER_FOLDER_ROWS_PER_PAGE - 1).toFloat() +
                                    GlazeMetrics.space1 * 2f

                            LaunchedEffect(pageCount) {
                                val lastPage = (pageCount - 1).coerceAtLeast(0)
                                if (pagerState.currentPage > lastPage) {
                                    pagerState.scrollToPage(lastPage)
                                }
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space1),
                            ) {
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(pagerHeight),
                                    beyondViewportPageCount = 1,
                                    pageSpacing = GlazeMetrics.space2,
                                ) { pageIndex ->
                                    val startIndex = pageIndex * pageCapacity
                                    val pageApps = members
                                        .drop(startIndex)
                                        .take(pageCapacity)
                                    val addTileIndex = members.size
                                    val showAddTile = !selectMode &&
                                        addTileIndex >= startIndex &&
                                        addTileIndex < startIndex + pageCapacity

                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(columnCount),
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.spacedBy(
                                            GlazeMetrics.space2,
                                        ),
                                        verticalArrangement = Arrangement.spacedBy(
                                            GlazeMetrics.space2,
                                        ),
                                        contentPadding = PaddingValues(
                                            vertical = GlazeMetrics.space1,
                                        ),
                                        userScrollEnabled = false,
                                    ) {
                                        items(
                                            pageApps,
                                            key = { it.workspaceKey() },
                                        ) { app ->
                                            val appKey = app.workspaceKey()
                                            val icon = rememberLauncherAppIcon(app)
                                            val selected = appKey in selectedKeys
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(min = folderCellMinHeight)
                                                    .testTag("launcher-folder-app-" + appKey),
                                                onClick = {
                                                    if (selectMode) {
                                                        selectedKeys = if (selected) {
                                                            selectedKeys - appKey
                                                        } else {
                                                            selectedKeys + appKey
                                                        }
                                                    } else {
                                                        dismissThen { onLaunchApp(app) }
                                                    }
                                                },
                                                shape = RoundedCornerShape(
                                                    GlazeMetrics.radiusLarge,
                                                ),
                                                color = if (selected) {
                                                    MaterialTheme.colorScheme.primaryContainer.copy(
                                                        alpha = 0.82f,
                                                    )
                                                } else {
                                                    Color.Transparent
                                                },
                                                border = if (selected) {
                                                    BorderStroke(
                                                        1.dp,
                                                        MaterialTheme.colorScheme.primary.copy(
                                                            alpha = 0.70f,
                                                        ),
                                                    )
                                                } else {
                                                    null
                                                },
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(
                                                        vertical = GlazeMetrics.space2,
                                                        horizontal = 2.dp,
                                                    ),
                                                    horizontalAlignment =
                                                        Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(
                                                        GlazeMetrics.space2,
                                                    ),
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        if (icon != null) {
                                                            Image(
                                                                bitmap = icon,
                                                                contentDescription = null,
                                                                contentScale = ContentScale.Crop,
                                                                modifier = Modifier
                                                                    .size(50.dp)
                                                                    .launcherIconMask(),
                                                            )
                                                        } else {
                                                            Surface(
                                                                modifier = Modifier.size(50.dp),
                                                                color = MaterialTheme.colorScheme
                                                                    .onSurface.copy(alpha = 0.08f),
                                                                shape = RoundedCornerShape(16.dp),
                                                            ) {}
                                                        }
                                                        if (selectMode && selected) {
                                                            Surface(
                                                                modifier = Modifier
                                                                    .align(Alignment.TopEnd)
                                                                    .size(22.dp),
                                                                shape = CircleShape,
                                                                color = MaterialTheme.colorScheme
                                                                    .primary,
                                                            ) {
                                                                Box(
                                                                    contentAlignment =
                                                                        Alignment.Center,
                                                                ) {
                                                                    GlazePopupActionGlyph(
                                                                        symbol =
                                                                            GlazePopupActionSymbol.CHECK,
                                                                        color = MaterialTheme
                                                                            .colorScheme.onPrimary,
                                                                        iconSize = 12.dp,
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                    Text(
                                                        app.label.toString(),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        textAlign = TextAlign.Center,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                }
                                            }
                                        }

                                        if (showAddTile) {
                                            item(key = "launcher-folder-add-apps") {
                                                Surface(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .heightIn(min = folderCellMinHeight)
                                                        .testTag("launcher-folder-add-apps")
                                                        .semantics {
                                                            contentDescription =
                                                                "Add apps to ${folder.name}"
                                                        },
                                                    onClick = { dismissThen(onAddApps) },
                                                    shape = RoundedCornerShape(
                                                        GlazeMetrics.radiusLarge,
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                        .copy(alpha = 0.035f),
                                                    border = BorderStroke(
                                                        1.dp,
                                                        MaterialTheme.colorScheme.onSurface
                                                            .copy(alpha = 0.12f),
                                                    ),
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(
                                                            vertical = GlazeMetrics.space2,
                                                            horizontal = 2.dp,
                                                        ),
                                                        horizontalAlignment =
                                                            Alignment.CenterHorizontally,
                                                        verticalArrangement =
                                                            Arrangement.spacedBy(
                                                                GlazeMetrics.space2,
                                                            ),
                                                    ) {
                                                        Surface(
                                                            modifier = Modifier.size(50.dp),
                                                            shape = CircleShape,
                                                            color = MaterialTheme.colorScheme
                                                                .onSurface.copy(alpha = 0.07f),
                                                            border = BorderStroke(
                                                                1.dp,
                                                                MaterialTheme.colorScheme.onSurface
                                                                    .copy(alpha = 0.10f),
                                                            ),
                                                        ) {
                                                            Box(
                                                                contentAlignment =
                                                                    Alignment.Center,
                                                            ) {
                                                                Text(
                                                                    "＋",
                                                                    style = MaterialTheme
                                                                        .typography.titleLarge,
                                                                    color = MaterialTheme
                                                                        .colorScheme.onSurface,
                                                                )
                                                            }
                                                        }
                                                        Text(
                                                            "Add apps",
                                                            style = MaterialTheme.typography
                                                                .labelSmall,
                                                            color = MaterialTheme.colorScheme
                                                                .onSurfaceVariant,
                                                            textAlign = TextAlign.Center,
                                                            maxLines = 1,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                if (pageCount > 1) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 20.dp)
                                            .semantics {
                                                contentDescription =
                                                    "Folder page " +
                                                    "${pagerState.currentPage + 1} of $pageCount"
                                            },
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        if (pageCount <= 5) {
                                            repeat(pageCount) { pageIndex ->
                                                Surface(
                                                    modifier = Modifier
                                                        .padding(horizontal = 3.dp)
                                                        .size(
                                                            if (
                                                                pageIndex ==
                                                                pagerState.currentPage
                                                            ) 7.dp else 5.dp,
                                                        )
                                                        .clearAndSetSemantics {},
                                                    shape = CircleShape,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                        .copy(
                                                            alpha = if (
                                                                pageIndex ==
                                                                pagerState.currentPage
                                                            ) 0.72f else 0.22f,
                                                        ),
                                                ) {}
                                            }
                                        } else {
                                            Text(
                                                "${pagerState.currentPage + 1} / $pageCount",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme
                                                    .onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (selectMode) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.045f),
                        ) {
                            Column(
                                modifier = Modifier.padding(GlazeMetrics.space3),
                                verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                            ) {
                                Text(
                                    "${selectedKeys.size} selected · Apps remain installed",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Button(
                                    onClick = { confirmRemove = true },
                                    enabled = selectedKeys.isNotEmpty(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = folderControlTarget),
                                ) {
                                    Text("Remove selected")
                                }
                            }
                        }
                    }
                    }
                }
            }
        }
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text("Remove ${selectedKeys.size} apps from folder?") },
            text = {
                Text("The selected apps will stay installed and remain available in the app drawer.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        members.filter { it.workspaceKey() in selectedKeys }.forEach(onRemoveApp)
                        selectedKeys = emptySet()
                        selectMode = false
                        confirmRemove = false
                    },
                ) {
                    Text("Remove from folder")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showMoveTargets) {
        AlertDialog(
            onDismissRequest = { showMoveTargets = false },
            title = { Text("Move ${folder.name} to a page") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space1)) {
                    moveTargets.forEach { page ->
                        TextButton(
                            onClick = {
                                showMoveTargets = false
                                onMoveToPage(page.pageId)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = folderControlTarget),
                        ) {
                            Text(
                                if (page.pageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID) {
                                    "Primary Home"
                                } else {
                                    "Home page ${page.rank + 1}"
                                },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMoveTargets = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete ${folder.name}?") },
            text = {
                Text(
                    "This removes the folder and its membership from Home and the app drawer. " +
                        "Installed apps remain. The folder cannot be restored automatically.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    },
                ) {
                    Text("Delete folder", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LauncherFolderAppPickerSheet(
    folder: LauncherFolder,
    availableApps: List<LauncherActivityInfo>,
    onAddApp: (LauncherActivityInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable(folder.id) { mutableStateOf("") }
    val presentationContext = LocalGlazeV16PresentationContext.current
    val pickerCellMinWidth = launcherFolderPickerMinCellWidthDp(
        largeText = presentationContext.largeText,
        extraLargeText = presentationContext.extraLargeText,
    ).dp
    val visibleApps = remember(availableApps, query) {
        availableApps
            .asSequence()
            .filter { app ->
                query.isBlank() || app.label.toString().contains(query.trim(), ignoreCase = true)
            }
            .sortedWith(
                compareBy<LauncherActivityInfo> { it.label.toString().lowercase(Locale.getDefault()) }
                    .thenBy { it.workspaceKey() },
            )
            .toList()
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = GlazeMetrics.space4, vertical = GlazeMetrics.space3),
            verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space3),
        ) {
            Text(
                "Add apps to ${folder.name}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Select installed personal apps to include in this folder.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = GlazeMetrics.minimumTarget),
                singleLine = true,
                shape = RoundedCornerShape(GlazeMetrics.radiusPill),
                label = { Text("Search apps") },
                placeholder = { Text("Find an installed app") },
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = pickerCellMinWidth),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp),
                horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                contentPadding = PaddingValues(vertical = GlazeMetrics.space1),
            ) {
                items(
                    items = visibleApps,
                    key = { it.workspaceKey() },
                ) { app ->
                    val icon = rememberLauncherAppIcon(app)
                    val alreadyAdded = app.workspaceKey() in folder.appKeys
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 104.dp),
                        onClick = { onAddApp(app) },
                        enabled = !alreadyAdded && folder.appKeys.size < 100,
                        shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
                        color = if (alreadyAdded) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.24f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (alreadyAdded) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            },
                        ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = GlazeMetrics.space1,
                                    vertical = GlazeMetrics.space2,
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space1),
                        ) {
                            if (icon != null) {
                                Image(
                                    bitmap = icon,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(46.dp).launcherIconMask(),
                                )
                            } else {
                                Surface(
                                    modifier = Modifier.size(46.dp),
                                    shape = RoundedCornerShape(15.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                ) {}
                            }
                            Text(
                                app.label.toString(),
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                if (alreadyAdded) "Added" else "Add",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (alreadyAdded) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
            if (visibleApps.isEmpty()) {
                Text(
                    "No matching apps in this profile.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (folder.appKeys.size >= 100) {
                Text(
                    "This folder has reached its 100-app limit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Done") }
            Spacer(Modifier.height(GlazeMetrics.space2))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LauncherFolderAssignmentSheet(
    app: LauncherActivityInfo,
    folders: List<LauncherFolder>,
    onAssign: (LauncherFolder) -> Unit,
    onCreateFolder: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = GlazeMetrics.space4, vertical = GlazeMetrics.space3),
            verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space3),
        ) {
            Text(
                "Add ${app.label} to folder",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
            ) {
                lazyItems(
                    items = folders,
                    key = { it.id },
                ) { folder ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onAssign(folder) },
                        shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(GlazeMetrics.space3),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(folder.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${folder.appKeys.size} apps",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text("Add", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            OutlinedButton(
                onClick = onCreateFolder,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Create new folder")
            }
            Spacer(Modifier.height(GlazeMetrics.space2))
        }
    }
}

private class AboveAppIconPopupPositionProvider(
    private val anchor: Rect,
    private val gapPx: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val margin = gapPx.coerceAtLeast(1)
        val centerX = ((anchor.left + anchor.right) / 2f).roundToInt()
        val maxX = (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin)
        val x = (centerX - popupContentSize.width / 2).coerceIn(margin, maxX)

        val above = anchor.top.roundToInt() - popupContentSize.height - gapPx
        val below = anchor.bottom.roundToInt() + gapPx
        val maxY = (windowSize.height - popupContentSize.height - margin).coerceAtLeast(margin)
        val y = if (above >= margin) above else below.coerceIn(margin, maxY)
        return IntOffset(x, y.coerceIn(margin, maxY))
    }
}

private data class LauncherContextShortcut(
    val label: String,
    val action: LauncherLaunchShortcutSearchAction,
)

@Composable
private fun rememberLauncherContextShortcuts(
    app: LauncherActivityInfo,
): List<LauncherContextShortcut> {
    val context = LocalContext.current
    var shortcuts by remember(app.componentName, app.user) {
        mutableStateOf<List<LauncherContextShortcut>>(emptyList())
    }

    LaunchedEffect(app.componentName, app.user) {
        shortcuts = withContext(Dispatchers.IO) {
            val launcherApps = context.applicationContext.getSystemService(LauncherApps::class.java)
            if (!launcherApps.hasShortcutHostPermission()) return@withContext emptyList()

            val query = LauncherApps.ShortcutQuery()
                .setPackage(app.componentName.packageName)
                .setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST,
                )
            runCatching {
                launcherApps.getShortcuts(query, app.user).orEmpty()
            }.getOrDefault(emptyList())
                .asSequence()
                .filter { shortcut -> shortcut.isEnabled }
                .sortedBy { shortcut -> shortcut.rank }
                .mapNotNull { shortcut ->
                    val label = shortcut.shortLabel?.toString()?.trim().orEmpty()
                    if (label.isBlank()) return@mapNotNull null
                    LauncherContextShortcut(
                        label = label,
                        action = LauncherLaunchShortcutSearchAction(
                            packageName = shortcut.getPackage(),
                            shortcutId = shortcut.id,
                            user = shortcut.userHandle,
                        ),
                    )
                }
                .take(4)
                .toList()
        }
    }

    return shortcuts
}

@Composable
private fun AppContextPopup(
    app: LauncherActivityInfo,
    anchor: Rect,
    contextOrigin: LauncherAppContextOrigin,
    workspace: WorkspaceState,
    layoutLocked: Boolean,
    drawerPinned: Boolean,
    canMoveDrawerPinnedEarlier: Boolean,
    canMoveDrawerPinnedLater: Boolean,
    canResetDrawerPinnedOrder: Boolean,
    hasDrawerTabs: Boolean,
    canMoveDockEarlier: Boolean,
    canMoveDockLater: Boolean,
    hiddenFromLauncher: Boolean,
    lockedByLauncher: Boolean,
    availableAndroidWidgets: List<LauncherWidgetProviderDescriptor>,
    onHomeAction: () -> Unit,
    onToggleDock: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onRequestUninstall: () -> Unit,
    onToggleDrawerPinned: () -> Unit,
    onMoveDrawerPinnedEarlier: () -> Unit,
    onMoveDrawerPinnedLater: () -> Unit,
    onMoveDockEarlier: () -> Unit,
    onMoveDockLater: () -> Unit,
    onResetDrawerPinnedOrder: () -> Unit,
    onManageDrawerTabs: () -> Unit,
    onToggleHidden: () -> Unit,
    onToggleLocked: () -> Unit,
    onAddToFolder: () -> Unit,
    onOpenWidgets: (List<LauncherWidgetProviderDescriptor>) -> Unit,
    onLaunchShortcut: (LauncherLaunchShortcutSearchAction) -> Unit,
    onClose: () -> Unit,
) {
    val key = app.workspaceKey()
    val isFavorite = key in workspace.favoriteKeys
    val isDocked = key in workspace.dockKeys
    val dockFull = !isDocked && workspace.dockKeys.size >= MAX_DOCK_ITEMS
    val canAddToFolder = true
    val icon = rememberLauncherAppIcon(app)
    val shortcuts = rememberLauncherContextShortcuts(app)
    val appWidgets = remember(
        availableAndroidWidgets,
        app.componentName.packageName,
        app.user,
    ) {
        if (app.user != Process.myUserHandle()) {
            emptyList()
        } else {
            availableAndroidWidgets.filter { descriptor ->
                descriptor.packageName == app.componentName.packageName
            }
        }
    }
    val badgeCounts by com.goreecloud.launcher.core.launcher.LauncherNotificationBadges.counts.collectAsState()
    val badgesEnabled by com.goreecloud.launcher.core.launcher.LauncherNotificationBadges.enabled.collectAsState()
    val badgeAccess by com.goreecloud.launcher.core.launcher.LauncherNotificationBadges.accessGranted.collectAsState()
    val badgeCount = if (badgesEnabled && badgeAccess) {
        com.goreecloud.launcher.core.launcher.LauncherNotificationBadges.countFor(app, badgeCounts)
    } else {
        0
    }
    val gapPx = with(LocalDensity.current) { 10.dp.roundToPx() }

    Popup(
        popupPositionProvider = remember(anchor, gapPx) {
            AboveAppIconPopupPositionProvider(anchor, gapPx)
        },
        onDismissRequest = onClose,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        Surface(
            modifier = Modifier
                .widthIn(min = 296.dp, max = 340.dp)
                .testTag("launcher-glaze-app-context-menu"),
            shape = RoundedCornerShape(GlazeMetrics.radius2ExtraLarge),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 18.dp,
        ) {
            Column(
                modifier = Modifier.padding(GlazeMetrics.space2),
                verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space1),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = GlazeMetrics.space2, vertical = GlazeMetrics.space2),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                ) {
                    if (icon != null) {
                        Image(
                            bitmap = icon,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(42.dp).launcherIconMask(),
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            app.label.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val status = buildList {
                            if (badgeCount > 0) {
                                add("$badgeCount " + if (badgeCount == 1) "notification" else "notifications")
                            }
                            if (drawerPinned) add("Pinned in Apps")
                            if (lockedByLauncher) add("App Lock")
                            if (layoutLocked) add("Home layout locked")
                        }.joinToString(" · ")
                        if (status.isNotBlank()) {
                            Text(
                                status,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.44f),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = GlazeMetrics.space1, vertical = GlazeMetrics.space1),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        GlazeLauncherPopupQuickAction(
                            label = if (
                                contextOrigin == LauncherAppContextOrigin.HOME || isFavorite
                            ) {
                                "Remove"
                            } else {
                                "Home"
                            },
                            symbol = GlazePopupActionSymbol.HOME,
                            onClick = onHomeAction,
                            enabled = !layoutLocked,
                            modifier = Modifier.weight(1f),
                        )
                        GlazeLauncherPopupQuickAction(
                            label = if (isDocked) "Undock" else "Dock",
                            symbol = GlazePopupActionSymbol.DOCK,
                            onClick = onToggleDock,
                            enabled = !layoutLocked && !dockFull,
                            modifier = Modifier.weight(1f),
                        )
                        GlazeLauncherPopupQuickAction(
                            label = "Widgets",
                            symbol = GlazePopupActionSymbol.WIDGET,
                            onClick = { onOpenWidgets(appWidgets) },
                            enabled = appWidgets.isNotEmpty() && !layoutLocked,
                            modifier = Modifier.weight(1f),
                        )
                        GlazeLauncherPopupQuickAction(
                            label = "Info",
                            symbol = GlazePopupActionSymbol.INFO,
                            onClick = onOpenAppInfo,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                if (shortcuts.isNotEmpty()) {
                    Text(
                        "Shortcuts",
                        modifier = Modifier.padding(
                            start = GlazeMetrics.space3,
                            top = GlazeMetrics.space1,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                    shortcuts.forEach { shortcut ->
                        GlazeLauncherPopupAction(
                            label = shortcut.label,
                            symbol = GlazePopupActionSymbol.SHORTCUT,
                            onClick = { onLaunchShortcut(shortcut.action) },
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                if (contextOrigin == LauncherAppContextOrigin.DRAWER) {
                    GlazeLauncherPopupAction(
                        label = if (drawerPinned) "Unpin in Apps" else "Pin in Apps",
                        symbol = GlazePopupActionSymbol.PIN,
                        onClick = onToggleDrawerPinned,
                    )
                    if (drawerPinned) {
                        GlazeLauncherPopupAction(
                            label = "Move pinned earlier",
                            symbol = GlazePopupActionSymbol.PIN,
                            onClick = onMoveDrawerPinnedEarlier,
                            enabled = canMoveDrawerPinnedEarlier,
                        )
                        GlazeLauncherPopupAction(
                            label = "Move pinned later",
                            symbol = GlazePopupActionSymbol.PIN,
                            onClick = onMoveDrawerPinnedLater,
                            enabled = canMoveDrawerPinnedLater,
                        )
                        GlazeLauncherPopupAction(
                            label = "Reset pinned order A–Z",
                            symbol = GlazePopupActionSymbol.PIN,
                            onClick = onResetDrawerPinnedOrder,
                            enabled = canResetDrawerPinnedOrder,
                        )
                    }
                    if (hasDrawerTabs) {
                        GlazeLauncherPopupAction(
                            label = "App drawer tabs",
                            symbol = GlazePopupActionSymbol.APPS,
                            onClick = onManageDrawerTabs,
                        )
                    }
                }
                if (contextOrigin == LauncherAppContextOrigin.DOCK) {
                    GlazeLauncherPopupAction(label = "Move earlier in Dock", symbol = GlazePopupActionSymbol.BACK, onClick = onMoveDockEarlier, enabled = canMoveDockEarlier && !layoutLocked)
                    GlazeLauncherPopupAction(label = "Move later in Dock", symbol = GlazePopupActionSymbol.FORWARD, onClick = onMoveDockLater, enabled = canMoveDockLater && !layoutLocked)
                }
                GlazeLauncherPopupAction(
                    label = "Add to folder",
                    symbol = GlazePopupActionSymbol.FOLDER,
                    onClick = onAddToFolder,
                    enabled = canAddToFolder && !layoutLocked,
                )
                GlazeLauncherPopupAction(
                    label = if (lockedByLauncher) "Remove App Lock" else "Lock app",
                    symbol = GlazePopupActionSymbol.LOCK,
                    onClick = onToggleLocked,
                )
                GlazeLauncherPopupAction(
                    label = if (hiddenFromLauncher) "Show in Apps & Search" else "Hide from Apps & Search",
                    symbol = GlazePopupActionSymbol.VISIBILITY,
                    onClick = onToggleHidden,
                )
                GlazeLauncherPopupAction(
                    label = "Uninstall",
                    symbol = GlazePopupActionSymbol.UNINSTALL,
                    onClick = onRequestUninstall,
                    destructive = true,
                )
            }
        }
    }
}

@Composable
private fun LauncherAppWidgetChoicesDialog(
    appLabel: String,
    providers: List<LauncherWidgetProviderDescriptor>,
    apps: List<LauncherActivityInfo>,
    onChoose: (LauncherWidgetProviderDescriptor) -> Unit,
    onClose: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Column {
                Text("Widgets")
                if (appLabel.isNotBlank()) {
                    Text(
                        appLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
            ) {
                providers.forEach { descriptor ->
                    InstalledWidgetPickerRow(
                        descriptor = descriptor,
                        apps = apps,
                        onClick = { onChoose(descriptor) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) { Text("Cancel") }
        },
    )
}

private enum class GlazePopupActionSymbol {
    HOME,
    DOCK,
    WIDGET,
    ADD,
    DELETE,
    SHORTCUT,
    PIN,
    FOLDER,
    WALLPAPER,
    APPS,
    SETTINGS,
    CHECK,
    BACK,
    FORWARD,
    CLOSE,
    INFO,
    LOCK,
    VISIBILITY,
    UNINSTALL,
}

/** Decorative vector geometry; labels remain the accessible action description. */
@Composable
private fun GlazePopupActionGlyph(
    symbol: GlazePopupActionSymbol,
    color: Color,
    iconSize: Dp = 22.dp,
) {
    Canvas(Modifier.size(iconSize)) {
        val u = size.minDimension
        val w = 1.8.dp.toPx()
        fun segment(x1: Float, y1: Float, x2: Float, y2: Float) {
            drawLine(
                color,
                Offset(x1 * u, y1 * u),
                Offset(x2 * u, y2 * u),
                strokeWidth = w,
                cap = StrokeCap.Round,
            )
        }
        when (symbol) {
            GlazePopupActionSymbol.HOME -> {
                segment(.14f, .45f, .50f, .14f)
                segment(.50f, .14f, .86f, .45f)
                segment(.24f, .38f, .24f, .86f)
                segment(.76f, .38f, .76f, .86f)
                segment(.24f, .86f, .76f, .86f)
                segment(.45f, .86f, .45f, .62f)
                segment(.45f, .62f, .58f, .62f)
                segment(.58f, .62f, .58f, .86f)
            }
            GlazePopupActionSymbol.DOCK -> {
                segment(.14f, .78f, .86f, .78f)
                segment(.20f, .22f, .20f, .56f)
                segment(.20f, .56f, .80f, .56f)
                segment(.80f, .56f, .80f, .22f)
                segment(.20f, .22f, .80f, .22f)
            }
            GlazePopupActionSymbol.WIDGET -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(u * .13f, u * .16f),
                    size = androidx.compose.ui.geometry.Size(u * .74f, u * .68f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(u * .11f),
                    style = Stroke(w),
                )
                segment(.30f, .34f, .70f, .34f)
                segment(.30f, .50f, .58f, .50f)
                segment(.30f, .66f, .48f, .66f)
            }
            GlazePopupActionSymbol.ADD -> {
                segment(.18f, .50f, .82f, .50f)
                segment(.50f, .18f, .50f, .82f)
            }
            GlazePopupActionSymbol.DELETE -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(u * .28f, u * .34f),
                    size = Size(u * .44f, u * .48f),
                    cornerRadius = CornerRadius(u * .06f),
                    style = Stroke(w),
                )
                segment(.22f, .28f, .78f, .28f)
                segment(.38f, .20f, .62f, .20f)
                segment(.40f, .42f, .40f, .70f)
                segment(.60f, .42f, .60f, .70f)
            }
            GlazePopupActionSymbol.SHORTCUT -> {
                segment(.24f, .76f, .76f, .24f)
                segment(.48f, .24f, .76f, .24f)
                segment(.76f, .24f, .76f, .52f)
                segment(.24f, .44f, .24f, .76f)
                segment(.24f, .76f, .56f, .76f)
            }
            GlazePopupActionSymbol.PIN -> {
                segment(.28f, .14f, .72f, .14f)
                segment(.72f, .14f, .72f, .48f)
                segment(.72f, .48f, .60f, .58f)
                segment(.60f, .58f, .60f, .86f)
                segment(.40f, .86f, .40f, .58f)
                segment(.40f, .58f, .28f, .48f)
                segment(.28f, .48f, .28f, .14f)
            }
            GlazePopupActionSymbol.FOLDER -> {
                segment(.10f, .25f, .43f, .25f)
                segment(.43f, .25f, .51f, .36f)
                segment(.51f, .36f, .89f, .36f)
                segment(.89f, .36f, .89f, .80f)
                segment(.89f, .80f, .10f, .80f)
                segment(.10f, .80f, .10f, .25f)
            }
            GlazePopupActionSymbol.WALLPAPER -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(u * .14f, u * .18f),
                    size = androidx.compose.ui.geometry.Size(u * .72f, u * .64f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(u * .10f),
                    style = Stroke(w),
                )
                drawCircle(
                    color = color,
                    radius = u * .08f,
                    center = Offset(u * .34f, u * .38f),
                    style = Stroke(w),
                )
                segment(.20f, .72f, .42f, .52f)
                segment(.42f, .52f, .55f, .63f)
                segment(.55f, .63f, .70f, .46f)
                segment(.70f, .46f, .82f, .58f)
            }
            GlazePopupActionSymbol.APPS -> {
                listOf(
                    .23f to .23f,
                    .61f to .23f,
                    .23f to .61f,
                    .61f to .61f,
                ).forEach { (x, y) ->
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(u * x, u * y),
                        size = androidx.compose.ui.geometry.Size(u * .18f, u * .18f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(u * .04f),
                        style = Stroke(w),
                    )
                }
            }
            GlazePopupActionSymbol.SETTINGS -> {
                drawCircle(
                    color = color,
                    radius = u * .24f,
                    center = Offset(u * .50f, u * .50f),
                    style = Stroke(w),
                )
                drawCircle(
                    color = color,
                    radius = u * .07f,
                    center = Offset(u * .50f, u * .50f),
                    style = Stroke(w),
                )
                listOf(
                    .50f to .12f,
                    .50f to .88f,
                    .12f to .50f,
                    .88f to .50f,
                    .23f to .23f,
                    .77f to .77f,
                    .77f to .23f,
                    .23f to .77f,
                ).forEach { (x, y) ->
                    segment(.50f, .50f, x, y)
                }
            }
            GlazePopupActionSymbol.CHECK -> {
                segment(.18f, .52f, .40f, .72f)
                segment(.40f, .72f, .82f, .28f)
            }
            GlazePopupActionSymbol.BACK -> {
                segment(.68f, .18f, .34f, .50f)
                segment(.34f, .50f, .68f, .82f)
            }
            GlazePopupActionSymbol.FORWARD -> {
                segment(.32f, .18f, .66f, .50f)
                segment(.66f, .50f, .32f, .82f)
            }
            GlazePopupActionSymbol.CLOSE -> {
                segment(.24f, .24f, .76f, .76f)
                segment(.76f, .24f, .24f, .76f)
            }
            GlazePopupActionSymbol.INFO -> {
                drawCircle(color, radius = u * .36f, center = Offset(u * .5f, u * .5f), style = Stroke(w))
                drawCircle(color, radius = w * .65f, center = Offset(u * .5f, u * .33f))
                segment(.50f, .47f, .50f, .70f)
            }
            GlazePopupActionSymbol.LOCK -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(u * .24f, u * .43f),
                    size = Size(u * .52f, u * .40f),
                    cornerRadius = CornerRadius(u * .08f),
                    style = Stroke(w),
                )
                drawArc(
                    color = color,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(u * .31f, u * .16f),
                    size = Size(u * .38f, u * .48f),
                    style = Stroke(w),
                )
                drawCircle(
                    color = color,
                    radius = u * .035f,
                    center = Offset(u * .50f, u * .61f),
                )
            }
            GlazePopupActionSymbol.VISIBILITY -> {
                drawOval(
                    color = color,
                    topLeft = Offset(u * .14f, u * .30f),
                    size = androidx.compose.ui.geometry.Size(u * .72f, u * .40f),
                    style = Stroke(w),
                )
                drawCircle(
                    color = color,
                    radius = u * .10f,
                    center = Offset(u * .50f, u * .50f),
                    style = Stroke(w),
                )
            }
            GlazePopupActionSymbol.UNINSTALL -> {
                segment(.24f, .24f, .76f, .76f)
                segment(.76f, .24f, .24f, .76f)
            }
        }
    }
}

@Composable
private fun GlazeLauncherPopupQuickAction(
    label: String,
    symbol: GlazePopupActionSymbol,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.heightIn(min = 64.dp),
        onClick = onClick,
        enabled = enabled,
        color = Color.Transparent,
        shape = RoundedCornerShape(GlazeMetrics.radiusMedium),
    ) {
        Column(
            modifier = Modifier.padding(vertical = GlazeMetrics.space1),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            }
            GlazePopupActionGlyph(symbol, color)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Consistent, compact Glaze action rows with full-width accessibility targets. */
@Composable
private fun GlazeLauncherPopupAction(
    label: String,
    symbol: GlazePopupActionSymbol,
    onClick: () -> Unit,
    enabled: Boolean = true,
    destructive: Boolean = false,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        onClick = onClick,
        enabled = enabled,
        color = Color.Transparent,
        shape = RoundedCornerShape(GlazeMetrics.radiusMedium),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = GlazeMetrics.space3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space3),
        ) {
            val actionColor = when {
                !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                destructive -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            }
            GlazePopupActionGlyph(symbol, actionColor)
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    destructive -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
