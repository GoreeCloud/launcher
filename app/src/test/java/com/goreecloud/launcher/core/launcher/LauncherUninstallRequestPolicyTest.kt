package com.goreecloud.launcher.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LauncherUninstallRequestPolicyTest {
    @Test
    fun installedPackageUsesAndroidUserConfirmedUninstallAction() {
        assertEquals(
            LauncherUninstallRequest(
                action = "android.intent.action.UNINSTALL_PACKAGE",
                uriScheme = "package",
                packageName = "com.example.camera",
            ),
            LauncherUninstallRequestPolicy.create("com.example.camera"),
        )
    }

    @Test
    fun packageNameIsNormalizedWithoutChangingIdentity() {
        assertEquals(
            "com.example.notes",
            LauncherUninstallRequestPolicy.create("  com.example.notes  ")?.packageName,
        )
    }

    @Test
    fun blankPackageCannotProduceAnUninstallRequest() {
        assertNull(LauncherUninstallRequestPolicy.create("   "))
    }
}
