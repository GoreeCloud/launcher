package com.goreecloud.launcher;

import org.junit.Test;
import static org.junit.Assert.*;

public final class BottomPrimaryNavPolicyTest {
    @Test public void regularPhoneHasDetachedFloatingBarAndTouchTargets() {
        assertEquals(48, BottomPrimaryNavPolicy.touchHeightDp(1f));
        assertEquals(66, BottomPrimaryNavPolicy.barHeightDp(1f));
        assertEquals(98, BottomPrimaryNavPolicy.contentClearanceDp(1f));
        assertEquals(280, BottomPrimaryNavPolicy.barWidthDp(320));
    }

    @Test public void tabletKeepsPrimaryNavCompact() {
        assertEquals(440, BottomPrimaryNavPolicy.barWidthDp(800));
        assertEquals(240, BottomPrimaryNavPolicy.barWidthDp(280));
    }

    @Test public void enlargedFontsExpandBarAndContentClearanceTogether() {
        assertEquals(84, BottomPrimaryNavPolicy.touchHeightDp(2f));
        assertEquals(102, BottomPrimaryNavPolicy.barHeightDp(2f));
        assertEquals(134, BottomPrimaryNavPolicy.contentClearanceDp(2f));
        assertTrue(BottomPrimaryNavPolicy.contentClearanceDp(1.5f)
                > BottomPrimaryNavPolicy.barHeightDp(1.5f));
    }

    @Test public void unexpectedlySmallFontScaleDoesNotReduceTouchHeight() {
        assertEquals(48, BottomPrimaryNavPolicy.touchHeightDp(0.8f));
        assertEquals(48, BottomPrimaryNavPolicy.touchHeightDp(0f));
    }
}
