package com.goreecloud.launcher.core.launcher

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

/**
 * Device-local crash-recovery journal for the existing bounded same-resolved-identity restore.
 *
 * This record is never part of the portable Launcher export format. It binds one local restore
 * attempt to exact before/after workspace fingerprints and exact before/after portable preference
 * values so a later process can reconcile an interrupted cross-store operation without guessing.
 */
data class LauncherPortableRestoreJournal(
    val transactionId: String,
    val previousWorkspaceFingerprint: String,
    val appliedWorkspaceFingerprint: String,
    val previousPreferences: LauncherPreferences,
    val targetPreferences: LauncherPreferences,
)

object LauncherPortableRestoreJournalCodec {
    const val FORMAT = "goreecloud-launcher-portable-restore-journal"
    const val VERSION = 1

    private const val MAX_BYTES = 16 * 1024
    private val transactionIdPattern = Regex("[A-Za-z0-9._:-]{1,128}")
    private val sha256Pattern = Regex("[0-9a-f]{64}")

    sealed interface DecodeResult {
        data class Success(val journal: LauncherPortableRestoreJournal) : DecodeResult
        data class Invalid(val reason: String) : DecodeResult
    }

    fun encode(journal: LauncherPortableRestoreJournal): String {
        validate(journal)?.let { reason ->
            throw IllegalArgumentException("invalid portable restore journal: $reason")
        }
        val previousPreferences = encodePreferenceSnapshot(journal.previousPreferences)
        val targetPreferences = encodePreferenceSnapshot(journal.targetPreferences)
        val payload = listOf(
            "format=$FORMAT",
            "version=$VERSION",
            "transaction_id=${journal.transactionId}",
            "previous_workspace_sha256=${journal.previousWorkspaceFingerprint}",
            "applied_workspace_sha256=${journal.appliedWorkspaceFingerprint}",
            "previous_preferences_b64=$previousPreferences",
            "target_preferences_b64=$targetPreferences",
        ).joinToString("\n")
        val encoded = "$payload\nchecksum=${sha256Hex(payload)}\n"
        check(encoded.toByteArray(StandardCharsets.UTF_8).size <= MAX_BYTES) {
            "portable restore journal exceeds the bounded size limit"
        }
        return encoded
    }

    fun decode(encoded: String): DecodeResult {
        if (encoded.toByteArray(StandardCharsets.UTF_8).size > MAX_BYTES) {
            return DecodeResult.Invalid("journal exceeds the bounded size limit")
        }
        if ('\r' in encoded) return DecodeResult.Invalid("journal must use canonical LF line endings")

        val canonical = encoded.removeSuffix("\n")
        val lines = canonical.split('\n')
        if (lines.size != 8 || lines.any { it.isBlank() }) {
            return DecodeResult.Invalid("journal must contain exactly the supported records")
        }
        if (lines[0] != "format=$FORMAT") return DecodeResult.Invalid("unsupported journal format")
        if (lines[1] != "version=$VERSION") return DecodeResult.Invalid("unsupported journal version")

        val expectedKeys = listOf(
            "transaction_id",
            "previous_workspace_sha256",
            "applied_workspace_sha256",
            "previous_preferences_b64",
            "target_preferences_b64",
        )
        expectedKeys.forEachIndexed { index, key ->
            if (!lines[index + 2].startsWith("$key=")) {
                return DecodeResult.Invalid("$key record is missing or out of order")
            }
        }
        if (!lines[7].startsWith("checksum=")) {
            return DecodeResult.Invalid("journal checksum record is missing")
        }

        val checksum = value(lines[7], "checksum")
        if (!sha256Pattern.matches(checksum)) {
            return DecodeResult.Invalid("journal checksum is not canonical SHA-256")
        }
        val payload = lines.take(7).joinToString("\n")
        if (sha256Hex(payload) != checksum) {
            return DecodeResult.Invalid("journal integrity check failed")
        }

        val transactionId = value(lines[2], "transaction_id")
        val previousWorkspaceFingerprint = value(lines[3], "previous_workspace_sha256")
        val appliedWorkspaceFingerprint = value(lines[4], "applied_workspace_sha256")
        val previousPreferences = decodePreferenceSnapshot(value(lines[5], "previous_preferences_b64"))
            ?: return DecodeResult.Invalid("previous preference snapshot is invalid")
        val targetPreferences = decodePreferenceSnapshot(value(lines[6], "target_preferences_b64"))
            ?: return DecodeResult.Invalid("target preference snapshot is invalid")

        val journal = LauncherPortableRestoreJournal(
            transactionId = transactionId,
            previousWorkspaceFingerprint = previousWorkspaceFingerprint,
            appliedWorkspaceFingerprint = appliedWorkspaceFingerprint,
            previousPreferences = previousPreferences,
            targetPreferences = targetPreferences,
        )
        validate(journal)?.let { reason -> return DecodeResult.Invalid(reason) }

        return try {
            if (encode(journal) != encoded.ensureTrailingLf()) {
                DecodeResult.Invalid("journal is not canonical")
            } else {
                DecodeResult.Success(journal)
            }
        } catch (_: IllegalArgumentException) {
            DecodeResult.Invalid("journal is invalid")
        }
    }

    private fun validate(journal: LauncherPortableRestoreJournal): String? {
        if (!transactionIdPattern.matches(journal.transactionId)) return "transaction id is invalid"
        if (!sha256Pattern.matches(journal.previousWorkspaceFingerprint)) {
            return "previous workspace fingerprint is invalid"
        }
        if (!sha256Pattern.matches(journal.appliedWorkspaceFingerprint)) {
            return "applied workspace fingerprint is invalid"
        }
        try {
            LauncherPortablePreferences.encode(journal.previousPreferences)
            LauncherPortablePreferences.encode(journal.targetPreferences)
        } catch (_: IllegalArgumentException) {
            return "preference snapshot is invalid"
        }
        return null
    }

    private fun encodePreferenceSnapshot(preferences: LauncherPreferences): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(
            LauncherPortablePreferences.encode(preferences).toByteArray(StandardCharsets.UTF_8)
        )

    private fun decodePreferenceSnapshot(value: String): LauncherPreferences? {
        if (value.isBlank() || '=' in value) return null
        return try {
            val bytes = Base64.getUrlDecoder().decode(value)
            val decodedText = String(bytes, StandardCharsets.UTF_8)
            val preferences = when (val decoded = LauncherPortablePreferences.decode(decodedText)) {
                is LauncherPortablePreferences.DecodeResult.Success -> decoded.preferences
                is LauncherPortablePreferences.DecodeResult.Invalid -> return null
            }
            if (encodePreferenceSnapshot(preferences) == value) preferences else null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun value(line: String, key: String): String = line.removePrefix("$key=")

    private fun String.ensureTrailingLf(): String = if (endsWith("\n")) this else "$this\n"

    private fun sha256Hex(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }
}

sealed interface LauncherPortableRestoreJournalReadResult {
    data object Absent : LauncherPortableRestoreJournalReadResult
    data class Present(val journal: LauncherPortableRestoreJournal) : LauncherPortableRestoreJournalReadResult
    data class Invalid(val reason: String) : LauncherPortableRestoreJournalReadResult
}
