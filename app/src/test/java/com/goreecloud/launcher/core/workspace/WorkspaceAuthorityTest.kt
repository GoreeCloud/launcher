package com.goreecloud.launcher.core.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class WorkspaceAuthorityTest {
    @Test
    fun fingerprintIsDeterministicAndOrderSensitive() {
        val original = WorkspaceState(
            initialized = true,
            favoriteKeys = listOf("favorite:a", "favorite:b"),
            dockKeys = listOf("dock:a", "dock:b"),
        )
        val same = original.copy()
        val reordered = original.copy(favoriteKeys = original.favoriteKeys.reversed())

        assertEquals(
            WorkspaceSnapshotFingerprint.of(original),
            WorkspaceSnapshotFingerprint.of(same),
        )
        assertNotEquals(
            WorkspaceSnapshotFingerprint.of(original),
            WorkspaceSnapshotFingerprint.of(reordered),
        )
    }

    @Test
    fun fingerprintSeparatesFavoritesFromDock() {
        val favoriteOnly = WorkspaceState(
            initialized = true,
            favoriteKeys = listOf("shared:key"),
        )
        val dockOnly = WorkspaceState(
            initialized = true,
            dockKeys = listOf("shared:key"),
        )

        assertNotEquals(
            WorkspaceSnapshotFingerprint.of(favoriteOnly),
            WorkspaceSnapshotFingerprint.of(dockOnly),
        )
    }

    @Test
    fun invalidAuthorityValueFailsClosedToDataStore() {
        assertEquals(
            WorkspaceAuthority.DATASTORE,
            WorkspaceAuthorityCodec.decode("unexpected-authority"),
        )
        assertEquals(
            WorkspaceAuthority.DATASTORE,
            WorkspaceAuthorityCodec.decode(null),
        )
    }
}
