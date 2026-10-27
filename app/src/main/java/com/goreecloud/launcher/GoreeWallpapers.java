package com.goreecloud.launcher;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import java.io.IOException;

/** Original, offline GoreeCloud wallpapers. No cloud service or external artwork. */
public final class GoreeWallpapers {
    public static final String[] NAMES = {
        "Goree • Atlantic", "Goree • Celestial", "Goree • Dunes", "Goree • Afterglow"
    };
    private static final int[][] PALETTES = {
        {0xFF071B37, 0xFF10506E, 0xFF00A7A1, 0xFF88E7D9},
        {0xFF0C102A, 0xFF25276A, 0xFF7664D6, 0xFFC1B9FF},
        {0xFF252A35, 0xFF5E5558, 0xFFB98B63, 0xFFF0D5A1},
        {0xFF181A32, 0xFF62415F, 0xFFC86F67, 0xFFFFD5A0}
    };
    private GoreeWallpapers() {}
    public static int count() { return NAMES.length; }

    public static Bitmap render(int theme, int width, int height) {
        if (theme < 0 || theme >= PALETTES.length) throw new IllegalArgumentException("theme");
        width = Math.max(100, Math.min(2048, width));
        height = Math.max(100, Math.min(4096, height));
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bitmap);
        c.save();
        c.scale(width / 1080f, height / 2400f);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        int[] a = PALETTES[theme];
        p.setShader(new LinearGradient(0, 0, 980, 2400,
                new int[]{a[0], a[1], a[2]}, new float[]{0f, 0.58f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, 1080, 2400, p);
        p.setShader(new RadialGradient(790, 520, 990, new int[]{
                color(a[3], 115), color(a[2], 26), Color.TRANSPARENT},
                null, Shader.TileMode.CLAMP));
        c.drawCircle(790, 520, 990, p);
        // Large hand-designed contour bands, kept below the shortcut reading zone.
        for (int band = 0; band < 5; band++) {
            Path wave = new Path();
            float y = 1170 + band * 240;
            float sway = (theme % 2 == 0 ? 1 : -1);
            wave.moveTo(-80, y + sway * 120);
            wave.cubicTo(240, y - 125, 580, y + 180, 1160, y - 70);
            wave.lineTo(1160, 2500);
            wave.lineTo(-80, 2500);
            wave.close();
            p.setShader(null);
            p.setStyle(Paint.Style.FILL);
            p.setColor(color(a[(band + 1) % a.length], 25 + band * 9));
            c.drawPath(wave, p);
        }
        // The GoreeCloud signature: paired translucent atmospheric halos.
        drawHalo(c, p, 815, 470, 445, a[3], 34);
        drawHalo(c, p, 815, 470, 355, a[3], 22);
        drawHalo(c, p, 145, 1820, 630, a[3], 23);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2.2f);
        p.setColor(color(a[3], 51));
        c.drawCircle(815, 470, 443, p);
        c.drawCircle(145, 1820, 628, p);
        p.setStyle(Paint.Style.FILL);
        // Quiet pinpoints add depth without fighting the UI.
        for (int k = 0; k < 130; k++) {
            int x = ((k * 479 + 71) % 1031) + 20;
            int y = ((k * 673 + 133) % 2277) + 45;
            float radius = (k % 11 == 0) ? 2.1f : 0.85f;
            p.setColor(color(a[3], 28 + ((k * 7) % 78)));
            c.drawCircle(x, y, radius, p);
        }
        c.restore();
        return bitmap;
    }

    private static void drawHalo(Canvas canvas, Paint paint, int x, int y,
                                  int radius, int pigment, int alpha) {
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new RadialGradient(x, y, radius,
                new int[]{color(pigment, alpha), color(pigment, 7), Color.TRANSPARENT},
                null, Shader.TileMode.CLAMP));
        canvas.drawCircle(x, y, radius, paint);
        paint.setShader(null);
    }
    private static int color(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }
    /** Called only after an explicit Apply tap in Home Screen Editor. */
    public static void apply(Context context, int theme) throws IOException {
        Bitmap bitmap = render(theme, 1080, 2400);
        try {
            WallpaperManager wallpaper = WallpaperManager.getInstance(context);
            if (android.os.Build.VERSION.SDK_INT >= 24) {
                wallpaper.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM);
            } else {
                wallpaper.setBitmap(bitmap);
            }
        } finally {
            bitmap.recycle();
        }
    }
}
