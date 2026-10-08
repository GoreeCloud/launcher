package com.goreecloud.launcher.core.workspace.db

import com.goreecloud.launcher.core.workspace.WorkspaceAuthority
import com.goreecloud.launcher.core.workspace.WorkspaceRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

sealed interface WorkspaceAuthoritativePlacementState {
    data object WaitingForInitialization : WorkspaceAuthoritativePlacementState
    data class Ready(
        val snapshot: WorkspaceAuthoritativePlacementSnapshot,
    ) : WorkspaceAuthoritativePlacementState
    data class RecoveryRequired(
        val reason: WorkspaceAuthoritativePlacementRecoveryReason,
    ) : WorkspaceAuthoritativePlacementState
}

sealed interface WorkspaceAuthoritativePlacementRecoveryReason {
    data object Unavailable : WorkspaceAuthoritativePlacementRecoveryReason
    data object Mismatch : WorkspaceAuthoritativePlacementRecoveryReason
    data class Failed(val failureType: String) : WorkspaceAuthoritativePlacementRecoveryReason
}

/**
 * Emits the placement state Home would consume after an accepted cutover.
 *
 * DATASTORE and ROOM_VERIFIED remain observable directly from WorkspaceRepository. Terminal ROOM
 * switches to Room's observable pages/items and never falls back to stale DataStore placement.
 * This observer remains intentionally unwired from production Home until a separate activation PR.
 */
class WorkspaceAuthoritativePlacementObserver(
    private val authorityRepository: WorkspaceRepository,
    private val workspaceDaoProvider: () -> WorkspaceDao?,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(): Flow<WorkspaceAuthoritativePlacementState> = authorityRepository.state
        .flatMapLatest { state ->
            if (!state.initialized) {
                return@flatMapLatest flowOf(
                    WorkspaceAuthoritativePlacementState.WaitingForInitialization
                )
            }

            when (state.authority) {
                WorkspaceAuthority.DATASTORE,
                WorkspaceAuthority.ROOM_VERIFIED -> flowOf(
                    WorkspaceAuthoritativePlacementState.Ready(
                        WorkspaceAuthoritativePlacementSnapshot(
                            favoriteKeys = state.favoriteKeys,
                            dockKeys = state.dockKeys,
                            source = WorkspacePlacementSource.DATASTORE,
                        )
                    )
                )
                WorkspaceAuthority.ROOM -> observeRoom()
            }
        }
        .distinctUntilChanged()

    private fun observeRoom(): Flow<WorkspaceAuthoritativePlacementState> {
        val workspaceDao = workspaceDaoOrNull()
            ?: return flowOf(
                WorkspaceAuthoritativePlacementState.RecoveryRequired(
                    WorkspaceAuthoritativePlacementRecoveryReason.Unavailable
                )
            )

        val pageIds = setOf(
            WorkspaceLegacyImportMapper.HOME_PAGE_ID,
            WorkspaceLegacyImportMapper.DOCK_PAGE_ID,
        )

        return combine(
            workspaceDao.observePages(),
            workspaceDao.observeItems(),
        ) { pages, items ->
            val canonical = WorkspaceRelationalReadMapper.map(
                pages = pages.filter { it.pageId in pageIds },
                items = items.filter { it.pageId in pageIds },
            )
            if (canonical == null) {
                WorkspaceAuthoritativePlacementState.RecoveryRequired(
                    WorkspaceAuthoritativePlacementRecoveryReason.Mismatch
                )
            } else {
                WorkspaceAuthoritativePlacementState.Ready(
                    WorkspaceAuthoritativePlacementSnapshot(
                        favoriteKeys = canonical.favoriteKeys,
                        dockKeys = canonical.dockKeys,
                        source = WorkspacePlacementSource.ROOM,
                    )
                )
            }
        }.catch { exception ->
            if (exception is CancellationException) throw exception
            emit(
                WorkspaceAuthoritativePlacementState.RecoveryRequired(
                    WorkspaceAuthoritativePlacementRecoveryReason.Failed(
                        exception::class.java.simpleName
                    )
                )
            )
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
