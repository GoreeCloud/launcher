package com.goreecloud.launcher;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

public final class CatalogRefreshPolicyTest {
    private static Set<String> of(String... names) {
        return new HashSet<>(Arrays.asList(names));
    }

    @Test public void updatedCatalogPackageInvalidatesCachedIcons() {
        assertTrue(CatalogRefreshPolicy.updatedVisiblePackage(
                of("org.example.updated"), of("org.example.updated", "org.example.other")));
    }

    @Test public void unrelatedUpdateDoesNotInterruptCurrentHomeFocus() {
        assertFalse(CatalogRefreshPolicy.updatedVisiblePackage(
                of("org.example.unrelated"), of("org.example.visible")));
    }

    @Test public void emptyOrMissingUpdatesCannotForceRedraw() {
        assertFalse(CatalogRefreshPolicy.updatedVisiblePackage(
                Collections.emptySet(), of("org.example.visible")));
        assertFalse(CatalogRefreshPolicy.updatedVisiblePackage(
                of("org.example.visible"), Collections.emptySet()));
        assertFalse(CatalogRefreshPolicy.updatedVisiblePackage(
                null, of("org.example.visible")));
    }

    @Test public void multipleCoalescedBroadcastsInvalidateWhenAnyPackageMatches() {
        assertTrue(CatalogRefreshPolicy.updatedVisiblePackage(
                of("org.example.unrelated", "org.example.visible"),
                of("org.example.visible")));
    }
}
