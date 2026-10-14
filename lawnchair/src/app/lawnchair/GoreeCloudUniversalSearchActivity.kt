package app.lawnchair

import app.lawnchair.preferences.PreferenceManager as GoreeCloudPreferenceManager
import android.app.Activity
import android.app.AppOpsManager
import android.app.SearchManager
import android.net.Uri
import android.content.ContentUris
import android.provider.ContactsContract
import android.provider.MediaStore
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.android.launcher3.InvariantDeviceProfile
import app.lawnchair.preferences2.PreferenceManager2
import app.lawnchair.search.algorithms.engine.SearchResult
import app.lawnchair.search.algorithms.engine.provider.CalculatorSearchProvider
import app.lawnchair.search.algorithms.engine.provider.ContactsSearchProvider
import app.lawnchair.search.algorithms.engine.provider.FileSearchProvider
import app.lawnchair.search.algorithms.data.FileInfo
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import app.lawnchair.preferences2.firstCached
import java.util.Locale
import kotlin.math.min

/**
 * Glaze Universal Search: translucent pull-down over the existing Home screen,
 * not a navigation to All Apps. Personal profile results stay local. Android
 * Usage Access is optional; launcher-owned open history is a local fallback.
 */
class GoreeCloudUniversalSearchActivity : androidx.activity.ComponentActivity() {
    private val accent = Color.rgb(0, 103, 181)
    private val dark = Color.rgb(24, 36, 54)
    private val muted = Color.rgb(98, 110, 123)
    private val tabs = arrayOf("FREQUENT", "RECENT", "NEW/UPDATED")
    private lateinit var apps: LauncherApps
    private lateinit var input: EditText
    private lateinit var grid: LinearLayout
    private lateinit var tabsBar: LinearLayout
    private var tab = 0
    private var query = ""
    private var extraSearchJob: Job? = null
    private var columns = 5
    private var allApps: List<LauncherActivityInfo> = emptyList()

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val prefs = PreferenceManager2.getInstance(this)
        if (!prefs.universalSearchEnabled.firstCached()) { finish(); return }
        columns = prefs.drawerColumns.firstCached(
            InvariantDeviceProfile.INSTANCE.get(this).closestProfile).coerceIn(4, 6)
        apps = getSystemService(LauncherApps::class.java)
        allApps = apps.profiles.flatMap { profile ->
            runCatching { apps.getActivityList(null, profile) }.getOrDefault(emptyList())
        }.filter { it.componentName.packageName != packageName }
            .distinctBy { it.componentName to it.user }
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.attributes = window.attributes.apply { dimAmount = .18f }
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        render()
        val openKeyboard = prefs.universalSearchKeyboardOnOpen.firstCached()
        if (openKeyboard) {
            input.postDelayed({
                if (!isFinishing) {
                    input.requestFocus()
                    (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                        .showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
                }
            }, 180L)
        } else {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        }
    }

