package com.goreecloud.launcher;

/** Pure layout contract for the native floating Home/All apps primary bar.
 * Units are Android dp; the caller adds actual system bar insets.
 */
final class BottomPrimaryNavPolicy {
    private BottomPrimaryNavPolicy() {}

    static int touchHeightDp(float fontScale) {
        return Math.max(48, Math.round(36 * Math.max(1f, fontScale) + 12));
    }

    static int barHeightDp(float fontScale) {
        return touchHeightDp(fontScale) + 18;
    }

    static int contentClearanceDp(float fontScale) {
        // Dock height plus space so pinned apps, search and page controls
        // remain scrollable and never end underneath the floating bar.
        return barHeightDp(fontScale) + 32;
    }

    static int barWidthDp(int viewportDp) {
        return Math.min(440, Math.max(120, viewportDp - 40));
    }
}
