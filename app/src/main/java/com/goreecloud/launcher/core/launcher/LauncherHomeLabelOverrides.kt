package com.goreecloud.launcher.core.launcher

import java.nio.charset.StandardCharsets
import java.util.Base64

object LauncherHomeLabelPolicy {
    const val MAX_LABEL_LENGTH = 64

    fun normalize(rawLabel: String): String? {
        val normalized = rawLabel
            .filterNot { Character.isISOControl(it.code) }
            .trim()
            .take(MAX_LABEL_LENGTH)
        return normalized.ifBlank { null }
    }
}

/**
 * Compact deterministic encoding for Launcher-local Home label overrides.
 *
 * This state intentionally remains outside the strict portable-preference v1 contract.
 */
object LauncherHomeLabelOverridesCodec {
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    fun encode(overrides: Map<String, String>): String =
        overrides
            .mapNotNull { (appKey, rawLabel) ->
                val label = LauncherHomeLabelPolicy.normalize(rawLabel) ?: return@mapNotNull null
                if (appKey.isBlank()) return@mapNotNull null
                encodePart(appKey) + ":" + encodePart(label)
            }
            .sorted()
            .joinToString("\n")

    fun decode(encoded: String?): Map<String, String> {
        if (encoded.isNullOrBlank()) return emptyMap()
        return buildMap {
            encoded.lineSequence().forEach { line ->
                val separator = line.indexOf(':')
                if (separator <= 0 || separator == line.lastIndex) return@forEach
                val appKey = decodePart(line.substring(0, separator)) ?: return@forEach
                val label = decodePart(line.substring(separator + 1))
                    ?.let(LauncherHomeLabelPolicy::normalize)
                    ?: return@forEach
                if (appKey.isNotBlank()) put(appKey, label)
            }
        }
    }

    private fun encodePart(value: String): String =
        encoder.encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decodePart(value: String): String? = runCatching {
        String(decoder.decode(value), StandardCharsets.UTF_8)
    }.getOrNull()
}