    private fun dp(v: Int) = (resources.displayMetrics.density * v + .5f).toInt()
    private fun shape(color: Int, radius: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }
    private fun text(value: String, size: Float = 14f, weight: Boolean = false, color: Int = dark) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            if (weight) setTypeface(null, Typeface.BOLD)
        }

    private fun render() {
        val background = FrameLayout(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            isClickable = true
            setOnClickListener { finish() }
        }
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            this.background = shape(Color.rgb(252, 251, 255), 26)
            elevation = dp(12).toFloat()
            isClickable = true
        }
        val cardLayout = FrameLayout.LayoutParams(-1, -2, Gravity.TOP).apply {
            setMargins(dp(13), dp(8), dp(13), 0)
        }
        background.addView(card, cardLayout)
        val searchRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }
        val searchIcon = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_search)
            imageTintList = android.content.res.ColorStateList.valueOf(dark)
            contentDescription = "Search"
        }
        searchRow.addView(searchIcon, LinearLayout.LayoutParams(dp(31), dp(31)))
        input = EditText(this).apply {
            setSingleLine(true)
            setTextColor(dark)
            setHintTextColor(muted)
            textSize = 19f
            hint = "Search apps and more"
            this.background = null
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
            setPadding(dp(10), 0, 0, 0)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    query = s?.toString().orEmpty()
                    showResults()
                }
                override fun afterTextChanged(s: Editable?) = Unit
            })
            setOnEditorActionListener { _, _, _ ->
                // IME Search is local: never submit device queries to the web implicitly.
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                    .hideSoftInputFromWindow(windowToken, 0)
                true
            }
        }
        searchRow.addView(input, LinearLayout.LayoutParams(0, dp(55), 1f))
        val close = text("×", 28f, color = muted).apply {
            contentDescription = "Close Universal Search"
            setOnClickListener { finish() }
            isFocusable = true
        }
        searchRow.addView(close, LinearLayout.LayoutParams(dp(35), dp(48)))
        card.addView(searchRow)
        tabsBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(8), dp(8), 0)
        }
        card.addView(tabsBar)
        grid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(9), dp(8), dp(9), dp(17))
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = false
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }
        scroll.addView(grid)
        // A bounded translucent panel leaves Home visible beneath the search.
        val h = min(resources.displayMetrics.heightPixels * 45 / 100, dp(440))
        card.addView(scroll, LinearLayout.LayoutParams(-1, h))
        setContentView(background)
        card.translationY = -dp(520).toFloat()
        card.animate().translationY(0f).setDuration(240L).start()
        showTabs()
        showResults()
    }

    private fun showTabs() {
        tabsBar.removeAllViews()
        for (i in tabs.indices) {
            val active = i == tab
            val slot = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                isClickable = true
                isFocusable = true
                contentDescription = "${tabs[i]} tab" + if (active) ", selected" else ""
                setOnClickListener { tab = i; showTabs(); showResults() }
            }
            slot.addView(text(tabs[i], 12f, active, if (active) dark else muted),
                LinearLayout.LayoutParams(-1, dp(57)))
            slot.addView(View(this).apply {
                setBackgroundColor(if (active) accent else Color.TRANSPARENT)
            }, LinearLayout.LayoutParams(-1, dp(3)))
            tabsBar.addView(slot, LinearLayout.LayoutParams(0, -2, 1f))
        }
    }

    private fun hasUsageAccess(): Boolean {
        val ops = getSystemService(AppOpsManager::class.java) ?: return false
        return ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(), packageName) == AppOpsManager.MODE_ALLOWED
    }

    private fun rankedApps(): List<LauncherActivityInfo> {
        val prefs = PreferenceManager2.getInstance(this)
        val hidden = prefs.hiddenApps.firstCached()
        val hiddenPolicy = prefs.hiddenAppsInSearch.firstCached()
        val visible = allApps.filter { activity ->
            val key = com.android.launcher3.util.ComponentKey(
                activity.componentName, activity.user).toString()
            key !in hidden || when (hiddenPolicy) {
                app.lawnchair.ui.preferences.components.HiddenAppsInSearch.ALWAYS -> true
                app.lawnchair.ui.preferences.components.HiddenAppsInSearch.IF_NAME_TYPED ->
                    query.isNotBlank() && query.equals(activity.label.toString(), ignoreCase = true)
                else -> false
            }
        }
        if (query.isNotBlank()) {
            val words = query.trim().lowercase(Locale.getDefault()).split(Regex("\\s+"))
            return visible.filter { activity ->
                val name = activity.label.toString().lowercase(Locale.getDefault())
                words.all(name::contains)
            }.sortedBy { it.label.toString().lowercase(Locale.getDefault()) }.take(18)
        }
        if (tab == 2) {
            return visible.sortedWith(compareByDescending<LauncherActivityInfo> { activity ->
                try {
                    val packageInfo = packageManager.getPackageInfo(activity.componentName.packageName, 0)
                    maxOf(packageInfo.firstInstallTime, packageInfo.lastUpdateTime)
                } catch (_: Exception) { 0L }
            }.thenBy { it.label.toString() }).take(18)
        }
        val local = getSharedPreferences("goreecloud_universal_search_history", Context.MODE_PRIVATE)
        val usage = if (hasUsageAccess()) {
            val now = System.currentTimeMillis()
            getSystemService(UsageStatsManager::class.java)
                ?.queryAndAggregateUsageStats(now - 30L * 86400_000L, now)
                .orEmpty()
        } else emptyMap()
        return visible.filter { activity ->
            val id = activity.componentName.packageName
            val stat = usage[id]
            (stat?.lastTimeUsed ?: 0L) > 0L || local.getLong("opened_$id", 0L) > 0L
        }.sortedByDescending { activity ->
            val id = activity.componentName.packageName
            val stat = usage[id]
            if (tab == 0) {
                (stat?.totalTimeInForeground ?: 0L) + local.getInt("count_$id", 0) * 60_000L
            } else {
                maxOf(stat?.lastTimeUsed ?: 0L, local.getLong("opened_$id", 0L))
            }
        }.take(18)
    }

    private fun showResults() {
        if (!::grid.isInitialized) return
        extraSearchJob?.cancel()
        grid.removeAllViews()
        val results = rankedApps()
        if (results.isEmpty()) {
            val emptyMessage = if (query.isNotBlank()) "No matching apps" else when (tab) {
                0 -> "Frequent apps appear as you use Search or grant optional Usage Access."
                1 -> "Recent apps appear as you launch them or grant optional Usage Access."
                else -> "No installed applications found."
            }
            grid.addView(text(emptyMessage, 14f, color = muted).apply {
                setPadding(dp(20), dp(25), dp(20), dp(25))
            })
            if (query.isBlank() && tab != 2 && !hasUsageAccess()) {
                actionRow("Enable optional Android Usage Access") {
                    startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
            }
        } else {
            results.chunked(columns).forEach { chunk ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.TOP
                }
                for (app in chunk) {
                    val tile = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                        isClickable = true
                        isFocusable = true
                        contentDescription = "Open ${app.label}"
                        setOnClickListener { openApp(app) }
                        setPadding(dp(2), dp(9), dp(2), dp(6))
                    }
                    val icon = ImageView(this).apply {
                        setImageDrawable(app.getIcon(resources.displayMetrics.densityDpi))
                        scaleType = ImageView.ScaleType.FIT_CENTER
                    }
                    val frame = FrameLayout(this)
                    frame.addView(icon, FrameLayout.LayoutParams(dp(43), dp(43)))
                    val badgePosition = GoreeCloudPreferenceManager.getInstance(this@GoreeCloudUniversalSearchActivity)
                        .profileBadgePosition.get()
                    val badgeGravity = when (badgePosition) {
                        "bottom_left" -> Gravity.BOTTOM or Gravity.START
                        "bottom_right" -> Gravity.BOTTOM or Gravity.END
                        else -> Gravity.TOP or Gravity.START
                    }
                    val workProfile = app.user != Process.myUserHandle()
                    frame.addView(text(if (workProfile) "W" else "P", 10f, true, Color.WHITE).apply {
                        this.background = shape(if (workProfile) 0xFF96501C.toInt() else accent, 10)
                        contentDescription = if (workProfile) "Work profile" else "Personal profile"
                    }, FrameLayout.LayoutParams(dp(17), dp(17), badgeGravity))
                    tile.addView(frame, LinearLayout.LayoutParams(dp(45), dp(45)).apply {
                        gravity = Gravity.CENTER_HORIZONTAL
                    })
                    val title = text(app.label.toString(), 11f).apply {
                        maxLines = 2
                        ellipsize = android.text.TextUtils.TruncateAt.END
                        setPadding(0, dp(7), 0, 0)
                    }
                    tile.addView(title, LinearLayout.LayoutParams(-1, dp(46)))
                    row.addView(tile, LinearLayout.LayoutParams(0, dp(112), 1f))
                }
                repeat(columns - chunk.size) {
                    row.addView(View(this), LinearLayout.LayoutParams(0, dp(112), 1f))
                }
                grid.addView(row)
            }
        }
        if (query.isNotBlank()) {
            val searchTerm = query
            extraSearchJob = lifecycleScope.launch { showEnabledSources(searchTerm) }
            actionRow("Search web for \"$query\"") { explicitWebSearch(query) }
        }
    }

    /** Reuse the existing permission-aware providers; do not initiate any network search. */
    private suspend fun showEnabledSources(term: String) {
        delay(180L)
        if (term != query || isFinishing) return
        suspend fun gather(provider: app.lawnchair.search.algorithms.engine.SearchProvider): List<SearchResult> {
            return try {
                provider.search(this@GoreeCloudUniversalSearchActivity, term).first()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                emptyList()
            }
        }
        val calculator = gather(CalculatorSearchProvider).filterIsInstance<SearchResult.Calculation>().take(1)
        val contacts = gather(ContactsSearchProvider).filterIsInstance<SearchResult.Contact>().take(4)
        val files = gather(FileSearchProvider).filterIsInstance<SearchResult.File>().take(4)
        if (term != query || isFinishing) return

        if (calculator.isNotEmpty()) {
            section("QUICK ANSWER")
            calculator.forEach { result ->
                actionRow("${result.data.equation} = ${result.data.result}") {
                    (getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager)
                        .setPrimaryClip(android.content.ClipData.newPlainText("Result", result.data.result))
                    android.widget.Toast.makeText(this, "Answer copied", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
        if (contacts.isNotEmpty()) {
            section("CONTACTS")
            contacts.forEach { result ->
                val contact = result.data
                actionRow("${contact.name}  ·  ${contact.number}") {
                    val id = contact.contactId.toLongOrNull()
                    val uri = if (id != null) ContentUris.withAppendedId(
                        ContactsContract.Contacts.CONTENT_URI, id
                    ) else Uri.parse("tel:${Uri.encode(contact.number)}")
                    val action = if (id != null) Intent.ACTION_VIEW else Intent.ACTION_DIAL
                    launchSupported(Intent(action, uri))
                }
            }
        }
        if (files.isNotEmpty()) {
            section("FILES & DOCUMENTS")
            files.forEach { result ->
                val file = result.data
                actionRow(file.name) {
                    val id = (file as? FileInfo)?.fileId?.toLongOrNull()
                    if (id == null) {
                        launchSupported(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "*/*"
                        })
                    } else {
                        val uri = ContentUris.withAppendedId(
                            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL), id)
                        launchSupported(Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, (file as FileInfo).mimeType ?: "*/*")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        })
                    }
                }
            }
        }
        // Android app shortcuts are queried only for an app already visible to
        // this search, so hidden-app preferences apply to shortcut results too.
        val matching = rankedApps()
        if (matching.size == 1) {
            val packageId = matching.first().componentName.packageName
            val shortcuts = try {
                val q = LauncherApps.ShortcutQuery()
                    .setPackage(packageId)
                    .setQueryFlags(
                        LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                            LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                            LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
                    )
                apps.getShortcuts(q, matching.first().user).orEmpty()
            } catch (_: Exception) {
                emptyList()
            }
            if (shortcuts.isNotEmpty() && term == query && !isFinishing) {
                section("APP SHORTCUTS")
                shortcuts.take(4).forEach { shortcut ->
                    actionRow(shortcut.shortLabel?.toString() ?: shortcut.id) {
                        try {
                            apps.startShortcut(
                                shortcut.`package`, shortcut.id, null, null, matching.first().user
                            )
                            finish()
                        } catch (_: Exception) {
                            android.widget.Toast.makeText(
                                this, "Shortcut unavailable", android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }

    private fun section(title: String) {
        grid.addView(text(title, 12f, weight = true, color = muted).apply {
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(8), dp(4))
        })
    }

    private fun launchSupported(intent: Intent) {
        try {
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
                finish()
            } else {
                android.widget.Toast.makeText(this, "No compatible application", android.widget.Toast.LENGTH_SHORT).show()
            }
        } catch (_: Exception) {
            android.widget.Toast.makeText(this, "Unable to open result", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun actionRow(title: String, action: () -> Unit) {
        val label = text(title, 13f, weight = true, color = accent).apply {
            setPadding(dp(8), dp(12), dp(8), dp(12))
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }
        grid.addView(label, LinearLayout.LayoutParams(-1, dp(55)))
    }

    private fun openApp(activity: LauncherActivityInfo) {
        val packageId = activity.componentName.packageName
        val history = getSharedPreferences("goreecloud_universal_search_history", Context.MODE_PRIVATE)
        try {
            apps.startMainActivity(activity.componentName, activity.user, null, null)
            history.edit()
                .putLong("opened_$packageId", System.currentTimeMillis())
                .putInt("count_$packageId", history.getInt("count_$packageId", 0) + 1)
                .apply()
            finish()
        } catch (_: Exception) {
            android.widget.Toast.makeText(this, "Unable to open app", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun explicitWebSearch(text: String) {
        if (text.isBlank()) return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(
            app.lawnchair.search.algorithms.engine.provider.web.BraveWebSearchProvider.getSearchUrl(text)
        ))
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
            finish()
        } else {
            android.widget.Toast.makeText(this, "No web search provider available", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
