package com.goreecloud.launcher.core.launcher

enum class LauncherDrawerSmartFolderKind(
    val displayName: String,
    val explanation: String,
) {
    PINNED(
        displayName = "Pinned",
        explanation = "Apps you explicitly pinned in the App Drawer.",
    ),
    SUGGESTED(
        displayName = "Suggested",
        explanation = "Local-only Launcher suggestions; first use falls back deterministically.",
    ),
    NEW(
        displayName = "New",
        explanation = "Apps installed within the bounded local freshness window.",
    ),
    UPDATED(
        displayName = "Updated",
        explanation = "Apps recently updated when Android exposes profile-correct metadata.",
    ),
}

internal data class LauncherDrawerSmartFolder(
    val kind: LauncherDrawerSmartFolderKind,
    val memberKeys: List<String>,
    val excludedCount: Int = 0,
) {
    val id: String = "smart:" + kind.name.lowercase()
    val name: String = kind.displayName
}

internal object LauncherDrawerSmartFolderExclusions {
    private const val SEPARATOR = "\t"
    private const val MAX_APP_KEY_LENGTH = 512

    fun decode(raw: Set<String>): Map<LauncherDrawerSmartFolderKind, Set<String>> {
        if (raw.isEmpty()) return emptyMap()
        val decoded = linkedMapOf<LauncherDrawerSmartFolderKind, MutableSet<String>>()
        raw.forEach { entry ->
            val separatorIndex = entry.indexOf(SEPARATOR)
            if (separatorIndex <= 0 || separatorIndex >= entry.lastIndex) return@forEach
            val kind = runCatching {
                LauncherDrawerSmartFolderKind.valueOf(entry.substring(0, separatorIndex))
            }.getOrNull() ?: return@forEach
            if (kind == LauncherDrawerSmartFolderKind.PINNED) return@forEach
            val appKey = entry.substring(separatorIndex + SEPARATOR.length).trim()
            if (appKey.isNotEmpty() && appKey.length <= MAX_APP_KEY_LENGTH) {
                decoded.getOrPut(kind) { linkedSetOf() } += appKey
            }
        }
        return decoded.mapValues { (_, keys) -> keys.toSet() }
    }

    fun setExcluded(
        raw: Set<String>,
        kind: LauncherDrawerSmartFolderKind,
        appKey: String,
        excluded: Boolean,
    ): Set<String> {
        if (kind == LauncherDrawerSmartFolderKind.PINNED) return raw
        val normalizedKey = appKey.trim()
        if (normalizedKey.isEmpty() || normalizedKey.length > MAX_APP_KEY_LENGTH) return raw
        val encoded = kind.name + SEPARATOR + normalizedKey
        val updated = raw.toMutableSet()
        if (excluded) {
            updated += encoded
        } else {
            updated -= encoded
        }
        return updated
    }

    fun clearKind(
        raw: Set<String>,
        kind: LauncherDrawerSmartFolderKind,
    ): Set<String> {
        if (kind == LauncherDrawerSmartFolderKind.PINNED) return raw
        val prefix = kind.name + SEPARATOR
        return raw.filterNot { entry -> entry.startsWith(prefix) }.toSet()
    }
}

internal object LauncherDrawerSmartFolderPolicy {
    private const val MAX_SMART_FOLDER_ITEMS = 24
    private const val MAX_SUGGESTION_CANDIDATES = 192

    fun build(
        availableKeys: Set<String>,
        pinnedKeys: Set<String>,
        recentAppKeys: List<String>,
        launchCounts: Map<String, Long>,
        labelByKey: Map<String, String>,
        freshnessByKey: Map<String, LauncherAppFreshness>,
        nowMillis: Long,
        includeSuggested: Boolean,
        excludedKeysByKind: Map<LauncherDrawerSmartFolderKind, Set<String>> = emptyMap(),
    ): List<LauncherDrawerSmartFolder> {
        if (availableKeys.isEmpty()) return emptyList()

        val alphabetical = compareBy<String>(
            { key -> labelByKey[key].orEmpty().lowercase() },
            { key -> key },
        )
        val result = mutableListOf<LauncherDrawerSmartFolder>()

        fun addFolder(
            kind: LauncherDrawerSmartFolderKind,
            orderedCandidates: List<String>,
        ) {
            if (orderedCandidates.isEmpty()) return
            val excluded = if (kind == LauncherDrawerSmartFolderKind.PINNED) {
                emptySet()
            } else {
                excludedKeysByKind[kind].orEmpty()
            }
            val excludedCount = orderedCandidates.count(excluded::contains)
            val visible = orderedCandidates
                .asSequence()
                .filterNot(excluded::contains)
                .take(MAX_SMART_FOLDER_ITEMS)
                .toList()
            result += LauncherDrawerSmartFolder(
                kind = kind,
                memberKeys = visible,
                excludedCount = excludedCount,
            )
        }

        addFolder(
            kind = LauncherDrawerSmartFolderKind.PINNED,
            orderedCandidates = availableKeys
                .filter { it in pinnedKeys }
                .sortedWith(alphabetical),
        )

        if (includeSuggested) {
            val suggested = LauncherDrawerDiscoveryPolicy.suggestedKeys(
                availableKeys = availableKeys,
                recentAppKeys = recentAppKeys,
                launchCounts = launchCounts,
                labelByKey = labelByKey,
                limit = availableKeys.size.coerceAtMost(MAX_SUGGESTION_CANDIDATES),
            ).toList()
            addFolder(
                kind = LauncherDrawerSmartFolderKind.SUGGESTED,
                orderedCandidates = suggested,
            )
        }

        addFolder(
            kind = LauncherDrawerSmartFolderKind.NEW,
            orderedCandidates = availableKeys
                .filter { key ->
                    LauncherDrawerDiscoveryPolicy.recentlyInstalled(
                        freshness = freshnessByKey[key],
                        nowMillis = nowMillis,
                    )
                }
                .sortedByDescending { key ->
                    freshnessByKey[key]?.firstInstallTimeMillis ?: 0L
                },
        )

        addFolder(
            kind = LauncherDrawerSmartFolderKind.UPDATED,
            orderedCandidates = availableKeys
                .filter { key ->
                    LauncherDrawerDiscoveryPolicy.recentlyUpdated(
                        freshness = freshnessByKey[key],
                        nowMillis = nowMillis,
                    )
                }
                .sortedByDescending { key ->
                    freshnessByKey[key]?.lastUpdateTimeMillis ?: 0L
                },
        )

        return result
    }
}
