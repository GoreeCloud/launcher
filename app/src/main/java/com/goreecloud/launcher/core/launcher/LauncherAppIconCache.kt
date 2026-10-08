package com.goreecloud.launcher.core.launcher

import android.content.ComponentName
import android.content.pm.LauncherActivityInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.UserHandle
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

internal const val LAUNCHER_ICON_DECODE_SIZE_PX = 144
internal const val LAUNCHER_ICON_CACHE_MAX_KIB = 12 * 1024
internal const val LAUNCHER_ICON_STALE_CACHE_MAX_KIB = 4 * 1024
internal const val LAUNCHER_ICON_PRELOAD_COUNT = 128
internal const val LAUNCHER_ICON_PRELOAD_PARALLELISM = 3
internal const val LAUNCHER_ADAPTIVE_FOREGROUND_OVERSCAN_FRACTION = 0.14f
internal const val LAUNCHER_LEGACY_ICON_NORMALIZE_THRESHOLD = 0.84f
internal const val LAUNCHER_LEGACY_ICON_TARGET_FRACTION = 0.90f
internal const val LAUNCHER_LEGACY_ICON_MAX_SCALE = 1.24f

internal data class LauncherIconCacheKey(
    val user: UserHandle,
    val componentName: ComponentName,
)

private data class LauncherIconPackageKey(
    val user: UserHandle,
    val packageName: String,
)

internal data class LauncherIconCacheStamp(
    val generation: Long,
    val packageGeneration: Long,
)

private data class LauncherIconLoadKey(
    val cacheKey: LauncherIconCacheKey,
    val stamp: LauncherIconCacheStamp,
)

internal fun <T> firstSuccessfulIconLoad(vararg loaders: () -> T?): T? {
    loaders.forEach { loader ->
        val value = runCatching(loader).getOrNull()
        if (value != null) return value
    }
    return null
}

/**
 * Normalizes Android launcher artwork into a square, mask-ready bitmap.
 *
 * Adaptive icons are flattened without the platform-selected mask and receive bounded foreground
 * overscan so artwork fills the Launcher mask. Legacy artwork is enlarged only when its alpha
 * bounds are unusually padded, with a safety cap that prevents destructive over-cropping.
 */
internal fun launcherLegacyIconNormalizationScale(
    contentWidth: Int,
    contentHeight: Int,
    canvasSize: Int,
): Float {
    if (contentWidth <= 0 || contentHeight <= 0 || canvasSize <= 0) return 1f
    val occupiedFraction = max(contentWidth, contentHeight).toFloat() / canvasSize.toFloat()
    if (occupiedFraction >= LAUNCHER_LEGACY_ICON_NORMALIZE_THRESHOLD) return 1f
    return (LAUNCHER_LEGACY_ICON_TARGET_FRACTION / occupiedFraction)
        .coerceIn(1f, LAUNCHER_LEGACY_ICON_MAX_SCALE)
}

private fun drawDrawableSquare(
    drawable: Drawable,
    sizePx: Int,
): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val copy = drawable.constantState?.newDrawable()?.mutate() ?: drawable.mutate()
    copy.setBounds(0, 0, sizePx, sizePx)
    copy.draw(canvas)
    return bitmap
}

private fun alphaBounds(bitmap: Bitmap): Rect? {
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(
        pixels,
        0,
        width,
        0,
        0,
        width,
        height,
    )

    var left = width
    var top = height
    var right = -1
    var bottom = -1
    for (y in 0 until height) {
        val rowOffset = y * width
        for (x in 0 until width) {
            if ((pixels[rowOffset + x] ushr 24) > 8) {
                if (x < left) left = x
                if (x > right) right = x
                if (y < top) top = y
                if (y > bottom) bottom = y
            }
        }
    }
    return if (right < left || bottom < top) null else Rect(left, top, right + 1, bottom + 1)
}

