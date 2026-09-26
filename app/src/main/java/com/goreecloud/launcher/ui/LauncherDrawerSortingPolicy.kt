package com.goreecloud.launcher.ui

import java.text.Normalizer
import java.util.Locale

/**
 * Treat apps and folders as peers in the app drawer. Sorting must remain stable regardless
 * of a provider's item-list order or the work profile's separate application enumeration.
 * Canonically equivalent Unicode labels must sort together, like the installed-app inventory.
 * Only presentation order changes: folder membership and persisted Home positions are untouched.
 */
internal object LauncherDrawerSortingPolicy {
    fun <T> order(
        entries: List<T>,
        label: (T) -> String,
        key: (T) -> String,
    ): List<T> = entries.sortedWith(
        compareBy<T>(
            { Normalizer.normalize(label(it), Normalizer.Form.NFC).lowercase(Locale.ROOT) },
            { key(it) },
        ),
    )
}
