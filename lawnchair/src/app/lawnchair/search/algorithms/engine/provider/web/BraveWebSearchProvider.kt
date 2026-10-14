package app.lawnchair.search.algorithms.engine.provider.web

import android.net.Uri
import com.android.launcher3.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * GoreeCloud-approved interim web provider. Search is dispatched exclusively
 * on user action; suggestion requests intentionally never leave the device.
 * Replace only after GoreeCloud Search AND GoreeCloud Index reach Stable.
 */
object BraveWebSearchProvider : WebSearchProvider {
    override val id = "brave"
    override val label = R.string.search_provider_brave
    override val iconRes = R.drawable.ic_apps
    override fun getSuggestions(query: String): Flow<List<String>> = flowOf(emptyList())
    override fun getSearchUrl(query: String): String =
        Uri.Builder().scheme("https").authority("search.brave.com").appendPath("search")
            .appendQueryParameter("q", query).build().toString()
    override fun toString(): String = id
}
