package com.goreecloud.launcher.core.launcher

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unprivileged Android 16 regression: a local preference must never grant Android's separate
 * notification-listener permission or expose counts without that OS grant. Real grant/revocation
 * and cross-profile behavior still require representative physical-device acceptance.
 */
@RunWith(AndroidJUnit4::class)
class LauncherNotificationBadgePrivacyRuntimeTest {
    @Test
    fun disabledByDefaultAndLocalOptInCannotBypassAndroidNotificationAccess() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = context.getSharedPreferences(
            "goreecloud_launcher_badges", Context.MODE_PRIVATE,
        )
        val previouslySet = preferences.contains("enabled")
        val previouslyEnabled = preferences.getBoolean("enabled", false)
        try {
            assertTrue(preferences.edit().remove("enabled").commit())
            LauncherNotificationBadges.initialize(context)
            assertFalse(LauncherNotificationBadges.enabled.value)
            assertTrue(LauncherNotificationBadges.counts.value.isEmpty())

            // The managed emulator must not have a preauthorized listener. Never revoke or
            // grant an actual user's OS permission just to run this test.
            assumeFalse(
                NotificationManagerCompat.getEnabledListenerPackages(context)
                    .contains(context.packageName),
            )
            LauncherNotificationBadges.setEnabled(context, true)
            assertTrue(LauncherNotificationBadges.enabled.value)
            assertFalse(LauncherNotificationBadges.accessGranted.value)
            assertTrue(LauncherNotificationBadges.counts.value.isEmpty())

            LauncherNotificationBadges.setEnabled(context, false)
            assertFalse(LauncherNotificationBadges.enabled.value)
            assertFalse(LauncherNotificationBadges.accessGranted.value)
            assertTrue(LauncherNotificationBadges.counts.value.isEmpty())
        } finally {
            val restore = preferences.edit()
            if (previouslySet) restore.putBoolean("enabled", previouslyEnabled)
            else restore.remove("enabled")
            assertTrue(restore.commit())
            LauncherNotificationBadges.initialize(context)
        }
    }
}
