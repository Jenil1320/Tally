package com.tally.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Posts the daily reminder. A [HiltWorker] so it flows through the app's
 * `HiltWorkerFactory` (set up in `TallyApp`), leaving room to inject
 * dependencies later (e.g. skip the nudge if something was already logged).
 */
@HiltWorker
class ReminderWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            ReminderNotifier.show(applicationContext)
            return Result.success()
        }
    }
