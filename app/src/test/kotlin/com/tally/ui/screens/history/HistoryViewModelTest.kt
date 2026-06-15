package com.tally.ui.screens.history

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tally.core.money.Pence
import com.tally.core.time.FakeClock
import com.tally.domain.model.Category
import com.tally.domain.model.NewTransaction
import com.tally.fakes.FakeCategoryRepository
import com.tally.fakes.FakeTransactionRepository
import com.tally.util.MainDispatcherExtension
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class HistoryViewModelTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val now = Instant.parse("2026-06-15T10:00:00Z")
    private val clock = FakeClock(now.toEpochMilliseconds())
    private val categories = FakeCategoryRepository(listOf(Category("c1", "Eating out", 1, true, 0)))
    private val transactions = FakeTransactionRepository(clock)

    @Test fun `groups by day, newest first, with per-day totals`() =
        runTest {
            transactions.add(NewTransaction(Pence(485), "Coffee", occurredAt = now, categoryId = "c1"))
            val yesterday = Instant.fromEpochMilliseconds(now.toEpochMilliseconds() - 24L * 3600 * 1000)
            transactions.add(NewTransaction(Pence(200), "Bus", occurredAt = yesterday))

            HistoryViewModel(transactions, categories, clock).uiState.test {
                var state = awaitItem()
                while (!state.loaded) state = awaitItem()
                assertThat(state.days).hasSize(2)
                assertThat(state.days[0].label).isEqualTo("Today")
                assertThat(state.days[0].total).isEqualTo(Pence(485))
                assertThat(state.days[1].label).isEqualTo("Yesterday")
                assertThat(state.days[1].items.first().subtitle).isEqualTo("Uncategorised")
                cancelAndIgnoreRemainingEvents()
            }
        }
}
