package com.goreecloud.launcher.core.workspace.db

import com.goreecloud.launcher.core.workspace.WorkspaceAuthority
import com.goreecloud.launcher.core.workspace.WorkspaceRepository
import com.goreecloud.launcher.core.workspace.WorkspaceState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

sealed interface WorkspaceCutoverReadinessResult {
    data object WaitingForInitialization : WorkspaceCutoverReadinessResult
    data object NeedsVerification : WorkspaceCutoverReadinessResult
    data object Ready : WorkspaceCutoverReadinessResult
    data object AlreadyRoomAuthoritative : WorkspaceCutoverReadinessResult
    data object Unavailable : WorkspaceCutoverReadinessResult
    data object Mismatch : WorkspaceCutoverReadinessResult
    data object StaleEvidence : WorkspaceCutoverReadinessResult
    data class Failed(val failureType: String) : WorkspaceCutoverReadinessResult
}

internal object WorkspaceCutoverReadinessGuard {
    fun stillMatchesVerifiedEvidence(
        expected: WorkspaceState,
        current: WorkspaceState,
    ): Boolean =
        expected.initialized &&
            current.initialized &&
            expected.authority == WorkspaceAuthority.ROOM_VERIFIED &&
            current.authority == WorkspaceAuthority.ROOM_VERIFIED &&
            expected.favoriteKeys == current.favoriteKeys &&
            expected.dockKeys == current.dockKeys &&
            expected.verifiedRoomFingerprint != null &&
            expected.verifiedRoomFingerprint == current.verifiedRoomFingerprint
}

/**
 * Proves that the current pre-cutover workspace is eligible for a future explicit Room promotion.
 *
 * This coordinator is observational. It never calls promoteRoomAuthority(), never writes Room, and
 * never changes the current DataStore authority. A Ready result is therefore readiness evidence,
 * not the authority transition itself.
 */
class WorkspaceCutoverReadinessCoordinator(
    private val repository: WorkspaceRepository,
    private val workspaceDaoProvider: () -> WorkspaceDao?,
) {
    suspend fun evaluate(): WorkspaceCutoverReadinessResult {
        val expected = repository.state.first()
        if (!expected.initialized) {
            return WorkspaceCutoverReadinessResult.WaitingForInitialization
        }

        when (expected.authority) {
            WorkspaceAuthority.DATASTORE -> return WorkspaceCutoverReadinessResult.NeedsVerification
            WorkspaceAuthority.ROOM -> return WorkspaceCutoverReadinessResult.AlreadyRoomAuthoritative
            WorkspaceAuthority.ROOM_VERIFIED -> Unit
        }

        val workspaceDao = workspaceDaoOrNull() ?: return WorkspaceCutoverReadinessResult.Unavailable

        return try {
            when (val dualRead = WorkspaceRelationalReader(workspaceDao).reconcile(expected)) {
                WorkspaceDualReadResult.Match -> Unit
                WorkspaceDualReadResult.Mismatch -> return WorkspaceCutoverReadinessResult.Mismatch
                WorkspaceDualReadResult.Skipped -> return WorkspaceCutoverReadinessResult.StaleEvidence
                is WorkspaceDualReadResult.Failed -> {
                    return WorkspaceCutoverReadinessResult.Failed(dualRead.failureType)
                }
            }

            val canonicalSnapshot = WorkspaceCanonicalRoomPlacementReader.read(workspaceDao)
                ?: return WorkspaceCutoverReadinessResult.Mismatch
            if (
                canonicalSnapshot.favoriteKeys != expected.favoriteKeys ||
                canonicalSnapshot.dockKeys != expected.dockKeys
            ) {
                return WorkspaceCutoverReadinessResult.Mismatch
            }

            val current = repository.state.first()
            if (WorkspaceCutoverReadinessGuard.stillMatchesVerifiedEvidence(expected, current)) {
                WorkspaceCutoverReadinessResult.Ready
            } else {
                WorkspaceCutoverReadinessResult.StaleEvidence
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            WorkspaceCutoverReadinessResult.Failed(exception::class.java.simpleName)
        }
    }

    private fun workspaceDaoOrNull(): WorkspaceDao? = try {
        workspaceDaoProvider()
    } catch (exception: CancellationException) {
        throw exception
    } catch (_: Exception) {
        null
    }
}
