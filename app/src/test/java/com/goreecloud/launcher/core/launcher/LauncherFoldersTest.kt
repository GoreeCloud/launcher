package com.goreecloud.launcher.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherFoldersTest {
    @Test
    fun roundTripPreservesUnicodeNamesAndAndroidAppKeys() {
        val folders = listOf(
            LauncherFolder(
                id = "folder-1",
                name = "Family 👪 & Work",
                appKeys = listOf(
                    "0:com.example.mail/.MainActivity",
                    "0:com.example.camera/.CameraActivity",
                ),
                profileId = 0,
            ),
            LauncherFolder(id = "folder-2", name = "中文工具", appKeys = emptyList()),
        )

        assertEquals(folders, LauncherFolderCodec.decode(LauncherFolderCodec.encode(folders)))
    }

    @Test
    fun rejectsMalformedRecordsWithoutDiscardingValidFolders() {
        val good = LauncherFolder(
            id = "valid",
            name = "Utilities",
            appKeys = listOf("0:com.example/.Main"),
        )
        val raw = listOf(
            "invalid",
            LauncherFolderCodec.encode(listOf(good)),
            "not_base64\t???\t???",
            LauncherFolderCodec.encode(listOf(good)),
        ).joinToString("\n")

        assertEquals(listOf(good), LauncherFolderCodec.decode(raw))
    }

    @Test
    fun normalizesNamesAndRejectsBlankFolderNames() {
        assertEquals("Work Projects", LauncherFolderPolicy.normalizeName("  Work   Projects   "))
        assertNull(LauncherFolderPolicy.normalizeName("  \n\t "))
        assertTrue(
            LauncherFolderPolicy.normalizeName("x".repeat(200))!!.length <=
                LauncherFolderPolicy.MAX_NAME_LENGTH
        )
    }

    @Test
    fun removesDuplicateAppMembershipDuringRoundTrip() {
        val folder = LauncherFolder(
            id = "test",
            name = "Media",
            appKeys = listOf("app-a", "app-b", "app-a"),
        )
        val restored = LauncherFolderCodec.decode(LauncherFolderCodec.encode(listOf(folder)))
        assertEquals(listOf("app-a", "app-b"), restored.single().appKeys)
        assertFalse(restored.single().appKeys.isEmpty())
    }

    @Test
    fun profilePolicyKeepsWorkFoldersOutOfPersonalAndOtherWorkProfiles() {
        val workFolder = LauncherFolder(
            id = "work",
            name = "Work",
            appKeys = listOf("10:com.example/.Main"),
            profileId = 10,
        )

        assertTrue(
            LauncherFolderProfilePolicy.belongsToProfile(
                folder = workFolder,
                profileId = 10,
                primaryProfileId = 0,
            ),
        )
        assertFalse(
            LauncherFolderProfilePolicy.belongsToProfile(
                folder = workFolder,
                profileId = 0,
                primaryProfileId = 0,
            ),
        )
        assertFalse(
            LauncherFolderProfilePolicy.belongsToProfile(
                folder = workFolder,
                profileId = 11,
                primaryProfileId = 0,
            ),
        )
        assertTrue(
            LauncherFolderProfilePolicy.canContainWorkspaceKey(
                folder = workFolder,
                appKey = "10:com.example/.Main",
                primaryProfileId = 0,
            ),
        )
        assertFalse(
            LauncherFolderProfilePolicy.canContainWorkspaceKey(
                folder = workFolder,
                appKey = "11:com.example/.Main",
                primaryProfileId = 0,
            ),
        )
    }

    @Test
    fun legacyFoldersRemainPrimaryProfileOnly() {
        val legacy = LauncherFolder(
            id = "legacy",
            name = "Personal",
            appKeys = emptyList(),
            profileId = null,
        )
        assertTrue(
            LauncherFolderProfilePolicy.belongsToProfile(
                folder = legacy,
                profileId = 0,
                primaryProfileId = 0,
            ),
        )
        assertFalse(
            LauncherFolderProfilePolicy.belongsToProfile(
                folder = legacy,
                profileId = 10,
                primaryProfileId = 0,
            ),
        )
    }

    @Test
    fun emptyAndMissingStorageDecodeToEmptyFolderList() {
        assertEquals(emptyList<LauncherFolder>(), LauncherFolderCodec.decode(null))
        assertEquals(emptyList<LauncherFolder>(), LauncherFolderCodec.decode(""))
    }
}
