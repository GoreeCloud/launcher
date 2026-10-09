package com.goreecloud.launcher.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Test

class LauncherInventoryRefreshScopeTest {
    @Test
    fun transientProfileEnumerationFailureRetriesAndReturnsCompleteSnapshot() = runBlocking {
        var attempts = 0
        val snapshot = launcherRetryInventoryScan(retryDelayMillis = 0L) {
            attempts += 1
            if (attempts == 1) throw IllegalStateException("Profile enumeration not ready")
            listOf("personal", "work")
        }

        assertEquals(listOf("personal", "work"), snapshot)
        assertEquals(2, attempts)
    }

    @Test
    fun persistentlyFailedProfileScanStopsAfterBudgetWithoutPublishingInventory() = runBlocking {
        var attempts = 0
        val snapshot = launcherRetryInventoryScan(retryDelayMillis = 0L) {
            attempts += 1
            throw IllegalStateException("Profile unavailable")
        }

        assertEquals(null, snapshot)
        assertEquals(3, attempts)
    }

    @Test
    fun cancelledInventoryCollectionDoesNotRetryOrSwallowCancellation() = runBlocking {
        var attempts = 0
        try {
            launcherRetryInventoryScan(retryDelayMillis = 0L) {
                attempts += 1
                throw CancellationException("Collector disposed")
            }
            fail("Cancellation must propagate")
        } catch (_: CancellationException) {
            assertEquals(1, attempts)
        }
    }

    @Test
    fun inventoryRetryPolicyRejectsUnboundedAttempts() = runBlocking {
        try {
            launcherRetryInventoryScan(attempts = 0, retryDelayMillis = 0L) {
                listOf("incorrect")
            }
            fail("Retry policy must reject zero attempts")
        } catch (_: IllegalArgumentException) {
            // Invalid attempt budgets must not silently skip the inventory.
        }
        try {
            launcherRetryInventoryScan(attempts = 6, retryDelayMillis = 0L) {
                listOf("incorrect")
            }
            fail("Retry policy must reject an unbounded attempt budget")
        } catch (_: IllegalArgumentException) {
            // Ensure future callers cannot silently run unbounded rescans.
        }
    }

    @Test
    fun identicalUserAndWorkAppsAlwaysSortInSameOrderRegardlessOfScanOrder() {
        data class Entry(
            val label: String,
            val pkg: String,
            val component: String,
            val profileId: Int,
        )
        val user = Entry("Browser", "com.example.browser", "Main", 0)
        val work = Entry("Browser", "com.example.browser", "Main", 10)
        val other = Entry("Calculator", "com.example.calc", "Main", 0)

        fun ordered(items: List<Entry>) = launcherInventoryStableOrder(
            items = items,
            labelOf = Entry::label,
            packageOf = Entry::pkg,
            classOf = Entry::component,
            profileOf = Entry::profileId,
        )

        val expected = listOf(user, work, other)
        assertEquals(expected, ordered(listOf(work, other, user)))
        assertEquals(expected, ordered(listOf(user, other, work)))
        assertEquals(expected, ordered(listOf(other, work, user)))
        assertEquals(3, ordered(listOf(work, other, user)).size)
    }

    @Test
    fun packageRemovalUsesPackageScopedRefresh() {
        assertEquals(
            LauncherInventoryRefreshScope.PACKAGE,
            launcherInventoryRefreshScope(LauncherInventoryChange.PACKAGE_REMOVED),
        )
    }

    @Test
    fun additionsChangesAvailabilityAndProfileTopologyRequireFullRefresh() {
        val fullChanges = listOf(
            LauncherInventoryChange.PACKAGE_ADDED,
            LauncherInventoryChange.PACKAGE_CHANGED,
            LauncherInventoryChange.PACKAGE_SUSPENDED,
            LauncherInventoryChange.PACKAGE_UNSUSPENDED,
            LauncherInventoryChange.PACKAGES_AVAILABLE,
            LauncherInventoryChange.PACKAGES_UNAVAILABLE,
            LauncherInventoryChange.PROFILE_TOPOLOGY,
        )

        fullChanges.forEach { change ->
            assertEquals(
                LauncherInventoryRefreshScope.FULL,
                launcherInventoryRefreshScope(change),
            )
        }
    }

    @Test
    fun drawerProfilePagesKeepPrimaryInventoryFirstAndOrdered() {
        data class Entry(val label: String, val user: String)

        val pages = launcherDrawerProfilePages(
            items = listOf(
                Entry("Camera", "primary"),
                Entry("Files", "work"),
                Entry("Memos", "primary"),
                Entry("Notes", "work"),
            ),
            primaryUser = "primary",
            userOf = Entry::user,
        )

        assertEquals(
            listOf(LauncherDrawerProfileKind.USER, LauncherDrawerProfileKind.WORK),
            pages.map { page -> page.kind },
        )
        assertEquals(listOf("Camera", "Memos"), pages[0].items.map(Entry::label))
        assertEquals(listOf("Files", "Notes"), pages[1].items.map(Entry::label))
    }

    @Test
    fun drawerProfilePagesDoNotExposeEmptyWorkPage() {
        val pages = launcherDrawerProfilePages(
            items = listOf("Camera", "Memos"),
            primaryUser = "primary",
            userOf = { "primary" },
        )

        assertEquals(1, pages.size)
        assertEquals(LauncherDrawerProfileKind.USER, pages.single().kind)
        assertEquals(listOf("Camera", "Memos"), pages.single().items)
    }

