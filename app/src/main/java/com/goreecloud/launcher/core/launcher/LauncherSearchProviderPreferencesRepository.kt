package com.goreecloud.launcher.core.launcher

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.launcherSearchProviderPreferencesStore by preferencesDataStore(
    name = "launcher_search_provider_preferences",
)

/**
 * Launcher-local persistence for Universal Search provider enable/order controls.
 *
 * This store deliberately remains separate from the strict portable-preference v1 backup/recovery
 * contract. It persists only the versioned [LauncherSearchProviderPreferenceSnapshot] payload from
 * [LauncherSearchProviderPreferenceContract]; queries, results, history, credentials, provider
 * payloads, authorization grants, and usage/frequency signals are outside this boundary.
 */
class LauncherSearchProviderPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) {
    constructor(context: Context) : this(context.launcherSearchProviderPreferencesStore)

    private object Keys {
        val providerPreferences = stringPreferencesKey("search_provider_preferences_v1")
    }

    /**
     * Decoded persisted state. Missing storage remains [LauncherSearchProviderPreferenceDecodeResult.Absent],
     * while a stored snapshot with an empty enabled set remains an explicit loaded user choice.
     * Invalid or unsupported stored values are surfaced fail-closed rather than silently reset.
     */
    val preferences: Flow<LauncherSearchProviderPreferenceDecodeResult> = dataStore.data
        .map { values ->
            LauncherSearchProviderPreferenceContract.decode(values[Keys.providerPreferences])
        }
        .distinctUntilChanged()

    suspend fun read(): LauncherSearchProviderPreferenceDecodeResult = preferences.first()

    /** Persist an explicit provider-control snapshot, including an explicit empty enabled set. */
    suspend fun set(snapshot: LauncherSearchProviderPreferenceSnapshot) {
        val encoded = LauncherSearchProviderPreferenceContract.encode(snapshot)
        dataStore.edit { values ->
            values[Keys.providerPreferences] = encoded
        }
    }

    /** Restore first-run absence without manufacturing a default snapshot. */
    suspend fun clear() {
        dataStore.edit { values ->
            values.remove(Keys.providerPreferences)
        }
    }
}
