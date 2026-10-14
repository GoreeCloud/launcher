/*
 * Copyright 2022, Lawnchair
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.lawnchair

import android.animation.AnimatorSet
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Pair
import android.view.Display
import android.view.View
import android.view.ViewTreeObserver
import android.window.SplashScreen
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import app.lawnchair.LawnchairApp.Companion.showQuickstepWarningIfNecessary
import app.lawnchair.compat.LawnchairQuickstepCompat
import app.lawnchair.data.AppDatabase
import app.lawnchair.data.wallpaper.service.WallpaperService
import app.lawnchair.gestures.GestureController
import app.lawnchair.gestures.VerticalSwipeTouchController
import app.lawnchair.gestures.config.GestureHandlerConfig
import app.lawnchair.gestures.ui.LawnchairShortcutActivity
import app.lawnchair.preferences.PreferenceManager
import app.lawnchair.preferences2.PreferenceManager2
import app.lawnchair.preferences2.firstCached
import app.lawnchair.root.RootHelperManager
import app.lawnchair.root.RootNotAvailableException
import app.lawnchair.theme.ThemeProvider
import app.lawnchair.ui.popup.LauncherOptionsPopup
import app.lawnchair.ui.popup.LawnchairShortcut
import app.lawnchair.util.getThemedIconPacksInstalled
import app.lawnchair.util.unsafeLazy
import app.lawnchair.views.LawnchairFloatingSurfaceView
import com.android.launcher3.AbstractFloatingView
import com.android.launcher3.BaseActivity
import com.android.launcher3.BubbleTextView
import com.android.launcher3.GestureNavContract
import com.android.launcher3.LauncherAppState
import com.android.launcher3.LauncherSettings.Favorites.CONTAINER_ALL_APPS_PREDICTION
import com.android.launcher3.LauncherSettings.Favorites.CONTAINER_HOTSEAT_PREDICTION
import com.android.launcher3.LauncherSettings.Favorites.CONTAINER_WIDGETS_PREDICTION
import com.android.launcher3.LauncherState
import com.android.launcher3.R
import com.android.launcher3.Utilities
import com.android.launcher3.folder.FolderIcon
import com.android.launcher3.model.data.ItemInfo
import com.android.launcher3.model.data.PredictedContainerInfo
import com.android.launcher3.popup.SystemShortcut
import com.android.launcher3.shortcuts.DeepShortcutView
import com.android.launcher3.statemanager.StateManager
import com.android.launcher3.statemanager.StateManager.StateHandler
import com.android.launcher3.uioverrides.QuickstepLauncher
import com.android.launcher3.uioverrides.states.AllAppsState
import com.android.launcher3.uioverrides.states.BackgroundAppState
import com.android.launcher3.uioverrides.states.OverviewState
import com.android.launcher3.util.ActivityOptionsWrapper
import com.android.launcher3.util.Executors
import com.android.launcher3.util.RunnableList
import com.android.launcher3.util.SystemUiController.UI_STATE_BASE_WINDOW
import com.android.launcher3.util.Themes
import com.android.launcher3.util.TouchController
import com.android.launcher3.views.ActivityContext
import com.android.launcher3.views.OptionsPopupView
import com.android.launcher3.views.OptionsPopupView.OptionItem
import com.android.launcher3.widget.LauncherWidgetHolder
import com.android.launcher3.widget.RoundedCornerEnforcement
import com.android.systemui.plugins.shared.LauncherOverlayManager
import com.android.systemui.shared.system.QuickStepContract
import com.kieronquinn.app.smartspacer.sdk.client.SmartspacerClient
import com.patrykmichalik.opto.core.onEach
import dev.kdrag0n.monet.theme.ColorScheme
import java.util.stream.Stream
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class LawnchairLauncher : QuickstepLauncher() {
    private var goreecloudSetupLaunching = false
    private val prefs by unsafeLazy { PreferenceManager.getInstance(this) }
    private val preferenceManager2 by unsafeLazy { PreferenceManager2.getInstance(this) }
    private val insetsController: WindowInsetsControllerCompat by lazy {
        val window = launcher.window
            ?: throw Exception("WindowInsetsControllerCompat not available.")
        WindowInsetsControllerCompat(window, rootView)
    }
    private val themeProvider by unsafeLazy { ThemeProvider.INSTANCE.get(this) }
    private val noStatusBarStateListener = object : StateManager.StateListener<LauncherState> {
        override fun onStateTransitionStart(toState: LauncherState) {
            if (toState is OverviewState) {
                insetsController.show(WindowInsetsCompat.Type.statusBars())
            }
        }
        override fun onStateTransitionComplete(finalState: LauncherState) {
            if (finalState !is OverviewState) {
                insetsController.hide(WindowInsetsCompat.Type.statusBars())
            }
        }
    }
    private val rememberPositionStateListener = object : StateManager.StateListener<LauncherState> {
        override fun onStateTransitionStart(toState: LauncherState) {
            if (toState is AllAppsState) {
                mAppsView.activeRecyclerView.restoreScrollPosition()
            }
        }
        override fun onStateTransitionComplete(finalState: LauncherState) {}
    }
    private val statusBarClockListener = object : StateManager.StateListener<LauncherState> {
        override fun onStateTransitionStart(toState: LauncherState) {
            when (toState) {
                is BackgroundAppState,
                is OverviewState,
                is AllAppsState,
                -> {
                    LawnchairApp.instance.restoreClockInStatusBar()
                }

                else -> {
                    workspace.updateStatusbarClock()
                }
            }
        }
        override fun onStateTransitionComplete(finalState: LauncherState) {}
    }
    private val clearSearchStateListener = object : StateManager.StateListener<LauncherState> {
        override fun onStateTransitionComplete(finalState: LauncherState) {
            if (finalState == LauncherState.NORMAL && mAppsView != null && mAppsView.isSearching) {
                mAppsView?.post {
                    mAppsView.reset(false, true)
                }
            }
        }
    }

    private lateinit var colorScheme: ColorScheme
    private var hasBackGesture = false

    val gestureController by unsafeLazy { GestureController(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        layoutInflater.factory2 = LawnchairLayoutFactory(this)
        super.onCreate(savedInstanceState)
        applyGoreeCloudSystemBarWallpaper()

        // One non-interactive wallpaper behind the workspace; no external sources.
        rootView.addView(
            GoreeCloudWallpaperView(this).apply {
                tag = "goreecloud_wallpaper"
                isClickable = false
                isFocusable = false
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            },
            0,
            android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )

        installGoreeCloudHomeOverviewActions()
        app.lawnchair.views.GoreeCloudOverviewToolbar.install(this)
        dragLayer.post { migrateGoreeCloudDrawerDefaults() }

        prefs.launcherTheme.subscribeChanges(this, ::updateTheme)

        launcher.stateManager.addStateListener(clearSearchStateListener)

        if (prefs.autoLaunchRoot.get()) {
            lifecycleScope.launch {
                try {
                    RootHelperManager.INSTANCE.get(this@LawnchairLauncher)
                } catch (_: RootNotAvailableException) {
                }
            }
        }

        preferenceManager2.showStatusBar.get().distinctUntilChanged().onEach {
            with(insetsController) {
                if (it) {
                    show(WindowInsetsCompat.Type.statusBars())
                } else {
                    hide(WindowInsetsCompat.Type.statusBars())
                }
            }
            with(launcher.stateManager) {
                if (it) {
                    removeStateListener(noStatusBarStateListener)
                } else {
                    addStateListener(noStatusBarStateListener)
                }
            }
        }.launchIn(scope = lifecycleScope)

        preferenceManager2.statusBarClock.get().distinctUntilChanged().onEach {
            with(launcher.stateManager) {
                if (it) {
                    addStateListener(statusBarClockListener)
                } else {
                    removeStateListener(statusBarClockListener)
                    // Make sure status bar clock is restored when the preference is toggled off
                    LawnchairApp.instance.restoreClockInStatusBar()
                }
            }
        }.launchIn(scope = lifecycleScope)
        preferenceManager2.rememberPosition.get().onEach {
            with(launcher.stateManager) {
                if (it) {
                    addStateListener(rememberPositionStateListener)
                } else {
                    removeStateListener(rememberPositionStateListener)
                }
            }
        }.launchIn(scope = lifecycleScope)

        prefs.overrideWindowCornerRadius.subscribeValues(this) {
            QuickStepContract.sHasCustomCornerRadius = it
        }
        prefs.windowCornerRadius.subscribeValues(this) {
            QuickStepContract.sCustomCornerRadius = it.toFloat()
        }
        preferenceManager2.roundedWidgets.onEach(launchIn = lifecycleScope) {
            RoundedCornerEnforcement.sRoundedCornerEnabled = it
        }
        val isWorkspaceDarkText = Themes.getAttrBoolean(this, R.attr.isWorkspaceDarkText)
        preferenceManager2.darkStatusBar.onEach(launchIn = lifecycleScope) { darkStatusBar ->
            systemUiController?.updateUiState(UI_STATE_BASE_WINDOW, isWorkspaceDarkText || darkStatusBar)
        }
        preferenceManager2.backPressGestureHandler.onEach(launchIn = lifecycleScope) { handler ->
            hasBackGesture = handler !is GestureHandlerConfig.NoOp
        }

        LauncherOptionsPopup.restoreMissingPopupOptions(launcher)
        LauncherOptionsPopup.migrateLegacyPreferences(launcher)

        // Handle update from version 12 Alpha 4 to version 12 Alpha 5.
        if (
            prefs.themedIcons.get() &&
            packageManager.getThemedIconPacksInstalled(this).isEmpty()
        ) {
            prefs.themedIcons.set(newValue = false)
        }

        colorScheme = themeProvider.colorScheme

        showQuickstepWarningIfNecessary()

        reloadIconsIfNeeded()

        AppDatabase.INSTANCE.get(this).checkpointSync()
    }

    override fun onNewIntent(intent: Intent?) {
        if (intent != null && intent.action == LawnchairShortcutActivity.START_ACTION) {
            val handlerString = intent.getStringExtra(LawnchairShortcutActivity.EXTRA_HANDLER)
            val config = handlerString?.let { GestureHandlerConfig.fromString(it) }
            if (config != null && config.isExternallyInvokable()) {
                gestureController.handle(config)
            }
        }

        super.onNewIntent(intent)
    }

    override fun collectStateHandlers(out: MutableList<StateHandler<LauncherState>>) {
        super.collectStateHandlers(out)
        out.add(SearchBarStateHandler(this))
    }

    override fun getAllAppsItemLongClickListener(): View.OnLongClickListener {
        return View.OnLongClickListener { view ->
            if (view is FolderIcon && view.mInfo.id != ItemInfo.NO_ID) {
                LawnchairShortcut.showAppDrawerFolderPopup(this, view)
            } else {
                super.getAllAppsItemLongClickListener().onLongClick(view)
            }
        }
    }

    override fun getSupportedShortcuts(container: Int): Stream<SystemShortcut.Factory<*>> = Stream.concat(
        super.getSupportedShortcuts(container),
        Stream.concat(
            Stream.of(LawnchairShortcut.UNINSTALL, LawnchairShortcut.CUSTOMIZE, LawnchairShortcut.OPEN_IN_STORE),
            if (LawnchairApp.isRecentsEnabled) Stream.of(LawnchairShortcut.PAUSE_APPS) else Stream.empty(),
        ),
    )

    fun updateTheme() {
        if (themeProvider.colorScheme != colorScheme) {
            recreate()
        } else {
            mWallpaperThemeManager.updateTheme()
        }
    }

    override fun onStateBack() {
        val searchInput = mAppsView?.searchUiManager?.editText
        val isSearching = mAppsView?.isSearching == true || searchInput?.hasFocus() == true
        if (isSearching) {
            mAppsView?.searchUiManager?.resetSearch()
            allAppsController.animateAllAppsToNoScale()
        } else {
            super.onStateBack()
        }
    }

    override fun createTouchControllers(): Array<TouchController> {
        val verticalSwipeController = VerticalSwipeTouchController(this, gestureController)
        return arrayOf<TouchController>(verticalSwipeController) + super.createTouchControllers()
    }

    override fun handleHomeTap() {
        gestureController.onHomePressed()
    }

    fun bindItems(items: List<ItemInfo>, forceAnimateIcons: Boolean) {
        // pE-TODO(QPR1): Note: null is modelWriter + bindItems override something
        val inflatedItems = items.map { i ->
            Pair.create(
                i,
                itemInflater?.inflateItem(
                    i,
                    null,
                ),
            )
        }.toList()
        bindInflatedItems(inflatedItems, if (forceAnimateIcons) AnimatorSet() else null)
    }

    override fun handleGestureContract(intent: Intent) {
        if (!LawnchairApp.isRecentsEnabled && prefs.enableGnc.get()) {
            val gnc = GestureNavContract.fromIntent(intent)
            if (gnc != null) {
                AbstractFloatingView.closeOpenViews(
                    this,
                    false,
                    AbstractFloatingView.TYPE_ICON_SURFACE,
                )
                LawnchairFloatingSurfaceView.show(this, gnc)
            }
        }
    }

    override fun onUiChangedWhileSleeping() {
        if (Utilities.ATLEAST_S) {
            super.onUiChangedWhileSleeping()
        }
    }

    /** Apply changed defaults once to older installs, then honor later user choices. */
    private fun migrateGoreeCloudDrawerDefaults() {
        val migration = getSharedPreferences("goreecloud_launcher_migrations", Context.MODE_PRIVATE)
        if (migration.getBoolean("drawer_search_hidden_v3", false)) return
        if (!migration.edit().putBoolean("drawer_search_hidden_v3", true).commit()) return
        lifecycleScope.launch {
            if (!preferenceManager2.hideAppDrawerSearchBar.firstCached()) {
                preferenceManager2.hideAppDrawerSearchBar.set(true)
            }
            if (preferenceManager2.showSuggestedAppsInDrawer.firstCached()) {
                preferenceManager2.showSuggestedAppsInDrawer.set(false)
            }
        }
    }

    /** Draw the Home wallpaper continuously behind transparent system bars. */
    private fun applyGoreeCloudSystemBarWallpaper() {
        val targetWindow = window ?: return
        targetWindow.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        targetWindow.statusBarColor = android.graphics.Color.TRANSPARENT
        targetWindow.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            targetWindow.isStatusBarContrastEnforced = false
            targetWindow.isNavigationBarContrastEnforced = false
            targetWindow.navigationBarDividerColor = android.graphics.Color.TRANSPARENT
        }
        WindowInsetsControllerCompat(targetWindow, rootView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }

    private fun installGoreeCloudHomeOverviewActions() {
        val scale = resources.displayMetrics.density
        fun dp(value: Int) = (value * scale + .5f).toInt()
        val bar = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            setPadding(dp(6), dp(2), dp(6), dp(2))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.TRANSPARENT)
                cornerRadius = dp(24).toFloat()
                setStroke(0, android.graphics.Color.TRANSPARENT)
            }
            elevation = 0f
            visibility = View.GONE
            tag = "goreecloud_home_overview_actions"
        }
        fun option(label: String, icon: Int, onClick: () -> Unit) {
            val tile = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
                contentDescription = label
                isClickable = true
                isFocusable = true
                setOnClickListener { onClick() }
            }
            tile.addView(android.widget.ImageView(this).apply {
                setImageResource(icon)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                imageTintList = android.content.res.ColorStateList.valueOf(0xFFFFFFFF.toInt())
            }, android.widget.LinearLayout.LayoutParams(dp(28), dp(28)))
            tile.addView(android.widget.TextView(this).apply {
                text = label
                textSize = 12f
                gravity = android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL
                maxLines = 2
                setTextColor(0xFFFFFFFF.toInt())
            }, android.widget.LinearLayout.LayoutParams(-1, dp(43)))
            bar.addView(tile, android.widget.LinearLayout.LayoutParams(0, dp(80), 1f))
        }
        option("Wallpapers & themes", R.drawable.goreecloud_edit_wallpaper) { GoreeCloudWallpapers.showPicker(this) }
        option("Widgets", R.drawable.goreecloud_edit_widgets) {
            com.android.launcher3.widget.picker.WidgetsFullSheet.show(this, true)
        }
        option("Home screen settings", R.drawable.goreecloud_edit_settings) {
            startActivity(Intent(this, app.lawnchair.ui.preferences.PreferenceActivity::class.java))
        }
        // LauncherRootView is an InsettableFrameLayout: ordinary FrameLayout
        // parameters are converted and can lose gravity, pinning actions to TOP.
        rootView.addView(bar, com.android.launcher3.InsettableFrameLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply {
            ignoreInsets = true
            gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
            bottomMargin = dp(66)
            leftMargin = dp(16)
            rightMargin = dp(16)
        })
        findViewById<app.lawnchair.views.EditModePageStrip>(
            R.id.edit_mode_page_strip,
        )?.let { strip ->
            (strip.layoutParams as? android.widget.FrameLayout.LayoutParams)?.let { params ->
                params.gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
                params.height = dp(64)
                params.bottomMargin = dp(94)
                strip.layoutParams = params
            }
        }
        stateManager.addStateListener(object : StateManager.StateListener<LauncherState> {
            override fun onStateTransitionStart(toState: LauncherState) {
                bar.visibility = if (toState == LauncherState.EDIT_MODE) View.VISIBLE else View.GONE
            }
            override fun onStateTransitionComplete(finalState: LauncherState) {
                bar.visibility = if (finalState == LauncherState.EDIT_MODE) View.VISIBLE else View.GONE
            }
        })
    }

    /** Empty Home long-press opens page overview, never the legacy popup. */
    override fun showDefaultOptions(x: Float, y: Float) {
        stateManager.goToState(LauncherState.EDIT_MODE)
    }

    private fun <T> show(
        activityContext: ActivityContext?,
        targetRect: RectF,
        items: List<OptionItem>,
        shouldAddArrow: Boolean = false,
        width: Int = 0,
    ): OptionsPopupView<T>? where T : Context?, T : ActivityContext? {
        if (activityContext == null) return null

        val isEmpty = WallpaperService.INSTANCE.get(this).getTopWallpapers().isEmpty()
        val layout = if (isEmpty) R.layout.longpress_options_menu else R.layout.wallpaper_options_popup

        val popup = activityContext.layoutInflater.inflate(layout, activityContext.dragLayer, false) as OptionsPopupView<T>
        popup.setTargetRect(targetRect)
        popup.setShouldAddArrow(shouldAddArrow)

        for (item in items) {
            val deepLayout = if (isEmpty) R.layout.system_shortcut else R.layout.wallpaper_options_popup_item

            val view = popup.inflateAndAdd<DeepShortcutView>(deepLayout, popup)
            if (width > 0) view.layoutParams.width = width
            view.iconView.setBackgroundDrawable(item.icon)
            view.bubbleText.text = item.label
            view.setOnClickListener(popup)
            view.setOnLongClickListener(popup)
            popup.mItemMap[view] = item
        }

        popup.show()
        return popup
    }

    fun createAppWidgetHolder(): LauncherWidgetHolder {
        val holder = LauncherWidgetHolder.newInstance(this)
        holder.setAppWidgetRemovedCallback { appWidgetId ->
            workspace.removeWidget(appWidgetId)
        }
        return holder
    }

    override fun makeDefaultActivityOptions(splashScreenStyle: Int): ActivityOptionsWrapper {
        val callbacks = RunnableList()
        val options = if (Utilities.ATLEAST_Q) {
            LawnchairQuickstepCompat.activityOptionsCompat.makeCustomAnimation(
                this,
                0,
                0,
                Executors.MAIN_EXECUTOR.handler,
                null,
            ) {
                callbacks.executeAllAndDestroy()
            }
        } else {
            ActivityOptions.makeBasic()
        }
        if (Utilities.ATLEAST_T) {
            options.splashScreenStyle = splashScreenStyle
        }

        Utilities.allowBGLaunch(options)
        return ActivityOptionsWrapper(options, callbacks)
    }

    override fun getActivityLaunchOptions(v: View?, item: ItemInfo?): ActivityOptionsWrapper {
        return runCatching {
            super.getActivityLaunchOptions(v, item)
        }.getOrElse {
            getActivityLaunchOptionsDefault(v)
        }
    }

    private fun getActivityLaunchOptionsDefault(v: View?): ActivityOptionsWrapper {
        var left = 0
        var top = 0
        var width = v!!.measuredWidth
        var height = v.measuredHeight
        if (v is BubbleTextView) {
            // Launch from center of icon, not entire view
            val icon: Drawable? = v.icon
            if (icon != null) {
                val bounds = icon.bounds
                left = (width - bounds.width()) / 2
                top = v.paddingTop
                width = bounds.width()
                height = bounds.height()
            }
        }
        val options = Utilities.allowBGLaunch(
            ActivityOptions.makeClipRevealAnimation(
                v,
                left,
                top,
                width,
                height,
            ),
        )
        if (Utilities.ATLEAST_T) {
            options.splashScreenStyle = SplashScreen.SPLASH_SCREEN_STYLE_ICON
        }
        options.launchDisplayId = if (v.display != null) v.display.displayId else Display.DEFAULT_DISPLAY
        val callback = RunnableList()
        return ActivityOptionsWrapper(options, callback)
    }

    override fun onPageEndTransition() {
        super.onPageEndTransition()
        if (stateManager.state == LauncherState.EDIT_MODE) {
            app.lawnchair.views.GoreeCloudOverviewFrames.apply(this, true)
            app.lawnchair.views.GoreeCloudOverviewToolbar.refresh(this)
        }
    }

    override fun startActivitySafely(
        v: View?,
        intent: Intent,
        item: ItemInfo?,
    ): com.android.launcher3.util.RunnableList? {
        val started = super.startActivitySafely(v, intent, item)
        if (started != null) {
            GoreeCloudHomeOnboarding.recordLaunch(
                this, intent.component?.packageName ?: item?.targetComponent?.packageName)
        }
        return started
    }

    override fun onResume() {
        super.onResume()
        applyGoreeCloudSystemBarWallpaper()
        restartIfPending()
        refreshPredictionContainersFromModel()
        dragLayer.post {
            if (!isFinishing && !isDestroyed) {
                if (!getSharedPreferences("goreecloud_home_onboarding", Context.MODE_PRIVATE)
                        .getBoolean("onboarding_finished", false)) {
                    if (!goreecloudSetupLaunching) {
                        goreecloudSetupLaunching = true
                        startActivity(Intent(this, GoreeCloudSetupActivity::class.java))
                    }
                } else {
                    goreecloudSetupLaunching = false
                    GoreeCloudHomeOnboarding.showIfNeeded(this)
                }
            }
        }

        dragLayer.viewTreeObserver.addOnDrawListener(
            object : ViewTreeObserver.OnDrawListener {
                private var handled = false

                override fun onDraw() {
                    if (handled) {
                        return
                    }
                    handled = true

                    dragLayer.post {
                        dragLayer.viewTreeObserver.removeOnDrawListener(this)
                        // Drop stuck All Apps RenderEffect on icons after returning home.
                        depthController.clearStuckBlurOnResumeIfHome()
                    }
                }
            },
        )
    }

    override fun onStateSetEnd(state: LauncherState) {
        super.onStateSetEnd(state)
        refreshPredictionContainersFromModel()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Only actually closes if required, safe to call if not enabled
        SmartspacerClient.close()
    }

    override fun getDefaultOverlay(): LauncherOverlayManager = object : LauncherOverlayManager {}

    // GoreeCloud deliberately has no minus-one Feeds page, including plugins.
    override fun onPluginConnected(overlayManager: com.android.systemui.plugins.LauncherOverlayPlugin, context: Context) = Unit

    fun recreateIfNotScheduled() {
        if (sRestartFlags == 0) {
            recreate()
        }
    }

    private fun restartIfPending() {
        when {
            sRestartFlags and FLAG_RESTART != 0 -> lawnchairApp.restart(false)

            sRestartFlags and FLAG_RECREATE != 0 -> {
                sRestartFlags = 0
                recreate()
            }
        }
    }

    private fun refreshPredictionContainersFromModel() {
        LauncherAppState.getInstance(this).model.loadAsync { dataModel ->
            if (dataModel == null || isDestroyed) return@loadAsync

            val predictedContainers = synchronized(dataModel) {
                listOf(
                    dataModel.itemsIdMap[CONTAINER_ALL_APPS_PREDICTION] as? PredictedContainerInfo,
                    dataModel.itemsIdMap[CONTAINER_HOTSEAT_PREDICTION] as? PredictedContainerInfo,
                    dataModel.itemsIdMap[CONTAINER_WIDGETS_PREDICTION] as? PredictedContainerInfo,
                ).filterNotNull()
            }

            Executors.MAIN_EXECUTOR.execute {
                if (isDestroyed) return@execute
                predictedContainers.forEach(::bindPredictedContainerInfo)
            }
        }
    }

    /**
     * Reloads app icons if there is an active icon pack & [PreferenceManager2.alwaysReloadIcons] is enabled.
     */
    private fun reloadIconsIfNeeded() {
        if (
            preferenceManager2.alwaysReloadIcons.firstCached()
        ) {
            LauncherAppState.getInstance(this).model.reloadIfActive()
        }
    }

    companion object {
        private const val FLAG_RECREATE = 1 shl 0
        private const val FLAG_RESTART = 1 shl 1

        var sRestartFlags = 0

        val instance get() = LawnchairApp.launcher
    }
}

