package com.goreecloud.launcher.core.launcher

/**
 * User-facing execution boundary for Launcher Universal Search providers.
 *
 * This policy intentionally separates a provider being registered from a provider being allowed to
 * receive every typed query automatically. Local, retention-free providers can participate in the
 * normal Launcher result stream. A reviewed remote provider may participate inline only when its
 * implementation explicitly adopts the remote-inline contract and the user opts in. Other network,
 * authorization, retention, or third-party providers stay behind an explicit user handoff.
 */
enum class LauncherSearchProviderInvocationMode {
    AUTOMATIC_LOCAL,
    OPT_IN_LOCAL,
    OPT_IN_REMOTE_INLINE,
    EXPLICIT_USER_HANDOFF,
}

data class LauncherSearchProviderControlOption(
    val providerId: String,
    val displayName: String,
    val invocationMode: LauncherSearchProviderInvocationMode,
    val defaultEnabled: Boolean,
    val privacySummary: String,
)

data class LauncherSearchProviderControlState(
    val orderedOptions: List<LauncherSearchProviderControlOption>,
    val enabledProviderIds: Set<String>,
) {
    fun isEnabled(providerId: String): Boolean = providerId in enabledProviderIds
}

/**
 * Normalizes provider visibility, ordering, and automatic execution without expanding trust.
 *
 * A null enabled-provider preference means first run and adopts only privacy-safe defaults. An
 * explicit empty set means the user disabled every optional source. Unknown/stale IDs are dropped.
 * Newly registered providers are appended after the user's known ordering instead of silently
 * displacing existing choices.
 */
object LauncherSearchProviderUserControlPolicy {
    fun optionFor(metadata: LauncherSearchProviderMetadata): LauncherSearchProviderControlOption =
        optionFor(metadata, invocationModeFor(metadata))

    fun optionFor(
        registration: LauncherSearchProviderRegistration,
    ): LauncherSearchProviderControlOption =
        optionFor(registration.metadata, invocationModeFor(registration))

    private fun optionFor(
        metadata: LauncherSearchProviderMetadata,
        invocationMode: LauncherSearchProviderInvocationMode,
    ): LauncherSearchProviderControlOption =
        LauncherSearchProviderControlOption(
            providerId = metadata.providerId,
            displayName = displayNameFor(metadata.providerId),
            invocationMode = invocationMode,
            defaultEnabled = invocationMode == LauncherSearchProviderInvocationMode.AUTOMATIC_LOCAL,
            privacySummary = privacySummaryFor(metadata),
        )

    /**
     * Resolves the persisted preference result into the executable provider-control state.
     *
     * Absence keeps first-run privacy-safe defaults. A loaded snapshot preserves the user's explicit
     * enabled set and ordering. Malformed or unsupported persisted state fails closed to no enabled
     * providers instead of silently replacing the unreadable choice with automatic defaults.
     */
    fun normalize(
        catalog: LauncherSearchProviderCatalog,
        persistedPreferences: LauncherSearchProviderPreferenceDecodeResult,
    ): LauncherSearchProviderControlState = when (persistedPreferences) {
        LauncherSearchProviderPreferenceDecodeResult.Absent -> normalize(
            catalog = catalog,
            requestedEnabledProviderIds = null,
            requestedProviderOrder = emptyList(),
        )
        is LauncherSearchProviderPreferenceDecodeResult.Loaded -> normalize(
            catalog = catalog,
            requestedEnabledProviderIds = persistedPreferences.snapshot.enabledProviderIds,
            requestedProviderOrder = persistedPreferences.snapshot.providerOrder,
        )
        is LauncherSearchProviderPreferenceDecodeResult.UnsupportedVersion -> normalize(
            catalog = catalog,
            requestedEnabledProviderIds = emptySet(),
            requestedProviderOrder = emptyList(),
        )
        is LauncherSearchProviderPreferenceDecodeResult.Invalid -> normalize(
            catalog = catalog,
            requestedEnabledProviderIds = emptySet(),
            requestedProviderOrder = emptyList(),
        )
    }

