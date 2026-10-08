package com.goreecloud.launcher.core.launcher

import com.goreecloud.launcher.core.workspace.db.WorkspaceLegacyImportMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherStarterHomeDefaultsPolicyTest {
    @Test
    fun exactEmptyStarterSignatureIsRepairable() {
        assertTrue(
            LauncherStarterHomeDefaultsPolicy.shouldRepairEmptyStarter(
                roomAuthoritative = true,
                starterLayoutApplied = true,
                startupWizardCompleted = true,
                hasApps = true,
                hasFavorites = false,
                dockItemCount = 5,
                expectedStarterDockSize = 5,
                pageIds = listOf(
                    WorkspaceLegacyImportMapper.HOME_PAGE_ID,
                    "home:user:one",
                    "home:user:two",
                ),
                allPagesEmpty = true,
            ),
        )
    }

    @Test
    fun userContentOrWrongDockSignaturePreventsRepair() {
        assertFalse(
            LauncherStarterHomeDefaultsPolicy.shouldRepairEmptyStarter(
                roomAuthoritative = true,
                starterLayoutApplied = true,
                startupWizardCompleted = true,
                hasApps = true,
                hasFavorites = true,
                dockItemCount = 5,
                expectedStarterDockSize = 5,
                pageIds = listOf(WorkspaceLegacyImportMapper.HOME_PAGE_ID),
                allPagesEmpty = true,
            ),
        )
        assertFalse(
            LauncherStarterHomeDefaultsPolicy.shouldRepairEmptyStarter(
                roomAuthoritative = true,
                starterLayoutApplied = true,
                startupWizardCompleted = true,
                hasApps = true,
                hasFavorites = false,
                dockItemCount = 1,
                expectedStarterDockSize = 5,
                pageIds = listOf(WorkspaceLegacyImportMapper.HOME_PAGE_ID),
                allPagesEmpty = true,
            ),
        )
        assertFalse(
            LauncherStarterHomeDefaultsPolicy.shouldRepairEmptyStarter(
                roomAuthoritative = true,
                starterLayoutApplied = true,
                startupWizardCompleted = true,
                hasApps = true,
                hasFavorites = false,
                dockItemCount = 5,
                expectedStarterDockSize = 5,
                pageIds = listOf(WorkspaceLegacyImportMapper.HOME_PAGE_ID),
                allPagesEmpty = false,
            ),
        )
    }

    @Test
    fun runtimeFixturePagesAreNeverStarterRepairCandidates() {
        assertFalse(
            LauncherStarterHomeDefaultsPolicy.shouldRepairEmptyStarter(
                roomAuthoritative = true,
                starterLayoutApplied = true,
                startupWizardCompleted = true,
                hasApps = true,
                hasFavorites = false,
                dockItemCount = 5,
                expectedStarterDockSize = 5,
                pageIds = listOf(
                    WorkspaceLegacyImportMapper.HOME_PAGE_ID,
                    "home:test:horizontal-swipe",
                ),
                allPagesEmpty = true,
            ),
        )
        assertFalse(
            LauncherStarterHomeDefaultsPolicy.shouldRepairEmptyStarter(
                roomAuthoritative = true,
                starterLayoutApplied = true,
                startupWizardCompleted = true,
                hasApps = true,
                hasFavorites = false,
                dockItemCount = 5,
                expectedStarterDockSize = 5,
                pageIds = listOf(
                    WorkspaceLegacyImportMapper.HOME_PAGE_ID,
                    "home:runtime:fixture",
                ),
                allPagesEmpty = true,
            ),
        )
    }

    @Test
    fun repairKeepsPrimaryAndOneSecondaryPage() {
        assertEquals(
            listOf("home:user:two", "home:user:three"),
            LauncherStarterHomeDefaultsPolicy.excessEmptySecondaryPageIds(
                listOf(
                    WorkspaceLegacyImportMapper.HOME_PAGE_ID,
                    "home:user:one",
                    "home:user:two",
                    "home:user:three",
                ),
            ),
        )
        assertEquals(
            emptyList<String>(),
            LauncherStarterHomeDefaultsPolicy.excessEmptySecondaryPageIds(
                listOf(
                    WorkspaceLegacyImportMapper.HOME_PAGE_ID,
                    "home:user:one",
                ),
            ),
        )
    }
}
