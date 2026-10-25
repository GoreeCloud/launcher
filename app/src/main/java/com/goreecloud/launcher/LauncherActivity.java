package com.goreecloud.launcher;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Native, single-page HOME foundation. Not a finished launcher workspace. */
public final class LauncherActivity extends Activity {
    private final List<Entry> catalog = new ArrayList<>();
    private final Set<String> pins = new HashSet<>();
    private SharedPreferences prefs;
    private WorkspaceState workspace;
    private boolean drawerOpen;
    private GridLayout grid;
    private LinearLayout pageControls;
    private TextView pageLabel;
    private TextView sectionLabel;
    private TextView homeTab;
    private TextView appsTab;
    private LinearLayout dock;
    private EditText query;
    private TextView empty;

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
        if (grid != null) refresh();
    }

    private void createScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(20), dp(16), dp(12));
        root.setBackgroundColor(0xFFF4F3FC); // GC UI Canvas
        root.setFitsSystemWindows(true);
        setContentView(root);

        LinearLayout heading = new LinearLayout(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(heading, new LinearLayout.LayoutParams(-1, dp(60)));

        TextView title = label("GoreeCloud", 26, true);
        heading.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
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
        LinearLayout.LayoutParams section = new LinearLayout.LayoutParams(-1, dp(32));
        section.topMargin = dp(12);
        root.addView(sectionLabel, section);

        pageControls = new LinearLayout(this);
        pageControls.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(pageControls, new LinearLayout.LayoutParams(-1, dp(52)));
        pageControls.addView(action("‹", () -> switchPage(-1)),
            new LinearLayout.LayoutParams(dp(48), dp(48)));
        pageLabel = label("", 15, true);
        pageLabel.setGravity(Gravity.CENTER);
        pageControls.addView(pageLabel, new LinearLayout.LayoutParams(0, dp(48), 1));
        pageControls.addView(action("›", () -> switchPage(1)),
            new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView addPage = action("+", this::addPage);
        addPage.setContentDescription("Add home page");
        pageControls.addView(addPage, new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView removePage = action("−", this::removeEmptyPage);
        removePage.setContentDescription("Remove empty home page");
        pageControls.addView(removePage, new LinearLayout.LayoutParams(dp(48), dp(48)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        grid = new GridLayout(this);
        scroll.addView(grid);

        empty = label("", 14, false);
        empty.setGravity(Gravity.CENTER);
        root.addView(empty, new LinearLayout.LayoutParams(-1, -2));

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
        // Remove pins for activities no longer present.
        pins.removeIf(key -> find(key) == null);
        savePins();
        showApps();
        showDock();
    }

    /** Home pages are locally persisted; the drawer always shows available apps. */
    private void setMode(boolean showDrawer) {
        drawerOpen = showDrawer;
        if (!showDrawer) query.setText("");
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

        List<Entry> shown = new ArrayList<>();
        if (allApps) {
            shown.addAll(AppNameFilter.filter(catalog, text, e -> e.name));
        } else {
            for (String key : workspace.currentItems()) {
                Entry item = find(key);
                if (item != null) shown.add(item);
            }
        }
        for (int i = 0; i < shown.size(); i++) {
            View tile = tile(shown.get(i), names, dp(48));
            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(i / columns), GridLayout.spec(i % columns, 1f));
            params.width = 0;
            params.height = dp(names ? 112 : 84);
            params.setMargins(dp(2), dp(2), dp(2), dp(2));
            grid.addView(tile, params);
        }
        if (shown.isEmpty()) {
            if (!allApps) {
                empty.setText("This page is empty. Open All apps, then long-press an app to add it.");
            } else {
                empty.setText(catalog.isEmpty() ? R.string.empty : R.string.no_match);
            }
            empty.setVisibility(View.VISIBLE);
        } else {
            empty.setVisibility(View.GONE);
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
            TextView text = label(app.name, 11, false);
            text.setGravity(Gravity.CENTER);
            text.setMaxLines(2);
            text.setEllipsize(android.text.TextUtils.TruncateAt.END);
            cell.addView(text, new LinearLayout.LayoutParams(-1, dp(40)));
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
        if (dock.getChildCount() == 0) {
            TextView hint = label(getString(R.string.dock_empty), 13, false);
            hint.setGravity(Gravity.CENTER);
            dock.addView(hint);
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
