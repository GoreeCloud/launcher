package com.goreecloud.launcher

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle
import java.util.Locale

/** Only the current user's launcher-visible applications are enumerated. */
data class LaunchItem(
    val id: String,
    val title: String,
    val packageName: String,
    val component: ComponentName,
    val user: UserHandle,
    val icon: Drawable?
)

fun matchesApp(query: String, title: String, packageName: String): Boolean {
    val q = query.trim()
    return q.isBlank() || title.contains(q, ignoreCase = true) ||
        packageName.contains(q, ignoreCase = true)
}

class LauncherCatalog(context: Context) {
    private val service = context.getSystemService(LauncherApps::class.java)

    fun discover(): List<LaunchItem> {
        val result = try { service.getActivityList(null, Process.myUserHandle()) }
        catch (_: RuntimeException) { emptyList<LauncherActivityInfo>() }
        return result.map { activity ->
            LaunchItem(
                id = activity.componentName.flattenToString(),
                title = activity.label.toString().ifBlank { activity.componentName.packageName },
                packageName = activity.componentName.packageName,
                component = activity.componentName,
                user = activity.user,
                icon = try { activity.getIcon(0) } catch (_: RuntimeException) { null }
            )
        }.distinctBy { it.id }.sortedWith(
            compareBy<LaunchItem> { it.title.lowercase(Locale.getDefault()) }.thenBy { it.id }
        )
    }

    fun open(item: LaunchItem): Boolean = try {
        service.startMainActivity(item.component, item.user, null, null)
        true
    } catch (_: RuntimeException) {
        false
    }
}
