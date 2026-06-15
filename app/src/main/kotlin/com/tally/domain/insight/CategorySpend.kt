package com.tally.domain.insight

import com.tally.core.money.Pence
import com.tally.domain.model.Category
import com.tally.domain.model.Transaction

/**
 * One slice of the category breakdown.
 *
 * @property fraction this category's share of the grand total, `0f..1f`
 *   (0 when the grand total is not positive), used to size chart slices.
 */
data class CategorySpend(
    val categoryId: String?,
    val name: String,
    val colorIndex: Int?,
    val total: Pence,
    val fraction: Float,
)

/**
 * Sums [transactions] by category and returns the breakdown, largest first,
 * with each slice's [CategorySpend.fraction] of the grand total. Pure — no
 * Android, no time — so it is unit-tested directly and runs on the default
 * dispatcher. Uncategorised spend collapses into a single "Uncategorised" slice.
 */
fun categoryBreakdown(
    transactions: List<Transaction>,
    categories: List<Category>,
): List<CategorySpend> {
    if (transactions.isEmpty()) return emptyList()
    val byId = categories.associateBy { it.id }
    val totals = LinkedHashMap<String?, Long>()
    for (transaction in transactions) {
        totals[transaction.categoryId] = (totals[transaction.categoryId] ?: 0L) + transaction.amount.value
    }
    val grandTotal = totals.values.sum()
    return totals.entries
        .map { (categoryId, sum) ->
            val category = categoryId?.let { byId[it] }
            CategorySpend(
                categoryId = categoryId,
                name = category?.name ?: "Uncategorised",
                colorIndex = category?.colorIndex,
                total = Pence(sum),
                fraction = if (grandTotal > 0L) (sum.toFloat() / grandTotal).coerceIn(0f, 1f) else 0f,
            )
        }
        .sortedByDescending { it.total.value }
}
