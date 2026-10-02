package com.goreecloud.launcher.core.launcher

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LauncherPreferencesTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun startupConfigurationPersistsSelectedStateAndCompletionTogether() = runBlocking {
        val dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { temporaryFolder.newFile("startup.preferences_pb") },
        )
        val repository = LauncherPreferencesRepository(dataStore)

        try {
            repository.setStartupWizardStep(2).join()
            assertEquals(2, repository.experiencePreferences.first().startupWizardStep)

            repository.applyStartupConfiguration(
                homeAppMode = LauncherHomeAppMode.MOST_USED,
                homeColumns = 6,
                homeRows = 7,
                showHomeLabels = false,
                universalSearchHomeMode = LauncherUniversalSearchHomeMode.PERMANENT,
                addNewAppsToHome = true,
                showHints = false,
            )

            val preferences = repository.preferences.first()
            val experience = repository.experiencePreferences.first()

            assertEquals(6, preferences.homeColumns)
            assertEquals(7, preferences.homeRows)
            assertEquals(
                LauncherUniversalSearchHomeMode.PERMANENT,
                preferences.universalSearchHomeMode,
            )
            assertEquals(LauncherHomeAppMode.MOST_USED, experience.homeAppMode)
            assertEquals(true, experience.useLocalUsageForSuggestions)
            assertEquals(false, experience.showHomeLabels)
            assertEquals(true, experience.addNewAppsToHome)
            assertEquals(true, experience.homeHintsDismissed)
            assertEquals(0, experience.startupWizardStep)
            assertEquals(true, experience.startupWizardCompleted)
        } finally {
            dataStoreScope.cancel()
        }
    }

    @Test
    fun hiddenHomeSuggestionsPersistIndependentlyFromManualPlacementPreferences() = runBlocking {
        val dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { temporaryFolder.newFile("hidden-home-suggestions.preferences_pb") },
        )
        val repository = LauncherPreferencesRepository(dataStore)

        try {
            val first = "0:com.example/.One"
            val second = "0:com.example/.Two"

            repository.setHomeSuggestionHidden(first, true).join()
            repository.setHomeSuggestionHidden(second, true).join()
            assertEquals(setOf(first, second), repository.hiddenHomeSuggestionKeys.first())

            repository.setHomeSuggestionHidden(first, false).join()
            assertEquals(setOf(second), repository.hiddenHomeSuggestionKeys.first())
            assertEquals(LauncherHomeAppMode.NONE, repository.experiencePreferences.first().homeAppMode)
        } finally {
            dataStoreScope.cancel()
        }
    }

    @Test
    fun drawerPinsPersistByProfileQualifiedWorkspaceKey() = runBlocking {
        val dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { temporaryFolder.newFile("drawer-pins.preferences_pb") },
        )
        val repository = LauncherPreferencesRepository(dataStore)

        try {
            val personal = "0:com.example/.Main"
            val work = "10:com.example/.Main"

            repository.setDrawerAppPinned(personal, true).join()
            repository.setDrawerAppPinned(work, true).join()
            assertEquals(setOf(personal, work), repository.drawerPinnedAppKeys.first())

            repository.setDrawerAppPinned(personal, false).join()
            assertEquals(setOf(work), repository.drawerPinnedAppKeys.first())
            assertEquals(
                LauncherHomeAppMode.NONE,
                repository.experiencePreferences.first().homeAppMode,
            )
        } finally {
            dataStoreScope.cancel()
        }
    }

    @Test
    fun drawerPinnedOrderFollowsPinningAndExplicitMovement() = runBlocking {
        val dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { temporaryFolder.newFile("drawer-pin-order.preferences_pb") },
        )
        val repository = LauncherPreferencesRepository(dataStore)

        try {
            val first = "0:com.example/.First"
            val second = "10:com.example/.Second"
            val third = "0:com.example/.Third"

            repository.setDrawerAppPinned(first, true).join()
            repository.setDrawerAppPinned(second, true).join()
            repository.setDrawerAppPinned(third, true).join()
            assertEquals(listOf(first, second, third), repository.drawerPinnedAppOrder.first())

            repository.moveDrawerPinnedApp(third, -2).join()
            assertEquals(listOf(third, first, second), repository.drawerPinnedAppOrder.first())

            repository.setDrawerAppPinned(first, false).join()
            assertEquals(listOf(third, second), repository.drawerPinnedAppOrder.first())
            assertEquals(setOf(third, second), repository.drawerPinnedAppKeys.first())
        } finally {
            dataStoreScope.cancel()
        }
    }

    @Test
    fun pinnedOrderCodecFailsClosedAndReconcilesMissingKeysDeterministically() {
        val encoded = LauncherDrawerPinnedOrder.encode(
            listOf("0:com.example/.One", "10:com.example/.Two"),
        )
        assertEquals(
            listOf("0:com.example/.One", "10:com.example/.Two"),
            LauncherDrawerPinnedOrder.decode(encoded),
        )
        assertEquals(emptyList<String>(), LauncherDrawerPinnedOrder.decode("bad:payload"))
        assertEquals(
            listOf("0:a", "0:b", "10:c"),
            LauncherDrawerPinnedOrder.reconcile(
                order = listOf("0:a", "stale", "0:b"),
                pinnedKeys = setOf("0:b", "10:c", "0:a"),
            ),
        )
    }

    @Test
    fun drawerSortSelectionPersistsAndCanResetToDefault() = runBlocking {
        val dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { temporaryFolder.newFile("drawer-sort.preferences_pb") },
        )
        val repository = LauncherPreferencesRepository(dataStore)

        try {
            assertEquals(null, repository.drawerSortOrderName.first())

            repository.setDrawerSortOrderName("PINNED_FIRST").join()
            assertEquals("PINNED_FIRST", repository.drawerSortOrderName.first())

            repository.setDrawerSortOrderName(null).join()
            assertEquals(null, repository.drawerSortOrderName.first())
        } finally {
            dataStoreScope.cancel()
        }
    }

    @Test
    fun replayStartupWizardReopensSetupWithoutResettingConfiguration() = runBlocking {
        val dataStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { temporaryFolder.newFile("startup-replay.preferences_pb") },
        )
        val repository = LauncherPreferencesRepository(dataStore)

        try {
            repository.applyStartupConfiguration(
                homeAppMode = LauncherHomeAppMode.MOST_USED,
                homeColumns = 6,
                homeRows = 7,
                showHomeLabels = false,
                universalSearchHomeMode = LauncherUniversalSearchHomeMode.PERMANENT,
                addNewAppsToHome = true,
                showHints = false,
            )

            repository.replayStartupWizard().join()

            val preferences = repository.preferences.first()
            val experience = repository.experiencePreferences.first()
            assertEquals(6, preferences.homeColumns)
            assertEquals(7, preferences.homeRows)
            assertEquals(
                LauncherUniversalSearchHomeMode.PERMANENT,
                preferences.universalSearchHomeMode,
            )
            assertEquals(LauncherHomeAppMode.MOST_USED, experience.homeAppMode)
            assertFalse(experience.showHomeLabels)
            assertEquals(true, experience.addNewAppsToHome)
            assertEquals(true, experience.homeHintsDismissed)
            assertEquals(0, experience.startupWizardStep)
            assertFalse(experience.startupWizardCompleted)
        } finally {
            dataStoreScope.cancel()
        }
    }

    @Test
    fun sanitizedClampsGridDrawerAndIconScale() {
        val result = LauncherPreferences(
            homeColumns = 2,
            homeRows = 99,
            drawerColumns = 9,
            iconScale = 3f,
        ).sanitized()

        assertEquals(4, result.homeColumns)
        assertEquals(7, result.homeRows)
        assertEquals(6, result.drawerColumns)
        assertEquals(1.15f, result.iconScale, 0f)
    }

    @Test
    fun homeCapacityReflectsConfiguredGrid() {
        assertEquals(
            30,
            LauncherPreferences(homeColumns = 5, homeRows = 6).homeCapacity,
        )
    }

    @Test
    fun defaultsKeepHomeVisuallyQuietAndLayoutUnlocked() {
        val defaults = LauncherPreferences()

        assertEquals(5, defaults.homeColumns)
        assertEquals(6, defaults.homeRows)
        assertFalse(defaults.layoutLocked)
        assertEquals(LauncherUniversalSearchHomeMode.SWIPE_DOWN_ONLY, defaults.universalSearchHomeMode)
    }

    @Test
    fun experienceDefaultsFavorLauncherLikeHomeWithoutDashboardActions() {
        val defaults = LauncherExperiencePreferences()

        assertFalse(defaults.showHomeQuickActions)
        assertEquals(true, defaults.showHomePageIndicator)
        assertEquals(true, defaults.showHomeLabels)
        assertEquals(true, defaults.showDrawerLabels)
        assertEquals(true, defaults.showDrawerPageIndicator)
        assertFalse(defaults.starterLayoutApplied)
        assertEquals(LauncherHomeAppMode.NONE, defaults.homeAppMode)
        assertFalse(defaults.useLocalUsageForSuggestions)
        assertFalse(defaults.addNewAppsToHome)
        assertEquals(0, defaults.startupWizardStep)
        assertFalse(defaults.startupWizardCompleted)
        assertFalse(defaults.homeHintsDismissed)
        assertEquals(LauncherDockStyle.GLASS, defaults.dockStyle)
        assertEquals(LauncherWallpaperShade.SOFT, defaults.wallpaperShade)
        assertEquals(LauncherIconShape.ROUNDED_SQUARE, defaults.iconShape)
        assertEquals(null, defaults.iconPackPackage)
        assertEquals(LauncherDrawerSearchPlacement.OFF, defaults.drawerSearchPlacement)
        assertEquals(LauncherDrawerNavigation.SCROLL, defaults.drawerNavigation)
        assertEquals(LauncherDrawerEntryMode.BROWSE, defaults.drawerEntryMode)
        assertEquals(LauncherDrawerSpacing.STANDARD, defaults.drawerSpacing)
        assertEquals(5, defaults.drawerPageRows)
        assertEquals(LauncherHomeGlanceAlignment.LEFT, defaults.homeGlanceAlignment)
        assertEquals(LauncherHomeSearchPlacement.BOTTOM, defaults.homeSearchPlacement)
        assertEquals(LauncherHomeSearchStyle.GLASS, defaults.homeSearchStyle)
        assertEquals(LauncherHomeSpacing.BALANCED, defaults.homeSpacing)
        assertEquals(
            LauncherGestureAction.builtIn(LauncherGestureActionType.APPS),
            defaults.swipeUpAction,
        )
        assertEquals(
            LauncherGestureAction.builtIn(LauncherGestureActionType.UNIVERSAL_SEARCH),
            defaults.swipeDownAction,
        )
        assertEquals(
            LauncherGestureAction.builtIn(LauncherGestureActionType.NONE),
            defaults.swipeLeftAction,
        )
        assertEquals(
            LauncherGestureAction.builtIn(LauncherGestureActionType.NONE),
            defaults.swipeRightAction,
        )
        assertEquals(
            LauncherGestureAction.builtIn(LauncherGestureActionType.NONE),
            defaults.doubleTapAction,
        )
        assertEquals(
            LauncherGestureAction.builtIn(LauncherGestureActionType.HOME_EDITOR),
            defaults.tapAndHoldAction,
        )
    }

    @Test
    fun visualPreferenceStorageDecodingFailsSafe() {
        assertEquals(LauncherDockStyle.CLEAR, LauncherDockStyle.fromStorage("clear"))
        assertEquals(LauncherDockStyle.EDGE, LauncherDockStyle.fromStorage("edge"))
        assertEquals(LauncherDockStyle.GLASS, LauncherDockStyle.fromStorage("unknown"))
        assertEquals(LauncherWallpaperShade.STRONG, LauncherWallpaperShade.fromStorage("strong"))
        assertEquals(LauncherWallpaperShade.SOFT, LauncherWallpaperShade.fromStorage(null))
        assertEquals(
            LauncherDrawerSearchPlacement.OFF,
            LauncherDrawerSearchPlacement.fromStorage("off"),
        )
        assertEquals(
            LauncherDrawerSearchPlacement.TOP,
            LauncherDrawerSearchPlacement.fromStorage("top"),
        )
        assertEquals(
            LauncherDrawerSearchPlacement.BOTTOM,
            LauncherDrawerSearchPlacement.fromStorage("bottom"),
        )
        assertEquals(
            LauncherDrawerSearchPlacement.OFF,
            LauncherDrawerSearchPlacement.fromStorage("unknown"),
        )
        assertEquals(
            LauncherDrawerSearchPlacement.OFF,
            LauncherDrawerSearchPlacement.fromStorage(null),
        )
        assertEquals(LauncherDrawerNavigation.SCROLL, LauncherDrawerNavigation.fromStorage("scroll"))
        assertEquals(LauncherDrawerNavigation.PAGES, LauncherDrawerNavigation.fromStorage("pages"))
        assertEquals(LauncherDrawerNavigation.SCROLL, LauncherDrawerNavigation.fromStorage("unknown"))
        assertEquals(LauncherDrawerNavigation.SCROLL, LauncherDrawerNavigation.fromStorage(null))
        assertEquals(
            LauncherDrawerEntryMode.SEARCH_FIRST,
            LauncherDrawerEntryMode.fromStorage("search_first"),
        )
        assertEquals(
            LauncherDrawerEntryMode.BROWSE,
            LauncherDrawerEntryMode.fromStorage("unknown"),
        )
        assertEquals(LauncherDrawerSpacing.TIGHT, LauncherDrawerSpacing.fromStorage("tight"))
        assertEquals(LauncherDrawerSpacing.RELAXED, LauncherDrawerSpacing.fromStorage("relaxed"))
        assertEquals(LauncherDrawerSpacing.STANDARD, LauncherDrawerSpacing.fromStorage("unknown"))
        assertEquals(
            LauncherHomeGlanceAlignment.CENTER,
            LauncherHomeGlanceAlignment.fromStorage("center"),
        )
        assertEquals(
            LauncherHomeGlanceAlignment.LEFT,
            LauncherHomeGlanceAlignment.fromStorage("unknown"),
        )
        assertEquals(
            LauncherHomeSearchPlacement.MOVABLE,
            LauncherHomeSearchPlacement.fromStorage("movable"),
        )
        assertEquals(
            LauncherHomeSearchPlacement.TOP,
            LauncherHomeSearchPlacement.fromStorage("top"),
        )
        assertEquals(
            LauncherHomeSearchPlacement.BOTTOM,
            LauncherHomeSearchPlacement.fromStorage("unknown"),
        )
        assertEquals(
            LauncherHomeSearchStyle.CLEAR,
            LauncherHomeSearchStyle.fromStorage("clear"),
        )
        assertEquals(
            LauncherHomeSearchStyle.SOLID,
            LauncherHomeSearchStyle.fromStorage("solid"),
        )
        assertEquals(
            LauncherHomeSearchStyle.GLASS,
            LauncherHomeSearchStyle.fromStorage("unknown"),
        )
        assertEquals(LauncherHomeAppMode.RECENT, LauncherHomeAppMode.fromStorage("recent"))
        assertEquals(LauncherHomeAppMode.MOST_USED, LauncherHomeAppMode.fromStorage("most_used"))
        assertEquals(LauncherHomeAppMode.NONE, LauncherHomeAppMode.fromStorage("unknown"))
        assertEquals(LauncherHomeSpacing.COMPACT, LauncherHomeSpacing.fromStorage("compact"))
        assertEquals(LauncherHomeSpacing.AIRY, LauncherHomeSpacing.fromStorage("airy"))
        assertEquals(LauncherHomeSpacing.BALANCED, LauncherHomeSpacing.fromStorage("unknown"))
    }

    @Test
    fun homeSearchSurfaceResolvesSwipeMovableAndFixedModes() {
        assertEquals(
            LauncherHomeSearchSurface.SWIPE_DOWN_ONLY,
            launcherHomeSearchSurface(
                LauncherUniversalSearchHomeMode.SWIPE_DOWN_ONLY,
                LauncherHomeSearchPlacement.MOVABLE,
            ),
        )
        assertEquals(
            LauncherHomeSearchSurface.MOVABLE,
            launcherHomeSearchSurface(
                LauncherUniversalSearchHomeMode.PERMANENT,
                LauncherHomeSearchPlacement.MOVABLE,
            ),
        )
        assertEquals(
            LauncherHomeSearchSurface.FIXED_TOP,
            launcherHomeSearchSurface(
                LauncherUniversalSearchHomeMode.PERMANENT,
                LauncherHomeSearchPlacement.TOP,
            ),
        )
        assertEquals(
            LauncherHomeSearchSurface.FIXED_BOTTOM,
            launcherHomeSearchSurface(
                LauncherUniversalSearchHomeMode.PERMANENT,
                LauncherHomeSearchPlacement.BOTTOM,
            ),
        )
    }

    @Test
    fun homeLabelOverridesRoundTripAndSanitize() {
        val encoded = LauncherHomeLabelOverridesCodec.encode(
            mapOf(
                "0:com.example/.Main" to "  Camera  ",
                "1:com.example/.Other" to "Gallery",
            )
        )

        assertEquals(
            mapOf(
                "0:com.example/.Main" to "Camera",
                "1:com.example/.Other" to "Gallery",
            ),
            LauncherHomeLabelOverridesCodec.decode(encoded),
        )
        assertEquals("My App", LauncherHomeLabelPolicy.normalize("  My\u0000 App  "))
        assertEquals(null, LauncherHomeLabelPolicy.normalize(" \n\t "))
    }

    @Test
    fun homeDragPolicySelectsNearestDifferentGridTarget() {
        val targets = listOf(
            LauncherHomeDragTarget("a", 0f, 0f),
            LauncherHomeDragTarget("b", 100f, 0f),
            LauncherHomeDragTarget("c", 0f, 100f),
        )

        assertEquals("b", LauncherHomeDragPolicy.nearestTargetKey("a", 90f, 4f, targets))
        assertEquals(null, LauncherHomeDragPolicy.nearestTargetKey("a", 4f, 3f, targets))
    }

    @Test
    fun gestureActionStorageRoundTripsBuiltInsAndAppTargets() {
        val settings =
            LauncherGestureAction.builtIn(LauncherGestureActionType.LAUNCHER_SETTINGS)
        assertEquals("launcher_settings", settings.storageValue)
        assertEquals(
            settings,
            LauncherGestureAction.fromStorage(
                "launcher_settings",
                LauncherGestureAction.builtIn(LauncherGestureActionType.NONE),
            ),
        )

        val app = LauncherGestureAction.openApp("10:com.goreecloud.camera/.MainActivity")
        assertEquals("app:10:com.goreecloud.camera/.MainActivity", app.storageValue)
        assertEquals(
            app,
            LauncherGestureAction.fromStorage(
                app.storageValue,
                LauncherGestureAction.builtIn(LauncherGestureActionType.NONE),
            ),
        )
    }

    @Test
    fun gestureActionStorageFailsSafeToConfiguredFallback() {
        val fallback = LauncherGestureAction.builtIn(LauncherGestureActionType.APPS)

        assertEquals(fallback, LauncherGestureAction.fromStorage("unknown", fallback))
        assertEquals(fallback, LauncherGestureAction.fromStorage("app:", fallback))
        assertEquals(fallback, LauncherGestureAction.fromStorage(null, fallback))
    }

    @Test
    fun drawerLayoutModeStorageDecodingFailsSafeToGrid() {
        assertEquals(LauncherDrawerLayoutMode.COMPACT, LauncherDrawerLayoutMode.fromStorage("compact"))
        assertEquals(LauncherDrawerLayoutMode.LIST, LauncherDrawerLayoutMode.fromStorage("list"))
        assertEquals(LauncherDrawerLayoutMode.CATEGORY, LauncherDrawerLayoutMode.fromStorage("category"))
        assertEquals(LauncherDrawerLayoutMode.GRID, LauncherDrawerLayoutMode.fromStorage("unknown"))
        assertEquals(LauncherDrawerLayoutMode.GRID, LauncherDrawerLayoutMode.fromStorage(null))
    }

    @Test
    fun universalSearchHomeModeStorageDecodingFailsSafeToSwipeDownOnly() {
        assertEquals(
            LauncherUniversalSearchHomeMode.SWIPE_DOWN_ONLY,
            LauncherUniversalSearchHomeMode.fromStorage("swipe_down_only"),
        )
        assertEquals(
            LauncherUniversalSearchHomeMode.SWIPE_DOWN_ONLY,
            LauncherUniversalSearchHomeMode.fromStorage("unknown"),
        )
        assertEquals(
            LauncherUniversalSearchHomeMode.SWIPE_DOWN_ONLY,
            LauncherUniversalSearchHomeMode.fromStorage(null),
        )
    }
}
