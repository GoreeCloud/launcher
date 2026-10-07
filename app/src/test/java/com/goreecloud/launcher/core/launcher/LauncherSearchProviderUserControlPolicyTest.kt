package com.goreecloud.launcher.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherSearchProviderUserControlPolicyTest {
    @Test fun workOnlyConnectedAppIsVisible() {
        data class App(val pkg: String, val user: String)
        val result = launcherConnectedAppVisibility(
            listOf(App("com.dropbox.android", "work")), "com.dropbox.android",
            "personal", { it.pkg }, { it.user },
        )
        assertEquals(LauncherConnectedAppVisibility.WORK, result)
        assertEquals(LauncherConnectedAppVisibility.BOTH, launcherConnectedAppVisibility(
            listOf(App("com.dropbox.android", "personal"), App("com.dropbox.android", "work")),
            "com.dropbox.android", "personal", { it.pkg }, { it.user },
        ))
    }

    @Test
    fun localRetentionFreeFirstPartyProviderExecutesAutomaticallyByDefault() {
        val registration = registration(
            providerId = "goreecloud.local",
            provenance = LauncherSearchProviderProvenance.GOREECLOUD_FIRST_PARTY,
        )
        val catalog = LauncherSearchProviderContract.evaluate(listOf(registration))

        val state = LauncherSearchProviderUserControlPolicy.normalize(
            catalog = catalog,
            requestedEnabledProviderIds = null,
            requestedProviderOrder = emptyList(),
        )

        assertTrue(state.isEnabled("goreecloud.local"))
        assertEquals(
            LauncherSearchProviderInvocationMode.AUTOMATIC_LOCAL,
            state.orderedOptions.single().invocationMode,
        )
        assertEquals(listOf("goreecloud.local"), automaticProviderIds(catalog, state))
    }

    @Test
    fun consentRequiredLocalProviderIsDisabledByDefaultAndRunsOnlyAfterEnablement() {
        val registration = registration(
            providerId = "local.contacts",
            authorizationRequirement = LauncherSearchAuthorizationRequirement.USER_CONSENT,
        )
        val catalog = LauncherSearchProviderContract.evaluate(listOf(registration))

        val initial = LauncherSearchProviderUserControlPolicy.normalize(
            catalog = catalog,
            requestedEnabledProviderIds = null,
            requestedProviderOrder = emptyList(),
        )

        assertFalse(initial.isEnabled("local.contacts"))
        assertEquals(
            LauncherSearchProviderInvocationMode.OPT_IN_LOCAL,
            initial.orderedOptions.single().invocationMode,
        )
        assertTrue(
            LauncherSearchProviderUserControlPolicy
                .automaticProviders(catalog, initial)
                .isEmpty(),
        )

        val enabledSnapshot = LauncherSearchProviderUserControlPolicy.withProviderEnabled(
            state = initial,
            providerId = "local.contacts",
            enabled = true,
        )
        val enabled = LauncherSearchProviderUserControlPolicy.normalize(
            catalog = catalog,
            requestedEnabledProviderIds = enabledSnapshot.enabledProviderIds,
            requestedProviderOrder = enabledSnapshot.providerOrder,
        )

        assertTrue(enabled.isEnabled("local.contacts"))
        assertEquals(listOf("local.contacts"), automaticProviderIds(catalog, enabled))
    }

    @Test
    fun thirdPartyProviderNeverReceivesAutomaticQueriesEvenWhenLocallyDescribed() {
        val registration = registration(
            providerId = "third.party",
            provenance = LauncherSearchProviderProvenance.THIRD_PARTY,
        )
        val catalog = LauncherSearchProviderContract.evaluate(listOf(registration))
        val state = LauncherSearchProviderUserControlPolicy.normalize(
            catalog = catalog,
            requestedEnabledProviderIds = setOf("third.party"),
            requestedProviderOrder = emptyList(),
        )

        assertTrue(state.isEnabled("third.party"))
        assertEquals(
            LauncherSearchProviderInvocationMode.EXPLICIT_USER_HANDOFF,
            state.orderedOptions.single().invocationMode,
        )
        assertTrue(LauncherSearchProviderUserControlPolicy.automaticProviders(catalog, state).isEmpty())
    }

    @Test
    fun reviewedRemoteInlineProviderDefaultsOffAndRunsOnlyAfterEnablement() {
        val registration = remoteInlineRegistration("third.inline")
        val catalog = LauncherSearchProviderContract.evaluate(listOf(registration))

        val initial = LauncherSearchProviderUserControlPolicy.normalize(
            catalog = catalog,
            requestedEnabledProviderIds = null,
            requestedProviderOrder = emptyList(),
        )

        assertFalse(initial.isEnabled("third.inline"))
        assertEquals(
            LauncherSearchProviderInvocationMode.OPT_IN_REMOTE_INLINE,
            initial.orderedOptions.single().invocationMode,
        )
        assertTrue(
            LauncherSearchProviderUserControlPolicy
                .automaticProviders(catalog, initial)
                .isEmpty(),
        )

        val enabledSnapshot = LauncherSearchProviderUserControlPolicy.withProviderEnabled(
            state = initial,
            providerId = "third.inline",
            enabled = true,
        )
        val enabled = LauncherSearchProviderUserControlPolicy.normalize(
            catalog = catalog,
            requestedEnabledProviderIds = enabledSnapshot.enabledProviderIds,
            requestedProviderOrder = enabledSnapshot.providerOrder,
        )

        assertEquals(listOf("third.inline"), automaticProviderIds(catalog, enabled))
    }

    @Test
    fun unconfiguredRemoteInlineProviderRemainsNonExecutingAfterEnablement() {
        val registration = remoteInlineRegistration(
            providerId = "third.inline",
            ready = false,
        )
        val catalog = LauncherSearchProviderContract.evaluate(listOf(registration))
        val state = LauncherSearchProviderUserControlPolicy.normalize(
            catalog = catalog,
            requestedEnabledProviderIds = setOf("third.inline"),
            requestedProviderOrder = listOf("third.inline"),
        )

        assertTrue(state.isEnabled("third.inline"))
        assertEquals(
            LauncherSearchProviderInvocationMode.OPT_IN_REMOTE_INLINE,
            state.orderedOptions.single().invocationMode,
        )
        assertTrue(LauncherSearchProviderUserControlPolicy.automaticProviders(catalog, state).isEmpty())
    }

    @Test
    fun networkRemoteOrRetainingProviderDefaultsDisabledAndExplicitHandoff() {
        val network = registration(
            providerId = "network",
            offlineBehavior = LauncherSearchOfflineBehavior.NETWORK_REQUIRED,
        )
        val remote = registration(
            providerId = "remote",
            remoteProcessing = LauncherSearchRemoteProcessing.REQUIRED,
        )
        val retaining = registration(
            providerId = "retaining",
            queryRetention = LauncherSearchQueryRetention.SESSION_ONLY,
        )
        val catalog = LauncherSearchProviderContract.evaluate(listOf(network, remote, retaining))

        val state = LauncherSearchProviderUserControlPolicy.normalize(
            catalog = catalog,
            requestedEnabledProviderIds = null,
            requestedProviderOrder = emptyList(),
        )

        assertTrue(state.enabledProviderIds.isEmpty())
        state.orderedOptions.forEach { option ->
            assertFalse(option.defaultEnabled)
            assertEquals(
                LauncherSearchProviderInvocationMode.EXPLICIT_USER_HANDOFF,
                option.invocationMode,
            )
        }
        assertTrue(LauncherSearchProviderUserControlPolicy.automaticProviders(catalog, state).isEmpty())
    }

    @Test
    fun explicitEmptySelectionIsNotReplacedByDefaults() {
        val registration = registration(providerId = "local")
        val catalog = LauncherSearchProviderContract.evaluate(listOf(registration))

        val state = LauncherSearchProviderUserControlPolicy.normalize(
            catalog = catalog,
            requestedEnabledProviderIds = emptySet(),
            requestedProviderOrder = emptyList(),
        )

        assertTrue(state.enabledProviderIds.isEmpty())
    }

    @Test
    fun absentPersistedStateUsesDefaultsButInvalidAndUnsupportedFailClosed() {
        val first = registration(providerId = "first")
        val second = registration(providerId = "second")
        val catalog = LauncherSearchProviderContract.evaluate(listOf(first, second))

        val absent = LauncherSearchProviderUserControlPolicy.normalize(
            catalog = catalog,
            persistedPreferences = LauncherSearchProviderPreferenceDecodeResult.Absent,
        )
        assertEquals(setOf("first", "second"), absent.enabledProviderIds)

        val invalid = LauncherSearchProviderUserControlPolicy.normalize(
            catalog = catalog,
            persistedPreferences = LauncherSearchProviderPreferenceDecodeResult.Invalid(
                LauncherSearchProviderPreferenceInvalidReason.INVALID_STRUCTURE,
            ),
        )
        assertTrue(invalid.enabledProviderIds.isEmpty())
        assertTrue(LauncherSearchProviderUserControlPolicy.automaticProviders(catalog, invalid).isEmpty())

        val unsupported = LauncherSearchProviderUserControlPolicy.normalize(
            catalog = catalog,
            persistedPreferences = LauncherSearchProviderPreferenceDecodeResult.UnsupportedVersion(2),
        )
        assertTrue(unsupported.enabledProviderIds.isEmpty())
        assertTrue(LauncherSearchProviderUserControlPolicy.automaticProviders(catalog, unsupported).isEmpty())
    }

    @Test
    fun loadedPersistedStatePreservesExplicitSelectionAndOrder() {
        val first = registration(providerId = "first")
        val second = registration(providerId = "second")
        val catalog = LauncherSearchProviderContract.evaluate(listOf(first, second))
        val persisted = LauncherSearchProviderPreferenceDecodeResult.Loaded(
            LauncherSearchProviderPreferenceSnapshot(
                enabledProviderIds = setOf("second"),
                providerOrder = listOf("second", "first"),
            ),
        )

        val state = LauncherSearchProviderUserControlPolicy.normalize(
            catalog = catalog,
            persistedPreferences = persisted,
        )

        assertEquals(listOf("second", "first"), state.orderedOptions.map { it.providerId })
        assertEquals(setOf("second"), state.enabledProviderIds)
        assertEquals(listOf("second"), automaticProviderIds(catalog, state))
    }

    @Test
    fun providerControlMutationsPersistOnlyKnownProvidersAndPreserveEnablement() {
        val first = registration(providerId = "first")
        val second = registration(providerId = "second")
        val catalog = LauncherSearchProviderContract.evaluate(listOf(first, second))
        val state = LauncherSearchProviderUserControlPolicy.normalize(
            catalog = catalog,
            requestedEnabledProviderIds = setOf("first"),
            requestedProviderOrder = listOf("first", "second"),
        )

        val disabled = LauncherSearchProviderUserControlPolicy.withProviderEnabled(
            state = state,
            providerId = "first",
            enabled = false,
        )
        assertTrue(disabled.enabledProviderIds.isEmpty())
        assertEquals(listOf("first", "second"), disabled.providerOrder)

        val unknown = LauncherSearchProviderUserControlPolicy.withProviderEnabled(
            state = state,
            providerId = "unknown",
            enabled = true,
        )
        assertEquals(setOf("first"), unknown.enabledProviderIds)
        assertEquals(listOf("first", "second"), unknown.providerOrder)

        val moved = LauncherSearchProviderUserControlPolicy.moveProviderBy(
            state = state,
            providerId = "second",
            offset = -1,
        )
        assertEquals(setOf("first"), moved.enabledProviderIds)
        assertEquals(listOf("second", "first"), moved.providerOrder)

        val outOfBounds = LauncherSearchProviderUserControlPolicy.moveProviderBy(
            state = state,
            providerId = "first",
            offset = -1,
        )
        assertEquals(listOf("first", "second"), outOfBounds.providerOrder)
    }

    @Test
    fun requestedOrderDropsUnknownAndDuplicatesThenAppendsNewProviders() {
        val first = registration(providerId = "first")
        val second = registration(providerId = "second")
        val third = registration(providerId = "third")
        val catalog = LauncherSearchProviderContract.evaluate(listOf(first, second, third))

        val state = LauncherSearchProviderUserControlPolicy.normalize(
            catalog = catalog,
            requestedEnabledProviderIds = setOf("second", "unknown"),
            requestedProviderOrder = listOf("second", "unknown", "second", "first"),
        )

        assertEquals(
            listOf("second", "first", "third"),
            state.orderedOptions.map { option -> option.providerId },
        )
        assertEquals(setOf("second"), state.enabledProviderIds)
    }

    @Test
    fun builtInNamesAndPrivacySummaryAreUserReadable() {
        val apps = metadata(LauncherInstalledAppsSearchProvider.PROVIDER_ID)
        val actions = metadata(LauncherCoreActionsSearchProvider.PROVIDER_ID)
        val quickAnswers = metadata(LauncherQuickAnswersSearchProvider.PROVIDER_ID)

        assertEquals(
            "Quick answers",
            LauncherSearchProviderUserControlPolicy.displayNameFor(quickAnswers.providerId),
        )
        assertEquals("Apps", LauncherSearchProviderUserControlPolicy.displayNameFor(apps.providerId))
        assertEquals(
            "Launcher actions",
            LauncherSearchProviderUserControlPolicy.displayNameFor(actions.providerId),
        )
        assertEquals(
            "App shortcuts",
            LauncherSearchProviderUserControlPolicy.displayNameFor(
                LauncherShortcutsSearchProvider.PROVIDER_ID,
            ),
        )
        assertEquals(
            "Contacts",
            LauncherSearchProviderUserControlPolicy.displayNameFor(
                LauncherContactsSearchProvider.PROVIDER_ID,
            ),
        )
        assertEquals(
            "Local only · No query retention",
            LauncherSearchProviderUserControlPolicy.privacySummaryFor(apps),
        )
    }

    @Test
    fun fileAndConnectedSourceNamesAndUnknownRetentionAreExplicit() {
        assertEquals(
            "Files",
            LauncherSearchProviderUserControlPolicy.displayNameFor(
                LauncherFilesSearchProvider.PROVIDER_ID,
            ),
        )
        assertEquals(
            "Google Drive",
            LauncherSearchProviderUserControlPolicy.displayNameFor(
                LauncherConnectedSearchProviderRegistry.GOOGLE_DRIVE_PROVIDER_ID,
            ),
        )
        assertEquals(
            "Dropbox",
            LauncherSearchProviderUserControlPolicy.displayNameFor(
                LauncherConnectedSearchProviderRegistry.DROPBOX_PROVIDER_ID,
            ),
        )
        assertEquals(
            "Brave Search",
            LauncherSearchProviderUserControlPolicy.displayNameFor(
                LauncherConnectedSearchProviderRegistry.BRAVE_SEARCH_PROVIDER_ID,
            ),
        )
        assertEquals(
            "Google Search",
            LauncherSearchProviderUserControlPolicy.displayNameFor(
                LauncherConnectedSearchProviderRegistry.GOOGLE_SEARCH_PROVIDER_ID,
            ),
        )
        assertEquals(
            "DuckDuckGo",
            LauncherSearchProviderUserControlPolicy.displayNameFor(
                LauncherConnectedSearchProviderRegistry.DUCKDUCKGO_PROVIDER_ID,
            ),
        )
        assertEquals(
            "ChatGPT",
            LauncherSearchProviderUserControlPolicy.displayNameFor(
                LauncherConnectedSearchProviderRegistry.CHATGPT_PROVIDER_ID,
            ),
        )
        assertEquals(
            "Gemini",
            LauncherSearchProviderUserControlPolicy.displayNameFor(
                LauncherConnectedSearchProviderRegistry.GEMINI_PROVIDER_ID,
            ),
        )
        assertEquals(
            "Perplexity",
            LauncherSearchProviderUserControlPolicy.displayNameFor(
                LauncherConnectedSearchProviderRegistry.PERPLEXITY_PROVIDER_ID,
            ),
        )
        assertEquals(
            "Claude",
            LauncherSearchProviderUserControlPolicy.displayNameFor(
                LauncherConnectedSearchProviderRegistry.CLAUDE_PROVIDER_ID,
            ),
        )
        assertTrue(
            LauncherConnectedSearchProviderRegistry.isWebSearchProvider(
                LauncherConnectedSearchProviderRegistry.GOOGLE_SEARCH_PROVIDER_ID,
            ),
        )
        assertTrue(
            LauncherConnectedSearchProviderRegistry.isAiProvider(
                LauncherConnectedSearchProviderRegistry.CHATGPT_PROVIDER_ID,
            ),
        )

        // Retired integration identities remain rejected even after a persisted-order migration.
        listOf("connected.bing-search", "connected.microsoft-copilot").forEach { retired ->
            assertFalse(LauncherConnectedSearchProviderRegistry.isConnectedProvider(retired))
            assertEquals(null, LauncherConnectedSearchProviderRegistry.displayNameFor(retired))
            assertEquals(emptyList<String>(), LauncherConnectedSearchProviderRegistry.iconPackageNamesFor(retired))
        }

        val metadata = metadata(
            providerId = LauncherConnectedSearchProviderRegistry.BRAVE_SEARCH_PROVIDER_ID,
            provenance = LauncherSearchProviderProvenance.THIRD_PARTY,
            offlineBehavior = LauncherSearchOfflineBehavior.NETWORK_REQUIRED,
            remoteProcessing = LauncherSearchRemoteProcessing.REQUIRED,
            queryRetention = LauncherSearchQueryRetention.UNKNOWN,
        )

        assertEquals(
            LauncherSearchProviderInvocationMode.EXPLICIT_USER_HANDOFF,
            LauncherSearchProviderUserControlPolicy.invocationModeFor(metadata),
        )
        assertEquals(
            "Network required · Remote processing · Provider retention policy applies",
            LauncherSearchProviderUserControlPolicy.privacySummaryFor(metadata),
        )
    }

    @Test
    fun privacySummarySurfacesNetworkConsentAndRetention() {
        val providerMetadata = metadata(
            providerId = "remote",
            offlineBehavior = LauncherSearchOfflineBehavior.NETWORK_REQUIRED,
            authorizationRequirement = LauncherSearchAuthorizationRequirement.USER_CONSENT,
            remoteProcessing = LauncherSearchRemoteProcessing.REQUIRED,
            queryRetention = LauncherSearchQueryRetention.PERSISTENT,
        )

        assertEquals(
            "Network required · Remote processing · Consent required · Persistent query retention",
            LauncherSearchProviderUserControlPolicy.privacySummaryFor(providerMetadata),
        )
    }

    private fun automaticProviderIds(
        catalog: LauncherSearchProviderCatalog,
        state: LauncherSearchProviderControlState,
    ): List<String> = LauncherSearchProviderUserControlPolicy
        .automaticProviders(catalog, state)
        .map { provider -> provider.id }

    private fun registration(
        providerId: String,
        provenance: LauncherSearchProviderProvenance = LauncherSearchProviderProvenance.GOREECLOUD_FIRST_PARTY,
        offlineBehavior: LauncherSearchOfflineBehavior = LauncherSearchOfflineBehavior.LOCAL_ONLY,
        authorizationRequirement: LauncherSearchAuthorizationRequirement = LauncherSearchAuthorizationRequirement.NONE,
        remoteProcessing: LauncherSearchRemoteProcessing = LauncherSearchRemoteProcessing.NONE,
        queryRetention: LauncherSearchQueryRetention = LauncherSearchQueryRetention.NONE,
    ): LauncherSearchProviderRegistration {
        val provider = object : LauncherSearchProvider {
            override val id: String = providerId
            override fun search(rawQuery: String): List<LauncherSearchResult> = emptyList()
        }
        return LauncherSearchProviderRegistration(
            provider = provider,
            metadata = metadata(
                providerId = providerId,
                provenance = provenance,
                offlineBehavior = offlineBehavior,
                authorizationRequirement = authorizationRequirement,
                remoteProcessing = remoteProcessing,
                queryRetention = queryRetention,
            ),
        )
    }

    private fun remoteInlineRegistration(
        providerId: String,
        ready: Boolean = true,
    ): LauncherSearchProviderRegistration {
        val provider = object : LauncherOptInRemoteInlineSearchProvider {
            override val id: String = providerId
            override val isInlineExecutionReady: Boolean = ready

            override fun search(rawQuery: String): List<LauncherSearchResult> = emptyList()

            override suspend fun searchAsync(
                request: LauncherSearchRequest,
            ): List<LauncherSearchResult> = emptyList()
        }
        return LauncherSearchProviderRegistration(
            provider = provider,
            metadata = metadata(
                providerId = providerId,
                provenance = LauncherSearchProviderProvenance.THIRD_PARTY,
                offlineBehavior = LauncherSearchOfflineBehavior.NETWORK_REQUIRED,
                remoteProcessing = LauncherSearchRemoteProcessing.REQUIRED,
                queryRetention = LauncherSearchQueryRetention.UNKNOWN,
            ),
        )
    }

    private fun metadata(
        providerId: String,
        provenance: LauncherSearchProviderProvenance = LauncherSearchProviderProvenance.LAUNCHER_BUILT_IN,
        offlineBehavior: LauncherSearchOfflineBehavior = LauncherSearchOfflineBehavior.LOCAL_ONLY,
        authorizationRequirement: LauncherSearchAuthorizationRequirement = LauncherSearchAuthorizationRequirement.NONE,
        remoteProcessing: LauncherSearchRemoteProcessing = LauncherSearchRemoteProcessing.NONE,
        queryRetention: LauncherSearchQueryRetention = LauncherSearchQueryRetention.NONE,
    ): LauncherSearchProviderMetadata = LauncherSearchProviderMetadata(
        providerId = providerId,
        contractVersion = LauncherSearchProviderContract.currentVersion,
        provenance = provenance,
        offlineBehavior = offlineBehavior,
        authorizationRequirement = authorizationRequirement,
        remoteProcessing = remoteProcessing,
        queryRetention = queryRetention,
    )
}
