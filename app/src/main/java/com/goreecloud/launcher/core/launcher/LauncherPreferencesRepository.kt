package com.goreecloud.launcher.core.launcher

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.launcherPreferencesStore by preferencesDataStore(name = "launcher_preferences")

enum class LauncherUniversalSearchHomeMode(val storageValue: String) {
    PERMANENT("permanent"),
    SWIPE_DOWN_ONLY("swipe_down_only");

    companion object {
        fun fromStorage(value: String?): LauncherUniversalSearchHomeMode =
            entries.firstOrNull { it.storageValue == value } ?: PERMANENT
    }
}

enum class LauncherDrawerLayoutMode(val storageValue: String) {
    GRID("grid"),
    COMPACT("compact"),
    LIST("list"),
    CATEGORY("category");

    companion object {
        fun fromStorage(value: String?): LauncherDrawerLayoutMode =
            entries.firstOrNull { it.storageValue == value } ?: GRID
    }
}

enum class LauncherHomeCardStyle(val storageValue: String) {
    CLOCK("clock"),
    COMPACT("compact"),
    OFF("off");

    companion object {
        fun fromStorage(value: String?): LauncherHomeCardStyle =
            entries.firstOrNull { it.storageValue == value } ?: CLOCK
    }
}

enum class LauncherDrawerBackdrop(val storageValue: String) {
    GLASS("glass"),
    SOLID("solid");

    companion object {
        fun fromStorage(value: String?): LauncherDrawerBackdrop =
            entries.firstOrNull { it.storageValue == value } ?: GLASS
    }
}

enum class LauncherDrawerSearchPlacement(val storageValue: String) {
    TOP("top"),
    BOTTOM("bottom");

    companion object {
        fun fromStorage(value: String?): LauncherDrawerSearchPlacement =
            entries.firstOrNull { it.storageValue == value } ?: BOTTOM
    }
}

enum class LauncherDrawerNavigation(val storageValue: String) {
    SCROLL("scroll"),
    PAGES("pages");

    companion object {
        fun fromStorage(value: String?): LauncherDrawerNavigation =
            entries.firstOrNull { it.storageValue == value } ?: SCROLL
    }
}

enum class LauncherDrawerEntryMode(val storageValue: String) {
    BROWSE("browse"),
    SEARCH_FIRST("search_first");

    companion object {
        fun fromStorage(value: String?): LauncherDrawerEntryMode =
            entries.firstOrNull { it.storageValue == value } ?: BROWSE
    }
}

enum class LauncherDrawerSpacing(val storageValue: String) {
    TIGHT("tight"),
    STANDARD("standard"),
    RELAXED("relaxed");

    companion object {
        fun fromStorage(value: String?): LauncherDrawerSpacing =
            entries.firstOrNull { it.storageValue == value } ?: STANDARD
    }
}

enum class LauncherHomeGlanceAlignment(val storageValue: String) {
    LEFT("left"),
    CENTER("center");

    companion object {
        fun fromStorage(value: String?): LauncherHomeGlanceAlignment =
            entries.firstOrNull { it.storageValue == value } ?: LEFT
    }
}

enum class LauncherHomeSearchPlacement(val storageValue: String) {
    TOP("top"),
    BOTTOM("bottom");

    companion object {
        fun fromStorage(value: String?): LauncherHomeSearchPlacement =
            entries.firstOrNull { it.storageValue == value } ?: BOTTOM
    }
}

enum class LauncherHomeSearchStyle(val storageValue: String) {
    GLASS("glass"),
    CLEAR("clear"),
    SOLID("solid");

    companion object {
        fun fromStorage(value: String?): LauncherHomeSearchStyle =
            entries.firstOrNull { it.storageValue == value } ?: GLASS
    }
}

enum class LauncherHomeSpacing(val storageValue: String) {
    COMPACT("compact"),
    BALANCED("balanced"),
    AIRY("airy");

    companion object {
        fun fromStorage(value: String?): LauncherHomeSpacing =
            entries.firstOrNull { it.storageValue == value } ?: BALANCED
    }
}

enum class LauncherHomeAppMode(
    val storageValue: String,
    val displayName: String,
) {
    NONE("none", "No automatic apps"),
    RECENT("recent", "10 most recent"),
    MOST_USED("most_used", "10 most used");

    companion object {
        fun fromStorage(value: String?): LauncherHomeAppMode =
            entries.firstOrNull { it.storageValue == value } ?: NONE
    }
}

enum class LauncherDockStyle(val storageValue: String) {
    GLASS("glass"),
    CLEAR("clear"),
    EDGE("edge");

