package app.lawnchair.views

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import app.lawnchair.LawnchairLauncher
import com.android.launcher3.LauncherState
import com.android.launcher3.R
import com.android.launcher3.model.data.ItemInfo
import com.android.launcher3.statemanager.StateManager
import java.util.WeakHashMap

/**
 * Edit Home chrome laid over the real (scaled) Workspace and hotseat.
 * Header, page actions and dock removal badges are independent touch targets.
 * No activity is launched or workspace data changed merely by entering edit mode.
 */
object GoreeCloudOverviewToolbar {
    private data class Controls(
        val heading: View,
        val done: View,
        val pageActions: View,
        val dockBadges: MutableList<View> = mutableListOf(),
    )
    private val controls = WeakHashMap<LawnchairLauncher, Controls>()
    private fun dp(host: LawnchairLauncher, n: Int) =
        (n * host.resources.displayMetrics.density + .5f).toInt()

    private fun glass(host: LawnchairLauncher, color: Int, corner: Int, border: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(host, corner).toFloat()
            setStroke(dp(host, 1), border)
        }

    private fun addOverlay(host: LawnchairLauncher, view: View, width: Int, height: Int) {
        host.rootView.addView(
            view, com.android.launcher3.InsettableFrameLayout.LayoutParams(width, height).apply {
                ignoreInsets = true
                gravity = Gravity.TOP or Gravity.START
            },
        )
        view.visibility = View.GONE
    }

    fun install(host: LawnchairLauncher) {
        if (controls.containsKey(host)) return
        val heading = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            contentDescription = "Edit Home Screen. Tap to set this page as Home"
            isFocusable = true
            isClickable = true
            setOnClickListener {
                val ws = host.workspace
                if (ws.childCount > 0) {
                    val start = ws.currentPage / ws.panelCount.coerceAtLeast(1) *
                        ws.panelCount.coerceAtLeast(1)
                    ws.setDefaultPage(start)
                    host.findViewById<EditModePageStrip>(R.id.edit_mode_page_strip)?.refreshItems()
                    Toast.makeText(host, "Default Home page updated", Toast.LENGTH_SHORT).show()
                }
            }
        }
        heading.addView(ImageView(host).apply {
            setImageResource(R.drawable.goreecloud_edit_home)
            imageTintList = ColorStateList.valueOf(Color.WHITE)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(dp(host, 28), dp(host, 28)))
        heading.addView(TextView(host).apply {
            text = "Edit Home Screen"
            setTextColor(Color.WHITE)
            textSize = 17f
            gravity = Gravity.CENTER
            includeFontPadding = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(-1, dp(host, 30)))
        addOverlay(host, heading, dp(host, 220), dp(host, 65))

        val done = TextView(host).apply {
            text = "Done"
            gravity = Gravity.CENTER
            textSize = 16f
            setTextColor(Color.WHITE)
            background = glass(host, 0xFF1375D5.toInt(), 24, 0x3394D9FF)
            contentDescription = "Done editing Home Screen"
            isFocusable = true
            isClickable = true
            setOnClickListener { host.stateManager.goToState(LauncherState.NORMAL) }
        }
        addOverlay(host, done, dp(host, 76), dp(host, 44))

        val actions = LinearLayout(host).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            clipChildren = false
        }
        fun action(label: String, icon: Int, onClick: () -> Unit) {
            val column = LinearLayout(host).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                contentDescription = label
                isFocusable = true
                isClickable = true
                setOnClickListener { onClick() }
            }
            column.addView(FrameLayout(host).apply {
                background = glass(host, 0x8A0A3043.toInt(), 22, 0x448BD5F1)
                addView(ImageView(host).apply {
                    setImageResource(icon)
                    imageTintList = ColorStateList.valueOf(
                        if (label == "Remove") 0xFFFF5F80.toInt() else Color.WHITE)
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }, FrameLayout.LayoutParams(dp(host, 25), dp(host, 25), Gravity.CENTER))
            }, LinearLayout.LayoutParams(dp(host, 51), dp(host, 51)))
            column.addView(TextView(host).apply {
                text = label
                textSize = 12f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                includeFontPadding = false
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(-1, dp(host, 24)))
            actions.addView(column, LinearLayout.LayoutParams(dp(host, 73), dp(host, 78)))
        }
        action("Add apps", R.drawable.goreecloud_edit_apps) {
            // App drawer owns app discovery; long-press from there copies to Home.
            Toast.makeText(host, "Touch and hold an app to add it to Home", Toast.LENGTH_LONG).show()
            host.stateManager.goToState(LauncherState.ALL_APPS)
        }
        action("Remove", R.drawable.goreecloud_edit_remove) {
            val removed = host.workspace.removeEmptyWorkspacePageByIndex(host.workspace.currentPage)
            if (!removed) {
                Toast.makeText(host, "Move icons and widgets before removing this page",
                    Toast.LENGTH_SHORT).show()
            }
            host.findViewById<EditModePageStrip>(R.id.edit_mode_page_strip)?.refreshItems()
            host.rootView.post { refresh(host) }
        }
        addOverlay(host, actions, dp(host, 146), dp(host, 82))

        controls[host] = Controls(heading, done, actions)
        host.stateManager.addStateListener(object : StateManager.StateListener<LauncherState> {
            override fun onStateTransitionStart(toState: LauncherState) {
                val editing = toState == LauncherState.EDIT_MODE
                GoreeCloudOverviewFrames.apply(host, editing)
                if (!editing) hide(host)
            }
            override fun onStateTransitionComplete(finalState: LauncherState) {
                val editing = finalState == LauncherState.EDIT_MODE
                GoreeCloudOverviewFrames.apply(host, editing)
                if (editing) {
                    refresh(host)
                    host.rootView.post { refresh(host) }
                } else hide(host)
            }
        })
    }

