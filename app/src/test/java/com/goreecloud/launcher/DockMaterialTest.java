package com.goreecloud.launcher;

import org.junit.Test;
import static org.junit.Assert.*;

public final class DockMaterialTest {
    @Test public void invalidValuesDefaultToFrosted() {
        assertEquals(DockMaterial.FROSTED, DockMaterial.normalize(null));
        assertEquals(DockMaterial.FROSTED, DockMaterial.normalize(""));
        assertEquals(DockMaterial.FROSTED, DockMaterial.normalize("unknown"));
    }

    @Test public void choicesRoundTripBetweenLabelsAndSettings() {
        String[] modes = {DockMaterial.FROSTED, DockMaterial.CLEAR, DockMaterial.SOLID};
        String[] labels = {"Frosted Glass", "Clear Glass", "Solid"};
        for (int i = 0; i < modes.length; i++) {
            assertEquals(modes[i], DockMaterial.fromIndex(i));
            assertEquals(i, DockMaterial.index(modes[i]));
            assertEquals(labels[i], DockMaterial.label(modes[i]));
        }
        assertEquals(DockMaterial.FROSTED, DockMaterial.fromIndex(-1));
        assertEquals(DockMaterial.FROSTED, DockMaterial.fromIndex(3));
        assertEquals(0, DockMaterial.index("invalid"));
    }
}
