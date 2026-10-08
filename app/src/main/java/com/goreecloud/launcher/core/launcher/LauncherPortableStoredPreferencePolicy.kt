package com.goreecloud.launcher.core.launcher

/**
 * Raw DataStore representation of the seven portable Launcher preferences.
 *
 * Nullable values mean the key is absent and therefore legitimately resolves to the product
 * default. Present values must already be canonical; recovery must never silently clamp or replace
 * a malformed persisted value before deciding whether an interrupted restore matches its journal.
 */
data class LauncherPortableStoredPreferences(
    val homeColumns: Int?,
    val homeRows: Int?,
    val drawerColumns: Int?,
    val showLabels: Boolean?,
    val iconScale: Float?,
    val layoutLocked: Boolean?,
    // This value is read from the legacy v1 DataStore/wire key `index_home_mode`.
    val universalSearchHomeMode: String?,
)

object LauncherPortableStoredPreferencePolicy {
    sealed interface DecodeResult {
        data class Success(val preferences: LauncherPreferences) : DecodeResult
        data class Invalid(val reason: String) : DecodeResult
    }

    fun decode(
        stored: LauncherPortableStoredPreferences,
        defaults: LauncherPreferences = LauncherPreferences(),
    ): DecodeResult {
        val universalSearchHomeMode = when (val rawMode = stored.universalSearchHomeMode) {
            null -> defaults.universalSearchHomeMode
            else -> LauncherUniversalSearchHomeMode.entries.firstOrNull { it.storageValue == rawMode }
                ?: return DecodeResult.Invalid("stored universal search home mode is unsupported")
        }

        val preferences = LauncherPreferences(
            homeColumns = stored.homeColumns ?: defaults.homeColumns,
            homeRows = stored.homeRows ?: defaults.homeRows,
            drawerColumns = stored.drawerColumns ?: defaults.drawerColumns,
            showLabels = stored.showLabels ?: defaults.showLabels,
            iconScale = stored.iconScale ?: defaults.iconScale,
            layoutLocked = stored.layoutLocked ?: defaults.layoutLocked,
            universalSearchHomeMode = universalSearchHomeMode,
        )

        return try {
            // Reuse the portable codec as the canonical range/finite/precision authority. Unlike
            // ordinary UI reads, recovery deliberately does not call sanitized().
            LauncherPortablePreferences.encode(preferences)
            DecodeResult.Success(preferences)
        } catch (_: IllegalArgumentException) {
            DecodeResult.Invalid("stored portable preferences are outside the canonical recovery domain")
        }
    }
}

sealed interface LauncherPortableRecoveryPreferenceReadResult {
    data class Success(val preferences: LauncherPreferences) : LauncherPortableRecoveryPreferenceReadResult
    data class Invalid(val reason: String) : LauncherPortableRecoveryPreferenceReadResult
}