    private fun hide(host: LawnchairLauncher) {
        controls[host]?.let { c ->
            c.heading.visibility = View.GONE
            c.done.visibility = View.GONE
            c.pageActions.visibility = View.GONE
            c.dockBadges.forEach { host.rootView.removeView(it) }
            c.dockBadges.clear()
        }
    }

    private fun setPosition(
        host: LawnchairLauncher, view: View, left: Int, top: Int, width: Int, height: Int,
    ) {
        view.layoutParams = com.android.launcher3.InsettableFrameLayout.LayoutParams(
            width, height,
        ).apply {
            ignoreInsets = true
            gravity = Gravity.TOP or Gravity.START
            leftMargin = left
            topMargin = top
        }
        view.visibility = View.VISIBLE
        view.bringToFront()
    }

    fun refresh(host: LawnchairLauncher) {
        val c = controls[host] ?: return
        if (!host.isInState(LauncherState.EDIT_MODE)) {
            hide(host)
            return
        }
        val ws = host.workspace
        val page = ws.getPageAt(ws.currentPage) ?: return
        val pageBounds = Rect()
        val origin = IntArray(2)
        if (!page.getGlobalVisibleRect(pageBounds) || pageBounds.isEmpty) return
        host.rootView.getLocationOnScreen(origin)
        val safeTop = maxOf(dp(host, 36), host.deviceProfile.getInsets().top + dp(host, 10))
        val previewLeft = pageBounds.left - origin[0]
        val previewTop = pageBounds.top - origin[1]
        val previewRight = pageBounds.right - origin[0]

        setPosition(host, c.heading,
            (host.rootView.width - dp(host, 220)) / 2, safeTop,
            dp(host, 220), dp(host, 65))
        setPosition(host, c.done,
            host.rootView.width - dp(host, 86), safeTop + dp(host, 7),
            dp(host, 76), dp(host, 44))
        // Page controls sit inside the upper right of the selected page.
        setPosition(host, c.pageActions,
            (previewRight - dp(host, 154)).coerceAtLeast(previewLeft + dp(host, 6)),
            previewTop + dp(host, 8), dp(host, 146), dp(host, 82))

        // Only actual dock shortcuts receive minus affordances; none are fabricated.
        c.dockBadges.forEach { host.rootView.removeView(it) }
        c.dockBadges.clear()
        val dock = host.hotseat.getCurrentPageLayout() ?: return
        val container = dock.shortcutsAndWidgets
        for (index in 0 until container.childCount) {
            val icon = container.getChildAt(index)
            val item = icon.tag as? ItemInfo ?: continue
            val bounds = Rect()
            if (!icon.getGlobalVisibleRect(bounds) || bounds.isEmpty) continue
            val badge = FrameLayout(host).apply {
                contentDescription = "Remove ${item.title ?: "app"} from dock"
                isClickable = true
                isFocusable = true
                addView(TextView(host).apply {
                    text = "−"
                    textSize = 17f
                    setTextColor(0xFF0D4262.toInt())
                    gravity = Gravity.CENTER
                    includeFontPadding = false
                    background = glass(host, 0xFFF8FCFF.toInt(), 14, 0x8869AEDB.toInt())
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }, FrameLayout.LayoutParams(dp(host, 21), dp(host, 21),
                    Gravity.TOP or Gravity.START))
                setOnClickListener {
                    // Removes only this dock placement, never the installed app.
                    host.modelWriter.deleteItemFromDatabase(item, "Remove icon from dock in Edit Home")
                    host.rootView.postDelayed({
                        if (!host.isDestroyed) host.model.forceReload()
                    }, 250L)
                    host.rootView.postDelayed({ refresh(host) }, 420L)
                }
            }
            addOverlay(host, badge, dp(host, 42), dp(host, 42))
            setPosition(host, badge,
                bounds.left - origin[0] - dp(host, 5),
                bounds.top - origin[1] - dp(host, 5),
                dp(host, 42), dp(host, 42))
            c.dockBadges.add(badge)
        }
    }
}
