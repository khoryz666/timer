package io.github.khoryz666.timer.projects

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * An Engine project. [sharePercent] is its target share of weekly Engine time; shares are
 * normalised against the other active projects, so they don't strictly have to add up to 100.
 * Removing a project archives it, so past sessions keep their name.
 */
@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sharePercent: Int,
    val archived: Boolean = false,
)
