package com.goreecloud.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Compact Launcher-owned line icons for the App Drawer header.
 *
 * These stay vector-drawn instead of relying on font glyphs so stroke weight, optical size, and
 * baseline remain stable across OEM fonts.
 */
@Composable
internal fun LauncherDrawerSortIcon(
    ascending: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val stroke = 1.9.dp.toPx()
        val cap = StrokeCap.Round

        // Sort direction at left.
        val x = size.width * 0.27f
        val top = size.height * 0.20f
        val bottom = size.height * 0.80f
        drawLine(color, Offset(x, top), Offset(x, bottom), stroke, cap = cap)
        val arrowTipY = if (ascending) top else bottom
        val armY = if (ascending) top + size.height * 0.13f else bottom - size.height * 0.13f
        drawLine(
            color,
            Offset(x, arrowTipY),
            Offset(x - size.width * 0.11f, armY),
            stroke,
            cap = cap,
        )
        drawLine(
            color,
            Offset(x, arrowTipY),
            Offset(x + size.width * 0.11f, armY),
            stroke,
            cap = cap,
        )

        // Three descending text/order lines at right.
        listOf(
            0.31f to 0.80f,
            0.50f to 0.70f,
            0.69f to 0.59f,
        ).forEach { (y, endX) ->
            drawLine(
                color,
                Offset(size.width * 0.50f, size.height * y),
                Offset(size.width * endX, size.height * y),
                stroke,
                cap = cap,
            )
        }
    }
}

@Composable
internal fun LauncherDrawerNewFolderIcon(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val stroke = 1.9.dp.toPx()
        val folder = Path().apply {
            moveTo(size.width * 0.12f, size.height * 0.31f)
            lineTo(size.width * 0.39f, size.height * 0.31f)
            lineTo(size.width * 0.47f, size.height * 0.22f)
            lineTo(size.width * 0.62f, size.height * 0.22f)
            lineTo(size.width * 0.68f, size.height * 0.31f)
            lineTo(size.width * 0.88f, size.height * 0.31f)
            lineTo(size.width * 0.88f, size.height * 0.79f)
            lineTo(size.width * 0.12f, size.height * 0.79f)
            close()
        }
        drawPath(
            path = folder,
            color = color,
            style = Stroke(
                width = stroke,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )

        val cx = size.width * 0.64f
        val cy = size.height * 0.55f
        val arm = size.minDimension * 0.105f
        drawLine(
            color,
            Offset(cx - arm, cy),
            Offset(cx + arm, cy),
            stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color,
            Offset(cx, cy - arm),
            Offset(cx, cy + arm),
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
        val stroke = 1.9.dp.toPx()
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            color = color,
            radius = size.minDimension * 0.25f,
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
            val outer = size.minDimension * 0.43f
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
