package io.github.khoryz666.timer.day

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class DayTypeTest {
    private val oct26 = LocalDate.of(2026, 10, 26)

    @Test
    fun `the slider runs Light, Class, Deep`() {
        assertEquals(listOf("Light", "Class", "Deep"), DayType.entries.map { it.label })
    }

    @Test
    fun `a day keeps its own choice, else the latest earlier one, else the default`() {
        val choices = listOf(DayTypeChoice(oct26, DayType.CLASS), DayTypeChoice(oct26.plusDays(1), DayType.DEEP))

        assertEquals(DayType.DEFAULT, choices.inForceOn(oct26.minusDays(1)))
        assertEquals(DayType.CLASS, choices.inForceOn(oct26))
        assertEquals(DayType.DEEP, choices.inForceOn(oct26.plusDays(1)))
        assertEquals(DayType.DEEP, choices.inForceOn(oct26.plusDays(5)))
    }

    private fun due(time: LocalTime, chosenToday: Boolean = false, lastPrompted: LocalDate? = null, settings: DayPromptSettings = DayPromptSettings()) =
        shouldPromptDayType(settings, oct26, time, chosenToday, lastPrompted)

    @Test
    fun `prompts once a day from the set time until the day type is chosen`() {
        assertFalse(due(LocalTime.of(6, 59)))
        assertTrue(due(LocalTime.of(7, 0)))
        assertTrue(due(LocalTime.of(22, 0), lastPrompted = oct26.minusDays(1)))
        assertFalse(due(LocalTime.of(9, 0), lastPrompted = oct26)) // already asked (and maybe dismissed)
        assertFalse(due(LocalTime.of(9, 0), chosenToday = true))
        assertFalse(due(LocalTime.of(9, 0), settings = DayPromptSettings(enabled = false)))
        assertTrue(due(LocalTime.of(5, 30), settings = DayPromptSettings(minuteOfDay = 5 * 60 + 30)))
    }
}
