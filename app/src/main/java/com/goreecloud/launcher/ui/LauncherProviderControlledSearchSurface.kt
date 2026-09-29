package com.goreecloud.launcher.ui

import android.content.pm.LauncherActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.net.Uri
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goreecloud.launcher.BuildConfig
import com.goreecloud.launcher.core.launcher.LaunchApplicationSearchAction
import com.goreecloud.launcher.core.launcher.LauncherConnectedSearchProviderRegistry
import com.goreecloud.launcher.core.launcher.LauncherContactsSearchProvider
import com.goreecloud.launcher.core.launcher.LauncherCallHistorySearchProvider
import com.goreecloud.launcher.core.launcher.LauncherFilesSearchProvider
import com.goreecloud.launcher.core.launcher.LauncherGoogleDriveAuthorizationState
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
import com.goreecloud.launcher.core.launcher.LauncherSearchExplicitHandoffProvider
import com.goreecloud.launcher.core.launcher.LauncherSearchProviderControlOption
import com.goreecloud.launcher.core.launcher.LauncherSearchProviderControlState
import com.goreecloud.launcher.core.launcher.LauncherSearchProviderInvocationMode
import com.goreecloud.launcher.core.launcher.LauncherSearchProviderPreferenceDecodeResult
import com.goreecloud.launcher.core.launcher.LauncherSearchProviderPreferenceSnapshot
import com.goreecloud.launcher.core.launcher.LauncherSearchPresentationPolicy
import com.goreecloud.launcher.core.launcher.LauncherSearchSuggestionPolicy
import com.goreecloud.launcher.core.launcher.LauncherSearchSuggestionTab
import com.goreecloud.launcher.core.launcher.LauncherSearchSuggestionPresentation
import com.goreecloud.launcher.core.launcher.LauncherSearchSuggestionPresentationRepository
import com.goreecloud.launcher.core.launcher.LauncherSearchProviderUserControlPolicy
import com.goreecloud.launcher.core.launcher.LauncherSearchResult
import com.goreecloud.launcher.core.launcher.LauncherUniversalSearch
import com.goreecloud.launcher.core.workspace.workspaceKey
import com.goreecloud.launcher.ui.theme.GlazeMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal fun launcherDriveInlineConnectionAvailable(
    debugBuild: Boolean,
    alreadyConnected: Boolean,
): Boolean = !debugBuild || alreadyConnected

