package com.tally.ui.screens.today

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

class TodayViewModelTest {
    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val now = Instant.parse("2026-06-15T10:00:00Z")
    private val clock = FakeClock(now.toEpochMilliseconds())
    private val categories = FakeCategoryRepository(listOf(Category("c1", "Eating out", 1, true, 0)))
    private val transactions = FakeTransactionRepository(clock)

    @Test fun `sums today, maps category, and excludes older days`() =
        runTest {
            transactions.add(NewTransaction(Pence(485), "Coffee", occurredAt = now, categoryId = "c1"))
            transactions.add(NewTransaction(Pence(200), "Bus", occurredAt = now))
            val threeDaysAgo = Instant.fromEpochMilliseconds(now.toEpochMilliseconds() - 3L * 24 * 3600 * 1000)
            transactions.add(NewTransaction(Pence(999), "Old", occurredAt = threeDaysAgo))

            TodayViewModel(transactions, categories, clock).uiState.test {
                var state = awaitItem()
                while (!state.loaded) state = awaitItem()
                assertThat(state.spentToday).isEqualTo(Pence(685))
                assertThat(state.items.map { it.merchant }).containsExactly("Coffee", "Bus")
                assertThat(state.items.first { it.merchant == "Coffee" }.categoryName).isEqualTo("Eating out")
                cancelAndIgnoreRemainingEvents()
            }
        }
}