    companion object {
        fun fromStorage(value: String?): LauncherDockStyle =
            entries.firstOrNull { it.storageValue == value } ?: GLASS
    }
}

enum class LauncherWallpaperShade(val storageValue: String) {
    OFF("off"),
    SOFT("soft"),
    STRONG("strong");

    companion object {
        fun fromStorage(value: String?): LauncherWallpaperShade =
            entries.firstOrNull { it.storageValue == value } ?: SOFT
    }
}

enum class LauncherIconShape(
    val storageValue: String,
    val displayName: String,
) {
    ROUNDED_SQUARE("rounded_square", "Rounded square"),
    ORIGINAL("original", "Original"),
    SQUIRCLE("squircle", "Squircle"),
    CIRCLE("circle", "Circle"),
    TEARDROP("teardrop", "Teardrop");

    companion object {
        fun fromStorage(value: String?): LauncherIconShape =
            entries.firstOrNull { it.storageValue == value } ?: ROUNDED_SQUARE
    }
}

enum class LauncherHomeGesture(val displayName: String) {
    SWIPE_UP("Swipe up"),
    SWIPE_DOWN("Swipe down"),
    SWIPE_LEFT("Swipe left"),
    SWIPE_RIGHT("Swipe right"),
    DOUBLE_TAP("Double-tap"),
    TAP_AND_HOLD("Tap and hold"),
}

enum class LauncherGestureActionType(
    val storageValue: String,
    val displayName: String,
) {
    NONE("none", "None"),
    APPS("apps", "Apps"),
    UNIVERSAL_SEARCH("universal_search", "Universal Search"),
    LAUNCHER_SETTINGS("launcher_settings", "Launcher settings"),
    HOME_EDITOR("home_editor", "Home editor"),
    WALLPAPER("wallpaper", "Wallpaper"),
    THEME_MANAGER("theme_manager", "Theme Manager"),
    OPEN_APP("open_app", "Open app"),
}

data class LauncherGestureAction(
    val type: LauncherGestureActionType,
    val appKey: String? = null,
) {
    val storageValue: String
        get() = when (type) {
            LauncherGestureActionType.OPEN_APP ->
                appKey?.takeIf { it.isNotBlank() }?.let { "app:$it" }
                    ?: LauncherGestureActionType.NONE.storageValue
            else -> type.storageValue
        }

    companion object {
        fun builtIn(type: LauncherGestureActionType): LauncherGestureAction =
            LauncherGestureAction(type = type)

        fun openApp(appKey: String): LauncherGestureAction =
            LauncherGestureAction(
                type = LauncherGestureActionType.OPEN_APP,
                appKey = appKey,
            )

        fun fromStorage(
            value: String?,
            fallback: LauncherGestureAction,
        ): LauncherGestureAction {
            if (value == null) return fallback
            if (value.startsWith("app:")) {
                val key = value.removePrefix("app:")
                return if (key.isBlank()) fallback else openApp(key)
            }

            val type = LauncherGestureActionType.entries
                .firstOrNull { it != LauncherGestureActionType.OPEN_APP && it.storageValue == value }
                ?: return fallback
            return builtIn(type)
        }
    }
}

data class LauncherExperiencePreferences(
    val homeCardStyle: LauncherHomeCardStyle = LauncherHomeCardStyle.CLOCK,
    val showHomeQuickActions: Boolean = false,
    val showHomePageIndicator: Boolean = true,
    val showHomeLabels: Boolean = true,
    val showDrawerLabels: Boolean = true,
    val showDrawerPageIndicator: Boolean = true,
    val drawerBackdrop: LauncherDrawerBackdrop = LauncherDrawerBackdrop.GLASS,
    val drawerSearchPlacement: LauncherDrawerSearchPlacement = LauncherDrawerSearchPlacement.BOTTOM,
    val drawerNavigation: LauncherDrawerNavigation = LauncherDrawerNavigation.SCROLL,
    val drawerEntryMode: LauncherDrawerEntryMode = LauncherDrawerEntryMode.BROWSE,
    val drawerSpacing: LauncherDrawerSpacing = LauncherDrawerSpacing.STANDARD,
    val drawerPageRows: Int = 5,
    val showDrawerAppCount: Boolean = false,
    val homeGlanceAlignment: LauncherHomeGlanceAlignment = LauncherHomeGlanceAlignment.LEFT,
    val homeSearchPlacement: LauncherHomeSearchPlacement = LauncherHomeSearchPlacement.BOTTOM,
    val homeSearchStyle: LauncherHomeSearchStyle = LauncherHomeSearchStyle.GLASS,
    val homeSpacing: LauncherHomeSpacing = LauncherHomeSpacing.BALANCED,
    val dockStyle: LauncherDockStyle = LauncherDockStyle.GLASS,
    val wallpaperShade: LauncherWallpaperShade = LauncherWallpaperShade.SOFT,
    val iconShape: LauncherIconShape = LauncherIconShape.ROUNDED_SQUARE,
    val iconPackPackage: String? = null,
    val swipeUpAction: LauncherGestureAction =
        LauncherGestureAction.builtIn(LauncherGestureActionType.APPS),
    val swipeDownAction: LauncherGestureAction =
        LauncherGestureAction.builtIn(LauncherGestureActionType.UNIVERSAL_SEARCH),
    val swipeLeftAction: LauncherGestureAction =
        LauncherGestureAction.builtIn(LauncherGestureActionType.NONE),
    val swipeRightAction: LauncherGestureAction =
        LauncherGestureAction.builtIn(LauncherGestureActionType.NONE),
    val doubleTapAction: LauncherGestureAction =
        LauncherGestureAction.builtIn(LauncherGestureActionType.NONE),
    val tapAndHoldAction: LauncherGestureAction =
        LauncherGestureAction.builtIn(LauncherGestureActionType.HOME_EDITOR),
    val starterLayoutApplied: Boolean = false,
    val homeAppMode: LauncherHomeAppMode = LauncherHomeAppMode.NONE,
    val useLocalUsageForSuggestions: Boolean = false,
    val addNewAppsToHome: Boolean = false,
    val startupWizardCompleted: Boolean = false,
    val homeHintsDismissed: Boolean = false,
)

