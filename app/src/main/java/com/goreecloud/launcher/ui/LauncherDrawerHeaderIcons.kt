package com.goreecloud.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size

@Composable
internal fun LauncherDrawerSortIcon(
    ascending: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val stroke = 2.dp.toPx()
        val left = size.width * 0.25f
        val right = size.width * 0.75f
        val top = size.height * 0.22f
        val bottom = size.height * 0.78f
        drawLine(
            color = color,
            start = Offset(left, top),
            end = Offset(left, bottom),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        val arrowY = if (ascending) top else bottom
        val direction = if (ascending) 1f else -1f
        drawLine(
            color = color,
            start = Offset(left, arrowY),
            end = Offset(left - size.width * 0.12f, arrowY + direction * size.height * 0.12f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(left, arrowY),
            end = Offset(left + size.width * 0.12f, arrowY + direction * size.height * 0.12f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        listOf(0.30f to 0.56f, 0.50f to 0.66f, 0.70f to 0.76f).forEach { (y, x) ->
            drawLine(
                color = color,
                start = Offset(size.width * 0.46f, size.height * y),
                end = Offset(size.width * x, size.height * y),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
        drawCircle(
            color = color,
            radius = size.minDimension * 0.055f,
            center = Offset(right, size.height * 0.18f),
        )
    }
}

@Composable
internal fun LauncherDrawerNewFolderIcon(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val stroke = 2.dp.toPx()
        val path = Path().apply {
            moveTo(size.width * 0.14f, size.height * 0.32f)
            lineTo(size.width * 0.40f, size.height * 0.32f)
            lineTo(size.width * 0.49f, size.height * 0.22f)
            lineTo(size.width * 0.86f, size.height * 0.22f)
            lineTo(size.width * 0.86f, size.height * 0.78f)
            lineTo(size.width * 0.14f, size.height * 0.78f)
            close()
        }
        drawPath(path = path, color = color, style = Stroke(width = stroke))
        drawLine(
            color = color,
            start = Offset(size.width * 0.50f, size.height * 0.50f),
            end = Offset(size.width * 0.74f, size.height * 0.50f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.62f, size.height * 0.38f),
            end = Offset(size.width * 0.62f, size.height * 0.62f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
internal fun LauncherDrawerSettingsIcon(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val stroke = 2.dp.toPx()
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            color = color,
            radius = size.minDimension * 0.26f,
            center = center,
            style = Stroke(width = stroke),
        )
        drawCircle(
            color = color,
            radius = size.minDimension * 0.075f,
            center = center,
            style = Stroke(width = stroke),
        )
        repeat(8) { index ->
            val angle = Math.toRadians(index * 45.0)
            val inner = size.minDimension * 0.34f
            val outer = size.minDimension * 0.44f
            drawLine(
                color = color,
                start = Offset(
                    center.x + (kotlin.math.cos(angle) * inner).toFloat(),
                    center.y + (kotlin.math.sin(angle) * inner).toFloat(),
                ),
                end = Offset(
                    center.x + (kotlin.math.cos(angle) * outer).toFloat(),
                    center.y + (kotlin.math.sin(angle) * outer).toFloat(),
                ),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}
