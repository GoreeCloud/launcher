package com.goreecloud.launcher.ui

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.appwidget.AppWidgetHostView
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.net.Uri
import android.os.BatteryManager
import android.os.Process
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import com.goreecloud.launcher.core.launcher.LauncherUniversalSearchHomeMode
import com.goreecloud.launcher.core.launcher.LaunchApplicationSearchAction
import com.goreecloud.launcher.core.launcher.LauncherDockStyle
import com.goreecloud.launcher.core.launcher.LauncherDrawerBackdrop
import com.goreecloud.launcher.core.launcher.LauncherDrawerEntryMode
import com.goreecloud.launcher.core.launcher.LauncherDrawerLayoutMode
import com.goreecloud.launcher.core.launcher.LauncherDrawerNavigation
import com.goreecloud.launcher.core.launcher.LauncherDrawerProfileKind
import com.goreecloud.launcher.core.launcher.LauncherDrawerSearchPlacement
import com.goreecloud.launcher.core.launcher.LauncherDrawerSpacing
import com.goreecloud.launcher.core.launcher.LauncherExperiencePreferences
import com.goreecloud.launcher.core.launcher.LauncherHomeCardStyle
import com.goreecloud.launcher.core.launcher.LauncherHomeLabelPolicy
import com.goreecloud.launcher.core.launcher.LauncherHomeAppMode
import com.goreecloud.launcher.core.launcher.LauncherHomeGlanceAlignment
import com.goreecloud.launcher.core.launcher.LauncherHomeSearchPlacement
import com.goreecloud.launcher.core.launcher.LauncherHomeSearchStyle
import com.goreecloud.launcher.core.launcher.LauncherHomeSpacing
import com.goreecloud.launcher.core.launcher.LauncherGestureAction
import com.goreecloud.launcher.core.launcher.LauncherGestureActionType
import com.goreecloud.launcher.core.launcher.LauncherFolder
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
import com.goreecloud.launcher.core.launcher.LauncherWidgetProviderDescriptor
import com.goreecloud.launcher.core.launcher.launcherDrawerProfilePages
import com.goreecloud.launcher.core.workspace.MAX_DOCK_ITEMS
import com.goreecloud.launcher.core.workspace.WorkspaceMoveDirection
import com.goreecloud.launcher.core.workspace.WorkspaceState
import com.goreecloud.launcher.core.workspace.WorkspaceWidgetCatalog
import com.goreecloud.launcher.core.workspace.WorkspaceWidgetDescriptor
import com.goreecloud.launcher.core.workspace.db.WorkspaceRenderedHomeFolder
import com.goreecloud.launcher.core.workspace.db.WorkspaceRenderedHomePage
import com.goreecloud.launcher.core.workspace.db.WorkspaceRenderedHomeWidget
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
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

enum class LauncherSurfaceMode { HOME, SEARCH, DRAWER, SETTINGS, THEME_MANAGER }

private enum class LauncherAppDragOrigin { HOME, DOCK, DRAWER }

private data class LauncherAppDragData(
    val appKey: String,
    val origin: LauncherAppDragOrigin,
)

private fun LauncherAppDragData.toTransferData(): DragAndDropTransferData =
    DragAndDropTransferData(
        clipData = ClipData.newPlainText("GoreeCloud Launcher app", appKey),
        localState = this,
    )

private fun DragAndDropEvent.launcherAppDragData(): LauncherAppDragData? =
    toAndroidDragEvent().localState as? LauncherAppDragData

private fun DragAndDropEvent.rootDropPoint(): Offset =
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