data class LauncherPreferences(
    val homeColumns: Int = 5,
    val homeRows: Int = 6,
    val drawerColumns: Int = 5,
    val showLabels: Boolean = true,
    val iconScale: Float = 1.0f,
    val layoutLocked: Boolean = false,
    val universalSearchHomeMode: LauncherUniversalSearchHomeMode = LauncherUniversalSearchHomeMode.SWIPE_DOWN_ONLY,
) {
    val homeCapacity: Int get() = homeColumns * homeRows

    fun sanitized(): LauncherPreferences = copy(
        homeColumns = homeColumns.coerceIn(4, 6),
        homeRows = homeRows.coerceIn(4, 7),
        drawerColumns = drawerColumns.coerceIn(4, 6),
        iconScale = iconScale.coerceIn(0.85f, 1.15f),
    )
}

class LauncherPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) : LauncherPortablePreferenceWriter {
    constructor(context: Context) : this(context.launcherPreferencesStore)

    private object Keys {
        val homeColumns = intPreferencesKey("home_columns")
        val homeRows = intPreferencesKey("home_rows")
        val drawerColumns = intPreferencesKey("drawer_columns")
        val showLabels = booleanPreferencesKey("show_labels")
        val iconScale = floatPreferencesKey("icon_scale")
        val layoutLocked = booleanPreferencesKey("layout_locked")
        // Legacy DataStore key is retained for strict v1 backup/recovery compatibility.
        val universalSearchHomeMode = stringPreferencesKey("index_home_mode")
        val drawerLayoutMode = stringPreferencesKey("drawer_layout_mode")
        val homeCardStyle = stringPreferencesKey("home_card_style")
        val showHomeQuickActions = booleanPreferencesKey("show_home_quick_actions")
        val showHomePageIndicator = booleanPreferencesKey("show_home_page_indicator")
        val showHomeLabels = booleanPreferencesKey("show_home_labels")
        val showDrawerLabels = booleanPreferencesKey("show_drawer_labels")
        val showDrawerPageIndicator = booleanPreferencesKey("show_drawer_page_indicator")
        val drawerBackdrop = stringPreferencesKey("drawer_backdrop")
        val drawerSearchPlacement = stringPreferencesKey("drawer_search_placement")
        val drawerNavigation = stringPreferencesKey("drawer_navigation")
        val drawerEntryMode = stringPreferencesKey("drawer_entry_mode")
        val drawerSpacing = stringPreferencesKey("drawer_spacing")
        val drawerPageRows = intPreferencesKey("drawer_page_rows")
        val showDrawerAppCount = booleanPreferencesKey("show_drawer_app_count")
        val homeGlanceAlignment = stringPreferencesKey("home_glance_alignment")
        val homeSearchPlacement = stringPreferencesKey("home_search_placement")
        val homeSearchStyle = stringPreferencesKey("home_search_style")
        val homeSpacing = stringPreferencesKey("home_spacing")
        val dockStyle = stringPreferencesKey("dock_style")
        val wallpaperShade = stringPreferencesKey("wallpaper_shade")
        val iconShape = stringPreferencesKey("icon_shape")
        val iconPackPackage = stringPreferencesKey("icon_pack_package")
        val gestureSwipeUpAction = stringPreferencesKey("gesture_swipe_up_action")
        val gestureSwipeDownAction = stringPreferencesKey("gesture_swipe_down_action")
        val gestureSwipeLeftAction = stringPreferencesKey("gesture_swipe_left_action")
        val gestureSwipeRightAction = stringPreferencesKey("gesture_swipe_right_action")
        val gestureDoubleTapAction = stringPreferencesKey("gesture_double_tap_action")
        val gestureTapAndHoldAction = stringPreferencesKey("gesture_tap_and_hold_action")
        val starterLayoutApplied = booleanPreferencesKey("starter_layout_applied")
        val homeAppMode = stringPreferencesKey("home_app_mode_v1")
        val useLocalUsageForSuggestions =
            booleanPreferencesKey("use_local_usage_for_suggestions")
        val addNewAppsToHome = booleanPreferencesKey("add_new_apps_to_home")
        val startupWizardCompleted = booleanPreferencesKey("startup_wizard_completed_v1")
        val homeHintsDismissed = booleanPreferencesKey("home_hints_dismissed_v1")
        val homeLabelOverrides = stringPreferencesKey("home_label_overrides_v1")
        val portableRestoreJournal = stringPreferencesKey("portable_restore_journal_v1")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val defaults = LauncherPreferences()

