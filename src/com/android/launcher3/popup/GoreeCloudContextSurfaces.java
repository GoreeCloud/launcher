package com.android.launcher3.popup;

import android.app.Notification;
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
        bg.setCornerRadius(dp(anchor, 22));
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
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setBackground(surface(0xFFF8F7FC, original));
        bar.setPadding(dp(original, 6), dp(original, 6), dp(original, 6), dp(original, 6));
        SystemShortcut remove = null, customize = null, appInfo = null;
        for (SystemShortcut s : shortcuts) {
            if (s instanceof SystemShortcut.RemoveApp) remove = s;
            if (s instanceof app.lawnchair.ui.popup.LawnchairShortcut.Customize) customize = s;
            if (s instanceof SystemShortcut.AppInfo) appInfo = s;
        }
        boolean home = item.container == LauncherSettings.Favorites.CONTAINER_DESKTOP
                || item.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT;
        action(bar, original, "✓", "Select", home, v -> {
            AbstractFloatingView.closeAllOpenViews(launcher);
            original.setSelected(true);
            launcher.getStateManager().goToState(LauncherState.EDIT_MODE);
        });
        action(bar, original, "▤", "Remove", home && remove != null,
                remove == null ? null : remove::onClick);
        action(bar, original, "✎", "Edit", customize != null,
                customize == null ? null : customize::onClick);
        action(bar, original, "ⓘ", "App info", appInfo != null,
                appInfo == null ? null : appInfo::onClick);
        return bar;
    }

    private static void action(LinearLayout row, View anchor, String icon, String name,
            boolean enabled, View.OnClickListener click) {
        LinearLayout tile = new LinearLayout(anchor.getContext());
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        TextView symbol = text(anchor, icon, 26);
        TextView label = text(anchor, name, 12);
        tile.addView(symbol, new LinearLayout.LayoutParams(-1, dp(anchor, 39)));
        tile.addView(label, new LinearLayout.LayoutParams(-1, dp(anchor, 26)));
        tile.setAlpha(enabled ? 1f : .35f);
        tile.setContentDescription(name);
        tile.setFocusable(enabled);
        if (enabled && click != null) tile.setOnClickListener(click);
        row.addView(tile, new LinearLayout.LayoutParams(0, dp(anchor, 72), 1f));
    }

    /** No permission, no card. Never invent contextual data. */
    public static View notificationCard(Launcher launcher, BubbleTextView anchor, ItemInfo item) {
        NotificationListener listener = NotificationListener.getInstanceIfConnected();
        if (listener == null || item.user == null || item.getTargetPackage() == null) return null;
        try {
            StatusBarNotification latest = null;
            StatusBarNotification[] all = listener.getActiveNotifications();
            if (all == null) return null;
            for (StatusBarNotification sbn : all) {
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
