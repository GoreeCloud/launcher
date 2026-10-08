package com.goreecloud.launcher.core.launcher

/**
 * Owner-editable idle Search tab presentation.
 *
 * [presentation] accepts "icons", "words", or "both". Keeping this as a single string makes the
 * presentation easy to change without touching ranking, provider, or privacy behavior.
 */
internal object LauncherSearchTabGlyphs {
    const val presentation: String = "icons"

    private fun render(word: String, codePoint: Int): String {
        val icon = String(Character.toChars(codePoint))
        return when (presentation) {
            "words" -> word
            "both" -> icon + " " + word
            else -> icon
        }
    }

    val frequent: String get() = render("Frequent", 0x25A5)
    val recent: String get() = render("Recent", 0x25F7)
    val newUpdated: String get() = render("New/updated", 0x2726)
}
