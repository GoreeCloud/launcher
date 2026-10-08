package com.goreecloud.launcher.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherInstalledAppBaselinePolicyTest {
    @Test
    fun firstObservationInitializesWithoutTreatingExistingAppsAsNew() {
        val result = LauncherInstalledAppBaselinePolicy.reconcile(
            previous = null,
            current = setOf("a", "b", "c"),
        )

        assertFalse(result.initializedBefore)
        assertEquals(emptyList<String>(), result.newAppKeys)
    }

    @Test
    fun subsequentObservationReturnsOnlyNewKeysInDeterministicOrder() {
        val result = LauncherInstalledAppBaselinePolicy.reconcile(
            previous = setOf("a", "b"),
            current = setOf("b", "c", "d"),
        )

        assertTrue(result.initializedBefore)
        assertEquals(listOf("c", "d"), result.newAppKeys)
    }

    @Test
    fun reinstallAfterPriorRemovalCanBeDetectedAsNewAgain() {
        val afterRemoval = LauncherInstalledAppBaselinePolicy.reconcile(
            previous = setOf("a", "b"),
            current = setOf("a"),
        )
        assertEquals(emptyList<String>(), afterRemoval.newAppKeys)

        val afterReinstall = LauncherInstalledAppBaselinePolicy.reconcile(
            previous = setOf("a"),
            current = setOf("a", "b"),
        )
        assertEquals(listOf("b"), afterReinstall.newAppKeys)
    }
}
