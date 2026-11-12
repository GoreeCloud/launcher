package com.goreecloud.launcher;

import org.junit.Test;
import static org.junit.Assert.*;

public final class GcUiPaletteTest {
    @Test public void unconfiguredOrInvalidPreferencesRemainNeutral() {
        assertEquals("neutral", GcUiPalette.normalize(null));
        assertEquals("neutral", GcUiPalette.normalize(""));
        assertEquals("neutral", GcUiPalette.normalize("violet"));
        assertEquals("neutral", GcUiPalette.normalize("../../../iris"));
        assertEquals(0, GcUiPalette.selectedIndex("unknown"));
        assertEquals(0xFF374151, GcUiPalette.resolve(null).primary);
    }
    @Test public void explicitUserColorsTakePrecedenceOverNeutralFallback() {
        assertEquals(0xFF5145D9, GcUiPalette.resolve("iris").primary);
        assertEquals(0xFF087C8D, GcUiPalette.resolve("lagoon").primary);
        assertEquals(0xFF1D4ED8, GcUiPalette.resolve("ocean").primary);
        assertEquals(0xFF3F744B, GcUiPalette.resolve("moss").primary);
        assertEquals(0xFFAE4D3F, GcUiPalette.resolve("ember").primary);
    }
    @Test public void everyPairedRoleMeetsNormalTextContrast() {
        assertEquals(GcUiPalette.IDS.length, GcUiPalette.LABELS.length);
        for (String name : GcUiPalette.IDS) {
            GcUiPalette.Roles roles = GcUiPalette.resolve(name);
            assertTrue(name+" primary", contrast(roles.primary, roles.onPrimary) >= 4.5);
            assertTrue(name+" container", contrast(roles.container, roles.onContainer) >= 4.5);
        }
    }
    private static double lum(int color) {
        double[] v = new double[3];
        for (int i=0;i<3;i++) {
            double c = ((color >> (16 - i * 8)) & 255)/255.0;
            v[i] = c <= 0.04045 ? c/12.92 : Math.pow((c+0.055)/1.055,2.4);
        }
        return 0.2126*v[0]+0.7152*v[1]+0.0722*v[2];
    }
    private static double contrast(int background,int foreground) {
        double a=lum(background), b=lum(foreground);
        return (Math.max(a,b)+0.05)/(Math.min(a,b)+0.05);
    }
}
