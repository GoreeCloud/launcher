package com.goreecloud.launcher.core.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import java.util.concurrent.ConcurrentHashMap
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

data class LauncherIconPackDescriptor(
    val packageName: String,
    val label: String,
)

internal object LauncherIconPackSafety {
    fun <T> failSoft(block: () -> T?): T? =
        try {
            block()
        } catch (_: Exception) {
            null
        }
}

class LauncherIconPackRepository(context: Context) {
    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager

    fun discover(): List<LauncherIconPackDescriptor> =
        ICON_PACK_ACTIONS
            .flatMap { action ->
                packageManager.queryIntentActivities(Intent(action), 0)
            }
            .mapNotNull { info ->
                val packageName = info.activityInfo?.packageName ?: return@mapNotNull null
                LauncherIconPackDescriptor(
                    packageName = packageName,
                    label = runCatching { info.loadLabel(packageManager).toString() }
                        .getOrDefault(packageName),
                )
            }
            .distinctBy { it.packageName }
            .sortedWith(
                compareBy<LauncherIconPackDescriptor> { it.label.lowercase(java.util.Locale.ROOT) }
                    .thenBy { it.packageName },
            )

    fun loadIcon(
        iconPackPackage: String,
        componentName: ComponentName,
        sizePx: Int = LAUNCHER_ICON_DECODE_SIZE_PX,
    ): Bitmap? {
        if (iconPackPackage.isBlank() || sizePx <= 0) return null
        val resources = runCatching {
            packageManager.getResourcesForApplication(iconPackPackage)
        }.getOrNull() ?: return null
        val mapping = mappingFor(iconPackPackage, resources)
        val drawableName = componentKeys(componentName)
            .firstNotNullOfOrNull { key -> mapping[key] }
            ?: return null
        val drawableId = resources.getIdentifier(drawableName, "drawable", iconPackPackage)
            .takeIf { it != 0 }
            ?: resources.getIdentifier(drawableName, "mipmap", iconPackPackage)
                .takeIf { it != 0 }
            ?: return null
        return LauncherIconPackSafety.failSoft {
            val drawable = ResourcesCompat.getDrawable(resources, drawableId, null)
                ?: return@failSoft null
            drawable.toBitmap(sizePx, sizePx)
        }
    }

    private fun mappingFor(
        packageName: String,
        resources: Resources,
    ): Map<String, String> {
        val cacheKey = packageName + ":" + packageVersionToken(packageName)
        return mappingCache.getOrPut(cacheKey) {
            loadResourceMapping(resources, packageName)
                .ifEmpty { loadAssetMapping(resources) }
        }
    }

    private fun loadResourceMapping(
        resources: Resources,
        packageName: String,
    ): Map<String, String> {
        val xmlId = resources.getIdentifier("appfilter", "xml", packageName)
        if (xmlId == 0) return emptyMap()
        return runCatching {
            val parser = resources.getXml(xmlId)
            try {
                parseAppFilter(parser)
            } finally {
                parser.close()
            }
        }.getOrDefault(emptyMap())
    }

    private fun loadAssetMapping(resources: Resources): Map<String, String> =
        runCatching {
            resources.assets.open("appfilter.xml").use { stream ->
                val parser = XmlPullParserFactory.newInstance().newPullParser().apply {
                    setInput(stream, "UTF-8")
                }
                parseAppFilter(parser)
            }
        }.getOrDefault(emptyMap())

    private fun parseAppFilter(parser: XmlPullParser): Map<String, String> {
        val mapping = linkedMapOf<String, String>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name.equals("item", ignoreCase = true)) {
                val rawComponent = parser.getAttributeValue(null, "component")
                val drawable = parser.getAttributeValue(null, "drawable")
                val normalized = normalizeComponent(rawComponent)
                if (!normalized.isNullOrBlank() && !drawable.isNullOrBlank()) {
                    mapping[normalized] = drawable
                }
            }
            event = parser.next()
        }
        return mapping
    }

    private fun normalizeComponent(raw: String?): String? {
        val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val unwrapped = value
            .removePrefix("ComponentInfo{")
            .removeSuffix("}")
        val component = ComponentName.unflattenFromString(unwrapped)
        return component?.flattenToString() ?: unwrapped
    }

    private fun componentKeys(componentName: ComponentName): List<String> = listOf(
        componentName.flattenToString(),
        componentName.flattenToShortString(),
        componentName.packageName + "/" + componentName.className,
    )

    @Suppress("DEPRECATION")
    private fun packageVersionToken(packageName: String): String =
        runCatching {
            val info = packageManager.getPackageInfo(packageName, 0)
            info.versionCode.toString()
        }.getOrDefault("unknown")

    companion object {
        const val GOREECLOUD_ICON_PACK_ACTION = "com.goreecloud.launcher.ICON_PACK"

        val ICON_PACK_ACTIONS: List<String> = listOf(
            GOREECLOUD_ICON_PACK_ACTION,
            "org.adw.launcher.THEMES",
            "com.novalauncher.THEME",
            "com.anddoes.launcher.THEME",
            "com.gau.go.launcherex.theme",
        )

        private val mappingCache = ConcurrentHashMap<String, Map<String, String>>()
    }
}
