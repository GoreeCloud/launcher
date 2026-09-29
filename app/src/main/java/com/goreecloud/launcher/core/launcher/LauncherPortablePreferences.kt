package com.goreecloud.launcher.core.launcher

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Versioned, deterministic Development portability boundary for explicit Launcher preferences.
 *
 * This codec has no DataStore, package/profile, widget, workspace, or restore authority. It only
 * serializes an already-materialized [LauncherPreferences] value and decodes into memory.
 */
object LauncherPortablePreferences {
    const val FORMAT = "goreecloud-launcher-preferences"
    const val VERSION = 1

    private const val MAX_SNAPSHOT_BYTES = 4096
    private const val ICON_SCALE_FACTOR = 1000
    private const val MIN_ICON_SCALE_MILLI = 850
    private const val MAX_ICON_SCALE_MILLI = 1150

    sealed interface DecodeResult {
        data class Success(val preferences: LauncherPreferences) : DecodeResult
        data class Invalid(val reason: String) : DecodeResult
    }

    fun encode(preferences: LauncherPreferences): String {
        validate(preferences)?.let { reason ->
            throw IllegalArgumentException("invalid launcher preferences: $reason")
        }

        val iconScaleMilli = iconScaleMilli(preferences.iconScale)
            ?: throw IllegalArgumentException("invalid launcher preferences: icon scale is not canonical")
        // Version 1 intentionally retains the legacy "index_home_mode" wire key so existing
        // backups remain byte-compatible while runtime search authority moves to Launcher.
        val payload = listOf(
            "format=$FORMAT",
            "version=$VERSION",
            "home_columns=${preferences.homeColumns}",
            "home_rows=${preferences.homeRows}",
            "drawer_columns=${preferences.drawerColumns}",
            "show_labels=${preferences.showLabels}",
            "icon_scale_milli=$iconScaleMilli",
            "layout_locked=${preferences.layoutLocked}",
            "index_home_mode=${preferences.universalSearchHomeMode.storageValue}",
        ).joinToString("\n")

        return "$payload\nchecksum=${sha256Hex(payload)}\n"
    }

    fun decode(encoded: String): DecodeResult {
        if (encoded.toByteArray(StandardCharsets.UTF_8).size > MAX_SNAPSHOT_BYTES) {
            return DecodeResult.Invalid("snapshot exceeds the bounded size limit")
        }
        if ('\r' in encoded) {
            return DecodeResult.Invalid("snapshot must use canonical LF line endings")
        }

        val canonical = encoded.removeSuffix("\n")
        val lines = canonical.split('\n')
        if (lines.size != 10 || lines.any { it.isBlank() }) {
            return DecodeResult.Invalid("snapshot must contain exactly the supported records")
        }
        if (lines[0] != "format=$FORMAT") {
            return DecodeResult.Invalid("unsupported snapshot format")
        }
        if (lines[1] != "version=$VERSION") {
            return DecodeResult.Invalid("unsupported snapshot version")
        }

        val expectedKeys = listOf(
            "home_columns",
            "home_rows",
            "drawer_columns",
            "show_labels",
            "icon_scale_milli",
            "layout_locked",
            "index_home_mode",
        )
        expectedKeys.forEachIndexed { index, key ->
            if (!lines[index + 2].startsWith("$key=")) {
                return DecodeResult.Invalid("$key record is missing or out of order")
            }
        }
        if (!lines[9].startsWith("checksum=")) {
            return DecodeResult.Invalid("snapshot checksum record is missing")
        }

        val checksum = lines[9].removePrefix("checksum=")
        if (!checksum.matches(Regex("[0-9a-f]{64}"))) {
            return DecodeResult.Invalid("snapshot checksum is not canonical SHA-256")
        }
        val payload = lines.take(9).joinToString("\n")
        if (sha256Hex(payload) != checksum) {
            return DecodeResult.Invalid("snapshot integrity check failed")
        }

        val homeColumns = parseCanonicalInt(value(lines[2], "home_columns"))
            ?: return DecodeResult.Invalid("home columns are invalid")
        val homeRows = parseCanonicalInt(value(lines[3], "home_rows"))
            ?: return DecodeResult.Invalid("home rows are invalid")
        val drawerColumns = parseCanonicalInt(value(lines[4], "drawer_columns"))
            ?: return DecodeResult.Invalid("drawer columns are invalid")
        val showLabels = parseCanonicalBoolean(value(lines[5], "show_labels"))
            ?: return DecodeResult.Invalid("show labels is invalid")
        val iconScaleMilli = parseCanonicalInt(value(lines[6], "icon_scale_milli"))
            ?: return DecodeResult.Invalid("icon scale is invalid")
        val layoutLocked = parseCanonicalBoolean(value(lines[7], "layout_locked"))
            ?: return DecodeResult.Invalid("layout locked is invalid")
        val universalSearchModeValue = value(lines[8], "index_home_mode")
        val universalSearchHomeMode = LauncherUniversalSearchHomeMode.entries.firstOrNull {
            it.storageValue == universalSearchModeValue
        } ?: return DecodeResult.Invalid("universal search home mode is unsupported")

        if (homeColumns !in 4..6) return DecodeResult.Invalid("home columns are outside the supported range")
        if (homeRows !in 4..7) return DecodeResult.Invalid("home rows are outside the supported range")
        if (drawerColumns !in 4..6) return DecodeResult.Invalid("drawer columns are outside the supported range")
        if (iconScaleMilli !in MIN_ICON_SCALE_MILLI..MAX_ICON_SCALE_MILLI) {
            return DecodeResult.Invalid("icon scale is outside the supported range")
        }

        val preferences = LauncherPreferences(
            homeColumns = homeColumns,
            homeRows = homeRows,
            drawerColumns = drawerColumns,
            showLabels = showLabels,
            iconScale = iconScaleMilli.toFloat() / ICON_SCALE_FACTOR,
            layoutLocked = layoutLocked,
            universalSearchHomeMode = universalSearchHomeMode,
        )
        validate(preferences)?.let { reason -> return DecodeResult.Invalid(reason) }
        return DecodeResult.Success(preferences)
    }

    private fun validate(preferences: LauncherPreferences): String? {
        if (preferences.homeColumns !in 4..6) return "home columns are outside the supported range"
        if (preferences.homeRows !in 4..7) return "home rows are outside the supported range"
        if (preferences.drawerColumns !in 4..6) return "drawer columns are outside the supported range"
        val iconScaleMilli = iconScaleMilli(preferences.iconScale)
            ?: return "icon scale is not canonical"
        if (iconScaleMilli !in MIN_ICON_SCALE_MILLI..MAX_ICON_SCALE_MILLI) {
            return "icon scale is outside the supported range"
        }
        return null
    }

    private fun iconScaleMilli(value: Float): Int? {
        if (!value.isFinite()) return null
        val scaled = value * ICON_SCALE_FACTOR
        val rounded = scaled.roundToInt()
        return rounded.takeIf { abs(scaled - rounded.toFloat()) < 0.0001f }
    }

    private fun value(line: String, key: String): String = line.removePrefix("$key=")

    private fun parseCanonicalInt(value: String): Int? {
        if (!value.matches(Regex("0|[1-9][0-9]*"))) return null
        return value.toIntOrNull()
    }

    private fun parseCanonicalBoolean(value: String): Boolean? = when (value) {
        "true" -> true
        "false" -> false
        else -> null
    }

    private fun sha256Hex(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }
}
