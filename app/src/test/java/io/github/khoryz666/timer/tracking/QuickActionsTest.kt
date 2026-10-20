package io.github.khoryz666.timer.tracking

import io.github.khoryz666.timer.projects.Project
import io.github.khoryz666.timer.tracking.StatusFixtures.running
import io.github.khoryz666.timer.tracking.StatusFixtures.status
import io.github.khoryz666.timer.tracking.StatusFixtures.totals
import org.junit.Assert.assertEquals
import org.junit.Test

class QuickActionsTest {
    private fun labels(status: TodayStatus) = quickActions(status).map { it.label }

    @Test
    fun `idle offers every project, NeetCode, Reading and Joy`() {
        assertEquals(listOf("QorFinder", "KEFOS", "NeetCode", "Reading", "Joy"), labels(status()))
    }

    @Test
    fun `while a project runs, offers Stop and every other switch, the other project included`() {
        val status = status(running(TrackTarget.Project(1)), totals(project = 150, neetCode = 60))
        assertEquals(listOf("Stop", "KEFOS", "NeetCode", "Reading", "Joy"), labels(status))
    }

    @Test
    fun `while Joy runs, offers Stop then every focus switch`() {
        assertEquals(listOf("Stop", "QorFinder", "KEFOS", "NeetCode", "Reading"), labels(status(running(TrackTarget.Joy))))
    }

    @Test
    fun `the order stays fixed however far each track is from its target`() {
        val met = status(totals = totals(project = 300, neetCode = 60, reading = 0))
        assertEquals(labels(status()), labels(met))
    }

    @Test
    fun `every project this week gets a button that starts it`() {
        val third = Project(3, "Rust CLI", 0)
        val actions = quickActions(status(projects = StatusFixtures.projects + third))
        assertEquals(QuickAction.Switch(TrackTarget.Project(3), "Rust CLI"), actions[2])
        assertEquals(6, actions.size)
    }
}
