package com.goreecloud.launcher.core.launcher

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The owner must see Drive even without an exported app search Activity. No registered third-party
 * provider receives typed input automatically, including when the owner opted into its handoff.
 */
@RunWith(AndroidJUnit4::class)
class LauncherConnectedSearchPrivacyRuntimeTest {
    @Test
    fun driveIsDiscoverableYetDisabledByDefaultAndCannotAutoSearch() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val registrations = LauncherConnectedSearchProviderRegistry.registrations(context)
        val driveId = LauncherConnectedSearchProviderRegistry.GOOGLE_DRIVE_PROVIDER_ID
        assertTrue(registrations.any { it.metadata.providerId == driveId })

        val catalog = LauncherSearchProviderContract.evaluate(registrations)
        val defaults = LauncherSearchProviderUserControlPolicy.normalize(
            catalog = catalog,
            requestedEnabledProviderIds = null,
            requestedProviderOrder = emptyList(),
        )
        assertTrue(defaults.orderedOptions.any { it.providerId == driveId })
        assertFalse(defaults.isEnabled(driveId))
        assertTrue(LauncherSearchProviderUserControlPolicy.automaticProviders(catalog, defaults).isEmpty())

        val optedIn = LauncherSearchProviderUserControlPolicy.normalize(
            catalog = catalog,
            requestedEnabledProviderIds = setOf(driveId),
            requestedProviderOrder = listOf(driveId),
        )
        assertTrue(optedIn.isEnabled(driveId))
        assertTrue(LauncherSearchProviderUserControlPolicy.automaticProviders(catalog, optedIn).isEmpty())
    }

    @Test
    fun explicitDriveHandoffEncodesQueryButRejectsBlankInput() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val driveId = LauncherConnectedSearchProviderRegistry.GOOGLE_DRIVE_PROVIDER_ID
        assertNull(
            LauncherConnectedSearchProviderRegistry.buildExplicitHandoffIntent(
                context, driveId, "   ",
            ),
        )

        val intent = LauncherConnectedSearchProviderRegistry.buildExplicitHandoffIntent(
            context, driveId, "camera receipts",
        )
        assertNotNull(intent)
        assertEquals(Intent.ACTION_VIEW, intent!!.action)
        assertEquals("https", intent.data!!.scheme)
        assertEquals("drive.google.com", intent.data!!.host)
        assertEquals("camera receipts", intent.data!!.getQueryParameter("q"))
    }
}
