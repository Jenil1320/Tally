package com.tally.ui.screens.insights

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tally.core.money.Pence
import com.tally.core.time.FakeClock
import com.tally.domain.model.Category
import com.tally.domain.model.NewTransaction
import com.tally.fakes.FakeCategoryRepository
import com.tally.fakes.FakeTransactionRepository
import com.tally.util.MainDispatcherExtension
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class InsightsViewModelTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val now = Instant.parse("2026-06-15T10:00:00Z")
    private val clock = FakeClock(now.toEpochMilliseconds())
    private val categories =
        FakeCategoryRepository(
            listOf(
                Category("c1", "Eating out", 1, true, 0),
                Category("c2", "Transport", 2, true, 1),
            ),
        )
    private val transactions = FakeTransactionRepository(clock)

    @Test fun `totals and breakdown cover this month only`() =
        runTest {
            transactions.add(NewTransaction(Pence(900), "Train", occurredAt = now, categoryId = "c2"))
            transactions.add(NewTransaction(Pence(600), "Lunch", occurredAt = now, categoryId = "c1"))
            val lastMonth = Instant.fromEpochMilliseconds(now.toEpochMilliseconds() - 40L * 24 * 3600 * 1000)
            transactions.add(NewTransaction(Pence(9999), "Old", occurredAt = lastMonth, categoryId = "c1"))

            InsightsViewModel(transactions, categories, clock, UnconfinedTestDispatcher()).uiState.test {
                var state = awaitItem()
                while (!state.loaded) state = awaitItem()
                assertThat(state.total).isEqualTo(Pence(1500))
                assertThat(state.breakdown.map { it.name }).containsExactly("Transport", "Eating out").inOrder()
                assertThat(state.monthLabel).isEqualTo("June 2026")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test fun `toggling a category off removes it from the analysis`() =
        runTest {
            transactions.add(NewTransaction(Pence(900), "Train", occurredAt = now, categoryId = "c2"))
            transactions.add(NewTransaction(Pence(600), "Lunch", occurredAt = now, categoryId = "c1"))
            val viewModel = InsightsViewModel(transactions, categories, clock, UnconfinedTestDispatcher())

            viewModel.uiState.test {
                var state = awaitItem()
                while (!state.loaded) state = awaitItem()
                assertThat(state.total).isEqualTo(Pence(1500))
                assertThat(state.chips.map { it.name }).containsExactly("Transport", "Eating out").inOrder()

                viewModel.toggleCategory("c2") // exclude Transport
                var filtered = awaitItem()
                while (filtered.total != Pence(600)) filtered = awaitItem()
                assertThat(filtered.breakdown.map { it.name }).containsExactly("Eating out")
                assertThat(filtered.chips.first { it.name == "Transport" }.included).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }
}
