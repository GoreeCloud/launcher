package com.goreecloud.launcher.ui

import android.content.pm.LauncherActivityInfo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.goreecloud.launcher.core.launcher.LauncherConnectedSearchProviderRegistry
import com.goreecloud.launcher.core.launcher.LauncherDirectApiAnswerClient
import com.goreecloud.launcher.core.launcher.LauncherDirectApiCatalog
import com.goreecloud.launcher.core.launcher.LauncherDirectApiKind
import com.goreecloud.launcher.core.launcher.LauncherDirectApiSource
import com.goreecloud.launcher.core.launcher.launcherDirectApiValidHttpsEndpoint
import com.goreecloud.launcher.ui.theme.GlazeMetrics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Explicit connection-management surface. API secrets never appear in the provider-preference
 * snapshot, saveable state, accessibility descriptions, result subtitles, or diagnostics.
 */
@Composable
internal fun LauncherDirectApiSourcesSettings(
    sources: List<LauncherDirectApiSource>,
    apps: List<LauncherActivityInfo>,
    onSave: (LauncherDirectApiSource) -> String?,
    onRemove: (String) -> String?,
    onResetAll: () -> String?,
    storageUnavailable: Boolean,
) {
    var editing by remember { mutableStateOf<LauncherDirectApiSource?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var confirmResetAll by remember { mutableStateOf(false) }
    val templates = remember { LauncherDirectApiCatalog.templates() }
    val options = templates.map { template ->
        sources.firstOrNull { it.id == template.id } ?: template
    } + sources.filter { it.id.startsWith("custom.") }

    Surface(
        modifier = Modifier.fillMaxWidth().testTag("launcher-direct-api-settings"),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.54f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(GlazeMetrics.radiusLarge),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(GlazeMetrics.space3),
            verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
        ) {
            Text(
                "Direct API answers",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Connect your own API keys and get answers here in Universal Search. " +
                    "No API is contacted as you type; only Ask sends your question. " +
                    "Provider charges and retention policies may apply.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            options.forEach { source ->
                val connected = when (source.kind) {
                    LauncherDirectApiKind.CUSTOM_CHAT,
                    LauncherDirectApiKind.CUSTOM_SEARCH ->
                        launcherDirectApiValidHttpsEndpoint(source.endpoint)
                    else -> source.secret.isNotBlank()
                }
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LauncherDirectApiSourceBrand(source = source, apps = apps)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            source.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            if (source.enabled && connected) "Ready · Ask to send"
                            else if (connected) "Configured · Off"
                            else "Not configured",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(
                        onClick = { editing = source },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) { Text(if (connected) "Edit" else "Connect") }
                    Switch(
                        checked = source.enabled && connected,
                        enabled = connected,
                        onCheckedChange = { checked ->
                            notice = onSave(source.copy(enabled = checked))
                        },
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space1),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    modifier = Modifier.heightIn(min = 48.dp),
                    onClick = {
                        editing = LauncherDirectApiSource(
                            id = LauncherDirectApiCatalog.nextCustomId(),
                            kind = LauncherDirectApiKind.CUSTOM_CHAT,
                            title = "Custom chat API",
                            endpoint = "https://",
                            model = "my-model",
                            secret = "",
                            enabled = false,
                        )
                    },
                ) { Text("Add chat API") }
                TextButton(
                    modifier = Modifier.heightIn(min = 48.dp),
                    onClick = {
                        editing = LauncherDirectApiSource(
                            id = LauncherDirectApiCatalog.nextCustomId(),
                            kind = LauncherDirectApiKind.CUSTOM_SEARCH,
                            title = "Custom search API",
                            endpoint = "https://",
                            model = "",
                            secret = "",
                            enabled = false,
                        )
                    },
                ) { Text("Add search API") }
            }
            if (sources.isNotEmpty() || storageUnavailable) {
                TextButton(
                    onClick = { confirmResetAll = true },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text("Delete all API connections") }
            }
            notice?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
    if (confirmResetAll) {
        AlertDialog(
            onDismissRequest = { confirmResetAll = false },
            title = { Text("Delete all API connections?") },
            text = {
                Text(
                    "This permanently removes the locally stored API keys and source settings " +
                        "from Launcher. It does not revoke keys at external providers."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    notice = onResetAll()
                    if (notice == null) confirmResetAll = false
                }) { Text("Delete connections") }
            },
            dismissButton = {
                TextButton(onClick = { confirmResetAll = false }) { Text("Cancel") }
            },
        )
    }
    editing?.let { source ->
        LauncherDirectApiSourceDialog(
            source = source,
            onDismiss = { editing = null },
            onSave = { updated ->
                val error = onSave(updated)
                if (error == null) editing = null
                error
            },
            onRemove = if (source.id.startsWith("custom.") &&
                sources.any { it.id == source.id }
            ) {
                {
                    val error = onRemove(source.id)
                    if (error == null) editing = null
                    error
                }
            } else null,
        )
    }
}