@Composable
internal fun LauncherProviderControlledSearchSurface(
    apps: List<LauncherActivityInfo>,
    recentAppKeys: List<String>,
    localLaunchCounts: Map<String, Long>,
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
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchAppearancePreferences = remember(context.applicationContext) {
        LauncherSearchSuggestionPresentationRepository(context.applicationContext)
    }
    val suggestionPresentation by searchAppearancePreferences.presentation.collectAsState(
        initial = LauncherSearchSuggestionPresentation.ICONS,
    )
    var query by rememberSaveable { mutableStateOf("") }
    var showSources by rememberSaveable { mutableStateOf(false) }
    var suggestionTabName by rememberSaveable {
        mutableStateOf(LauncherSearchSuggestionTab.FREQUENT.name)
    }
    val suggestionTab = remember(suggestionTabName) {
        runCatching { LauncherSearchSuggestionTab.valueOf(suggestionTabName) }
            .getOrDefault(LauncherSearchSuggestionTab.FREQUENT)
    }
    val appsByKey = remember(apps) { apps.associateBy { it.workspaceKey() } }
    var appFreshness by remember(apps) { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var appFreshnessLoaded by remember(apps) { mutableStateOf(false) }

    LaunchedEffect(apps) {
        appFreshnessLoaded = false
        appFreshness = loadLauncherAppFreshness(context.packageManager, apps)
        appFreshnessLoaded = true
    }

    val suggestionKeys = remember(
        suggestionTab,
        appsByKey,
        recentAppKeys,
        localLaunchCounts,
        appFreshness,
    ) {
        LauncherSearchSuggestionPolicy.selectKeys(
            tab = suggestionTab,
            availableAppKeys = appsByKey.keys,
            recentAppKeys = recentAppKeys,
            launchCounts = localLaunchCounts,
            freshnessByAppKey = appFreshness,
            limit = LauncherSearchSuggestionPolicy.DEFAULT_LIMIT * 3,
        )
    }
    val suggestionApps = remember(suggestionKeys, appsByKey) {
        suggestionKeys
            .asSequence()
            .mapNotNull(appsByKey::get)
            .distinctBy { app ->
                app.user.hashCode().toString() + ":" + app.componentName.packageName
            }
            .take(LauncherSearchSuggestionPolicy.DEFAULT_LIMIT)
            .toList()
    }

    val dismissSearch = {
        // Clear ownership before changing the root Launcher surface. Leaving the text field focused
        // while the IME is animating out can keep an obsolete Search focus target alive through
        // AnimatedContent disposal and delay the next Home gesture on representative devices.
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        onBack()
    }

    BackHandler {
        if (showSources) {
            showSources = false
        } else {
            dismissSearch()
        }
    }

    val googleDriveAuthorizationRevision by
        LauncherGoogleDriveAuthorizationState.revision.collectAsState()
    val catalog = remember(
        apps,
        context,
        fileSearchRoots,
        googleDriveAuthorizationRevision,
    ) {
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
    val automaticProviderIds = remember(providers) {
        providers.mapTo(linkedSetOf()) { provider -> provider.id }
    }
    val explicitHandoffs = remember(query, controls, automaticProviderIds) {
        LauncherSearchPresentationPolicy.explicitHandoffProviders(
            rawQuery = query,
            providerControls = controls,
        ).filterNot { provider -> provider.providerId in automaticProviderIds }
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

    // Search remains a light floating overlay above the Launcher wallpaper. Current connected
    // providers stay tap-only; future reviewed remote-inline providers require explicit opt-in.
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
                            "Choose where Universal Search looks",
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
            LauncherSearchSuggestionPresentationControl(
                selected = suggestionPresentation,
                onSelect = searchAppearancePreferences::setPresentation,
            )
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
            val idleSearch = query.isBlank()
            val searchContainerColor by animateColorAsState(
                targetValue = if (idleSearch) {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.20f)
                },
                animationSpec = tween(durationMillis = 220),
                label = "launcherSearchContainerColor",
            )
            val searchContainerOutline by animateColorAsState(
                targetValue = if (idleSearch) {
                    MaterialTheme.colorScheme.outlineVariant
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f)
                },
                animationSpec = tween(durationMillis = 220),
                label = "launcherSearchContainerOutline",
            )
            val searchContainerRadius by animateDpAsState(
                targetValue = if (idleSearch) {
                    GlazeMetrics.radius2ExtraLarge
                } else {
                    GlazeMetrics.radiusExtraLarge
                },
                animationSpec = tween(durationMillis = 220),
                label = "launcherSearchContainerRadius",
            )
            val searchContainerElevation by animateDpAsState(
                targetValue = if (idleSearch) 12.dp else 2.dp,
                animationSpec = tween(durationMillis = 220),
                label = "launcherSearchContainerElevation",
            )
            val searchContainerPadding by animateDpAsState(
                targetValue = if (idleSearch) GlazeMetrics.space1 else 0.dp,
                animationSpec = tween(durationMillis = 220),
                label = "launcherSearchContainerPadding",
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = tween(durationMillis = 220))
                    .then(
                        if (idleSearch) {
                            Modifier.testTag("launcher-universal-search-suggestions")
                        } else {
                            Modifier
                        },
                    ),
                shape = RoundedCornerShape(searchContainerRadius),
                color = searchContainerColor,
                border = BorderStroke(1.dp, searchContainerOutline),
                shadowElevation = searchContainerElevation,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(searchContainerPadding),
                    verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space1),
                ) {
                    GlazeAppSearchField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        requestFocus = true,
                        placeholder = "Search apps",
                        inputTestTag = "launcher-universal-search-field",
                        trailingContent = {
                            LauncherUniversalSearchSettingsAction(
                                onClick = { showSources = true },
                            )
                        },
                    )
                    if (idleSearch) {
                        LauncherUniversalSearchSuggestions(
                            selectedTab = suggestionTab,
                            presentation = suggestionPresentation,
                            apps = suggestionApps,
                            freshnessLoaded = appFreshnessLoaded,
                            onSelectTab = { tab -> suggestionTabName = tab.name },
                            onLaunchApp = onLaunchApp,
                        )
                    }
                }
            }
            if (query.isBlank()) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("launcher-universal-search-empty-dismiss")
                        .semantics {
                            contentDescription = "Close Universal Search"
                        }
                        .clickable(onClick = dismissSearch),
                )
            } else {
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

                    if (results.isEmpty() && explicitHandoffs.isEmpty()) {
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
                                when {
                                    enabledIssues.isNotEmpty() ->
                                        "Check source status for missing results."
                                    else -> "Try a different name, number, app or filename."
                                },
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
                                            sourceLabel = LauncherGlazeSearchGroups.connectedSourceLabel(
                                                result = result,
                                                providerControls = controls,
                                            ),
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
                        Text(
                            "Search online",
                            modifier = Modifier.padding(
                                start = GlazeMetrics.space2,
                                top = GlazeMetrics.space1,
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                        )
                        explicitHandoffs.forEach { provider ->
                            LauncherSearchHandoffRow(
                                provider = provider,
                                query = query,
                                apps = apps,
                                onClick = {
                                    onSearchWithConnectedProvider(provider.providerId, query)
                                },
                            )
                        }
                        Text(
                            "Connected queries are sent only after you tap a result.",
                            modifier = Modifier.padding(horizontal = GlazeMetrics.space2),
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

@Suppress("DEPRECATION")
private fun launcherPackageFreshnessMillis(
    packageManager: PackageManager,
    packageName: String,
): Long? = runCatching {
    val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(
            packageName,
            PackageManager.PackageInfoFlags.of(0L),
        )
    } else {
        packageManager.getPackageInfo(packageName, 0)
    }
    maxOf(packageInfo.firstInstallTime, packageInfo.lastUpdateTime)
        .takeIf { it > 0L }
}.getOrNull()

private suspend fun loadLauncherAppFreshness(
    packageManager: PackageManager,
    apps: List<LauncherActivityInfo>,
): Map<String, Long> = withContext(Dispatchers.IO) {
    val packageTimes = mutableMapOf<String, Long?>()
    buildMap {
        apps.forEach { app ->
            val packageName = app.componentName.packageName
            if (!packageTimes.containsKey(packageName)) {
                packageTimes[packageName] =
                    launcherPackageFreshnessMillis(packageManager, packageName)
            }
            packageTimes[packageName]?.let { timestamp ->
                put(app.workspaceKey(), timestamp)
            }
        }
    }
}


@Composable
private fun LauncherSearchSuggestionPresentationControl(
    selected: LauncherSearchSuggestionPresentation,
    onSelect: (LauncherSearchSuggestionPresentation) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(GlazeMetrics.radiusExtraLarge),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(GlazeMetrics.space2),
            verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space1),
        ) {
            Text(
                "Suggestion tabs",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Choose icons, words, or both.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space1),
            ) {
                LauncherSearchSuggestionPresentation.entries.forEach { option ->
                    val active = option == selected
                    Surface(
                        onClick = { onSelect(option) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .semantics {
                                contentDescription =
                                    "Search suggestion tabs: " + option.displayName
                            },
                        shape = RoundedCornerShape(GlazeMetrics.radiusPill),
                        color = if (active) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (active) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.46f)
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                        ),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                option.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (active) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontWeight = if (active) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Medium
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
private fun LauncherUniversalSearchSuggestions(
    selectedTab: LauncherSearchSuggestionTab,
    presentation: LauncherSearchSuggestionPresentation,
    apps: List<LauncherActivityInfo>,
    freshnessLoaded: Boolean,
    onSelectTab: (LauncherSearchSuggestionTab) -> Unit,
    onLaunchApp: (LauncherActivityInfo) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space1),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("launcher-search-suggestion-tabs"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LauncherSearchSuggestionTab.entries.forEach { tab ->
                val selected = tab == selectedTab
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .selectable(
                            selected = selected,
                            onClick = { onSelectTab(tab) },
                            role = Role.Tab,
                        )
                        .testTag(
                            "launcher-search-suggestion-tab-" +
                                tab.name.lowercase(),
                        )
                        .semantics {
                            contentDescription =
                                launcherSearchSuggestionTabAccessibilityLabel(tab)
                        }
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    val tabColor = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        if (presentation != LauncherSearchSuggestionPresentation.WORDS) {
                            LauncherSearchSuggestionTabIcon(
                                tab = tab,
                                color = tabColor,
                            )
                        }
                        if (presentation == LauncherSearchSuggestionPresentation.BOTH) {
                            Spacer(Modifier.size(6.dp))
                        }
                        if (presentation != LauncherSearchSuggestionPresentation.ICONS) {
                            Text(
                                tab.displayName,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selected) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.SemiBold
                                },
                                color = tabColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Spacer(Modifier.size(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.82f)
                            .height(3.dp)
                            .background(
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.Transparent
                                },
                                shape = RoundedCornerShape(GlazeMetrics.radiusPill),
                            ),
                    )
                }
            }
        }

        LauncherUniversalSearchSuggestionGrid(
            tab = selectedTab,
            apps = apps,
            freshnessLoaded = freshnessLoaded,
            onLaunchApp = onLaunchApp,
        )
    }
}

