package com.goreecloud.launcher.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherSearchProviderContractTest {
    @Test
    fun builtInMetadataIsCurrentLocalOnlyAndRetentionFree() {
        val providerIds = listOf(
            LauncherCoreActionsSearchProvider.PROVIDER_ID,
            LauncherInstalledAppsSearchProvider.PROVIDER_ID,
        )
        val metadata = providerIds.map(LauncherBuiltInSearchProviderRegistry::metadataFor)

        metadata.forEachIndexed { index, providerMetadata ->
            assertEquals(providerIds[index], providerMetadata.providerId)
            assertEquals(
                LauncherSearchProviderContract.currentVersion,
                providerMetadata.contractVersion,
            )
            assertEquals(
                LauncherSearchProviderProvenance.LAUNCHER_BUILT_IN,
                providerMetadata.provenance,
            )
            assertEquals(
                LauncherSearchOfflineBehavior.LOCAL_ONLY,
                providerMetadata.offlineBehavior,
            )
            assertEquals(
                LauncherSearchAuthorizationRequirement.NONE,
                providerMetadata.authorizationRequirement,
            )
            assertEquals(
                LauncherSearchRemoteProcessing.NONE,
                providerMetadata.remoteProcessing,
            )
            assertEquals(
                LauncherSearchQueryRetention.NONE,
                providerMetadata.queryRetention,
            )
        }
    }

    @Test
    fun newerProviderMinorVersionFailsClosed() {
        val provider = stubProvider("future")
        val current = LauncherSearchProviderContract.currentVersion
        val registration = registration(
            provider = provider,
            version = LauncherSearchProviderContractVersion(
                major = current.major,
                minor = current.minor + 1,
            ),
        )

        val catalog = LauncherSearchProviderContract.evaluate(listOf(registration))

        assertTrue(catalog.providers.isEmpty())
        assertEquals(
            listOf(
                LauncherSearchProviderRejection(
                    providerId = provider.id,
                    reason = LauncherSearchProviderRejectionReason.INCOMPATIBLE_CONTRACT_VERSION,
                ),
            ),
            catalog.rejections,
        )
    }

    @Test
    fun incompatibleProviderMajorVersionFailsClosed() {
        val provider = stubProvider("future-major")
        val current = LauncherSearchProviderContract.currentVersion
        val registration = registration(
            provider = provider,
            version = LauncherSearchProviderContractVersion(
                major = current.major + 1,
                minor = 0,
            ),
        )

        val catalog = LauncherSearchProviderContract.evaluate(listOf(registration))

        assertTrue(catalog.providers.isEmpty())
        assertEquals(
            LauncherSearchProviderRejectionReason.INCOMPATIBLE_CONTRACT_VERSION,
            catalog.rejections.single().reason,
        )
    }

    @Test
    fun duplicateProviderIdKeepsFirstAndRejectsLaterRegistration() {
        val first = stubProvider("duplicate")
        val second = stubProvider("duplicate")

        val catalog = LauncherSearchProviderContract.evaluate(
            listOf(
                registration(first),
                registration(second),
            ),
        )

        assertEquals(listOf(first), catalog.providers)
        assertEquals(
            listOf(
                LauncherSearchProviderRejection(
                    providerId = "duplicate",
                    reason = LauncherSearchProviderRejectionReason.DUPLICATE_PROVIDER_ID,
                ),
            ),
            catalog.rejections,
        )
    }

    @Test
    fun incompatibleRegistrationDoesNotClaimIdFromLaterCompatibleProvider() {
        val incompatible = stubProvider("shared")
        val compatible = stubProvider("shared")
        val current = LauncherSearchProviderContract.currentVersion

        val catalog = LauncherSearchProviderContract.evaluate(
            listOf(
                registration(
                    provider = incompatible,
                    version = LauncherSearchProviderContractVersion(
                        major = current.major,
                        minor = current.minor + 1,
                    ),
                ),
                registration(compatible),
            ),
        )

        assertEquals(listOf(compatible), catalog.providers)
        assertEquals(
            listOf(
                LauncherSearchProviderRejection(
                    providerId = "shared",
                    reason = LauncherSearchProviderRejectionReason.INCOMPATIBLE_CONTRACT_VERSION,
                ),
            ),
            catalog.rejections,
        )
    }

    @Test
    fun registrationRequiresMetadataIdentityToMatchProvider() {
        var rejected = false
        try {
            LauncherSearchProviderRegistration(
                provider = stubProvider("actual"),
                metadata = localMetadata(providerId = "different"),
            )
        } catch (_: IllegalArgumentException) {
            rejected = true
        }

        assertTrue(rejected)
    }

    private fun registration(
        provider: LauncherSearchProvider,
        version: LauncherSearchProviderContractVersion = LauncherSearchProviderContract.currentVersion,
    ): LauncherSearchProviderRegistration = LauncherSearchProviderRegistration(
        provider = provider,
        metadata = localMetadata(providerId = provider.id, version = version),
    )

    private fun localMetadata(
        providerId: String,
        version: LauncherSearchProviderContractVersion = LauncherSearchProviderContract.currentVersion,
    ): LauncherSearchProviderMetadata = LauncherSearchProviderMetadata(
        providerId = providerId,
        contractVersion = version,
        provenance = LauncherSearchProviderProvenance.LAUNCHER_BUILT_IN,
        offlineBehavior = LauncherSearchOfflineBehavior.LOCAL_ONLY,
        authorizationRequirement = LauncherSearchAuthorizationRequirement.NONE,
        remoteProcessing = LauncherSearchRemoteProcessing.NONE,
        queryRetention = LauncherSearchQueryRetention.NONE,
    )

    private fun stubProvider(id: String): LauncherSearchProvider = object : LauncherSearchProvider {
        override val id: String = id

        override fun search(rawQuery: String): List<LauncherSearchResult> = emptyList()
    }
}
