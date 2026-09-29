package com.goreecloud.launcher.core.launcher

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

private val Context.launcherInstalledAppBaselineStore by preferencesDataStore(
    name = "launcher_installed_app_baseline",
)

data class LauncherInstalledAppBaselineResult(
    val initializedBefore: Boolean,
    val newAppKeys: List<String>,
)

internal object LauncherInstalledAppBaselinePolicy {
    fun reconcile(
        previous: Set<String>?,
        current: Set<String>,
    ): LauncherInstalledAppBaselineResult {
        if (previous == null) {
            return LauncherInstalledAppBaselineResult(
                initializedBefore = false,
                newAppKeys = emptyList(),
            )
        }
        return LauncherInstalledAppBaselineResult(
            initializedBefore = true,
            newAppKeys = (current - previous).sorted(),
        )
    }
}

/**
 * Persists only the locally visible launchable-app workspace keys needed to detect installs that
 * occurred while the Launcher process was not running.
 *
 * A missing baseline is initialization, not evidence that every currently installed app is new.
 */
class LauncherInstalledAppBaselineRepository(
    private val dataStore: DataStore<Preferences>,
) {
    constructor(context: Context) : this(context.launcherInstalledAppBaselineStore)

    private object Keys {
        val knownApps = stringSetPreferencesKey("known_apps_v1")
    }

    suspend fun reconcile(currentAppKeys: Set<String>): LauncherInstalledAppBaselineResult {
        val normalized = currentAppKeys.filterTo(linkedSetOf()) { it.isNotBlank() }
        var result = LauncherInstalledAppBaselineResult(
            initializedBefore = false,
            newAppKeys = emptyList(),
        )

        dataStore.edit { values ->
            result = LauncherInstalledAppBaselinePolicy.reconcile(
                previous = values[Keys.knownApps],
                current = normalized,
            )
            values[Keys.knownApps] = normalized
        }
        return result
    }
}
