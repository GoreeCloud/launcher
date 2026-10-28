package com.goreecloud.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Build;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.animation.DecelerateInterpolator;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.Space;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Native HOME source with a bounded local workspace; full Launcher acceptance remains pending. */
public final class LauncherActivity extends Activity {
    private final List<Entry> catalog = new ArrayList<>();
    private final Set<String> pins = new HashSet<>();
    private final List<String> dockOrder = new ArrayList<>();
    private SharedPreferences prefs;
    private WorkspaceState workspace;
    private boolean drawerOpen;
    private boolean choosingDockApp;
    private boolean choosingHomeApp;
    private boolean compactLayout;
    private float systemFontScale;
    private GridLayout grid;
    private LinearLayout rootSurface;
    private View homeStatusContrast; // Decorative, never interactive
    private LinearLayout pageControls;
    private TextView pageLabel;
    private TextView sectionLabel;
    private TextView homeTab;
    private TextView appsTab;
    private LinearLayout dock;
    private LinearLayout dashboard;
    private LinearLayout widgetArea;
    private LinearLayout topHeading;
    private LinearLayout modeTabs;
    private Space workspaceSpacer;
    private static final int EDIT_HOME_REQUEST = 810;
    private static final int PICK_WIDGET_REQUEST = 811;
    private static final int CONFIGURE_WIDGET_REQUEST = 812;
    private static final int WIDGET_HOST_ID = 32019;
    private AppWidgetHost widgetHost;
    private int pendingWidgetId = -1;
    private TextView dockTitle;
    private FrameLayout homeViewport;
    private FrameLayout dragSurface;
    private ScrollView viewportScroll;
    private ScrollView fullPageScroll;
    private float gestureStartX;
    private float gestureStartY;
    private boolean gestureEligible;
    private boolean blankHoldPending;
    private final Handler blankHoldHandler = new Handler(Looper.getMainLooper());
    private final Runnable blankHoldAction = () -> {
        if (!blankHoldPending || drawerOpen) return;
        blankHoldPending = false;
        gestureEligible = false;
        openHomeEditor();
    };
    private EditText query;
    private TextView drawerSettingsAction;
    private TextView drawerSortAction;
    private TextView empty;
    private TextView emptyTitle;
    private TextView emptyAction;
    private LinearLayout emptyPanel;
    private TextView previousPage;
    private TextView nextPage;
    private TextView removePageAction;

