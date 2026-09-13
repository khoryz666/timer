package com.example.timer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: TimeRecord)

    @Query("SELECT * FROM daily_records WHERE date = :date")
    suspend fun getRecordByDate(date: String): TimeRecord?

    @Query("SELECT * FROM daily_records ORDER BY date DESC")
    fun getAllRecordsDescending(): Flow<List<TimeRecord>>
}
