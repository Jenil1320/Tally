package com.tally.fakes

import com.tally.domain.notify.NotificationIngestPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [NotificationIngestPreferences] for tests. */
class FakeNotificationIngestPreferences(
    initial: Boolean = false,
) : NotificationIngestPreferences {
    private val flow = MutableStateFlow(initial)
    override val enabled: Flow<Boolean> = flow

    override suspend fun setEnabled(enabled: Boolean) {
        flow.value = enabled
    }
}
