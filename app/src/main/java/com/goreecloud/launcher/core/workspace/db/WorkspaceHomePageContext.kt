package com.goreecloud.launcher.core.workspace.db

data class WorkspaceHomePageContext(
    val appCount: Int,
    val unsupportedItemCount: Int,
) {
    init {
        require(appCount >= 0)
        require(unsupportedItemCount >= 0)
    }

    val compactLabel: String
        get() = when {
            unsupportedItemCount > 0 -> "$appCount app${if (appCount == 1) "" else "s"} · $unsupportedItemCount other"
            else -> "$appCount app${if (appCount == 1) "" else "s"}"
        }

    fun moveTargetLabel(pageNumber: Int, primary: Boolean = false): String {
        require(pageNumber >= 1)
        val pageLabel = if (primary) "Primary Home" else "Page $pageNumber"
        return "$pageLabel · $compactLabel"
    }

    fun switcherAccessibilityLabel(pageNumber: Int, selected: Boolean): String {
        require(pageNumber >= 1)
        val appLabel = "$appCount app${if (appCount == 1) "" else "s"}"
        val unsupportedLabel = if (unsupportedItemCount > 0) {
            ", $unsupportedItemCount other workspace item${if (unsupportedItemCount == 1) "" else "s"}"
        } else {
            ""
        }
        return "Page $pageNumber, $appLabel$unsupportedLabel, ${if (selected) "selected" else "not selected"}"
    }

    fun accessibilityLabel(pageNumber: Int, selected: Boolean): String =
        switcherAccessibilityLabel(pageNumber, selected)
}

fun WorkspaceRenderedHomePage.context(): WorkspaceHomePageContext = WorkspaceHomePageContext(
    appCount = appKeys.size,
    unsupportedItemCount = unsupportedItemCount,
)
