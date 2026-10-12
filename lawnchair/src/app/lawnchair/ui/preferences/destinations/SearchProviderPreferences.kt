package app.lawnchair.ui.preferences.destinations

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.lawnchair.preferences.getAdapter
import app.lawnchair.preferences2.preferenceManager2
import app.lawnchair.qsb.providers.QsbSearchProvider
import app.lawnchair.qsb.providers.QsbSearchProviderType
import app.lawnchair.ui.preferences.components.layout.ClickableIcon
import app.lawnchair.ui.preferences.components.layout.DividerColumn
import app.lawnchair.ui.preferences.components.layout.ExpandAndShrink
import app.lawnchair.ui.preferences.components.layout.PreferenceDivider
import app.lawnchair.ui.preferences.components.layout.PreferenceGroup
import app.lawnchair.ui.preferences.components.layout.PreferenceLayout
import app.lawnchair.ui.preferences.components.layout.PreferenceTemplate
import com.android.launcher3.R

@Composable
fun SearchProviderPreferences(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val adapter = preferenceManager2().hotseatQsbProvider.getAdapter()
    val forceWebsiteAdapter = preferenceManager2().hotseatQsbForceWebsite.getAdapter()

    PreferenceLayout(
        label = stringResource(R.string.search_provider),
        modifier = modifier,
    ) {
        PreferenceGroup(
            itemSpacing = 0.dp,
        ) {
            QsbSearchProvider.values().forEach { qsbSearchProvider ->
                val appInstalled = qsbSearchProvider.isDownloaded(context)
                val selected = adapter.state.value == qsbSearchProvider
                val hasAppAndWebsite = qsbSearchProvider.type == QsbSearchProviderType.APP_AND_WEBSITE
                val showDownloadButton = qsbSearchProvider.type == QsbSearchProviderType.APP && !appInstalled
                Column {
                    val title = stringResource(id = qsbSearchProvider.name)
                    ListItem(
                        title = title,
                        showDownloadButton = showDownloadButton,
                        enabled = qsbSearchProvider.type != QsbSearchProviderType.APP || appInstalled,
                        selected = selected,
                        onClick = { adapter.onChange(newValue = qsbSearchProvider) },
                        onDownloadClick = { qsbSearchProvider.launchOnAppMarket(context = context) },
                        description = if (showDownloadButton) {
                            stringResource(id = R.string.qsb_search_provider_app_required)
                        } else {
                            null
                        },
                    )
                    ExpandAndShrink(visible = selected && hasAppAndWebsite) {
                        Options(
                            appEnabled = appInstalled,
                            appSelected = !forceWebsiteAdapter.state.value && appInstalled,
                            onAppClick = { forceWebsiteAdapter.onChange(newValue = false) },
                            onAppDownloadClick = { qsbSearchProvider.launchOnAppMarket(context = context) },
                            onWebsiteClick = { forceWebsiteAdapter.onChange(newValue = true) },
                            showAppDownloadButton = !appInstalled,
                        )
                    }
                    Spacer(Modifier.height(ListItemDefaults.SegmentedGap))
                }
            }
        }
    }
}

@Composable
private fun ListItem(
    title: String,
    description: String?,
    showDownloadButton: Boolean,
    enabled: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        PreferenceTemplate(
            title = { Text(text = title) },
            enabled = enabled,
            description = if (description != null) {
                { Text(text = description) }
            } else {
                null
            },
            startWidget = {
                RadioButton(
                    selected = selected,
                    onClick = null,
                    enabled = enabled,
                    modifier = Modifier.padding(start = 16.dp),
                )
            },
            endWidget = {
                Row {
                    if (showDownloadButton) {
                        ClickableIcon(
                            painter = painterResource(id = R.drawable.ic_download),
                            onClick = onDownloadClick,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                }
            },
            onClick = if (enabled) onClick else null,
        )
    }
}

@Composable
private fun Options(
    appEnabled: Boolean,
    appSelected: Boolean,
    showAppDownloadButton: Boolean,
    onAppClick: () -> Unit,
    onAppDownloadClick: () -> Unit,
    onWebsiteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PreferenceDivider(startIndent = 40.dp)
    DividerColumn(
        modifier = modifier,
        startIndent = 40.dp,
    ) {
        PreferenceTemplate(
            title = { Text(stringResource(id = R.string.app_label)) },
            enabled = appEnabled,
            startWidget = {
                RadioButton(
                    selected = appSelected,
                    onClick = null,
                    enabled = appEnabled,
                    modifier = Modifier.padding(start = 56.dp),
                )
            },
            endWidget = {
                if (showAppDownloadButton) {
                    ClickableIcon(
                        painter = painterResource(R.drawable.ic_download),
                        onClick = onAppDownloadClick,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                }
            },
            onClick = if (appEnabled) onAppClick else null,
        )
        PreferenceTemplate(
            title = { Text(text = stringResource(id = R.string.website_label)) },
            startWidget = {
                RadioButton(
                    selected = !appSelected,
                    onClick = null,
                    modifier = Modifier.padding(start = 56.dp),
                )
            },
            onClick = onWebsiteClick,
        )
    }
}
