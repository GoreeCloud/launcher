package com.goreecloud.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;

/** Native Home overview and live personalization; mockup edit screen, not a static preview. */
public final class HomeEditActivity extends Activity {
    public static final String REQUEST_WIDGET = "com.goreecloud.launcher.REQUEST_WIDGET";
    private SharedPreferences prefs;
    private WorkspaceState workspace;
    private TextView pageTitle;
    private LinearLayout previewItems;
    private LinearLayout indicators;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("launcher", MODE_PRIVATE);
        workspace = WorkspaceState.decode(prefs.getString("workspace_pages_v1", ""));
        workspace.selectPage(prefs.getInt("workspace_current_v1", 0));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackground(GoreeVisuals.atmosphere(true));
        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            android.graphics.Insets bars = android.os.Build.VERSION.SDK_INT >= 30
                ? insets.getInsets(WindowInsets.Type.systemBars())
                : android.graphics.Insets.of(insets.getSystemWindowInsetLeft(),
                    insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(),
                    insets.getSystemWindowInsetBottom());
            v.setPadding(dp(18) + bars.left, dp(12) + bars.top,
                    dp(18) + bars.right, dp(24) + bars.bottom);
            return insets;
        });
        setContentView(scroll);
        getWindow().setStatusBarColor(0xFF242341);
        getWindow().setNavigationBarColor(0xFF191A32);
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            android.view.WindowInsetsController bars = getWindow().getInsetsController();
            if (bars != null) {
                int flags = android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                bars.setSystemBarsAppearance(0, flags);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(0);
        }
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));

        Button back = button("‹  Back to Home", () -> finish());
        content.addView(back, new LinearLayout.LayoutParams(-1, dp(52)));
        content.addView(text("Edit Home", 27, 0xFFFFFFFF, true));
        content.addView(text("Your pages, your layout", 14, 0xFFDDDDF6, false));

        LinearLayout preview = card(0x817772B1);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(-1, -2);
        previewParams.topMargin = dp(22);
        content.addView(preview, previewParams);
        pageTitle = text("", 19, 0xFFFFFFFF, true);
        pageTitle.setGravity(Gravity.CENTER);
        preview.addView(pageTitle);
        previewItems = new LinearLayout(this);
        previewItems.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams itemsParams = new LinearLayout.LayoutParams(-1, dp(95));
        itemsParams.topMargin = dp(10);
        preview.addView(previewItems, itemsParams);
        indicators = new LinearLayout(this);
        indicators.setGravity(Gravity.CENTER);
        preview.addView(indicators, new LinearLayout.LayoutParams(-1, dp(25)));
        LinearLayout pages = new LinearLayout(this);
        preview.addView(pages);
        addFlexButton(pages, "‹ Page", () -> {
            workspace.selectPage(Math.max(0, workspace.currentPage() - 1));
            persistWorkspace();
        });
        addFlexButton(pages, "Add page +", () -> {
            if (!workspace.addPage()) Toast.makeText(this, "Maximum seven pages", Toast.LENGTH_SHORT).show();
            persistWorkspace();
        });
        addFlexButton(pages, "− Empty", () -> {
            if (!workspace.removeCurrentEmptyPage())
                Toast.makeText(this, "Remove only an empty extra page", Toast.LENGTH_SHORT).show();
            persistWorkspace();
        });
        addFlexButton(pages, "Page ›", () -> {
            workspace.selectPage(Math.min(workspace.pageCount() - 1, workspace.currentPage() + 1));
            persistWorkspace();
        });
        renderPreview();

        content.addView(text("PERSONALIZE", 13, 0xFFE1DBFF, true));
        LinearLayout actions = new LinearLayout(this);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(-1, dp(64));
        actionsParams.topMargin = dp(10);
        content.addView(actions, actionsParams);
        addFlexButton(actions, "Wallpapers", this::chooseWallpaper);
        addFlexButton(actions, "Widgets", () -> {
            setResult(RESULT_OK, new Intent().putExtra(REQUEST_WIDGET, true));
            finish();
        });
        addFlexButton(actions, "Themes", this::chooseTheme);
        addFlexButton(actions, "Settings", () -> startActivity(new Intent(this, SettingsActivity.class)));

        LinearLayout settings = card(0xDC303052);
        LinearLayout.LayoutParams settingsParams = new LinearLayout.LayoutParams(-1, -2);
        settingsParams.topMargin = dp(16);
        content.addView(settings, settingsParams);
        settings.addView(text("Home screen settings", 19, 0xFFFFFFFF, true));
        TextView grid = text("Home screen grid: " + prefs.getInt("columns", 4) + " columns",
                14, 0xFFE5E4FF, false);
        settings.addView(grid);
        SeekBar columns = new SeekBar(this);
        columns.setMax(3);
        columns.setProgress(Math.max(0, Math.min(3, prefs.getInt("columns", 4) - 3)));
        columns.setContentDescription("Home screen grid columns");
        columns.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean byUser) {
                grid.setText("Home screen grid: " + (progress + 3) + " columns");
                if (byUser) prefs.edit().putInt("columns", progress + 3).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        settings.addView(columns, new LinearLayout.LayoutParams(-1, dp(48)));
        toggle(settings, "Show app labels", "labels", true);
        toggle(settings, "Show dock", "dock_enabled", true);
        toggle(settings, "Swipe up for app drawer", "swipe_up_drawer", true);
        toggle(settings, "Animate drawer", "drawer_animation", true);
        settings.addView(text("Settings apply when you return Home. Wallpaper is handled by Android.",
                12, 0xFFCDCCEA, false));
    }

    private void renderPreview() {
        pageTitle.setText("Home page " + (workspace.currentPage() + 1) + " of " + workspace.pageCount());
        previewItems.removeAllViews();
        List<String> items = workspace.currentItems();
        if (items.isEmpty()) {
            TextView empty = text("Empty page  ·  Add apps from Home", 13, 0xFFFFFFFF, false);
            empty.setGravity(Gravity.CENTER);
            previewItems.addView(empty);
        } else {
            int count = Math.min(4, items.size());
            for (int i = 0; i < count; i++) {
                ComponentName component = ComponentName.unflattenFromString(items.get(i));
                LinearLayout cell = new LinearLayout(this);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams slot = new LinearLayout.LayoutParams(0, -1, 1);
                previewItems.addView(cell, slot);
                String title = "Unavailable";
                if (component != null) {
                    try {
                        title = getPackageManager().getActivityInfo(component, 0)
                                .loadLabel(getPackageManager()).toString();
                        Drawable icon = getPackageManager().getActivityIcon(component);
                        android.widget.ImageView image = new android.widget.ImageView(this);
                        image.setImageDrawable(icon);
                        cell.addView(image, new LinearLayout.LayoutParams(dp(39), dp(39)));
                    } catch (android.content.pm.PackageManager.NameNotFoundException ignored) {
                        title = "Unavailable";
                    }
                }
                TextView label = text(title, 11, 0xFFFFFFFF, false);
                label.setMaxLines(2);
                label.setGravity(Gravity.CENTER);
                cell.addView(label);
            }
        }
        indicators.removeAllViews();
        for (int i = 0; i < workspace.pageCount(); i++) {
            TextView dot = text(i == workspace.currentPage() ? "● " : "○ ",
                    16, 0xFFFFFFFF, false);
            indicators.addView(dot);
        }
    }

    private void persistWorkspace() {
        prefs.edit().putString("workspace_pages_v1", workspace.encode())
                .putInt("workspace_current_v1", workspace.currentPage()).apply();
        renderPreview();
    }

    private void chooseWallpaper() {
        try {
            startActivity(new Intent(Intent.ACTION_SET_WALLPAPER));
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, "No wallpaper picker available on this device",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void chooseTheme() {
        String[] items = {"GoreeCloud violet", "Midnight", "Use device wallpaper"};
        String selected = prefs.getString("launcher_skin", "violet");
        int initial = "night".equals(selected) ? 1 : "wallpaper".equals(selected) ? 2 : 0;
        new AlertDialog.Builder(this).setTitle("Home appearance")
            .setSingleChoiceItems(items, initial, (dialog, which) -> {
                prefs.edit().putString("launcher_skin",
                    which == 1 ? "night" : which == 2 ? "wallpaper" : "violet").apply();
                dialog.dismiss();
            }).setNegativeButton("Cancel", null).show();
    }

    private void toggle(LinearLayout parent, String title, String key, boolean def) {
        Switch item = new Switch(this);
        item.setText(title);
        item.setTextSize(15);
        item.setTextColor(0xFFFFFFFF);
        item.setChecked(prefs.getBoolean(key, def));
        item.setMinHeight(dp(49));
        item.setOnCheckedChangeListener((v, checked) -> prefs.edit().putBoolean(key, checked).apply());
        parent.addView(item);
    }

    private LinearLayout card(int color) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(15), dp(15), dp(15), dp(17));
        panel.setBackground(GoreeVisuals.card(getResources(), color, 24));
        return panel;
    }

    private void addFlexButton(LinearLayout row, String caption, Runnable click) {
        Button item = button(caption, click);
        item.setTextSize(12);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
        params.setMargins(dp(2), 0, dp(2), 0);
        row.addView(item, params);
    }

    private Button button(String caption, Runnable click) {
        Button b = new Button(this);
        b.setText(caption);
        b.setAllCaps(false);
        b.setTextColor(0xFF2E285B);
        b.setBackground(GoreeVisuals.card(getResources(), 0xEAF1EEFF, 16));
        b.setOnClickListener(v -> click.run());
        return b;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextColor(color);
        t.setTextSize(size);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        t.setPadding(dp(3), dp(3), dp(3), dp(3));
        return t;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
