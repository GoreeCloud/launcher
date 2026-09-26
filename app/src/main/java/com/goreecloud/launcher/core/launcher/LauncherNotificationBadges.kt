package com.goreecloud.launcher.core.launcher

import android.app.Notification
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Badge presentation is independent of notification access or reading notification content. */
enum class LauncherBadgeStyle { COUNT, DOT }
enum class LauncherBadgeSize { SMALL, MEDIUM, LARGE }
enum class LauncherBadgeCorner { TOP_START, TOP_END, BOTTOM_START, BOTTOM_END }

/** Only non-sensitive package/profile counts are kept in process memory; no notification content. */
data class LauncherBadgeAppKey(val packageName: String, val profile: android.os.UserHandle)

/** A content-free counting input: keep package/profile key and notification state only. */
internal data class LauncherBadgeObservation<Key>(
    val key: Key,
    val clearable: Boolean,
    val groupSummary: Boolean,
)

/** Shared, testable counting rule; Android notification content never enters this policy. */
internal object LauncherBadgeCountingPolicy {
    fun <Key> aggregate(observations: Iterable<LauncherBadgeObservation<Key>>): Map<Key, Int> =
        observations.filter { it.clearable && !it.groupSummary }
            .groupingBy { it.key }
            .eachCount()
}


/**
 * Android's notification-access grant remains an explicit system decision. Preference is opt-in
 * and disabled by default; declining permission or turning badges off clears every in-memory count.
 * No title, message, sender, image, or notification history is read, retained, or transmitted.
 */
object LauncherNotificationBadges {
    private const val STORE = "goreecloud_launcher_badges"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_STYLE = "style"
    private const val KEY_SIZE = "size"
    private const val KEY_CORNER = "corner"

    private val enabledState = MutableStateFlow(false)
    val enabled = enabledState.asStateFlow()
    private val styleState = MutableStateFlow(LauncherBadgeStyle.COUNT)
    val style = styleState.asStateFlow()
    private val sizeState = MutableStateFlow(LauncherBadgeSize.MEDIUM)
    val size = sizeState.asStateFlow()
    private val cornerState = MutableStateFlow(LauncherBadgeCorner.TOP_END)
    val corner = cornerState.asStateFlow()
    private val accessState = MutableStateFlow(false)
    val accessGranted = accessState.asStateFlow()
    private var refreshFromListener: (() -> Unit)? = null
    private val countState = MutableStateFlow<Map<LauncherBadgeAppKey, Int>>(emptyMap())
    val counts = countState.asStateFlow()

    fun initialize(context: Context) {
        val preferences = context.applicationContext.getSharedPreferences(
            STORE, Context.MODE_PRIVATE,
        )
        enabledState.value = preferences.getBoolean(KEY_ENABLED, false)
        styleState.value = LauncherBadgeStyle.entries.firstOrNull {
            it.name == preferences.getString(KEY_STYLE, LauncherBadgeStyle.COUNT.name)
        } ?: LauncherBadgeStyle.COUNT
        sizeState.value = LauncherBadgeSize.entries.firstOrNull {
            it.name == preferences.getString(KEY_SIZE, LauncherBadgeSize.MEDIUM.name)
        } ?: LauncherBadgeSize.MEDIUM
        cornerState.value = LauncherBadgeCorner.entries.firstOrNull {
            it.name == preferences.getString(KEY_CORNER, LauncherBadgeCorner.TOP_END.name)
        } ?: LauncherBadgeCorner.TOP_END
        refreshAccess(context)
    }

    fun setEnabled(context: Context, newEnabled: Boolean) {
        context.applicationContext.getSharedPreferences(STORE, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, newEnabled).apply()
        enabledState.value = newEnabled
        if (!newEnabled) countState.value = emptyMap()
        // refreshAccess already triggers one refresh when both gates are satisfied.
        refreshAccess(context)
    }

    fun setStyle(context: Context, newStyle: LauncherBadgeStyle) {
        context.applicationContext.getSharedPreferences(STORE, Context.MODE_PRIVATE)
            .edit().putString(KEY_STYLE, newStyle.name).apply()
        styleState.value = newStyle
    }

    fun setSize(context: Context, newSize: LauncherBadgeSize) {
        context.applicationContext.getSharedPreferences(STORE, Context.MODE_PRIVATE)
            .edit().putString(KEY_SIZE, newSize.name).apply()
        sizeState.value = newSize
    }

    fun setCorner(context: Context, newCorner: LauncherBadgeCorner) {
        context.applicationContext.getSharedPreferences(STORE, Context.MODE_PRIVATE)
            .edit().putString(KEY_CORNER, newCorner.name).apply()
        cornerState.value = newCorner
    }

    fun refreshAccess(context: Context) {
        accessState.value = try {
            context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)
        } catch (_: SecurityException) {
            false
        }
        if (!accessState.value || !enabledState.value) countState.value = emptyMap()
        else refreshFromListener?.invoke()
    }

    internal fun setActiveListener(refresh: (() -> Unit)?) {
        refreshFromListener = refresh
    }

    fun countFor(app: LauncherActivityInfo, visibleCounts: Map<LauncherBadgeAppKey, Int>): Int =
        visibleCounts[LauncherBadgeAppKey(app.componentName.packageName, app.user)] ?: 0

    internal fun acceptActiveNotifications(notifications: Array<StatusBarNotification>?) {
        if (!enabledState.value || !accessState.value) {
            countState.value = emptyMap()
            return
        }
        countState.value = LauncherBadgeCountingPolicy.aggregate(
            notifications.orEmpty().map { sbn ->
                LauncherBadgeObservation(
                    key = LauncherBadgeAppKey(sbn.packageName, sbn.user),
                    clearable = sbn.isClearable,
                    groupSummary = (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0,
                )
            },
        )
    }

    internal fun clear() { countState.value = emptyMap() }
}

/** User-granted access is used solely to render ephemeral notification counts on app icons. */
class LauncherNotificationBadgeListener : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        LauncherNotificationBadges.initialize(this)
        LauncherNotificationBadges.setActiveListener(::refresh)
        LauncherNotificationBadges.refreshAccess(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) { refresh() }
    override fun onNotificationRemoved(sbn: StatusBarNotification?) { refresh() }

    override fun onListenerDisconnected() {
        LauncherNotificationBadges.setActiveListener(null)
        LauncherNotificationBadges.clear()
        super.onListenerDisconnected()
    }

    private fun refresh() {
        // The OS may keep the listener bound after the owner turns Launcher badges off.
        // Do not retrieve the active notification array without BOTH current opt-in and OS grant.
        if (!LauncherNotificationBadges.enabled.value ||
            !LauncherNotificationBadges.accessGranted.value
        ) {
            LauncherNotificationBadges.clear()
            return
        }
        val active = try { activeNotifications } catch (_: SecurityException) { null }
        LauncherNotificationBadges.acceptActiveNotifications(active)
    }
}
