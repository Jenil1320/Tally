package com.tally.ui.screens.home

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tally.core.money.Pence
import com.tally.core.time.FakeClock
import com.tally.domain.model.Category
import com.tally.domain.model.NewTransaction
import com.tally.domain.model.Transaction
import com.tally.domain.model.TransactionSource
import com.tally.fakes.FakeCategoryRepository
import com.tally.fakes.FakeTransactionRepository
import com.tally.util.MainDispatcherExtension
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class HomeViewModelTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val utc = TimeZone.UTC
    private val eatingOut = Category("c-eat", "Eating out", 1, true, 1)
    private val transport = Category("c-trn", "Transport", 2, true, 2)
    private val categories = listOf(eatingOut, transport)
    private var counter = 0

    // Tuesday 6 October 2026, midday UTC.
    private val today = LocalDate(2026, 10, 6)

    private fun at(
        date: String,
        time: String = "12:00:00",
    ): Instant = Instant.parse("${date}T${time}Z")

    private fun tx(
        date: String,
        amount: Long,
        categoryId: String? = null,
        myShare: Long? = null,
        merchant: String = "Shop",
    ): Transaction {
        val occurredAt = at(date)
        return Transaction(
            id = "t${counter++}",
            amount = Pence(amount),
            merchant = merchant,
            note = null,
            categoryId = categoryId,
            occurredAt = occurredAt,
            createdAt = occurredAt,
            source = TransactionSource.MANUAL,
            myShare = myShare?.let(::Pence),
        )
    }

    private fun state(vararg transactions: Transaction) =
        homeState(
            transactions.sortedByDescending { it.occurredAt },
            categories,
            today,
            utc,
        )

    @Test fun `this month sums only the current month and counts today`() {
        val s =
            state(
                tx("2026-10-06", 485, "c-trn"),
                tx("2026-10-06", 200),
                tx("2026-10-02", 1_000, "c-eat"),
                tx("2026-09-30", 9_999),
            )
        assertThat(s.monthTotal).isEqualTo(Pence(1_685))
        assertThat(s.spentToday).isEqualTo(Pence(685))
        assertThat(s.todayCount).isEqualTo(2)
        assertThat(s.breakdown.first().name).isEqualTo("Eating out")
        assertThat(s.monthName).isEqualTo("October")
        assertThat(s.dateKicker).isEqualTo("Tuesday · 6 October")
        assertThat(s.daysLeft).isEqualTo(25)
    }

    @Test fun `change is measured against the same point last month`() {
        val s =
            state(
                // This month so far.
                tx("2026-10-03", 1_500),
                // Inside the same window last month.
                tx("2026-09-04", 1_000),
                // Later in September, so it must not count.
                tx("2026-09-20", 8_000),
            )
        assertThat(s.deltaPercent).isEqualTo(50)
        assertThat(s.lastMonthName).isEqualTo("September")
    }

    @Test fun `no change figure when last month had nothing to compare`() {
        assertThat(state(tx("2026-10-03", 1_500)).deltaPercent).isNull()
        assertThat(state(tx("2026-10-03", 1_500)).forecastGap).isNull()
    }

    @Test fun `forecast gap compares the month-end pace with last month in full`() {
        val s =
            state(
                // 60.00 over 6 of 31 days gives a pace of 310.00.
                tx("2026-10-03", 6_000),
                // Last month in full.
                tx("2026-09-10", 40_000),
            )
        assertThat(s.forecast).isEqualTo(Pence(6_000L * 31 / 6))
        assertThat(s.forecastGap).isEqualTo(Pence(6_000L * 31 / 6 - 40_000))
    }

    @Test fun `eating out reports the share but the totals stay at the full amount`() {
        val s =
            state(
                tx("2026-10-05", 6_000, "c-eat", myShare = 1_500),
                tx("2026-10-04", 2_000, "c-trn"),
            )
        assertThat(s.monthTotal).isEqualTo(Pence(8_000))
        assertThat(s.eatingOut!!.paid).isEqualTo(Pence(6_000))
        assertThat(s.eatingOut!!.mine).isEqualTo(Pence(1_500))
        assertThat(s.recent.first { it.categoryName == "Eating out" }.share).isEqualTo(Pence(1_500))
        assertThat(s.recent.first { it.categoryName == "Eating out" }.amount).isEqualTo(Pence(6_000))
    }

    @Test fun `recent shows the five newest with a day in the subtitle`() {
        val s =
            state(
                tx("2026-10-06", 600),
                tx("2026-10-05", 500),
                tx("2026-10-04", 400),
                tx("2026-10-03", 300),
                tx("2026-10-02", 200),
                tx("2026-10-01", 100),
                tx("2026-09-30", 50),
            )
        assertThat(s.recent).hasSize(5)
        assertThat(s.recent.map { it.amount.value }).containsExactly(600L, 500L, 400L, 300L, 200L).inOrder()
        assertThat(s.recent.first().subtitle).isEqualTo("Uncategorised · Today, 12:00")
        assertThat(s.recent[1].subtitle).isEqualTo("Uncategorised · Yesterday, 12:00")
        assertThat(s.recent[2].subtitle).isEqualTo("Uncategorised · 4 Oct, 12:00")
    }

    @Test fun `an empty ledger is flagged so the screen can show its empty state`() {
        val s = state()
        assertThat(s.hasAnyTransactions).isFalse()
        assertThat(s.loaded).isTrue()
        assertThat(s.monthTotal).isEqualTo(Pence.ZERO)
    }

    @Test fun `the view model follows the ledger and rolls the month on resume`() =
        runTest {
            val clock = FakeClock(at("2026-10-31", "10:00:00").toEpochMilliseconds())
            val repository = FakeTransactionRepository(clock)
            repository.add(NewTransaction(Pence(500), "Late October", occurredAt = at("2026-10-31", "09:00:00")))

            val viewModel = HomeViewModel(repository, FakeCategoryRepository(categories), clock)
            viewModel.uiState.test {
                var current = awaitItem()
                while (!current.loaded) current = awaitItem()
                assertThat(current.monthTotal).isEqualTo(Pence(500))

                clock.setTo(at("2026-11-01", "08:00:00").toEpochMilliseconds())
                viewModel.onResume()
                var rolled = awaitItem()
                while (rolled.monthTotal != Pence.ZERO) rolled = awaitItem()
                assertThat(rolled.monthName).isEqualTo("November")
                cancelAndIgnoreRemainingEvents()
            }
        }
}
