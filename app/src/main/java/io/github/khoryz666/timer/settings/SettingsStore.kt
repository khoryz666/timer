package io.github.khoryz666.timer.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import io.github.khoryz666.timer.day.DayPromptSettings
import io.github.khoryz666.timer.day.DayType
import io.github.khoryz666.timer.quota.DayTargets
import io.github.khoryz666.timer.quota.QuotaSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class SettingsStore(private val dataStore: DataStore<Preferences>) {

    val quota: Flow<QuotaSettings> = dataStore.data.map { it.toQuota() }.distinctUntilChanged()

    /** Gentle notice when Joy starts before the first focus block of the day. Off by default. */
    val nudgeEnabled: Flow<Boolean> = dataStore.data.map { it[NUDGE] ?: false }.distinctUntilChanged()

    val dayPrompt: Flow<DayPromptSettings> = dataStore.data.map {
        val defaults = DayPromptSettings()
        DayPromptSettings(
            enabled = it[DAY_PROMPT_ENABLED] ?: defaults.enabled,
            minuteOfDay = it[DAY_PROMPT_MINUTE] ?: defaults.minuteOfDay,
        )
    }.distinctUntilChanged()

    suspend fun updateQuota(transform: (QuotaSettings) -> QuotaSettings) {
        dataStore.edit { prefs ->
            val updated = transform(prefs.toQuota()).sanitized()
            prefs[BLOCK_MINUTES] = updated.blockMinutes
            prefs[JOY_TARGET_MINUTES] = updated.joyTargetMinutes
            DayType.entries.forEach { prefs.setTargets(it, updated.targets(it)) }
        }
    }

    suspend fun setNudgeEnabled(enabled: Boolean) {
        dataStore.edit { it[NUDGE] = enabled }
    }

    suspend fun isNudgeEnabled(): Boolean = nudgeEnabled.first()

    suspend fun setDayPrompt(settings: DayPromptSettings) {
        dataStore.edit {
            it[DAY_PROMPT_ENABLED] = settings.enabled
            it[DAY_PROMPT_MINUTE] = settings.minuteOfDay.coerceIn(DayPromptSettings.MINUTE_OF_DAY)
        }
    }

    /** The last day the day-type prompt was posted, so it is posted at most once a day. */
    suspend fun lastDayPrompt(): LocalDate? = dataStore.data.first()[LAST_DAY_PROMPT]?.let(LocalDate::ofEpochDay)

    suspend fun setLastDayPrompt(date: LocalDate) {
        dataStore.edit { it[LAST_DAY_PROMPT] = date.toEpochDay() }
    }

    private fun Preferences.toQuota(): QuotaSettings {
        val defaults = QuotaSettings()
        return QuotaSettings(
            blockMinutes = this[BLOCK_MINUTES] ?: defaults.blockMinutes,
            dayTargets = DayType.entries.associateWith { type ->
                val keys = TargetKeys.of(type)
                val fallback = defaults.targets(type)
                DayTargets(
                    projectFloor = this[keys.projectFloor] ?: fallback.projectFloor,
                    project = this[keys.project] ?: fallback.project,
                    neetCode = this[keys.neetCode] ?: fallback.neetCode,
                    reading = this[keys.reading] ?: fallback.reading,
                )
            },
            joyTargetMinutes = this[JOY_TARGET_MINUTES] ?: defaults.joyTargetMinutes,
        )
    }

    private fun MutablePreferences.setTargets(type: DayType, targets: DayTargets) {
        val keys = TargetKeys.of(type)
        this[keys.projectFloor] = targets.projectFloor
        this[keys.project] = targets.project
        this[keys.neetCode] = targets.neetCode
        this[keys.reading] = targets.reading
    }

    private class TargetKeys(prefix: String) {
        val projectFloor = intPreferencesKey("${prefix}_project_floor")
        val project = intPreferencesKey("${prefix}_project_target")
        val neetCode = intPreferencesKey("${prefix}_neetcode_target")
        val reading = intPreferencesKey("${prefix}_reading_target")

        companion object {
            private val byType = DayType.entries.associateWith { TargetKeys(it.name.lowercase()) }
            fun of(type: DayType) = byType.getValue(type)
        }
    }

    private companion object {
        // A new key: 2.0 stored 90-minute blocks under "block_minutes"; 2.1 starts again at 60.
        val BLOCK_MINUTES = intPreferencesKey("block_length_minutes")
        val JOY_TARGET_MINUTES = intPreferencesKey("joy_target_minutes")
        val NUDGE = booleanPreferencesKey("nudge_enabled")
        val DAY_PROMPT_ENABLED = booleanPreferencesKey("day_prompt_enabled")
        val DAY_PROMPT_MINUTE = intPreferencesKey("day_prompt_minute")
        val LAST_DAY_PROMPT = longPreferencesKey("last_day_prompt")
    }
}
