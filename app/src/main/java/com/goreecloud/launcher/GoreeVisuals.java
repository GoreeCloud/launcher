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

    /** Native layered Lens: translucent but no capture or live wallpaper blur. */
    public static GradientDrawable translucentDock(Resources resources) {
        return translucentDock(resources, DockMaterial.FROSTED);
    }

    public static GradientDrawable translucentDock(Resources resources, String requested) {
        String mode = DockMaterial.normalize(requested);
        int[] stops = DockMaterial.SOLID.equals(mode)
            ? new int[]{0xFF253744, 0xFF1B2D3A}
            : DockMaterial.CLEAR.equals(mode)
            ? new int[]{0xC2F5FBFF, 0xB5D5E9F2}
            : new int[]{0xEBFBFDFF, 0xCAE9F3F8, 0xB6DAE8EF};
        GradientDrawable surface = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM, stops);
        float density = resources.getDisplayMetrics().density;
        surface.setCornerRadius(30f * density);
        surface.setStroke(Math.max(1, Math.round(density)),
            DockMaterial.SOLID.equals(mode) ? 0xFF546A76 : 0xB8FFFFFF);
        surface.setDither(true);
        return surface;
    }
}
