package com.goreecloud.launcher.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
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
) {
    val filters = LauncherDrawerDiscoveryFilter.entries.filter { filter ->
        (filter != LauncherDrawerDiscoveryFilter.PINNED || pinnedAvailable) &&
            (filter != LauncherDrawerDiscoveryFilter.SUGGESTED || suggestionsEnabled)
    }
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        filters.forEach { filter ->
            val active = selectedFilter == filter
            Surface(
                onClick = { chooseFilter(filter) },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .semantics {
                        role = Role.Tab
                        selected = active
                        contentDescription = filter.displayName + " apps"
                    },
                shape = RoundedCornerShape(GlazeMetrics.radiusPill),
                color = if (active) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                } else {
                    androidx.compose.ui.graphics.Color.Transparent
                },
                border = BorderStroke(
                    1.dp,
                    if (active) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.46f)
                    } else {
                        secondaryColor.copy(alpha = 0.18f)
                    },
                ),
            ) {
                Text(
                    filter.displayName,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    color = if (active) MaterialTheme.colorScheme.primary else secondaryColor,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
