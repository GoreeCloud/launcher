package com.goreecloud.launcher;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/** On-device controls for the initial native home surface. */
public final class SettingsActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        SharedPreferences prefs = getSharedPreferences("launcher", MODE_PRIVATE);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(34), dp(24), dp(24));
        root.setBackgroundColor(0xFFF4F3FC);
        root.setFitsSystemWindows(true);
        setContentView(root);

        TextView title = new TextView(this);
        title.setText("Home settings");
        title.setTextSize(28);
        title.setTextColor(0xFF211F30);
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);

        TextView gridLabel = new TextView(this);
        gridLabel.setTextSize(18);
        gridLabel.setTextColor(0xFF211F30);
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(-1, -2);
        l.topMargin = dp(40);
        root.addView(gridLabel, l);

        SeekBar slider = new SeekBar(this);
        slider.setMax(3);
        slider.setProgress(Math.max(0, Math.min(3, prefs.getInt("columns", 4) - 3)));
        gridLabel.setText("Grid columns: " + (slider.getProgress() + 3));
        slider.setContentDescription("Grid columns");
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                gridLabel.setText("Grid columns: " + (progress + 3));
                if (fromUser) prefs.edit().putInt("columns", progress + 3).apply();
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        root.addView(slider);

        Switch labels = new Switch(this);
        labels.setText("Show application labels");
        labels.setTextSize(17);
        labels.setChecked(prefs.getBoolean("labels", true));
        labels.setOnCheckedChangeListener((v, checked) ->
                prefs.edit().putBoolean("labels", checked).apply());
        root.addView(labels, new LinearLayout.LayoutParams(-1, dp(76)));

        Button home = new Button(this);
        home.setText(R.string.home_settings);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, dp(60));
        hp.topMargin = dp(24);
        root.addView(home, hp);
        home.setOnClickListener(v -> {
            try { startActivity(new Intent(Settings.ACTION_HOME_SETTINGS)); }
            catch (ActivityNotFoundException ex) {
                Toast.makeText(this, "Home selection settings unavailable", Toast.LENGTH_LONG).show();
            }
        });

        TextView note = new TextView(this);
        note.setText("Application discovery and dock preferences stay on this device.");
        note.setTextSize(13);
        note.setTextColor(0xFF5C596B);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
        np.topMargin = dp(28);
        root.addView(note, np);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
