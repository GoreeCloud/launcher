package com.goreecloud.launcher.core.workspace.db

import com.goreecloud.launcher.core.workspace.WorkspaceAuthority
import com.goreecloud.launcher.core.workspace.WorkspaceRepository
import kotlinx.coroutines.flow.first

sealed interface WorkspacePostCutoverStartupResult {
    data object NotRoomAuthoritative : WorkspacePostCutoverStartupResult
    data object Ready : WorkspacePostCutoverStartupResult
    data class RecoveryRequired(
        val health: WorkspacePostCutoverHealthResult,
    ) : WorkspacePostCutoverStartupResult
}

/**
 * Evaluates terminal ROOM state at startup without activating Home routing.
 *
 * This coordinator is deliberately read-only. It never demotes terminal ROOM back to DataStore,
 * because DataStore placement may be stale after a future accepted cutover. Production activation
 * remains blocked by the source-level cutover guard until Home routing changes in an explicit PR.
 */
class WorkspacePostCutoverStartupCoordinator(
    private val repository: WorkspaceRepository,
    private val workspaceDaoProvider: () -> WorkspaceDao?,
) {
    suspend fun reconcile(): WorkspacePostCutoverStartupResult {
        val state = repository.state.first()
        if (!state.initialized || state.authority != WorkspaceAuthority.ROOM) {
            return WorkspacePostCutoverStartupResult.NotRoomAuthoritative
        }

        return when (
            val health = WorkspacePostCutoverHealthEvaluator(
                repository = repository,
                workspaceDaoProvider = workspaceDaoProvider,
            ).evaluate()
        ) {
            WorkspacePostCutoverHealthResult.Healthy -> WorkspacePostCutoverStartupResult.Ready
            else -> WorkspacePostCutoverStartupResult.RecoveryRequired(health)
        }
    }
}
