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
            val border = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(if (selected) 0x160C5CBE else 0x090C5CBE)
                cornerRadius = 24f * density
                setStroke(
                    (if (selected) 2f else 1f).times(density).toInt().coerceAtLeast(1),
                    if (selected) Color.WHITE else 0xA6FFFFFF.toInt(),
                )
            }
            page.foreground = border
        }
    }
}
