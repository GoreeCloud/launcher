package com.goreecloud.launcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import org.junit.Test;
import static org.junit.Assert.*;

public final class DockOrderTest {
    @Test public void legacyPinsStartWithDeterministicOrder() {
        assertEquals(Arrays.asList("app.a/.A", "app.b/.B"),
                DockOrder.reconcile(new HashSet<>(Arrays.asList("app.b/.B", "app.a/.A")), ""));
    }

    @Test public void customOrderReconcilesMissingAndUninstalledWithoutErasingPins() {
        assertEquals(Arrays.asList("app.b/.B", "app.a/.A", "app.c/.C"),
                DockOrder.reconcile(new HashSet<>(Arrays.asList("app.a/.A", "app.b/.B", "app.c/.C")),
                        "not.installed/.Missing|app.b/.B|app.b/.B|app.a/.A"));
    }

    @Test public void movingBoundedPinsSwapsOnlyAdjacentPositions() {
        ArrayList<String> list = new ArrayList<>(Arrays.asList("app.a/.A", "app.b/.B", "app.c/.C"));
        assertTrue(DockOrder.move(list, "app.c/.C", -1));
        assertEquals(Arrays.asList("app.a/.A", "app.c/.C", "app.b/.B"), list);
        assertEquals("app.a/.A|app.c/.C|app.b/.B", DockOrder.encode(list));
        assertFalse(DockOrder.move(list, "app.a/.A", -1));
        assertFalse(DockOrder.move(list, "absent", 1));
        assertFalse(DockOrder.move(list, "app.c/.C", 2));
    }

    @Test public void zeroPinsStayEmpty() {
        assertEquals(Collections.emptyList(), DockOrder.reconcile(Collections.emptySet(), "app.a/.A"));
    }
}
