package app.lawnchair.search.algorithms.engine.provider.web

import android.content.Context
import android.net.Uri
import android.util.Log
import app.lawnchair.preferences2.PreferenceManager2
import app.lawnchair.preferences2.firstCached
import com.android.launcher3.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

/**
 * A WebSearchProvider that uses user-defined URLs for searching and suggestions.
 */
object CustomWebSearchProvider : WebSearchProvider {
    private const val TAG = "CustomWebSearchProvider"

    override val id: String = "custom"

    /**
     * The label for this search provider.
     *
     * This label is dynamic. If the user provides a name for the custom search provider,
     * that name will be used. Otherwise, a default label will be used.
     *
     * Note: This property returns a raw [String], not a `@StringRes`. The UI layer
     * is responsible for handling this and displaying the appropriate string resource
     * if the user-provided name is not available.
     *
     * For simplicity in the [WebSearchProvider] interface, this property points to a
     * generic string resource ([R.string.search_provider_custom]) as a fallback.
     */
    override val label: Int = R.string.search_provider_custom

    override val iconRes: Int = R.drawable.ic_search

    private var searchUrlTemplate: String = ""
    private var suggestionsUrlTemplate: String = ""
    private var displayName: String = ""
    // A user-approved endpoint must not silently redirect typed terms to another host
    // or downgrade a HTTPS suggestion request to HTTP.
    private val okHttpClient = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    fun getDisplayName(): String = displayName

    override fun configure(context: Context): WebSearchProvider {
        val prefs = PreferenceManager2.getInstance(context)
        searchUrlTemplate = prefs.webSuggestionProviderUrl.firstCached()
        suggestionsUrlTemplate = prefs.webSuggestionProviderSuggestionsUrl.firstCached()
        displayName = prefs.webSuggestionProviderName.firstCached()
        return this
    }

    override fun getSuggestions(query: String): Flow<List<String>> = flow {
        if (query.isBlank() || !isValidHttpsTemplate(suggestionsUrlTemplate)) {
            emit(emptyList())
            return@flow
        }

        try {
            val encodedQuery = Uri.encode(query)
            val url = suggestionsUrlTemplate.replace("%s", encodedQuery)

            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body.string()
                    // We assume a standard OpenSearch format, as it's the most common.
                    val suggestions = parseOpenSearchResponse(responseBody)
                    emit(suggestions)
                } else {
                    Log.w(TAG, "Failed to retrieve suggestions: ${response.code}")
                    emit(emptyList())
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Custom web suggestions unavailable")
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    override fun getSearchUrl(query: String): String {
        if (!isValidHttpsTemplate(searchUrlTemplate)) {
            // Invalid custom configuration never redirects a query to another provider.
            return ""
        }
        val encodedQuery = Uri.encode(query)
        return searchUrlTemplate.replace("%s", encodedQuery)
    }

    /**
     * The user's custom endpoint must be HTTPS and explicitly interpolate the
     * search term. This prevents insecure query transmission or a forgotten
     * template from quietly choosing an unrelated external search service.
     */
    fun isValidHttpsTemplate(template: String): Boolean {
        if (!template.startsWith("https://", ignoreCase = true) || !template.contains("%s")) {
            return false
        }
        val uri = Uri.parse(template.replace("%s", "query"))
        return uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()
    }

    private fun parseOpenSearchResponse(responseBody: String): List<String> {
        return try {
            val jsonArray = JSONArray(responseBody)
            val suggestionsArray = jsonArray.getJSONArray(1)
            (0 until suggestionsArray.length()).map { suggestionsArray.getString(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Custom web suggestion response invalid")
            emptyList()
        }
    }

    override fun toString(): String = id
}
