package com.goreecloud.launcher.core.launcher

import com.goreecloud.launcher.core.workspace.WorkspaceGridPlacement
import com.goreecloud.launcher.core.workspace.WorkspacePagedPlacement
import com.goreecloud.launcher.core.workspace.WorkspacePortableSnapshot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class LauncherPortableRestoreImportTest {
    private val workspace = WorkspacePortableSnapshot.Snapshot(
        grid = WorkspaceGridPlacement.Grid(columns = 4, rows = 5),
        pages = listOf(
            WorkspacePagedPlacement.Page(
                pageId = "home",
                rank = 0,
                placements = listOf(
                    WorkspaceGridPlacement.Placement(
                        itemId = "profile:10/com.example.notes",
                        cellX = 0,
                        cellY = 0,
                    )
                ),
            )
        ),
    )

    private val preferences = LauncherPreferences(
        homeColumns = 4,
        homeRows = 5,
        drawerColumns = 5,
        showLabels = true,
        iconScale = 1.0f,
        layoutLocked = false,
        universalSearchHomeMode = LauncherUniversalSearchHomeMode.PERMANENT,
    )

    private data class Write(
        val workspace: WorkspacePortableSnapshot.Snapshot,
        val preferences: LauncherPreferences,
    )

    private class RecordingWriter : LauncherPortableRestoreWriter {
        val writes = mutableListOf<Write>()

        override suspend fun replacePortableState(
            workspace: WorkspacePortableSnapshot.Snapshot,
            preferences: LauncherPreferences,
        ) {
            writes += Write(workspace, preferences)
        }
    }

    @Test
    fun twoValidCompatibleSnapshotsProduceExactlyOneCombinedWrite() = runBlocking {
        val writer = RecordingWriter()

        val result = LauncherPortableRestoreImport.apply(
            workspaceEncoded = WorkspacePortableSnapshot.encode(workspace),
            preferencesEncoded = LauncherPortablePreferences.encode(preferences),
            writer = writer,
        )

        assertTrue(result is LauncherPortableRestoreImport.ApplyResult.Applied)
        assertEquals(listOf(Write(workspace, preferences)), writer.writes)
    }

    @Test
    fun reviewedApplyUsesExactReviewedInputPair() = runBlocking {
        val writer = RecordingWriter()
        val workspaceEncoded = WorkspacePortableSnapshot.encode(workspace)
        val preferencesEncoded = LauncherPortablePreferences.encode(preferences)
        val validation = LauncherPortableRestoreImport.validate(workspaceEncoded, preferencesEncoded)
        assertTrue(validation is LauncherPortableRestoreImport.ValidationResult.Ready)
        val reviewToken = (validation as LauncherPortableRestoreImport.ValidationResult.Ready).reviewToken

        val result = LauncherPortableRestoreImport.applyReviewed(
            workspaceEncoded = workspaceEncoded,
            preferencesEncoded = preferencesEncoded,
            expectedReviewToken = reviewToken,
            writer = writer,
        )

        assertTrue(result is LauncherPortableRestoreImport.ApplyResult.Applied)
        assertEquals(listOf(Write(workspace, preferences)), writer.writes)
    }

    @Test
    fun reviewedApplyRejectsDifferentValidPairWithoutWriting() = runBlocking {
        val writer = RecordingWriter()
        val workspaceEncoded = WorkspacePortableSnapshot.encode(workspace)
        val preferencesEncoded = LauncherPortablePreferences.encode(preferences)
        val validation = LauncherPortableRestoreImport.validate(workspaceEncoded, preferencesEncoded)
            as LauncherPortableRestoreImport.ValidationResult.Ready
        val changedPreferences = preferences.copy(drawerColumns = 6)

        val result = LauncherPortableRestoreImport.applyReviewed(
            workspaceEncoded = workspaceEncoded,
            preferencesEncoded = LauncherPortablePreferences.encode(changedPreferences),
            expectedReviewToken = validation.reviewToken,
            writer = writer,
        )

        assertTrue(result is LauncherPortableRestoreImport.ApplyResult.Rejected)
        val rejected = result as LauncherPortableRestoreImport.ApplyResult.Rejected
        assertEquals(LauncherPortableRestoreImport.RejectionSource.REVIEW_CHANGED, rejected.source)
        assertTrue(rejected.reason.contains("changed after review"))
        assertTrue(writer.writes.isEmpty())
    }

    @Test
    fun invalidWorkspacePerformsZeroCombinedWrites() = runBlocking {
        val writer = RecordingWriter()
        val tamperedWorkspace = WorkspacePortableSnapshot.encode(workspace)
            .replaceFirst("item=", "item=X")

        val result = LauncherPortableRestoreImport.apply(
            workspaceEncoded = tamperedWorkspace,
            preferencesEncoded = LauncherPortablePreferences.encode(preferences),
            writer = writer,
        )

        assertTrue(result is LauncherPortableRestoreImport.ApplyResult.Rejected)
        val rejected = result as LauncherPortableRestoreImport.ApplyResult.Rejected
        assertEquals(LauncherPortableRestoreImport.RejectionSource.WORKSPACE, rejected.source)
        assertTrue(writer.writes.isEmpty())
    }

    @Test
    fun invalidPreferencesPerformZeroCombinedWritesAfterWorkspaceValidation() = runBlocking {
        val writer = RecordingWriter()
        val tamperedPreferences = LauncherPortablePreferences.encode(preferences)
            .replace("show_labels=true", "show_labels=false")

        val result = LauncherPortableRestoreImport.apply(
            workspaceEncoded = WorkspacePortableSnapshot.encode(workspace),
            preferencesEncoded = tamperedPreferences,
            writer = writer,
        )

        assertTrue(result is LauncherPortableRestoreImport.ApplyResult.Rejected)
        val rejected = result as LauncherPortableRestoreImport.ApplyResult.Rejected
        assertEquals(LauncherPortableRestoreImport.RejectionSource.PREFERENCES, rejected.source)
        assertTrue(writer.writes.isEmpty())
    }

    @Test
    fun individuallyValidButIncompatibleHomeGridsPerformZeroCombinedWrites() = runBlocking {
        val writer = RecordingWriter()
        val incompatiblePreferences = preferences.copy(homeColumns = 5)

        val result = LauncherPortableRestoreImport.apply(
            workspaceEncoded = WorkspacePortableSnapshot.encode(workspace),
            preferencesEncoded = LauncherPortablePreferences.encode(incompatiblePreferences),
            writer = writer,
        )

        assertTrue(result is LauncherPortableRestoreImport.ApplyResult.Rejected)
        val rejected = result as LauncherPortableRestoreImport.ApplyResult.Rejected
        assertEquals(LauncherPortableRestoreImport.RejectionSource.COMPATIBILITY, rejected.source)
        assertTrue(rejected.reason.contains("does not match portable Home grid"))
        assertTrue(writer.writes.isEmpty())
    }

    @Test
    fun persistenceFailureIsNeverReportedAsApplied() {
        val writer = object : LauncherPortableRestoreWriter {
            override suspend fun replacePortableState(
                workspace: WorkspacePortableSnapshot.Snapshot,
                preferences: LauncherPreferences,
            ) {
                throw IllegalStateException("synthetic combined persistence failure")
            }
        }

        try {
            runBlocking {
                LauncherPortableRestoreImport.apply(
                    workspaceEncoded = WorkspacePortableSnapshot.encode(workspace),
                    preferencesEncoded = LauncherPortablePreferences.encode(preferences),
                    writer = writer,
                )
            }
            fail("persistence failure must propagate")
        } catch (expected: IllegalStateException) {
            assertEquals("synthetic combined persistence failure", expected.message)
        }
    }
}
