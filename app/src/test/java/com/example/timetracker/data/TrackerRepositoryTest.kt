package com.example.timetracker.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class TrackerRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var repository: TrackerRepository
    private lateinit var prefs: TrackerPreferences
    private lateinit var dao: TimeRecordDao

    private fun todayDateString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, LocalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.timeRecordDao()
        // A fresh DataStore file per test - the Context.dataStore extension used in
        // production is a process-wide singleton, which would leak state between tests.
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { tempFolder.newFile("test_prefs.preferences_pb") }
        )
        prefs = TrackerPreferences(dataStore)
        repository = TrackerRepository(prefs, dao)
    }

    @Test
    fun `switching away from a running category saves its elapsed time`() = runTest {
        prefs.setActiveState(ActiveState.WORK, System.currentTimeMillis() - 5_000L)

        repository.switchState(ActiveState.SELF)

        val record = dao.getRecordByDate(todayDateString())
        assertEquals(ActiveState.SELF, prefs.activeStateFlow.first())
        assertTrue("expected work duration to be saved, got $record", (record?.workDurationMs ?: 0L) >= 5_000L)
    }

    @Test
    fun `switching away from idle saves nothing`() = runTest {
        repository.switchState(ActiveState.WORK)

        val record = dao.getRecordByDate(todayDateString())
        assertNull(record)
    }

    @Test
    fun `elapsed time crossing midnight is attributed to the end date, not the start date`() = runTest {
        val twentyFiveHoursAgo = System.currentTimeMillis() - 25L * 60 * 60 * 1000
        prefs.setActiveState(ActiveState.SLEEP, twentyFiveHoursAgo)

        repository.switchState(ActiveState.IDLE)

        val todayRecord = dao.getRecordByDate(todayDateString())
        assertTrue(
            "expected the multi-day session to be saved under today's date, got $todayRecord",
            (todayRecord?.sleepDurationMs ?: 0L) > 0L
        )
    }

    @Test
    fun `resetActiveTimer zeroes only the active category`() = runTest {
        val dateStr = todayDateString()
        dao.insertOrUpdate(
            TimeRecord(date = dateStr, workDurationMs = 10_000L, selfDurationMs = 20_000L)
        )
        prefs.setActiveState(ActiveState.WORK, System.currentTimeMillis())

        repository.resetActiveTimer()

        val record = dao.getRecordByDate(dateStr)
        assertEquals(0L, record?.workDurationMs)
        assertEquals(20_000L, record?.selfDurationMs)
    }

    @Test
    fun `forceSaveCurrentProgress saves elapsed time and restarts the timer from now`() = runTest {
        val start = System.currentTimeMillis() - 3_000L
        prefs.setActiveState(ActiveState.WORK, start)

        repository.forceSaveCurrentProgress()

        val record = dao.getRecordByDate(todayDateString())
        assertTrue((record?.workDurationMs ?: 0L) >= 3_000L)
        assertEquals(ActiveState.WORK, prefs.activeStateFlow.first())
        assertTrue(prefs.startTimeFlow.first() > start)
    }

    @Test
    fun `snapshotFlow reflects idle state with zero ticking`() = runTest {
        val snapshot = repository.snapshotFlow().first()

        assertEquals(ActiveState.IDLE, snapshot.activeState)
        assertEquals(0L, snapshot.activelyTickingMs)
    }

    @Test
    fun `snapshotFlow ticks while a category is active`() = runTest {
        prefs.setActiveState(ActiveState.WORK, System.currentTimeMillis() - 500L)

        val snapshots = repository.snapshotFlow(tickIntervalMs = 10L).take(2).toList()

        assertEquals(2, snapshots.size)
        snapshots.forEach {
            assertEquals(ActiveState.WORK, it.activeState)
            assertTrue(it.activelyTickingMs >= 500L)
        }
    }

    @Test
    fun `snapshotFlow separates stored base duration from the actively ticking category`() = runTest {
        val dateStr = todayDateString()
        dao.insertOrUpdate(TimeRecord(date = dateStr, workDurationMs = 60_000L, selfDurationMs = 30_000L))
        prefs.setActiveState(ActiveState.WORK, System.currentTimeMillis() - 1_000L)

        val snapshot = repository.snapshotFlow().first()

        assertEquals(60_000L, snapshot.workDurationMs)
        assertEquals(30_000L, snapshot.selfDurationMs)
        assertTrue(snapshot.activelyTickingMs >= 1_000L)
    }
}
