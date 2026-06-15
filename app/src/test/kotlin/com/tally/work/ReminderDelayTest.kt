package com.tally.work

import com.google.common.truth.Truth.assertThat
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

class ReminderDelayTest {
    private val utc = TimeZone.UTC
    private val hour = 3_600_000L

    @Test fun `delay until later the same day`() {
        val now = Instant.parse("2026-06-15T20:00:00Z")
        assertThat(nextReminderDelayMillis(now, utc, 22, 0)).isEqualTo(2 * hour)
    }

    @Test fun `delay rolls to tomorrow once the time has passed`() {
        val now = Instant.parse("2026-06-15T23:00:00Z")
        assertThat(nextReminderDelayMillis(now, utc, 22, 0)).isEqualTo(23 * hour)
    }

    @Test fun `exactly at the time schedules for tomorrow`() {
        val now = Instant.parse("2026-06-15T22:00:00Z")
        assertThat(nextReminderDelayMillis(now, utc, 22, 0)).isEqualTo(24 * hour)
    }
}
