package com.goreecloud.launcher.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.goreecloud.launcher.core.launcher.LauncherBuiltInWallpaper
import com.goreecloud.launcher.core.launcher.LauncherBuiltInWallpaperId
import com.goreecloud.launcher.core.launcher.LauncherBuiltInWallpapers
import com.goreecloud.launcher.ui.theme.GlazeMetrics

/**
 * Launcher-owned wallpaper chooser.
 *
 * Selecting a card only changes this transient preview. Wallpaper cards expose explicit
 * single-selection semantics for assistive technology, and the system wallpaper is mutated only
 * after the user explicitly presses Apply.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun LauncherWallpaperPickerSheet(
    onDismiss: () -> Unit,
    onApply: (LauncherBuiltInWallpaperId) -> Unit,
    onOpenAndroidPicker: () -> Unit,
) {
    val wallpapers = LauncherBuiltInWallpapers.all
    var selectedId by remember { mutableStateOf(wallpapers.first().id) }
    val selected = LauncherBuiltInWallpapers.find(selectedId)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = GlazeMetrics.space4, vertical = GlazeMetrics.space3),
            verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space3),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Wallpapers",
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Preview a GoreeCloud wallpaper before applying it. Selecting a card does not change your wallpaper.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            WallpaperHomePreview(selected)

            Text(
                "GoreeCloud collection",
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space3),
            ) {
                wallpapers.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                    ) {
                        row.forEach { wallpaper ->
                            WallpaperChoiceCard(
                                wallpaper = wallpaper,
                                selected = wallpaper.id == selectedId,
                                onSelect = { selectedId = wallpaper.id },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (row.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
            ) {
                OutlinedButton(
                    onClick = onOpenAndroidPicker,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("More from Android")
                }
                Button(
                    onClick = { onApply(selected.id) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Apply")
                }
            }

            Text(
                "Apply changes the Android system wallpaper. Cancel or swipe down to leave it unchanged.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(GlazeMetrics.space2))
        }
    }
}

@Composable
private fun WallpaperChoiceCard(
    wallpaper: LauncherBuiltInWallpaper,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(GlazeMetrics.radiusLarge)
    Surface(
        modifier = modifier
            .testTag("launcher-wallpaper-choice-${wallpaper.id.name}")
            .selectable(
                selected = selected,
                onClick = onSelect,
                role = Role.RadioButton,
            )
            .semantics(mergeDescendants = true) {},
        shape = shape,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
        },
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.78f)
                    .background(wallpaperBrush(wallpaper), RoundedCornerShape(18.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(36.dp)
                        .background(
                            Color(wallpaper.accentColor).copy(alpha = 0.24f),
                            CircleShape,
                        ),
                )
                if (selected) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp),
                        shape = RoundedCornerShape(999.dp),
                        color = Color.Black.copy(alpha = 0.52f),
                    ) {
                        Text(
                            "Selected",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                        )
                    }
                }
            }
            Text(
                wallpaper.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                wallpaper.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WallpaperHomePreview(wallpaper: LauncherBuiltInWallpaper) {
    val previewForeground = Color.White
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(GlazeMetrics.radiusExtraLarge),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .background(wallpaperBrush(wallpaper)),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(GlazeMetrics.space4),
            ) {
                Text(
                    "2:37",
                    style = MaterialTheme.typography.displayMedium,
                    color = previewForeground.copy(alpha = 0.94f),
                    fontWeight = FontWeight.Light,
                )
                Text(
                    "Wed, Sep 23",
                    style = MaterialTheme.typography.bodyMedium,
                    color = previewForeground.copy(alpha = 0.80f),
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(GlazeMetrics.space3),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    repeat(5) { index ->
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(
                                    if (index % 2 == 0) {
                                        Color.White.copy(alpha = 0.82f)
                                    } else {
                                        Color(wallpaper.secondaryAccentColor).copy(alpha = 0.72f)
                                    },
                                    RoundedCornerShape(12.dp),
                                ),
                        )
                    }
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = Color.Black.copy(alpha = 0.26f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            "Search GoreeCloud",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.84f),
                        )
                    }
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(alpha = 0.30f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        repeat(5) { index ->
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(
                                        if (index == 3) {
                                            Color(wallpaper.accentColor).copy(alpha = 0.80f)
                                        } else {
                                            Color.White.copy(alpha = 0.78f)
                                        },
                                        RoundedCornerShape(11.dp),
                                    ),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun wallpaperBrush(wallpaper: LauncherBuiltInWallpaper): Brush =
    Brush.linearGradient(
        colors = listOf(
            Color(wallpaper.startColor),
            Color(wallpaper.middleColor),
            Color(wallpaper.endColor),
        ),
    )
