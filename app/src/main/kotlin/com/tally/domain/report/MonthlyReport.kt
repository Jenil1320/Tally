package com.tally.domain.report

import com.tally.core.money.Pence
import com.tally.domain.insight.CategorySpend
import com.tally.domain.insight.EatingOutSummary
import com.tally.domain.insight.categoryBreakdown
import com.tally.domain.insight.eatingOutSummary
import com.tally.domain.insight.lengthOfMonth
import com.tally.domain.model.Category
import com.tally.domain.model.Transaction
import com.tally.domain.model.TransactionSource
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** A merchant and what was spent there in the month. */
data class MerchantSpend(
    val merchant: String,
    val total: Pence,
    val visits: Int,
)

/** A calendar day and its spend. */
data class DaySpend(
    val date: LocalDate,
    val total: Pence,
)

/** This month against the one before it. [deltaPercent] is signed (+ = spent more). */
data class MonthDelta(
    val previousTotal: Pence,
    val deltaPercent: Int,
)

/**
 * A finished (or in-progress) month's spending summary — the content of the
 * Reports tab and the end-of-month notification. Pure data; built by
 * [monthlyReport], which does no Android/IO work and is unit-tested directly.
 */
data class MonthlyReport(
    val monthLabel: String,
    val total: Pence,
    val transactionCount: Int,
    val byCategory: List<CategorySpend>,
    val topMerchant: MerchantSpend?,
    val priciestDay: DaySpend?,
    val avgPerDay: Pence,
    val autoCapturedPercent: Int,
    val vsPrevious: MonthDelta?,
    val eatingOut: EatingOutSummary? = null,
) {
    val isEmpty: Boolean get() = transactionCount == 0
}

private val MONTH_NAMES =
    listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December",
    )

/** "June 2026" for the 1st-of-month [date]. */
fun monthLabel(date: LocalDate): String = "${MONTH_NAMES[date.monthNumber - 1]} ${date.year}"

/**
 * Summarises [transactions] (already scoped to [firstOfMonth]'s month) into a
 * [MonthlyReport]. [previousMonthTotal] drives the month-over-month delta (pass
 * null or zero to omit it). Merchants are grouped case-insensitively; the
 * average is over the whole month's length. Totals are cash flow, the full
 * amount paid; [MonthlyReport.eatingOut] is the awareness figure for the user's
 * own share of eating out (ADR-0010). Pure and deterministic.
 */
fun monthlyReport(
    transactions: List<Transaction>,
    categories: List<Category>,
    previousMonthTotal: Pence?,
    firstOfMonth: LocalDate,
    timeZone: TimeZone,
): MonthlyReport {
    val total = Pence(transactions.sumOf { it.amount.value })
    val count = transactions.size

    val topMerchant =
        transactions
            .groupBy { it.merchant.trim().lowercase() }
            .values
            .map { group ->
                MerchantSpend(
                    merchant = group.first().merchant.trim(),
                    total = Pence(group.sumOf { it.amount.value }),
                    visits = group.size,
                )
            }
            .maxByOrNull { it.total.value }

    val priciestDay =
        transactions
            .groupBy { it.occurredAt.toLocalDateTime(timeZone).date }
            .map { (date, group) -> DaySpend(date, Pence(group.sumOf { it.amount.value })) }
            .maxByOrNull { it.total.value }

    val daysInMonth = firstOfMonth.lengthOfMonth()
    val avgPerDay = if (daysInMonth > 0) Pence(total.value / daysInMonth) else Pence.ZERO

    val autoCapturedPercent =
        if (count > 0) transactions.count { it.source == TransactionSource.NOTIFICATION } * 100 / count else 0

    val vsPrevious =
        previousMonthTotal
            ?.takeIf { it.value > 0 }
            ?.let { prev -> MonthDelta(prev, ((total.value - prev.value) * 100 / prev.value).toInt()) }

    return MonthlyReport(
        monthLabel = monthLabel(firstOfMonth),
        total = total,
        transactionCount = count,
        byCategory = categoryBreakdown(transactions, categories),
        topMerchant = topMerchant,
        priciestDay = priciestDay,
        avgPerDay = avgPerDay,
        autoCapturedPercent = autoCapturedPercent,
        vsPrevious = vsPrevious,
        eatingOut = eatingOutSummary(transactions, categories),
    )
}
