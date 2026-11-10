package com.goreecloud.launcher;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;

/** Native, resolution-independent GoreeCloud surfaces; no screenshot/wallpaper asset. */
public final class GoreeVisuals {
    public static final int IRIS = 0xFF7564F5;
    public static final int WHITE = Color.WHITE;
    public static final int NIGHT = 0xFF171831;
    private GoreeVisuals() {}

    public static Drawable atmosphere(boolean drawer) {
        return new Drawable() {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override public void draw(Canvas canvas) {
                Rect b = getBounds();
                float width = b.width(), height = b.height();
                if (width <= 0 || height <= 0) return;
                int[] colors = drawer
                        ? new int[] {0xFF242341, 0xFF353454, 0xFF191A32}
                        : new int[] {0xFFA9B7FF, 0xFF787BD9, 0xFF474892};
                paint.setShader(new LinearGradient(0, b.top, width, height,
                        colors, null, Shader.TileMode.CLAMP));
                canvas.drawRect(b, paint);
                if (!drawer) {
                    // Soft orbital form inspired by the founder's mockup;
                    // purely native Canvas rendering, not a generated image.
                    float cx = b.left + width * 0.40f, cy = b.top + height * 0.33f;
                    float radius = Math.max(width, height) * 0.46f;
                    paint.setShader(new RadialGradient(cx, cy, radius,
                            new int[] {0xD1D6E5FF, 0x6698B1FF, 0x00746BCD},
                            new float[] {0f, 0.68f, 1f}, Shader.TileMode.CLAMP));
                    canvas.drawCircle(cx, cy, radius, paint);
                }
                paint.setShader(null);
            }
            @Override public void setAlpha(int alpha) { invalidateSelf(); }
            @Override public void setColorFilter(ColorFilter filter) { invalidateSelf(); }
            @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
        };
    }

    public static GradientDrawable card(Resources resources, int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radiusDp * resources.getDisplayMetrics().density);
        return drawable;
    }

    public static GradientDrawable translucentDock(Resources resources) {
        return card(resources, 0xA7D6D6FF, 28);
    }
}
