package com.tally.domain.insight

import com.tally.core.money.Pence
import com.tally.domain.model.Transaction
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/** Number of days in [firstOfMonth]'s month. */
fun LocalDate.lengthOfMonth(): Int {
    val first = LocalDate(year, monthNumber, 1)
    return first.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).dayOfMonth
}

/**
 * Spend per day of [firstOfMonth]'s month (index 0 = the 1st), for the trend
 * chart. Transactions outside that month are ignored. Pure.
 */
fun dailyTotals(
    transactions: List<Transaction>,
    timeZone: TimeZone,
    firstOfMonth: LocalDate,
): List<Pence> {
    val buckets = LongArray(firstOfMonth.lengthOfMonth())
    for (transaction in transactions) {
        val date = transaction.occurredAt.toLocalDateTime(timeZone).date
        if (date.year == firstOfMonth.year && date.monthNumber == firstOfMonth.monthNumber) {
            buckets[date.dayOfMonth - 1] += transaction.amount.value
        }
    }
    return buckets.map { Pence(it) }
}

/**
 * Spend per weekday across [transactions] (index 0 = Monday … 6 = Sunday), for
 * the rhythm chart. Pure.
 */
fun weekdayTotals(
    transactions: List<Transaction>,
    timeZone: TimeZone,
): List<Pence> {
    val buckets = LongArray(7)
    for (transaction in transactions) {
        val weekday = transaction.occurredAt.toLocalDateTime(timeZone).date.dayOfWeek.isoDayNumber
        buckets[weekday - 1] += transaction.amount.value
    }
    return buckets.map { Pence(it) }
}

/**
 * Straight-line projection of the month-end total from the pace so far:
 * `spentSoFar / dayOfMonth * daysInMonth`. Integer-only. Returns [spentSoFar]
 * unchanged if [dayOfMonth] is not positive.
 */
fun forecastMonthEnd(
    spentSoFar: Pence,
    dayOfMonth: Int,
    daysInMonth: Int,
): Pence {
    if (dayOfMonth <= 0) return spentSoFar
    return Pence(spentSoFar.value * daysInMonth / dayOfMonth)
}
