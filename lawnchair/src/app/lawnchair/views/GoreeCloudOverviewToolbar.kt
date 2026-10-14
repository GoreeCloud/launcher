package app.lawnchair.views

import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import app.lawnchair.LawnchairLauncher
import com.android.launcher3.LauncherState
import com.android.launcher3.R
import com.android.launcher3.statemanager.StateManager

object GoreeCloudOverviewToolbar {
    fun install(host: LawnchairLauncher) {
        fun dp(v: Int) = (v * host.resources.displayMetrics.density + .5f).toInt()
        val row = LinearLayout(host).apply { gravity = Gravity.CENTER; visibility = View.GONE }
        fun item(glyph: String, description: String, click: () -> Unit) {
            row.addView(TextView(host).apply {
                text = glyph; textSize = 26f
                gravity = Gravity.CENTER; setTextColor(if (description == "Remove empty Home page") 0xFFFF6480.toInt() else 0xFFFFFFFF.toInt())
                contentDescription = description; isFocusable = true
                setOnClickListener { click() }
            }, LinearLayout.LayoutParams(dp(64), dp(52)))
        }
        item("⌂", "Set Home page") {
            val ws = host.workspace; val pages = ws.reorderablePageGroupStarts
            for (i in 0 until pages.size()) if (pages[i] == ws.currentPage) {
                ws.setDefaultPageForReorderableGroup(i); break
            }
            host.findViewById<EditModePageStrip>(R.id.edit_mode_page_strip)?.refreshItems()
        }
        item("▤", "Remove empty Home page") {
            val ws = host.workspace
            if (!ws.removeEmptyWorkspacePageByIndex(ws.currentPage)) {
                Toast.makeText(host, "Move Home items first", Toast.LENGTH_SHORT).show()
            }
            host.findViewById<EditModePageStrip>(R.id.edit_mode_page_strip)?.refreshItems()
        }
        item("✓", "Done") { host.stateManager.goToState(LauncherState.NORMAL) }
        host.dragLayer.addView(row, FrameLayout.LayoutParams(
            dp(210), dp(52), Gravity.TOP or Gravity.CENTER_HORIZONTAL
        ).apply { topMargin = dp(30) })
        host.stateManager.addStateListener(object : StateManager.StateListener<LauncherState> {
            override fun onStateTransitionStart(toState: LauncherState) {
                row.visibility = if (toState == LauncherState.EDIT_MODE) View.VISIBLE else View.GONE
            }
            override fun onStateTransitionComplete(finalState: LauncherState) {
                row.visibility = if (finalState == LauncherState.EDIT_MODE) View.VISIBLE else View.GONE
            }
        })
    }
}
