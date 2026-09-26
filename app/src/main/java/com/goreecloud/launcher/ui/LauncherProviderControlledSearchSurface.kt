package com.goreecloud.launcher.ui

import android.content.pm.LauncherActivityInfo
import android.net.Uri
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goreecloud.launcher.core.launcher.LaunchApplicationSearchAction
import com.goreecloud.launcher.core.launcher.LauncherFilesSearchProvider
import com.goreecloud.launcher.core.launcher.LauncherLaunchShortcutSearchAction
import com.goreecloud.launcher.core.launcher.LauncherOpenDocumentSearchAction
import com.goreecloud.launcher.core.launcher.LauncherLocalSearchPermissions
import com.goreecloud.launcher.core.launcher.LauncherLocalSearchDiagnostics
import com.goreecloud.launcher.core.launcher.LauncherLocalSearchIssue
import com.goreecloud.launcher.core.launcher.LauncherMessagesSearchProvider
import com.goreecloud.launcher.core.launcher.LauncherOpenUriSearchAction
import com.goreecloud.launcher.core.launcher.LauncherRuntimeSearchProviderRegistry
import com.goreecloud.launcher.core.launcher.LauncherNavigateSearchAction
import com.goreecloud.launcher.core.launcher.LauncherSearchCategory
import com.goreecloud.launcher.core.launcher.LauncherSearchDestination
import com.goreecloud.launcher.core.launcher.LauncherSearchExecutionPolicy
import com.goreecloud.launcher.core.launcher.LauncherSearchProviderControlState
import com.goreecloud.launcher.core.launcher.LauncherSearchProviderPreferenceDecodeResult
import com.goreecloud.launcher.core.launcher.LauncherSearchProviderPreferenceSnapshot
import com.goreecloud.launcher.core.launcher.LauncherSearchPresentationPolicy
import com.goreecloud.launcher.core.launcher.LauncherSearchProviderUserControlPolicy
import com.goreecloud.launcher.core.launcher.LauncherSearchResult
import com.goreecloud.launcher.core.launcher.LauncherUniversalSearch
import com.goreecloud.launcher.ui.theme.GlazeMetrics

