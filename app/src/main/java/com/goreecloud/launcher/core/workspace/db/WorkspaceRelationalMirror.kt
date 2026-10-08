package com.goreecloud.launcher.core.workspace.db

import com.goreecloud.launcher.core.workspace.WorkspaceState
import kotlinx.coroutines.CancellationException

sealed interface WorkspaceMirrorResult {
    data object Skipped : WorkspaceMirrorResult
    data object Verified : WorkspaceMirrorResult
    data object Mismatch : WorkspaceMirrorResult
    data class Failed(val failureType: String) : WorkspaceMirrorResult
}

class WorkspaceRelationalMirror(
    private val workspaceDao: WorkspaceDao,
) {
    suspend fun sync(state: WorkspaceState): WorkspaceMirrorResult {
        if (!state.initialized) return WorkspaceMirrorResult.Skipped

        return try {
            val import = WorkspaceLegacyImportMapper.map(
                favoriteKeys = state.favoriteKeys,
                dockKeys = state.dockKeys,
            )
            workspaceDao.replaceLegacySnapshot(
                pages = import.pages,
                items = import.items,
            )

            val pageIds = import.pages.map { it.pageId }
            val actualPages = workspaceDao.readPages(pageIds)
            val actualItems = workspaceDao.readItems(pageIds)

            if (WorkspaceRelationalVerifier.matches(import, actualPages, actualItems)) {
                WorkspaceMirrorResult.Verified
            } else {
                WorkspaceMirrorResult.Mismatch
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            WorkspaceMirrorResult.Failed(exception::class.java.simpleName)
        }
    }
}
