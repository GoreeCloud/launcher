package com.goreecloud.launcher.core.workspace.db

internal object WorkspaceRelationalVerifier {
    private val pageComparator = compareBy<WorkspacePageEntity>(
        { it.containerType },
        { it.rank },
        { it.pageId },
    )

    private val itemComparator = compareBy<WorkspaceItemEntity>(
        { it.pageId },
        { it.rank },
        { it.itemId },
    )

    fun matches(
        expected: LegacyWorkspaceImport,
        actualPages: List<WorkspacePageEntity>,
        actualItems: List<WorkspaceItemEntity>,
    ): Boolean =
        expected.pages.sortedWith(pageComparator) == actualPages.sortedWith(pageComparator) &&
            expected.items.sortedWith(itemComparator) == actualItems.sortedWith(itemComparator)
}
