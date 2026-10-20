package io.github.khoryz666.timer.projects

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/** A project's identity. Rows are never deleted, so past sessions keep their name. */
@Entity(tableName = "projects")
data class ProjectRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

/**
 * A project in one week's plan. [sharePercent] is its target share of that week's Project time;
 * shares are normalised against the week's other projects, so they don't have to add up to 100.
 */
@Entity(tableName = "week_projects", primaryKeys = ["weekStart", "projectId"])
data class WeekProject(val weekStart: LocalDate, val projectId: Long, val sharePercent: Int)

/** A project as planned for a week - what the screens work with. */
data class Project(val id: Long, val name: String, val sharePercent: Int)

object WeekPlan {
    /**
     * The week key of the plan from before plans were per week (and of a fresh install's
     * starting plan). Every week without a plan of its own inherits from it.
     */
    val BASELINE: LocalDate = LocalDate.ofEpochDay(0)
}
