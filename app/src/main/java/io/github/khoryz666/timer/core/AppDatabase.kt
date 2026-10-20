package io.github.khoryz666.timer.core

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.khoryz666.timer.day.DayTypeChoice
import io.github.khoryz666.timer.day.DayTypeDao
import io.github.khoryz666.timer.projects.ProjectDao
import io.github.khoryz666.timer.projects.ProjectRecord
import io.github.khoryz666.timer.projects.WeekProject
import io.github.khoryz666.timer.tracking.Session
import io.github.khoryz666.timer.tracking.SessionDao
import java.time.LocalDate

@Database(entities = [Session::class, ProjectRecord::class, WeekProject::class, DayTypeChoice::class], version = 2)
@TypeConverters(AppDatabase.Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessions(): SessionDao
    abstract fun projects(): ProjectDao
    abstract fun dayTypes(): DayTypeDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "timer.db")
                .addMigrations(Migration1To2)
                .addCallback(SeedProjects)
                .build()
    }

    /** Dates are stored as epoch days. */
    class Converters {
        @TypeConverter
        fun toEpochDay(date: LocalDate): Long = date.toEpochDay()

        @TypeConverter
        fun fromEpochDay(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)
    }

    /** The starting project list, as the plan every week inherits; editable on the Week screen. */
    object SeedProjects : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            db.execSQL("INSERT INTO projects (id, name) VALUES (1, 'QorFinder'), (2, 'KEFOS')")
            db.execSQL("INSERT INTO week_projects (weekStart, projectId, sharePercent) VALUES (0, 1, 80), (0, 2, 20)")
        }
    }

    /**
     * 2.0 -> 2.1: Engine becomes the Project track, the project list moves into per-week plans
     * (2.0's active projects and shares become the baseline plan), and day types are added.
     */
    object Migration1To2 : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("UPDATE sessions SET pillar = 'PROJECT' WHERE pillar = 'ENGINE'")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `week_projects` (`weekStart` INTEGER NOT NULL, `projectId` INTEGER NOT NULL, " +
                    "`sharePercent` INTEGER NOT NULL, PRIMARY KEY(`weekStart`, `projectId`))"
            )
            db.execSQL("INSERT INTO week_projects (weekStart, projectId, sharePercent) SELECT 0, id, sharePercent FROM projects WHERE archived = 0")
            db.execSQL("CREATE TABLE IF NOT EXISTS `projects_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)")
            db.execSQL("INSERT INTO projects_new (id, name) SELECT id, name FROM projects")
            db.execSQL("DROP TABLE projects")
            db.execSQL("ALTER TABLE projects_new RENAME TO projects")
            db.execSQL("CREATE TABLE IF NOT EXISTS `day_types` (`date` INTEGER NOT NULL, `type` TEXT NOT NULL, PRIMARY KEY(`date`))")
        }
    }
}
