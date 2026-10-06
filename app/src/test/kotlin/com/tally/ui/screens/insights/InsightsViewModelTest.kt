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

    private fun viewModel(
        repository: FakeTransactionRepository = transactions,
        viewModelClock: FakeClock = clock,
    ) = InsightsViewModel(repository, categories, FakeCategoryLimitStore(), viewModelClock, UnconfinedTestDispatcher())

    private fun daysAgo(days: Long) = Instant.fromEpochMilliseconds(now.toEpochMilliseconds() - days * 24 * 3600 * 1000)

    @Test fun `this month's figures cover this month and the trend covers six months`() =
        runTest {
            transactions.add(NewTransaction(Pence(900), "Train", occurredAt = now, categoryId = "c2"))
            transactions.add(NewTransaction(Pence(600), "Lunch", occurredAt = now, categoryId = "c1"))
            transactions.add(NewTransaction(Pence(9999), "Old", occurredAt = daysAgo(40), categoryId = "c1"))

            viewModel().uiState.test {
                var state = awaitItem()
                while (!state.loaded) state = awaitItem()
                assertThat(state.total).isEqualTo(Pence(1500))
                assertThat(state.transactionCount).isEqualTo(2)
                assertThat(state.monthLabel).isEqualTo("June 2026")
                assertThat(state.trend.map { it.shortLabel }).containsExactly("Jan", "Feb", "Mar", "Apr", "May", "Jun").inOrder()
                assertThat(state.trend.last().total).isEqualTo(Pence(1500))
                assertThat(state.trend[4].total).isEqualTo(Pence(9999)) // May
                assertThat(state.usualMonth).isEqualTo(Pence(9999)) // only May had spend before June
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test fun `a typical week and the day by day chart are built from the window`() =
        runTest {
            transactions.add(NewTransaction(Pence(900), "Train", occurredAt = now, categoryId = "c2"))
            transactions.add(NewTransaction(Pence(600), "Lunch", occurredAt = daysAgo(3), categoryId = "c1"))

            viewModel().uiState.test {
                var state = awaitItem()
                while (!state.loaded) state = awaitItem()
                assertThat(state.weekdayAverages).hasSize(7)
                assertThat(state.rhythm).isNotNull()
                assertThat(state.dailyTotals).hasSize(30) // June
                assertThat(state.todayIndex).isEqualTo(14)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test fun `eating out by month reports the share while the totals stay at the full amount paid`() =
        runTest {
            transactions.add(NewTransaction(Pence(6000), "Dishoom", occurredAt = now, categoryId = "c1", myShare = Pence(1500)))
            transactions.add(NewTransaction(Pence(900), "Train", occurredAt = now, categoryId = "c2"))

            val viewModel = viewModel()
            viewModel.uiState.test {
                var state = awaitItem()
                while (!state.loaded) state = awaitItem()
                assertThat(state.total).isEqualTo(Pence(6900))
                val june = state.eatingOutMonths.single()
                assertThat(june.paid).isEqualTo(Pence(6000))
                assertThat(june.mine).isEqualTo(Pence(1500))
                assertThat(june.hasShared).isTrue()

                viewModel.toggleCategory("c1") // leave Eating out out of the view
                var filtered = awaitItem()
                while (filtered.total != Pence(900)) filtered = awaitItem()
                assertThat(filtered.eatingOutMonths).isEmpty()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test fun `toggling a category off removes it from the trend and the other views`() =
        runTest {
            transactions.add(NewTransaction(Pence(900), "Train", occurredAt = now, categoryId = "c2"))
            transactions.add(NewTransaction(Pence(600), "Lunch", occurredAt = now, categoryId = "c1"))
            val viewModel = viewModel()

            viewModel.uiState.test {
                var state = awaitItem()
                while (!state.loaded) state = awaitItem()
                assertThat(state.total).isEqualTo(Pence(1500))
                assertThat(state.chips.map { it.name }).containsExactly("Transport", "Eating out").inOrder()

                viewModel.toggleCategory("c2") // exclude Transport
                var filtered = awaitItem()
                while (filtered.total != Pence(600)) filtered = awaitItem()
                assertThat(filtered.trend.last().total).isEqualTo(Pence(600))
                assertThat(filtered.chips.first { it.name == "Transport" }.included).isFalse()
                assertThat(filtered.topMerchants.map { it.merchant }).containsExactly("Lunch")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test fun `an empty ledger has no data to read`() =
        runTest {
            viewModel().uiState.test {
                var state = awaitItem()
                while (!state.loaded) state = awaitItem()
                assertThat(state.hasData).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test fun `the window rolls over when the screen resumes after a month change`() =
        runTest {
            val lateJune = Instant.parse("2026-06-30T10:00:00Z")
            val monthClock = FakeClock(lateJune.toEpochMilliseconds())
            val monthTransactions = FakeTransactionRepository(monthClock)
            monthTransactions.add(NewTransaction(Pence(500), "June thing", occurredAt = lateJune, categoryId = "c1"))
            val viewModel = viewModel(monthTransactions, monthClock)

            viewModel.uiState.test {
                var state = awaitItem()
                while (!state.loaded) state = awaitItem()
                assertThat(state.monthLabel).isEqualTo("June 2026")

                monthClock.setTo(Instant.parse("2026-07-01T08:00:00Z").toEpochMilliseconds())
                viewModel.onResume()
                var july = awaitItem()
                while (july.monthLabel != "July 2026") july = awaitItem()
                assertThat(july.total).isEqualTo(Pence.ZERO)
                assertThat(july.trend.last().shortLabel).isEqualTo("Jul")
                assertThat(july.trend[4].total).isEqualTo(Pence(500)) // June is now last month
                assertThat(july.hasData).isTrue() // June is still inside the window
                cancelAndIgnoreRemainingEvents()
            }
        }
}
