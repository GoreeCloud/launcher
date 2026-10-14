package com.android.launcher3.popup;

import android.app.Notification;
import android.app.KeyguardManager;
import android.content.Context;
import android.app.PendingIntent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.service.notification.StatusBarNotification;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.notification.NotificationListener;
import java.util.List;

/** Glaze context surfaces backed by Android's genuine actions and notifications. */
public final class GoreeCloudContextSurfaces {
    private GoreeCloudContextSurfaces() { }

    private static int dp(View anchor, int size) {
        return Math.round(anchor.getResources().getDisplayMetrics().density * size);
    }

    private static GradientDrawable surface(int tint, View anchor) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(tint);
        bg.setCornerRadius(dp(anchor, 20));
        bg.setStroke(dp(anchor, 1), 0xFFD9E5F0);
        return bg;
    }

    private static TextView text(View anchor, String label, int size) {
        TextView v = new TextView(anchor.getContext());
        v.setText(label);
        v.setTextSize(size);
        v.setTextColor(0xFF203247);
        v.setGravity(Gravity.CENTER);
        v.setMaxLines(2);
        return v;
    }

    public static View actionBar(Launcher launcher, BubbleTextView original,
            ItemInfo item, List<SystemShortcut> shortcuts) {
        LinearLayout bar = new LinearLayout(launcher);
        bar.setOrientation(LinearLayout.VERTICAL);
        bar.setBackground(surface(0xFFF8FBFF, original));
        bar.setPadding(dp(original, 8), dp(original, 8), dp(original, 8), dp(original, 8));
        bar.setElevation(dp(original, 6));
        SystemShortcut remove = null, customize = null, appInfo = null;
        for (SystemShortcut s : shortcuts) {
            if (s instanceof SystemShortcut.RemoveApp) remove = s;
            if (s instanceof app.lawnchair.ui.popup.LawnchairShortcut.Customize) customize = s;
            if (s instanceof SystemShortcut.AppInfo) appInfo = s;
        }
        boolean home = item.container == LauncherSettings.Favorites.CONTAINER_DESKTOP
                || item.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT;
        LinearLayout first = new LinearLayout(launcher);
        first.setOrientation(LinearLayout.HORIZONTAL);
        if (home) {
            action(first, original, "✓", "Select", true, v -> {
                AbstractFloatingView.closeAllOpenViews(launcher);
                original.setSelected(true);
                launcher.getStateManager().goToState(LauncherState.EDIT_MODE);
            });
            if (remove != null) action(first, original, "▤", "Remove", true, remove::onClick);
        }
        if (first.getChildCount() > 0) bar.addView(first);
        LinearLayout second = new LinearLayout(launcher);
        second.setOrientation(LinearLayout.HORIZONTAL);
        if (customize != null) action(second, original, "✎", "Edit", true, customize::onClick);
        if (appInfo != null) action(second, original, "ⓘ", "App info", true, appInfo::onClick);
        if (second.getChildCount() > 0) bar.addView(second);
        return bar;
    }

    private static void action(LinearLayout row, View anchor, String icon, String name,
            boolean enabled, View.OnClickListener click) {
        if (!enabled || click == null) return;
        LinearLayout tile = new LinearLayout(anchor.getContext());
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setBackground(surface(Color.WHITE, anchor));
        TextView symbol = text(anchor, icon, 24);
        symbol.setTextColor(0xFF116992);
        TextView label = text(anchor, name, 13);
        tile.addView(symbol, new LinearLayout.LayoutParams(-1, dp(anchor, 31)));
        tile.addView(label, new LinearLayout.LayoutParams(-1, dp(anchor, 25)));
        tile.setContentDescription(name);
        tile.setFocusable(true);
        tile.setOnClickListener(click);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, dp(anchor, 66), 1f);
        params.setMargins(dp(anchor, 4), dp(anchor, 4), dp(anchor, 4), dp(anchor, 4));
        row.addView(tile, params);
    }

    /** No permission, no card. Never invent contextual data. */
    public static View notificationCard(Launcher launcher, BubbleTextView anchor, ItemInfo item) {
        NotificationListener listener = NotificationListener.getInstanceIfConnected();
        if (listener == null || item.user == null || item.getTargetPackage() == null) return null;
        KeyguardManager keyguard = (KeyguardManager) launcher.getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguard != null && keyguard.isKeyguardLocked()) return null;
        try {
            StatusBarNotification latest = null;
            StatusBarNotification[] all = listener.getActiveNotifications();
            if (all == null) return null;
            for (StatusBarNotification sbn : all) {
                if ((sbn.getNotification().flags & Notification.FLAG_GROUP_SUMMARY) != 0) continue;
                if (!item.user.equals(sbn.getUser())
                        || !item.getTargetPackage().equals(sbn.getPackageName())) continue;
                if (latest == null || sbn.getPostTime() > latest.getPostTime()) latest = sbn;
            }
            if (latest == null || latest.getNotification().extras == null) return null;
            Notification notice = latest.getNotification();
            CharSequence title = notice.extras.getCharSequence(Notification.EXTRA_TITLE);
            CharSequence detail = notice.extras.getCharSequence(Notification.EXTRA_TEXT);
            if (android.text.TextUtils.isEmpty(title) && android.text.TextUtils.isEmpty(detail)) return null;
            LinearLayout card = new LinearLayout(launcher);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(anchor, 16), dp(anchor, 10), dp(anchor, 16), dp(anchor, 12));
            card.setBackground(surface(0xFFF4EDF9, anchor));
            TextView section = text(anchor, "RECENT NOTIFICATION", 11);
            section.setGravity(Gravity.START);
            card.addView(section);
            TextView head = text(anchor, title == null ? "Notification" : title.toString(), 15);
            head.setGravity(Gravity.START);
            head.setTypeface(null, android.graphics.Typeface.BOLD);
            card.addView(head);
            if (detail != null) {
                TextView body = text(anchor, detail.toString(), 13);
                body.setGravity(Gravity.START);
                card.addView(body);
            }
            PendingIntent action = notice.contentIntent;
            if (action != null) {
                card.setFocusable(true);
                card.setOnClickListener(v -> {
                    try { action.send(); } catch (PendingIntent.CanceledException ignored) { }
                    AbstractFloatingView.closeAllOpenViews(launcher);
                });
            }
            return card;
        } catch (SecurityException | IllegalStateException ignored) {
            return null;
        }
    }
}
