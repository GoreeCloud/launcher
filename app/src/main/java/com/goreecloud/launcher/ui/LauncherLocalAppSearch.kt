package com.goreecloud.launcher.ui

import java.text.Normalizer
import java.util.Locale

/**
 * Pure local matching policy for the installed-app drawer.
 *
 * This helper consumes only labels and package names supplied by Android's
 * LauncherApps inventory. It performs no network, Index, account, telemetry,
 * usage-history, or behavioral ranking work.
 */
object LauncherLocalAppSearch {
    fun matches(
        label: String,
        packageName: String,
        rawQuery: String,
    ): Boolean {
        val terms = normalize(rawQuery)
            .split(' ')
            .filter { it.isNotBlank() }
        if (terms.isEmpty()) return true

        val normalizedLabel = normalize(label)
        val normalizedPackage = normalizePackage(packageName)
        val labelWords = normalizedLabel.split(' ').filter { it.isNotBlank() }

        return terms.all { term ->
            normalizedLabel.contains(term) ||
                labelWords.any { word -> word.startsWith(term) } ||
                normalizedPackage.contains(term)
        }
    }

    internal fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFKD)
            .asSequence()
            .filterNot { Character.getType(it) == Character.NON_SPACING_MARK.toInt() }
            .map { ch ->
                when {
                    ch.isLetterOrDigit() -> ch.lowercaseChar()
                    ch == '.' || ch == '_' || ch == '-' -> ' '
                    else -> ' '
                }
            }
            .joinToString("")
            .trim()
            .replace(Regex("\\s+"), " ")
            .lowercase(Locale.ROOT)

    private fun normalizePackage(value: String): String =
        value.lowercase(Locale.ROOT)
            .replace('.', ' ')
            .replace('_', ' ')
            .replace('-', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
}
