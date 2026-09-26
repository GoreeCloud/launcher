package com.goreecloud.launcher.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class StarterWorkspacePolicyTest {
    private fun candidate(key: String, label: String, pkg: String = "example.$key") =
        StarterWorkspaceCandidate(key = key, label = label, packageName = pkg)

    @Test
    fun selectsIntentionalDockAndFavoritesWithoutLauncherPackages() {
        val selection = StarterWorkspacePolicy.select(
            listOf(
                candidate("launcher", "GoreeCloud Launcher", "com.goreecloud.launcher.dev"),
                candidate("phone", "Phone"),
                candidate("messages", "GoreeCloud Messenger"),
                candidate("mail", "GoreeCloud Mail"),
                candidate("browser", "GoreeCloud Browser"),
                candidate("camera", "Camera"),
                candidate("calendar", "Calendar"),
                candidate("clock", "Clock"),
                candidate("contacts", "Contacts"),
                candidate("gallery", "GoreeCloud Gallery"),
                candidate("memos", "GoreeCloud Memos"),
                candidate("files", "Files"),
                candidate("drive", "Drive"),
                candidate("music", "Music"),
            ),
        )

        assertEquals(
            listOf("phone", "messages", "mail", "browser", "camera"),
            selection.dockKeys,
        )
        assertEquals(
            listOf("calendar", "clock", "contacts", "gallery", "memos", "files", "drive", "music"),
            selection.favoriteKeys,
        )
        assertFalse(selection.dockKeys.contains("launcher"))
        assertFalse(selection.favoriteKeys.contains("launcher"))
    }

    @Test
    fun localLaunchCountsRankFavoritesWithoutDisplacingPreferredDockRoles() {
        val selection = StarterWorkspacePolicy.select(
            listOf(
                candidate("phone", "Phone"),
                candidate("messages", "Messages"),
                candidate("mail", "Mail"),
                candidate("browser", "Browser"),
                candidate("camera", "Camera"),
                candidate("alpha", "Alpha").copy(localLaunchCount = 4),
                candidate("beta", "Beta").copy(localLaunchCount = 20),
                candidate("gamma", "Gamma").copy(localLaunchCount = 9),
                candidate("calendar", "Calendar"),
            ),
            maxFavorites = 4,
        )

        assertEquals(
            listOf("phone", "messages", "mail", "browser", "camera"),
            selection.dockKeys,
        )
        assertEquals(
            listOf("beta", "gamma", "alpha", "calendar"),
            selection.favoriteKeys,
        )
    }

    @Test
    fun recencyOrderingWinsOverFrequencyForDefaultHomeFavorites() {
        val selection = StarterWorkspacePolicy.select(
            listOf(
                candidate("phone", "Phone"),
                candidate("messages", "Messages"),
                candidate("mail", "Mail"),
                candidate("browser", "Browser"),
                candidate("camera", "Camera"),
                candidate("newest", "Newest").copy(localLaunchCount = 1, localRecencyRank = 0),
                candidate("middle", "Middle").copy(localLaunchCount = 2, localRecencyRank = 1),
                candidate("older", "Older").copy(localLaunchCount = 100, localRecencyRank = 2),
                candidate("calendar", "Calendar"),
            ),
            maxFavorites = 3,
        )

        assertEquals(
            listOf("newest", "middle", "older"),
            selection.favoriteKeys,
        )
    }

    @Test
    fun homeSuggestionsPreferLaunchOrderWithoutDockDuplicates() {
        assertEquals(
            listOf("new", "older", "savedA", "savedB"),
            LauncherHomeSuggestionsPolicy.selectKeys(
                recentAppKeys = listOf("dock", "new", "older", "new"),
                savedFavoriteKeys = listOf("savedA", "dock", "savedB"),
                dockKeys = listOf("dock"),
                limit = 4,
            ),
        )
    }

    @Test
    fun homeSuggestionsUseDefaultTenItemBound() {
        assertEquals(
            (0 until 10).map { "item-$it" },
            LauncherHomeSuggestionsPolicy.selectKeys(
                recentAppKeys = (0 until 14).map { "item-$it" },
                savedFavoriteKeys = emptyList(),
                dockKeys = emptyList(),
            ),
        )
    }

    @Test
    fun tenStarterAppsOccupyBottomTwoRowsOfFiveBySixHome() {
        assertEquals(
            listOf(
                0 to 4, 1 to 4, 2 to 4, 3 to 4, 4 to 4,
                0 to 5, 1 to 5, 2 to 5, 3 to 5, 4 to 5,
            ),
            StarterWorkspacePolicy.homeCells(itemCount = 10, columns = 5, rows = 6),
        )
    }

    @Test
    fun fillsSparseDevicesDeterministicallyWithoutDuplicates() {
        val selection = StarterWorkspacePolicy.select(
            listOf(
                candidate("a", "Alpha"),
                candidate("b", "Beta"),
                candidate("c", "Camera"),
                candidate("d", "Delta"),
                candidate("e", "Echo"),
            ),
            maxFavorites = 3,
            maxDock = 3,
        )

        assertEquals(3, selection.dockKeys.size)
        assertEquals(2, selection.favoriteKeys.size)
        assertEquals(5, (selection.dockKeys + selection.favoriteKeys).distinct().size)
    }
}
