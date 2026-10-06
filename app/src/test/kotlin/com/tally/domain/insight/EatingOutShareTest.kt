package com.tally.domain.insight

import com.google.common.truth.Truth.assertThat
import com.tally.core.money.Pence
import com.tally.domain.model.Category
import com.tally.domain.model.NewTransaction
import com.tally.domain.model.Transaction
import com.tally.domain.model.TransactionSource
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class EatingOutShareTest {
    private val eatingOut = Category("c-eat", "Eating out", 1, true, 1)
    private val groceries = Category("c-gro", "Groceries", 0, true, 0)
    private val categories = listOf(groceries, eatingOut)
    private var counter = 0

    private fun tx(
        amount: Long,
        categoryId: String?,
        myShare: Long? = null,
    ) = Transaction(
        id = "t${counter++}",
        amount = Pence(amount),
        merchant = "Somewhere",
        note = null,
        categoryId = categoryId,
        occurredAt = Instant.fromEpochMilliseconds(0),
        createdAt = Instant.fromEpochMilliseconds(0),
        source = TransactionSource.MANUAL,
        myShare = myShare?.let(::Pence),
    )

    @Test fun `own spend is the share when set and the whole amount otherwise`() {
        assertThat(tx(4000, "c-eat", myShare = 1000).ownSpend).isEqualTo(Pence(1000))
        assertThat(tx(4000, "c-eat").ownSpend).isEqualTo(Pence(4000))
    }

    @Test fun `a share equal to the amount is not counted as shared`() {
        assertThat(tx(4000, "c-eat", myShare = 4000).isShared).isFalse()
        assertThat(tx(4000, "c-eat", myShare = 3999).isShared).isTrue()
        assertThat(tx(4000, "c-eat").isShared).isFalse()
    }

    @Test fun `summary totals what was paid and what was mine`() {
        val summary =
            eatingOutSummary(
                listOf(
                    tx(6000, "c-eat", myShare = 1500),
                    tx(2000, "c-eat"),
                    tx(9999, "c-gro"),
                ),
                categories,
            )!!
        assertThat(summary.paid).isEqualTo(Pence(8000))
        assertThat(summary.mine).isEqualTo(Pence(3500))
        assertThat(summary.coveredByOthers).isEqualTo(Pence(4500))
        assertThat(summary.count).isEqualTo(2)
        assertThat(summary.sharedCount).isEqualTo(1)
        assertThat(summary.hasShared).isTrue()
    }

    @Test fun `summary is null without eating out spend`() {
        assertThat(eatingOutSummary(listOf(tx(500, "c-gro")), categories)).isNull()
        assertThat(eatingOutSummary(emptyList(), categories)).isNull()
        assertThat(eatingOutSummary(listOf(tx(500, "c-eat")), listOf(groceries))).isNull()
    }

    @Test fun `unshared meals do not trigger the awareness line`() {
        val summary = eatingOutSummary(listOf(tx(500, "c-eat"), tx(700, "c-eat")), categories)!!
        assertThat(summary.hasShared).isFalse()
        assertThat(summary.mine).isEqualTo(summary.paid)
    }

    @Test fun `the category is found by name, ignoring case`() {
        assertThat(eatingOut.isEatingOut).isTrue()
        assertThat(Category("x", "eating OUT", 1, false, 1).isEatingOut).isTrue()
        assertThat(groceries.isEatingOut).isFalse()
    }

    @Test fun `a share above the amount is rejected when drafting`() {
        assertThrows<IllegalArgumentException> {
            NewTransaction(Pence(1000), "Dishoom", Instant.fromEpochMilliseconds(0), myShare = Pence(1001))
        }
        assertThrows<IllegalArgumentException> {
            NewTransaction(Pence(1000), "Dishoom", Instant.fromEpochMilliseconds(0), myShare = Pence(-1))
        }
        // The edges are valid.
        NewTransaction(Pence(1000), "Dishoom", Instant.fromEpochMilliseconds(0), myShare = Pence(0))
        NewTransaction(Pence(1000), "Dishoom", Instant.fromEpochMilliseconds(0), myShare = Pence(1000))
    }
}
