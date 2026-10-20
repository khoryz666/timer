package io.github.khoryz666.timer.day

import io.github.khoryz666.timer.core.AppClock
import io.github.khoryz666.timer.settings.SettingsStore
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** The once-a-day "what kind of day is today?" notification. */
data class DayPromptSettings(val enabled: Boolean = true, val minuteOfDay: Int = 7 * 60) {
    val time: LocalTime get() = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)

    companion object {
        const val STEP_MINUTES = 30
        val MINUTE_OF_DAY = 0..(24 * 60 - STEP_MINUTES)
    }
}

/** At most once a day, from the set time on, and only until today's day type is chosen. */
fun shouldPromptDayType(
    settings: DayPromptSettings,
    today: LocalDate,
    timeNow: LocalTime,
    chosenToday: Boolean,
    lastPrompted: LocalDate?,
): Boolean = settings.enabled && !chosenToday && lastPrompted != today && !timeNow.isBefore(settings.time)

fun interface DayPromptPoster {
    /** Asks for today's day type; [inForce] is what today uses until one is picked. */
    fun postDayPrompt(inForce: DayType)
}

class DayPrompter(
    private val dayTypes: DayTypeDao,
    private val settings: SettingsStore,
    private val clock: AppClock,
    private val poster: DayPromptPoster,
) {
    /** Posts today's prompt if it is due. Cheap enough to call every minute. */
    suspend fun postIfDue() {
        val now = clock.now()
        val today = clock.dateOf(now)
        val choice = dayTypes.inForce(today)
        val due = shouldPromptDayType(
            settings = settings.dayPrompt.first(),
            today = today,
            timeNow = Instant.ofEpochMilli(now).atZone(clock.zone()).toLocalTime(),
            chosenToday = choice?.date == today,
            lastPrompted = settings.lastDayPrompt(),
        )
        if (due) {
            settings.setLastDayPrompt(today)
            poster.postDayPrompt(choice?.type ?: DayType.DEFAULT)
        }
    }
}
