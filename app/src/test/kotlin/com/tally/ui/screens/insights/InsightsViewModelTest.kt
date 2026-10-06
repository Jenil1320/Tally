package com.tally.ui.screens.insights

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tally.core.money.Pence
import com.tally.core.time.FakeClock
import com.tally.domain.model.Category
import com.tally.domain.model.NewTransaction
import com.tally.fakes.FakeCategoryLimitStore
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

            InsightsViewModel(transactions, categories, FakeCategoryLimitStore(), clock, UnconfinedTestDispatcher()).uiState.test {
                var state = awaitItem()
                while (!state.loaded) state = awaitItem()
                assertThat(state.total).isEqualTo(Pence(1500))
                assertThat(state.breakdown.map { it.name }).containsExactly("Transport", "Eating out").inOrder()
                assertThat(state.monthLabel).isEqualTo("June 2026")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test fun `eating out reports the share while totals stay at the full amount paid`() =
        runTest {
            transactions.add(NewTransaction(Pence(6000), "Dishoom", occurredAt = now, categoryId = "c1", myShare = Pence(1500)))
            transactions.add(NewTransaction(Pence(900), "Train", occurredAt = now, categoryId = "c2"))
            val viewModel = InsightsViewModel(transactions, categories, FakeCategoryLimitStore(), clock, UnconfinedTestDispatcher())

            viewModel.uiState.test {
                var state = awaitItem()
                while (!state.loaded) state = awaitItem()
                assertThat(state.total).isEqualTo(Pence(6900))
                assertThat(state.eatingOut!!.paid).isEqualTo(Pence(6000))
                assertThat(state.eatingOut!!.mine).isEqualTo(Pence(1500))

                viewModel.toggleCategory("c1") // hide Eating out from the view
                var filtered = awaitItem()
                while (filtered.total != Pence(900)) filtered = awaitItem()
                assertThat(filtered.eatingOut).isNull()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test fun `the month rolls over when the screen resumes after a month change`() =
        runTest {
            val lateJune = Instant.parse("2026-06-30T10:00:00Z")
            val monthClock = FakeClock(lateJune.toEpochMilliseconds())
            val monthTransactions = FakeTransactionRepository(monthClock)
            monthTransactions.add(NewTransaction(Pence(500), "June thing", occurredAt = lateJune, categoryId = "c1"))
            val viewModel =
                InsightsViewModel(monthTransactions, categories, FakeCategoryLimitStore(), monthClock, UnconfinedTestDispatcher())

            viewModel.uiState.test {
                var state = awaitItem()
                while (!state.loaded) state = awaitItem()
                assertThat(state.monthLabel).isEqualTo("June 2026")

                monthClock.setTo(Instant.parse("2026-07-01T08:00:00Z").toEpochMilliseconds())
                viewModel.onResume()
                var july = awaitItem()
                while (july.monthLabel != "July 2026") july = awaitItem()
                assertThat(july.monthHasTransactions).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test fun `toggling a category off removes it from the analysis`() =
        runTest {
            transactions.add(NewTransaction(Pence(900), "Train", occurredAt = now, categoryId = "c2"))
            transactions.add(NewTransaction(Pence(600), "Lunch", occurredAt = now, categoryId = "c1"))
            val viewModel = InsightsViewModel(transactions, categories, FakeCategoryLimitStore(), clock, UnconfinedTestDispatcher())

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
