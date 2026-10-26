package com.goreecloud.launcher;

/** Pure layout policy; keep orientation/accessibility decisions testable without Android. */
public final class ResponsiveHomePolicy {
    private ResponsiveHomePolicy() {}

    public static boolean shouldScroll(boolean isLandscape, float fontScale) {
        return isLandscape || fontScale >= 1.3f;
    }

    /** Prioritize readable names over the preferred compact grid at extreme font sizes. */
    public static int visibleColumns(int preferredColumns, float fontScale) {
        int bounded = Math.max(3, Math.min(6, preferredColumns));
        return fontScale >= 1.75f ? Math.min(2, bounded) : bounded;
    }

    public static int maximumTileWidthDp(float fontScale) {
        return fontScale >= 1.75f ? 180 : 112;
    }

    public static int shortcutHeightDp(boolean hasLabels, boolean scrollableHome) {
        return shortcutHeightDp(hasLabels, scrollableHome, 1.0f);
    }

    /** Two-line app labels need extra measured space when the user enlarges system text. */
    public static int labelHeightDp(float fontScale) {
        float safeScale = Float.isNaN(fontScale) ? 1f
                : Math.max(1f, Math.min(3f, fontScale));
        return (int) Math.ceil(40f * safeScale);
    }

    public static int shortcutHeightDp(boolean hasLabels, boolean scrollableHome, float fontScale) {
        if (!hasLabels) return 84;
        return (scrollableHome ? 132 : 112) + labelHeightDp(fontScale) - 40;
    }
}
