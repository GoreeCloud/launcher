package com.goreecloud.launcher.core.workspace.db

import com.goreecloud.launcher.core.workspace.WorkspaceAuthority
import com.goreecloud.launcher.core.workspace.WorkspaceRepository
import com.goreecloud.launcher.core.workspace.WorkspaceState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

data class WorkspacePromotionCandidate(
    val expectedState: WorkspaceState,
)

sealed interface WorkspacePromotionRehearsalResult {
    data object WaitingForInitialization : WorkspacePromotionRehearsalResult
    data object NeedsVerification : WorkspacePromotionRehearsalResult
    data class Candidate(val candidate: WorkspacePromotionCandidate) : WorkspacePromotionRehearsalResult
    data object AlreadyRoomAuthoritative : WorkspacePromotionRehearsalResult
    data object Unavailable : WorkspacePromotionRehearsalResult
    data object Mismatch : WorkspacePromotionRehearsalResult
    data object StaleEvidence : WorkspacePromotionRehearsalResult
    data class Failed(val failureType: String) : WorkspacePromotionRehearsalResult
}

/**
 * Rehearses the final promotion boundary without performing the authority mutation.
 *
 * A candidate is produced only after the existing observational readiness gate succeeds and a
 * fresh Room/DataStore boundary check still matches the same ROOM_VERIFIED evidence. Production
 * code must not treat a candidate as authority; the guarded repository promotion remains disabled
 * by the source-level cutover CI policy.
 */
class WorkspacePromotionRehearsalCoordinator(
    private val repository: WorkspaceRepository,
    private val workspaceDaoProvider: () -> WorkspaceDao?,
) {
    suspend fun evaluate(): WorkspacePromotionRehearsalResult {
        when (
            val readiness = WorkspaceCutoverReadinessCoordinator(
                repository = repository,
                workspaceDaoProvider = workspaceDaoProvider,
            ).evaluate()
        ) {
            WorkspaceCutoverReadinessResult.WaitingForInitialization -> {
                return WorkspacePromotionRehearsalResult.WaitingForInitialization
            }
            WorkspaceCutoverReadinessResult.NeedsVerification -> {
                return WorkspacePromotionRehearsalResult.NeedsVerification
            }
            WorkspaceCutoverReadinessResult.AlreadyRoomAuthoritative -> {
                return WorkspacePromotionRehearsalResult.AlreadyRoomAuthoritative
            }
            WorkspaceCutoverReadinessResult.Unavailable -> {
                return WorkspacePromotionRehearsalResult.Unavailable
            }
            WorkspaceCutoverReadinessResult.Mismatch -> {
                return WorkspacePromotionRehearsalResult.Mismatch
            }
            WorkspaceCutoverReadinessResult.StaleEvidence -> {
                return WorkspacePromotionRehearsalResult.StaleEvidence
            }
            is WorkspaceCutoverReadinessResult.Failed -> {
                return WorkspacePromotionRehearsalResult.Failed(readiness.failureType)
            }
            WorkspaceCutoverReadinessResult.Ready -> Unit
        }

        val expected = repository.state.first()
        if (
            !expected.initialized ||
            expected.authority != WorkspaceAuthority.ROOM_VERIFIED ||
            expected.verifiedRoomFingerprint == null
        ) {
            return WorkspacePromotionRehearsalResult.StaleEvidence
        }

        val workspaceDao = workspaceDaoOrNull() ?: return WorkspacePromotionRehearsalResult.Unavailable

        return try {
            val relationalState = WorkspaceCanonicalRoomPlacementReader.read(workspaceDao)
                ?: return WorkspacePromotionRehearsalResult.Mismatch
            if (
                relationalState.favoriteKeys != expected.favoriteKeys ||
                relationalState.dockKeys != expected.dockKeys
            ) {
                return WorkspacePromotionRehearsalResult.Mismatch
            }

            val current = repository.state.first()
            if (!WorkspaceCutoverReadinessGuard.stillMatchesVerifiedEvidence(expected, current)) {
                return WorkspacePromotionRehearsalResult.StaleEvidence
            }

            WorkspacePromotionRehearsalResult.Candidate(
                WorkspacePromotionCandidate(expectedState = current)
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            WorkspacePromotionRehearsalResult.Failed(exception::class.java.simpleName)
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

sealed interface WorkspacePostCutoverHealthResult {
    data object NotRoomAuthoritative : WorkspacePostCutoverHealthResult
    data object Healthy : WorkspacePostCutoverHealthResult
    data object Unavailable : WorkspacePostCutoverHealthResult
    data object Mismatch : WorkspacePostCutoverHealthResult
    data object AuthorityChanged : WorkspacePostCutoverHealthResult
    data class Failed(val failureType: String) : WorkspacePostCutoverHealthResult
}

/**
 * Observes the terminal ROOM phase without attempting automatic rollback.
 *
 * Once ROOM has been recorded, an unavailable or malformed relational database must surface as a
 * recovery condition. This evaluator intentionally never rewrites authority to DataStore.
 */
class WorkspacePostCutoverHealthEvaluator(
    private val repository: WorkspaceRepository,
    private val workspaceDaoProvider: () -> WorkspaceDao?,
) {
    suspend fun evaluate(): WorkspacePostCutoverHealthResult {
        val startingState = repository.state.first()
        if (!startingState.initialized || startingState.authority != WorkspaceAuthority.ROOM) {
            return WorkspacePostCutoverHealthResult.NotRoomAuthoritative
        }

        val workspaceDao = workspaceDaoOrNull() ?: return WorkspacePostCutoverHealthResult.Unavailable

        return try {
            if (WorkspaceCanonicalRoomPlacementReader.read(workspaceDao) == null) {
                return WorkspacePostCutoverHealthResult.Mismatch
            }

            val current = repository.state.first()
            if (current.authority != WorkspaceAuthority.ROOM) {
                WorkspacePostCutoverHealthResult.AuthorityChanged
            } else {
                WorkspacePostCutoverHealthResult.Healthy
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            WorkspacePostCutoverHealthResult.Failed(exception::class.java.simpleName)
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