    fun normalize(
        catalog: LauncherSearchProviderCatalog,
        requestedEnabledProviderIds: Set<String>?,
        requestedProviderOrder: List<String>,
    ): LauncherSearchProviderControlState {
        val registrationsById = catalog.acceptedRegistrations.associateBy { registration ->
            registration.metadata.providerId
        }
        val knownIds = registrationsById.keys
        val orderedIds = buildList {
            requestedProviderOrder.forEach { providerId ->
                if (providerId in knownIds && providerId !in this) add(providerId)
            }
            catalog.acceptedRegistrations.forEach { registration ->
                val providerId = registration.metadata.providerId
                if (providerId !in this) add(providerId)
            }
        }
        val orderedOptions = orderedIds.map { providerId ->
            optionFor(checkNotNull(registrationsById[providerId]))
        }
        val enabledProviderIds = if (requestedEnabledProviderIds == null) {
            orderedOptions
                .filter { option -> option.defaultEnabled }
                .mapTo(linkedSetOf()) { option -> option.providerId }
        } else {
            requestedEnabledProviderIds
                .filterTo(linkedSetOf()) { providerId -> providerId in knownIds }
        }

        return LauncherSearchProviderControlState(
            orderedOptions = orderedOptions,
            enabledProviderIds = enabledProviderIds,
        )
    }

    /**
     * Builds the next persisted snapshot for one explicit enable/disable action.
     *
     * Unknown provider IDs are ignored so stale UI events cannot manufacture persisted authority for
     * a provider that is not present in the accepted catalog-derived control state.
     */
    fun withProviderEnabled(
        state: LauncherSearchProviderControlState,
        providerId: String,
        enabled: Boolean,
    ): LauncherSearchProviderPreferenceSnapshot {
        val knownProviderIds = state.orderedOptions.map { option -> option.providerId }
        if (providerId !in knownProviderIds) {
            return LauncherSearchProviderPreferenceSnapshot.fromControlState(state)
        }

        val enabledProviderIds = state.enabledProviderIds.toMutableSet()
        if (enabled) {
            enabledProviderIds += providerId
        } else {
            enabledProviderIds -= providerId
        }
        return LauncherSearchProviderPreferenceSnapshot(
            enabledProviderIds = enabledProviderIds,
            providerOrder = knownProviderIds,
        )
    }

    /**
     * Builds the next persisted snapshot for a bounded provider-order move.
     *
     * Moves outside the accepted ordering are no-ops. Enablement is preserved independently from
     * ordering so reordering never grants a provider automatic execution authority.
     */
    fun moveProviderBy(
        state: LauncherSearchProviderControlState,
        providerId: String,
        offset: Int,
    ): LauncherSearchProviderPreferenceSnapshot {
        val providerOrder = state.orderedOptions
            .map { option -> option.providerId }
            .toMutableList()
        val currentIndex = providerOrder.indexOf(providerId)
        if (currentIndex < 0 || offset == 0) {
            return LauncherSearchProviderPreferenceSnapshot.fromControlState(state)
        }
        val targetIndex = currentIndex + offset
        if (targetIndex !in providerOrder.indices) {
            return LauncherSearchProviderPreferenceSnapshot.fromControlState(state)
        }

        providerOrder.removeAt(currentIndex)
        providerOrder.add(targetIndex, providerId)
        return LauncherSearchProviderPreferenceSnapshot(
            enabledProviderIds = state.enabledProviderIds,
            providerOrder = providerOrder,
        )
    }

    /**
     * Providers eligible for the live query result stream.
     *
     * Automatic and opt-in local providers remain local. A reviewed remote-inline provider joins
     * only after explicit source enablement and after its implementation reports that the governed
     * credential/authorization/configuration path is ready. Enabling an explicit-handoff provider
     * never authorizes live query fan-out and only makes it available to the Search-with picker.
     */
    fun automaticProviders(
        catalog: LauncherSearchProviderCatalog,
        state: LauncherSearchProviderControlState,
    ): List<LauncherSearchProvider> {
        val optionsById = state.orderedOptions.associateBy { option -> option.providerId }
        val providersById = catalog.acceptedRegistrations.associateBy { registration ->
            registration.metadata.providerId
        }
        return state.orderedOptions.mapNotNull { option ->
            if (
                !state.isEnabled(option.providerId) ||
                option.invocationMode == LauncherSearchProviderInvocationMode.EXPLICIT_USER_HANDOFF
            ) {
                return@mapNotNull null
            }
            val provider = providersById[option.providerId]?.provider ?: return@mapNotNull null
            if (
                option.invocationMode == LauncherSearchProviderInvocationMode.OPT_IN_REMOTE_INLINE &&
                (provider as? LauncherOptInRemoteInlineSearchProvider)
                    ?.isInlineExecutionReady != true
            ) {
                return@mapNotNull null
            }
            provider
        }.filter { provider -> provider.id in optionsById }
    }