private fun normalizeLegacyIconBitmap(bitmap: Bitmap): Bitmap {
    val bounds = alphaBounds(bitmap) ?: return bitmap
    val scale = launcherLegacyIconNormalizationScale(
        contentWidth = bounds.width(),
        contentHeight = bounds.height(),
        canvasSize = bitmap.width.coerceAtMost(bitmap.height),
    )
    if (scale <= 1.001f) return bitmap

    val destinationWidth = (bounds.width() * scale).coerceAtMost(bitmap.width.toFloat())
    val destinationHeight = (bounds.height() * scale).coerceAtMost(bitmap.height.toFloat())
    val left = (bitmap.width - destinationWidth) / 2f
    val top = (bitmap.height - destinationHeight) / 2f
    return Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888).also { result ->
        Canvas(result).drawBitmap(
            bitmap,
            bounds,
            RectF(left, top, left + destinationWidth, top + destinationHeight),
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
        )
    }
}

internal fun renderLauncherMaskReadyBitmap(
    drawable: Drawable,
    sizePx: Int,
): Bitmap? {
    if (sizePx <= 0) return null
    val adaptive = drawable as? AdaptiveIconDrawable
    if (adaptive == null) {
        return normalizeLegacyIconBitmap(drawDrawableSquare(drawable, sizePx))
    }

    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    fun Drawable.render(left: Int, top: Int, right: Int, bottom: Int) {
        val copy = constantState?.newDrawable()?.mutate() ?: mutate()
        copy.setBounds(left, top, right, bottom)
        copy.draw(canvas)
    }

    adaptive.background.render(0, 0, sizePx, sizePx)
    val overscan = (sizePx * LAUNCHER_ADAPTIVE_FOREGROUND_OVERSCAN_FRACTION).roundToInt()
    adaptive.foreground.render(-overscan, -overscan, sizePx + overscan, sizePx + overscan)
    return bitmap
}

internal class LauncherIconSingleFlightLoader<K : Any, V>(
    private val scope: CoroutineScope,
) {
    private val inFlight = ConcurrentHashMap<K, CompletableDeferred<V>>()

    suspend fun load(key: K, block: suspend () -> V): V {
        val candidate = CompletableDeferred<V>()
        val existing = inFlight.putIfAbsent(key, candidate)
        if (existing != null) return existing.await()

        scope.launch {
            try {
                candidate.complete(block())
            } catch (failure: Throwable) {
                candidate.completeExceptionally(failure)
            } finally {
                inFlight.remove(key, candidate)
            }
        }
        return candidate.await()
    }
}

/**
 * Owns the single replaceable background icon-preload job.
 *
 * Inventory snapshots can change again while an older warm pass is still walking its bounded tail.
 * Replacing that pass prevents stale background work from continuing to schedule additional icon
 * loads. Already-started single-flight decodes remain safe: callers for the newest snapshot can join
 * them, while cache stamps still reject results made stale by package/profile invalidation.
 */
internal class LauncherLatestPreloadRunner(
    private val scope: CoroutineScope,
) {
    private val stateLock = Any()
    private var generation = 0L
    private var currentJob: Job? = null

    fun replace(block: suspend CoroutineScope.() -> Unit) {
        val (ticket, previousJob) = synchronized(stateLock) {
            generation += 1L
            val previous = currentJob
            currentJob = null
            generation to previous
        }
        previousJob?.cancel()

        val candidate = scope.launch(start = CoroutineStart.LAZY, block = block)
        val accepted = synchronized(stateLock) {
            if (ticket != generation) {
                false
            } else {
                currentJob = candidate
                true
            }
        }
        if (!accepted) {
            candidate.cancel()
            return
        }

        candidate.invokeOnCompletion {
            synchronized(stateLock) {
                if (currentJob === candidate) {
                    currentJob = null
                }
            }
        }
        candidate.start()
    }

    fun cancel() {
        val job = synchronized(stateLock) {
            generation += 1L
            currentJob.also { currentJob = null }
        }
        job?.cancel()
    }
}

/**
 * Bounded process-local cache for Android-provided badged launcher icons.
 *
 * LauncherApps remains inventory authority. This cache owns presentation bitmaps only; nothing is
 * persisted and package invalidation makes stale decode work unable to re-enter the cache.
 */
