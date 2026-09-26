package com.goreecloud.launcher.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherNotificationBadgeCountingPolicyTest {
    @Test
    fun onlyClearableNonSummaryNotificationsProduceCounts() {
        val observations = listOf(
            LauncherBadgeObservation("personal:camera", clearable = true, groupSummary = false),
            LauncherBadgeObservation("personal:camera", clearable = true, groupSummary = false),
            LauncherBadgeObservation("personal:camera", clearable = true, groupSummary = true),
            LauncherBadgeObservation("personal:camera", clearable = false, groupSummary = false),
            LauncherBadgeObservation("personal:mail", clearable = false, groupSummary = true),
        )
        assertEquals(
            mapOf("personal:camera" to 2),
            LauncherBadgeCountingPolicy.aggregate(observations),
        )
    }

    @Test
    fun identicalPackagesInDifferentProfilesRemainSeparate() {
        val personal = "personal:com.example.mail"
        val work = "work:com.example.mail"
        assertEquals(
            mapOf(personal to 1, work to 2),
            LauncherBadgeCountingPolicy.aggregate(
                listOf(
                    LauncherBadgeObservation(personal, clearable = true, groupSummary = false),
                    LauncherBadgeObservation(work, clearable = true, groupSummary = false),
                    LauncherBadgeObservation(work, clearable = true, groupSummary = false),
                ),
            ),
        )
    }

    @Test
    fun emptyAndIneligibleSnapshotsNeverShowBadges() {
        assertEquals(
            emptyMap<String, Int>(),
            LauncherBadgeCountingPolicy.aggregate(emptyList<LauncherBadgeObservation<String>>()),
        )
        assertEquals(
            emptyMap<String, Int>(),
            LauncherBadgeCountingPolicy.aggregate(
                listOf(
                    LauncherBadgeObservation("personal:app", clearable = false, groupSummary = false),
                    LauncherBadgeObservation("work:app", clearable = true, groupSummary = true),
                ),
            ),
        )
    }
}
