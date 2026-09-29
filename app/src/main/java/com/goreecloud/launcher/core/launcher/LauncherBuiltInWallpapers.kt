package com.goreecloud.launcher.core.launcher

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.max

enum class LauncherBuiltInWallpaperId {
    AURORA,
    HORIZON,
    NOCTURNE,
    CASCADE,
    SOLSTICE,
    TIDAL,
}

data class LauncherBuiltInWallpaper(
    val id: LauncherBuiltInWallpaperId,
    val name: String,
    val description: String,
    val startColor: Int,
    val middleColor: Int,
    val endColor: Int,
    val accentColor: Int,
    val secondaryAccentColor: Int,
)

object LauncherBuiltInWallpapers {
    val all: List<LauncherBuiltInWallpaper> = listOf(
        LauncherBuiltInWallpaper(
            id = LauncherBuiltInWallpaperId.AURORA,
            name = "Glaze Aurora",
            description = "Deep indigo with soft aqua and violet light.",
            startColor = 0xFF091026.toInt(),
            middleColor = 0xFF1C405C.toInt(),
            endColor = 0xFF231342.toInt(),
            accentColor = 0xFF5FE1DD.toInt(),
            secondaryAccentColor = 0xFFAA73FF.toInt(),
        ),
        LauncherBuiltInWallpaper(
            id = LauncherBuiltInWallpaperId.HORIZON,
            name = "Glaze Horizon",
            description = "Midnight blue fading into a warm GoreeCloud horizon.",
            startColor = 0xFF08172B.toInt(),
            middleColor = 0xFF214A69.toInt(),
            endColor = 0xFF512A44.toInt(),
            accentColor = 0xFF67D6EA.toInt(),
            secondaryAccentColor = 0xFFFF9C7E.toInt(),
        ),
        LauncherBuiltInWallpaper(
            id = LauncherBuiltInWallpaperId.NOCTURNE,
            name = "Glaze Nocturne",
            description = "Near-black depth with restrained cobalt and teal bloom.",
            startColor = 0xFF030812.toInt(),
            middleColor = 0xFF0C1C30.toInt(),
            endColor = 0xFF040F1B.toInt(),
            accentColor = 0xFF3786FF.toInt(),
            secondaryAccentColor = 0xFF37DEC5.toInt(),
        ),
        LauncherBuiltInWallpaper(
            id = LauncherBuiltInWallpaperId.CASCADE,
            name = "Glaze Cascade",
            description = "Cool slate layers with emerald and sky-blue highlights.",
            startColor = 0xFF0C1A22.toInt(),
            middleColor = 0xFF193942.toInt(),
            endColor = 0xFF0F243A.toInt(),
            accentColor = 0xFF44DCAC.toInt(),
            secondaryAccentColor = 0xFF59A7FF.toInt(),
        ),
        LauncherBuiltInWallpaper(
            id = LauncherBuiltInWallpaperId.SOLSTICE,
            name = "Glaze Solstice",
            description = "Dawn blue, sea-glass and lilac with a calm, luminous finish.",
            startColor = 0xFF24445E.toInt(),
            middleColor = 0xFF3C6F82.toInt(),
            endColor = 0xFF5F5685.toInt(),
            accentColor = 0xFF8FCFCA.toInt(),
            secondaryAccentColor = 0xFFD1B5EA.toInt(),
        ),
        LauncherBuiltInWallpaper(
            id = LauncherBuiltInWallpaperId.TIDAL,
            name = "Glaze Tidal",
            description = "Deep ocean teal with jade and restrained turquoise highlights.",
            startColor = 0xFF092A33.toInt(),
            middleColor = 0xFF13505A.toInt(),
            endColor = 0xFF102C46.toInt(),
            accentColor = 0xFF5CE2C3.toInt(),
            secondaryAccentColor = 0xFF4DBAC8.toInt(),
        ),
    )

    fun find(id: LauncherBuiltInWallpaperId): LauncherBuiltInWallpaper =
        checkNotNull(all.firstOrNull { it.id == id })

    fun render(
        id: LauncherBuiltInWallpaperId,
        width: Int,
        height: Int,
    ): Bitmap {
        require(width > 0 && height > 0)
        val wallpaper = find(id)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.shader = LinearGradient(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            intArrayOf(
                wallpaper.startColor,
                wallpaper.middleColor,
                wallpaper.endColor,
            ),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        val longSide = max(width, height).toFloat()
        drawBloom(
            canvas = canvas,
            paint = paint,
            centerX = width * 0.18f,
            centerY = height * 0.22f,
            radius = longSide * 0.46f,
            color = wallpaper.accentColor,
            alpha = 116,
        )
        drawBloom(
            canvas = canvas,
            paint = paint,
            centerX = width * 0.88f,
            centerY = height * 0.62f,
            radius = longSide * 0.52f,
            color = wallpaper.secondaryAccentColor,
            alpha = 98,
        )
        drawBloom(
            canvas = canvas,
            paint = paint,
            centerX = width * 0.40f,
            centerY = height * 0.92f,
            radius = longSide * 0.38f,
            color = Color.WHITE,
            alpha = 28,
        )

        paint.shader = null
        paint.color = Color.argb(20, 255, 255, 255)
        canvas.drawCircle(width * 0.76f, height * 0.18f, width * 0.16f, paint)
        paint.color = Color.argb(12, 255, 255, 255)
        canvas.drawCircle(width * 0.16f, height * 0.72f, width * 0.24f, paint)

        return bitmap
    }

    private fun drawBloom(
        canvas: Canvas,
        paint: Paint,
        centerX: Float,
        centerY: Float,
        radius: Float,
        color: Int,
        alpha: Int,
    ) {
        paint.shader = RadialGradient(
            centerX,
            centerY,
            radius,
            intArrayOf(
                Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color)),
                Color.argb(0, Color.red(color), Color.green(color), Color.blue(color)),
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(centerX, centerY, radius, paint)
    }
}
