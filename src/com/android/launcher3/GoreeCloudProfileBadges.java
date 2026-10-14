package com.android.launcher3;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Process;
import android.os.UserHandle;
import androidx.annotation.NonNull;
import app.lawnchair.preferences.PreferenceManager;

/** A readable P / W badge kept out of notification dots' top-right corner. */
public final class GoreeCloudProfileBadges {
    private static final Paint FILL = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint LETTER = new Paint(Paint.ANTI_ALIAS_FLAG);
    private GoreeCloudProfileBadges() { }

    public static void draw(@NonNull Canvas canvas, @NonNull BubbleTextView icon) {
        Object item = icon.getTag();
        if (!(item instanceof com.android.launcher3.model.data.ItemInfoWithIcon info)) return;
        UserHandle user = info.user;
        if (user == null) return;
        boolean work = !Process.myUserHandle().equals(user);
        String position = PreferenceManager.getInstance(icon.getContext())
                .getProfileBadgePosition().get();
        // No top_right option: native notifications always own that corner.
        if (!("bottom_left".equals(position) || "bottom_right".equals(position))) {
            position = "top_left";
        }
        Rect bounds = new Rect();
        icon.getIconBounds(bounds);
        if (bounds.isEmpty()) return;
        final float density = icon.getResources().getDisplayMetrics().density;
        float radius = Math.max(7f * density, bounds.width() * .16f);
        float cx = "bottom_right".equals(position) ? bounds.right - radius / 2f
                : bounds.left + radius / 2f;
        float cy = "top_left".equals(position) ? bounds.top + radius / 2f
                : bounds.bottom - radius / 2f;
        FILL.setStyle(Paint.Style.FILL);
        FILL.setColor(Color.WHITE);
        canvas.drawCircle(cx, cy, radius + 1.5f * density, FILL);
        FILL.setColor(work ? Color.rgb(151, 76, 21) : Color.rgb(0, 104, 180));
        canvas.drawCircle(cx, cy, radius, FILL);
        LETTER.setColor(Color.WHITE);
        LETTER.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD));
        LETTER.setTextAlign(Paint.Align.CENTER);
        LETTER.setTextSize(radius * 1.3f);
        Paint.FontMetrics fm = LETTER.getFontMetrics();
        canvas.drawText(work ? "W" : "P", cx, cy - (fm.ascent + fm.descent) / 2f, LETTER);
    }
}
