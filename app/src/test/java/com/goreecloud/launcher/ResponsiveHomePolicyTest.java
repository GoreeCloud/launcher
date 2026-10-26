package com.goreecloud.launcher;

import org.junit.Test;
import static org.junit.Assert.*;

public final class ResponsiveHomePolicyTest {
    @Test public void portraitDefaultKeepsDockVisibleInFixedLayout() {
        assertFalse(ResponsiveHomePolicy.shouldScroll(false, 1.0f));
        assertFalse(ResponsiveHomePolicy.shouldScroll(false, 1.29f));
        assertEquals(112, ResponsiveHomePolicy.shortcutHeightDp(true, false));
    }
    @Test public void landscapeUsesScrollableFallback() {
        assertTrue(ResponsiveHomePolicy.shouldScroll(true, 1.0f));
        assertEquals(132, ResponsiveHomePolicy.shortcutHeightDp(true, true));
    }
    @Test public void largeTextUsesScrollableFallback() {
        assertTrue(ResponsiveHomePolicy.shouldScroll(false, 1.3f));
        assertTrue(ResponsiveHomePolicy.shouldScroll(false, 2.0f));
        assertEquals(84, ResponsiveHomePolicy.shortcutHeightDp(false, true));
    }
    @Test public void enlargedLabelsGetMeasuredHeight() {
        assertEquals(40, ResponsiveHomePolicy.labelHeightDp(1.0f));
        assertEquals(58, ResponsiveHomePolicy.labelHeightDp(1.45f));
        assertEquals(80, ResponsiveHomePolicy.labelHeightDp(2.0f));
        assertEquals(150, ResponsiveHomePolicy.shortcutHeightDp(true, true, 1.45f));
        assertEquals(172, ResponsiveHomePolicy.shortcutHeightDp(true, true, 2.0f));
        assertEquals(84, ResponsiveHomePolicy.shortcutHeightDp(false, true, 2.0f));
    }

    @Test public void fontScalingRejectsNonsensicalBounds() {
        assertEquals(40, ResponsiveHomePolicy.labelHeightDp(Float.NaN));
        assertEquals(40, ResponsiveHomePolicy.labelHeightDp(0.5f));
        assertEquals(120, ResponsiveHomePolicy.labelHeightDp(4.0f));
        assertEquals(212, ResponsiveHomePolicy.shortcutHeightDp(true, true, 4f));
    }
}
