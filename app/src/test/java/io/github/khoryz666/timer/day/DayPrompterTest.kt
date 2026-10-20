package io.github.khoryz666.timer.day

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.khoryz666.timer.core.AppDatabase
import io.github.khoryz666.timer.core.FixedClock
import io.github.khoryz666.timer.settings.SettingsStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
class DayPrompterTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val clock = FixedClock(LocalDateTime.of(2026, 10, 26, 6, 0))
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
    private val posted = mutableListOf<DayType>()

    @After
    fun closeDb() = db.close()

    @Test
    fun `posts once from 07 00, saying what today uses until a type is picked`() = runTest {
        val settings = SettingsStore(PreferenceDataStoreFactory.create(scope = backgroundScope) { tmp.newFile("s.preferences_pb") })
        val prompter = DayPrompter(db.dayTypes(), settings, clock) { posted += it }
        db.dayTypes().set(DayTypeChoice(LocalDate.of(2026, 10, 23), DayType.CLASS))

        prompter.postIfDue()
        assertEquals(emptyList<DayType>(), posted) // too early

        clock.set(LocalDateTime.of(2026, 10, 26, 7, 1))
        prompter.postIfDue()
        prompter.postIfDue()
        assertEquals(listOf(DayType.CLASS), posted)

        clock.set(LocalDateTime.of(2026, 10, 27, 8, 0))
        db.dayTypes().set(DayTypeChoice(LocalDate.of(2026, 10, 27), DayType.DEEP))
        prompter.postIfDue()
        assertEquals(listOf(DayType.CLASS), posted) // already chosen today
    }

    @Test
    fun `the week window includes the choice in force when the week starts`() = runTest {
        val dao = db.dayTypes()
        val monday = LocalDate.of(2026, 10, 26)
        dao.set(DayTypeChoice(monday.minusDays(10), DayType.LIGHT))
        dao.set(DayTypeChoice(monday.minusDays(3), DayType.CLASS))
        dao.set(DayTypeChoice(monday.plusDays(1), DayType.DEEP))
        dao.set(DayTypeChoice(monday.plusDays(7), DayType.LIGHT))

        val window = dao.observeInForce(monday, monday.plusDays(7)).first()

        assertEquals(listOf(monday.minusDays(3), monday.plusDays(1)), window.map { it.date })
        assertEquals(DayType.CLASS, window.inForceOn(monday))
    }
}
