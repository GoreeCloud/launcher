package com.goreecloud.launcher.core.launcher

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject

private class LauncherConnectedSearchProvider(
    override val id: String,
) : LauncherSearchProvider {
    override fun search(rawQuery: String): List<LauncherSearchResult> = emptyList()
}

/**
 * Process-local Google Drive authorization state.
 *
 * Access tokens are intentionally kept only in memory. They are never written to DataStore, Room,
 * logs, diagnostics, or repository state. The revision flow contains no credential material; it
 * exists only so Compose can rebuild the accepted provider catalog after connect/disconnect or an
 * authorization failure.
 */
object LauncherGoogleDriveAuthorizationState {
    private val tokenState = MutableStateFlow<String?>(null)
    private val revisionState = MutableStateFlow(0L)

    val revision = revisionState.asStateFlow()

    fun isConnected(): Boolean = !tokenState.value.isNullOrBlank()

    internal fun accessTokenOrNull(): String? = tokenState.value

    fun setAccessToken(token: String) {
        val normalized = token.trim()
        require(normalized.isNotEmpty()) { "Google Drive access token must not be blank" }
        tokenState.value = normalized
        revisionState.value = revisionState.value + 1L
    }

    fun clearAccessToken(): String? {
        val previous = tokenState.value
        tokenState.value = null
        revisionState.value = revisionState.value + 1L
        return previous
    }
}

internal data class LauncherGoogleDriveFileHit(
    val id: String,
    val name: String,
    val mimeType: String?,
    val webViewLink: String?,
)

internal fun interface LauncherGoogleDriveSearchTransport {
    suspend fun search(
        accessToken: String,
        rawQuery: String,
    ): List<LauncherGoogleDriveFileHit>
}

private class LauncherGoogleDriveAuthorizationRejectedException :
    IllegalStateException("Google Drive authorization was rejected")

/**
 * Bounded Drive API v3 transport.
 *
 * The bearer token is supplied only as an Authorization header to Google's HTTPS endpoint and is
 * never persisted. Cancellation disconnects the active HTTP request. Query text is sent only after
 * the user has explicitly enabled and authorized the Google Drive Search source.
 */
internal class LauncherGoogleDriveHttpSearchTransport(
    private val connectTimeoutMillis: Int = 5_000,
    private val readTimeoutMillis: Int = 5_000,
    private val resultLimit: Int = 8,
) : LauncherGoogleDriveSearchTransport {
    override suspend fun search(
        accessToken: String,
        rawQuery: String,
    ): List<LauncherGoogleDriveFileHit> = withContext(Dispatchers.IO) {
        val query = rawQuery.trim()
        if (query.isBlank()) return@withContext emptyList()

        val escapedQuery = query
            .take(MAX_QUERY_LENGTH)
            .replace("\\", "\\\\")
            .replace("'", "\\'")
        val requestUri = Uri.Builder()
            .scheme("https")
            .authority("www.googleapis.com")
            .appendPath("drive")
            .appendPath("v3")
            .appendPath("files")
            .appendQueryParameter("q", "name contains '$escapedQuery' and trashed = false")
            .appendQueryParameter("spaces", "drive")
            .appendQueryParameter("pageSize", resultLimit.coerceIn(1, 20).toString())
            .appendQueryParameter("orderBy", "modifiedTime desc")
            .appendQueryParameter(
                "fields",
                "files(id,name,mimeType,webViewLink,modifiedTime)",
            )
            .appendQueryParameter("supportsAllDrives", "true")
            .appendQueryParameter("includeItemsFromAllDrives", "true")
            .build()

        val connection = (URL(requestUri.toString()).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = connectTimeoutMillis
            readTimeout = readTimeoutMillis
            instanceFollowRedirects = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $accessToken")
        }
        val cancellationHandle = coroutineContext.job.invokeOnCompletion {
            connection.disconnect()
        }

        try {
            coroutineContext.ensureActive()
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED ||
                responseCode == HttpURLConnection.HTTP_FORBIDDEN
            ) {
                throw LauncherGoogleDriveAuthorizationRejectedException()
            }
            if (responseCode !in 200..299) {
                throw IllegalStateException("Google Drive search request failed")
            }

            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            coroutineContext.ensureActive()
            parseGoogleDriveFiles(body)
        } finally {
            cancellationHandle.dispose()
            connection.disconnect()
        }
    }

    private fun parseGoogleDriveFiles(body: String): List<LauncherGoogleDriveFileHit> {
        val files = JSONObject(body).optJSONArray("files") ?: return emptyList()
        return buildList {
            for (index in 0 until files.length()) {
                val item = files.optJSONObject(index) ?: continue
                val id = item.optString("id").trim()
                val name = item.optString("name").trim()
                if (id.isEmpty() || name.isEmpty()) continue
                add(
                    LauncherGoogleDriveFileHit(
                        id = id,
                        name = name,
                        mimeType = item.optString("mimeType").takeIf { it.isNotBlank() },
                        webViewLink = item.optString("webViewLink").takeIf { it.isNotBlank() },
                    ),
                )
            }
        }
    }

    private companion object {
        const val MAX_QUERY_LENGTH = 200
    }
}