@Composable
private fun LauncherUniversalSearchSuggestionGrid(
    tab: LauncherSearchSuggestionTab,
    apps: List<LauncherActivityInfo>,
    freshnessLoaded: Boolean,
    onLaunchApp: (LauncherActivityInfo) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("launcher-search-suggestion-grid"),
    ) {
        val columns = when {
            maxWidth >= 700.dp -> 8
            maxWidth >= 500.dp -> 7
            maxWidth >= 360.dp -> 6
            else -> 4
        }
        val visibleApps = apps.take(columns * 2)

        if (visibleApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 104.dp)
                    .padding(horizontal = GlazeMetrics.space3),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    when (tab) {
                        LauncherSearchSuggestionTab.FREQUENT ->
                            "No frequent apps yet. Open apps normally and they will appear here."
                        LauncherSearchSuggestionTab.RECENT ->
                            "No recently opened apps yet."
                        LauncherSearchSuggestionTab.NEW_UPDATED ->
                            if (freshnessLoaded) {
                                "Install or update information is not available for visible apps."
                            } else {
                                "Checking installed apps…"
                            }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space1),
            ) {
                visibleApps.chunked(columns).forEach { rowApps ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space1),
                    ) {
                        rowApps.forEach { app ->
                            LauncherUniversalSearchSuggestionApp(
                                app = app,
                                onLaunch = { onLaunchApp(app) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(columns - rowApps.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LauncherUniversalSearchSuggestionApp(
    app: LauncherActivityInfo,
    onLaunch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val icon = rememberLauncherAppIcon(app)
    Surface(
        onClick = onLaunch,
        modifier = modifier
            .heightIn(min = 82.dp)
            .testTag("launcher-search-suggestion-app-" + app.workspaceKey()),
        shape = RoundedCornerShape(GlazeMetrics.radiusMedium),
        color = Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = GlazeMetrics.space1),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space1),
        ) {
            if (icon != null) {
                Image(
                    bitmap = icon,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(42.dp)
                        .launcherIconMask(),
                )
            } else {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            app.label.toString().trim().take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
            Text(
                app.label.toString(),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LauncherSearchHandoffRow(
    provider: LauncherSearchExplicitHandoffProvider,
    query: String,
    apps: List<LauncherActivityInfo>,
    onClick: () -> Unit,
) {
    val packageName = remember(provider.providerId) {
        LauncherConnectedSearchProviderRegistry.iconPackageNameFor(provider.providerId)
    }
    val sourceApp = remember(apps, packageName) {
        packageName?.let { targetPackage ->
            apps.firstOrNull { app -> app.componentName.packageName == targetPackage }
        }
    }
    val icon = sourceApp?.let { rememberLauncherAppIcon(it) }
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("launcher-search-handoff-" + provider.providerId)
            .semantics {
                contentDescription = "Search " + provider.displayName + " for " + query
            },
        shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GlazeMetrics.space2, vertical = GlazeMetrics.space1),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(GlazeMetrics.radiusPill),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (icon != null) {
                        Image(
                            bitmap = icon,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(28.dp).launcherIconMask(),
                        )
                    } else {
                        LauncherConnectedProviderFallbackGlyph(provider.providerId)
                    }
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "Search " + provider.displayName + " for “" + query + "”",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "Open the full online results",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "›",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}


@Composable
private fun LauncherConnectedProviderFallbackGlyph(
    providerId: String,
    modifier: Modifier = Modifier,
) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier.size(28.dp)) {
        val unit = size.minDimension
        val strokeWidth = unit * 0.075f
        when (providerId) {
            LauncherConnectedSearchProviderRegistry.BRAVE_SEARCH_PROVIDER_ID -> {
                // A local Glaze shield + search glyph avoids the old initials placeholder when
                // Brave Browser is not installed. It is intentionally not a copied brand asset.
                val shield = androidx.compose.ui.graphics.Path().apply {
                    moveTo(unit * 0.50f, unit * 0.06f)
                    lineTo(unit * 0.82f, unit * 0.18f)
                    lineTo(unit * 0.78f, unit * 0.58f)
                    lineTo(unit * 0.50f, unit * 0.90f)
                    lineTo(unit * 0.22f, unit * 0.58f)
                    lineTo(unit * 0.18f, unit * 0.18f)
                    close()
                }
                drawPath(shield, color = color, style = Stroke(width = strokeWidth))
                val center = androidx.compose.ui.geometry.Offset(unit * 0.46f, unit * 0.43f)
                drawCircle(
                    color = color,
                    radius = unit * 0.14f,
                    center = center,
                    style = Stroke(width = strokeWidth),
                )
                drawLine(
                    color = color,
                    start = androidx.compose.ui.geometry.Offset(unit * 0.56f, unit * 0.54f),
                    end = androidx.compose.ui.geometry.Offset(unit * 0.68f, unit * 0.66f),
                    strokeWidth = strokeWidth,
                )
            }
            LauncherConnectedSearchProviderRegistry.GOOGLE_DRIVE_PROVIDER_ID -> {
                drawLine(
                    color,
                    androidx.compose.ui.geometry.Offset(unit * 0.50f, unit * 0.10f),
                    androidx.compose.ui.geometry.Offset(unit * 0.16f, unit * 0.72f),
                    strokeWidth,
                )
                drawLine(
                    color,
                    androidx.compose.ui.geometry.Offset(unit * 0.50f, unit * 0.10f),
                    androidx.compose.ui.geometry.Offset(unit * 0.84f, unit * 0.72f),
                    strokeWidth,
                )
                drawLine(
                    color,
                    androidx.compose.ui.geometry.Offset(unit * 0.16f, unit * 0.72f),
                    androidx.compose.ui.geometry.Offset(unit * 0.84f, unit * 0.72f),
                    strokeWidth,
                )
            }
            LauncherConnectedSearchProviderRegistry.DROPBOX_PROVIDER_ID -> {
                listOf(
                    0.34f to 0.32f,
                    0.66f to 0.32f,
                    0.34f to 0.62f,
                    0.66f to 0.62f,
                ).forEach { (x, y) ->
                    drawCircle(
                        color = color,
                        radius = unit * 0.115f,
                        center = androidx.compose.ui.geometry.Offset(unit * x, unit * y),
                    )
                }
            }
            else -> {
                val center = androidx.compose.ui.geometry.Offset(unit * 0.44f, unit * 0.42f)
                drawCircle(
                    color = color,
                    radius = unit * 0.20f,
                    center = center,
                    style = Stroke(width = strokeWidth),
                )
                drawLine(
                    color = color,
                    start = androidx.compose.ui.geometry.Offset(unit * 0.58f, unit * 0.58f),
                    end = androidx.compose.ui.geometry.Offset(unit * 0.80f, unit * 0.80f),
                    strokeWidth = strokeWidth,
                )
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

    fun connectedSourceLabel(
        result: LauncherSearchResult,
        providerControls: LauncherSearchProviderControlState,
    ): String? {
        if (result.category != LauncherSearchCategory.CONNECTED_SOURCE) return null

        val displayName = providerControls.orderedOptions
            .firstOrNull { option -> option.providerId == result.providerId }
            ?.displayName
            ?.takeIf { it.isNotBlank() }
        return "From " + (displayName ?: result.providerId)
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
    sourceLabel: String?,
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
                        sourceLabel?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
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
    var detailProviderId by rememberSaveable { mutableStateOf<String?>(null) }

    val sections = remember(controls.orderedOptions) {
        LauncherSearchSourceSection.entries.mapNotNull { section ->
            controls.orderedOptions
                .filter { option -> sourceSectionFor(option) == section }
                .takeIf { it.isNotEmpty() }
                ?.let { options -> section to options }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .testTag("launcher-search-source-manager"),
        verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
    ) {
        item(key = "provider-controls") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = GlazeMetrics.space3,
                            end = GlazeMetrics.space1,
                            top = GlazeMetrics.space1,
                            bottom = GlazeMetrics.space1,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space1),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Private by default",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            if (ready) {
                                "Local stays local · connected sources are opt-in"
                            } else {
                                "Loading source controls…"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    TextButton(onClick = onReset, enabled = ready) { Text("Reset") }
                    TextButton(
                        onClick = { reorderMode = !reorderMode },
                        enabled = ready && controls.orderedOptions.size > 1,
                    ) { Text(if (reorderMode) "Done" else "Order") }
                }
            }
        }

        sections.forEach { (section, options) ->
            item(key = "section-block-" + section.name) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(GlazeMetrics.radiusExtraLarge),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    ),
                ) {
                    Column {
                        Column(
                            modifier = Modifier.padding(
                                horizontal = GlazeMetrics.space3,
                                vertical = GlazeMetrics.space2,
                            ),
                        ) {
                            Text(
                                section.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                section.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f),
                        )

                        options.forEachIndexed { sectionIndex, option ->
                            val index = controls.orderedOptions.indexOfFirst {
                                it.providerId == option.providerId
                            }
                            val enabled = controls.isEnabled(option.providerId)
                            val permissionGranted =
                                LauncherLocalSearchPermissions.isGranted(context, option.providerId)
                            val driveSource =
                                option.providerId ==
                                    LauncherConnectedSearchProviderRegistry.GOOGLE_DRIVE_PROVIDER_ID
                            val driveConnected =
                                driveSource && LauncherGoogleDriveAuthorizationState.isConnected()
                            val driveConnectionAvailable =
                                !driveSource || launcherDriveInlineConnectionAvailable(
                                    debugBuild = BuildConfig.DEBUG,
                                    alreadyConnected = driveConnected,
                                )
                            val providerReady = !driveSource || driveConnected
                            val issue = issues[option.providerId]
                            val isMessages =
                                option.providerId == LauncherMessagesSearchProvider.PROVIDER_ID
                            val expanded = detailProviderId == option.providerId
                            val errorMessage = when {
                                issue == LauncherLocalSearchIssue.ANDROID_RESTRICTED && isMessages ->
                                    "Android denied restricted SMS access. Launcher cannot override it."
                                issue == LauncherLocalSearchIssue.ANDROID_RESTRICTED ->
                                    "Android blocked this source even though it was enabled."
                                issue == LauncherLocalSearchIssue.SOURCE_UNAVAILABLE ->
                                    "This Android data source could not be queried."
                                !permissionGranted && isMessages ->
                                    "SMS permission is required; some Android devices restrict it."
                                !permissionGranted ->
                                    "Android permission is required before this source can be searched."
                                else -> null
                            }
                            val statusLabel = when {
                                issue == LauncherLocalSearchIssue.ANDROID_RESTRICTED ->
                                    "Restricted by Android"
                                issue == LauncherLocalSearchIssue.SOURCE_UNAVAILABLE ->
                                    "Unavailable"
                                !permissionGranted && isMessages ->
                                    "SMS permission required"
                                !permissionGranted ->
                                    "Permission required"
                                option.providerId == LauncherFilesSearchProvider.PROVIDER_ID &&
                                    fileSearchRoots.isEmpty() ->
                                    "Choose a folder to enable"
                                driveSource && !driveConnectionAvailable ->
                                    "Signed Development build required"
                                else -> null
                            }

                            if (sectionIndex > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = GlazeMetrics.space3),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        detailProviderId =
                                            if (expanded) null else option.providerId
                                    }
                                    .padding(
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
                                    if (section == LauncherSearchSourceSection.CONNECTED) {
                                        Surface(
                                            modifier = Modifier.size(40.dp),
                                            shape = RoundedCornerShape(GlazeMetrics.radiusMedium),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                                .copy(alpha = 0.46f),
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                LauncherConnectedProviderFallbackGlyph(
                                                    option.providerId,
                                                )
                                            }
                                        }
                                    }

                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            option.displayName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            compactSourceSummary(
                                                context = context,
                                                option = option,
                                                fileSearchRoots = fileSearchRoots,
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        if (statusLabel != null) {
                                            Text(
                                                statusLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (errorMessage != null) {
                                                    MaterialTheme.colorScheme.error
                                                } else {
                                                    MaterialTheme.colorScheme.primary
                                                },
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                            )
                                        }
                                    }

                                    Switch(
                                        checked = enabled && permissionGranted && providerReady,
                                        onCheckedChange = {
                                            onSetEnabled(controls, option.providerId, it)
                                        },
                                        enabled = ready && driveConnectionAvailable,
                                        modifier = Modifier.testTag(
                                            "launcher-search-source-" + option.providerId,
                                        ),
                                    )
                                }

                                if (
                                    option.providerId == LauncherFilesSearchProvider.PROVIDER_ID &&
                                    fileSearchRoots.isEmpty()
                                ) {
                                    TextButton(
                                        onClick = onChooseFileSearchRoot,
                                        enabled = ready,
                                        modifier = Modifier
                                            .align(Alignment.End)
                                            .heightIn(min = 48.dp),
                                    ) {
                                        Text("Choose folder")
                                    }
                                }

                                if (expanded) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(GlazeMetrics.radiusMedium),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                            .copy(alpha = 0.34f),
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(GlazeMetrics.space2),
                                            verticalArrangement = Arrangement.spacedBy(
                                                GlazeMetrics.space1,
                                            ),
                                        ) {
                                            if (errorMessage != null) {
                                                Text(
                                                    errorMessage,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.error,
                                                )
                                            }
                                            Text(
                                                option.privacySummary,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            connectedSourceDetail(
                                                context = context,
                                                option = option,
                                                fileSearchRoots = fileSearchRoots,
                                            )?.let { detail ->
                                                Text(
                                                    detail,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }

                                            if (
                                                option.providerId ==
                                                LauncherFilesSearchProvider.PROVIDER_ID
                                            ) {
                                                TextButton(
                                                    onClick = onChooseFileSearchRoot,
                                                    enabled = ready,
                                                    modifier = Modifier.heightIn(min = 48.dp),
                                                ) {
                                                    Text(
                                                        if (fileSearchRoots.isEmpty()) {
                                                            "Choose folder"
                                                        } else {
                                                            "Add folder"
                                                        },
                                                    )
                                                }
                                                fileSearchRoots.forEach { root ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement =
                                                            Arrangement.spacedBy(
                                                                GlazeMetrics.space2,
                                                            ),
                                                    ) {
                                                        Text(
                                                            root.lastPathSegment
                                                                ?.substringAfterLast(':')
                                                                ?.takeIf { it.isNotBlank() }
                                                                ?: "Selected folder",
                                                            modifier = Modifier.weight(1f),
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme
                                                                .onSurfaceVariant,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                        )
                                                        TextButton(
                                                            onClick = {
                                                                onRemoveFileSearchRoot(root)
                                                            },
                                                            enabled = ready,
                                                        ) { Text("Remove") }
                                                    }
                                                }
                                            }
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
                                                onSet(
                                                    LauncherSearchProviderUserControlPolicy
                                                        .moveProviderBy(
                                                            controls,
                                                            option.providerId,
                                                            -1,
                                                        ),
                                                )
                                            },
                                            enabled = ready && index > 0,
                                        ) { Text("↑") }
                                        TextButton(
                                            onClick = {
                                                onSet(
                                                    LauncherSearchProviderUserControlPolicy
                                                        .moveProviderBy(
                                                            controls,
                                                            option.providerId,
                                                            1,
                                                        ),
                                                )
                                            },
                                            enabled = ready &&
                                                index in 0 until
                                                    controls.orderedOptions.lastIndex,
                                        ) { Text("↓") }
                                    }
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
private fun LauncherSearchSourceStatusPill(
    label: String,
    isError: Boolean,
) {
    val foreground = if (isError) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val background = if (isError) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.10f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f)
    }
    Surface(
        shape = RoundedCornerShape(GlazeMetrics.radiusPill),
        color = background,
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = foreground,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private enum class LauncherSearchSourceSection(
    val title: String,
    val subtitle: String,
) {
    DEVICE("On-device", "Fast local sources with no network query"),
    PERSONAL("Your content", "Permission-scoped personal data and selected folders"),
    CONNECTED("Connected", "External services and reviewed inline adapters"),
}

private fun sourceSectionFor(
    option: LauncherSearchProviderControlOption,
): LauncherSearchSourceSection = when (option.providerId) {
    LauncherContactsSearchProvider.PROVIDER_ID,
    LauncherCallHistorySearchProvider.PROVIDER_ID,
    LauncherMessagesSearchProvider.PROVIDER_ID,
    LauncherFilesSearchProvider.PROVIDER_ID,
    -> LauncherSearchSourceSection.PERSONAL

    LauncherConnectedSearchProviderRegistry.GOOGLE_DRIVE_PROVIDER_ID,
    LauncherConnectedSearchProviderRegistry.DROPBOX_PROVIDER_ID,
    LauncherConnectedSearchProviderRegistry.BRAVE_SEARCH_PROVIDER_ID,
    -> LauncherSearchSourceSection.CONNECTED

    else -> LauncherSearchSourceSection.DEVICE
}

private fun compactSourceSummary(
    context: android.content.Context,
    option: LauncherSearchProviderControlOption,
    fileSearchRoots: List<Uri>,
): String = when (option.providerId) {
    LauncherFilesSearchProvider.PROVIDER_ID -> when (fileSearchRoots.size) {
        0 -> "Folders · None selected"
        1 -> "Folders · 1 selected"
        else -> "Folders · " + fileSearchRoots.size + " selected"
    }
    LauncherConnectedSearchProviderRegistry.GOOGLE_DRIVE_PROVIDER_ID ->
        if (LauncherGoogleDriveAuthorizationState.isConnected()) {
            "Connected · Online Google Drive"
        } else if (BuildConfig.DEBUG) {
            "Google Drive · Inline auth unavailable in CI build"
        } else {
            "Google account · Authorization required"
        }
    LauncherConnectedSearchProviderRegistry.BRAVE_SEARCH_PROVIDER_ID ->
        "Web handoff · Inline adapter not authorized yet"
    LauncherConnectedSearchProviderRegistry.DROPBOX_PROVIDER_ID ->
        "App handoff · OAuth adapter not authorized yet"
    else -> when (option.invocationMode) {
        LauncherSearchProviderInvocationMode.AUTOMATIC_LOCAL -> "Local · Automatic"
        LauncherSearchProviderInvocationMode.OPT_IN_LOCAL -> "Local · Permission controlled"
        LauncherSearchProviderInvocationMode.OPT_IN_REMOTE_INLINE -> "Connected · inline when ready"
        LauncherSearchProviderInvocationMode.EXPLICIT_USER_HANDOFF -> "Connected · tap to search"
    }
}

private fun connectedSourceDetail(
    context: android.content.Context,
    option: LauncherSearchProviderControlOption,
    fileSearchRoots: List<Uri>,
): String? = when (option.providerId) {
    LauncherConnectedSearchProviderRegistry.GOOGLE_DRIVE_PROVIDER_ID ->
        if (LauncherGoogleDriveAuthorizationState.isConnected()) {
            "Universal Search sends the typed query to the Google Drive API only while this source " +
                "is enabled. Launcher keeps the short-lived access token in process memory only."
        } else if (BuildConfig.DEBUG) {
            "This CI Development build uses a temporary Android debug signing identity, so Google " +
                "cannot authorize it as the registered Launcher OAuth client. Use Files → Choose " +
                "folder for a permission-scoped Drive folder now; inline Drive Search stays off " +
                "until a protected signed Development build is registered with Google."
        } else {
            "Enable Google Drive to authorize a Google account for metadata-only Drive search. " +
                "Folder-scoped local document search remains under Files."
        }
    LauncherConnectedSearchProviderRegistry.BRAVE_SEARCH_PROVIDER_ID ->
        "The official Brave Autosuggest API requires a confidential subscription token. Launcher " +
            "will not embed that provider secret in the APK or scrape Brave pages; inline suggestions " +
            "remain unavailable until a governed server-side credential path exists."
    LauncherConnectedSearchProviderRegistry.DROPBOX_PROVIDER_ID ->
        "Dropbox inline results require a reviewed OAuth adapter. Until that authorization path " +
            "exists, Launcher keeps this source behind an explicit handoff."
    else -> null
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
