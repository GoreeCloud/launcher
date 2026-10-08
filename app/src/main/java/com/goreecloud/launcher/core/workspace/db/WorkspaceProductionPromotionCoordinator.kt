package com.goreecloud.launcher.core.workspace.db

import com.goreecloud.launcher.core.workspace.WorkspaceAuthority
import com.goreecloud.launcher.core.workspace.WorkspaceRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

sealed interface WorkspaceProductionPromotionResult {
    data object WaitingForInitialization : WorkspaceProductionPromotionResult
    data object NeedsVerification : WorkspaceProductionPromotionResult
    data object AlreadyRoomAuthoritative : WorkspaceProductionPromotionResult
    data object Unavailable : WorkspaceProductionPromotionResult
    data object Mismatch : WorkspaceProductionPromotionResult
    data object StaleEvidence : WorkspaceProductionPromotionResult
    data object Rejected : WorkspaceProductionPromotionResult
    data object PromotedHealthy : WorkspaceProductionPromotionResult
    data class PromotedRecoveryRequired(
        val health: WorkspacePostCutoverHealthResult,
    ) : WorkspaceProductionPromotionResult
    data class Failed(val failureType: String) : WorkspaceProductionPromotionResult
}

/**
 * Implements the reviewed production authority transaction without activating it in Home.
 *
 * This coordinator is intentionally not instantiated by MainActivity or any other production
 * runtime entry point. The source-level Room cutover guard permits the guarded promotion primitive
 * only here while separately rejecting production instantiation of this coordinator. A later
 * explicit cutover change must modify that activation boundary and Home routing together.
 */
class WorkspaceProductionPromotionCoordinator(
    private val repository: WorkspaceRepository,
    private val workspaceDaoProvider: () -> WorkspaceDao?,
) {
    suspend fun promote(): WorkspaceProductionPromotionResult {
        val candidate = when (
            val rehearsal = WorkspacePromotionRehearsalCoordinator(
                repository = repository,
                workspaceDaoProvider = workspaceDaoProvider,
            ).evaluate()
        ) {
            WorkspacePromotionRehearsalResult.WaitingForInitialization -> {
                return WorkspaceProductionPromotionResult.WaitingForInitialization
            }
            WorkspacePromotionRehearsalResult.NeedsVerification -> {
                return WorkspaceProductionPromotionResult.NeedsVerification
            }
            WorkspacePromotionRehearsalResult.AlreadyRoomAuthoritative -> {
                return WorkspaceProductionPromotionResult.AlreadyRoomAuthoritative
            }
            WorkspacePromotionRehearsalResult.Unavailable -> {
                return WorkspaceProductionPromotionResult.Unavailable
            }
            WorkspacePromotionRehearsalResult.Mismatch -> {
                return WorkspaceProductionPromotionResult.Mismatch
            }
            WorkspacePromotionRehearsalResult.StaleEvidence -> {
                return WorkspaceProductionPromotionResult.StaleEvidence
            }
            is WorkspacePromotionRehearsalResult.Failed -> {
                return WorkspaceProductionPromotionResult.Failed(rehearsal.failureType)
            }
            is WorkspacePromotionRehearsalResult.Candidate -> rehearsal.candidate
        }

        val workspaceDao = workspaceDaoOrNull() ?: return WorkspaceProductionPromotionResult.Unavailable

        return try {
            val relationalState = WorkspaceCanonicalRoomPlacementReader.read(workspaceDao)
                ?: return WorkspaceProductionPromotionResult.Mismatch
            if (
                relationalState.favoriteKeys != candidate.expectedState.favoriteKeys ||
                relationalState.dockKeys != candidate.expectedState.dockKeys
            ) {
                return WorkspaceProductionPromotionResult.Mismatch
            }

            val current = repository.state.first()
            if (
                !WorkspaceCutoverReadinessGuard.stillMatchesVerifiedEvidence(
                    candidate.expectedState,
                    current,
                )
            ) {
                return WorkspaceProductionPromotionResult.StaleEvidence
            }

            if (!repository.promoteRoomAuthority(current)) {
                return WorkspaceProductionPromotionResult.Rejected
            }

            if (repository.state.first().authority != WorkspaceAuthority.ROOM) {
                return WorkspaceProductionPromotionResult.Rejected
            }

            when (
                val health = WorkspacePostCutoverHealthEvaluator(
                    repository = repository,
                    workspaceDaoProvider = workspaceDaoProvider,
                ).evaluate()
            ) {
                WorkspacePostCutoverHealthResult.Healthy -> {
                    WorkspaceProductionPromotionResult.PromotedHealthy
                }
                else -> WorkspaceProductionPromotionResult.PromotedRecoveryRequired(health)
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            WorkspaceProductionPromotionResult.Failed(exception::class.java.simpleName)
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
