package com.example.timer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tracker_prefs")

class TrackerPreferences(private val dataStore: DataStore<Preferences>) {

    constructor(context: Context) : this(context.dataStore)

    companion object {
        val ACTIVE_STATE_KEY = stringPreferencesKey("active_state")
        val START_TIME_KEY = longPreferencesKey("start_time")
    }

    val activeStateFlow: Flow<ActiveState> = dataStore.data.map { preferences ->
        val stateString = preferences[ACTIVE_STATE_KEY] ?: ActiveState.IDLE.name
        try {
            ActiveState.valueOf(stateString)
        } catch (e: Exception) {
            ActiveState.IDLE
        }
    }

    val startTimeFlow: Flow<Long> = dataStore.data.map { preferences ->
        preferences[START_TIME_KEY] ?: 0L
    }

    suspend fun setActiveState(state: ActiveState, startTime: Long) {
        dataStore.edit { preferences ->
            preferences[ACTIVE_STATE_KEY] = state.name
            preferences[START_TIME_KEY] = startTime
        }
    }
}
