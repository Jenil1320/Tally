package com.tally.domain.notify

import kotlinx.coroutines.flow.Flow

/**
 * Whether the user has turned on auto-capture from payment notifications. This is
 * Tally's own switch and is independent of the OS "Notification access" grant —
 * both must be on for capture to run, so the user can pause capture without
 * revoking system access. Off by default (opt-in, per the privacy constitution).
 */
interface NotificationIngestPreferences {
    /** Live enabled/disabled state. */
    val enabled: Flow<Boolean>

    /** Turns auto-capture on or off. */
    suspend fun setEnabled(enabled: Boolean)
}
