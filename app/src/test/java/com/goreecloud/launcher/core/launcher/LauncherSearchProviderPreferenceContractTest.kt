package com.goreecloud.launcher.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherSearchProviderPreferenceContractTest {
    @Test
    fun absentStoredValueRemainsDistinctFromExplicitEmptySelection() {
        assertTrue(
            LauncherSearchProviderPreferenceContract.decode(null) is
                LauncherSearchProviderPreferenceDecodeResult.Absent,
        )

        val encoded = LauncherSearchProviderPreferenceContract.encode(
            LauncherSearchProviderPreferenceSnapshot(
                enabledProviderIds = emptySet(),
                providerOrder = emptyList(),
            ),
        )
        val decoded = LauncherSearchProviderPreferenceContract.decode(encoded)
            as LauncherSearchProviderPreferenceDecodeResult.Loaded

        assertTrue(decoded.snapshot.enabledProviderIds.isEmpty())
        assertTrue(decoded.snapshot.providerOrder.isEmpty())
    }

    @Test
    fun roundTripPreservesProviderOrderAndEnabledMembership() {
        val snapshot = LauncherSearchProviderPreferenceSnapshot(
            enabledProviderIds = setOf("third.party", "launcher.installed-apps"),
            providerOrder = listOf(
                "launcher.installed-apps",
                "third.party",
                "goreecloud.search",
            ),
        )

        val decoded = LauncherSearchProviderPreferenceContract.decode(
            LauncherSearchProviderPreferenceContract.encode(snapshot),
        ) as LauncherSearchProviderPreferenceDecodeResult.Loaded

        assertEquals(snapshot.enabledProviderIds, decoded.snapshot.enabledProviderIds)
        assertEquals(snapshot.providerOrder, decoded.snapshot.providerOrder)
    }

    @Test
    fun encodingIsDeterministicForEnabledSetInsertionOrder() {
        val first = LauncherSearchProviderPreferenceSnapshot(
            enabledProviderIds = linkedSetOf("z-provider", "a-provider"),
            providerOrder = listOf("z-provider", "a-provider"),
        )
        val second = LauncherSearchProviderPreferenceSnapshot(
            enabledProviderIds = linkedSetOf("a-provider", "z-provider"),
            providerOrder = listOf("z-provider", "a-provider"),
        )

        assertEquals(
            LauncherSearchProviderPreferenceContract.encode(first),
            LauncherSearchProviderPreferenceContract.encode(second),
        )
    }

    @Test
    fun providerIdsMayContainDelimiterCharactersWithoutCorruptingFormat() {
        val snapshot = LauncherSearchProviderPreferenceSnapshot(
            enabledProviderIds = setOf("provider,with=delimiters"),
            providerOrder = listOf("provider,with=delimiters"),
        )

        val decoded = LauncherSearchProviderPreferenceContract.decode(
            LauncherSearchProviderPreferenceContract.encode(snapshot),
        ) as LauncherSearchProviderPreferenceDecodeResult.Loaded

        assertEquals(snapshot, decoded.snapshot)
    }

    @Test
    fun unknownFutureVersionIsReportedInsteadOfSilentlyInterpreted() {
        val result = LauncherSearchProviderPreferenceContract.decode(
            "goreecloud-launcher-search-provider-preferences-v99\nenabled=\norder=",
        )

        assertEquals(
            LauncherSearchProviderPreferenceDecodeResult.UnsupportedVersion(99),
            result,
        )
    }

    @Test
    fun malformedProviderIdEncodingFailsClosed() {
        val result = LauncherSearchProviderPreferenceContract.decode(
            "goreecloud-launcher-search-provider-preferences-v1\nenabled=%%%\norder=",
        )

        assertEquals(
            LauncherSearchProviderPreferenceDecodeResult.Invalid(
                LauncherSearchProviderPreferenceInvalidReason.INVALID_PROVIDER_ID_ENCODING,
            ),
            result,
        )
    }

    @Test
    fun base64ProviderIdWithMalformedUtf8FailsClosed() {
        val result = LauncherSearchProviderPreferenceContract.decode(
            "goreecloud-launcher-search-provider-preferences-v1\nenabled=wyg\norder=",
        )

        assertEquals(
            LauncherSearchProviderPreferenceDecodeResult.Invalid(
                LauncherSearchProviderPreferenceInvalidReason.INVALID_PROVIDER_ID_ENCODING,
            ),
            result,
        )
    }

    @Test
    fun duplicateProviderIdsAreRejectedDuringDecode() {
        val result = LauncherSearchProviderPreferenceContract.decode(
            "goreecloud-launcher-search-provider-preferences-v1\nenabled=ZHVwbGljYXRl,ZHVwbGljYXRl\norder=",
        )

        assertEquals(
            LauncherSearchProviderPreferenceDecodeResult.Invalid(
                LauncherSearchProviderPreferenceInvalidReason.DUPLICATE_PROVIDER_ID,
            ),
            result,
        )
    }

    @Test
    fun controlStateSnapshotContainsOnlyProviderControlFields() {
        val provider = object : LauncherSearchProvider {
            override val id: String = "goreecloud.local"
            override fun search(rawQuery: String): List<LauncherSearchResult> = emptyList()
        }
        val registration = LauncherSearchProviderRegistration(
            provider = provider,
            metadata = LauncherSearchProviderMetadata(
                providerId = provider.id,
                contractVersion = LauncherSearchProviderContract.currentVersion,
                provenance = LauncherSearchProviderProvenance.GOREECLOUD_FIRST_PARTY,
                offlineBehavior = LauncherSearchOfflineBehavior.LOCAL_ONLY,
                authorizationRequirement = LauncherSearchAuthorizationRequirement.NONE,
                remoteProcessing = LauncherSearchRemoteProcessing.NONE,
                queryRetention = LauncherSearchQueryRetention.NONE,
            ),
        )
        val catalog = LauncherSearchProviderContract.evaluate(listOf(registration))
        val state = LauncherSearchProviderUserControlPolicy.normalize(
            catalog = catalog,
            requestedEnabledProviderIds = setOf(provider.id),
            requestedProviderOrder = listOf(provider.id),
        )

        val snapshot = LauncherSearchProviderPreferenceSnapshot.fromControlState(state)
        val encoded = LauncherSearchProviderPreferenceContract.encode(snapshot)

        assertEquals(setOf(provider.id), snapshot.enabledProviderIds)
        assertEquals(listOf(provider.id), snapshot.providerOrder)
        assertTrue(!encoded.contains("query", ignoreCase = true))
        assertTrue(!encoded.contains("history", ignoreCase = true))
    }
}
