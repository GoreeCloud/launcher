package com.goreecloud.launcher;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
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

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        SharedPreferences prefs = getSharedPreferences("launcher", MODE_PRIVATE);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(CANVAS);
        scroll.setClipToPadding(false);
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
        body.addView(header, new LinearLayout.LayoutParams(-1, dp(74)));
        TextView back = text("‹", 30, true, TEXT);
        back.setGravity(Gravity.CENTER);
        back.setContentDescription("Back to Home");
        back.setFocusable(true);
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

        Button home = new Button(this);
        home.setText(R.string.home_settings);
        home.setAllCaps(false);
        home.setTextSize(16);
        home.setTextColor(0xFFFFFFFF);
        home.setBackground(panel(IRIS, 18));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, dp(56));
        hp.topMargin = dp(22);
        homeCard.addView(home, hp);
        home.setOnClickListener(view -> {
            try {
                startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
            } catch (ActivityNotFoundException | SecurityException ex) {
                Toast.makeText(this, "Home selection settings unavailable", Toast.LENGTH_LONG).show();
            }
        });

        TextView note = text("Your workspace preferences are saved locally on this device.", 13, false, MUTED);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
        np.topMargin = dp(26);
        body.addView(note, np);
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
