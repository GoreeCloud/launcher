package com.goreecloud.launcher.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
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
    val dockSize: Int = 5,
    val enableDrawerTabs: Boolean = false,
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
    initialEnableDrawerTabs: Boolean = false,
    initialDockSize: Int = 5,
    initialStep: Int = 0,
    onStepChange: (Int) -> Unit = {},
    onRequestHomeRole: () -> Unit,
    onFinish: (LauncherStartupConfiguration) -> Unit,
) {
    var step by rememberSaveable(initialStep) { mutableIntStateOf(initialStep.coerceIn(0, 2)) }
    var homeAppModeName by rememberSaveable {
        mutableStateOf(initialHomeAppMode.name)
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
    var enableDrawerTabs by rememberSaveable { mutableStateOf(initialEnableDrawerTabs) }
    var dockSize by rememberSaveable { mutableIntStateOf(initialDockSize.coerceIn(4, 6)) }
    var showDetails by rememberSaveable { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    LaunchedEffect(step) {
        showDetails = false
        scrollState.scrollTo(0)
    }

    val selectedHomeAppMode = runCatching { LauncherHomeAppMode.valueOf(homeAppModeName) }
        .getOrDefault(LauncherHomeAppMode.NONE)
    val selectedHomeAppLabel = when (selectedHomeAppMode) {
        LauncherHomeAppMode.NONE -> "None"
        LauncherHomeAppMode.RECENT -> "Recent"
        LauncherHomeAppMode.MOST_USED -> "Most used"
    }
    val selectedSearchMode = runCatching {
        LauncherUniversalSearchHomeMode.valueOf(searchModeName)
    }.getOrDefault(LauncherUniversalSearchHomeMode.SWIPE_DOWN_ONLY)
    val selectedGrid = when (gridName) {
        "4 x 5" -> 4 to 5
        "6 x 7" -> 6 to 7
        else -> 5 to 6
    }

    val stepTitle = when (step) {
        0 -> "Welcome to GoreeCloud Launcher"
        1 -> "Build your Home"
        else -> "Search and gestures"
    }
    val stepSummary = when (step) {
        0 -> "Choose the essentials. Change anything later."
        1 -> "Start clean, then choose what appears automatically."
        else -> "Choose Search and learn the core gestures."
    }
    val stepSymbol = when (step) {
        0 -> WizardVisualSymbol.HOME
        1 -> WizardVisualSymbol.APPS
        else -> WizardVisualSymbol.SEARCH
    }
    val stepAccent = when (step) {
        0 -> MaterialTheme.colorScheme.primary
        1 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.secondary
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
                .padding(horizontal = GlazeMetrics.space3, vertical = GlazeMetrics.space2),
            contentAlignment = Alignment.TopCenter,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(
                    GlazeMetrics.radius2ExtraLarge,
                ),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                tonalElevation = 0.dp,
                shadowElevation = 2.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(GlazeMetrics.space3),
                    verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                ) {
                    WizardProgress(step = step, accent = stepAccent)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                    ) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(
                                GlazeMetrics.radiusLarge,
                            ),
                            color = stepAccent.copy(alpha = 0.13f),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                WizardVisualGlyph(
                                    symbol = stepSymbol,
                                    tint = stepAccent,
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = stepTitle,
                                modifier = Modifier.semantics { heading() },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                stepSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    when (step) {
                        0 -> {
                            WizardFeatureCard(
                                title = if (isDefaultHome) {
                                    "Default launcher ready"
                                } else {
                                    "Make GoreeCloud Launcher your Home"
                                },
                                summary = if (isDefaultHome) {
                                    "Android already sends the Home action here."
                                } else {
                                    "Grant the Android Home role so Launcher can fully replace your current Home."
                                },
                                symbol = WizardVisualSymbol.HOME,
                                accent = MaterialTheme.colorScheme.primary,
                            )
                            if (!isDefaultHome) {
                                Button(
                                    onClick = onRequestHomeRole,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("Set as default launcher")
                                }
                            }
                            WizardFeatureCard(
                                title = "Private by default",
                                summary = "Local usage ranking stays on-device; no Android Usage Access or behavioral tracking.",
                                symbol = WizardVisualSymbol.PRIVACY,
                                accent = MaterialTheme.colorScheme.tertiary,
                            )
                        }

                        1 -> {
                            WizardFeatureCard(
                                title = "Clean starter Home",
                                summary = "No built-in widgets are placed automatically. Add only the cards and widgets you want.",
                                symbol = WizardVisualSymbol.WIDGETS,
                                accent = MaterialTheme.colorScheme.primary,
                            )

                            WizardCompactChoiceStrip(
                                title = "Home apps",
                                options = listOf("None", "Recent", "Most used"),
                                selected = selectedHomeAppLabel,
                                onSelect = { selected ->
                                    homeAppModeName = when (selected) {
                                        "Recent" -> LauncherHomeAppMode.RECENT.name
                                        "Most used" -> LauncherHomeAppMode.MOST_USED.name
                                        else -> LauncherHomeAppMode.NONE.name
                                    }
                                },
                            )

                            WizardSectionTitle("Grid and Dock")
                            WizardCompactChoiceStrip(
                                title = "Grid",
                                options = listOf("4 x 5", "5 x 6", "6 x 7"),
                                selected = gridName,
                                onSelect = { gridName = it },
                            )
                            WizardCompactChoiceStrip(
                                title = "Dock",
                                options = listOf("4", "5", "6"),
                                selected = dockSize.toString(),
                                onSelect = { dockSize = it.toInt() },
                            )
                            WizardSwitchRow(
                                title = "Show Home labels",
                                summary = "Show names beneath Home icons.",
                                checked = showHomeLabels,
                                onCheckedChange = { showHomeLabels = it },
                            )
                            WizardSwitchRow(
                                title = "Add new apps to Home",
                                summary = "Place newly installed apps on Home.",
                                checked = addNewAppsToHome,
                                onCheckedChange = { addNewAppsToHome = it },
                            )
                            WizardSectionTitle("App Drawer (optional)")
                            WizardSwitchRow(
                                title = "Enable App Drawer Tabs",
                                summary = "Off by default. Add custom tabs beside the Apps heading.",
                                checked = enableDrawerTabs,
                                onCheckedChange = { enableDrawerTabs = it },
                            )
                        }

                        else -> {
                            WizardSectionTitle("Universal Search")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                            ) {
                                WizardSearchModeCard(
                                    title = "Swipe down",
                                    summary = "Keep Home minimal",
                                    selected =
                                        selectedSearchMode ==
                                            LauncherUniversalSearchHomeMode.SWIPE_DOWN_ONLY,
                                    showSearchBar = false,
                                    onClick = {
                                        searchModeName =
                                            LauncherUniversalSearchHomeMode.SWIPE_DOWN_ONLY.name
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                                WizardSearchModeCard(
                                    title = "Search bar",
                                    summary = "Keep Search visible",
                                    selected =
                                        selectedSearchMode ==
                                            LauncherUniversalSearchHomeMode.PERMANENT,
                                    showSearchBar = true,
                                    onClick = {
                                        searchModeName =
                                            LauncherUniversalSearchHomeMode.PERMANENT.name
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }

                            WizardSectionTitle("Everyday controls")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                            ) {
                                WizardMiniFeatureCard(
                                    title = "Apps",
                                    summary = "Hold + drag to place",
                                    symbol = WizardVisualSymbol.APPS,
                                    accent = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f),
                                )
                                WizardMiniFeatureCard(
                                    title = "Widgets",
                                    summary = "Hold + drag to move",
                                    symbol = WizardVisualSymbol.WIDGETS,
                                    accent = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                            ) {
                                WizardMiniFeatureCard(
                                    title = "Folders",
                                    summary = "Add and move in place",
                                    symbol = WizardVisualSymbol.FOLDER,
                                    accent = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.weight(1f),
                                )
                                WizardMiniFeatureCard(
                                    title = "App Lock",
                                    summary = "Protect selected app launches",
                                    symbol = WizardVisualSymbol.LOCK,
                                    accent = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f),
                                )
                            }

                            WizardGestureStrip()

                            WizardSwitchRow(
                                title = "Show Launcher hints",
                                summary = "Show short, dismissible usage tips.",
                                checked = showHints,
                                onCheckedChange = { showHints = it },
                            )

                            TextButton(
                                onClick = { showDetails = !showDetails },
                                modifier = Modifier.align(Alignment.End),
                            ) {
                                Text(if (showDetails) "Hide details" else "Learn more")
                            }
                            if (showDetails) {
                                WizardInfoCard(
                                    title = "Exact placement",
                                    summary = "Hold an app in Apps and drag it to an exact Home cell or Dock position. Hold a saved Home app at a page edge to switch pages before release.",
                                )
                                WizardInfoCard(
                                    title = "Widgets and folders",
                                    summary = "Widgets and folders move directly on Home. Edge drops can move them to an adjacent page when the destination is valid.",
                                )
                                WizardInfoCard(
                                    title = "Connected Search",
                                    summary = "Local Search stays local by default. Connected sources remain off until you explicitly enable and authorize them.",
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(GlazeMetrics.space1))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                    ) {
                        if (step > 0) {
                            OutlinedButton(
                                onClick = {
                                    val nextStep = (step - 1).coerceAtLeast(0)
                                    step = nextStep
                                    onStepChange(nextStep)
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Back")
                            }
                        }
                        Button(
                            onClick = {
                                if (step < 2) {
                                    val nextStep = step + 1
                                    step = nextStep
                                    onStepChange(nextStep)
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
                                            dockSize = dockSize,
                                            enableDrawerTabs = enableDrawerTabs,
                                        ),
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(if (step < 2) "Continue" else "Finish setup")
                        }
                    }
                }
            }
        }
    }
}

private enum class WizardVisualSymbol {
    HOME,
    PRIVACY,
    APPS,
    WIDGETS,
    FOLDER,
    SEARCH,
    LOCK,
    GESTURE,
    EDIT,
}

@Composable
private fun WizardProgress(
    step: Int,
    accent: Color,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Setup",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Step ${step + 1} of 3",
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            repeat(3) { index ->
                val reached = index <= step
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(
                        GlazeMetrics.radiusPill,
                    ),
                    color = if (reached) {
                        accent.copy(alpha = if (index == step) 1f else 0.34f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                ) {}
            }
        }
    }
}

@Composable
private fun WizardFeatureCard(
    title: String,
    summary: String,
    symbol: WizardVisualSymbol,
    accent: Color,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(GlazeMetrics.radiusLarge),
        color = accent.copy(alpha = 0.075f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.16f)),
    ) {
        Row(
            modifier = Modifier.padding(GlazeMetrics.space2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(
                    GlazeMetrics.radiusMedium,
                ),
                color = accent.copy(alpha = 0.13f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    WizardVisualGlyph(symbol = symbol, tint = accent)
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
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
private fun WizardMiniFeatureCard(
    title: String,
    summary: String,
    symbol: WizardVisualSymbol,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.heightIn(min = 76.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(GlazeMetrics.radiusLarge),
        color = accent.copy(alpha = 0.07f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.14f)),
    ) {
        Column(
            modifier = Modifier.padding(GlazeMetrics.space2),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            WizardVisualGlyph(symbol = symbol, tint = accent)
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                summary,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WizardSearchModeCard(
    title: String,
    summary: String,
    selected: Boolean,
    showSearchBar: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 120.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(GlazeMetrics.radiusLarge),
        color = if (selected) {
            accent.copy(alpha = 0.14f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f)
        },
        border = BorderStroke(
            1.dp,
            if (selected) accent else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(GlazeMetrics.space2),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            WizardSearchPreview(
                showSearchBar = showSearchBar,
                selected = selected,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                RadioButton(selected = selected, onClick = null)
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        summary,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun WizardSearchPreview(
    showSearchBar: Boolean,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    val foreground = MaterialTheme.colorScheme.onSurfaceVariant
    val surface = MaterialTheme.colorScheme.surface
    val backdrop = MaterialTheme.colorScheme.surfaceVariant
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(62.dp),
    ) {
        val w = size.width
        val h = size.height
        val stroke = 1.75.dp.toPx()
        val iconColor = if (selected) accent else foreground

        // Miniature Home canvas: one quiet surface, no nested "card in a card".
        drawRoundRect(
            color = backdrop.copy(alpha = 0.34f),
            topLeft = Offset(w * 0.035f, h * 0.03f),
            size = androidx.compose.ui.geometry.Size(w * 0.93f, h * 0.94f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.20f),
        )

        // Two rows of optically consistent app placeholders.
        repeat(6) { index ->
            val column = index % 3
            val row = index / 3
            val cx = w * (0.26f + column * 0.24f)
            val cy = h * (0.31f + row * 0.27f)
            drawRoundRect(
                color = foreground.copy(alpha = 0.22f),
                topLeft = Offset(cx - h * 0.075f, cy - h * 0.075f),
                size = androidx.compose.ui.geometry.Size(h * 0.15f, h * 0.15f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.045f),
            )
        }

        if (showSearchBar) {
            val pillLeft = w * 0.16f
            val pillTop = h * 0.70f
            val pillWidth = w * 0.68f
            val pillHeight = h * 0.20f
            drawRoundRect(
                color = surface.copy(alpha = 0.94f),
                topLeft = Offset(pillLeft, pillTop),
                size = androidx.compose.ui.geometry.Size(pillWidth, pillHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(pillHeight / 2f),
            )
            val center = Offset(pillLeft + pillHeight * 0.52f, pillTop + pillHeight * 0.47f)
            drawCircle(
                color = iconColor,
                radius = pillHeight * 0.20f,
                center = center,
                style = Stroke(stroke),
            )
            drawLine(
                color = iconColor,
                start = Offset(center.x + pillHeight * 0.14f, center.y + pillHeight * 0.14f),
                end = Offset(center.x + pillHeight * 0.28f, center.y + pillHeight * 0.28f),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            drawRoundRect(
                color = foreground.copy(alpha = 0.24f),
                topLeft = Offset(pillLeft + pillHeight * 0.95f, pillTop + pillHeight * 0.37f),
                size = androidx.compose.ui.geometry.Size(pillWidth * 0.42f, pillHeight * 0.18f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(pillHeight * 0.09f),
            )
        } else {
            // A touch ring + motion trail conveys the real swipe gesture without a crude arrow.
            val x = w * 0.50f
            drawCircle(
                color = iconColor.copy(alpha = 0.16f),
                radius = h * 0.105f,
                center = Offset(x, h * 0.19f),
            )
            drawCircle(
                color = iconColor,
                radius = h * 0.042f,
                center = Offset(x, h * 0.19f),
            )
            drawLine(
                color = iconColor,
                start = Offset(x, h * 0.29f),
                end = Offset(x, h * 0.63f),
                strokeWidth = stroke * 1.15f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = iconColor,
                start = Offset(x, h * 0.63f),
                end = Offset(x - h * 0.08f, h * 0.55f),
                strokeWidth = stroke * 1.15f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = iconColor,
                start = Offset(x, h * 0.63f),
                end = Offset(x + h * 0.08f, h * 0.55f),
                strokeWidth = stroke * 1.15f,
                cap = StrokeCap.Round,
            )
            drawRoundRect(
                color = surface.copy(alpha = 0.90f),
                topLeft = Offset(w * 0.28f, h * 0.72f),
                size = androidx.compose.ui.geometry.Size(w * 0.44f, h * 0.12f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.06f),
            )
        }
    }
}

@Composable
private fun WizardGestureStrip() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(GlazeMetrics.radiusLarge),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.74f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(
                Triple("Swipe up", "Apps", WizardVisualSymbol.APPS),
                Triple("Swipe down", "Search", WizardVisualSymbol.SEARCH),
                Triple("Hold", "Edit", WizardVisualSymbol.EDIT),
            ).forEach { (gesture, destination, symbol) ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .padding(horizontal = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    WizardVisualGlyph(
                        symbol = symbol,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                    ) {
                        Text(
                            gesture,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                        Text(
                            destination,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WizardVisualGlyph(
    symbol: WizardVisualSymbol,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val glyph = when (symbol) {
        WizardVisualSymbol.HOME -> LauncherOutlineGlyph.HOME
        WizardVisualSymbol.PRIVACY -> LauncherOutlineGlyph.SHIELD
        WizardVisualSymbol.APPS -> LauncherOutlineGlyph.APPS
        WizardVisualSymbol.WIDGETS -> LauncherOutlineGlyph.WIDGETS
        WizardVisualSymbol.FOLDER -> LauncherOutlineGlyph.FOLDER
        WizardVisualSymbol.SEARCH -> LauncherOutlineGlyph.SEARCH
        WizardVisualSymbol.LOCK -> LauncherOutlineGlyph.LOCK
        WizardVisualSymbol.GESTURE -> LauncherOutlineGlyph.GESTURE
        WizardVisualSymbol.EDIT -> LauncherOutlineGlyph.EDIT
    }
    LauncherOutlineGlyph(
        glyph = glyph,
        color = tint,
        modifier = modifier.size(22.dp),
    )
}

@Composable
fun LauncherHomeHintCard(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.widthIn(max = 560.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(GlazeMetrics.radiusExtraLarge),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f),
        ),
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(GlazeMetrics.space4),
            verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space3),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Launcher hints",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Core gestures and organization",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            WizardGestureStrip()
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space3),
            ) {
                WizardHintRow(
                    title = "Place precisely",
                    summary = "Hold an app, widget, or folder and drag it to a Home cell or Dock position.",
                    symbol = WizardVisualSymbol.EDIT,
                )
                WizardHintRow(
                    title = "Move across pages",
                    summary = "Keep holding at a page edge to switch pages, then release on the target.",
                    symbol = WizardVisualSymbol.GESTURE,
                )
                WizardHintRow(
                    title = "Use Dock pages",
                    summary = "Add more favorites than fit in one row, then swipe the Dock independently of Home pages.",
                    symbol = WizardVisualSymbol.GESTURE,
                )
                WizardHintRow(
                    title = "Organize Apps",
                    summary = "Use pins, tabs, folders, and local Smart Folders to organize larger app libraries.",
                    symbol = WizardVisualSymbol.APPS,
                )
            }
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
private fun WizardHintRow(
    title: String,
    summary: String,
    symbol: WizardVisualSymbol,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                WizardVisualGlyph(
                    symbol = symbol,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
            modifier = Modifier.padding(GlazeMetrics.space2),
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
private fun WizardCompactChoiceStrip(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(GlazeMetrics.radiusLarge),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                title,
                modifier = Modifier.widthIn(min = 38.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            options.forEach { option ->
                val active = option == selected
                Surface(
                    onClick = { onSelect(option) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .semantics {
                            this.selected = active
                            role = Role.RadioButton
                        },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(
                        GlazeMetrics.radiusPill,
                    ),
                    color = if (active) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.68f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (active) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
                        },
                    ),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            option,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            color = if (active) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 1,
                        )
                    }
                }
            }
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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
        ) {
            RadioButton(selected = selected, onClick = null)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    summary,
                    style = MaterialTheme.typography.labelSmall,
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
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = if (title == "Enable App Drawer Tabs") {
                Modifier.testTag("launcher-wizard-drawer-tabs")
            } else {
                Modifier
            },
        )
    }
}
