package com.goreecloud.launcher.ui

import android.content.pm.LauncherActivityInfo
import android.os.Process
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goreecloud.launcher.core.launcher.LauncherHomeLabelPolicy
import com.goreecloud.launcher.core.launcher.LauncherFolder
import com.goreecloud.launcher.core.workspace.WorkspaceMoveDirection
import com.goreecloud.launcher.core.workspace.db.WorkspaceHomeSpatialDirection
import com.goreecloud.launcher.core.workspace.db.WorkspaceLegacyImportMapper
import com.goreecloud.launcher.core.workspace.db.WorkspaceRenderedHomePage
import com.goreecloud.launcher.core.workspace.db.context
import com.goreecloud.launcher.core.workspace.workspaceKey
import com.goreecloud.launcher.ui.theme.GlazeMetrics

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

    val canDelete = !layoutLocked && pages.size > 1 &&
        selectedPage != null &&
        selectedPage.pageId != WorkspaceLegacyImportMapper.HOME_PAGE_ID &&
        selectedPage.appKeys.isEmpty() &&
        selectedPage.unsupportedItemCount == 0
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
fun HomePageDots(
    pages: List<WorkspaceRenderedHomePage>,
    selectedPageId: String,
    onSelectPage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (pages.size <= 1) return

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(GlazeMetrics.radiusPill),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.44f),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = GlazeMetrics.space2,
                vertical = GlazeMetrics.space1,
            ),
            horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space1),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            pages.forEach { page ->
                val selected = page.pageId == selectedPageId
                Surface(
                    modifier = Modifier
                        .size(if (selected) 9.dp else 7.dp)
                        .clickable { onSelectPage(page.pageId) },
                    shape = CircleShape,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.34f)
                    },
                ) {}
            }
        }
    }
}

private sealed interface PagedHomeVisualItem {
    val key: String
    val cellOrder: Int

    data class App(
        val info: LauncherActivityInfo,
        override val cellOrder: Int,
    ) : PagedHomeVisualItem {
        override val key: String = "app:" + info.workspaceKey()
    }

    data class Folder(
        val info: LauncherFolder,
        override val cellOrder: Int,
    ) : PagedHomeVisualItem {
        override val key: String = "folder:" + info.id
    }
}

