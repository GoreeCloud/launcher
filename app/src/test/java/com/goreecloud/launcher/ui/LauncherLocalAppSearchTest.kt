package com.goreecloud.launcher.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherLocalAppSearchTest {
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
