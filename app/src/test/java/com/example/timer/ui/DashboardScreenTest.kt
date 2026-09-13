package com.example.timer.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.timer.data.ActiveState
import com.example.timer.data.TrackerSnapshot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DashboardScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `shows accumulated duration for each category`() {
        composeRule.setContent {
            DashboardScreen(
                uiState = TrackerSnapshot(
                    activeState = ActiveState.IDLE,
                    workDurationMs = 65_000L
                ),
                onStateChange = {},
                onResetCurrent = {},
                onForceSave = {}
            )
        }

        composeRule.onNodeWithText("00 h 01 m 05 s").assertExists()
    }

    @Test
    fun `pressing a category button reports the press`() {
        var pressed: ActiveState? = null

        composeRule.setContent {
            DashboardScreen(
                uiState = TrackerSnapshot(),
                onStateChange = { pressed = it },
                onResetCurrent = {},
                onForceSave = {}
            )
        }

        composeRule.onNodeWithText("Work").performClick()

        assert(pressed == ActiveState.WORK) { "expected ActiveState.WORK, got $pressed" }
    }
}
