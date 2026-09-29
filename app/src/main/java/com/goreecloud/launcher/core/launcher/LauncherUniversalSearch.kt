package com.goreecloud.launcher.core.launcher

import android.content.pm.LauncherActivityInfo
import android.os.Process
import android.os.UserHandle
import java.text.Normalizer
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Launcher-owned search provider contract.
 *
 * Providers return normalized result metadata and an optional action. A provider failure is
 * isolated by [LauncherUniversalSearch] so one optional source cannot disable core search.
 */
interface LauncherSearchProvider {
    val id: String

    fun search(rawQuery: String): List<LauncherSearchResult>
}

/**
 * Optional asynchronous execution contract for providers that need cooperative cancellation.
 *
 * Providers implementing this interface are executed through [LauncherUniversalSearch.searchAsync]
 * without blocking the caller. The synchronous [LauncherSearchProvider.search] contract remains the
 * built-in/local compatibility path for specialized synchronous surfaces such as drawer filtering.
 */
interface LauncherAsyncSearchProvider {
    suspend fun searchAsync(request: LauncherSearchRequest): List<LauncherSearchResult>
}

/**
 * Marker contract for a reviewed network or third-party provider that may receive live typed
 * queries only after the user explicitly enables inline execution for that source.
 *
 * Registration alone never grants this authority. The provider must still be enabled through the
 * Launcher source controls, and its metadata remains subject to the normal provider contract.
 */
interface LauncherOptInRemoteInlineSearchProvider :
    LauncherSearchProvider,
    LauncherAsyncSearchProvider {
    /**
     * Whether this provider currently has the governed credential/authorization/configuration
     * required to receive live typed queries.
     *
     * Opt-in alone is not configuration. An unconfigured provider remains visible to source
     * management but must fail closed out of the live result stream. Readiness is a boolean
     * execution gate only; active credential values must remain outside this contract.
     */
    val isInlineExecutionReady: Boolean
}

data class LauncherSearchRequest(
    val rawQuery: String,
)

/**
 * Caller-owned execution policy for asynchronous provider aggregation.
 *
 * There is intentionally no product-default timeout here. Callers may execute with cooperative
 * cancellation only, or supply a measured positive provider timeout when they have evidence for an
 * interaction budget. A timeout must not be invented merely to satisfy this contract.
 */
data class LauncherSearchExecutionPolicy(
    val providerTimeoutMillis: Long?,
) {
    init {
        require(providerTimeoutMillis == null || providerTimeoutMillis > 0L) {
            "providerTimeoutMillis must be null or greater than zero"
        }
    }

    companion object {
        fun cancellationOnly(): LauncherSearchExecutionPolicy =
            LauncherSearchExecutionPolicy(providerTimeoutMillis = null)
    }
}

enum class LauncherSearchCategory {
    APPLICATION,
    SHORTCUT,
    CONTACT,
    CALL_HISTORY,
    MESSAGE,
    FILE,
    CONNECTED_SOURCE,
    SETTING,
    ACTION,
}

/**
 * Version of the Launcher provider-registration contract.
 *
 * Major changes are incompatible. A Launcher host may accept a provider from the same major version
 * only when that provider does not require a newer minor revision than the host understands.
 */
data class LauncherSearchProviderContractVersion(
    val major: Int,
    val minor: Int,
) {
    init {
        require(major >= 0) { "major must be non-negative" }
        require(minor >= 0) { "minor must be non-negative" }
    }
}

/** Descriptive source provenance. This value never grants provider authorization by itself. */
enum class LauncherSearchProviderProvenance {
    LAUNCHER_BUILT_IN,
    GOREECLOUD_FIRST_PARTY,
    THIRD_PARTY,
}

enum class LauncherSearchOfflineBehavior {
    LOCAL_ONLY,
    OFFLINE_CAPABLE,
    NETWORK_REQUIRED,
}

enum class LauncherSearchAuthorizationRequirement {
    NONE,
    USER_CONSENT,
    ACCOUNT,
    SYSTEM_POLICY,
}

enum class LauncherSearchRemoteProcessing {
    NONE,
    OPTIONAL,
    REQUIRED,
}

enum class LauncherSearchQueryRetention {
    NONE,
    SESSION_ONLY,
    PERSISTENT,
    UNKNOWN,
}

/**
 * Explicit registration metadata for one provider implementation.
 *
 * Metadata is declarative contract information only. It does not bypass Android permissions,
 * GoreeCloud policy, user consent, profile isolation, or any later external-provider trust review.
 */