/**
 * Use the provider's actual installed Android app icon when available. Launcher never forges
 * third-party logos or presents fallback shapes as official provider artwork.
 */
@Composable
private fun LauncherDirectApiSourceBrand(
    source: LauncherDirectApiSource,
    apps: List<LauncherActivityInfo>,
) {
    val providerId = when (source.kind) {
        LauncherDirectApiKind.OPENAI ->
            LauncherConnectedSearchProviderRegistry.CHATGPT_PROVIDER_ID
        LauncherDirectApiKind.CLAUDE ->
            LauncherConnectedSearchProviderRegistry.CLAUDE_PROVIDER_ID
        LauncherDirectApiKind.GEMINI ->
            LauncherConnectedSearchProviderRegistry.GEMINI_PROVIDER_ID
        LauncherDirectApiKind.PERPLEXITY ->
            LauncherConnectedSearchProviderRegistry.PERPLEXITY_PROVIDER_ID
        LauncherDirectApiKind.CUSTOM_CHAT,
        LauncherDirectApiKind.CUSTOM_SEARCH -> ""
    }
    val packages = remember(providerId) {
        LauncherConnectedSearchProviderRegistry.iconPackageNamesFor(providerId)
    }
    val installedApp = remember(apps, packages) {
        apps.firstOrNull { app -> app.componentName.packageName in packages }
    }
    val bitmap = installedApp?.let { rememberLauncherAppIcon(it) }
    Surface(
        modifier = Modifier.size(40.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(
            GlazeMetrics.radiusMedium,
        ),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.62f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(28.dp).launcherIconMask(),
                )
            } else {
                Text(
                    "API",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun LauncherDirectApiSourceDialog(
    source: LauncherDirectApiSource,
    onDismiss: () -> Unit,
    onSave: (LauncherDirectApiSource) -> String?,
    onRemove: (() -> String?)?,
) {
    var title by remember(source.id) { mutableStateOf(source.title) }
    var endpoint by remember(source.id) { mutableStateOf(source.endpoint) }
    var model by remember(source.id) { mutableStateOf(source.model) }
    var authHeader by remember(source.id) { mutableStateOf(source.authHeader) }
    var searchParameter by remember(source.id) { mutableStateOf(source.searchParameter) }
    var responsePath by remember(source.id) { mutableStateOf(source.responsePath) }
    var keyEntry by remember(source.id) { mutableStateOf("") }
    var enabled by remember(source.id) { mutableStateOf(source.enabled) }
    var error by remember(source.id) { mutableStateOf<String?>(null) }
    val custom = source.kind in setOf(
        LauncherDirectApiKind.CUSTOM_CHAT,
        LauncherDirectApiKind.CUSTOM_SEARCH,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure " + source.title) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 430.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
            ) {
                if (custom) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it.take(60) },
                        label = { Text("Source name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = endpoint,
                        onValueChange = { endpoint = it.take(1000) },
                        label = { Text("HTTPS API endpoint") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Text(
                        "Official API endpoint: " + endpoint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (source.kind != LauncherDirectApiKind.CUSTOM_SEARCH) {
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it.take(120) },
                        label = { Text("Model ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (custom) {
                    OutlinedTextField(
                        value = authHeader,
                        onValueChange = { authHeader = it.take(64) },
                        label = { Text("Authentication header") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (source.kind == LauncherDirectApiKind.CUSTOM_SEARCH) {
                    OutlinedTextField(
                        value = searchParameter,
                        onValueChange = { searchParameter = it.take(64) },
                        label = { Text("GET query parameter") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = responsePath,
                        onValueChange = { responsePath = it.take(160) },
                        label = { Text("JSON answer path (for example results)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutlinedTextField(
                    value = keyEntry,
                    onValueChange = { keyEntry = it.take(8192) },
                    label = {
                        Text(
                            if (source.secret.isNotBlank()) "Replace API key (leave blank to keep)"
                            else "API key" +
                                if (source.kind == LauncherDirectApiKind.CUSTOM_SEARCH) " (optional)" else ""
                        )
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("launcher-direct-api-key"),
                )
                if (source.secret.isNotBlank()) {
                    Text(
                        "An API key is stored securely on this device. Its value cannot be displayed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Enabled", modifier = Modifier.weight(1f))
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
                Text(
                    "Questions are sent only when you tap Ask. Replies stay in Search until " +
                        "you change or close it. Provider usage may be billed separately. " +
                        "Local encrypted keys are never included in Launcher backups.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (onRemove != null) {
                    TextButton(onClick = {
                        error = onRemove()
                    }) { Text("Delete connection") }
                }
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val effectiveSecret = keyEntry.trim().ifEmpty { source.secret }
                if (enabled && effectiveSecret.isEmpty() &&
                    source.kind in setOf(
                        LauncherDirectApiKind.OPENAI,
                        LauncherDirectApiKind.CLAUDE,
                        LauncherDirectApiKind.GEMINI,
                        LauncherDirectApiKind.PERPLEXITY,
                    )
                ) {
                    error = "Enter an API key to enable this source"
                } else {
                    error = onSave(source.copy(
                        title = title.trim(),
                        endpoint = endpoint.trim(),
                        model = model.trim(),
                        secret = effectiveSecret,
                        enabled = enabled,
                        authHeader = authHeader.trim(),
                        searchParameter = searchParameter.trim(),
                        responsePath = responsePath.trim(),
                    ))
                    if (error == null) keyEntry = ""
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Direct answers are not automatically dispatched by Search text changes. */
@Composable
internal fun LauncherDirectApiAnswerPanel(
    query: String,
    sources: List<LauncherDirectApiSource>,
) {
    val readySources = sources.filter { source ->
        source.enabled &&
            (
                source.secret.isNotBlank() ||
                    source.kind in setOf(
                        LauncherDirectApiKind.CUSTOM_CHAT,
                        LauncherDirectApiKind.CUSTOM_SEARCH,
                    )
                )
    }
    if (readySources.isEmpty()) return

    val scope = rememberCoroutineScope()
    val client = remember { LauncherDirectApiAnswerClient() }
    var job by remember { mutableStateOf<Job?>(null) }
    var requestVersion by remember { mutableStateOf(0) }
    var selected by remember { mutableStateOf<LauncherDirectApiSource?>(null) }
    var answer by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    LaunchedEffect(query) {
        job?.cancel()
        requestVersion += 1
        selected = null
        answer = null
        error = null
        loading = false
    }
    DisposableEffect(Unit) {
        onDispose { job?.cancel() }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("launcher-direct-api-answer-panel"),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(GlazeMetrics.radiusLarge),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(GlazeMetrics.space2),
            verticalArrangement = Arrangement.spacedBy(GlazeMetrics.space1),
        ) {
            Text(
                "Ask with your API",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Only the selected provider receives your question after you tap Ask. " +
                    "API usage can incur charges.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            readySources.forEach { source ->
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                ) {
                    Text(
                        source.title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    TextButton(
                        modifier = Modifier.heightIn(min = 48.dp),
                        enabled = !loading,
                        onClick = {
                            job?.cancel()
                            requestVersion += 1
                            val request = requestVersion
                            selected = source
                            loading = true
                            answer = null
                            error = null
                            job = scope.launch {
                                try {
                                    val text = client.answer(source, query)
                                    if (request == requestVersion) answer = text
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (_: Exception) {
                                    if (request == requestVersion) {
                                        error = "Could not get an answer. Check the connection, " +
                                            "API key, model, and provider usage limits."
                                    }
                                } finally {
                                    if (request == requestVersion) loading = false
                                }
                            }
                        },
                    ) { Text(if (loading && selected?.id == source.id) "Asking…" else "Ask") }
                }
            }
            if (loading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(GlazeMetrics.space2),
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                    Text("Waiting for " + (selected?.title ?: "API") + "…")
                    TextButton(
                        onClick = {
                            job?.cancel()
                            requestVersion += 1
                            loading = false
                            selected = null
                            answer = null
                            error = null
                        },
                    ) { Text("Cancel") }
                }
            }
            answer?.let {
                Text(selected?.title ?: "Direct API", style = MaterialTheme.typography.labelLarge)
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .heightIn(max = 230.dp)
                        .verticalScroll(rememberScrollState())
                        .testTag("launcher-direct-api-answer"),
                )
            }
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
