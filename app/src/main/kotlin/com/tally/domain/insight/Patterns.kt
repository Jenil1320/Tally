package com.tally.domain.insight

import com.tally.core.money.Pence
import com.tally.domain.model.Category
import com.tally.domain.model.Transaction
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToLong
import kotlin.math.sqrt

/*
 * Patterns over time: the pure analytics behind the Insights tab. Home shows the
 * month right now and Reports summarises one finished month, so these answer the
 * questions neither can: how this month compares with the ones before, what a
 * normal week looks like, and what stood out. Everything is cash flow (the full
 * amount paid), except [eatingOutByMonth], which also carries the owner's share.
 * No Android, no clock: dates and zones are passed in, so each is unit-tested.
 */

/** How many months the trend looks back, including the current one. */
const val TREND_MONTHS = 6

private val SHORT_MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
private val FULL_MONTHS =
    listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December",
    )

/** One calendar month's spend. [fullLabel] reads "September 2026". */
data class MonthTotal(
    val firstOfMonth: LocalDate,
    val shortLabel: String,
    val fullLabel: String,
    val total: Pence,
)

private fun LocalDate.firstOfMonth(): LocalDate = LocalDate(year, monthNumber, 1)

private fun monthTotal(
    first: LocalDate,
    total: Long,
) = MonthTotal(
    firstOfMonth = first,
    shortLabel = SHORT_MONTHS[first.monthNumber - 1],
    fullLabel = "${FULL_MONTHS[first.monthNumber - 1]} ${first.year}",
    total = Pence(total),
)

/**
 * Spend for each of the [count] months ending at [currentMonth], oldest first. A
 * month with no spend is present with a zero total, so the chart keeps its shape.
 */
fun monthlyTotals(
    transactions: List<Transaction>,
    timeZone: TimeZone,
    currentMonth: LocalDate,
    count: Int = TREND_MONTHS,
): List<MonthTotal> {
    val sums = HashMap<LocalDate, Long>()
    for (transaction in transactions) {
        val key = transaction.occurredAt.toLocalDateTime(timeZone).date.firstOfMonth()
        sums[key] = (sums[key] ?: 0L) + transaction.amount.value
    }
    return (count - 1 downTo 0).map { back ->
        val first = currentMonth.firstOfMonth().minus(back, DateTimeUnit.MONTH)
        monthTotal(first, sums[first] ?: 0L)
    }
}

/**
 * The average spend for each weekday (index 0 = Monday), per occurrence of that
 * weekday between [from] and [to] inclusive. Dividing by how many Mondays (and so
 * on) the window holds makes the weekdays comparable, however the window falls.
 * Transactions outside the window are ignored.
 */
fun weekdayAverages(
    transactions: List<Transaction>,
    timeZone: TimeZone,
    from: LocalDate,
    to: LocalDate,
): List<Pence> {
    val totals = LongArray(7)
    val days = IntArray(7)
    var day = from
    while (day <= to) {
        days[day.dayOfWeek.isoDayNumber - 1]++
        day = day.plus(1, DateTimeUnit.DAY)
    }
    for (transaction in transactions) {
        val date = transaction.occurredAt.toLocalDateTime(timeZone).date
        if (date in from..to) totals[date.dayOfWeek.isoDayNumber - 1] += transaction.amount.value
    }
    return (0..6).map { Pence(if (days[it] > 0) totals[it] / days[it] else 0L) }
}

/** What an ordinary weekday and an ordinary weekend day cost, on average. */
data class WeekRhythm(
    val weekdayAverage: Pence,
    val weekendAverage: Pence,
) {
    /** True when a weekend day costs more than a weekday. */
    val weekendHeavier: Boolean get() = weekendAverage > weekdayAverage
}

/** Splits seven [averages] (Monday first) into the weekday and weekend figure. Null for any other size. */
fun weekRhythm(averages: List<Pence>): WeekRhythm? {
    if (averages.size != 7) return null
    return WeekRhythm(
        weekdayAverage = Pence(averages.take(5).sumOf { it.value } / 5),
        weekendAverage = Pence(averages.drop(5).sumOf { it.value } / 2),
    )
}

