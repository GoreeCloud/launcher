package com.goreecloud.launcher.ui

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.goreecloud.launcher.ui.theme.GlazeMetrics
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

private const val LAYOUT_UNLOCK_HOLD_MILLIS = 5_000L

@Composable
fun LayoutLockHoldControl(
    locked: Boolean,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!locked) return

    var holding by remember(locked) { mutableStateOf(false) }
    var progress by remember(locked) { mutableFloatStateOf(0f) }

    LaunchedEffect(holding, locked) {
        if (!holding || !locked) {
            progress = 0f
            return@LaunchedEffect
        }

        val startedAt = SystemClock.uptimeMillis()
        while (holding && locked && progress < 1f) {
            progress = ((SystemClock.uptimeMillis() - startedAt).toFloat() /
                LAYOUT_UNLOCK_HOLD_MILLIS.toFloat()).coerceIn(0f, 1f)
            delay(50)
        }
    }

    Surface(
        modifier = modifier
            .semantics {
                contentDescription = "Home screen layout locked. Unlock in Launcher settings or hold here for five seconds."
            }
            .pointerInput(locked, onUnlock) {
                detectTapGestures(
                    onPress = {
                        holding = true
                        val releasedBeforeTimeout = withTimeoutOrNull(LAYOUT_UNLOCK_HOLD_MILLIS) {
                            tryAwaitRelease()
                        }
                        if (releasedBeforeTimeout == null) {
                            progress = 1f
                            onUnlock()
                        }
                        holding = false
                    },
                )
            },
        shape = RoundedCornerShape(GlazeMetrics.radiusControl),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = GlazeMetrics.space3,
                vertical = GlazeMetrics.space2,
            ),
            verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space1),
        ) {
            Text(
                text = "Layout locked",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (holding) "Keep holding to unlock…" else "Hold 5 seconds to unlock",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(3.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}
