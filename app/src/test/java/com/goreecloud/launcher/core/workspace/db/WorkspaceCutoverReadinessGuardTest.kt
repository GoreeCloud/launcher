package com.goreecloud.launcher.core.workspace.db

import com.goreecloud.launcher.core.workspace.WorkspaceAuthority
import com.goreecloud.launcher.core.workspace.WorkspaceState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceCutoverReadinessGuardTest {
    private val verified = WorkspaceState(
        initialized = true,
        favoriteKeys = listOf("favorite-a", "favorite-b"),
        dockKeys = listOf("dock-a"),
        authority = WorkspaceAuthority.ROOM_VERIFIED,
        verifiedRoomFingerprint = "verified-fingerprint",
    )

    @Test
    fun exactVerifiedEvidenceMatches() {
        assertTrue(
            WorkspaceCutoverReadinessGuard.stillMatchesVerifiedEvidence(
                expected = verified,
                current = verified.copy(),
            )
        )
    }

    @Test
    fun changedWorkspaceOrFingerprintIsStale() {
        assertFalse(
            WorkspaceCutoverReadinessGuard.stillMatchesVerifiedEvidence(
                expected = verified,
                current = verified.copy(favoriteKeys = listOf("favorite-b", "favorite-a")),
            )
        )
        assertFalse(
            WorkspaceCutoverReadinessGuard.stillMatchesVerifiedEvidence(
                expected = verified,
                current = verified.copy(dockKeys = listOf("dock-b")),
            )
        )
        assertFalse(
            WorkspaceCutoverReadinessGuard.stillMatchesVerifiedEvidence(
                expected = verified,
                current = verified.copy(verifiedRoomFingerprint = "different"),
            )
        )
    }

    @Test
    fun authorityChangeOrMissingFingerprintIsStale() {
        assertFalse(
            WorkspaceCutoverReadinessGuard.stillMatchesVerifiedEvidence(
                expected = verified,
                current = verified.copy(authority = WorkspaceAuthority.DATASTORE),
            )
        )
        assertFalse(
            WorkspaceCutoverReadinessGuard.stillMatchesVerifiedEvidence(
                expected = verified,
                current = verified.copy(authority = WorkspaceAuthority.ROOM),
            )
        )
        assertFalse(
            WorkspaceCutoverReadinessGuard.stillMatchesVerifiedEvidence(
                expected = verified,
                current = verified.copy(verifiedRoomFingerprint = null),
            )
        )
    }
}
