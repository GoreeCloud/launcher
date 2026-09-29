package com.goreecloud.launcher.core.workspace

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkspaceCodecTest {
    @Test
    fun encodeDecodePreservesOrder() {
        val values = listOf("0:com.example/.One", "10:com.example/.Two", "0:com.example/.Three")

        assertEquals(values, WorkspaceCodec.decode(WorkspaceCodec.encode(values)))
    }

    @Test
    fun toggleAddsAndRemovesWithoutReorderingExistingEntries() {
        val initial = listOf("one", "two")

        val added = WorkspaceCodec.toggled(initial, "three")
        assertEquals(listOf("one", "two", "three"), added)

        val removed = WorkspaceCodec.toggled(added, "two")
        assertEquals(listOf("one", "three"), removed)
    }

    @Test
    fun toggleRespectsDockLimitButStillAllowsRemoval() {
        val fullDock = listOf("one", "two", "three", "four", "five")

        assertEquals(fullDock, WorkspaceCodec.toggled(fullDock, "six", 5))
        assertEquals(
            listOf("one", "two", "four", "five"),
            WorkspaceCodec.toggled(fullDock, "three", 5),
        )
    }

    @Test
    fun moveEarlierAndLaterPreserveAllEntries() {
        val initial = listOf("one", "two", "three", "four")

        assertEquals(
            listOf("one", "three", "two", "four"),
            WorkspaceCodec.moved(initial, "three", WorkspaceMoveDirection.EARLIER),
        )
        assertEquals(
            listOf("one", "three", "two", "four"),
            WorkspaceCodec.moved(initial, "two", WorkspaceMoveDirection.LATER),
        )
    }

    @Test
    fun moveStopsAtBoundariesAndIgnoresUnknownKeys() {
        val initial = listOf("one", "two", "three")

        assertEquals(initial, WorkspaceCodec.moved(initial, "one", WorkspaceMoveDirection.EARLIER))
        assertEquals(initial, WorkspaceCodec.moved(initial, "three", WorkspaceMoveDirection.LATER))
        assertEquals(initial, WorkspaceCodec.moved(initial, "missing", WorkspaceMoveDirection.LATER))
    }

    @Test
    fun moveToTargetSupportsForwardAndBackwardDragDrops() {
        val initial = listOf("one", "two", "three", "four")

        assertEquals(
            listOf("one", "three", "four", "two"),
            WorkspaceCodec.movedToTarget(initial, "two", "four"),
        )
        assertEquals(
            listOf("one", "four", "two", "three"),
            WorkspaceCodec.movedToTarget(initial, "four", "two"),
        )
    }

    @Test
    fun moveToTargetIgnoresSelfAndUnknownKeys() {
        val initial = listOf("one", "two", "three")

        assertEquals(initial, WorkspaceCodec.movedToTarget(initial, "two", "two"))
        assertEquals(initial, WorkspaceCodec.movedToTarget(initial, "missing", "two"))
        assertEquals(initial, WorkspaceCodec.movedToTarget(initial, "two", "missing"))
    }
}
