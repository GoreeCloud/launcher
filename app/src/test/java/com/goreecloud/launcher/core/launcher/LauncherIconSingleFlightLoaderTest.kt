package com.goreecloud.launcher.core.launcher

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherIconSingleFlightLoaderTest {
    @Test
    fun iconFallbackChainUsesFirstSuccessfulLoaderAndSkipsLaterLoaders() {
        val calls = mutableListOf<String>()

        val result = firstSuccessfulIconLoad(
            {
                calls += "badged"
                null
            },
            {
                calls += "activity"
                "activity-icon"
            },
            {
                calls += "application"
                "application-icon"
            },
        )

        assertEquals("activity-icon", result)
        assertEquals(listOf("badged", "activity"), calls)
    }

    @Test
    fun iconFallbackChainSurvivesLoaderFailureAndUsesPackageFallback() {
        val result = firstSuccessfulIconLoad(
            { error("badged resource failed") },
            { null },
            { "application-icon" },
        )

        assertEquals("application-icon", result)
    }

    @Test
    fun iconFallbackChainReachesAndroidLastResortAfterEarlierFailures() {
        val calls = mutableListOf<String>()

        val result = firstSuccessfulIconLoad(
            {
                calls += "badged"
                null
            },
            {
                calls += "launcher-activity"
                error("temporary activity resource failure")
            },
            {
                calls += "package-manager-activity"
                null
            },
            {
                calls += "application"
                null
            },
            {
                calls += "android-default"
                "default-icon"
            },
        )

        assertEquals("default-icon", result)
        assertEquals(
            listOf(
                "badged",
                "launcher-activity",
                "package-manager-activity",
                "application",
                "android-default",
            ),
            calls,
        )
    }

    @Test
    fun legacyIconNormalizationLeavesInvalidOrAlreadyFullArtworkUnscaled() {
        assertEquals(
            1f,
            launcherLegacyIconNormalizationScale(
                contentWidth = 0,
                contentHeight = 64,
                canvasSize = 100,
            ),
            0.0001f,
        )
        assertEquals(
            1f,
            launcherLegacyIconNormalizationScale(
                contentWidth = 84,
                contentHeight = 60,
                canvasSize = 100,
            ),
            0.0001f,
        )
    }

    @Test
    fun legacyIconNormalizationTargetsPaddedArtworkWithoutExceedingSafetyCap() {
        assertEquals(
            1.125f,
            launcherLegacyIconNormalizationScale(
                contentWidth = 80,
                contentHeight = 60,
                canvasSize = 100,
            ),
            0.0001f,
        )
        assertEquals(
            1.24f,
            launcherLegacyIconNormalizationScale(
                contentWidth = 40,
                contentHeight = 40,
                canvasSize = 100,
            ),
            0.0001f,
        )
    }

    @Test
    fun concurrentWaitersShareOneDecode() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val loader = LauncherIconSingleFlightLoader<String, String>(scope)
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val calls = AtomicInteger(0)

        val first = async {
            loader.load("same") {
                calls.incrementAndGet()
                started.complete(Unit)
                release.await()
                "icon"
            }
        }
        started.await()
        val second = async(start = CoroutineStart.UNDISPATCHED) {
            loader.load("same") {
                calls.incrementAndGet()
                "unexpected-second-decode"
            }
        }

        release.complete(Unit)

        assertEquals("icon", first.await())
        assertEquals("icon", second.await())
        assertEquals(1, calls.get())
        scope.cancel()
    }

    @Test
    fun latestPreloadReplacementCancelsSupersededTail() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val runner = LauncherLatestPreloadRunner(scope)
        val firstStarted = CompletableDeferred<Unit>()
        val firstCancelled = CompletableDeferred<Unit>()
        val neverRelease = CompletableDeferred<Unit>()
        val replacementCompleted = CompletableDeferred<Unit>()
        val supersededTailCalls = AtomicInteger(0)

        runner.replace {
            firstStarted.complete(Unit)
            try {
                neverRelease.await()
                supersededTailCalls.incrementAndGet()
            } finally {
                firstCancelled.complete(Unit)
            }
        }
        firstStarted.await()

        runner.replace {
            replacementCompleted.complete(Unit)
        }

        firstCancelled.await()
        replacementCompleted.await()

        assertEquals(0, supersededTailCalls.get())
        scope.cancel()
    }
}
