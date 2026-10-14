package io.github.khoryz666.timer.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseTest {
    private fun json(tag: String = "v2.1.0", assets: String = APK_ASSET, body: String = "### Added\\r\\n- **Weekly** split\\r\\n- `Fix`") = """
        {
          "tag_name": "$tag",
          "name": "Release $tag",
          "body": "$body",
          "draft": false,
          "assets": [$assets]
        }
    """.trimIndent()

    @Test
    fun `parses the tag, notes and the APK asset`() {
        val release = Release.parse(json())!!

        assertEquals(AppVersion(2, 1, 0), release.version)
        assertEquals("https://github.com/khoryz666/timer/releases/download/v2.1.0/Timer-2.1.0.apk", release.apkUrl)
        assertEquals(1_308_749L, release.apkSize)
        assertEquals("Added\n• Weekly split\n• Fix", release.notes)
    }

    @Test
    fun `picks the APK among other assets`() {
        val release = Release.parse(json(assets = """{"name": "checksums.txt", "browser_download_url": "x", "size": 1}, $APK_ASSET"""))
        assertEquals(1_308_749L, release?.apkSize)
    }

    @Test
    fun `no APK, a non-version tag or malformed JSON give null`() {
        assertNull(Release.parse(json(assets = "")))
        assertNull(Release.parse(json(tag = "nightly")))
        assertNull(Release.parse("{ not json"))
        assertNull(Release.parse("""{"message": "Not Found"}"""))
    }

    @Test
    fun `is newer only when the release version is greater than the installed one`() {
        val release = Release.parse(json())!!
        assertTrue(release.isNewerThan("2.0.0"))
        assertTrue(release.isNewerThan("2.1.0-debug"))
        assertFalse(release.isNewerThan("2.1.0"))
        assertFalse(release.isNewerThan("2.2.0"))
        assertFalse(release.isNewerThan("unknown"))
    }

    @Test
    fun `plain notes strip markdown headings, bold and code and collapse blank runs`() {
        val notes = Release.plainNotes("## [2.1.0]\n\n\n\n### Fixed\n* A **bold** `fix`\n  - nested\n")
        assertEquals("[2.1.0]\n\nFixed\n• A bold fix\n  • nested", notes)
    }

    @Test
    fun `auto-check is due once a day`() {
        val day = AUTO_CHECK_INTERVAL_MS
        assertTrue(isAutoCheckDue(lastCheck = 0, now = 5 * day))
        assertFalse(isAutoCheckDue(lastCheck = 5 * day, now = 5 * day + day - 1))
        assertTrue(isAutoCheckDue(lastCheck = 5 * day, now = 6 * day))
        assertTrue(isAutoCheckDue(lastCheck = 5 * day, now = 4 * day)) // clock set back
    }

    private companion object {
        const val APK_ASSET = """{"name": "Timer-2.1.0.apk", "size": 1308749, "browser_download_url": "https://github.com/khoryz666/timer/releases/download/v2.1.0/Timer-2.1.0.apk"}"""
    }
}