    val preferences: Flow<LauncherPreferences> = dataStore.data
        .map(::portablePreferencesFrom)
        .distinctUntilChanged()

    /**
     * Launcher-local Home label overrides. These are presentation metadata only and intentionally
     * remain outside the strict portable-preference v1 backup/recovery contract.
     */
    val homeLabelOverrides: Flow<Map<String, String>> = dataStore.data
        .map { values -> LauncherHomeLabelOverridesCodec.decode(values[Keys.homeLabelOverrides]) }
        .distinctUntilChanged()

    /**
     * Drawer presentation mode is intentionally stored outside the strict v1 portable preference
     * subset. Adding it here must not silently change the seven-field backup/recovery contract.
     */
    val drawerLayoutMode: Flow<LauncherDrawerLayoutMode> = dataStore.data
        .map { values -> LauncherDrawerLayoutMode.fromStorage(values[Keys.drawerLayoutMode]) }
        .distinctUntilChanged()

    /**
     * Launcher-owned visual preferences that intentionally remain outside the strict seven-field
     * v1 portable preference subset. These settings may evolve during Development without silently
     * changing backup/recovery compatibility.
     */
    val experiencePreferences: Flow<LauncherExperiencePreferences> = dataStore.data
        .map { values ->
            LauncherExperiencePreferences(
                homeCardStyle = LauncherHomeCardStyle.fromStorage(values[Keys.homeCardStyle]),
                showHomeQuickActions = values[Keys.showHomeQuickActions] ?: false,
                showHomePageIndicator = values[Keys.showHomePageIndicator] ?: true,
                showHomeLabels = values[Keys.showHomeLabels] ?: (values[Keys.showLabels] ?: true),
                showDrawerLabels = values[Keys.showDrawerLabels] ?: (values[Keys.showLabels] ?: true),
                showDrawerPageIndicator = values[Keys.showDrawerPageIndicator] ?: true,
                drawerBackdrop = LauncherDrawerBackdrop.fromStorage(values[Keys.drawerBackdrop]),
                drawerSearchPlacement = LauncherDrawerSearchPlacement.fromStorage(values[Keys.drawerSearchPlacement]),
                drawerNavigation = LauncherDrawerNavigation.fromStorage(values[Keys.drawerNavigation]),
                drawerEntryMode = LauncherDrawerEntryMode.fromStorage(values[Keys.drawerEntryMode]),
                drawerSpacing = LauncherDrawerSpacing.fromStorage(values[Keys.drawerSpacing]),
                drawerPageRows = (values[Keys.drawerPageRows] ?: 5).coerceIn(4, 6),
                showDrawerAppCount = values[Keys.showDrawerAppCount] ?: false,
                homeGlanceAlignment = LauncherHomeGlanceAlignment.fromStorage(values[Keys.homeGlanceAlignment]),
                homeSearchPlacement = LauncherHomeSearchPlacement.fromStorage(values[Keys.homeSearchPlacement]),
                homeSearchStyle = LauncherHomeSearchStyle.fromStorage(values[Keys.homeSearchStyle]),
                homeSpacing = LauncherHomeSpacing.fromStorage(values[Keys.homeSpacing]),
                dockStyle = LauncherDockStyle.fromStorage(values[Keys.dockStyle]),
                wallpaperShade = LauncherWallpaperShade.fromStorage(values[Keys.wallpaperShade]),
                iconShape = LauncherIconShape.fromStorage(values[Keys.iconShape]),
                iconPackPackage = values[Keys.iconPackPackage]?.takeIf { it.isNotBlank() },
                swipeUpAction = LauncherGestureAction.fromStorage(
                    values[Keys.gestureSwipeUpAction],
                    LauncherGestureAction.builtIn(LauncherGestureActionType.APPS),
                ),
                swipeDownAction = LauncherGestureAction.fromStorage(
                    values[Keys.gestureSwipeDownAction],
                    LauncherGestureAction.builtIn(LauncherGestureActionType.UNIVERSAL_SEARCH),
                ),
                swipeLeftAction = LauncherGestureAction.fromStorage(
                    values[Keys.gestureSwipeLeftAction],
                    LauncherGestureAction.builtIn(LauncherGestureActionType.NONE),
                ),
                swipeRightAction = LauncherGestureAction.fromStorage(
                    values[Keys.gestureSwipeRightAction],
                    LauncherGestureAction.builtIn(LauncherGestureActionType.NONE),
                ),
                doubleTapAction = LauncherGestureAction.fromStorage(
                    values[Keys.gestureDoubleTapAction],
                    LauncherGestureAction.builtIn(LauncherGestureActionType.NONE),
                ),
                tapAndHoldAction = LauncherGestureAction.fromStorage(
                    values[Keys.gestureTapAndHoldAction],
                    LauncherGestureAction.builtIn(LauncherGestureActionType.HOME_EDITOR),
                ),
                starterLayoutApplied = values[Keys.starterLayoutApplied] ?: false,
                homeAppMode = LauncherHomeAppMode.fromStorage(values[Keys.homeAppMode]),
                useLocalUsageForSuggestions =
                    values[Keys.useLocalUsageForSuggestions]
                        ?: (LauncherHomeAppMode.fromStorage(values[Keys.homeAppMode]) != LauncherHomeAppMode.NONE),
                addNewAppsToHome = values[Keys.addNewAppsToHome] ?: false,
                startupWizardCompleted =
                    values[Keys.startupWizardCompleted]
                        ?: (values[Keys.starterLayoutApplied] ?: false),
                homeHintsDismissed =
                    values[Keys.homeHintsDismissed]
                        ?: (values[Keys.starterLayoutApplied] ?: false),
            )
        }
        .distinctUntilChanged()