@Composable
fun LauncherBetaRoot(
    apps: List<LauncherActivityInfo>,
    workspace: WorkspaceState,
    preferences: LauncherPreferences,
    drawerLayoutMode: LauncherDrawerLayoutMode,
    experiencePreferences: LauncherExperiencePreferences,
    recentAppKeys: List<String>,
    localLaunchCounts: Map<String, Long>,
    searchProviderPreferences: com.goreecloud.launcher.core.launcher.LauncherSearchProviderPreferenceDecodeResult?,
    fileSearchRoots: List<Uri>,
    homePageCount: Int,
    homeResetSequence: Long,
    homeLabelOverrides: Map<String, String>,
    folders: List<LauncherFolder>,
    primaryHomePage: WorkspaceRenderedHomePage?,
    homePages: List<WorkspaceRenderedHomePage>,
    onMoveFolderToPage: (LauncherFolder, String) -> Unit,
    onCreateFolder: (String, Boolean, LauncherActivityInfo?) -> Unit,
    onRenameFolder: (String, String) -> Unit,
    onDeleteFolder: (LauncherFolder) -> Unit,
    onAddAppToFolder: (String, LauncherActivityInfo) -> Unit,
    onRemoveAppFromFolder: (String, LauncherActivityInfo) -> Unit,
    onAddFolderToHome: (LauncherFolder) -> Unit,
    onRemoveFolderFromHome: (LauncherFolder) -> Unit,
    onMoveHomeFolderToCell: (LauncherFolder, Int, Int) -> Unit,
    onManageHomePages: () -> Unit,
    isDefaultHome: Boolean,
    onRequestHomeRole: () -> Unit,
    onLaunchApp: (LauncherActivityInfo) -> Unit,
    onOpenAppInfo: (LauncherActivityInfo) -> Unit,
    onAddBuiltInWidget: (String) -> Unit,
    availableAndroidWidgets: List<LauncherWidgetProviderDescriptor>,
    availableIconPacks: List<LauncherIconPackDescriptor>,
    onPickInstalledAndroidWidget: (LauncherWidgetProviderDescriptor) -> Unit,
    onPickAndroidWidget: () -> Unit,
    onCreateAndroidWidgetView: (Int) -> AppWidgetHostView?,
    onRemoveWidget: (WorkspaceRenderedHomeWidget) -> Unit,
    onResizeWidget: (WorkspaceRenderedHomeWidget, Int, Int) -> Unit,
    onMoveWidget: (WorkspaceRenderedHomeWidget, Int, Int) -> Unit,
    onToggleFavorite: (LauncherActivityInfo) -> Unit,
    onToggleDock: (LauncherActivityInfo) -> Unit,
    onMoveFavorite: (LauncherActivityInfo, WorkspaceMoveDirection) -> Unit,
    onMoveFavoriteToCell: (LauncherActivityInfo, Int, Int) -> Unit,
    onMoveFavoriteToDock: (LauncherActivityInfo, String?) -> Unit,
    onMoveDockToHomeCell: (LauncherActivityInfo, Int, Int) -> Unit,
    onCopyDrawerToHomeCell: (LauncherActivityInfo, Int, Int) -> Unit,
    onCopyDrawerToDock: (LauncherActivityInfo, String?) -> Unit,
    onReorderDockByDrop: (LauncherActivityInfo, String?) -> Unit,
    onMoveDock: (LauncherActivityInfo, WorkspaceMoveDirection) -> Unit,
    onSetHomeLabelOverride: (LauncherActivityInfo, String?) -> Unit,
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
    onShowHintsAgain: () -> Unit,
    onSetDrawerBackdrop: (LauncherDrawerBackdrop) -> Unit,
    onSetDrawerSearchPlacement: (LauncherDrawerSearchPlacement) -> Unit,
    onSetDrawerNavigation: (LauncherDrawerNavigation) -> Unit,
    onSetDrawerEntryMode: (LauncherDrawerEntryMode) -> Unit,
    onSetDrawerSpacing: (LauncherDrawerSpacing) -> Unit,
    onSetDrawerPageRows: (Int) -> Unit,
    onSetShowDrawerAppCount: (Boolean) -> Unit,
    onSetHomeGlanceAlignment: (LauncherHomeGlanceAlignment) -> Unit,
    onSetHomeSearchPlacement: (LauncherHomeSearchPlacement) -> Unit,
    onSetHomeSearchStyle: (LauncherHomeSearchStyle) -> Unit,
    onSetHomeSpacing: (LauncherHomeSpacing) -> Unit,
    onSetDockStyle: (LauncherDockStyle) -> Unit,
    onSetWallpaperShade: (LauncherWallpaperShade) -> Unit,
    onSetGestureAction: (LauncherHomeGesture, LauncherGestureAction) -> Unit,
    onOpenWallpaperPicker: () -> Unit,
    onSurfaceModeChanged: (LauncherSurfaceMode) -> Unit,
) {
    var surfaceModeName by rememberSaveable { mutableStateOf(LauncherSurfaceMode.HOME.name) }
    val surfaceMode = runCatching { LauncherSurfaceMode.valueOf(surfaceModeName) }
        .getOrDefault(LauncherSurfaceMode.HOME)
    var selectedApp by remember { mutableStateOf<LauncherActivityInfo?>(null) }
    var selectedAppAnchor by remember { mutableStateOf<Rect?>(null) }
    var showDetailedAppOptions by remember { mutableStateOf(false) }
    var selectedWidget by remember { mutableStateOf<WorkspaceRenderedHomeWidget?>(null) }
    var selectedFolderId by rememberSaveable { mutableStateOf<String?>(null) }
    var folderAppPickerId by rememberSaveable { mutableStateOf<String?>(null) }
    var showFolderManager by rememberSaveable { mutableStateOf(false) }
    var folderManagerAddToHome by rememberSaveable { mutableStateOf(false) }
    var folderAssignmentAppKey by rememberSaveable { mutableStateOf<String?>(null) }
    val rootAppsByKey = remember(apps) {
        val personalUser = Process.myUserHandle()
        apps.asSequence()
            .filter { it.user == personalUser }
            .associateBy { it.workspaceKey() }
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
    var homeEditMode by rememberSaveable { mutableStateOf(false) }

    val currentApps by rememberUpdatedState(apps)
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

    val launcherDragTarget = remember {
        object : DragAndDropTarget {
            override fun onStarted(event: DragAndDropEvent) {
                val drag = event.launcherAppDragData() ?: return
                if (drag.origin != LauncherAppDragOrigin.DRAWER) return
                selectedApp = null
                selectedAppAnchor = null
                showDetailedAppOptions = false
                homeEditMode = true
                activeDrag = drag
                dragPoint = null
                drawerSearchRequested = false
                surfaceModeName = LauncherSurfaceMode.HOME.name
            }

            override fun onMoved(event: DragAndDropEvent) {
                if (activeDrag?.origin == LauncherAppDragOrigin.DRAWER) {
                    dragPoint = event.rootDropPoint()
                }
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                val drag = activeDrag ?: event.launcherAppDragData() ?: return false
                if (drag.origin != LauncherAppDragOrigin.DRAWER) return false
                val point = event.rootDropPoint()
                dragPoint = point
                return routeAppDrop(drag, point)
            }

            override fun onEnded(event: DragAndDropEvent) {
                if (activeDrag?.origin == LauncherAppDragOrigin.DRAWER) {
                    activeDrag = null
                    dragPoint = null
                }
            }
        }
    }

    val beginLocalDrag: (LauncherAppDragData, Offset) -> Unit = { drag, point ->
        selectedApp = null
        selectedAppAnchor = null
        showDetailedAppOptions = false
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
        showDetailedAppOptions = false
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
            .dragAndDropTarget(
                shouldStartDragAndDrop = { event ->
                    event.launcherAppDragData()?.origin == LauncherAppDragOrigin.DRAWER
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
            LauncherSurfaceMode.HOME -> HomeSurface(
                apps = apps,
                workspace = workspace,
                preferences = preferences,
                experiencePreferences = experiencePreferences,
                recentAppKeys = recentAppKeys,
                localLaunchCounts = localLaunchCounts,
                homePageCount = homePageCount,
                homeEditorRequestSequence = homeEditorRequestSequence,
                homeLabelOverrides = homeLabelOverrides,
                folders = folders,
                primaryHomePage = primaryHomePage,
                editMode = homeEditMode,
                activeDrag = activeDrag,
                dragPoint = dragPoint,
                homeCellBounds = homeCellBounds,
                dockItemBounds = dockItemBounds,
                onDockBoundsChanged = { dockBounds = it },
                onBeginLocalDrag = beginLocalDrag,
                onUpdateLocalDrag = updateLocalDrag,
                onEndLocalDrag = endLocalDrag,
                onCancelLocalDrag = cancelLocalDrag,
                onExitEditMode = {
                    homeEditMode = false
                    selectedApp = null
                    selectedAppAnchor = null
                    showDetailedAppOptions = false
                    selectedWidget = null
                    activeDrag = null
                    dragPoint = null
                },
                onManageHomePages = onManageHomePages,
                onManageFolders = {
                    folderManagerAddToHome = true
                    showFolderManager = true
                },
                onOpenFolder = { folder -> selectedFolderId = folder.id },
                onMoveFavoriteToCell = onMoveFavoriteToCell,
                onMoveWidget = onMoveWidget,
                onMoveHomeFolderToCell = onMoveHomeFolderToCell,
                onLaunchApp = onLaunchApp,
                onAddBuiltInWidget = onAddBuiltInWidget,
                availableAndroidWidgets = availableAndroidWidgets,
                onPickInstalledAndroidWidget = onPickInstalledAndroidWidget,
                onPickAndroidWidget = onPickAndroidWidget,
                onCreateAndroidWidgetView = onCreateAndroidWidgetView,
                onManageWidget = {
                    homeEditMode = true
                    selectedApp = null
                    selectedAppAnchor = null
                    showDetailedAppOptions = false
                    selectedWidget = it
                },
                onOpenLauncherSearch = {
                    drawerSearchRequested = false
                    surfaceModeName = LauncherSurfaceMode.SEARCH.name
                },
                onManageApp = { app, anchor ->
                    homeEditMode = true
                    selectedWidget = null
                    selectedApp = app
                    selectedAppAnchor = anchor
                    showDetailedAppOptions = false
                },
                onOpenDrawer = {
                    drawerSearchRequested =
                        experiencePreferences.drawerEntryMode == LauncherDrawerEntryMode.SEARCH_FIRST
                    surfaceModeName = LauncherSurfaceMode.DRAWER.name
                },
                onOpenSettings = { surfaceModeName = LauncherSurfaceMode.SETTINGS.name },
                onOpenThemeManager = { surfaceModeName = LauncherSurfaceMode.THEME_MANAGER.name },
                onOpenWallpaperPicker = onOpenWallpaperPicker,
            )
            LauncherSurfaceMode.SEARCH -> LauncherProviderControlledSearchSurface(
                apps = apps,
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
                apps = apps,
                folders = folders,
                preferences = preferences,
                drawerLayoutMode = drawerLayoutMode,
                experiencePreferences = experiencePreferences,
                focusSearch = drawerSearchRequested,
                onLaunchApp = onLaunchApp,
                onManageApp = { app, anchor ->
                    selectedApp = app
                    selectedAppAnchor = anchor
                    showDetailedAppOptions = false
                },
                onOpenFolder = { folder -> selectedFolderId = folder.id },
                onManageFolders = {
                    folderManagerAddToHome = false
                    showFolderManager = true
                },
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
                        experiencePreferences = experiencePreferences,
                        availableIconPacks = availableIconPacks,
                        themeMode = themeMode,
                        isDefaultHome = isDefaultHome,
                        onRequestHomeRole = onRequestHomeRole,
                        onManageFolders = {
                            folderManagerAddToHome = false
                            showFolderManager = true
                        },
                        onSetHomeGrid = onSetHomeGrid,
                        onSetDrawerColumns = onSetDrawerColumns,
                        onSetDrawerLayoutMode = onSetDrawerLayoutMode,
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
                        onShowHintsAgain = onShowHintsAgain,
                        onSetDrawerBackdrop = onSetDrawerBackdrop,
                        onSetDrawerSearchPlacement = onSetDrawerSearchPlacement,
                        onSetDrawerNavigation = onSetDrawerNavigation,
                        onSetDrawerEntryMode = onSetDrawerEntryMode,
                        onSetDrawerSpacing = onSetDrawerSpacing,
                        onSetDrawerPageRows = onSetDrawerPageRows,
                        onSetShowDrawerAppCount = onSetShowDrawerAppCount,
                        onSetHomeGlanceAlignment = onSetHomeGlanceAlignment,
                        onSetHomeSearchPlacement = onSetHomeSearchPlacement,
                        onSetHomeSearchStyle = onSetHomeSearchStyle,
                        onSetHomeSpacing = onSetHomeSpacing,
                        onSetDockStyle = onSetDockStyle,
                        onSetWallpaperShade = onSetWallpaperShade,
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
        if (showDetailedAppOptions || selectedAppAnchor == null) {
            AppPlacementDialog(
                app = app,
                workspace = workspace,
                layoutLocked = preferences.layoutLocked,
                onToggleFavorite = { onToggleFavorite(app) },
                onToggleDock = { onToggleDock(app) },
                onMoveFavorite = { onMoveFavorite(app, it) },
                onMoveDock = { onMoveDock(app, it) },
                homeLabelOverride = homeLabelOverrides[app.workspaceKey()],
                onSetHomeLabelOverride = { onSetHomeLabelOverride(app, it) },
                onOpenAppInfo = { onOpenAppInfo(app) },
                onRequestUninstall = { onRequestUninstall(app) },
                onClose = {
                    selectedApp = null
                    selectedAppAnchor = null
                    showDetailedAppOptions = false
                },
            )
        } else {
            AppContextPopup(
                app = app,
                anchor = selectedAppAnchor!!,
                workspace = workspace,
                layoutLocked = preferences.layoutLocked,
                onToggleFavorite = {
                    onToggleFavorite(app)
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
                onAddToFolder = {
                    selectedApp = null
                    selectedAppAnchor = null
                    folderAssignmentAppKey = app.workspaceKey()
                    if (folders.isEmpty()) {
                        folderManagerAddToHome = false
                        showFolderManager = true
                    }
                },
                onMoreOptions = { showDetailedAppOptions = true },
                onClose = {
                    selectedApp = null
                    selectedAppAnchor = null
                },
            )
        }
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
            onClose = { selectedWidget = null },
        )
    }

    selectedFolderId
        ?.let { id -> folders.firstOrNull { it.id == id } }
        ?.let { folder ->
            LauncherFolderContentsSheet(
                folder = folder,
                appsByKey = rootAppsByKey,
                isOnHome = folder.id in homeFolderIds,
                onLaunchApp = onLaunchApp,
                onRemoveApp = { app -> onRemoveAppFromFolder(folder.id, app) },
                onAddApps = {
                    selectedFolderId = null
                    folderAppPickerId = folder.id
                },
                onRename = { name -> onRenameFolder(folder.id, name) },
                onAddToHome = { onAddFolderToHome(folder) },
                onRemoveFromHome = { onRemoveFolderFromHome(folder) },
                moveTargets = homePages.filter { page ->
                    page.folderPlacements.none { placement -> placement.folderId == folder.id }
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
            LauncherFolderAppPickerSheet(
                folder = folder,
                availableApps = rootAppsByKey.values.toList(),
                onAddApp = { app -> onAddAppToFolder(folder.id, app) },
                onDismiss = {
                    folderAppPickerId = null
                    selectedFolderId = folder.id
                },
            )
        }

    if (showFolderManager) {
        LauncherFolderManagerSheet(
            folders = folders,
            appsByKey = rootAppsByKey,
            homeFolderIds = homeFolderIds,
            defaultAddToHome = folderManagerAddToHome,
            onCreate = { name, addToHome ->
                val initialApp = folderAssignmentAppKey?.let(rootAppsByKey::get)
                onCreateFolder(name, addToHome, initialApp)
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
            LauncherFolderAssignmentSheet(
                app = app,
                folders = folders,
                onAssign = { folder ->
                    onAddAppToFolder(folder.id, app)
                    folderAssignmentAppKey = null
                },
                onCreateFolder = {
                    folderManagerAddToHome = false
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
    recentAppKeys: List<String>,
    localLaunchCounts: Map<String, Long>,
    homePageCount: Int,
    homeEditorRequestSequence: Long,
    homeLabelOverrides: Map<String, String>,
    folders: List<LauncherFolder>,
    primaryHomePage: WorkspaceRenderedHomePage?,
    editMode: Boolean,
    activeDrag: LauncherAppDragData?,
    dragPoint: Offset?,
    homeCellBounds: MutableMap<Pair<Int, Int>, Rect>,
    dockItemBounds: MutableMap<String, Rect>,
    onDockBoundsChanged: (Rect) -> Unit,
    onBeginLocalDrag: (LauncherAppDragData, Offset) -> Unit,
    onUpdateLocalDrag: (Offset) -> Unit,
    onEndLocalDrag: (LauncherAppDragData, Offset) -> Unit,
    onCancelLocalDrag: () -> Unit,
    onExitEditMode: () -> Unit,
    onManageHomePages: () -> Unit,
    onManageFolders: () -> Unit,
    onOpenFolder: (LauncherFolder) -> Unit,
    onMoveFavoriteToCell: (LauncherActivityInfo, Int, Int) -> Unit,
    onMoveWidget: (WorkspaceRenderedHomeWidget, Int, Int) -> Unit,
    onMoveHomeFolderToCell: (LauncherFolder, Int, Int) -> Unit,
    onLaunchApp: (LauncherActivityInfo) -> Unit,
    onAddBuiltInWidget: (String) -> Unit,
    availableAndroidWidgets: List<LauncherWidgetProviderDescriptor>,
    onPickInstalledAndroidWidget: (LauncherWidgetProviderDescriptor) -> Unit,
    onPickAndroidWidget: () -> Unit,
    onCreateAndroidWidgetView: (Int) -> AppWidgetHostView?,
    onManageWidget: (WorkspaceRenderedHomeWidget) -> Unit,
    onOpenLauncherSearch: () -> Unit,
    onManageApp: (LauncherActivityInfo, Rect?) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenThemeManager: () -> Unit,
    onOpenWallpaperPicker: () -> Unit,
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
    ) {
        val excluded = workspace.favoriteKeys.toSet() + workspace.dockKeys.toSet()
        val keys = when (experiencePreferences.homeAppMode) {
            LauncherHomeAppMode.NONE -> emptyList()
            LauncherHomeAppMode.RECENT -> recentAppKeys
            LauncherHomeAppMode.MOST_USED -> localLaunchCounts.entries
                .sortedWith(
                    compareByDescending<Map.Entry<String, Long>> { it.value }
                        .thenBy { it.key },
                )
                .map { it.key }
        }
        keys.asSequence()
            .filterNot(excluded::contains)
            .mapNotNull(appsByKey::get)
            .filterNot { it.componentName.packageName.contains("launcher", ignoreCase = true) }
            .distinctBy { it.workspaceKey() }
            .take(10)
            .toList()
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

    val swipeThreshold = with(LocalDensity.current) { 56.dp.toPx() }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var showHomeEditor by rememberSaveable { mutableStateOf(false) }
    var showWidgetPicker by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(homeEditorRequestSequence) {
        if (homeEditorRequestSequence > 0L) {
            showHomeEditor = true
        }
    }

    val executeGestureAction: (LauncherGestureAction) -> Unit = { action ->
        when (action.type) {
            LauncherGestureActionType.NONE -> Unit
            LauncherGestureActionType.APPS -> onOpenDrawer()
            LauncherGestureActionType.UNIVERSAL_SEARCH -> onOpenLauncherSearch()
            LauncherGestureActionType.LAUNCHER_SETTINGS -> showHomeEditor = true
            LauncherGestureActionType.HOME_EDITOR -> showHomeEditor = true
            LauncherGestureActionType.WALLPAPER -> onOpenWallpaperPicker()
            LauncherGestureActionType.THEME_MANAGER -> onOpenThemeManager()
            LauncherGestureActionType.OPEN_APP -> {
                action.appKey
                    ?.let(appsByKey::get)
                    ?.let(onLaunchApp)
            }
        }
    }
    val currentGesturePreferences by rememberUpdatedState(experiencePreferences)
    val currentExecuteGestureAction by rememberUpdatedState(executeGestureAction)

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
    val showPermanentSearch = preferences.universalSearchHomeMode == LauncherUniversalSearchHomeMode.PERMANENT
    val searchAtTop =
        experiencePreferences.homeSearchPlacement == LauncherHomeSearchPlacement.TOP
    val openSearch = onOpenLauncherSearch

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag(
                "launcher-home-swipe-up-" +
                    experiencePreferences.swipeUpAction.storageValue,
            )
            .pointerInput(swipeThreshold) {
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
                                drag >= swipeThreshold -> {
                                    triggered = true
                                    currentExecuteGestureAction(
                                        currentGesturePreferences.swipeDownAction,
                                    )
                                }
                                drag <= -swipeThreshold -> {
                                    triggered = true
                                    currentExecuteGestureAction(
                                        currentGesturePreferences.swipeUpAction,
                                    )
                                }
                            }
                        }
                    },
                )
            },
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .testTag("launcher-home-empty-space-actions")
                .pointerInput(swipeThreshold) {
                    detectTapGestures(
                        onDoubleTap = {
                            currentExecuteGestureAction(
                                currentGesturePreferences.doubleTapAction,
                            )
                        },
                        onLongPress = {
                            showHomeEditor = true
                        },
                    )
                }
                .pointerInput(swipeThreshold) {
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
                                        currentExecuteGestureAction(
                                            currentGesturePreferences.swipeRightAction,
                                        )
                                    }
                                    drag <= -swipeThreshold -> {
                                        triggered = true
                                        currentExecuteGestureAction(
                                            currentGesturePreferences.swipeLeftAction,
                                        )
                                    }
                                }
                            }
                        },
                    )
                },
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
                .navigationBarsPadding()
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
                                "Drop on a Home cell or in the Dock"
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
                )
            }

            if (showPermanentSearch && searchAtTop) {
                GlazeSearchCapsule(
                    value = "Search GoreeCloud",
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
                    iconScale = preferences.iconScale,
                    showLabels = experiencePreferences.showHomeLabels,
                    spacing = experiencePreferences.homeSpacing,
                    layoutLocked = preferences.layoutLocked,
                    homeLabelOverrides = homeLabelOverrides,
                    cellBounds = homeCellBounds,
                    editMode = editMode,
                    activeDrag = activeDrag,
                    dragPoint = dragPoint,
                    onBeginLocalDrag = onBeginLocalDrag,
                    onUpdateLocalDrag = onUpdateLocalDrag,
                    onEndLocalDrag = onEndLocalDrag,
                    onCancelLocalDrag = onCancelLocalDrag,
                    onLaunchApp = onLaunchApp,
                    onManageApp = onManageApp,
                    onCreateAndroidWidgetView = onCreateAndroidWidgetView,
                    onManageWidget = onManageWidget,
                    onMoveWidget = onMoveWidget,
                    onMoveHomeFolderToCell = onMoveHomeFolderToCell,
                    onOpenFolder = onOpenFolder,
                    onOpenWidgetSearch = openSearch,
                    onOpenWidgetApps = onOpenDrawer,
                    onOpenWidgetEditor = { showHomeEditor = true },
                    onOpenWidgetSettings = onOpenSettings,
                    onSwipeUp = {
                        executeGestureAction(experiencePreferences.swipeUpAction)
                    },
                    onSwipeDown = {
                        executeGestureAction(experiencePreferences.swipeDownAction)
                    },
                )
            }

            if (showPermanentSearch && !searchAtTop) {
                GlazeSearchCapsule(
                    value = "Search GoreeCloud",
                    style = experiencePreferences.homeSearchStyle,
                    onClick = openSearch,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (dockApps.isNotEmpty() || activeDrag != null) {
                GlazeDock(
                    apps = dockApps,
                    iconScale = preferences.iconScale,
                    style = experiencePreferences.dockStyle,
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
                    onManageApp = onManageApp,
                    onSwipeUp = {
                        executeGestureAction(experiencePreferences.swipeUpAction)
                    },
                    onSwipeDown = {
                        executeGestureAction(experiencePreferences.swipeDownAction)
                    },
                )
            }

            Spacer(Modifier.height(2.dp))
        }

        if (showHomeEditor) {
            ModalBottomSheet(
                onDismissRequest = { showHomeEditor = false },
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                tonalElevation = 0.dp,
            ) {
                HomeEditorSheet(
                    now = now,
                    favoriteApps = favoriteApps,
                    dockApps = dockApps,
                    preferences = preferences,
                    experiencePreferences = experiencePreferences,
                    homePageCount = homePageCount,
                    onWallpaper = {
                        showHomeEditor = false
                        onOpenWallpaperPicker()
                    },
                    onPages = {
                        showHomeEditor = false
                        onManageHomePages()
                    },
                    onWidgets = {
                        showHomeEditor = false
                        showWidgetPicker = true
                    },
                    onFolders = {
                        showHomeEditor = false
                        onManageFolders()
                    },
                    onApps = {
                        showHomeEditor = false
                        onOpenDrawer()
                    },
                    onSettings = {
                        showHomeEditor = false
                        onOpenSettings()
                    },
                )
            }
        }

        if (showWidgetPicker) {
            ModalBottomSheet(
                onDismissRequest = { showWidgetPicker = false },
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                tonalElevation = 0.dp,
            ) {
                LauncherWidgetPickerSheet(
                    availableAndroidWidgets = availableAndroidWidgets,
                    onAddBuiltInWidget = { typeId ->
                        showWidgetPicker = false
                        onAddBuiltInWidget(typeId)
                    },
                    onPickInstalledAndroidWidget = { descriptor ->
                        showWidgetPicker = false
                        onPickInstalledAndroidWidget(descriptor)
                    },
                    onPickAndroidWidget = {
                        showWidgetPicker = false
                        onPickAndroidWidget()
                    },
                )
            }
        }
    }
}

