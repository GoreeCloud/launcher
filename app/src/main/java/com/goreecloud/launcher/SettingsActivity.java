package com.goreecloud.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/** Native GC UI-inspired, permission-free launcher settings. */
public final class SettingsActivity extends Activity {
    private static final int CANVAS = 0xFFF4F3FC;
    private static final int TEXT = 0xFF211F30;
    private static final int MUTED = 0xFF5C596B;
    private static final int IRIS = 0xFF5145D9;
    private static final int EXPORT_FILE = 701;
    private static final int IMPORT_FILE = 702;
    private static final int SELECT_HOME_ROLE = 703;
    private SharedPreferences prefs;
    private TextView homeRoleStatus;
    private Button homeRoleAction;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("launcher", MODE_PRIVATE);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(CANVAS);
        scroll.setClipToPadding(true); // Prevent scrolled content obscuring system status icons.
        scroll.setPadding(dp(20), dp(16), dp(20), dp(24));
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
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
            view.setPadding(dp(20) + left, dp(16) + top, dp(20) + right, dp(24) + bottom);
            return insets;
        });
        setContentView(scroll);
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

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(body, new ScrollView.LayoutParams(-1, -2));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        // At 200% system text size the title and subtitle together exceed
        // 74dp. Never clip the heading or back control in a fixed-height row.
        header.setMinimumHeight(dp(74));
        body.addView(header, new LinearLayout.LayoutParams(-1, -2));
        TextView back = text("‹", 30, true, TEXT);
        back.setGravity(Gravity.CENTER);
        back.setContentDescription("Back to Home");
        back.setFocusable(true);
        back.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override public void onInitializeAccessibilityNodeInfo(
                    View host, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(Button.class.getName());
            }
        });
        back.setBackground(panel(0xFFFFFFFF, 18));
        back.setOnClickListener(view -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));
        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams headParams = new LinearLayout.LayoutParams(0, -2, 1);
        headParams.leftMargin = dp(16);
        header.addView(heading, headParams);
        heading.addView(text("Home settings", 26, true, TEXT));
        heading.addView(text("Personalize your workspace", 13, false, MUTED));

        TextView layoutTitle = section("LAYOUT");
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-1, -2);
        labelParams.topMargin = dp(24);
        labelParams.bottomMargin = dp(12);
        body.addView(layoutTitle, labelParams);

        LinearLayout surface = new LinearLayout(this);
        surface.setOrientation(LinearLayout.VERTICAL);
        surface.setPadding(dp(20), dp(20), dp(20), dp(18));
        surface.setBackground(panel(0xFFFFFFFF, 26));
        body.addView(surface, new LinearLayout.LayoutParams(-1, -2));
        surface.addView(text("Home grid", 20, true, TEXT));
        TextView description = text("Choose how many shortcuts appear in each row.", 14, false, MUTED);
        LinearLayout.LayoutParams descriptionParams = new LinearLayout.LayoutParams(-1, -2);
        descriptionParams.topMargin = dp(6);
        surface.addView(description, descriptionParams);

        TextView gridLabel = text("", 15, true, 0xFF2C227C);
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(-1, -2);
        gp.topMargin = dp(22);
        surface.addView(gridLabel, gp);

        SeekBar slider = new SeekBar(this);
        slider.setMax(3);
        slider.setProgress(Math.max(0, Math.min(3, prefs.getInt("columns", 4) - 3)));
        gridLabel.setText("Grid columns: " + (slider.getProgress() + 3));
        slider.setContentDescription("Grid columns");
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                gridLabel.setText("Grid columns: " + (progress + 3));
                if (fromUser) prefs.edit().putInt("columns", progress + 3).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        surface.addView(slider, new LinearLayout.LayoutParams(-1, dp(56)));

        View divider = new View(this);
        divider.setBackgroundColor(0xFFE9E7F3);
        LinearLayout.LayoutParams dv = new LinearLayout.LayoutParams(-1, dp(1));
        dv.topMargin = dp(8);
        dv.bottomMargin = dp(12);
        surface.addView(divider, dv);

        Switch labels = new Switch(this);
        labels.setText("Show application labels");
        labels.setTextSize(16);
        labels.setTextColor(TEXT);
        labels.setChecked(prefs.getBoolean("labels", true));
        labels.setOnCheckedChangeListener((button, checked) ->
            prefs.edit().putBoolean("labels", checked).apply());
        surface.addView(labels, new LinearLayout.LayoutParams(-1, dp(60)));
        surface.addView(text("Show app names beneath Home and drawer shortcuts.", 13, false, MUTED));

        LinearLayout.LayoutParams defaultHeaderParams = new LinearLayout.LayoutParams(-1, -2);
        defaultHeaderParams.topMargin = dp(28);
        defaultHeaderParams.bottomMargin = dp(12);
        body.addView(section("ANDROID HOME"), defaultHeaderParams);

        LinearLayout homeCard = new LinearLayout(this);
        homeCard.setOrientation(LinearLayout.VERTICAL);
        homeCard.setPadding(dp(20), dp(22), dp(20), dp(20));
        homeCard.setBackground(panel(0xFFE8E6FF, 26));
        body.addView(homeCard, new LinearLayout.LayoutParams(-1, -2));
        homeCard.addView(text("Open with GoreeCloud", 20, true, 0xFF2C227C));
        TextView homeHint = text("Choose which launcher opens when you press the Android Home button.", 14, false, 0xFF514A76);
        LinearLayout.LayoutParams hnt = new LinearLayout.LayoutParams(-1, -2);
        hnt.topMargin = dp(8);
        homeCard.addView(homeHint, hnt);
        homeRoleStatus = text("", 13, true, 0xFF2C227C);
        homeCard.addView(homeRoleStatus);
        updateHomeRoleStatus();

        Button home = new Button(this);
        homeRoleAction = home;
        home.setText(R.string.home_settings);
        home.setAllCaps(false);
        home.setTextSize(16);
        home.setTextColor(0xFFFFFFFF);
        home.setBackground(panel(IRIS, 18));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, dp(56));
        hp.topMargin = dp(22);
        homeCard.addView(home, hp);
        home.setOnClickListener(view -> requestHomeSelection());
        updateHomeRoleStatus();

        LinearLayout.LayoutParams backupHeader = new LinearLayout.LayoutParams(-1, -2);
        backupHeader.topMargin = dp(28);
        backupHeader.bottomMargin = dp(12);
        body.addView(section("BACKUP AND RESTORE"), backupHeader);
        LinearLayout backupCard = new LinearLayout(this);
        backupCard.setOrientation(LinearLayout.VERTICAL);
        backupCard.setPadding(dp(20), dp(20), dp(20), dp(22));
        backupCard.setBackground(panel(0xFFFFFFFF, 26));
        body.addView(backupCard, new LinearLayout.LayoutParams(-1, -2));
        backupCard.addView(text("Keep your layout", 20, true, TEXT));
        TextView backupHelp = text(
            "Save your Home pages, dock pins, grid and label preferences to a file you choose.",
            14, false, MUTED);
        LinearLayout.LayoutParams helpParams = new LinearLayout.LayoutParams(-1, -2);
        helpParams.topMargin = dp(8);
        backupCard.addView(backupHelp, helpParams);

        Button export = backupButton("Export backup", true);
        LinearLayout.LayoutParams exportParams = new LinearLayout.LayoutParams(-1, dp(56));
        exportParams.topMargin = dp(18);
        backupCard.addView(export, exportParams);
        export.setOnClickListener(v -> chooseExport());

        Button restore = backupButton("Import backup", false);
        LinearLayout.LayoutParams restoreParams = new LinearLayout.LayoutParams(-1, dp(56));
        restoreParams.topMargin = dp(10);
        backupCard.addView(restore, restoreParams);
        restore.setOnClickListener(v -> chooseImport());

        TextView caution = text("Backups contain installed-app identifiers and are not encrypted. Import replaces your current layout after confirmation. Widgets and passwords are not included.",
            13, false, MUTED);
        LinearLayout.LayoutParams cautionParams = new LinearLayout.LayoutParams(-1, -2);
        cautionParams.topMargin = dp(16);
        backupCard.addView(caution, cautionParams);

        TextView note = text("Your workspace preferences are saved locally on this device.", 13, false, MUTED);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
        np.topMargin = dp(26);
        body.addView(note, np);
    }


    @Override protected void onResume() {
        super.onResume();
        updateHomeRoleStatus();
    }

    private void updateHomeRoleStatus() {
        if (homeRoleStatus == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager role = getSystemService(RoleManager.class);
            if (role != null && role.isRoleAvailable(RoleManager.ROLE_HOME)) {
                boolean held = role.isRoleHeld(RoleManager.ROLE_HOME);
                homeRoleStatus.setText(held
                        ? "Current default Home: GoreeCloud"
                        : "GoreeCloud is not the current default Home");
                if (homeRoleAction != null) homeRoleAction.setText(
                        held ? "Manage default Home apps" : "Select default Home app");
                return;
            }
        }
        homeRoleStatus.setText("Choose a default Home app in Android settings");
    }

    private void requestHomeSelection() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager role = getSystemService(RoleManager.class);
            if (role != null && role.isRoleAvailable(RoleManager.ROLE_HOME)) {
                if (role.isRoleHeld(RoleManager.ROLE_HOME)) {
                    openPlatformHomeSettings();
                    return;
                }
                try {
                    startActivityForResult(
                            role.createRequestRoleIntent(RoleManager.ROLE_HOME), SELECT_HOME_ROLE);
                    return;
                } catch (ActivityNotFoundException | SecurityException | IllegalArgumentException ex) {
                    // A device may expose the Home role without a request UI.
                }
            }
        }
        openPlatformHomeSettings();
    }

    private void openPlatformHomeSettings() {
        // API 26–28 and OEM variants may not handle ACTION_HOME_SETTINGS.
        // Safely degrade to Default apps and finally general Android Settings.
        String[] actions = {Settings.ACTION_HOME_SETTINGS,
                Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS, Settings.ACTION_SETTINGS};
        for (String action : actions) {
            try {
                Intent intent = new Intent(action);
                if (intent.resolveActivity(getPackageManager()) == null) continue;
                startActivity(intent);
                return;
            } catch (ActivityNotFoundException | SecurityException | IllegalArgumentException ex) {
                // Try the next documented Settings entry point.
            }
        }
        Toast.makeText(this, "Android Home settings unavailable on this device",
                Toast.LENGTH_LONG).show();
    }

    private Button backupButton(String caption, boolean primary) {
        Button button = new Button(this);
        button.setText(caption);
        button.setAllCaps(false);
        button.setTextSize(16);
        button.setTextColor(primary ? 0xFFFFFFFF : 0xFF2C227C);
        button.setBackground(panel(primary ? IRIS : 0xFFE8E6FF, 18));
        return button;
    }

    private void chooseExport() {
        try {
            Intent picker = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            picker.setType("text/plain");
            picker.addCategory(Intent.CATEGORY_OPENABLE);
            picker.putExtra(Intent.EXTRA_TITLE, "GoreeCloud-Launcher-backup.gcl");
            startActivityForResult(picker, EXPORT_FILE);
        } catch (ActivityNotFoundException | SecurityException ex) {
            Toast.makeText(this, "Document picker unavailable", Toast.LENGTH_LONG).show();
        }
    }

    private void chooseImport() {
        try {
            Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            picker.addCategory(Intent.CATEGORY_OPENABLE);
            picker.setType("*/*");
            picker.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(picker, IMPORT_FILE);
        } catch (ActivityNotFoundException | SecurityException ex) {
            Toast.makeText(this, "Document picker unavailable", Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == SELECT_HOME_ROLE) {
            updateHomeRoleStatus();
            return;
        }
        if (result != RESULT_OK || data == null || data.getData() == null) return;
        Uri chosen = data.getData();
        if (request == EXPORT_FILE) exportTo(chosen);
        if (request == IMPORT_FILE) importFrom(chosen);
    }

    private void exportTo(Uri uri) {
        try {
            List<String> pins = new ArrayList<>(prefs.getStringSet("pins",
                    java.util.Collections.emptySet()));
            BackupCodec.Data snapshot = new BackupCodec.Data(
                    prefs.getString("workspace_pages_v1", ""),
                    prefs.getInt("workspace_current_v1", 0),
                    prefs.getInt("columns", 4), prefs.getBoolean("labels", true), pins);
            byte[] body = BackupCodec.encode(snapshot).getBytes(StandardCharsets.UTF_8);
            try (OutputStream out = getContentResolver().openOutputStream(uri, "w")) {
                if (out == null) throw new IOException("Document could not be opened");
                out.write(body);
                out.flush();
            }
            Toast.makeText(this, "Workspace backup exported", Toast.LENGTH_LONG).show();
        } catch (IOException | IllegalArgumentException | SecurityException ex) {
            Toast.makeText(this, "Export unsuccessful; verify the destination", Toast.LENGTH_LONG).show();
        }
    }

    private void importFrom(Uri uri) {
        try {
            byte[] content;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                if (in == null) throw new IOException("Document could not be opened");
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                byte[] chunk = new byte[4096];
                int count;
                while ((count = in.read(chunk)) != -1) {
                    if (bytes.size() + count > BackupCodec.MAX_BYTES)
                        throw new IllegalArgumentException("Backup file too large");
                    bytes.write(chunk, 0, count);
                }
                content = bytes.toByteArray();
            }
            BackupCodec.Data candidate = BackupCodec.decode(
                    new String(content, StandardCharsets.UTF_8));
            String preview = "Replace the current layout with " + candidate.pageCount()
                    + " page(s), " + candidate.shortcutCount() + " shortcut(s) and "
                    + candidate.pins.size() + " dock pin(s)?\n\n"
                    + "This will also replace grid and label settings. Missing apps may "
                    + "remain as inactive shortcuts until installed.";
            new AlertDialog.Builder(this)
                .setTitle("Restore launcher backup?")
                .setMessage(preview)
                .setNegativeButton("Cancel", (dialog, which) -> {})
                .setPositiveButton("Replace layout", (dialog, which) -> {
                    boolean saved = prefs.edit()
                        .putString("workspace_pages_v1", candidate.workspace)
                        .putInt("workspace_current_v1", candidate.selectedPage)
                        .putStringSet("pins", new HashSet<>(candidate.pins))
                        .putInt("columns", candidate.columns)
                        .putBoolean("labels", candidate.labels)
                        .commit();
                    Toast.makeText(this, saved ? "Backup restored" : "Restore could not be saved", Toast.LENGTH_LONG).show();
                    if (saved) recreate();
                }).show();
        } catch (IOException | IllegalArgumentException | SecurityException ex) {
            Toast.makeText(this, "Invalid, unsupported or unreadable backup", Toast.LENGTH_LONG).show();
        }
    }

    private TextView section(String label) {
        return text(label, 12, true, 0xFF5C5388);
    }

    private TextView text(String value, int sp, boolean bold, int color) {
        TextView label = new TextView(this);
        label.setText(value);
        label.setTextSize(sp);
        label.setTextColor(color);
        if (bold) label.setTypeface(null, Typeface.BOLD);
        return label;
    }

    private GradientDrawable panel(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
