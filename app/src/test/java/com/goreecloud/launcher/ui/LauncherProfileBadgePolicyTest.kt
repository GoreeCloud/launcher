package com.goreecloud.launcher.ui

import com.goreecloud.launcher.core.launcher.LauncherDrawerProfileKind
import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherProfileBadgePolicyTest {
    @Test
    fun primaryIdentityUsesUserBadge() {
        assertEquals(
            LauncherDrawerProfileKind.USER,
            launcherProfileBadgeKindForUser(
                user = "primary",
                primaryUser = "primary",
            ),
        )
        assertEquals(
            "User profile",
            launcherProfileBadgeContentDescription(LauncherDrawerProfileKind.USER),
        )
    }

    @Test
    fun nonPrimaryIdentityUsesWorkBadge() {
        assertEquals(
            LauncherDrawerProfileKind.WORK,
            launcherProfileBadgeKindForUser(
                user = "managed-profile",
                primaryUser = "primary",
            ),
        )
        assertEquals(
            "Work profile",
            launcherProfileBadgeContentDescription(LauncherDrawerProfileKind.WORK),
        )
    }
}
