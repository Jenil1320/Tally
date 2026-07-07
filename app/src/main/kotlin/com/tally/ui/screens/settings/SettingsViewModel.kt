package com.tally.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tally.domain.notify.NotificationIngestPreferences
import com.tally.domain.reminder.ReminderPreferences
import com.tally.domain.reminder.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val reminderEnabled: Boolean = true,
    val reminderTime: String = "22:00",
    val captureEnabled: Boolean = false,
    val loaded: Boolean = false,
)

/**
 * Backs the Settings screen: the daily-reminder toggle (M2) and the payment
 * notification-capture toggle (M4). Capture-enabled is Tally's own switch; the
 * screen separately checks the OS "Notification access" grant.
 */
@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val reminderPreferences: ReminderPreferences,
        private val reminderScheduler: ReminderScheduler,
        private val notificationIngestPreferences: NotificationIngestPreferences,
    ) : ViewModel() {
        val uiState: StateFlow<SettingsUiState> =
            combine(
                reminderPreferences.state,
                notificationIngestPreferences.enabled,
            ) { reminder, captureEnabled ->
                SettingsUiState(
                    reminderEnabled = reminder.enabled,
                    reminderTime = "%02d:%02d".format(reminder.hour, reminder.minute),
                    captureEnabled = captureEnabled,
                    loaded = true,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

        fun setReminderEnabled(enabled: Boolean) {
            viewModelScope.launch {
                reminderPreferences.setEnabled(enabled)
                val state = reminderPreferences.state.first()
                if (enabled) {
                    reminderScheduler.schedule(state.hour, state.minute)
                } else {
                    reminderScheduler.cancel()
                }
            }
        }

        fun setCaptureEnabled(enabled: Boolean) {
            viewModelScope.launch {
                notificationIngestPreferences.setEnabled(enabled)
            }
        }
    }
