package com.tally.ui.screens.transactions

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

class TransactionsViewModelTest {
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

            TransactionsViewModel(transactions, categories, clock).uiState.test {
                var state = awaitItem()
                while (!state.loaded) state = awaitItem()
                assertThat(state.days).hasSize(2)
                assertThat(state.days[0].label).isEqualTo("Today")
                assertThat(state.days[0].total).isEqualTo(Pence(485))
                assertThat(state.days[1].label).isEqualTo("Yesterday")
                assertThat(state.days[1].items.first().subtitle).startsWith("Uncategorised · ")
                assertThat(state.days[0].items.first().subtitle).startsWith("Eating out · ")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test fun `a shared meal shows the share but the day total stays the full amount`() =
        runTest {
            transactions.add(
                NewTransaction(Pence(6_000), "Dishoom", occurredAt = now, categoryId = "c1", myShare = Pence(1_500)),
            )
            TransactionsViewModel(transactions, categories, clock).uiState.test {
                var state = awaitItem()
                while (!state.loaded) state = awaitItem()
                val day = state.days.single()
                assertThat(day.total).isEqualTo(Pence(6_000))
                assertThat(day.items.single().amount).isEqualTo(Pence(6_000))
                assertThat(day.items.single().share).isEqualTo(Pence(1_500))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test fun `a share equal to the amount shows no footnote`() =
        runTest {
            transactions.add(
                NewTransaction(Pence(2_000), "Pret", occurredAt = now, categoryId = "c1", myShare = Pence(2_000)),
            )
            TransactionsViewModel(transactions, categories, clock).uiState.test {
                var state = awaitItem()
                while (!state.loaded) state = awaitItem()
                assertThat(state.days.single().items.single().share).isNull()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test fun `labels move on after midnight when the screen resumes`() =
        runTest {
            transactions.add(NewTransaction(Pence(485), "Coffee", occurredAt = now))
            val viewModel = TransactionsViewModel(transactions, categories, clock)
            viewModel.uiState.test {
                var state = awaitItem()
                while (!state.loaded) state = awaitItem()
                assertThat(state.days.single().label).isEqualTo("Today")

                clock.advanceBy(24L * 3600 * 1000)
                viewModel.onResume()
                var later = awaitItem()
                while (later.days.single().label == "Today") later = awaitItem()
                assertThat(later.days.single().label).isEqualTo("Yesterday")
                cancelAndIgnoreRemainingEvents()
            }
        }
}
