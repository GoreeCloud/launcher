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
import android.os.Bundle
import android.os.Process
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
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
import com.goreecloud.launcher.core.launcher.LauncherFolderRepository
import com.goreecloud.launcher.core.launcher.LauncherInstalledAppBaselineRepository
import com.goreecloud.launcher.core.launcher.LauncherHomeAppMode
import com.goreecloud.launcher.core.launcher.LauncherHomeSearchPlacement
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
import com.goreecloud.launcher.core.launcher.LauncherPreferencesRepository
import com.goreecloud.launcher.core.launcher.LauncherSearchProviderControlState
import com.goreecloud.launcher.core.launcher.LauncherSearchProviderPreferenceDecodeResult
import com.goreecloud.launcher.core.launcher.LauncherSearchProviderPreferenceSnapshot
import com.goreecloud.launcher.core.launcher.LauncherSearchProviderUserControlPolicy
import com.goreecloud.launcher.core.launcher.LauncherSearchProviderPreferencesRepository
import com.goreecloud.launcher.core.launcher.LauncherUniversalSearchHomeMode
import com.goreecloud.launcher.core.launcher.LauncherUninstallRequestPolicy
import com.goreecloud.launcher.core.launcher.LauncherWallpaperShade
import com.goreecloud.launcher.core.launcher.LauncherWidgetProviderDescriptor
import com.goreecloud.launcher.core.launcher.StarterWorkspaceCandidate
import com.goreecloud.launcher.core.launcher.StarterWorkspacePolicy
import com.goreecloud.launcher.core.workspace.MAX_DOCK_ITEMS
import com.goreecloud.launcher.core.workspace.WorkspaceAuthority
import com.goreecloud.launcher.core.workspace.WorkspaceRepository
import com.goreecloud.launcher.core.workspace.WorkspaceState
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
import com.goreecloud.launcher.ui.HomePageSwitcher
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
import com.goreecloud.launcher.ui.theme.GlazeTheme
import com.goreecloud.launcher.ui.theme.GlazeThemeRepository
import com.goreecloud.launcher.ui.theme.rememberAndroidGlazeV16PresentationContext
import kotlinx.coroutines.Dispatchers
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

    private val fileSearchRootRequest =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            val pending = pendingFileSearchProviderSnapshot
            pendingFileSearchProviderSnapshot = null
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LauncherNotificationBadges.initialize(this)
        LauncherFolderAppearance.initialize(this)
        enableEdgeToEdge()
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
            val portableRestoreRecovery by portableRestoreRecoveryResult.collectAsStateWithLifecycle()

            if (!LauncherPortableRestoreStartupGate.allowsMutations(portableRestoreRecovery)) {
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

            val apps by appsRepository.apps.collectAsStateWithLifecycle(initialValue = emptyList())
            val availableAndroidWidgets = remember(apps) {
                appWidgetHostController.installedProviders()
            }
            val availableIconPacks = remember(apps) {
                LauncherIconPackRepository(this@MainActivity).discover()
            }
            val launcherPreferences by launcherPreferencesRepository.preferences.collectAsStateWithLifecycle(
                initialValue = launcherPreferencesRepository.defaults,
            )
            val drawerLayoutMode by launcherPreferencesRepository.drawerLayoutMode.collectAsStateWithLifecycle(
                initialValue = LauncherDrawerLayoutMode.GRID,
            )
            val experiencePreferences by launcherPreferencesRepository.experiencePreferences.collectAsStateWithLifecycle(
                initialValue = LauncherExperiencePreferences(),
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
            val folders by folderRepository.folders.collectAsStateWithLifecycle(
                initialValue = emptyList(),
            )
            val placement by workspaceRuntimeCoordinator.observePlacement().collectAsStateWithLifecycle(
                initialValue = WorkspaceAuthoritativePlacementState.WaitingForInitialization
            )
            val pagedHome by workspaceRuntimeCoordinator.observeHomePages().collectAsStateWithLifecycle(
                initialValue = WorkspacePagedHomeState.WaitingForRoom
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
            val renderedPages = (pagedHome as? WorkspacePagedHomeState.Ready)?.pages.orEmpty()
            var selectedHomePageId by rememberSaveable {
                mutableStateOf(WorkspaceLegacyImportMapper.HOME_PAGE_ID)
            }
            var primarySurfaceModeName by rememberSaveable {
                mutableStateOf(LauncherSurfaceMode.HOME.name)
            }
            val primarySurfaceMode = runCatching {
                LauncherSurfaceMode.valueOf(primarySurfaceModeName)
            }.getOrDefault(LauncherSurfaceMode.HOME)

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
                        initialShowHints = true,
                        onRequestHomeRole = ::requestHomeRole,
                        onFinish = { configuration ->
                            launcherPreferencesRepository.setHomeAppMode(configuration.homeAppMode)
                            launcherPreferencesRepository.setHomeGrid(
                                configuration.homeColumns,
                                configuration.homeRows,
                            )
                            launcherPreferencesRepository.setShowHomeLabels(
                                configuration.showHomeLabels,
                            )
                            launcherPreferencesRepository.setUniversalSearchHomeMode(
                                configuration.universalSearchHomeMode,
                            )
                            launcherPreferencesRepository.setAddNewAppsToHome(
                                configuration.addNewAppsToHome,
                            )
                            launcherPreferencesRepository.setHomeHintsDismissed(
                                !configuration.showHints,
                            )
                            launcherPreferencesRepository.markStartupWizardCompleted()
                        },
                    )
                } else {
                Box {
                    val selectedPage = renderedPages.firstOrNull { it.pageId == selectedHomePageId }
                    val onPrimaryPage = selectedPage == null ||
                        selectedPage.pageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID
                    val showingHome = !onPrimaryPage || primarySurfaceMode == LauncherSurfaceMode.HOME

                    if (!onPrimaryPage) {
                        val secondaryPage = checkNotNull(selectedPage)
                        ReadOnlyPagedHomeSurface(
                            apps = apps,
                            folders = folders,
                            page = secondaryPage,
                            pages = renderedPages,
                            homeColumns = launcherPreferences.homeColumns,
                            showLabels = experiencePreferences.showHomeLabels,
                            iconScale = launcherPreferences.iconScale,
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
                                        )
                                        if (result is WorkspacePagedRoomMutationResult.UpdatedItem) {
                                            selectedHomePageId = result.pageId
                                        }
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
                        )
                    } else {
                        LauncherBetaRoot(
                            apps = apps,
                            workspace = workspace,
                            preferences = launcherPreferences,
                            drawerLayoutMode = drawerLayoutMode,
                            experiencePreferences = experiencePreferences,
                            recentAppKeys = localRecentAppKeys,
                            localLaunchCounts = localLaunchCounts,
                            searchProviderPreferences = searchProviderPreferences,
                            fileSearchRoots = fileSearchRoots,
                            homePageCount = renderedPages.size.coerceAtLeast(1),
                            homeResetSequence = homeResetSequenceValue,
                            homeLabelOverrides = homeLabelOverrides,
                            folders = folders,
                            primaryHomePage = renderedPages.firstOrNull {
                                it.pageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID
                            },
                            homePages = renderedPages,
                            onMoveFolderToPage = { folder, target ->
                                moveFolderToPage(folder, target) { selectedHomePageId = it }
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
                                val secondaryPage = renderedPages.firstOrNull {
                                    it.pageId != WorkspaceLegacyImportMapper.HOME_PAGE_ID
                                }
                                if (secondaryPage != null) {
                                    selectedHomePageId = secondaryPage.pageId
                                } else if (!launcherPreferences.layoutLocked) {
                                    val pageId = "home:user:${UUID.randomUUID()}"
                                    lifecycleScope.launch {
                                        val result = workspaceRuntimeCoordinator.createHomePage(pageId)
                                        if (result is WorkspacePagedRoomMutationResult.CreatedPage) {
                                            selectedHomePageId = result.pageId
                                        }
                                    }
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
                            onShowHintsAgain = {
                                launcherPreferencesRepository.setHomeHintsDismissed(false)
                            },
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
                            onRequestUninstall = ::requestUninstall,
                            onOpenWallpaperPicker = ::openWallpaperPicker,
                            onSurfaceModeChanged = { mode ->
                                primarySurfaceModeName = mode.name
                                LauncherTransitionDiagnostics.recordSurfaceMode(mode)
                            },
                        )
                    }

                    if (
                        experiencePreferences.showHomePageIndicator &&
                        renderedPages.size > 1 &&
                        showingHome
                    ) {
                        val indicatorBottomPadding = when {
                            !onPrimaryPage -> 24.dp
                            launcherPreferences.universalSearchHomeMode == LauncherUniversalSearchHomeMode.PERMANENT &&
                                experiencePreferences.homeSearchPlacement == LauncherHomeSearchPlacement.BOTTOM ->
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

                    // Detailed page-management controls remain available on secondary pages.
                    val showPageSwitcher = renderedPages.size > 1 && showingHome && !onPrimaryPage
                    if (showPageSwitcher) {
                        HomePageSwitcher(
                            pages = renderedPages,
                            selectedPageId = selectedHomePageId,
                            onSelectPage = { selectedHomePageId = it },
                            onMovePage = { pageId, targetRank ->
                                if (!launcherPreferences.layoutLocked) {
                                    lifecycleScope.launch {
                                        workspaceRuntimeCoordinator.moveHomePage(pageId, targetRank)
                                    }
                                }
                            },
                            onCreatePage = {
                                if (!launcherPreferences.layoutLocked) {
                                    val pageId = "home:user:${UUID.randomUUID()}"
                                    lifecycleScope.launch {
                                        val result = workspaceRuntimeCoordinator.createHomePage(pageId)
                                        if (result is WorkspacePagedRoomMutationResult.CreatedPage) {
                                            selectedHomePageId = result.pageId
                                        }
                                    }
                                }
                            },
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
                            layoutLocked = launcherPreferences.layoutLocked,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }

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
        LauncherNotificationBadges.refreshAccess(this)
        refreshHomeRoleState()
        if (
            ::workspaceRuntimeCoordinator.isInitialized &&
            LauncherPortableRestoreStartupGate.allowsMutations(portableRestoreRecoveryResult.value)
        ) {
            lifecycleScope.launch {
                workspaceRuntimeCoordinator.reconcileAndActivate()
            }
        }
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
    ) {
        lifecycleScope.launch {
            val folder = folderRepository.create(name)
            if (folder == null) {
                Toast.makeText(
                    this@MainActivity,
                    "Folder could not be created.",
                    Toast.LENGTH_SHORT,
                ).show()
                return@launch
            }
            if (initialApp != null && initialApp.user == Process.myUserHandle()) {
                if (!folderRepository.addApp(folder.id, initialApp.workspaceKey())) {
                    Toast.makeText(
                        this@MainActivity,
                        "Folder created, but the app could not be added.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
            if (addToHome) addFolderToHomeInternal(folder)
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
        if (app.user != Process.myUserHandle()) {
            Toast.makeText(
                this@MainActivity,
                "Personal folders cannot contain apps from another profile.",
                Toast.LENGTH_SHORT,
            ).show()
            return
        }
        lifecycleScope.launch {
            if (!folderRepository.addApp(folderId, app.workspaceKey())) {
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

        if (enabled && providerId == LauncherFilesSearchProvider.PROVIDER_ID) {
            lifecycleScope.launch {
                val roots = fileSearchPreferencesRepository.roots.first()
                if (roots.isEmpty()) {
                    pendingFileSearchProviderSnapshot = snapshot
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

}