    fun setHomeLabelOverride(appKey: String, rawLabel: String?) {
        if (appKey.isBlank()) return
        scope.launch {
            dataStore.edit { values ->
                val updated = LauncherHomeLabelOverridesCodec
                    .decode(values[Keys.homeLabelOverrides])
                    .toMutableMap()
                val label = rawLabel?.let(LauncherHomeLabelPolicy::normalize)
                if (label == null) {
                    updated.remove(appKey)
                } else {
                    updated[appKey] = label
                }
                if (updated.isEmpty()) {
                    values.remove(Keys.homeLabelOverrides)
                } else {
                    values[Keys.homeLabelOverrides] = LauncherHomeLabelOverridesCodec.encode(updated)
                }
            }
        }
    }

    fun setHomeGrid(columns: Int, rows: Int) {
        val normalized = LauncherPreferences(homeColumns = columns, homeRows = rows).sanitized()
        scope.launch {
            dataStore.edit { values ->
                values[Keys.homeColumns] = normalized.homeColumns
                values[Keys.homeRows] = normalized.homeRows
            }
        }
    }

    fun setDrawerColumns(columns: Int) {
        val normalized = columns.coerceIn(4, 6)
        scope.launch {
            dataStore.edit { values ->
                values[Keys.drawerColumns] = normalized
            }
        }
    }

    fun setDrawerLayoutMode(mode: LauncherDrawerLayoutMode) {
        scope.launch {
            dataStore.edit { values ->
                values[Keys.drawerLayoutMode] = mode.storageValue
            }
        }
    }

    fun setShowLabels(show: Boolean) {
        scope.launch {
            dataStore.edit { values ->
                values[Keys.showLabels] = show
            }
        }
    }

    fun setIconScale(scale: Float) {
        val normalized = scale.coerceIn(0.85f, 1.15f)
        scope.launch {
            dataStore.edit { values ->
                values[Keys.iconScale] = normalized
            }
        }
    }

    fun setLayoutLocked(locked: Boolean) {
        scope.launch {
            dataStore.edit { values ->
                values[Keys.layoutLocked] = locked
            }
        }
    }

