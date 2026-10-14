package app.lawnchair.views

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.View
import app.lawnchair.LawnchairLauncher
import java.util.WeakHashMap

/**
 * Frame around the ACTUAL scaled CellLayout while editing Home.
 *
 * The wallpaper, widgets, icons and adjacent Workspace pages remain live.
 * Foregrounds are restored to their original values when edit mode closes.
 */
object GoreeCloudOverviewFrames {
    private val originals = WeakHashMap<View, Drawable?>()

    fun apply(host: LawnchairLauncher, editing: Boolean) {
        if (!editing) {
            originals.forEach { (page, old) -> page.foreground = old }
            originals.clear()
            return
        }
        val workspace = host.workspace
        val density = host.resources.displayMetrics.density
        for (index in 0 until workspace.childCount) {
            val page = workspace.getPageAt(index) ?: continue
            if (!originals.containsKey(page)) originals[page] = page.foreground
            page.foreground = PageChrome(
                density = density,
                selected = index == workspace.currentPage,
            )
        }
    }

    /** Semi-translucent page toolbar with a drag motif and a distinct delete affordance. */
    private class PageChrome(
        private val density: Float,
        private val selected: Boolean,
    ) : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private fun px(dp: Float) = dp * density
        override fun draw(canvas: Canvas) {
            val b = bounds
            if (b.isEmpty) return
            val rect = RectF(
                b.left + px(1.5f), b.top + px(1.5f),
                b.right - px(1.5f), b.bottom - px(1.5f),
            )
            val radius = px(25f)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = px(if (selected) 2f else 1.25f)
            paint.color = if (selected) Color.WHITE else 0x9DFFFFFF.toInt()
            canvas.drawRoundRect(rect, radius, radius, paint)
            if (!selected) return

            val headerBottom = rect.top + px(52f)
            canvas.save()
            val rounded = android.graphics.Path().apply {
                addRoundRect(rect, radius, radius, android.graphics.Path.Direction.CW)
            }
            canvas.clipPath(rounded)
            paint.style = Paint.Style.FILL
            paint.color = 0x2CFFFFFF
            canvas.drawRect(rect.left, rect.top, rect.right, headerBottom, paint)
            paint.color = 0xDEFFFFFF.toInt()
            canvas.drawRect(rect.left, headerBottom - px(1f), rect.right, headerBottom, paint)
            val trashLeft = rect.right - px(64f)
            paint.color = 0x63FFFFFF
            canvas.drawRect(trashLeft, rect.top, rect.right, headerBottom, paint)
            paint.color = 0x9DFFFFFF.toInt()
            canvas.drawRect(trashLeft, rect.top, trashLeft + px(1f), headerBottom, paint)
            canvas.restore()

            // 3x3 light dots echoing the draggable page grid in the reference.
            paint.style = Paint.Style.FILL
            paint.color = 0xE8FFFFFF.toInt()
            val cx = (rect.left + trashLeft) / 2f
            val cy = (rect.top + headerBottom) / 2f
            for (row in -1..1) for (col in -1..1) {
                canvas.drawCircle(
                    cx + col * px(7f), cy + row * px(7f), px(2.6f), paint,
                )
            }
            // Trash glyph in rose red: functional tap target is installed above the page.
            val tx = (trashLeft + rect.right) / 2f
            paint.color = 0xFFFF5276.toInt()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = px(2.2f)
            paint.strokeCap = Paint.Cap.ROUND
            canvas.drawRoundRect(
                tx - px(9f), cy - px(7f), tx + px(9f), cy + px(11f),
                px(2f), px(2f), paint,
            )
            canvas.drawLine(tx - px(12f), cy - px(11f), tx + px(12f), cy - px(11f), paint)
            canvas.drawLine(tx - px(4f), cy - px(15f), tx + px(4f), cy - px(15f), paint)
            canvas.drawLine(tx - px(3f), cy - px(3f), tx - px(3f), cy + px(7f), paint)
            canvas.drawLine(tx + px(3f), cy - px(3f), tx + px(3f), cy + px(7f), paint)
            paint.style = Paint.Style.FILL
        }

        override fun setAlpha(alpha: Int) { paint.alpha = alpha }
        override fun setColorFilter(filter: ColorFilter?) { paint.colorFilter = filter }
        @Deprecated("Android Drawable opacity contract")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }
}
