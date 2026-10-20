package io.github.khoryz666.timer.core

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.khoryz666.timer.projects.WeekPlan
import io.github.khoryz666.timer.tracking.Pillar
import io.github.khoryz666.timer.tracking.TrackTarget
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

/** Opens a database written by 2.0 (schema 1) with the 2.1 app. */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `2_0 data survives - Engine becomes Project and active projects become the baseline plan`() = runTest {
        val file = context.getDatabasePath("migration-test.db").apply { parentFile?.mkdirs(); delete() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { v1 ->
            v1.execSQL("CREATE TABLE sessions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, pillar TEXT NOT NULL, projectId INTEGER, startedAt INTEGER NOT NULL, endedAt INTEGER)")
            v1.execSQL("CREATE INDEX index_sessions_endedAt ON sessions (endedAt)")
            v1.execSQL("CREATE TABLE projects (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, sharePercent INTEGER NOT NULL, archived INTEGER NOT NULL)")
            v1.execSQL("INSERT INTO projects VALUES (1, 'KEFOS', 60, 0), (2, 'QorFinder', 40, 0), (3, 'Old', 0, 1)")
            v1.execSQL("INSERT INTO sessions VALUES (1, 'ENGINE', 2, 1000, 2000), (2, 'JOY', NULL, 2000, 3000), (3, 'ENGINE', 3, 3000, NULL)")
            v1.version = 1
        }

        val db = Room.databaseBuilder(context, AppDatabase::class.java, file.absolutePath)
            .addMigrations(AppDatabase.Migration1To2)
            .build()
        try {
            val sessions = db.sessions().endingIn(0, Long.MAX_VALUE)
            assertEquals(listOf(Pillar.PROJECT, Pillar.JOY, Pillar.PROJECT), sessions.map { it.pillar })
            assertEquals(TrackTarget.Project(3), db.sessions().running()?.target)

            assertEquals(listOf("KEFOS", "QorFinder", "Old"), db.projects().observeAll().first().map { it.name })
            val plan = db.projects().week(LocalDate.of(2026, 10, 26))
            assertEquals(listOf("KEFOS" to 60, "QorFinder" to 40), plan.map { it.name to it.sharePercent })
            assertEquals(plan, db.projects().week(WeekPlan.BASELINE))

            // New projects still get fresh ids after the table rebuild.
            db.projects().add(LocalDate.of(2026, 10, 26), "New")
            assertEquals(4L, db.projects().observeAll().first().last().id)
        } finally {
            db.close()
        }
    }
}