internal object LauncherAppIconCache {
    private val stateLock = Any()
    private var generation = 0L
    private val packageGenerations = mutableMapOf<LauncherIconPackageKey, Long>()
    private val loadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val singleFlight = LauncherIconSingleFlightLoader<LauncherIconLoadKey, Bitmap?>(loadScope)
    private val preloadRunner = LauncherLatestPreloadRunner(loadScope)

    private val cache = object : LruCache<LauncherIconCacheKey, Bitmap>(LAUNCHER_ICON_CACHE_MAX_KIB) {
        override fun sizeOf(key: LauncherIconCacheKey, value: Bitmap): Int =
            bitmapSizeKib(value)
    }

    /**
     * Short-lived stale-while-revalidate fallback.
     *
     * Package/profile refreshes should not flash a generic placeholder while Android's replacement
     * icon is being decoded. Stale entries are process-local, bounded, and removed as soon as a
     * fresh authoritative icon is available.
     */
    private val staleCache =
        object : LruCache<LauncherIconCacheKey, Bitmap>(LAUNCHER_ICON_STALE_CACHE_MAX_KIB) {
            override fun sizeOf(key: LauncherIconCacheKey, value: Bitmap): Int =
                bitmapSizeKib(value)
        }

    fun peek(app: LauncherActivityInfo): Bitmap? = synchronized(stateLock) {
        val key = app.cacheKey()
        cache.get(key) ?: staleCache.get(key)
    }

    /**
     * Stable presentation identity for Compose icon state.
     *
     * The stamp changes only when the complete icon cache or this app's package scope is invalidated,
     * so ordinary LauncherApps snapshot object churn does not discard an already-warm icon.
     */
    fun stamp(app: LauncherActivityInfo): LauncherIconCacheStamp = synchronized(stateLock) {
        stampFor(app)
    }

    fun preload(
        apps: List<LauncherActivityInfo>,
        packageManager: PackageManager? = null,
        maxCount: Int = LAUNCHER_ICON_PRELOAD_COUNT,
    ) {
        if (maxCount <= 0) {
            preloadRunner.cancel()
            return
        }

        // Every new authoritative inventory snapshot replaces the older bounded warm pass. Candidate
        // selection stays off the UI thread, and the existing single-flight loader still coalesces any
        // decode already in progress when a newer snapshot requests the same icon.
        preloadRunner.replace {
            val candidates = apps.asSequence()
                .distinctBy { app -> app.cacheKey() }
                .take(maxCount)
                .filter { app -> peek(app) == null }
                .toList()
            if (candidates.isEmpty()) return@replace

            candidates.chunked(LAUNCHER_ICON_PRELOAD_PARALLELISM).forEach { batch ->
                batch.map { app ->
                    async { load(app, packageManager) }
                }.awaitAll()
            }
        }
    }