val Context.launcher: LawnchairLauncher
    get() = BaseActivity.fromContext(this)

val Context.launcherNullable: LawnchairLauncher? get() = try {
    launcher
} catch (_: IllegalArgumentException) {
    null
}

/**
 * Local GoreeCloud-only wallpaper catalog. No external picker, network data, or
 * system wallpaper permission is needed. The current home layout is preserved.
 */
object GoreeCloudWallpapers {
    private val names = arrayOf("Tide", "Aurora", "Iris")
    private val top = intArrayOf(0xFF051C36.toInt(), 0xFF10143C.toInt(), 0xFF261442.toInt())
    private val lower = intArrayOf(0xFF0B7C98.toInt(), 0xFF155779.toInt(), 0xFF594F8F.toInt())
    private val accent = intArrayOf(0xFF34C3CF.toInt(), 0xFF638DFF.toInt(), 0xFFD17AD8.toInt())

    private fun prefs(context: Context) =
        context.getSharedPreferences("goreecloud_wallpapers", Context.MODE_PRIVATE)

    fun selected(context: Context): Int = prefs(context).getInt("theme", 0).coerceIn(0, 2)

    fun render(canvas: android.graphics.Canvas, width: Int, height: Int, choice: Int) {
        if (width <= 0 || height <= 0) return
        val index = choice.coerceIn(0, 2)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        val w = width.toFloat()
        val h = height.toFloat()
        paint.shader = android.graphics.LinearGradient(
            0f, 0f, w, h, top[index], lower[index],
            android.graphics.Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        val glow = accent[index]
        paint.shader = android.graphics.RadialGradient(
            w * .8f, h * .38f, (w * .9f).coerceAtLeast(1f),
            intArrayOf(
                android.graphics.Color.argb(138, android.graphics.Color.red(glow),
                    android.graphics.Color.green(glow), android.graphics.Color.blue(glow)),
                android.graphics.Color.TRANSPARENT,
            ), null, android.graphics.Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(w * .8f, h * .38f, w * .9f, paint)
        paint.shader = null
        paint.color = android.graphics.Color.argb(18, 255, 255, 255)
        canvas.drawCircle(w * .12f, h * .82f, w * .7f, paint)
    }

    /** Picker is strictly the three bundled GoreeCloud themes. */
    @JvmStatic
    fun showPicker(activity: android.app.Activity) {
        val density = activity.resources.displayMetrics.density
        fun dp(v: Int): Int = (v * density + .5f).toInt()
        val rows = android.widget.LinearLayout(activity).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }
        val dialog = android.app.AlertDialog.Builder(activity)
            .setTitle("GoreeCloud wallpapers")
            .setView(rows)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        names.indices.forEach { choice ->
            val row = android.widget.LinearLayout(activity).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(dp(4), dp(8), dp(4), dp(8))
                isClickable = true
                isFocusable = true
                contentDescription = "Select GoreeCloud ${names[choice]}"
            }
            row.addView(
                GoreeCloudWallpaperView(activity).apply { previewTheme = choice },
                android.widget.LinearLayout.LayoutParams(dp(68), dp(86)),
            )
            row.addView(android.widget.TextView(activity).apply {
                text = "GoreeCloud ${names[choice]}" +
                    if (choice == selected(activity)) " ✓" else ""
                textSize = 16f
                setPadding(dp(12), 0, 0, 0)
            }, android.widget.LinearLayout.LayoutParams(
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f,
            ))
            row.setOnClickListener {
                prefs(activity).edit().putInt("theme", choice).apply()
                activity.window.decorView.findViewWithTag<GoreeCloudWallpaperView>(
                    "goreecloud_wallpaper",
                )?.invalidate()
                dialog.dismiss()
            }
            rows.addView(row)
        }
        dialog.show()
    }
}

/** Native, offline GoreeCloud wallpaper drawn behind workspace content. */
class GoreeCloudWallpaperView @JvmOverloads constructor(
    context: Context,
    attrs: android.util.AttributeSet? = null,
) : View(context, attrs) {
    var previewTheme: Int? = null

    override fun onDraw(canvas: android.graphics.Canvas) {
        super.onDraw(canvas)
        GoreeCloudWallpapers.render(
            canvas, width, height, previewTheme ?: GoreeCloudWallpapers.selected(context),
        )
    }
}
