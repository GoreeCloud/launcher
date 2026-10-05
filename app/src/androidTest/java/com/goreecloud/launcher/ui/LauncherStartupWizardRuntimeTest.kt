package com.goreecloud.launcher.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
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
        composeRule.onNodeWithText("Step 1 of 3")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Continue").performScrollTo().performClick()

        composeRule.onNodeWithText("Build your Home")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("None")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsSelected()
        composeRule.onNodeWithText("Recent")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsNotSelected()
        composeRule.onNodeWithText("Most used")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsNotSelected()
            .performClick()
            .assertIsSelected()
        composeRule.onNodeWithText("None")
            .assertIsNotSelected()
        composeRule.onNodeWithText("Continue").performScrollTo().performClick()

        composeRule.onNodeWithText("Search and gestures")
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
    fun wizardKeepsAdvancedDetailsBehindLearnMore() {
        composeRule.setContent {
            MaterialTheme {
                LauncherStartupWizard(
                    isDefaultHome = true,
                    initialHomeAppMode = LauncherHomeAppMode.NONE,
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

        composeRule.onNodeWithText("Search and gestures")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Hold + drag to place")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Widgets")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Folders")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("App Lock")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Learn more")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("Exact placement")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Connected Search")
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

        composeRule.onNodeWithText("Swipe up").assertIsDisplayed()
        composeRule.onNodeWithText("Swipe down").assertIsDisplayed()
        composeRule.onNodeWithText("Hold").assertIsDisplayed()
        composeRule.onNodeWithText("Edit").assertIsDisplayed()
        composeRule.onNodeWithText("Place precisely").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Hold an app, widget, or folder and drag it to a Home cell or Dock position.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Move across pages").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Keep holding at a page edge to switch pages, then release on the target.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Use Dock pages").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Add more favorites than fit in one row, then swipe the Dock independently of Home pages.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Organize Apps").assertIsDisplayed()
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

        composeRule.onNodeWithText("Build your Home")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Step 2 of 3")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Continue").performScrollTo().performClick()

        assertEquals(2, reportedStep)
        composeRule.onNodeWithText("Search and gestures")
            .performScrollTo()
            .assertIsDisplayed()
    }

}
