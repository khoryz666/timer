package com.example.timetracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tracker_prefs")

class TrackerPreferences(private val context: Context) {

    companion object {
        val ACTIVE_STATE_KEY = stringPreferencesKey("active_state")
        val START_TIME_KEY = longPreferencesKey("start_time")
        val SHOW_NOTIFICATION_KEY = stringPreferencesKey("show_notification") // "true" or "false"
        val ENABLE_SERVICE_KEY = stringPreferencesKey("enable_service") // "true" or "false"
    }

    val activeStateFlow: Flow<ActiveState> = context.dataStore.data.map { preferences ->
        val stateString = preferences[ACTIVE_STATE_KEY] ?: ActiveState.IDLE.name
        try {
            ActiveState.valueOf(stateString)
        } catch (e: Exception) {
            ActiveState.IDLE
        }
    }

    val startTimeFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[START_TIME_KEY] ?: 0L
    }

    val showNotificationFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        (preferences[SHOW_NOTIFICATION_KEY] ?: "false").toBoolean()
    }

    val enableServiceFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        (preferences[ENABLE_SERVICE_KEY] ?: "false").toBoolean()
    }

    suspend fun setActiveState(state: ActiveState, startTime: Long) {
        context.dataStore.edit { preferences ->
            preferences[ACTIVE_STATE_KEY] = state.name
            preferences[START_TIME_KEY] = startTime
        }
    }

    suspend fun setShowNotification(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_NOTIFICATION_KEY] = show.toString()
        }
    }

    suspend fun setEnableService(enable: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ENABLE_SERVICE_KEY] = enable.toString()
        }
    }
}
