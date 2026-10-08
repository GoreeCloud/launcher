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
 * Selects transient automatic Home apps without mutating persisted workspace placement.
 *
 * Both ranking modes are intentionally Launcher-local: RECENT consumes only the bounded launch
 * ordering and MOST_USED consumes only aggregate Launcher launch counts. Persisted Favorites and
 * Dock items are excluded so automatic presentation never duplicates or competes with manual
 * placement.
 */
object LauncherHomeSuggestionsPolicy {
    fun selectKeys(
        mode: LauncherHomeAppMode,
        recentAppKeys: List<String>,
        launchCounts: Map<String, Long>,
        availableAppKeys: Set<String>,
        favoriteKeys: Set<String>,
        dockKeys: Set<String>,
        limit: Int = 10,
    ): List<String> {
        if (limit <= 0 || mode == LauncherHomeAppMode.NONE) return emptyList()

        val excluded = favoriteKeys + dockKeys
        val ranked = when (mode) {
            LauncherHomeAppMode.NONE -> emptyList()
            LauncherHomeAppMode.RECENT -> recentAppKeys
            LauncherHomeAppMode.MOST_USED -> launchCounts.entries
                .asSequence()
                .filter { it.value > 0L }
                .sortedWith(
                    compareByDescending<Map.Entry<String, Long>> { it.value }
                        .thenBy { it.key },
                )
                .map { it.key }
                .toList()
        }

        return ranked.asSequence()
            .filter { it.isNotBlank() }
            .filter(availableAppKeys::contains)
            .filterNot(excluded::contains)
            .distinct()
            .take(limit)
            .toList()
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
