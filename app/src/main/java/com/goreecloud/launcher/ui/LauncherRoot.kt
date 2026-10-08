package com.goreecloud.launcher.ui

import android.content.pm.LauncherActivityInfo
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.graphics.drawable.toBitmap
import com.goreecloud.launcher.core.workspace.MAX_DOCK_ITEMS
import com.goreecloud.launcher.core.workspace.WorkspaceMoveDirection
import com.goreecloud.launcher.core.workspace.WorkspaceState
import com.goreecloud.launcher.core.workspace.workspaceKey
import com.goreecloud.launcher.ui.theme.GlazeMetrics
import com.goreecloud.launcher.ui.theme.GlazeThemeMode

@Composable
fun LauncherRoot(
    apps: List<LauncherActivityInfo>,
    workspace: WorkspaceState,
    isDefaultHome: Boolean,
    onRequestHomeRole: () -> Unit,
    onLaunchApp: (LauncherActivityInfo) -> Unit,
    onToggleFavorite: (LauncherActivityInfo) -> Unit,
    onToggleDock: (LauncherActivityInfo) -> Unit,
    onMoveFavorite: (LauncherActivityInfo, WorkspaceMoveDirection) -> Unit,
    onMoveDock: (LauncherActivityInfo, WorkspaceMoveDirection) -> Unit,
    onMoveFavoriteToTarget: (LauncherActivityInfo, String) -> Unit,
    onMoveDockToTarget: (LauncherActivityInfo, String) -> Unit,
    themeMode: GlazeThemeMode,
    onCycleTheme: (GlazeThemeMode) -> Unit,
) {
    var drawerOpen by remember { mutableStateOf(false) }
    var selectedApp by remember { mutableStateOf<LauncherActivityInfo?>(null) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        AnimatedContent(drawerOpen, label = "launcher_surface") { drawer ->
            if (drawer) {
                AppDrawer(
                    apps = apps,
                    onLaunchApp = onLaunchApp,
                    onManageApp = { selectedApp = it },
                    onClose = { drawerOpen = false },
                )
            } else {
                HomeSurface(
                    apps = apps,
                    workspace = workspace,
                    isDefaultHome = isDefaultHome,
                    onRequestHomeRole = onRequestHomeRole,
                    onOpenDrawer = { drawerOpen = true },
                    onLaunchApp = onLaunchApp,
                    onManageApp = { selectedApp = it },
                    onMoveFavoriteToTarget = onMoveFavoriteToTarget,
                    onMoveDockToTarget = onMoveDockToTarget,
                    themeMode = themeMode,
                    onCycleTheme = onCycleTheme,
                )
            }
        }
    }

    selectedApp?.let { app ->
        AppPlacementDialog(
            app = app,
            workspace = workspace,
            onToggleFavorite = { onToggleFavorite(app) },
            onToggleDock = { onToggleDock(app) },
            onMoveFavorite = { onMoveFavorite(app, it) },
            onMoveDock = { onMoveDock(app, it) },
            onClose = { selectedApp = null },
        )
    }
}

