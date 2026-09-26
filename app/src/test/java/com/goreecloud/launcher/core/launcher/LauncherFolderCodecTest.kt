package com.goreecloud.launcher.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherFolderCodecTest {
    @Test
    fun foldersRoundTripWithNamesAndOrderedMembers() {
        val folders = listOf(
            LauncherFolder(
                id = "folder-a",
                name = "Work & Notes",
                appKeys = listOf("user:app.one", "user:app.two"),
            ),
            LauncherFolder(
                id = "folder-b",
                name = "Media",
                appKeys = emptyList(),
            ),
        )

        assertEquals(folders, LauncherFolderCodec.decode(LauncherFolderCodec.encode(folders)))
    }

    @Test
    fun malformedRowsFailSoftWithoutInventingFolders() {
        val encoded = LauncherFolderCodec.encode(
            listOf(LauncherFolder("good", "Utilities", listOf("app"))),
        )
        val decoded = LauncherFolderCodec.decode("not\ta\tvalid\textra\n" + encoded)

        assertEquals(1, decoded.size)
        assertEquals("good", decoded.single().id)
    }

    @Test
    fun namesAreNormalizedAndBounded() {
        val normalized = LauncherFolderPolicy.normalizeName("   My   Folder   ")
        assertEquals("My Folder", normalized)
        assertTrue(
            LauncherFolderPolicy.normalizeName("x".repeat(100))!!.length <=
                LauncherFolderPolicy.MAX_NAME_LENGTH,
        )
    }
}
