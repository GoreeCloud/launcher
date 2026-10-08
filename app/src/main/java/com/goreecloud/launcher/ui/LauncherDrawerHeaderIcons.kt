package com.goreecloud.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.goreecloud.launcher.core.launcher.LauncherDrawerLayoutMode

@Composable
internal fun LauncherDrawerSortIcon(
    ascending: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val stroke = 2.15.dp.toPx()
        val cap = StrokeCap.Round
        val x = size.width * 0.26f
        val top = size.height * 0.20f
        val bottom = size.height * 0.80f
        drawLine(color, Offset(x, top), Offset(x, bottom), stroke, cap = cap)
        val tipY = if (ascending) top else bottom
        val armY = if (ascending) top + size.height * 0.14f else bottom - size.height * 0.14f
        drawLine(color, Offset(x, tipY), Offset(x - size.width * 0.10f, armY), stroke, cap = cap)
        drawLine(color, Offset(x, tipY), Offset(x + size.width * 0.10f, armY), stroke, cap = cap)
        listOf(0.31f to 0.82f, 0.50f to 0.73f, 0.69f to 0.64f).forEach { (y, endX) ->
            drawLine(
                color,
                Offset(size.width * 0.49f, size.height * y),
                Offset(size.width * endX, size.height * y),
                stroke,
                cap = cap,
            )
        }
    }
}

@Composable
internal fun LauncherDrawerLayoutIcon(
    mode: LauncherDrawerLayoutMode,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val stroke = 2.05.dp.toPx()
        val corner = CornerRadius(size.minDimension * 0.055f)
        when (mode) {
            LauncherDrawerLayoutMode.GRID -> {
                listOf(0.19f to 0.19f, 0.56f to 0.19f, 0.19f to 0.56f, 0.56f to 0.56f)
                    .forEach { (x, y) ->
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(size.width * x, size.height * y),
                            size = Size(size.width * 0.25f, size.height * 0.25f),
                            cornerRadius = corner,
                            style = Stroke(stroke),
                        )
                    }
            }
            LauncherDrawerLayoutMode.COMPACT -> {
                repeat(3) { row ->
                    repeat(3) { column ->
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(
                                size.width * (0.18f + column * 0.24f),
                                size.height * (0.18f + row * 0.24f),
                            ),
                            size = Size(size.width * 0.14f, size.height * 0.14f),
                            cornerRadius = corner,
                            style = Stroke(stroke),
                        )
                    }
                }
            }
            LauncherDrawerLayoutMode.LIST -> {
                repeat(3) { row ->
                    val y = size.height * (0.28f + row * 0.22f)
                    drawCircle(
                        color = color,
                        radius = size.minDimension * 0.045f,
                        center = Offset(size.width * 0.22f, y),
                    )
                    drawLine(
                        color,
                        Offset(size.width * 0.36f, y),
                        Offset(size.width * 0.82f, y),
                        stroke,
                        cap = StrokeCap.Round,
                    )
                }
            }
            LauncherDrawerLayoutMode.CATEGORY -> {
                listOf(
                    0.18f to 0.20f,
                    0.54f to 0.20f,
                    0.18f to 0.56f,
                    0.54f to 0.56f,
                ).forEach { (x, y) ->
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(size.width * x, size.height * y),
                        size = Size(size.width * 0.28f, size.height * 0.21f),
                        cornerRadius = CornerRadius(size.minDimension * 0.07f),
                        style = Stroke(stroke),
                    )
                }
            }
        }
    }
}

@Composable
internal fun LauncherDrawerNewFolderIcon(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val stroke = 2.15.dp.toPx()
        val folder = Path().apply {
            moveTo(size.width * 0.13f, size.height * 0.32f)
            lineTo(size.width * 0.39f, size.height * 0.32f)
            lineTo(size.width * 0.47f, size.height * 0.23f)
            lineTo(size.width * 0.62f, size.height * 0.23f)
            lineTo(size.width * 0.70f, size.height * 0.32f)
            lineTo(size.width * 0.87f, size.height * 0.32f)
            lineTo(size.width * 0.87f, size.height * 0.80f)
            lineTo(size.width * 0.13f, size.height * 0.80f)
            close()
        }
        drawPath(
            path = folder,
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        val center = Offset(size.width * 0.64f, size.height * 0.57f)
        val arm = size.minDimension * 0.11f
        drawLine(
            color,
            Offset(center.x - arm, center.y),
            Offset(center.x + arm, center.y),
            stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color,
            Offset(center.x, center.y - arm),
            Offset(center.x, center.y + arm),
            stroke,
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
        val stroke = 2.15.dp.toPx()
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            color = color,
            radius = size.minDimension * 0.26f,
            center = center,
            style = Stroke(width = stroke),
        )
        drawCircle(
            color = color,
            radius = size.minDimension * 0.085f,
            center = center,
            style = Stroke(width = stroke),
        )
        repeat(8) { index ->
            val angle = Math.toRadians(index * 45.0)
            val inner = size.minDimension * 0.255f
            val outer = size.minDimension * 0.41f
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
