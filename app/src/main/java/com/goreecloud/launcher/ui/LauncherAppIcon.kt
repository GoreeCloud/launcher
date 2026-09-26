package com.goreecloud.launcher.ui

import android.content.pm.LauncherActivityInfo
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.goreecloud.launcher.core.launcher.LauncherAppIconCache
import com.goreecloud.launcher.core.launcher.LauncherIconPackRepository
import com.goreecloud.launcher.core.launcher.LauncherIconShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class LauncherIconAppearance(
    val shape: LauncherIconShape = LauncherIconShape.ROUNDED_SQUARE,
    val iconPackPackage: String? = null,
)

internal val LocalLauncherIconAppearance = compositionLocalOf {
    LauncherIconAppearance()
}

/**
 * Reads cached Android icon state synchronously and replaces it with the selected icon-pack asset
 * when one exists. Missing pack mappings fail soft to the original Android-provided icon.
 */
@Composable
internal fun rememberLauncherAppIcon(app: LauncherActivityInfo): ImageBitmap? {
    val appearance = LocalLauncherIconAppearance.current
    val appContext = LocalContext.current.applicationContext
    val cacheStamp = LauncherAppIconCache.stamp(app)
    var icon by remember(
        app.componentName,
        app.user,
        cacheStamp,
        appearance.iconPackPackage,
    ) {
        mutableStateOf(LauncherAppIconCache.peek(app)?.asImageBitmap())
    }

    LaunchedEffect(
        app.componentName,
        app.user,
        cacheStamp,
        appearance.iconPackPackage,
    ) {
        val iconPackPackage = appearance.iconPackPackage
        val themed = if (iconPackPackage.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                LauncherIconPackRepository(appContext)
                    .loadIcon(iconPackPackage, app.componentName)
            }
        }

        icon = themed?.asImageBitmap()
            ?: LauncherAppIconCache.load(app)?.asImageBitmap()
    }

    return icon
}

@Composable
internal fun Modifier.launcherIconMask(): Modifier {
    val shape = LocalLauncherIconAppearance.current.shape
    return if (shape == LauncherIconShape.ORIGINAL) {
        this
    } else {
        clip(shape.toLauncherComposeShape())
    }
}

internal fun LauncherIconShape.toLauncherComposeShape(): Shape = when (this) {
    LauncherIconShape.ROUNDED_SQUARE -> RoundedCornerShape(24)
    LauncherIconShape.ORIGINAL -> RoundedCornerShape(0)
    LauncherIconShape.SQUIRCLE -> RoundedCornerShape(38)
    LauncherIconShape.CIRCLE -> CircleShape
    LauncherIconShape.TEARDROP -> RoundedCornerShape(
        topStart = CornerSize(50),
        topEnd = CornerSize(50),
        bottomEnd = CornerSize(12),
        bottomStart = CornerSize(50),
    )
}
