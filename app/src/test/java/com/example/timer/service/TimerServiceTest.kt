package com.example.timer.service

import android.app.Application
import android.app.NotificationManager
import android.content.Intent
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.timer.MainDispatcherRule
import com.example.timer.data.ActiveState
import com.example.timer.data.LocalDatabase
import com.example.timer.data.TrackerPreferences
import com.example.timer.data.TrackerRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class TimerServiceTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var prefs: TrackerPreferences

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val db = Room.inMemoryDatabaseBuilder(context, LocalDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { tempFolder.newFile("test_prefs.preferences_pb") }
        )
        prefs = TrackerPreferences(dataStore)
        val repository = TrackerRepository(prefs, db.timeRecordDao())
        TimerService.repositoryFactory = { repository }
    }

    @After
    fun tearDown() {
        TimerService.repositoryFactory = { TrackerRepository(it) }
    }

    @Test
    fun `ACTION_START posts a foreground notification immediately`() {
        val service = Robolectric.buildService(TimerService::class.java).create().get()

        service.onStartCommand(Intent().apply { action = TimerService.ACTION_START }, 0, 1)

        val notificationManager = ApplicationProvider.getApplicationContext<Application>()
            .getSystemService(NotificationManager::class.java)
        assertTrue(shadowOf(notificationManager).activeNotifications.isNotEmpty())
    }

    @Test
    fun `ACTION_WORK switches the active state`() = runBlocking {
        val service = Robolectric.buildService(TimerService::class.java).create().get()

        service.onStartCommand(Intent().apply { action = TimerService.ACTION_WORK }, 0, 1)

        val state = withTimeout(10_000) { prefs.activeStateFlow.first { it == ActiveState.WORK } }
        assertEquals(ActiveState.WORK, state)
    }
}
