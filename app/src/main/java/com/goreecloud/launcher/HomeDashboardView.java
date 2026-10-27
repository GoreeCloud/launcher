package com.goreecloud.launcher;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

/** Compact wallpaper-friendly, live Home clock and date. No fake weather/data. */
public final class HomeDashboardView {
    private HomeDashboardView() {}

    public static LinearLayout create(Context context, Runnable edit, Runnable addWidget) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(context, 4), dp(context, 6), dp(context, 4), dp(context, 4));
        row.setContentDescription("Live Home clock and date");

        LinearLayout time = new LinearLayout(context);
        time.setOrientation(LinearLayout.VERTICAL);
        time.setGravity(Gravity.CENTER_VERTICAL);
        time.setPadding(dp(context, 14), dp(context, 6), dp(context, 8), dp(context, 6));
        time.setBackground(GoreeVisuals.card(context.getResources(), 0x65121D2D, 22));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(context, 88), 1);
        row.addView(time, lp);
        TextClock clock = new TextClock(context);
        clock.setFormat12Hour("h:mm");
        clock.setFormat24Hour("HH:mm");
        clock.setTextColor(0xFFFFFFFF);
        clock.setTextSize(29);
        clock.setTypeface(null, Typeface.BOLD);
        time.addView(clock);
        TextClock day = new TextClock(context);
        day.setFormat12Hour("EEEE, MMM d");
        day.setFormat24Hour("EEEE, MMM d");
        day.setTextColor(0xFFE5EAF4);
        day.setTextSize(13);
        time.addView(day);
        time.setOnLongClickListener(v -> { edit.run(); return true; });

        // Wallpaper-first desktop: no widget, page or edit glyphs on Home.
        // The clock is an unobtrusive accessible way into the editor too.
        time.setContentDescription("Edit Home screen");
        time.setFocusable(true);
        time.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override public void onInitializeAccessibilityNodeInfo(
                    View host, android.view.accessibility.AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(android.widget.Button.class.getName());
            }
        });
        time.setOnClickListener(v -> edit.run());
        time.setOnLongClickListener(v -> { edit.run(); return true; });
        return row;
    }

    private static void markButton(TextView control) {
        control.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override public void onInitializeAccessibilityNodeInfo(
                    View host, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(Button.class.getName());
            }
        });
    }

    private static int dp(Context c, int v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }
}
