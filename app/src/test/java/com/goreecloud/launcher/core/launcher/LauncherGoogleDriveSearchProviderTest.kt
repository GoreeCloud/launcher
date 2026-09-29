package com.goreecloud.launcher.core.launcher

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherGoogleDriveSearchProviderTest {
    @Test
    fun unconfiguredProviderFailsClosedWithoutSendingQuery() = runBlocking {
        var calls = 0
        val provider = LauncherGoogleDriveSearchProvider(
            tokenProvider = { null },
            transport = LauncherGoogleDriveSearchTransport { _, _ ->
                calls += 1
                emptyList()
            },
        )

        val results = provider.searchAsync(LauncherSearchRequest("budget"))

        assertFalse(provider.isInlineExecutionReady)
        assertTrue(results.isEmpty())
        assertEquals(0, calls)
    }

    @Test
    fun authorizedProviderMapsDriveFilesIntoInlineConnectedResults() = runBlocking {
        val provider = LauncherGoogleDriveSearchProvider(
            tokenProvider = { "ephemeral-token" },
            transport = LauncherGoogleDriveSearchTransport { token, query ->
                assertEquals("ephemeral-token", token)
                assertEquals("launch plan", query)
                listOf(
                    LauncherGoogleDriveFileHit(
                        id = "file-1",
                        name = "Launch plan",
                        mimeType = "application/vnd.google-apps.document",
                        webViewLink = "https://drive.google.com/file/d/file-1/view",
                    ),
                    LauncherGoogleDriveFileHit(
                        id = "folder-1",
                        name = "Launch plans",
                        mimeType = "application/vnd.google-apps.folder",
                        webViewLink = null,
                    ),
                )
            },
        )

        val results = provider.searchAsync(LauncherSearchRequest("launch plan"))

        assertTrue(provider.isInlineExecutionReady)
        assertEquals(2, results.size)
        assertTrue(results.all { it.providerId == LauncherConnectedSearchProviderRegistry.GOOGLE_DRIVE_PROVIDER_ID })
        assertTrue(results.all { it.category == LauncherSearchCategory.CONNECTED_SOURCE })
        assertEquals("Launch plan", results[0].title)
        assertEquals("Google Docs document", results[0].subtitle)
        assertEquals(
            "https://drive.google.com/file/d/file-1/view",
            (results[0].action as LauncherOpenUriSearchAction).uri,
        )
        assertEquals("Google Drive folder", results[1].subtitle)
        assertEquals(
            "https://drive.google.com/open?id=folder-1",
            (results[1].action as LauncherOpenUriSearchAction).uri,
        )
    }
}
