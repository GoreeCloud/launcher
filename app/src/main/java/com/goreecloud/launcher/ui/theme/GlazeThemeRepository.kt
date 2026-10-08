package com.goreecloud.launcher.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.themeStore by preferencesDataStore(name = "glaze_theme")

class GlazeThemeRepository(private val context: Context) {
    private val key = stringPreferencesKey("appearance")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val defaultMode = GlazeThemeMode.SYSTEM
    val themeMode: Flow<GlazeThemeMode> = context.themeStore.data.map {
        GlazeThemeModeCodec.decode(it[key], defaultMode)
    }

    fun setMode(mode: GlazeThemeMode) {
        scope.launch { context.themeStore.edit { prefs -> prefs[key] = mode.name } }
    }

    fun cycleMode(current: GlazeThemeMode) {
        val next = when (current) {
            GlazeThemeMode.SYSTEM -> GlazeThemeMode.LIGHT
            GlazeThemeMode.LIGHT -> GlazeThemeMode.DARK
            GlazeThemeMode.DARK -> GlazeThemeMode.DEEP_DARK
            GlazeThemeMode.DEEP_DARK -> GlazeThemeMode.SYSTEM
        }
        setMode(next)
    }
}

internal object GlazeThemeModeCodec {
    fun decode(value: String?, fallback: GlazeThemeMode = GlazeThemeMode.SYSTEM): GlazeThemeMode =
        value?.let { stored -> runCatching { GlazeThemeMode.valueOf(stored) }.getOrNull() } ?: fallback
}
