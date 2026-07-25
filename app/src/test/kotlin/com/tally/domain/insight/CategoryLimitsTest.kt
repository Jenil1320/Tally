package com.tally.domain.insight

import com.google.common.truth.Truth.assertThat
import com.tally.core.money.Pence
import com.tally.domain.model.Category
import com.tally.domain.model.Transaction
import com.tally.domain.model.TransactionSource
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test

class CategoryLimitsTest {
    private val categories =
        listOf(
            Category("food", "Eating out", 1, true, 0),
            Category("shop", "Shopping", 3, true, 1),
        )
    private var counter = 0

    private fun tx(
        amount: Long,
        categoryId: String?,
    ) = Transaction(
        id = "t${counter++}",
        amount = Pence(amount),
        merchant = "m",
        note = null,
        categoryId = categoryId,
        occurredAt = Instant.fromEpochMilliseconds(0),
        createdAt = Instant.fromEpochMilliseconds(0),
        source = TransactionSource.MANUAL,
    )

    @Test fun `only categories with a positive limit appear`() {
        val states =
            limitStates(
                transactions = listOf(tx(5000, "food"), tx(9000, "shop")),
                categories = categories,
                limits = mapOf("food" to Pence(10000), "shop" to Pence(0)),
                dayOfMonth = 10,
                daysInMonth = 30,
            )
        assertThat(states.map { it.categoryId }).containsExactly("food")
    }

    @Test fun `remaining and per-day allowance are floored over the days left`() {
        val states =
            limitStates(
                listOf(tx(4000, "food")),
                categories,
                mapOf("food" to Pence(10000)),
                dayOfMonth = 21,
                daysInMonth = 30,
            )
        val food = states.single()
        assertThat(food.remaining.value).isEqualTo(6000L)
        // 10 days left (21..30) -> 6000/10
        assertThat(food.dailyAllowance.value).isEqualTo(600L)
    }

    @Test fun `status reflects position against the limit`() {
        fun statusFor(spent: Long) =
            limitStates(
                listOf(tx(spent, "food")),
                categories,
                mapOf("food" to Pence(10000)),
                dayOfMonth = 15,
                daysInMonth = 30,
            ).single().status

        assertThat(statusFor(5000)).isEqualTo(LimitStatus.WITHIN)
        assertThat(statusFor(8000)).isEqualTo(LimitStatus.NEAR)
        assertThat(statusFor(10000)).isEqualTo(LimitStatus.NEAR)
        assertThat(statusFor(10001)).isEqualTo(LimitStatus.PAST)
    }

    @Test fun `past the limit gives negative remaining and no daily allowance`() {
        val food =
            limitStates(
                listOf(tx(12000, "food")),
                categories,
                mapOf("food" to Pence(10000)),
                dayOfMonth = 15,
                daysInMonth = 30,
            ).single()
        assertThat(food.remaining.value).isEqualTo(-2000L)
        assertThat(food.dailyAllowance.value).isEqualTo(0L)
        assertThat(food.fillFraction).isEqualTo(1f)
    }

    @Test fun `pace fraction tracks progress through the month`() {
        val food =
            limitStates(
                listOf(tx(1000, "food")),
                categories,
                mapOf("food" to Pence(10000)),
                dayOfMonth = 15,
                daysInMonth = 30,
            ).single()
        assertThat(food.paceFraction).isEqualTo(0.5f)
    }

    @Test fun `projection flags a limit that the current pace passes`() {
        // £60 by day 10 of 30 projects to £180 against a £100 limit.
        val food =
            limitStates(
                listOf(tx(6000, "food")),
                categories,
                mapOf("food" to Pence(10000)),
                dayOfMonth = 10,
                daysInMonth = 30,
            ).single()
        assertThat(food.projected.value).isEqualTo(18000L)
        assertThat(food.projectedPast).isTrue()
        assertThat(food.status).isEqualTo(LimitStatus.WITHIN)
    }

    @Test fun `fullest category is listed first`() {
        val states =
            limitStates(
                listOf(tx(2000, "food"), tx(9000, "shop")),
                categories,
                mapOf("food" to Pence(10000), "shop" to Pence(10000)),
                dayOfMonth = 10,
                daysInMonth = 30,
            )
        assertThat(states.map { it.categoryId }).containsExactly("shop", "food").inOrder()
    }

    @Test fun `no limits means nothing to show`() {
        assertThat(limitStates(listOf(tx(2000, "food")), categories, emptyMap(), 10, 30)).isEmpty()
    }
}
