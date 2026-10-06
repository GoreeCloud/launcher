package com.goreecloud.launcher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.goreecloud.launcher.core.launcher.LauncherDrawerDiscoveryFilter
import com.goreecloud.launcher.ui.theme.GlazeMetrics

@Composable
internal fun LauncherDrawerDiscoveryFiltersRow(
    selectedFilter: LauncherDrawerDiscoveryFilter,
    pinnedAvailable: Boolean,
    suggestionsEnabled: Boolean,
    secondaryColor: androidx.compose.ui.graphics.Color,
    chooseFilter: (LauncherDrawerDiscoveryFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filters = LauncherDrawerDiscoveryFilter.entries.filter { filter ->
        (filter != LauncherDrawerDiscoveryFilter.PINNED || pinnedAvailable) &&
            (filter != LauncherDrawerDiscoveryFilter.SUGGESTED || suggestionsEnabled)
    }
    var menuExpanded by remember { mutableStateOf(false) }
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            onClick = { menuExpanded = true },
            modifier = Modifier
                .size(48.dp)
                .testTag("launcher-drawer-filter")
                .semantics {
                    role = Role.Button
                    contentDescription = "Filter apps. Current " + selectedFilter.displayName
                },
            shape = CircleShape,
            color = if (selectedFilter == LauncherDrawerDiscoveryFilter.ALL) {
                androidx.compose.ui.graphics.Color.Transparent
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
            },
        ) {
            Box(contentAlignment = Alignment.Center) {
                LauncherOutlineGlyph(
                    glyph = LauncherOutlineGlyph.SLIDERS,
                    color = if (selectedFilter == LauncherDrawerDiscoveryFilter.ALL) {
                        secondaryColor
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }
        }
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
        ) {
            filters.forEach { filter ->
                DropdownMenuItem(
                    text = {
                        Text(
                            filter.displayName,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                    onClick = {
                        chooseFilter(filter)
                        menuExpanded = false
                    },
                    trailingIcon = if (filter == selectedFilter) {
                        { LauncherDrawerFilterSelectedGlyph(MaterialTheme.colorScheme.primary) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
private fun LauncherDrawerFilterSelectedGlyph(
    color: androidx.compose.ui.graphics.Color,
) {
    androidx.compose.foundation.Canvas(Modifier.size(16.dp)) {
        val stroke = 2.dp.toPx()
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(size.width * 0.18f, size.height * 0.52f),
            end = androidx.compose.ui.geometry.Offset(size.width * 0.41f, size.height * 0.73f),
            strokeWidth = stroke,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(size.width * 0.41f, size.height * 0.73f),
            end = androidx.compose.ui.geometry.Offset(size.width * 0.82f, size.height * 0.28f),
            strokeWidth = stroke,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
    }
}
