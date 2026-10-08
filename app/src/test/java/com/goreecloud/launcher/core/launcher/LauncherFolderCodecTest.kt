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
                appKeys = listOf("10:app.one", "10:app.two"),
                profileId = 10,
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
    fun legacyThreeFieldRowsDecodeAsPrimaryProfileCompatibleFolders() {
        val current = LauncherFolderCodec.encode(
            listOf(
                LauncherFolder(
                    id = "legacy",
                    name = "Utilities",
                    appKeys = listOf("0:com.example/.Main"),
                    profileId = 0,
                ),
            ),
        )
        val parts = current.split('\t')
        val legacy = listOf(parts[0], parts[1], parts[3]).joinToString("\t")

        assertEquals(
            LauncherFolder(
                id = "legacy",
                name = "Utilities",
                appKeys = listOf("0:com.example/.Main"),
                profileId = null,
            ),
            LauncherFolderCodec.decode(legacy).single(),
        )
    }

    @Test
    fun workspaceKeyProfileIdsAreParsedWithoutGuessingPackageIdentity() {
        assertEquals(10, launcherProfileIdFromWorkspaceKey("10:com.example/.Main"))
        assertEquals(null, launcherProfileIdFromWorkspaceKey("invalid"))
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
