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

/** Home-only glyph badges; profile tabs distinguish drawer results. */
public final class GoreeCloudProfileBadges {
    private static final Paint FILL = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint GLYPH = new Paint(Paint.ANTI_ALIAS_FLAG);
    private GoreeCloudProfileBadges() { }

    public static void draw(@NonNull Canvas canvas, @NonNull BubbleTextView icon) {
        // App drawer and search use separate profile pages: never overlay icon badges there.
        if (icon.mDisplay != BubbleTextView.DISPLAY_WORKSPACE) return;
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
        GLYPH.setColor(Color.WHITE);
        GLYPH.setStyle(Paint.Style.FILL);
        if (work) {
            // Briefcase: body, carry handle and centre clasp.
            canvas.drawRoundRect(cx - radius * .64f, cy - radius * .28f,
                    cx + radius * .64f, cy + radius * .48f,
                    radius * .12f, radius * .12f, GLYPH);
            GLYPH.setStyle(Paint.Style.STROKE);
            GLYPH.setStrokeWidth(radius * .16f);
            canvas.drawRoundRect(cx - radius * .25f, cy - radius * .60f,
                    cx + radius * .25f, cy + radius * .08f,
                    radius * .13f, radius * .13f, GLYPH);
            GLYPH.setStyle(Paint.Style.FILL);
        } else {
            // Person: head and shoulder silhouette, no letter overlays.
            canvas.drawCircle(cx, cy - radius * .28f, radius * .28f, GLYPH);
            canvas.drawRoundRect(cx - radius * .52f, cy + radius * .10f,
                    cx + radius * .52f, cy + radius * .65f,
                    radius * .35f, radius * .35f, GLYPH);
        }
    }
}