data class LauncherSearchProviderMetadata(
    val providerId: String,
    val contractVersion: LauncherSearchProviderContractVersion,
    val provenance: LauncherSearchProviderProvenance,
    val offlineBehavior: LauncherSearchOfflineBehavior,
    val authorizationRequirement: LauncherSearchAuthorizationRequirement,
    val remoteProcessing: LauncherSearchRemoteProcessing,
    val queryRetention: LauncherSearchQueryRetention,
) {
    init {
        require(providerId.isNotBlank()) { "providerId must not be blank" }
    }
}

data class LauncherSearchProviderRegistration(
    val provider: LauncherSearchProvider,
    val metadata: LauncherSearchProviderMetadata,
) {
    init {
        require(provider.id == metadata.providerId) {
            "provider id must match registration metadata"
        }
    }
}

enum class LauncherSearchProviderRejectionReason {
    DUPLICATE_PROVIDER_ID,
    INCOMPATIBLE_CONTRACT_VERSION,
}

data class LauncherSearchProviderRejection(
    val providerId: String,
    val reason: LauncherSearchProviderRejectionReason,
)

data class LauncherSearchProviderCatalog(
    val acceptedRegistrations: List<LauncherSearchProviderRegistration>,
    val rejections: List<LauncherSearchProviderRejection>,
) {
    val providers: List<LauncherSearchProvider>
        get() = acceptedRegistrations.map { registration -> registration.provider }
}

/**
 * Host-side compatibility and fail-closed registration evaluation.
 *
 * This is intentionally not external provider discovery. Callers must supply already reviewed
 * registrations. Duplicate IDs and versions newer than this host understands are excluded rather
 * than silently replacing an accepted provider.
 */
object LauncherSearchProviderContract {
    val currentVersion = LauncherSearchProviderContractVersion(major = 1, minor = 0)

    fun isCompatible(version: LauncherSearchProviderContractVersion): Boolean =
        version.major == currentVersion.major && version.minor <= currentVersion.minor

    fun evaluate(
        registrations: List<LauncherSearchProviderRegistration>,
    ): LauncherSearchProviderCatalog {
        val acceptedIds = mutableSetOf<String>()
        val accepted = mutableListOf<LauncherSearchProviderRegistration>()
        val rejected = mutableListOf<LauncherSearchProviderRejection>()

        registrations.forEach { registration ->
            val providerId = registration.provider.id
            val rejectionReason = when {
                !isCompatible(registration.metadata.contractVersion) ->
                    LauncherSearchProviderRejectionReason.INCOMPATIBLE_CONTRACT_VERSION
                !acceptedIds.add(providerId) -> LauncherSearchProviderRejectionReason.DUPLICATE_PROVIDER_ID
                else -> null
            }

            if (rejectionReason == null) {
                accepted += registration
            } else {
                rejected += LauncherSearchProviderRejection(
                    providerId = providerId,
                    reason = rejectionReason,
                )
            }
        }

        return LauncherSearchProviderCatalog(
            acceptedRegistrations = accepted,
            rejections = rejected,
        )
    }
}

interface LauncherSearchAction

data class LauncherSearchResult(
    val providerId: String,
    val resultId: String,
    val title: String,
    val subtitle: String?,
    val category: LauncherSearchCategory,
    val score: Int,
    val action: LauncherSearchAction? = null,
)

data class LaunchApplicationSearchAction(
    val app: LauncherActivityInfo,
) : LauncherSearchAction

enum class LauncherSearchDestination {
    HOME,
    APPS,
    SETTINGS,
    HOME_EDITOR,
    WALLPAPER,
    THEME_MANAGER,
}

data class LauncherNavigateSearchAction(
    val destination: LauncherSearchDestination,
) : LauncherSearchAction

/**
 * First built-in Universal Search provider. Android LauncherApps remains application-inventory
 * authority; this provider only projects that authoritative inventory into Launcher search.
 *
 * Results expose a small profile label so same-name personal/work applications are distinguishable
 * without merging or reclassifying Android's authoritative per-user inventory.
 */
