package com.goreecloud.launcher.core.launcher

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

enum class LauncherWeatherVisualKind {
    CLEAR,
    PARTLY_CLOUDY,
    CLOUDY,
    FOG,
    WIND,
    RAIN,
    SNOW,
    THUNDERSTORM,
    UNKNOWN,
}

data class LauncherWeatherSnapshot(
    val temperature: Int,
    val unit: String,
    val condition: String,
    val weatherCode: Int,
    val isDay: Boolean,
    val windSpeed: Int,
    val windGust: Int,
    val windUnit: String,
)

internal const val LAUNCHER_WEATHER_CACHE_TTL_MILLIS: Long = 15 * 60 * 1000L

internal fun launcherWeatherCacheIsFresh(
    cachedAtMillis: Long,
    nowMillis: Long,
    ttlMillis: Long = LAUNCHER_WEATHER_CACHE_TTL_MILLIS,
): Boolean =
    cachedAtMillis > 0L &&
        nowMillis >= cachedAtMillis &&
        nowMillis - cachedAtMillis <= ttlMillis

private data class LauncherWeatherCacheEntry(
    val snapshot: LauncherWeatherSnapshot,
    val cachedAtMillis: Long,
)

internal fun launcherWeatherCondition(code: Int): String = when (code) {
    0 -> "Clear"
    1 -> "Mostly clear"
    2 -> "Partly cloudy"
    3 -> "Cloudy"
    45, 48 -> "Fog"
    51, 53, 55, 56, 57 -> "Drizzle"
    61, 63, 65, 66, 67 -> "Rain"
    71, 73, 75, 77 -> "Snow"
    80, 81, 82 -> "Showers"
    85, 86 -> "Snow showers"
    95, 96, 99 -> "Thunderstorms"
    else -> "Conditions"
}

internal fun launcherWeatherVisualKind(
    code: Int,
    windSpeed: Int = 0,
    windGust: Int = 0,
    windUnit: String = "km/h",
): LauncherWeatherVisualKind {
    val highWind = launcherWeatherIsHighWind(windSpeed, windGust, windUnit)
    return when (code) {
        95, 96, 99 -> LauncherWeatherVisualKind.THUNDERSTORM
        71, 73, 75, 77, 85, 86 -> LauncherWeatherVisualKind.SNOW
        51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 ->
            LauncherWeatherVisualKind.RAIN
        45, 48 -> LauncherWeatherVisualKind.FOG
        0 -> if (highWind) LauncherWeatherVisualKind.WIND else LauncherWeatherVisualKind.CLEAR
        1, 2 -> if (highWind) LauncherWeatherVisualKind.WIND
            else LauncherWeatherVisualKind.PARTLY_CLOUDY
        3 -> if (highWind) LauncherWeatherVisualKind.WIND else LauncherWeatherVisualKind.CLOUDY
        else -> if (highWind) LauncherWeatherVisualKind.WIND else LauncherWeatherVisualKind.UNKNOWN
    }
}

internal fun launcherWeatherDisplayCondition(
    code: Int,
    windSpeed: Int,
    windGust: Int,
    windUnit: String,
): String {
    val base = launcherWeatherCondition(code)
    val drySky = code in 0..3 || code !in setOf(
        45, 48,
        51, 53, 55, 56, 57,
        61, 63, 65, 66, 67,
        71, 73, 75, 77,
        80, 81, 82,
        85, 86,
        95, 96, 99,
    )
    return if (drySky && launcherWeatherIsHighWind(windSpeed, windGust, windUnit)) {
        "High winds"
    } else {
        base
    }
}

internal fun launcherWeatherIsHighWind(
    windSpeed: Int,
    windGust: Int,
    windUnit: String,
): Boolean {
    val mph = windUnit.equals("mph", ignoreCase = true)
    val sustainedThreshold = if (mph) 28 else 45
    val gustThreshold = if (mph) 40 else 65
    return windSpeed >= sustainedThreshold || windGust >= gustThreshold
}

/**
 * Foreground-only weather for Launcher Glance.
 *
 * The UI requests location only after a deliberate user tap. Coordinates are never persisted.
 * Before the HTTPS request to Open-Meteo they are rounded to two decimals, which is sufficient for
 * local weather while avoiding unnecessary coordinate precision.
 */
object LauncherWeather {
    @Volatile
    private var cacheEntry: LauncherWeatherCacheEntry? = null

