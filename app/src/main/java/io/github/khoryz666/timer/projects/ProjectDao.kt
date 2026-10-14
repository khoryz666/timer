package io.github.khoryz666.timer.projects

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    /** All projects, archived included, in the order they were added. */
    @Query("SELECT * FROM projects ORDER BY id")
    fun observeAll(): Flow<List<Project>>

    @Insert
    suspend fun insert(project: Project): Long

    @Update
    suspend fun update(project: Project)
}
