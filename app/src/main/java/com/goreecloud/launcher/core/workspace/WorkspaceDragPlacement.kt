package com.goreecloud.launcher.core.workspace

data class WorkspaceDragPlacement(
    val favoriteKeys: List<String>,
    val dockKeys: List<String>,
)

object WorkspaceDragPlacementPolicy {
    fun moveHomeToDock(
        favoriteKeys: List<String>,
        dockKeys: List<String>,
        key: String,
        targetDockKey: String?,
        dockLimit: Int = MAX_DOCK_ITEMS,
    ): WorkspaceDragPlacement {
        val favorites = favoriteKeys.distinct()
        val dock = dockKeys.distinct().take(dockLimit)
        if (key !in favorites) return WorkspaceDragPlacement(favorites, dock)

        val nextDock = insertBeforeTarget(dock, key, targetDockKey, dockLimit)
            ?: return WorkspaceDragPlacement(favorites, dock)
        return WorkspaceDragPlacement(
            favoriteKeys = favorites.filterNot { it == key },
            dockKeys = nextDock,
        )
    }

    fun moveDockToHome(
        favoriteKeys: List<String>,
        dockKeys: List<String>,
        key: String,
        homeLimit: Int,
    ): WorkspaceDragPlacement {
        val favorites = favoriteKeys.distinct()
        val dock = dockKeys.distinct().take(MAX_DOCK_ITEMS)
        if (key !in dock) return WorkspaceDragPlacement(favorites, dock)
        if (key !in favorites && favorites.size >= homeLimit) {
            return WorkspaceDragPlacement(favorites, dock)
        }

        return WorkspaceDragPlacement(
            favoriteKeys = appendUnique(favorites, key),
            dockKeys = dock.filterNot { it == key },
        )
    }

    fun copyDrawerToHome(
        favoriteKeys: List<String>,
        dockKeys: List<String>,
        key: String,
        homeLimit: Int,
    ): WorkspaceDragPlacement {
        val favorites = favoriteKeys.distinct()
        val dock = dockKeys.distinct().take(MAX_DOCK_ITEMS)
        if (key in favorites || favorites.size >= homeLimit) {
            return WorkspaceDragPlacement(favorites, dock)
        }
        return WorkspaceDragPlacement(
            favoriteKeys = favorites + key,
            dockKeys = dock,
        )
    }

    fun copyDrawerToDock(
        favoriteKeys: List<String>,
        dockKeys: List<String>,
        key: String,
        targetDockKey: String?,
        dockLimit: Int = MAX_DOCK_ITEMS,
    ): WorkspaceDragPlacement {
        val favorites = favoriteKeys.distinct()
        val dock = dockKeys.distinct().take(dockLimit)
        val nextDock = insertBeforeTarget(dock, key, targetDockKey, dockLimit)
            ?: dock
        return WorkspaceDragPlacement(favorites, nextDock)
    }

    fun reorderDock(
        favoriteKeys: List<String>,
        dockKeys: List<String>,
        key: String,
        targetDockKey: String?,
        dockLimit: Int = MAX_DOCK_ITEMS,
    ): WorkspaceDragPlacement {
        val favorites = favoriteKeys.distinct()
        val dock = dockKeys.distinct().take(dockLimit)
        if (key !in dock) return WorkspaceDragPlacement(favorites, dock)
        return WorkspaceDragPlacement(
            favoriteKeys = favorites,
            dockKeys = insertBeforeTarget(dock, key, targetDockKey, dockLimit) ?: dock,
        )
    }

    private fun appendUnique(values: List<String>, key: String): List<String> =
        if (key in values) values else values + key

    private fun insertBeforeTarget(
        values: List<String>,
        key: String,
        targetKey: String?,
        limit: Int,
    ): List<String>? {
        if (key.isBlank() || limit <= 0) return null
        val normalized = values.distinct().take(limit)
        val existed = key in normalized
        if (!existed && normalized.size >= limit) return null

        val remaining = normalized.filterNot { it == key }.toMutableList()
        val targetIndex = targetKey
            ?.takeUnless { it == key }
            ?.let(remaining::indexOf)
            ?.takeIf { it >= 0 }
            ?: remaining.size
        remaining.add(targetIndex, key)
        return remaining.take(limit)
    }
}
