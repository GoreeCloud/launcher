package com.goreecloud.launcher.core.launcher

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

private class LauncherConnectedSearchProvider(
    override val id: String,
) : LauncherSearchProvider {
    override fun search(rawQuery: String): List<LauncherSearchResult> = emptyList()
}

private data class LauncherConnectedSearchDefinition(
    val providerId: String,
    val displayName: String,
    val authorizationRequirement: LauncherSearchAuthorizationRequirement,
    val requiresResolution: Boolean,
    val buildIntent: (String) -> Intent,
)

object LauncherConnectedSearchProviderRegistry {
    const val GOOGLE_DRIVE_PROVIDER_ID = "connected.google-drive"
    const val DROPBOX_PROVIDER_ID = "connected.dropbox"
    const val BRAVE_SEARCH_PROVIDER_ID = "connected.brave-search"

    private const val GOOGLE_DRIVE_PACKAGE = "com.google.android.apps.docs"
    private const val DROPBOX_PACKAGE = "com.dropbox.android"

    fun registrations(context: Context): List<LauncherSearchProviderRegistration> =
        availableDefinitions(context).map { definition ->
            LauncherSearchProviderRegistration(
                provider = LauncherConnectedSearchProvider(definition.providerId),
                metadata = LauncherSearchProviderMetadata(
                    providerId = definition.providerId,
                    contractVersion = LauncherSearchProviderContract.currentVersion,
                    provenance = LauncherSearchProviderProvenance.THIRD_PARTY,
                    offlineBehavior = LauncherSearchOfflineBehavior.NETWORK_REQUIRED,
                    authorizationRequirement = definition.authorizationRequirement,
                    remoteProcessing = LauncherSearchRemoteProcessing.REQUIRED,
                    queryRetention = LauncherSearchQueryRetention.UNKNOWN,
                ),
            )
        }

    fun buildExplicitHandoffIntent(
        context: Context,
        providerId: String,
        rawQuery: String,
    ): Intent? {
        val query = rawQuery.trim()
        if (query.isBlank()) return null
        val definition = availableDefinitions(context)
            .firstOrNull { it.providerId == providerId }
            ?: return null
        val intent = definition.buildIntent(query)
        return if (!definition.requiresResolution || resolves(context, intent)) intent else null
    }

    private fun availableDefinitions(context: Context): List<LauncherConnectedSearchDefinition> =
        definitions().filter { definition ->
            !definition.requiresResolution ||
                resolves(context, definition.buildIntent("goreecloud"))
        }

    private fun definitions(): List<LauncherConnectedSearchDefinition> = listOf(
        LauncherConnectedSearchDefinition(
            providerId = GOOGLE_DRIVE_PROVIDER_ID,
            displayName = "Google Drive",
            authorizationRequirement = LauncherSearchAuthorizationRequirement.ACCOUNT,
            // Keep Drive visible even when it does not publish an exported search Activity.
            // An explicit tap may open its website; automatic local Search never sends queries.
            requiresResolution = false,
            buildIntent = { query ->
                Intent(Intent.ACTION_VIEW, Uri.Builder()
                    .scheme("https")
                    .authority("drive.google.com")
                    .appendPath("drive")
                    .appendPath("u")
                    .appendPath("0")
                    .appendPath("search")
                    .appendQueryParameter("q", query)
                    .build())
            },
        ),
        LauncherConnectedSearchDefinition(
            providerId = DROPBOX_PROVIDER_ID,
            displayName = "Dropbox",
            authorizationRequirement = LauncherSearchAuthorizationRequirement.ACCOUNT,
            requiresResolution = true,
            buildIntent = { query ->
                Intent(Intent.ACTION_SEARCH)
                    .setPackage(DROPBOX_PACKAGE)
                    .putExtra(SearchManager.QUERY, query)
            },
        ),
        LauncherConnectedSearchDefinition(
            providerId = BRAVE_SEARCH_PROVIDER_ID,
            displayName = "Brave Search",
            authorizationRequirement = LauncherSearchAuthorizationRequirement.NONE,
            requiresResolution = false,
            buildIntent = { query ->
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.Builder()
                        .scheme("https")
                        .authority("search.brave.com")
                        .appendPath("search")
                        .appendQueryParameter("q", query)
                        .build(),
                )
            },
        ),
    )

    private fun resolves(context: Context, intent: Intent): Boolean =
        context.packageManager.resolveActivity(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY,
        ) != null
}
