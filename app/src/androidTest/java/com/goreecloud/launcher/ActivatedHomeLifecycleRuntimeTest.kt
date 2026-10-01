package com.goreecloud.launcher

import android.app.role.RoleManager
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.goreecloud.launcher.core.launcher.LauncherAppsRepository
import com.goreecloud.launcher.core.launcher.LauncherGestureAction
import com.goreecloud.launcher.core.launcher.LauncherGestureActionType
import com.goreecloud.launcher.core.launcher.LauncherHomeAppMode
import com.goreecloud.launcher.core.launcher.LauncherHomeGesture
import com.goreecloud.launcher.core.launcher.LauncherLocalUsageRepository
import com.goreecloud.launcher.core.launcher.LauncherPreferencesRepository
import com.goreecloud.launcher.core.workspace.WorkspaceAuthority
import com.goreecloud.launcher.core.workspace.WorkspaceGridPlacement
import com.goreecloud.launcher.core.workspace.WorkspaceRepository
import com.goreecloud.launcher.core.workspace.WorkspaceWidgetCatalog
import com.goreecloud.launcher.core.workspace.db.LauncherDatabaseProvider
import com.goreecloud.launcher.core.workspace.db.WorkspaceAuthoritativePlacementState
import com.goreecloud.launcher.core.workspace.db.WorkspaceAuthoritativeWriteResult
import com.goreecloud.launcher.core.workspace.db.WorkspaceLegacyImportMapper
import com.goreecloud.launcher.core.workspace.db.WorkspacePagedHomeState
import com.goreecloud.launcher.core.workspace.db.WorkspacePagedRoomMutationResult
import com.goreecloud.launcher.core.workspace.db.WorkspacePrimaryHomeSpatialResult
import com.goreecloud.launcher.core.workspace.db.WorkspaceProductionRuntimeCoordinator
import com.goreecloud.launcher.core.workspace.db.WorkspaceRoomPlacementRepository
import com.goreecloud.launcher.core.workspace.db.WorkspaceRoomWriteResult
import com.goreecloud.launcher.core.workspace.db.WorkspaceWidgetMutationResult
import com.goreecloud.launcher.core.workspace.workspaceKey
import java.io.FileInputStream
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActivatedHomeLifecycleRuntimeTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private lateinit var lifecyclePreferencesRepository: LauncherPreferencesRepository
    private var previousHomeAppMode = LauncherHomeAppMode.NONE

    @Before
    fun isolatePersistedWorkspaceTestsFromAutomaticHomeApps() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        lifecyclePreferencesRepository = LauncherPreferencesRepository(context)
        previousHomeAppMode =
            lifecyclePreferencesRepository.experiencePreferences.first().homeAppMode
        lifecyclePreferencesRepository.setHomeAppMode(LauncherHomeAppMode.NONE).join()
        withTimeout(5_000) {
            lifecyclePreferencesRepository.experiencePreferences.first {
                it.homeAppMode == LauncherHomeAppMode.NONE
            }
        }
        LauncherLocalUsageRepository(context).clear().join()
    }

    @After
    fun restoreAutomaticHomeAppMode() = runBlocking {
        lifecyclePreferencesRepository.setHomeAppMode(previousHomeAppMode).join()
        withTimeout(5_000) {
            lifecyclePreferencesRepository.experiencePreferences.first {
                it.homeAppMode == previousHomeAppMode
            }
        }
        Unit
    }

    @Before
    fun completeStartupForEstablishedRuntimeTests() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = LauncherPreferencesRepository(context)
        preferences.setHomeHintsDismissed(true).join()
        preferences.markStartupWizardCompleted().join()
    }

    @Test
    fun recreatedMainActivityRecollectsRoomPlacementAndRemainsReactive() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val roleManager = context.getSystemService(RoleManager::class.java)
        val alreadyDefaultHome =
            roleManager.isRoleAvailable(RoleManager.ROLE_HOME) && roleManager.isRoleHeld(RoleManager.ROLE_HOME)

        if (!alreadyDefaultHome) {
            runShellCommand(
                "cmd role add-role-holder ${RoleManager.ROLE_HOME} ${context.packageName}"
            )
            withTimeout(10_000) {
                while (!roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                    delay(100)
                }
            }
        }

        try {
            val apps = withTimeout(10_000) {
                LauncherAppsRepository(context).apps.first { candidates ->
                    candidates.count { it.componentName.packageName != context.packageName } >= 2
                }
            }
            val candidates = apps
                .filter { it.componentName.packageName != context.packageName }
                .distinctBy { it.label.toString() }
            check(candidates.size >= 2) { "API 36 lifecycle test requires two distinct launchable app labels." }

            val firstApp = candidates[0]
            val secondApp = candidates[1]
            val firstKey = firstApp.workspaceKey()
            val secondKey = secondApp.workspaceKey()
            val repository = WorkspaceRepository(context)
            repository.ensureDefaults(
                favoriteKeys = listOf(firstKey),
                dockKeys = emptyList(),
            )

            val scenario = ActivityScenario.launch(MainActivity::class.java)
            try {
                withTimeout(15_000) {
                    repository.state.first { it.authority == WorkspaceAuthority.ROOM }
                }
                val preferences = LauncherPreferencesRepository(context).preferences.first()
                val roomPlacement = WorkspaceRoomPlacementRepository(
                    authorityRepository = repository,
                    workspaceDaoProvider = {
                        LauncherDatabaseProvider.get(context).workspaceDao()
                    },
                )
                val baseline = roomPlacement.replace(
                    favoriteKeys = listOf(firstKey),
                    dockKeys = emptyList(),
                    homeGrid = WorkspaceGridPlacement.Grid(
                        columns = preferences.homeColumns,
                        rows = preferences.homeRows,
                    ),
                )
                check(baseline is WorkspaceRoomWriteResult.Written)
                waitForDisplayedLabel(firstApp.label.toString())

                scenario.recreate()

                withTimeout(15_000) {
                    repository.state.first { it.authority == WorkspaceAuthority.ROOM }
                }
                waitForDisplayedLabel(firstApp.label.toString())

                val runtime = WorkspaceProductionRuntimeCoordinator(
                    authorityRepository = repository,
                    workspaceDaoProvider = {
                        LauncherDatabaseProvider.get(context).workspaceDao()
                    },
                )
                val write = runtime.toggleFavorite(
                    key = secondKey,
                    homeColumns = preferences.homeColumns,
                    homeRows = preferences.homeRows,
                )
                check(write is WorkspaceAuthoritativeWriteResult.Written)
                assertEquals(WorkspaceAuthority.ROOM, repository.state.first().authority)

                waitForDisplayedLabel(secondApp.label.toString())
            } finally {
                scenario.close()
            }
        } finally {
            if (!alreadyDefaultHome) {
                runShellCommand(
                    "cmd role remove-role-holder ${RoleManager.ROLE_HOME} ${context.packageName}"
                )
            }
        }
    }

    @Test
    fun horizontalSwipeSwitchesHomePagesAndReturns() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val roleManager = context.getSystemService(RoleManager::class.java)
        val alreadyDefaultHome =
            roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
                roleManager.isRoleHeld(RoleManager.ROLE_HOME)

        if (!alreadyDefaultHome) {
            runShellCommand(
                "cmd role add-role-holder ${RoleManager.ROLE_HOME} ${context.packageName}"
            )
            withTimeout(10_000) {
                while (!roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                    delay(100)
                }
            }
        }

        val secondaryPageId = "home:test:horizontal-swipe"
        val repository = WorkspaceRepository(context)
        val preferencesRepository = LauncherPreferencesRepository(context)
        val previousSwipeRight =
            preferencesRepository.experiencePreferences.first().swipeRightAction
        val searchAction =
            LauncherGestureAction.builtIn(LauncherGestureActionType.UNIVERSAL_SEARCH)
        var runtime: WorkspaceProductionRuntimeCoordinator? = null
        var directlyAddedPageId: String? = null

        try {
            val apps = withTimeout(10_000) {
                LauncherAppsRepository(context).apps.first { candidates ->
                    candidates.any { it.componentName.packageName != context.packageName }
                }
            }
            val candidate = apps.first { it.componentName.packageName != context.packageName }
            repository.ensureDefaults(
                favoriteKeys = listOf(candidate.workspaceKey()),
                dockKeys = listOf(candidate.workspaceKey()),
            )

            val scenario = ActivityScenario.launch(MainActivity::class.java)
            try {
                withTimeout(15_000) {
                    repository.state.first { it.authority == WorkspaceAuthority.ROOM }
                }

                runtime = WorkspaceProductionRuntimeCoordinator(
                    authorityRepository = repository,
                    workspaceDaoProvider = {
                        LauncherDatabaseProvider.get(context).workspaceDao()
                    },
                )

                // Let the launched Home root finish its own startup reconciliation before this
                // test performs a direct Room page mutation. Racing startup-owned placement work can
                // correctly produce a snapshot-conflict result even though page creation itself is
                // healthy.
                composeRule.waitUntil(timeoutMillis = 15_000) {
                    composeRule
                        .onAllNodesWithTag("launcher-home-swipe-surface", useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
                composeRule.waitForIdle()
                withTimeout(10_000) {
                    runtime!!.observeHomePages().first { state ->
                        state is WorkspacePagedHomeState.Ready
                    }
                }

                // ensureDefaults() only seeds an uninitialized workspace. This test may run after
                // another lifecycle case has already initialized Room, so explicitly ensure the
                // candidate exists in the authoritative Dock before asserting persistent-Dock
                // geometry on the secondary page.
                val placement = withTimeout(10_000) {
                    runtime!!.observePlacement().first { state ->
                        state is WorkspaceAuthoritativePlacementState.Ready
                    }
                } as WorkspaceAuthoritativePlacementState.Ready
                if (candidate.workspaceKey() !in placement.snapshot.dockKeys) {
                    val dockWrite = runtime!!.toggleDock(candidate.workspaceKey())
                    check(dockWrite is WorkspaceAuthoritativeWriteResult.Written) {
                        "Expected authoritative Dock placement; result was $dockWrite."
                    }
                    withTimeout(10_000) {
                        runtime!!.observePlacement().first { state ->
                            state is WorkspaceAuthoritativePlacementState.Ready &&
                                candidate.workspaceKey() in state.snapshot.dockKeys
                        }
                    }
                }
                waitForDisplayedTag("launcher-home-dock")

                // Remove any residue from an interrupted prior emulator attempt, then create one
                // empty secondary page so the test exercises the exact primary -> secondary path.
                runtime?.deleteEmptyHomePage(secondaryPageId)
                val created = runtime?.createHomePage(secondaryPageId)
                check(
                    created is WorkspacePagedRoomMutationResult.CreatedPage ||
                        created is WorkspacePagedRoomMutationResult.PageAlreadyExists
                ) { "Expected a usable secondary Home page; create result was $created." }

                val placementAfterPageCreate = withTimeout(10_000) {
                    runtime!!.observePlacement().first { state ->
                        state is WorkspaceAuthoritativePlacementState.Ready
                    }
                } as WorkspaceAuthoritativePlacementState.Ready
                check(candidate.workspaceKey() in placementAfterPageCreate.snapshot.dockKeys) {
                    "Creating a secondary Home page must not remove the authoritative Dock item; " +
                        "dockKeys=${placementAfterPageCreate.snapshot.dockKeys}"
                }
                waitForDisplayedTag("launcher-home-dock")

                composeRule.waitUntil(timeoutMillis = 10_000) {
                    composeRule
                        .onAllNodesWithTag(
                            "launcher-home-page-indicator",
                            useUnmergedTree = true,
                        )
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
                // The page indicator enters with Home content. Wait for rendered visibility rather
                // than racing the bounded transition after semantic-tree insertion.
                waitForDisplayedTag("launcher-home-page-indicator")
                val indicatorBounds = composeRule
                    .onNodeWithTag("launcher-home-page-indicator", useUnmergedTree = true)
                    .fetchSemanticsNode()
                    .boundsInRoot
                val density = context.resources.displayMetrics.density
                check(indicatorBounds.width <= 64f * density) {
                    "Two-page Home indicator must remain compact."
                }
                check(indicatorBounds.height <= 24f * density) {
                    "Home indicator must not consume a full touch-target row."
                }

                composeRule
                    .onNodeWithTag("launcher-home-swipe-surface", useUnmergedTree = true)
                    .performTouchInput {
                        swipeLeft(
                            startX = right - 24f,
                            endX = left + 24f,
                            durationMillis = 420,
                        )
                    }

                composeRule.waitUntil(timeoutMillis = 10_000) {
                    composeRule
                        .onAllNodesWithTag(
                            "launcher-home-page-" + secondaryPageId,
                            useUnmergedTree = true,
                        )
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
                check(
                    composeRule
                        .onAllNodesWithTag("launcher-home-empty-page", useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .isNotEmpty(),
                ) { "Empty secondary Home page should render as a clean page surface." }
                check(
                    composeRule
                        .onAllNodesWithText("This Home page is empty.", useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .isEmpty(),
                ) { "Normal Home must not show an empty-page message." }
                check(
                    composeRule
                        .onAllNodesWithText("Page 2", useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .isEmpty(),
                ) { "Normal Home must not show numbered page chrome." }
                waitForDisplayedTag("launcher-home-page-indicator")
                val secondaryIndicatorBounds = composeRule
                    .onNodeWithTag("launcher-home-page-indicator", useUnmergedTree = true)
                    .fetchSemanticsNode()
                    .boundsInRoot
                val placementOnSecondary = withTimeout(10_000) {
                    runtime!!.observePlacement().first { state ->
                        state is WorkspaceAuthoritativePlacementState.Ready
                    }
                } as WorkspaceAuthoritativePlacementState.Ready
                check(candidate.workspaceKey() in placementOnSecondary.snapshot.dockKeys) {
                    "Secondary Home selection must not remove authoritative Dock placement; " +
                        "dockKeys=${placementOnSecondary.snapshot.dockKeys}"
                }
                withTimeout(10_000) {
                    LauncherAppsRepository(context).apps.first { inventory ->
                        inventory.any { it.workspaceKey() == candidate.workspaceKey() }
                    }
                }
                val secondaryDockNodes = composeRule
                    .onAllNodesWithTag("launcher-home-dock", useUnmergedTree = true)
                    .fetchSemanticsNodes()
                check(secondaryDockNodes.isNotEmpty()) {
                    "Persistent Dock semantics disappeared on secondary Home while placement and " +
                        "LauncherApps inventory still contain ${candidate.workspaceKey()}."
                }
                waitForDisplayedTag("launcher-home-dock")
                val secondaryDockBounds = composeRule
                    .onNodeWithTag("launcher-home-dock", useUnmergedTree = true)
                    .fetchSemanticsNode()
                    .boundsInRoot
                check(secondaryIndicatorBounds.bottom <= secondaryDockBounds.top) {
                    "Secondary Home page indicator must stay above the persistent Dock."
                }

                composeRule
                    .onNodeWithTag(
                        "launcher-home-page-" + secondaryPageId,
                        useUnmergedTree = true,
                    )
                    .performTouchInput {
                        down(center)
                        advanceEventTime(700)
                        up()
                    }
                composeRule.waitUntil(timeoutMillis = 15_000) {
                    composeRule
                        .onAllNodesWithText("Edit Home", useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
                waitForDisplayedText("Edit Home")
                composeRule.onNodeWithText("Done", useUnmergedTree = true).performClick()
                composeRule.waitUntil(timeoutMillis = 10_000) {
                    composeRule
                        .onAllNodesWithTag(
                            "launcher-home-page-" + secondaryPageId,
                            useUnmergedTree = true,
                        )
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }

                composeRule
                    .onNodeWithTag("launcher-home-unified-pager", useUnmergedTree = true)
                    .performTouchInput {
                        swipeRight(
                            startX = left + 24f,
                            endX = right - 24f,
                            durationMillis = 420,
                        )
                    }

                waitForSelectedHomePage(pageNumber = 1)
                waitForDisplayedLabel(candidate.label.toString())
                waitForDisplayedTag("launcher-home-page-indicator")

                preferencesRepository.setGestureAction(
                    LauncherHomeGesture.SWIPE_RIGHT,
                    searchAction,
                ).join()
                withTimeout(5_000) {
                    preferencesRepository.experiencePreferences.first {
                        it.swipeRightAction == searchAction
                    }
                }

                composeRule
                    .onNodeWithTag("launcher-home-unified-pager", useUnmergedTree = true)
                    .performTouchInput {
                        swipeRight(
                            startX = left + 24f,
                            endX = right - 24f,
                            durationMillis = 420,
                        )
                    }

                composeRule.waitUntil(timeoutMillis = 10_000) {
                    composeRule
                        .onAllNodesWithTag(
                            "launcher-universal-search-field",
                            useUnmergedTree = true,
                        )
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
                composeRule
                    .onNodeWithTag(
                        "launcher-universal-search-field",
                        useUnmergedTree = true,
                    )
                    .assertIsDisplayed()

                runShellCommand("input keyevent KEYCODE_HOME")
                waitForSelectedHomePage(pageNumber = 1)
                waitForDisplayedLabel(candidate.label.toString())
                waitForDisplayedTag("launcher-home-page-indicator")
                waitForDisplayedTag("launcher-home-empty-space-actions")
                composeRule.waitForIdle()

                composeRule
                    .onNodeWithTag(
                        "launcher-home-empty-space-actions",
                        useUnmergedTree = true,
                    )
                    .performTouchInput {
                        // The primary page intentionally contains an app. Long-press a lower-right
                        // empty grid region rather than the node center so child app content cannot
                        // consume the gesture that opens Edit Home.
                        val emptyPoint = center.copy(
                            x = right - 32f,
                            y = bottom - 32f,
                        )
                        down(emptyPoint)
                        advanceEventTime(700)
                        up()
                    }

                composeRule.waitUntil(timeoutMillis = 15_000) {
                    composeRule
                        .onAllNodesWithTag(
                            "launcher-home-editor-page-overview",
                            useUnmergedTree = true,
                        )
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
                check(
                    composeRule
                        .onAllNodesWithText("Delete", useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .isNotEmpty(),
                ) { "Empty secondary pages should expose direct guarded deletion in Edit Home." }
                check(
                    composeRule
                        .onAllNodesWithTag(
                            "launcher-home-editor-actions",
                            useUnmergedTree = true,
                        )
                        .fetchSemanticsNodes()
                        .isNotEmpty(),
                ) { "Edit Home actions must remain visible below the page overview." }

                val pageIdsBeforeDirectAdd = withTimeout(10_000) {
                    runtime!!.observeHomePages().first { state ->
                        state is WorkspacePagedHomeState.Ready
                    }
                }.let { state ->
                    (state as WorkspacePagedHomeState.Ready).pages.map { it.pageId }.toSet()
                }

                composeRule
                    .onNodeWithTag(
                        "launcher-home-editor-page-carousel",
                        useUnmergedTree = true,
                    )
                    .performTouchInput {
                        swipeLeft(
                            startX = right - 24f,
                            endX = left + 24f,
                            durationMillis = 420,
                        )
                    }

                waitForDisplayedTag("launcher-home-editor-fullscreen")

                repeat((pageIdsBeforeDirectAdd.size - 1).coerceAtLeast(1)) {
                    composeRule
                        .onNodeWithTag(
                            "launcher-home-editor-page-carousel",
                            useUnmergedTree = true,
                        )
                        .performTouchInput {
                            swipeLeft(
                                startX = right - 24f,
                                endX = left + 24f,
                                durationMillis = 420,
                            )
                        }
                }

                composeRule.waitUntil(timeoutMillis = 10_000) {
                    composeRule
                        .onAllNodesWithTag(
                            "launcher-home-editor-add-page",
                            useUnmergedTree = true,
                        )
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
                waitForDisplayedTag("launcher-home-editor-add-page")
                composeRule
                    .onNodeWithTag("launcher-home-editor-add-page", useUnmergedTree = true)
                    .performClick()

                val readyAfterDirectAdd = withTimeout(10_000) {
                    runtime!!.observeHomePages().first { state ->
                        state is WorkspacePagedHomeState.Ready &&
                            state.pages.map { it.pageId }.toSet().size > pageIdsBeforeDirectAdd.size
                    }
                } as WorkspacePagedHomeState.Ready
                directlyAddedPageId = readyAfterDirectAdd.pages
                    .first { it.pageId !in pageIdsBeforeDirectAdd }
                    .pageId

                // Adding a page changes the pager's page count, which intentionally returns the
                // editor carousel to its selected initial page. Navigate to the appended page
                // before asserting its preview instead of assuming an offscreen HorizontalPager
                // page remains composed.
                repeat((readyAfterDirectAdd.pages.size - 1).coerceAtLeast(0)) {
                    composeRule
                        .onNodeWithTag(
                            "launcher-home-editor-page-carousel",
                            useUnmergedTree = true,
                        )
                        .performTouchInput {
                            swipeLeft(
                                startX = right - 24f,
                                endX = left + 24f,
                                durationMillis = 420,
                            )
                        }
                }
                waitForDisplayedTag("launcher-home-editor-page-" + directlyAddedPageId)
                waitForDisplayedTag("launcher-home-editor-fullscreen")
                waitForDisplayedTag("launcher-home-editor-actions")
                Unit
            } finally {
                resetHomeBeforeScenarioClose()
                scenario.close()
            }
        } finally {
            preferencesRepository.setGestureAction(
                LauncherHomeGesture.SWIPE_RIGHT,
                previousSwipeRight,
            ).join()
            directlyAddedPageId?.let { runtime?.deleteEmptyHomePage(it) }
            runtime?.deleteEmptyHomePage(secondaryPageId)
            if (!alreadyDefaultHome) {
                runShellCommand(
                    "cmd role remove-role-holder ${RoleManager.ROLE_HOME} ${context.packageName}"
                )
            }
        }
    }

    @Test
    fun secondaryHomeRendersMovedBuiltInWidget() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val roleManager = context.getSystemService(RoleManager::class.java)
        val alreadyDefaultHome =
            roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
                roleManager.isRoleHeld(RoleManager.ROLE_HOME)
        if (!alreadyDefaultHome) {
            runShellCommand(
                "cmd role add-role-holder ${RoleManager.ROLE_HOME} ${context.packageName}"
            )
            withTimeout(10_000) {
                while (!roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                    delay(100)
                }
            }
        }

        val secondaryPageId = "home:test:secondary-widget"
        val widgetItemId = "widget:builtin:secondary-render"
        val repository = WorkspaceRepository(context)
        var runtime: WorkspaceProductionRuntimeCoordinator? = null

        try {
            val apps = withTimeout(10_000) {
                LauncherAppsRepository(context).apps.first { candidates ->
                    candidates.any { it.componentName.packageName != context.packageName }
                }
            }
            val candidate = apps.first { it.componentName.packageName != context.packageName }
            repository.ensureDefaults(
                favoriteKeys = listOf(candidate.workspaceKey()),
                dockKeys = emptyList(),
            )

            val scenario = ActivityScenario.launch(MainActivity::class.java)
            try {
                withTimeout(15_000) {
                    repository.state.first { it.authority == WorkspaceAuthority.ROOM }
                }
                val preferences = LauncherPreferencesRepository(context).preferences.first()
                runtime = WorkspaceProductionRuntimeCoordinator(
                    authorityRepository = repository,
                    workspaceDaoProvider = {
                        LauncherDatabaseProvider.get(context).workspaceDao()
                    },
                )

                // Keep direct Room test mutations behind the same settled startup boundary used by
                // the paging acceptance test. Activity launch can still be finishing authoritative
                // reconciliation after WorkspaceRepository has reported ROOM authority.
                composeRule.waitUntil(timeoutMillis = 15_000) {
                    composeRule
                        .onAllNodesWithTag("launcher-home-swipe-surface", useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
                composeRule.waitForIdle()
                withTimeout(10_000) {
                    runtime!!.observeHomePages().first { state ->
                        state is WorkspacePagedHomeState.Ready
                    }
                }

                runtime?.removeWidget(widgetItemId)
                runtime?.deleteEmptyHomePage(secondaryPageId)
                val created = runtime?.createHomePage(secondaryPageId)
                check(
                    created is WorkspacePagedRoomMutationResult.CreatedPage ||
                        created is WorkspacePagedRoomMutationResult.PageAlreadyExists
                ) { "Expected a usable secondary Home page; create result was $created." }

                check(
                    runtime?.addBuiltInWidget(
                        itemId = widgetItemId,
                        typeId = WorkspaceWidgetCatalog.BATTERY,
                        columns = preferences.homeColumns,
                        rows = preferences.homeRows,
                    ) is WorkspaceWidgetMutationResult.Added,
                )
                check(
                    runtime?.moveWidgetToPage(
                        itemId = widgetItemId,
                        targetPageId = secondaryPageId,
                        columns = preferences.homeColumns,
                        rows = preferences.homeRows,
                        targetCellX = 0,
                        targetCellY = 0,
                    ) is WorkspaceWidgetMutationResult.MovedToPage,
                )

                composeRule.waitUntil(timeoutMillis = 15_000) {
                    composeRule
                        .onAllNodesWithTag(
                            "launcher-home-page-indicator",
                            useUnmergedTree = true,
                        )
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
                composeRule
                    .onNodeWithTag("launcher-home-swipe-surface", useUnmergedTree = true)
                    .performTouchInput {
                        swipeLeft(
                            startX = right - 24f,
                            endX = left + 24f,
                            durationMillis = 420,
                        )
                    }

                composeRule.waitUntil(timeoutMillis = 15_000) {
                    composeRule
                        .onAllNodesWithTag(
                            "launcher-home-widget-" + widgetItemId,
                            useUnmergedTree = true,
                        )
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
                composeRule
                    .onNodeWithTag(
                        "launcher-home-widget-" + widgetItemId,
                        useUnmergedTree = true,
                    )
                    .assertIsDisplayed()
                composeRule
                    .onNodeWithTag(
                        "launcher-home-page-" + secondaryPageId,
                        useUnmergedTree = true,
                    )
                    .assertIsDisplayed()
                Unit
            } finally {
                // This test intentionally selects a temporary secondary page. Return through the
                // real HOME path before deleting that page so the selected page cannot leak into
                // later ActivityScenario tests as a now-stale page selection.
                //
                // HOME may either reset the currently observed root or replace the scenario-owned
                // LAUNCHER Activity with a fresh HOME Activity. In the replacement path the Compose
                // test rule legitimately loses the old hierarchy, while the fresh Activity starts
                // from primary Home. Do not require post-HOME page-indicator semantics from the
                // scenario-owned hierarchy during teardown.
                resetHomeBeforeScenarioClose()
                scenario.close()
            }
        } finally {
            runtime?.removeWidget(widgetItemId)
            runtime?.deleteEmptyHomePage(secondaryPageId)
            if (!alreadyDefaultHome) {
                runShellCommand(
                    "cmd role remove-role-holder ${RoleManager.ROLE_HOME} ${context.packageName}"
                )
            }
        }
    }

    @Test
    fun swipeUpOpensDrawerAndSwipeDownDismissesWithoutHeaderActions() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val roleManager = context.getSystemService(RoleManager::class.java)
        val alreadyDefaultHome =
            roleManager.isRoleAvailable(RoleManager.ROLE_HOME) && roleManager.isRoleHeld(RoleManager.ROLE_HOME)
        val preferencesRepository = LauncherPreferencesRepository(context)
        val previousSwipeUp = preferencesRepository.experiencePreferences.first().swipeUpAction
        val appsAction = LauncherGestureAction.builtIn(LauncherGestureActionType.APPS)

        if (!alreadyDefaultHome) {
            runShellCommand(
                "cmd role add-role-holder ${RoleManager.ROLE_HOME} ${context.packageName}"
            )
            withTimeout(10_000) {
                while (!roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                    delay(100)
                }
            }
        }

        try {
            preferencesRepository.setGestureAction(
                LauncherHomeGesture.SWIPE_UP,
                appsAction,
            ).join()

            val apps = withTimeout(10_000) {
                LauncherAppsRepository(context).apps.first { candidates ->
                    candidates.any { it.componentName.packageName != context.packageName }
                }
            }
            val candidate = apps.first { it.componentName.packageName != context.packageName }
            val candidateKey = candidate.workspaceKey()
            val repository = WorkspaceRepository(context)
            repository.ensureDefaults(
                favoriteKeys = listOf(candidateKey),
                dockKeys = emptyList(),
            )

            val scenario = ActivityScenario.launch(MainActivity::class.java)
            try {
                withTimeout(15_000) {
                    repository.state.first { it.authority == WorkspaceAuthority.ROOM }
                }

                val runtime = WorkspaceProductionRuntimeCoordinator(
                    authorityRepository = repository,
                    workspaceDaoProvider = {
                        LauncherDatabaseProvider.get(context).workspaceDao()
                    },
                )

                // ROOM authority can become visible before the launched Home finishes startup-owned
                // reconciliation. Wait for the real Home surface plus both authoritative Room
                // projections before performing this test-owned setup mutation; otherwise a healthy
                // guarded write can legitimately lose a snapshot race and contaminate later tests.
                composeRule.waitUntil(timeoutMillis = 15_000) {
                    composeRule
                        .onAllNodesWithTag(
                            "launcher-home-swipe-up-apps",
                            useUnmergedTree = true,
                        )
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
                composeRule.waitForIdle()
                withTimeout(10_000) {
                    runtime.observeHomePages().first { state ->
                        state is WorkspacePagedHomeState.Ready
                    }
                }
                val placement = withTimeout(10_000) {
                    runtime.observePlacement().first { state ->
                        state is WorkspaceAuthoritativePlacementState.Ready
                    }
                } as WorkspaceAuthoritativePlacementState.Ready
                if (candidateKey !in placement.snapshot.favoriteKeys) {
                    val preferences = LauncherPreferencesRepository(context).preferences.first()
                    val write = runtime.toggleFavorite(
                        key = candidateKey,
                        homeColumns = preferences.homeColumns,
                        homeRows = preferences.homeRows,
                    )
                    check(write is WorkspaceAuthoritativeWriteResult.Written) {
                        "Expected authoritative Home setup placement; result was $write."
                    }
                    withTimeout(10_000) {
                        runtime.observePlacement().first { state ->
                            state is WorkspaceAuthoritativePlacementState.Ready &&
                                candidateKey in state.snapshot.favoriteKeys
                        }
                    }
                }

                waitForDisplayedLabel(candidate.label.toString())
                composeRule.waitUntil(timeoutMillis = 15_000) {
                    composeRule
                        .onAllNodesWithTag(
                            "launcher-home-swipe-up-apps",
                            useUnmergedTree = true,
                        )
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }

                // The Home gesture surface intentionally leaves composition when Apps opens.
                // Inject at the Android input layer so Compose's touch injector is not attached to a
                // node that disappears mid-gesture. Use empty right-side Home space, away from the
                // seeded app tile and the bottom system-gesture edge.
                val gestureBounds = composeRule
                    .onNodeWithTag(
                        "launcher-home-swipe-up-apps",
                        useUnmergedTree = true,
                    )
                    .fetchSemanticsNode()
                    .boundsInRoot
                val swipeX = (gestureBounds.right - 32f).toInt()
                injectTouchSwipe(
                    startX = swipeX,
                    startY = (gestureBounds.bottom * 0.72f).toInt(),
                    endX = swipeX,
                    endY = (gestureBounds.top + gestureBounds.height * 0.28f).toInt(),
                    durationMillis = 360L,
                )

                composeRule.waitUntil(timeoutMillis = 10_000) {
                    composeRule.onAllNodesWithTag("launcher-app-drawer", useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
                composeRule
                    .onNodeWithTag("launcher-app-drawer", useUnmergedTree = true)
                    .assertIsDisplayed()
                check(
                    composeRule.onAllNodesWithText("Search apps", useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .isEmpty(),
                )
                check(
                    composeRule.onAllNodesWithText("⚙", useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .isEmpty(),
                )
                check(
                    composeRule.onAllNodesWithText("⌄", useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .isEmpty(),
                )

                // The Drawer intentionally leaves composition as soon as its downward
                // dismissal threshold is crossed. Inject this gesture at the Android input layer
                // rather than keeping Compose's touch injector attached to a node that removes
                // itself mid-gesture.
                val drawerBounds = composeRule
                    .onNodeWithTag("launcher-app-drawer", useUnmergedTree = true)
                    .fetchSemanticsNode()
                    .boundsInRoot
                injectTouchSwipe(
                    startX = (drawerBounds.right - 32f).toInt(),
                    startY = (drawerBounds.top + drawerBounds.height * 0.24f).toInt(),
                    endX = (drawerBounds.right - 32f).toInt(),
                    endY = (drawerBounds.bottom * 0.84f).toInt(),
                    durationMillis = 360L,
                )

                composeRule.waitUntil(timeoutMillis = 10_000) {
                    composeRule
                        .onAllNodesWithTag("launcher-app-drawer", useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .isEmpty()
                }
                waitForDisplayedLabel(candidate.label.toString())
                Unit
            } finally {
                scenario.close()
            }
        } finally {
            preferencesRepository.setGestureAction(
                LauncherHomeGesture.SWIPE_UP,
                previousSwipeUp,
            ).join()
            if (!alreadyDefaultHome) {
                runShellCommand(
                    "cmd role remove-role-holder ${RoleManager.ROLE_HOME} ${context.packageName}"
                )
            }
        }
    }

    @Test
    fun configuredSwipeDownStartingOnWorkspaceAppContentOpensUniversalSearch() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val roleManager = context.getSystemService(RoleManager::class.java)
        val alreadyDefaultHome =
            roleManager.isRoleAvailable(RoleManager.ROLE_HOME) && roleManager.isRoleHeld(RoleManager.ROLE_HOME)
        val preferencesRepository = LauncherPreferencesRepository(context)
        val previousSwipeDown = preferencesRepository.experiencePreferences.first().swipeDownAction
        val searchAction =
            LauncherGestureAction.builtIn(LauncherGestureActionType.UNIVERSAL_SEARCH)

        if (!alreadyDefaultHome) {
            runShellCommand(
                "cmd role add-role-holder ${RoleManager.ROLE_HOME} ${context.packageName}"
            )
            withTimeout(10_000) {
                while (!roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                    delay(100)
                }
            }
        }

        try {
            preferencesRepository.setGestureAction(
                LauncherHomeGesture.SWIPE_DOWN,
                searchAction,
            ).join()

            val apps = withTimeout(10_000) {
                LauncherAppsRepository(context).apps.first { candidates ->
                    candidates.any { it.componentName.packageName != context.packageName }
                }
            }
            val candidate = apps.first { it.componentName.packageName != context.packageName }
            val candidateKey = candidate.workspaceKey()
            val repository = WorkspaceRepository(context)
            repository.ensureDefaults(
                favoriteKeys = listOf(candidateKey),
                dockKeys = emptyList(),
            )

            val scenario = ActivityScenario.launch(MainActivity::class.java)
            try {
                withTimeout(15_000) {
                    repository.state.first { it.authority == WorkspaceAuthority.ROOM }
                }
                waitForDisplayedLabel(candidate.label.toString())

                composeRule
                    .onNodeWithText(candidate.label.toString(), useUnmergedTree = true)
                    .performTouchInput {
                        swipeDown(
                            startY = top + 1f,
                            endY = bottom + 320f,
                            durationMillis = 400,
                        )
                    }

                // Swipe-down Search enters a compact, keyboard-focused app-discovery panel.
                // Frequent/Recent remain local-only usage projections; New / Updated is derived
                // from observable Android package metadata. Query results still appear only after
                // the user types.
                composeRule.waitUntil(timeoutMillis = 10_000) {
                    composeRule
                        .onAllNodesWithTag("launcher-universal-search-field", useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
                composeRule
                    .onNodeWithTag(
                        "launcher-universal-search-field",
                        useUnmergedTree = true,
                    )
                    .assertIsDisplayed()
                assertEquals(
                    1,
                    composeRule
                        .onAllNodesWithTag("launcher-universal-search-field", useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .size,
                )
                composeRule
                    .onNodeWithText(
                        "Search apps",
                        useUnmergedTree = true,
                    )
                    .assertIsDisplayed()
                composeRule
                    .onNodeWithTag(
                        "launcher-universal-search-suggestions",
                        useUnmergedTree = true,
                    )
                    .assertIsDisplayed()
                composeRule
                    .onNodeWithTag(
                        "launcher-search-suggestion-tab-frequent",
                        useUnmergedTree = true,
                    )
                    .assertIsDisplayed()
                    .assertIsSelected()
                composeRule
                    .onNodeWithTag(
                        "launcher-search-suggestion-tab-recent",
                        useUnmergedTree = true,
                    )
                    .assertIsDisplayed()
                    .assertIsNotSelected()
                    .performClick()
                composeRule
                    .onNodeWithTag(
                        "launcher-search-suggestion-tab-recent",
                        useUnmergedTree = true,
                    )
                    .assertIsSelected()
                composeRule
                    .onNodeWithTag(
                        "launcher-search-suggestion-tab-new_updated",
                        useUnmergedTree = true,
                    )
                    .assertIsDisplayed()
                    .assertIsNotSelected()
                    .performClick()
                composeRule
                    .onNodeWithTag(
                        "launcher-search-suggestion-tab-new_updated",
                        useUnmergedTree = true,
                    )
                    .assertIsSelected()
                assertEquals(
                    0,
                    composeRule
                        .onAllNodesWithText("Universal Search", useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .size,
                )
                assertEquals(
                    0,
                    composeRule
                        .onAllNodesWithText(
                            "Search privately across enabled sources",
                            useUnmergedTree = true,
                        )
                        .fetchSemanticsNodes()
                        .size,
                )
                assertEquals(
                    0,
                    composeRule
                        .onAllNodesWithTag("launcher-glaze-search-panel", useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .size,
                )
                composeRule
                    .onNodeWithTag(
                        "launcher-universal-search-settings",
                        useUnmergedTree = true,
                    )
                    .assertIsDisplayed()
                    .assertHasClickAction()
                    .performClick()
                composeRule.waitUntil(timeoutMillis = 10_000) {
                    composeRule
                        .onAllNodesWithTag("launcher-search-source-manager", useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
                composeRule
                    .onNodeWithText("Back", useUnmergedTree = true)
                    .performClick()
                composeRule.waitUntil(timeoutMillis = 10_000) {
                    composeRule
                        .onAllNodesWithTag("launcher-universal-search-field", useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
                composeRule
                    .onNodeWithTag(
                        "launcher-universal-search-field",
                        useUnmergedTree = true,
                    )
                    .performTextInput("theme")
                composeRule.waitUntil(timeoutMillis = 10_000) {
                    composeRule
                        .onAllNodesWithTag("launcher-glaze-search-panel", useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }

                composeRule.waitUntil(timeoutMillis = 10_000) {
                    composeRule.onAllNodesWithText("Theme Manager", useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
                composeRule
                    .onNodeWithText("Theme Manager", useUnmergedTree = true)
                    .performClick()

                composeRule.waitUntil(timeoutMillis = 10_000) {
                    composeRule
                        .onAllNodesWithText(
                            "Preview and choose the current Launcher appearance",
                            useUnmergedTree = true,
                        )
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
                composeRule
                    .onNodeWithText(
                        "Preview and choose the current Launcher appearance",
                        useUnmergedTree = true,
                    )
                    .assertIsDisplayed()
                Unit
            } finally {
                scenario.close()
            }
        } finally {
            preferencesRepository.setGestureAction(
                LauncherHomeGesture.SWIPE_DOWN,
                previousSwipeDown,
            ).join()
            if (!alreadyDefaultHome) {
                runShellCommand(
                    "cmd role remove-role-holder ${RoleManager.ROLE_HOME} ${context.packageName}"
                )
            }
        }
    }

    @Test
    fun longPressDragMovesPrimaryHomeAppIntoEmptyCellAndPersists() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val roleManager = context.getSystemService(RoleManager::class.java)
        val alreadyDefaultHome =
            roleManager.isRoleAvailable(RoleManager.ROLE_HOME) && roleManager.isRoleHeld(RoleManager.ROLE_HOME)

        if (!alreadyDefaultHome) {
            runShellCommand(
                "cmd role add-role-holder ${RoleManager.ROLE_HOME} ${context.packageName}"
            )
            withTimeout(10_000) {
                while (!roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                    delay(100)
                }
            }
        }

        try {
            val apps = withTimeout(10_000) {
                LauncherAppsRepository(context).apps.first { candidates ->
                    candidates
                        .filter { it.componentName.packageName != context.packageName }
                        .distinctBy { it.label.toString() }
                        .size >= 2
                }
            }
            val candidates = apps
                .filter { it.componentName.packageName != context.packageName }
                .distinctBy { it.label.toString() }
            val firstApp = candidates[0]
            val secondApp = candidates[1]
            val firstKey = firstApp.workspaceKey()
            val secondKey = secondApp.workspaceKey()
            val repository = WorkspaceRepository(context)
            repository.ensureDefaults(
                favoriteKeys = listOf(firstKey, secondKey),
                dockKeys = emptyList(),
            )

            val scenario = ActivityScenario.launch(MainActivity::class.java)
            try {
                withTimeout(15_000) {
                    repository.state.first { it.authority == WorkspaceAuthority.ROOM }
                }

                val dao = LauncherDatabaseProvider.get(context).workspaceDao()
                val preferences = LauncherPreferencesRepository(context).preferences.first()
                val roomPlacement = WorkspaceRoomPlacementRepository(
                    authorityRepository = repository,
                    workspaceDaoProvider = { dao },
                )
                val baseline = roomPlacement.replace(
                    favoriteKeys = listOf(firstKey, secondKey),
                    dockKeys = emptyList(),
                    homeGrid = WorkspaceGridPlacement.Grid(
                        columns = preferences.homeColumns,
                        rows = preferences.homeRows,
                    ),
                )
                check(baseline is WorkspaceRoomWriteResult.Written)

                val runtime = WorkspaceProductionRuntimeCoordinator(
                    authorityRepository = repository,
                    workspaceDaoProvider = {
                        LauncherDatabaseProvider.get(context).workspaceDao()
                    },
                )
                val spatialReady = runtime.ensurePrimaryHomeSpatialGrid(
                    columns = preferences.homeColumns,
                    rows = preferences.homeRows,
                )
                check(spatialReady is WorkspacePrimaryHomeSpatialResult.Ready)

                waitForDisplayedLabel(firstApp.label.toString())
                waitForDisplayedLabel(secondApp.label.toString())

                val spatialItems = dao
                    .readItems(listOf(WorkspaceLegacyImportMapper.HOME_PAGE_ID))
                val sourceItem = checkNotNull(spatialItems.singleOrNull { it.appKey == firstKey })
                val sourceX = checkNotNull(sourceItem.cellX)
                val sourceY = checkNotNull(sourceItem.cellY)
                val occupied = buildSet {
                    spatialItems.forEach { item ->
                        val originX = item.cellX ?: return@forEach
                        val originY = item.cellY ?: return@forEach
                        val spanX = item.spanX.coerceAtLeast(1)
                        val spanY = item.spanY.coerceAtLeast(1)
                        for (cellY in originY until originY + spanY) {
                            for (cellX in originX until originX + spanX) {
                                add(cellX to cellY)
                            }
                        }
                    }
                }
                val target = buildList {
                    for (cellY in 0 until preferences.homeRows) {
                        for (cellX in 0 until preferences.homeColumns) {
                            add(cellX to cellY)
                        }
                    }
                }.firstOrNull { it !in occupied }
                checkNotNull(target) { "Primary Home runtime test requires at least one empty cell." }
                val targetX = target.first
                val targetY = target.second
                val sourceTag = "launcher-home-cell-$sourceX-$sourceY"
                val sourceAppTag = "launcher-home-app-$firstKey"
                val targetTag = "launcher-home-cell-$targetX-$targetY"

                composeRule.waitUntil(timeoutMillis = 15_000) {
                    composeRule
                        .onAllNodesWithTag(sourceAppTag, useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .isNotEmpty() &&
                        composeRule
                            .onAllNodesWithTag(targetTag, useUnmergedTree = true)
                            .fetchSemanticsNodes()
                            .isNotEmpty()
                }

                val sourceBounds = composeRule
                    .onNodeWithTag(sourceAppTag, useUnmergedTree = true)
                    .fetchSemanticsNode()
                    .boundsInRoot
                val targetBounds = composeRule
                    .onNodeWithTag(targetTag, useUnmergedTree = true)
                    .fetchSemanticsNode()
                    .boundsInRoot
                val delta = targetBounds.center - sourceBounds.center

                composeRule
                    .onNodeWithTag(sourceAppTag, useUnmergedTree = true)
                    .performTouchInput {
                        val dragDelta = targetBounds.center - sourceBounds.center
                        down(center)
                        advanceEventTime(
                            ViewConfiguration.getLongPressTimeout().toLong() + 180L,
                        )
                        repeat(12) { index ->
                            val fraction = (index + 1).toFloat() / 12f
                            moveTo(center + dragDelta * fraction)
                            advanceEventTime(30L)
                        }
                        up()
                    }

                withTimeout(15_000) {
                    while (true) {
                        val moved = dao
                            .readItems(listOf(WorkspaceLegacyImportMapper.HOME_PAGE_ID))
                            .singleOrNull { it.appKey == firstKey }
                        if (moved?.cellX == targetX && moved.cellY == targetY) break
                        delay(100)
                    }
                }

                val afterMove = dao
                    .readItems(listOf(WorkspaceLegacyImportMapper.HOME_PAGE_ID))
                    .sortedBy { it.rank }
                val firstStored = checkNotNull(afterMove.singleOrNull { it.appKey == firstKey })
                val secondStored = checkNotNull(afterMove.singleOrNull { it.appKey == secondKey })
                assertEquals(targetX, firstStored.cellX)
                assertEquals(targetY, firstStored.cellY)
                check(firstStored.cellX != secondStored.cellX || firstStored.cellY != secondStored.cellY)

                scenario.recreate()
                withTimeout(15_000) {
                    repository.state.first { it.authority == WorkspaceAuthority.ROOM }
                }
                waitForDisplayedLabel(firstApp.label.toString())

                val afterRecreate = dao
                    .readItems(listOf(WorkspaceLegacyImportMapper.HOME_PAGE_ID))
                    .single { it.appKey == firstKey }
                assertEquals(targetX, afterRecreate.cellX)
                assertEquals(targetY, afterRecreate.cellY)
            } finally {
                scenario.close()
            }
        } finally {
            if (!alreadyDefaultHome) {
                runShellCommand(
                    "cmd role remove-role-holder ${RoleManager.ROLE_HOME} ${context.packageName}"
                )
            }
        }
    }

    @Test
    fun homeButtonFromDrawerReturnsPrimaryHomeSurface() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val roleManager = context.getSystemService(RoleManager::class.java)
        val alreadyDefaultHome =
            roleManager.isRoleAvailable(RoleManager.ROLE_HOME) && roleManager.isRoleHeld(RoleManager.ROLE_HOME)

        if (!alreadyDefaultHome) {
            runShellCommand(
                "cmd role add-role-holder ${RoleManager.ROLE_HOME} ${context.packageName}"
            )
            withTimeout(10_000) {
                while (!roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                    delay(100)
                }
            }
        }

        try {
            val apps = withTimeout(10_000) {
                LauncherAppsRepository(context).apps.first { candidates ->
                    candidates.any { it.componentName.packageName != context.packageName }
                }
            }
            val candidate = apps.first { it.componentName.packageName != context.packageName }
            val candidateKey = candidate.workspaceKey()
            val repository = WorkspaceRepository(context)
            repository.ensureDefaults(
                favoriteKeys = listOf(candidateKey),
                dockKeys = emptyList(),
            )

            val scenario = ActivityScenario.launch(MainActivity::class.java)
            try {
                withTimeout(15_000) {
                    repository.state.first { it.authority == WorkspaceAuthority.ROOM }
                }
                waitForDisplayedLabel(candidate.label.toString())

                composeRule
                    .onNodeWithText(candidate.label.toString(), useUnmergedTree = true)
                    .performTouchInput {
                        swipeUp(
                            startY = bottom - 1f,
                            endY = top - 320f,
                            durationMillis = 400,
                        )
                    }

                composeRule.waitUntil(timeoutMillis = 10_000) {
                    composeRule.onAllNodesWithTag("launcher-app-drawer", useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }

                runShellCommand("input keyevent KEYCODE_HOME")

                composeRule.waitUntil(timeoutMillis = 10_000) {
                    composeRule.onAllNodesWithTag("launcher-app-drawer", useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .isEmpty()
                }
                waitForDisplayedLabel(candidate.label.toString())
                Unit
            } finally {
                scenario.close()
            }
        } finally {
            if (!alreadyDefaultHome) {
                runShellCommand(
                    "cmd role remove-role-holder ${RoleManager.ROLE_HOME} ${context.packageName}"
                )
            }
        }
    }

    @Test
    fun emptyHomeLongPressAlwaysOpensHomeEditor() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val roleManager = context.getSystemService(RoleManager::class.java)
        val alreadyDefaultHome =
            roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
                roleManager.isRoleHeld(RoleManager.ROLE_HOME)
        val preferencesRepository = LauncherPreferencesRepository(context)
        val previousTapAndHold =
            preferencesRepository.experiencePreferences.first().tapAndHoldAction

        if (!alreadyDefaultHome) {
            runShellCommand(
                "cmd role add-role-holder ${RoleManager.ROLE_HOME} ${context.packageName}"
            )
            withTimeout(10_000) {
                while (!roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                    delay(100)
                }
            }
        }

        try {
            preferencesRepository.setGestureAction(
                LauncherHomeGesture.TAP_AND_HOLD,
                LauncherGestureAction.builtIn(LauncherGestureActionType.APPS),
            ).join()

            val scenario = ActivityScenario.launch(MainActivity::class.java)
            try {
                composeRule.waitUntil(timeoutMillis = 15_000) {
                    composeRule
                        .onAllNodesWithTag(
                            "launcher-home-empty-space-actions",
                            useUnmergedTree = true,
                        )
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }

                composeRule
                    .onNodeWithTag(
                        "launcher-home-empty-space-actions",
                        useUnmergedTree = true,
                    )
                    .performTouchInput {
                        down(center)
                        advanceEventTime(700)
                        up()
                    }

                composeRule.waitUntil(timeoutMillis = 15_000) {
                    composeRule
                        .onAllNodesWithText("Edit Home", useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
                composeRule
                    .onNodeWithText("Edit Home", useUnmergedTree = true)
                    .assertIsDisplayed()
                composeRule
                    .onNodeWithTag("launcher-home-editor-fullscreen", useUnmergedTree = true)
                    .assertIsDisplayed()
                composeRule
                    .onNodeWithText("Done", useUnmergedTree = true)
                    .assertIsDisplayed()
                check(
                    composeRule
                        .onAllNodesWithTag(
                            "launcher-home-editor-page-overview",
                            useUnmergedTree = true,
                        )
                        .fetchSemanticsNodes()
                        .isNotEmpty(),
                ) { "Long-press Edit Home should expose the visual page overview." }
                check(
                    composeRule
                        .onAllNodesWithTag(
                            "launcher-home-editor-actions",
                            useUnmergedTree = true,
                        )
                        .fetchSemanticsNodes()
                        .isNotEmpty(),
                ) { "Long-press Edit Home must keep the Launcher action toolbar visible." }
                check(
                    composeRule
                        .onAllNodesWithText("Settings", useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .isNotEmpty(),
                )
                check(
                    composeRule
                        .onAllNodesWithTag("launcher-app-drawer", useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .isEmpty(),
                )

                // Leave the full-screen editor through its supported UI path before closing the
                // ActivityScenario. Immediate Activity destruction while the Dialog composition is
                // still settling can race Compose SlotTable disposal and turn test teardown into a
                // process crash that is unrelated to the behavior under assertion.
                composeRule
                    .onNodeWithText("Done", useUnmergedTree = true)
                    .performClick()
                composeRule.waitUntil(timeoutMillis = 10_000) {
                    composeRule
                        .onAllNodesWithTag(
                            "launcher-home-editor-fullscreen",
                            useUnmergedTree = true,
                        )
                        .fetchSemanticsNodes()
                        .isEmpty()
                }
                composeRule.waitForIdle()
                Unit
            } finally {
                scenario.close()
            }
        } finally {
            preferencesRepository.setGestureAction(
                LauncherHomeGesture.TAP_AND_HOLD,
                previousTapAndHold,
            ).join()
            if (!alreadyDefaultHome) {
                runShellCommand(
                    "cmd role remove-role-holder ${RoleManager.ROLE_HOME} ${context.packageName}"
                )
            }
        }
    }

    @Test
    fun wallpaperPickerExposesSingleSelectionSemanticsWithoutApplyingWallpaper() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val roleManager = context.getSystemService(RoleManager::class.java)
        val alreadyDefaultHome =
            roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
                roleManager.isRoleHeld(RoleManager.ROLE_HOME)
        val preferencesRepository = LauncherPreferencesRepository(context)
        val previousTapAndHold =
            preferencesRepository.experiencePreferences.first().tapAndHoldAction

        if (!alreadyDefaultHome) {
            runShellCommand(
                "cmd role add-role-holder ${RoleManager.ROLE_HOME} ${context.packageName}"
            )
            withTimeout(10_000) {
                while (!roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                    delay(100)
                }
            }
        }

        try {
            preferencesRepository.setGestureAction(
                LauncherHomeGesture.TAP_AND_HOLD,
                LauncherGestureAction.builtIn(LauncherGestureActionType.APPS),
            ).join()

            val scenario = ActivityScenario.launch(MainActivity::class.java)
            try {
                composeRule.waitUntil(timeoutMillis = 15_000) {
                    composeRule
                        .onAllNodesWithTag(
                            "launcher-home-empty-space-actions",
                            useUnmergedTree = true,
                        )
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
                composeRule
                    .onNodeWithTag(
                        "launcher-home-empty-space-actions",
                        useUnmergedTree = true,
                    )
                    .performTouchInput {
                        down(center)
                        advanceEventTime(700)
                        up()
                    }

                composeRule.waitUntil(timeoutMillis = 15_000) {
                    composeRule
                        .onAllNodesWithText("Edit Home", useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
                composeRule.onNodeWithText("Wallpaper").performClick()

                composeRule.waitUntil(timeoutMillis = 10_000) {
                    composeRule
                        .onAllNodesWithText("Wallpapers", useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }

                composeRule
                    .onNodeWithTag(
                        "launcher-wallpaper-choice-AURORA",
                        useUnmergedTree = true,
                    )
                    .assertIsSelected()
                composeRule
                    .onNodeWithTag(
                        "launcher-wallpaper-choice-HORIZON",
                        useUnmergedTree = true,
                    )
                    .assertIsNotSelected()
                    .performScrollTo()
                    .performClick()
                composeRule.waitUntil(timeoutMillis = 10_000) {
                    runCatching {
                        composeRule
                            .onNodeWithTag(
                                "launcher-wallpaper-choice-HORIZON",
                                useUnmergedTree = true,
                            )
                            .assertIsSelected()
                        composeRule
                            .onNodeWithTag(
                                "launcher-wallpaper-choice-AURORA",
                                useUnmergedTree = true,
                            )
                            .assertIsNotSelected()
                    }.isSuccess
                }
                composeRule
                    .onNodeWithTag(
                        "launcher-wallpaper-choice-HORIZON",
                        useUnmergedTree = true,
                    )
                    .assertIsSelected()
                composeRule
                    .onNodeWithTag(
                        "launcher-wallpaper-choice-AURORA",
                        useUnmergedTree = true,
                    )
                    .assertIsNotSelected()

                composeRule.waitUntil(timeoutMillis = 10_000) {
                    composeRule
                        .onAllNodesWithText("Apply", useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
                Unit
            } finally {
                resetHomeBeforeScenarioClose()
                scenario.close()
            }
        } finally {
            preferencesRepository.setGestureAction(
                LauncherHomeGesture.TAP_AND_HOLD,
                previousTapAndHold,
            ).join()
            if (!alreadyDefaultHome) {
                runShellCommand(
                    "cmd role remove-role-holder ${RoleManager.ROLE_HOME} ${context.packageName}"
                )
            }
        }
    }

    @Test
    fun widgetPickerSearchExposesUnifiedAccessibleGalleryWithoutMutatingWorkspace() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val roleManager = context.getSystemService(RoleManager::class.java)
        val alreadyDefaultHome =
            roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
                roleManager.isRoleHeld(RoleManager.ROLE_HOME)
        val preferencesRepository = LauncherPreferencesRepository(context)
        val previousTapAndHold =
            preferencesRepository.experiencePreferences.first().tapAndHoldAction

        if (!alreadyDefaultHome) {
            runShellCommand(
                "cmd role add-role-holder ${RoleManager.ROLE_HOME} ${context.packageName}"
            )
            withTimeout(10_000) {
                while (!roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                    delay(100)
                }
            }
        }

        try {
            preferencesRepository.setGestureAction(
                LauncherHomeGesture.TAP_AND_HOLD,
                LauncherGestureAction.builtIn(LauncherGestureActionType.APPS),
            ).join()

            val scenario = ActivityScenario.launch(MainActivity::class.java)
            try {
                composeRule.waitUntil(timeoutMillis = 15_000) {
                    composeRule
                        .onAllNodesWithTag(
                            "launcher-home-empty-space-actions",
                            useUnmergedTree = true,
                        )
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
                composeRule
                    .onNodeWithTag(
                        "launcher-home-empty-space-actions",
                        useUnmergedTree = true,
                    )
                    .performTouchInput {
                        // This test does not own the persisted workspace. Target a lower-right
                        // empty grid region so an existing app tile cannot consume the long-press
                        // that is intended to open Edit Home.
                        val emptyPoint = center.copy(
                            x = right - 32f,
                            y = bottom - 32f,
                        )
                        down(emptyPoint)
                        advanceEventTime(700)
                        up()
                    }

                waitForDisplayedTag("launcher-home-editor-fullscreen")
                waitForDisplayedTag("launcher-home-editor-actions")
                waitForDisplayedTag("launcher-home-editor-action-widgets")
                composeRule
                    .onNodeWithTag(
                        "launcher-home-editor-action-widgets",
                        useUnmergedTree = true,
                    )
                    .assertHasClickAction()
                    .performClick()

                composeRule.waitUntil(timeoutMillis = 10_000) {
                    composeRule
                        .onAllNodesWithTag(
                            "launcher-widget-picker-sheet",
                            useUnmergedTree = true,
                        )
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
                composeRule
                    .onNodeWithTag("launcher-widget-search-field", useUnmergedTree = true)
                    .assertIsDisplayed()
                composeRule
                    .onNodeWithTag(
                        "launcher-widget-built-in-goreecloud.battery",
                        useUnmergedTree = true,
                    )
                    .assertHasClickAction()

                composeRule
                    .onNodeWithTag("launcher-widget-search-field", useUnmergedTree = true)
                    .performTextInput("calendar")

                composeRule.waitUntil(timeoutMillis = 10_000) {
                    composeRule
                        .onAllNodesWithTag(
                            "launcher-widget-built-in-goreecloud.month",
                            useUnmergedTree = true,
                        )
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
                composeRule
                    .onNodeWithTag(
                        "launcher-widget-built-in-goreecloud.month",
                        useUnmergedTree = true,
                    )
                    .assertHasClickAction()
                check(
                    composeRule
                        .onAllNodesWithTag(
                            "launcher-widget-built-in-goreecloud.battery",
                            useUnmergedTree = true,
                        )
                        .fetchSemanticsNodes()
                        .isEmpty(),
                )

                composeRule
                    .onNodeWithTag("launcher-widget-search-field", useUnmergedTree = true)
                    .performTextClearance()
                composeRule
                    .onNodeWithTag("launcher-widget-search-field", useUnmergedTree = true)
                    .performTextInput("weather")

                composeRule.waitUntil(timeoutMillis = 10_000) {
                    composeRule
                        .onAllNodesWithTag(
                            "launcher-widget-built-in-goreecloud.glance",
                            useUnmergedTree = true,
                        )
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
                composeRule
                    .onNodeWithTag(
                        "launcher-widget-built-in-goreecloud.glance",
                        useUnmergedTree = true,
                    )
                    .assertHasClickAction()
                check(
                    composeRule
                        .onAllNodesWithTag(
                            "launcher-widget-built-in-goreecloud.battery",
                            useUnmergedTree = true,
                        )
                        .fetchSemanticsNodes()
                        .isEmpty(),
                )
                Unit
            } finally {
                resetHomeBeforeScenarioClose()
                scenario.close()
            }
        } finally {
            preferencesRepository.setGestureAction(
                LauncherHomeGesture.TAP_AND_HOLD,
                previousTapAndHold,
            ).join()
            if (!alreadyDefaultHome) {
                runShellCommand(
                    "cmd role remove-role-holder ${RoleManager.ROLE_HOME} ${context.packageName}"
                )
            }
        }
    }

    @Test
    fun legacyLauncherSettingsGestureOpensHomeEditorInstead() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val roleManager = context.getSystemService(RoleManager::class.java)
        val alreadyDefaultHome =
            roleManager.isRoleAvailable(RoleManager.ROLE_HOME) && roleManager.isRoleHeld(RoleManager.ROLE_HOME)
        val preferencesRepository = LauncherPreferencesRepository(context)
        val previousSwipeUp = preferencesRepository.experiencePreferences.first().swipeUpAction
        val configuredAction =
            LauncherGestureAction.builtIn(LauncherGestureActionType.LAUNCHER_SETTINGS)
        val renderedGestureTag = "launcher-home-swipe-up-launcher_settings"

        if (!alreadyDefaultHome) {
            runShellCommand(
                "cmd role add-role-holder ${RoleManager.ROLE_HOME} ${context.packageName}"
            )
            withTimeout(10_000) {
                while (!roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                    delay(100)
                }
            }
        }

        try {
            preferencesRepository.setGestureAction(
                LauncherHomeGesture.SWIPE_UP,
                configuredAction,
            ).join()

            val scenario = ActivityScenario.launch(MainActivity::class.java)
            try {
                composeRule.waitUntil(timeoutMillis = 15_000) {
                    composeRule
                        .onAllNodesWithTag(
                            renderedGestureTag,
                            useUnmergedTree = true,
                        )
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }

                composeRule
                    .onNodeWithTag(
                        renderedGestureTag,
                        useUnmergedTree = true,
                    )
                    .performTouchInput {
                        swipeUp(
                            startY = bottom - 1f,
                            endY = top + 1f,
                            durationMillis = 400,
                        )
                    }

                composeRule.waitUntil(timeoutMillis = 15_000) {
                    composeRule
                        .onAllNodesWithText("Edit Home", useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
                composeRule
                    .onNodeWithText("Edit Home", useUnmergedTree = true)
                    .assertIsDisplayed()
                composeRule
                    .onNodeWithTag("launcher-home-editor-fullscreen", useUnmergedTree = true)
                    .assertIsDisplayed()
                composeRule
                    .onNodeWithText("Done", useUnmergedTree = true)
                    .assertIsDisplayed()
                check(
                    composeRule
                        .onAllNodesWithText("Settings", useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .isNotEmpty(),
                )
                check(
                    composeRule
                        .onAllNodesWithText(
                            "Home, apps, dock, search and Glaze",
                            useUnmergedTree = true,
                        )
                        .fetchSemanticsNodes()
                        .isEmpty(),
                )
                Unit
            } finally {
                resetHomeBeforeScenarioClose()
                scenario.close()
            }
        } finally {
            preferencesRepository.setGestureAction(
                LauncherHomeGesture.SWIPE_UP,
                previousSwipeUp,
            ).join()
            if (!alreadyDefaultHome) {
                runShellCommand(
                    "cmd role remove-role-holder ${RoleManager.ROLE_HOME} ${context.packageName}"
                )
            }
        }
    }

    private fun resetHomeBeforeScenarioClose() {
        runShellCommand("input keyevent KEYCODE_HOME")
        composeRule.waitUntil(timeoutMillis = 10_000) {
            // HOME may replace the ActivityScenario-launched LAUNCHER activity with a new HOME
            // intent instance. In that valid teardown path the test rule can briefly have no
            // Compose hierarchy at all; that already proves the scenario-owned overlays are gone.
            runCatching {
                composeRule
                    .onAllNodesWithTag(
                        "launcher-home-editor-fullscreen",
                        useUnmergedTree = true,
                    )
                    .fetchSemanticsNodes()
                    .isEmpty() &&
                    composeRule
                        .onAllNodesWithTag(
                            "launcher-widget-picker-sheet",
                            useUnmergedTree = true,
                        )
                        .fetchSemanticsNodes()
                        .isEmpty() &&
                    composeRule
                        .onAllNodesWithText("Wallpapers", useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .isEmpty()
            }.getOrDefault(true)
        }
        runCatching { composeRule.waitForIdle() }
    }

    private fun waitForSelectedHomePage(
        pageNumber: Int,
        timeoutMillis: Long = 10_000,
    ) {
        composeRule.waitUntil(timeoutMillis = timeoutMillis) {
            runCatching {
                val descriptions = composeRule
                    .onNodeWithTag("launcher-home-page-indicator", useUnmergedTree = true)
                    .fetchSemanticsNode()
                    .config[SemanticsProperties.ContentDescription]
                descriptions.any { description ->
                    description.startsWith("Page $pageNumber,") &&
                        description.endsWith(", selected")
                }
            }.getOrDefault(false)
        }
    }

    private fun waitForDisplayedLabel(label: String) {
        waitForDisplayedText(label)
    }

    private fun waitForDisplayedText(text: String, timeoutMillis: Long = 15_000) {
        composeRule.waitUntil(timeoutMillis = timeoutMillis) {
            runCatching {
                composeRule
                    .onNodeWithText(text, useUnmergedTree = true)
                    .assertIsDisplayed()
            }.isSuccess
        }
    }

    private fun waitForDisplayedTag(tag: String, timeoutMillis: Long = 15_000) {
        composeRule.waitUntil(timeoutMillis = timeoutMillis) {
            runCatching {
                composeRule
                    .onNodeWithTag(tag, useUnmergedTree = true)
                    .assertIsDisplayed()
            }.isSuccess
        }
    }

    private fun injectLongPressDrag(
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
    ) {
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val downTime = SystemClock.uptimeMillis()
        val holdMillis = ViewConfiguration.getLongPressTimeout().toLong() + 180L
        val moveDurationMillis = 360L
        val moveSteps = 12

        fun inject(
            action: Int,
            x: Float,
            y: Float,
            eventTime: Long,
        ) {
            val event = MotionEvent.obtain(
                downTime,
                eventTime,
                action,
                x,
                y,
                0,
            ).apply {
                source = InputDevice.SOURCE_TOUCHSCREEN
            }
            try {
                check(uiAutomation.injectInputEvent(event, true)) {
                    "Android drag input injection failed for action=$action at ($x, $y)."
                }
            } finally {
                event.recycle()
            }
        }

        inject(
            action = MotionEvent.ACTION_DOWN,
            x = startX.toFloat(),
            y = startY.toFloat(),
            eventTime = downTime,
        )
        SystemClock.sleep(holdMillis)

        repeat(moveSteps) { index ->
            val fraction = (index + 1).toFloat() / moveSteps.toFloat()
            val targetTime =
                downTime + holdMillis + (moveDurationMillis * fraction).toLong()
            val sleepMillis = (targetTime - SystemClock.uptimeMillis()).coerceAtLeast(0L)
            if (sleepMillis > 0L) SystemClock.sleep(sleepMillis)
            inject(
                action = MotionEvent.ACTION_MOVE,
                x = startX + (endX - startX) * fraction,
                y = startY + (endY - startY) * fraction,
                eventTime = SystemClock.uptimeMillis(),
            )
        }

        inject(
            action = MotionEvent.ACTION_UP,
            x = endX.toFloat(),
            y = endY.toFloat(),
            eventTime = SystemClock.uptimeMillis(),
        )
    }

    private fun injectTouchSwipe(
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        durationMillis: Long,
    ) {
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val downTime = SystemClock.uptimeMillis()
        val steps = 12

        fun inject(
            action: Int,
            x: Float,
            y: Float,
            eventTime: Long,
        ) {
            val event = MotionEvent.obtain(
                downTime,
                eventTime,
                action,
                x,
                y,
                0,
            ).apply {
                source = InputDevice.SOURCE_TOUCHSCREEN
            }
            try {
                check(uiAutomation.injectInputEvent(event, true)) {
                    "Android input injection failed for action=$action at ($x, $y)."
                }
            } finally {
                event.recycle()
            }
        }

        inject(
            action = MotionEvent.ACTION_DOWN,
            x = startX.toFloat(),
            y = startY.toFloat(),
            eventTime = downTime,
        )
        for (step in 1 until steps) {
            val fraction = step.toFloat() / steps.toFloat()
            val targetTime = downTime + (durationMillis * fraction).toLong()
            val sleepMillis = (targetTime - SystemClock.uptimeMillis()).coerceAtLeast(0L)
            if (sleepMillis > 0L) {
                SystemClock.sleep(sleepMillis)
            }
            inject(
                action = MotionEvent.ACTION_MOVE,
                x = startX + (endX - startX) * fraction,
                y = startY + (endY - startY) * fraction,
                eventTime = SystemClock.uptimeMillis(),
            )
        }
        val finalSleep = (downTime + durationMillis - SystemClock.uptimeMillis()).coerceAtLeast(0L)
        if (finalSleep > 0L) {
            SystemClock.sleep(finalSleep)
        }
        inject(
            action = MotionEvent.ACTION_UP,
            x = endX.toFloat(),
            y = endY.toFloat(),
            eventTime = SystemClock.uptimeMillis(),
        )
    }

    private fun runShellCommand(command: String) {
        val descriptor: ParcelFileDescriptor =
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        FileInputStream(descriptor.fileDescriptor).use { input ->
            input.readBytes()
        }
        descriptor.close()
    }


}
