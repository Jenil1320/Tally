package com.tally.domain.insight

import com.tally.core.money.Pence
import com.tally.domain.model.Category
import com.tally.domain.model.Transaction

/** Where a category sits against its limit. Naming is descriptive, never a verdict. */
enum class LimitStatus {
    /** Comfortably inside the limit. */
    WITHIN,

    /** At or past [NEAR_THRESHOLD_PERCENT] of the limit, but not past it. */
    NEAR,

    /** Spending has passed the limit. */
    PAST,
}

/** The share of a limit at which a category is called [LimitStatus.NEAR]. */
const val NEAR_THRESHOLD_PERCENT = 80

/**
 * One category measured against the limit the user set for it.
 *
 * @property remaining limit minus spend; negative once spending passes the limit.
 * @property dailyAllowance what is left, spread over the days left in the month
 *   (floored, so the stated rate is never optimistic). Zero once past the limit.
 * @property projected month-end total at the current pace.
 * @property fillFraction spend as a share of the limit, `0f..1f`, for the bar.
 * @property paceFraction how far through the month it is, `0f..1f`, for the tick
 *   — spending sits "on pace" when the fill is level with the tick.
 */
data class LimitState(
    val categoryId: String,
    val name: String,
    val colorIndex: Int?,
    val limit: Pence,
    val spent: Pence,
    val remaining: Pence,
    val dailyAllowance: Pence,
    val projected: Pence,
    val fillFraction: Float,
    val paceFraction: Float,
    val status: LimitStatus,
) {
    /** True when the current pace lands past the limit by month end. */
    val projectedPast: Boolean get() = projected.value > limit.value
}

/**
 * Measures each limited category's spend this month against its limit.
 *
 * A limit is a pre-commitment made in a calm moment that then informs decisions
 * later in the month; restating what is left as a **per-day** figure is what
 * makes it usable at the till. Only categories with a limit are returned, most
 * pressing (fullest) first. Pure — integer pence, no clock, no Android — so it
 * is unit-tested directly. Ratios are Float for display only.
 *
 * @param transactions this month's transactions.
 * @param limits categoryId → limit; entries of zero or less are ignored.
 * @param dayOfMonth today's day (1-based).
 * @param daysInMonth length of the month.
 */
fun limitStates(
    transactions: List<Transaction>,
    categories: List<Category>,
    limits: Map<String, Pence>,
    dayOfMonth: Int,
    daysInMonth: Int,
): List<LimitState> {
    if (limits.isEmpty()) return emptyList()
    val spentByCategory = transactions.groupBy { it.categoryId }.mapValues { (_, txs) -> txs.sumOf { it.amount.value } }
    val daysLeft = (daysInMonth - dayOfMonth + 1).coerceAtLeast(1)

    return categories
        .mapNotNull { category ->
            val limit = limits[category.id]?.takeIf { it.value > 0L } ?: return@mapNotNull null
            val spent = Pence(spentByCategory[category.id] ?: 0L)
            val remaining = Pence(limit.value - spent.value)
            val status =
                when {
                    spent.value > limit.value -> LimitStatus.PAST
                    spent.value * 100 >= limit.value * NEAR_THRESHOLD_PERCENT -> LimitStatus.NEAR
                    else -> LimitStatus.WITHIN
                }
            LimitState(
                categoryId = category.id,
                name = category.name,
                colorIndex = category.colorIndex,
                limit = limit,
                spent = spent,
                remaining = remaining,
                dailyAllowance = Pence(remaining.value.coerceAtLeast(0L) / daysLeft),
                projected = forecastMonthEnd(spent, dayOfMonth, daysInMonth),
                fillFraction = (spent.value.toFloat() / limit.value).coerceIn(0f, 1f),
                paceFraction = (dayOfMonth.toFloat() / daysInMonth).coerceIn(0f, 1f),
                status = status,
            )
        }
        .sortedByDescending { it.spent.value.toFloat() / it.limit.value }
}
