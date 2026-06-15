package com.tally.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tally.domain.reminder.ReminderPreferences
import com.tally.domain.reminder.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val reminderEnabled: Boolean = true,
    val reminderTime: String = "22:00",
    val loaded: Boolean = false,
)

/** Backs the Settings screen. M2 exposes the daily-reminder on/off toggle. */
@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val reminderPreferences: ReminderPreferences,
        private val reminderScheduler: ReminderScheduler,
    ) : ViewModel() {
        val uiState: StateFlow<SettingsUiState> =
            reminderPreferences.state
                .map { state ->
                    SettingsUiState(
                        reminderEnabled = state.enabled,
                        reminderTime = "%02d:%02d".format(state.hour, state.minute),
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
    }
