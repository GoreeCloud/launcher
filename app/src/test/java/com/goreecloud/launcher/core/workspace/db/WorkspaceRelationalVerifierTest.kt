package com.goreecloud.launcher.core.workspace.db

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceRelationalVerifierTest {
    private val expected = WorkspaceLegacyImportMapper.map(
        favoriteKeys = listOf("profile:one", "profile:two"),
        dockKeys = listOf("profile:two", "profile:three"),
    )

    @Test
    fun matchesAcceptsEquivalentRowsInDifferentDaoOrder() {
        assertTrue(
            WorkspaceRelationalVerifier.matches(
                expected = expected,
                actualPages = expected.pages.reversed(),
                actualItems = expected.items.reversed(),
            )
        )
    }

    @Test
    fun matchesRejectsMissingOrExtraRows() {
        assertFalse(
            WorkspaceRelationalVerifier.matches(
                expected = expected,
                actualPages = expected.pages.dropLast(1),
                actualItems = expected.items,
            )
        )
        assertFalse(
            WorkspaceRelationalVerifier.matches(
                expected = expected,
                actualPages = expected.pages,
                actualItems = expected.items.dropLast(1),
            )
        )
        assertFalse(
            WorkspaceRelationalVerifier.matches(
                expected = expected,
                actualPages = expected.pages + WorkspacePageEntity("home:extra", WorkspaceContainerType.HOME, 1),
                actualItems = expected.items,
            )
        )
    }

    @Test
    fun matchesRejectsOrderingOrPlacementChanges() {
        val rankChanged = expected.items.toMutableList().apply {
            this[0] = this[0].copy(rank = this[0].rank + 10)
        }
        assertFalse(
            WorkspaceRelationalVerifier.matches(expected, expected.pages, rankChanged)
        )

        val placementChanged = expected.items.toMutableList().apply {
            this[0] = this[0].copy(cellX = 4, cellY = 2, spanX = 2, spanY = 3)
        }
        assertFalse(
            WorkspaceRelationalVerifier.matches(expected, expected.pages, placementChanged)
        )
    }

    @Test
    fun matchesRejectsIdentityAndTypeChanges() {
        val appKeyChanged = expected.items.toMutableList().apply {
            this[0] = this[0].copy(appKey = "profile:replacement")
        }
        assertFalse(
            WorkspaceRelationalVerifier.matches(expected, expected.pages, appKeyChanged)
        )

        val typeChanged = expected.items.toMutableList().apply {
            this[0] = this[0].copy(itemType = WorkspaceItemType.FOLDER)
        }
        assertFalse(
            WorkspaceRelationalVerifier.matches(expected, expected.pages, typeChanged)
        )
    }
}
