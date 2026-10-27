package com.goreecloud.launcher;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;

/** Neutral fallbacks. Home itself displays the user's REAL Android wallpaper. */
public final class GoreeVisuals {
    public static final int IRIS = 0xFF6558DF;
    public static final int WHITE = Color.WHITE;
    public static final int NIGHT = 0xFF171923;
    private GoreeVisuals() {}

    /** Dark destinations must use bright system-bar icons, including Android 16. */
    public static void useLightSystemBarIcons(android.app.Activity activity) {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            android.view.WindowInsetsController controller =
                    activity.getWindow().getInsetsController();
            if (controller != null) {
                int mask = android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                controller.setSystemBarsAppearance(0, mask);
            }
        } else {
            android.view.View decor = activity.getWindow().getDecorView();
            int flags = decor.getSystemUiVisibility();
            decor.setSystemUiVisibility(flags
                    & ~android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    & ~android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
    }

    /** No artificial purple gradient; an opaque matte behind the app drawer. */
    public static Drawable atmosphere(boolean drawer) {
        return new ColorDrawable(drawer ? 0xFF191B29 : 0xFF202630);
    }

    public static GradientDrawable card(Resources resources, int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radiusDp * resources.getDisplayMetrics().density);
        return drawable;
    }

    /** Subtle dark-glass dock rather than five rectangular empty buttons. */
    public static GradientDrawable translucentDock(Resources resources) {
        return card(resources, 0xAD1E2632, 30);
    }
}
