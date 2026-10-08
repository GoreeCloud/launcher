package com.goreecloud.launcher.ui

import androidx.compose.ui.geometry.Offset
import com.goreecloud.launcher.core.launcher.LauncherDockStyle
import com.goreecloud.launcher.core.launcher.LauncherHomeSearchStyle
import com.goreecloud.launcher.ui.theme.GlazeV16MaterialRole
import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherHomeTilePresentationPolicyTest {
    @Test
    fun `compact labeled cell reserves a reduced but usable icon slot`() {
        assertEquals(
            47f,
            launcherHomeTileIconSlotHeightDp(
                availableHeightDp = 68f,
                showLabel = true,
            ),
        )
    }

    @Test
    fun `balanced labeled cell keeps a fifty plus dp icon slot`() {
        assertEquals(
            53f,
            launcherHomeTileIconSlotHeightDp(
                availableHeightDp = 74f,
                showLabel = true,
            ),
        )
    }

    @Test
    fun `airy labeled cell caps icon slot at sixty dp`() {
        assertEquals(
            60f,
            launcherHomeTileIconSlotHeightDp(
                availableHeightDp = 82f,
                showLabel = true,
            ),
        )
    }

    @Test
    fun `unlabeled dock cell can use its full bounded icon slot`() {
        assertEquals(
            56f,
            launcherHomeTileIconSlotHeightDp(
                availableHeightDp = 56f,
                showLabel = false,
            ),
        )
    }

    @Test
    fun `unmeasured cells fail closed to coarse icon floor`() {
        assertEquals(
            44f,
            launcherHomeTileIconSlotHeightDp(
                availableHeightDp = 0f,
                showLabel = true,
            ),
        )
    }


    @Test
    fun `Home search styles request the matching material roles`() {
        assertEquals(
            GlazeV16MaterialRole.FUNCTIONAL_GLASS,
            launcherHomeSearchMaterialRole(LauncherHomeSearchStyle.GLASS),
        )
        assertEquals(
            GlazeV16MaterialRole.CLEAR_GLASS,
            launcherHomeSearchMaterialRole(LauncherHomeSearchStyle.CLEAR),
        )
        assertEquals(
            GlazeV16MaterialRole.SOLID,
            launcherHomeSearchMaterialRole(LauncherHomeSearchStyle.SOLID),
        )
    }

    @Test
    fun `Dock glass styles request presentation-policy material roles`() {
        assertEquals(
            GlazeV16MaterialRole.CLEAR_GLASS,
            launcherDockMaterialRole(LauncherDockStyle.CLEAR),
        )
        assertEquals(
            GlazeV16MaterialRole.FUNCTIONAL_GLASS,
            launcherDockMaterialRole(LauncherDockStyle.GLASS),
        )
        assertEquals(
            GlazeV16MaterialRole.FUNCTIONAL_GLASS,
            launcherDockMaterialRole(LauncherDockStyle.EDGE),
        )
    }

    @Test
    fun `Dock width stays compact while preserving room for common densities`() {
        assertEquals(0.32f, launcherDockWidthFraction(appCount = 1, showSearch = false))
        assertEquals(0.42f, launcherDockWidthFraction(appCount = 2, showSearch = false))
        assertEquals(0.55f, launcherDockWidthFraction(appCount = 3, showSearch = false))
        assertEquals(0.68f, launcherDockWidthFraction(appCount = 4, showSearch = false))
        assertEquals(0.80f, launcherDockWidthFraction(appCount = 5, showSearch = false))
        assertEquals(0.88f, launcherDockWidthFraction(appCount = 6, showSearch = false))
        assertEquals(0.88f, launcherDockWidthFraction(appCount = 2, showSearch = true))
    }

    @Test
    fun `external Dock clearance tracks Dock presentation height`() {
        assertEquals(
            76f,
            launcherExternalDockContentClearanceDp(
                showLabels = false,
                style = LauncherDockStyle.CLEAR,
            ),
        )
        assertEquals(
            84f,
            launcherExternalDockContentClearanceDp(
                showLabels = false,
                style = LauncherDockStyle.EDGE,
            ),
        )
        assertEquals(
            100f,
            launcherExternalDockContentClearanceDp(
                showLabels = true,
                style = LauncherDockStyle.GLASS,
            ),
        )
    }

    @Test
    fun `Home search height yields to large text`() {
        assertEquals(
            50f,
            launcherHomeSearchHeightDp(
                style = LauncherHomeSearchStyle.CLEAR,
                largeText = false,
                extraLargeText = false,
            ),
        )
        assertEquals(
            54f,
            launcherHomeSearchHeightDp(
                style = LauncherHomeSearchStyle.GLASS,
                largeText = false,
                extraLargeText = false,
            ),
        )
        assertEquals(
            60f,
            launcherHomeSearchHeightDp(
                style = LauncherHomeSearchStyle.GLASS,
                largeText = true,
                extraLargeText = false,
            ),
        )
        assertEquals(
            64f,
            launcherHomeSearchHeightDp(
                style = LauncherHomeSearchStyle.CLEAR,
                largeText = true,
                extraLargeText = true,
            ),
        )
    }


    @Test
    fun `wallpaper glass is disabled for solid fallback roles`() {
        assertEquals(true, launcherUsesWallpaperGlass(GlazeV16MaterialRole.CLEAR_GLASS))
        assertEquals(true, launcherUsesWallpaperGlass(GlazeV16MaterialRole.FUNCTIONAL_GLASS))
        assertEquals(false, launcherUsesWallpaperGlass(GlazeV16MaterialRole.SOLID))
        assertEquals(false, launcherUsesWallpaperGlass(GlazeV16MaterialRole.RAISED))
    }

    @Test
    fun `widget drag only commits after deliberate movement`() {
        assertEquals(false, launcherWidgetDragMoved(Offset(4f, 5f), thresholdPx = 12f))
        assertEquals(true, launcherWidgetDragMoved(Offset(13f, 2f), thresholdPx = 12f))
        assertEquals(true, launcherWidgetDragMoved(Offset(1f, -12f), thresholdPx = 12f))
        assertEquals(false, launcherWidgetDragMoved(Offset(20f, 20f), thresholdPx = 0f))
    }

    @Test
    fun `Drive inline connection fails closed in CI debug builds`() {
        assertEquals(false, launcherDriveInlineConnectionAvailable(debugBuild = true, alreadyConnected = false))
        assertEquals(true, launcherDriveInlineConnectionAvailable(debugBuild = false, alreadyConnected = false))
        assertEquals(true, launcherDriveInlineConnectionAvailable(debugBuild = true, alreadyConnected = true))
    }

    @Test
    fun `wallpaper surfaces keep light system bar icons`() {
        assertEquals(
            false,
            launcherUsesDarkSystemBarIcons(
                surfaceMode = LauncherSurfaceMode.HOME,
                startupWizardCompleted = true,
                homeEditorVisible = false,
                darkTheme = false,
            ),
        )
        assertEquals(
            false,
            launcherUsesDarkSystemBarIcons(
                surfaceMode = LauncherSurfaceMode.DRAWER,
                startupWizardCompleted = true,
                homeEditorVisible = false,
                darkTheme = false,
            ),
        )
        assertEquals(
            false,
            launcherUsesDarkSystemBarIcons(
                surfaceMode = LauncherSurfaceMode.SEARCH,
                startupWizardCompleted = true,
                homeEditorVisible = false,
                darkTheme = false,
            ),
        )
    }

    @Test
    fun `light full screen setup and editor surfaces use dark system bar icons`() {
        assertEquals(
            true,
            launcherUsesDarkSystemBarIcons(
                surfaceMode = LauncherSurfaceMode.HOME,
                startupWizardCompleted = false,
                homeEditorVisible = false,
                darkTheme = false,
            ),
        )
        assertEquals(
            true,
            launcherUsesDarkSystemBarIcons(
                surfaceMode = LauncherSurfaceMode.HOME,
                startupWizardCompleted = true,
                homeEditorVisible = true,
                darkTheme = false,
            ),
        )
        assertEquals(
            true,
            launcherUsesDarkSystemBarIcons(
                surfaceMode = LauncherSurfaceMode.SETTINGS,
                startupWizardCompleted = true,
                homeEditorVisible = false,
                darkTheme = false,
            ),
        )
        assertEquals(
            true,
            launcherUsesDarkSystemBarIcons(
                surfaceMode = LauncherSurfaceMode.THEME_MANAGER,
                startupWizardCompleted = true,
                homeEditorVisible = false,
                darkTheme = false,
            ),
        )
    }

    @Test
    fun `dark theme keeps light system bar icons on material surfaces`() {
        assertEquals(
            false,
            launcherUsesDarkSystemBarIcons(
                surfaceMode = LauncherSurfaceMode.SETTINGS,
                startupWizardCompleted = true,
                homeEditorVisible = false,
                darkTheme = true,
            ),
        )
        assertEquals(
            false,
            launcherUsesDarkSystemBarIcons(
                surfaceMode = LauncherSurfaceMode.HOME,
                startupWizardCompleted = false,
                homeEditorVisible = true,
                darkTheme = true,
            ),
        )
    }

    @Test
    fun `Search fields yield to large text`() {
        assertEquals(
            54f,
            launcherSearchFieldHeightDp(
                largeText = false,
                extraLargeText = false,
            ),
        )
        assertEquals(
            60f,
            launcherSearchFieldHeightDp(
                largeText = true,
                extraLargeText = false,
            ),
        )
        assertEquals(
            64f,
            launcherSearchFieldHeightDp(
                largeText = true,
                extraLargeText = true,
            ),
        )
    }
}