class LauncherInstalledAppsSearchProvider(
    private val apps: List<LauncherActivityInfo>,
    private val primaryUser: UserHandle = Process.myUserHandle(),
) : LauncherSearchProvider {
    override val id: String = PROVIDER_ID

    override fun search(rawQuery: String): List<LauncherSearchResult> =
        apps.mapNotNull { app ->
            val label = app.label.toString()
            val packageName = app.componentName.packageName
            val profileLabel = if (app.user == primaryUser) "User" else "Work"
            val subtitle = "$profileLabel · $packageName"
            val score = LauncherSearchTextRanking.score(
                title = label,
                subtitle = subtitle,
                rawQuery = rawQuery,
            ) ?: return@mapNotNull null

            LauncherSearchResult(
                providerId = id,
                resultId = app.user.hashCode().toString() + ":" + app.componentName.flattenToString(),
                title = label,
                subtitle = subtitle,
                category = LauncherSearchCategory.APPLICATION,
                score = score,
                action = LaunchApplicationSearchAction(app),
            )
        }

    companion object {
        const val PROVIDER_ID = "launcher.installed-apps"
    }
}

/**
 * Launcher-owned local actions that make Universal Search an action surface as well as a discovery
 * surface. These entries require no network access and do not transfer authority to another
 * GoreeCloud service.
 */
class LauncherCoreActionsSearchProvider : LauncherSearchProvider {
    override val id: String = PROVIDER_ID

    override fun search(rawQuery: String): List<LauncherSearchResult> =
        entries.mapNotNull { entry ->
            val score = LauncherSearchTextRanking.score(
                title = entry.title,
                subtitle = listOfNotNull(entry.subtitle, entry.searchTerms)
                    .joinToString(separator = " "),
                rawQuery = rawQuery,
            ) ?: return@mapNotNull null

            LauncherSearchResult(
                providerId = id,
                resultId = entry.id,
                title = entry.title,
                subtitle = entry.subtitle,
                category = entry.category,
                score = score + CORE_ACTION_SCORE_BIAS,
                action = LauncherNavigateSearchAction(entry.destination),
            )
        }

    private data class Entry(
        val id: String,
        val title: String,
        val subtitle: String?,
        val searchTerms: String,
        val category: LauncherSearchCategory,
        val destination: LauncherSearchDestination,
    )

    companion object {
        const val PROVIDER_ID = "launcher.core-actions"
        private const val CORE_ACTION_SCORE_BIAS = 25

        private val entries = listOf(
            Entry(
                id = "apps",
                title = "Apps",
                subtitle = "Browse installed applications",
                searchTerms = "drawer applications installed browse",
                category = LauncherSearchCategory.ACTION,
                destination = LauncherSearchDestination.APPS,
            ),
            Entry(
                id = "home",
                title = "Home",
                subtitle = "Return to the primary Launcher surface",
                searchTerms = "launcher start workspace",
                category = LauncherSearchCategory.ACTION,
                destination = LauncherSearchDestination.HOME,
            ),
            Entry(
                id = "home-editor",
                title = "Edit Home",
                subtitle = "Pages, wallpaper, apps and Launcher settings",
                searchTerms = "home editor edit customize pages layout",
                category = LauncherSearchCategory.ACTION,
                destination = LauncherSearchDestination.HOME_EDITOR,
            ),
            Entry(
                id = "wallpaper",
                title = "Wallpaper",
                subtitle = "Choose the Home wallpaper",
                searchTerms = "background appearance personalize home",
                category = LauncherSearchCategory.SETTING,
                destination = LauncherSearchDestination.WALLPAPER,
            ),
            Entry(
                id = "theme-manager",
                title = "Theme Manager",
                subtitle = "Preview and choose Launcher appearance",
                searchTerms = "theme appearance light dark deep dark glaze",
                category = LauncherSearchCategory.SETTING,
                destination = LauncherSearchDestination.THEME_MANAGER,
            ),
        )
    }
}

/**
 * Trusted built-in provider registration for core Launcher search.
 *
 * Built-ins are explicitly cataloged with local-only/no-retention metadata and the current contract
 * version. This registry remains allowlisted and in-process. Optional external providers require a
 * separately reviewed discovery/trust boundary and are not activated by this metadata model.
 */
object LauncherBuiltInSearchProviderRegistry {
    fun registrations(apps: List<LauncherActivityInfo>): List<LauncherSearchProviderRegistration> =
        listOf(
            builtInRegistration(LauncherCoreActionsSearchProvider()),
            builtInRegistration(LauncherInstalledAppsSearchProvider(apps)),
        )

    fun catalog(apps: List<LauncherActivityInfo>): LauncherSearchProviderCatalog =
        LauncherSearchProviderContract.evaluate(registrations(apps))

    fun providers(apps: List<LauncherActivityInfo>): List<LauncherSearchProvider> =
        catalog(apps).providers