    fun setUniversalSearchHomeMode(mode: LauncherUniversalSearchHomeMode) {
        scope.launch {
            dataStore.edit { values ->
                values[Keys.universalSearchHomeMode] = mode.storageValue
            }
        }
    }

    fun setHomeCardStyle(style: LauncherHomeCardStyle) {
        scope.launch {
            dataStore.edit { values ->
                values[Keys.homeCardStyle] = style.storageValue
            }
        }
    }

    fun setShowHomeQuickActions(show: Boolean) {
        scope.launch {
            dataStore.edit { values ->
                values[Keys.showHomeQuickActions] = show
            }
        }
    }

    fun setShowHomePageIndicator(show: Boolean) {
        scope.launch {
            dataStore.edit { values ->
                values[Keys.showHomePageIndicator] = show
            }
        }
    }

    fun setShowHomeLabels(show: Boolean) {
        scope.launch {
            dataStore.edit { values ->
                values[Keys.showHomeLabels] = show
            }
        }
    }

    fun setShowDrawerLabels(show: Boolean) {
        scope.launch {
            dataStore.edit { values ->
                values[Keys.showDrawerLabels] = show
            }
        }
    }

    fun setShowDrawerPageIndicator(show: Boolean) {
        scope.launch {
            dataStore.edit { values ->
                values[Keys.showDrawerPageIndicator] = show
            }
        }
    }

    fun setDrawerBackdrop(backdrop: LauncherDrawerBackdrop) {
        scope.launch {
            dataStore.edit { values ->
                values[Keys.drawerBackdrop] = backdrop.storageValue
            }
        }
    }

    fun setDrawerSearchPlacement(placement: LauncherDrawerSearchPlacement) {
        scope.launch {
            dataStore.edit { values ->
                values[Keys.drawerSearchPlacement] = placement.storageValue
            }
        }
    }

    fun setDrawerNavigation(navigation: LauncherDrawerNavigation) {
        scope.launch {
            dataStore.edit { values ->
                values[Keys.drawerNavigation] = navigation.storageValue
            }
        }
    }

    fun setDrawerEntryMode(mode: LauncherDrawerEntryMode) {
        scope.launch {
            dataStore.edit { values ->
                values[Keys.drawerEntryMode] = mode.storageValue
            }
        }
    }

    fun setDrawerSpacing(spacing: LauncherDrawerSpacing) {
        scope.launch {
            dataStore.edit { values ->
                values[Keys.drawerSpacing] = spacing.storageValue
            }
        }
    }

    fun setDrawerPageRows(rows: Int) {
        scope.launch {
            dataStore.edit { values ->
                values[Keys.drawerPageRows] = rows.coerceIn(4, 6)
            }
        }
    }

    fun setHomeGlanceAlignment(alignment: LauncherHomeGlanceAlignment) {
        scope.launch {
            dataStore.edit { values ->
                values[Keys.homeGlanceAlignment] = alignment.storageValue
            }
        }
    }

    fun setShowDrawerAppCount(show: Boolean) {
        scope.launch {
            dataStore.edit { values ->
                values[Keys.showDrawerAppCount] = show
            }
        }
    }

    fun setHomeSearchPlacement(placement: LauncherHomeSearchPlacement) {
        scope.launch {
            dataStore.edit { values ->
                values[Keys.homeSearchPlacement] = placement.storageValue
            }
        }
    }

    fun setHomeSearchStyle(style: LauncherHomeSearchStyle) {
        scope.launch {
            dataStore.edit { values ->
                values[Keys.homeSearchStyle] = style.storageValue
            }
        }
    }

    fun setHomeSpacing(spacing: LauncherHomeSpacing) {
        scope.launch {
            dataStore.edit { values ->
                values[Keys.homeSpacing] = spacing.storageValue
            }
        }
    }

    fun setDockStyle(style: LauncherDockStyle) {
        scope.launch {
            dataStore.edit { values ->
                values[Keys.dockStyle] = style.storageValue
            }
        }
    }

    fun setWallpaperShade(shade: LauncherWallpaperShade) {
        scope.launch {
            dataStore.edit { values ->
                values[Keys.wallpaperShade] = shade.storageValue
            }
        }
    }

    fun setIconShape(shape: LauncherIconShape) {
        scope.launch {
            dataStore.edit { values ->
                values[Keys.iconShape] = shape.storageValue
            }
        }
    }

    fun setIconPackPackage(packageName: String?) {
        scope.launch {
            dataStore.edit { values ->
                val normalized = packageName?.trim()?.takeIf { it.isNotEmpty() }
                if (normalized == null) {
                    values.remove(Keys.iconPackPackage)
                } else {
                    values[Keys.iconPackPackage] = normalized
                }
            }
        }
    }

