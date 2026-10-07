package com.goreecloud.launcher.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.LauncherActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.net.Uri
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goreecloud.launcher.BuildConfig
import com.goreecloud.launcher.R
import com.goreecloud.launcher.core.launcher.LaunchApplicationSearchAction
import com.goreecloud.launcher.core.launcher.LauncherDirectApiSource
import com.goreecloud.launcher.core.launcher.LauncherDirectApiSourceStore
import com.goreecloud.launcher.core.launcher.LauncherConnectedSearchProviderRegistry
import com.goreecloud.launcher.core.launcher.LauncherCopyTextSearchAction
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
import com.goreecloud.launcher.core.launcher.LauncherQuickAnswersSearchProvider
import com.goreecloud.launcher.core.launcher.LauncherInstalledAppsSearchProvider
import com.goreecloud.launcher.core.launcher.LauncherCoreActionsSearchProvider
import com.goreecloud.launcher.core.launcher.LauncherShortcutsSearchProvider
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
import com.goreecloud.launcher.core.launcher.launcherVisibleAppLabel
import com.goreecloud.launcher.core.workspace.workspaceKey
import com.goreecloud.launcher.ui.theme.GlazeMetrics
import com.goreecloud.launcher.ui.theme.GlazeV16MaterialRole
import com.goreecloud.launcher.ui.theme.GlazeV16PresentationPolicy
import com.goreecloud.launcher.ui.theme.LocalGlazeV16PresentationContext
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
    val directApiStore = remember(context.applicationContext) {
        LauncherDirectApiSourceStore(context.applicationContext)
    }
    var directApiSources by remember { mutableStateOf<List<LauncherDirectApiSource>>(emptyList()) }
    var directApiStorageError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(directApiStore) {
        try {
            directApiSources = withContext(Dispatchers.IO) { directApiStore.list() }
            directApiStorageError = null
        } catch (_: Exception) {
            directApiStorageError = "Direct API connections are unavailable on this device."
        }
    }
    val onSaveDirectApi: (LauncherDirectApiSource) -> String? = { source ->
        try {
            directApiStore.upsert(source)
            directApiSources = directApiStore.list()
            directApiStorageError = null
            null
        } catch (_: Exception) {
            "Could not save this API connection. Check its HTTPS URL, model, and fields."
        }
    }
    val onRemoveDirectApi: (String) -> String? = { id ->
        try {
            directApiStore.remove(id)
            directApiSources = directApiStore.list()
            directApiStorageError = null
            null
        } catch (_: Exception) {
            "Could not delete this API connection."
        }
    }
    val onResetAllDirectApi: () -> String? = {
        try {
            directApiStore.resetAll()
            directApiSources = emptyList()
            directApiStorageError = null
            null
        } catch (_: Exception) {
            "Could not clear secure API connection storage."
        }
    }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val presentationContext = LocalGlazeV16PresentationContext.current
    val searchPresentation = remember(presentationContext) {
        GlazeV16PresentationPolicy.resolve(
            requestedMaterial = GlazeV16MaterialRole.FUNCTIONAL_GLASS,
            context = presentationContext,
        )
    }
    val searchSurfaceAlpha = when (searchPresentation.materialRole) {
        GlazeV16MaterialRole.SOLID -> 1.00f
        GlazeV16MaterialRole.RAISED -> 0.97f
        GlazeV16MaterialRole.FUNCTIONAL_GLASS -> 0.70f
        GlazeV16MaterialRole.CLEAR_GLASS -> 0.58f
        GlazeV16MaterialRole.CANVAS,
        GlazeV16MaterialRole.OVERLAY -> 0.72f
    }
    val searchSurfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = searchSurfaceAlpha)
    val searchSurfaceOutline = MaterialTheme.colorScheme.onSurface.copy(
        alpha = if (
            searchPresentation.materialRole == GlazeV16MaterialRole.FUNCTIONAL_GLASS ||
                searchPresentation.materialRole == GlazeV16MaterialRole.CLEAR_GLASS
        ) {
            0.11f
        } else {
            0.08f
        },
    )
    val searchSurfaceElevation = when (searchPresentation.materialRole) {
        GlazeV16MaterialRole.FUNCTIONAL_GLASS -> 8.dp
        GlazeV16MaterialRole.CLEAR_GLASS -> 3.dp
        GlazeV16MaterialRole.RAISED -> 4.dp
        else -> 1.dp
    }
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
            .safeDrawingPadding()
            .imePadding()
            .padding(horizontal = GlazeMetrics.space3, vertical = GlazeMetrics.space3),
        verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
    ) {
        if (showSources) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    onClick = { showSources = false },
                    modifier = Modifier
                        .size(48.dp)
                        .semantics {
                            contentDescription = "Back to Universal Search"
                        },
                    shape = RoundedCornerShape(GlazeMetrics.radiusPill),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    ),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        LauncherBackGlyph(
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                Text(
                    "Search Sources",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            LauncherSearchSourceManager(
                apps = apps,
                suggestionPresentation = suggestionPresentation,
                onSelectSuggestionPresentation = searchAppearancePreferences::setPresentation,
                directApiSources = directApiSources,
                directApiStorageError = directApiStorageError,
                onSaveDirectApi = onSaveDirectApi,
                onRemoveDirectApi = onRemoveDirectApi,
                onResetAllDirectApi = onResetAllDirectApi,
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
                    searchSurfaceColor
                } else {
                    MaterialTheme.colorScheme.surface.copy(
                        alpha = (searchSurfaceAlpha - 0.06f).coerceAtLeast(0.76f),
                    )
                },
                animationSpec = tween(durationMillis = 220),
                label = "launcherSearchContainerColor",
            )
            val searchContainerOutline by animateColorAsState(
                targetValue = if (idleSearch) {
                    searchSurfaceOutline
                } else {
                    searchSurfaceOutline.copy(alpha = searchSurfaceOutline.alpha * 0.72f)
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
                targetValue = if (idleSearch) searchSurfaceElevation else 2.dp,
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
                        placeholder = "Search with GoreeCloud…",
                        inputTestTag = "launcher-universal-search-field",
                        trailingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                if (query.isNotBlank()) {
                                    LauncherUniversalSearchClearAction(
                                        onClick = { query = "" },
                                    )
                                }
                                LauncherUniversalSearchSettingsAction(
                                    onClick = { showSources = true },
                                )
                            }
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
                color = Color.Transparent,
                border = null,
                shadowElevation = 0.dp,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = GlazeMetrics.space2,
                                        end = GlazeMetrics.space1,
                                        top = 2.dp,
                                        bottom = 2.dp,
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space1),
                            ) {
                                Text(
                                    "Some sources need attention.",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                                TextButton(
                                    onClick = { showSources = true },
                                    modifier = Modifier.heightIn(min = 48.dp),
                                ) {
                                    Text("Sources")
                                }
                            }
                        }
                    }

                    LauncherDirectApiAnswerPanel(
                        query = query,
                        sources = directApiSources,
                    )

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
                            TextButton(
                                onClick = { showSources = true },
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .heightIn(min = 40.dp),
                            ) {
                                Text("Search sources")
                            }
                        }
                    } else {
                        val topResult = results.firstOrNull()
                        val fullSectionCounts = results
                            .groupBy { result -> result.category }
                            .mapValues { (_, items) -> items.size }
                        val grouped = LauncherGlazeSearchGroups.group(results.drop(1))
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
                                .testTag("launcher-glaze-search-results"),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            if (topResult != null) {
                                item(key = "top-result-label") {
                                    Text(
                                        "Top result",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .semantics { heading() }
                                            .padding(
                                                start = GlazeMetrics.space2,
                                                top = GlazeMetrics.space1,
                                                bottom = GlazeMetrics.space1,
                                            ),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                item(
                                    key = "top-result:" +
                                        topResult.providerId + ":" + topResult.resultId,
                                ) {
                                    LauncherGlazeSearchResult(
                                        result = topResult,
                                        sourceLabel =
                                            LauncherGlazeSearchGroups.connectedSourceLabel(
                                                result = topResult,
                                                providerControls = controls,
                                            ),
                                        onActivate = {
                                            when (val action = topResult.action) {
                                                is LaunchApplicationSearchAction ->
                                                    onLaunchApp(action.app)
                                                is LauncherLaunchShortcutSearchAction ->
                                                    onLaunchShortcut(action)
                                                is LauncherOpenUriSearchAction ->
                                                    onOpenSearchUri(action)
                                                is LauncherOpenDocumentSearchAction ->
                                                    onOpenDocument(action)
                                                is LauncherNavigateSearchAction ->
                                                    onNavigate(action.destination)
                                                else -> Unit
                                            }
                                        },
                                        onOpenSearchUri = onOpenSearchUri,
                                        prominent = true,
                                    )
                                }
                            }
                            grouped.forEach { section ->
                                item(key = "header:" + section.category.name) {
                                    Text(
                                        section.title + " (" +
                                            (fullSectionCounts[section.category] ?: section.items.size) +
                                            ")",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .semantics { heading() }
                                            .padding(
                                                start = GlazeMetrics.space2,
                                                top = 4.dp,
                                                bottom = 1.dp,
                                            ),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                if (section.category == LauncherSearchCategory.APPLICATION) {
                                    val topApps = section.items
                                    topApps.chunked(2).forEachIndexed { index, chunk ->
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
                                                repeat(2 - chunk.size) { Spacer(Modifier.weight(1f)) }
                                            }
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
                                                    is LauncherCopyTextSearchAction -> copyQuickAnswer(context, action)
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
                            color = MaterialTheme.colorScheme.primary,
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
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
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
        shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.76f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.065f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                "Suggestion tabs",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
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
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (active) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.055f)
                            },
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            LauncherSuggestionPresentationGlyph(
                                option = option,
                                tint = if (active) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                            Spacer(Modifier.size(4.dp))
                            Text(
                                option.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                color = if (active) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LauncherSuggestionPresentationGlyph(
    option: LauncherSearchSuggestionPresentation,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(16.dp)) {
        val u = size.minDimension
        val stroke = u * 0.095f
        val cap = androidx.compose.ui.graphics.StrokeCap.Round
        fun iconGrid(left: Float, width: Float) {
            val cell = width * 0.32f
            val gap = width * 0.16f
            repeat(2) { row ->
                repeat(2) { col ->
                    drawRoundRect(
                        color = tint,
                        topLeft = androidx.compose.ui.geometry.Offset(
                            left + col * (cell + gap),
                            u * 0.22f + row * (cell + gap),
                        ),
                        size = androidx.compose.ui.geometry.Size(cell, cell),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cell * 0.22f),
                    )
                }
            }
        }
        fun wordLines(left: Float, right: Float) {
            listOf(0.32f, 0.50f, 0.68f).forEach { y ->
                drawLine(
                    tint,
                    androidx.compose.ui.geometry.Offset(u * left, u * y),
                    androidx.compose.ui.geometry.Offset(u * right, u * y),
                    stroke,
                    cap = cap,
                )
            }
        }
        when (option) {
            LauncherSearchSuggestionPresentation.ICONS -> iconGrid(u * 0.20f, u * 0.60f)
            LauncherSearchSuggestionPresentation.WORDS -> wordLines(0.18f, 0.82f)
            LauncherSearchSuggestionPresentation.BOTH -> {
                iconGrid(u * 0.06f, u * 0.36f)
                wordLines(0.55f, 0.94f)
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
                    contentScale = ContentScale.Crop,
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
    val packageNames = remember(provider.providerId) {
        LauncherConnectedSearchProviderRegistry.iconPackageNamesFor(provider.providerId)
    }
    val sourceApp = remember(apps, packageNames) {
        apps.firstOrNull { app -> app.componentName.packageName in packageNames }
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
        shape = RoundedCornerShape(GlazeMetrics.radiusMedium),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.30f),
        border = null,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(GlazeMetrics.radiusPill),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.70f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (icon != null) {
                        Image(
                            bitmap = icon,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(26.dp).launcherIconMask(),
                        )
                    } else {
                        LauncherConnectedProviderFallbackGlyph(provider.providerId)
                    }
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    provider.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "Search online for “" + query + "”",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            LauncherSourceDisclosureGlyph(
                expanded = false,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}


@Composable
private fun LauncherConnectedProviderFallbackGlyph(
    providerId: String,
    modifier: Modifier = Modifier,
) {
    if (providerId == LauncherConnectedSearchProviderRegistry.BRAVE_SEARCH_PROVIDER_ID) {
        Image(
            painter = painterResource(R.drawable.provider_brave),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier.size(28.dp),
        )
        return
    }

    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier.size(28.dp)) {
        val unit = size.minDimension
        val strokeWidth = unit * 0.075f
        when (providerId) {
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
private fun LauncherUniversalSearchClearAction(
    onClick: () -> Unit,
) {
    val iconColor = MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .testTag("launcher-universal-search-clear")
            .semantics { contentDescription = "Clear Universal Search query" },
        shape = RoundedCornerShape(GlazeMetrics.radiusPill),
        color = Color.Transparent,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(18.dp)) {
                val strokeWidth = 1.8.dp.toPx()
                val inset = 4.dp.toPx()
                drawLine(
                    color = iconColor,
                    start = androidx.compose.ui.geometry.Offset(inset, inset),
                    end = androidx.compose.ui.geometry.Offset(size.width - inset, size.height - inset),
                    strokeWidth = strokeWidth,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
                drawLine(
                    color = iconColor,
                    start = androidx.compose.ui.geometry.Offset(size.width - inset, inset),
                    end = androidx.compose.ui.geometry.Offset(inset, size.height - inset),
                    strokeWidth = strokeWidth,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
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
        LauncherSearchCategory.CONTACT to "Contacts",
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
        modifier = modifier.heightIn(min = 50.dp),
        onClick = onLaunch,
        color = Color.Transparent,
        border = null,
        shape = RoundedCornerShape(GlazeMetrics.radiusMedium),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(modifier = Modifier.size(34.dp)) {
                if (icon != null) {
                    Image(
                        bitmap = icon,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(30.dp).align(Alignment.Center).launcherIconMask(),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .align(Alignment.Center)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(10.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            result.title.take(1),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                LauncherAppProfileBadge(
                    app = action.app,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-3).dp, y = (-3).dp),
                    compact = true,
                )
            }
            Text(
                result.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
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
    var expanded by remember(packageName, shortcuts) { mutableStateOf(false) }
    val visibleShortcuts = if (expanded) shortcuts else shortcuts.take(2)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag("launcher-glaze-shortcut-panel"),
        shape = RoundedCornerShape(GlazeMetrics.radiusMedium),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.28f),
        border = null,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (icon != null) {
                Image(
                    bitmap = icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp).launcherIconMask(),
                    contentScale = ContentScale.Fit,
                )
            }
            Text(
                app?.let(::launcherVisibleAppLabel) ?: packageName.substringAfterLast('.'),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier
                    .widthIn(max = if (expanded) 184.dp else 148.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                visibleShortcuts.forEach { shortcut ->
                    LauncherShortcutActionButton(
                        result = shortcut,
                        onLaunchShortcut = onLaunchShortcut,
                    )
                }
                if (shortcuts.size > 2) {
                    LauncherShortcutOverflowButton(
                        expanded = expanded,
                        hiddenCount = (shortcuts.size - 2).coerceAtLeast(0),
                        onClick = { expanded = !expanded },
                    )
                }
            }
        }
    }
}

@Composable
private fun LauncherShortcutOverflowButton(
    expanded: Boolean,
    hiddenCount: Int,
    onClick: () -> Unit,
) {
    val tint = MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .semantics {
                contentDescription = if (expanded) {
                    "Show fewer app shortcuts"
                } else {
                    "Show " + hiddenCount + " more app shortcuts"
                }
            },
        shape = RoundedCornerShape(GlazeMetrics.radiusPill),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.065f),
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(20.dp)) {
                val u = size.minDimension
                val stroke = u * 0.09f
                val cap = androidx.compose.ui.graphics.StrokeCap.Round
                if (expanded) {
                    drawLine(
                        tint,
                        androidx.compose.ui.geometry.Offset(u * 0.24f, u * 0.62f),
                        androidx.compose.ui.geometry.Offset(u * 0.50f, u * 0.36f),
                        stroke,
                        cap = cap,
                    )
                    drawLine(
                        tint,
                        androidx.compose.ui.geometry.Offset(u * 0.50f, u * 0.36f),
                        androidx.compose.ui.geometry.Offset(u * 0.76f, u * 0.62f),
                        stroke,
                        cap = cap,
                    )
                } else {
                    listOf(0.25f, 0.50f, 0.75f).forEach { x ->
                        drawCircle(
                            color = tint,
                            radius = u * 0.07f,
                            center = androidx.compose.ui.geometry.Offset(u * x, u * 0.50f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LauncherShortcutActionButton(
    result: LauncherSearchResult,
    onLaunchShortcut: (LauncherLaunchShortcutSearchAction) -> Unit,
) {
    val action = result.action as? LauncherLaunchShortcutSearchAction ?: return
    Surface(
        onClick = { onLaunchShortcut(action) },
        modifier = Modifier
            .size(48.dp)
            .semantics { contentDescription = result.title },
        shape = RoundedCornerShape(GlazeMetrics.radiusPill),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f),
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            LauncherShortcutActionGlyph(
                label = result.title,
                tint = MaterialTheme.colorScheme.primary,
            )
            LauncherProfileBadge(
                kind = launcherProfileBadgeKindForUser(
                    action.user,
                    android.os.Process.myUserHandle(),
                ),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-3).dp, y = (-3).dp),
                compact = true,
            )
        }
    }
}

@Composable
private fun LauncherShortcutActionGlyph(
    label: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val normalized = label.lowercase()
    Canvas(modifier.size(20.dp)) {
        val u = size.minDimension
        val stroke = u * 0.085f
        val cap = androidx.compose.ui.graphics.StrokeCap.Round
        when {
            "voice" in normalized || "microphone" in normalized -> {
                drawRoundRect(
                    color = tint,
                    topLeft = androidx.compose.ui.geometry.Offset(u * 0.36f, u * 0.12f),
                    size = androidx.compose.ui.geometry.Size(u * 0.28f, u * 0.48f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(u * 0.14f),
                    style = Stroke(width = stroke),
                )
                drawLine(
                    tint,
                    androidx.compose.ui.geometry.Offset(u * 0.24f, u * 0.48f),
                    androidx.compose.ui.geometry.Offset(u * 0.24f, u * 0.58f),
                    stroke,
                    cap = cap,
                )
                drawArc(
                    color = tint,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(u * 0.22f, u * 0.34f),
                    size = androidx.compose.ui.geometry.Size(u * 0.56f, u * 0.42f),
                    style = Stroke(width = stroke),
                )
                drawLine(
                    tint,
                    androidx.compose.ui.geometry.Offset(u * 0.50f, u * 0.75f),
                    androidx.compose.ui.geometry.Offset(u * 0.50f, u * 0.88f),
                    stroke,
                    cap = cap,
                )
            }
            "camera" in normalized -> {
                drawRoundRect(
                    color = tint,
                    topLeft = androidx.compose.ui.geometry.Offset(u * 0.12f, u * 0.28f),
                    size = androidx.compose.ui.geometry.Size(u * 0.76f, u * 0.52f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(u * 0.10f),
                    style = Stroke(width = stroke),
                )
                drawCircle(
                    color = tint,
                    radius = u * 0.13f,
                    center = androidx.compose.ui.geometry.Offset(u * 0.50f, u * 0.54f),
                    style = Stroke(width = stroke),
                )
                drawLine(
                    tint,
                    androidx.compose.ui.geometry.Offset(u * 0.34f, u * 0.28f),
                    androidx.compose.ui.geometry.Offset(u * 0.41f, u * 0.18f),
                    stroke,
                    cap = cap,
                )
                drawLine(
                    tint,
                    androidx.compose.ui.geometry.Offset(u * 0.41f, u * 0.18f),
                    androidx.compose.ui.geometry.Offset(u * 0.59f, u * 0.18f),
                    stroke,
                    cap = cap,
                )
            }
            "image" in normalized || "photo" in normalized -> {
                drawRoundRect(
                    color = tint,
                    topLeft = androidx.compose.ui.geometry.Offset(u * 0.12f, u * 0.16f),
                    size = androidx.compose.ui.geometry.Size(u * 0.76f, u * 0.68f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(u * 0.10f),
                    style = Stroke(width = stroke),
                )
                drawCircle(
                    color = tint,
                    radius = u * 0.07f,
                    center = androidx.compose.ui.geometry.Offset(u * 0.35f, u * 0.38f),
                )
                drawLine(
                    tint,
                    androidx.compose.ui.geometry.Offset(u * 0.20f, u * 0.72f),
                    androidx.compose.ui.geometry.Offset(u * 0.43f, u * 0.50f),
                    stroke,
                    cap = cap,
                )
                drawLine(
                    tint,
                    androidx.compose.ui.geometry.Offset(u * 0.43f, u * 0.50f),
                    androidx.compose.ui.geometry.Offset(u * 0.58f, u * 0.64f),
                    stroke,
                    cap = cap,
                )
                drawLine(
                    tint,
                    androidx.compose.ui.geometry.Offset(u * 0.58f, u * 0.64f),
                    androidx.compose.ui.geometry.Offset(u * 0.78f, u * 0.44f),
                    stroke,
                    cap = cap,
                )
            }
            "list" in normalized || "check" in normalized -> {
                drawRoundRect(
                    color = tint,
                    topLeft = androidx.compose.ui.geometry.Offset(u * 0.18f, u * 0.16f),
                    size = androidx.compose.ui.geometry.Size(u * 0.64f, u * 0.68f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(u * 0.08f),
                    style = Stroke(width = stroke),
                )
                listOf(0.34f, 0.50f, 0.66f).forEach { y ->
                    drawCircle(
                        color = tint,
                        radius = u * 0.035f,
                        center = androidx.compose.ui.geometry.Offset(u * 0.32f, u * y),
                    )
                    drawLine(
                        tint,
                        androidx.compose.ui.geometry.Offset(u * 0.43f, u * y),
                        androidx.compose.ui.geometry.Offset(u * 0.70f, u * y),
                        stroke,
                        cap = cap,
                    )
                }
            }
            "draw" in normalized || "sketch" in normalized -> {
                drawLine(
                    tint,
                    androidx.compose.ui.geometry.Offset(u * 0.22f, u * 0.76f),
                    androidx.compose.ui.geometry.Offset(u * 0.70f, u * 0.28f),
                    stroke,
                    cap = cap,
                )
                drawLine(
                    tint,
                    androidx.compose.ui.geometry.Offset(u * 0.62f, u * 0.20f),
                    androidx.compose.ui.geometry.Offset(u * 0.78f, u * 0.36f),
                    stroke,
                    cap = cap,
                )
                drawLine(
                    tint,
                    androidx.compose.ui.geometry.Offset(u * 0.20f, u * 0.80f),
                    androidx.compose.ui.geometry.Offset(u * 0.38f, u * 0.76f),
                    stroke,
                    cap = cap,
                )
            }
            "search" in normalized || "find" in normalized -> {
                drawCircle(
                    color = tint,
                    radius = u * 0.24f,
                    center = androidx.compose.ui.geometry.Offset(u * 0.42f, u * 0.42f),
                    style = Stroke(width = stroke),
                )
                drawLine(
                    tint,
                    androidx.compose.ui.geometry.Offset(u * 0.60f, u * 0.60f),
                    androidx.compose.ui.geometry.Offset(u * 0.84f, u * 0.84f),
                    stroke,
                    cap = cap,
                )
            }
            "note" in normalized || "text" in normalized -> {
                drawRoundRect(
                    color = tint,
                    topLeft = androidx.compose.ui.geometry.Offset(u * 0.22f, u * 0.14f),
                    size = androidx.compose.ui.geometry.Size(u * 0.56f, u * 0.72f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(u * 0.07f),
                    style = Stroke(width = stroke),
                )
                listOf(0.38f, 0.52f, 0.66f).forEach { y ->
                    drawLine(
                        tint,
                        androidx.compose.ui.geometry.Offset(u * 0.34f, u * y),
                        androidx.compose.ui.geometry.Offset(u * 0.66f, u * y),
                        stroke,
                        cap = cap,
                    )
                }
            }
            "template" in normalized || "document" in normalized || "new" in normalized -> {
                drawRoundRect(
                    color = tint,
                    topLeft = androidx.compose.ui.geometry.Offset(u * 0.22f, u * 0.12f),
                    size = androidx.compose.ui.geometry.Size(u * 0.56f, u * 0.76f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(u * 0.08f),
                    style = Stroke(width = stroke),
                )
                drawLine(
                    tint,
                    androidx.compose.ui.geometry.Offset(u * 0.50f, u * 0.40f),
                    androidx.compose.ui.geometry.Offset(u * 0.50f, u * 0.70f),
                    stroke,
                    cap = cap,
                )
                drawLine(
                    tint,
                    androidx.compose.ui.geometry.Offset(u * 0.35f, u * 0.55f),
                    androidx.compose.ui.geometry.Offset(u * 0.65f, u * 0.55f),
                    stroke,
                    cap = cap,
                )
            }
            else -> {
                // Generic Android app shortcut: use an action/launch arrow rather than a
                // misleading Search magnifier when the shortcut label has no known semantic.
                drawRoundRect(
                    color = tint,
                    topLeft = androidx.compose.ui.geometry.Offset(u * 0.16f, u * 0.30f),
                    size = androidx.compose.ui.geometry.Size(u * 0.52f, u * 0.52f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(u * 0.08f),
                    style = Stroke(width = stroke),
                )
                drawLine(
                    tint,
                    androidx.compose.ui.geometry.Offset(u * 0.44f, u * 0.56f),
                    androidx.compose.ui.geometry.Offset(u * 0.82f, u * 0.18f),
                    stroke,
                    cap = cap,
                )
                drawLine(
                    tint,
                    androidx.compose.ui.geometry.Offset(u * 0.58f, u * 0.18f),
                    androidx.compose.ui.geometry.Offset(u * 0.82f, u * 0.18f),
                    stroke,
                    cap = cap,
                )
                drawLine(
                    tint,
                    androidx.compose.ui.geometry.Offset(u * 0.82f, u * 0.18f),
                    androidx.compose.ui.geometry.Offset(u * 0.82f, u * 0.42f),
                    stroke,
                    cap = cap,
                )
            }
        }
    }
}

/** Local result actions are explicit user taps; Message opens the system's default SMS handler. */
private fun copyQuickAnswer(
    context: android.content.Context,
    action: LauncherCopyTextSearchAction,
) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("Quick answer", action.text))
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
        Toast.makeText(context, "Answer copied", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun LauncherGlazeSearchResult(
    result: LauncherSearchResult,
    sourceLabel: String?,
    onActivate: () -> Unit,
    onOpenSearchUri: (LauncherOpenUriSearchAction) -> Unit,
    prominent: Boolean = false,
) {
    val isContact = result.category == LauncherSearchCategory.CONTACT
    val appAction = result.action as? LaunchApplicationSearchAction
    val appIcon = if (appAction != null) rememberLauncherAppIcon(appAction.app) else null
    val profileKind = when (val action = result.action) {
        is LaunchApplicationSearchAction ->
            launcherProfileBadgeKindForUser(
                action.app.user,
                android.os.Process.myUserHandle(),
            )
        is LauncherLaunchShortcutSearchAction ->
            launcherProfileBadgeKindForUser(
                action.user,
                android.os.Process.myUserHandle(),
            )
        else -> null
    }
    val number = result.subtitle?.takeIf { it.any(Char::isDigit) }
    val iconSize = if (prominent) 42.dp else 34.dp
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(
            if (prominent) GlazeMetrics.radiusLarge else GlazeMetrics.radiusMedium,
        ),
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (prominent) 0.68f else 0.50f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.onSurface.copy(alpha = if (prominent) 0.07f else 0.05f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = if (prominent) 8.dp else 2.dp),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = if (prominent) 56.dp else 48.dp),
                onClick = onActivate,
                shape = RoundedCornerShape(GlazeMetrics.radiusMedium),
                color = Color.Transparent,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(modifier = Modifier.size(iconSize), contentAlignment = Alignment.Center) {
                        if (appIcon != null) {
                            Image(
                                bitmap = appIcon,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().launcherIconMask(),
                            )
                        } else if (isContact) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                shape = RoundedCornerShape(GlazeMetrics.radiusPill),
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        result.title.trim().take(1).uppercase(),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }
                        } else {
                            LauncherSearchResultCategoryGlyph(result.category)
                        }
                        profileKind?.let { kind ->
                            LauncherProfileBadge(
                                kind = kind,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset(x = (-3).dp, y = (-3).dp),
                                compact = true,
                            )
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            result.title,
                            style = if (prominent) {
                                MaterialTheme.typography.titleSmall
                            } else {
                                MaterialTheme.typography.bodyMedium
                            },
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
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
                    if (isContact && number != null) {
                        LauncherContactQuickAction(
                            type = LauncherContactQuickActionType.CALL,
                            contentDescription = "Call " + result.title,
                            onClick = {
                                onOpenSearchUri(
                                    LauncherOpenUriSearchAction(
                                        Intent.ACTION_DIAL,
                                        Uri.fromParts("tel", number, null).toString(),
                                    ),
                                )
                            },
                        )
                        LauncherContactQuickAction(
                            type = LauncherContactQuickActionType.MESSAGE,
                            contentDescription = "Message " + result.title,
                            onClick = {
                                onOpenSearchUri(
                                    LauncherOpenUriSearchAction(
                                        Intent.ACTION_SENDTO,
                                        Uri.fromParts("smsto", number, null).toString(),
                                    ),
                                )
                            },
                        )
                    }
                    LauncherSearchResultTrailingGlyph(
                        copy = result.action is LauncherCopyTextSearchAction,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun LauncherSearchResultCategoryGlyph(
    category: LauncherSearchCategory,
) {
    val color = MaterialTheme.colorScheme.primary
    val glyph = when (category) {
        LauncherSearchCategory.APPLICATION -> LauncherOutlineGlyph.APPS
        LauncherSearchCategory.SHORTCUT -> LauncherOutlineGlyph.SHORTCUT
        LauncherSearchCategory.CONTACT -> LauncherOutlineGlyph.CONTACT
        LauncherSearchCategory.CALL_HISTORY -> LauncherOutlineGlyph.PHONE
        LauncherSearchCategory.MESSAGE -> LauncherOutlineGlyph.MESSAGE
        LauncherSearchCategory.FILE -> LauncherOutlineGlyph.FILE
        LauncherSearchCategory.CONNECTED_SOURCE -> LauncherOutlineGlyph.SEARCH
        LauncherSearchCategory.SETTING -> LauncherOutlineGlyph.SETTINGS
        LauncherSearchCategory.ACTION -> LauncherOutlineGlyph.ACTION
    }
    Surface(
        modifier = Modifier.size(38.dp),
        shape = RoundedCornerShape(GlazeMetrics.radiusPill),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            LauncherOutlineGlyph(
                glyph = glyph,
                color = color,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private enum class LauncherContactQuickActionType {
    CALL,
    MESSAGE,
}

@Composable
private fun LauncherContactQuickAction(
    type: LauncherContactQuickActionType,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val tint = MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .semantics { this.contentDescription = contentDescription },
        shape = RoundedCornerShape(GlazeMetrics.radiusPill),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(18.dp)) {
                val u = size.minDimension
                val stroke = u * 0.09f
                val cap = androidx.compose.ui.graphics.StrokeCap.Round
                when (type) {
                    LauncherContactQuickActionType.CALL -> {
                        val handset = androidx.compose.ui.graphics.Path().apply {
                            moveTo(u * 0.28f, u * 0.19f)
                            cubicTo(
                                u * 0.18f, u * 0.31f,
                                u * 0.27f, u * 0.58f,
                                u * 0.46f, u * 0.73f,
                            )
                            cubicTo(
                                u * 0.61f, u * 0.85f,
                                u * 0.78f, u * 0.84f,
                                u * 0.82f, u * 0.70f,
                            )
                        }
                        drawPath(
                            path = handset,
                            color = tint,
                            style = Stroke(
                                width = stroke * 1.22f,
                                cap = cap,
                                join = androidx.compose.ui.graphics.StrokeJoin.Round,
                            ),
                        )
                    }
                    LauncherContactQuickActionType.MESSAGE -> {
                        drawRoundRect(
                            color = tint,
                            topLeft = androidx.compose.ui.geometry.Offset(u * 0.14f, u * 0.22f),
                            size = androidx.compose.ui.geometry.Size(u * 0.72f, u * 0.50f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(u * 0.15f),
                            style = Stroke(width = stroke),
                        )
                        drawLine(
                            tint,
                            androidx.compose.ui.geometry.Offset(u * 0.34f, u * 0.72f),
                            androidx.compose.ui.geometry.Offset(u * 0.27f, u * 0.84f),
                            stroke,
                            cap = cap,
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun LauncherSearchResultTrailingGlyph(
    copy: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(18.dp)) {
        val u = size.minDimension
        val stroke = u * 0.09f
        val cap = androidx.compose.ui.graphics.StrokeCap.Round
        if (copy) {
            drawRoundRect(
                color = tint,
                topLeft = androidx.compose.ui.geometry.Offset(u * 0.30f, u * 0.18f),
                size = androidx.compose.ui.geometry.Size(u * 0.48f, u * 0.58f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(u * 0.08f),
                style = Stroke(width = stroke),
            )
            drawRoundRect(
                color = tint.copy(alpha = 0.72f),
                topLeft = androidx.compose.ui.geometry.Offset(u * 0.16f, u * 0.32f),
                size = androidx.compose.ui.geometry.Size(u * 0.48f, u * 0.52f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(u * 0.08f),
                style = Stroke(width = stroke),
            )
        } else {
            drawLine(
                tint,
                androidx.compose.ui.geometry.Offset(u * 0.36f, u * 0.24f),
                androidx.compose.ui.geometry.Offset(u * 0.64f, u * 0.50f),
                stroke,
                cap = cap,
            )
            drawLine(
                tint,
                androidx.compose.ui.geometry.Offset(u * 0.64f, u * 0.50f),
                androidx.compose.ui.geometry.Offset(u * 0.36f, u * 0.76f),
                stroke,
                cap = cap,
            )
        }
    }
}

@Composable
private fun LauncherBackGlyph(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(20.dp)) {
        val u = size.minDimension
        val stroke = u * 0.11f
        val cap = androidx.compose.ui.graphics.StrokeCap.Round
        drawLine(
            tint,
            androidx.compose.ui.geometry.Offset(u * 0.68f, u * 0.18f),
            androidx.compose.ui.geometry.Offset(u * 0.34f, u * 0.50f),
            stroke,
            cap = cap,
        )
        drawLine(
            tint,
            androidx.compose.ui.geometry.Offset(u * 0.34f, u * 0.50f),
            androidx.compose.ui.geometry.Offset(u * 0.68f, u * 0.82f),
            stroke,
            cap = cap,
        )
    }
}

@Composable
private fun LauncherPrivacyShieldGlyph(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(24.dp)) {
        val u = size.minDimension
        val stroke = u * 0.075f
        val shield = androidx.compose.ui.graphics.Path().apply {
            moveTo(u * 0.50f, u * 0.08f)
            lineTo(u * 0.80f, u * 0.20f)
            lineTo(u * 0.76f, u * 0.58f)
            lineTo(u * 0.50f, u * 0.90f)
            lineTo(u * 0.24f, u * 0.58f)
            lineTo(u * 0.20f, u * 0.20f)
            close()
        }
        drawPath(shield, color = tint, style = Stroke(width = stroke))
    }
}

@Composable
private fun LauncherSourceDisclosureGlyph(
    expanded: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(18.dp)) {
        val u = size.minDimension
        val stroke = u * 0.095f
        val cap = androidx.compose.ui.graphics.StrokeCap.Round
        if (expanded) {
            drawLine(
                tint,
                androidx.compose.ui.geometry.Offset(u * 0.24f, u * 0.62f),
                androidx.compose.ui.geometry.Offset(u * 0.50f, u * 0.36f),
                stroke,
                cap = cap,
            )
            drawLine(
                tint,
                androidx.compose.ui.geometry.Offset(u * 0.50f, u * 0.36f),
                androidx.compose.ui.geometry.Offset(u * 0.76f, u * 0.62f),
                stroke,
                cap = cap,
            )
        } else {
            drawLine(
                tint,
                androidx.compose.ui.geometry.Offset(u * 0.38f, u * 0.24f),
                androidx.compose.ui.geometry.Offset(u * 0.64f, u * 0.50f),
                stroke,
                cap = cap,
            )
            drawLine(
                tint,
                androidx.compose.ui.geometry.Offset(u * 0.64f, u * 0.50f),
                androidx.compose.ui.geometry.Offset(u * 0.38f, u * 0.76f),
                stroke,
                cap = cap,
            )
        }
    }
}

@Composable
private fun LauncherSearchSectionGlyph(
    section: LauncherSearchSourceSection,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(24.dp)) {
        val u = size.minDimension
        val stroke = u * 0.075f
        when (section) {
            LauncherSearchSourceSection.DEVICE -> {
                drawRoundRect(
                    color = tint,
                    topLeft = androidx.compose.ui.geometry.Offset(u * 0.27f, u * 0.10f),
                    size = androidx.compose.ui.geometry.Size(u * 0.46f, u * 0.80f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                        u * 0.09f,
                        u * 0.09f,
                    ),
                    style = Stroke(width = stroke),
                )
                drawCircle(
                    color = tint,
                    radius = u * 0.025f,
                    center = androidx.compose.ui.geometry.Offset(u * 0.50f, u * 0.80f),
                )
            }
            LauncherSearchSourceSection.PERSONAL -> {
                drawCircle(
                    color = tint,
                    radius = u * 0.15f,
                    center = androidx.compose.ui.geometry.Offset(u * 0.50f, u * 0.34f),
                )
                drawLine(
                    color = tint,
                    start = androidx.compose.ui.geometry.Offset(u * 0.25f, u * 0.78f),
                    end = androidx.compose.ui.geometry.Offset(u * 0.75f, u * 0.78f),
                    strokeWidth = stroke * 1.45f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
            }
            LauncherSearchSourceSection.CONNECTED -> {
                drawCircle(
                    color = tint,
                    radius = u * 0.16f,
                    center = androidx.compose.ui.geometry.Offset(u * 0.40f, u * 0.50f),
                    style = Stroke(width = stroke),
                )
                drawCircle(
                    color = tint,
                    radius = u * 0.20f,
                    center = androidx.compose.ui.geometry.Offset(u * 0.56f, u * 0.42f),
                    style = Stroke(width = stroke),
                )
                drawCircle(
                    color = tint,
                    radius = u * 0.14f,
                    center = androidx.compose.ui.geometry.Offset(u * 0.70f, u * 0.54f),
                    style = Stroke(width = stroke),
                )
                drawLine(
                    color = tint,
                    start = androidx.compose.ui.geometry.Offset(u * 0.28f, u * 0.66f),
                    end = androidx.compose.ui.geometry.Offset(u * 0.78f, u * 0.66f),
                    strokeWidth = stroke,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
private fun LauncherSearchSourceBadge(
    option: LauncherSearchProviderControlOption,
    section: LauncherSearchSourceSection,
    accent: Color,
    apps: List<LauncherActivityInfo>,
) {
    Surface(
        modifier = Modifier.size(40.dp),
        shape = RoundedCornerShape(GlazeMetrics.radiusMedium),
        color = accent.copy(alpha = 0.10f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (section == LauncherSearchSourceSection.CONNECTED) {
                val packageNames = remember(option.providerId) {
                    LauncherConnectedSearchProviderRegistry.iconPackageNamesFor(option.providerId)
                }
                val sourceApp = remember(apps, packageNames) {
                    apps.firstOrNull { app -> app.componentName.packageName in packageNames }
                }
                val icon = sourceApp?.let { rememberLauncherAppIcon(it) }
                if (icon != null) {
                    Image(
                        bitmap = icon,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(24.dp).launcherIconMask(),
                    )
                } else {
                    LauncherConnectedProviderFallbackGlyph(
                        providerId = option.providerId,
                        modifier = Modifier.size(26.dp),
                    )
                }
            } else {
                LauncherLocalSearchSourceGlyph(
                    providerId = option.providerId,
                    tint = accent,
                )
            }
        }
    }
}

@Composable
private fun LauncherLocalSearchSourceGlyph(
    providerId: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val glyph = when (providerId) {
        LauncherQuickAnswersSearchProvider.PROVIDER_ID -> LauncherOutlineGlyph.QUICK_ANSWER
        LauncherCoreActionsSearchProvider.PROVIDER_ID -> LauncherOutlineGlyph.ACTION
        LauncherInstalledAppsSearchProvider.PROVIDER_ID -> LauncherOutlineGlyph.APPS
        LauncherShortcutsSearchProvider.PROVIDER_ID -> LauncherOutlineGlyph.SHORTCUT
        LauncherContactsSearchProvider.PROVIDER_ID -> LauncherOutlineGlyph.CONTACT
        LauncherCallHistorySearchProvider.PROVIDER_ID -> LauncherOutlineGlyph.PHONE
        LauncherMessagesSearchProvider.PROVIDER_ID -> LauncherOutlineGlyph.MESSAGE
        LauncherFilesSearchProvider.PROVIDER_ID -> LauncherOutlineGlyph.FOLDER
        else -> LauncherOutlineGlyph.SEARCH
    }
    LauncherOutlineGlyph(
        glyph = glyph,
        color = tint,
        modifier = modifier.size(24.dp),
    )
}

private enum class LauncherSourceToolbarActionType {
    RESET,
    ORDER,
    DONE,
}

@Composable
private fun LauncherSourceToolbarAction(
    action: LauncherSourceToolbarActionType,
    enabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val tint = if (enabled) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.36f)
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(40.dp)
            .semantics { this.contentDescription = contentDescription },
        shape = RoundedCornerShape(GlazeMetrics.radiusPill),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(
            alpha = if (enabled) 0.42f else 0.20f,
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.onSurface.copy(
                alpha = if (enabled) 0.06f else 0.03f,
            ),
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(18.dp)) {
                val u = size.minDimension
                val stroke = u * 0.09f
                val cap = androidx.compose.ui.graphics.StrokeCap.Round
                when (action) {
                    LauncherSourceToolbarActionType.RESET -> {
                        drawArc(
                            color = tint,
                            startAngle = -35f,
                            sweepAngle = 290f,
                            useCenter = false,
                            topLeft = androidx.compose.ui.geometry.Offset(u * 0.14f, u * 0.14f),
                            size = androidx.compose.ui.geometry.Size(u * 0.72f, u * 0.72f),
                            style = Stroke(width = stroke),
                        )
                        drawLine(
                            tint,
                            androidx.compose.ui.geometry.Offset(u * 0.19f, u * 0.17f),
                            androidx.compose.ui.geometry.Offset(u * 0.19f, u * 0.38f),
                            stroke,
                            cap = cap,
                        )
                        drawLine(
                            tint,
                            androidx.compose.ui.geometry.Offset(u * 0.19f, u * 0.17f),
                            androidx.compose.ui.geometry.Offset(u * 0.40f, u * 0.17f),
                            stroke,
                            cap = cap,
                        )
                    }
                    LauncherSourceToolbarActionType.ORDER -> {
                        listOf(0.28f, 0.50f, 0.72f).forEach { y ->
                            drawCircle(
                                color = tint,
                                radius = u * 0.045f,
                                center = androidx.compose.ui.geometry.Offset(u * 0.20f, u * y),
                            )
                            drawLine(
                                tint,
                                androidx.compose.ui.geometry.Offset(u * 0.36f, u * y),
                                androidx.compose.ui.geometry.Offset(u * 0.82f, u * y),
                                stroke,
                                cap = cap,
                            )
                        }
                    }
                    LauncherSourceToolbarActionType.DONE -> {
                        drawLine(
                            tint,
                            androidx.compose.ui.geometry.Offset(u * 0.18f, u * 0.52f),
                            androidx.compose.ui.geometry.Offset(u * 0.40f, u * 0.73f),
                            stroke,
                            cap = cap,
                        )
                        drawLine(
                            tint,
                            androidx.compose.ui.geometry.Offset(u * 0.40f, u * 0.73f),
                            androidx.compose.ui.geometry.Offset(u * 0.82f, u * 0.28f),
                            stroke,
                            cap = cap,
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun LauncherSearchSourceManager(
    apps: List<LauncherActivityInfo>,
    suggestionPresentation: LauncherSearchSuggestionPresentation,
    onSelectSuggestionPresentation: (LauncherSearchSuggestionPresentation) -> Unit,
    directApiSources: List<LauncherDirectApiSource>,
    directApiStorageError: String?,
    onSaveDirectApi: (LauncherDirectApiSource) -> String?,
    onRemoveDirectApi: (String) -> String?,
    onResetAllDirectApi: () -> String?,
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
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item(key = "suggestion-presentation") {
            LauncherSearchSuggestionPresentationControl(
                selected = suggestionPresentation,
                onSelect = onSelectSuggestionPresentation,
            )
        }

        item(key = "direct-api-connections") {
            LauncherDirectApiSourcesSettings(
                sources = directApiSources,
                apps = apps,
                onSave = onSaveDirectApi,
                onRemove = onRemoveDirectApi,
                onResetAll = onResetAllDirectApi,
                storageUnavailable = directApiStorageError != null,
            )
            directApiStorageError?.let { message ->
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        item(key = "provider-controls") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.54f),
                border = null,
                shadowElevation = 0.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = GlazeMetrics.space2,
                            end = GlazeMetrics.space1,
                            top = 6.dp,
                            bottom = 6.dp,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                ) {
                    Surface(
                        modifier = Modifier.size(34.dp),
                        shape = RoundedCornerShape(GlazeMetrics.radiusMedium),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.11f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            LauncherPrivacyShieldGlyph(
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Private by default",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            if (ready) {
                                "Local stays local · connected sources are optional."
                            } else {
                                "Loading source controls…"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    LauncherSourceToolbarAction(
                        action = LauncherSourceToolbarActionType.RESET,
                        enabled = ready,
                        contentDescription = "Reset Search source order and enabled defaults",
                        onClick = onReset,
                    )
                    LauncherSourceToolbarAction(
                        action = if (reorderMode) {
                            LauncherSourceToolbarActionType.DONE
                        } else {
                            LauncherSourceToolbarActionType.ORDER
                        },
                        enabled = ready && controls.orderedOptions.size > 1,
                        contentDescription = if (reorderMode) {
                            "Finish ordering Search sources"
                        } else {
                            "Reorder Search sources"
                        },
                        onClick = { reorderMode = !reorderMode },
                    )
                }
            }
        }

        sections.forEach { (section, options) ->
            item(key = "section-block-" + section.name) {
                val accent = when (section) {
                    LauncherSearchSourceSection.DEVICE -> MaterialTheme.colorScheme.primary
                    LauncherSearchSourceSection.PERSONAL -> MaterialTheme.colorScheme.tertiary
                    LauncherSearchSourceSection.CONNECTED -> MaterialTheme.colorScheme.secondary
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f),
                    border = null,
                    shadowElevation = 0.dp,
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = GlazeMetrics.space2,
                                    end = GlazeMetrics.space3,
                                    top = 8.dp,
                                    bottom = 8.dp,
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                        ) {
                            Surface(
                                modifier = Modifier.size(32.dp),
                                shape = RoundedCornerShape(GlazeMetrics.radiusMedium),
                                color = accent.copy(alpha = 0.11f),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    LauncherSearchSectionGlyph(section = section, tint = accent)
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    section.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    section.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            LauncherSearchSourceStatusPill(
                                label = controls.orderedOptions.count { option ->
                                    option in options && controls.isEnabled(option.providerId)
                                }.toString() + "/" + options.size + " enabled",
                                isError = false,
                            )
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(GlazeMetrics.radiusMedium),
                            color = Color.Transparent,
                            border = null,
                        ) {
                            Column {
                                options.forEachIndexed { sectionIndex, option ->
                                    val index = controls.orderedOptions.indexOfFirst {
                                        it.providerId == option.providerId
                                    }
                                    val enabled = controls.isEnabled(option.providerId)
                                    val permissionGranted =
                                        LauncherLocalSearchPermissions.isGranted(
                                            context,
                                            option.providerId,
                                        )
                                    val driveSource =
                                        option.providerId ==
                                            LauncherConnectedSearchProviderRegistry
                                                .GOOGLE_DRIVE_PROVIDER_ID
                                    val driveConnected =
                                        driveSource &&
                                            LauncherGoogleDriveAuthorizationState.isConnected()
                                    val driveConnectionAvailable =
                                        !driveSource || launcherDriveInlineConnectionAvailable(
                                            debugBuild = BuildConfig.DEBUG,
                                            alreadyConnected = driveConnected,
                                        )
                                    val connectedHandoffAvailable =
                                        !option.providerId.startsWith("connected.") ||
                                            LauncherConnectedSearchProviderRegistry
                                                .isExplicitHandoffAvailable(
                                                    context,
                                                    option.providerId,
                                                )
                                    val providerReady = when {
                                        driveSource -> driveConnected
                                        else -> connectedHandoffAvailable
                                    }
                                    val issue = issues[option.providerId]
                                    val isMessages =
                                        option.providerId ==
                                            LauncherMessagesSearchProvider.PROVIDER_ID
                                    val expanded = detailProviderId == option.providerId
                                    val errorMessage = when {
                                        issue == LauncherLocalSearchIssue.ANDROID_RESTRICTED &&
                                            isMessages ->
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
                                            "SMS permission"
                                        !permissionGranted ->
                                            "Needs permission"
                                        option.providerId ==
                                            LauncherFilesSearchProvider.PROVIDER_ID &&
                                            fileSearchRoots.isEmpty() ->
                                            "Choose folder"
                                        driveSource && !driveConnectionAvailable ->
                                            "Signed build required"
                                        !connectedHandoffAvailable &&
                                            option.providerId ==
                                                LauncherConnectedSearchProviderRegistry
                                                    .DROPBOX_PROVIDER_ID ->
                                            "Dropbox app required"
                                        !connectedHandoffAvailable ->
                                            "Provider unavailable"
                                        else -> null
                                    }
                                    val rowSecondaryText =
                                        statusLabel ?: compactSourceSummary(
                                            context = context,
                                            option = option,
                                            fileSearchRoots = fileSearchRoots,
                                        )
                                    val rowSecondaryColor = when {
                                        errorMessage != null -> MaterialTheme.colorScheme.error
                                        statusLabel != null -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }

                                    if (sectionIndex > 0) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(start = 62.dp),
                                            color = MaterialTheme.colorScheme.onSurface
                                                .copy(alpha = 0.055f),
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
                                                horizontal = 12.dp,
                                                vertical = 6.dp,
                                            ),
                                        verticalArrangement =
                                            Arrangement.spacedBy(GlazeMetrics.space1),
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement =
                                                Arrangement.spacedBy(GlazeMetrics.space2),
                                        ) {
                                            LauncherSearchSourceBadge(
                                                option = option,
                                                section = section,
                                                accent = accent,
                                                apps = apps,
                                            )

                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    option.displayName,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                Text(
                                                    rowSecondaryText,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = rowSecondaryColor,
                                                    fontWeight = if (statusLabel != null) {
                                                        FontWeight.SemiBold
                                                    } else {
                                                        FontWeight.Normal
                                                    },
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.widthIn(
                                                    min = 88.dp,
                                                    max = 88.dp,
                                                ),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Switch(
                                                    checked =
                                                        enabled &&
                                                            permissionGranted &&
                                                            providerReady,
                                                    onCheckedChange = {
                                                        onSetEnabled(
                                                            controls,
                                                            option.providerId,
                                                            it,
                                                        )
                                                    },
                                                    enabled =
                                                        ready &&
                                                            driveConnectionAvailable &&
                                                            connectedHandoffAvailable,
                                                    modifier = Modifier.testTag(
                                                        "launcher-search-source-" +
                                                            option.providerId,
                                                    ),
                                                )
                                                LauncherSourceDisclosureGlyph(
                                                    expanded = expanded,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                        .copy(alpha = 0.72f),
                                                )
                                            }
                                        }

                                        if (expanded) {
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(
                                                    GlazeMetrics.radiusMedium,
                                                ),
                                                color = MaterialTheme.colorScheme.surfaceVariant
                                                    .copy(alpha = 0.18f),
                                                border = null,
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(
                                                        GlazeMetrics.space2,
                                                    ),
                                                    verticalArrangement =
                                                        Arrangement.spacedBy(
                                                            GlazeMetrics.space1,
                                                        ),
                                                ) {
                                                    if (errorMessage != null) {
                                                        Text(
                                                            errorMessage,
                                                            style =
                                                                MaterialTheme.typography.bodySmall,
                                                            color =
                                                                MaterialTheme.colorScheme.error,
                                                        )
                                                    }
                                                    Text(
                                                        option.privacySummary,
                                                        style =
                                                            MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme
                                                            .onSurfaceVariant,
                                                    )
                                                    connectedSourceDetail(
                                                        context = context,
                                                        option = option,
                                                        fileSearchRoots = fileSearchRoots,
                                                    )?.let { detail ->
                                                        Text(
                                                            detail,
                                                            style =
                                                                MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme
                                                                .onSurfaceVariant,
                                                        )
                                                    }

                                                    if (
                                                        option.providerId ==
                                                            LauncherFilesSearchProvider.PROVIDER_ID
                                                    ) {
                                                        TextButton(
                                                            onClick = onChooseFileSearchRoot,
                                                            enabled = ready,
                                                            modifier =
                                                                Modifier.heightIn(min = 48.dp),
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
                                                                verticalAlignment =
                                                                    Alignment.CenterVertically,
                                                                horizontalArrangement =
                                                                    Arrangement.spacedBy(
                                                                        GlazeMetrics.space2,
                                                                    ),
                                                            ) {
                                                                Text(
                                                                    root.lastPathSegment
                                                                        ?.substringAfterLast(':')
                                                                        ?.takeIf {
                                                                            it.isNotBlank()
                                                                        }
                                                                        ?: "Selected folder",
                                                                    modifier =
                                                                        Modifier.weight(1f),
                                                                    style = MaterialTheme
                                                                        .typography.bodySmall,
                                                                    color = MaterialTheme
                                                                        .colorScheme
                                                                        .onSurfaceVariant,
                                                                    maxLines = 1,
                                                                    overflow =
                                                                        TextOverflow.Ellipsis,
                                                                )
                                                                TextButton(
                                                                    onClick = {
                                                                        onRemoveFileSearchRoot(
                                                                            root,
                                                                        )
                                                                    },
                                                                    enabled = ready,
                                                                ) {
                                                                    Text("Remove")
                                                                }
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
                                                ) {
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                    ) {
                                                        LauncherSourceMoveGlyph(
                                                            earlier = true,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                        )
                                                        Text("Earlier")
                                                    }
                                                }
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
                                                    enabled =
                                                        ready &&
                                                            index in 0 until
                                                                controls.orderedOptions.lastIndex,
                                                ) {
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                    ) {
                                                        LauncherSourceMoveGlyph(
                                                            earlier = false,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                        )
                                                        Text("Later")
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
            }
        }
    }
}

@Composable
private fun LauncherSourceMoveGlyph(
    earlier: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(16.dp)) {
        val u = size.minDimension
        val stroke = u * 0.10f
        val cap = androidx.compose.ui.graphics.StrokeCap.Round
        val centerX = u * 0.50f
        val startY = if (earlier) u * 0.76f else u * 0.24f
        val endY = if (earlier) u * 0.24f else u * 0.76f
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(centerX, startY),
            end = androidx.compose.ui.geometry.Offset(centerX, endY),
            strokeWidth = stroke,
            cap = cap,
        )
        val armY = if (earlier) u * 0.42f else u * 0.58f
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(centerX, endY),
            end = androidx.compose.ui.geometry.Offset(u * 0.30f, armY),
            strokeWidth = stroke,
            cap = cap,
        )
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(centerX, endY),
            end = androidx.compose.ui.geometry.Offset(u * 0.70f, armY),
            strokeWidth = stroke,
            cap = cap,
        )
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
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
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
    DEVICE("On-device", "Local · no network"),
    PERSONAL("Your content", "Contacts, messages, and files"),
    CONNECTED("Connected", "Optional external services"),
}

private fun sourceSectionFor(
    option: LauncherSearchProviderControlOption,
): LauncherSearchSourceSection = when (option.providerId) {
    LauncherContactsSearchProvider.PROVIDER_ID,
    LauncherCallHistorySearchProvider.PROVIDER_ID,
    LauncherMessagesSearchProvider.PROVIDER_ID,
    LauncherFilesSearchProvider.PROVIDER_ID,
    -> LauncherSearchSourceSection.PERSONAL

    else -> if (LauncherConnectedSearchProviderRegistry.isConnectedProvider(option.providerId)) {
        LauncherSearchSourceSection.CONNECTED
    } else {
        LauncherSearchSourceSection.DEVICE
    }
}

private fun compactSourceSummary(
    context: android.content.Context,
    option: LauncherSearchProviderControlOption,
    fileSearchRoots: List<Uri>,
): String = when (option.providerId) {
    LauncherFilesSearchProvider.PROVIDER_ID -> when (fileSearchRoots.size) {
        0 -> "No folder selected"
        1 -> "1 folder selected"
        else -> fileSearchRoots.size.toString() + " folders selected"
    }
    LauncherConnectedSearchProviderRegistry.GOOGLE_DRIVE_PROVIDER_ID ->
        if (LauncherGoogleDriveAuthorizationState.isConnected()) {
            "Cloud · Connected"
        } else {
            "Cloud · Optional"
        }
    LauncherConnectedSearchProviderRegistry.DROPBOX_PROVIDER_ID ->
        "App handoff · Optional"
    else -> when {
        LauncherConnectedSearchProviderRegistry.isAiProvider(option.providerId) ->
            "AI · Explicit handoff"
        LauncherConnectedSearchProviderRegistry.isWebSearchProvider(option.providerId) ->
            "Web · Explicit handoff"
        else -> when (option.invocationMode) {
        LauncherSearchProviderInvocationMode.AUTOMATIC_LOCAL -> "Local · Automatic"
        LauncherSearchProviderInvocationMode.OPT_IN_LOCAL -> "Local · Permission"
        LauncherSearchProviderInvocationMode.OPT_IN_REMOTE_INLINE -> "Connected · Optional"
        LauncherSearchProviderInvocationMode.EXPLICIT_USER_HANDOFF -> "Handoff · Optional"
        }
    }
}

private fun connectedSourceDetail(
    context: android.content.Context,
    option: LauncherSearchProviderControlOption,
    fileSearchRoots: List<Uri>,
): String? = when {
    option.providerId == LauncherConnectedSearchProviderRegistry.GOOGLE_DRIVE_PROVIDER_ID ->
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
    option.providerId == LauncherConnectedSearchProviderRegistry.DROPBOX_PROVIDER_ID ->
        "Dropbox inline results require a reviewed OAuth adapter. Until that authorization path " +
            "exists, Launcher keeps this source behind an explicit handoff."
    LauncherConnectedSearchProviderRegistry.isAiProvider(option.providerId) ->
        option.displayName + " receives the query only after you explicitly choose this source. " +
            "Launcher does not send typed queries to this AI service in the background. The " +
            "provider's own account, privacy, and retention rules apply."
    LauncherConnectedSearchProviderRegistry.isWebSearchProvider(option.providerId) ->
        option.displayName + " receives the query only after you explicitly choose this source. " +
            "Launcher does not fan typed queries out to web providers automatically. The provider's " +
            "own privacy and retention rules apply."
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
        shape = RoundedCornerShape(GlazeMetrics.radiusMedium),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.54f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f),
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    result.title,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
