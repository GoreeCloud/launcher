package com.goreecloud.launcher.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LauncherIconPackSafetyTest {
    @Test
    fun externalIconPackResourceFailuresFallBackInsteadOfEscaping() {
        assertNull(
            LauncherIconPackSafety.failSoft<String> {
                throw IllegalStateException("malformed third-party icon-pack resource")
            },
        )
    }

    @Test
    fun validExternalIconPackResourceResultIsPreserved() {
        assertEquals("drawable", LauncherIconPackSafety.failSoft { "drawable" })
    }
}
