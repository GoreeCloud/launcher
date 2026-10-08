package com.goreecloud.launcher.ui.theme

data class GlazeThemeChoice(
    val mode: GlazeThemeMode,
    val title: String,
    val summary: String,
) {
    val previewAccessibilityLabel: String
        get() = "$title appearance preview"

    val selectedAccessibilityState: String
        get() = "$title appearance selected"
}

object GlazeThemeManagerCatalog {
    val choices: List<GlazeThemeChoice> = listOf(
        GlazeThemeChoice(
            mode = GlazeThemeMode.SYSTEM,
            title = "System",
            summary = "Follow the current Android light or dark appearance.",
        ),
        GlazeThemeChoice(
            mode = GlazeThemeMode.LIGHT,
            title = "Light",
            summary = "Use the Launcher light palette under GLAZE UI V1.6 semantics.",
        ),
        GlazeThemeChoice(
            mode = GlazeThemeMode.DARK,
            title = "Dark",
            summary = "Use the Launcher dark palette under GLAZE UI V1.6 semantics.",
        ),
        GlazeThemeChoice(
            mode = GlazeThemeMode.DEEP_DARK,
            title = "Deep Dark",
            summary = "Use the Launcher Deep Dark palette under GLAZE UI V1.6 semantics.",
        ),
    )

    fun choiceFor(mode: GlazeThemeMode): GlazeThemeChoice =
        choices.first { it.mode == mode }
}
