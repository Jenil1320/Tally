package com.tally.domain.reminder

/** Schedules or cancels the daily reminder. Implementation uses WorkManager. */
interface ReminderScheduler {
    /** (Re)schedule the daily reminder to fire at [hour]:[minute] local time. */
    fun schedule(
        hour: Int,
        minute: Int,
    )

    /** Cancel any scheduled reminder. */
    fun cancel()
}
