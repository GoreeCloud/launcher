package com.goreecloud.launcher.core.launcher

import kotlinx.coroutines.CancellationException

/**
 * Orders the bounded portable-restore recovery check ahead of deferred workspace reconciliation.
 *
 * MainActivity must not publish a gate-opening recovery result until this sequence returns. If
 * portable recovery requires attention, workspace reconciliation is skipped. Unexpected recovery
 * or workspace-reconciliation failures are converted to an explicit fail-closed RecoveryRequired
 * result so the startup surface can report that recovery needs attention instead of remaining
 * indefinitely in the pending state. Coroutine cancellation still propagates and is never
 * converted into application recovery evidence.
 */
object LauncherPortableRestoreStartupSequence {
    suspend fun reconcileBeforeMutation(
        recoverPortableRestore: suspend () -> LauncherPortableRestoreRecoveryCoordinator.Result,
        reconcileWorkspace: suspend () -> Unit,
    ): LauncherPortableRestoreRecoveryCoordinator.Result {
        val recovery = try {
            recoverPortableRestore()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            return operationFailure(failure)
        }

        if (!LauncherPortableRestoreStartupGate.allowsMutations(recovery)) {
            return recovery
        }

        try {
            reconcileWorkspace()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            return operationFailure(failure)
        }
        return recovery
    }

    private fun operationFailure(
        failure: Exception,
    ): LauncherPortableRestoreRecoveryCoordinator.Result.RecoveryRequired =
        LauncherPortableRestoreRecoveryCoordinator.Result.RecoveryRequired(
            reason = LauncherPortableRestoreRecoveryCoordinator.RecoveryReason.OPERATION_FAILED,
            failureType = failure::class.java.name,
        )
}
