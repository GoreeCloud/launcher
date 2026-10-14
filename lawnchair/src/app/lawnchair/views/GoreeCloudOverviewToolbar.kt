package app.lawnchair.views

import android.graphics.drawable.GradientDrawable
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

/** Compact editor header, with page controls contained inside a Glaze surface. */
object GoreeCloudOverviewToolbar {
    fun install(host: LawnchairLauncher) {
        fun dp(n: Int) = (n * host.resources.displayMetrics.density + .5f).toInt()
        fun surface(color: Int, radius: Int) = GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
        val row = LinearLayout(host).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            background = surface(0xFFF8FBFF.toInt(), 22).apply {
                setStroke(dp(1), 0xFFD7E6F2.toInt())
            }
            setPadding(dp(15), dp(6), dp(7), dp(6))
            elevation = dp(6).toFloat()
            visibility = View.GONE
            tag = "goreecloud_edit_toolbar"
        }
        row.addView(TextView(host).apply {
            text = "Edit Home"
            textSize = 14f
            setTextColor(0xFF233F58.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
        }, LinearLayout.LayoutParams(0, -1, 1f))
        fun action(label: String, prominent: Boolean = false, run: () -> Unit) {
            val width = if (label == "Remove") 72 else 59
            row.addView(TextView(host).apply {
                text = label
                textSize = 12f
                setTextColor(if (prominent) 0xFFFFFFFF.toInt() else 0xFF24506E.toInt())
                gravity = Gravity.CENTER
                background = surface(if (prominent) 0xFF006DAF.toInt()
                    else 0xFFE8F2FA.toInt(), 16)
                isFocusable = true
                isClickable = true
                contentDescription = label + " Home edit action"
                setOnClickListener { run() }
            }, LinearLayout.LayoutParams(dp(width), dp(37)).apply { marginStart = dp(5) })
        }
        action("Home") {
            val ws = host.workspace
            val groups = ws.reorderablePageGroupStarts
            for (i in 0 until groups.size()) {
                if (groups[i] == ws.currentPage) {
                    ws.setDefaultPageForReorderableGroup(i)
                    break
                }
            }
            host.findViewById<EditModePageStrip>(R.id.edit_mode_page_strip)?.refreshItems()
        }
        action("Remove") {
            val ws = host.workspace
            if (!ws.removeEmptyWorkspacePageByIndex(ws.currentPage)) {
                Toast.makeText(host, "This page has apps or widgets", Toast.LENGTH_SHORT).show()
            }
            host.findViewById<EditModePageStrip>(R.id.edit_mode_page_strip)?.refreshItems()
        }
        action("Done", prominent = true) {
            host.stateManager.goToState(LauncherState.NORMAL)
        }
        host.rootView.addView(row, FrameLayout.LayoutParams(
            -1, dp(54), Gravity.TOP or Gravity.CENTER_HORIZONTAL
        ).apply {
            leftMargin = dp(16)
            rightMargin = dp(16)
            topMargin = dp(54)
        })
        host.stateManager.addStateListener(object : StateManager.StateListener<LauncherState> {
            override fun onStateTransitionStart(toState: LauncherState) {
                row.visibility = if (toState == LauncherState.EDIT_MODE) View.VISIBLE else View.GONE
                GoreeCloudOverviewFrames.apply(host, toState == LauncherState.EDIT_MODE)
            }
            override fun onStateTransitionComplete(finalState: LauncherState) {
                row.visibility = if (finalState == LauncherState.EDIT_MODE) View.VISIBLE else View.GONE
                GoreeCloudOverviewFrames.apply(host, finalState == LauncherState.EDIT_MODE)
            }
        })
    }
}
