package com.goreecloud.launcher;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import java.text.DateFormat;
import java.util.Date;

/** First-party interactive Home cards. Values use device time, never fabricated weather. */
public final class HomeDashboardView {
    private HomeDashboardView() {}

    public static LinearLayout create(Context context, Runnable edit, Runnable addWidget) {
        LinearLayout stack = new LinearLayout(context);
        stack.setOrientation(LinearLayout.VERTICAL);
        stack.setContentDescription("Home dashboard");
        stack.setPadding(0, dp(context, 4), 0, dp(context, 4));

        LinearLayout hero = new LinearLayout(context);
        hero.setOrientation(LinearLayout.HORIZONTAL);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        stack.addView(hero, new LinearLayout.LayoutParams(-1, dp(context, 112)));

        LinearLayout time = new LinearLayout(context);
        time.setGravity(Gravity.CENTER_VERTICAL);
        time.setOrientation(LinearLayout.VERTICAL);
        time.setPadding(dp(context, 16), dp(context, 8), dp(context, 12), dp(context, 8));
        time.setBackground(GoreeVisuals.card(context.getResources(), 0x953E58BE, 24));
        LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(0, -1, 3);
        hero.addView(time, timeParams);
        TextClock clock = new TextClock(context);
        clock.setFormat12Hour("h:mm");
        clock.setFormat24Hour("HH:mm");
        clock.setTextSize(30);
        clock.setTypeface(null, Typeface.BOLD);
        clock.setTextColor(0xFFFFFFFF);
        // Leave live TextClock text accessible; do not replace the time with a static label.
        time.addView(clock);
        TextView today = label(context, "Today", 13, 0xFFF3F0FF, true);
        time.addView(today);
        TextView subtitle = label(context, "Your GoreeCloud workspace", 12, 0xFFE5E9FF, false);
        time.addView(subtitle);

        LinearLayout date = new LinearLayout(context);
        date.setOrientation(LinearLayout.VERTICAL);
        date.setGravity(Gravity.CENTER);
        date.setBackground(GoreeVisuals.card(context.getResources(), 0xDDFFFFFF, 24));
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(0, -1, 2);
        dateParams.leftMargin = dp(context, 10);
        hero.addView(date, dateParams);
        date.addView(label(context, android.text.format.DateFormat.format("EEE", new Date()).toString(),
                14, 0xFF514984, true));
        date.addView(label(context, android.text.format.DateFormat.format("d", new Date()).toString(),
                34, 0xFF211F30, true));
        date.addView(label(context, android.text.format.DateFormat.format("MMM", new Date()).toString(),
                15, 0xFF514984, false));

        LinearLayout secondary = new LinearLayout(context);
        secondary.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams secondParams = new LinearLayout.LayoutParams(-1, dp(context, 66));
        secondParams.topMargin = dp(context, 8);
        stack.addView(secondary, secondParams);
        secondary.addView(actionCard(context, "Customize", "Wallpaper, layout and gestures", edit),
                new LinearLayout.LayoutParams(0, -1, 1));
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(0, -1, 1);
        wp.leftMargin = dp(context, 10);
        secondary.addView(actionCard(context, "Widgets", "Add Android widgets", addWidget), wp);
        return stack;
    }

    private static LinearLayout actionCard(Context c, String title, String subtitle, Runnable action) {
        LinearLayout panel = new LinearLayout(c);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER_VERTICAL);
        panel.setPadding(dp(c, 14), dp(c, 9), dp(c, 8), dp(c, 9));
        panel.setBackground(GoreeVisuals.card(c.getResources(), 0xDFF2F0FF, 22));
        panel.setFocusable(true);
        panel.setClickable(true);
        panel.setContentDescription(title + ". " + subtitle);
        panel.addView(label(c, title, 15, 0xFF27224B, true));
        panel.addView(label(c, subtitle, 11, 0xFF56516C, false));
        panel.setOnClickListener(v -> action.run());
        return panel;
    }

    private static TextView label(Context c, String s, int size, int color, boolean bold) {
        TextView view = new TextView(c);
        view.setText(s);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(null, Typeface.BOLD);
        view.setMaxLines(2);
        return view;
    }

    private static int dp(Context c, int v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }
}
