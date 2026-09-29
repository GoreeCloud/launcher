package com.goreecloud.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.goreecloud.launcher.core.launcher.LauncherSearchSuggestionTab

internal fun launcherSearchSuggestionTabAccessibilityLabel(
    tab: LauncherSearchSuggestionTab,
): String = when (tab) {
    LauncherSearchSuggestionTab.FREQUENT -> "Frequent"
    LauncherSearchSuggestionTab.RECENT -> "Recent"
    LauncherSearchSuggestionTab.NEW_UPDATED -> "New or updated"
}

/**
 * Uses one shared outlined visual grammar for all idle Universal Search tabs.
 *
 * The enclosing ring keeps Frequent, Recent, and New/updated visually consistent while the
 * interior mark remains semantic. Accessibility never depends on the icon shape.
 */
@Composable
internal fun LauncherSearchSuggestionTabIcon(
    tab: LauncherSearchSuggestionTab,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(22.dp)) {
        val stroke = 1.8.dp.toPx()
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.39f

        drawCircle(
            color = color,
            radius = radius,
            center = center,
            style = Stroke(width = stroke),
        )

        when (tab) {
            LauncherSearchSuggestionTab.FREQUENT -> {
                listOf(
                    0.35f to 0.55f,
                    0.50f to 0.43f,
                    0.65f to 0.31f,
                ).forEach { (x, top) ->
                    drawLine(
                        color = color,
                        start = Offset(size.width * x, size.height * 0.66f),
                        end = Offset(size.width * x, size.height * top),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                    )
                }
            }

            LauncherSearchSuggestionTab.RECENT -> {
                drawLine(
                    color = color,
                    start = center,
                    end = Offset(center.x, center.y - size.height * 0.19f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = center,
                    end = Offset(
                        center.x + size.width * 0.16f,
                        center.y + size.height * 0.09f,
                    ),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }

            LauncherSearchSuggestionTab.NEW_UPDATED -> {
                val arm = size.minDimension * 0.18f
                drawLine(
                    color = color,
                    start = Offset(center.x - arm, center.y),
                    end = Offset(center.x + arm, center.y),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(center.x, center.y - arm),
                    end = Offset(center.x, center.y + arm),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawCircle(
                    color = color,
                    radius = size.minDimension * 0.045f,
                    center = Offset(size.width * 0.72f, size.height * 0.29f),
                )
            }
        }
    }
}
