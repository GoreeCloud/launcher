package com.goreecloud.launcher.core.launcher

import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherAppIconRuntimeTest {
    @Test
    fun repositorySnapshotMatchesVisibleLauncherActivitiesAcrossProfiles() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val launcherApps = context.getSystemService(LauncherApps::class.java)
        val expected = visibleLauncherActivities(launcherApps)
            .map(::inventoryKey)
            .sorted()
        val actual = withTimeout(15_000) {
            LauncherAppsRepository(context).apps.first()
        }.map(::inventoryKey).sorted()

        assertEquals(
            "Drawer inventory must match every Android-visible launcher activity across profiles.",
            expected,
            actual,
        )
    }

    @Test
    fun everyVisibleLauncherActivityResolvesArtworkThroughSharedCache() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val activities = visibleLauncherActivities(context.getSystemService(LauncherApps::class.java))
        assertTrue(activities.isNotEmpty())

        LauncherAppIconCache.clear()
        try {
            val unresolved = activities.filter { app ->
                LauncherAppIconCache.load(app, context.packageManager) == null
            }
            assertTrue(
                "Missing artwork for: " + unresolved.joinToString { it.componentName.flattenToShortString() },
                unresolved.isEmpty(),
            )
        } finally {
            LauncherAppIconCache.clear()
        }
    }

    @Test
    fun everyVisibleLauncherActivityReloadsArtworkAfterPackageInvalidation() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val activities = visibleLauncherActivities(context.getSystemService(LauncherApps::class.java))
        assertTrue(activities.isNotEmpty())

        LauncherAppIconCache.clear()
        try {
            val unresolved = activities.filter { app ->
                val initial = LauncherAppIconCache.load(app, context.packageManager)
                LauncherAppIconCache.invalidatePackage(
                    packageName = app.componentName.packageName,
                    user = app.user,
                )
                val refreshed = LauncherAppIconCache.load(app, context.packageManager)
                initial == null || refreshed == null
            }
            assertTrue(
                "Artwork did not survive invalidation for: " +
                    unresolved.joinToString { it.componentName.flattenToShortString() },
                unresolved.isEmpty(),
            )
        } finally {
            LauncherAppIconCache.clear()
        }
    }

    private fun visibleLauncherActivities(launcherApps: LauncherApps): List<LauncherActivityInfo> =
        launcherApps.profiles
            .flatMap { profile -> launcherApps.getActivityList(null, profile) }
            .distinctBy(::inventoryKey)

    private fun inventoryKey(app: LauncherActivityInfo): String =
        app.user.hashCode().toString() + ":" + app.componentName.flattenToString()
}
