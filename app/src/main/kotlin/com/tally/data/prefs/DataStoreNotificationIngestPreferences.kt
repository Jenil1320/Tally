package com.tally.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.tally.domain.notify.NotificationIngestPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.notificationIngestDataStore by preferencesDataStore(name = "notification_ingest")

/** DataStore-backed [NotificationIngestPreferences]. Defaults to off (opt-in). */
@Singleton
class DataStoreNotificationIngestPreferences
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : NotificationIngestPreferences {
        override val enabled: Flow<Boolean> =
            context.notificationIngestDataStore.data.map { prefs -> prefs[ENABLED] ?: false }

        override suspend fun setEnabled(enabled: Boolean) {
            context.notificationIngestDataStore.edit { it[ENABLED] = enabled }
        }

        private companion object {
            val ENABLED = booleanPreferencesKey("capture_enabled")
        }
    }
