package com.goreecloud.launcher.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.goreecloud.launcher.core.launcher.LauncherHomeAppMode
import com.goreecloud.launcher.core.launcher.LauncherUniversalSearchHomeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherStartupWizardRuntimeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun wizardOffersHomeModesAndReturnsSelectedConfiguration() {
        var completed: LauncherStartupConfiguration? = null

        composeRule.setContent {
            MaterialTheme {
                LauncherStartupWizard(
                    isDefaultHome = false,
                    initialHomeAppMode = LauncherHomeAppMode.NONE,
                    initialHomeColumns = 5,
                    initialHomeRows = 6,
                    initialShowHomeLabels = true,
                    initialUniversalSearchHomeMode =
                        LauncherUniversalSearchHomeMode.SWIPE_DOWN_ONLY,
                    initialAddNewAppsToHome = false,
                    initialShowHints = true,
                    onRequestHomeRole = {},
                    onFinish = { completed = it },
                )
            }
        }

        composeRule.onNodeWithText("Welcome to GoreeCloud Launcher")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Continue").performScrollTo().performClick()

        composeRule.onNodeWithText("Set up your Home")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("No automatic apps")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("10 most recent apps")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("10 most used apps")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithText("Continue").performScrollTo().performClick()

        composeRule.onNodeWithText("Search, folders, and hints")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Finish setup").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) { completed != null }
        val result = completed
        assertNotNull(result)
        assertEquals(LauncherHomeAppMode.MOST_USED, result?.homeAppMode)
        assertEquals(5, result?.homeColumns)
        assertEquals(6, result?.homeRows)
        assertEquals(LauncherUniversalSearchHomeMode.SWIPE_DOWN_ONLY, result?.universalSearchHomeMode)
    }
    @Test
    fun wizardExplainsCurrentPlacementFolderAndConnectedSearchBoundaries() {
        composeRule.setContent {
            MaterialTheme {
                LauncherStartupWizard(
                    isDefaultHome = true,
                    initialHomeAppMode = LauncherHomeAppMode.RECENT,
                    initialHomeColumns = 5,
                    initialHomeRows = 6,
                    initialShowHomeLabels = true,
                    initialUniversalSearchHomeMode =
                        LauncherUniversalSearchHomeMode.SWIPE_DOWN_ONLY,
                    initialAddNewAppsToHome = false,
                    initialShowHints = true,
                    initialStep = 2,
                    onRequestHomeRole = {},
                    onFinish = {},
                )
            }
        }

        composeRule.onNodeWithText("Search, folders, and hints")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Place apps exactly")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(
            "Long-press an app in Apps, keep holding, and drag it to the exact primary Home cell or Dock position you want. On Home, keep holding a saved app at a left or right page edge briefly to switch pages, then release over the exact target cell. Long-press empty Home space for Edit Home; Launcher Settings is available there and from the gear in Apps.",
        )
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Move widgets directly")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(
            "Long-press a Home widget and drag it to a free cell, or release it at a valid page edge to move it to the adjacent Home page. A stationary hold opens widget options, including Move to another Home page. Fresh starter layouts use movable Glance.",
        )
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Manage folders in place")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(
            "Open a folder and use Add apps at the end of its grid or from the folder menu. On Home, long-press a folder to move it to a free cell or release it at a page edge to move it to the adjacent Home page. Larger opened folders swipe across compact pages.",
        )
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Connected Search is opt-in")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun homeHintCoversExactPlacementLivePageSwitchAndCurrentFolderAddPath() {
        composeRule.setContent {
            MaterialTheme {
                LauncherHomeHintCard(onDismiss = {})
            }
        }

        composeRule.onNodeWithText(
            "Long-press an app in Apps to drag it to Home/Dock or Pin in Apps; use Pinned first or the ★ filter to keep favorites easy to reach.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            "Home apps: keep holding at a left or right page edge briefly to switch pages, then release over the exact target cell.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            "Widgets: long-press and drag to a free Home cell or adjacent page edge; movable Universal Search uses the same Home-grid behavior.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            "Folders: long-press a Home folder to move it to a free cell or adjacent page edge; use Add apps at the end of the grid and swipe larger opened folders between pages.",
        ).assertIsDisplayed()
    }

    @Test
    fun wizardResumesAtPersistedStepAndReportsProgress() {
        var reportedStep = -1

        composeRule.setContent {
            MaterialTheme {
                LauncherStartupWizard(
                    isDefaultHome = true,
                    initialHomeAppMode = LauncherHomeAppMode.RECENT,
                    initialHomeColumns = 5,
                    initialHomeRows = 6,
                    initialShowHomeLabels = true,
                    initialUniversalSearchHomeMode =
                        LauncherUniversalSearchHomeMode.SWIPE_DOWN_ONLY,
                    initialAddNewAppsToHome = false,
                    initialShowHints = true,
                    initialStep = 1,
                    onStepChange = { reportedStep = it },
                    onRequestHomeRole = {},
                    onFinish = {},
                )
            }
        }

        composeRule.onNodeWithText("Set up your Home")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Continue").performScrollTo().performClick()

        assertEquals(2, reportedStep)
        composeRule.onNodeWithText("Search, folders, and hints")
            .performScrollTo()
            .assertIsDisplayed()
    }

}
