package io.github.khoryz666.timer.tracking

import io.github.khoryz666.timer.day.DayType
import io.github.khoryz666.timer.tracking.StatusFixtures.running
import io.github.khoryz666.timer.tracking.StatusFixtures.status
import io.github.khoryz666.timer.tracking.StatusFixtures.totals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationContentTest {
    @Test
    fun `collapsed shows what is running and a one-line summary, expanded one line per track`() {
        val status = status(running(TrackTarget.Project(1), startedAt = 1234), totals(project = 150, neetCode = 60, joy = 70))
        val content = NotificationContent.of(status)

        assertEquals("Project · QorFinder", content.title)
        assertEquals("Deep day", content.dayType)
        assertEquals("Project 2/5 · Neet 1/1 · Read 0/2", content.text)
        assertEquals(
            listOf("Project 2/5 blocks (floor 3)", "NeetCode 1/1 ✓", "Reading 0/2", "Joy 1h 10m / 4h"),
            content.lines,
        )
        assertEquals(1234L, content.runningSince)
        assertEquals(listOf("Stop", "KEFOS", "NeetCode", "Reading", "Joy"), content.actions.map { it.label })
    }

    @Test
    fun `tracks with no target and no time today are left out`() {
        val content = NotificationContent.of(status(dayType = DayType.LIGHT, totals = totals(reading = 70)))

        assertEquals("Project 0/3 · Read 1", content.text)
        assertEquals(listOf("Project 0/3 blocks (floor 1)", "Reading 1 · no target", "Joy 0m / 4h"), content.lines)
    }

    @Test
    fun `an unpicked day type is marked, and idle shows no chronometer`() {
        val content = NotificationContent.of(status(chosen = false))

        assertEquals("Not tracking", content.title)
        assertEquals("Deep day?", content.dayType)
        assertNull(content.runningSince)
    }

    @Test
    fun `NeetCode and Reading titles are just the track`() {
        assertEquals("NeetCode", NotificationContent.of(status(running(TrackTarget.NeetCode))).title)
        assertEquals("Reading", NotificationContent.of(status(running(TrackTarget.Reading))).title)
    }
}
