package com.goreecloud.launcher.core.launcher

/**
 * UI-facing structure for Launcher Universal Search results.
 *
 * The presentation model intentionally keeps ranking authority in [LauncherUniversalSearch]. It
 * only separates already-ranked local results into stable visual groups so direct application
 * matches can be visually prominent without changing provider scores or inventing a second ranking
 * algorithm in Compose.
 */
data class LauncherSearchPresentationModel(
    val applicationResults: List<LauncherSearchResult>,
    val actionAndSettingResults: List<LauncherSearchResult>,
    val explicitHandoffProviders: List<LauncherSearchExplicitHandoffProvider>,
) {
    val hasLocalResults: Boolean
        get() = applicationResults.isNotEmpty() || actionAndSettingResults.isNotEmpty()

    val hasExplicitHandoffProviders: Boolean
        get() = explicitHandoffProviders.isNotEmpty()
}

/**
 * Descriptive entry for a future explicit `Search with` surface.
 *
 * This model contains no query and performs no invocation. It is safe to render before external
 * provider execution exists because selecting a provider must remain a separate explicit action.
 */
data class LauncherSearchExplicitHandoffProvider(
    val providerId: String,
    val displayName: String,
    val privacySummary: String,
)

object LauncherSearchPresentationPolicy {
    /**
     * Groups an already-ranked result stream for rendering.
     *
     * Relative order inside each group is preserved exactly. Applications are surfaced separately;
     * settings and Launcher actions share a structured secondary group. No result is re-scored.
     */
    fun arrange(
        rawQuery: String,
        rankedResults: List<LauncherSearchResult>,
        providerControls: LauncherSearchProviderControlState,
    ): LauncherSearchPresentationModel {
        val applications = mutableListOf<LauncherSearchResult>()
        val actionsAndSettings = mutableListOf<LauncherSearchResult>()

        rankedResults.forEach { result ->
            when (result.category) {
                LauncherSearchCategory.APPLICATION -> applications += result
                LauncherSearchCategory.SHORTCUT,
                LauncherSearchCategory.CONTACT,
                LauncherSearchCategory.CALL_HISTORY,
                LauncherSearchCategory.MESSAGE,
                LauncherSearchCategory.FILE,
                LauncherSearchCategory.CONNECTED_SOURCE,
                LauncherSearchCategory.SETTING,
                LauncherSearchCategory.ACTION,
                -> actionsAndSettings += result
            }
        }

        return LauncherSearchPresentationModel(
            applicationResults = applications,
            actionAndSettingResults = actionsAndSettings,
            explicitHandoffProviders = explicitHandoffProviders(
                rawQuery = rawQuery,
                providerControls = providerControls,
            ),
        )
    }

    /**
     * Returns user-enabled connected providers that expose an explicit fallback handoff.
     *
     * Remote-inline providers keep this fallback even after inline execution is configured so a
     * provider outage or partial authorization never removes the deliberate external-search path.
     *
     * A blank query never exposes a handoff action. This method does not carry or dispatch the query;
     * the future invocation layer must require an explicit user action and re-check provider trust.
     */
    fun explicitHandoffProviders(
        rawQuery: String,
        providerControls: LauncherSearchProviderControlState,
    ): List<LauncherSearchExplicitHandoffProvider> {
        if (rawQuery.isBlank()) return emptyList()

        return providerControls.orderedOptions.mapNotNull { option ->
            if (!providerControls.isEnabled(option.providerId)) {
                return@mapNotNull null
            }
            if (
                option.invocationMode != LauncherSearchProviderInvocationMode.EXPLICIT_USER_HANDOFF &&
                option.invocationMode != LauncherSearchProviderInvocationMode.OPT_IN_REMOTE_INLINE
            ) {
                return@mapNotNull null
            }

            LauncherSearchExplicitHandoffProvider(
                providerId = option.providerId,
                displayName = option.displayName,
                privacySummary = option.privacySummary,
            )
        }
    }
}


/**
 * The idle swipe-down Universal Search panel exposes locally derived app shortcuts before a query
 * exists. These labels describe the source of each ordering; the policy never invents "frequent"
 * or "recent" membership when local evidence is absent.
 */
enum class LauncherSearchSuggestionTab(
    val displayName: String,
) {
    FREQUENT("Frequent"),
    RECENT("Recent"),
    NEW_UPDATED("New/updated"),
}

object LauncherSearchSuggestionPolicy {
    const val DEFAULT_LIMIT = 12

    fun selectKeys(
        tab: LauncherSearchSuggestionTab,
        availableAppKeys: Set<String>,
        recentAppKeys: List<String>,
        launchCounts: Map<String, Long>,
        freshnessByAppKey: Map<String, Long>,
        limit: Int = DEFAULT_LIMIT,
    ): List<String> {
        if (limit <= 0 || availableAppKeys.isEmpty()) return emptyList()

        val recentRank = recentAppKeys
            .asSequence()
            .filter { it in availableAppKeys }
            .distinct()
            .withIndex()
            .associate { indexed -> indexed.value to indexed.index }

        return when (tab) {
            LauncherSearchSuggestionTab.FREQUENT ->
                launchCounts.entries
                    .asSequence()
                    .filter { (key, count) -> key in availableAppKeys && count > 0L }
                    .sortedWith(
                        compareByDescending<Map.Entry<String, Long>> { it.value }
                            .thenBy { recentRank[it.key] ?: Int.MAX_VALUE }
                            .thenBy { it.key },
                    )
                    .map { it.key }
                    .take(limit)
                    .toList()

            LauncherSearchSuggestionTab.RECENT ->
                recentAppKeys
                    .asSequence()
                    .filter { it in availableAppKeys }
                    .distinct()
                    .take(limit)
                    .toList()

            LauncherSearchSuggestionTab.NEW_UPDATED ->
                freshnessByAppKey.entries
                    .asSequence()
                    .filter { (key, timestamp) -> key in availableAppKeys && timestamp > 0L }
                    .sortedWith(
                        compareByDescending<Map.Entry<String, Long>> { it.value }
                            .thenBy { it.key },
                    )
                    .map { it.key }
                    .take(limit)
                    .toList()
        }
    }
}
