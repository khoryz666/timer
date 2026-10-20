package io.github.khoryz666.timer.day

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/** How demanding a day is; it picks the day's targets. In slider order, lightest first. */
enum class DayType(val label: String) {
    LIGHT("Light"),
    CLASS("Class"),
    DEEP("Deep");

    companion object {
        /** Used before any day type has ever been chosen. */
        val DEFAULT = CLASS
    }
}

/** The day type chosen for [date]. A day without a choice keeps the latest earlier one. */
@Entity(tableName = "day_types")
data class DayTypeChoice(@PrimaryKey val date: LocalDate, val type: DayType)

/**
 * The day type in force on [date]: its own choice, else the latest one before it, else
 * [DayType.DEFAULT]. Expects choices sorted by date.
 */
fun List<DayTypeChoice>.inForceOn(date: LocalDate): DayType =
    lastOrNull { it.date <= date }?.type ?: DayType.DEFAULT
