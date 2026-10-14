package io.github.khoryz666.timer.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionTest {
    private fun v(text: String) = requireNotNull(AppVersion.parse(text)) { text }

    @Test
    fun `parses plain, v-prefixed and pre-release versions`() {
        assertEquals(AppVersion(2, 0, 0), v("2.0.0"))
        assertEquals(AppVersion(2, 10, 3), v("v2.10.3"))
        assertEquals(AppVersion(2, 0, 0, "debug"), v("2.0.0-debug"))
        assertEquals("2.1.0-rc.1", v("v2.1.0-rc.1").toString())
    }

    @Test
    fun `rejects anything that isn't MAJOR_MINOR_PATCH`() {
        listOf("", "2", "2.0", "2.0.0.1", "version 2", "v2.x.0", "latest").forEach { assertNull(it, AppVersion.parse(it)) }
    }

    @Test
    fun `compares numerically, not as text`() {
        assertTrue(v("2.10.0") > v("2.9.9"))
        assertTrue(v("10.0.0") > v("9.99.99"))
        assertTrue(v("2.0.1") > v("2.0.0"))
        assertEquals(0, v("v2.0.0").compareTo(v("2.0.0")))
    }

    @Test
    fun `a pre-release sorts before its release`() {
        assertTrue(v("2.0.0-rc1") < v("2.0.0"))
        assertTrue(v("2.0.0-debug") < v("2.0.0"))
        assertTrue(v("2.0.0-debug") > v("1.9.9"))
    }
}
