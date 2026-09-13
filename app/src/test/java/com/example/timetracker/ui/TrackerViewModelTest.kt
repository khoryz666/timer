package com.example.timetracker.ui

import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.timetracker.MainDispatcherRule
import com.example.timetracker.data.ActiveState
import com.example.timetracker.data.LocalDatabase
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
class TrackerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var viewModel: TrackerViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val db = Room.inMemoryDatabaseBuilder(context, LocalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { tempFolder.newFile("test_prefs.preferences_pb") }
        )
        val repository = TrackerRepository(TrackerPreferences(dataStore), db.timeRecordDao())
        viewModel = TrackerViewModel(context, repository)
    }

    @Test
    fun `initial state is idle`() = runBlocking {
        val state = withTimeout(5_000) { viewModel.uiState.first() }
        assertEquals(ActiveState.IDLE, state.activeState)
    }

    @Test
    fun `pressing a button is reflected in uiState`() = runBlocking {
        viewModel.onButtonPress(ActiveState.WORK)

        val state = withTimeout(5_000) {
            viewModel.uiState.first { it.activeState == ActiveState.WORK }
        }
        assertEquals(ActiveState.WORK, state.activeState)
    }

    @Test
    fun `resetActiveTimer zeroes the active category in uiState`() = runBlocking {
        viewModel.onButtonPress(ActiveState.WORK)
        withTimeout(5_000) { viewModel.uiState.first { it.activeState == ActiveState.WORK } }

        viewModel.resetActiveTimer()

        val state = withTimeout(5_000) {
            viewModel.uiState.first { it.activeState == ActiveState.WORK && it.workDurationMs == 0L }
        }
        assertEquals(0L, state.workDurationMs)
    }
}
