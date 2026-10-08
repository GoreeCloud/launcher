package com.goreecloud.launcher.core.launcher

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.launcherFileSearchPreferencesStore by preferencesDataStore(
    name = "launcher_file_search_preferences",
)

/**
 * Device-local Storage Access Framework roots explicitly selected for Launcher file search.
 *
 * Only URI grants are persisted. File names, file contents, queries, and search-result history are
 * not written here. This DataStore is outside the repository's current backup include domains.
 */
class LauncherFileSearchPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) {
    constructor(context: Context) : this(context.launcherFileSearchPreferencesStore)

    private object Keys {
        val roots = stringSetPreferencesKey("search_file_roots")
    }

    val roots: Flow<List<Uri>> = dataStore.data
        .map { values ->
            values[Keys.roots]
                .orEmpty()
                .mapNotNull { raw -> runCatching { Uri.parse(raw) }.getOrNull() }
                .distinctBy(Uri::toString)
                .sortedBy(Uri::toString)
        }
        .distinctUntilChanged()

    suspend fun addRoot(uri: Uri) {
        val raw = uri.toString()
        if (raw.isBlank()) return
        dataStore.edit { values ->
            values[Keys.roots] = values[Keys.roots].orEmpty() + raw
        }
    }

    suspend fun removeRoot(uri: Uri) {
        dataStore.edit { values ->
            values[Keys.roots] = values[Keys.roots].orEmpty() - uri.toString()
        }
    }

    suspend fun clear() {
        dataStore.edit { values -> values.remove(Keys.roots) }
    }
}