    @Test
    fun activeProfileLossRequiresConfirmation() {
        data class Entry(val key: String, val user: String)

        val previous = listOf(
            Entry("personal", "primary"),
            Entry("work-mail", "work"),
            Entry("work-files", "work"),
        )
        val candidate = previous.filterNot { it.key == "work-files" }

        assertTrue(
            launcherInventoryHasActiveProfileLoss(
                previous = previous,
                candidate = candidate,
                activeProfiles = listOf("primary", "work"),
                userOf = Entry::user,
                keyOf = Entry::key,
            ),
        )
    }

    @Test
    fun removedProfileDoesNotTriggerInventoryLossConfirmation() {
        data class Entry(val key: String, val user: String)

        val previous = listOf(
            Entry("personal", "primary"),
            Entry("work-mail", "work"),
        )
        val candidate = listOf(Entry("personal", "primary"))

        assertFalse(
            launcherInventoryHasActiveProfileLoss(
                previous = previous,
                candidate = candidate,
                activeProfiles = listOf("primary"),
                userOf = Entry::user,
                keyOf = Entry::key,
            ),
        )
    }

    @Test
    fun missingPreviouslyVisibleWorkProfileTriggersConfirmation() {
        data class Entry(val key: String, val user: String)
        val previous = listOf(
            Entry("personal", "primary"),
            Entry("work-mail", "work"),
        )
        assertTrue(
            launcherInventoryHasMissingPreviousProfile(
                previous = previous,
                activeProfiles = listOf("primary"),
                userOf = Entry::user,
            ),
        )
        assertFalse(
            launcherInventoryHasMissingPreviousProfile(
                previous = previous,
                activeProfiles = listOf("primary", "work"),
                userOf = Entry::user,
            ),
        )
        assertFalse(
            launcherInventoryHasMissingPreviousProfile(
                previous = emptyList<Entry>(),
                activeProfiles = listOf("primary"),
                userOf = Entry::user,
            ),
        )
    }

    @Test
    fun additionsAndStableInventoryDoNotTriggerLossConfirmation() {
        data class Entry(val key: String, val user: String)

        val previous = listOf(Entry("personal", "primary"))
        val candidate = previous + Entry("camera", "primary")

        assertFalse(
            launcherInventoryHasActiveProfileLoss(
                previous = previous,
                candidate = candidate,
                activeProfiles = listOf("primary"),
                userOf = Entry::user,
                keyOf = Entry::key,
            ),
        )
    }

    @Test
    fun packageRecoveryQueriesOnlyActivitiesMissingFromCurrentProfiles() {
        data class Entry(val name: String, val user: String)
        val personal = Entry("Mail", "primary")
        val work = Entry("Mail", "work")
        val missing = Entry("Files", "work")
        val retiredProfile = Entry("Old Files", "retired")
        val recovered = launcherInventoryMissingPreviousActivities(
            previous = listOf(personal, work, missing, missing, retiredProfile),
            candidate = listOf(work, personal),
            activeProfiles = setOf("primary", "work"),
            userOf = Entry::user,
            keyOf = { it.user to it.name },
        )

        assertEquals(listOf(missing), recovered)
        assertTrue(
            launcherInventoryMissingPreviousActivities(
                previous = listOf(personal, work),
                candidate = listOf(work, personal),
                activeProfiles = setOf("primary", "work"),
                userOf = Entry::user,
                keyOf = { it.user to it.name },
            ).isEmpty(),
        )
    }

    @Test
    fun repeatedTransientOmissionRetainsOnlyOsVerifiedEntries() {
        data class Entry(val name: String, val user: String)
        val personal = Entry("Mail", "primary")
        val work = Entry("Mail", "work")
        val missing = Entry("Files", "work")
        val disabled = Entry("Disabled", "work")
        val retiredProfile = Entry("Retired", "retired")
        val verified = mutableListOf<String>()
        val result = launcherInventoryRetainVerifiedActive(
            previous = listOf(personal, work, missing, disabled, retiredProfile, missing),
            candidate = listOf(personal, work),
            activeProfiles = setOf("primary", "work"),
            userOf = Entry::user,
            keyOf = { it.user to it.name },
            stillEnabled = { entry ->
                verified += entry.name
                entry == missing
            },
        )

        assertEquals(listOf(personal, work, missing), result)
        assertEquals(listOf("Files", "Disabled"), verified)
    }

    @Test
    fun alreadyObservedActivitiesAreNotRevalidatedOrDuplicated() {
        data class Entry(val name: String, val user: String)
        val work = Entry("Mail", "work")
        val result = launcherInventoryRetainVerifiedActive(
            previous = listOf(work, work),
            candidate = listOf(work),
            activeProfiles = setOf("work"),
            userOf = Entry::user,
            keyOf = { it.user to it.name },
            stillEnabled = { error("Already observed activities must not be probed") },
        )

        assertEquals(listOf(work), result)
    }

    @Test
    fun drawerProfilePagesGroupAllSecondaryProfilesIntoInitialWorkPage() {
        data class Entry(val label: String, val user: String)

        val pages = launcherDrawerProfilePages(
            items = listOf(
                Entry("Personal", "primary"),
                Entry("Shelter", "work-one"),
                Entry("Secondary", "work-two"),
            ),
            primaryUser = "primary",
            userOf = Entry::user,
        )

        assertEquals(
            listOf("Shelter", "Secondary"),
            pages.single { page -> page.kind == LauncherDrawerProfileKind.WORK }
                .items
                .map(Entry::label),
        )
    }
}
