package com.goreecloud.launcher.core.launcher

internal enum class LauncherDrawerSmartFolderKind(
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
) {
    val id: String = "smart:" + kind.name.lowercase()
    val name: String = kind.displayName
}

internal object LauncherDrawerSmartFolderPolicy {
    private const val MAX_SMART_FOLDER_ITEMS = 24

    fun build(
        availableKeys: Set<String>,
        pinnedKeys: Set<String>,
        recentAppKeys: List<String>,
        launchCounts: Map<String, Long>,
        labelByKey: Map<String, String>,
        freshnessByKey: Map<String, LauncherAppFreshness>,
        nowMillis: Long,
        includeSuggested: Boolean,
    ): List<LauncherDrawerSmartFolder> {
        if (availableKeys.isEmpty()) return emptyList()

        val alphabetical = compareBy<String>(
            { key -> labelByKey[key].orEmpty().lowercase() },
            { key -> key },
        )
        val result = mutableListOf<LauncherDrawerSmartFolder>()

        availableKeys
            .filter { it in pinnedKeys }
            .sortedWith(alphabetical)
            .take(MAX_SMART_FOLDER_ITEMS)
            .takeIf(List<String>::isNotEmpty)
            ?.let { members ->
                result += LauncherDrawerSmartFolder(
                    kind = LauncherDrawerSmartFolderKind.PINNED,
                    memberKeys = members,
                )
            }

        if (includeSuggested) {
            LauncherDrawerDiscoveryPolicy.suggestedKeys(
                availableKeys = availableKeys,
                recentAppKeys = recentAppKeys,
                launchCounts = launchCounts,
                labelByKey = labelByKey,
                limit = MAX_SMART_FOLDER_ITEMS,
            )
                .toList()
                .takeIf(List<String>::isNotEmpty)
                ?.let { members ->
                    result += LauncherDrawerSmartFolder(
                        kind = LauncherDrawerSmartFolderKind.SUGGESTED,
                        memberKeys = members,
                    )
                }
        }

        availableKeys
            .filter { key ->
                LauncherDrawerDiscoveryPolicy.recentlyInstalled(
                    freshness = freshnessByKey[key],
                    nowMillis = nowMillis,
                )
            }
            .sortedByDescending { key -> freshnessByKey[key]?.firstInstallTimeMillis ?: 0L }
            .take(MAX_SMART_FOLDER_ITEMS)
            .takeIf(List<String>::isNotEmpty)
            ?.let { members ->
                result += LauncherDrawerSmartFolder(
                    kind = LauncherDrawerSmartFolderKind.NEW,
                    memberKeys = members,
                )
            }

        availableKeys
            .filter { key ->
                LauncherDrawerDiscoveryPolicy.recentlyUpdated(
                    freshness = freshnessByKey[key],
                    nowMillis = nowMillis,
                )
            }
            .sortedByDescending { key -> freshnessByKey[key]?.lastUpdateTimeMillis ?: 0L }
            .take(MAX_SMART_FOLDER_ITEMS)
            .takeIf(List<String>::isNotEmpty)
            ?.let { members ->
                result += LauncherDrawerSmartFolder(
                    kind = LauncherDrawerSmartFolderKind.UPDATED,
                    memberKeys = members,
                )
            }

        return result
    }
}
