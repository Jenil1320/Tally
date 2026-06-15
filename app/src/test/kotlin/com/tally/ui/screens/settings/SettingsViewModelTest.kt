package com.tally.ui.screens.settings

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tally.domain.reminder.ReminderState
import com.tally.fakes.FakeReminderPreferences
import com.tally.fakes.FakeReminderScheduler
import com.tally.util.MainDispatcherExtension
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class SettingsViewModelTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    @Test fun `enabling schedules and persists, disabling cancels`() =
        runTest {
            val prefs = FakeReminderPreferences(ReminderState(enabled = false, hour = 22, minute = 0))
            val scheduler = FakeReminderScheduler()
            val viewModel = SettingsViewModel(prefs, scheduler)

            viewModel.setReminderEnabled(true)
            assertThat(scheduler.scheduledAt).isEqualTo(22 to 0)
            assertThat(prefs.state.first().enabled).isTrue()

            viewModel.setReminderEnabled(false)
            assertThat(scheduler.cancelled).isTrue()
            assertThat(prefs.state.first().enabled).isFalse()
        }

    @Test fun `uiState formats the configured time`() =
        runTest {
            val viewModel =
                SettingsViewModel(
                    FakeReminderPreferences(ReminderState(enabled = true, hour = 22, minute = 0)),
                    FakeReminderScheduler(),
                )
            viewModel.uiState.test {
                var state = awaitItem()
                while (!state.loaded) state = awaitItem()
                assertThat(state.reminderEnabled).isTrue()
                assertThat(state.reminderTime).isEqualTo("22:00")
                cancelAndIgnoreRemainingEvents()
            }
        }
}