internal class LauncherGoogleDriveSearchProvider(
    private val tokenProvider: () -> String? =
        LauncherGoogleDriveAuthorizationState::accessTokenOrNull,
    private val onAuthorizationRejected: () -> Unit = {
        LauncherGoogleDriveAuthorizationState.clearAccessToken()
    },
    private val transport: LauncherGoogleDriveSearchTransport =
        LauncherGoogleDriveHttpSearchTransport(),
) : LauncherOptInRemoteInlineSearchProvider {
    override val id: String = LauncherConnectedSearchProviderRegistry.GOOGLE_DRIVE_PROVIDER_ID

    override val isInlineExecutionReady: Boolean
        get() = !tokenProvider().isNullOrBlank()

    override fun search(rawQuery: String): List<LauncherSearchResult> = emptyList()

    override suspend fun searchAsync(
        request: LauncherSearchRequest,
    ): List<LauncherSearchResult> {
        val token = tokenProvider()?.takeIf { it.isNotBlank() } ?: return emptyList()
        val hits = try {
            transport.search(token, request.rawQuery)
        } catch (rejected: LauncherGoogleDriveAuthorizationRejectedException) {
            onAuthorizationRejected()
            return emptyList()
        }

        return hits.map { hit ->
            val score = LauncherSearchTextRanking.score(
                title = hit.name,
                subtitle = hit.mimeType,
                rawQuery = request.rawQuery,
            ) ?: 150
            val openUri = hit.webViewLink ?: (
                "https://drive.google.com/open?id=" +
                    URLEncoder.encode(hit.id, StandardCharsets.UTF_8.name())
                )
            LauncherSearchResult(
                providerId = id,
                resultId = "google-drive:" + hit.id,
                title = hit.name,
                subtitle = googleDriveTypeLabel(hit.mimeType),
                category = LauncherSearchCategory.CONNECTED_SOURCE,
                score = score,
                action = LauncherOpenUriSearchAction(
                    intentAction = Intent.ACTION_VIEW,
                    uri = openUri,
                ),
            )
        }
    }

    private fun googleDriveTypeLabel(mimeType: String?): String = when (mimeType) {
        "application/vnd.google-apps.folder" -> "Google Drive folder"
        "application/vnd.google-apps.document" -> "Google Docs document"
        "application/vnd.google-apps.spreadsheet" -> "Google Sheets spreadsheet"
        "application/vnd.google-apps.presentation" -> "Google Slides presentation"
        else -> "Google Drive file"
    }
}

private enum class LauncherConnectedSearchKind {
    CLOUD,
    WEB,
    AI,
}

private data class LauncherConnectedSearchDefinition(
    val providerId: String,
    val displayName: String,
    val authorizationRequirement: LauncherSearchAuthorizationRequirement,
    val requiresResolution: Boolean,
    val iconPackageNames: List<String>,
    val kind: LauncherConnectedSearchKind,
    val buildIntent: (Context, String) -> Intent,
)

