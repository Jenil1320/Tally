package com.tally.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tally.domain.reminder.ReminderPreferences
import com.tally.domain.reminder.ReminderState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.reminderDataStore by preferencesDataStore(name = "reminder")

/** DataStore-backed [ReminderPreferences]. */
@Singleton
class DataStoreReminderPreferences
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ReminderPreferences {
        override val state: Flow<ReminderState> =
            context.reminderDataStore.data.map { prefs ->
                ReminderState(
                    enabled = prefs[ENABLED] ?: true,
                    hour = prefs[HOUR] ?: DEFAULT_HOUR,
                    minute = prefs[MINUTE] ?: 0,
                )
            }

        override suspend fun setEnabled(enabled: Boolean) {
            context.reminderDataStore.edit { it[ENABLED] = enabled }
        }

        override suspend fun setTime(
            hour: Int,
            minute: Int,
        ) {
            context.reminderDataStore.edit {
                it[HOUR] = hour
                it[MINUTE] = minute
            }
        }

        private companion object {
            val ENABLED = booleanPreferencesKey("reminder_enabled")
            val HOUR = intPreferencesKey("reminder_hour")
            val MINUTE = intPreferencesKey("reminder_minute")
            const val DEFAULT_HOUR = 22
        }
    }
