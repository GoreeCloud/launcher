package com.goreecloud.launcher.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.goreecloud.launcher.core.launcher.LauncherHomeAppMode
import com.goreecloud.launcher.core.launcher.LauncherUniversalSearchHomeMode
import com.goreecloud.launcher.ui.theme.GlazeMetrics

data class LauncherStartupConfiguration(
    val homeAppMode: LauncherHomeAppMode,
    val homeColumns: Int,
    val homeRows: Int,
    val showHomeLabels: Boolean,
    val universalSearchHomeMode: LauncherUniversalSearchHomeMode,
    val addNewAppsToHome: Boolean,
    val showHints: Boolean,
)

@Composable
fun LauncherStartupWizard(
    isDefaultHome: Boolean,
    initialHomeAppMode: LauncherHomeAppMode,
    initialHomeColumns: Int,
    initialHomeRows: Int,
    initialShowHomeLabels: Boolean,
    initialUniversalSearchHomeMode: LauncherUniversalSearchHomeMode,
    initialAddNewAppsToHome: Boolean,
    initialShowHints: Boolean,
    onRequestHomeRole: () -> Unit,
    onFinish: (LauncherStartupConfiguration) -> Unit,
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var homeAppModeName by rememberSaveable {
        mutableStateOf(
            (if (initialHomeAppMode == LauncherHomeAppMode.NONE) {
                LauncherHomeAppMode.RECENT
            } else {
                initialHomeAppMode
            }).name,
        )
    }
    var gridName by rememberSaveable {
        mutableStateOf("${initialHomeColumns} x ${initialHomeRows}")
    }
    var showHomeLabels by rememberSaveable { mutableStateOf(initialShowHomeLabels) }
    var searchModeName by rememberSaveable {
        mutableStateOf(initialUniversalSearchHomeMode.name)
    }
    var addNewAppsToHome by rememberSaveable { mutableStateOf(initialAddNewAppsToHome) }
    var showHints by rememberSaveable { mutableStateOf(initialShowHints) }

    val selectedHomeAppMode = runCatching { LauncherHomeAppMode.valueOf(homeAppModeName) }
        .getOrDefault(LauncherHomeAppMode.RECENT)
    val selectedSearchMode = runCatching {
        LauncherUniversalSearchHomeMode.valueOf(searchModeName)
    }.getOrDefault(LauncherUniversalSearchHomeMode.SWIPE_DOWN_ONLY)
    val selectedGrid = when (gridName) {
        "4 x 5" -> 4 to 5
        "6 x 7" -> 6 to 7
        else -> 5 to 6
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = GlazeMetrics.space4, vertical = GlazeMetrics.space3),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(GlazeMetrics.radius2ExtraLarge),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                tonalElevation = 0.dp,
                shadowElevation = 18.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(GlazeMetrics.space4),
                    verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space3),
                ) {
                    Text(
                        text = when (step) {
                            0 -> "Welcome to GoreeCloud Launcher"
                            1 -> "Set up your Home"
                            else -> "Search, gestures, and hints"
                        },
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        when (step) {
                            0 -> "Choose the essentials now. You can change every option later from Edit Home → Settings."
                            1 -> "Choose what Launcher should place automatically above the Dock. Manual drag-and-drop always remains available."
                            else -> "Choose how Search appears and whether Launcher should keep showing helpful usage hints."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    when (step) {
                        0 -> {
                            WizardInfoCard(
                                title = if (isDefaultHome) "Default launcher ready" else "Make GoreeCloud Launcher your default",
                                summary = if (isDefaultHome) {
                                    "Android is already routing Home to GoreeCloud Launcher."
                                } else {
                                    "Android must grant the Home role before GoreeCloud Launcher can fully replace the current launcher."
                                },
                            )
                            if (!isDefaultHome) {
                                Button(
                                    onClick = onRequestHomeRole,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("Set as default launcher")
                                }
                            }
                            WizardInfoCard(
                                title = "Private by default",
                                summary = "Recent and most-used Home modes use only launches made through GoreeCloud Launcher. They do not request Android Usage Access, store timestamps, or collect dwell time.",
                            )
                        }

                        1 -> {
                            WizardSectionTitle("Automatic Home apps")
                            WizardRadioRow(
                                title = "No automatic apps",
                                summary = "Start with an uncluttered Home. Add apps manually whenever you want.",
                                selected = selectedHomeAppMode == LauncherHomeAppMode.NONE,
                                onClick = { homeAppModeName = LauncherHomeAppMode.NONE.name },
                            )
                            WizardRadioRow(
                                title = "10 most recent apps",
                                summary = "Keep up to 10 apps you most recently launched from GoreeCloud Launcher in the automatic Home slots.",
                                selected = selectedHomeAppMode == LauncherHomeAppMode.RECENT,
                                onClick = { homeAppModeName = LauncherHomeAppMode.RECENT.name },
                            )
                            WizardRadioRow(
                                title = "10 most used apps",
                                summary = "Keep up to 10 apps with the highest local Launcher launch counts in the automatic Home slots.",
                                selected = selectedHomeAppMode == LauncherHomeAppMode.MOST_USED,
                                onClick = { homeAppModeName = LauncherHomeAppMode.MOST_USED.name },
                            )

                            WizardSectionTitle("Home grid")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                            ) {
                                listOf("4 x 5", "5 x 6", "6 x 7").forEach { option ->
                                    OutlinedButton(
                                        onClick = { gridName = option },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text(
                                            if (gridName == option) "✓ $option" else option,
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                            WizardSwitchRow(
                                title = "Show Home app labels",
                                summary = "Display app names below Home icons.",
                                checked = showHomeLabels,
                                onCheckedChange = { showHomeLabels = it },
                            )
                            WizardSwitchRow(
                                title = "Add newly installed apps to Home",
                                summary = "Automatically pin newly discovered primary-profile apps. Off by default.",
                                checked = addNewAppsToHome,
                                onCheckedChange = { addNewAppsToHome = it },
                            )
                        }

                        else -> {
                            WizardSectionTitle("Universal Search")
                            WizardRadioRow(
                                title = "Swipe down to search",
                                summary = "Keep the Home visually minimal and open Universal Search with a downward swipe.",
                                selected = selectedSearchMode == LauncherUniversalSearchHomeMode.SWIPE_DOWN_ONLY,
                                onClick = {
                                    searchModeName = LauncherUniversalSearchHomeMode.SWIPE_DOWN_ONLY.name
                                },
                            )
                            WizardRadioRow(
                                title = "Show a Search bar on Home",
                                summary = "Keep a permanent Search affordance on the Home screen.",
                                selected = selectedSearchMode == LauncherUniversalSearchHomeMode.PERMANENT,
                                onClick = {
                                    searchModeName = LauncherUniversalSearchHomeMode.PERMANENT.name
                                },
                            )
                            WizardSwitchRow(
                                title = "Show Launcher hints",
                                summary = "Show short, dismissible hints for Apps, Search, editing Home, and drag-and-drop. You can show them again later from Settings.",
                                checked = showHints,
                                onCheckedChange = { showHints = it },
                            )
                            WizardInfoCard(
                                title = "Default gestures",
                                summary = "Swipe up opens Apps. Swipe down opens Universal Search. Long-press empty Home space opens Edit Home.",
                            )
                        }
                    }

                    Spacer(Modifier.height(GlazeMetrics.space1))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                    ) {
                        if (step > 0) {
                            OutlinedButton(
                                onClick = { step -= 1 },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Back")
                            }
                        }
                        Button(
                            onClick = {
                                if (step < 2) {
                                    step += 1
                                } else {
                                    onFinish(
                                        LauncherStartupConfiguration(
                                            homeAppMode = selectedHomeAppMode,
                                            homeColumns = selectedGrid.first,
                                            homeRows = selectedGrid.second,
                                            showHomeLabels = showHomeLabels,
                                            universalSearchHomeMode = selectedSearchMode,
                                            addNewAppsToHome = addNewAppsToHome,
                                            showHints = showHints,
                                        ),
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(if (step < 2) "Continue" else "Finish setup")
                        }
                    }
                    Text(
                        "Step ${step + 1} of 3",
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun LauncherHomeHintCard(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.widthIn(max = 620.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(GlazeMetrics.radiusExtraLarge),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 12.dp,
    ) {
        Column(
            modifier = Modifier.padding(GlazeMetrics.space3),
            verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
        ) {
            Text(
                "Launcher hints",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Swipe up for Apps • Swipe down for Search • Long-press empty Home space to customize • Drag apps from Apps onto Home or the Dock.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(onClick = onDismiss) {
                    Text("Got it")
                }
            }
        }
    }
}

@Composable
private fun WizardSectionTitle(text: String) {
    Text(
        text,
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun WizardInfoCard(
    title: String,
    summary: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(GlazeMetrics.radiusLarge),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
    ) {
        Column(
            modifier = Modifier.padding(GlazeMetrics.space3),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WizardRadioRow(
    title: String,
    summary: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(GlazeMetrics.radiusLarge),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f)
        },
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(GlazeMetrics.space3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
        ) {
            RadioButton(selected = selected, onClick = null)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun WizardSwitchRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = GlazeMetrics.space1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space3),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
