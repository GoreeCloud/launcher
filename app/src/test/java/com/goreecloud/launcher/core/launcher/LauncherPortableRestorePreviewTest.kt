package com.goreecloud.launcher.core.launcher

import com.goreecloud.launcher.core.workspace.WorkspaceGridPlacement
import com.goreecloud.launcher.core.workspace.WorkspacePagedPlacement
import com.goreecloud.launcher.core.workspace.WorkspacePortableSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherPortableRestorePreviewTest {
    @Test
    fun validCompatibleSnapshotsReturnAggregateWorkspaceAndReviewedPreferences() {
        val workspace = WorkspacePortableSnapshot.Snapshot(
            grid = WorkspaceGridPlacement.Grid(columns = 5, rows = 6),
            pages = listOf(
                WorkspacePagedPlacement.Page(
                    pageId = "page-a",
                    rank = 0,
                    placements = listOf(
                        WorkspaceGridPlacement.Placement("item-a", 0, 0, 1, 1),
                        WorkspaceGridPlacement.Placement("item-b", 1, 0, 1, 1),
                    ),
                ),
                WorkspacePagedPlacement.Page(
                    pageId = "page-b",
                    rank = 1,
                    placements = listOf(
                        WorkspaceGridPlacement.Placement("item-c", 0, 1, 2, 1),
                    ),
                ),
            ),
        )
        val preferences = LauncherPreferences(
            homeColumns = 5,
            homeRows = 6,
            drawerColumns = 4,
            showLabels = false,
            iconScale = 1.1f,
            layoutLocked = true,
            universalSearchHomeMode = LauncherUniversalSearchHomeMode.SWIPE_DOWN_ONLY,
        )

        val result = LauncherPortableRestorePreview.inspect(
            WorkspacePortableSnapshot.encode(workspace),
            LauncherPortablePreferences.encode(preferences),
        )

        assertTrue(result is LauncherPortableRestorePreview.Result.Ready)
        val ready = result as LauncherPortableRestorePreview.Result.Ready
        val summary = ready.summary
        assertEquals(5, summary.gridColumns)
        assertEquals(6, summary.gridRows)
        assertEquals(2, summary.pageCount)
        assertEquals(3, summary.itemCount)
        assertEquals(5, summary.homeColumns)
        assertEquals(6, summary.homeRows)
        assertEquals(4, summary.drawerColumns)
        assertFalse(summary.showLabels)
        assertEquals(1.1f, summary.iconScale)
        assertTrue(summary.layoutLocked)
        assertEquals(LauncherUniversalSearchHomeMode.SWIPE_DOWN_ONLY, summary.universalSearchHomeMode)
        assertTrue(ready.reviewToken.matches(Regex("[0-9a-f]{64}")))
    }

    @Test
    fun reviewTokenChangesWhenAReviewedValidPreferenceChanges() {
        val workspace = WorkspacePortableSnapshot.Snapshot(
            grid = WorkspaceGridPlacement.Grid(columns = 4, rows = 5),
            pages = listOf(WorkspacePagedPlacement.Page("page-a", 0, emptyList())),
        )
        val workspaceEncoded = WorkspacePortableSnapshot.encode(workspace)
        val first = LauncherPortableRestorePreview.inspect(
            workspaceEncoded,
            LauncherPortablePreferences.encode(LauncherPreferences(homeColumns = 4, homeRows = 5)),
        ) as LauncherPortableRestorePreview.Result.Ready
        val second = LauncherPortableRestorePreview.inspect(
            workspaceEncoded,
            LauncherPortablePreferences.encode(
                LauncherPreferences(homeColumns = 4, homeRows = 5, drawerColumns = 6),
            ),
        ) as LauncherPortableRestorePreview.Result.Ready

        assertNotEquals(first.reviewToken, second.reviewToken)
    }

    @Test
    fun invalidWorkspaceFailsBeforePreferencePreview() {
        val result = LauncherPortableRestorePreview.inspect(
            "not-a-workspace-snapshot",
            "not-a-preference-snapshot",
        )

        assertTrue(result is LauncherPortableRestorePreview.Result.Rejected)
        assertEquals(
            LauncherPortableRestoreImport.RejectionSource.WORKSPACE,
            (result as LauncherPortableRestorePreview.Result.Rejected).source,
        )
    }

    @Test
    fun validWorkspaceWithInvalidPreferencesIdentifiesPreferenceRejection() {
        val workspace = WorkspacePortableSnapshot.Snapshot(
            grid = WorkspaceGridPlacement.Grid(columns = 4, rows = 5),
            pages = listOf(WorkspacePagedPlacement.Page("page-a", 0, emptyList())),
        )

        val result = LauncherPortableRestorePreview.inspect(
            WorkspacePortableSnapshot.encode(workspace),
            "not-a-preference-snapshot",
        )

        assertTrue(result is LauncherPortableRestorePreview.Result.Rejected)
        assertEquals(
            LauncherPortableRestoreImport.RejectionSource.PREFERENCES,
            (result as LauncherPortableRestorePreview.Result.Rejected).source,
        )
    }

    @Test
    fun individuallyValidButMismatchedHomeGridsAreRejectedBeforePreview() {
        val workspace = WorkspacePortableSnapshot.Snapshot(
            grid = WorkspaceGridPlacement.Grid(columns = 4, rows = 5),
            pages = listOf(WorkspacePagedPlacement.Page("page-a", 0, emptyList())),
        )
        val preferences = LauncherPreferences(homeColumns = 5, homeRows = 5)

        val result = LauncherPortableRestorePreview.inspect(
            WorkspacePortableSnapshot.encode(workspace),
            LauncherPortablePreferences.encode(preferences),
        )

        assertTrue(result is LauncherPortableRestorePreview.Result.Rejected)
        val rejected = result as LauncherPortableRestorePreview.Result.Rejected
        assertEquals(LauncherPortableRestoreImport.RejectionSource.COMPATIBILITY, rejected.source)
    }

    @Test
    fun summaryShapeContainsNoWorkspaceIdentityCollectionsOrStrings() {
        val fieldTypes = LauncherPortableRestorePreview.Summary::class.java.declaredFields
            .map { it.type }

        assertFalse(fieldTypes.any { it == String::class.java })
        assertFalse(fieldTypes.any { java.util.Collection::class.java.isAssignableFrom(it) })
        assertFalse(fieldTypes.any { java.util.Map::class.java.isAssignableFrom(it) })
    }
}
