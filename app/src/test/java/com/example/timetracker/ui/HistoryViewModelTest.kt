package com.example.timetracker.ui

import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.timetracker.MainDispatcherRule
import com.example.timetracker.data.LocalDatabase
import com.example.timetracker.data.TimeRecord
import com.example.timetracker.data.TimeRecordDao
import com.example.timetracker.data.TrackerPreferences
import com.example.timetracker.data.TrackerRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var viewModel: HistoryViewModel
    private lateinit var dao: TimeRecordDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val db = Room.inMemoryDatabaseBuilder(context, LocalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.timeRecordDao()
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { tempFolder.newFile("test_prefs.preferences_pb") }
        )
        val repository = TrackerRepository(TrackerPreferences(dataStore), dao)
        viewModel = HistoryViewModel(context, repository)
    }

    @Test
    fun `records is empty when nothing has been tracked`() = runBlocking {
        val records = withTimeout(10_000) { viewModel.records.first() }
        assertEquals(emptyList<TimeRecord>(), records)
    }

    @Test
    fun `records reflects what is stored in the database`() = runBlocking {
        dao.insertOrUpdate(TimeRecord(date = "2026-01-01", workDurationMs = 1_000L))

        val records = withTimeout(10_000) { viewModel.records.first { it.isNotEmpty() } }

        assertEquals(1, records.size)
        assertEquals("2026-01-01", records.first().date)
    }
}
