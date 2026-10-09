package com.goreecloud.launcher.ui

import com.goreecloud.launcher.core.launcher.LauncherDrawerDiscoveryFilter
import com.goreecloud.launcher.core.launcher.LauncherDrawerLayoutMode
import com.goreecloud.launcher.core.launcher.LauncherDrawerNavigation
import com.goreecloud.launcher.core.launcher.LauncherDrawerProfileKind
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherDrawerViewportPolicyTest {
    @Test
    fun userAndWorkProfileTabsRespectGlazeInteractionFloor() {
        assertTrue(LAUNCHER_DRAWER_PROFILE_TAB_TOUCH_TARGET_DP >= 48)
    }

    @Test
    fun representativeProfileInventoriesStayComposedWhileScrolling() {
        assertTrue(launcherDrawerUsesEagerGrid(0))
        assertTrue(launcherDrawerUsesEagerGrid(76))
        assertTrue(launcherDrawerUsesEagerGrid(320))
        assertFalse(launcherDrawerUsesEagerGrid(321))
    }

    @Test
    fun rememberedPositionContextChangesWhenDrawerPresentationChanges() {
        val baseline = launcherDrawerPositionContextKey(
            profileKind = LauncherDrawerProfileKind.USER,
            layoutMode = LauncherDrawerLayoutMode.GRID,
            navigation = LauncherDrawerNavigation.SCROLL,
            sortOrder = LauncherDrawerSortOrder.ALPHABETICAL,
            discoveryFilter = LauncherDrawerDiscoveryFilter.ALL,
            drawerTabId = null,
            columns = 5,
            rowsPerPage = 5,
        )
        val workProfile = launcherDrawerPositionContextKey(
            profileKind = LauncherDrawerProfileKind.WORK,
            layoutMode = LauncherDrawerLayoutMode.GRID,
            navigation = LauncherDrawerNavigation.SCROLL,
            sortOrder = LauncherDrawerSortOrder.ALPHABETICAL,
            discoveryFilter = LauncherDrawerDiscoveryFilter.ALL,
            drawerTabId = null,
            columns = 5,
            rowsPerPage = 5,
        )
        val paged = launcherDrawerPositionContextKey(
            profileKind = LauncherDrawerProfileKind.USER,
            layoutMode = LauncherDrawerLayoutMode.GRID,
            navigation = LauncherDrawerNavigation.PAGES,
            sortOrder = LauncherDrawerSortOrder.ALPHABETICAL,
            discoveryFilter = LauncherDrawerDiscoveryFilter.ALL,
            drawerTabId = null,
            columns = 5,
            rowsPerPage = 5,
        )

        assertTrue(baseline != workProfile)
        assertTrue(baseline != paged)
        assertTrue(baseline.startsWith("USER|GRID|SCROLL|ALPHABETICAL|ALL|"))
    }

    @Test
    fun rememberedPositionProfileKindParsesOnlyKnownProfiles() {
        assertTrue(
            launcherDrawerPositionProfileKind(
                "WORK|GRID|SCROLL|ALPHABETICAL|ALL||5|5",
            ) == LauncherDrawerProfileKind.WORK,
        )
        assertTrue(
            launcherDrawerPositionProfileKind(
                "USER|LIST|SCROLL|ALPHABETICAL|ALL||5|5",
            ) == LauncherDrawerProfileKind.USER,
        )
        assertTrue(launcherDrawerPositionProfileKind("UNKNOWN|GRID") == null)
        assertTrue(launcherDrawerPositionProfileKind(null) == null)
    }

    @Test
    fun categoryViewportCountMatchesHeadersAndChunkedRows() {
        assertTrue(
            launcherDrawerCategoryLazyItemCount(
                categoryAppCounts = listOf(7, 1),
                folderCount = 6,
                smartFolderCount = 3,
                columns = 5,
            ) == 10,
        )
        assertTrue(
            launcherDrawerCategoryLazyItemCount(
                categoryAppCounts = emptyList(),
                folderCount = 0,
                smartFolderCount = 0,
                columns = 5,
            ) == 0,
        )
    }

    @Test
    fun stableRowsNeverDropEntries() {
        val input = (1..76).toList()
        val rows = launcherDrawerStableRows(input, columns = 5)

        assertTrue(rows.all { it.size in 1..5 })
        assertTrue(rows.flatten() == input)
    }
}
