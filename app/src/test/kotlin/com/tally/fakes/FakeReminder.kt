package com.tally.fakes

import com.tally.domain.reminder.ReminderPreferences
import com.tally.domain.reminder.ReminderScheduler
import com.tally.domain.reminder.ReminderState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [ReminderPreferences] for tests. */
class FakeReminderPreferences(
    initial: ReminderState = ReminderState(),
) : ReminderPreferences {
    private val flow = MutableStateFlow(initial)
    override val state: Flow<ReminderState> = flow

    override suspend fun setEnabled(enabled: Boolean) {
        flow.value = flow.value.copy(enabled = enabled)
    }

    override suspend fun setTime(
        hour: Int,
        minute: Int,
    ) {
        flow.value = flow.value.copy(hour = hour, minute = minute)
    }
}

/** Records scheduler calls for assertions. */
class FakeReminderScheduler : ReminderScheduler {
    var scheduledAt: Pair<Int, Int>? = null
    var cancelled = false

    override fun schedule(
        hour: Int,
        minute: Int,
    ) {
        scheduledAt = hour to minute
        cancelled = false
    }

    override fun cancel() {
        cancelled = true
        scheduledAt = null
    }
}