object LauncherConnectedSearchProviderRegistry {
    const val GOOGLE_DRIVE_PROVIDER_ID = "connected.google-drive"
    const val DROPBOX_PROVIDER_ID = "connected.dropbox"
    const val GOOGLE_SEARCH_PROVIDER_ID = "connected.google-search"
    const val DUCKDUCKGO_PROVIDER_ID = "connected.duckduckgo"
    const val BRAVE_SEARCH_PROVIDER_ID = "connected.brave-search"
    const val CHATGPT_PROVIDER_ID = "connected.chatgpt"
    const val GEMINI_PROVIDER_ID = "connected.gemini"
    const val PERPLEXITY_PROVIDER_ID = "connected.perplexity"
    const val CLAUDE_PROVIDER_ID = "connected.claude"

    private const val GOOGLE_DRIVE_PACKAGE = "com.google.android.apps.docs"
    const val DROPBOX_PACKAGE = "com.dropbox.android"
    private const val GOOGLE_APP_PACKAGE = "com.google.android.googlequicksearchbox"
    private const val GOOGLE_GEMINI_PACKAGE = "com.google.android.apps.bard"
    private const val DUCKDUCKGO_PACKAGE = "com.duckduckgo.mobile.android"
    private const val BRAVE_BROWSER_PACKAGE = "com.brave.browser"
    private const val CHATGPT_PACKAGE = "com.openai.chatgpt"
    private const val PERPLEXITY_PACKAGE = "ai.perplexity.app.android"
    private const val CLAUDE_PACKAGE = "com.anthropic.claude"

    fun iconPackageNamesFor(providerId: String): List<String> =
        definitions().firstOrNull { it.providerId == providerId }?.iconPackageNames.orEmpty()

    fun iconPackageNameFor(providerId: String): String? =
        iconPackageNamesFor(providerId).firstOrNull()

    fun displayNameFor(providerId: String): String? =
        definitions().firstOrNull { it.providerId == providerId }?.displayName

    fun isConnectedProvider(providerId: String): Boolean =
        definitions().any { it.providerId == providerId }

    fun isAiProvider(providerId: String): Boolean =
        definitions().firstOrNull { it.providerId == providerId }?.kind == LauncherConnectedSearchKind.AI

    fun isWebSearchProvider(providerId: String): Boolean =
        definitions().firstOrNull { it.providerId == providerId }?.kind == LauncherConnectedSearchKind.WEB

