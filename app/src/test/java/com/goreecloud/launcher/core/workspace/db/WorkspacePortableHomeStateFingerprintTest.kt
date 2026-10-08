package com.goreecloud.launcher.core.workspace.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class WorkspacePortableHomeStateFingerprintTest {
    @Test
    fun equivalentStateHasStableFingerprintRegardlessOfInputOrder() {
        val pages = listOf(
            WorkspacePageEntity("home:0", WorkspaceContainerType.HOME, 0),
            WorkspacePageEntity("home:1", WorkspaceContainerType.HOME, 1),
        )
        val items = listOf(
            item("item:a", "home:0", "10:com.example.a/.Main", 0, 0),
            item("item:b", "home:1", "10:com.example.b/.Main", 0, 1),
        )

        assertEquals(
            WorkspacePortableHomeStateFingerprint.of(pages, items),
            WorkspacePortableHomeStateFingerprint.of(pages.reversed(), items.reversed()),
        )
    }

    @Test
    fun applicationProfileIdentityChangeChangesFingerprint() {
        val pages = listOf(WorkspacePageEntity("home:0", WorkspaceContainerType.HOME, 0))
        val before = listOf(item("item:a", "home:0", "10:com.example.a/.Main", 0, 0))
        val rebound = listOf(item("item:a", "home:0", "20:com.example.a/.Main", 0, 0))

        assertNotEquals(
            WorkspacePortableHomeStateFingerprint.of(pages, before),
            WorkspacePortableHomeStateFingerprint.of(pages, rebound),
        )
    }

    @Test
    fun placementAndItemTypeChangesChangeFingerprint() {
        val pages = listOf(WorkspacePageEntity("home:0", WorkspaceContainerType.HOME, 0))
        val original = listOf(item("item:a", "home:0", "10:com.example.a/.Main", 0, 0))
        val moved = listOf(item("item:a", "home:0", "10:com.example.a/.Main", 1, 0))
        val shortcut = original.map { it.copy(itemType = WorkspaceItemType.SHORTCUT) }

        val originalFingerprint = WorkspacePortableHomeStateFingerprint.of(pages, original)
        assertNotEquals(originalFingerprint, WorkspacePortableHomeStateFingerprint.of(pages, moved))
        assertNotEquals(originalFingerprint, WorkspacePortableHomeStateFingerprint.of(pages, shortcut))
    }

    private fun item(
        id: String,
        pageId: String,
        appKey: String,
        cellX: Int,
        rank: Int,
    ): WorkspaceItemEntity = WorkspaceItemEntity(
        itemId = id,
        pageId = pageId,
        itemType = WorkspaceItemType.APP,
        appKey = appKey,
        rank = rank,
        cellX = cellX,
        cellY = 0,
        spanX = 1,
        spanY = 1,
    )
}
