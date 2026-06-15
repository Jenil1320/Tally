package com.tally.data.repo

import com.google.common.truth.Truth.assertThat
import com.tally.core.id.UlidGenerator
import com.tally.core.money.Pence
import com.tally.core.time.FakeClock
import com.tally.domain.model.NewTransaction
import com.tally.domain.model.TransactionSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test

class DefaultTransactionRepositoryTest {
    private val dao = FakeTransactionDao()
    private val clock = FakeClock(1_000)
    private var counter = 0
    private val ulid = UlidGenerator { "ID%026d".format(counter++) }
    private val repository =
        DefaultTransactionRepository(dao, ulid, clock, UnconfinedTestDispatcher())

    private fun draft(
        amount: Long = 485,
        merchant: String = "Coffee",
        occurredAt: Long = 900,
    ) = NewTransaction(
        amount = Pence(amount),
        merchant = merchant,
        occurredAt = Instant.fromEpochMilliseconds(occurredAt),
        categoryId = "cat1",
        source = TransactionSource.MANUAL,
    )

    @Test fun `add mints an id and stamps createdAt from the clock`() =
        runTest {
            val id = repository.add(draft())
            val stored = dao.findById(id)!!
            assertThat(stored.amount).isEqualTo(485L)
            assertThat(stored.merchant).isEqualTo("Coffee")
            assertThat(stored.occurredAt).isEqualTo(900L)
            assertThat(stored.createdAt).isEqualTo(1_000L)
            assertThat(stored.source).isEqualTo("MANUAL")
            assertThat(stored.deletedAt).isNull()
        }

    @Test fun `observeAll maps entities to domain, newest first`() =
        runTest {
            repository.add(draft(amount = 100, occurredAt = 50))
            repository.add(draft(amount = 200, occurredAt = 999))
            val all = repository.observeAll().first()
            assertThat(all.map { it.amount }).containsExactly(Pence(200), Pence(100)).inOrder()
        }

    @Test fun `softDelete tombstones with the clock time and hides the row`() =
        runTest {
            val id = repository.add(draft())
            clock.setTo(5_000)
            repository.softDelete(id)
            assertThat(repository.observeAll().first()).isEmpty()
            assertThat(repository.countLive()).isEqualTo(0)
            assertThat(dao.findById(id)!!.deletedAt).isEqualTo(5_000L)
        }

    @Test fun `findById returns the domain transaction or null`() =
        runTest {
            val id = repository.add(draft())
            assertThat(repository.findById(id)?.merchant).isEqualTo("Coffee")
            assertThat(repository.findById("missing")).isNull()
        }

    @Test fun `update changes editable fields but preserves createdAt`() =
        runTest {
            val id = repository.add(draft(amount = 485, merchant = "Coffee"))
            val createdAt = dao.findById(id)!!.createdAt
            clock.setTo(9_000)
            repository.update(id, draft(amount = 999, merchant = "Lunch"))
            val updated = dao.findById(id)!!
            assertThat(updated.amount).isEqualTo(999L)
            assertThat(updated.merchant).isEqualTo("Lunch")
            assertThat(updated.createdAt).isEqualTo(createdAt)
            assertThat(updated.deletedAt).isNull()
        }
}
