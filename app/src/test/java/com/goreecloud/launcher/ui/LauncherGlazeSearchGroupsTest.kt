package com.goreecloud.launcher.ui

import com.goreecloud.launcher.core.launcher.LauncherSearchCategory
import com.goreecloud.launcher.core.launcher.LauncherSearchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Glaze Search presentation must not alter local provider results, rankings within
 * a category or the user's explicit connected-provider privacy boundary.
 */
class LauncherGlazeSearchGroupsTest {
    @Test
    fun groupsAppsFirstThenLocalCategoriesAndPreservesProviderOrderWithinCategory() {
        fun result(category: LauncherSearchCategory, id: String) = LauncherSearchResult(
            providerId = "local.test",
            resultId = id,
            title = id,
            subtitle = null,
            category = category,
            score = 10,
        )
        val input = listOf(
            result(LauncherSearchCategory.CONTACT, "person-a"),
            result(LauncherSearchCategory.FILE, "file-a"),
            result(LauncherSearchCategory.APPLICATION, "app-a"),
            result(LauncherSearchCategory.CONTACT, "person-b"),
            result(LauncherSearchCategory.APPLICATION, "app-b"),
        )

        val groups = LauncherGlazeSearchGroups.group(input)

        assertEquals(
            listOf(
                LauncherSearchCategory.APPLICATION,
                LauncherSearchCategory.CONTACT,
                LauncherSearchCategory.FILE,
            ),
            groups.map { it.category },
        )
        assertEquals(listOf("app-a", "app-b"), groups[0].items.map { it.resultId })
        assertEquals(listOf("person-a", "person-b"), groups[1].items.map { it.resultId })
        assertEquals(input.toSet(), groups.flatMap { it.items }.toSet())
    }

    @Test
    fun blankResultSetShowsNoSyntheticAppsSuggestionsOrConnectedResults() {
        assertTrue(LauncherGlazeSearchGroups.group(emptyList()).isEmpty())
    }
}
