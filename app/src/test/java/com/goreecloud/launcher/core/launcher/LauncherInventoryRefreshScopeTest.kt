package com.goreecloud.launcher.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherInventoryRefreshScopeTest {
    @Test
    fun packageRemovalUsesPackageScopedRefresh() {
        assertEquals(
            LauncherInventoryRefreshScope.PACKAGE,
            launcherInventoryRefreshScope(LauncherInventoryChange.PACKAGE_REMOVED),
        )
    }

    @Test
    fun additionsChangesAvailabilityAndProfileTopologyRequireFullRefresh() {
        val fullChanges = listOf(
            LauncherInventoryChange.PACKAGE_ADDED,
            LauncherInventoryChange.PACKAGE_CHANGED,
            LauncherInventoryChange.PACKAGE_SUSPENDED,
            LauncherInventoryChange.PACKAGE_UNSUSPENDED,
            LauncherInventoryChange.PACKAGES_AVAILABLE,
            LauncherInventoryChange.PACKAGES_UNAVAILABLE,
            LauncherInventoryChange.PROFILE_TOPOLOGY,
        )

        fullChanges.forEach { change ->
            assertEquals(
                LauncherInventoryRefreshScope.FULL,
                launcherInventoryRefreshScope(change),
            )
        }
    }

    @Test
    fun drawerProfilePagesKeepPrimaryInventoryFirstAndOrdered() {
        data class Entry(val label: String, val user: String)

        val pages = launcherDrawerProfilePages(
            items = listOf(
                Entry("Camera", "primary"),
                Entry("Files", "work"),
                Entry("Memos", "primary"),
                Entry("Notes", "work"),
            ),
            primaryUser = "primary",
            userOf = Entry::user,
        )

        assertEquals(
            listOf(LauncherDrawerProfileKind.USER, LauncherDrawerProfileKind.WORK),
            pages.map { page -> page.kind },
        )
        assertEquals(listOf("Camera", "Memos"), pages[0].items.map(Entry::label))
        assertEquals(listOf("Files", "Notes"), pages[1].items.map(Entry::label))
    }

    @Test
    fun drawerProfilePagesDoNotExposeEmptyWorkPage() {
        val pages = launcherDrawerProfilePages(
            items = listOf("Camera", "Memos"),
            primaryUser = "primary",
            userOf = { "primary" },
        )

        assertEquals(1, pages.size)
        assertEquals(LauncherDrawerProfileKind.USER, pages.single().kind)
        assertEquals(listOf("Camera", "Memos"), pages.single().items)
    }

    @Test
    fun drawerProfilePagesGroupAllSecondaryProfilesIntoInitialWorkPage() {
        data class Entry(val label: String, val user: String)

        val pages = launcherDrawerProfilePages(
            items = listOf(
                Entry("Personal", "primary"),
                Entry("Shelter", "work-one"),
                Entry("Secondary", "work-two"),
            ),
            primaryUser = "primary",
            userOf = Entry::user,
        )

        assertEquals(
            listOf("Shelter", "Secondary"),
            pages.single { page -> page.kind == LauncherDrawerProfileKind.WORK }
                .items
                .map(Entry::label),
        )
    }
}
