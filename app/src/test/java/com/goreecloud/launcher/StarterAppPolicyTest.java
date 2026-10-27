package com.goreecloud.launcher;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Arrays;

public final class StarterAppPolicyTest {
    @Test public void supportsAllFiveDefaultRoles() {
        assertArrayEquals(new String[]{"Phone","Messages","Email","Browser","Camera"},
                StarterAppPolicy.ROLES);
    }
    @Test public void recognizesCommonAndroidReplacements() {
        assertTrue(StarterAppPolicy.aliases("Email").contains("Gmail"));
        assertTrue(StarterAppPolicy.aliases("Browser").contains("Chrome"));
        assertTrue(StarterAppPolicy.aliases("Messages").contains("Messaging"));
    }
    @Test public void limitsFiveRowPreferencesSafely() {
        assertEquals(5,StarterAppPolicy.boundedRows(5));
        assertEquals(4,StarterAppPolicy.boundedRows(2));
        assertEquals(7,StarterAppPolicy.boundedRows(10));
    }
}
