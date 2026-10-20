package io.github.khoryz666.timer.tracking

import io.github.khoryz666.timer.tracking.StatusFixtures.running
import io.github.khoryz666.timer.tracking.StatusFixtures.status
import io.github.khoryz666.timer.tracking.StatusFixtures.totals
import org.junit.Assert.assertEquals
import org.junit.Test

class QuickActionsTest {
    private fun labels(status: TodayStatus) = quickActions(status).map { it.label }

    @Test
    fun `idle offers the track furthest from its target, then Joy, then the next track`() {
        // Deep day: Project 5h to go, Reading 2h, NeetCode 1h.
        assertEquals(listOf("QorFinder", "Joy", "Reading"), labels(status()))
    }

    @Test
    fun `while running offers Stop, the furthest other track, then Joy`() {
        val status = status(running(TrackTarget.Project(1)), totals(project = 150, neetCode = 60))
        assertEquals(listOf("Stop", "Reading", "Joy"), labels(status))
    }

    @Test
    fun `during Joy offers Stop then the two furthest tracks`() {
        assertEquals(listOf("Stop", "QorFinder", "Reading"), labels(status(running(TrackTarget.Joy))))
    }

    @Test
    fun `when every target is met, tracks keep their order`() {
        assertEquals(listOf("QorFinder", "Joy", "NeetCode"), labels(status(totals = totals(project = 300, neetCode = 60, reading = 120))))
    }

    @Test
    fun `the Project button starts the project with the largest share this week`() {
        val action = quickActions(status()).first() as QuickAction.Switch
        assertEquals(TrackTarget.Project(1), action.target)
    }

    @Test
    fun `with no projects this week there is no Project button`() {
        assertEquals(listOf("Reading", "Joy", "NeetCode"), labels(status(projects = emptyList())))
    }
}
