package app.lawnchair.views

import android.graphics.Rect
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import app.lawnchair.LawnchairLauncher
import com.android.launcher3.LauncherState
import com.android.launcher3.R
import com.android.launcher3.statemanager.StateManager
import java.util.WeakHashMap

/** Functional controls over the real scaled Home-page preview. */
object GoreeCloudOverviewToolbar {
    private data class Controls(val home: View, val remove: View)
    private val controls = WeakHashMap<LawnchairLauncher, Controls>()

    fun install(host: LawnchairLauncher) {
        fun dp(n: Int) = (n * host.resources.displayMetrics.density + .5f).toInt()
        val home = ImageView(host).apply {
            setImageResource(R.drawable.goreecloud_edit_home)
            contentDescription = "Set current page as Home"
            isClickable = true
            isFocusable = true
            setOnClickListener {
                val workspace = host.workspace
                val groups = workspace.reorderablePageGroupStarts
                for (i in 0 until groups.size()) {
                    if (groups[i] == workspace.currentPage) {
                        workspace.setDefaultPageForReorderableGroup(i)
                        host.findViewById<EditModePageStrip>(R.id.edit_mode_page_strip)?.refreshItems()
                        break
                    }
                }
            }
        }
        val remove = View(host).apply {
            contentDescription = "Remove current empty Home screen"
            isClickable = true
            isFocusable = true
            setOnClickListener {
                val removed = host.workspace.removeEmptyWorkspacePageByIndex(host.workspace.currentPage)
                if (!removed) {
                    Toast.makeText(host, "Move icons and widgets before removing this page", Toast.LENGTH_SHORT).show()
                }
                host.findViewById<EditModePageStrip>(R.id.edit_mode_page_strip)?.refreshItems()
                host.rootView.post { refresh(host) }
            }
        }
        host.rootView.addView(home,
            com.android.launcher3.InsettableFrameLayout.LayoutParams(dp(36), dp(36)).apply {
                ignoreInsets = true
            },
        )
        host.rootView.addView(remove,
            com.android.launcher3.InsettableFrameLayout.LayoutParams(dp(64), dp(52)).apply {
                ignoreInsets = true
            },
        )
        home.visibility = View.GONE
        remove.visibility = View.GONE
        controls[host] = Controls(home, remove)
        host.stateManager.addStateListener(object : StateManager.StateListener<LauncherState> {
            override fun onStateTransitionStart(toState: LauncherState) {
                val editing = toState == LauncherState.EDIT_MODE
                GoreeCloudOverviewFrames.apply(host, editing)
                if (!editing) hide(host)
            }
            override fun onStateTransitionComplete(finalState: LauncherState) {
                val editing = finalState == LauncherState.EDIT_MODE
                GoreeCloudOverviewFrames.apply(host, editing)
                refresh(host)
                if (editing) host.rootView.post { refresh(host) }
            }
        })
    }
    private fun hide(host: LawnchairLauncher) {
        controls[host]?.let {
            it.home.visibility = View.GONE
            it.remove.visibility = View.GONE
        }
    }

    fun refresh(host: LawnchairLauncher) {
        val overlay = controls[host] ?: return
        if (!host.isInState(LauncherState.EDIT_MODE)) {
            hide(host)
            return
        }
        val ws = host.workspace
        val page = ws.getPageAt(ws.currentPage) ?: return
        val bounds = Rect()
        if (!page.getGlobalVisibleRect(bounds) || bounds.isEmpty) return
        val origin = IntArray(2)
        host.rootView.getLocationOnScreen(origin)
        val density = host.resources.displayMetrics.density
        fun dp(n: Int) = (n * density + .5f).toInt()
        val scale = ws.scaleX.coerceIn(.3f, 1f)
        val controlWidth = maxOf(dp(42), (dp(64) * scale).toInt())
        val controlHeight = maxOf(dp(35), (dp(52) * scale).toInt())
        overlay.remove.layoutParams = com.android.launcher3.InsettableFrameLayout.LayoutParams(
            controlWidth, controlHeight,
        ).apply {
            ignoreInsets = true
            gravity = Gravity.TOP or Gravity.START
            leftMargin = bounds.right - origin[0] - controlWidth
            topMargin = bounds.top - origin[1]
        }
        overlay.home.layoutParams = com.android.launcher3.InsettableFrameLayout.LayoutParams(
            dp(36), dp(36),
        ).apply {
            ignoreInsets = true
            gravity = Gravity.TOP or Gravity.START
            leftMargin = bounds.centerX() - origin[0] - dp(18)
            topMargin = maxOf(dp(52), bounds.top - origin[1] - dp(55))
        }
        overlay.home.visibility = View.VISIBLE
        overlay.remove.visibility = View.VISIBLE
        overlay.home.bringToFront()
        overlay.remove.bringToFront()
    }
}