    internal fun metadataFor(providerId: String): LauncherSearchProviderMetadata =
        LauncherSearchProviderMetadata(
            providerId = providerId,
            contractVersion = LauncherSearchProviderContract.currentVersion,
            provenance = LauncherSearchProviderProvenance.LAUNCHER_BUILT_IN,
            offlineBehavior = LauncherSearchOfflineBehavior.LOCAL_ONLY,
            authorizationRequirement = LauncherSearchAuthorizationRequirement.NONE,
            remoteProcessing = LauncherSearchRemoteProcessing.NONE,
            queryRetention = LauncherSearchQueryRetention.NONE,
        )

    private fun builtInRegistration(
        provider: LauncherSearchProvider,
    ): LauncherSearchProviderRegistration = LauncherSearchProviderRegistration(
        provider = provider,
        metadata = metadataFor(provider.id),
    )
}

object LauncherUniversalSearch {
    fun search(
        rawQuery: String,
        providers: List<LauncherSearchProvider>,
    ): List<LauncherSearchResult> =
        normalizeResults(
            providers.flatMap { provider ->
                runCatching { provider.search(rawQuery) }
                    .getOrDefault(emptyList())
            },
        )

    /**
     * Executes providers concurrently with caller-owned cancellation/timeout policy.
     *
     * Providers that implement [LauncherAsyncSearchProvider] receive a cancellable suspend request.
     * Existing synchronous providers run on [Dispatchers.Default] so the caller is not blocked.
     * Provider failures and measured provider-local timeouts fail soft to an empty contribution.
     * Caller cancellation always propagates and is never converted into an ordinary provider failure.
     */
    suspend fun searchAsync(
        rawQuery: String,
        providers: List<LauncherSearchProvider>,
        policy: LauncherSearchExecutionPolicy,
    ): List<LauncherSearchResult> = coroutineScope {
        val request = LauncherSearchRequest(rawQuery = rawQuery)
        val providerResults = providers.map { provider ->
            async {
                executeProviderAsync(
                    provider = provider,
                    request = request,
                    policy = policy,
                )
            }
        }.awaitAll()

        normalizeResults(providerResults.flatten())
    }

    private suspend fun executeProviderAsync(
        provider: LauncherSearchProvider,
        request: LauncherSearchRequest,
        policy: LauncherSearchExecutionPolicy,
    ): List<LauncherSearchResult> {
        return try {
            val executeProvider: suspend () -> List<LauncherSearchResult> = {
                if (provider is LauncherAsyncSearchProvider) {
                    provider.searchAsync(request)
                } else {
                    withContext(Dispatchers.Default) {
                        provider.search(request.rawQuery)
                    }
                }
            }

            policy.providerTimeoutMillis?.let { timeoutMillis ->
                withTimeoutOrNull(timeoutMillis) {
                    executeProvider()
                }.orEmpty()
            } ?: executeProvider()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun normalizeResults(
        results: List<LauncherSearchResult>,
    ): List<LauncherSearchResult> {
        val distinctResults = results.distinctBy { result -> result.providerId to result.resultId }

        // Blank app-drawer browsing produces zero-scored results from the already normalized
        // LauncherApps inventory. Preserve that provider order instead of normalizing every title
        // and sorting the full list again on drawer entry/recomposition.
        if (distinctResults.all { result -> result.score == 0 }) {
            return distinctResults
        }

        return distinctResults.sortedWith(
            compareByDescending<LauncherSearchResult> { it.score }
                .thenBy { LauncherSearchTextRanking.normalize(it.title) }
                .thenBy { it.providerId }
                .thenBy { it.resultId },
        )
    }
}

/**
 * Small deterministic local ranking policy used by the built-in Launcher providers.
 *
 * Blank queries preserve browse behavior. Non-blank queries prioritize exact title, title prefix,
 * title substring, then searchable metadata matches. This is intentionally local and
 * telemetry-free.
 */
object LauncherSearchTextRanking {
    fun score(
        title: String,
        subtitle: String?,
        rawQuery: String,
    ): Int? {
        val query = normalize(rawQuery).trim()
        if (query.isEmpty()) return 0

        val normalizedTitle = normalize(title)
        val normalizedSubtitle = normalize(subtitle.orEmpty())

        return when {
            normalizedTitle == query -> 400
            normalizedTitle.startsWith(query) -> 300
            normalizedTitle.contains(query) -> 200
            normalizedSubtitle.contains(query) -> 100
            else -> null
        }
    }

    fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFC).lowercase(Locale.ROOT)
}