    fun cachedSnapshot(nowMillis: Long = System.currentTimeMillis()): LauncherWeatherSnapshot? =
        cacheEntry
            ?.takeIf { launcherWeatherCacheIsFresh(it.cachedAtMillis, nowMillis) }
            ?.snapshot

    fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED

    suspend fun load(
        context: Context,
        forceRefresh: Boolean = false,
    ): LauncherWeatherSnapshot? = withContext(Dispatchers.IO) {
        if (!hasLocationPermission(context)) return@withContext null
        if (!forceRefresh) {
            cachedSnapshot()?.let { return@withContext it }
        }
        val location = withTimeoutOrNull(6_000) { currentLocation(context) }
            ?: lastKnownLocation(context)
            ?: return@withContext null

        val useFahrenheit = Locale.getDefault().country.equals("US", ignoreCase = true)
        val unitQuery = if (useFahrenheit) "fahrenheit" else "celsius"
        val displayUnit = if (useFahrenheit) "°F" else "°C"
        val windUnitQuery = if (useFahrenheit) "mph" else "kmh"
        val displayWindUnit = if (useFahrenheit) "mph" else "km/h"
        val latitude = String.format(Locale.US, "%.2f", location.latitude)
        val longitude = String.format(Locale.US, "%.2f", location.longitude)
        val endpoint =
            "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$latitude&longitude=$longitude" +
                "&current=temperature_2m,weather_code,is_day,wind_speed_10m,wind_gusts_10m" +
                "&temperature_unit=$unitQuery&wind_speed_unit=$windUnitQuery&timezone=auto"

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5_000
            readTimeout = 5_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "GoreeCloud-Launcher/0.1")
        }
        try {
            if (connection.responseCode !in 200..299) return@withContext null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val current = JSONObject(body).optJSONObject("current") ?: return@withContext null
            val temperature = current.optDouble("temperature_2m", Double.NaN)
            if (!temperature.isFinite()) return@withContext null
            val weatherCode = current.optInt("weather_code", -1)
            val windSpeed = current.optDouble("wind_speed_10m", 0.0)
                .takeIf(Double::isFinite)
                ?.roundToInt()
                ?: 0
            val windGust = current.optDouble("wind_gusts_10m", 0.0)
                .takeIf(Double::isFinite)
                ?.roundToInt()
                ?: 0
            LauncherWeatherSnapshot(
                temperature = temperature.roundToInt(),
                unit = displayUnit,
                condition = launcherWeatherDisplayCondition(
                    code = weatherCode,
                    windSpeed = windSpeed,
                    windGust = windGust,
                    windUnit = displayWindUnit,
                ),
                weatherCode = weatherCode,
                isDay = current.optInt("is_day", 1) != 0,
                windSpeed = windSpeed,
                windGust = windGust,
                windUnit = displayWindUnit,
            ).also { snapshot ->
                cacheEntry = LauncherWeatherCacheEntry(
                    snapshot = snapshot,
                    cachedAtMillis = System.currentTimeMillis(),
                )
            }
        } finally {
            connection.disconnect()
        }
    }

    @Suppress("MissingPermission")
    private suspend fun currentLocation(context: Context): Location? {
        val manager = context.getSystemService(LocationManager::class.java)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return lastKnownLocation(manager)
        val provider = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
        ).firstOrNull { candidate ->
            runCatching { manager.isProviderEnabled(candidate) }.getOrDefault(false)
        } ?: return lastKnownLocation(manager)

        return suspendCancellableCoroutine { continuation ->
            val cancellation = CancellationSignal()
            continuation.invokeOnCancellation { cancellation.cancel() }
            runCatching {
                manager.getCurrentLocation(
                    provider,
                    cancellation,
                    context.mainExecutor,
                ) { location ->
                    if (continuation.isActive) continuation.resume(location)
                }
            }.onFailure {
                if (continuation.isActive) continuation.resume(lastKnownLocation(manager))
            }
        }
    }

    @Suppress("MissingPermission")
    private fun lastKnownLocation(context: Context): Location? =
        lastKnownLocation(context.getSystemService(LocationManager::class.java))

    @Suppress("MissingPermission")
    private fun lastKnownLocation(manager: LocationManager): Location? =
        listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        ).mapNotNull { provider ->
            runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
        }.maxByOrNull { it.time }
}
