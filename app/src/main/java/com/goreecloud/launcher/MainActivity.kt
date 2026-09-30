package com.goreecloud.launcher

import android.app.Activity
import android.app.AlertDialog
import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.ClearTokenRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.goreecloud.launcher.core.launcher.LauncherAppWidgetHostController
import com.goreecloud.launcher.core.launcher.LauncherAppsRepository
import com.goreecloud.launcher.core.launcher.LauncherBuiltInWallpaperId
import com.goreecloud.launcher.core.launcher.LauncherBuiltInWallpapers
import com.goreecloud.launcher.core.launcher.LauncherConnectedSearchProviderRegistry
import com.goreecloud.launcher.core.launcher.LauncherDrawerLayoutMode
import com.goreecloud.launcher.core.launcher.LauncherDockStyle
import com.goreecloud.launcher.core.launcher.LauncherExperiencePreferences
import com.goreecloud.launcher.core.launcher.LauncherFileSearchPreferencesRepository
import com.goreecloud.launcher.core.launcher.LauncherFilesSearchProvider
import com.goreecloud.launcher.core.launcher.LauncherFolder
import com.goreecloud.launcher.core.launcher.LauncherFolderProfilePolicy
import com.goreecloud.launcher.core.launcher.LauncherFolderRepository
import com.goreecloud.launcher.core.launcher.LauncherGoogleDriveAuthorizationState
import com.goreecloud.launcher.core.launcher.LauncherGestureAction
import com.goreecloud.launcher.core.launcher.LauncherGestureActionType
import com.goreecloud.launcher.core.launcher.LauncherInstalledAppBaselineRepository
import com.goreecloud.launcher.core.launcher.LauncherHomeAppMode
import com.goreecloud.launcher.core.launcher.LauncherHomeSearchPlacement
import com.goreecloud.launcher.core.launcher.LauncherHomeSpacing
import com.goreecloud.launcher.core.launcher.LauncherIconPackDescriptor
import com.goreecloud.launcher.core.launcher.LauncherIconPackRepository
import com.goreecloud.launcher.core.launcher.LauncherLaunchShortcutSearchAction
import com.goreecloud.launcher.core.launcher.LauncherLocalSearchPermissions
import com.goreecloud.launcher.core.launcher.LauncherLocalSearchDiagnostics
import com.goreecloud.launcher.core.launcher.LauncherLocalSearchIssue
import com.goreecloud.launcher.core.launcher.LauncherMessagesSearchProvider
import com.goreecloud.launcher.core.launcher.LauncherNotificationBadges
import com.goreecloud.launcher.core.launcher.LauncherFolderAppearance
import com.goreecloud.launcher.core.launcher.LauncherLocalUsageRepository
import com.goreecloud.launcher.core.launcher.LauncherOpenDocumentSearchAction
import com.goreecloud.launcher.core.launcher.LauncherOpenUriSearchAction
import com.goreecloud.launcher.core.launcher.LauncherPortableRestoreRecoveryCoordinator
import com.goreecloud.launcher.core.launcher.LauncherPortableRestoreStartupGate
import com.goreecloud.launcher.core.launcher.LauncherPortableRestoreStartupSequence
import com.goreecloud.launcher.core.launcher.LauncherPreferences
import com.goreecloud.launcher.core.launcher.LauncherPreferencesRepository
import com.goreecloud.launcher.core.launcher.LauncherSearchProviderControlState
import com.goreecloud.launcher.core.launcher.LauncherSearchProviderPreferenceDecodeResult
import com.goreecloud.launcher.core.launcher.LauncherSearchProviderPreferenceSnapshot
import com.goreecloud.launcher.core.launcher.LauncherSearchProviderUserControlPolicy
import com.goreecloud.launcher.core.launcher.LauncherSearchProviderPreferencesRepository
import com.goreecloud.launcher.core.launcher.LauncherUniversalSearchHomeMode
import com.goreecloud.launcher.core.launcher.LauncherUninstallRequestPolicy
import com.goreecloud.launcher.core.launcher.LauncherWallpaperShade
import com.goreecloud.launcher.core.launcher.LauncherVisualPreferences
import com.goreecloud.launcher.core.launcher.LauncherVisualPreferencesRepository
import com.goreecloud.launcher.core.launcher.LauncherWidgetProviderDescriptor
import com.goreecloud.launcher.core.launcher.StarterWorkspaceCandidate
import com.goreecloud.launcher.core.launcher.StarterWorkspacePolicy
import com.goreecloud.launcher.core.workspace.MAX_DOCK_ITEMS
import com.goreecloud.launcher.core.workspace.WorkspaceAuthority
import com.goreecloud.launcher.core.workspace.WorkspaceRepository
import com.goreecloud.launcher.core.workspace.WorkspaceState
import com.goreecloud.launcher.core.workspace.WorkspaceWidgetCatalog
import com.goreecloud.launcher.core.workspace.WorkspaceWidgetDescriptor
import com.goreecloud.launcher.core.workspace.db.LauncherDatabaseProvider
import com.goreecloud.launcher.core.workspace.db.WorkspaceAuthoritativePlacementState
import com.goreecloud.launcher.core.workspace.db.WorkspaceAuthoritativeWriteResult
import com.goreecloud.launcher.core.workspace.db.WorkspaceFolderMutationResult
import com.goreecloud.launcher.core.workspace.db.WorkspaceLegacyImportMapper
import com.goreecloud.launcher.core.workspace.db.WorkspacePagedHomeState
import com.goreecloud.launcher.core.workspace.db.WorkspacePagedRoomMutationResult
import com.goreecloud.launcher.core.workspace.db.WorkspacePlacementSource
import com.goreecloud.launcher.core.workspace.db.WorkspacePrimaryHomeSpatialResult
import com.goreecloud.launcher.core.workspace.db.WorkspaceProductionRuntimeCoordinator
import com.goreecloud.launcher.core.workspace.db.WorkspaceRenderedHomeWidget
import com.goreecloud.launcher.core.workspace.db.WorkspaceWidgetMutationResult
import com.goreecloud.launcher.core.workspace.workspaceKey
import com.goreecloud.launcher.ui.HomePageDots
import com.goreecloud.launcher.ui.HomePageManagerSheet
import com.goreecloud.launcher.ui.LauncherAppDragData
import com.goreecloud.launcher.ui.LauncherAppDragOrigin
import com.goreecloud.launcher.ui.LayoutLockHoldControl
import com.goreecloud.launcher.ui.LauncherBetaRoot
import com.goreecloud.launcher.ui.LauncherIconAppearance
import com.goreecloud.launcher.ui.LauncherHomeHintCard
import com.goreecloud.launcher.ui.LauncherStartupWizard
import com.goreecloud.launcher.ui.LocalLauncherIconAppearance
import com.goreecloud.launcher.ui.LauncherSurfaceMode
import com.goreecloud.launcher.ui.LauncherTransitionDiagnostics
import com.goreecloud.launcher.ui.LauncherWallpaperPickerSheet
import com.goreecloud.launcher.ui.ReadOnlyPagedHomeSurface
import com.goreecloud.launcher.ui.homePageCellAtPoint
import com.goreecloud.launcher.ui.homePageEdgeDropTarget
import com.goreecloud.launcher.ui.launcherAppDragData
import com.goreecloud.launcher.ui.launcherUsesDarkSystemBarIcons
import com.goreecloud.launcher.ui.rootDropPoint
import com.goreecloud.launcher.ui.theme.GlazeMetrics
import com.goreecloud.launcher.ui.theme.GlazeTheme
import com.goreecloud.launcher.ui.theme.GlazeThemeMode
import com.goreecloud.launcher.ui.homePageSwipeNavigation
import com.goreecloud.launcher.ui.homeVerticalGestureNavigation
import com.goreecloud.launcher.ui.theme.GlazeThemeRepository
import com.goreecloud.launcher.ui.theme.rememberAndroidGlazeV16PresentationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class MainActivity : ComponentActivity() {
    private lateinit var appsRepository: LauncherAppsRepository
    private lateinit var launcherPreferencesRepository: LauncherPreferencesRepository
    private lateinit var searchProviderPreferencesRepository: LauncherSearchProviderPreferencesRepository
    private lateinit var fileSearchPreferencesRepository: LauncherFileSearchPreferencesRepository
    private lateinit var installedAppBaselineRepository: LauncherInstalledAppBaselineRepository
    private lateinit var localUsageRepository: LauncherLocalUsageRepository
    private lateinit var folderRepository: LauncherFolderRepository
    private lateinit var appWidgetHostController: LauncherAppWidgetHostController
    private lateinit var themeRepository: GlazeThemeRepository
    private lateinit var workspaceRepository: WorkspaceRepository
    private lateinit var workspaceRuntimeCoordinator: WorkspaceProductionRuntimeCoordinator
    private val defaultHomeState = MutableStateFlow(false)
    private val homeResetSequence = MutableStateFlow(0L)
    private val searchProviderPreferencesState =
        MutableStateFlow<LauncherSearchProviderPreferenceDecodeResult?>(null)
    private val portableRestoreRecoveryResult =
        MutableStateFlow<LauncherPortableRestoreRecoveryCoordinator.Result?>(null)
    private var pendingAppWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private var showWallpaperPicker by mutableStateOf(false)
    private var pendingSearchProviderSnapshot: LauncherSearchProviderPreferenceSnapshot? = null
    private var pendingSearchProviderId: String? = null
    private var pendingFileSearchProviderSnapshot: LauncherSearchProviderPreferenceSnapshot? = null
    private var pendingFileSearchProviderId: String? = null
    private var pendingGoogleDriveSearchProviderSnapshot:
        LauncherSearchProviderPreferenceSnapshot? = null
    private var googleDriveStartupRestoreAttempted = false
    private val googleDriveAuthorizationClient by lazy {
        Identity.getAuthorizationClient(this)
    }


    private val fileSearchRootRequest =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            val pending = pendingFileSearchProviderSnapshot
            val pendingProviderId = pendingFileSearchProviderId
            pendingFileSearchProviderSnapshot = null
            pendingFileSearchProviderId = null
            if (uri == null) return@registerForActivityResult

            val persisted = runCatching {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }.isSuccess
            if (!persisted) {
                Toast.makeText(
                    this,
                    "Android did not grant persistent access to that folder.",
                    Toast.LENGTH_SHORT,
                ).show()
                return@registerForActivityResult
            }

            lifecycleScope.launch {
                fileSearchPreferencesRepository.addRoot(uri)
                if (pending != null) {
                    searchProviderPreferencesRepository.set(pending)
                }
            }
        }

    private val googleDriveAuthorizationRequest =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            val pending = pendingGoogleDriveSearchProviderSnapshot
            pendingGoogleDriveSearchProviderSnapshot = null
            if (result.resultCode != Activity.RESULT_OK || result.data == null || pending == null) {
                Toast.makeText(
                    this,
                    "Google Drive connection was not completed.",
                    Toast.LENGTH_SHORT,
                ).show()
                return@registerForActivityResult
            }

            val authorizationResult = runCatching {
                googleDriveAuthorizationClient.getAuthorizationResultFromIntent(result.data!!)
            }.getOrElse {
                Toast.makeText(
                    this,
                    "Google Drive authorization could not be read.",
                    Toast.LENGTH_SHORT,
                ).show()
                return@registerForActivityResult
            }
            acceptGoogleDriveAuthorization(authorizationResult, pending)
        }

    private val searchSourcePermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val pending = pendingSearchProviderSnapshot
            val source = pendingSearchProviderId
            pendingSearchProviderSnapshot = null
            pendingSearchProviderId = null
            if (granted && pending != null) {
                if (source != null) LauncherLocalSearchDiagnostics.record(source, null)
                lifecycleScope.launch {
                    searchProviderPreferencesRepository.set(pending)
                }
            } else if (!granted) {
                if (source != null) {
                    LauncherLocalSearchDiagnostics.record(
                        source,
                        if (source == LauncherMessagesSearchProvider.PROVIDER_ID)
                            LauncherLocalSearchIssue.ANDROID_RESTRICTED
                        else LauncherLocalSearchIssue.PERMISSION_REQUIRED,
                    )
                }
                Toast.makeText(
                    this,
                    if (source == LauncherMessagesSearchProvider.PROVIDER_ID)
                        "Android denied SMS access. Messages search stays off; Launcher cannot override it."
                    else "Android permission denied. This Search source remains disabled.",
                    Toast.LENGTH_LONG,
                ).show()
            }
        }

    private val widgetConfigureRequest =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val appWidgetId = widgetResultId(result.data)
            if (result.resultCode == Activity.RESULT_OK && appWidgetId > 0) {
                persistAndroidWidget(appWidgetId)
            } else {
                discardPendingAppWidget(appWidgetId)
            }
        }

    private val widgetPickerRequest =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val appWidgetId = widgetResultId(result.data)
            if (result.resultCode != Activity.RESULT_OK || appWidgetId <= 0) {
                discardPendingAppWidget(appWidgetId)
                return@registerForActivityResult
            }
            continueAndroidWidgetSetup(appWidgetId)
        }

    private val homeRoleRequest =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            refreshHomeRoleState()
        }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (LauncherHomeIntentPolicy.shouldResetToPrimaryHome(intent.action, intent.categories)) {
            homeResetSequence.value = homeResetSequence.value + 1L
        }
        refreshHomeRoleState()
    }

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LauncherNotificationBadges.initialize(this)
        LauncherFolderAppearance.initialize(this)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        appsRepository = LauncherAppsRepository(this)
        launcherPreferencesRepository = LauncherPreferencesRepository(this)
        searchProviderPreferencesRepository = LauncherSearchProviderPreferencesRepository(this)
        fileSearchPreferencesRepository = LauncherFileSearchPreferencesRepository(this)
        installedAppBaselineRepository = LauncherInstalledAppBaselineRepository(this)
        localUsageRepository = LauncherLocalUsageRepository(this)
        folderRepository = LauncherFolderRepository(this)
        appWidgetHostController = LauncherAppWidgetHostController(this)
        themeRepository = GlazeThemeRepository(this)
        workspaceRepository = WorkspaceRepository(this)
        workspaceRuntimeCoordinator = WorkspaceProductionRuntimeCoordinator(
            authorityRepository = workspaceRepository,
            workspaceDaoProvider = {
                LauncherDatabaseProvider.get(this).workspaceDao()
            },
        )
        lifecycleScope.launch {
            searchProviderPreferencesRepository.preferences.collect { decoded ->
                searchProviderPreferencesState.value = decoded
                if (
                    !googleDriveStartupRestoreAttempted &&
                    decoded is LauncherSearchProviderPreferenceDecodeResult.Loaded &&
                    LauncherConnectedSearchProviderRegistry.GOOGLE_DRIVE_PROVIDER_ID in
                        decoded.snapshot.enabledProviderIds
                ) {
                    googleDriveStartupRestoreAttempted = true
                    restoreGoogleDriveSearchAuthorizationIfGranted()
                }
            }
        }
        lifecycleScope.launch {
            val recovery = LauncherPortableRestoreStartupSequence.reconcileBeforeMutation(
                recoverPortableRestore = {
                    LauncherPortableRestoreRecoveryCoordinator(this@MainActivity).reconcile()
                },
                reconcileWorkspace = {
                    workspaceRuntimeCoordinator.reconcileAndActivate()
                    Unit
                },
            )
            portableRestoreRecoveryResult.value = recovery
        }
        refreshHomeRoleState()

        setContent {
            val themeMode by themeRepository.themeMode.collectAsState(initial = themeRepository.defaultMode)
            val systemInDarkTheme = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                GlazeThemeMode.SYSTEM -> systemInDarkTheme
                GlazeThemeMode.LIGHT -> false
                GlazeThemeMode.DARK,
                GlazeThemeMode.DEEP_DARK,
                -> true
            }
            val portableRestoreRecovery by portableRestoreRecoveryResult.collectAsStateWithLifecycle()

            if (!LauncherPortableRestoreStartupGate.allowsMutations(portableRestoreRecovery)) {
                LaunchedEffect(darkTheme) {
                    setSystemBarIconAppearance(useDarkIcons = !darkTheme)
                }
                val glazePresentationContext = rememberAndroidGlazeV16PresentationContext()
            GlazeTheme(themeMode, presentationContext = glazePresentationContext) {
                    Box(
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(24.dp),
                    ) {
                        Text(LauncherPortableRestoreStartupGate.userMessage(portableRestoreRecovery))
                    }
                }
                return@setContent
            }

            val appsState by produceState<List<LauncherActivityInfo>?>(initialValue = null) {
                appsRepository.apps.collect { value = it }
            }
            val launcherPreferencesState by produceState<LauncherPreferences?>(initialValue = null) {
                launcherPreferencesRepository.preferences.collect { value = it }
            }
            val experiencePreferencesState by produceState<LauncherExperiencePreferences?>(initialValue = null) {
                launcherPreferencesRepository.experiencePreferences.collect { value = it }
            }
            val placementState by produceState<WorkspaceAuthoritativePlacementState?>(initialValue = null) {
                workspaceRuntimeCoordinator.observePlacement().collect { value = it }
            }
            val pagedHomeState by produceState<WorkspacePagedHomeState?>(initialValue = null) {
                workspaceRuntimeCoordinator.observeHomePages().collect { value = it }
            }

            if (
                appsState == null ||
                launcherPreferencesState == null ||
                experiencePreferencesState == null ||
                placementState == null ||
                pagedHomeState == null
            ) {
                GlazeTheme(themeMode) {
                    Box(Modifier.fillMaxSize())
                }
                return@setContent
            }

            val apps = appsState!!
            val launcherPreferences = launcherPreferencesState!!
            val experiencePreferences = experiencePreferencesState!!
            val placement = placementState!!
            val pagedHome = pagedHomeState!!

            val availableAndroidWidgets = remember(apps) {
                appWidgetHostController.installedProviders()
            }
            val availableIconPacks = remember(apps) {
                LauncherIconPackRepository(this@MainActivity).discover()
            }
            val drawerLayoutMode by launcherPreferencesRepository.drawerLayoutMode.collectAsStateWithLifecycle(
                initialValue = LauncherDrawerLayoutMode.GRID,
            )
            val visualPreferencesRepository = remember {
                LauncherVisualPreferencesRepository(applicationContext)
            }
            val visualPreferences by visualPreferencesRepository.preferences.collectAsStateWithLifecycle(
                initialValue = LauncherVisualPreferences(),
            )
            val localLaunchCounts by localUsageRepository.launchCounts.collectAsStateWithLifecycle(
                initialValue = emptyMap(),
            )
            val localRecentAppKeys by localUsageRepository.recentAppKeys.collectAsStateWithLifecycle(
                initialValue = emptyList(),
            )
            val searchProviderPreferences by searchProviderPreferencesState.collectAsStateWithLifecycle()
            val fileSearchRoots by fileSearchPreferencesRepository.roots.collectAsStateWithLifecycle(
                initialValue = emptyList(),
            )
            val homeLabelOverrides by launcherPreferencesRepository.homeLabelOverrides.collectAsStateWithLifecycle(
                initialValue = emptyMap(),
            )
            val hiddenHomeSuggestionKeys by launcherPreferencesRepository.hiddenHomeSuggestionKeys.collectAsStateWithLifecycle(
                initialValue = emptySet(),
            )
            val folders by folderRepository.folders.collectAsStateWithLifecycle(
                initialValue = emptyList(),
            )
            val isDefaultHome by defaultHomeState.collectAsStateWithLifecycle()
            val homeResetSequenceValue by homeResetSequence.collectAsStateWithLifecycle()

            val workspace = when (val current = placement) {
                WorkspaceAuthoritativePlacementState.WaitingForInitialization -> WorkspaceState()
                is WorkspaceAuthoritativePlacementState.RecoveryRequired -> WorkspaceState(
                    initialized = true,
                    authority = WorkspaceAuthority.ROOM,
                )
                is WorkspaceAuthoritativePlacementState.Ready -> WorkspaceState(
                    initialized = true,
                    favoriteKeys = current.snapshot.favoriteKeys,
                    dockKeys = current.snapshot.dockKeys,
                    authority = when (current.snapshot.source) {
                        WorkspacePlacementSource.DATASTORE -> WorkspaceAuthority.DATASTORE
                        WorkspacePlacementSource.ROOM -> WorkspaceAuthority.ROOM
                    },
                )
            }
            val appsByWorkspaceKey = remember(apps) {
                apps.associateBy { app -> app.workspaceKey() }
            }
            val persistentDockApps = remember(appsByWorkspaceKey, workspace.dockKeys) {
                workspace.dockKeys.mapNotNull(appsByWorkspaceKey::get).take(MAX_DOCK_ITEMS)
            }
            val renderedPages = (pagedHome as? WorkspacePagedHomeState.Ready)?.pages.orEmpty()
            var selectedHomePageId by rememberSaveable {
                mutableStateOf(WorkspaceLegacyImportMapper.HOME_PAGE_ID)
            }
            var currentHomeGridBounds by remember { mutableStateOf<Rect?>(null) }
            var activeHomeAppDrag by remember { mutableStateOf<LauncherAppDragData?>(null) }
            var activeHomeAppDragPoint by remember { mutableStateOf<Offset?>(null) }
            var showHomePageManager by rememberSaveable { mutableStateOf(false) }
            var homeEditorVisible by rememberSaveable { mutableStateOf(false) }
            var pendingHomeEditorPageId by rememberSaveable { mutableStateOf<String?>(null) }
            var primarySurfaceModeName by rememberSaveable {
                mutableStateOf(LauncherSurfaceMode.HOME.name)
            }
            var primaryHomeEditorRequestSequence by rememberSaveable { mutableStateOf(0L) }
            val primarySurfaceMode = runCatching {
                LauncherSurfaceMode.valueOf(primarySurfaceModeName)
            }.getOrDefault(LauncherSurfaceMode.HOME)
            val useDarkSystemBarIcons = launcherUsesDarkSystemBarIcons(
                surfaceMode = primarySurfaceMode,
                startupWizardCompleted = experiencePreferences.startupWizardCompleted,
                homeEditorVisible = homeEditorVisible,
                darkTheme = darkTheme,
            )
            LaunchedEffect(useDarkSystemBarIcons) {
                setSystemBarIconAppearance(useDarkIcons = useDarkSystemBarIcons)
            }

            val launchApp: (LauncherActivityInfo) -> Unit = { app ->
                appsRepository.launch(app)
                if (experiencePreferences.homeAppMode != LauncherHomeAppMode.NONE) {
                    localUsageRepository.recordLaunch(app.workspaceKey())
                }
            }

            LaunchedEffect(
                apps,
                experiencePreferences.addNewAppsToHome,
                launcherPreferences.layoutLocked,
                launcherPreferences.homeColumns,
                launcherPreferences.homeRows,
                workspace.authority,
                workspace.favoriteKeys,
            ) {
                val primaryAppsByKey = apps
                    .asSequence()
                    .filter {
                        it.user == Process.myUserHandle() &&
                            it.componentName.packageName != packageName
                    }
                    .associateBy { it.workspaceKey() }
                val baseline = installedAppBaselineRepository.reconcile(
                    primaryAppsByKey.keys,
                )

                if (
                    !baseline.initializedBefore ||
                    !experiencePreferences.addNewAppsToHome ||
                    launcherPreferences.layoutLocked ||
                    workspace.authority != WorkspaceAuthority.ROOM
                ) {
                    return@LaunchedEffect
                }

                for (appKey in baseline.newAppKeys) {
                    if (appKey in workspace.favoriteKeys) continue
                    if (primaryAppsByKey[appKey] == null) continue
                    workspaceRuntimeCoordinator.toggleFavorite(
                        key = appKey,
                        homeColumns = launcherPreferences.homeColumns,
                        homeRows = launcherPreferences.homeRows,
                    )
                }
            }

            LaunchedEffect(homeResetSequenceValue) {
                selectedHomePageId = WorkspaceLegacyImportMapper.HOME_PAGE_ID
                showHomePageManager = false
                primarySurfaceModeName = LauncherSurfaceMode.HOME.name
            }

            LaunchedEffect(renderedPages) {
                if (renderedPages.isNotEmpty() && renderedPages.none { it.pageId == selectedHomePageId }) {
                    selectedHomePageId = renderedPages
                        .firstOrNull { it.pageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID }
                        ?.pageId
                        ?: renderedPages.first().pageId
                }
            }

            LaunchedEffect(
                apps,
                workspace.initialized,
                workspace.authority,
                workspace.favoriteKeys,
                workspace.dockKeys,
                experiencePreferences.starterLayoutApplied,
                experiencePreferences.startupWizardCompleted,
                visualPreferences.starterDockSize,
            ) {
                if (
                    apps.isEmpty() ||
                    experiencePreferences.starterLayoutApplied ||
                    !experiencePreferences.startupWizardCompleted
                ) {
                    return@LaunchedEffect
                }

                val starterSelection = StarterWorkspacePolicy.select(
                    apps
                        .filterNot { it.componentName.packageName == packageName }
                        .map { app ->
                            StarterWorkspaceCandidate(
                                key = app.workspaceKey(),
                                label = app.label.toString(),
                                packageName = app.componentName.packageName,
                            )
                        },
                    maxFavorites = 0,
                    maxDock = visualPreferences.starterDockSize.coerceIn(4, 6),
                )

                if (!workspace.initialized) {
                    workspaceRepository.ensureDefaults(
                        favoriteKeys = emptyList(),
                        dockKeys = starterSelection.dockKeys,
                    )
                } else if (workspace.favoriteKeys.isEmpty() && workspace.dockKeys.isEmpty()) {
                    for (key in starterSelection.dockKeys) {
                        if (
                            workspaceRuntimeCoordinator.toggleDock(key) !is
                                WorkspaceAuthoritativeWriteResult.Written
                        ) {
                            return@LaunchedEffect
                        }
                    }
                } else {
                    launcherPreferencesRepository.markStarterLayoutApplied()
                    return@LaunchedEffect
                }

                workspaceRuntimeCoordinator.reconcileAndActivate()

                val starterGlance = workspaceRuntimeCoordinator.addBuiltInWidget(
                    itemId = STARTER_GLANCE_WIDGET_ID,
                    typeId = WorkspaceWidgetCatalog.GLANCE,
                    columns = launcherPreferences.homeColumns,
                    rows = launcherPreferences.homeRows,
                )
                if (starterGlance is WorkspaceWidgetMutationResult.Added) {
                    launcherPreferencesRepository.setHomeCardStyle(
                        com.goreecloud.launcher.core.launcher.LauncherHomeCardStyle.OFF,
                    )
                }

                launcherPreferencesRepository.markStarterLayoutApplied()
            }

            LaunchedEffect(
                workspace.initialized,
                workspace.authority,
                workspace.favoriteKeys,
                workspace.dockKeys,
            ) {
                if (workspace.initialized) {
                    workspaceRuntimeCoordinator.reconcileAndActivate()
                }
            }

            GlazeTheme(themeMode) {
                CompositionLocalProvider(
                    LocalLauncherIconAppearance provides LauncherIconAppearance(
                        shape = experiencePreferences.iconShape,
                        iconPackPackage = experiencePreferences.iconPackPackage,
                    ),
                ) {
                if (!experiencePreferences.startupWizardCompleted) {
                    LauncherStartupWizard(
                        isDefaultHome = isDefaultHome,
                        initialHomeAppMode = experiencePreferences.homeAppMode,
                        initialHomeColumns = launcherPreferences.homeColumns,
                        initialHomeRows = launcherPreferences.homeRows,
                        initialShowHomeLabels = experiencePreferences.showHomeLabels,
                        initialUniversalSearchHomeMode = launcherPreferences.universalSearchHomeMode,
                        initialAddNewAppsToHome = experiencePreferences.addNewAppsToHome,
                        initialShowHints = !experiencePreferences.homeHintsDismissed,
                        initialDockSize = visualPreferences.starterDockSize,
                        initialStep = experiencePreferences.startupWizardStep,
                        onStepChange = launcherPreferencesRepository::setStartupWizardStep,
                        onRequestHomeRole = ::requestHomeRole,
                        onFinish = { configuration ->
                            lifecycleScope.launch {
                                visualPreferencesRepository.setStarterDockSize(configuration.dockSize)
                                launcherPreferencesRepository.applyStartupConfiguration(
                                    homeAppMode = configuration.homeAppMode,
                                    homeColumns = configuration.homeColumns,
                                    homeRows = configuration.homeRows,
                                    showHomeLabels = configuration.showHomeLabels,
                                    universalSearchHomeMode = configuration.universalSearchHomeMode,
                                    addNewAppsToHome = configuration.addNewAppsToHome,
                                    showHints = configuration.showHints,
                                )
                            }
                        },
                    )
                } else {
                val selectedPage = renderedPages.firstOrNull { it.pageId == selectedHomePageId }
                val onPrimaryPage = selectedPage == null ||
                    selectedPage.pageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID
                val showingHome = !onPrimaryPage || primarySurfaceMode == LauncherSurfaceMode.HOME
                val selectedHomePageIndex = renderedPages.indexOfFirst {
                    it.pageId == selectedHomePageId
                }.let { index -> if (index >= 0) index else 0 }
                val appsByWorkspaceKey = remember(apps) {
                    apps.associateBy { app -> app.workspaceKey() }
                }
                val persistentDockApps = remember(appsByWorkspaceKey, workspace.dockKeys) {
                    workspace.dockKeys.mapNotNull(appsByWorkspaceKey::get)
                }
                val createHomePage: (Boolean) -> Unit = { selectCreatedPage ->
                    if (!launcherPreferences.layoutLocked) {
                        val pageId = "home:user:${UUID.randomUUID()}"
                        lifecycleScope.launch {
                            val result = workspaceRuntimeCoordinator.createHomePage(pageId)
                            if (
                                selectCreatedPage &&
                                result is WorkspacePagedRoomMutationResult.CreatedPage
                            ) {
                                selectedHomePageId = result.pageId
                            }
                        }
                    }
                }

                val density = LocalDensity.current
                val crossPageDragEdgeThresholdPx = with(density) { 36.dp.toPx() }
                val currentRenderedPages by rememberUpdatedState(renderedPages)
                val currentSelectedHomePageId by rememberUpdatedState(selectedHomePageId)
                val currentGridBounds by rememberUpdatedState(currentHomeGridBounds)
                val currentLauncherPreferences by rememberUpdatedState(launcherPreferences)
                val currentExperiencePreferences by rememberUpdatedState(experiencePreferences)

                val resolveHomeDropCell: (Offset, String, Rect) -> Pair<Int, Int>? =
                    { point, pageId, bounds ->
                        val (horizontalSpacingPx, verticalSpacingPx, tileHeightPx) =
                            if (pageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID) {
                                val spacingDp = when (currentExperiencePreferences.homeSpacing) {
                                    LauncherHomeSpacing.COMPACT -> 2.dp
                                    LauncherHomeSpacing.BALANCED -> GlazeMetrics.space1
                                    LauncherHomeSpacing.AIRY -> GlazeMetrics.space2
                                }
                                val tileHeightDp = when (currentExperiencePreferences.homeSpacing) {
                                    LauncherHomeSpacing.COMPACT -> 72.dp
                                    LauncherHomeSpacing.BALANCED -> 78.dp
                                    LauncherHomeSpacing.AIRY -> 86.dp
                                }
                                Triple(
                                    with(density) { spacingDp.toPx() },
                                    with(density) { spacingDp.toPx() },
                                    with(density) { tileHeightDp.toPx() },
                                )
                            } else {
                                Triple(
                                    with(density) { GlazeMetrics.space1.toPx() },
                                    with(density) { GlazeMetrics.space3.toPx() },
                                    with(density) { 96.dp.toPx() },
                                )
                            }
                        homePageCellAtPoint(
                            point = point,
                            surfaceBounds = bounds,
                            columns = currentLauncherPreferences.homeColumns.coerceIn(4, 6),
                            rows = currentLauncherPreferences.homeRows.coerceIn(4, 7),
                            horizontalSpacingPx = horizontalSpacingPx,
                            verticalSpacingPx = verticalSpacingPx,
                            tileHeightPx = tileHeightPx,
                        )
                    }
                val currentResolveHomeDropCell by rememberUpdatedState(resolveHomeDropCell)

                val routeHomeAppDrop: (LauncherAppDragData, Offset) -> Boolean = route@{ drag, point ->
                    if (currentLauncherPreferences.layoutLocked) return@route false
                    val sourcePageId = drag.sourcePageId ?: return@route false
                    val targetPageId = currentSelectedHomePageId.takeIf { candidate ->
                        currentRenderedPages.any { it.pageId == candidate }
                    } ?: WorkspaceLegacyImportMapper.HOME_PAGE_ID
                    val bounds = currentGridBounds ?: return@route false

                    if (
                        sourcePageId == targetPageId &&
                        targetPageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID
                    ) {
                        // The primary LauncherBetaRoot target owns primary same-page and Dock drops.
                        return@route false
                    }

                    val adjacentEdge = if (sourcePageId == targetPageId) {
                        homePageEdgeDropTarget(
                            pages = currentRenderedPages,
                            currentPageId = targetPageId,
                            dropX = point.x,
                            dropY = point.y,
                            surfaceLeftPx = bounds.left,
                            surfaceTopPx = bounds.top,
                            surfaceRightPx = bounds.right,
                            surfaceBottomPx = bounds.bottom,
                            edgeThresholdPx = crossPageDragEdgeThresholdPx,
                            columns = currentLauncherPreferences.homeColumns.coerceIn(4, 6),
                            rows = currentLauncherPreferences.homeRows.coerceIn(4, 7),
                        )
                    } else {
                        null
                    }

                    if (adjacentEdge != null) {
                        lifecycleScope.launch {
                            val result = workspaceRuntimeCoordinator.moveHomeAppToPage(
                                sourcePageId = sourcePageId,
                                appKey = drag.appKey,
                                targetPageId = adjacentEdge.pageId,
                                primaryColumns = currentLauncherPreferences.homeColumns,
                                primaryRows = currentLauncherPreferences.homeRows,
                                targetCellX = adjacentEdge.cellX,
                                targetCellY = adjacentEdge.cellY,
                            )
                            if (result is WorkspacePagedRoomMutationResult.UpdatedItem) {
                                selectedHomePageId = result.pageId
                            }
                        }
                        true
                    } else {
                        val targetCell =
                            currentResolveHomeDropCell(point, targetPageId, bounds)
                                ?: return@route false
                        lifecycleScope.launch {
                            if (sourcePageId == targetPageId) {
                                workspaceRuntimeCoordinator.moveHomeAppToCellWithinPage(
                                    pageId = targetPageId,
                                    appKey = drag.appKey,
                                    columns = currentLauncherPreferences.homeColumns,
                                    rows = currentLauncherPreferences.homeRows,
                                    cellX = targetCell.first,
                                    cellY = targetCell.second,
                                )
                            } else {
                                val result = workspaceRuntimeCoordinator.moveHomeAppToPage(
                                    sourcePageId = sourcePageId,
                                    appKey = drag.appKey,
                                    targetPageId = targetPageId,
                                    primaryColumns = currentLauncherPreferences.homeColumns,
                                    primaryRows = currentLauncherPreferences.homeRows,
                                    targetCellX = targetCell.first,
                                    targetCellY = targetCell.second,
                                )
                                if (result is WorkspacePagedRoomMutationResult.UpdatedItem) {
                                    selectedHomePageId = result.pageId
                                }
                            }
                        }
                        true
                    }
                }
                val currentRouteHomeAppDrop by rememberUpdatedState(routeHomeAppDrop)

                val homeAppDragTarget = remember {
                    object : DragAndDropTarget {
                        override fun onStarted(event: DragAndDropEvent) {
                            val drag = event.launcherAppDragData() ?: return
                            if (
                                drag.origin != LauncherAppDragOrigin.HOME ||
                                drag.sourcePageId == null
                            ) {
                                return
                            }
                            activeHomeAppDrag = drag
                            activeHomeAppDragPoint = null
                        }

                        override fun onMoved(event: DragAndDropEvent) {
                            val drag = event.launcherAppDragData() ?: return
                            if (
                                drag.origin == LauncherAppDragOrigin.HOME &&
                                drag.sourcePageId != null &&
                                activeHomeAppDrag?.appKey == drag.appKey
                            ) {
                                activeHomeAppDragPoint = event.rootDropPoint()
                            }
                        }

                        override fun onDrop(event: DragAndDropEvent): Boolean {
                            val drag = event.launcherAppDragData() ?: return false
                            if (
                                drag.origin != LauncherAppDragOrigin.HOME ||
                                drag.sourcePageId == null
                            ) {
                                return false
                            }
                            val point = event.rootDropPoint()
                            activeHomeAppDragPoint = point
                            return currentRouteHomeAppDrop(drag, point)
                        }

                        override fun onEnded(event: DragAndDropEvent) {
                            val drag = event.launcherAppDragData()
                            if (drag?.origin == LauncherAppDragOrigin.HOME) {
                                activeHomeAppDrag = null
                                activeHomeAppDragPoint = null
                            }
                        }
                    }
                }

                val hoverTargetPageId = run {
                    val drag = activeHomeAppDrag
                    val point = activeHomeAppDragPoint
                    val bounds = currentHomeGridBounds
                    if (
                        drag == null ||
                        drag.sourcePageId == null ||
                        point == null ||
                        bounds == null ||
                        launcherPreferences.layoutLocked ||
                        showHomePageManager ||
                        !showingHome
                    ) {
                        null
                    } else {
                        homePageEdgeDropTarget(
                            pages = renderedPages,
                            currentPageId = selectedHomePageId,
                            dropX = point.x,
                            dropY = point.y,
                            surfaceLeftPx = bounds.left,
                            surfaceTopPx = bounds.top,
                            surfaceRightPx = bounds.right,
                            surfaceBottomPx = bounds.bottom,
                            edgeThresholdPx = crossPageDragEdgeThresholdPx,
                            columns = launcherPreferences.homeColumns.coerceIn(4, 6),
                            rows = launcherPreferences.homeRows.coerceIn(4, 7),
                        )?.pageId
                    }
                }

                LaunchedEffect(
                    activeHomeAppDrag?.appKey,
                    activeHomeAppDrag?.sourcePageId,
                    selectedHomePageId,
                    hoverTargetPageId,
                ) {
                    if (hoverTargetPageId != null) {
                        delay(550)
                        if (
                            activeHomeAppDrag != null &&
                            !launcherPreferences.layoutLocked &&
                            !showHomePageManager &&
                            selectedHomePageId != hoverTargetPageId
                        ) {
                            // Clear the old page geometry before composing the new page so the
                            // same held pointer cannot accidentally target stale cell bounds.
                            currentHomeGridBounds = null
                            selectedHomePageId = hoverTargetPageId
                        }
                    }
                }

                val executeSecondaryHomeGesture: (LauncherGestureAction) -> Unit = { action ->
                    when (action.type) {
                        LauncherGestureActionType.NONE -> Unit
                        LauncherGestureActionType.APPS -> {
                            selectedHomePageId = WorkspaceLegacyImportMapper.HOME_PAGE_ID
                            primarySurfaceModeName = LauncherSurfaceMode.DRAWER.name
                        }
                        LauncherGestureActionType.UNIVERSAL_SEARCH -> {
                            selectedHomePageId = WorkspaceLegacyImportMapper.HOME_PAGE_ID
                            primarySurfaceModeName = LauncherSurfaceMode.SEARCH.name
                        }
                        LauncherGestureActionType.LAUNCHER_SETTINGS -> {
                            selectedHomePageId = WorkspaceLegacyImportMapper.HOME_PAGE_ID
                            primarySurfaceModeName = LauncherSurfaceMode.SETTINGS.name
                        }
                        LauncherGestureActionType.HOME_EDITOR -> {
                            pendingHomeEditorPageId = selectedHomePageId
                            selectedHomePageId = WorkspaceLegacyImportMapper.HOME_PAGE_ID
                            primarySurfaceModeName = LauncherSurfaceMode.HOME.name
                            primaryHomeEditorRequestSequence += 1L
                        }
                        LauncherGestureActionType.WALLPAPER -> {
                            selectedHomePageId = WorkspaceLegacyImportMapper.HOME_PAGE_ID
                            primarySurfaceModeName = LauncherSurfaceMode.HOME.name
                            openWallpaperPicker()
                        }
                        LauncherGestureActionType.THEME_MANAGER -> {
                            selectedHomePageId = WorkspaceLegacyImportMapper.HOME_PAGE_ID
                            primarySurfaceModeName = LauncherSurfaceMode.THEME_MANAGER.name
                        }
                        LauncherGestureActionType.OPEN_APP ->
                            action.appKey?.let(appsByWorkspaceKey::get)?.let(launchApp)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("launcher-home-swipe-surface")
                        .homeVerticalGestureNavigation(
                            enabled = showingHome &&
                                !onPrimaryPage &&
                                !showHomePageManager &&
                                activeHomeAppDrag == null,
                            onSwipeUp = {
                                executeSecondaryHomeGesture(experiencePreferences.swipeUpAction)
                            },
                            onSwipeDown = {
                                executeSecondaryHomeGesture(experiencePreferences.swipeDownAction)
                            },
                        )
                        .dragAndDropTarget(
                            shouldStartDragAndDrop = { event ->
                                event.launcherAppDragData()?.let { drag ->
                                    drag.origin == LauncherAppDragOrigin.HOME &&
                                        drag.sourcePageId != null
                                } == true
                            },
                            target = homeAppDragTarget,
                        )
                        .homePageSwipeNavigation(
                            enabled = showingHome &&
                                renderedPages.size > 1 &&
                                !showHomePageManager &&
                                activeHomeAppDrag == null,
                            currentIndex = selectedHomePageIndex,
                            pageCount = renderedPages.size,
                            onPageSelected = { pageIndex ->
                                renderedPages.getOrNull(pageIndex)?.let { page ->
                                    selectedHomePageId = page.pageId
                                }
                            },
                        ),
                ) {
                    if (!onPrimaryPage) {
                        val secondaryPage = checkNotNull(selectedPage)
                        ReadOnlyPagedHomeSurface(
                            apps = apps,
                            folders = folders,
                            page = secondaryPage,
                            pages = renderedPages,
                            homeColumns = launcherPreferences.homeColumns,
                            homeRows = launcherPreferences.homeRows,
                            showLabels = experiencePreferences.showHomeLabels,
                            iconScale = launcherPreferences.iconScale,
                            dockApps = persistentDockApps,
                            dockStyle = experiencePreferences.dockStyle,
                            pageTransition = visualPreferences.homePageTransition,
                            // One Activity-owned indicator stays visually fixed above the persistent
                            // Dock while page content transitions. Avoid a second page-local copy.
                            showPageIndicator = false,
                            onSelectPage = { selectedHomePageId = it },
                            layoutLocked = launcherPreferences.layoutLocked,
                            homeLabelOverrides = homeLabelOverrides,
                            onLaunchApp = launchApp,
                            onSetHomeLabelOverride = { app, label ->
                                launcherPreferencesRepository.setHomeLabelOverride(app.workspaceKey(), label)
                            },
                            onRequestUninstall = ::requestUninstall,
                            onMoveAppToPage = { app, targetPageId ->
                                if (!launcherPreferences.layoutLocked) {
                                    lifecycleScope.launch {
                                        val result = workspaceRuntimeCoordinator.moveHomeAppToPage(
                                            sourcePageId = secondaryPage.pageId,
                                            appKey = app.workspaceKey(),
                                            targetPageId = targetPageId,
                                            primaryColumns = launcherPreferences.homeColumns,
                                            primaryRows = launcherPreferences.homeRows,
                                        )
                                        if (result is WorkspacePagedRoomMutationResult.UpdatedItem) {
                                            selectedHomePageId = result.pageId
                                        }
                                    }
                                }
                            },
                            onMoveAppToPageCell = { app, targetPageId, cellX, cellY ->
                                if (!launcherPreferences.layoutLocked) {
                                    lifecycleScope.launch {
                                        val result = workspaceRuntimeCoordinator.moveHomeAppToPage(
                                            sourcePageId = secondaryPage.pageId,
                                            appKey = app.workspaceKey(),
                                            targetPageId = targetPageId,
                                            primaryColumns = launcherPreferences.homeColumns,
                                            primaryRows = launcherPreferences.homeRows,
                                            targetCellX = cellX,
                                            targetCellY = cellY,
                                        )
                                        if (result is WorkspacePagedRoomMutationResult.UpdatedItem) {
                                            selectedHomePageId = result.pageId
                                        }
                                    }
                                }
                            },
                            onMoveAppToCell = { app, cellX, cellY ->
                                if (!launcherPreferences.layoutLocked) {
                                    lifecycleScope.launch {
                                        workspaceRuntimeCoordinator.moveHomeAppToCellWithinPage(
                                            pageId = secondaryPage.pageId,
                                            appKey = app.workspaceKey(),
                                            columns = launcherPreferences.homeColumns,
                                            rows = launcherPreferences.homeRows,
                                            cellX = cellX,
                                            cellY = cellY,
                                        )
                                    }
                                }
                            },
                            onMoveAppWithinPage = { app, direction ->
                                if (!launcherPreferences.layoutLocked) {
                                    lifecycleScope.launch {
                                        workspaceRuntimeCoordinator.moveHomeAppWithinPage(
                                            pageId = secondaryPage.pageId,
                                            appKey = app.workspaceKey(),
                                            direction = direction,
                                        )
                                    }
                                }
                            },
                            onMoveAppOneCell = { app, direction ->
                                if (!launcherPreferences.layoutLocked) {
                                    lifecycleScope.launch {
                                        workspaceRuntimeCoordinator.moveHomeAppOneCellWithinPage(
                                            pageId = secondaryPage.pageId,
                                            appKey = app.workspaceKey(),
                                            direction = direction,
                                        )
                                    }
                                }
                            },
                            onRenameFolder = ::renameFolder,
                            onDeleteFolder = ::deleteFolder,
                            onAddAppToFolder = ::addAppToFolder,
                            onRemoveAppFromFolder = ::removeAppFromFolder,
                            onRemoveFolderFromHome = ::removeFolderFromHome,
                            onMoveFolderToPage = { folder, target ->
                                moveFolderToPage(folder, target) { selectedHomePageId = it }
                            },
                            onMoveFolderToPageCell = { folder, target, cellX, cellY ->
                                moveFolderToPage(
                                    folder = folder,
                                    targetPageId = target,
                                    targetCellX = cellX,
                                    targetCellY = cellY,
                                ) { selectedHomePageId = it }
                            },
                            onMoveFolderToCell = { folder, cellX, cellY ->
                                if (!launcherPreferences.layoutLocked) {
                                    lifecycleScope.launch {
                                        val result =
                                            workspaceRuntimeCoordinator.moveHomeFolderToCellWithinPage(
                                                pageId = secondaryPage.pageId,
                                                folderId = folder.id,
                                                columns = launcherPreferences.homeColumns,
                                                rows = launcherPreferences.homeRows,
                                                cellX = cellX,
                                                cellY = cellY,
                                            )
                                        if (result !is WorkspaceFolderMutationResult.Moved) {
                                            Toast.makeText(
                                                this@MainActivity,
                                                if (result == WorkspaceFolderMutationResult.NoSpace) {
                                                    "This Home cell is occupied. Choose an empty cell."
                                                } else {
                                                    "Folder could not be moved; its original placement is preserved."
                                                },
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        }
                                    }
                                }
                            },
                            onCreateAndroidWidgetView =
                                appWidgetHostController::createHostView,
                            onRemoveWidget = ::removeWidget,
                            onResizeWidget = ::resizeWidget,
                            onMoveWidgetToCell = ::moveWidget,
                            onMoveWidgetToPage = { widget, targetPageId ->
                                moveWidgetToPage(widget, targetPageId) {
                                    selectedHomePageId = it
                                }
                            },
                            onMoveWidgetToPageCell = {
                                    widget, targetPageId, cellX, cellY ->
                                moveWidgetToPage(
                                    widget = widget,
                                    targetPageId = targetPageId,
                                    targetCellX = cellX,
                                    targetCellY = cellY,
                                ) {
                                    selectedHomePageId = it
                                }
                            },
                            onOpenWidgetSearch = {
                                selectedHomePageId = WorkspaceLegacyImportMapper.HOME_PAGE_ID
                                primarySurfaceModeName = LauncherSurfaceMode.SEARCH.name
                            },
                            onOpenWidgetApps = {
                                selectedHomePageId = WorkspaceLegacyImportMapper.HOME_PAGE_ID
                                primarySurfaceModeName = LauncherSurfaceMode.DRAWER.name
                            },
                            onOpenWidgetEditor = {
                                pendingHomeEditorPageId = secondaryPage.pageId
                                selectedHomePageId = WorkspaceLegacyImportMapper.HOME_PAGE_ID
                                primarySurfaceModeName = LauncherSurfaceMode.HOME.name
                                primaryHomeEditorRequestSequence += 1L
                            },
                            onOpenHomeEditor = {
                                pendingHomeEditorPageId = secondaryPage.pageId
                                selectedHomePageId = WorkspaceLegacyImportMapper.HOME_PAGE_ID
                                primarySurfaceModeName = LauncherSurfaceMode.HOME.name
                                primaryHomeEditorRequestSequence += 1L
                            },
                            onOpenWidgetSettings = {
                                selectedHomePageId = WorkspaceLegacyImportMapper.HOME_PAGE_ID
                                primarySurfaceModeName = LauncherSurfaceMode.SETTINGS.name
                            },
                            onGridBoundsChanged = { bounds ->
                                if (selectedHomePageId == secondaryPage.pageId) {
                                    currentHomeGridBounds = bounds
                                }
                            },
                        )
                    } else {
                        LauncherBetaRoot(
                            apps = apps,
                            workspace = workspace,
                            preferences = launcherPreferences,
                            drawerLayoutMode = drawerLayoutMode,
                            experiencePreferences = experiencePreferences,
                            homePageTransition = visualPreferences.homePageTransition,
                            recentAppKeys = localRecentAppKeys,
                            localLaunchCounts = localLaunchCounts,
                            hiddenHomeSuggestionKeys = hiddenHomeSuggestionKeys,
                            searchProviderPreferences = searchProviderPreferences,
                            fileSearchRoots = fileSearchRoots,
                            homePageCount = renderedPages.size.coerceAtLeast(1),
                            homeResetSequence = homeResetSequenceValue,
                            requestedSurfaceMode = primarySurfaceMode,
                            externalHomeEditorRequestSequence = primaryHomeEditorRequestSequence,
                            homeEditorInitialPageId = pendingHomeEditorPageId,
                            homeLabelOverrides = homeLabelOverrides,
                            folders = folders,
                            primaryHomePage = renderedPages.firstOrNull {
                                it.pageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID
                            },
                            homePages = renderedPages,
                            onMoveFolderToPage = { folder, target ->
                                moveFolderToPage(folder, target) { selectedHomePageId = it }
                            },
                            onMoveFolderToPageCell = { folder, target, cellX, cellY ->
                                moveFolderToPage(
                                    folder = folder,
                                    targetPageId = target,
                                    targetCellX = cellX,
                                    targetCellY = cellY,
                                ) { selectedHomePageId = it }
                            },
                            onCreateFolder = ::createFolder,
                            onRenameFolder = ::renameFolder,
                            onDeleteFolder = ::deleteFolder,
                            onAddAppToFolder = ::addAppToFolder,
                            onRemoveAppFromFolder = ::removeAppFromFolder,
                            onAddFolderToHome = ::addFolderToHome,
                            onRemoveFolderFromHome = ::removeFolderFromHome,
                            onMoveHomeFolderToCell = ::moveHomeFolderToCell,
                            onManageHomePages = {
                                showHomePageManager = true
                            },
                            onCreateHomePage = { createHomePage(false) },
                            onSelectHomePage = { pageId ->
                                if (renderedPages.any { it.pageId == pageId }) {
                                    selectedHomePageId = pageId
                                }
                            },
                            onDeleteHomePage = { pageId ->
                                if (!launcherPreferences.layoutLocked) {
                                    lifecycleScope.launch {
                                        val result = workspaceRuntimeCoordinator.deleteEmptyHomePage(pageId)
                                        if (
                                            result is WorkspacePagedRoomMutationResult.DeletedPage &&
                                            selectedHomePageId == result.pageId
                                        ) {
                                            selectedHomePageId = WorkspaceLegacyImportMapper.HOME_PAGE_ID
                                        }
                                    }
                                }
                            },
                            onSwipeHomePageLeft = {
                                val target = renderedPages.getOrNull(selectedHomePageIndex + 1)
                                if (target == null) {
                                    false
                                } else {
                                    selectedHomePageId = target.pageId
                                    true
                                }
                            },
                            onSwipeHomePageRight = {
                                val target = renderedPages.getOrNull(selectedHomePageIndex - 1)
                                if (target == null) {
                                    false
                                } else {
                                    selectedHomePageId = target.pageId
                                    true
                                }
                            },
                            isDefaultHome = isDefaultHome,
                            onRequestHomeRole = ::requestHomeRole,
                            onLaunchApp = launchApp,
                            onOpenAppInfo = appsRepository::openDetails,
                            onAddBuiltInWidget = ::addBuiltInWidget,
                            availableAndroidWidgets = availableAndroidWidgets,
                            availableIconPacks = availableIconPacks,
                            onPickInstalledAndroidWidget = { descriptor: LauncherWidgetProviderDescriptor ->
                                beginAndroidWidgetBind(descriptor.provider)
                            },
                            onPickAndroidWidget = ::beginAndroidWidgetPick,
                            onCreateAndroidWidgetView = appWidgetHostController::createHostView,
                            onRemoveWidget = ::removeWidget,
                            onResizeWidget = ::resizeWidget,
                            onMoveWidget = ::moveWidget,
                            onMoveWidgetToPage = { widget, targetPageId ->
                                moveWidgetToPage(widget, targetPageId) {
                                    selectedHomePageId = it
                                }
                            },
                            onMoveWidgetToPageCell = {
                                    widget, targetPageId, cellX, cellY ->
                                moveWidgetToPage(
                                    widget = widget,
                                    targetPageId = targetPageId,
                                    targetCellX = cellX,
                                    targetCellY = cellY,
                                ) {
                                    selectedHomePageId = it
                                }
                            },
                            onToggleFavorite = { app ->
                                if (!launcherPreferences.layoutLocked) {
                                    lifecycleScope.launch {
                                        workspaceRuntimeCoordinator.toggleFavorite(
                                            key = app.workspaceKey(),
                                            homeColumns = launcherPreferences.homeColumns,
                                            homeRows = launcherPreferences.homeRows,
                                        )
                                    }
                                }
                            },
                            onToggleDock = { app ->
                                if (!launcherPreferences.layoutLocked) {
                                    lifecycleScope.launch {
                                        workspaceRuntimeCoordinator.toggleDock(app.workspaceKey())
                                    }
                                }
                            },
                            onMoveFavorite = { app, direction ->
                                if (!launcherPreferences.layoutLocked) {
                                    lifecycleScope.launch {
                                        workspaceRuntimeCoordinator.moveFavorite(app.workspaceKey(), direction)
                                    }
                                }
                            },
                            onMoveFavoriteToPage = { app, targetPageId ->
                                if (!launcherPreferences.layoutLocked) {
                                    lifecycleScope.launch {
                                        val result = workspaceRuntimeCoordinator.moveHomeAppToPage(
                                            sourcePageId = WorkspaceLegacyImportMapper.HOME_PAGE_ID,
                                            appKey = app.workspaceKey(),
                                            targetPageId = targetPageId,
                                            primaryColumns = launcherPreferences.homeColumns,
                                            primaryRows = launcherPreferences.homeRows,
                                        )
                                        if (result is WorkspacePagedRoomMutationResult.UpdatedItem) {
                                            selectedHomePageId = result.pageId
                                        }
                                    }
                                }
                            },
                            onMoveFavoriteToPageCell = { app, targetPageId, cellX, cellY ->
                                if (!launcherPreferences.layoutLocked) {
                                    lifecycleScope.launch {
                                        val result = workspaceRuntimeCoordinator.moveHomeAppToPage(
                                            sourcePageId = WorkspaceLegacyImportMapper.HOME_PAGE_ID,
                                            appKey = app.workspaceKey(),
                                            targetPageId = targetPageId,
                                            primaryColumns = launcherPreferences.homeColumns,
                                            primaryRows = launcherPreferences.homeRows,
                                            targetCellX = cellX,
                                            targetCellY = cellY,
                                        )
                                        if (result is WorkspacePagedRoomMutationResult.UpdatedItem) {
                                            selectedHomePageId = result.pageId
                                        }
                                    }
                                }
                            },
                            onMoveFavoriteToCell = { app, cellX, cellY ->
                                if (!launcherPreferences.layoutLocked) {
                                    lifecycleScope.launch {
                                        workspaceRuntimeCoordinator.movePrimaryHomeAppToCell(
                                            appKey = app.workspaceKey(),
                                            columns = launcherPreferences.homeColumns,
                                            rows = launcherPreferences.homeRows,
                                            cellX = cellX,
                                            cellY = cellY,
                                        )
                                    }
                                }
                            },
                            onMoveFavoriteToDock = { app, targetDockKey ->
                                if (!launcherPreferences.layoutLocked) {
                                    val key = app.workspaceKey()
                                    if (
                                        key !in workspace.dockKeys &&
                                        workspace.dockKeys.size >= MAX_DOCK_ITEMS
                                    ) {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Dock is full.",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    } else {
                                        lifecycleScope.launch {
                                            workspaceRuntimeCoordinator.moveHomeToDock(
                                                key = key,
                                                targetDockKey = targetDockKey,
                                            )
                                        }
                                    }
                                }
                            },
                            onMoveDockToHomeCell = { app, cellX, cellY ->
                                if (!launcherPreferences.layoutLocked) {
                                    val key = app.workspaceKey()
                                    if (
                                        key !in workspace.favoriteKeys &&
                                        workspace.favoriteKeys.size >= launcherPreferences.homeCapacity
                                    ) {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Home screen is full.",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    } else {
                                        lifecycleScope.launch {
                                            workspaceRuntimeCoordinator.moveDockToPrimaryHomeCell(
                                                key = key,
                                                columns = launcherPreferences.homeColumns,
                                                rows = launcherPreferences.homeRows,
                                                cellX = cellX,
                                                cellY = cellY,
                                            )
                                        }
                                    }
                                }
                            },
                            onCopyDrawerToHomeCell = { app, cellX, cellY ->
                                if (!launcherPreferences.layoutLocked) {
                                    val key = app.workspaceKey()
                                    if (
                                        key !in workspace.favoriteKeys &&
                                        workspace.favoriteKeys.size >= launcherPreferences.homeCapacity
                                    ) {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Home screen is full.",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    } else {
                                        lifecycleScope.launch {
                                            workspaceRuntimeCoordinator.copyDrawerToPrimaryHomeCell(
                                                key = key,
                                                columns = launcherPreferences.homeColumns,
                                                rows = launcherPreferences.homeRows,
                                                cellX = cellX,
                                                cellY = cellY,
                                            )
                                        }
                                    }
                                }
                            },
                            onCopyDrawerToDock = { app, targetDockKey ->
                                if (!launcherPreferences.layoutLocked) {
                                    val key = app.workspaceKey()
                                    if (
                                        key !in workspace.dockKeys &&
                                        workspace.dockKeys.size >= MAX_DOCK_ITEMS
                                    ) {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Dock is full.",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    } else {
                                        lifecycleScope.launch {
                                            workspaceRuntimeCoordinator.copyDrawerToDock(
                                                key = key,
                                                targetDockKey = targetDockKey,
                                            )
                                        }
                                    }
                                }
                            },
                            onReorderDockByDrop = { app, targetDockKey ->
                                if (!launcherPreferences.layoutLocked) {
                                    lifecycleScope.launch {
                                        workspaceRuntimeCoordinator.reorderDockByDrop(
                                            key = app.workspaceKey(),
                                            targetDockKey = targetDockKey,
                                        )
                                    }
                                }
                            },
                            onMoveDock = { app, direction ->
                                if (!launcherPreferences.layoutLocked) {
                                    lifecycleScope.launch {
                                        workspaceRuntimeCoordinator.moveDock(app.workspaceKey(), direction)
                                    }
                                }
                            },
                            themeMode = themeMode,
                            onSetThemeMode = themeRepository::setMode,
                            onSetHomeGrid = { columns, rows ->
                                if (workspace.authority != WorkspaceAuthority.ROOM) {
                                    launcherPreferencesRepository.setHomeGrid(columns, rows)
                                } else {
                                    lifecycleScope.launch {
                                        when (
                                            workspaceRuntimeCoordinator.ensurePrimaryHomeSpatialGrid(
                                                columns = columns,
                                                rows = rows,
                                            )
                                        ) {
                                            is WorkspacePrimaryHomeSpatialResult.Ready -> {
                                                launcherPreferencesRepository.setHomeGrid(columns, rows)
                                            }
                                            WorkspacePrimaryHomeSpatialResult.Reserved -> Unit
                                            WorkspacePrimaryHomeSpatialResult.Unavailable,
                                            WorkspacePrimaryHomeSpatialResult.InvalidWorkspace,
                                            WorkspacePrimaryHomeSpatialResult.StoredWorkspaceChanged,
                                            is WorkspacePrimaryHomeSpatialResult.Failed,
                                            is WorkspacePrimaryHomeSpatialResult.Moved -> {
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "Home grid could not be changed safely.",
                                                    Toast.LENGTH_SHORT,
                                                ).show()
                                            }
                                        }
                                    }
                                }
                            },
                            onSetDrawerColumns = launcherPreferencesRepository::setDrawerColumns,
                            onSetDrawerLayoutMode = launcherPreferencesRepository::setDrawerLayoutMode,
                            onSetShowLabels = launcherPreferencesRepository::setShowLabels,
                            onSetIconScale = launcherPreferencesRepository::setIconScale,
                            onSetIconShape = launcherPreferencesRepository::setIconShape,
                            onSetIconPackPackage = launcherPreferencesRepository::setIconPackPackage,
                            onSetLayoutLocked = launcherPreferencesRepository::setLayoutLocked,
                            onSetUniversalSearchHomeMode = launcherPreferencesRepository::setUniversalSearchHomeMode,
                            onSetSearchProviderPreferences = { snapshot ->
                                lifecycleScope.launch {
                                    searchProviderPreferencesRepository.set(snapshot)
                                }
                            },
                            onSetSearchProviderEnabled = ::setSearchProviderEnabled,
                            onChooseFileSearchRoot = ::chooseFileSearchRoot,
                            onRemoveFileSearchRoot = ::confirmRemoveFileSearchRoot,
                            onLaunchSearchShortcut = { action ->
                                runCatching {
                                    appsRepository.launchShortcut(
                                        packageName = action.packageName,
                                        shortcutId = action.shortcutId,
                                        user = action.user,
                                    )
                                }.onFailure {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "That shortcut is no longer available.",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            },
                            onOpenSearchUri = ::openSearchUri,
                            onOpenDocument = ::openDocument,
                            onSearchWithConnectedProvider = ::searchWithConnectedProvider,
                            onResetSearchProviderPreferences = {
                                lifecycleScope.launch {
                                    searchProviderPreferencesRepository.clear()
                                }
                            },
                            onSetHomeCardStyle = launcherPreferencesRepository::setHomeCardStyle,
                            onSetShowHomeQuickActions = launcherPreferencesRepository::setShowHomeQuickActions,
                            onSetShowHomePageIndicator = launcherPreferencesRepository::setShowHomePageIndicator,
                            onSetShowHomeLabels = launcherPreferencesRepository::setShowHomeLabels,
                            onSetShowDrawerLabels = launcherPreferencesRepository::setShowDrawerLabels,
                            onSetShowDrawerPageIndicator = launcherPreferencesRepository::setShowDrawerPageIndicator,
                            onSetHomeAppMode =
                                launcherPreferencesRepository::setHomeAppMode,
                            onSetAddNewAppsToHome =
                                launcherPreferencesRepository::setAddNewAppsToHome,
                            onClearLocalUsage = {
                                localUsageRepository.clear()
                                Unit
                            },
                            onSetHintsEnabled = { enabled ->
                                launcherPreferencesRepository.setHomeHintsDismissed(!enabled)
                            },
                            onReplayStartupWizard =
                                launcherPreferencesRepository::replayStartupWizard,
                            onSetDrawerBackdrop = launcherPreferencesRepository::setDrawerBackdrop,
                            onSetDrawerSearchPlacement = launcherPreferencesRepository::setDrawerSearchPlacement,
                            onSetDrawerNavigation = launcherPreferencesRepository::setDrawerNavigation,
                            onSetDrawerEntryMode = launcherPreferencesRepository::setDrawerEntryMode,
                            onSetDrawerSpacing = launcherPreferencesRepository::setDrawerSpacing,
                            onSetDrawerPageRows = launcherPreferencesRepository::setDrawerPageRows,
                            onSetShowDrawerAppCount = launcherPreferencesRepository::setShowDrawerAppCount,
                            onSetHomeGlanceAlignment = launcherPreferencesRepository::setHomeGlanceAlignment,
                            onSetHomeSearchPlacement = launcherPreferencesRepository::setHomeSearchPlacement,
                            onSetHomeSearchStyle = launcherPreferencesRepository::setHomeSearchStyle,
                            onSetHomeSpacing = launcherPreferencesRepository::setHomeSpacing,
                            onSetDockStyle = launcherPreferencesRepository::setDockStyle,
                            onSetWallpaperShade = launcherPreferencesRepository::setWallpaperShade,
                            onSetGestureAction = { gesture, action ->
                                launcherPreferencesRepository.setGestureAction(gesture, action)
                                Unit
                            },
                            onSetHomeLabelOverride = { app, label ->
                                launcherPreferencesRepository.setHomeLabelOverride(app.workspaceKey(), label)
                            },
                            onSetHomeSuggestionHidden = launcherPreferencesRepository::setHomeSuggestionHidden,
                            onRequestUninstall = ::requestUninstall,
                            onOpenWallpaperPicker = ::openWallpaperPicker,
                            onSurfaceModeChanged = { mode ->
                                primarySurfaceModeName = mode.name
                                LauncherTransitionDiagnostics.recordSurfaceMode(mode)
                            },
                            onHomeEditorVisibilityChanged = { visible ->
                                val wasVisible = homeEditorVisible
                                homeEditorVisible = visible
                                if (wasVisible && !visible) {
                                    pendingHomeEditorPageId?.let { targetPageId ->
                                        if (renderedPages.any { it.pageId == targetPageId }) {
                                            selectedHomePageId = targetPageId
                                        }
                                    }
                                    pendingHomeEditorPageId = null
                                }
                            },
                            onPrimaryHomeGridBoundsChanged = { bounds ->
                                if (
                                    selectedHomePageId ==
                                    WorkspaceLegacyImportMapper.HOME_PAGE_ID
                                ) {
                                    currentHomeGridBounds = bounds
                                }
                            },
                        )
                    }

                    if (
                        experiencePreferences.showHomePageIndicator &&
                        renderedPages.size > 1 &&
                        showingHome
                    ) {
                        val indicatorBottomPadding = when {
                            onPrimaryPage &&
                                launcherPreferences.universalSearchHomeMode ==
                                    LauncherUniversalSearchHomeMode.PERMANENT &&
                                experiencePreferences.homeSearchPlacement ==
                                    LauncherHomeSearchPlacement.BOTTOM ->
                                176.dp
                            else -> 112.dp
                        }
                        HomePageDots(
                            pages = renderedPages,
                            selectedPageId = selectedHomePageId,
                            onSelectPage = { selectedHomePageId = it },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = indicatorBottomPadding),
                        )
                    }

                    if (showHomePageManager && showingHome && renderedPages.isNotEmpty()) {
                        HomePageManagerSheet(
                            pages = renderedPages,
                            selectedPageId = selectedHomePageId,
                            homeColumns = launcherPreferences.homeColumns,
                            homeRows = launcherPreferences.homeRows,
                            onSelectPage = { selectedHomePageId = it },
                            onMovePage = { pageId, targetRank ->
                                if (!launcherPreferences.layoutLocked) {
                                    lifecycleScope.launch {
                                        workspaceRuntimeCoordinator.moveHomePage(pageId, targetRank)
                                    }
                                }
                            },
                            onCreatePage = { createHomePage(true) },
                            onDeletePage = { pageId ->
                                if (!launcherPreferences.layoutLocked) {
                                    lifecycleScope.launch {
                                        val result = workspaceRuntimeCoordinator.deleteEmptyHomePage(pageId)
                                        if (
                                            result is WorkspacePagedRoomMutationResult.DeletedPage &&
                                            selectedHomePageId == result.pageId
                                        ) {
                                            selectedHomePageId = WorkspaceLegacyImportMapper.HOME_PAGE_ID
                                        }
                                    }
                                }
                            },
                            onDismiss = { showHomePageManager = false },
                            layoutLocked = launcherPreferences.layoutLocked,
                        )
                    }

                    // Normal Home pages remain visually clean. Page navigation is exposed
                    // through horizontal swipes and the bottom page dots; management belongs in
                    // the long-press Edit Home overview instead of a persistent top switcher.

                    // The layout-lock affordance is intentionally absent from the primary Home
                    // surface. It remains available from Launcher settings without permanently
                    // occupying wallpaper space.
                    if (launcherPreferences.layoutLocked && showingHome && !onPrimaryPage) {
                        LayoutLockHoldControl(
                            locked = true,
                            onUnlock = { launcherPreferencesRepository.setLayoutLocked(false) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .statusBarsPadding()
                                .padding(top = 72.dp, end = 12.dp),
                        )
                    }

                    if (
                        showingHome &&
                        onPrimaryPage &&
                        !experiencePreferences.homeHintsDismissed
                    ) {
                        LauncherHomeHintCard(
                            onDismiss = {
                                launcherPreferencesRepository.setHomeHintsDismissed(true)
                            },
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }

                    if (showWallpaperPicker) {
                        LauncherWallpaperPickerSheet(
                            onDismiss = { showWallpaperPicker = false },
                            onApply = { id ->
                                showWallpaperPicker = false
                                applyBuiltInWallpaper(id)
                            },
                            onOpenAndroidPicker = {
                                showWallpaperPicker = false
                                openSystemWallpaperPicker()
                            },
                        )
                    }
                }
                }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (::appWidgetHostController.isInitialized) {
            runCatching { appWidgetHostController.startListening() }
        }
    }

    override fun onStop() {
        if (::appWidgetHostController.isInitialized) {
            runCatching { appWidgetHostController.stopListening() }
        }
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        // LauncherApps callbacks remain collected for the Activity lifetime, so returning Home must
        // not force a full inventory rescan that makes icons visibly reload.
        LauncherNotificationBadges.refreshAccess(this)
        refreshHomeRoleState()
        // Workspace startup reconciliation is performed during initialization and mutation paths.
        // Re-running it for every HOME resume causes unnecessary Room churn and widget rebind work.
    }

    private fun refreshHomeRoleState() {
        val manager = getSystemService(RoleManager::class.java)
        defaultHomeState.value =
            manager.isRoleAvailable(RoleManager.ROLE_HOME) && manager.isRoleHeld(RoleManager.ROLE_HOME)
    }

    private fun requestHomeRole() {
        val manager = getSystemService(RoleManager::class.java)
        if (manager.isRoleAvailable(RoleManager.ROLE_HOME) && !manager.isRoleHeld(RoleManager.ROLE_HOME)) {
            homeRoleRequest.launch(manager.createRequestRoleIntent(RoleManager.ROLE_HOME))
        }
    }

    private fun continueAndroidWidgetSetup(appWidgetId: Int) {
        val info = appWidgetHostController.providerInfo(appWidgetId)
        if (info == null) {
            discardPendingAppWidget(appWidgetId)
            Toast.makeText(this, "That widget is no longer available.", Toast.LENGTH_SHORT).show()
            return
        }

        val configure = info.configure
        if (configure != null) {
            pendingAppWidgetId = appWidgetId
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                component = configure
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            runCatching { widgetConfigureRequest.launch(intent) }.onFailure {
                discardPendingAppWidget(appWidgetId)
                Toast.makeText(
                    this,
                    "That widget could not be configured.",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        } else {
            persistAndroidWidget(appWidgetId)
        }
    }

    private fun allocatePendingWidgetId(): Int? {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_APP_WIDGETS)) {
            Toast.makeText(this, "Android widgets are not supported on this device.", Toast.LENGTH_SHORT).show()
            return null
        }
        val appWidgetId = runCatching { appWidgetHostController.allocateAppWidgetId() }
            .getOrElse {
                Toast.makeText(this, "A widget ID could not be allocated.", Toast.LENGTH_SHORT).show()
                return null
            }
        pendingAppWidgetId = appWidgetId
        return appWidgetId
    }

    private fun beginAndroidWidgetBind(provider: ComponentName) {
        val appWidgetId = allocatePendingWidgetId() ?: return
        if (appWidgetHostController.bindAppWidgetIdIfAllowed(appWidgetId, provider)) {
            continueAndroidWidgetSetup(appWidgetId)
            return
        }

        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)
        }
        runCatching { widgetPickerRequest.launch(intent) }.onFailure {
            discardPendingAppWidget(appWidgetId)
            Toast.makeText(this, "Android could not authorize that widget.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun beginAndroidWidgetPick() {
        val appWidgetId = allocatePendingWidgetId() ?: return
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        runCatching { widgetPickerRequest.launch(intent) }.onFailure {
            discardPendingAppWidget(appWidgetId)
            Toast.makeText(this, "No Android widget picker is available.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun widgetResultId(data: Intent?): Int {
        val returned = data?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        return if (returned > 0) returned else pendingAppWidgetId
    }

    private fun discardPendingAppWidget(appWidgetId: Int) {
        val id = if (appWidgetId > 0) appWidgetId else pendingAppWidgetId
        pendingAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        if (::appWidgetHostController.isInitialized) {
            appWidgetHostController.deleteAppWidgetId(id)
        }
    }

    private fun persistAndroidWidget(appWidgetId: Int) {
        pendingAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        val info = appWidgetHostController.providerInfo(appWidgetId)
        if (info == null) {
            appWidgetHostController.deleteAppWidgetId(appWidgetId)
            return
        }

        lifecycleScope.launch {
            val preferences = launcherPreferencesRepository.preferences.first()
            val result = workspaceRuntimeCoordinator.addAndroidWidget(
                itemId = "widget:android:${UUID.randomUUID()}",
                appWidgetId = appWidgetId,
                providerComponent = info.provider.flattenToString(),
                columns = preferences.homeColumns,
                rows = preferences.homeRows,
            )
            if (result !is WorkspaceWidgetMutationResult.Added) {
                appWidgetHostController.deleteAppWidgetId(appWidgetId)
                Toast.makeText(
                    this@MainActivity,
                    if (result == WorkspaceWidgetMutationResult.NoSpace) {
                        "There is not enough room on Home for that widget."
                    } else {
                        "That widget could not be added."
                    },
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private fun createFolder(
        name: String,
        addToHome: Boolean,
        initialApp: LauncherActivityInfo?,
        profileId: Int,
    ) {
        lifecycleScope.launch {
            val primaryProfileId = Process.myUserHandle().hashCode()
            val folder = folderRepository.create(
                rawName = name,
                profileId = profileId,
            )
            if (folder == null) {
                Toast.makeText(
                    this@MainActivity,
                    "Folder could not be created.",
                    Toast.LENGTH_SHORT,
                ).show()
                return@launch
            }
            if (initialApp != null && initialApp.user.hashCode() == profileId) {
                if (
                    !folderRepository.addApp(
                        folderId = folder.id,
                        appKey = initialApp.workspaceKey(),
                        primaryProfileId = primaryProfileId,
                    )
                ) {
                    Toast.makeText(
                        this@MainActivity,
                        "Folder created, but the app could not be added.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
            if (addToHome && profileId == primaryProfileId) {
                addFolderToHomeInternal(folder)
            }
        }
    }

    private fun renameFolder(folderId: String, name: String) {
        lifecycleScope.launch {
            if (!folderRepository.rename(folderId, name)) {
                Toast.makeText(
                    this@MainActivity,
                    "Folder could not be renamed.",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private fun addAppToFolder(folderId: String, app: LauncherActivityInfo) {
        lifecycleScope.launch {
            val folder = folderRepository.folders.first()
                .firstOrNull { it.id == folderId }
            val primaryProfileId = Process.myUserHandle().hashCode()
            if (
                folder == null ||
                !LauncherFolderProfilePolicy.belongsToProfile(
                    folder = folder,
                    profileId = app.user.hashCode(),
                    primaryProfileId = primaryProfileId,
                )
            ) {
                Toast.makeText(
                    this@MainActivity,
                    "That folder belongs to a different Android profile.",
                    Toast.LENGTH_SHORT,
                ).show()
                return@launch
            }
            if (
                !folderRepository.addApp(
                    folderId = folderId,
                    appKey = app.workspaceKey(),
                    primaryProfileId = primaryProfileId,
                )
            ) {
                Toast.makeText(
                    this@MainActivity,
                    "App could not be added to that folder.",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private fun removeAppFromFolder(folderId: String, app: LauncherActivityInfo) {
        lifecycleScope.launch {
            if (!folderRepository.removeApp(folderId, app.workspaceKey())) {
                Toast.makeText(
                    this@MainActivity,
                    "App could not be removed from that folder.",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private fun addFolderToHome(folder: LauncherFolder) {
        val primaryProfileId = Process.myUserHandle().hashCode()
        if (
            !LauncherFolderProfilePolicy.belongsToProfile(
                folder = folder,
                profileId = primaryProfileId,
                primaryProfileId = primaryProfileId,
            )
        ) {
            Toast.makeText(
                this,
                "Work-profile folders stay in Work Apps.",
                Toast.LENGTH_SHORT,
            ).show()
            return
        }
        lifecycleScope.launch {
            addFolderToHomeInternal(folder)
        }
    }

    private suspend fun addFolderToHomeInternal(folder: LauncherFolder) {
        val preferences = launcherPreferencesRepository.preferences.first()
        when (
            workspaceRuntimeCoordinator.addFolderToHome(
                itemId = "folder:home:${UUID.randomUUID()}",
                folderId = folder.id,
                columns = preferences.homeColumns,
                rows = preferences.homeRows,
            )
        ) {
            is WorkspaceFolderMutationResult.Added -> Unit
            WorkspaceFolderMutationResult.NoSpace -> Toast.makeText(
                this@MainActivity,
                "There is not enough room on Home for that folder.",
                Toast.LENGTH_SHORT,
            ).show()
            else -> Toast.makeText(
                this@MainActivity,
                "Folder could not be added to Home.",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun moveHomeFolderToCell(folder: LauncherFolder, cellX: Int, cellY: Int) {
        lifecycleScope.launch {
            val preferences = launcherPreferencesRepository.preferences.first()
            if (preferences.layoutLocked) {
                Toast.makeText(
                    this@MainActivity,
                    "Unlock the Home layout to move folders.",
                    Toast.LENGTH_SHORT,
                ).show()
                return@launch
            }
            when (workspaceRuntimeCoordinator.movePrimaryHomeFolderToCell(
                folderId = folder.id,
                columns = preferences.homeColumns,
                rows = preferences.homeRows,
                cellX = cellX,
                cellY = cellY,
            )) {
                is WorkspaceFolderMutationResult.Moved -> Unit
                WorkspaceFolderMutationResult.NoSpace -> Toast.makeText(
                    this@MainActivity,
                    "This Home cell is occupied. Choose an empty cell.",
                    Toast.LENGTH_SHORT,
                ).show()
                else -> Toast.makeText(
                    this@MainActivity,
                    "Folder could not be moved; its original placement is preserved.",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private fun moveFolderToPage(
        folder: LauncherFolder,
        targetPageId: String,
        targetCellX: Int? = null,
        targetCellY: Int? = null,
        onMoved: (String) -> Unit = {},
    ) {
        lifecycleScope.launch {
            val prefs = launcherPreferencesRepository.preferences.first()
            if (prefs.layoutLocked) return@launch
            when (val result = workspaceRuntimeCoordinator.moveHomeFolderToPage(
                folderId = folder.id,
                targetPageId = targetPageId,
                columns = prefs.homeColumns,
                rows = prefs.homeRows,
                targetCellX = targetCellX,
                targetCellY = targetCellY,
            )) {
                is WorkspaceFolderMutationResult.MovedToPage -> onMoved(result.targetPageId)
                WorkspaceFolderMutationResult.NoSpace -> Toast.makeText(
                    this@MainActivity,
                    "There is not enough room on that Home page.",
                    Toast.LENGTH_SHORT,
                ).show()
                else -> Toast.makeText(
                    this@MainActivity,
                    "Folder could not be moved. Its existing placement was preserved.",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private fun removeFolderFromHome(folder: LauncherFolder) {
        lifecycleScope.launch {
            when (workspaceRuntimeCoordinator.removeFolderFromHome(folder.id)) {
                is WorkspaceFolderMutationResult.Removed,
                WorkspaceFolderMutationResult.NotFound,
                -> Unit
                else -> Toast.makeText(
                    this@MainActivity,
                    "Folder could not be removed from Home.",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private fun deleteFolder(folder: LauncherFolder) {
        lifecycleScope.launch {
            when (workspaceRuntimeCoordinator.removeFolderFromHome(folder.id)) {
                is WorkspaceFolderMutationResult.Removed,
                WorkspaceFolderMutationResult.NotFound,
                -> {
                    if (!folderRepository.delete(folder.id)) {
                        Toast.makeText(
                            this@MainActivity,
                            "Folder could not be deleted.",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
                else -> Toast.makeText(
                    this@MainActivity,
                    "Folder remains available because Home cleanup could not be verified.",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private fun addBuiltInWidget(typeId: String) {
        lifecycleScope.launch {
            val preferences = launcherPreferencesRepository.preferences.first()
            val result = workspaceRuntimeCoordinator.addBuiltInWidget(
                itemId = "widget:builtin:${UUID.randomUUID()}",
                typeId = typeId,
                columns = preferences.homeColumns,
                rows = preferences.homeRows,
            )
            if (result !is WorkspaceWidgetMutationResult.Added) {
                Toast.makeText(
                    this@MainActivity,
                    if (result == WorkspaceWidgetMutationResult.NoSpace) {
                        "There is not enough room on Home for that widget."
                    } else {
                        "That widget could not be added."
                    },
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private fun removeWidget(widget: WorkspaceRenderedHomeWidget) {
        lifecycleScope.launch {
            val result = workspaceRuntimeCoordinator.removeWidget(widget.itemId)
            if (result is WorkspaceWidgetMutationResult.Removed) {
                val descriptor = widget.descriptor
                if (descriptor is WorkspaceWidgetDescriptor.Android) {
                    appWidgetHostController.deleteAppWidgetId(descriptor.appWidgetId)
                }
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "That widget could not be removed.",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private fun moveWidget(
        widget: WorkspaceRenderedHomeWidget,
        cellX: Int,
        cellY: Int,
    ) {
        lifecycleScope.launch {
            val preferences = launcherPreferencesRepository.preferences.first()
            if (preferences.layoutLocked) return@launch
            val result = workspaceRuntimeCoordinator.moveWidget(
                itemId = widget.itemId,
                columns = preferences.homeColumns,
                rows = preferences.homeRows,
                cellX = cellX,
                cellY = cellY,
            )
            if (result !is WorkspaceWidgetMutationResult.Moved) {
                Toast.makeText(
                    this@MainActivity,
                    if (result == WorkspaceWidgetMutationResult.NoSpace) {
                        "That widget cannot fit there. Choose an unoccupied area."
                    } else {
                        "That widget could not be moved."
                    },
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private fun resizeWidget(
        widget: WorkspaceRenderedHomeWidget,
        spanX: Int,
        spanY: Int,
    ) {
        lifecycleScope.launch {
            val preferences = launcherPreferencesRepository.preferences.first()
            val result = workspaceRuntimeCoordinator.resizeWidget(
                itemId = widget.itemId,
                columns = preferences.homeColumns,
                rows = preferences.homeRows,
                spanX = spanX,
                spanY = spanY,
            )
            if (result !is WorkspaceWidgetMutationResult.Resized) {
                Toast.makeText(
                    this@MainActivity,
                    if (result == WorkspaceWidgetMutationResult.NoSpace) {
                        "That widget size does not fit the current Home layout."
                    } else {
                        "That widget could not be resized."
                    },
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }
    private fun moveWidgetToPage(
        widget: WorkspaceRenderedHomeWidget,
        targetPageId: String,
        targetCellX: Int? = null,
        targetCellY: Int? = null,
        onMoved: (String) -> Unit = {},
    ) {
        lifecycleScope.launch {
            val preferences = launcherPreferencesRepository.preferences.first()
            if (preferences.layoutLocked) return@launch
            when (val result = workspaceRuntimeCoordinator.moveWidgetToPage(
                itemId = widget.itemId,
                targetPageId = targetPageId,
                columns = preferences.homeColumns,
                rows = preferences.homeRows,
                targetCellX = targetCellX,
                targetCellY = targetCellY,
            )) {
                is WorkspaceWidgetMutationResult.MovedToPage -> onMoved(result.targetPageId)
                WorkspaceWidgetMutationResult.NoSpace -> Toast.makeText(
                    this@MainActivity,
                    "That widget cannot fit at the requested destination.",
                    Toast.LENGTH_SHORT,
                ).show()
                else -> Toast.makeText(
                    this@MainActivity,
                    "That widget could not be moved. Its existing placement was preserved.",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }


    private fun setSearchProviderEnabled(
        state: LauncherSearchProviderControlState,
        providerId: String,
        enabled: Boolean,
    ) {
        val snapshot = LauncherSearchProviderUserControlPolicy.withProviderEnabled(
            state = state,
            providerId = providerId,
            enabled = enabled,
        )

        if (
            providerId == LauncherConnectedSearchProviderRegistry.GOOGLE_DRIVE_PROVIDER_ID
        ) {
            if (enabled) {
                requestGoogleDriveSearchAuthorization(snapshot)
            } else {
                disconnectGoogleDriveSearch(snapshot)
            }
            return
        }

        if (enabled && providerId == LauncherFilesSearchProvider.PROVIDER_ID) {
            lifecycleScope.launch {
                val roots = fileSearchPreferencesRepository.roots.first()
                if (roots.isEmpty()) {
                    pendingFileSearchProviderSnapshot = snapshot
                    pendingFileSearchProviderId = providerId
                    fileSearchRootRequest.launch(null)
                } else {
                    searchProviderPreferencesRepository.set(snapshot)
                }
            }
            return
        }

        val permission = LauncherLocalSearchPermissions.permissionFor(providerId)
        if (
            enabled &&
            permission != null &&
            ContextCompat.checkSelfPermission(this, permission) !=
                PackageManager.PERMISSION_GRANTED
        ) {
            pendingSearchProviderSnapshot = snapshot
            pendingSearchProviderId = providerId
            searchSourcePermissionRequest.launch(permission)
            return
        }

        lifecycleScope.launch {
            searchProviderPreferencesRepository.set(snapshot)
        }
    }

    private fun restoreGoogleDriveSearchAuthorizationIfGranted() {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(GOOGLE_DRIVE_METADATA_READONLY_SCOPE)))
            .build()
        googleDriveAuthorizationClient.authorize(request)
            .addOnSuccessListener { result ->
                if (!result.hasResolution()) {
                    val token = result.accessToken?.trim().orEmpty()
                    if (
                        token.isNotEmpty() &&
                        GOOGLE_DRIVE_METADATA_READONLY_SCOPE in result.grantedScopes
                    ) {
                        LauncherGoogleDriveAuthorizationState.setAccessToken(token)
                    }
                }
            }
    }

    private fun requestGoogleDriveSearchAuthorization(
        snapshot: LauncherSearchProviderPreferenceSnapshot,
    ) {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(GOOGLE_DRIVE_METADATA_READONLY_SCOPE)))
            .build()

        googleDriveAuthorizationClient.authorize(request)
            .addOnSuccessListener { result ->
                if (result.hasResolution()) {
                    val pendingIntent = result.pendingIntent
                    if (pendingIntent == null) {
                        Toast.makeText(
                            this,
                            "Google Drive authorization could not be opened.",
                            Toast.LENGTH_SHORT,
                        ).show()
                        return@addOnSuccessListener
                    }
                    pendingGoogleDriveSearchProviderSnapshot = snapshot
                    googleDriveAuthorizationRequest.launch(
                        IntentSenderRequest.Builder(pendingIntent.intentSender).build(),
                    )
                } else {
                    acceptGoogleDriveAuthorization(result, snapshot)
                }
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Google Drive authorization is unavailable on this device.",
                    Toast.LENGTH_LONG,
                ).show()
            }
    }

    private fun acceptGoogleDriveAuthorization(
        result: AuthorizationResult,
        snapshot: LauncherSearchProviderPreferenceSnapshot,
    ) {
        val token = result.accessToken?.trim().orEmpty()
        val scopeGranted = GOOGLE_DRIVE_METADATA_READONLY_SCOPE in result.grantedScopes
        if (token.isEmpty() || !scopeGranted) {
            Toast.makeText(
                this,
                "Google Drive permission was not granted. Drive Search stays off.",
                Toast.LENGTH_LONG,
            ).show()
            return
        }

        LauncherGoogleDriveAuthorizationState.setAccessToken(token)
        lifecycleScope.launch {
            searchProviderPreferencesRepository.set(snapshot)
        }
    }

    private fun disconnectGoogleDriveSearch(
        snapshot: LauncherSearchProviderPreferenceSnapshot,
    ) {
        val token = LauncherGoogleDriveAuthorizationState.clearAccessToken()
        if (!token.isNullOrBlank()) {
            googleDriveAuthorizationClient.clearToken(
                ClearTokenRequest.builder().setToken(token).build(),
            )
        }
        lifecycleScope.launch {
            searchProviderPreferencesRepository.set(snapshot)
        }
    }

    private fun chooseFileSearchRoot() {
        fileSearchRootRequest.launch(null)
    }

    private fun confirmRemoveFileSearchRoot(uri: Uri) {
        AlertDialog.Builder(this)
            .setTitle("Remove Search folder?")
            .setMessage(
                "Launcher will stop searching this folder and release its saved read access. " +
                    "You can choose the folder again later.",
            )
            .setPositiveButton("Remove") { _, _ ->
                lifecycleScope.launch {
                    fileSearchPreferencesRepository.removeRoot(uri)
                    runCatching {
                        contentResolver.releasePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                    }
                    Toast.makeText(
                        this@MainActivity,
                        "Search folder removed.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openSearchUri(action: LauncherOpenUriSearchAction) {
        val intent = Intent(action.intentAction, Uri.parse(action.uri))
        runCatching { startActivity(intent) }.onFailure {
            Toast.makeText(
                this,
                "No compatible app is available for this result.",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun openDocument(action: LauncherOpenDocumentSearchAction) {
        val uri = Uri.parse(action.uri)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, action.mimeType ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { startActivity(intent) }.onFailure {
            Toast.makeText(
                this,
                "No compatible app is available for this file.",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun searchWithConnectedProvider(providerId: String, rawQuery: String) {
        val intent = LauncherConnectedSearchProviderRegistry.buildExplicitHandoffIntent(
            context = this,
            providerId = providerId,
            rawQuery = rawQuery,
        )
        if (intent == null) {
            Toast.makeText(
                this,
                "That connected Search provider is unavailable.",
                Toast.LENGTH_SHORT,
            ).show()
            return
        }

        runCatching { startActivity(intent) }.onFailure {
            Toast.makeText(
                this,
                "That connected Search provider could not be opened.",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun requestUninstall(app: LauncherActivityInfo) {
        if (app.user != Process.myUserHandle()) {
            runCatching { appsRepository.openDetails(app) }
                .onSuccess {
                    Toast.makeText(
                        this,
                        "Opened app info for this Android profile. Choose Uninstall there.",
                        Toast.LENGTH_LONG,
                    ).show()
                }
                .onFailure {
                    Toast.makeText(
                        this,
                        "Android app info is unavailable for this profile.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            return
        }

        val request = LauncherUninstallRequestPolicy.create(app.componentName.packageName)
        if (request == null) {
            Toast.makeText(
                this,
                "Android uninstall is unavailable for this app.",
                Toast.LENGTH_SHORT,
            ).show()
            return
        }

        val intent = Intent(
            request.action,
            Uri.fromParts(request.uriScheme, request.packageName, null),
        ).putExtra(Intent.EXTRA_RETURN_RESULT, false)
        runCatching { startActivity(intent) }.onFailure {
            runCatching { appsRepository.openDetails(app) }
                .onSuccess {
                    Toast.makeText(
                        this,
                        "Android uninstall confirmation could not open. Opened App info—choose Uninstall there.",
                        Toast.LENGTH_LONG,
                    ).show()
                }
                .onFailure {
                    Toast.makeText(
                        this,
                        "Android uninstall is unavailable for this app.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
        }
    }

    private fun applyBuiltInWallpaper(id: LauncherBuiltInWallpaperId) {
        lifecycleScope.launch(Dispatchers.Default) {
            val metrics = resources.displayMetrics
            val width = metrics.widthPixels.coerceAtLeast(1080)
            val height = metrics.heightPixels.coerceAtLeast(1920)
            val bitmap = LauncherBuiltInWallpapers.render(id, width, height)
            val result = runCatching {
                WallpaperManager.getInstance(this@MainActivity).setBitmap(
                    bitmap,
                    null,
                    true,
                    WallpaperManager.FLAG_SYSTEM,
                )
            }
            bitmap.recycle()
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@MainActivity,
                    if (result.isSuccess) "Wallpaper applied" else "Wallpaper could not be applied",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private fun openWallpaperPicker() {
        showWallpaperPicker = true
    }

    private fun openSystemWallpaperPicker() {
        runCatching {
            startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SET_WALLPAPER),
                    "Choose wallpaper",
                ),
            )
        }.onFailure {
            Toast.makeText(
                this,
                "No wallpaper picker is available",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun setSystemBarIconAppearance(useDarkIcons: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = useDarkIcons
            isAppearanceLightNavigationBars = useDarkIcons
        }
    }

    private companion object {
        const val STARTER_GLANCE_WIDGET_ID = "widget:builtin:starter-glance-v1"
        const val GOOGLE_DRIVE_METADATA_READONLY_SCOPE =
            "https://www.googleapis.com/auth/drive.metadata.readonly"
    }

}
