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

    @Test fun `updateCategory changes only the category`() =
        runTest {
            val id = repository.add(draft(merchant = "Coffee"))
            repository.updateCategory(id, "cat2")
            val updated = dao.findById(id)!!
            assertThat(updated.categoryId).isEqualTo("cat2")
            assertThat(updated.merchant).isEqualTo("Coffee")
        }

    @Test fun `topCategoryIds ranks live categories by frequency`() =
        runTest {
            repeat(3) { repository.add(draft(merchant = "a").copy(categoryId = "food")) }
            repeat(1) { repository.add(draft(merchant = "b").copy(categoryId = "travel")) }
            repeat(2) { repository.add(draft(merchant = "c").copy(categoryId = "shop")) }
            assertThat(repository.topCategoryIds(2)).containsExactly("food", "shop").inOrder()
        }

    @Test fun `my share is stored and read back, and an old row reads as unshared`() =
        runTest {
            val shared = repository.add(draft(amount = 6000).copy(myShare = Pence(1500)))
            val plain = repository.add(draft(amount = 2000))
            assertThat(dao.findById(shared)!!.myShare).isEqualTo(1500L)
            assertThat(dao.findById(plain)!!.myShare).isNull()
            assertThat(repository.findById(shared)!!.myShare).isEqualTo(Pence(1500))
            assertThat(repository.findById(plain)!!.ownSpend).isEqualTo(Pence(2000))
        }

    @Test fun `update can set and clear my share without touching the amount paid`() =
        runTest {
            val id = repository.add(draft(amount = 6000))
            repository.update(id, draft(amount = 6000).copy(myShare = Pence(2000)))
            assertThat(dao.findById(id)!!.amount).isEqualTo(6000L)
            assertThat(dao.findById(id)!!.myShare).isEqualTo(2000L)
            repository.update(id, draft(amount = 6000))
            assertThat(dao.findById(id)!!.myShare).isNull()
        }

    @Test fun `updateShare sets only the share and refuses a share above the amount paid`() =
        runTest {
            val id = repository.add(draft(amount = 3840, merchant = "Dishoom"))
            repository.updateShare(id, Pence(1920))
            assertThat(dao.findById(id)!!.myShare).isEqualTo(1920L)
            assertThat(dao.findById(id)!!.amount).isEqualTo(3840L)
            assertThat(dao.findById(id)!!.merchant).isEqualTo("Dishoom")

            repository.updateShare(id, Pence(3841)) // above the amount paid: ignored
            assertThat(dao.findById(id)!!.myShare).isEqualTo(1920L)

            repository.updateShare(id, null) // clears it
            assertThat(dao.findById(id)!!.myShare).isNull()

            repository.updateShare("missing", Pence(100)) // unknown id: no crash
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
