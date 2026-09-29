package com.goreecloud.launcher.core.workspace

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkspaceGridPlacementTest {
    private val grid = WorkspaceGridPlacement.Grid(columns = 4, rows = 5)

    @Test
    fun acceptsNonOverlappingPlacementsAndSpans() {
        assertEquals(
            WorkspaceGridPlacement.Validation.Valid,
            WorkspaceGridPlacement.validate(
                grid,
                listOf(
                    WorkspaceGridPlacement.Placement("app-a", 0, 0),
                    WorkspaceGridPlacement.Placement("widget-b", 1, 0, spanX = 2, spanY = 2),
                    WorkspaceGridPlacement.Placement("app-c", 3, 4),
                ),
            ),
        )
    }

    @Test
    fun rejectsOutOfBoundsSpan() {
        assertEquals(
            WorkspaceGridPlacement.Validation.OutOfBounds("widget"),
            WorkspaceGridPlacement.validate(
                grid,
                listOf(WorkspaceGridPlacement.Placement("widget", 3, 0, spanX = 2)),
            ),
        )
    }

    @Test
    fun rejectsCollisionAcrossSpans() {
        assertEquals(
            WorkspaceGridPlacement.Validation.Collision("widget", "app"),
            WorkspaceGridPlacement.validate(
                grid,
                listOf(
                    WorkspaceGridPlacement.Placement("widget", 0, 0, spanX = 2, spanY = 2),
                    WorkspaceGridPlacement.Placement("app", 1, 1),
                ),
            ),
        )
    }

    @Test
    fun rejectsDuplicateItemIdentity() {
        assertEquals(
            WorkspaceGridPlacement.Validation.DuplicateItem("same"),
            WorkspaceGridPlacement.validate(
                grid,
                listOf(
                    WorkspaceGridPlacement.Placement("same", 0, 0),
                    WorkspaceGridPlacement.Placement("same", 2, 2),
                ),
            ),
        )
    }
}
