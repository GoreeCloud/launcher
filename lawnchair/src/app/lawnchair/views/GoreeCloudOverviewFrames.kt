package app.lawnchair.views

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.view.View
import app.lawnchair.LawnchairLauncher
import java.util.WeakHashMap

/**
 * Glaze edit preview chrome around live Workspace pages and dock.
 * Never replaces the page with a screenshot: widgets, wallpaper and page state
 * remain authoritative. Preview decoration is noninteractive.
 */
object GoreeCloudOverviewFrames {
    private val overlays = WeakHashMap<LawnchairLauncher, View>()

    fun apply(host: LawnchairLauncher, editing: Boolean) {
        val overlay = overlays.getOrPut(host) {
            object : View(host) {
                private val origin = IntArray(2)
                private val page = Rect()
                private val dock = Rect()
                private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                override fun onDraw(canvas: Canvas) {
                    host.rootView.getLocationOnScreen(origin)
                    val workspace = host.workspace
                    val density = resources.displayMetrics.density
                    var selectedFrame: RectF? = null
                    for (index in 0 until workspace.childCount) {
                        val child = workspace.getPageAt(index) ?: continue
                        if (!child.getGlobalVisibleRect(page) || page.isEmpty) continue
                        val selected = index == workspace.currentPage
                        val rect = RectF(
                            page.left - origin[0] + density,
                            page.top - origin[1] + density,
                            page.right - origin[0] - density,
                            page.bottom - origin[1] - density,
                        )
                        if (selected && host.hotseat.getGlobalVisibleRect(dock)) {
                            rect.bottom = maxOf(rect.bottom, dock.bottom - origin[1] + 9f * density)
                        }
                        if (selected) selectedFrame = RectF(rect)
                        val radius = 24f * density
                        // Transparent cool-blue glass follows the selected wallpaper.
                        paint.style = Paint.Style.FILL
                        paint.color = if (selected) 0x1314CFF7 else 0x0DDBE9FF
                        canvas.drawRoundRect(rect, radius, radius, paint)
                        if (selected) {
                            paint.style = Paint.Style.STROKE
                            paint.strokeWidth = 5f * density
                            paint.color = 0x3571DCFF
                            canvas.drawRoundRect(rect, radius, radius, paint)
                        }
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = if (selected) 1.75f * density else 1.2f * density
                        paint.color = if (selected) Color.WHITE else 0x9DDAE8F5.toInt()
                        canvas.drawRoundRect(rect, radius, radius, paint)
                        paint.style = Paint.Style.FILL
                    }

                    // The selected first/last page has no real neighbor on one
                    // side. Draw a clearly subdued empty-page slot there, so the
                    // edit carousel never looks cut off. This is decoration only:
                    // do not create or reorder real Home pages on entering Edit.
                    val selected = selectedFrame
                    if (selected != null) {
                        fun emptyNeighbor(direction: Float) {
                            val ghost = RectF(selected)
                            ghost.offset(direction * (selected.width() + 14f * density), 0f)
                            val radius = 24f * density
                            paint.style = Paint.Style.FILL
                            paint.color = 0x263B83A5
                            canvas.drawRoundRect(ghost, radius, radius, paint)
                            paint.style = Paint.Style.STROKE
                            paint.strokeWidth = 1.3f * density
                            paint.color = 0xA7DDEAF5.toInt()
                            canvas.drawRoundRect(ghost, radius, radius, paint)
                            paint.style = Paint.Style.FILL
                        }
                        if (workspace.currentPage == 0) emptyNeighbor(-1f)
                        if (workspace.currentPage == workspace.childCount - 1) {
                            emptyNeighbor(1f)
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
}
