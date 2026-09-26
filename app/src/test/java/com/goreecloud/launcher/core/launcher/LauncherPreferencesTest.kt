package com.goreecloud.launcher.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LauncherPreferencesTest {
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
        assertFalse(defaults.startupWizardCompleted)
        assertFalse(defaults.homeHintsDismissed)
        assertEquals(LauncherDockStyle.GLASS, defaults.dockStyle)
        assertEquals(LauncherWallpaperShade.SOFT, defaults.wallpaperShade)
        assertEquals(LauncherIconShape.ROUNDED_SQUARE, defaults.iconShape)
        assertEquals(null, defaults.iconPackPackage)
        assertEquals(LauncherDrawerSearchPlacement.BOTTOM, defaults.drawerSearchPlacement)
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
            LauncherDrawerSearchPlacement.TOP,
            LauncherDrawerSearchPlacement.fromStorage("top"),
        )
        assertEquals(
            LauncherDrawerSearchPlacement.BOTTOM,
            LauncherDrawerSearchPlacement.fromStorage("unknown"),
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
    fun universalSearchHomeModeStorageDecodingFailsSafeToPermanent() {
        assertEquals(
            LauncherUniversalSearchHomeMode.SWIPE_DOWN_ONLY,
            LauncherUniversalSearchHomeMode.fromStorage("swipe_down_only"),
        )
        assertEquals(
            LauncherUniversalSearchHomeMode.PERMANENT,
            LauncherUniversalSearchHomeMode.fromStorage("unknown"),
        )
        assertEquals(
            LauncherUniversalSearchHomeMode.PERMANENT,
            LauncherUniversalSearchHomeMode.fromStorage(null),
        )
    }
}
