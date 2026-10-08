package com.goreecloud.launcher.core.launcher

import android.content.Context
import com.goreecloud.launcher.core.workspace.db.LauncherDatabaseProvider
import com.goreecloud.launcher.core.workspace.db.WorkspaceDao
import com.goreecloud.launcher.core.workspace.db.WorkspacePortableHomeStateFingerprint
import kotlinx.coroutines.CancellationException

/**
 * Reconciles one interrupted device-local bounded portable restore from its durable DataStore
 * journal and exact current Room state.
 *
 * Callers must run this before allowing new workspace/preference mutations. This Development seam
 * does not itself install a production startup gate, does not infer provenance, and does not widen
 * the existing same-resolved-identity restore into clean-device or cross-device recovery.
 */
class LauncherPortableRestoreRecoveryCoordinator(
    private val workspaceDao: WorkspaceDao,
    private val preferencesRepository: LauncherPreferencesRepository,
) {
    constructor(context: Context) : this(
        workspaceDao = LauncherDatabaseProvider.get(context).workspaceDao(),
        preferencesRepository = LauncherPreferencesRepository(context.applicationContext),
    )

    enum class RecoveryReason {
        JOURNAL_INVALID,
        PREFERENCES_INVALID,
        STATE_MISMATCH,
        OPERATION_FAILED,
    }

    sealed interface Result {
        data object Clean : Result
        data object AbandonedBeforeWorkspaceApply : Result
        data object FinalizedAfterWorkspaceApply : Result
        data object ConfirmedCommitted : Result
        data class RecoveryRequired(
            val reason: RecoveryReason,
            val failureType: String? = null,
        ) : Result
    }

    suspend fun reconcile(): Result = try {
        when (val journalRead = preferencesRepository.readPortableRestoreJournal()) {
            LauncherPortableRestoreJournalReadResult.Absent -> Result.Clean
            is LauncherPortableRestoreJournalReadResult.Invalid ->
                Result.RecoveryRequired(RecoveryReason.JOURNAL_INVALID)
            is LauncherPortableRestoreJournalReadResult.Present -> reconcile(journalRead.journal)
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Exception) {
        Result.RecoveryRequired(
            reason = RecoveryReason.OPERATION_FAILED,
            failureType = failure::class.java.name,
        )
    }

    private suspend fun reconcile(journal: LauncherPortableRestoreJournal): Result {
        val workspace = workspaceDao.readPortableHomeState()
        val workspaceFingerprint = WorkspacePortableHomeStateFingerprint.of(
            pages = workspace.pages,
            items = workspace.items,
        )
        val preferences = when (val read = preferencesRepository.readPortablePreferencesForRecovery()) {
            is LauncherPortableRecoveryPreferenceReadResult.Success -> read.preferences
            is LauncherPortableRecoveryPreferenceReadResult.Invalid ->
                return Result.RecoveryRequired(RecoveryReason.PREFERENCES_INVALID)
        }

        return when {
            workspaceFingerprint == journal.previousWorkspaceFingerprint &&
                preferences == journal.previousPreferences -> {
                if (!preferencesRepository.clearPortableRestoreJournalIfMatches(journal)) {
                    return Result.RecoveryRequired(RecoveryReason.STATE_MISMATCH)
                }
                if (!matchesExactState(journal.previousWorkspaceFingerprint, journal.previousPreferences)) {
                    Result.RecoveryRequired(RecoveryReason.STATE_MISMATCH)
                } else {
                    Result.AbandonedBeforeWorkspaceApply
                }
            }

            workspaceFingerprint == journal.appliedWorkspaceFingerprint &&
                preferences == journal.previousPreferences -> {
                if (!preferencesRepository.finalizePortableRestoreJournal(journal)) {
                    return Result.RecoveryRequired(RecoveryReason.STATE_MISMATCH)
                }
                if (!matchesExactState(journal.appliedWorkspaceFingerprint, journal.targetPreferences)) {
                    Result.RecoveryRequired(RecoveryReason.STATE_MISMATCH)
                } else {
                    Result.FinalizedAfterWorkspaceApply
                }
            }

            workspaceFingerprint == journal.appliedWorkspaceFingerprint &&
                preferences == journal.targetPreferences -> {
                if (!preferencesRepository.clearPortableRestoreJournalIfMatches(journal)) {
                    return Result.RecoveryRequired(RecoveryReason.STATE_MISMATCH)
                }
                if (!matchesExactState(journal.appliedWorkspaceFingerprint, journal.targetPreferences)) {
                    Result.RecoveryRequired(RecoveryReason.STATE_MISMATCH)
                } else {
                    Result.ConfirmedCommitted
                }
            }

            else -> Result.RecoveryRequired(RecoveryReason.STATE_MISMATCH)
        }
    }

    private suspend fun matchesExactState(
        expectedWorkspaceFingerprint: String,
        expectedPreferences: LauncherPreferences,
    ): Boolean {
        val workspace = workspaceDao.readPortableHomeState()
        val fingerprint = WorkspacePortableHomeStateFingerprint.of(workspace.pages, workspace.items)
        if (fingerprint != expectedWorkspaceFingerprint) return false
        val preferences = when (val read = preferencesRepository.readPortablePreferencesForRecovery()) {
            is LauncherPortableRecoveryPreferenceReadResult.Success -> read.preferences
            is LauncherPortableRecoveryPreferenceReadResult.Invalid -> return false
        }
        if (preferences != expectedPreferences) return false
        return preferencesRepository.readPortableRestoreJournal() ==
            LauncherPortableRestoreJournalReadResult.Absent
    }
}