@Composable
private fun LauncherWidgetPickerSheet(
    availableAndroidWidgets: List<LauncherWidgetProviderDescriptor>,
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
            .fillMaxWidth()
            .testTag("launcher-widget-picker-sheet")
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = GlazeMetrics.space4, vertical = GlazeMetrics.space3),
        verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space3),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Choose widget",
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "GoreeCloud widgets and installed Android widgets in one Launcher gallery.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("launcher-widget-search-field"),
            singleLine = true,
            label = { Text("Search widgets") },
            placeholder = { Text("GoreeCloud widget, app, or package") },
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
            filteredBuiltIns.chunked(2).forEach { row ->
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
                    if (row.size == 1) Spacer(Modifier.weight(1f))
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
                    onClick = { onPickInstalledAndroidWidget(descriptor) },
                )
            }
        }

        OutlinedButton(
            onClick = onPickAndroidWidget,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Open Android widget picker")
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
    val glyph = when (typeId) {
        WorkspaceWidgetCatalog.CLOCK -> "12:34"
        WorkspaceWidgetCatalog.COMPACT_CLOCK -> "12:34"
        WorkspaceWidgetCatalog.ANALOG_CLOCK -> "◷"
        WorkspaceWidgetCatalog.DATE -> "23"
        WorkspaceWidgetCatalog.SEARCH -> "⌕"
        WorkspaceWidgetCatalog.QUICK_ACTIONS -> "•••"
        WorkspaceWidgetCatalog.BATTERY -> "78%"
        WorkspaceWidgetCatalog.LAUNCHER_STATUS -> "GC"
        else -> "•"
    }
    Surface(
        modifier = modifier.testTag("launcher-widget-built-in-$typeId"),
        onClick = onClick,
        shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
    ) {
        Column(
            modifier = Modifier.padding(GlazeMetrics.space3),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        glyph,
                        modifier = Modifier.clearAndSetSemantics { },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Text(
                WorkspaceWidgetCatalog.displayName(typeId),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                WorkspaceWidgetCatalog.description(typeId),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "${span.first} × ${span.second}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun InstalledWidgetPickerRow(
    descriptor: LauncherWidgetProviderDescriptor,
    onClick: () -> Unit,
) {
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
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        descriptor.label.take(1).uppercase(Locale.getDefault()),
                        modifier = Modifier.clearAndSetSemantics { },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    descriptor.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    descriptor.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (descriptor.minWidth > 0 && descriptor.minHeight > 0) {
                Text(
                    "${descriptor.minWidth} × ${descriptor.minHeight}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LauncherWidgetManagementDialog(
    widget: WorkspaceRenderedHomeWidget,
    columns: Int,
    rows: Int,
    layoutLocked: Boolean,
    onResize: (Int, Int) -> Unit,
    onRemove: () -> Unit,
    onMove: (Int, Int) -> Unit,
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
private fun HomeEditorSheet(
    now: LocalDateTime,
    favoriteApps: List<LauncherActivityInfo>,
    dockApps: List<LauncherActivityInfo>,
    preferences: LauncherPreferences,
    experiencePreferences: LauncherExperiencePreferences,
    homePageCount: Int,
    onWallpaper: () -> Unit,
    onPages: () -> Unit,
    onWidgets: () -> Unit,
    onFolders: () -> Unit,
    onApps: () -> Unit,
    onSettings: () -> Unit,
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Edit Home",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    homePageCount.toString() + if (homePageCount == 1) " Home page" else " Home pages",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "Long-press Home anytime",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        HomeEditorPreview(
            now = now,
            favoriteApps = favoriteApps,
            dockApps = dockApps,
            preferences = preferences,
            experiencePreferences = experiencePreferences,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
        ) {
            HomeEditorAction("Wallpaper", "◫", onWallpaper, Modifier.weight(1f))
            HomeEditorAction("Widgets", "▤", onWidgets, Modifier.weight(1f))
            HomeEditorAction("Pages", "▣", onPages, Modifier.weight(1f))
            HomeEditorAction("Apps", "▦", onApps, Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
        ) {
            HomeEditorAction("Folders", "▦", onFolders, Modifier.weight(1f))
            HomeEditorAction("Settings", "⚙", onSettings, Modifier.weight(1f))
            Spacer(Modifier.weight(2f))
        }
        Spacer(Modifier.height(GlazeMetrics.space1))
    }
}

@Composable
private fun HomeEditorPreview(
    now: LocalDateTime,
    favoriteApps: List<LauncherActivityInfo>,
    dockApps: List<LauncherActivityInfo>,
    preferences: LauncherPreferences,
    experiencePreferences: LauncherExperiencePreferences,
) {
    val locale = Locale.getDefault()
    val time = remember(now.minute, locale) {
        now.format(DateTimeFormatter.ofPattern("h:mm", locale))
    }
    val date = remember(now.dayOfYear, locale) {
        now.format(DateTimeFormatter.ofPattern("EEE, MMM d", locale))
    }
    val searchAtTop =
        experiencePreferences.homeSearchPlacement == LauncherHomeSearchPlacement.TOP
    val showSearch = preferences.universalSearchHomeMode == LauncherUniversalSearchHomeMode.PERMANENT

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(330.dp),
        shape = RoundedCornerShape(34.dp),
        color = GlazeAtmosphere.canvasBlack.copy(alpha = 0.40f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(GlazeMetrics.space4),
            verticalArrangement = Arrangement.spacedBy(
                when (experiencePreferences.homeSpacing) {
                    LauncherHomeSpacing.COMPACT -> GlazeMetrics.space1
                    LauncherHomeSpacing.BALANCED -> GlazeMetrics.space2
                    LauncherHomeSpacing.AIRY -> GlazeMetrics.space3
                },
            ),
        ) {
            if (experiencePreferences.homeCardStyle != LauncherHomeCardStyle.OFF) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (
                        experiencePreferences.homeGlanceAlignment == LauncherHomeGlanceAlignment.CENTER
                    ) Alignment.CenterHorizontally else Alignment.Start,
                ) {
                    Text(
                        time,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Light,
                        color = Color.White,
                    )
                    Text(
                        date,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.78f),
                    )
                }
            }

            if (showSearch && searchAtTop) {
                HomeEditorPreviewSearch(experiencePreferences.homeSearchStyle)
            }

            Spacer(Modifier.weight(1f))

            if (favoriteApps.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    favoriteApps.take(preferences.homeColumns.coerceAtMost(5)).forEach { app ->
                        HomeEditorPreviewIcon(app)
                    }
                }
            }

            if (showSearch && !searchAtTop) {
                HomeEditorPreviewSearch(experiencePreferences.homeSearchStyle)
            }

            if (dockApps.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = if (experiencePreferences.dockStyle == LauncherDockStyle.EDGE) {
                        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 10.dp, bottomEnd = 10.dp)
                    } else {
                        RoundedCornerShape(GlazeMetrics.radiusExtraLarge)
                    },
                    color = when (experiencePreferences.dockStyle) {
                        LauncherDockStyle.CLEAR -> Color.Transparent
                        LauncherDockStyle.GLASS -> GlazeAtmosphere.canvasBlack.copy(alpha = 0.24f)
                        LauncherDockStyle.EDGE -> GlazeAtmosphere.canvasBlack.copy(alpha = 0.40f)
                    },
                    border = if (experiencePreferences.dockStyle == LauncherDockStyle.CLEAR) {
                        null
                    } else {
                        BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        dockApps.take(MAX_DOCK_ITEMS).forEach { app ->
                            HomeEditorPreviewIcon(app)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeEditorPreviewSearch(style: LauncherHomeSearchStyle) {
    val fill = when (style) {
        LauncherHomeSearchStyle.GLASS -> Color.White.copy(alpha = 0.14f)
        LauncherHomeSearchStyle.CLEAR -> Color.White.copy(alpha = 0.05f)
        LauncherHomeSearchStyle.SOLID -> Color.White.copy(alpha = 0.90f)
    }
    val foreground = if (style == LauncherHomeSearchStyle.SOLID) {
        GlazeAtmosphere.canvasBlack.copy(alpha = 0.88f)
    } else {
        Color.White.copy(alpha = 0.84f)
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp),
        shape = RoundedCornerShape(GlazeMetrics.radiusPill),
        color = fill,
        border = BorderStroke(
            1.dp,
            if (style == LauncherHomeSearchStyle.CLEAR) {
                Color.White.copy(alpha = 0.16f)
            } else {
                Color.White.copy(alpha = 0.08f)
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = GlazeMetrics.space3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space1),
        ) {
            Text("⌕", color = foreground)
            Text(
                "Search phone",
                style = MaterialTheme.typography.labelSmall,
                color = foreground.copy(alpha = 0.86f),
            )
        }
    }
}

@Composable
private fun HomeEditorPreviewIcon(app: LauncherActivityInfo) {
    val icon = rememberLauncherAppIcon(app)
    if (icon != null) {
        Image(
            bitmap = icon,
            contentDescription = null,
            contentScale = ContentScale.Fit,
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
private fun HomeEditorAction(
    label: String,
    glyph: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(glyph, style = MaterialTheme.typography.titleLarge)
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
private fun HomeAtAGlance(
    now: LocalDateTime,
    compact: Boolean,
    alignment: LauncherHomeGlanceAlignment,
) {
    val locale = Locale.getDefault()
    val time = remember(now.minute, locale) {
        now.format(DateTimeFormatter.ofPattern("h:mm", locale))
    }
    val date = remember(now.dayOfYear, locale) {
        now.format(DateTimeFormatter.ofPattern("EEE, MMM d", locale))
    }
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
    val glanceShadow = Shadow(
        color = Color.Black.copy(alpha = 0.48f),
        offset = Offset(0f, 2f),
        blurRadius = 7f,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (compact) GlazeMetrics.space2 else GlazeMetrics.space3,
                vertical = if (compact) GlazeMetrics.space1 else GlazeMetrics.space2,
            ),
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
            style = (
                if (compact) MaterialTheme.typography.headlineMedium
                else MaterialTheme.typography.displayMedium
            ).copy(shadow = glanceShadow),
            color = Color.White,
            fontWeight = FontWeight.Light,
            textAlign = textAlign,
        )
        Text(
            date,
            modifier = if (alignment == LauncherHomeGlanceAlignment.CENTER) {
                Modifier.fillMaxWidth()
            } else {
                Modifier
            },
            style = MaterialTheme.typography.bodyMedium.copy(shadow = glanceShadow),
            color = Color.White.copy(alpha = 0.92f),
            textAlign = textAlign,
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
    iconScale: Float,
    showLabels: Boolean,
    spacing: LauncherHomeSpacing,
    layoutLocked: Boolean,
    homeLabelOverrides: Map<String, String>,
    cellBounds: MutableMap<Pair<Int, Int>, Rect>,
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
    onMoveHomeFolderToCell: (LauncherFolder, Int, Int) -> Unit,
    onOpenFolder: (LauncherFolder) -> Unit,
    onOpenWidgetSearch: () -> Unit,
    onOpenWidgetApps: () -> Unit,
    onOpenWidgetEditor: () -> Unit,
    onOpenWidgetSettings: () -> Unit,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
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
        modifier = Modifier
            .fillMaxWidth()
            .height(tileHeight * rows + gridSpacing * (rows - 1)),
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
                    .size(width = cellWidth, height = tileHeight),
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
                        widgetTargetBounds.entries.firstOrNull { (_, bounds) ->
                            bounds.contains(point)
                        }?.key?.let { (cellX, cellY) ->
                            onMoveHomeFolderToCell(selected, cellX, cellY)
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
                        widgetTargetBounds.entries.firstOrNull { (_, bounds) ->
                            bounds.contains(point)
                        }?.key?.let { (cellX, cellY) ->
                            onMoveWidget(candidate, cellX, cellY)
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
    Column(
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
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(modifier = Modifier.size(folderIconSize)) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = folderShape,
                color = folderBackground,
                border = if (previewOutline) BorderStroke(
                    1.dp,
                    if (labelOnWallpaper) Color.White.copy(alpha = if (editMode) 0.38f else 0.24f)
                    else MaterialTheme.colorScheme.outlineVariant,
                ) else null,
            ) {
                if (previewApps.isEmpty()) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "＋",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (labelOnWallpaper) Color.White.copy(alpha = 0.78f)
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        val spots = when (previewLayout) {
                            com.goreecloud.launcher.core.launcher.LauncherFolderPreviewLayout.GRID ->
                                listOf(
                                    (-11).dp to (-11).dp, 11.dp to (-11).dp,
                                    (-11).dp to 11.dp, 11.dp to 11.dp,
                                )
                            com.goreecloud.launcher.core.launcher.LauncherFolderPreviewLayout.RADIAL ->
                                listOf(
                                    0.dp to (-13).dp, 13.dp to 0.dp,
                                    0.dp to 13.dp, (-13).dp to 0.dp,
                                )
                            com.goreecloud.launcher.core.launcher.LauncherFolderPreviewLayout.STACK ->
                                listOf(
                                    (-9).dp to (-9).dp, (-3).dp to (-3).dp,
                                    3.dp to 3.dp, 9.dp to 9.dp,
                                )
                            com.goreecloud.launcher.core.launcher.LauncherFolderPreviewLayout.FAN ->
                                listOf(
                                    (-15).dp to 7.dp, (-5).dp to (-6).dp,
                                    5.dp to (-6).dp, 15.dp to 7.dp,
                                )
                            com.goreecloud.launcher.core.launcher.LauncherFolderPreviewLayout.LINE ->
                                listOf(
                                    (-17).dp to 0.dp, (-6).dp to 0.dp,
                                    6.dp to 0.dp, 17.dp to 0.dp,
                                )
                        }
                        val previewIconSize = when (previewLayout) {
                            com.goreecloud.launcher.core.launcher.LauncherFolderPreviewLayout.STACK ->
                                20.dp
                            com.goreecloud.launcher.core.launcher.LauncherFolderPreviewLayout.LINE ->
                                13.dp
                            else -> 17.dp
                        }
                        previewApps.forEachIndexed { index, app ->
                            key(app.workspaceKey()) {
                                val icon = rememberLauncherAppIcon(app)
                                val position = spots[index]
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .offset(x = position.first, y = position.second)
                                        .size(previewIconSize),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (icon != null) Image(
                                        bitmap = icon,
                                        contentDescription = null,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize().launcherIconMask(),
                                    ) else Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                                RoundedCornerShape(5.dp),
                                            ),
                                    )
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
        if (showLabel) {
            Spacer(Modifier.height(4.dp))
            Text(
                folder.name,
                modifier = Modifier.fillMaxWidth(),
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
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
private fun HomeWidgetTile(
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
    Box(
        modifier = modifier
            .graphicsLayer {
                translationX = dragDelta.x
                translationY = dragDelta.y
                alpha = if (startRoot != null) 0.80f else 1f
            }
            .onGloballyPositioned { tileBounds = it.boundsInRoot() }
            .padding(2.dp)
            .then(
                if (!editMode) Modifier.observeLongPressWithoutConsuming { onManageWidget(widget) }
                else Modifier
            )
            .then(
                if (editMode) {
                    Modifier.border(
                        1.dp,
                        Color.White.copy(alpha = 0.24f),
                        RoundedCornerShape(GlazeMetrics.radiusLarge),
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
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
                        color = GlazeAtmosphere.canvasBlack.copy(alpha = 0.34f),
                    ) {
                        Box(
                            modifier = Modifier.padding(GlazeMetrics.space3),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "Widget unavailable",
                                color = Color.White.copy(alpha = 0.78f),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
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
                                startRoot?.let { origin -> onDropWidget(widget, origin + dragDelta) }
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

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
        color = GlazeAtmosphere.canvasBlack.copy(alpha = 0.34f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
    ) {
        when (typeId) {
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
                            color = Color.White.copy(alpha = 0.58f),
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
                        color = Color.White,
                        fontWeight = FontWeight.Light,
                        maxLines = 1,
                    )
                    Surface(
                        shape = RoundedCornerShape(GlazeMetrics.radiusPill),
                        color = Color.White.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
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
                                color = Color.White.copy(alpha = 0.80f),
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
                        color = Color.White,
                        fontWeight = FontWeight.Light,
                    )
                    Text(
                        now.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault())),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.72f),
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
                            color = Color.White.copy(alpha = 0.12f),
                            radius = radius,
                            center = center,
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.82f),
                            radius = 3.dp.toPx(),
                            center = center,
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.92f),
                            start = center,
                            end = Offset(
                                x = center.x + cos(hourRadians).toFloat() * radius * 0.52f,
                                y = center.y + sin(hourRadians).toFloat() * radius * 0.52f,
                            ),
                            strokeWidth = 4.dp.toPx(),
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.84f),
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
                        color = Color.White,
                        fontWeight = FontWeight.Light,
                    )
                    Column {
                        Text(
                            now.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault())),
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            now.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.72f),
                        )
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
                        Text(
                            "⌕",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Search GoreeCloud",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                            Text(
                                "Apps, contacts, files & actions",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.70f),
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
                        GlazeActionChip("Apps", onOpenApps, Modifier.weight(1f))
                        GlazeActionChip("Search", onOpenSearch, Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                    ) {
                        GlazeActionChip("Edit Home", onOpenHomeEditor, Modifier.weight(1f))
                        GlazeActionChip("Settings", onOpenSettings, Modifier.weight(1f))
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
                            color = Color.White,
                            fontWeight = FontWeight.Light,
                        )
                        Text(
                            if (battery.charging) "Charging" else "Battery",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.70f),
                        )
                    }
                    Text(
                        if (battery.charging) "⚡" else "▰",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
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
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Home ready · local-first",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.76f),
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
                        color = Color.White.copy(alpha = 0.78f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

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
) {
    val icon = rememberLauncherAppIcon(app)
    val iconSize = (50f * iconScale.coerceIn(0.85f, 1.15f)).dp
    val moveThreshold = with(LocalDensity.current) { 14.dp.toPx() }
    val swipeThreshold = with(LocalDensity.current) { 42.dp.toPx() }
    var dragging by remember(app.componentName, app.user) { mutableStateOf(false) }
    var dragOffset by remember(app.componentName, app.user) { mutableStateOf(Offset.Zero) }
    var tileBounds by remember(app.componentName, app.user) { mutableStateOf<Rect?>(null) }
    var dragStartCenter by remember(app.componentName, app.user) { mutableStateOf<Offset?>(null) }

    val gestureModifier = if (layoutLocked || dragData == null) {
        Modifier.combinedClickable(
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
            .pointerInput(onSwipeUp, onSwipeDown, swipeThreshold) {
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
            .clickable(enabled = !dragging) { onLaunchApp(app) }
    }

    Column(
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
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(modifier = Modifier.size(iconSize)) {
            if (icon != null) {
                Image(
                    bitmap = icon,
                    contentDescription = displayLabel,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().launcherIconMask(),
                )
            } else {
                Surface(
                    modifier = Modifier.fillMaxSize().launcherIconMask(),
                    shape = RoundedCornerShape(15.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(displayLabel.take(1).uppercase(), fontWeight = FontWeight.Bold)
                    }
                }
            }
            LauncherAppBadgeMark(
                app,
                modifier = launcherBadgePositionModifier(),
            )
        }

        if (showLabel) {
            Spacer(Modifier.height(3.dp))
            Text(
                displayLabel,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelSmall.copy(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.60f),
                        offset = Offset(0f, 1.5f),
                        blurRadius = 5f,
                    ),
                ),
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
        GlazeActionChip("Apps", onOpenApps, Modifier.weight(1f))
        GlazeActionChip("Search", onOpenSearch, Modifier.weight(1f))
    }
}

@Composable
private fun GlazeActionChip(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(GlazeMetrics.radiusPill),
        color = GlazeAtmosphere.canvasBlack.copy(alpha = 0.20f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = GlazeMetrics.space2, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.92f),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun EmptyWorkspaceCard(
    onOpenApps: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(GlazeMetrics.radiusExtraLarge),
        color = GlazeAtmosphere.canvasBlack.copy(alpha = 0.20f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GlazeMetrics.space3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space3),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Add apps to Home",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Long-press an app to place it on Home or in the dock.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.74f),
                )
            }
            GlazeTextAction("Apps", onOpenApps)
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
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
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
        shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.64f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GlazeMetrics.space3, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space3),
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        when (result.category) {
                            LauncherSearchCategory.APPLICATION -> "◫"
                            LauncherSearchCategory.SHORTCUT -> "↗"
                            LauncherSearchCategory.CONTACT -> "●"
                            LauncherSearchCategory.CALL_HISTORY -> "☎"
                            LauncherSearchCategory.MESSAGE -> "✉"
                            LauncherSearchCategory.FILE -> "▤"
                            LauncherSearchCategory.CONNECTED_SOURCE -> "⌕"
                            LauncherSearchCategory.SETTING -> "⚙"
                            LauncherSearchCategory.ACTION -> "→"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    result.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                result.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
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
            )
        }
    }
}

private sealed interface LauncherDrawerVisualEntry {
    val label: String
    val stableKey: String

    data class Application(val app: LauncherActivityInfo) : LauncherDrawerVisualEntry {
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
): List<LauncherDrawerVisualEntry> = buildList {
    apps.forEach { add(LauncherDrawerVisualEntry.Application(it)) }
    folders.forEach { add(LauncherDrawerVisualEntry.Folder(it)) }
}.let { entries ->
    LauncherDrawerSortingPolicy.order(
        entries = entries,
        label = { it.label },
        key = { it.stableKey },
    )
}

@Composable
private fun LauncherDrawerVisualTile(
    entry: LauncherDrawerVisualEntry,
    allApps: List<LauncherActivityInfo>,
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
            onOpen = { onOpenFolder(entry.folder) },
            modifier = modifier.testTag("launcher-drawer-inline-folder-" + entry.folder.id),
        )
    }
}

@Composable
@Suppress("UNUSED_PARAMETER")
private fun AppDrawerSurface(
    apps: List<LauncherActivityInfo>,
    folders: List<LauncherFolder>,
    preferences: LauncherPreferences,
    drawerLayoutMode: LauncherDrawerLayoutMode,
    experiencePreferences: LauncherExperiencePreferences,
    focusSearch: Boolean,
    onLaunchApp: (LauncherActivityInfo) -> Unit,
    onManageApp: (LauncherActivityInfo, Rect?) -> Unit,
    onOpenFolder: (LauncherFolder) -> Unit,
    onManageFolders: () -> Unit,
    onHome: () -> Unit,
) {
    val primaryUser = remember { Process.myUserHandle() }
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
    LaunchedEffect(profilePager.currentPage, profilePages.map { it.kind }) {
        selectedProfileName = selectedPage.kind.name
    }

    val dismissThreshold = with(LocalDensity.current) { 56.dp.toPx() }
    val glass = experiencePreferences.drawerBackdrop == LauncherDrawerBackdrop.GLASS
    val drawerSurfaceColor = if (glass) {
        GlazeAtmosphere.canvasBlack.copy(alpha = 0.76f)
    } else {
        MaterialTheme.colorScheme.background
    }
    val drawerSecondaryColor = if (glass) {
        Color.White.copy(alpha = 0.68f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val layoutDescription = when (drawerLayoutMode) {
        LauncherDrawerLayoutMode.GRID -> "Grid · ${preferences.drawerColumns} columns"
        LauncherDrawerLayoutMode.COMPACT -> "Compact · ${preferences.drawerColumns} columns"
        LauncherDrawerLayoutMode.LIST -> "Alphabetical list"
        LauncherDrawerLayoutMode.CATEGORY -> "Grouped by app category"
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            selectedPage.kind.displayName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            if (experiencePreferences.showDrawerAppCount) {
                                selectedPage.items.size.toString() + " installed · " + layoutDescription
                            } else layoutDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = drawerSecondaryColor,
                        )
                    }
                    if (selectedPage.kind == LauncherDrawerProfileKind.USER) {
                        TextButton(
                            onClick = onManageFolders,
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) { Text("+ Folder") }
                    }
                }
                if (profilePages.size > 1) {
                    Spacer(Modifier.height(GlazeMetrics.space3))
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
                HorizontalPager(
                    state = profilePager,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                        .testTag("launcher-drawer-profile-pager"),
                    userScrollEnabled = profilePages.size > 1,
                ) { index ->
                    val page = profilePages[index]
                    val pageFolders =
                        if (page.kind == LauncherDrawerProfileKind.USER) folders else emptyList()
                    if (page.items.isEmpty() && pageFolders.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "No apps are available in " + page.kind.displayName + ".",
                                color = drawerSecondaryColor,
                            )
                        }
                    } else {
                        DrawerAppsContent(
                            apps = page.items,
                            folders = pageFolders,
                            query = "",
                            preferences = preferences,
                            drawerLayoutMode = drawerLayoutMode,
                            experiencePreferences = experiencePreferences,
                            onLaunchApp = onLaunchApp,
                            onManageApp = onManageApp,
                            onOpenFolder = onOpenFolder,
                            onDismiss = onHome,
                            secondaryColor = drawerSecondaryColor,
                            allowHorizontalPaging = profilePages.size == 1,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
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
    query: String,
    preferences: LauncherPreferences,
    drawerLayoutMode: LauncherDrawerLayoutMode,
    experiencePreferences: LauncherExperiencePreferences,
    onLaunchApp: (LauncherActivityInfo) -> Unit,
    onManageApp: (LauncherActivityInfo, Rect?) -> Unit,
    onOpenFolder: (LauncherFolder) -> Unit,
    onDismiss: () -> Unit,
    secondaryColor: Color,
    allowHorizontalPaging: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val entries = remember(apps, folders) { orderedDrawerVisualEntries(apps, folders) }
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
    // Reserve room for two-line labels and scaled icons; never clip a row at large icon sizes.
    val iconGrowth = if (preferences.iconScale > 1.0f) 8.dp else 0.dp
    val gridTileHeight = when (experiencePreferences.drawerSpacing) {
        LauncherDrawerSpacing.TIGHT -> 88.dp
        LauncherDrawerSpacing.STANDARD -> 98.dp
        LauncherDrawerSpacing.RELAXED -> 108.dp
    } + iconGrowth
    val compactTileHeight = when (experiencePreferences.drawerSpacing) {
        LauncherDrawerSpacing.TIGHT -> 72.dp
        LauncherDrawerSpacing.STANDARD -> 80.dp
        LauncherDrawerSpacing.RELAXED -> 88.dp
    } + iconGrowth

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
                    items(pageItems, key = { it.stableKey }) { entry ->
                        LauncherDrawerVisualTile(
                            entry = entry,
                            allApps = apps,
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
            val gridState = rememberLazyGridState()
            val dismissConnection = rememberDrawerDismissNestedScrollConnection(
                canScrollBackward = { gridState.canScrollBackward },
                onDismiss = onDismiss,
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(preferences.drawerColumns),
                state = gridState,
                modifier = modifier
                    .fillMaxWidth()
                    .nestedScroll(dismissConnection),
                contentPadding = PaddingValues(vertical = standardSpacing),
                horizontalArrangement = Arrangement.spacedBy(standardSpacing),
                verticalArrangement = Arrangement.spacedBy(standardSpacing),
            ) {
                items(entries, key = { it.stableKey }) { entry ->
                    LauncherDrawerVisualTile(
                        entry = entry,
                        allApps = apps,
                        iconScale = preferences.iconScale,
                        showLabel = experiencePreferences.showDrawerLabels,
                        compact = false,
                        layoutLocked = preferences.layoutLocked,
                        onLaunchApp = onLaunchApp,
                        onManageApp = onManageApp,
                        onOpenFolder = onOpenFolder,
                        modifier = Modifier.height(gridTileHeight),
                    )
                }
            }
        }
        LauncherDrawerLayoutMode.COMPACT -> {
            val gridState = rememberLazyGridState()
            val dismissConnection = rememberDrawerDismissNestedScrollConnection(
                canScrollBackward = { gridState.canScrollBackward },
                onDismiss = onDismiss,
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(preferences.drawerColumns),
                state = gridState,
                modifier = modifier
                    .fillMaxWidth()
                    .nestedScroll(dismissConnection),
                contentPadding = PaddingValues(vertical = compactSpacing),
                horizontalArrangement = Arrangement.spacedBy(compactSpacing),
                verticalArrangement = Arrangement.spacedBy(compactSpacing),
            ) {
                items(entries, key = { it.stableKey }) { entry ->
                    LauncherDrawerVisualTile(
                        entry = entry,
                        allApps = apps,
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
        LauncherDrawerLayoutMode.LIST -> {
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
                lazyItems(entries, key = { it.stableKey }) { entry ->
                    when (entry) {
                        is LauncherDrawerVisualEntry.Application -> LauncherAppListRow(
                            app = entry.app,
                            iconScale = preferences.iconScale,
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
        LauncherDrawerLayoutMode.CATEGORY -> {
            // Explicitly selected category view still needs a dense single grid when folders
            // are present. Never put folders in a giant standalone heading/row above apps.
            if (folders.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(preferences.drawerColumns),
                    modifier = modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(standardSpacing),
                    verticalArrangement = Arrangement.spacedBy(standardSpacing),
                    contentPadding = PaddingValues(vertical = standardSpacing),
                ) {
                    items(entries, key = { it.stableKey }) { entry ->
                        LauncherDrawerVisualTile(
                            entry = entry,
                            allApps = apps,
                            iconScale = preferences.iconScale,
                            showLabel = experiencePreferences.showDrawerLabels,
                            compact = false,
                            layoutLocked = preferences.layoutLocked,
                            onLaunchApp = onLaunchApp,
                            onManageApp = onManageApp,
                            onOpenFolder = onOpenFolder,
                            modifier = Modifier.height(gridTileHeight),
                        )
                    }
                }
            } else {
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
                            key = { row ->
                                "category:$category:" + row.first().workspaceKey()
                            },
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
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { page ->
            Surface(
                onClick = { onSelectPage(page) },
                modifier = Modifier.size(if (page == currentPage) 8.dp else 6.dp),
                shape = CircleShape,
                color = if (page == currentPage) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
                },
            ) {}
        }
    }
}


@Composable
@Suppress("UNUSED_PARAMETER")
private fun LauncherSettingsRootSurface(
    apps: List<LauncherActivityInfo>,
    preferences: LauncherPreferences,
    drawerLayoutMode: LauncherDrawerLayoutMode,
    experiencePreferences: LauncherExperiencePreferences,
    availableIconPacks: List<LauncherIconPackDescriptor>,
    themeMode: GlazeThemeMode,
    isDefaultHome: Boolean,
    onRequestHomeRole: () -> Unit,
    onManageFolders: () -> Unit,
    onSetHomeGrid: (Int, Int) -> Unit,
    onSetDrawerColumns: (Int) -> Unit,
    onSetDrawerLayoutMode: (LauncherDrawerLayoutMode) -> Unit,
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
    onShowHintsAgain: () -> Unit,
    onSetDrawerBackdrop: (LauncherDrawerBackdrop) -> Unit,
    onSetDrawerSearchPlacement: (LauncherDrawerSearchPlacement) -> Unit,
    onSetDrawerNavigation: (LauncherDrawerNavigation) -> Unit,
    onSetDrawerEntryMode: (LauncherDrawerEntryMode) -> Unit,
    onSetDrawerSpacing: (LauncherDrawerSpacing) -> Unit,
    onSetDrawerPageRows: (Int) -> Unit,
    onSetShowDrawerAppCount: (Boolean) -> Unit,
    onSetHomeGlanceAlignment: (LauncherHomeGlanceAlignment) -> Unit,
    onSetHomeSearchPlacement: (LauncherHomeSearchPlacement) -> Unit,
    onSetHomeSearchStyle: (LauncherHomeSearchStyle) -> Unit,
    onSetHomeSpacing: (LauncherHomeSpacing) -> Unit,
    onSetDockStyle: (LauncherDockStyle) -> Unit,
    onSetWallpaperShade: (LauncherWallpaperShade) -> Unit,
    onSetGestureAction: (LauncherHomeGesture, LauncherGestureAction) -> Unit,
    onOpenThemeManager: () -> Unit,
    onBack: () -> Unit,
) {
    var gestureToConfigure by remember { mutableStateOf<LauncherHomeGesture?>(null) }
    var showIconPackPicker by rememberSaveable { mutableStateOf(false) }
    val badgeContext = androidx.compose.ui.platform.LocalContext.current
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
                        "Launcher settings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Home, apps, dock, search and Glaze",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                GlazeTextAction("Done", onBack)
            }

            SettingsSection("Home screen", "Layout and glance content") {
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
                    "Automatic Home apps",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                ChoiceRow(
                    choices = listOf("No apps", "10 recent", "10 most used"),
                    selected = when (experiencePreferences.homeAppMode) {
                        LauncherHomeAppMode.NONE -> "No apps"
                        LauncherHomeAppMode.RECENT -> "10 recent"
                        LauncherHomeAppMode.MOST_USED -> "10 most used"
                    },
                    onChoice = {
                        onSetHomeAppMode(
                            when (it) {
                                "10 recent" -> LauncherHomeAppMode.RECENT
                                "10 most used" -> LauncherHomeAppMode.MOST_USED
                                else -> LauncherHomeAppMode.NONE
                            },
                        )
                    },
                )
                GlazeSettingsAction(
                    title = "Local app activity",
                    summary = "Recent and most-used modes store only Launcher launch counts and a bounded recent order on this device; no timestamps, dwell time, or Android Usage Access.",
                    value = "Clear",
                    onClick = onClearLocalUsage,
                )
                GlazeSettingsAction(
                    title = "Launcher hints",
                    summary = "Show the dismissible Home guide for gestures, Search, editing, and drag-and-drop again.",
                    value = "Show again",
                    onClick = onShowHintsAgain,
                )
                SettingSwitch(
                    "Add new apps to Home",
                    experiencePreferences.addNewAppsToHome,
                    onSetAddNewAppsToHome,
                )
                SettingSwitch("Lock layout", preferences.layoutLocked, onSetLayoutLocked)
            }

            SettingsSection("Dock", "Bottom-row apps and material") {
                Text(
                    "Dock material",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                ChoiceRow(
                    choices = listOf("Glass", "Clear", "Edge"),
                    selected = when (experiencePreferences.dockStyle) {
                        LauncherDockStyle.GLASS -> "Glass"
                        LauncherDockStyle.CLEAR -> "Clear"
                        LauncherDockStyle.EDGE -> "Edge"
                    },
                    onChoice = {
                        onSetDockStyle(
                            when (it) {
                                "Clear" -> LauncherDockStyle.CLEAR
                                "Edge" -> LauncherDockStyle.EDGE
                                else -> LauncherDockStyle.GLASS
                            },
                        )
                    },
                )
                SettingsReadOnlyRow("Capacity", "Up to 5 apps")
                SettingsReadOnlyRow("Edit", "Long-press an app")
            }

            SettingsSection("Search", "Home access and Launcher Universal Search") {
                ChoiceRow(
                    choices = listOf("Gesture only", "Show bar"),
                    selected = if (preferences.universalSearchHomeMode == LauncherUniversalSearchHomeMode.PERMANENT) "Show bar" else "Gesture only",
                    onChoice = {
                        onSetUniversalSearchHomeMode(
                            if (it == "Show bar") LauncherUniversalSearchHomeMode.PERMANENT
                            else LauncherUniversalSearchHomeMode.SWIPE_DOWN_ONLY,
                        )
                    },
                )
                if (preferences.universalSearchHomeMode == LauncherUniversalSearchHomeMode.PERMANENT) {
                    Text(
                        "Home bar position",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    ChoiceRow(
                        choices = listOf("Top", "Bottom"),
                        selected = if (
                            experiencePreferences.homeSearchPlacement == LauncherHomeSearchPlacement.TOP
                        ) "Top" else "Bottom",
                        onChoice = {
                            onSetHomeSearchPlacement(
                                if (it == "Top") LauncherHomeSearchPlacement.TOP
                                else LauncherHomeSearchPlacement.BOTTOM,
                            )
                        },
                    )
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
                SettingsReadOnlyRow("Home gestures", "Configured in Gestures")
                SettingsReadOnlyRow("Core provider", "Installed apps · Launcher")
            }

            SettingsSection("App drawer", "Profiles, layout, density and background") {
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
            }

            SettingsSection("Folders", "Create, organize and customize") {
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
                        "Long-press a Home folder to drag it to another free cell.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SettingsSection("Icons", "Shape, size and icon packs") {
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

            SettingsSection("Notification badges", "Local unread indicators and privacy") {
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
                        if (badgeAccess) "Android notification access is enabled."
                        else "Tap Grant access to activate badges. Android must approve it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!badgeAccess) GlazeSettingsAction(
                        title = "Grant access",
                        summary = "Opens Android's notification access controls.",
                        value = "Android settings",
                        onClick = {
                            runCatching {
                                badgeContext.startActivity(
                                    android.content.Intent(
                                        android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS,
                                    ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            }
                        },
                    )
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

            SettingsSection("Appearance", "Glaze theme and wallpaper treatment") {
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
                "Gestures",
                "Assign Home gestures to Launcher actions or installed apps",
            ) {
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

            SettingsSection("System", "Default HOME and Development status") {
                if (!isDefaultHome) {
                    GlazeSettingsAction(
                        title = "Default Home app",
                        summary = "Use GoreeCloud Launcher for the Home gesture",
                        value = "Set Home",
                        onClick = onRequestHomeRole,
                    )
                } else {
                    SettingsReadOnlyRow("Default Home app", "GoreeCloud Launcher")
                }
                SettingsReadOnlyRow("Build channel", "Development")
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
    content: @Composable ColumnScope.() -> Unit,
) {
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
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
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
    val solid = style == LauncherHomeSearchStyle.SOLID
    val foreground = if (solid) {
        MaterialTheme.colorScheme.onSurface
    } else {
        Color.White
    }
    val background = when (style) {
        LauncherHomeSearchStyle.GLASS -> GlazeAtmosphere.canvasBlack.copy(alpha = 0.34f)
        LauncherHomeSearchStyle.CLEAR -> GlazeAtmosphere.canvasBlack.copy(alpha = 0.12f)
        LauncherHomeSearchStyle.SOLID -> MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    }
    val outline = when (style) {
        LauncherHomeSearchStyle.GLASS -> Color.White.copy(alpha = 0.14f)
        LauncherHomeSearchStyle.CLEAR -> Color.White.copy(alpha = 0.24f)
        LauncherHomeSearchStyle.SOLID -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    }
    val leadingFill = when (style) {
        LauncherHomeSearchStyle.GLASS -> Color.White.copy(alpha = 0.13f)
        LauncherHomeSearchStyle.CLEAR -> Color.White.copy(alpha = 0.06f)
        LauncherHomeSearchStyle.SOLID -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    }
    val height = if (style == LauncherHomeSearchStyle.CLEAR) 50.dp else 54.dp

    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(GlazeMetrics.radiusPill),
        color = background,
        border = BorderStroke(1.dp, outline),
        shadowElevation = if (style == LauncherHomeSearchStyle.CLEAR) 0.dp else 3.dp,
    ) {
        Row(
            modifier = Modifier.height(height).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier.size(if (style == LauncherHomeSearchStyle.CLEAR) 32.dp else 36.dp),
                shape = CircleShape,
                color = leadingFill,
                border = if (style == LauncherHomeSearchStyle.CLEAR) null else {
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
                color = foreground.copy(alpha = if (solid) 0.86f else 0.90f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (style != LauncherHomeSearchStyle.CLEAR) {
                Text(
                    "›",
                    modifier = Modifier.padding(end = 10.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = foreground.copy(alpha = 0.72f),
                    maxLines = 1,
                )
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

    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(GlazeMetrics.radiusExtraLarge),
        color = if (darkSurface) Color.White.copy(alpha = 0.10f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
        border = BorderStroke(
            1.dp,
            if (darkSurface) Color.White.copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
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
                .height(54.dp)
                .focusRequester(focusRequester)
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

@Composable
private fun GlazeDock(
    apps: List<LauncherActivityInfo>,
    iconScale: Float,
    style: LauncherDockStyle,
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
        RoundedCornerShape(GlazeMetrics.radius2ExtraLarge)
    }
    val color = when (style) {
        LauncherDockStyle.CLEAR -> Color.Transparent
        LauncherDockStyle.GLASS -> GlazeAtmosphere.canvasBlack.copy(alpha = 0.24f)
        LauncherDockStyle.EDGE -> GlazeAtmosphere.canvasBlack.copy(alpha = 0.40f)
    }
    var measuredBounds by remember { mutableStateOf<Rect?>(null) }
    val dockHovered = activeDrag != null &&
        dragPoint?.let { point -> measuredBounds?.contains(point) } == true
    val border = when {
        dockHovered -> BorderStroke(2.dp, Color.White.copy(alpha = 0.58f))
        style == LauncherDockStyle.CLEAR -> null
        else -> BorderStroke(
            1.dp,
            Color.White.copy(alpha = if (style == LauncherDockStyle.EDGE) 0.12f else 0.10f),
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned {
                val bounds = it.boundsInRoot()
                measuredBounds = bounds
                onDockBoundsChanged(bounds)
            },
        shape = shape,
        color = color,
        border = border,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (style == LauncherDockStyle.EDGE) 78.dp else 72.dp)
                .padding(horizontal = GlazeMetrics.space2),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (apps.isEmpty() && activeDrag != null) {
                Text(
                    "Drop in Dock",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.76f),
                )
            } else {
                apps.forEach { app ->
                    val appKey = app.workspaceKey()
                    Box(
                        modifier = Modifier.onGloballyPositioned {
                            dockItemBounds[appKey] = it.boundsInRoot()
                        },
                    ) {
                        HomeFavoriteTile(
                            app = app,
                            displayLabel = app.label.toString(),
                            iconScale = iconScale,
                            showLabel = false,
                            layoutLocked = layoutLocked,
                            editMode = editMode,
                            dragData = if (layoutLocked) null else {
                                LauncherAppDragData(
                                    appKey = appKey,
                                    origin = LauncherAppDragOrigin.DOCK,
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
                            modifier = Modifier.size(60.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LauncherAppTile(
    app: LauncherActivityInfo,
    iconScale: Float,
    showLabel: Boolean,
    compact: Boolean,
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
    val swipeThreshold = with(LocalDensity.current) { 42.dp.toPx() }
    var tileBounds by remember(app.componentName, app.user) { mutableStateOf<Rect?>(null) }
    val dragModifier = if (dragData != null) {
        Modifier.dragAndDropSource(transferData = { _ -> dragData.toTransferData() })
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
            .combinedClickable(onClick = onClick, onLongClick = { onLongClick(tileBounds) })
            .padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(modifier = Modifier.size(iconSize)) {
            if (icon != null) {
                Image(
                    bitmap = icon,
                    contentDescription = app.label.toString(),
                    contentScale = ContentScale.Fit,
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
        }

        if (showLabel) {
            Spacer(Modifier.height(if (compact) 3.dp else 4.dp))
            Text(
                app.label.toString(),
                modifier = Modifier.fillMaxWidth(),
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
                maxLines = if (compact) 1 else 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LauncherAppListRow(
    app: LauncherActivityInfo,
    iconScale: Float,
    onClick: () -> Unit,
    onLongClick: (Rect?) -> Unit,
    dragData: LauncherAppDragData? = null,
) {
    val icon = rememberLauncherAppIcon(app)
    val iconSize = (44f * iconScale.coerceIn(0.85f, 1.15f)).dp
    var rowBounds by remember(app.componentName, app.user) { mutableStateOf<Rect?>(null) }
    val dragModifier = if (dragData != null) {
        Modifier.dragAndDropSource(transferData = { _ -> dragData.toTransferData() })
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(dragModifier)
            .onGloballyPositioned { rowBounds = it.boundsInRoot() }
            .combinedClickable(onClick = onClick, onLongClick = { onLongClick(rowBounds) })
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = app.label.toString(),
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(iconSize).launcherIconMask(),
            )
        } else {
            Surface(
                modifier = Modifier.size(iconSize).launcherIconMask(),
                shape = RoundedCornerShape(13.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(app.label.toString().take(1).uppercase(), fontWeight = FontWeight.Bold)
                }
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
private fun LauncherFolderManagerSheet(
    folders: List<LauncherFolder>,
    appsByKey: Map<String, LauncherActivityInfo>,
    homeFolderIds: Set<String>,
    defaultAddToHome: Boolean,
    onCreate: (String, Boolean) -> Unit,
    onOpen: (LauncherFolder) -> Unit,
    onAddToHome: (LauncherFolder) -> Unit,
    onRemoveFromHome: (LauncherFolder) -> Unit,
    onDismiss: () -> Unit,
) {
    var nameDraft by rememberSaveable { mutableStateOf("") }
    var addToHome by rememberSaveable(defaultAddToHome) { mutableStateOf(defaultAddToHome) }

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
                        Text(
                            "▦",
                            style = MaterialTheme.typography.titleLarge,
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
                        leadingIcon = { Text("▦") },
                    )
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
                                        Text(
                                            "▦",
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.titleMedium,
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
                                TextButton(
                                    onClick = {
                                        if (folder.id in homeFolderIds) {
                                            onRemoveFromHome(folder)
                                        } else {
                                            onAddToHome(folder)
                                        }
                                    },
                                ) {
                                    Text(if (folder.id in homeFolderIds) "Remove Home" else "Add Home")
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

/** Glaze floating folder: a compact three-column icon panel, not a full-screen management sheet. */
@Composable
internal fun LauncherFolderContentsSheet(
    folder: LauncherFolder,
    appsByKey: Map<String, LauncherActivityInfo>,
    isOnHome: Boolean,
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
    val members = remember(folder, appsByKey, alphabetical) {
        folder.appKeys.mapNotNull(appsByKey::get).let { apps ->
            if (alphabetical) apps.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) {
                it.label.toString()
            }) else apps
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.90f)
                .widthIn(max = 460.dp)
                .testTag("launcher-glaze-folder-popup"),
            shape = RoundedCornerShape(GlazeMetrics.radius2ExtraLarge),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 20.dp,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(GlazeMetrics.space3),
                verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space1),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            folder.name,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${members.size} apps" + if (alphabetical) " · A–Z view" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(
                        onClick = { selectMode = !selectMode; selectedKeys = emptySet() },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) { Text(if (selectMode) "Done" else "Select") }
                    Box {
                        TextButton(
                            onClick = { showActions = true },
                            modifier = Modifier.heightIn(min = 48.dp)
                                .semantics { contentDescription = "Folder actions" },
                        ) { Text("⋮", style = MaterialTheme.typography.titleLarge) }
                        DropdownMenu(
                            expanded = showActions,
                            onDismissRequest = { showActions = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isOnHome) "Remove from Home" else "Add to Home") },
                                onClick = {
                                    showActions = false
                                    if (isOnHome) onRemoveFromHome() else onAddToHome()
                                },
                            )
                            if (isOnHome && moveTargets.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Move to another Home page") },
                                    onClick = {
                                        showActions = false
                                        showMoveTargets = true
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Add apps") },
                                onClick = { showActions = false; onAddApps() },
                            )
                            DropdownMenuItem(
                                text = { Text(if (alphabetical) "Original order" else "Sort A–Z") },
                                onClick = {
                                    alphabetical = !alphabetical
                                    showActions = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Edit folder name") },
                                onClick = { editName = true; showActions = false },
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Delete folder", color = MaterialTheme.colorScheme.error) },
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
                        TextButton(onClick = { nameDraft = folder.name; editName = false }) {
                            Text("Cancel")
                        }
                        TextButton(
                            enabled = nameDraft.trim().isNotBlank(),
                            onClick = {
                                onRename(nameDraft.trim())
                                editName = false
                            },
                        ) { Text("Save") }
                    }
                }

                if (members.isEmpty()) {
                    Text(
                        "This folder is empty. Add apps to get started.",
                        modifier = Modifier.fillMaxWidth().padding(vertical = GlazeMetrics.space4),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 345.dp),
                        horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space1),
                        verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                    ) {
                        items(members, key = { it.workspaceKey() }) { app ->
                            val appKey = app.workspaceKey()
                            val icon = rememberLauncherAppIcon(app)
                            val selected = appKey in selectedKeys
                            Surface(
                                modifier = Modifier.fillMaxWidth().heightIn(min = 106.dp)
                                    .testTag("launcher-folder-app-" + appKey),
                                onClick = {
                                    if (selectMode) {
                                        selectedKeys = if (selected) selectedKeys - appKey
                                        else selectedKeys + appKey
                                    } else {
                                        onDismiss()
                                        onLaunchApp(app)
                                    }
                                },
                                shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant,
                                ),
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = GlazeMetrics.space2, horizontal = 3.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (icon != null) {
                                            Image(
                                                bitmap = icon,
                                                contentDescription = null,
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier.size(47.dp).launcherIconMask(),
                                            )
                                        } else {
                                            Surface(
                                                modifier = Modifier.size(47.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(15.dp),
                                            ) {}
                                        }
                                        if (selectMode && selected) {
                                            Text(
                                                "✓",
                                                modifier = Modifier.align(Alignment.TopEnd)
                                                    .background(
                                                        MaterialTheme.colorScheme.primary,
                                                        CircleShape,
                                                    ).padding(horizontal = 5.dp),
                                                color = MaterialTheme.colorScheme.onPrimary,
                                            )
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
                    }
                }
                if (selectMode) {
                    Text(
                        "${selectedKeys.size} selected · Removing only changes folder membership",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = { confirmRemove = true },
                        enabled = selectedKeys.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) { Text("Remove selected") }
                } else {
                    TextButton(
                        onClick = onAddApps,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) { Text("＋  Add apps") }
                }
            }
        }
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text("Remove ${selectedKeys.size} apps from folder?") },
            text = { Text("The selected apps will stay installed and remain available in the app drawer.") },
            confirmButton = {
                TextButton(onClick = {
                    members.filter { it.workspaceKey() in selectedKeys }.forEach(onRemoveApp)
                    selectedKeys = emptySet()
                    selectMode = false
                    confirmRemove = false
                }) { Text("Remove from folder") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = false }) { Text("Cancel") }
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
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) { Text("Home page ${page.rank + 1}") }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMoveTargets = false }) { Text("Cancel") }
            },
        )
    }
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete ${folder.name}?") },
            text = { Text(
                "This removes the folder and its membership from Home and the app drawer. " +
                    "Installed apps remain. The folder cannot be restored automatically.",
            ) },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirmation = false; onDelete() }) {
                    Text("Delete folder", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancel") }
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
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Search apps") },
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space1),
            ) {
                lazyItems(
                    items = visibleApps,
                    key = { it.workspaceKey() },
                ) { app ->
                    val icon = rememberLauncherAppIcon(app)
                    val alreadyAdded = app.workspaceKey() in folder.appKeys
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onAddApp(app) },
                        enabled = !alreadyAdded && folder.appKeys.size < 100,
                        shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (icon != null) {
                                Image(
                                    bitmap = icon,
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.size(38.dp).launcherIconMask(),
                                )
                            }
                            Text(
                                app.label.toString(),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                if (alreadyAdded) "Added" else "Add",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (alreadyAdded) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                            )
                        }
                    }
                }
            }
            if (visibleApps.isEmpty()) {
                Text(
                    "No matching personal apps.",
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

@Composable
private fun AppContextPopup(
    app: LauncherActivityInfo,
    anchor: Rect,
    workspace: WorkspaceState,
    layoutLocked: Boolean,
    onToggleFavorite: () -> Unit,
    onToggleDock: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onRequestUninstall: () -> Unit,
    onAddToFolder: () -> Unit,
    onMoreOptions: () -> Unit,
    onClose: () -> Unit,
) {
    val key = app.workspaceKey()
    val isFavorite = key in workspace.favoriteKeys
    val isDocked = key in workspace.dockKeys
    val dockFull = !isDocked && workspace.dockKeys.size >= MAX_DOCK_ITEMS
    val canAddToFolder = app.user == Process.myUserHandle()
    val icon = rememberLauncherAppIcon(app)
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
            modifier = Modifier.widthIn(min = 252.dp, max = 300.dp)
                .testTag("launcher-glaze-app-context-menu"),
            shape = RoundedCornerShape(GlazeMetrics.radius2ExtraLarge),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
            ),
            shadowElevation = 18.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = GlazeMetrics.space2, vertical = GlazeMetrics.space2),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(
                        horizontal = GlazeMetrics.space2,
                        vertical = GlazeMetrics.space2,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                ) {
                    if (icon != null) {
                        Image(
                            bitmap = icon,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(38.dp).launcherIconMask(),
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            app.label.toString(),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (layoutLocked) Text(
                            "Home layout locked",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                GlazeLauncherPopupAction(
                    label = if (isFavorite) "Remove from Home" else "Add to Home",
                    symbol = GlazePopupActionSymbol.HOME,
                    onClick = onToggleFavorite,
                    enabled = !layoutLocked,
                )
                GlazeLauncherPopupAction(
                    label = if (isDocked) "Remove from Dock" else "Add to Dock",
                    symbol = GlazePopupActionSymbol.DOCK,
                    onClick = onToggleDock,
                    enabled = !layoutLocked && !dockFull,
                )
                GlazeLauncherPopupAction(
                    label = "Add to folder",
                    symbol = GlazePopupActionSymbol.FOLDER,
                    onClick = onAddToFolder,
                    enabled = canAddToFolder && !layoutLocked,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                GlazeLauncherPopupAction(label = "App info", symbol = GlazePopupActionSymbol.INFO, onClick = onOpenAppInfo)
                GlazeLauncherPopupAction(
                    label = "Uninstall",
                    symbol = GlazePopupActionSymbol.UNINSTALL,
                    onClick = onRequestUninstall,
                    destructive = true,
                )
                GlazeLauncherPopupAction(label = "More options", symbol = GlazePopupActionSymbol.MORE, onClick = onMoreOptions)
            }
        }
    }
}

private enum class GlazePopupActionSymbol { HOME, DOCK, FOLDER, INFO, UNINSTALL, MORE }

/** Decorative vector geometry; labels remain the accessible action description. */
@Composable
private fun GlazePopupActionGlyph(symbol: GlazePopupActionSymbol, color: Color) {
    Canvas(Modifier.size(22.dp)) {
        val u = size.minDimension
        val w = 1.8.dp.toPx()
        fun segment(x1: Float, y1: Float, x2: Float, y2: Float) {
            drawLine(color, Offset(x1 * u, y1 * u), Offset(x2 * u, y2 * u), strokeWidth = w)
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
            GlazePopupActionSymbol.FOLDER -> {
                segment(.10f, .25f, .43f, .25f)
                segment(.43f, .25f, .51f, .36f)
                segment(.51f, .36f, .89f, .36f)
                segment(.89f, .36f, .89f, .80f)
                segment(.89f, .80f, .10f, .80f)
                segment(.10f, .80f, .10f, .25f)
            }
            GlazePopupActionSymbol.INFO -> {
                drawCircle(color, radius = u * .36f, center = Offset(u * .5f, u * .5f), style = Stroke(w))
                drawCircle(color, radius = w * .65f, center = Offset(u * .5f, u * .33f))
                segment(.50f, .47f, .50f, .70f)
            }
            GlazePopupActionSymbol.UNINSTALL -> {
                segment(.24f, .24f, .76f, .76f)
                segment(.76f, .24f, .24f, .76f)
            }
            GlazePopupActionSymbol.MORE -> {
                drawCircle(color, radius = w * .78f, center = Offset(u * .27f, u * .5f))
                drawCircle(color, radius = w * .78f, center = Offset(u * .5f, u * .5f))
                drawCircle(color, radius = w * .78f, center = Offset(u * .73f, u * .5f))
            }
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

@Composable
private fun AppPlacementDialog(
    app: LauncherActivityInfo,
    workspace: WorkspaceState,
    layoutLocked: Boolean,
    onToggleFavorite: () -> Unit,
    onToggleDock: () -> Unit,
    onMoveFavorite: (WorkspaceMoveDirection) -> Unit,
    onMoveDock: (WorkspaceMoveDirection) -> Unit,
    homeLabelOverride: String?,
    onSetHomeLabelOverride: (String?) -> Unit,
    onOpenAppInfo: () -> Unit,
    onRequestUninstall: () -> Unit,
    onClose: () -> Unit,
) {
    val key = app.workspaceKey()
    val favoriteIndex = workspace.favoriteKeys.indexOf(key)
    val dockIndex = workspace.dockKeys.indexOf(key)
    val isFavorite = favoriteIndex >= 0
    val isDocked = dockIndex >= 0
    val dockFull = !isDocked && workspace.dockKeys.size >= MAX_DOCK_ITEMS
    val originalLabel = app.label.toString()
    var labelDraft by remember(key, homeLabelOverride) {
        mutableStateOf(homeLabelOverride ?: originalLabel)
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(homeLabelOverride ?: originalLabel) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (layoutLocked) {
                    Text("Home layout is locked.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(
                    onClick = onToggleFavorite,
                    enabled = !layoutLocked,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (isFavorite) "Remove from Home" else "Add to Home") }
                OutlinedButton(
                    onClick = onToggleDock,
                    enabled = !layoutLocked && !dockFull,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (isDocked) "Remove from Dock" else "Add to Dock") }
                if (isFavorite && !layoutLocked) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { onMoveFavorite(WorkspaceMoveDirection.EARLIER) }) { Text("Earlier") }
                        TextButton(onClick = { onMoveFavorite(WorkspaceMoveDirection.LATER) }) { Text("Later") }
                    }
                }
                if (isDocked && !layoutLocked) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { onMoveDock(WorkspaceMoveDirection.EARLIER) }) { Text("Dock left") }
                        TextButton(onClick = { onMoveDock(WorkspaceMoveDirection.LATER) }) { Text("Dock right") }
                    }
                }

                if (isFavorite) {
                    Text("Rename on Home", fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = labelDraft,
                        onValueChange = { labelDraft = it.take(LauncherHomeLabelPolicy.MAX_LABEL_LENGTH) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        supportingText = { Text("Rename this label on Home only.") },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = {
                                val normalized = LauncherHomeLabelPolicy.normalize(labelDraft)
                                val original = LauncherHomeLabelPolicy.normalize(originalLabel)
                                onSetHomeLabelOverride(normalized?.takeUnless { it == original })
                            },
                        ) { Text("Save label") }
                        TextButton(
                            onClick = {
                                labelDraft = originalLabel
                                onSetHomeLabelOverride(null)
                            },
                        ) { Text("Reset") }
                    }
                }

                HorizontalDivider()
                OutlinedButton(
                    onClick = {
                        onOpenAppInfo()
                        onClose()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("App info") }
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
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Done") } },
    )
}
