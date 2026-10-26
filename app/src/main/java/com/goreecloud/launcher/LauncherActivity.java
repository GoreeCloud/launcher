package com.goreecloud.launcher;

import android.app.Activity;
import android.content.ActivityNotFoundException;
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
import android.os.Build;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
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
    private SharedPreferences prefs;
    private WorkspaceState workspace;
    private boolean drawerOpen;
    private boolean compactLayout;
    private float systemFontScale;
    private GridLayout grid;
    private LinearLayout rootSurface;
    private LinearLayout pageControls;
    private TextView pageLabel;
    private TextView sectionLabel;
    private TextView homeTab;
    private TextView appsTab;
    private LinearLayout dock;
    private EditText query;
    private TextView empty;
    private TextView emptyTitle;
    private TextView emptyAction;
    private LinearLayout emptyPanel;
    private TextView previousPage;
    private TextView nextPage;
    private TextView removePageAction;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("launcher", MODE_PRIVATE);
        pins.addAll(prefs.getStringSet("pins", Collections.emptySet()));
        workspace = WorkspaceState.decode(prefs.getString("workspace_pages_v1", ""));
        workspace.selectPage(prefs.getInt("workspace_current_v1", 0));
        createScreen();
        refresh();
    }

    @Override protected void onResume() {
        super.onResume();
        if (grid != null) {
            // The Settings activity may have atomically restored a different saved layout.
            // Reload rather than accidentally showing stale shortcuts until process restart.
            pins.clear();
            pins.addAll(prefs.getStringSet("pins", Collections.emptySet()));
            workspace = WorkspaceState.decode(prefs.getString("workspace_pages_v1", ""));
            workspace.selectPage(prefs.getInt("workspace_current_v1", 0));
            refresh();
        }
    }

    private void createScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setFocusableInTouchMode(true);
        rootSurface = root;
        root.setBackgroundColor(0xFFF4F3FC); // GC UI Canvas
        // Landscape and larger accessibility fonts must not clip Home controls
        // behind the fixed dock. A full-page scrolling surface keeps them reachable.
        systemFontScale = getResources().getConfiguration().fontScale;
        compactLayout = ResponsiveHomePolicy.shouldScroll(
                getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE,
                systemFontScale);
        View insetSurface;
        if (compactLayout) {
            ScrollView fullPage = new ScrollView(this);
            fullPage.setFillViewport(true);
            fullPage.setClipToPadding(true);
            fullPage.setBackgroundColor(0xFFF4F3FC);
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
        setContentView(insetSurface);
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
        heading.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(heading, new LinearLayout.LayoutParams(-1,
                dp(compactLayout ? Math.max(76, Math.round(48 * systemFontScale + 24)) : 60)));

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        TextView title = label("GoreeCloud", 25, true);
        brand.addView(title);
        TextView subtitle = label("Your Android workspace", 12, false);
        subtitle.setTextColor(0xFF5C596B);
        brand.addView(subtitle);
        heading.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        TextView settings = label("⚙", 26, false);
        settings.setContentDescription(getString(R.string.settings));
        settings.setGravity(Gravity.CENTER);
        settings.setFocusable(true);
        settings.setBackground(background(0xFFFFFFFF, 18));
        settings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        heading.addView(settings, new LinearLayout.LayoutParams(dp(52), dp(48)));

        LinearLayout modes = new LinearLayout(this);
        modes.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams modesParams = new LinearLayout.LayoutParams(-1, dp(52));
        modesParams.topMargin = dp(8);
        root.addView(modes, modesParams);
        homeTab = action("Home", () -> setMode(false));
        appsTab = action("All apps", () -> setMode(true));
        modes.addView(homeTab, new LinearLayout.LayoutParams(0, dp(48), 1));
        LinearLayout.LayoutParams appsTabParams = new LinearLayout.LayoutParams(0, dp(48), 1);
        appsTabParams.leftMargin = dp(8);
        modes.addView(appsTab, appsTabParams);

        query = new EditText(this);
        query.setSingleLine(true);
        query.setHint(R.string.search_hint);
        query.setTextSize(16);
        query.setPadding(dp(16), dp(9), dp(16), dp(9));
        query.setBackground(background(0xFFFFFFFF, 18));
        LinearLayout.LayoutParams search = new LinearLayout.LayoutParams(-1, dp(52));
        search.topMargin = dp(16);
        root.addView(query, search);
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
        root.addView(viewport, compactLayout
                ? new LinearLayout.LayoutParams(-1, dp(232))
                : new LinearLayout.LayoutParams(-1, 0, 1));
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        viewport.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        grid = new GridLayout(this);
        scroll.addView(grid);

        emptyPanel = new LinearLayout(this);
        emptyPanel.setOrientation(LinearLayout.VERTICAL);
        emptyPanel.setGravity(Gravity.CENTER);
        emptyPanel.setPadding(dp(20), dp(28), dp(20), dp(28));
        emptyPanel.setBackground(background(0xFFE8E6FF, 26));
        FrameLayout.LayoutParams placeholder = new FrameLayout.LayoutParams(-1, dp(230), Gravity.CENTER);
        viewport.addView(emptyPanel, placeholder);
        emptyTitle = label("", 22, true);
        emptyTitle.setGravity(Gravity.CENTER);
        emptyTitle.setTextColor(0xFF2C227C);
        emptyPanel.addView(emptyTitle);
        empty = label("", 15, false);
        empty.setGravity(Gravity.CENTER);
        empty.setTextColor(0xFF514A76);
        LinearLayout.LayoutParams note = new LinearLayout.LayoutParams(-1, -2);
        note.topMargin = dp(12);
        emptyPanel.addView(empty, note);
        emptyAction = action("Browse all apps", () -> setMode(true));
        emptyAction.setTextColor(0xFFFFFFFF);
        emptyAction.setBackground(background(0xFF5145D9, 18));
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(-1, dp(48));
        actionParams.topMargin = dp(20);
        emptyPanel.addView(emptyAction, actionParams);

        TextView dockTitle = label("Pinned apps", 16, true);
        root.addView(dockTitle, new LinearLayout.LayoutParams(-1, dp(36)));
        dock = new LinearLayout(this);
        dock.setGravity(Gravity.CENTER);
        dock.setPadding(dp(6), dp(7), dp(6), dp(7));
        dock.setBackground(background(0xFFECEAF8, 26)); // GC UI Lens tonal fallback
        root.addView(dock, new LinearLayout.LayoutParams(-1, dp(90)));
    }

    private void refresh() {
        PackageManager pm = getPackageManager();
        Intent discover = new Intent(Intent.ACTION_MAIN);
        discover.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> results = pm.queryIntentActivities(discover, 0);
        Set<String> seen = new HashSet<>();
        catalog.clear();
        for (ResolveInfo item : results) {
            if (item.activityInfo == null) continue;
            ComponentName component = new ComponentName(item.activityInfo.packageName, item.activityInfo.name);
            String key = component.flattenToString();
            if (!seen.add(key) || getPackageName().equals(item.activityInfo.packageName)) continue;
            catalog.add(new Entry(component, String.valueOf(item.loadLabel(pm)), item.loadIcon(pm)));
        }
        catalog.sort(Comparator.comparing(a -> a.name.toLowerCase(java.util.Locale.ROOT)));
        // Preserve unavailable apps in user preferences, so reinstalling or re-enabling
        // a package does not silently destroy the person's dock configuration.
        showApps();
        showDock();
    }

    /** Home pages are locally persisted; the drawer always shows available apps. */
    private void setMode(boolean showDrawer) {
        drawerOpen = showDrawer;
        if (!showDrawer) {
            query.setText("");
            InputMethodManager keyboard = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (keyboard != null) keyboard.hideSoftInputFromWindow(query.getWindowToken(), 0);
            query.clearFocus();
            rootSurface.requestFocus();
        }
        showApps();
    }

    private void switchPage(int offset) {
        if (workspace.selectPage(workspace.currentPage() + offset)) {
            saveWorkspace();
            showApps();
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
        v.setBackground(background(0xFFE8E6FF, 18));
        v.setOnClickListener(ignored -> onClick.run());
        return v;
    }

    private void showApps() {
        if (grid == null) return;
        grid.removeAllViews();
        int columns = Math.max(3, Math.min(6, prefs.getInt("columns", 4)));
        grid.setColumnCount(columns);
        boolean names = prefs.getBoolean("labels", true);
        String text = query.getText().toString();
        boolean allApps = drawerOpen || !text.trim().isEmpty();
        pageControls.setVisibility(allApps ? View.GONE : View.VISIBLE);
        sectionLabel.setText(allApps ? "All applications" : "Home workspace");
        homeTab.setBackground(background(allApps ? 0xFFE8E6FF : 0xFF5145D9, 18));
        appsTab.setBackground(background(allApps ? 0xFF5145D9 : 0xFFE8E6FF, 18));
        homeTab.setTextColor(allApps ? 0xFF2C227C : 0xFFFFFFFF);
        appsTab.setTextColor(allApps ? 0xFFFFFFFF : 0xFF2C227C);
        pageLabel.setText("Page " + (workspace.currentPage() + 1) + " / " + workspace.pageCount());
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
        } else {
            for (String key : workspace.currentItems()) {
                Entry item = find(key);
                if (item != null) shown.add(item);
            }
        }
        // Real grid slots: a sparse Home page must not stretch its one shortcut
        // across the row. Bound widths on tablets and scale icons on compact phones.
        int availableWidth = getResources().getDisplayMetrics().widthPixels - dp(40);
        int slotWidth = Math.max(dp(32), Math.min(dp(112),
                (availableWidth - dp(4) * columns) / columns));
        int iconSize = Math.min(dp(48), Math.max(dp(22), slotWidth - dp(12)));
        for (int i = 0; i < shown.size(); i++) {
            View tile = tile(shown.get(i), names, iconSize);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(i / columns), GridLayout.spec(i % columns));
            params.width = slotWidth;
            params.height = dp(ResponsiveHomePolicy.shortcutHeightDp(
                    names, compactLayout, systemFontScale));
            params.setMargins(dp(2), dp(2), dp(2), dp(2));
            grid.addView(tile, params);
        }
        if (shown.isEmpty()) {
            if (!allApps) {
                emptyTitle.setText("Make this space yours");
                empty.setText("Find an app in All apps, then long-press it to add a shortcut to this page.");
            } else {
                emptyTitle.setText(catalog.isEmpty() ? "No apps yet" : "Nothing matches");
                empty.setText(catalog.isEmpty() ? getString(R.string.empty)
                    : "Try another application name.");
            }
            emptyAction.setVisibility(allApps ? View.GONE : View.VISIBLE);
            emptyPanel.setVisibility(View.VISIBLE);
        } else {
            emptyPanel.setVisibility(View.GONE);
        }
    }

    private View tile(Entry app, boolean names, int iconSize) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);
        cell.setPadding(dp(2), dp(6), dp(2), dp(6));
        cell.setBackground(background(0xFFFFFFFF, 18)); // GC UI Surface
        cell.setContentDescription(app.name);
        cell.setFocusable(true);
        ImageView image = new ImageView(this);
        image.setImageDrawable(app.icon);
        cell.addView(image, new LinearLayout.LayoutParams(iconSize, iconSize));
        if (names) {
            TextView text = label(app.name, 12, false);
            text.setGravity(Gravity.CENTER);
            text.setMaxLines(2);
            text.setEllipsize(android.text.TextUtils.TruncateAt.END);
            cell.addView(text, new LinearLayout.LayoutParams(-1,
                    dp(ResponsiveHomePolicy.labelHeightDp(systemFontScale))));
        }
        cell.setOnClickListener(v -> launch(app));
        cell.setOnLongClickListener(v -> { showActions(cell, app); return true; });
        return cell;
    }

    private void showDock() {
        dock.removeAllViews();
        List<String> sortedPins = new ArrayList<>(pins);
        Collections.sort(sortedPins);
        for (String key : sortedPins) {
            Entry app = find(key);
            if (app == null) continue;
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(72), 1);
            params.setMargins(dp(2), 0, dp(2), 0);
            dock.addView(tile(app, false, dp(44)), params);
        }
        int occupied = dock.getChildCount();
        if (occupied == 0) {
            TextView hint = label(getString(R.string.dock_empty), 13, false);
            hint.setGravity(Gravity.CENTER);
            dock.addView(hint);
        } else {
            // Five equal dock slots; one app never expands to occupy the entire dock.
            for (int i = occupied; i < 5; i++) {
                dock.addView(new Space(this), new LinearLayout.LayoutParams(0, dp(72), 1));
            }
        }
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
        menu.getMenu().add(pins.contains(id) ? R.string.unpin : R.string.pin)
            .setOnMenuItemClickListener(item -> {
                if (!pins.remove(id)) {
                    if (pins.size() >= 5) {
                        Toast.makeText(this, "Dock supports up to five apps", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    pins.add(id);
                }
                savePins();
                showDock();
                return true;
            });
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
    private void savePins() {
        prefs.edit().putStringSet("pins", new HashSet<>(pins)).apply();
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
