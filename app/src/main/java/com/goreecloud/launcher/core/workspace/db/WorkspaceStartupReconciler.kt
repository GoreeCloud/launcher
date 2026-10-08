package com.goreecloud.launcher.core.workspace.db

import com.goreecloud.launcher.core.workspace.WorkspaceAuthority
import com.goreecloud.launcher.core.workspace.WorkspaceRepository
import com.goreecloud.launcher.core.workspace.WorkspaceState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

sealed interface WorkspaceStartupResult {
    data object WaitingForInitialization : WorkspaceStartupResult
    data object DataStoreOnly : WorkspaceStartupResult
    data object RoomVerifiedMatch : WorkspaceStartupResult
    data object FellBackToDataStore : WorkspaceStartupResult
    data object RoomAuthorityReserved : WorkspaceStartupResult
}

/**
 * Reconciles the pre-cutover workspace on startup and after local workspace mutations.
 *
 * Preferences DataStore remains authoritative. This coordinator may mirror DataStore to Room,
 * verify an independent Room read, or fall back to DataStore. It never promotes ROOM authority.
 */
class WorkspaceStartupReconciler(
    private val repository: WorkspaceRepository,
    private val workspaceDaoProvider: () -> WorkspaceDao?,
) {
    suspend fun reconcile(): WorkspaceStartupResult {
        val state = repository.state.first()
        if (!state.initialized) return WorkspaceStartupResult.WaitingForInitialization
        if (state.authority == WorkspaceAuthority.ROOM) {
            return WorkspaceStartupResult.RoomAuthorityReserved
        }

        val workspaceDao = try {
            workspaceDaoProvider()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            null
        }

        if (workspaceDao == null) {
            return fallbackForUnavailableRoom(state)
        }

        return when (state.authority) {
            WorkspaceAuthority.DATASTORE -> reconcileDataStore(state, workspaceDao)
            WorkspaceAuthority.ROOM_VERIFIED -> reconcileVerifiedRoom(state, workspaceDao)
            WorkspaceAuthority.ROOM -> WorkspaceStartupResult.RoomAuthorityReserved
        }
    }

    private suspend fun reconcileDataStore(
        state: WorkspaceState,
        workspaceDao: WorkspaceDao,
    ): WorkspaceStartupResult = when (WorkspaceRelationalMirror(workspaceDao).sync(state)) {
        WorkspaceMirrorResult.Verified -> {
            if (!repository.markRoomVerified(state)) {
                WorkspaceStartupResult.FellBackToDataStore
            } else {
                val verifiedState = repository.state.first()
                if (verifiedState.authority != WorkspaceAuthority.ROOM_VERIFIED) {
                    WorkspaceStartupResult.FellBackToDataStore
                } else {
                    reconcileVerifiedRoom(verifiedState, workspaceDao)
                }
            }
        }

        WorkspaceMirrorResult.Mismatch,
        is WorkspaceMirrorResult.Failed -> {
            repository.markDataStoreAuthoritative()
            WorkspaceStartupResult.FellBackToDataStore
        }

        WorkspaceMirrorResult.Skipped -> WorkspaceStartupResult.WaitingForInitialization
    }

    private suspend fun reconcileVerifiedRoom(
        state: WorkspaceState,
        workspaceDao: WorkspaceDao,
    ): WorkspaceStartupResult = when (WorkspaceRelationalReader(workspaceDao).reconcile(state)) {
        WorkspaceDualReadResult.Match -> {
            val current = repository.state.first()
            if (
                current.authority == WorkspaceAuthority.ROOM_VERIFIED &&
                current.favoriteKeys == state.favoriteKeys &&
                current.dockKeys == state.dockKeys &&
                current.verifiedRoomFingerprint == state.verifiedRoomFingerprint
            ) {
                WorkspaceStartupResult.RoomVerifiedMatch
            } else {
                WorkspaceStartupResult.FellBackToDataStore
            }
        }

        WorkspaceDualReadResult.Mismatch,
        is WorkspaceDualReadResult.Failed,
        WorkspaceDualReadResult.Skipped -> {
            repository.markDataStoreAuthoritative()
            WorkspaceStartupResult.FellBackToDataStore
        }
    }

    private suspend fun fallbackForUnavailableRoom(state: WorkspaceState): WorkspaceStartupResult {
        return if (state.authority == WorkspaceAuthority.ROOM_VERIFIED) {
            repository.markDataStoreAuthoritative()
            WorkspaceStartupResult.FellBackToDataStore
        } else {
            WorkspaceStartupResult.DataStoreOnly
        }
    }
}
