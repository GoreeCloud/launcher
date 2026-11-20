package com.goreecloud.launcher

import org.junit.Assert.*
import org.junit.Test

class LauncherCatalogTest {
    @Test fun searchMatchesNameOrPackage() {
        assertTrue(matchesApp("browser", "Browser", "org.example.browser"))
        assertTrue(matchesApp("org.example", "Browser", "org.example.browser"))
        assertFalse(matchesApp("camera", "Browser", "org.example.browser"))
        assertTrue(matchesApp(" ", "Browser", "org.example.browser"))
    }
}
