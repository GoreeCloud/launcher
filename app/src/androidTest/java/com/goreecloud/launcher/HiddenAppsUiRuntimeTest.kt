package com.goreecloud.launcher

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.goreecloud.launcher.core.launcher.LauncherHomeAppMode
import com.goreecloud.launcher.core.launcher.LauncherPreferencesRepository
import com.goreecloud.launcher.core.workspace.WorkspaceAuthority
import com.goreecloud.launcher.core.workspace.WorkspaceRepository
import com.goreecloud.launcher.core.workspace.db.LauncherDatabaseProvider
import com.goreecloud.launcher.core.workspace.db.WorkspaceLegacyImportMapper
import com.goreecloud.launcher.core.workspace.db.WorkspacePagedHomeState
import com.goreecloud.launcher.core.workspace.db.WorkspaceProductionRuntimeCoordinator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HiddenAppsUiRuntimeTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private lateinit var preferencesRepository: LauncherPreferencesRepository
    private var previousHomeAppMode = LauncherHomeAppMode.NONE
    private var previousHiddenKeys: Set<String> = emptySet()
    private var previousLockedKeys: Set<String> = emptySet()

    @Before
    fun prepareEstablishedLauncher(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        preferencesRepository = LauncherPreferencesRepository(context)
        previousHomeAppMode = preferencesRepository.experiencePreferences.first().homeAppMode
        previousHiddenKeys = preferencesRepository.hiddenAppKeys.first()
        previousLockedKeys = preferencesRepository.lockedAppKeys.first()

        previousHiddenKeys.forEach { key ->
            preferencesRepository.setAppHidden(key, false).join()
        }
        previousLockedKeys.forEach { key ->
            preferencesRepository.setAppLocked(key, false).join()
        }
        preferencesRepository.setHomeAppMode(LauncherHomeAppMode.NONE).join()
        preferencesRepository.setHomeHintsDismissed(true).join()
        preferencesRepository.markStartupWizardCompleted().join()

        val workspaceRepository = WorkspaceRepository(context)
        workspaceRepository.ensureDefaults(
            favoriteKeys = emptyList(),
            dockKeys = emptyList(),
        )
        val runtime = WorkspaceProductionRuntimeCoordinator(
            authorityRepository = workspaceRepository,
            workspaceDaoProvider = {
                LauncherDatabaseProvider.get(context).workspaceDao()
            },
        )
        runtime.reconcileAndActivate()
        withTimeout(10_000) {
            workspaceRepository.state.first {
                it.initialized && it.authority == WorkspaceAuthority.ROOM
            }
        }
        withTimeout(10_000) {
            runtime.observeHomePages().first { state ->
                state is WorkspacePagedHomeState.Ready &&
                    state.pages.any { page ->
                        page.pageId == WorkspaceLegacyImportMapper.HOME_PAGE_ID
                    }
            }
        }

        preferencesRepository.markStarterLayoutApplied().join()
        withTimeout(5_000) {
            preferencesRepository.experiencePreferences.first {
                it.homeAppMode == LauncherHomeAppMode.NONE &&
                    it.startupWizardCompleted &&
                    it.starterLayoutApplied
            }
        }
        Unit
    }

    @After
    fun restorePreferencesAndRole() = runBlocking {
        if (!::preferencesRepository.isInitialized) return@runBlocking
        val currentHiddenKeys = preferencesRepository.hiddenAppKeys.first()
        currentHiddenKeys.forEach { key ->
            preferencesRepository.setAppHidden(key, false).join()
        }
        previousHiddenKeys.forEach { key ->
            preferencesRepository.setAppHidden(key, true).join()
        }
        val currentLockedKeys = preferencesRepository.lockedAppKeys.first()
        currentLockedKeys.forEach { key ->
            preferencesRepository.setAppLocked(key, false).join()
        }
        previousLockedKeys.forEach { key ->
            preferencesRepository.setAppLocked(key, true).join()
        }
        preferencesRepository.setHomeAppMode(previousHomeAppMode).join()
    }

    @Test
    fun settingsExposesHiddenAppsRecoverySurfaceWhenNothingIsHidden() {
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
                .performSemanticsAction(SemanticsActions.OnLongClick)

            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule
                    .onAllNodesWithTag(
                        "launcher-home-editor-fullscreen",
                        useUnmergedTree = true,
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeRule
                .onAllNodesWithText("Settings", useUnmergedTree = true)[0]
                .performClick()

            composeRule
                .onNodeWithText("Launcher settings", useUnmergedTree = true)
                .assertIsDisplayed()
            composeRule
                .onNodeWithText("App Drawer", useUnmergedTree = true)
                .performClick()

            composeRule
                .onAllNodesWithText("Suggested apps", useUnmergedTree = true)
                .assertCountEquals(1)

            composeRule
                .onNodeWithTag("launcher-settings-hidden-apps", useUnmergedTree = true)
                .performScrollTo()
                .performClick()

            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule
                    .onAllNodesWithTag(
                        "launcher-hidden-apps-manager",
                        useUnmergedTree = true,
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule
                .onNodeWithTag("launcher-hidden-apps-manager", useUnmergedTree = true)
                .assertIsDisplayed()
            composeRule
                .onNodeWithText("No hidden apps.", useUnmergedTree = true)
                .assertIsDisplayed()

            composeRule
                .onNodeWithText("Done", useUnmergedTree = true)
                .performClick()
            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule
                    .onAllNodesWithTag(
                        "launcher-hidden-apps-manager",
                        useUnmergedTree = true,
                    )
                    .fetchSemanticsNodes()
                    .isEmpty()
            }
        } finally {
            scenario.close()
        }
    }

    @Test
    fun settingsExposesAppLockManagerAndAuthorityBoundary() {
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
                .performSemanticsAction(SemanticsActions.OnLongClick)

            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule
                    .onAllNodesWithTag(
                        "launcher-home-editor-fullscreen",
                        useUnmergedTree = true,
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            composeRule
                .onAllNodesWithText("Settings", useUnmergedTree = true)[0]
                .performClick()

            composeRule
                .onNodeWithText("Privacy & Permissions", useUnmergedTree = true)
                .performScrollTo()
                .performClick()

            composeRule
                .onNodeWithTag("launcher-settings-app-lock", useUnmergedTree = true)
                .performScrollTo()
                .performClick()

            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule
                    .onAllNodesWithTag(
                        "launcher-app-lock-manager",
                        useUnmergedTree = true,
                    )
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule
                .onNodeWithTag("launcher-app-lock-manager", useUnmergedTree = true)
                .assertIsDisplayed()
            composeRule
                .onNodeWithText(
                    "Launcher-only boundary: direct launches from notifications, Android Settings, " +
                        "deep links, other launchers, or another app are not intercepted.",
                    useUnmergedTree = true,
                )
                .assertIsDisplayed()
            composeRule
                .onNodeWithTag("launcher-app-lock-search", useUnmergedTree = true)
                .assertIsDisplayed()
            composeRule
                .onNodeWithTag("launcher-app-lock-filter-all", useUnmergedTree = true)
                .assertIsDisplayed()
            composeRule
                .onNodeWithTag("launcher-app-lock-filter-locked", useUnmergedTree = true)
                .assertIsDisplayed()
            composeRule
                .onNodeWithTag("launcher-app-lock-close", useUnmergedTree = true)
                .performClick()
            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule
                    .onAllNodesWithTag(
                        "launcher-app-lock-manager",
                        useUnmergedTree = true,
                    )
                    .fetchSemanticsNodes()
                    .isEmpty()
            }
        } finally {
            scenario.close()
        }
    }


}
