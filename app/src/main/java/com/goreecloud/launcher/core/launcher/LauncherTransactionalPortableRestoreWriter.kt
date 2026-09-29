package com.goreecloud.launcher.core.launcher

import android.content.Context
import com.goreecloud.launcher.core.workspace.WorkspacePortableSnapshot
import com.goreecloud.launcher.core.workspace.db.LauncherDatabaseProvider
import com.goreecloud.launcher.core.workspace.db.WorkspaceDao
import com.goreecloud.launcher.core.workspace.db.WorkspacePortableHomeStateFingerprint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Concrete Development persistence authority for the currently approved Launcher portability pair.
 *
 * Room and Preferences DataStore cannot share one transaction. This writer therefore records a
 * strict device-local recovery journal before the Room mutation, applies an exact precomputed Room
 * plan, then atomically writes target portable preferences and clears the matching journal. A later
 * process can reconcile the bounded interrupted operation without guessing which store committed.
 *
 * This remains same-resolved-identity recovery. It does not establish Everkeep recovery,
 * cross-device rebinding, provenance, or complete clean-target reconstruction.
 */
class LauncherTransactionalPortableRestoreWriter(
    private val workspaceDao: WorkspaceDao,
    private val preferencesRepository: LauncherPreferencesRepository,
) : LauncherPortableRestoreWriter {

    constructor(context: Context) : this(
        workspaceDao = LauncherDatabaseProvider.get(context).workspaceDao(),
        preferencesRepository = LauncherPreferencesRepository(context.applicationContext),
    )

    override suspend fun replacePortableState(
        workspace: WorkspacePortableSnapshot.Snapshot,
        preferences: LauncherPreferences,
    ) {
        // Defend the concrete persistence seam as well as the higher-level import coordinator.
        WorkspacePortableSnapshot.encode(workspace)
        LauncherPortablePreferences.encode(preferences)
        check(
            workspace.grid.columns == preferences.homeColumns &&
                workspace.grid.rows == preferences.homeRows
        ) {
            "portable workspace grid must match the portable Home-grid preferences"
        }

        when (
            val recovery = LauncherPortableRestoreRecoveryCoordinator(
                workspaceDao = workspaceDao,
                preferencesRepository = preferencesRepository,
            ).reconcile()
        ) {
            is LauncherPortableRestoreRecoveryCoordinator.Result.RecoveryRequired -> {
                throw LauncherPortableRestoreRecoveryRequiredException(
                    "portable Launcher restore cannot start while prior recovery remains unresolved: ${recovery.reason}",
                )
            }
            else -> Unit
        }

        val previousPreferences = preferencesRepository.readPortablePreferences()
        val roomCommit = workspaceDao.planPortableHomePlacements(workspace)
        val journal = LauncherPortableRestoreJournal(
            transactionId = UUID.randomUUID().toString(),
            previousWorkspaceFingerprint = WorkspacePortableHomeStateFingerprint.of(
                roomCommit.previousPages,
                roomCommit.previousItems,
            ),
            appliedWorkspaceFingerprint = WorkspacePortableHomeStateFingerprint.of(
                roomCommit.appliedPages,
                roomCommit.appliedItems,
            ),
            previousPreferences = previousPreferences,
            targetPreferences = preferences,
        )

        check(preferencesRepository.beginPortableRestoreJournal(journal)) {
            "portable Launcher restore cannot start because a recovery journal already exists"
        }

        var workspaceApplied = false
        try {
            workspaceDao.applyPortableHomeRestoreCommit(roomCommit)
            workspaceApplied = true

            check(preferencesRepository.finalizePortableRestoreJournal(journal)) {
                "portable Launcher preference finalization refused because journal or preferences changed"
            }
            check(preferencesRepository.readPortablePreferences() == preferences) {
                "portable Launcher preference readback verification failed"
            }
            check(
                preferencesRepository.readPortableRestoreJournal() ==
                    LauncherPortableRestoreJournalReadResult.Absent
            ) {
                "portable Launcher restore journal remained after successful finalization"
            }
        } catch (applyFailure: Throwable) {
            finishPortableRestoreFailure(
                applyFailure = applyFailure,
                rollbackWorkspace = {
                    if (workspaceApplied) {
                        workspaceDao.rollbackPortableHomePlacements(roomCommit)
                    }
                },
                rollbackPreferences = {
                    preferencesRepository.rollbackPortablePreferencesAfterFailedApply(
                        expectedApplied = preferences,
                        previous = previousPreferences,
                    )
                },
                clearJournal = {
                    preferencesRepository.clearPortableRestoreJournalIfMatches(journal)
                },
            )
        }
    }
}

/**
 * Complete compensating rollback even when the apply coroutine has already been cancelled.
 *
 * Cancellation can arrive after the Room commit but before the DataStore stage finishes. Running
 * rollback in the cancelled context would let the first suspension abort compensation and strand a
 * split cross-store state. NonCancellable is intentionally scoped only to the bounded rollback.
 * The durable journal is cleared only after both workspace and preference rollback are verified; a
 * rollback failure deliberately leaves recovery evidence in place for later reconciliation.
 */
internal suspend fun finishPortableRestoreFailure(
    applyFailure: Throwable,
    rollbackWorkspace: suspend () -> Unit,
    rollbackPreferences: suspend () -> Boolean,
    clearJournal: suspend () -> Boolean = { true },
): Nothing {
    val rollbackFailures = withContext(NonCancellable) {
        buildList {
            try {
                rollbackWorkspace()
            } catch (rollbackFailure: Throwable) {
                add(rollbackFailure)
            }

            try {
                check(rollbackPreferences()) {
                    "portable preference rollback refused because preferences changed after restore apply"
                }
            } catch (rollbackFailure: Throwable) {
                add(rollbackFailure)
            }

            // Preserve the journal when either rollback failed; it is the durable recovery evidence.
            if (isEmpty()) {
                try {
                    check(clearJournal()) {
                        "portable restore journal cleanup refused because journal state changed"
                    }
                } catch (rollbackFailure: Throwable) {
                    add(rollbackFailure)
                }
            }
        }
    }

    if (rollbackFailures.isNotEmpty()) {
        val failure = LauncherPortableRestoreRollbackException(
            message = "portable Launcher restore failed and compensating rollback could not be fully verified",
            cause = applyFailure,
        )
        rollbackFailures.forEach(failure::addSuppressed)
        throw failure
    }

    if (applyFailure is CancellationException) {
        throw applyFailure
    }

    throw LauncherPortableRestoreApplyException(
        message = "portable Launcher restore failed; the previously persisted portable state was restored",
        cause = applyFailure,
    )
}

class LauncherPortableRestoreApplyException(
    message: String,
    cause: Throwable,
) : IllegalStateException(message, cause)

class LauncherPortableRestoreRollbackException(
    message: String,
    cause: Throwable,
) : IllegalStateException(message, cause)

class LauncherPortableRestoreRecoveryRequiredException(
    message: String,
) : IllegalStateException(message)
