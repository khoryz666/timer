package io.github.khoryz666.timer.today

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.khoryz666.timer.core.ui.TimerTheme
import io.github.khoryz666.timer.projects.Project
import io.github.khoryz666.timer.quota.DayTotals
import io.github.khoryz666.timer.quota.QuotaSettings
import io.github.khoryz666.timer.tracking.Pillar
import io.github.khoryz666.timer.tracking.Session
import io.github.khoryz666.timer.tracking.TodayStatus
import io.github.khoryz666.timer.tracking.TrackTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class TodayScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val minute = 60_000L
    private val projects = listOf(Project(1, "KEFOS", 60), Project(2, "QorFinder", 40))
    private val switched = mutableListOf<TrackTarget>()
    private var stopped = false

    private fun show(running: Session?, totals: DayTotals) {
        val status = TodayStatus.of(LocalDate.of(2026, 10, 14), 200 * minute, running, projects, totals, QuotaSettings())
        compose.setContent {
            TimerTheme {
                TodayScreen(status, onSwitch = { switched += it }, onStop = { stopped = true })
            }
        }
    }

    @Test
    fun `shows engine blocks, floor state and joy progress`() {
        show(null, DayTotals(engineMs = 200 * minute, joyMs = 70 * minute, engineByProject = mapOf(1L to 200 * minute)))

        compose.onNodeWithText("NOT TRACKING").assertExists()
        compose.onNodeWithText("/ 4 blocks", substring = true).assertExists()
        compose.onNodeWithText("Floor met").assertExists()
        compose.onNodeWithText("3h 20m of 6h").assertExists()
        compose.onNodeWithText("1h 10m").assertExists()
        compose.onNodeWithText("Below target").assertExists()
    }

    @Test
    fun `tapping a project or Joy switches to it`() {
        show(null, DayTotals())

        compose.onNodeWithText("QorFinder").performClick()
        compose.onNodeWithText("Start Joy").performScrollTo().performClick()

        assertEquals(listOf(TrackTarget.Engine(2), TrackTarget.Joy), switched)
    }

    @Test
    fun `while running, shows the session and can stop it`() {
        val running = Session(pillar = Pillar.ENGINE, projectId = 1, startedAt = 125 * minute)
        show(running, DayTotals(engineMs = 75 * minute, engineByProject = mapOf(1L to 75 * minute)))

        compose.onNodeWithText("ENGINE · KEFOS").assertExists()
        compose.onNodeWithText("1:15:00").assertExists()
        compose.onNodeWithText("KEFOS").assertIsNotEnabled()
        compose.onNodeWithText("Stop").performClick()

        assertTrue(stopped)
    }
}
