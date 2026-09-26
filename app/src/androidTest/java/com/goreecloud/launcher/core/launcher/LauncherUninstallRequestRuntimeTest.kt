package com.goreecloud.launcher.core.launcher

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Safe Android runtime regression for the Launcher uninstall handoff.
 *
 * The test verifies capability/handler availability only. It never launches the uninstaller and
 * never removes a package.
 */
@RunWith(AndroidJUnit4::class)
class LauncherUninstallRequestRuntimeTest {
    @Test
    fun installedDevelopmentApkDeclaresDeleteRequestAndAndroidCanResolveUninstaller() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS,
        )
        assertTrue(
            Manifest.permission.REQUEST_DELETE_PACKAGES in
                packageInfo.requestedPermissions.orEmpty(),
        )

        val request = requireNotNull(
            LauncherUninstallRequestPolicy.create(context.packageName),
        )
        val intent = Intent(
            request.action,
            Uri.fromParts(request.uriScheme, request.packageName, null),
        ).putExtra(Intent.EXTRA_RETURN_RESULT, false)

        assertNotNull(
            context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY),
        )
    }
}
