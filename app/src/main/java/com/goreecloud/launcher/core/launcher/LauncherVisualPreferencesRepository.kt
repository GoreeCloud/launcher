package com.goreecloud.launcher.core.launcher

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.launcherVisualPreferencesStore by preferencesDataStore(
    name = "launcher_visual_preferences",
)

enum class LauncherHomePageTransition(
    val storageValue: String,
    val displayName: String,
) {
    SLIDE("slide", "Slide"),
    FADE("fade", "Fade"),
    ZOOM("zoom", "Zoom"),
    NONE("none", "Minimal");

    companion object {
        fun fromStorage(value: String?): LauncherHomePageTransition =
            entries.firstOrNull { it.storageValue == value } ?: SLIDE
    }
}

enum class LauncherDrawerHeaderPresentation(
    val storageValue: String,
    val displayName: String,
) {
    ICONS("icons", "Icons"),
    WORDS("words", "Words");

    companion object {
        fun fromStorage(value: String?): LauncherDrawerHeaderPresentation =
            entries.firstOrNull { it.storageValue == value } ?: ICONS
    }
}

data class LauncherVisualPreferences(
    val starterDockSize: Int = 5,
    val homePageTransition: LauncherHomePageTransition = LauncherHomePageTransition.SLIDE,
    val drawerHeaderPresentation: LauncherDrawerHeaderPresentation =
        LauncherDrawerHeaderPresentation.ICONS,
)

class LauncherVisualPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) {
    constructor(context: Context) : this(context.launcherVisualPreferencesStore)

    private object Keys {
        val starterDockSize = intPreferencesKey("starter_dock_size_v1")
        val homePageTransition = stringPreferencesKey("home_page_transition_v1")
        val drawerHeaderPresentation = stringPreferencesKey("drawer_header_presentation_v1")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val preferences: Flow<LauncherVisualPreferences> = dataStore.data
        .map { values ->
            LauncherVisualPreferences(
                starterDockSize = (values[Keys.starterDockSize] ?: 5).coerceIn(4, 6),
                homePageTransition =
                    LauncherHomePageTransition.fromStorage(values[Keys.homePageTransition]),
                drawerHeaderPresentation =
                    LauncherDrawerHeaderPresentation.fromStorage(
                        values[Keys.drawerHeaderPresentation],
                    ),
            )
        }
        .distinctUntilChanged()

    suspend fun setStarterDockSize(size: Int) {
        dataStore.edit { values ->
            values[Keys.starterDockSize] = size.coerceIn(4, 6)
        }
    }

    fun setHomePageTransition(transition: LauncherHomePageTransition) {
        scope.launch {
            dataStore.edit { values ->
                values[Keys.homePageTransition] = transition.storageValue
            }
        }
    }

    fun setDrawerHeaderPresentation(presentation: LauncherDrawerHeaderPresentation) {
        scope.launch {
            dataStore.edit { values ->
                values[Keys.drawerHeaderPresentation] = presentation.storageValue
            }
        }
    }
}
