package com.goreecloud.launcher.ui

import android.content.pm.LauncherActivityInfo
import android.os.Process
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.goreecloud.launcher.core.launcher.LauncherDrawerProfileKind

/**
 * Mirrors the current bounded App Drawer profile projection: the primary Android user is User Apps,
 * while any non-primary LauncherApps inventory is presented as Work Apps. Android remains identity
 * authority; this helper changes presentation only.
 */
internal fun <U> launcherProfileBadgeKindForUser(
    user: U,
    primaryUser: U,
): LauncherDrawerProfileKind =
    if (user == primaryUser) LauncherDrawerProfileKind.USER else LauncherDrawerProfileKind.WORK

internal fun launcherProfileBadgeContentDescription(
    kind: LauncherDrawerProfileKind,
): String = when (kind) {
    LauncherDrawerProfileKind.USER -> "User profile"
    LauncherDrawerProfileKind.WORK -> "Work profile"
}

@Composable
internal fun LauncherAppProfileBadge(
    app: LauncherActivityInfo,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    LauncherProfileBadge(
        kind = launcherProfileBadgeKindForUser(app.user, Process.myUserHandle()),
        modifier = modifier,
        compact = compact,
    )
}

/**
 * Dedicated profile identity kept visually separate from notification, pin and App Lock badges.
 * Both User and Work receive a mark so absence is never used as the identity signal.
 */
@Composable
internal fun LauncherProfileBadge(
    kind: LauncherDrawerProfileKind,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val width = if (compact) 17.dp else 19.dp
    val height = if (compact) 15.dp else 17.dp
    val glyphSize = if (compact) 9.dp else 10.dp
    val container = when (kind) {
        LauncherDrawerProfileKind.USER ->
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.98f)
        LauncherDrawerProfileKind.WORK ->
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.98f)
    }
    val content = when (kind) {
        LauncherDrawerProfileKind.USER -> MaterialTheme.colorScheme.onSurfaceVariant
        LauncherDrawerProfileKind.WORK -> MaterialTheme.colorScheme.onTertiaryContainer
    }
    val outline = when (kind) {
        LauncherDrawerProfileKind.USER -> MaterialTheme.colorScheme.outline.copy(alpha = 0.48f)
        LauncherDrawerProfileKind.WORK -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.52f)
    }

    Surface(
        modifier = modifier
            .size(width = width, height = height)
            .semantics {
                contentDescription = launcherProfileBadgeContentDescription(kind)
            },
        shape = RoundedCornerShape(percent = 50),
        color = container,
        contentColor = content,
        border = BorderStroke(1.dp, outline),
        shadowElevation = 1.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(glyphSize)) {
                val u = size.minDimension
                val stroke = (u * 0.13f).coerceAtLeast(1f)
                when (kind) {
                    LauncherDrawerProfileKind.USER -> {
                        drawCircle(
                            color = content,
                            radius = u * 0.16f,
                            center = Offset(u * 0.50f, u * 0.29f),
                            style = Stroke(width = stroke),
                        )
                        drawArc(
                            color = content,
                            startAngle = 202f,
                            sweepAngle = 136f,
                            useCenter = false,
                            topLeft = Offset(u * 0.18f, u * 0.43f),
                            size = Size(u * 0.64f, u * 0.46f),
                            style = Stroke(width = stroke, cap = StrokeCap.Round),
                        )
                    }
                    LauncherDrawerProfileKind.WORK -> {
                        drawRoundRect(
                            color = content,
                            topLeft = Offset(u * 0.16f, u * 0.32f),
                            size = Size(u * 0.68f, u * 0.50f),
                            cornerRadius = CornerRadius(u * 0.08f),
                            style = Stroke(width = stroke),
                        )
                        drawArc(
                            color = content,
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = false,
                            topLeft = Offset(u * 0.34f, u * 0.14f),
                            size = Size(u * 0.32f, u * 0.30f),
                            style = Stroke(width = stroke, cap = StrokeCap.Round),
                        )
                        drawLine(
                            color = content,
                            start = Offset(u * 0.16f, u * 0.51f),
                            end = Offset(u * 0.84f, u * 0.51f),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round,
                        )
                    }
                }
            }
        }
    }
}
