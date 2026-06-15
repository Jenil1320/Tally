package com.tally.domain.insight

import com.google.common.truth.Truth.assertThat
import com.tally.core.money.Pence
import com.tally.domain.model.Category
import com.tally.domain.model.Transaction
import com.tally.domain.model.TransactionSource
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test

class CategoryBreakdownTest {
    private val categories =
        listOf(
            Category("c1", "Eating out", 1, true, 0),
            Category("c2", "Transport", 2, true, 1),
        )

    private fun tx(
        amount: Long,
        categoryId: String?,
    ) = Transaction(
        id = "x",
        amount = Pence(amount),
        merchant = "m",
        note = null,
        categoryId = categoryId,
        occurredAt = Instant.fromEpochMilliseconds(0),
        createdAt = Instant.fromEpochMilliseconds(0),
        source = TransactionSource.MANUAL,
    )

    @Test fun `empty input gives empty breakdown`() {
        assertThat(categoryBreakdown(emptyList(), categories)).isEmpty()
    }

    @Test fun `sums per category, largest first, with fractions`() {
        val breakdown =
            categoryBreakdown(
                listOf(tx(400, "c1"), tx(200, "c1"), tx(900, "c2")),
                categories,
            )
        assertThat(breakdown.map { it.name }).containsExactly("Transport", "Eating out").inOrder()
        assertThat(breakdown[0].total).isEqualTo(Pence(900))
        assertThat(breakdown[0].fraction).isWithin(0.001f).of(0.6f)
        assertThat(breakdown[1].total).isEqualTo(Pence(600))
        assertThat(breakdown[1].fraction).isWithin(0.001f).of(0.4f)
    }

    @Test fun `uncategorised spend collapses into one slice`() {
        val breakdown = categoryBreakdown(listOf(tx(500, null), tx(500, "c1")), categories)
        val uncategorised = breakdown.first { it.categoryId == null }
        assertThat(uncategorised.name).isEqualTo("Uncategorised")
        assertThat(uncategorised.colorIndex).isNull()
        assertThat(uncategorised.total).isEqualTo(Pence(500))
    }
}
