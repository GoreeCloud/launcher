package com.goreecloud.launcher.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherDrawerTabsTest {
    @Test
    fun roundTripPreservesNamesAndProfileQualifiedMembers() {
        val tabs = listOf(
            LauncherDrawerTab(
                id = "tab-work",
                name = "Work",
                memberKeys = linkedSetOf(
                    "user:0/pkg.one/.Main",
                    "user:10/pkg.one/.Main",
                ),
            ),
            LauncherDrawerTab(
                id = "tab-media",
                name = "Media & Photos",
                memberKeys = setOf("user:0/pkg.camera/.Main"),
            ),
        )

        assertEquals(
            tabs,
            LauncherDrawerTabsCodec.decode(LauncherDrawerTabsCodec.encode(tabs)),
        )
    }

    @Test
    fun invalidOrUnknownPayloadFailsClosed() {
        assertEquals(emptyList<LauncherDrawerTab>(), LauncherDrawerTabsCodec.decode(null))
        assertEquals(emptyList<LauncherDrawerTab>(), LauncherDrawerTabsCodec.decode("v2\nabc"))
        assertEquals(emptyList<LauncherDrawerTab>(), LauncherDrawerTabsCodec.decode("not-a-version"))
    }

    @Test
    fun namesAndTabCountAreBounded() {
        val tabs = (0 until 20).map { index ->
            LauncherDrawerTab(
                id = "tab-$index",
                name = "  " + "x".repeat(80) + " $index  ",
                memberKeys = emptySet(),
            )
        }

        val decoded = LauncherDrawerTabsCodec.decode(LauncherDrawerTabsCodec.encode(tabs))
        assertEquals(LauncherDrawerTabsCodec.MAX_TABS, decoded.size)
        assertTrue(decoded.all { it.name.length <= LauncherDrawerTabsCodec.MAX_NAME_LENGTH })
    }

    @Test
    fun duplicateIdsKeepFirstEntry() {
        val decoded = LauncherDrawerTabsCodec.decode(
            LauncherDrawerTabsCodec.encode(
                listOf(
                    LauncherDrawerTab("tab-1", "First", setOf("a")),
                    LauncherDrawerTab("tab-1", "Second", setOf("b")),
                ),
            ),
        )

        assertEquals(1, decoded.size)
        assertEquals("First", decoded.single().name)
        assertEquals(setOf("a"), decoded.single().memberKeys)
    }
}
