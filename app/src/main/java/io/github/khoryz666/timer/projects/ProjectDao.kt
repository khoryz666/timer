package io.github.khoryz666.timer.projects

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Each week has its own project list. A week with no list of its own uses the latest earlier
 * week's, so a new week starts with last week's projects; the first edit copies that list into
 * the week, so editing never changes an earlier week.
 */
@Dao
abstract class ProjectDao {
    /** Every project ever added, for naming past sessions. */
    @Query("SELECT * FROM projects ORDER BY id")
    abstract fun observeAll(): Flow<List<ProjectRecord>>

    @Query(WEEK_PLAN)
    abstract fun observeWeek(weekStart: LocalDate): Flow<List<Project>>

    @Query(WEEK_PLAN)
    abstract suspend fun week(weekStart: LocalDate): List<Project>

    @Transaction
    open suspend fun add(weekStart: LocalDate, name: String) {
        ownPlan(weekStart)
        val id = insert(ProjectRecord(name = name))
        insert(WeekProject(weekStart, id, sharePercent = 0))
    }

    @Transaction
    open suspend fun setShare(weekStart: LocalDate, projectId: Long, percent: Int) {
        ownPlan(weekStart)
        updateShare(weekStart, projectId, percent.coerceIn(0, 100))
    }

    /** Takes the project out of this week's plan; its past time and name stay. The last one stays. */
    @Transaction
    open suspend fun remove(weekStart: LocalDate, projectId: Long) {
        ownPlan(weekStart)
        if (countPlanned(weekStart) > 1) deletePlanned(weekStart, projectId)
    }

    /** Renames the project everywhere, past weeks included - it is still the same project. */
    @Query("UPDATE projects SET name = :name WHERE id = :projectId")
    abstract suspend fun rename(projectId: Long, name: String)

    /** Gives [weekStart] a plan of its own, copied from the one it inherits. */
    private suspend fun ownPlan(weekStart: LocalDate) {
        if (countPlanned(weekStart) > 0) return
        week(weekStart).forEach { insert(WeekProject(weekStart, it.id, it.sharePercent)) }
    }

    @Insert
    protected abstract suspend fun insert(project: ProjectRecord): Long

    @Insert
    protected abstract suspend fun insert(planned: WeekProject)

    @Query("SELECT COUNT(*) FROM week_projects WHERE weekStart = :weekStart")
    protected abstract suspend fun countPlanned(weekStart: LocalDate): Int

    @Query("UPDATE week_projects SET sharePercent = :percent WHERE weekStart = :weekStart AND projectId = :projectId")
    protected abstract suspend fun updateShare(weekStart: LocalDate, projectId: Long, percent: Int)

    @Query("DELETE FROM week_projects WHERE weekStart = :weekStart AND projectId = :projectId")
    protected abstract suspend fun deletePlanned(weekStart: LocalDate, projectId: Long)

    private companion object {
        /** The plan of the latest week on or before :weekStart, in the order projects were added. */
        const val WEEK_PLAN = """
            SELECT p.id, p.name, w.sharePercent FROM week_projects w JOIN projects p ON p.id = w.projectId
            WHERE w.weekStart = (SELECT MAX(weekStart) FROM week_projects WHERE weekStart <= :weekStart)
            ORDER BY p.id
        """
    }
}
