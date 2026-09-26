package com.goreecloud.launcher.ui

import android.content.pm.LauncherActivityInfo
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.goreecloud.launcher.core.launcher.LauncherBadgeAppKey
import com.goreecloud.launcher.core.launcher.LauncherBadgeCorner
import com.goreecloud.launcher.core.launcher.LauncherBadgeSize
import com.goreecloud.launcher.core.launcher.LauncherBadgeStyle
import com.goreecloud.launcher.core.launcher.LauncherNotificationBadges

/**
 * Badge geometry follows the user's explicit choice in local preferences. A profile's
 * notifications are never disclosed to another profile or used without Android's grant.
 */
@Composable
internal fun BoxScope.launcherBadgePositionModifier(): Modifier {
    val corner by LauncherNotificationBadges.corner.collectAsState()
    val placement = when (corner) {
        LauncherBadgeCorner.TOP_START -> Alignment.TopStart
        LauncherBadgeCorner.TOP_END -> Alignment.TopEnd
        LauncherBadgeCorner.BOTTOM_START -> Alignment.BottomStart
        LauncherBadgeCorner.BOTTOM_END -> Alignment.BottomEnd
    }
    val dx = if (corner == LauncherBadgeCorner.TOP_START ||
        corner == LauncherBadgeCorner.BOTTOM_START
    ) (-6).dp else 6.dp
    val dy = if (corner == LauncherBadgeCorner.TOP_START ||
        corner == LauncherBadgeCorner.TOP_END
    ) (-5).dp else 5.dp
    return Modifier.align(placement).offset(x = dx, y = dy)
}

@Composable
internal fun LauncherAppBadgeMark(
    app: LauncherActivityInfo,
    modifier: Modifier = Modifier,
) {
    val enabled by LauncherNotificationBadges.enabled.collectAsState()
    val granted by LauncherNotificationBadges.accessGranted.collectAsState()
    val counts by LauncherNotificationBadges.counts.collectAsState()
    val count = if (enabled && granted) LauncherNotificationBadges.countFor(app, counts) else 0
    LauncherBadgeMark(count, modifier)
}

@Composable
internal fun LauncherFolderBadgeMark(
    apps: List<LauncherActivityInfo>,
    modifier: Modifier = Modifier,
) {
    val enabled by LauncherNotificationBadges.enabled.collectAsState()
    val granted by LauncherNotificationBadges.accessGranted.collectAsState()
    val counts by LauncherNotificationBadges.counts.collectAsState()
    // An app may expose several launcher activities or aliases: count its package once.
    val count = if (enabled && granted) {
        apps.map { LauncherBadgeAppKey(it.componentName.packageName, it.user) }
            .toSet().sumOf { key -> counts[key] ?: 0 }
    } else 0
    LauncherBadgeMark(count, modifier)
}

@Composable
private fun LauncherBadgeMark(count: Int, modifier: Modifier) {
    if (count <= 0) return
    val style by LauncherNotificationBadges.style.collectAsState()
    val size by LauncherNotificationBadges.size.collectAsState()
    val label = if (style == LauncherBadgeStyle.DOT) {
        "Active notifications"
    } else {
        "$count active notification" + if (count == 1) "" else "s"
    }
    val accessible = modifier.semantics(mergeDescendants = true) {
        contentDescription = label
    }
    if (style == LauncherBadgeStyle.DOT) {
        val diameter = when (size) {
            LauncherBadgeSize.SMALL -> 7.dp
            LauncherBadgeSize.MEDIUM -> 10.dp
            LauncherBadgeSize.LARGE -> 14.dp
        }
        Surface(
            modifier = accessible.size(diameter),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surface),
        ) {}
    } else {
        val diameter = when (size) {
            LauncherBadgeSize.SMALL -> 17.dp
            LauncherBadgeSize.MEDIUM -> 20.dp
            LauncherBadgeSize.LARGE -> 25.dp
        }
        Surface(
            modifier = accessible.defaultMinSize(minWidth = diameter, minHeight = diameter),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surface),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 3.dp)) {
                Text(
                    if (count > 99) "99+" else count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
        }
    }
}
