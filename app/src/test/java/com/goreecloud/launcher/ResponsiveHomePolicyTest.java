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
}
