package app.lawnchair.search.algorithms.engine.provider

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import app.lawnchair.search.LawnchairRecentSuggestionProvider
import app.lawnchair.search.algorithms.data.RecentKeyword
import app.lawnchair.search.algorithms.engine.SearchResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

object HistorySearchProvider {
    private const val MAX_BUFFERED_HISTORY_ROWS = 512
    /**
     * Fetches recent search keywords from the suggestion provider.
     * This is a suspend function that performs its work on the IO dispatcher.
     *
     * @param context The application context.
     * @param maxResults The maximum number of keywords to return.
     * @return A list of [SearchResult.History] items.
     */
    suspend fun getRecentKeywords(context: Context, maxResults: Int): List<SearchResult.History> {
        return withContext(Dispatchers.IO) {
            if (maxResults <= 0) return@withContext emptyList<SearchResult.History>()
            try {
                val contentResolver: ContentResolver = context.contentResolver
                val uri: Uri = "content://${LawnchairRecentSuggestionProvider.AUTHORITY}/suggestions".toUri()

                // The generic SearchRecentSuggestionsProvider URI accepts an
                // explicit sort order. Read oldest first so the bounded deque
                // retains the most recent rows, even for large databases.
                val cursor: Cursor? = contentResolver.query(
                    uri, null, null, null, "date ASC, _id ASC",
                )
                val recentKeywords = ArrayDeque<RecentKeyword>()

                cursor?.use {
                    val columnCount = it.columnCount
                    var scannedRows = 0
                    while (it.moveToNext()) {
                        // Providers may return thousands of rows; abandon a
                        // cancelled search rather than scanning to the end.
                        if (scannedRows++ % 64 == 0) currentCoroutineContext().ensureActive()
                        val recentKeywordData = mutableMapOf<String, String>()
                        for (i in 0 until columnCount) {
                            val columnName = it.getColumnName(i)
                            val columnValue = it.getString(i) ?: ""
                            recentKeywordData[columnName] = columnValue
                        }
                        if (recentKeywords.size >= MAX_BUFFERED_HISTORY_ROWS) recentKeywords.removeFirst()
                        recentKeywords.addLast(RecentKeyword(recentKeywordData))
                    }
                }

                recentKeywords.toList().asReversed()
                    // Search history is actionable UI: omit empty entries and
                    // keep only the latest occurrence of a repeated query.
                    .filter { !it.getValueByKey("display1").isNullOrBlank() }
                    .distinctBy { it.getValueByKey("display1")!!.trim().lowercase() }
                    .take(maxResults.coerceIn(0, 20))
                    .map { SearchResult.History(data = it) }
            } catch (e: CancellationException) {
                // Never suppress cancellation of an abandoned search request.
                throw e
            } catch (e: Exception) {
                Log.w("HistorySearchProvider", "Recent search history unavailable")
                emptyList()
            }
        }
    }
}
