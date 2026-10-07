package com.goreecloud.launcher.core.launcher

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** On-device proof that a user-owned API key is encrypted and excluded from Android backup. */
@RunWith(AndroidJUnit4::class)
class LauncherDirectApiSourceStoreRuntimeTest {
    @Test
    fun credentialDoesNotAppearInStoredBytesAndDisconnectDeletesIt() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = LauncherDirectApiSourceStore(context)
        val id = LauncherDirectApiCatalog.nextCustomId()
        val fakeKey = "fixture-secret-" + UUID.randomUUID().toString()
        val file = File(context.noBackupFilesDir, "direct_api_sources_v1.enc")
        val source = LauncherDirectApiSource(
            id = id,
            kind = LauncherDirectApiKind.CUSTOM_CHAT,
            title = "Test custom chat source",
            endpoint = "https://api.example.org/v1/chat/completions",
            model = "test-model",
            secret = fakeKey,
            enabled = false,
        )
        try {
            store.upsert(source)
            assertTrue(store.list().any {
                it.id == id && it.secret == fakeKey && !it.enabled
            })
            assertTrue(file.exists())
            assertTrue(file.canonicalPath.startsWith(context.noBackupFilesDir.canonicalPath))
            assertFalse(String(file.readBytes(), Charsets.ISO_8859_1).contains(fakeKey))
            assertFalse(source.toString().contains(fakeKey))
        } finally {
            store.remove(id)
        }
        assertFalse(store.list().any { it.id == id })
    }
}
