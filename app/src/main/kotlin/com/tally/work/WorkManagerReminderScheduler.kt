package com.tally.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.tally.core.time.Clock
import com.tally.domain.reminder.ReminderScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Milliseconds from [now] until the next [hour]:[minute] in [tz] — today if it
 * is still ahead, otherwise tomorrow. Pure, so it is unit-tested directly.
 */
internal fun nextReminderDelayMillis(
    now: Instant,
    tz: TimeZone,
    hour: Int,
    minute: Int,
): Long {
    val nowLocal = now.toLocalDateTime(tz)
    val todayTarget =
        LocalDateTime(nowLocal.year, nowLocal.monthNumber, nowLocal.dayOfMonth, hour, minute)
            .toInstant(tz)
    val target = if (todayTarget > now) todayTarget else todayTarget.plus(1, DateTimeUnit.DAY, tz)
    return (target - now).inWholeMilliseconds
}

/**
 * WorkManager-backed [ReminderScheduler]. Uses a unique periodic (24h) request
 * with an initial delay to the next occurrence; `UPDATE` keeps the time in sync
 * when the user changes it. The reminder is not an exact alarm — a few minutes'
 * drift is fine for a nudge.
 */
@Singleton
class WorkManagerReminderScheduler
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val clock: Clock,
    ) : ReminderScheduler {
        private val timeZone = TimeZone.currentSystemDefault()

        override fun schedule(
            hour: Int,
            minute: Int,
        ) {
            val delay = nextReminderDelayMillis(clock.now(), timeZone, hour, minute)
            val request =
                PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        override fun cancel() {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        private companion object {
            const val WORK_NAME = "tally-daily-reminder"
        }
    }
