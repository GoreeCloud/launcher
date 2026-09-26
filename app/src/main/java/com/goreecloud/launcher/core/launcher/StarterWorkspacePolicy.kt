package com.goreecloud.launcher.core.launcher

data class StarterWorkspaceCandidate(
    val key: String,
    val label: String,
    val packageName: String,
    val localLaunchCount: Long = 0L,
    val localRecencyRank: Int? = null,
)

data class StarterWorkspaceSelection(
    val favoriteKeys: List<String>,
    val dockKeys: List<String>,
)

/**
 * Builds the live default Home suggestion order without mutating persisted workspace placement.
 *
 * Recent Launcher launches lead, Dock apps are excluded to avoid duplicate presentation, and saved
 * Home favorites fill any remaining slots. The caller decides when suggestion mode is appropriate;
 * manual Home editing can disable it so user placement remains authoritative.
 */
object LauncherHomeSuggestionsPolicy {
    fun selectKeys(
        recentAppKeys: List<String>,
        savedFavoriteKeys: List<String>,
        dockKeys: List<String>,
        limit: Int = 10,
    ): List<String> {
        if (limit <= 0) return emptyList()
        val dock = dockKeys.toSet()
        return buildList {
            (recentAppKeys + savedFavoriteKeys).forEach { key ->
                if (size >= limit) return@buildList
                if (key.isBlank() || key in dock || key in this) return@forEach
                add(key)
            }
        }
    }
}

/**
 * Picks an intentional first-run launcher layout without pretending to know the user's final
 * preferences. The policy favors common phone tasks and GoreeCloud-branded apps, never launcher
 * packages, and is only applied once to a completely empty Development workspace.
 */
object StarterWorkspacePolicy {
    private val dockPriorityGroups = listOf(
        listOf("phone", "dialer"),
        listOf("messages", "messaging", "messenger"),
        listOf("email", "mail"),
        listOf("browser", "chrome", "firefox", "internet"),
        listOf("camera"),
    )

    private val favoritePriorityGroups = listOf(
        listOf("calendar"),
        listOf("clock"),
        listOf("contacts"),
        listOf("gallery", "photos"),
        listOf("memos", "notes"),
        listOf("files", "file manager"),
        listOf("drive"),
        listOf("music"),
        listOf("app store", "store"),
    )

    fun select(
        candidates: List<StarterWorkspaceCandidate>,
        maxFavorites: Int = 10,
        maxDock: Int = 5,
    ): StarterWorkspaceSelection {
        val usable = candidates
            .filterNot { candidate ->
                candidate.packageName.contains("launcher", ignoreCase = true) ||
                    candidate.label.contains("launcher", ignoreCase = true)
            }
            .sortedWith(
                compareByDescending<StarterWorkspaceCandidate> {
                    it.label.contains("goreecloud", ignoreCase = true)
                }.thenBy { it.label.lowercase() },
            )

        val used = linkedSetOf<String>()

        fun pick(groups: List<List<String>>, limit: Int): List<String> {
            val result = mutableListOf<String>()
            for (group in groups) {
                if (result.size >= limit) break
                val match = usable.firstOrNull { candidate ->
                    candidate.key !in used &&
                        group.any { keyword ->
                            candidate.label.contains(keyword, ignoreCase = true) ||
                                candidate.packageName.contains(keyword, ignoreCase = true)
                        }
                } ?: continue
                result += match.key
                used += match.key
            }
            return result
        }

        val dock = pick(dockPriorityGroups, maxDock).toMutableList()
        if (dock.size < maxDock) {
            usable.asSequence()
                .filter { it.key !in used }
                .take(maxDock - dock.size)
                .forEach {
                    dock += it.key
                    used += it.key
                }
        }

        val favorites = mutableListOf<String>()
        val rankedByLocalUse = usable
            .filter {
                it.key !in used &&
                    (it.localRecencyRank != null || it.localLaunchCount > 0L)
            }
            .sortedWith(
                compareBy<StarterWorkspaceCandidate> {
                    it.localRecencyRank ?: Int.MAX_VALUE
                }.thenByDescending { it.localLaunchCount }
                    .thenByDescending { it.label.contains("goreecloud", ignoreCase = true) }
                    .thenBy { it.label.lowercase() },
            )
        rankedByLocalUse
            .take(maxFavorites)
            .forEach {
                favorites += it.key
                used += it.key
            }

        if (favorites.size < maxFavorites) {
            favorites += pick(
                groups = favoritePriorityGroups,
                limit = maxFavorites - favorites.size,
            )
        }
        if (favorites.size < maxFavorites) {
            usable.asSequence()
                .filter { it.key !in used }
                .take(maxFavorites - favorites.size)
                .forEach {
                    favorites += it.key
                    used += it.key
                }
        }

        return StarterWorkspaceSelection(
            favoriteKeys = favorites,
            dockKeys = dock,
        )
    }

    fun homeCells(
        itemCount: Int,
        columns: Int = 5,
        rows: Int = 6,
    ): List<Pair<Int, Int>> {
        if (itemCount <= 0 || columns <= 0 || rows <= 0) return emptyList()
        val count = itemCount.coerceAtMost(columns * rows)
        val occupiedRows = (count + columns - 1) / columns
        val firstRow = rows - occupiedRows
        return List(count) { index ->
            (index % columns) to (firstRow + index / columns)
        }
    }
}
