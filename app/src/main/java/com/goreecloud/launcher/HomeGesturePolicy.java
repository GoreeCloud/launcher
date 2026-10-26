package com.goreecloud.launcher;

/** Pure, deterministic home/drawer swipe routing. Android coordinates grow downward. */
public final class HomeGesturePolicy {
    public enum Action { NONE, OPEN_DRAWER, CLOSE_DRAWER, PREVIOUS_PAGE, NEXT_PAGE }

    private HomeGesturePolicy() {}

    public static Action resolve(float deltaX, float deltaY, float threshold,
            boolean drawerOpen, boolean swipeUpEnabled, boolean swipeDownEnabled,
            boolean drawerAtTop) {
        if (threshold <= 0f || Float.isNaN(deltaX) || Float.isNaN(deltaY)) {
            return Action.NONE;
        }
        float x = Math.abs(deltaX);
        float y = Math.abs(deltaY);
        if (y >= threshold && y > x * 1.35f) {
            if (!drawerOpen && deltaY < 0f && swipeUpEnabled) return Action.OPEN_DRAWER;
            if (drawerOpen && deltaY > 0f && swipeDownEnabled && drawerAtTop)
                return Action.CLOSE_DRAWER;
        }
        // Horizontal gestures are reserved for Home workspace pages, never drawer scrolling.
        if (!drawerOpen && x >= threshold && x > y * 1.35f) {
            return deltaX > 0f ? Action.PREVIOUS_PAGE : Action.NEXT_PAGE;
        }
        return Action.NONE;
    }
}
