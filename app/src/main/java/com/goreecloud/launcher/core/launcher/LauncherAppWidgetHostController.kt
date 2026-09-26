package com.goreecloud.launcher.core.launcher

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context

data class LauncherWidgetProviderDescriptor(
    val provider: ComponentName,
    val label: String,
    val packageName: String,
    val minWidth: Int,
    val minHeight: Int,
)

class LauncherAppWidgetHostController(context: Context) {
    private val appContext = context.applicationContext
    private val host = AppWidgetHost(appContext, HOST_ID)
    private val manager = AppWidgetManager.getInstance(appContext)

    fun startListening() {
        host.startListening()
    }

    fun stopListening() {
        host.stopListening()
    }

    fun allocateAppWidgetId(): Int = host.allocateAppWidgetId()

    fun deleteAppWidgetId(appWidgetId: Int) {
        if (appWidgetId > 0) {
            runCatching { host.deleteAppWidgetId(appWidgetId) }
        }
    }

    fun providerInfo(appWidgetId: Int): AppWidgetProviderInfo? =
        if (appWidgetId > 0) manager.getAppWidgetInfo(appWidgetId) else null

    fun installedProviders(): List<LauncherWidgetProviderDescriptor> =
        manager.installedProviders
            .map { info ->
                LauncherWidgetProviderDescriptor(
                    provider = info.provider,
                    label = runCatching {
                        info.loadLabel(appContext.packageManager).toString()
                    }.getOrDefault(info.provider.className.substringAfterLast('.')),
                    packageName = info.provider.packageName,
                    minWidth = info.minWidth.coerceAtLeast(0),
                    minHeight = info.minHeight.coerceAtLeast(0),
                )
            }
            .sortedWith(
                compareBy<LauncherWidgetProviderDescriptor> { it.label.lowercase(java.util.Locale.ROOT) }
                    .thenBy { it.packageName },
            )

    fun bindAppWidgetIdIfAllowed(appWidgetId: Int, provider: ComponentName): Boolean =
        appWidgetId > 0 && runCatching {
            manager.bindAppWidgetIdIfAllowed(appWidgetId, provider)
        }.getOrDefault(false)

    fun createHostView(appWidgetId: Int): AppWidgetHostView? {
        val info = providerInfo(appWidgetId) ?: return null
        return host.createView(appContext, appWidgetId, info)
    }

    companion object {
        // Stable within the GoreeCloud Launcher package; AppWidgetHost IDs are host-package scoped.
        private const val HOST_ID = 0x4743
    }
}
