package com.tally.ui.screens.settings

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tally.domain.reminder.ReminderState
import com.tally.fakes.FakeCategoryLimitStore
import com.tally.fakes.FakeCategoryRepository
import com.tally.fakes.FakeNotificationIngestPreferences
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

    private fun viewModel(
        reminder: ReminderState = ReminderState(enabled = false, hour = 22, minute = 0),
        scheduler: FakeReminderScheduler = FakeReminderScheduler(),
        capture: FakeNotificationIngestPreferences = FakeNotificationIngestPreferences(),
    ) = SettingsViewModel(FakeReminderPreferences(reminder), scheduler, capture, FakeCategoryLimitStore(), FakeCategoryRepository())

    @Test fun `enabling schedules and persists, disabling cancels`() =
        runTest {
            val prefs = FakeReminderPreferences(ReminderState(enabled = false, hour = 22, minute = 0))
            val scheduler = FakeReminderScheduler()
            val viewModel =
                SettingsViewModel(prefs, scheduler, FakeNotificationIngestPreferences(), FakeCategoryLimitStore(), FakeCategoryRepository())

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
                    FakeNotificationIngestPreferences(),
                    FakeCategoryLimitStore(),
                    FakeCategoryRepository(),
                )
            viewModel.uiState.test {
                var state = awaitItem()
                while (!state.loaded) state = awaitItem()
                assertThat(state.reminderEnabled).isTrue()
                assertThat(state.reminderTime).isEqualTo("22:00")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test fun `capture toggle persists and surfaces in uiState`() =
        runTest {
            val capture = FakeNotificationIngestPreferences(initial = false)
            val viewModel = viewModel(capture = capture)

            viewModel.setCaptureEnabled(true)
            assertThat(capture.enabled.first()).isTrue()

            viewModel.uiState.test {
                var state = awaitItem()
                while (!state.loaded || !state.captureEnabled) state = awaitItem()
                assertThat(state.captureEnabled).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }
}