@Composable
internal fun LauncherProviderControlledSearchSurface(
    apps: List<LauncherActivityInfo>,
    searchProviderPreferences: LauncherSearchProviderPreferenceDecodeResult?,
    fileSearchRoots: List<Uri>,
    onSetSearchProviderPreferences: (LauncherSearchProviderPreferenceSnapshot) -> Unit,
    onSetSearchProviderEnabled: (LauncherSearchProviderControlState, String, Boolean) -> Unit,
    onChooseFileSearchRoot: () -> Unit,
    onRemoveFileSearchRoot: (Uri) -> Unit,
    onResetSearchProviderPreferences: () -> Unit,
    onLaunchApp: (LauncherActivityInfo) -> Unit,
    onLaunchShortcut: (LauncherLaunchShortcutSearchAction) -> Unit,
    onOpenSearchUri: (LauncherOpenUriSearchAction) -> Unit,
    onOpenDocument: (LauncherOpenDocumentSearchAction) -> Unit,
    onSearchWithConnectedProvider: (String, String) -> Unit,
    onNavigate: (LauncherSearchDestination) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var showSources by rememberSaveable { mutableStateOf(false) }

    BackHandler {
        if (showSources) showSources = false else onBack()
    }

    val catalog = remember(apps, context, fileSearchRoots) {
        LauncherRuntimeSearchProviderRegistry.catalog(context, apps, fileSearchRoots)
    }
    val controls = remember(catalog, searchProviderPreferences) {
        searchProviderPreferences?.let {
            LauncherSearchProviderUserControlPolicy.normalize(catalog, it)
        } ?: LauncherSearchProviderUserControlPolicy.normalize(
            catalog = catalog,
            requestedEnabledProviderIds = emptySet(),
            requestedProviderOrder = emptyList(),
        )
    }
    val providers = remember(catalog, controls, searchProviderPreferences) {
        if (searchProviderPreferences == null) emptyList()
        else LauncherSearchProviderUserControlPolicy.automaticProviders(catalog, controls)
    }
    var results by remember(providers, query) {
        mutableStateOf<List<LauncherSearchResult>>(emptyList())
    }
    var complete by remember(providers, query, searchProviderPreferences) {
        mutableStateOf(false)
    }
    val explicitHandoffs = remember(query, controls) {
        LauncherSearchPresentationPolicy.explicitHandoffProviders(
            rawQuery = query,
            providerControls = controls,
        )
    }

    LaunchedEffect(providers, query, searchProviderPreferences) {
        if (searchProviderPreferences == null) {
            results = emptyList()
            complete = false
            return@LaunchedEffect
        }
        if (query.isBlank()) {
            results = emptyList()
            complete = true
            return@LaunchedEffect
        }
        complete = false
        results = LauncherUniversalSearch.searchAsync(
            rawQuery = query,
            providers = providers,
            policy = LauncherSearchExecutionPolicy.cancellationOnly(),
        )
        complete = true
    }

    // Search remains a light floating overlay above the Launcher wallpaper. It never
    // automatically forwards typed input to connected providers.
    Column(
        modifier = Modifier.fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = GlazeMetrics.space3, vertical = GlazeMetrics.space3),
        verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
    ) {
        if (showSources) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(GlazeMetrics.radiusExtraLarge),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(GlazeMetrics.space3),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Search sources",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            "Local consent and explicit provider handoffs",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(
                        onClick = { showSources = false },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) { Text("Back") }
                }
            }
            LauncherSearchSourceManager(
                persisted = searchProviderPreferences,
                controls = controls,
                onSet = onSetSearchProviderPreferences,
                fileSearchRoots = fileSearchRoots,
                onSetEnabled = onSetSearchProviderEnabled,
                onChooseFileSearchRoot = onChooseFileSearchRoot,
                onRemoveFileSearchRoot = onRemoveFileSearchRoot,
                onReset = onResetSearchProviderPreferences,
                modifier = Modifier.weight(1f),
            )
        } else {
            GlazeAppSearchField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                requestFocus = true,
                placeholder = "Find anything on your device…",
                inputTestTag = "launcher-universal-search-field",
                trailingContent = {
                    LauncherUniversalSearchSettingsAction(
                        onClick = { showSources = true },
                    )
                },
            )
            if (query.isNotBlank()) {
                Surface(
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
                    .testTag("launcher-glaze-search-panel"),
                shape = RoundedCornerShape(GlazeMetrics.radius2ExtraLarge),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 12.dp,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(GlazeMetrics.space2),
                    verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space1),
                ) {
                    val providerIssues by LauncherLocalSearchDiagnostics.issues.collectAsState()
                    val enabledIssues = providerIssues.filterKeys { controls.isEnabled(it) }
                    if (enabledIssues.isNotEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(GlazeMetrics.radiusMedium),
                            color = MaterialTheme.colorScheme.errorContainer,
                        ) {
                            Text(
                                "Some sources are unavailable. Review their permissions or status.",
                                modifier = Modifier.padding(GlazeMetrics.space2),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }

                    if (results.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(
                                horizontal = GlazeMetrics.space3,
                                vertical = GlazeMetrics.space2,
                            ),
                            verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space1),
                        ) {
                            Text(
                                when {
                                    searchProviderPreferences == null -> "Loading search sources"
                                    providers.isEmpty() -> "Turn on sources to search this device"
                                    !complete -> "Searching…"
                                    else -> "No results found"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                if (enabledIssues.isNotEmpty()) "Check source status for missing results."
                                else "Try a different name, number, app or filename.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        val grouped = LauncherGlazeSearchGroups.group(results)
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
                                .testTag("launcher-glaze-search-results"),
                            verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space1),
                        ) {
                            grouped.forEach { section ->
                                item(key = "header:" + section.category.name) {
                                    Text(
                                        section.title,
                                        modifier = Modifier.fillMaxWidth().padding(
                                            start = GlazeMetrics.space2,
                                            top = GlazeMetrics.space2,
                                            bottom = GlazeMetrics.space1,
                                        ),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                if (section.category == LauncherSearchCategory.APPLICATION) {
                                    val topApps = section.items.take(8)
                                    topApps.chunked(4).forEachIndexed { index, chunk ->
                                        item(key = "app-grid:" + index) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space1),
                                            ) {
                                                chunk.forEach { result ->
                                                    val action = result.action as? LaunchApplicationSearchAction
                                                    if (action != null) {
                                                        LauncherGlazeSearchAppTile(
                                                            result = result,
                                                            action = action,
                                                            onLaunch = { onLaunchApp(action.app) },
                                                            modifier = Modifier.weight(1f),
                                                        )
                                                    }
                                                }
                                                repeat(4 - chunk.size) { Spacer(Modifier.weight(1f)) }
                                            }
                                        }
                                    }
                                    items(
                                        section.items.drop(8),
                                        key = { "app:" + it.providerId + ":" + it.resultId },
                                    ) { result ->
                                        LauncherProviderSearchRow(result) {
                                            (result.action as? LaunchApplicationSearchAction)
                                                ?.let { onLaunchApp(it.app) }
                                        }
                                    }
                                } else if (section.category == LauncherSearchCategory.SHORTCUT) {
                                    val shortcutResults = section.items.mapNotNull { result ->
                                        (result.action as? LauncherLaunchShortcutSearchAction)?.let { it to result }
                                    }
                                    val byApplication = shortcutResults.groupBy {
                                        it.first.packageName to it.first.user
                                    }
                                    val groupedShortcuts = byApplication.entries.toList()
                                    items(
                                        groupedShortcuts,
                                        key = {
                                            "shortcuts:" + it.key.first + ":" + it.key.second.hashCode()
                                        },
                                    ) { entry ->
                                        val matchingApp = apps.firstOrNull { app ->
                                            app.componentName.packageName == entry.key.first &&
                                                app.user == entry.key.second
                                        }
                                        LauncherGlazeShortcutPanel(
                                            app = matchingApp,
                                            packageName = entry.key.first,
                                            shortcuts = entry.value.map { it.second },
                                            onLaunchShortcut = onLaunchShortcut,
                                        )
                                    }
                                    items(
                                        section.items.filterNot { it.action is LauncherLaunchShortcutSearchAction },
                                        key = { "other-shortcut:" + it.providerId + ":" + it.resultId },
                                    ) { result ->
                                        LauncherProviderSearchRow(result) {
                                            (result.action as? LauncherLaunchShortcutSearchAction)
                                                ?.let(onLaunchShortcut)
                                        }
                                    }
                                } else {
                                    items(
                                        section.items,
                                        key = { it.category.name + ":" + it.providerId + ":" + it.resultId },
                                    ) { result ->
                                        LauncherGlazeSearchResult(
                                            result = result,
                                            onActivate = {
                                                when (val action = result.action) {
                                                    is LauncherLaunchShortcutSearchAction -> onLaunchShortcut(action)
                                                    is LauncherOpenUriSearchAction -> onOpenSearchUri(action)
                                                    is LauncherOpenDocumentSearchAction -> onOpenDocument(action)
                                                    is LauncherNavigateSearchAction -> onNavigate(action.destination)
                                                    else -> Unit
                                                }
                                            },
                                            onOpenSearchUri = onOpenSearchUri,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (explicitHandoffs.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                        ) {
                            Text(
                                "Search with",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(
                                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space1),
                            ) {
                                explicitHandoffs.forEach { provider ->
                                    androidx.compose.material3.OutlinedButton(
                                        onClick = { onSearchWithConnectedProvider(provider.providerId, query) },
                                        modifier = Modifier.heightIn(min = 48.dp),
                                    ) {
                                        Text(provider.displayName, maxLines = 1)
                                    }
                                }
                            }
                        }
                        Text(
                            "Your query is sent only to the provider you tap.",
                            modifier = Modifier.padding(start = GlazeMetrics.space2),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun LauncherUniversalSearchSettingsAction(
    onClick: () -> Unit,
) {
    val iconColor = MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .testTag("launcher-universal-search-settings")
            .semantics { contentDescription = "Universal Search settings" },
        shape = RoundedCornerShape(GlazeMetrics.radiusPill),
        color = Color.Transparent,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(22.dp)) {
                val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                val strokeWidth = 1.8.dp.toPx()
                val hubRadius = 3.2.dp.toPx()
                val bodyRadius = 6.6.dp.toPx()
                val toothInner = 8.0.dp.toPx()
                val toothOuter = 10.0.dp.toPx()

                drawCircle(
                    color = iconColor,
                    radius = bodyRadius,
                    center = center,
                    style = Stroke(width = strokeWidth),
                )
                drawCircle(
                    color = iconColor,
                    radius = hubRadius,
                    center = center,
                    style = Stroke(width = strokeWidth),
                )
                repeat(8) { index ->
                    val angle = index * (PI / 4.0)
                    val cosAngle = cos(angle).toFloat()
                    val sinAngle = sin(angle).toFloat()
                    val start = androidx.compose.ui.geometry.Offset(
                        x = center.x + cosAngle * toothInner,
                        y = center.y + sinAngle * toothInner,
                    )
                    val end = androidx.compose.ui.geometry.Offset(
                        x = center.x + cosAngle * toothOuter,
                        y = center.y + sinAngle * toothOuter,
                    )
                    drawLine(
                        color = iconColor,
                        start = start,
                        end = end,
                        strokeWidth = strokeWidth,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    )
                }
            }
        }
    }
}

internal data class LauncherGlazeSearchSection(
    val category: LauncherSearchCategory,
    val title: String,
    val items: List<LauncherSearchResult>,
)

/** Presentation-only grouping; never changes provider execution, opt-in, or result contents. */
internal object LauncherGlazeSearchGroups {
    private val order = listOf(
        LauncherSearchCategory.APPLICATION to "Apps",
        LauncherSearchCategory.SHORTCUT to "App shortcuts",
        LauncherSearchCategory.CONTACT to "People",
        LauncherSearchCategory.CALL_HISTORY to "Recent calls",
        LauncherSearchCategory.MESSAGE to "Messages",
        LauncherSearchCategory.FILE to "Files",
        LauncherSearchCategory.ACTION to "Actions",
        LauncherSearchCategory.SETTING to "Settings",
        LauncherSearchCategory.CONNECTED_SOURCE to "Connected sources",
    )

    fun group(results: List<LauncherSearchResult>): List<LauncherGlazeSearchSection> =
        order.mapNotNull { (category, title) ->
            results.filter { it.category == category }
                .takeIf { it.isNotEmpty() }
                ?.let { LauncherGlazeSearchSection(category, title, it) }
        }
}

@Composable
private fun LauncherGlazeSearchAppTile(
    result: LauncherSearchResult,
    action: LaunchApplicationSearchAction,
    onLaunch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val icon = rememberLauncherAppIcon(action.app)
    Surface(
        modifier = modifier.heightIn(min = 96.dp),
        onClick = onLaunch,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
        shape = RoundedCornerShape(GlazeMetrics.radiusMedium),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 3.dp, vertical = GlazeMetrics.space2),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space1),
        ) {
            if (icon != null) {
                Image(
                    bitmap = icon,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(42.dp).launcherIconMask(),
                )
            } else {
                Box(
                    modifier = Modifier.size(42.dp).background(
                        MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(14.dp),
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        result.title.take(1),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Text(
                result.title,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * Shortcut results are grouped by Android package AND profile. A Work-profile shortcut
 * must never be displayed or launched as a Personal-profile shortcut with the same label.
 */
@Composable
private fun LauncherGlazeShortcutPanel(
    app: LauncherActivityInfo?,
    packageName: String,
    shortcuts: List<LauncherSearchResult>,
    onLaunchShortcut: (LauncherLaunchShortcutSearchAction) -> Unit,
) {
    val icon = if (app != null) rememberLauncherAppIcon(app) else null
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("launcher-glaze-shortcut-panel"),
        shape = RoundedCornerShape(GlazeMetrics.radiusMedium),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
    ) {
        Column(
            modifier = Modifier.padding(GlazeMetrics.space2),
            verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
            ) {
                if (icon != null) {
                    Image(
                        bitmap = icon,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp).launcherIconMask(),
                        contentScale = ContentScale.Fit,
                    )
                }
                Text(
                    app?.label?.toString() ?: packageName.substringAfterLast('.'),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            shortcuts.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space1),
                ) {
                    row.forEach { shortcut ->
                        androidx.compose.material3.OutlinedButton(
                            onClick = {
                                (shortcut.action as? LauncherLaunchShortcutSearchAction)
                                    ?.let(onLaunchShortcut)
                            },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        ) {
                            Text(
                                shortcut.title,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** Local result actions are explicit user taps; Message opens the system's default SMS handler. */
@Composable
private fun LauncherGlazeSearchResult(
    result: LauncherSearchResult,
    onActivate: () -> Unit,
    onOpenSearchUri: (LauncherOpenUriSearchAction) -> Unit,
) {
    val isContact = result.category == LauncherSearchCategory.CONTACT
    val number = result.subtitle?.takeIf { it.any(Char::isDigit) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(GlazeMetrics.radiusMedium),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(
                horizontal = GlazeMetrics.space3,
                vertical = GlazeMetrics.space1,
            ),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                onClick = onActivate,
                shape = RoundedCornerShape(GlazeMetrics.radiusMedium),
                color = Color.Transparent,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                ) {
                    if (isContact) {
                        Surface(
                            modifier = Modifier.size(38.dp),
                            shape = RoundedCornerShape(GlazeMetrics.radiusPill),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    result.title.trim().take(1).uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            result.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        result.subtitle?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Text(
                        if (isContact) "View ›" else "Open ›",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            if (isContact && number != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2)) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            onOpenSearchUri(LauncherOpenUriSearchAction(
                                Intent.ACTION_DIAL,
                                Uri.fromParts("tel", number, null).toString(),
                            ))
                        },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) { Text("Call") }
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            onOpenSearchUri(LauncherOpenUriSearchAction(
                                Intent.ACTION_SENDTO,
                                Uri.fromParts("smsto", number, null).toString(),
                            ))
                        },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) { Text("Message") }
                }
            }
        }
    }
}

@Composable
private fun LauncherSearchSourceManager(
    persisted: LauncherSearchProviderPreferenceDecodeResult?,
    controls: LauncherSearchProviderControlState,
    fileSearchRoots: List<Uri>,
    onSet: (LauncherSearchProviderPreferenceSnapshot) -> Unit,
    onSetEnabled: (LauncherSearchProviderControlState, String, Boolean) -> Unit,
    onChooseFileSearchRoot: () -> Unit,
    onRemoveFileSearchRoot: (Uri) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val ready = persisted != null
    val issues by LauncherLocalSearchDiagnostics.issues.collectAsState()
    var reorderMode by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxWidth().testTag("launcher-search-source-manager"),
        verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
    ) {
        item(key = "provider-controls") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
            ) {
                Column(
                    modifier = Modifier.padding(GlazeMetrics.space3),
                    verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space1),
                ) {
                    Text(
                        "Your data stays under your control",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        if (ready)
                            "Only enabled local sources receive typed queries. Connected providers " +
                                "receive a query only after you tap Search with."
                        else "Loading your saved controls; Search remains off until they load.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                    ) {
                        TextButton(onClick = onReset, enabled = ready) { Text("Safe defaults") }
                        TextButton(
                            onClick = { reorderMode = !reorderMode },
                            enabled = ready && controls.orderedOptions.size > 1,
                        ) { Text(if (reorderMode) "Finish ordering" else "Reorder") }
                    }
                }
            }
        }
        items(controls.orderedOptions, key = { it.providerId }) { option ->
            val index = controls.orderedOptions.indexOfFirst { it.providerId == option.providerId }
            val permissionGranted = LauncherLocalSearchPermissions.isGranted(context, option.providerId)
            val issue = issues[option.providerId]
            val isMessages = option.providerId == LauncherMessagesSearchProvider.PROVIDER_ID
            val errorMessage = when {
                issue == LauncherLocalSearchIssue.ANDROID_RESTRICTED && isMessages ->
                    "Android denied restricted SMS access. Launcher cannot override this or " +
                        "change your default messaging app."
                issue == LauncherLocalSearchIssue.ANDROID_RESTRICTED ->
                    "Android blocked this source even though it was enabled."
                issue == LauncherLocalSearchIssue.SOURCE_UNAVAILABLE ->
                    "This Android data source could not be queried. No result does not mean no data."
                !permissionGranted && isMessages ->
                    "SMS permission required. Some Android devices restrict this permission; " +
                        "messages stay in your default app."
                !permissionGranted -> "Android permission required before this source can be searched."
                else -> null
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(
                        horizontal = GlazeMetrics.space3,
                        vertical = GlazeMetrics.space2,
                    ),
                    verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space1),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                option.displayName,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                option.privacySummary + if (
                                    option.providerId == LauncherFilesSearchProvider.PROVIDER_ID
                                ) {
                                    " · " + when (fileSearchRoots.size) {
                                        0 -> "No folders selected"
                                        1 -> "1 folder selected"
                                        else -> fileSearchRoots.size.toString() + " folders selected"
                                    }
                                } else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = controls.isEnabled(option.providerId) && permissionGranted,
                            onCheckedChange = { onSetEnabled(controls, option.providerId, it) },
                            enabled = ready,
                            modifier = Modifier.testTag(
                                "launcher-search-source-" + option.providerId,
                            ),
                        )
                    }
                    if (errorMessage != null && (
                        controls.isEnabled(option.providerId) || !permissionGranted
                    )) {
                        Text(
                            errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    if (option.providerId == LauncherFilesSearchProvider.PROVIDER_ID) {
                        TextButton(
                            onClick = onChooseFileSearchRoot,
                            enabled = ready,
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) {
                            Text(if (fileSearchRoots.isEmpty()) "Choose folder" else "Add folder")
                        }
                        fileSearchRoots.forEach { root ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                            ) {
                                Text(
                                    root.lastPathSegment?.substringAfterLast(':')
                                        ?.takeIf { it.isNotBlank() } ?: "Selected folder",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                TextButton(
                                    onClick = { onRemoveFileSearchRoot(root) },
                                    enabled = ready,
                                ) { Text("Remove") }
                            }
                        }
                    }
                    if (reorderMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(
                                onClick = {
                                    onSet(LauncherSearchProviderUserControlPolicy.moveProviderBy(
                                        controls, option.providerId, -1,
                                    ))
                                },
                                enabled = ready && index > 0,
                            ) { Text("↑ Earlier") }
                            TextButton(
                                onClick = {
                                    onSet(LauncherSearchProviderUserControlPolicy.moveProviderBy(
                                        controls, option.providerId, 1,
                                    ))
                                },
                                enabled = ready && index in 0 until controls.orderedOptions.lastIndex,
                            ) { Text("↓ Later") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LauncherProviderSearchRow(
    result: LauncherSearchResult,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(GlazeMetrics.space3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space3),
        ) {
            Column(Modifier.weight(1f)) {
                Text(result.title, fontWeight = FontWeight.SemiBold)
                // A launcher search presents app labels, never diagnostic package names.
                result.subtitle?.takeUnless { result.category == LauncherSearchCategory.APPLICATION }?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                when (result.category) {
                    LauncherSearchCategory.APPLICATION -> "App"
                    LauncherSearchCategory.SHORTCUT -> "Shortcut"
                    LauncherSearchCategory.CONTACT -> "Contact"
                    LauncherSearchCategory.CALL_HISTORY -> "Call"
                    LauncherSearchCategory.MESSAGE -> "Message"
                    LauncherSearchCategory.FILE -> "File"
                    LauncherSearchCategory.CONNECTED_SOURCE -> "Connected"
                    LauncherSearchCategory.SETTING -> "Setting"
                    LauncherSearchCategory.ACTION -> "Action"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
