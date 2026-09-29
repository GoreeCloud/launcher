package com.goreecloud.launcher.core.launcher

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherUniversalSearchTest {
    @Test
    fun textRankingPrioritizesExactPrefixContainsAndSubtitle() {
        assertEquals(400, LauncherSearchTextRanking.score("Camera", "com.goreecloud.camera", "camera"))
        assertEquals(300, LauncherSearchTextRanking.score("Camera Pro", "com.goreecloud.camera", "cam"))
        assertEquals(200, LauncherSearchTextRanking.score("GoreeCloud Camera", "com.goreecloud.camera", "camera"))
        assertEquals(100, LauncherSearchTextRanking.score("Photos", "com.goreecloud.camera", "camera"))
        assertNull(LauncherSearchTextRanking.score("Photos", "com.goreecloud.gallery", "camera"))
    }

    @Test
    fun blankQueryPreservesBrowseEligibility() {
        assertEquals(0, LauncherSearchTextRanking.score("Camera", "com.goreecloud.camera", "   "))
    }

    @Test
    fun zeroScoreBrowseResultsPreserveProviderOrder() {
        val provider = object : LauncherSearchProvider {
            override val id = "browse"

            override fun search(rawQuery: String): List<LauncherSearchResult> =
                listOf(
                    searchResult(id, "beta", "Beta", 0),
                    searchResult(id, "alpha", "Alpha", 0),
                )
        }

        val results = LauncherUniversalSearch.search("", listOf(provider))

        assertEquals(listOf("Beta", "Alpha"), results.map { it.title })
    }

    @Test
    fun providerFailureDoesNotDisableOtherResults() {
        val failing = object : LauncherSearchProvider {
            override val id = "failing"

            override fun search(rawQuery: String): List<LauncherSearchResult> {
                error("provider failed")
            }
        }
        val working = object : LauncherSearchProvider {
            override val id = "working"

            override fun search(rawQuery: String): List<LauncherSearchResult> =
                listOf(
                    LauncherSearchResult(
                        providerId = id,
                        resultId = "2",
                        title = "Beta",
                        subtitle = null,
                        category = LauncherSearchCategory.APPLICATION,
                        score = 100,
                    ),
                    LauncherSearchResult(
                        providerId = id,
                        resultId = "1",
                        title = "Alpha",
                        subtitle = null,
                        category = LauncherSearchCategory.APPLICATION,
                        score = 200,
                    ),
                )
        }

        val results = LauncherUniversalSearch.search("a", listOf(failing, working))

        assertEquals(listOf("Alpha", "Beta"), results.map { it.title })
    }

    @Test
    fun duplicateProviderResultsAreCollapsedDeterministically() {
        val provider = object : LauncherSearchProvider {
            override val id = "working"

            override fun search(rawQuery: String): List<LauncherSearchResult> {
                val result = LauncherSearchResult(
                    providerId = id,
                    resultId = "same",
                    title = "Camera",
                    subtitle = null,
                    category = LauncherSearchCategory.APPLICATION,
                    score = 400,
                )
                return listOf(result, result)
            }
        }

        assertEquals(1, LauncherUniversalSearch.search("camera", listOf(provider)).size)
    }

    @Test
    fun asyncSearchTimesOutSlowProviderWithoutSuppressingFastResults() = runBlocking {
        val slow = object : LauncherSearchProvider, LauncherAsyncSearchProvider {
            override val id = "slow"

            override fun search(rawQuery: String): List<LauncherSearchResult> =
                error("async provider should use searchAsync")

            override suspend fun searchAsync(
                request: LauncherSearchRequest,
            ): List<LauncherSearchResult> {
                delay(5_000)
                return listOf(searchResult(id, "slow", "Slow", 500))
            }
        }
        val fast = object : LauncherSearchProvider {
            override val id = "fast"

            override fun search(rawQuery: String): List<LauncherSearchResult> =
                listOf(searchResult(id, "fast", "Fast", 100))
        }

        val results = LauncherUniversalSearch.searchAsync(
            rawQuery = "f",
            providers = listOf(slow, fast),
            policy = LauncherSearchExecutionPolicy(providerTimeoutMillis = 50),
        )

        assertEquals(listOf("Fast"), results.map { it.title })
    }

    @Test
    fun cancellationOnlyPolicyDoesNotInventProviderTimeout() = runBlocking {
        val provider = object : LauncherSearchProvider, LauncherAsyncSearchProvider {
            override val id = "measured-later"

            override fun search(rawQuery: String): List<LauncherSearchResult> =
                error("async provider should use searchAsync")

            override suspend fun searchAsync(
                request: LauncherSearchRequest,
            ): List<LauncherSearchResult> {
                delay(75)
                return listOf(searchResult(id, "ready", "Ready", 200))
            }
        }
        val policy = LauncherSearchExecutionPolicy.cancellationOnly()

        val results = LauncherUniversalSearch.searchAsync(
            rawQuery = "ready",
            providers = listOf(provider),
            policy = policy,
        )

        assertNull(policy.providerTimeoutMillis)
        assertEquals(listOf("Ready"), results.map { it.title })
    }

    @Test
    fun asyncProviderFailureDoesNotDisableOtherResults() = runBlocking {
        val failing = object : LauncherSearchProvider, LauncherAsyncSearchProvider {
            override val id = "failing-async"

            override fun search(rawQuery: String): List<LauncherSearchResult> =
                error("async provider should use searchAsync")

            override suspend fun searchAsync(
                request: LauncherSearchRequest,
            ): List<LauncherSearchResult> {
                error("provider failed")
            }
        }
        val working = object : LauncherSearchProvider {
            override val id = "working"

            override fun search(rawQuery: String): List<LauncherSearchResult> =
                listOf(searchResult(id, "1", "Alpha", 200))
        }

        val results = LauncherUniversalSearch.searchAsync(
            rawQuery = "a",
            providers = listOf(failing, working),
            policy = LauncherSearchExecutionPolicy(providerTimeoutMillis = 1_000),
        )

        assertEquals(listOf("Alpha"), results.map { it.title })
    }

    @Test
    fun asyncSearchPropagatesCallerCancellation() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val provider = object : LauncherSearchProvider, LauncherAsyncSearchProvider {
            override val id = "cancellable"

            override fun search(rawQuery: String): List<LauncherSearchResult> =
                error("async provider should use searchAsync")

            override suspend fun searchAsync(
                request: LauncherSearchRequest,
            ): List<LauncherSearchResult> {
                started.complete(Unit)
                awaitCancellation()
            }
        }

        val job = launch {
            LauncherUniversalSearch.searchAsync(
                rawQuery = "camera",
                providers = listOf(provider),
                policy = LauncherSearchExecutionPolicy.cancellationOnly(),
            )
        }

        started.await()
        job.cancelAndJoin()

        assertTrue(job.isCancelled)
    }

    @Test
    fun asyncExecutionPolicyRejectsNonPositiveTimeout() {
        var rejected = false
        try {
            LauncherSearchExecutionPolicy(providerTimeoutMillis = 0)
        } catch (_: IllegalArgumentException) {
            rejected = true
        }

        assertTrue(rejected)
    }

    @Test
    fun coreActionsProviderDoesNotExposeDirectLauncherSettingsEntry() {
        val results = LauncherCoreActionsSearchProvider().search("settings")

        assertTrue(results.none { it.title == "Launcher settings" })
        assertTrue(
            results.none {
                (it.action as? LauncherNavigateSearchAction)?.destination ==
                    LauncherSearchDestination.SETTINGS
            },
        )
    }

    @Test
    fun coreActionsProviderSearchesActionMetadataLocally() {
        val results = LauncherCoreActionsSearchProvider().search("drawer")

        assertEquals(listOf("Apps"), results.map { it.title })
        assertTrue(results.all { it.providerId == LauncherCoreActionsSearchProvider.PROVIDER_ID })
        assertEquals(
            LauncherSearchDestination.APPS,
            (results.single().action as LauncherNavigateSearchAction).destination,
        )
    }

    @Test
    fun coreActionsProviderExposesTrustedHomeAppearanceActions() {
        val provider = LauncherCoreActionsSearchProvider()

        val editHome = provider.search("edit home")
            .single { it.title == "Edit Home" }
        assertEquals(
            LauncherSearchDestination.HOME_EDITOR,
            (editHome.action as LauncherNavigateSearchAction).destination,
        )

        val wallpaper = provider.search("wallpaper")
            .single { it.title == "Wallpaper" }
        assertEquals("Wallpaper", wallpaper.title)
        assertEquals(LauncherSearchCategory.SETTING, wallpaper.category)
        assertEquals(
            LauncherSearchDestination.WALLPAPER,
            (wallpaper.action as LauncherNavigateSearchAction).destination,
        )

        val themeManager = provider.search("theme")
            .single { it.title == "Theme Manager" }
        assertEquals("Theme Manager", themeManager.title)
        assertEquals(LauncherSearchCategory.SETTING, themeManager.category)
        assertEquals(
            LauncherSearchDestination.THEME_MANAGER,
            (themeManager.action as LauncherNavigateSearchAction).destination,
        )
    }

    @Test
    fun phoneNumberSearchIgnoresFormattingWithoutMatchingUnrelatedNumbers() {
        assertEquals(
            160,
            LauncherLocalPhoneSearchPolicy.score("Taylor", "+1 (555) 120-2099", "555120"),
        )
        assertEquals(
            300,
            LauncherLocalPhoneSearchPolicy.score("Taylor Adams", "+1 (555) 120-2099", "tay"),
        )
        assertNull(LauncherLocalPhoneSearchPolicy.score("Taylor", "+1 (555) 120-2099", "442288"))
        assertNull(LauncherLocalPhoneSearchPolicy.score("Taylor", "+1 (555) 120-2099", "tay22"))
        assertNull(LauncherLocalPhoneSearchPolicy.score("Taylor", "+1 (555) 120-2099", "1"))
    }

    @Test
    fun localSourceDiagnosticsDistinguishPermissionRestrictionAndProviderFailure() {
        val providerId = LauncherMessagesSearchProvider.PROVIDER_ID
        try {
            LauncherLocalSearchDiagnostics.record(providerId, LauncherLocalSearchIssue.PERMISSION_REQUIRED)
            assertEquals(
                LauncherLocalSearchIssue.PERMISSION_REQUIRED,
                LauncherLocalSearchDiagnostics.issues.value[providerId],
            )
            LauncherLocalSearchDiagnostics.record(providerId, LauncherLocalSearchIssue.ANDROID_RESTRICTED)
            assertEquals(
                LauncherLocalSearchIssue.ANDROID_RESTRICTED,
                LauncherLocalSearchDiagnostics.issues.value[providerId],
            )
            LauncherLocalSearchDiagnostics.record(providerId, LauncherLocalSearchIssue.SOURCE_UNAVAILABLE)
            assertEquals(
                LauncherLocalSearchIssue.SOURCE_UNAVAILABLE,
                LauncherLocalSearchDiagnostics.issues.value[providerId],
            )
        } finally {
            LauncherLocalSearchDiagnostics.record(providerId, null)
        }
        assertTrue(providerId !in LauncherLocalSearchDiagnostics.issues.value)
    }

    private fun searchResult(
        providerId: String,
        resultId: String,
        title: String,
        score: Int,
    ): LauncherSearchResult = LauncherSearchResult(
        providerId = providerId,
        resultId = resultId,
        title = title,
        subtitle = null,
        category = LauncherSearchCategory.ACTION,
        score = score,
    )
}
