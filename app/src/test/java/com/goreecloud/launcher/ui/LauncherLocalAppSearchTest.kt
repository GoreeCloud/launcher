package com.goreecloud.launcher.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherLocalAppSearchTest {
    @Test
    fun suggestionsKeepDifferentActivitiesFromSamePackageAcrossProfiles() {
        data class Entry(
            val display: String,
            val profile: String,
            val component: String,
        )
        val personalNotes = Entry("Notes", "personal", "com.example.notes/.Notes")
        val personalTasks = Entry("Tasks", "personal", "com.example.notes/.Tasks")
        val workNotes = Entry("Work Notes", "work", "com.example.notes/.Notes")
        val repeatedNotes = personalNotes.copy(display = "Duplicate Notes")

        val suggestions = launcherUniqueSearchSuggestionActivities(
            apps = sequenceOf(personalNotes, personalTasks, workNotes, repeatedNotes),
            profileOf = Entry::profile,
            componentOf = Entry::component,
        ).toList()

        assertEquals(listOf(personalNotes, personalTasks, workNotes), suggestions)
    }

    @Test
    fun suggestionIdentityDoesNotMixActivitiesOrProfilesWhenOrderChanges() {
        data class Entry(val profile: Int, val component: String)
        val first = Entry(0, "pkg/.First")
        val second = Entry(0, "pkg/.Second")
        val work = Entry(10, "pkg/.First")
        val ordered = launcherUniqueSearchSuggestionActivities(
            apps = sequenceOf(work, second, first, second, work),
            profileOf = Entry::profile,
            componentOf = Entry::component,
        ).toList()

        assertEquals(listOf(work, second, first), ordered)
    }

    @Test
    fun blankQueryShowsAllLocalApps() {
        assertTrue(
            LauncherLocalAppSearch.matches(
                label = "GoreeCloud Memos",
                packageName = "com.goreecloud.memos",
                rawQuery = "   ",
            ),
        )
    }

    @Test
    fun multiTermQueryMustMatchAcrossLocalLabelOrPackage() {
        assertTrue(
            LauncherLocalAppSearch.matches(
                label = "GoreeCloud Browser",
                packageName = "com.goreecloud.browser",
                rawQuery = "goree brow",
            ),
        )
        assertFalse(
            LauncherLocalAppSearch.matches(
                label = "GoreeCloud Browser",
                packageName = "com.goreecloud.browser",
                rawQuery = "goree dial",
            ),
        )
    }

    @Test
    fun packageNameRemainsSearchableWithoutNetworkAuthority() {
        assertTrue(
            LauncherLocalAppSearch.matches(
                label = "Messenger",
                packageName = "com.goreecloud.messenger.development",
                rawQuery = "development",
            ),
        )
    }

    @Test
    fun diacriticsAndPunctuationNormalizeForHumanSearch() {
        assertTrue(
            LauncherLocalAppSearch.matches(
                label = "Café Notes",
                packageName = "com.example.cafe_notes",
                rawQuery = "cafe notes",
            ),
        )
    }

    @Test
    fun folderLabelsCanUseTheSameLocalMatcherWithoutPackageData() {
        assertTrue(
            LauncherLocalAppSearch.matches(
                label = "Work & Productivity",
                packageName = "",
                rawQuery = "prod",
            ),
        )
        assertFalse(
            LauncherLocalAppSearch.matches(
                label = "Work & Productivity",
                packageName = "",
                rawQuery = "games",
            ),
        )
    }

    @Test
    fun prefixTermsMatchWordStarts() {
        assertTrue(
            LauncherLocalAppSearch.matches(
                label = "Advanced Tab Manager",
                packageName = "com.goreecloud.tabs",
                rawQuery = "adv man",
            ),
        )
    }
}