@Composable
fun ReadOnlyPagedHomeSurface(
    apps: List<LauncherActivityInfo>,
    folders: List<LauncherFolder>,
    page: WorkspaceRenderedHomePage,
    pages: List<WorkspaceRenderedHomePage>,
    homeColumns: Int,
    showLabels: Boolean,
    iconScale: Float,
    homeLabelOverrides: Map<String, String>,
    onLaunchApp: (LauncherActivityInfo) -> Unit,
    onSetHomeLabelOverride: (LauncherActivityInfo, String?) -> Unit,
    onRequestUninstall: (LauncherActivityInfo) -> Unit,
    onMoveAppToPage: (LauncherActivityInfo, String) -> Unit,
    onMoveAppWithinPage: (LauncherActivityInfo, WorkspaceMoveDirection) -> Unit,
    onMoveAppOneCell: (LauncherActivityInfo, WorkspaceHomeSpatialDirection) -> Unit,
    onRenameFolder: (String, String) -> Unit,
    onDeleteFolder: (LauncherFolder) -> Unit,
    onAddAppToFolder: (String, LauncherActivityInfo) -> Unit,
    onRemoveAppFromFolder: (String, LauncherActivityInfo) -> Unit,
    onRemoveFolderFromHome: (LauncherFolder) -> Unit,
    onMoveFolderToPage: (LauncherFolder, String) -> Unit,
    layoutLocked: Boolean = false,
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
    val visualItems = remember(pageApps, pageFolders, page.appPlacements, page.folderPlacements, homeColumns) {
        val appPlacements = page.appPlacements.associateBy { it.appKey }
        val folderPlacements = page.folderPlacements.associateBy { it.folderId }
        buildList<PagedHomeVisualItem> {
            pageApps.forEachIndexed { index, app ->
                val placement = appPlacements[app.workspaceKey()]
                add(PagedHomeVisualItem.App(
                    app,
                    if (placement?.cellX != null && placement.cellY != null) {
                        placement.cellY * homeColumns + placement.cellX
                    } else 100_000 + index,
                ))
            }
            pageFolders.forEach { folder ->
                val placement = folderPlacements[folder.id]
                if (placement != null) add(PagedHomeVisualItem.Folder(
                    folder,
                    placement.cellY * homeColumns + placement.cellX,
                ))
            }
        }.sortedWith(compareBy({ it.cellOrder }, { it.key }))
    }
    var openedFolderId by remember(page.pageId) { mutableStateOf<String?>(null) }
    var pickingFolderId by remember(page.pageId) { mutableStateOf<String?>(null) }
    val targetPages = remember(pages, page.pageId) {
        pages.filterNot {
            it.pageId == page.pageId || it.pageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID
        }
    }

    // The activity window asks Android to draw the system wallpaper underneath Launcher.
    // This surface remains translucent and does not read wallpaper files directly.
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.08f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
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
                        "${page.unsupportedItemCount} item${if (page.unsupportedItemCount == 1) "" else "s"} on this page still need folder, shortcut, or widget rendering support.",
                        modifier = Modifier.padding(GlazeMetrics.space3),
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            if (visualItems.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        shape = RoundedCornerShape(GlazeMetrics.radiusExtraLarge),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                    ) {
                        Text(
                            "This Home page is empty.",
                            modifier = Modifier.padding(GlazeMetrics.space5),
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(homeColumns.coerceIn(4, 6)),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = GlazeMetrics.space4),
                    horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space1),
                    verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space3),
                ) {
                    items(visualItems, key = { it.key }) { entry ->
                        when (entry) {
                            is PagedHomeVisualItem.App -> {
                                val app = entry.info
                                PagedAppTile(
                                    app = app,
                                    displayLabel = homeLabelOverrides[app.workspaceKey()]
                                        ?: app.label.toString(),
                                    homeLabelOverride = homeLabelOverrides[app.workspaceKey()],
                                    showLabel = showLabels,
                                    iconScale = iconScale,
                                    targetPages = targetPages,
                                    layoutLocked = layoutLocked,
                                    onLaunchApp = onLaunchApp,
                                    onSetHomeLabelOverride = { label ->
                                        onSetHomeLabelOverride(app, label)
                                    },
                                    onRequestUninstall = { onRequestUninstall(app) },
                                    onMoveAppToPage = onMoveAppToPage,
                                    onMoveAppWithinPage = onMoveAppWithinPage,
                                    onMoveAppOneCell = onMoveAppOneCell,
                                )
                            }
                            is PagedHomeVisualItem.Folder -> HomeFolderTile(
                                folder = entry.info,
                                allApps = personalApps,
                                showLabel = showLabels,
                                editMode = false,
                                onOpen = { openedFolderId = entry.info.id },
                                modifier = Modifier.fillMaxWidth().height(96.dp),
                            )
                        }
                    }
                }
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
    targetPages: List<WorkspaceRenderedHomePage>,
    layoutLocked: Boolean,
    onLaunchApp: (LauncherActivityInfo) -> Unit,
    onSetHomeLabelOverride: (String?) -> Unit,
    onRequestUninstall: () -> Unit,
    onMoveAppToPage: (LauncherActivityInfo, String) -> Unit,
    onMoveAppWithinPage: (LauncherActivityInfo, WorkspaceMoveDirection) -> Unit,
    onMoveAppOneCell: (LauncherActivityInfo, WorkspaceHomeSpatialDirection) -> Unit,
) {
    val icon = rememberLauncherAppIcon(app)
    var manageOpen by remember(app.componentName, app.user) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .combinedClickable(
                onClick = { onLaunchApp(app) },
                onLongClick = { manageOpen = true },
            )
            .padding(GlazeMetrics.space1),
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
                            Text(target.context().moveTargetLabel(target.rank + 1))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Done") } },
    )
}