    fun setGestureAction(
        gesture: LauncherHomeGesture,
        action: LauncherGestureAction,
    ): Job = scope.launch {
        dataStore.edit { values ->
            val key = when (gesture) {
                LauncherHomeGesture.SWIPE_UP -> Keys.gestureSwipeUpAction
                LauncherHomeGesture.SWIPE_DOWN -> Keys.gestureSwipeDownAction
                LauncherHomeGesture.SWIPE_LEFT -> Keys.gestureSwipeLeftAction
                LauncherHomeGesture.SWIPE_RIGHT -> Keys.gestureSwipeRightAction
                LauncherHomeGesture.DOUBLE_TAP -> Keys.gestureDoubleTapAction
                LauncherHomeGesture.TAP_AND_HOLD -> Keys.gestureTapAndHoldAction
            }
            values[key] = action.storageValue
        }
    }

    fun setHomeAppMode(mode: LauncherHomeAppMode): Job = scope.launch {
        dataStore.edit { values ->
            values[Keys.homeAppMode] = mode.storageValue
            values[Keys.useLocalUsageForSuggestions] = mode != LauncherHomeAppMode.NONE
        }
    }

    /**
     * Compatibility setter for older call sites. New UI should use [setHomeAppMode].
     */
    fun setUseLocalUsageForSuggestions(enabled: Boolean): Job =
        setHomeAppMode(
            if (enabled) LauncherHomeAppMode.RECENT else LauncherHomeAppMode.NONE,
        )

    fun markStartupWizardCompleted(): Job = scope.launch {
        dataStore.edit { values ->
            values[Keys.startupWizardCompleted] = true
        }
    }

    fun setHomeHintsDismissed(dismissed: Boolean): Job = scope.launch {
        dataStore.edit { values ->
            values[Keys.homeHintsDismissed] = dismissed
        }
    }

    fun setAddNewAppsToHome(enabled: Boolean) {
        scope.launch {
            dataStore.edit { values ->
                values[Keys.addNewAppsToHome] = enabled
            }
        }
    }

    fun markStarterLayoutApplied() {
        scope.launch {
            dataStore.edit { values ->
                values[Keys.starterLayoutApplied] = true
            }
        }
    }

    suspend fun readPortablePreferences(): LauncherPreferences = preferences.first()

    /**
     * Strict recovery-only read of the seven persisted portable preferences.
     *
     * Ordinary UI reads intentionally sanitize legacy/out-of-range local values for resilience.
     * Recovery cannot use that behavior as evidence: an interrupted restore may be finalized or
     * cleared only when the raw persisted values are already inside the canonical portable domain.
     */
    suspend fun readPortablePreferencesForRecovery(): LauncherPortableRecoveryPreferenceReadResult {
        return when (val decoded = portableRecoveryPreferencesFrom(dataStore.data.first())) {
            is LauncherPortableStoredPreferencePolicy.DecodeResult.Success ->
                LauncherPortableRecoveryPreferenceReadResult.Success(decoded.preferences)
            is LauncherPortableStoredPreferencePolicy.DecodeResult.Invalid ->
                LauncherPortableRecoveryPreferenceReadResult.Invalid(decoded.reason)
        }
    }

    suspend fun readPortableRestoreJournal(): LauncherPortableRestoreJournalReadResult {
        val raw = dataStore.data.first()[Keys.portableRestoreJournal]
            ?: return LauncherPortableRestoreJournalReadResult.Absent
        return when (val decoded = LauncherPortableRestoreJournalCodec.decode(raw)) {
            is LauncherPortableRestoreJournalCodec.DecodeResult.Success ->
                LauncherPortableRestoreJournalReadResult.Present(decoded.journal)
            is LauncherPortableRestoreJournalCodec.DecodeResult.Invalid ->
                LauncherPortableRestoreJournalReadResult.Invalid(decoded.reason)
        }
    }

    /**
     * Persist one recovery journal before Room mutation. Any pre-existing journal fails closed so a
     * new restore cannot hide unresolved recovery evidence from an earlier attempt.
     */
    suspend fun beginPortableRestoreJournal(journal: LauncherPortableRestoreJournal): Boolean {
        val encoded = LauncherPortableRestoreJournalCodec.encode(journal)
        var stored = false
        dataStore.edit { values ->
            if (values[Keys.portableRestoreJournal] == null) {
                values[Keys.portableRestoreJournal] = encoded
                stored = true
            }
        }
        return stored
    }

