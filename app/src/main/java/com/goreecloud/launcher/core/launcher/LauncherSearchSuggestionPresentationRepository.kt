package com.goreecloud.launcher.core.launcher

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.launcherSearchAppearanceStore by preferencesDataStore(
    name = "launcher_search_appearance",
)

enum class LauncherSearchSuggestionPresentation(
    val storageValue: String,
    val displayName: String,
) {
    ICONS("icons", "Icons"),
    WORDS("words", "Words"),
    BOTH("both", "Both");

    companion object {
        fun fromStorage(value: String?): LauncherSearchSuggestionPresentation =
            entries.firstOrNull { it.storageValue == value } ?: ICONS
    }
}

class LauncherSearchSuggestionPresentationRepository(
    private val dataStore: DataStore<Preferences>,
) {
    constructor(context: Context) : this(context.launcherSearchAppearanceStore)

    private object Keys {
        val presentation = stringPreferencesKey("suggestion_tab_presentation_v1")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val presentation: Flow<LauncherSearchSuggestionPresentation> = dataStore.data
        .map { values ->
            LauncherSearchSuggestionPresentation.fromStorage(values[Keys.presentation])
        }
        .distinctUntilChanged()

    fun setPresentation(value: LauncherSearchSuggestionPresentation) {
        scope.launch {
            dataStore.edit { values ->
                values[Keys.presentation] = value.storageValue
            }
        }
    }
}
