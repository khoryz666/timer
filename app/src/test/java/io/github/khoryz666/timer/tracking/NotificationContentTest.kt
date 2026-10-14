package io.github.khoryz666.timer.tracking

import io.github.khoryz666.timer.projects.Project
import io.github.khoryz666.timer.quota.DayTotals
import io.github.khoryz666.timer.quota.QuotaSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class NotificationContentTest {
    private val minute = 60_000L
    private val projects = listOf(Project(1, "KEFOS", 60), Project(2, "QorFinder", 40))

    private fun status(running: Session?, engineMs: Long, joyMs: Long) = TodayStatus.of(
        LocalDate.of(2026, 10, 14), 0, running, projects, DayTotals(engineMs = engineMs, joyMs = joyMs), QuotaSettings()
    )

    @Test
    fun `shows the running project, Engine blocks and Joy time`() {
        val running = Session(pillar = Pillar.ENGINE, projectId = 1, startedAt = 1234)
        val content = NotificationContent.of(status(running, engineMs = 200 * minute, joyMs = 70 * minute))

        assertEquals("Engine · KEFOS", content.title)
        assertEquals("Engine 2/4 blocks · Joy 1h 10m/4h", content.text)
        assertEquals(1234L, content.runningSince)
        assertEquals(listOf("Stop", "Joy", "QorFinder"), content.actions.map { it.label })
    }

    @Test
    fun `idle shows no chronometer`() {
        val content = NotificationContent.of(status(null, 0, 0))

        assertEquals("Not tracking", content.title)
        assertNull(content.runningSince)
    }
}
