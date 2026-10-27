package com.goreecloud.launcher;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static com.goreecloud.launcher.HomeGesturePolicy.Action;
import static com.goreecloud.launcher.HomeGesturePolicy.resolve;

public final class HomeGesturePolicyTest {
    @Test public void swipingUpFromHomeOpensDrawer() {
        assertEquals(Action.OPEN_DRAWER, resolve(4, -160, 96, false, true, true, true));
    }
    @Test public void swipingDownOnHomeOpensUniversalSearch() {
        assertEquals(Action.OPEN_SEARCH, resolve(4, 180, 96, false, true, true, true));
        assertEquals(Action.NONE, resolve(4, 180, 96, false, true, false, true));
    }
    @Test public void swipingDownFromTopClosesDrawer() {
        assertEquals(Action.CLOSE_DRAWER, resolve(1, 180, 96, true, true, true, true));
    }
    @Test public void scrollingDrawerDoesNotCloseIt() {
        assertEquals(Action.NONE, resolve(0, 220, 96, true, true, true, false));
    }
    @Test public void userDisabledGesturesAreHonored() {
        assertEquals(Action.NONE, resolve(0, -180, 96, false, false, true, true));
        assertEquals(Action.NONE, resolve(0, 180, 96, true, true, false, true));
    }
    @Test public void diagonalScrollDoesNotTriggerDrawer() {
        assertEquals(Action.NONE, resolve(130, -140, 96, false, true, true, true));
        assertEquals(Action.NONE, resolve(44, -72, 96, false, true, true, true));
    }
    @Test public void sidewaysSwipesPageHomeOnly() {
        assertEquals(Action.PREVIOUS_PAGE, resolve(150, 10, 96, false, true, true, true));
        assertEquals(Action.NEXT_PAGE, resolve(-150, 10, 96, false, true, true, true));
        assertEquals(Action.NONE, resolve(-150, 10, 96, true, true, true, true));
    }
    @Test public void zeroOrNaNMovementIsIgnored() {
        assertEquals(Action.NONE, resolve(0, 0, 96, false, true, true, true));
        assertEquals(Action.NONE, resolve(Float.NaN, -150, 96, false, true, true, true));
    }
}