@Composable
private fun HomeSurface(
    apps: List<LauncherActivityInfo>,
    workspace: WorkspaceState,
    isDefaultHome: Boolean,
    onRequestHomeRole: () -> Unit,
    onOpenDrawer: () -> Unit,
    onLaunchApp: (LauncherActivityInfo) -> Unit,
    onManageApp: (LauncherActivityInfo) -> Unit,
    onMoveFavoriteToTarget: (LauncherActivityInfo, String) -> Unit,
    onMoveDockToTarget: (LauncherActivityInfo, String) -> Unit,
    themeMode: GlazeThemeMode,
    onCycleTheme: (GlazeThemeMode) -> Unit,
) {
    val appsByKey = remember(apps) { apps.associateBy { it.workspaceKey() } }
    val favoriteApps = remember(appsByKey, workspace.favoriteKeys) {
        workspace.favoriteKeys.mapNotNull(appsByKey::get)
    }
    val dockApps = remember(appsByKey, workspace.dockKeys) {
        workspace.dockKeys.mapNotNull(appsByKey::get)
    }

    var reorderMode by rememberSaveable { mutableStateOf(false) }
    val favoriteBounds = remember { mutableStateMapOf<String, Rect>() }
    var favoriteDragKey by remember { mutableStateOf<String?>(null) }
    var favoriteDragOffset by remember { mutableStateOf(Offset.Zero) }
    var favoritePointerInRoot by remember { mutableStateOf(Offset.Zero) }
    var favoriteDropTargetKey by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(reorderMode) {
        if (!reorderMode) {
            favoriteDragKey = null
            favoriteDragOffset = Offset.Zero
            favoritePointerInRoot = Offset.Zero
            favoriteDropTargetKey = null
        }
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = GlazeMetrics.space5, vertical = GlazeMetrics.space4)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column {
                Text("GoreeCloud", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Glaze Home", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                themeMode.name.lowercase().replaceFirstChar { it.uppercase() },
                Modifier.background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(GlazeMetrics.radiusControl),
                ).clickable { onCycleTheme(themeMode) }
                    .defaultMinSize(minHeight = GlazeMetrics.comfortableTarget)
                    .padding(horizontal = GlazeMetrics.space4, vertical = GlazeMetrics.space3),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Spacer(Modifier.height(GlazeMetrics.space6))
        if (!isDefaultHome) {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(GlazeMetrics.radiusExtraLarge),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(Modifier.padding(GlazeMetrics.space5)) {
                    Text("Make this your Home app", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(GlazeMetrics.space2))
                    Text("Android keeps this choice under your control. GoreeCloud Launcher does not force itself as the default.")
                    Spacer(Modifier.height(GlazeMetrics.space4))
                    Button(onClick = onRequestHomeRole) { Text("Choose default launcher") }
                }
            }
            Spacer(Modifier.height(GlazeMetrics.space5))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Favorites", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            TextButton(
                onClick = { reorderMode = !reorderMode },
                modifier = Modifier.defaultMinSize(minHeight = GlazeMetrics.comfortableTarget),
            ) {
                Text(if (reorderMode) "Done" else "Reorder")
            }
        }
        Spacer(Modifier.height(GlazeMetrics.space1))
        Text(
            if (reorderMode) {
                "Drag a Favorite or Dock app onto another item to change its order. Move controls remain available outside Reorder mode."
            } else {
                "Long-press a Home or Dock item to manage placement and ordering."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(GlazeMetrics.space3))

        if (favoriteApps.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "No favorites yet. Long-press an app in All apps to add one.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space5),
                horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space3),
            ) {
                items(favoriteApps, key = { it.workspaceKey() }) { app ->
                    val key = app.workspaceKey()
                    val isDragging = favoriteDragKey == key
                    val reorderModifier = if (reorderMode) {
                        Modifier
                            .onGloballyPositioned { coordinates ->
                                favoriteBounds[key] = coordinates.boundsInRoot()
                            }
                            .zIndex(
                                when {
                                    isDragging -> 2f
                                    favoriteDropTargetKey == key -> 1f
                                    else -> 0f
                                }
                            )
                            .graphicsLayer {
                                if (isDragging) {
                                    translationX = favoriteDragOffset.x
                                    translationY = favoriteDragOffset.y
                                    alpha = 0.88f
                                    scaleX = 1.04f
                                    scaleY = 1.04f
                                }
                            }
                            .pointerInput(key, reorderMode) {
                                detectDragGestures(
                                    onDragStart = { localOffset ->
                                        favoriteDragKey = key
                                        favoriteDragOffset = Offset.Zero
                                        favoritePointerInRoot =
                                            (favoriteBounds[key]?.topLeft ?: Offset.Zero) + localOffset
                                        favoriteDropTargetKey = null
                                    },
                                    onDragCancel = {
                                        favoriteDragKey = null
                                        favoriteDragOffset = Offset.Zero
                                        favoritePointerInRoot = Offset.Zero
                                        favoriteDropTargetKey = null
                                    },
                                    onDragEnd = {
                                        val targetKey = favoriteDropTargetKey
                                        if (targetKey != null && targetKey != key) {
                                            onMoveFavoriteToTarget(app, targetKey)
                                        }
                                        favoriteDragKey = null
                                        favoriteDragOffset = Offset.Zero
                                        favoritePointerInRoot = Offset.Zero
                                        favoriteDropTargetKey = null
                                    },
                                ) { change, dragAmount ->
                                    change.consume()
                                    favoriteDragOffset += dragAmount
                                    favoritePointerInRoot += dragAmount
                                    favoriteDropTargetKey = favoriteBounds.entries
                                        .firstOrNull { (targetKey, bounds) ->
                                            targetKey != key && bounds.contains(favoritePointerInRoot)
                                        }
                                        ?.key
                                }
                            }
                    } else {
                        Modifier
                    }

                    AppTile(
                        app = app,
                        onClick = { onLaunchApp(app) },
                        onLongClick = { onManageApp(app) },
                        modifier = reorderModifier,
                        interactionEnabled = !reorderMode,
                        highlighted = favoriteDropTargetKey == key,
                    )
                }
            }
        }

        if (dockApps.isNotEmpty()) {
            DockStrip(
                apps = dockApps,
                reorderMode = reorderMode,
                onLaunchApp = onLaunchApp,
                onManageApp = onManageApp,
                onMoveToTarget = onMoveDockToTarget,
            )
            Spacer(Modifier.height(GlazeMetrics.space3))
        }

        Card(
            Modifier.fillMaxWidth().clickable(onClick = onOpenDrawer),
            shape = RoundedCornerShape(GlazeMetrics.radius2ExtraLarge),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Text(
                "Search apps  •  Open app drawer",
                Modifier.fillMaxWidth().defaultMinSize(minHeight = GlazeMetrics.comfortableTarget)
                    .padding(vertical = GlazeMetrics.space4, horizontal = GlazeMetrics.space5),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun DockStrip(
    apps: List<LauncherActivityInfo>,
    reorderMode: Boolean,
    onLaunchApp: (LauncherActivityInfo) -> Unit,
    onManageApp: (LauncherActivityInfo) -> Unit,
    onMoveToTarget: (LauncherActivityInfo, String) -> Unit,
) {
    val dockBounds = remember { mutableStateMapOf<String, Rect>() }
    var dragKey by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var pointerInRoot by remember { mutableStateOf(Offset.Zero) }
    var dropTargetKey by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(reorderMode) {
        if (!reorderMode) {
            dragKey = null
            dragOffset = Offset.Zero
            pointerInRoot = Offset.Zero
            dropTargetKey = null
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(GlazeMetrics.radiusExtraLarge),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(GlazeMetrics.space3),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            apps.take(MAX_DOCK_ITEMS).forEach { app ->
                val key = app.workspaceKey()
                val isDragging = dragKey == key
                val reorderModifier = if (reorderMode) {
                    Modifier
                        .onGloballyPositioned { coordinates ->
                            dockBounds[key] = coordinates.boundsInRoot()
                        }
                        .zIndex(
                            when {
                                isDragging -> 2f
                                dropTargetKey == key -> 1f
                                else -> 0f
                            }
                        )
                        .graphicsLayer {
                            if (isDragging) {
                                translationX = dragOffset.x
                                translationY = dragOffset.y
                                alpha = 0.88f
                                scaleX = 1.04f
                                scaleY = 1.04f
                            }
                        }
                        .pointerInput(key, reorderMode) {
                            detectDragGestures(
                                onDragStart = { localOffset ->
                                    dragKey = key
                                    dragOffset = Offset.Zero
                                    pointerInRoot = (dockBounds[key]?.topLeft ?: Offset.Zero) + localOffset
                                    dropTargetKey = null
                                },
                                onDragCancel = {
                                    dragKey = null
                                    dragOffset = Offset.Zero
                                    pointerInRoot = Offset.Zero
                                    dropTargetKey = null
                                },
                                onDragEnd = {
                                    val targetKey = dropTargetKey
                                    if (targetKey != null && targetKey != key) {
                                        onMoveToTarget(app, targetKey)
                                    }
                                    dragKey = null
                                    dragOffset = Offset.Zero
                                    pointerInRoot = Offset.Zero
                                    dropTargetKey = null
                                },
                            ) { change, dragAmount ->
                                change.consume()
                                dragOffset += dragAmount
                                pointerInRoot += dragAmount
                                dropTargetKey = dockBounds.entries
                                    .firstOrNull { (targetKey, bounds) ->
                                        targetKey != key && bounds.contains(pointerInRoot)
                                    }
                                    ?.key
                            }
                        }
                } else {
                    Modifier
                }

                DockTile(
                    app = app,
                    onClick = { onLaunchApp(app) },
                    onLongClick = { onManageApp(app) },
                    modifier = reorderModifier,
                    interactionEnabled = !reorderMode,
                    highlighted = dropTargetKey == key,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DockTile(
    app: LauncherActivityInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionEnabled: Boolean = true,
    highlighted: Boolean = false,
) {
    val icon = rememberAppIcon(app, size = 96)
    val interactionModifier = if (interactionEnabled) {
        Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    } else {
        Modifier
    }
    Column(
        modifier = modifier.width(60.dp)
            .then(interactionModifier)
            .defaultMinSize(minHeight = GlazeMetrics.comfortableTarget)
            .padding(vertical = GlazeMetrics.space1),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(48.dp).background(
                if (highlighted) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(GlazeMetrics.radiusControl),
            ),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) Image(icon, app.label.toString(), Modifier.size(42.dp))
            else Text(app.label.toString().take(1).uppercase(), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(GlazeMetrics.space1))
        Text(
            app.label.toString(),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AppDrawer(
    apps: List<LauncherActivityInfo>,
    onLaunchApp: (LauncherActivityInfo) -> Unit,
    onManageApp: (LauncherActivityInfo) -> Unit,
    onClose: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filteredApps = remember(apps, query) {
        val normalizedQuery = query.trim().lowercase()
        if (normalizedQuery.isEmpty()) apps
        else apps.filter { app ->
            app.label.toString().lowercase().contains(normalizedQuery) ||
                app.componentName.packageName.lowercase().contains(normalizedQuery)
        }
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = GlazeMetrics.space5, vertical = GlazeMetrics.space4)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column {
                Text("All apps", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Long-press an app to manage Home and Dock",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "Home",
                Modifier.clickable(onClick = onClose)
                    .defaultMinSize(minHeight = GlazeMetrics.comfortableTarget)
                    .padding(GlazeMetrics.space3),
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(GlazeMetrics.space3))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Search apps") },
            placeholder = { Text("Name or package") },
            shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
        )
        Spacer(Modifier.height(GlazeMetrics.space4))
        if (filteredApps.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No apps found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(76.dp),
                contentPadding = PaddingValues(bottom = GlazeMetrics.space5),
                verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space5),
                horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space3),
            ) {
                items(filteredApps, key = { it.workspaceKey() }) { app ->
                    AppTile(
                        app = app,
                        onClick = { onLaunchApp(app) },
                        onLongClick = { onManageApp(app) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppPlacementDialog(
    app: LauncherActivityInfo,
    workspace: WorkspaceState,
    onToggleFavorite: () -> Unit,
    onToggleDock: () -> Unit,
    onMoveFavorite: (WorkspaceMoveDirection) -> Unit,
    onMoveDock: (WorkspaceMoveDirection) -> Unit,
    onClose: () -> Unit,
) {
    val key = app.workspaceKey()
    val favoriteIndex = workspace.favoriteKeys.indexOf(key)
    val dockIndex = workspace.dockKeys.indexOf(key)
    val isFavorite = favoriteIndex >= 0
    val isDocked = dockIndex >= 0
    val dockFull = !isDocked && workspace.dockKeys.size >= MAX_DOCK_ITEMS

    AlertDialog(
        onDismissRequest = onClose,
        shape = RoundedCornerShape(GlazeMetrics.radiusExtraLarge),
        title = { Text(app.label.toString()) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space3)) {
                Text(
                    "Manage where this app appears and its order. These choices stay local on this device.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PlacementSection(
                    title = "Favorites",
                    isMember = isFavorite,
                    position = favoriteIndex.takeIf { it >= 0 }?.let {
                        "Position ${it + 1} of ${workspace.favoriteKeys.size}"
                    },
                    addLabel = "Add favorite",
                    removeLabel = "Remove favorite",
                    toggleEnabled = true,
                    canMoveEarlier = favoriteIndex > 0,
                    canMoveLater = favoriteIndex >= 0 && favoriteIndex < workspace.favoriteKeys.lastIndex,
                    onToggle = onToggleFavorite,
                    onMoveEarlier = { onMoveFavorite(WorkspaceMoveDirection.EARLIER) },
                    onMoveLater = { onMoveFavorite(WorkspaceMoveDirection.LATER) },
                )
                PlacementSection(
                    title = "Dock",
                    isMember = isDocked,
                    position = dockIndex.takeIf { it >= 0 }?.let {
                        "Position ${it + 1} of ${workspace.dockKeys.size}"
                    },
                    addLabel = if (dockFull) "Dock full" else "Add to dock",
                    removeLabel = "Remove from dock",
                    toggleEnabled = !dockFull,
                    canMoveEarlier = dockIndex > 0,
                    canMoveLater = dockIndex >= 0 && dockIndex < workspace.dockKeys.lastIndex,
                    onToggle = onToggleDock,
                    onMoveEarlier = { onMoveDock(WorkspaceMoveDirection.EARLIER) },
                    onMoveLater = { onMoveDock(WorkspaceMoveDirection.LATER) },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onClose,
                modifier = Modifier.defaultMinSize(minHeight = GlazeMetrics.comfortableTarget),
            ) { Text("Done") }
        },
    )
}

@Composable
private fun PlacementSection(
    title: String,
    isMember: Boolean,
    position: String?,
    addLabel: String,
    removeLabel: String,
    toggleEnabled: Boolean,
    canMoveEarlier: Boolean,
    canMoveLater: Boolean,
    onToggle: () -> Unit,
    onMoveEarlier: () -> Unit,
    onMoveLater: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(GlazeMetrics.space4),
            verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space3),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (position != null) {
                        Text(
                            position,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                FilledTonalButton(
                    onClick = onToggle,
                    enabled = toggleEnabled,
                    modifier = Modifier.defaultMinSize(minHeight = GlazeMetrics.comfortableTarget),
                ) {
                    Text(if (isMember) removeLabel else addLabel)
                }
            }

            if (isMember) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                ) {
                    OutlinedButton(
                        onClick = onMoveEarlier,
                        enabled = canMoveEarlier,
                        modifier = Modifier.weight(1f)
                            .defaultMinSize(minHeight = GlazeMetrics.comfortableTarget),
                    ) { Text("Move earlier") }
                    OutlinedButton(
                        onClick = onMoveLater,
                        enabled = canMoveLater,
                        modifier = Modifier.weight(1f)
                            .defaultMinSize(minHeight = GlazeMetrics.comfortableTarget),
                    ) { Text("Move later") }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppTile(
    app: LauncherActivityInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    interactionEnabled: Boolean = true,
    highlighted: Boolean = false,
) {
    val icon = rememberAppIcon(app, size = 128)
    val interactionModifier = when {
        !interactionEnabled -> Modifier
        onLongClick != null -> Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
        else -> Modifier.clickable(onClick = onClick)
    }
    Column(
        modifier.then(interactionModifier).padding(GlazeMetrics.space1),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(62.dp).background(
                if (highlighted) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(GlazeMetrics.space5),
            ),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) Image(icon, app.label.toString(), Modifier.size(52.dp))
            else Text(app.label.toString().take(1).uppercase(), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(GlazeMetrics.space2))
        Text(
            app.label.toString(),
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable
private fun rememberAppIcon(app: LauncherActivityInfo, size: Int): ImageBitmap? =
    remember(app.componentName, app.user, size) {
        runCatching { app.getIcon(0).toBitmap(size, size).asImageBitmap() }.getOrNull()
    }
