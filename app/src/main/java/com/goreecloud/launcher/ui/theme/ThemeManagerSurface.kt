package com.goreecloud.launcher.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Native Theme Manager presentation foundation for the currently implemented
 * System/Light/Dark/Deep Dark appearance modes under the GLAZE UI V1.6 source mapping.
 *
 * This surface consumes caller/platform-supplied V1.6 presentation context without inferring
 * accessibility, performance, privacy, security, or capability truth. Icon-pack discovery,
 * wallpaper sampling, environmental color memory, broader Theme Engine behavior, and complete
 * V1.6 application acceptance remain separately gated.
 */
@Composable
fun ThemeManagerSurface(
    selectedMode: GlazeThemeMode,
    onSelectMode: (GlazeThemeMode) -> Unit,
    onBack: () -> Unit,
) {
    val presentation = GlazeV16PresentationPolicy.resolve(
        requestedMaterial = GlazeV16MaterialRole.RAISED,
        context = LocalGlazeV16PresentationContext.current,
    )
    val raisedContainer = if (presentation.materialRole == GlazeV16MaterialRole.SOLID) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = GlazeMetrics.space5, vertical = GlazeMetrics.space4),
            verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space4),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Theme Manager",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Preview and choose the current Launcher appearance",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.heightIn(min = GlazeMetrics.touchAssistanceTarget),
                ) { Text("Done") }
            }

            GlazeThemeManagerCatalog.choices.forEach { choice ->
                ThemeChoiceCard(
                    choice = choice,
                    selected = choice.mode == selectedMode,
                    onSelect = { onSelectMode(choice.mode) },
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(GlazeMetrics.radiusExtraLarge),
                colors = CardDefaults.cardColors(
                    containerColor = raisedContainer,
                ),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(GlazeMetrics.space4),
                    verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                ) {
                    Text("Theme Manager foundation", fontWeight = FontWeight.SemiBold)
                    Text(
                        "System, Light, Dark and Deep Dark use the current GLAZE UI V1.6 source mapping. Accessibility and performance presentation context may simplify material cost without changing task or authority truth. Rendered/device acceptance, icon packs, masking, wallpaper-derived palettes, environmental sampling and advanced expression controls remain separate Development work.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeChoiceCard(
    choice: GlazeThemeChoice,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val presentation = GlazeV16PresentationPolicy.resolve(
        requestedMaterial = GlazeV16MaterialRole.RAISED,
        context = LocalGlazeV16PresentationContext.current,
    )
    val cardContainer = if (presentation.materialRole == GlazeV16MaterialRole.SOLID) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(GlazeMetrics.radiusExtraLarge),
        colors = CardDefaults.cardColors(containerColor = cardContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(GlazeMetrics.space4),
            verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space3),
        ) {
            Column {
                Text(choice.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    choice.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            GlazeTheme(choice.mode) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clearAndSetSemantics {
                            contentDescription = choice.previewAccessibilityLabel
                        },
                    shape = RoundedCornerShape(GlazeMetrics.radiusLarge),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(GlazeMetrics.space3),
                        horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier.size(GlazeMetrics.touchAssistanceTarget),
                            shape = RoundedCornerShape(GlazeMetrics.radiusControl),
                            color = MaterialTheme.colorScheme.primary,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "G",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text("GoreeCloud", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Launcher appearance under V1.6 presentation semantics",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Surface(
                            modifier = Modifier.size(24.dp),
                            shape = RoundedCornerShape(GlazeMetrics.opticalMicro),
                            color = GlazeAtmosphere.softAmber,
                            content = {},
                        )
                    }
                }
            }

            if (selected) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = GlazeMetrics.touchAssistanceTarget)
                        .semantics {
                            liveRegion = LiveRegionMode.Polite
                            stateDescription = choice.selectedAccessibilityState
                        },
                    shape = RoundedCornerShape(GlazeMetrics.radiusControl),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("Selected", fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onSelect,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = GlazeMetrics.touchAssistanceTarget),
                ) {
                    Text("Use ${choice.title}")
                }
            }
        }
    }

    Spacer(Modifier.height(GlazeMetrics.space1))
}
