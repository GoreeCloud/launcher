package com.goreecloud.launcher;

import org.junit.Test;
import static org.junit.Assert.*;

public final class ResponsiveHomePolicyTest {
    @Test public void drawerIconRowsAreCompactAndScaleForAccessibility() {
        assertEquals(108, ResponsiveHomePolicy.drawerTileHeightDp(true, 1f));
        assertEquals(126, ResponsiveHomePolicy.drawerTileHeightDp(true, 1.45f));
        assertEquals(148, ResponsiveHomePolicy.drawerTileHeightDp(true, 2f));
        assertEquals(176, ResponsiveHomePolicy.drawerTileHeightDp(true, 4f));
        assertEquals(80, ResponsiveHomePolicy.drawerTileHeightDp(false, 1f));
    }

    @Test public void dockFitsIconsAndOptionalLabelsAtAnySupportedFontScale() {
        assertEquals(68, ResponsiveHomePolicy.dockHeightDp(false, 1f));
        assertEquals(68, ResponsiveHomePolicy.dockHeightDp(false, 2f));
        assertEquals(108, ResponsiveHomePolicy.dockHeightDp(true, 1f));
        assertEquals(124, ResponsiveHomePolicy.dockHeightDp(true, 1.45f));
        assertEquals(146, ResponsiveHomePolicy.dockHeightDp(true, 2f));
        assertEquals(186, ResponsiveHomePolicy.dockHeightDp(true, 4f));
        assertEquals(108, ResponsiveHomePolicy.dockHeightDp(true, Float.NaN));
        assertTrue(ResponsiveHomePolicy.dockHeightDp(true, 2f) >
                   ResponsiveHomePolicy.dockHeightDp(true, 1f));
    }

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

    @Test public void veryLargeTextOverridesDenseGridWithoutMutatingPreference() {
        assertEquals(4, ResponsiveHomePolicy.visibleColumns(4, 1.0f));
        assertEquals(4, ResponsiveHomePolicy.visibleColumns(4, 1.45f));
        assertEquals(2, ResponsiveHomePolicy.visibleColumns(4, 2.0f));
        assertEquals(2, ResponsiveHomePolicy.visibleColumns(6, 2.0f));
        assertEquals(3, ResponsiveHomePolicy.visibleColumns(2, 1.0f));
        assertEquals(112, ResponsiveHomePolicy.maximumTileWidthDp(1.45f));
        assertEquals(180, ResponsiveHomePolicy.maximumTileWidthDp(2.0f));
    }

    @Test public void fontScalingRejectsNonsensicalBounds() {
        assertEquals(40, ResponsiveHomePolicy.labelHeightDp(Float.NaN));
        assertEquals(40, ResponsiveHomePolicy.labelHeightDp(0.5f));
        assertEquals(120, ResponsiveHomePolicy.labelHeightDp(4.0f));
        assertEquals(212, ResponsiveHomePolicy.shortcutHeightDp(true, true, 4f));
    }
}
