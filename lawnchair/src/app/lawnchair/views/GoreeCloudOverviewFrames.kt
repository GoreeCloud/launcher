package app.lawnchair.views

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import app.lawnchair.LawnchairLauncher
import java.util.WeakHashMap

/**
 * Glaze Home edit page frames. Foregrounds are strictly temporary:
 * all original page foregrounds are restored when leaving edit mode.
 */
object GoreeCloudOverviewFrames {
    private val originals = WeakHashMap<View, Drawable?>()

    fun apply(host: LawnchairLauncher, isEditing: Boolean) {
        if (!isEditing) {
            originals.forEach { (page, original) -> page.foreground = original }
            originals.clear()
            return
        }
        val workspace = host.workspace
        val density = host.resources.displayMetrics.density
        for (index in 0 until workspace.childCount) {
            val page = workspace.getPageAt(index) ?: continue
            if (!originals.containsKey(page)) {
                originals[page] = page.foreground
            }
            val selected = index == workspace.currentPage
            // Selected page is indicated by the page-strip chip. Avoid the
            // full-height white outline that overwhelmed the edit canvas.
            val border = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(if (selected) 0x070C5CBE else Color.TRANSPARENT)
                cornerRadius = 20f * density
            }
            page.foreground = border
        }
    }
}
