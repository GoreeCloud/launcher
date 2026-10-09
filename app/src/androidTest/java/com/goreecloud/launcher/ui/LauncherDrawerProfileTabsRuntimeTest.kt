package com.goreecloud.launcher.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.goreecloud.launcher.core.launcher.LauncherDrawerProfileKind
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runtime verification of the explicit User/Work tab interaction contract.
 * Does not require creation of a managed Android profile or replace two-profile device QA.
 */
@RunWith(AndroidJUnit4::class)
class LauncherDrawerProfileTabsRuntimeTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun profileTabsExposeSelectedStateAndSwitchWithNonGestureClick() {
        composeRule.setContent {
            var chosen by remember { mutableStateOf(LauncherDrawerProfileKind.USER) }
            MaterialTheme {
                DrawerProfileTabs(
                    pages = listOf(
                        LauncherDrawerProfileKind.USER to 18,
                        LauncherDrawerProfileKind.WORK to 9,
                    ),
                    selected = chosen,
                    onSelect = { chosen = it },
                    secondaryColor = Color.Gray,
                )
            }
        }

        val user = composeRule.onNodeWithTag("launcher-drawer-profile-user")
        val work = composeRule.onNodeWithTag("launcher-drawer-profile-work")

        user.assertHasClickAction().assertIsSelected()
        work.assertHasClickAction().assertIsNotSelected()
        user.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
        work.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))

        work.performClick()
        work.assertIsSelected()
        user.assertIsNotSelected()

        user.performClick()
        user.assertIsSelected()
        work.assertIsNotSelected()
    }
}
