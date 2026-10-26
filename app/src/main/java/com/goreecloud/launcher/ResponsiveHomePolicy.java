package com.goreecloud.launcher;

/** Pure layout policy; keep orientation/accessibility decisions testable without Android. */
public final class ResponsiveHomePolicy {
    private ResponsiveHomePolicy() {}

    public static boolean shouldScroll(boolean isLandscape, float fontScale) {
        return isLandscape || fontScale >= 1.3f;
    }

    public static int shortcutHeightDp(boolean hasLabels, boolean scrollableHome) {
        if (!hasLabels) return 84;
        return scrollableHome ? 132 : 112;
    }
}
