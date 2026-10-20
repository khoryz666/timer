package io.github.khoryz666.timer.today

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import io.github.khoryz666.timer.core.ui.TimerTheme
import io.github.khoryz666.timer.day.DayType
import io.github.khoryz666.timer.tracking.StatusFixtures
import io.github.khoryz666.timer.tracking.StatusFixtures.MINUTE
import io.github.khoryz666.timer.tracking.StatusFixtures.totals
import io.github.khoryz666.timer.tracking.TodayStatus
import io.github.khoryz666.timer.tracking.TrackTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TodayScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val switched = mutableListOf<TrackTarget>()
    private val dayTypes = mutableListOf<DayType>()
    private var stopped = false

    private fun show(status: TodayStatus) {
        compose.setContent {
            TimerTheme {
                TodayScreen(status, onSwitch = { switched += it }, onStop = { stopped = true }, onDayType = { dayTypes += it })
            }
        }
    }

    @Test
    fun `shows the day type, each track's blocks and joy progress`() {
        show(StatusFixtures.status(totals = totals(project = 200, neetCode = 60, joy = 70), now = 200 * MINUTE))

        compose.onNodeWithText("Deep day").assertExists()
        compose.onNodeWithText("Targets: Project 5 · NeetCode 1 · Reading 2 blocks").assertExists()
        compose.onNodeWithText("NOT TRACKING").assertExists()
        compose.onNodeWithText("/ 5 blocks", substring = true).assertExists()
        compose.onNodeWithText("Floor met").assertExists()
        compose.onNodeWithText("3h 20m of 5h").assertExists()
        compose.onNodeWithText("Target met").performScrollTo().assertExists() // NeetCode 1/1
        compose.onNodeWithText("2h to go").performScrollTo().assertExists() //   Reading 0/2
        compose.onNodeWithText("1h 10m").performScrollTo().assertExists()
    }

    @Test
    fun `the slider sets the day type, and an unpicked day can keep the last one`() {
        show(StatusFixtures.status(chosen = false))

        compose.onNodeWithText("Not picked yet, so today uses your last choice.").assertExists()
        compose.onNodeWithText("Keep Deep").performClick()
        compose.onNodeWithContentDescription("Day type").performSemanticsAction(SemanticsActions.SetProgress) { it(0f) }

        assertEquals(listOf(DayType.DEEP, DayType.LIGHT), dayTypes)
    }

    @Test
    fun `tapping a project, a track or Joy switches to it`() {
        show(StatusFixtures.status())

        compose.onNodeWithText("KEFOS").performScrollTo().performClick()
        compose.onNodeWithText("Start NeetCode").performScrollTo().performClick()
        compose.onNodeWithText("Start Reading").performScrollTo().performClick()
        compose.onNodeWithText("Start Joy").performScrollTo().performClick()

        assertEquals(listOf(TrackTarget.Project(2), TrackTarget.NeetCode, TrackTarget.Reading, TrackTarget.Joy), switched)
    }

    @Test
    fun `while running, shows the session and can stop it`() {
        val running = StatusFixtures.running(TrackTarget.Project(1), startedAt = 125 * MINUTE)
        show(StatusFixtures.status(running, totals(project = 75), now = 200 * MINUTE))

        compose.onNodeWithText("PROJECT · QORFINDER").assertExists()
        compose.onNodeWithText("1:15:00").assertExists()
        compose.onNodeWithText("QorFinder").assertIsNotEnabled()
        compose.onNodeWithText("Stop").performClick()

        assertTrue(stopped)
    }
}