    // Listen only while the visible Home activity is active. Package actions
    // are protected Android system broadcasts; do not add manifest receivers
    // or broad package visibility privileges. Coalesce bursts from app updates.
    private final Handler packageChangeHandler = new Handler(Looper.getMainLooper());
    private final Set<String> packagesWithChangedAssets = new HashSet<>();
    private final Runnable reloadVisibleCatalog = () -> {
        Set<String> changed = new HashSet<>(packagesWithChangedAssets);
        packagesWithChangedAssets.clear();
        if (grid != null && !isFinishing()) refreshChangedCatalog(changed);
    };
    private final BroadcastReceiver packageChanges = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent event) {
            if (event == null) return;
            String action = event.getAction();
            if (!Intent.ACTION_PACKAGE_ADDED.equals(action)
                    && !Intent.ACTION_PACKAGE_REMOVED.equals(action)
                    && !Intent.ACTION_PACKAGE_CHANGED.equals(action)
                    && !Intent.ACTION_PACKAGE_REPLACED.equals(action)) return;
            // Icons can change on package update even when labels and activity
            // names remain identical. Retain only the affected package names.
            if (Intent.ACTION_PACKAGE_CHANGED.equals(action)
                    || Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
                Uri data = event.getData();
                if (data != null && data.getSchemeSpecificPart() != null) {
                    packagesWithChangedAssets.add(data.getSchemeSpecificPart());
                }
            }
            packageChangeHandler.removeCallbacks(reloadVisibleCatalog);
            packageChangeHandler.postDelayed(reloadVisibleCatalog, 300);
        }
    };
    private boolean packageReceiverActive;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("launcher", MODE_PRIVATE);
        widgetHost = new AppWidgetHost(this, WIDGET_HOST_ID);
        if (state != null) pendingWidgetId = state.getInt("pendingWidgetId", -1);
        reloadPins();
        workspace = WorkspaceState.decode(prefs.getString("workspace_pages_v1", ""));
        workspace.selectPage(prefs.getInt("workspace_current_v1", 0));
        // Activity recreation (rotation, density or font change) must keep the
        // visible drawer and the user's in-progress local app search. These
        // ephemeral UI values are not part of a persistent workspace backup.
        if (state != null) {
            drawerOpen = state.getBoolean("drawerOpen", false);
            choosingDockApp = state.getBoolean("choosingDockApp", false);
            choosingHomeApp = state.getBoolean("choosingHomeApp", false);
        }
        createScreen();
        if (state != null) {
            String search = state.getString("searchQuery", "");
            if (!search.isEmpty()) query.setText(search);
        }
        refresh();
        // A process restart can inherit a compact ScrollView's previous
        // position from the Android task. A newly opened HOME should display
        // the full clock rather than begin under the status bar; rotation with
        // a saved Activity state still preserves the user's scroll position.
        if (state == null && fullPageScroll != null) {
            fullPageScroll.post(() -> {
                if (!isFinishing() && !isDestroyed()) fullPageScroll.scrollTo(0, 0);
            });
        }
        if (state == null && !prefs.getBoolean("onboarding_completed_v1", false)) {
            startActivity(new Intent(this, LauncherOnboardingActivity.class));
        }
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("drawerOpen", drawerOpen);
        outState.putInt("pendingWidgetId", pendingWidgetId);
        outState.putBoolean("choosingDockApp", choosingDockApp);
        outState.putBoolean("choosingHomeApp", choosingHomeApp);
        outState.putString("searchQuery", query == null ? "" : query.getText().toString());
        super.onSaveInstanceState(outState);
    }

    /** Native workspace gestures, scoped to the content surface, not Settings or search. */
    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                gestureStartX = event.getRawX();
                gestureStartY = event.getRawY();
                gestureEligible = gestureStartedInWorkspace(gestureStartX, gestureStartY);
                blankHoldPending = !drawerOpen && isBlankWorkspacePoint(
                        gestureStartX, gestureStartY);
                blankHoldHandler.removeCallbacks(blankHoldAction);
                if (blankHoldPending) blankHoldHandler.postDelayed(
                        blankHoldAction, android.view.ViewConfiguration.getLongPressTimeout());
                break;
            case MotionEvent.ACTION_MOVE:
                if (blankHoldPending &&
                        (Math.abs(event.getRawX() - gestureStartX) > dp(14)
                        || Math.abs(event.getRawY() - gestureStartY) > dp(14))) {
                    blankHoldPending = false;
                    blankHoldHandler.removeCallbacks(blankHoldAction);
                }
                break;
            case MotionEvent.ACTION_UP:
                blankHoldPending = false;
                blankHoldHandler.removeCallbacks(blankHoldAction);
                if (gestureEligible && homeViewport != null) {
                    boolean atTop = (viewportScroll == null || viewportScroll.getScrollY() == 0)
                            && (fullPageScroll == null || fullPageScroll.getScrollY() == 0);
                    HomeGesturePolicy.Action gesture = HomeGesturePolicy.resolve(
                        event.getRawX() - gestureStartX, event.getRawY() - gestureStartY,
                        dp(96), drawerOpen,
                        prefs.getBoolean("swipe_up_drawer", true),
                        prefs.getBoolean(drawerOpen ? "swipe_down_close" : "swipe_down_search", true), atTop);
                    gestureEligible = false;
                    if (gesture != HomeGesturePolicy.Action.NONE) {
                        // Cancel the child's pending click/long-press rather than
                        // accidentally opening an app while changing launcher pages.
                        MotionEvent cancel = MotionEvent.obtain(event);
                        cancel.setAction(MotionEvent.ACTION_CANCEL);
                        super.dispatchTouchEvent(cancel);
                        cancel.recycle();
                        if (gesture == HomeGesturePolicy.Action.OPEN_DRAWER) setMode(true);
                        else if (gesture == HomeGesturePolicy.Action.CLOSE_DRAWER) setMode(false);
                        else if (gesture == HomeGesturePolicy.Action.OPEN_SEARCH)
                            startActivity(new Intent(this, UniversalSearchActivity.class));
                        else if (gesture == HomeGesturePolicy.Action.PREVIOUS_PAGE) switchPage(-1);
                        else if (gesture == HomeGesturePolicy.Action.NEXT_PAGE) switchPage(1);
                        return true;
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                gestureEligible = false;
                blankHoldPending = false;
                blankHoldHandler.removeCallbacks(blankHoldAction);
                break;
            default:
                break;
        }
        return super.dispatchTouchEvent(event);
    }

    private boolean isBlankWorkspacePoint(float x, float y) {
        if (!containsScreenPoint(homeViewport, x, y)) return false;
        // A shortcut's own long press keeps its native context menu.
        for (int i = 0; i < grid.getChildCount(); i++) {
            if (containsScreenPoint(grid.getChildAt(i), x, y)) return false;
        }
        return true;
    }

    private boolean gestureStartedInWorkspace(float x, float y) {
        // In landscape and large-text mode the entire Home is a scrolling page.
        // A swipe over icons must scroll that page, NOT unexpectedly open All apps.
        // The fixed dock remains an explicit drawer-swipe starting region.
        // The normal, non-scrolling Home still supports swipe-up anywhere on its
        // workspace and swipe-down at the top of the drawer.
        if (compactLayout && !drawerOpen) {
            return containsScreenPoint(dock, x, y);
        }
        return containsScreenPoint(homeViewport, x, y)
                || (!drawerOpen && containsScreenPoint(dock, x, y));
    }

    private static boolean containsScreenPoint(View view, float x, float y) {
        if (view == null || view.getVisibility() != View.VISIBLE) return false;
        int[] pos = new int[2];
        view.getLocationOnScreen(pos);
        return x >= pos[0] && x < pos[0] + view.getWidth()
                && y >= pos[1] && y < pos[1] + view.getHeight();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && Intent.ACTION_MAIN.equals(intent.getAction())
                && intent.hasCategory(Intent.CATEGORY_HOME) && grid != null) {
            setMode(false);
            // Returning HOME is a fresh top-of-desktop gesture, not a request
            // to restore an offscreen landscape scroll offset.
            if (fullPageScroll != null) {
                fullPageScroll.post(() -> fullPageScroll.scrollTo(0, 0));
            }
        }
    }

    @Override protected void onStart() {
        super.onStart();
        try { widgetHost.startListening(); } catch (RuntimeException ignored) {
            // Widget hosting availability must never prevent Home startup.
        }
        if (!packageReceiverActive) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_PACKAGE_ADDED);
            filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
            filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
            filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
            filter.addDataScheme("package");
            // These actions are protected system-only broadcasts, which are
            // exempt from Android 14's exported-receiver flag requirement.
            registerReceiver(packageChanges, filter);
            packageReceiverActive = true;
        }
    }

    @Override protected void onStop() {
        packageChangeHandler.removeCallbacks(reloadVisibleCatalog);
        packagesWithChangedAssets.clear();
        if (packageReceiverActive) {
            unregisterReceiver(packageChanges);
            packageReceiverActive = false;
        }
        if (widgetHost != null) widgetHost.stopListening();
        super.onStop();
    }

    @Override protected void onResume() {
        super.onResume();
        if (grid != null) {
            // The Settings activity may have atomically restored a different saved layout.
            // Reload rather than accidentally showing stale shortcuts until process restart.
            reloadPins();
            workspace = WorkspaceState.decode(prefs.getString("workspace_pages_v1", ""));
            workspace.selectPage(prefs.getInt("workspace_current_v1", 0));
            refresh();
            // Settings may have changed the translucent material while Home paused.
            dock.setBackground(GoreeVisuals.translucentDock(getResources(),
                DockMaterial.normalize(prefs.getString(DockMaterial.KEY, DockMaterial.FROSTED))));
            renderWidgets();
        }
    }

    private void createScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setFocusableInTouchMode(true);
        rootSurface = root;
        root.setBackgroundColor(android.graphics.Color.TRANSPARENT); // System wallpaper
        // Landscape and larger accessibility fonts must not clip Home controls
        // behind the fixed dock. A full-page scrolling surface keeps them reachable.
        systemFontScale = getResources().getConfiguration().fontScale;
        compactLayout = ResponsiveHomePolicy.shouldScroll(
                getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE,
                systemFontScale);
        View insetSurface;
        fullPageScroll = null;
        viewportScroll = null;
        if (compactLayout) {
            ScrollView fullPage = new ScrollView(this);
            fullPageScroll = fullPage;
            fullPage.setFillViewport(true);
            fullPage.setClipToPadding(true);
            fullPage.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            fullPage.addView(root, new ScrollView.LayoutParams(-1, -2));
            insetSurface = fullPage;
        } else {
            insetSurface = root;
        }
        insetSurface.setPadding(dp(20), dp(16), dp(20), dp(12));
        // Preserve GC UI gutters while also respecting status/navigation bars on API 26+.
        insetSurface.setOnApplyWindowInsetsListener((view, insets) -> {
            int top, bottom, left, right;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                top = bars.top; bottom = bars.bottom; left = bars.left; right = bars.right;
            } else {
                top = insets.getSystemWindowInsetTop();
                bottom = insets.getSystemWindowInsetBottom();
                left = insets.getSystemWindowInsetLeft();
                right = insets.getSystemWindowInsetRight();
            }
            view.setPadding(dp(20) + left, dp(16) + top, dp(20) + right, dp(12) + bottom);
            return insets;
        });
        FrameLayout activityFrame = new FrameLayout(this);
        // Paint the wallpaper vignette BELOW the content. A foreground vignette
        // could obscure the Home clock when a scroll position crosses the
        // system-bar region, especially in landscape or with large type.
        // A native top vignette protects status glyph contrast on bright
        // wallpaper patches even when the wallpaper-wide color hint is dark.
        View contrast = new View(this);
        homeStatusContrast = contrast;
        contrast.setClickable(false);
        contrast.setFocusable(false);
        contrast.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        contrast.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xC0000000, 0x88000000, 0x00000000}));
        contrast.setOnApplyWindowInsetsListener((v, insets) -> {
            int top = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                    ? insets.getInsets(WindowInsets.Type.statusBars()).top
                    : insets.getSystemWindowInsetTop();
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) v.getLayoutParams();
            int height = Math.max(dp(48), top + dp(40));
            if (lp.height != height) {
                lp.height = height;
                v.setLayoutParams(lp);
            }
            return insets;
        });
        activityFrame.addView(contrast,
                new FrameLayout.LayoutParams(-1, dp(72), Gravity.TOP));
        activityFrame.addView(insetSurface, new FrameLayout.LayoutParams(-1, -1));
        dragSurface = new FrameLayout(this);
        dragSurface.setVisibility(View.GONE);
        activityFrame.addView(dragSurface, new FrameLayout.LayoutParams(-1, -1));
        setContentView(activityFrame);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                int light = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                controller.setSystemBarsAppearance(light, light);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }

        LinearLayout heading = new LinearLayout(this);
        topHeading = heading;
        heading.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(heading, new LinearLayout.LayoutParams(-1,
                dp(compactLayout ? Math.max(76, Math.round(48 * systemFontScale + 24)) : 48)));

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        TextView title = label("GoreeCloud", 22, true);
        title.setTextColor(0xFFFFFFFF);
        brand.addView(title);
        TextView subtitle = label("Your Android workspace", 12, false);
        subtitle.setTextColor(0xFFE4E3FF);
        brand.addView(subtitle);
        heading.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        TextView settings = label("⚙", 26, false);
        settings.setContentDescription(getString(R.string.settings));
        settings.setGravity(Gravity.CENTER);
        settings.setFocusable(true);
        markAsAccessibilityButton(settings);
        settings.setTextColor(0xFFFFFFFF);
        settings.setBackground(background(0x735E58A3, 18));
        settings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        heading.addView(settings, new LinearLayout.LayoutParams(dp(52), dp(48)));

        LinearLayout modes = new LinearLayout(this);
        modes.setGravity(Gravity.CENTER_VERTICAL);
        modeTabs = modes;
        LinearLayout.LayoutParams modesParams = new LinearLayout.LayoutParams(-1, dp(42));
        modesParams.topMargin = dp(5);
        root.addView(modes, modesParams);
        homeTab = action("Home", () -> setMode(false));
        appsTab = action("All apps", () -> setMode(true));
        modes.addView(homeTab, new LinearLayout.LayoutParams(0, dp(38), 1));
        LinearLayout.LayoutParams appsTabParams = new LinearLayout.LayoutParams(0, dp(38), 1);
        appsTabParams.leftMargin = dp(8);
        modes.addView(appsTab, appsTabParams);
        heading.setOnLongClickListener(v -> { openHomeEditor(); return true; });

        dashboard = HomeDashboardView.create(this, this::openHomeEditor, this::pickWidget);
        LinearLayout.LayoutParams dashboardParams = new LinearLayout.LayoutParams(-1, -2);
        dashboardParams.topMargin = dp(4);
        root.addView(dashboard, dashboardParams);

        widgetArea = new LinearLayout(this);
        widgetArea.setOrientation(LinearLayout.VERTICAL);
        root.addView(widgetArea, new LinearLayout.LayoutParams(-1, -2));

        query = new EditText(this);
        query.setSingleLine(true);
        query.setHint("Search apps");
        query.setTextSize(16);
        query.setPadding(dp(16), dp(9), dp(4), dp(9));
        query.setTextColor(0xFF232243);
        query.setHintTextColor(0xFF68677C);
        query.setBackground(background(0xE9FFFFFF, 28));
        query.setOnFocusChangeListener((v, focused) -> {
            if (focused && !drawerOpen) setMode(true);
        });
        LinearLayout.LayoutParams search = new LinearLayout.LayoutParams(-1, dp(52));
        search.topMargin = dp(8);
        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(searchRow, search);
        searchRow.addView(query, new LinearLayout.LayoutParams(0, -1, 1));
        drawerSettingsAction = action("⚙", () ->
                startActivity(new Intent(this, SettingsActivity.class)));
        drawerSettingsAction.setContentDescription("Launcher Settings");
        drawerSettingsAction.setBackground(background(0xC7EFF4F5, 18));
        LinearLayout.LayoutParams drawerSettingsParams = new LinearLayout.LayoutParams(dp(46), -1);
        drawerSettingsParams.leftMargin = dp(8);
        searchRow.addView(drawerSettingsAction, drawerSettingsParams);
        drawerSortAction = action("⇅", this::showDrawerSort);
        drawerSortAction.setContentDescription("Sort apps");
        drawerSortAction.setBackground(background(0xC7EFF4F5, 18));
        LinearLayout.LayoutParams drawerSortParams = new LinearLayout.LayoutParams(dp(46), -1);
        drawerSortParams.leftMargin = dp(5);
        searchRow.addView(drawerSortAction, drawerSortParams);
        query.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { showApps(); }
            public void afterTextChanged(Editable e) {}
        });

        sectionLabel = label("Home workspace", 17, true);
        LinearLayout.LayoutParams section = new LinearLayout.LayoutParams(-1,
                dp(compactLayout ? Math.max(40, Math.round(26 * systemFontScale + 8)) : 32));
        section.topMargin = dp(12);
        root.addView(sectionLabel, section);
        sectionLabel.setVisibility(View.GONE);

        pageControls = new LinearLayout(this);
        pageControls.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(pageControls, new LinearLayout.LayoutParams(-1, dp(52)));
        previousPage = action("‹", () -> switchPage(-1));
        previousPage.setContentDescription("Previous home page");
        pageControls.addView(previousPage, new LinearLayout.LayoutParams(dp(48), dp(48)));
        pageLabel = label("", 15, true);
        pageLabel.setGravity(Gravity.CENTER);
        pageControls.addView(pageLabel, new LinearLayout.LayoutParams(0, dp(48), 1));
        nextPage = action("›", () -> switchPage(1));
        nextPage.setContentDescription("Next home page");
        pageControls.addView(nextPage, new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView addPage = action("+", this::addPage);
        addPage.setContentDescription("Add home page");
        pageControls.addView(addPage, new LinearLayout.LayoutParams(dp(48), dp(48)));
        removePageAction = action("−", this::removeEmptyPage);
        removePageAction.setContentDescription("Remove empty home page");
        pageControls.addView(removePageAction, new LinearLayout.LayoutParams(dp(48), dp(48)));

        FrameLayout viewport = new FrameLayout(this);
        homeViewport = viewport;
        root.addView(viewport, compactLayout
                ? new LinearLayout.LayoutParams(-1, -2)
                : new LinearLayout.LayoutParams(-1, 0, 1));
        grid = new GridLayout(this);
        if (compactLayout) {
            // One full-page scroller in landscape and enlarged-text mode.
            // Nested ScrollViews had intercepted gestures before the dock was reached.
            viewport.addView(grid, new FrameLayout.LayoutParams(-1, -2));
        } else {
            ScrollView scroll = new ScrollView(this);
            viewportScroll = scroll;
            scroll.setFillViewport(true);
            viewport.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
            // When Home has few shortcuts, keep its icons near the dock
            // instead of floating in the middle of an empty screen.
            LinearLayout canvasColumn = new LinearLayout(this);
            canvasColumn.setOrientation(LinearLayout.VERTICAL);
            scroll.addView(canvasColumn, new ScrollView.LayoutParams(-1, -1));
            workspaceSpacer = new Space(this);
            canvasColumn.addView(workspaceSpacer,
                    new LinearLayout.LayoutParams(-1, 0, 1));
            canvasColumn.addView(grid, new LinearLayout.LayoutParams(-1, -2));
        }

        emptyPanel = new LinearLayout(this);
        emptyPanel.setOrientation(LinearLayout.VERTICAL);
        emptyPanel.setGravity(Gravity.CENTER);
        emptyPanel.setPadding(dp(16), dp(12), dp(16), dp(12));
        emptyPanel.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        FrameLayout.LayoutParams placeholder = new FrameLayout.LayoutParams(-1, dp(68), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        viewport.addView(emptyPanel, placeholder);
        emptyTitle = label("", 17, true);
        emptyTitle.setGravity(Gravity.CENTER);
        emptyTitle.setTextColor(0xFF2C227C);
        emptyTitle.setVisibility(View.GONE);
        emptyPanel.addView(emptyTitle);
        empty = label("", 12, false);
        empty.setGravity(Gravity.CENTER);
        empty.setTextColor(0xFF514A76);
        LinearLayout.LayoutParams note = new LinearLayout.LayoutParams(-1, -2);
        note.topMargin = dp(6);
        empty.setVisibility(View.GONE);
        emptyPanel.addView(empty, note);
        emptyAction = action("+  Add apps", this::beginHomePicker);
        emptyAction.setTextColor(0xFFFFFFFF);
        emptyAction.setBackground(background(0xC82A3141, 23));
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(dp(150), dp(46));
        actionParams.topMargin = dp(8);
        emptyPanel.addView(emptyAction, actionParams);

        // The mockup uses page dots, NOT a toolbar with page arrows and +/-.
        // Page creation/removal stays in the native Home editor.
        root.removeView(pageControls);
        pageControls.removeView(previousPage);
        pageControls.removeView(nextPage);
        pageControls.removeView(removePageAction);
        pageControls.removeView(addPage);
        pageControls.setGravity(Gravity.CENTER);
        pageControls.setMinimumHeight(dp(20));
        root.addView(pageControls, new LinearLayout.LayoutParams(-1, dp(24)));
        dockTitle = label("Pinned apps", 13, true);
        dockTitle.setTextColor(0xFFECEBFF);
        dockTitle.setVisibility(View.GONE);
        root.addView(dockTitle, new LinearLayout.LayoutParams(-1, 0));
        dock = new LinearLayout(this);
        dock.setGravity(Gravity.CENTER);
        dock.setPadding(dp(8), dp(5), dp(8), dp(5));
        dock.setBackground(GoreeVisuals.translucentDock(getResources(), DockMaterial.normalize(prefs.getString(DockMaterial.KEY, DockMaterial.FROSTED)))); // GC Lens
        LinearLayout.LayoutParams dockParams = new LinearLayout.LayoutParams(-1, dp(68));
        dockParams.bottomMargin = dp(6);
        root.addView(dock, dockParams);
        // Avoid an Activity-wide long-press handler: on small/large-font Homes a
        // scroll starting on blank workspace was interpreted as Edit Home.
        // Editing remains available through Customize and header long-press.
        applyAppearance(false);
        renderWidgets();
    }

    private void openHomeEditor() {
        startActivityForResult(new Intent(this, HomeEditActivity.class), EDIT_HOME_REQUEST);
    }

    private void pickWidget() {
        if (prefs.getString("home_widget_ids_v1", "").split(",").length >= 3
                && !prefs.getString("home_widget_ids_v1", "").isEmpty()) {
            Toast.makeText(this, "This preview supports three widgets", Toast.LENGTH_LONG).show();
            return;
        }
        int id;
        try { id = widgetHost.allocateAppWidgetId(); }
        catch (RuntimeException unavailable) {
            Toast.makeText(this, "Widgets are not available on this device", Toast.LENGTH_SHORT).show();
            return;
        }
        pendingWidgetId = id;
        Intent picker = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
        picker.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        try {
            startActivityForResult(picker, PICK_WIDGET_REQUEST);
        } catch (ActivityNotFoundException | SecurityException unavailable) {
            widgetHost.deleteAppWidgetId(id);
            pendingWidgetId = -1;
            Toast.makeText(this, "Android widget picker is unavailable", Toast.LENGTH_SHORT).show();
        }
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == EDIT_HOME_REQUEST) {
            if (result == RESULT_OK && data != null
                    && data.getBooleanExtra(HomeEditActivity.REQUEST_WIDGET, false)) {
                pickWidget();
            }
            refresh();
            return;
        }
        if (request != PICK_WIDGET_REQUEST && request != CONFIGURE_WIDGET_REQUEST) return;
        int chosen = data == null ? pendingWidgetId : data.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId);
        int originallyAllocated = pendingWidgetId;
        if (request == PICK_WIDGET_REQUEST && result == RESULT_OK && chosen > 0) {
            AppWidgetProviderInfo info = AppWidgetManager.getInstance(this).getAppWidgetInfo(chosen);
            if (info != null && info.configure != null) {
                Intent configure = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
                configure.setComponent(info.configure);
                configure.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, chosen);
                pendingWidgetId = chosen;
                try {
                    startActivityForResult(configure, CONFIGURE_WIDGET_REQUEST);
                    if (originallyAllocated > 0 && originallyAllocated != chosen)
                        widgetHost.deleteAppWidgetId(originallyAllocated);
                    return;
                } catch (ActivityNotFoundException | SecurityException exception) {
                    // Never retain an unconfigured provider as a working widget.
                    result = RESULT_CANCELED;
                }
            }
        }
        pendingWidgetId = -1;
        if (result == RESULT_OK && chosen > 0
                && AppWidgetManager.getInstance(this).getAppWidgetInfo(chosen) != null) {
            String saved = prefs.getString("home_widget_ids_v1", "");
            if (!saved.isEmpty()) saved += ",";
            prefs.edit().putString("home_widget_ids_v1", saved + chosen).apply();
            setMode(false);
            renderWidgets();
        } else if (chosen > 0) {
            widgetHost.deleteAppWidgetId(chosen);
        }
        if (originallyAllocated > 0 && originallyAllocated != chosen)
            widgetHost.deleteAppWidgetId(originallyAllocated);
    }

    private void renderWidgets() {
        if (widgetArea == null || widgetHost == null) return;
        widgetArea.removeAllViews();
        String raw = prefs.getString("home_widget_ids_v1", "");
        if (raw.isEmpty()) return;
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        for (String token : raw.split(",")) {
            int id;
            try { id = Integer.parseInt(token); }
            catch (NumberFormatException badId) { continue; }
            AppWidgetProviderInfo info = manager.getAppWidgetInfo(id);
            if (info == null) continue;
            AppWidgetHostView view;
            try { view = widgetHost.createView(this, id, info); }
            catch (RuntimeException unavailable) { continue; }
            LinearLayout panel = new LinearLayout(this);
            panel.setOrientation(LinearLayout.VERTICAL);
            panel.setBackground(background(0xA0EEEFFF, 22));
            LinearLayout.LayoutParams panelParams = new LinearLayout.LayoutParams(-1, -2);
            panelParams.bottomMargin = dp(8);
            widgetArea.addView(panel, panelParams);
            panel.addView(view, new LinearLayout.LayoutParams(-1,
                    dp(Math.max(96, Math.min(220, info.minHeight > 0 ? info.minHeight : 142)))));
            TextView remove = action("Remove widget", () ->
                new AlertDialog.Builder(this).setTitle("Remove this widget?")
                    .setMessage("The original app data will not be deleted.")
                    .setPositiveButton("Remove", (dialog, which) -> removeWidget(id))
                    .setNegativeButton("Cancel", null).show());
            panel.addView(remove, new LinearLayout.LayoutParams(-1, dp(42)));
        }
    }

    private void removeWidget(int id) {
        String raw = prefs.getString("home_widget_ids_v1", "");
        java.util.List<String> remaining = new java.util.ArrayList<>();
        for (String token : raw.split(",")) {
            if (!token.equals(Integer.toString(id)) && !token.isEmpty()) remaining.add(token);
        }
        prefs.edit().putString("home_widget_ids_v1", android.text.TextUtils.join(",", remaining)).apply();
        widgetHost.deleteAppWidgetId(id);
        renderWidgets();
    }

    private void applyAppearance(boolean drawer) {
        // Older violet selection is deliberately retired: the founder
        // rejected that gradient. Wallpaper is always the Home default.
        String skin = prefs.getString("launcher_skin", "wallpaper");
        boolean deviceWallpaper = !drawer && !"night".equals(skin);
        if (deviceWallpaper) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            rootSurface.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            if (fullPageScroll != null) fullPageScroll.setBackgroundColor(
                    android.graphics.Color.TRANSPARENT);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
            android.graphics.drawable.Drawable visual = GoreeVisuals.atmosphere(
                    drawer || "night".equals(skin));
            rootSurface.setBackground(visual);
            if (fullPageScroll != null) fullPageScroll.setBackground(
                    GoreeVisuals.atmosphere(drawer || "night".equals(skin)));
        }
        // Older Android versions may not lay out app views behind the status
        // bar, so use an opaque dark platform bar when the overlay is outside
        // its drawing area. On modern edge-to-edge devices the vignette handles it.
        getWindow().setStatusBarColor(drawer || (deviceWallpaper && Build.VERSION.SDK_INT < 35)
                ? 0xFF191B29 : android.graphics.Color.TRANSPARENT);
        getWindow().setNavigationBarColor(drawer ? 0xFF191B29 : android.graphics.Color.TRANSPARENT);
        // Edge-to-edge Home uses a native dark scrim; drawer/night are already
        // opaque and dark. White system icons remain readable without querying
        // wallpaper pixels, new permissions or whole-image color guesses.
        if (homeStatusContrast != null) {
            homeStatusContrast.setVisibility(deviceWallpaper ? View.VISIBLE : View.GONE);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                int mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                controller.setSystemBarsAppearance(0, mask);
            }
        } else {
            View decor = getWindow().getDecorView();
            int flags = decor.getSystemUiVisibility();
            decor.setSystemUiVisibility(flags
                    & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    & ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
    }

    private void refresh() {
        refreshCatalog(true);
    }

    /** Package broadcasts should never destroy focus or open context menus when
     * the set of installed launchable activities has not actually changed. */
    private void refreshChangedCatalog(Set<String> changedPackages) {
        refreshCatalog(false, changedPackages);
    }

    private void refreshCatalog(boolean forceLayout) {
        refreshCatalog(forceLayout, Collections.emptySet());
    }

    private void refreshCatalog(boolean forceLayout, Set<String> changedPackages) {
        PackageManager pm = getPackageManager();
        Intent discover = new Intent(Intent.ACTION_MAIN);
        discover.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> results = pm.queryIntentActivities(discover, 0);
        Set<String> seen = new HashSet<>();
        List<Entry> discovered = new ArrayList<>();
        for (ResolveInfo item : results) {
            if (item.activityInfo == null) continue;
            ComponentName component = new ComponentName(item.activityInfo.packageName, item.activityInfo.name);
            String key = component.flattenToString();
            if (!seen.add(key) || getPackageName().equals(item.activityInfo.packageName)) continue;
            discovered.add(new Entry(component, String.valueOf(item.loadLabel(pm)), item.loadIcon(pm)));
        }
        discovered.sort(Comparator.comparing(a -> a.name.toLowerCase(java.util.Locale.ROOT)));
        boolean changed = catalog.size() != discovered.size();
        if (!changed) {
            for (int i = 0; i < catalog.size(); i++) {
                Entry before = catalog.get(i), after = discovered.get(i);
                if (!before.component.equals(after.component) || !before.name.equals(after.name)) {
                    changed = true;
                    break;
                }
            }
        }
        if (!changed && !changedPackages.isEmpty()) {
            Set<String> catalogPackages = new HashSet<>();
            for (Entry entry : catalog) catalogPackages.add(entry.component.getPackageName());
            changed = CatalogRefreshPolicy.updatedVisiblePackage(changedPackages, catalogPackages);
        }
        if (!forceLayout && !changed) return;
        catalog.clear();
        catalog.addAll(discovered);
        initializeStarterHomeIfNew();
        // Save unavailable app IDs, do not delete the person's Home/dock layout.
        showApps();
        showDock();
    }

    /** Seed a real, usable desktop ONLY for a new installation. Never overwrite
     * saved workspace pages, pinned apps or existing user's customization. */
    private Entry preferredInstalledApp(String role) {
        Intent candidate;
        switch (role) {
            case "Phone":
                candidate = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:"));
                break;
            case "Messages":
                candidate = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"));
                break;
            case "Email":
                candidate = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"));
                break;
            case "Browser":
                candidate = new Intent(Intent.ACTION_VIEW, Uri.parse("https://example.org"));
                break;
            case "Camera":
                candidate = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                break;
            default: return null;
        }
        ResolveInfo defaultActivity = getPackageManager().resolveActivity(
                candidate, PackageManager.MATCH_DEFAULT_ONLY);
        if (defaultActivity == null || defaultActivity.activityInfo == null) return null;
        String preferredPackage = defaultActivity.activityInfo.packageName;
        for (Entry app : catalog) {
            if (app.component.getPackageName().equals(preferredPackage)) return app;
        }
        return null;
    }

    private void initializeStarterHomeIfNew() {
        if (catalog.isEmpty() || prefs.getBoolean("starter_layout_initialized", false)) return;
        if (prefs.contains("workspace_pages_v1") || prefs.contains("dock_order_v1")
                || prefs.contains("pins")) {
            prefs.edit().putBoolean("starter_layout_initialized", true).apply();
            return;
        }
        String[] homeApps = {"Play Store", "Gallery", "Calendar", "Clock", "Settings"};
        for (String name : homeApps) {
            for (Entry app : catalog) {
                if (app.name.equalsIgnoreCase(name)) {
                    workspace.place(app.component.flattenToString());
                    break;
                }
            }
        }
        for (String role : StarterAppPolicy.ROLES) {
            Entry systemDefault = preferredInstalledApp(role);
            if (systemDefault != null) {
                String id = systemDefault.component.flattenToString();
                if (pins.add(id)) dockOrder.add(id);
                continue;
            }
            for (String alias : StarterAppPolicy.aliases(role)) {
                Entry chosen = null;
                for (Entry app : catalog) {
                    if (app.name.equalsIgnoreCase(alias)) { chosen = app; break; }
                }
                if (chosen != null) {
                    String id = chosen.component.flattenToString();
                    if (pins.add(id)) dockOrder.add(id);
                    break;
                }
            }
        }
        // Two Home pages by default; only seed clean installations.
        if (workspace.pageCount() < 2) {
            workspace.addPage();
            workspace.selectPage(0);
        }
        // Seed markers are persistent. Do not repeatedly reshuffle an empty
        // or deliberately cleared desktop during later app catalog changes.
        prefs.edit().putBoolean("starter_layout_initialized", true).apply();
        saveWorkspace();
        if (!pins.isEmpty()) savePins();
    }

    /** Home pages are locally persisted; the drawer always shows available apps. */
    private void beginHomePicker() {
        choosingHomeApp = true;
        choosingDockApp = false;
        setMode(true);
        showApps();
    }

    private void setMode(boolean showDrawer) {
        boolean openingDrawer = showDrawer && !drawerOpen;
        drawerOpen = showDrawer;
        if (!showDrawer) {
            choosingDockApp = false;
            choosingHomeApp = false;
        }
        if (!showDrawer) {
            query.setText("");
            InputMethodManager keyboard = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (keyboard != null) keyboard.hideSoftInputFromWindow(query.getWindowToken(), 0);
            query.clearFocus();
            rootSurface.requestFocus();
        }
        showApps();
        if (openingDrawer && prefs.getBoolean("drawer_animation", true)) {
            grid.animate().cancel();
            grid.setTranslationY(dp(230));
            grid.setAlpha(0.45f);
            grid.animate().translationY(0f).alpha(1f).setDuration(300)
                    .setInterpolator(new DecelerateInterpolator()).start();
        } else {
            grid.animate().cancel();
            grid.setTranslationY(0f);
            grid.setAlpha(1f);
        }
    }

    private void switchPage(int offset) {
        if (workspace.selectPage(workspace.currentPage() + offset)) {
            saveWorkspace();
            grid.animate().cancel();
            showApps();
            grid.setTranslationX(dp(offset > 0 ? 65 : -65));
            grid.setAlpha(0.58f);
            grid.animate().translationX(0f).alpha(1f).setDuration(220)
                    .setInterpolator(new DecelerateInterpolator()).start();
        }
    }

    private void addPage() {
        if (workspace.addPage()) {
            saveWorkspace();
            setMode(false);
        } else {
            Toast.makeText(this, "Maximum of seven home pages", Toast.LENGTH_SHORT).show();
        }
    }

    private void removeEmptyPage() {
        if (workspace.removeCurrentEmptyPage()) {
            saveWorkspace();
            setMode(false);
        } else {
            Toast.makeText(this, "Only an empty extra page can be removed", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveWorkspace() {
        prefs.edit().putString("workspace_pages_v1", workspace.encode())
            .putInt("workspace_current_v1", workspace.currentPage()).apply();
    }

    private TextView action(String title, Runnable onClick) {
        TextView v = label(title, 16, true);
        v.setGravity(Gravity.CENTER);
        v.setFocusable(true);
        markAsAccessibilityButton(v);
        v.setBackground(background(0xFFE8E6FF, 18));
        v.setOnClickListener(ignored -> onClick.run());
        return v;
    }

    private void showApps() {
        if (grid == null) return;
        grid.removeAllViews();
        boolean drawerMode = drawerOpen || !query.getText().toString().trim().isEmpty();
        int columns = ResponsiveHomePolicy.visibleColumns(
                prefs.getInt(drawerMode ? "drawer_columns" : "columns", 4), systemFontScale);
        grid.setColumnCount(columns);
        boolean names = prefs.getBoolean("labels", true);
        String text = query.getText().toString();
        boolean allApps = drawerOpen || !text.trim().isEmpty();
        pageControls.setVisibility(allApps ? View.GONE : View.VISIBLE);
        if (workspaceSpacer != null) workspaceSpacer.setVisibility(allApps ? View.GONE : View.VISIBLE);
        // Home is a desktop, not a two-tab workspace settings app.
        topHeading.setVisibility(View.GONE);
        modeTabs.setVisibility(View.GONE);
        drawerSettingsAction.setVisibility(allApps ? View.VISIBLE : View.GONE);
        drawerSortAction.setVisibility(allApps ? View.VISIBLE : View.GONE);
        boolean showDockOnHome = !allApps && prefs.getBoolean("dock_enabled", true);
        dock.setVisibility(showDockOnHome ? View.VISIBLE : View.GONE);
        dockTitle.setVisibility(View.GONE);
        // Space-first responsive Home: a decorative dashboard must not hide the
        // real workspace, shortcut recovery, or dock in landscape/large text.
        dashboard.setVisibility(allApps ? View.GONE : View.VISIBLE);
        widgetArea.setVisibility(allApps ? View.GONE : View.VISIBLE);
        topHeading.setVisibility(View.GONE);
        applyAppearance(allApps);
        sectionLabel.setTextColor(0xFFF8F7FF);
        sectionLabel.setVisibility((choosingDockApp || choosingHomeApp) && allApps
                ? View.VISIBLE : View.GONE);
        sectionLabel.setText(choosingDockApp && allApps ? "Choose an app for your dock"
                : choosingHomeApp && allApps ? "Choose an app for Home"
                : allApps ? "All applications" : "Home workspace");
        homeTab.setBackground(background(allApps ? 0xB0B8B0E4 : 0xFF6557E3, 22));
        appsTab.setBackground(background(allApps ? 0xFF6557E3 : 0xB0B8B0E4, 22));
        homeTab.setTextColor(allApps ? 0xFF2C227C : 0xFFFFFFFF);
        appsTab.setTextColor(allApps ? 0xFFFFFFFF : 0xFF2C227C);
        homeTab.setSelected(!allApps);
        appsTab.setSelected(allApps);
        homeTab.setContentDescription(allApps ? "Home tab" : "Home tab, selected");
        appsTab.setContentDescription(allApps ? "All apps tab, selected" : "All apps tab");
        StringBuilder dots = new StringBuilder();
        for (int i = 0; i < workspace.pageCount(); i++) {
            if (i > 0) dots.append("  ");
            dots.append(i == workspace.currentPage() ? "●" : "○");
        }
        pageLabel.setText(dots.toString());
        pageLabel.setContentDescription("Home page " + (workspace.currentPage() + 1)
                + " of " + workspace.pageCount());
        pageLabel.setTextColor(0xFFF8F8FC);
        pageLabel.setTextSize(12);
        boolean canPrevious = workspace.currentPage() > 0;
        boolean canNext = workspace.currentPage() + 1 < workspace.pageCount();
        boolean canRemove = workspace.pageCount() > 1 && workspace.currentItems().isEmpty();
        previousPage.setEnabled(canPrevious);
        nextPage.setEnabled(canNext);
        removePageAction.setEnabled(canRemove);
        previousPage.setAlpha(canPrevious ? 1f : 0.38f);
        nextPage.setAlpha(canNext ? 1f : 0.38f);
        removePageAction.setAlpha(canRemove ? 1f : 0.38f);

        List<Entry> shown = new ArrayList<>();
        if (allApps) {
            shown.addAll(AppNameFilter.filter(catalog, text, e -> e.name));
            if ("za".equals(prefs.getString("drawer_sort", "az")))
                Collections.reverse(shown);
        } else {
            for (String key : workspace.currentItems()) {
                Entry item = find(key);
                if (item != null) shown.add(item);
            }
        }
        // Real grid slots: a sparse Home page must not stretch its one shortcut
        // across the row. Bound widths on tablets and scale icons on compact phones.
        int availableWidth = getResources().getDisplayMetrics().widthPixels - dp(40);
        int slotWidth = Math.max(dp(32), Math.min(
                dp(ResponsiveHomePolicy.maximumTileWidthDp(systemFontScale)),
                (availableWidth - dp(4) * columns) / columns));
        int iconSize = Math.min(dp(Math.max(36, Math.min(60, prefs.getInt("icon_size", 48)))),
                Math.max(dp(22), slotWidth - dp(12)));
        int preferredRows = StarterAppPolicy.boundedRows(
                prefs.getInt(drawerMode ? "drawer_rows" : "home_rows", 5));
        int rowHeight = drawerMode
                ? dp(ResponsiveHomePolicy.drawerTileHeightDp(names, systemFontScale))
                : compactLayout
                    ? dp(ResponsiveHomePolicy.shortcutHeightDp(names, true, systemFontScale))
                    : Math.max(dp(88), Math.min(dp(150),
                        (getResources().getDisplayMetrics().heightPixels - dp(300)) / preferredRows));
        for (int i = 0; i < shown.size(); i++) {
            View tile = tile(shown.get(i), names, iconSize, false);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(i / columns), GridLayout.spec(i % columns));
            params.width = slotWidth;
            params.height = rowHeight;
            params.setMargins(dp(2), dp(2), dp(2), dp(2));
            grid.addView(tile, params);
        }
        // No plus widgets or page buttons on the desktop. Edit via long-press.
        if (shown.isEmpty()) {
            if (!allApps) {
                if (workspace.currentItems().isEmpty()) {
                    emptyTitle.setText("Your Home screen");
                    empty.setText("Choose your first app below, or swipe up to browse the app drawer.");
                } else {
                    emptyTitle.setText(R.string.shortcuts_unavailable);
                    empty.setText(R.string.shortcuts_unavailable_help);
                }
            } else {
                emptyTitle.setText(catalog.isEmpty() ? "No apps yet" : "Nothing matches");
                empty.setText(catalog.isEmpty() ? getString(R.string.empty)
                    : "Try another application name.");
            }
            emptyAction.setVisibility(View.GONE);
            emptyPanel.setVisibility(allApps ? View.VISIBLE : View.GONE);
        } else {
            emptyPanel.setVisibility(View.GONE);
        }
    }

    private View tile(Entry app, boolean names, int iconSize, boolean dockTile) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);
        cell.setPadding(dp(2), dp(6), dp(2), dp(6));
        boolean inDrawer = drawerOpen && !dockTile;
        // Individual icons sit directly on wallpaper/drawer like a launcher;
        // no square cards behind each application.
        cell.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        cell.setContentDescription(app.name);
        cell.setFocusable(true);
        markTileAccessible(cell, app, dockTile);
        ImageView image = new ImageView(this);
        image.setImageDrawable(app.icon);
        cell.addView(image, new LinearLayout.LayoutParams(iconSize, iconSize));
        if (names) {
            TextView text = label(app.name, 12, false);
            text.setTextColor(0xFFFFFFFF);
            text.setGravity(Gravity.CENTER);
            text.setMaxLines(2);
            text.setEllipsize(android.text.TextUtils.TruncateAt.END);
            cell.addView(text, new LinearLayout.LayoutParams(-1,
                    dp(ResponsiveHomePolicy.labelHeightDp(systemFontScale))));
        }
        cell.setOnClickListener(v -> {
            if (!dockTile && choosingHomeApp) {
                if (workspace.place(app.component.flattenToString())) {
                    saveWorkspace();
                    setMode(false);
                } else {
                    Toast.makeText(this, "Could not add app to this Home page",
                            Toast.LENGTH_SHORT).show();
                }
            } else if (!dockTile && choosingDockApp) {
                String id = app.component.flattenToString();
                if (!pins.contains(id)) {
                    if (pins.size() >= 5) {
                        Toast.makeText(this, "Dock supports up to five apps",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    pins.add(id);
                    dockOrder.add(id);
                    savePins();
                }
                setMode(false);
                showDock();
            } else {
                launch(app);
            }
        });
        cell.setOnLongClickListener(v -> { showActions(cell, app); return true; });
        attachDragAndContextGesture(cell, app, dockTile);
        // An external keyboard must not require a touch long-press to place,
        // reorder or pin apps. Android Menu and Shift+F10 expose the same menu.
        cell.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_UP) return false;
            if (keyCode == KeyEvent.KEYCODE_MENU
                    || (keyCode == KeyEvent.KEYCODE_F10 && event.isShiftPressed())) {
                showActions(cell, app);
                return true;
            }
            return false;
        });
        return cell;
    }

    private List<String> visibleDockKeys() {
        List<String> keys = new ArrayList<>();
        for (String pinned : dockOrder) if (find(pinned) != null) keys.add(pinned);
        return keys;
    }

    private void showDock() {
        dock.removeAllViews();
        for (String key : dockOrder) {
            Entry app = find(key);
            if (app == null) continue;
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(72), 1);
            params.setMargins(dp(2), 0, dp(2), 0);
            dock.addView(tile(app, prefs.getBoolean("dock_labels", false), dp(44), true), params);
        }
        int occupied = dock.getChildCount();
        for (int i = occupied; i < 5; i++) {
            TextView blank = action(" ", () -> {
                choosingDockApp = true;
                choosingHomeApp = false;
                setMode(true);
                showApps();
            });
            blank.setContentDescription("Add app to dock");
            blank.setBackground(background(0x57404758, 40));
            dock.addView(blank, new LinearLayout.LayoutParams(0, dp(56), 1));
        }
    }

    /** Hold then move a shortcut to start Android drag; a stationary hold opens its menu. */
    private void attachDragAndContextGesture(View cell, Entry app, boolean dockTile) {
        final float[] start = new float[2];
        final long[] down = new long[1];
        final boolean[] moved = new boolean[1], menu = new boolean[1], dragging = new boolean[1];
        final Runnable openMenu = () -> {
            if (!moved[0] && !dragging[0]) {
                menu[0] = true;
                showActions(cell, app);
            }
        };
        cell.setOnTouchListener((v, event) -> {
            long elapsed = android.os.SystemClock.uptimeMillis() - down[0];
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    start[0] = event.getRawX();
                    start[1] = event.getRawY();
                    down[0] = android.os.SystemClock.uptimeMillis();
                    moved[0] = false;
                    menu[0] = false;
                    dragging[0] = false;
                    cell.postDelayed(openMenu,
                            android.view.ViewConfiguration.getLongPressTimeout() + 160L);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (Math.abs(event.getRawX() - start[0]) > dp(18)
                            || Math.abs(event.getRawY() - start[1]) > dp(18)) {
                        if (!moved[0] && !menu[0] && !dockTile
                                && elapsed >= android.view.ViewConfiguration.getLongPressTimeout() - 70) {
                            cell.removeCallbacks(openMenu);
                            if (beginShortcutDrag(cell, app)) dragging[0] = true;
                        }
                        moved[0] = true;
                        cell.removeCallbacks(openMenu);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    cell.removeCallbacks(openMenu);
                    if (!moved[0] && !dragging[0] && !menu[0]) {
                        if (elapsed >= android.view.ViewConfiguration.getLongPressTimeout())
                            showActions(cell, app);
                        else cell.performClick();
                    }
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    cell.removeCallbacks(openMenu);
                    return true;
                default: return true;
            }
        });
    }

    private boolean beginShortcutDrag(View cell, Entry app) {
        if (dragSurface == null) return false;
        HomeDragOverlay.show(this, dragSurface, workspace, prefs.getInt("columns", 4),
                (key, page, slot) -> {
                    if (!workspace.placeAt(key, page, slot)) {
                        Toast.makeText(this, "This Home page is full", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    saveWorkspace();
                    setMode(false);
                    return true;
                });
        String key = app.component.flattenToString();
        boolean started = cell.startDragAndDrop(
                android.content.ClipData.newPlainText("GoreeCloud shortcut", key),
                new View.DragShadowBuilder(cell), key, 0);
        if (!started) dragSurface.setVisibility(View.GONE);
        return started;
    }

    private void showDrawerSort() {
        // An anchored popup obscured the first row of apps on real phones.
        // Use an explicit single-choice Android dialog with current selection.
        String[] options = {"Name A–Z", "Name Z–A"};
        int selected = "za".equals(prefs.getString("drawer_sort", "az")) ? 1 : 0;
        new AlertDialog.Builder(this)
            .setTitle("Sort apps")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                prefs.edit().putString("drawer_sort", which == 1 ? "za" : "az").apply();
                dialog.dismiss();
                showApps();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showActions(View target, Entry app) {
        PopupMenu menu = new PopupMenu(this, target);
        String id = app.component.flattenToString();
        int existing = workspace.pageFor(id);
        menu.getMenu().add(existing == workspace.currentPage()
                ? "Remove from home page" : existing >= 0 ? "Move to current home page"
                : "Add to current home page").setOnMenuItemClickListener(item -> {
            if (existing == workspace.currentPage()) {
                workspace.remove(id);
            } else if (!workspace.place(id)) {
                Toast.makeText(this, "Page full (24 apps)", Toast.LENGTH_SHORT).show();
                return true;
            }
            saveWorkspace();
            showApps();
            return true;
        });
        // Menu actions are keyboard and accessibility-friendly alternatives to drag/drop.
        // Only offer page operations for a shortcut actually on the visible Home page.
        if (!drawerOpen && existing == workspace.currentPage()) {
            int slot = workspace.currentItems().indexOf(id);
            if (slot > 0) {
                menu.getMenu().add("Move earlier").setOnMenuItemClickListener(item -> {
                    if (workspace.moveWithinCurrentPage(id, -1)) {
                        saveWorkspace();
                        showApps();
                    }
                    return true;
                });
            }
            if (slot >= 0 && slot < workspace.currentItems().size() - 1) {
                menu.getMenu().add("Move later").setOnMenuItemClickListener(item -> {
                    if (workspace.moveWithinCurrentPage(id, 1)) {
                        saveWorkspace();
                        showApps();
                    }
                    return true;
                });
            }
            if (workspace.currentPage() > 0) {
                menu.getMenu().add("Move to previous page").setOnMenuItemClickListener(item -> {
                    if (workspace.moveToAdjacentPage(id, -1)) {
                        saveWorkspace();
                        showApps();
                    }
                    return true;
                });
            }
            if (workspace.currentPage() + 1 < workspace.pageCount()) {
                menu.getMenu().add("Move to next page").setOnMenuItemClickListener(item -> {
                    if (!workspace.moveToAdjacentPage(id, 1)) {
                        Toast.makeText(this, "Destination page is full", Toast.LENGTH_SHORT).show();
                    } else {
                        saveWorkspace();
                        showApps();
                    }
                    return true;
                });
            }
        }
        List<String> shownDock = visibleDockKeys();
        int dockSlot = shownDock.indexOf(id);
        if (dockSlot > 0) {
            String left = shownDock.get(dockSlot - 1);
            menu.getMenu().add("Move left in dock").setOnMenuItemClickListener(item -> {
                if (DockOrder.swap(dockOrder, id, left)) { savePins(); showDock(); }
                return true;
            });
        }
        if (dockSlot >= 0 && dockSlot < shownDock.size() - 1) {
            String right = shownDock.get(dockSlot + 1);
            menu.getMenu().add("Move right in dock").setOnMenuItemClickListener(item -> {
                if (DockOrder.swap(dockOrder, id, right)) { savePins(); showDock(); }
                return true;
            });
        }
        menu.getMenu().add(pins.contains(id) ? R.string.unpin : R.string.pin)
            .setOnMenuItemClickListener(item -> {
                if (pins.remove(id)) {
                    dockOrder.remove(id);
                } else {
                    if (pins.size() >= 5) {
                        Toast.makeText(this, "Dock supports up to five apps", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    pins.add(id);
                    dockOrder.add(id);
                }
                savePins();
                showDock();
                return true;
            });
        boolean locked=LauncherAppLock.locked(this,app.component.getPackageName());
        menu.getMenu().add(locked?"Unlock":"Lock").setOnMenuItemClickListener(item->{
            LauncherAppLock.setLocked(this,app.component.getPackageName(),!locked,()->{
                showApps();showDock();
            });
            return true;
        });
        try {
            android.content.pm.ApplicationInfo info = getPackageManager()
                    .getApplicationInfo(app.component.getPackageName(), 0);
            if ((info.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0) {
                menu.getMenu().add("Uninstall…").setOnMenuItemClickListener(item -> {
                    // An app protected by the launcher also requires its PIN
                    // before *this launcher* opens the system uninstall flow.
                    // Android Settings remains outside our security boundary.
                    LauncherAppLock.guard(this, app.component.getPackageName(), () -> {
                        try {
                            startActivity(new Intent(Intent.ACTION_DELETE,
                                    Uri.parse("package:" + app.component.getPackageName())));
                        } catch (ActivityNotFoundException | SecurityException ex) {
                            Toast.makeText(this, "Uninstall unavailable",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                    return true;
                });
            }
        } catch (PackageManager.NameNotFoundException ignored) {}
        menu.getMenu().add(R.string.app_info).setOnMenuItemClickListener(item -> {
            try {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + app.component.getPackageName())));
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(this, R.string.launch_failed, Toast.LENGTH_SHORT).show();
            }
            return true;
        });
        menu.show();
    }

    private void launch(Entry app) {
        LauncherAppLock.guard(this,app.component.getPackageName(),()->launchUnlocked(app));
    }
    private void launchUnlocked(Entry app) {
        Intent open = Intent.makeMainActivity(app.component);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try { startActivity(open); }
        catch (ActivityNotFoundException | SecurityException ex) {
            Toast.makeText(this, R.string.launch_failed, Toast.LENGTH_SHORT).show();
            refresh();
        }
    }

    private Entry find(String key) {
        for (Entry entry : catalog) if (entry.component.flattenToString().equals(key)) return entry;
        return null;
    }
    private void reloadPins() {
        pins.clear();
        pins.addAll(prefs.getStringSet("pins", Collections.emptySet()));
        dockOrder.clear();
        dockOrder.addAll(DockOrder.reconcile(pins, prefs.getString("dock_order_v1", "")));
    }
    private void savePins() {
        // Explicit user gestures are rare; persist the set and its custom order
        // before the activity can be stopped or an accessibility client exits.
        if (!prefs.edit().putStringSet("pins", new HashSet<>(pins))
                .putString("dock_order_v1", DockOrder.encode(dockOrder)).commit()) {
            Toast.makeText(this, "Could not save dock changes", Toast.LENGTH_SHORT).show();
        }
    }
    /** Native app shortcut semantics and direct, non-gesture dock controls. */
    private void markTileAccessible(View cell, Entry app, boolean dockTile) {
        final String id = app.component.flattenToString();
        cell.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override public void onInitializeAccessibilityNodeInfo(
                    View host, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(android.widget.Button.class.getName());
                if (!dockTile) return;
                List<String> visible = visibleDockKeys();
                int index = visible.indexOf(id);
                if (index < 0) return;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    info.setHintText("Dock shortcut " + (index + 1) + " of " + visible.size());
                }
                if (index > 0) {
                    info.addAction(new AccessibilityNodeInfo.AccessibilityAction(
                            R.id.a11y_dock_move_left, "Move left in dock"));
                }
                if (index + 1 < visible.size()) {
                    info.addAction(new AccessibilityNodeInfo.AccessibilityAction(
                            R.id.a11y_dock_move_right, "Move right in dock"));
                }
            }

            @Override public boolean performAccessibilityAction(
                    View host, int action, Bundle args) {
                if (dockTile && (action == R.id.a11y_dock_move_left
                        || action == R.id.a11y_dock_move_right)) {
                    List<String> visible = visibleDockKeys();
                    int index = visible.indexOf(id);
                    int offset = action == R.id.a11y_dock_move_left ? -1 : 1;
                    int destination = index + offset;
                    if (index < 0 || destination < 0 || destination >= visible.size()) return false;
                    if (!DockOrder.swap(dockOrder, id, visible.get(destination))) return false;
                    savePins();
                    showDock();
                    dock.announceForAccessibility(app.name + ", dock position "
                            + (destination + 1) + " of " + visible.size());
                    return true;
                }
                return super.performAccessibilityAction(host, action, args);
            }
        });
    }

    /** Expose styled interactive text as a native accessibility button. */
    private static void markAsAccessibilityButton(View view) {
        view.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override public void onInitializeAccessibilityNodeInfo(
                    View host, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(android.widget.Button.class.getName());
            }
        });
    }

    private TextView label(String text, int size, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(0xFF211F30);
        if (bold) view.setTypeface(null, Typeface.BOLD);
        return view;
    }
    private GradientDrawable background(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }
    private int dp(int count) {
        return Math.round(count * getResources().getDisplayMetrics().density);
    }
    private static final class Entry {
        final ComponentName component;
        final String name;
        final Drawable icon;
        Entry(ComponentName c, String n, Drawable i) { component = c; name = n; icon = i; }
    }
}
