package io.github.khoryz666.timer.tracking

import io.github.khoryz666.timer.projects.Project
import org.junit.Assert.assertEquals
import org.junit.Test

class QuickActionsTest {
    private val projects = listOf(Project(1, "KEFOS", 60), Project(2, "QorFinder", 40))

    private fun labels(running: TrackTarget?, list: List<Project> = projects) = quickActions(running, list).map { it.label }

    @Test
    fun `idle offers projects then Joy`() {
        assertEquals(listOf("KEFOS", "QorFinder", "Joy"), labels(null))
    }

    @Test
    fun `during Engine offers Stop, Joy, then the other project`() {
        assertEquals(listOf("Stop", "Joy", "QorFinder"), labels(TrackTarget.Engine(1)))
    }

    @Test
    fun `during Joy offers Stop then projects`() {
        assertEquals(listOf("Stop", "KEFOS", "QorFinder"), labels(TrackTarget.Joy))
    }

    @Test
    fun `never more than three actions`() {
        val many = projects + Project(3, "Third", 0)
        assertEquals(listOf("KEFOS", "QorFinder", "Third"), labels(null, many))
        assertEquals(listOf("Stop", "Joy", "QorFinder"), labels(TrackTarget.Engine(1), many))
    }

    @Test
    fun `switch actions carry their target`() {
        assertEquals(
            QuickAction.Switch(TrackTarget.Engine(2), "QorFinder"),
            quickActions(TrackTarget.Joy, projects)[2],
        )
    }
}
