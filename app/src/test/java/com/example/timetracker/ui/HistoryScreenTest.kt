package com.example.timetracker.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.timetracker.data.TimeRecord
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HistoryScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `renders a card per record with its formatted durations`() {
        composeRule.setContent {
            HistoryScreen(
                records = listOf(
                    TimeRecord(date = "2026-01-01", workDurationMs = 3_600_000L)
                )
            )
        }

        composeRule.onNodeWithText("2026-01-01").assertExists()
        composeRule.onNodeWithText("Work: 01 h 00 m 00 s").assertExists()
    }
}