/**
 * A purchase well above what is usual in its category.
 *
 * @property usual the category's usual amount, from everything else in the window.
 * @property times how many times [usual] this purchase was.
 */
data class UnusualSpend(
    val transactionId: String,
    val merchant: String,
    val categoryName: String,
    val occurredAt: Instant,
    val amount: Pence,
    val usual: Pence,
    val times: Float,
)

/** Fewest other purchases a category needs before one can be called unusual. */
private const val MIN_HISTORY = 5
private const val SPREADS_ABOVE = 2.0
private const val MIN_TIMES_USUAL = 1.5

/**
 * Purchases on or after [since] that sit more than two standard deviations above
 * their category's usual amount, and at least 1.5 times it. The usual amount and
 * spread come from every other purchase in [transactions] for that category, so a
 * big purchase never hides itself, and a category needs [MIN_HISTORY] others
 * before anything counts. Uncategorised spend is skipped. Biggest ratio first,
 * up to [limit]. Pure.
 */
fun unusualSpends(
    transactions: List<Transaction>,
    categories: List<Category>,
    since: Instant,
    limit: Int = 3,
): List<UnusualSpend> {
    val names = categories.associate { it.id to it.name }
    return transactions
        .filter { it.categoryId != null && it.amount.value > 0 }
        .groupBy { it.categoryId }
        .flatMap { (categoryId, group) ->
            if (group.size <= MIN_HISTORY) return@flatMap emptyList()
            val sum = group.sumOf { it.amount.value }
            val sumOfSquares = group.sumOf { it.amount.value.toDouble() * it.amount.value }
            group.filter { it.occurredAt >= since }.mapNotNull { candidate ->
                val amount = candidate.amount.value
                val others = group.size - 1
                val mean = (sum - amount).toDouble() / others
                val variance = (sumOfSquares - amount.toDouble() * amount) / others - mean * mean
                val spread = sqrt(variance.coerceAtLeast(0.0))
                if (mean > 0 && amount > mean + SPREADS_ABOVE * spread && amount >= MIN_TIMES_USUAL * mean) {
                    UnusualSpend(
                        transactionId = candidate.id,
                        merchant = candidate.merchant,
                        categoryName = names[categoryId].orEmpty(),
                        occurredAt = candidate.occurredAt,
                        amount = candidate.amount,
                        usual = Pence(mean.roundToLong()),
                        times = (amount / mean).toFloat(),
                    )
                } else {
                    null
                }
            }
        }
        .sortedByDescending { it.times }
        .take(limit)
}

/** Eating out for one month: what was paid, and the user's own share of it. */
data class EatingOutMonth(
    val firstOfMonth: LocalDate,
    val label: String,
    val paid: Pence,
    val mine: Pence,
    val hasShared: Boolean,
)

/**
 * Eating out per month for the [count] months ending at [currentMonth], newest
 * first. Months with no eating out are left out. [EatingOutMonth.paid] is cash
 * flow; [EatingOutMonth.mine] is the awareness figure (ADR-0010).
 */
fun eatingOutByMonth(
    transactions: List<Transaction>,
    categories: List<Category>,
    timeZone: TimeZone,
    currentMonth: LocalDate,
    count: Int = TREND_MONTHS,
): List<EatingOutMonth> {
    val byMonth = transactions.groupBy { it.occurredAt.toLocalDateTime(timeZone).date.firstOfMonth() }
    return (0 until count).mapNotNull { back ->
        val first = currentMonth.firstOfMonth().minus(back, DateTimeUnit.MONTH)
        val summary = eatingOutSummary(byMonth[first].orEmpty(), categories) ?: return@mapNotNull null
        EatingOutMonth(
            firstOfMonth = first,
            label = "${FULL_MONTHS[first.monthNumber - 1]} ${first.year}",
            paid = summary.paid,
            mine = summary.mine,
            hasShared = summary.hasShared,
        )
    }
}
