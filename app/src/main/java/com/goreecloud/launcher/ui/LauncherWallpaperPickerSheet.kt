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
import com.goreecloud.launcher.core.launcher.LauncherWeatherVisualKind
import com.goreecloud.launcher.ui.theme.GlazeMetrics
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    val now = remember { LocalDateTime.now() }
    val locale = Locale.getDefault()
    val previewTime = remember(now.minute, locale) {
        now.format(DateTimeFormatter.ofPattern("h:mm", locale))
    }
    val previewDate = remember(now.dayOfYear, locale) {
        now.format(DateTimeFormatter.ofPattern("EEE, MMM d", locale))
    }
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
                    .fillMaxWidth()
                    .padding(GlazeMetrics.space3),
                verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = GlazeMetrics.space1),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        previewTime,
                        style = MaterialTheme.typography.displaySmall,
                        color = previewForeground.copy(alpha = 0.96f),
                        fontWeight = FontWeight.Light,
                    )
                    Text(
                        previewDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = previewForeground.copy(alpha = 0.82f),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                ) {
                    Surface(
                        modifier = Modifier.weight(1f).height(92.dp),
                        shape = RoundedCornerShape(22.dp),
                        color = Color(wallpaper.accentColor).copy(alpha = 0.34f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            LauncherWeatherIcon(
                                kind = LauncherWeatherVisualKind.UNKNOWN,
                                isDay = true,
                                color = previewForeground,
                                size = 32.dp,
                            )
                            Text(
                                "Weather · permission-gated",
                                style = MaterialTheme.typography.labelSmall,
                                color = previewForeground.copy(alpha = 0.80f),
                                maxLines = 1,
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier.weight(1f).height(92.dp),
                        shape = RoundedCornerShape(22.dp),
                        color = Color.Black.copy(alpha = 0.24f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "Today",
                                style = MaterialTheme.typography.labelMedium,
                                color = previewForeground.copy(alpha = 0.78f),
                            )
                            Text(
                                now.dayOfMonth.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = previewForeground,
                                fontWeight = FontWeight.Light,
                            )
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    repeat(4) { index ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (index % 2 == 0) {
                                        Color.White.copy(alpha = 0.84f)
                                    } else {
                                        Color(wallpaper.secondaryAccentColor).copy(alpha = 0.78f)
                                    },
                                    RoundedCornerShape(13.dp),
                                ),
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    repeat(3) { index ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (index == 1) 7.dp else 5.dp)
                                .background(
                                    Color.White.copy(alpha = if (index == 1) 0.92f else 0.46f),
                                    CircleShape,
                                ),
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = Color.White.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(Color.White.copy(alpha = 0.74f), CircleShape),
                        )
                        Text(
                            "Search with GoreeCloud…",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.86f),
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(alpha = 0.28f),
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
                                    .size(36.dp)
                                    .background(
                                        if (index == 2) {
                                            Color(wallpaper.accentColor).copy(alpha = 0.84f)
                                        } else {
                                            Color.White.copy(alpha = 0.80f)
                                        },
                                        RoundedCornerShape(12.dp),
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
