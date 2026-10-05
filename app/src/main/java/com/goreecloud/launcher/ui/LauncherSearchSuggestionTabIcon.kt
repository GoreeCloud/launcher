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

@Composable
internal fun LauncherSearchSuggestionTabIcon(
    tab: LauncherSearchSuggestionTab,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(22.dp)) {
        val stroke = 2.05.dp.toPx()
        val cap = StrokeCap.Round
        val center = Offset(size.width / 2f, size.height / 2f)
        when (tab) {
            LauncherSearchSuggestionTab.FREQUENT -> {
                listOf(
                    Triple(0.31f, 0.66f, 0.53f),
                    Triple(0.50f, 0.66f, 0.38f),
                    Triple(0.69f, 0.66f, 0.27f),
                ).forEach { (x, bottom, top) ->
                    drawLine(
                        color = color,
                        start = Offset(size.width * x, size.height * bottom),
                        end = Offset(size.width * x, size.height * top),
                        strokeWidth = stroke,
                        cap = cap,
                    )
                }
            }
            LauncherSearchSuggestionTab.RECENT -> {
                drawCircle(
                    color = color,
                    radius = size.minDimension * 0.34f,
                    center = center,
                    style = Stroke(width = stroke),
                )
                drawLine(
                    color = color,
                    start = center,
                    end = Offset(center.x, center.y - size.height * 0.18f),
                    strokeWidth = stroke,
                    cap = cap,
                )
                drawLine(
                    color = color,
                    start = center,
                    end = Offset(center.x + size.width * 0.15f, center.y + size.height * 0.08f),
                    strokeWidth = stroke,
                    cap = cap,
                )
            }
            LauncherSearchSuggestionTab.NEW_UPDATED -> {
                val arm = size.minDimension * 0.22f
                drawLine(
                    color = color,
                    start = Offset(center.x - arm, center.y),
                    end = Offset(center.x + arm, center.y),
                    strokeWidth = stroke,
                    cap = cap,
                )
                drawLine(
                    color = color,
                    start = Offset(center.x, center.y - arm),
                    end = Offset(center.x, center.y + arm),
                    strokeWidth = stroke,
                    cap = cap,
                )
                drawCircle(
                    color = color,
                    radius = size.minDimension * 0.055f,
                    center = Offset(size.width * 0.76f, size.height * 0.25f),
                )
            }
        }
    }
}
