package com.goreecloud.launcher.core.launcher

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class LauncherPortablePreferenceImportTest {
    private class RecordingWriter : LauncherPortablePreferenceWriter {
        val writes = mutableListOf<LauncherPreferences>()

        override suspend fun replacePortablePreferences(preferences: LauncherPreferences) {
            writes += preferences
        }
    }

    @Test
    fun validSnapshotProducesExactlyOneCompleteWrite() = runBlocking {
        val expected = LauncherPreferences(
            homeColumns = 6,
            homeRows = 7,
            drawerColumns = 4,
            showLabels = false,
            iconScale = 0.9f,
            layoutLocked = true,
            universalSearchHomeMode = LauncherUniversalSearchHomeMode.SWIPE_DOWN_ONLY,
        )
        val writer = RecordingWriter()

        val result = LauncherPortablePreferenceImport.apply(
            LauncherPortablePreferences.encode(expected),
            writer,
        )

        assertTrue(result is LauncherPortablePreferenceImport.ApplyResult.Applied)
        assertEquals(listOf(expected), writer.writes)
    }

    @Test
    fun tamperedSnapshotPerformsZeroWrites() = runBlocking {
        val encoded = LauncherPortablePreferences.encode(LauncherPreferences())
        val writer = RecordingWriter()

        val result = LauncherPortablePreferenceImport.apply(
            encoded.replace("show_labels=true", "show_labels=false"),
            writer,
        )

        assertTrue(result is LauncherPortablePreferenceImport.ApplyResult.Rejected)
        assertEquals(emptyList<LauncherPreferences>(), writer.writes)
    }

    @Test
    fun unsupportedExpandedSnapshotPerformsZeroWrites() = runBlocking {
        val encoded = LauncherPortablePreferences.encode(LauncherPreferences())
        val expanded = encoded.replace(
            "layout_locked=false\n",
            "layout_locked=false\nhidden_apps=com.example.private\n",
        )
        val writer = RecordingWriter()

        val result = LauncherPortablePreferenceImport.apply(expanded, writer)

        assertTrue(result is LauncherPortablePreferenceImport.ApplyResult.Rejected)
        assertEquals(emptyList<LauncherPreferences>(), writer.writes)
    }

    @Test
    fun persistenceFailureIsNotReportedAsApplied() {
        val expected = LauncherPreferences()
        val writer = object : LauncherPortablePreferenceWriter {
            override suspend fun replacePortablePreferences(preferences: LauncherPreferences) {
                throw IllegalStateException("synthetic persistence failure")
            }
        }

        try {
            runBlocking {
                LauncherPortablePreferenceImport.apply(
                    LauncherPortablePreferences.encode(expected),
                    writer,
                )
            }
            fail("persistence failure must propagate")
        } catch (expectedFailure: IllegalStateException) {
            assertEquals("synthetic persistence failure", expectedFailure.message)
        }
    }
}
