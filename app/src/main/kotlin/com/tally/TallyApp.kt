package com.tally

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Tally application entry point.
 *
 * Implements [Configuration.Provider] so [androidx.work.WorkManager] picks up
 * Hilt-injected workers (the 22:00 reminder in M2 ships through here).
 */
@HiltAndroidApp
class TallyApp : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() =
            Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .setMinimumLoggingLevel(android.util.Log.INFO)
                .build()
}
