package com.goreecloud.launcher.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import com.goreecloud.launcher.core.launcher.LauncherHomePageTransition

/**
 * Animates only the page content layer. The Dock and page indicator are intentionally kept
 * outside this modifier so they remain spatially stable while Home pages change.
 */
internal fun Modifier.launcherHomePageEntryTransition(
    transition: LauncherHomePageTransition,
    transitionKey: Any?,
): Modifier = composed {
    val progress = remember { Animatable(1f) }

    LaunchedEffect(transitionKey, transition) {
        if (transition == LauncherHomePageTransition.NONE) {
            progress.snapTo(1f)
        } else {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = when (transition) {
                        LauncherHomePageTransition.SLIDE -> 220
                        LauncherHomePageTransition.FADE -> 180
                        LauncherHomePageTransition.ZOOM -> 200
                        LauncherHomePageTransition.NONE -> 0
                    },
                ),
            )
        }
    }

    graphicsLayer {
        val p = progress.value
        when (transition) {
            LauncherHomePageTransition.SLIDE -> {
                alpha = 0.82f + (0.18f * p)
                translationX = size.width * 0.07f * (1f - p)
            }
            LauncherHomePageTransition.FADE -> {
                alpha = p
            }
            LauncherHomePageTransition.ZOOM -> {
                alpha = 0.72f + (0.28f * p)
                val scale = 0.96f + (0.04f * p)
                scaleX = scale
                scaleY = scale
            }
            LauncherHomePageTransition.NONE -> Unit
        }
    }
}