    @Suppress("UNUSED_PARAMETER")
    fun registrations(
        context: Context,
        fileRoots: List<Uri> = emptyList(),
    ): List<LauncherSearchProviderRegistration> =
        definitions().map { definition ->
            val provider: LauncherSearchProvider =
                if (definition.providerId == GOOGLE_DRIVE_PROVIDER_ID) {
                    LauncherGoogleDriveSearchProvider()
                } else {
                    LauncherConnectedSearchProvider(definition.providerId)
                }
            LauncherSearchProviderRegistration(
                provider = provider,
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
        val definition = definitions().firstOrNull { it.providerId == providerId } ?: return null
        val intent = definition.buildIntent(context, query)
        return if (!definition.requiresResolution || resolves(context, intent)) intent else null
    }

    fun isExplicitHandoffAvailable(context: Context, providerId: String): Boolean {
        val definition = definitions().firstOrNull { it.providerId == providerId } ?: return false
        return !definition.requiresResolution ||
            resolves(context, definition.buildIntent(context, "goreecloud"))
    }

    private fun shareTextIntent(
        context: Context,
        packageNames: List<String>,
        query: String,
    ): Intent {
        val candidates = packageNames.map { packageName ->
            Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .setPackage(packageName)
                .putExtra(Intent.EXTRA_TEXT, query)
        }
        return candidates.firstOrNull { resolves(context, it) } ?: candidates.first()
    }

    private fun webSearchIntent(
        authority: String,
        path: String,
        query: String,
    ): Intent = Intent(
        Intent.ACTION_VIEW,
        Uri.Builder()
            .scheme("https")
            .authority(authority)
            .apply {
                path.trim('/').takeIf { it.isNotEmpty() }?.split('/')?.forEach { segment ->
                    appendPath(segment)
                }
            }
            .appendQueryParameter("q", query)
            .build(),
    )

    private fun definitions(): List<LauncherConnectedSearchDefinition> = listOf(
        LauncherConnectedSearchDefinition(
            GOOGLE_DRIVE_PROVIDER_ID, "Google Drive",
            LauncherSearchAuthorizationRequirement.ACCOUNT, false,
            listOf(GOOGLE_DRIVE_PACKAGE), LauncherConnectedSearchKind.CLOUD,
        ) { _, query ->
            Intent(
                Intent.ACTION_VIEW,
                Uri.Builder()
                    .scheme("https").authority("drive.google.com")
                    .appendPath("drive").appendPath("u").appendPath("0").appendPath("search")
                    .appendQueryParameter("q", query).build(),
            )
        },
        LauncherConnectedSearchDefinition(
            DROPBOX_PROVIDER_ID, "Dropbox",
            LauncherSearchAuthorizationRequirement.ACCOUNT, true,
            listOf(DROPBOX_PACKAGE), LauncherConnectedSearchKind.CLOUD,
        ) { _, query ->
            Intent(Intent.ACTION_SEARCH).setPackage(DROPBOX_PACKAGE).putExtra(SearchManager.QUERY, query)
        },
        LauncherConnectedSearchDefinition(
            GOOGLE_SEARCH_PROVIDER_ID, "Google Search",
            LauncherSearchAuthorizationRequirement.NONE, false,
            listOf(GOOGLE_APP_PACKAGE), LauncherConnectedSearchKind.WEB,
        ) { _, query -> webSearchIntent("www.google.com", "search", query) },
        LauncherConnectedSearchDefinition(
            DUCKDUCKGO_PROVIDER_ID, "DuckDuckGo",
            LauncherSearchAuthorizationRequirement.NONE, false,
            listOf(DUCKDUCKGO_PACKAGE), LauncherConnectedSearchKind.WEB,
        ) { _, query -> webSearchIntent("duckduckgo.com", "", query) },
        LauncherConnectedSearchDefinition(
            BRAVE_SEARCH_PROVIDER_ID, "Brave Search",
            LauncherSearchAuthorizationRequirement.NONE, false,
            listOf(BRAVE_BROWSER_PACKAGE), LauncherConnectedSearchKind.WEB,
        ) { _, query -> webSearchIntent("search.brave.com", "search", query) },
        LauncherConnectedSearchDefinition(
            CHATGPT_PROVIDER_ID, "ChatGPT",
            LauncherSearchAuthorizationRequirement.NONE, true,
            listOf(CHATGPT_PACKAGE), LauncherConnectedSearchKind.AI,
        ) { context, query -> shareTextIntent(context, listOf(CHATGPT_PACKAGE), query) },
        LauncherConnectedSearchDefinition(
            GEMINI_PROVIDER_ID, "Gemini",
            LauncherSearchAuthorizationRequirement.NONE, true,
            listOf(GOOGLE_GEMINI_PACKAGE, GOOGLE_APP_PACKAGE), LauncherConnectedSearchKind.AI,
        ) { context, query ->
            shareTextIntent(context, listOf(GOOGLE_GEMINI_PACKAGE, GOOGLE_APP_PACKAGE), query)
        },
        LauncherConnectedSearchDefinition(
            PERPLEXITY_PROVIDER_ID, "Perplexity",
            LauncherSearchAuthorizationRequirement.NONE, true,
            listOf(PERPLEXITY_PACKAGE), LauncherConnectedSearchKind.AI,
        ) { context, query -> shareTextIntent(context, listOf(PERPLEXITY_PACKAGE), query) },
        LauncherConnectedSearchDefinition(
            CLAUDE_PROVIDER_ID, "Claude",
            LauncherSearchAuthorizationRequirement.NONE, true,
            listOf(CLAUDE_PACKAGE), LauncherConnectedSearchKind.AI,
        ) { context, query -> shareTextIntent(context, listOf(CLAUDE_PACKAGE), query) },
    )

    private fun resolves(context: Context, intent: Intent): Boolean =
        context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null
}
