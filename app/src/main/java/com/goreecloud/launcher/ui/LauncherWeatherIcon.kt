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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.goreecloud.launcher.core.launcher.LauncherWeatherVisualKind

@Composable
internal fun LauncherWeatherIcon(
    kind: LauncherWeatherVisualKind,
    isDay: Boolean,
    color: Color,
    size: Dp = 38.dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(size)) {
        val stroke = (this.size.minDimension * 0.075f).coerceAtLeast(2.dp.toPx())
        val cloudColor = when (kind) {
            LauncherWeatherVisualKind.FOG -> color.copy(alpha = 0.72f)
            LauncherWeatherVisualKind.RAIN,
            LauncherWeatherVisualKind.SNOW,
            LauncherWeatherVisualKind.THUNDERSTORM,
            LauncherWeatherVisualKind.PARTLY_CLOUDY,
            LauncherWeatherVisualKind.CLOUDY,
            -> color.copy(alpha = 0.94f)
            else -> color.copy(alpha = 0.90f)
        }
        val accentColor = when (kind) {
            LauncherWeatherVisualKind.CLEAR ->
                if (isDay) Color(0xFFFFC857) else Color(0xFFC7D7FF)
            LauncherWeatherVisualKind.PARTLY_CLOUDY ->
                if (isDay) Color(0xFFFFC857) else Color(0xFFC7D7FF)
            LauncherWeatherVisualKind.FOG -> Color(0xFFD7E3EA)
            LauncherWeatherVisualKind.WIND -> Color(0xFF8EDCFF)
            LauncherWeatherVisualKind.RAIN -> Color(0xFF74B9FF)
            LauncherWeatherVisualKind.SNOW -> Color(0xFFEAF7FF)
            LauncherWeatherVisualKind.THUNDERSTORM -> Color(0xFFFFD166)
            LauncherWeatherVisualKind.CLOUDY,
            LauncherWeatherVisualKind.UNKNOWN,
            -> color
        }
        val width = this.size.width
        val height = this.size.height

        fun drawCloud(centerY: Float = height * 0.52f, scale: Float = 1f) {
            val baseLeft = width * 0.18f
            val baseTop = centerY
            val baseWidth = width * 0.66f
            val baseHeight = height * 0.22f
            drawRoundRect(
                color = cloudColor,
                topLeft = Offset(baseLeft, baseTop),
                size = Size(baseWidth, baseHeight),
                cornerRadius = CornerRadius(baseHeight * 0.48f, baseHeight * 0.48f),
            )
            drawCircle(
                color = cloudColor,
                radius = width * 0.16f * scale,
                center = Offset(width * 0.39f, centerY + height * 0.02f),
            )
            drawCircle(
                color = cloudColor,
                radius = width * 0.20f * scale,
                center = Offset(width * 0.56f, centerY - height * 0.03f),
            )
        }

        fun drawSun() {
            val center = Offset(width * 0.45f, height * 0.42f)
            val radius = width * 0.18f
            drawCircle(color = accentColor, radius = radius, center = center)
            val rayInner = width * 0.25f
            val rayOuter = width * 0.35f
            repeat(8) { index ->
                val angle = Math.toRadians((index * 45.0) - 90.0)
                drawLine(
                    color = accentColor.copy(alpha = 0.86f),
                    start = Offset(
                        center.x + (kotlin.math.cos(angle) * rayInner).toFloat(),
                        center.y + (kotlin.math.sin(angle) * rayInner).toFloat(),
                    ),
                    end = Offset(
                        center.x + (kotlin.math.cos(angle) * rayOuter).toFloat(),
                        center.y + (kotlin.math.sin(angle) * rayOuter).toFloat(),
                    ),
                    strokeWidth = stroke * 0.72f,
                    cap = StrokeCap.Round,
                )
            }
        }

        fun drawMoon() {
            drawCircle(
                color = accentColor,
                radius = width * 0.23f,
                center = Offset(width * 0.45f, height * 0.42f),
            )
            drawCircle(
                color = Color.Transparent,
                radius = width * 0.21f,
                center = Offset(width * 0.54f, height * 0.34f),
            )
            val crescent = Path().apply {
                moveTo(width * 0.50f, height * 0.18f)
                cubicTo(
                    width * 0.30f, height * 0.22f,
                    width * 0.24f, height * 0.51f,
                    width * 0.42f, height * 0.66f,
                )
                cubicTo(
                    width * 0.30f, height * 0.48f,
                    width * 0.35f, height * 0.29f,
                    width * 0.50f, height * 0.18f,
                )
                close()
            }
            drawPath(crescent, color = accentColor)
        }

        when (kind) {
            LauncherWeatherVisualKind.CLEAR -> {
                if (isDay) drawSun() else drawMoon()
            }
            LauncherWeatherVisualKind.PARTLY_CLOUDY -> {
                if (isDay) drawSun() else drawMoon()
                drawCloud(centerY = height * 0.50f, scale = 0.90f)
            }
            LauncherWeatherVisualKind.CLOUDY -> {
                drawCloud(centerY = height * 0.43f)
            }
            LauncherWeatherVisualKind.FOG -> {
                drawCloud(centerY = height * 0.27f, scale = 0.88f)
                listOf(0.64f, 0.77f, 0.90f).forEachIndexed { index, y ->
                    drawLine(
                        color = accentColor.copy(alpha = 0.80f - index * 0.10f),
                        start = Offset(width * 0.18f, height * y),
                        end = Offset(width * (if (index == 1) 0.78f else 0.86f), height * y),
                        strokeWidth = stroke * 0.78f,
                        cap = StrokeCap.Round,
                    )
                }
            }
            LauncherWeatherVisualKind.WIND -> {
                listOf(
                    Triple(0.30f, 0.72f, 0.20f),
                    Triple(0.50f, 0.84f, 0.44f),
                    Triple(0.70f, 0.68f, 0.30f),
                ).forEach { (y, endX, curlY) ->
                    drawLine(
                        color = accentColor,
                        start = Offset(width * 0.14f, height * y),
                        end = Offset(width * endX, height * y),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                    )
                    drawArc(
                        color = accentColor,
                        startAngle = -95f,
                        sweepAngle = 170f,
                        useCenter = false,
                        topLeft = Offset(width * (endX - 0.12f), height * (curlY - 0.10f)),
                        size = Size(width * 0.22f, height * 0.20f),
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
            }
            LauncherWeatherVisualKind.RAIN -> {
                drawCloud(centerY = height * 0.30f)
                listOf(0.31f, 0.50f, 0.69f).forEachIndexed { index, x ->
                    val startY = if (index == 1) 0.70f else 0.67f
                    drawLine(
                        color = accentColor,
                        start = Offset(width * x, height * startY),
                        end = Offset(width * (x - 0.035f), height * 0.88f),
                        strokeWidth = stroke * 0.78f,
                        cap = StrokeCap.Round,
                    )
                }
            }
            LauncherWeatherVisualKind.SNOW -> {
                drawCloud(centerY = height * 0.28f)
                listOf(0.32f, 0.52f, 0.72f).forEach { x ->
                    val center = Offset(width * x, height * 0.80f)
                    drawLine(
                        color = accentColor,
                        start = Offset(center.x - width * 0.06f, center.y),
                        end = Offset(center.x + width * 0.06f, center.y),
                        strokeWidth = stroke * 0.60f,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = accentColor,
                        start = Offset(center.x, center.y - height * 0.06f),
                        end = Offset(center.x, center.y + height * 0.06f),
                        strokeWidth = stroke * 0.60f,
                        cap = StrokeCap.Round,
                    )
                }
            }
            LauncherWeatherVisualKind.THUNDERSTORM -> {
                drawCloud(centerY = height * 0.25f)
                val bolt = Path().apply {
                    moveTo(width * 0.54f, height * 0.57f)
                    lineTo(width * 0.40f, height * 0.76f)
                    lineTo(width * 0.52f, height * 0.76f)
                    lineTo(width * 0.43f, height * 0.96f)
                    lineTo(width * 0.68f, height * 0.69f)
                    lineTo(width * 0.56f, height * 0.69f)
                    close()
                }
                drawPath(bolt, color = accentColor)
            }
            LauncherWeatherVisualKind.UNKNOWN -> {
                drawCircle(
                    color = accentColor.copy(alpha = 0.16f),
                    radius = width * 0.36f,
                    center = Offset(width / 2f, height / 2f),
                )
                drawCircle(
                    color = accentColor,
                    radius = width * 0.06f,
                    center = Offset(width / 2f, height * 0.70f),
                )
                drawArc(
                    color = accentColor,
                    startAngle = -120f,
                    sweepAngle = 225f,
                    useCenter = false,
                    topLeft = Offset(width * 0.31f, height * 0.22f),
                    size = Size(width * 0.38f, height * 0.42f),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
    }
}