    /**
     * Atomically write the target portable preferences and clear the exact matching journal.
     *
     * Finalization is refused if either the journal changed or the raw current portable preferences
     * are not canonically equal to the journal's previous state, protecting both concurrent edits
     * and recovery from silently sanitized/corrupted persisted values.
     */
    suspend fun finalizePortableRestoreJournal(journal: LauncherPortableRestoreJournal): Boolean {
        val encoded = LauncherPortableRestoreJournalCodec.encode(journal)
        var finalized = false
        dataStore.edit { values ->
            val current = portableRecoveryPreferencesFrom(values)
            if (
                values[Keys.portableRestoreJournal] == encoded &&
                current is LauncherPortableStoredPreferencePolicy.DecodeResult.Success &&
                current.preferences == journal.previousPreferences
            ) {
                writePortablePreferences(values, journal.targetPreferences)
                values.remove(Keys.portableRestoreJournal)
                finalized = true
            }
        }
        return finalized
    }

    /** Remove only the caller's exact journal. An absent journal is already a safe cleared state. */
    suspend fun clearPortableRestoreJournalIfMatches(
        journal: LauncherPortableRestoreJournal,
    ): Boolean {
        val encoded = LauncherPortableRestoreJournalCodec.encode(journal)
        var safe = false
        dataStore.edit { values ->
            when (values[Keys.portableRestoreJournal]) {
                null -> safe = true
                encoded -> {
                    values.remove(Keys.portableRestoreJournal)
                    safe = true
                }
                else -> safe = false
            }
        }
        return safe
    }

    /**
     * Replace the complete v1 portable preference subset in one DataStore transaction.
     *
     * The portable codec is reused as the defensive validation authority so this path never
     * silently clamps malformed external values through [LauncherPreferences.sanitized].
     */
    override suspend fun replacePortablePreferences(preferences: LauncherPreferences) {
        LauncherPortablePreferences.encode(preferences)
        dataStore.edit { values ->
            writePortablePreferences(values, preferences)
        }
    }

    /**
     * Compensate a failed Room/DataStore restore without overwriting a concurrent preference edit.
     *
     * The rollback is safe when the store still contains either the just-applied portable value or
     * the original value (for example when the failed DataStore edit never committed). Any third
     * state is treated as a concurrent change and is left untouched.
     */
    suspend fun rollbackPortablePreferencesAfterFailedApply(
        expectedApplied: LauncherPreferences,
        previous: LauncherPreferences,
    ): Boolean {
        LauncherPortablePreferences.encode(expectedApplied)
        LauncherPortablePreferences.encode(previous)

        var safe = false
        dataStore.edit { values ->
            when (portablePreferencesFrom(values)) {
                previous -> safe = true
                expectedApplied -> {
                    writePortablePreferences(values, previous)
                    safe = true
                }
                else -> safe = false
            }
        }
        return safe
    }

    private fun portablePreferencesFrom(values: Preferences): LauncherPreferences =
        LauncherPreferences(
            homeColumns = values[Keys.homeColumns] ?: defaults.homeColumns,
            homeRows = values[Keys.homeRows] ?: defaults.homeRows,
            drawerColumns = values[Keys.drawerColumns] ?: defaults.drawerColumns,
            showLabels = values[Keys.showLabels] ?: defaults.showLabels,
            iconScale = values[Keys.iconScale] ?: defaults.iconScale,
            layoutLocked = values[Keys.layoutLocked] ?: defaults.layoutLocked,
            universalSearchHomeMode = LauncherUniversalSearchHomeMode.fromStorage(values[Keys.universalSearchHomeMode]),
        ).sanitized()

    private fun portableRecoveryPreferencesFrom(
        values: Preferences,
    ): LauncherPortableStoredPreferencePolicy.DecodeResult =
        LauncherPortableStoredPreferencePolicy.decode(
            stored = LauncherPortableStoredPreferences(
                homeColumns = values[Keys.homeColumns],
                homeRows = values[Keys.homeRows],
                drawerColumns = values[Keys.drawerColumns],
                showLabels = values[Keys.showLabels],
                iconScale = values[Keys.iconScale],
                layoutLocked = values[Keys.layoutLocked],
                universalSearchHomeMode = values[Keys.universalSearchHomeMode],
            ),
            defaults = defaults,
        )

    private fun writePortablePreferences(
        values: MutablePreferences,
        preferences: LauncherPreferences,
    ) {
        values[Keys.homeColumns] = preferences.homeColumns
        values[Keys.homeRows] = preferences.homeRows
        values[Keys.drawerColumns] = preferences.drawerColumns
        values[Keys.showLabels] = preferences.showLabels
        values[Keys.iconScale] = preferences.iconScale
        values[Keys.layoutLocked] = preferences.layoutLocked
        values[Keys.universalSearchHomeMode] = preferences.universalSearchHomeMode.storageValue
    }
}
