package com.goreecloud.launcher.core.launcher

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherPortableRestoreStartupSequenceTest {
    @Test
    fun reconciledRecoveryRunsWorkspaceBeforeCallerCanPublishGateOpenResult() = runBlocking {
        val events = mutableListOf<String>()

        val result = LauncherPortableRestoreStartupSequence.reconcileBeforeMutation(
            recoverPortableRestore = {
                events += "portable-recovery"
                LauncherPortableRestoreRecoveryCoordinator.Result.Clean
            },
            reconcileWorkspace = {
                events += "workspace-reconcile"
            },
        )
        events += "publish-gate-result"

        assertEquals(LauncherPortableRestoreRecoveryCoordinator.Result.Clean, result)
        assertEquals(
            listOf("portable-recovery", "workspace-reconcile", "publish-gate-result"),
            events,
        )
    }

    @Test
    fun recoveryRequiredSkipsWorkspaceReconciliation() = runBlocking {
        var workspaceRan = false
        val expected = LauncherPortableRestoreRecoveryCoordinator.Result.RecoveryRequired(
            LauncherPortableRestoreRecoveryCoordinator.RecoveryReason.STATE_MISMATCH,
        )

        val result = LauncherPortableRestoreStartupSequence.reconcileBeforeMutation(
            recoverPortableRestore = { expected },
            reconcileWorkspace = { workspaceRan = true },
        )

        assertEquals(expected, result)
        assertFalse(workspaceRan)
    }

    @Test
    fun unexpectedRecoveryFailureBecomesExplicitFailClosedStateAndSkipsWorkspace() = runBlocking {
        var workspaceRan = false

        val result = LauncherPortableRestoreStartupSequence.reconcileBeforeMutation(
            recoverPortableRestore = {
                error("portable recovery failed unexpectedly")
            },
            reconcileWorkspace = {
                workspaceRan = true
            },
        )

        assertTrue(result is LauncherPortableRestoreRecoveryCoordinator.Result.RecoveryRequired)
        result as LauncherPortableRestoreRecoveryCoordinator.Result.RecoveryRequired
        assertEquals(
            LauncherPortableRestoreRecoveryCoordinator.RecoveryReason.OPERATION_FAILED,
            result.reason,
        )
        assertEquals(IllegalStateException::class.java.name, result.failureType)
        assertFalse(workspaceRan)
        assertFalse(LauncherPortableRestoreStartupGate.allowsMutations(result))
    }

    @Test
    fun recoveryCancellationStillPropagates() = runBlocking {
        var cancellationObserved = false
        var workspaceRan = false

        try {
            LauncherPortableRestoreStartupSequence.reconcileBeforeMutation(
                recoverPortableRestore = {
                    throw CancellationException("cancel recovery")
                },
                reconcileWorkspace = {
                    workspaceRan = true
                },
            )
        } catch (cancellation: CancellationException) {
            cancellationObserved = cancellation.message == "cancel recovery"
        }

        assertTrue(cancellationObserved)
        assertFalse(workspaceRan)
    }

    @Test
    fun workspaceFailureBecomesExplicitFailClosedRecoveryState() = runBlocking {
        val result = LauncherPortableRestoreStartupSequence.reconcileBeforeMutation(
            recoverPortableRestore = {
                LauncherPortableRestoreRecoveryCoordinator.Result.ConfirmedCommitted
            },
            reconcileWorkspace = {
                error("workspace reconciliation failed")
            },
        )

        assertTrue(result is LauncherPortableRestoreRecoveryCoordinator.Result.RecoveryRequired)
        result as LauncherPortableRestoreRecoveryCoordinator.Result.RecoveryRequired
        assertEquals(
            LauncherPortableRestoreRecoveryCoordinator.RecoveryReason.OPERATION_FAILED,
            result.reason,
        )
        assertEquals(IllegalStateException::class.java.name, result.failureType)
        assertFalse(LauncherPortableRestoreStartupGate.allowsMutations(result))
    }

    @Test
    fun workspaceCancellationStillPropagates() = runBlocking {
        var cancellationObserved = false

        try {
            LauncherPortableRestoreStartupSequence.reconcileBeforeMutation(
                recoverPortableRestore = {
                    LauncherPortableRestoreRecoveryCoordinator.Result.Clean
                },
                reconcileWorkspace = {
                    throw CancellationException("cancel startup")
                },
            )
        } catch (cancellation: CancellationException) {
            cancellationObserved = cancellation.message == "cancel startup"
        }

        assertTrue(cancellationObserved)
    }
}
