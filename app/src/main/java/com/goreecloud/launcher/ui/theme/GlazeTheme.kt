package com.goreecloud.launcher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

enum class GlazeThemeMode { SYSTEM, LIGHT, DARK, DEEP_DARK }

/**
 * Launcher-owned structural palettes mapped under the GLAZE UI V1.6 semantic presentation model.
 *
 * These pigments are retained application choices rather than claims that every value is a
 * canonical V1.6 token. Glaze UI owns presentation semantics; authoritative privacy, security,
 * identity, recovery, availability, capability, and workflow state remain outside this theme.
 */
private val light = lightColorScheme(
    primary = Color(0xFF3478F6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0x1F3478F6),
    onPrimaryContainer = Color(0xFF151A23),
    secondary = Color(0xFF7657F6),
    background = Color(0xFFF5F7FA),
    onBackground = Color(0xFF151A23),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF151A23),
    surfaceVariant = Color(0xE0FFFFFF),
    onSurfaceVariant = Color(0xFF5D6675),
)

private val dark = darkColorScheme(
    primary = Color(0xFF8DB5FF),
    onPrimary = Color(0xFF0B0D11),
    primaryContainer = Color(0x1F8DB5FF),
    onPrimaryContainer = Color(0xFFF5F7FA),
    secondary = Color(0xFFA990FF),
    background = Color(0xFF0B0D11),
    onBackground = Color(0xFFF5F7FA),
    surface = Color(0xFF12151B),
    onSurface = Color(0xFFF5F7FA),
    surfaceVariant = Color(0xDB181D26),
    onSurfaceVariant = Color(0xFFB0B7C3),
)

private val deepDark = darkColorScheme(
    primary = Color(0xFF8DB5FF),
    onPrimary = Color(0xFF05070A),
    primaryContainer = Color(0x1F8DB5FF),
    onPrimaryContainer = Color(0xFFF5F7FA),
    secondary = Color(0xFFA990FF),
    background = Color(0xFF05070A),
    onBackground = Color(0xFFF5F7FA),
    surface = Color(0xFF0D1015),
    onSurface = Color(0xFFF5F7FA),
    surfaceVariant = Color(0xE612161D),
    onSurfaceVariant = Color(0xFFABB4C2),
)

@Composable
fun GlazeTheme(
    mode: GlazeThemeMode,
    presentationContext: GlazeV16PresentationContext? = null,
    content: @Composable () -> Unit,
) {
    val inheritedPresentationContext = LocalGlazeV16PresentationContext.current
    val effectivePresentationContext = presentationContext ?: inheritedPresentationContext

    val scheme = when (mode) {
        GlazeThemeMode.SYSTEM -> if (isSystemInDarkTheme()) dark else light
        GlazeThemeMode.LIGHT -> light
        GlazeThemeMode.DARK -> dark
        GlazeThemeMode.DEEP_DARK -> deepDark
    }

    CompositionLocalProvider(
        LocalGlazeV16PresentationContext provides effectivePresentationContext,
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
