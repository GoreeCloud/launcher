package com.goreecloud.launcher.core.launcher

/**
 * Pure startup decision for the durable portable-restore recovery coordinator.
 *
 * A null result means recovery has not finished. RecoveryRequired must keep workspace and
 * preference mutation surfaces closed. All other coordinator results have either found no
 * interrupted restore or reconciled its bounded journal/state before normal launcher mutation.
 */
object LauncherPortableRestoreStartupGate {
    fun allowsMutations(result: LauncherPortableRestoreRecoveryCoordinator.Result?): Boolean =
        when (result) {
            LauncherPortableRestoreRecoveryCoordinator.Result.Clean,
            LauncherPortableRestoreRecoveryCoordinator.Result.AbandonedBeforeWorkspaceApply,
            LauncherPortableRestoreRecoveryCoordinator.Result.FinalizedAfterWorkspaceApply,
            LauncherPortableRestoreRecoveryCoordinator.Result.ConfirmedCommitted -> true
            null,
            is LauncherPortableRestoreRecoveryCoordinator.Result.RecoveryRequired -> false
        }

    fun userMessage(result: LauncherPortableRestoreRecoveryCoordinator.Result?): String =
        when (result) {
            null -> "Checking launcher recovery state…"
            is LauncherPortableRestoreRecoveryCoordinator.Result.RecoveryRequired ->
                "Launcher recovery needs attention before workspace changes can continue."
            else -> "Launcher recovery check complete."
        }
}
