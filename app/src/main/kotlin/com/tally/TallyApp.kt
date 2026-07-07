package com.tally

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.tally.data.seed.DatabaseSeeder
import com.tally.domain.reminder.ReminderPreferences
import com.tally.domain.reminder.ReminderScheduler
import com.tally.work.CaptureNotifier
import com.tally.work.ReminderNotifier
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Tally application entry point.
 *
 * Implements [Configuration.Provider] so [androidx.work.WorkManager] picks up
 * Hilt-injected workers (the 22:00 reminder in M2 ships through here).
 *
 * On first launch it seeds the system categories and an example transaction on a
 * background scope (idempotent — see [DatabaseSeeder]).
 */
@HiltAndroidApp
class TallyApp : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var databaseSeeder: DatabaseSeeder

    @Inject lateinit var reminderPreferences: ReminderPreferences

    @Inject lateinit var reminderScheduler: ReminderScheduler

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        ReminderNotifier.ensureChannel(this)
        CaptureNotifier.ensureChannel(this)
        applicationScope.launch {
            databaseSeeder.ensureSetup()
            val reminder = reminderPreferences.state.first()
            if (reminder.enabled) {
                reminderScheduler.schedule(reminder.hour, reminder.minute)
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() =
            Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .setMinimumLoggingLevel(android.util.Log.INFO)
                .build()
}
