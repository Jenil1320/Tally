package com.tally.domain.insight

import com.tally.core.money.Pence
import com.tally.domain.model.Category
import com.tally.domain.model.Transaction

/** The name of the seeded category that asks "what was your share?" when it is chosen. */
const val EATING_OUT_CATEGORY_NAME = "Eating out"

/** True for the Eating out category. Matches by name because ids are minted per install. */
val Category.isEatingOut: Boolean get() = name.equals(EATING_OUT_CATEGORY_NAME, ignoreCase = true)

/**
 * What eating out cost, two ways.
 *
 * @property paid everything paid for eating out. This is the cash flow figure.
 * @property mine the part that was the user's own spend. It is never more than [paid].
 * @property count how many eating-out transactions there are.
 * @property sharedCount how many of them have a recorded share below the amount paid.
 */
data class EatingOutSummary(
    val paid: Pence,
    val mine: Pence,
    val count: Int,
    val sharedCount: Int,
) {
    /** What other people covered. Zero when nothing was shared. */
    val coveredByOthers: Pence get() = paid - mine

    /** True when at least one meal was shared, so the "your share" line is worth showing. */
    val hasShared: Boolean get() = sharedCount > 0
}

/**
 * Totals the Eating out transactions, paid and own share. Returns null when there
 * are none. A transaction with no recorded share counts in full as the user's
 * own spend. Pure, so it is unit-tested directly.
 */
fun eatingOutSummary(
    transactions: List<Transaction>,
    categories: List<Category>,
): EatingOutSummary? {
    val eatingOutIds = categories.filter { it.isEatingOut }.map { it.id }.toSet()
    if (eatingOutIds.isEmpty()) return null
    val meals = transactions.filter { it.categoryId in eatingOutIds }
    if (meals.isEmpty()) return null
    return EatingOutSummary(
        paid = Pence(meals.sumOf { it.amount.value }),
        mine = Pence(meals.sumOf { it.ownSpend.value }),
        count = meals.size,
        sharedCount = meals.count { it.isShared },
    )
}
