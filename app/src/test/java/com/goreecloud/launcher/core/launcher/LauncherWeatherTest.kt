package com.goreecloud.launcher.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherWeatherTest {
    @Test
    fun wmoCodesMapToCompactLauncherConditions() {
        assertEquals("Clear", launcherWeatherCondition(0))
        assertEquals("Partly cloudy", launcherWeatherCondition(2))
        assertEquals("Rain", launcherWeatherCondition(63))
        assertEquals("Snow", launcherWeatherCondition(75))
        assertEquals("Thunderstorms", launcherWeatherCondition(95))
        assertEquals("Conditions", launcherWeatherCondition(-1))
    }

    @Test
    fun weatherVisualsCoverRainFogLightningSnowAndHighWind() {
        assertEquals(
            LauncherWeatherVisualKind.RAIN,
            launcherWeatherVisualKind(code = 63),
        )
        assertEquals(
            LauncherWeatherVisualKind.FOG,
            launcherWeatherVisualKind(code = 45),
        )
        assertEquals(
            LauncherWeatherVisualKind.THUNDERSTORM,
            launcherWeatherVisualKind(code = 95),
        )
        assertEquals(
            LauncherWeatherVisualKind.SNOW,
            launcherWeatherVisualKind(code = 75),
        )
        assertEquals(
            LauncherWeatherVisualKind.WIND,
            launcherWeatherVisualKind(
                code = 0,
                windSpeed = 32,
                windGust = 45,
                windUnit = "mph",
            ),
        )
        assertEquals(
            "High winds",
            launcherWeatherDisplayCondition(
                code = 1,
                windSpeed = 30,
                windGust = 44,
                windUnit = "mph",
            ),
        )
    }

}
