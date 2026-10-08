package com.goreecloud.launcher.core.launcher

import com.goreecloud.launcher.core.workspace.db.WorkspaceLegacyImportMapper

internal object LauncherStarterHomeDefaultsPolicy {
    const val DEFAULT_SECONDARY_PAGE_ID = "home:starter:secondary-v3"

    fun shouldRepairEmptyStarter(
        roomAuthoritative: Boolean,
        starterLayoutApplied: Boolean,
        startupWizardCompleted: Boolean,
        hasApps: Boolean,
        hasFavorites: Boolean,
        dockItemCount: Int,
        expectedStarterDockSize: Int,
        pageIds: List<String>,
        allPagesEmpty: Boolean,
    ): Boolean {
        val expectedDockCount = expectedStarterDockSize.coerceIn(4, 6)
        val hasFixturePage = pageIds.any { pageId ->
            pageId.startsWith("home:test:") || pageId.startsWith("home:runtime:")
        }
        return roomAuthoritative &&
            starterLayoutApplied &&
            startupWizardCompleted &&
            hasApps &&
            !hasFavorites &&
            dockItemCount == expectedDockCount &&
            pageIds.firstOrNull() == WorkspaceLegacyImportMapper.HOME_PAGE_ID &&
            !hasFixturePage &&
            allPagesEmpty
    }

    fun excessEmptySecondaryPageIds(pageIds: List<String>): List<String> {
        if (pageIds.firstOrNull() != WorkspaceLegacyImportMapper.HOME_PAGE_ID) return emptyList()
        return pageIds.drop(2)
    }
}
