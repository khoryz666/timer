package io.github.khoryz666.timer.core

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.khoryz666.timer.projects.Project
import io.github.khoryz666.timer.projects.ProjectDao
import io.github.khoryz666.timer.tracking.Session
import io.github.khoryz666.timer.tracking.SessionDao

@Database(entities = [Session::class, Project::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessions(): SessionDao
    abstract fun projects(): ProjectDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "timer.db")
                .addCallback(SeedProjects)
                .build()
    }

    /** The starting project list (60/40 split); editable in Settings. */
    object SeedProjects : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            db.execSQL("INSERT INTO projects (name, sharePercent, archived) VALUES ('KEFOS', 60, 0), ('QorFinder', 40, 0)")
        }
    }
}