    suspend fun load(
        app: LauncherActivityInfo,
        packageManager: PackageManager? = null,
    ): Bitmap? {
        val key = app.cacheKey()
        val requestedStamp = synchronized(stateLock) {
            cache.get(key)?.let { return it }
            stampFor(app)
        }
        val loadKey = LauncherIconLoadKey(key, requestedStamp)

        return singleFlight.load(loadKey) {
            val cachedAfterClaim = synchronized(stateLock) {
                if (stampFor(app) == requestedStamp) cache.get(key) else null
            }
            if (cachedAfterClaim != null) {
                cachedAfterClaim
            } else if (!isCurrentStamp(app, requestedStamp)) {
                null
            } else {
                val decoded = firstSuccessfulIconLoad(
                    {
                        renderLauncherMaskReadyBitmap(
                            drawable = app.getIcon(0),
                            sizePx = LAUNCHER_ICON_DECODE_SIZE_PX,
                        )?.let { maskReady ->
                            if (packageManager == null) {
                                maskReady
                            } else {
                                packageManager.getUserBadgedIcon(
                                    BitmapDrawable(Resources.getSystem(), maskReady),
                                    app.user,
                                ).toBitmap(
                                    width = LAUNCHER_ICON_DECODE_SIZE_PX,
                                    height = LAUNCHER_ICON_DECODE_SIZE_PX,
                                )
                            }
                        }
                    },
                    {
                        app.getBadgedIcon(0).toBitmap(
                            width = LAUNCHER_ICON_DECODE_SIZE_PX,
                            height = LAUNCHER_ICON_DECODE_SIZE_PX,
                        )
                    },
                    {
                        // Some OEM/activity resources intermittently fail through the badged path.
                        // Re-badge the activity icon when PackageManager is available so work or
                        // secondary-profile identity is preserved on this fallback as well.
                        app.getIcon(0).let { drawable ->
                            packageManager?.getUserBadgedIcon(drawable, app.user) ?: drawable
                        }.toBitmap(
                            width = LAUNCHER_ICON_DECODE_SIZE_PX,
                            height = LAUNCHER_ICON_DECODE_SIZE_PX,
                        )
                    },
                    {
                        // PackageManager can still expose the activity resource when the direct
                        // LauncherActivityInfo resource path is temporarily unavailable.
                        packageManager?.let { manager ->
                            manager.getUserBadgedIcon(
                                manager.getActivityIcon(app.componentName),
                                app.user,
                            ).toBitmap(
                                width = LAUNCHER_ICON_DECODE_SIZE_PX,
                                height = LAUNCHER_ICON_DECODE_SIZE_PX,
                            )
                        }
                    },
                    {
                        // A small number of vendor launch activities expose a broken activity icon
                        // while their application icon remains valid.
                        packageManager?.let { manager ->
                            manager.getUserBadgedIcon(
                                app.applicationInfo.loadIcon(manager),
                                app.user,
                            ).toBitmap(
                                width = LAUNCHER_ICON_DECODE_SIZE_PX,
                                height = LAUNCHER_ICON_DECODE_SIZE_PX,
                            )
                        }
                    },
                    {
                        // Final Android-owned fail-soft presentation fallback. This prevents a
                        // blank drawer slot when every app-owned icon resource is unreadable.
                        packageManager?.let { manager ->
                            manager.getUserBadgedIcon(
                                manager.defaultActivityIcon,
                                app.user,
                            ).toBitmap(
                                width = LAUNCHER_ICON_DECODE_SIZE_PX,
                                height = LAUNCHER_ICON_DECODE_SIZE_PX,
                            )
                        }
                    },
                )

                synchronized(stateLock) {
                    if (stampFor(app) != requestedStamp) {
                        staleCache.get(key)
                    } else if (decoded == null) {
                        staleCache.get(key)
                    } else {
                        cache.get(key) ?: decoded.also {
                            cache.put(key, it)
                            staleCache.remove(key)
                        }
                    }
                }
            }
        }
    }

    fun invalidatePackage(packageName: String, user: UserHandle) = synchronized(stateLock) {
        val packageKey = LauncherIconPackageKey(user, packageName)
        packageGenerations[packageKey] = (packageGenerations[packageKey] ?: 0L) + 1L
        cache.snapshot().keys
            .filter { key -> key.user == user && key.componentName.packageName == packageName }
            .forEach { key ->
                cache.get(key)?.let { staleCache.put(key, it) }
                cache.remove(key)
            }
    }

    fun clear() {
        preloadRunner.cancel()
        synchronized(stateLock) {
            generation += 1L
            packageGenerations.clear()
            cache.evictAll()
            staleCache.evictAll()
        }
    }

    private fun isCurrentStamp(
        app: LauncherActivityInfo,
        expected: LauncherIconCacheStamp,
    ): Boolean = synchronized(stateLock) {
        stampFor(app) == expected
    }

    private fun stampFor(app: LauncherActivityInfo): LauncherIconCacheStamp =
        LauncherIconCacheStamp(
            generation = generation,
            packageGeneration = packageGenerations[
                LauncherIconPackageKey(app.user, app.componentName.packageName)
            ] ?: 0L,
        )

    private fun LauncherActivityInfo.cacheKey(): LauncherIconCacheKey =
        LauncherIconCacheKey(user = user, componentName = componentName)

    private fun bitmapSizeKib(value: Bitmap): Int =
        ((value.allocationByteCount.toLong() + 1023L) / 1024L)
            .coerceAtLeast(1L)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
}
