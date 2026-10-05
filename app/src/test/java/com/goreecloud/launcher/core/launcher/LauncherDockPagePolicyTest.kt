package com.goreecloud.launcher.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherDockPagePolicyTest {
    @Test
    fun preferredFiveUsesOnePageForStarterDock() {
        assertEquals(
            LauncherDockPagePlan(5, 5, 1),
            launcherDockPagePlan(
                itemCount = 5,
                configuredPageSize = 5,
                availableAppWidthDp = 320f,
                minimumInteractionTargetDp = 48f,
            ),
        )
    }

    @Test
    fun overflowCreatesIndependentDockPages() {
        assertEquals(
            LauncherDockPagePlan(5, 5, 3),
            launcherDockPagePlan(
                itemCount = 12,
                configuredPageSize = 5,
                availableAppWidthDp = 320f,
                minimumInteractionTargetDp = 48f,
            ),
        )
    }

    @Test
    fun narrowLayoutsPageBeforeViolatingInteractionFloor() {
        assertEquals(
            LauncherDockPagePlan(7, 4, 2),
            launcherDockPagePlan(
                itemCount = 7,
                configuredPageSize = 7,
                availableAppWidthDp = 200f,
                minimumInteractionTargetDp = 48f,
            ),
        )
    }

    @Test
    fun configuredDensityIsBounded() {
        assertEquals(4, launcherDockPagePlan(1, 1, 500f, 48f).configuredPageSize)
        assertEquals(7, launcherDockPagePlan(1, 99, 500f, 48f).configuredPageSize)
    }

    @Test
    fun loopingAddsSentinelPagesWithoutChangingLogicalOrder() {
        assertEquals(5, launcherDockVirtualPageCount(logicalPageCount = 3, loop = true))
        assertEquals(1, launcherDockInitialVirtualPage(logicalPageCount = 3, loop = true))
        assertEquals(2, launcherDockLogicalPage(virtualPage = 0, logicalPageCount = 3, loop = true))
        assertEquals(0, launcherDockLogicalPage(virtualPage = 1, logicalPageCount = 3, loop = true))
        assertEquals(2, launcherDockLogicalPage(virtualPage = 3, logicalPageCount = 3, loop = true))
        assertEquals(0, launcherDockLogicalPage(virtualPage = 4, logicalPageCount = 3, loop = true))
    }

    @Test
    fun loopingBoundaryTargetsJumpToEquivalentRealPages() {
        assertEquals(3, launcherDockLoopBoundaryTarget(virtualPage = 0, logicalPageCount = 3, loop = true))
        assertEquals(1, launcherDockLoopBoundaryTarget(virtualPage = 4, logicalPageCount = 3, loop = true))
        assertEquals(null, launcherDockLoopBoundaryTarget(virtualPage = 2, logicalPageCount = 3, loop = true))
        assertEquals(null, launcherDockLoopBoundaryTarget(virtualPage = 0, logicalPageCount = 3, loop = false))
    }
    @Test
    fun loopingAddsSentinelPagesAndMapsEdges() {
        assertEquals(5, launcherDockVirtualPageCount(3, true))
        assertEquals(1, launcherDockInitialVirtualPage(3, true))
        assertEquals(2, launcherDockLogicalPage(0, 3, true))
        assertEquals(0, launcherDockLogicalPage(4, 3, true))
        assertEquals(3, launcherDockLoopBoundaryTarget(0, 3, true))
        assertEquals(1, launcherDockLoopBoundaryTarget(4, 3, true))
    }

    @Test
    fun loopingStaysInactiveForSinglePage() {
        assertEquals(1, launcherDockVirtualPageCount(1, true))
        assertEquals(0, launcherDockInitialVirtualPage(1, true))
        assertEquals(0, launcherDockLogicalPage(0, 1, true))
        assertEquals(null, launcherDockLoopBoundaryTarget(0, 1, true))
    }

}
