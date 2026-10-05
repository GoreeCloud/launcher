package com.goreecloud.launcher.ui

import android.appwidget.AppWidgetHostView
import android.content.pm.LauncherActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import com.goreecloud.launcher.core.launcher.LauncherDockStyle
import com.goreecloud.launcher.core.launcher.LauncherFolder
import com.goreecloud.launcher.core.launcher.LauncherHomePageTransition
import com.goreecloud.launcher.core.workspace.db.WorkspaceHomeSpatialDirection
import com.goreecloud.launcher.core.workspace.WorkspaceMoveDirection
import com.goreecloud.launcher.core.workspace.db.WorkspaceRenderedHomePage
import com.goreecloud.launcher.core.workspace.db.WorkspaceRenderedHomeWidget

@Composable
internal fun PersistentHomeDock(
    apps: List<LauncherActivityInfo>,
    iconScale: Float,
    style: LauncherDockStyle,
    onLaunchApp: (LauncherActivityInfo) -> Unit,
    pageSize: Int = 5,
    loopPages: Boolean = false,
    showLabels: Boolean = false,
    showSearch: Boolean = false,
    onOpenSearch: () -> Unit = {},
) {
    val itemBounds = remember(apps) { mutableMapOf<String, Rect>() }
    GlazeDock(
        apps = apps,
        iconScale = iconScale,
        style = style,
        pageSize = pageSize,
        loopPages = loopPages,
        showLabels = showLabels,
        showSearch = showSearch,
        layoutLocked = true,
        editMode = false,
        activeDrag = null,
        dragPoint = null,
        onDockBoundsChanged = {},
        dockItemBounds = itemBounds,
        onBeginLocalDrag = { _, _ -> },
        onUpdateLocalDrag = {},
        onEndLocalDrag = { _, _ -> },
        onCancelLocalDrag = {},
        onLaunchApp = onLaunchApp,
        onManageApp = { _, _ -> },
        onOpenSearch = onOpenSearch,
        onSwipeUp = {},
        onSwipeDown = {},
    )
}


/**
 * Compatibility overload for secondary Home callers that use the widget-editor action as the
 * Home-editor action. The full overload owns the explicit wallpaper editor callback.
 */
@Composable
internal fun ReadOnlyPagedHomeSurface(
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
    onOpenWidgetSettings: () -> Unit,
    dockApps: List<LauncherActivityInfo> = emptyList(),
    dockStyle: LauncherDockStyle = LauncherDockStyle.GLASS,
    pageTransition: LauncherHomePageTransition = LauncherHomePageTransition.SLIDE,
    showPageIndicator: Boolean = true,
    onSelectPage: (String) -> Unit = {},
    layoutLocked: Boolean = false,
    onGridBoundsChanged: (Rect?) -> Unit = {},
) {
    ReadOnlyPagedHomeSurface(
        apps = apps,
        folders = folders,
        page = page,
        pages = pages,
        homeColumns = homeColumns,
        homeRows = homeRows,
        showLabels = showLabels,
        iconScale = iconScale,
        homeLabelOverrides = homeLabelOverrides,
        onLaunchApp = onLaunchApp,
        onSetHomeLabelOverride = onSetHomeLabelOverride,
        onRequestUninstall = onRequestUninstall,
        onMoveAppToPage = onMoveAppToPage,
        onMoveAppToPageCell = onMoveAppToPageCell,
        onMoveAppToCell = onMoveAppToCell,
        onMoveAppWithinPage = onMoveAppWithinPage,
        onMoveAppOneCell = onMoveAppOneCell,
        onRenameFolder = onRenameFolder,
        onDeleteFolder = onDeleteFolder,
        onAddAppToFolder = onAddAppToFolder,
        onRemoveAppFromFolder = onRemoveAppFromFolder,
        onRemoveFolderFromHome = onRemoveFolderFromHome,
        onMoveFolderToPage = onMoveFolderToPage,
        onMoveFolderToPageCell = onMoveFolderToPageCell,
        onMoveFolderToCell = onMoveFolderToCell,
        onCreateAndroidWidgetView = onCreateAndroidWidgetView,
        onRemoveWidget = onRemoveWidget,
        onResizeWidget = onResizeWidget,
        onMoveWidgetToCell = onMoveWidgetToCell,
        onMoveWidgetToPage = onMoveWidgetToPage,
        onMoveWidgetToPageCell = onMoveWidgetToPageCell,
        onOpenWidgetSearch = onOpenWidgetSearch,
        onOpenWidgetApps = onOpenWidgetApps,
        onOpenWidgetEditor = onOpenWidgetEditor,
        onOpenHomeEditor = onOpenWidgetEditor,
        onOpenWidgetSettings = onOpenWidgetSettings,
        dockApps = dockApps,
        dockStyle = dockStyle,
        pageTransition = pageTransition,
        showPageIndicator = showPageIndicator,
        onSelectPage = onSelectPage,
        layoutLocked = layoutLocked,
        onGridBoundsChanged = onGridBoundsChanged,
    )
}
