package io.github.khoryz666.timer.day

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DayTypeDao {
    /** The choice in force on [date]: its own, or the latest before it. */
    @Query("SELECT * FROM day_types WHERE date <= :date ORDER BY date DESC LIMIT 1")
    fun observeInForce(date: LocalDate): Flow<DayTypeChoice?>

    @Query("SELECT * FROM day_types WHERE date <= :date ORDER BY date DESC LIMIT 1")
    suspend fun inForce(date: LocalDate): DayTypeChoice?

    /** Every choice in force at some point in [from, until): those inside it plus the latest before it. */
    @Query(
        "SELECT * FROM day_types WHERE date < :until AND " +
            "date >= IFNULL((SELECT MAX(date) FROM day_types WHERE date <= :from), :from) ORDER BY date"
    )
    fun observeInForce(from: LocalDate, until: LocalDate): Flow<List<DayTypeChoice>>

    @Upsert
    suspend fun set(choice: DayTypeChoice)
}
