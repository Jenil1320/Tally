package com.tally.domain.reminder

import kotlinx.coroutines.flow.Flow

/**
 * The daily-reminder configuration.
 *
 * @property enabled whether the 22:00 reminder fires (default on).
 * @property hour / [minute] when it fires (24h local time; default 22:00). The
 *   time is stored now so a time-picker can be added later without a data change;
 *   M2 exposes only the on/off toggle.
 */
data class ReminderState(
    val enabled: Boolean = true,
    val hour: Int = 22,
    val minute: Int = 0,
)

/** Persistent store for the reminder config. */
interface ReminderPreferences {
    val state: Flow<ReminderState>

    suspend fun setEnabled(enabled: Boolean)

    suspend fun setTime(
        hour: Int,
        minute: Int,
    )
}
