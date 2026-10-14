/*
 * GoreeCloud Launcher - product About page.
 * Derived from Lawnchair under Apache-2.0; notices remain in Acknowledgements.
 */
package app.lawnchair.ui.preferences.about

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import app.lawnchair.ui.preferences.LocalIsExpandedScreen
import app.lawnchair.ui.preferences.components.NavigationActionPreference
import app.lawnchair.ui.preferences.components.controls.ClickablePreference
import app.lawnchair.ui.preferences.components.layout.PreferenceGroupHeading
import app.lawnchair.ui.preferences.components.layout.PreferenceGroupItem
import app.lawnchair.ui.preferences.components.layout.PreferenceLayoutLazyColumn
import app.lawnchair.ui.preferences.navigation.AboutLicenses
import com.android.launcher3.BuildConfig
import com.android.launcher3.R

/** Product identity only. Upstream license and attribution remain accessible. */
@Composable
fun About(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    PreferenceLayoutLazyColumn(
        label = "About GoreeCloud Launcher",
        modifier = modifier,
        backArrowVisible = !LocalIsExpandedScreen.current,
    ) {
        item { Spacer(Modifier.height(16.dp)) }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Image(
                    painter = painterResource(id = R.drawable.goreecloud_launcher_foreground),
                    contentDescription = "GoreeCloud Launcher icon",
                    modifier = Modifier.size(88.dp),
                )
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
        item {
            Text(
                text = "GoreeCloud Launcher",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Text(
                text = BuildConfig.VERSION_DISPLAY_NAME,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Text(
                text = if (BuildConfig.DEBUG) "Development / QA" else "Release",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 20.dp),
            )
        }
        item { PreferenceGroupHeading("GoreeCloud") }
        item {
            PreferenceGroupItem(cutTop = false, cutBottom = false) {
                ClickablePreference(
                    label = "Source code",
                    subtitle = "GoreeCloud/launcher",
                    onClick = {
                        val link = Intent(Intent.ACTION_VIEW, "https://github.com/GoreeCloud/launcher".toUri())
                        if (link.resolveActivity(context.packageManager) != null) context.startActivity(link)
                    },
                )
            }
        }
        item { PreferenceGroupHeading("Legal & acknowledgements") }
        item {
            PreferenceGroupItem(cutTop = false, cutBottom = true) {
                NavigationActionPreference(
                    label = "Open-source licenses and upstream acknowledgements",
                    destination = AboutLicenses,
                )
            }
        }
    }
}
