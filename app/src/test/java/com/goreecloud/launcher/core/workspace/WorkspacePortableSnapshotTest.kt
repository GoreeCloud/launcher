package com.goreecloud.launcher.core.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class WorkspacePortableSnapshotTest {
    private val grid = WorkspaceGridPlacement.Grid(columns = 4, rows = 5)

    @Test
    fun roundTripIsDeterministicAndPreservesUnicodeOpaqueIdentities() {
        val first = WorkspacePagedPlacement.Page(
            pageId = "home/α",
            rank = 0,
            placements = listOf(
                WorkspaceGridPlacement.Placement(
                    itemId = "profile:10/com.example.天气",
                    cellX = 2,
                    cellY = 1,
                ),
                WorkspaceGridPlacement.Placement(
                    itemId = "profile:10/com.example.notes",
                    cellX = 0,
                    cellY = 0,
                    spanX = 2,
                ),
            ),
        )
        val second = WorkspacePagedPlacement.Page(
            pageId = "work",
            rank = 1,
            placements = listOf(
                WorkspaceGridPlacement.Placement(
                    itemId = "profile:11/com.example.mail",
                    cellX = 1,
                    cellY = 2,
                )
            ),
        )

        val encodedFromReversedInput = WorkspacePortableSnapshot.encode(
            WorkspacePortableSnapshot.Snapshot(grid, listOf(second, first))
        )
        val encodedFromOrderedInput = WorkspacePortableSnapshot.encode(
            WorkspacePortableSnapshot.Snapshot(
                grid,
                listOf(first.copy(placements = first.placements.reversed()), second),
            )
        )

        assertEquals(encodedFromOrderedInput, encodedFromReversedInput)
        assertTrue(encodedFromOrderedInput.startsWith("format=${WorkspacePortableSnapshot.FORMAT}\n"))
        assertTrue("raw item identity must not be emitted into the line format", "com.example.天气" !in encodedFromOrderedInput)

        val decoded = WorkspacePortableSnapshot.decode(encodedFromOrderedInput)
        assertTrue(decoded is WorkspacePortableSnapshot.DecodeResult.Success)
        val snapshot = (decoded as WorkspacePortableSnapshot.DecodeResult.Success).snapshot
        assertEquals(grid, snapshot.grid)
        assertEquals(listOf("home/α", "work"), snapshot.pages.map { it.pageId })
        assertEquals(
            listOf("profile:10/com.example.notes", "profile:10/com.example.天气"),
            snapshot.pages.first().placements.map { it.itemId },
        )
    }

    @Test
    fun tamperingFailsBeforeSnapshotMaterialization() {
        val snapshot = WorkspacePortableSnapshot.Snapshot(
            grid,
            listOf(
                WorkspacePagedPlacement.Page(
                    pageId = "home",
                    rank = 0,
                    placements = listOf(
                        WorkspaceGridPlacement.Placement("app", 0, 0)
                    ),
                )
            ),
        )
        val encoded = WorkspacePortableSnapshot.encode(snapshot)
        val tampered = encoded.replaceFirst("item=", "item=X")

        val decoded = WorkspacePortableSnapshot.decode(tampered)
        assertTrue(decoded is WorkspacePortableSnapshot.DecodeResult.Invalid)
        assertEquals(
            "snapshot integrity check failed",
            (decoded as WorkspacePortableSnapshot.DecodeResult.Invalid).reason,
        )
    }

    @Test
    fun encodeRejectsNonContiguousPageRanks() {
        val snapshot = WorkspacePortableSnapshot.Snapshot(
            grid,
            listOf(
                WorkspacePagedPlacement.Page("home", 0, emptyList()),
                WorkspacePagedPlacement.Page("work", 2, emptyList()),
            ),
        )

        try {
            WorkspacePortableSnapshot.encode(snapshot)
            fail("non-contiguous page ranks must be rejected")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message.orEmpty().contains("contiguous zero-based"))
        }
    }

    @Test
    fun encodeRejectsCollidingPlacementState() {
        val snapshot = WorkspacePortableSnapshot.Snapshot(
            grid,
            listOf(
                WorkspacePagedPlacement.Page(
                    pageId = "home",
                    rank = 0,
                    placements = listOf(
                        WorkspaceGridPlacement.Placement("first", 0, 0, spanX = 2),
                        WorkspaceGridPlacement.Placement("second", 1, 0),
                    ),
                )
            ),
        )

        try {
            WorkspacePortableSnapshot.encode(snapshot)
            fail("invalid placement state must be rejected")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message.orEmpty().contains("placement validation failed"))
        }
    }

    @Test
    fun oversizedInputIsRejectedBeforeParsing() {
        val decoded = WorkspacePortableSnapshot.decode("x".repeat((1 shl 20) + 1))
        assertTrue(decoded is WorkspacePortableSnapshot.DecodeResult.Invalid)
        assertEquals(
            "snapshot exceeds the bounded size limit",
            (decoded as WorkspacePortableSnapshot.DecodeResult.Invalid).reason,
        )
    }
}
