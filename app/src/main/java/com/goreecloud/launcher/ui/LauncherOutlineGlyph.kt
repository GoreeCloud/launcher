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

/**
 * Shared first-party Launcher outline glyph family.
 *
 * These glyphs intentionally share one optical box, one rounded stroke system and one coordinate
 * grammar so Settings, onboarding and Universal Search no longer drift into separate hand-drawn
 * icon dialects. Brand/provider marks remain separate because their geometry is not GoreeCloud-owned.
 */
internal enum class LauncherOutlineGlyph {
    HOME,
    APPS,
    DOCK,
    FOLDER,
    SEARCH,
    WIDGETS,
    GESTURE,
    APPEARANCE,
    BELL,
    SHIELD,
    BACKUP,
    SLIDERS,
    INFO,
    LOCK,
    QUICK_ANSWER,
    ACTION,
    SHORTCUT,
    CONTACT,
    PHONE,
    MESSAGE,
    FILE,
    SETTINGS,
    EDIT,
}

@Composable
internal fun LauncherOutlineGlyph(
    glyph: LauncherOutlineGlyph,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val u = size.minDimension
        val strokeWidth = 1.8.dp.toPx()
        val stroke = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )

        fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
            drawLine(
                color = color,
                start = Offset(u * x1, u * y1),
                end = Offset(u * x2, u * y2),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }

        fun roundedBox(
            left: Float,
            top: Float,
            width: Float,
            height: Float,
            radius: Float = 0.07f,
        ) {
            drawRoundRect(
                color = color,
                topLeft = Offset(u * left, u * top),
                size = Size(u * width, u * height),
                cornerRadius = CornerRadius(u * radius),
                style = stroke,
            )
        }

        when (glyph) {
            LauncherOutlineGlyph.HOME -> {
                val house = Path().apply {
                    moveTo(u * .16f, u * .46f)
                    lineTo(u * .50f, u * .18f)
                    lineTo(u * .84f, u * .46f)
                    moveTo(u * .26f, u * .41f)
                    lineTo(u * .26f, u * .78f)
                    lineTo(u * .74f, u * .78f)
                    lineTo(u * .74f, u * .41f)
                }
                drawPath(house, color, style = stroke)
                roundedBox(.43f, .57f, .14f, .21f, .025f)
            }

            LauncherOutlineGlyph.APPS -> {
                listOf(.20f to .20f, .56f to .20f, .20f to .56f, .56f to .56f)
                    .forEach { (x, y) -> roundedBox(x, y, .24f, .24f, .055f) }
            }

            LauncherOutlineGlyph.DOCK -> {
                listOf(.20f, .41f, .62f).forEach { x ->
                    roundedBox(x, .28f, .18f, .18f, .05f)
                }
                val tray = Path().apply {
                    moveTo(u * .16f, u * .59f)
                    lineTo(u * .16f, u * .69f)
                    quadraticBezierTo(u * .16f, u * .76f, u * .23f, u * .76f)
                    lineTo(u * .77f, u * .76f)
                    quadraticBezierTo(u * .84f, u * .76f, u * .84f, u * .69f)
                    lineTo(u * .84f, u * .59f)
                }
                drawPath(tray, color, style = stroke)
            }

            LauncherOutlineGlyph.FOLDER -> {
                val folder = Path().apply {
                    moveTo(u * .17f, u * .33f)
                    lineTo(u * .38f, u * .33f)
                    lineTo(u * .46f, u * .25f)
                    lineTo(u * .61f, u * .25f)
                    lineTo(u * .68f, u * .33f)
                    lineTo(u * .81f, u * .33f)
                    quadraticBezierTo(u * .85f, u * .33f, u * .85f, u * .38f)
                    lineTo(u * .85f, u * .73f)
                    quadraticBezierTo(u * .85f, u * .78f, u * .80f, u * .78f)
                    lineTo(u * .20f, u * .78f)
                    quadraticBezierTo(u * .15f, u * .78f, u * .15f, u * .73f)
                    lineTo(u * .15f, u * .38f)
                    quadraticBezierTo(u * .15f, u * .33f, u * .17f, u * .33f)
                    close()
                }
                drawPath(folder, color, style = stroke)
            }

            LauncherOutlineGlyph.SEARCH -> {
                drawCircle(
                    color = color,
                    radius = u * .235f,
                    center = Offset(u * .43f, u * .42f),
                    style = stroke,
                )
                line(.60f, .59f, .80f, .79f)
            }

            LauncherOutlineGlyph.WIDGETS -> {
                roundedBox(.17f, .18f, .29f, .29f, .055f)
                roundedBox(.54f, .18f, .29f, .45f, .055f)
                roundedBox(.17f, .55f, .29f, .28f, .055f)
                line(.62f, .73f, .76f, .73f)
            }

            LauncherOutlineGlyph.GESTURE -> {
                val swipe = Path().apply {
                    moveTo(u * .17f, u * .74f)
                    cubicTo(u * .35f, u * .74f, u * .54f, u * .61f, u * .68f, u * .34f)
                }
                drawPath(swipe, color, style = stroke)
                line(.47f, .35f, .70f, .29f)
                line(.70f, .29f, .75f, .52f)
                drawCircle(color, u * .035f, Offset(u * .21f, u * .59f))
            }

            LauncherOutlineGlyph.APPEARANCE -> {
                drawCircle(
                    color = color,
                    radius = u * .29f,
                    center = Offset(u * .47f, u * .49f),
                    style = stroke,
                )
                drawCircle(color, u * .035f, Offset(u * .37f, u * .36f))
                drawCircle(color, u * .035f, Offset(u * .56f, u * .33f))
                drawCircle(color, u * .035f, Offset(u * .61f, u * .52f))
                drawCircle(
                    color = color,
                    radius = u * .07f,
                    center = Offset(u * .40f, u * .61f),
                    style = stroke,
                )
            }

            LauncherOutlineGlyph.BELL -> {
                val bell = Path().apply {
                    moveTo(u * .28f, u * .70f)
                    quadraticBezierTo(u * .36f, u * .60f, u * .36f, u * .47f)
                    quadraticBezierTo(u * .36f, u * .30f, u * .50f, u * .27f)
                    quadraticBezierTo(u * .64f, u * .30f, u * .64f, u * .47f)
                    quadraticBezierTo(u * .64f, u * .60f, u * .72f, u * .70f)
                    close()
                }
                drawPath(bell, color, style = stroke)
                drawArc(
                    color = color,
                    startAngle = 5f,
                    sweepAngle = 170f,
                    useCenter = false,
                    topLeft = Offset(u * .45f, u * .70f),
                    size = Size(u * .10f, u * .10f),
                    style = stroke,
                )
            }

            LauncherOutlineGlyph.SHIELD -> {
                val shield = Path().apply {
                    moveTo(u * .50f, u * .14f)
                    lineTo(u * .76f, u * .24f)
                    lineTo(u * .72f, u * .57f)
                    quadraticBezierTo(u * .67f, u * .75f, u * .50f, u * .85f)
                    quadraticBezierTo(u * .33f, u * .75f, u * .28f, u * .57f)
                    lineTo(u * .24f, u * .24f)
                    close()
                }
                drawPath(shield, color, style = stroke)
                line(.50f, .42f, .50f, .64f)
                drawCircle(color, u * .035f, Offset(u * .50f, u * .35f))
            }

            LauncherOutlineGlyph.BACKUP -> {
                roundedBox(.20f, .51f, .60f, .28f, .065f)
                line(.33f, .69f, .67f, .69f)
                drawArc(
                    color = color,
                    startAngle = 200f,
                    sweepAngle = 260f,
                    useCenter = false,
                    topLeft = Offset(u * .28f, u * .16f),
                    size = Size(u * .44f, u * .44f),
                    style = stroke,
                )
                line(.30f, .22f, .30f, .38f)
                line(.30f, .38f, .46f, .38f)
            }

            LauncherOutlineGlyph.SLIDERS -> {
                listOf(.30f, .50f, .70f).forEachIndexed { index, y ->
                    val x = listOf(.38f, .63f, .46f)[index]
                    line(.17f, y, x - .095f, y)
                    line(x + .095f, y, .83f, y)
                    drawCircle(
                        color = color,
                        radius = u * .07f,
                        center = Offset(u * x, u * y),
                        style = stroke,
                    )
                }
            }

            LauncherOutlineGlyph.INFO -> {
                drawCircle(color, u * .31f, Offset(u * .50f, u * .50f), style = stroke)
                drawCircle(color, u * .025f, Offset(u * .50f, u * .36f))
                line(.50f, .48f, .50f, .67f)
            }

            LauncherOutlineGlyph.LOCK -> {
                roundedBox(.27f, .43f, .46f, .36f, .07f)
                drawArc(
                    color = color,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(u * .34f, u * .18f),
                    size = Size(u * .32f, u * .42f),
                    style = stroke,
                )
            }

            LauncherOutlineGlyph.QUICK_ANSWER -> {
                line(.50f, .16f, .50f, .84f)
                line(.16f, .50f, .84f, .50f)
                line(.28f, .28f, .72f, .72f)
                line(.72f, .28f, .28f, .72f)
            }

            LauncherOutlineGlyph.ACTION -> {
                val compass = Path().apply {
                    moveTo(u * .27f, u * .73f)
                    lineTo(u * .41f, u * .35f)
                    lineTo(u * .76f, u * .20f)
                    lineTo(u * .64f, u * .59f)
                    close()
                }
                drawPath(compass, color, style = stroke)
                drawCircle(color, u * .035f, Offset(u * .57f, u * .40f))
            }

            LauncherOutlineGlyph.SHORTCUT -> {
                drawCircle(color, u * .18f, Offset(u * .39f, u * .50f), style = stroke)
                drawCircle(color, u * .18f, Offset(u * .61f, u * .50f), style = stroke)
                line(.45f, .50f, .55f, .50f)
            }

            LauncherOutlineGlyph.CONTACT -> {
                drawCircle(color, u * .13f, Offset(u * .50f, u * .34f), style = stroke)
                drawArc(
                    color = color,
                    startAngle = 205f,
                    sweepAngle = 130f,
                    useCenter = false,
                    topLeft = Offset(u * .24f, u * .50f),
                    size = Size(u * .52f, u * .31f),
                    style = stroke,
                )
            }

            LauncherOutlineGlyph.PHONE -> {
                val phone = Path().apply {
                    moveTo(u * .28f, u * .20f)
                    cubicTo(u * .20f, u * .31f, u * .29f, u * .57f, u * .46f, u * .71f)
                    cubicTo(u * .60f, u * .82f, u * .75f, u * .82f, u * .81f, u * .70f)
                }
                drawPath(phone, color, style = stroke)
                line(.25f, .20f, .35f, .27f)
                line(.71f, .68f, .81f, .75f)
            }

            LauncherOutlineGlyph.MESSAGE -> {
                roundedBox(.17f, .23f, .66f, .45f, .14f)
                line(.35f, .68f, .28f, .80f)
                listOf(.38f, .50f, .62f).forEach { x ->
                    drawCircle(color, u * .026f, Offset(u * x, u * .45f))
                }
            }

            LauncherOutlineGlyph.FILE -> {
                val file = Path().apply {
                    moveTo(u * .29f, u * .16f)
                    lineTo(u * .58f, u * .16f)
                    lineTo(u * .75f, u * .33f)
                    lineTo(u * .75f, u * .83f)
                    lineTo(u * .29f, u * .83f)
                    close()
                }
                drawPath(file, color, style = stroke)
                line(.58f, .16f, .58f, .33f)
                line(.58f, .33f, .75f, .33f)
            }

            LauncherOutlineGlyph.SETTINGS -> {
                val center = Offset(u * .50f, u * .50f)
                drawCircle(color, u * .22f, center, style = stroke)
                drawCircle(color, u * .06f, center, style = stroke)
                repeat(8) { index ->
                    val angle = Math.toRadians(index * 45.0)
                    val inner = u * .22f
                    val outer = u * .35f
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
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                }
            }

            LauncherOutlineGlyph.EDIT -> {
                roundedBox(.20f, .22f, .52f, .56f, .06f)
                line(.45f, .60f, .77f, .28f)
                line(.68f, .24f, .81f, .37f)
            }
        }
    }
}
