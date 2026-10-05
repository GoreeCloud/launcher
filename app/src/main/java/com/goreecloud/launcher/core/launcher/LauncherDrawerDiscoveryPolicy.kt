package com.goreecloud.launcher.core.launcher

internal enum class LauncherDrawerDiscoveryFilter(
    val displayName: String,
) {
    ALL("All"),
    PINNED("Pinned"),
    SUGGESTED("Suggested"),
    NEW("New"),
    UPDATED("Updated"),
}

internal data class LauncherAppFreshness(
    val firstInstallTimeMillis: Long,
    val lastUpdateTimeMillis: Long,
    val updateMetadataAvailable: Boolean = true,
)

internal object LauncherDrawerDiscoveryPolicy {
    const val FRESHNESS_WINDOW_MILLIS = 30L * 24L * 60L * 60L * 1000L
    const val UPDATE_SEPARATION_MILLIS = 60L * 1000L
    const val DEFAULT_SUGGESTION_LIMIT = 12

    fun recentlyInstalled(
        freshness: LauncherAppFreshness?,
        nowMillis: Long,
    ): Boolean {
        val installedAt = freshness?.firstInstallTimeMillis ?: return false
        if (installedAt <= 0L || nowMillis < installedAt) return false
        return nowMillis - installedAt <= FRESHNESS_WINDOW_MILLIS
    }

    fun recentlyUpdated(
        freshness: LauncherAppFreshness?,
        nowMillis: Long,
    ): Boolean {
        val value = freshness ?: return false
        if (
            !value.updateMetadataAvailable ||
            value.firstInstallTimeMillis <= 0L ||
            value.lastUpdateTimeMillis <= 0L ||
            nowMillis < value.lastUpdateTimeMillis ||
            value.lastUpdateTimeMillis - value.firstInstallTimeMillis <= UPDATE_SEPARATION_MILLIS
        ) {
            return false
        }
        return nowMillis - value.lastUpdateTimeMillis <= FRESHNESS_WINDOW_MILLIS
    }

    fun suggestedKeys(
        availableKeys: Set<String>,
        recentAppKeys: List<String>,
        launchCounts: Map<String, Long>,
        labelByKey: Map<String, String>,
        limit: Int = DEFAULT_SUGGESTION_LIMIT,
    ): Set<String> {
        if (availableKeys.isEmpty() || limit <= 0) return emptySet()

        val recentRank = recentAppKeys
            .filter(availableKeys::contains)
            .distinct()
            .withIndex()
            .associate { (index, key) -> key to index }
        val hasTruthfulUsage = recentRank.isNotEmpty() ||
            launchCounts.any { (key, count) -> key in availableKeys && count > 0L }

        return availableKeys
            .sortedWith(
                compareBy<String>(
                    { key -> if (hasTruthfulUsage) recentRank[key] ?: Int.MAX_VALUE else Int.MAX_VALUE },
                    { key -> if (hasTruthfulUsage) -(launchCounts[key] ?: 0L) else 0L },
                    { key -> labelByKey[key].orEmpty().lowercase() },
                    { key -> key },
                ),
            )
            .take(limit)
            .toCollection(linkedSetOf())
    }

    fun filterKeys(
        filter: LauncherDrawerDiscoveryFilter,
        availableKeys: Set<String>,
        pinnedKeys: Set<String>,
        recentAppKeys: List<String>,
        launchCounts: Map<String, Long>,
        labelByKey: Map<String, String>,
        freshnessByKey: Map<String, LauncherAppFreshness>,
        nowMillis: Long,
    ): Set<String> = when (filter) {
        LauncherDrawerDiscoveryFilter.ALL -> availableKeys
        LauncherDrawerDiscoveryFilter.PINNED -> availableKeys.intersect(pinnedKeys)
        LauncherDrawerDiscoveryFilter.SUGGESTED -> suggestedKeys(
            availableKeys = availableKeys,
            recentAppKeys = recentAppKeys,
            launchCounts = launchCounts,
            labelByKey = labelByKey,
        )
        LauncherDrawerDiscoveryFilter.NEW -> availableKeys.filterTo(linkedSetOf()) { key ->
            recentlyInstalled(freshnessByKey[key], nowMillis)
        }
        LauncherDrawerDiscoveryFilter.UPDATED -> availableKeys.filterTo(linkedSetOf()) { key ->
            recentlyUpdated(freshnessByKey[key], nowMillis)
        }
    }
}
