package com.goreecloud.launcher.core.workspace.db

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkspaceHomePageContextTest {
    @Test
    fun compactLabelSummarizesAppsOnly() {
        val page = WorkspaceRenderedHomePage(
            pageId = "home-1",
            rank = 0,
            appKeys = listOf("a", "b"),
            unsupportedItemCount = 0,
        )

        assertEquals("2 apps", page.context().compactLabel)
    }

    @Test
    fun compactLabelKeepsUnsupportedItemsVisible() {
        val page = WorkspaceRenderedHomePage(
            pageId = "home-2",
            rank = 1,
            appKeys = listOf("a"),
            unsupportedItemCount = 2,
        )

        assertEquals("1 app · 2 other", page.context().compactLabel)
    }

    @Test
    fun moveTargetLabelIncludesPageNumberAndCurrentContext() {
        val context = WorkspaceHomePageContext(appCount = 3, unsupportedItemCount = 1)

        assertEquals("Page 4 · 3 apps · 1 other", context.moveTargetLabel(pageNumber = 4))
    }

    @Test
    fun moveTargetLabelIdentifiesPrimaryHomeWithoutLosingContext() {
        val context = WorkspaceHomePageContext(appCount = 2, unsupportedItemCount = 1)

        assertEquals(
            "Primary Home · 2 apps · 1 other",
            context.moveTargetLabel(pageNumber = 1, primary = true),
        )
    }

    @Test
    fun accessibilityLabelAnnouncesPageContextAndSelection() {
        val context = WorkspaceHomePageContext(appCount = 1, unsupportedItemCount = 2)

        assertEquals(
            "Page 3, 1 app, 2 other workspace items, selected",
            context.switcherAccessibilityLabel(pageNumber = 3, selected = true),
        )
        assertEquals(
            "Page 3, 1 app, 2 other workspace items, not selected",
            context.switcherAccessibilityLabel(pageNumber = 3, selected = false),
        )
    }
}
