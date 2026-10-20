package io.github.khoryz666.timer.projects

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.khoryz666.timer.core.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class ProjectDaoTest {
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
        .addCallback(AppDatabase.SeedProjects)
        .build()
    private val dao = db.projects()
    private val lastWeek = LocalDate.of(2026, 10, 19)
    private val thisWeek = LocalDate.of(2026, 10, 26)
    private val nextWeek = LocalDate.of(2026, 11, 2)

    @After
    fun closeDb() = db.close()

    private suspend fun plan(week: LocalDate) = dao.week(week).map { "${it.name} ${it.sharePercent}" }

    @Test
    fun `every week starts from the seeded plan`() = runTest {
        assertEquals(listOf("QorFinder 80", "KEFOS 20"), plan(thisWeek))
    }

    @Test
    fun `editing this week leaves earlier weeks alone and carries into later weeks`() = runTest {
        dao.add(thisWeek, "Rust CLI")
        dao.setShare(thisWeek, projectId = 2, percent = 10)
        dao.remove(thisWeek, projectId = 1)

        assertEquals(listOf("QorFinder 80", "KEFOS 20"), plan(lastWeek))
        assertEquals(listOf("KEFOS 10", "Rust CLI 0"), plan(thisWeek))
        assertEquals(listOf("KEFOS 10", "Rust CLI 0"), plan(nextWeek))
    }

    @Test
    fun `a later week's edits don't reach back`() = runTest {
        dao.setShare(thisWeek, projectId = 1, percent = 70)
        dao.setShare(nextWeek, projectId = 1, percent = 50)

        assertEquals(listOf("QorFinder 70", "KEFOS 20"), plan(thisWeek))
        assertEquals(listOf("QorFinder 50", "KEFOS 20"), plan(nextWeek))
    }

    @Test
    fun `the last project of a week can't be removed`() = runTest {
        dao.remove(thisWeek, 1)
        dao.remove(thisWeek, 2)
        assertEquals(listOf("KEFOS 20"), plan(thisWeek))
    }

    @Test
    fun `renaming applies everywhere, and removed projects keep their name`() = runTest {
        dao.remove(thisWeek, 2)
        dao.rename(1, "QorFinder v2")

        assertEquals(listOf("QorFinder v2 80", "KEFOS 20"), plan(lastWeek))
        assertEquals(listOf("QorFinder v2", "KEFOS"), dao.observeAll().first().map { it.name })
    }
}
