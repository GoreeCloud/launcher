package com.goreecloud.launcher.core.launcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherPortableRestoreStartupGateTest {
    @Test
    fun pendingAndRecoveryRequiredStatesBlockMutations() {
        assertFalse(LauncherPortableRestoreStartupGate.allowsMutations(null))
        assertFalse(
            LauncherPortableRestoreStartupGate.allowsMutations(
                LauncherPortableRestoreRecoveryCoordinator.Result.RecoveryRequired(
                    reason = LauncherPortableRestoreRecoveryCoordinator.RecoveryReason.STATE_MISMATCH,
                ),
            ),
        )
    }

    @Test
    fun everyReconciledOutcomeAllowsNormalMutation() {
        val reconciled = listOf(
            LauncherPortableRestoreRecoveryCoordinator.Result.Clean,
            LauncherPortableRestoreRecoveryCoordinator.Result.AbandonedBeforeWorkspaceApply,
            LauncherPortableRestoreRecoveryCoordinator.Result.FinalizedAfterWorkspaceApply,
            LauncherPortableRestoreRecoveryCoordinator.Result.ConfirmedCommitted,
        )

        reconciled.forEach { result ->
            assertTrue(LauncherPortableRestoreStartupGate.allowsMutations(result))
        }
    }
}
