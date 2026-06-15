package com.tally.data.seed

import com.google.common.truth.Truth.assertThat
import com.tally.core.id.UlidGenerator
import com.tally.core.time.FakeClock
import com.tally.domain.model.Category
import com.tally.fakes.FakeCategoryRepository
import com.tally.fakes.FakeTransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class DatabaseSeederTest {
    private val clock = FakeClock(1_000)
    private var counter = 0
    private val ulid = UlidGenerator { "id${counter++}" }

    @Test fun `fresh install seeds all default categories and one example`() =
        runTest {
            val categories = FakeCategoryRepository()
            val transactions = FakeTransactionRepository(clock)
            DatabaseSeeder(categories, transactions, clock, ulid).ensureSetup()

            val names = categories.observeAll().first().map { it.name }
            assertThat(names).containsAtLeast("Groceries", "Rent", "Subscriptions")
            assertThat(transactions.countLive()).isEqualTo(1)
        }

    @Test fun `existing install gains only missing categories and no example`() =
        runTest {
            val categories =
                FakeCategoryRepository(
                    listOf(
                        Category("c1", "Groceries", 0, true, 0),
                        Category("c2", "Other", 8, true, 8),
                    ),
                )
            val transactions = FakeTransactionRepository(clock)
            DatabaseSeeder(categories, transactions, clock, ulid).ensureSetup()

            val names = categories.observeAll().first().map { it.name }
            assertThat(names).containsAtLeast("Rent", "Subscriptions")
            assertThat(names.count { it == "Groceries" }).isEqualTo(1) // not duplicated
            assertThat(transactions.countLive()).isEqualTo(0) // example only on fresh install
        }
}
