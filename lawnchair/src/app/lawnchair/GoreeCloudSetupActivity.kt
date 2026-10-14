package app.lawnchair

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import app.lawnchair.preferences.PreferenceManager
import app.lawnchair.preferences2.PreferenceManager2
import com.android.launcher3.InvariantDeviceProfile
import kotlinx.coroutines.launch

/** Five-step GoreeCloud setup, with visible Back, Skip and Continue actions. */
class GoreeCloudSetupActivity : ComponentActivity() {
    private val store by lazy { getSharedPreferences("goreecloud_home_onboarding", Context.MODE_PRIVATE) }
    private var page = 0
    private var homeMode = 0
    private var homeColumns = 5
    private var dockColumns = 5
    private var drawerColumns = 5
    private var compact = true
    private var showLabels = true
    private var searchEnabled = true
    private var keyboard = true
    private var saving = false
    private val ink = Color.rgb(24, 42, 60)
    private val muted = Color.rgb(79, 101, 119)
    private val accent = Color.rgb(0, 105, 178)
    private val pageBg = Color.rgb(235, 245, 250)

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        if (store.getBoolean("onboarding_finished", false)) { finish(); return }
        page = state?.getInt("page") ?: 0
        homeMode = state?.getInt("homeMode") ?: 0
        homeColumns = state?.getInt("homeColumns") ?: 5
        dockColumns = state?.getInt("dockColumns") ?: 5
        drawerColumns = state?.getInt("drawerColumns") ?: 5
        compact = state?.getBoolean("compact") ?: true
        showLabels = state?.getBoolean("showLabels") ?: true
        searchEnabled = state?.getBoolean("searchEnabled") ?: true
        keyboard = state?.getBoolean("keyboard") ?: true
        window.statusBarColor = pageBg
        window.navigationBarColor = pageBg
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        render()
    }
    override fun onSaveInstanceState(state: Bundle) {
        state.putInt("page", page); state.putInt("homeMode", homeMode)
        state.putInt("homeColumns", homeColumns)
        state.putInt("dockColumns", dockColumns)
        state.putInt("drawerColumns", drawerColumns)
        state.putBoolean("compact", compact); state.putBoolean("showLabels", showLabels)
        state.putBoolean("searchEnabled", searchEnabled); state.putBoolean("keyboard", keyboard)
        super.onSaveInstanceState(state)
    }
    @Deprecated("Legacy Android back-navigation compatibility")
    override fun onBackPressed() { if (page > 0) { page--; render() } else save(skip = true) }

    private fun dp(n: Int) = (n * resources.displayMetrics.density + .5f).toInt()
    private fun backdrop(color: Int, border: Int? = null): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(18).toFloat()
            if (border != null) setStroke(dp(2), border)
        }
    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun copy(value: String, size: Float = 16f, bold: Boolean = false, color: Int = ink) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
            setLineSpacing(dp(3).toFloat(), 1f)
        }
    private fun gap(into: LinearLayout, amount: Int) =
        into.addView(View(this), LinearLayout.LayoutParams(1, dp(amount)))

    private fun choice(into: LinearLayout, title: String, subtitle: String, active: Boolean, onSelect: () -> Unit) {
        val card = column().apply {
            background = backdrop(if (active) Color.rgb(217, 239, 251) else Color.WHITE,
                if (active) accent else Color.rgb(223, 233, 239))
            setPadding(dp(18), dp(16), dp(18), dp(16))
            isClickable = true
            isFocusable = true
            contentDescription = title + ". " + subtitle + if (active) ". Selected" else ""
            setOnClickListener { onSelect() }
        }
        card.addView(copy((if (active) "✓  " else "○  ") + title, 17f, true))
        gap(card, 5)
        card.addView(copy(subtitle, 14f, color = muted))
        into.addView(card, LinearLayout.LayoutParams(-1, -2))
        gap(into, 12)
    }
    private fun info(into: LinearLayout, title: String, description: String) {
        val card = column().apply {
            background = backdrop(Color.WHITE, Color.rgb(223, 233, 239))
            setPadding(dp(18), dp(16), dp(18), dp(16))
        }
        card.addView(copy(title, 17f, true))
        gap(card, 5)
        card.addView(copy(description, 14f, color = muted))
        into.addView(card, LinearLayout.LayoutParams(-1, -2))
        gap(into, 12)
    }
    private fun toggle(into: LinearLayout, title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
        into.addView(CheckBox(this).apply {
            text = title
            textSize = 16f
            setTextColor(ink)
            isChecked = checked
            buttonTintList = ColorStateList.valueOf(accent)
            setOnCheckedChangeListener { _, value -> onChange(value) }
        })
        into.addView(copy(subtitle, 13f, color = muted).apply { setPadding(dp(38), 0, 0, dp(16)) })
    }
    private fun button(title: String, filled: Boolean, click: () -> Unit) =
        copy(title, 15f, true, if (filled) Color.WHITE else accent).apply {
            gravity = Gravity.CENTER
            background = backdrop(if (filled) accent else Color.rgb(222, 240, 249))
            minHeight = dp(52)
            setPadding(dp(8), dp(10), dp(8), dp(10))
            isClickable = true
            isFocusable = true
            setOnClickListener { click() }
        }

    private fun gridChoice(into: LinearLayout, title: String, current: Int, onSelected: (Int) -> Unit) {
        into.addView(copy(title, 17f, true))
        gap(into, 9)
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for (columns in 4..6) {
            val selected = columns == current
            val cell = button("$columns columns", selected) { onSelected(columns); render() }
            if (!selected) cell.background = backdrop(Color.WHITE, Color.rgb(220, 233, 243))
            row.addView(cell, LinearLayout.LayoutParams(0, dp(55), 1f).apply {
                if (columns > 4) marginStart = dp(8)
            })
        }
        into.addView(row)
        gap(into, 22)
    }

    private fun render() {
        val names = arrayOf("Welcome", "Your Home screen", "Layout", "Appearance", "Universal Search", "Privacy & finish")
        val root = column().apply { setBackgroundColor(pageBg) }
        val scroll = ScrollView(this).apply { isFillViewport = true }
        val content = column().apply { setPadding(dp(24), dp(24), dp(24), dp(16)) }
        content.addView(copy("GOREECLOUD  /  LAUNCHER", 13f, true, accent))
        gap(content, 25)
        content.addView(copy("STEP ${page + 1} OF 6", 12f, true, muted))
        gap(content, 9)
        content.addView(copy(names[page], 30f, true))
        gap(content, 20)
        when (page) {
            0 -> {
                content.addView(copy("A polished, personal Home screen. Five columns, a compact Glaze dock, and local-first search.", 17f, color = muted))
                gap(content, 22)
                info(content, "Make it yours", "Choose which apps appear on Home.")
                info(content, "GoreeCloud Glaze", "Customize icon size and labels.")
                info(content, "Private by default", "Protected search sources remain opt-in.")
            }
            1 -> {
                content.addView(copy("Choose your initial Home shortcuts. Your dock remains separate.", 15f, color = muted))
                gap(content, 19)
                choice(content, "No Apps", "Start with a clear Home screen. Recommended.", homeMode == 0) { homeMode = 0; render() }
                choice(content, "10 Recently Used Apps", "Use permission-controlled on-device usage history.", homeMode == 1) { homeMode = 1; render() }
                choice(content, "10 Most Used Apps", "Rank installed apps by usage time on your device.", homeMode == 2) { homeMode = 2; render() }
                content.addView(copy("Any existing personalized Home layout is preserved during upgrades.", 13f, color = muted))
            }
            2 -> {
                content.addView(copy("Choose the number of columns for each area. Five columns is the default.", 15f, color = muted))
                gap(content, 20)
                gridChoice(content, "Home screen", homeColumns) { homeColumns = it }
                gridChoice(content, "Dock", dockColumns) { dockColumns = it }
                gridChoice(content, "App drawer", drawerColumns) { drawerColumns = it }
                content.addView(copy("Each area is independently configurable later in Launcher Settings.", 13f, color = muted))
            }
            3 -> {
                content.addView(copy("Choose comfortable default icon sizing.", 15f, color = muted))
                gap(content, 19)
                choice(content, "Compact icons", "Smaller Home and dock icons.", compact) { compact = true; render() }
                choice(content, "Balanced icons", "Slightly larger Home and dock icons.", !compact) { compact = false; render() }
                toggle(content, "Show Home icon labels", "Display app names on Home.", showLabels) { showLabels = it }
            }
            4 -> {
                content.addView(copy("Swipe down on empty Home space to search apps, shortcuts, quick answers and enabled sources.", 15f, color = muted))
                gap(content, 20)
                toggle(content, "Enable Universal Search", "Gesture can be customized later in Settings.", searchEnabled) { searchEnabled = it }
                toggle(content, "Open keyboard automatically", "Start typing when search opens.", keyboard) { keyboard = it }
                content.addView(copy("Contacts, files and web suggestions stay independently permission-controlled.", 13f, color = muted))
            }
            else -> {
                content.addView(copy("Local searches stay on your device. Other sources require separate permission and consent.", 16f, color = muted))
                gap(content, 22)
                info(content, "Contacts & files", "Only available if you enable access.")
                info(content, "Web suggestions", "Optional, with a configurable provider.")
                info(content, "Usage history", "Only used for Home options that require it.")
                if (homeMode != 0)
                    content.addView(copy("Android Usage Access is optional; without it, Home stays empty until granted.", 13f, color = muted))
            }
        }

        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(20), dp(12), dp(20), dp(22))
        }
        if (page > 0) {
            footer.addView(button("Back", false) { page--; render() },
                LinearLayout.LayoutParams(0, dp(54), 0.72f))
        }
        footer.addView(button("Skip", false) { save(skip = true) },
            LinearLayout.LayoutParams(0, dp(54), 0.76f).apply {
                if (page > 0) marginStart = dp(8)
            })
        footer.addView(button(if (page == 5) "Finish" else "Continue", true) {
            if (page == 5) save(skip = false) else { page++; render() }
        }, LinearLayout.LayoutParams(0, dp(54), 1.15f).apply { marginStart = dp(9) })
        root.addView(footer)
        setContentView(root)
    }

    private fun save(skip: Boolean) {
        if (saving) return
        saving = true
        val selectedMode = if (skip) 0 else homeMode
        lifecycleScope.launch {
            try {
                if (!skip) {
                    val prefs = PreferenceManager2.getInstance(this@GoreeCloudSetupActivity)
                    prefs.homeIconSizeFactor.set(if (compact) 0.78f else 0.90f)
                    prefs.showIconLabelsOnHomeScreen.set(showLabels)
                    prefs.universalSearchEnabled.set(searchEnabled)
                    prefs.universalSearchKeyboardOnOpen.set(keyboard)
                    val grid = PreferenceManager.getInstance(this@GoreeCloudSetupActivity)
                    grid.batchEdit {
                        grid.workspaceColumns.set(homeColumns)
                        grid.hotseatColumns.set(dockColumns)
                        grid.hotseatColumnsUnfolded.set(dockColumns)
                    }
                    prefs.drawerColumns.set(drawerColumns)
                    InvariantDeviceProfile.INSTANCE.get(this@GoreeCloudSetupActivity)
                        .onPreferencesChanged(this@GoreeCloudSetupActivity)
                }
                store.edit().putInt("mode", selectedMode)
                    .putBoolean("completed", selectedMode == 0)
                    .putBoolean("onboarding_finished", true).apply()
                finish()
            } catch (_: Exception) {
                saving = false
                Toast.makeText(this@GoreeCloudSetupActivity,
                    "Setup could not be saved. Please try again.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
