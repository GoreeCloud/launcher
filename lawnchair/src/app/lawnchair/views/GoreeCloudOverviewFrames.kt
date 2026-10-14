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
    private val overlays = WeakHashMap<LawnchairLauncher, View>()

    /** Paint outside the CellLayout so the border can enclose the scaled dock. */
    fun apply(host: LawnchairLauncher, editing: Boolean) {
        val overlay = overlays.getOrPut(host) {
            object : View(host) {
                private val origin = IntArray(2)
                private val pageRect = android.graphics.Rect()
                private val dockRect = android.graphics.Rect()
                override fun onDraw(canvas: Canvas) {
                    super.onDraw(canvas)
                    host.rootView.getLocationOnScreen(origin)
                    val ws = host.workspace
                    val density = resources.displayMetrics.density
                    for (i in 0 until ws.childCount) {
                        val page = ws.getPageAt(i) ?: continue
                        if (!page.getGlobalVisibleRect(pageRect)) continue
                        val selected = i == ws.currentPage
                        val left = pageRect.left - origin[0]
                        val top = pageRect.top - origin[1]
                        val right = pageRect.right - origin[0]
                        var bottom = pageRect.bottom - origin[1]
                        if (selected && host.hotseat.getGlobalVisibleRect(dockRect)) {
                            bottom = maxOf(bottom, dockRect.bottom - origin[1] + (12 * density).toInt())
                        }
                        PageChrome(density, selected).apply {
                            setBounds(left, top, right, bottom)
                            draw(canvas)
                        }
                    }
                }
            }.apply {
                tag = "goreecloud_edit_preview_chrome"
                isClickable = false
                isFocusable = false
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                setWillNotDraw(false)
                visibility = View.GONE
                host.rootView.addView(
                    this,
                    com.android.launcher3.InsettableFrameLayout.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    ).apply { ignoreInsets = true },
                )
            }
        }
        overlay.visibility = if (editing) View.VISIBLE else View.GONE
        if (editing) {
            overlay.invalidate()
            overlay.post { overlay.invalidate() }
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
