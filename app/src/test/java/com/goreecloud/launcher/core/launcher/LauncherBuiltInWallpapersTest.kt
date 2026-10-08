package com.goreecloud.launcher.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class LauncherBuiltInWallpapersTest {
    @Test
    fun catalogContainsSixDistinctGoreeCloudWallpapers() {
        val wallpapers = LauncherBuiltInWallpapers.all

        assertEquals(6, wallpapers.size)
        assertTrue(LauncherBuiltInWallpaperId.SOLSTICE in wallpapers.map { it.id })
        assertTrue(LauncherBuiltInWallpaperId.TIDAL in wallpapers.map { it.id })
        assertEquals(wallpapers.size, wallpapers.map { it.id }.distinct().size)
        assertEquals(wallpapers.size, wallpapers.map { it.name }.distinct().size)
        assertTrue(wallpapers.all { it.name.startsWith("Glaze ") })
        assertTrue(wallpapers.all { it.description.isNotBlank() })
    }
    @Test
    fun allBuiltInWallpaperBaseStopsRetainWhiteTextContrast() {
        // Conservative static palette guard. Rendered bloom, shading and on-device contrast
        // still require representative-device visual and accessibility acceptance.
        LauncherBuiltInWallpapers.all.forEach { wallpaper ->
            listOf(wallpaper.startColor, wallpaper.middleColor, wallpaper.endColor).forEach { color ->
                fun linear(component: Int): Double {
                    val normalized = component / 255.0
                    return if (normalized <= 0.04045) normalized / 12.92
                    else ((normalized + 0.055) / 1.055).pow(2.4)
                }
                val luminance =
                    0.2126 * linear((color ushr 16) and 0xFF) +
                    0.7152 * linear((color ushr 8) and 0xFF) +
                    0.0722 * linear(color and 0xFF)
                val whiteContrast = 1.05 / (luminance + 0.05)
                assertTrue(
                    "${wallpaper.name} base palette should support white text (ratio $whiteContrast)",
                    whiteContrast >= 4.5,
                )
            }
        }
    }

}
