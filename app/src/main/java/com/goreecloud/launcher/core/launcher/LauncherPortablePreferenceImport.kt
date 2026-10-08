package com.goreecloud.launcher.core.launcher

/**
 * Minimal persistence authority required by the portable Launcher preference importer.
 *
 * The writer receives one complete, already-validated [LauncherPreferences] value so a concrete
 * implementation can commit the seven supported settings atomically rather than issuing a series
 * of independently scheduled partial writes.
 */
interface LauncherPortablePreferenceWriter {
    suspend fun replacePortablePreferences(preferences: LauncherPreferences)
}

object LauncherPortablePreferenceImport {
    sealed interface ApplyResult {
        data class Applied(val preferences: LauncherPreferences) : ApplyResult
        data class Rejected(val reason: String) : ApplyResult
    }

    /**
     * Decode the complete snapshot before granting any persistence call.
     *
     * Invalid, tampered, expanded, or unsupported snapshots perform zero writes. Persistence
     * failures are deliberately allowed to propagate so callers cannot misrepresent an incomplete
     * storage commit as a successful import.
     */
    suspend fun apply(
        encoded: String,
        writer: LauncherPortablePreferenceWriter,
    ): ApplyResult = when (val decoded = LauncherPortablePreferences.decode(encoded)) {
        is LauncherPortablePreferences.DecodeResult.Invalid -> ApplyResult.Rejected(decoded.reason)
        is LauncherPortablePreferences.DecodeResult.Success -> {
            writer.replacePortablePreferences(decoded.preferences)
            ApplyResult.Applied(decoded.preferences)
        }
    }
}
