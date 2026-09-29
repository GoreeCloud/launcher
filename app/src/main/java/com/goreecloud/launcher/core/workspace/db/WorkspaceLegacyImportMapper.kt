package com.goreecloud.launcher.core.workspace.db

data class LegacyWorkspaceImport(
    val pages: List<WorkspacePageEntity>,
    val items: List<WorkspaceItemEntity>,
)

object WorkspaceLegacyImportMapper {
    const val HOME_PAGE_ID = "home:0"
    const val DOCK_PAGE_ID = "dock:0"

    fun map(
        favoriteKeys: List<String>,
        dockKeys: List<String>,
    ): LegacyWorkspaceImport {
        val pages = listOf(
            WorkspacePageEntity(
                pageId = HOME_PAGE_ID,
                containerType = WorkspaceContainerType.HOME,
                rank = 0,
            ),
            WorkspacePageEntity(
                pageId = DOCK_PAGE_ID,
                containerType = WorkspaceContainerType.DOCK,
                rank = 0,
            ),
        )

        val favoriteItems = favoriteKeys.distinct().mapIndexed { rank, appKey ->
            WorkspaceItemEntity(
                itemId = "legacy:home:$appKey",
                pageId = HOME_PAGE_ID,
                itemType = WorkspaceItemType.APP,
                appKey = appKey,
                rank = rank,
                cellX = null,
                cellY = null,
            )
        }
        val dockItems = dockKeys.distinct().mapIndexed { rank, appKey ->
            WorkspaceItemEntity(
                itemId = "legacy:dock:$appKey",
                pageId = DOCK_PAGE_ID,
                itemType = WorkspaceItemType.APP,
                appKey = appKey,
                rank = rank,
                cellX = null,
                cellY = null,
            )
        }

        return LegacyWorkspaceImport(
            pages = pages,
            items = favoriteItems + dockItems,
        )
    }
}