    fun invocationModeFor(
        registration: LauncherSearchProviderRegistration,
    ): LauncherSearchProviderInvocationMode {
        val baseline = invocationModeFor(registration.metadata)
        if (baseline != LauncherSearchProviderInvocationMode.EXPLICIT_USER_HANDOFF) {
            return baseline
        }

        return if (
            registration.provider is LauncherOptInRemoteInlineSearchProvider &&
            (
                registration.metadata.provenance == LauncherSearchProviderProvenance.THIRD_PARTY ||
                    registration.metadata.offlineBehavior != LauncherSearchOfflineBehavior.LOCAL_ONLY ||
                    registration.metadata.remoteProcessing != LauncherSearchRemoteProcessing.NONE
            )
        ) {
            LauncherSearchProviderInvocationMode.OPT_IN_REMOTE_INLINE
        } else {
            LauncherSearchProviderInvocationMode.EXPLICIT_USER_HANDOFF
        }
    }

    fun invocationModeFor(
        metadata: LauncherSearchProviderMetadata,
    ): LauncherSearchProviderInvocationMode {
        val trustedLocal =
            metadata.provenance != LauncherSearchProviderProvenance.THIRD_PARTY &&
                metadata.offlineBehavior == LauncherSearchOfflineBehavior.LOCAL_ONLY &&
                metadata.remoteProcessing == LauncherSearchRemoteProcessing.NONE &&
                metadata.queryRetention == LauncherSearchQueryRetention.NONE

        return when {
            trustedLocal &&
                metadata.authorizationRequirement == LauncherSearchAuthorizationRequirement.NONE ->
                LauncherSearchProviderInvocationMode.AUTOMATIC_LOCAL

            trustedLocal &&
                metadata.authorizationRequirement == LauncherSearchAuthorizationRequirement.USER_CONSENT ->
                LauncherSearchProviderInvocationMode.OPT_IN_LOCAL

            else -> LauncherSearchProviderInvocationMode.EXPLICIT_USER_HANDOFF
        }
    }

    fun displayNameFor(providerId: String): String = when (providerId) {
        LauncherQuickAnswersSearchProvider.PROVIDER_ID -> "Quick answers"
        LauncherInstalledAppsSearchProvider.PROVIDER_ID -> "Apps"
        LauncherCoreActionsSearchProvider.PROVIDER_ID -> "Launcher actions"
        LauncherShortcutsSearchProvider.PROVIDER_ID -> "App shortcuts"
        LauncherContactsSearchProvider.PROVIDER_ID -> "Contacts"
        LauncherCallHistorySearchProvider.PROVIDER_ID -> "Call history"
        LauncherMessagesSearchProvider.PROVIDER_ID -> "Messages"
        LauncherFilesSearchProvider.PROVIDER_ID -> "Files"
        else -> LauncherConnectedSearchProviderRegistry.displayNameFor(providerId) ?: providerId
    }

    fun privacySummaryFor(metadata: LauncherSearchProviderMetadata): String {
        val parts = mutableListOf<String>()
        parts += when (metadata.offlineBehavior) {
            LauncherSearchOfflineBehavior.LOCAL_ONLY -> "Local only"
            LauncherSearchOfflineBehavior.OFFLINE_CAPABLE -> "Offline capable"
            LauncherSearchOfflineBehavior.NETWORK_REQUIRED -> "Network required"
        }
        when (metadata.remoteProcessing) {
            LauncherSearchRemoteProcessing.NONE -> Unit
            LauncherSearchRemoteProcessing.OPTIONAL -> parts += "Optional remote processing"
            LauncherSearchRemoteProcessing.REQUIRED -> parts += "Remote processing"
        }
        when (metadata.authorizationRequirement) {
            LauncherSearchAuthorizationRequirement.NONE -> Unit
            LauncherSearchAuthorizationRequirement.USER_CONSENT -> parts += "Consent required"
            LauncherSearchAuthorizationRequirement.ACCOUNT -> parts += "Account required"
            LauncherSearchAuthorizationRequirement.SYSTEM_POLICY -> parts += "Policy approval required"
        }
        parts += when (metadata.queryRetention) {
            LauncherSearchQueryRetention.NONE -> "No query retention"
            LauncherSearchQueryRetention.SESSION_ONLY -> "Session query retention"
            LauncherSearchQueryRetention.PERSISTENT -> "Persistent query retention"
            LauncherSearchQueryRetention.UNKNOWN -> "Provider retention policy applies"
        }
        return parts.joinToString(separator = " · ")
    }
}
