package com.tally.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tally.core.money.Pence
import com.tally.core.time.Clock
import com.tally.domain.insight.CategorySpend
import com.tally.domain.insight.EatingOutSummary
import com.tally.domain.insight.categoryBreakdown
import com.tally.domain.insight.eatingOutSummary
import com.tally.domain.insight.forecastMonthEnd
import com.tally.domain.insight.lengthOfMonth
import com.tally.domain.model.Category
import com.tally.domain.model.Transaction
import com.tally.domain.repo.CategoryRepository
import com.tally.domain.repo.TransactionRepository
import com.tally.ui.components.TransactionRowUi
import com.tally.ui.model.dayLabel
import com.tally.ui.model.timeLabel
import com.tally.ui.model.toRowUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import kotlin.math.min

/**
 * State for the Home screen: this month at a glance.
 *
 * Every total here is cash flow, the full amount paid. [eatingOut] is the one
 * awareness figure: what the user's own share of eating out came to (ADR-0010).
 *
 * @property deltaPercent this month so far against the same point last month,
 *   signed (+ means more). Null when last month has nothing to compare to.
 * @property forecastGap the month-end forecast minus last month's full total,
 *   signed (negative means under). Null when last month has no spend.
 */
data class HomeUiState(
    val dateKicker: String = "",
    val monthName: String = "",
    val lastMonthName: String = "",
    val monthTotal: Pence = Pence.ZERO,
    val deltaPercent: Int? = null,
    val daysLeft: Int = 0,
    val breakdown: List<CategorySpend> = emptyList(),
    val spentToday: Pence = Pence.ZERO,
    val todayCount: Int = 0,
    val forecast: Pence = Pence.ZERO,
    val forecastGap: Pence? = null,
    val eatingOut: EatingOutSummary? = null,
    val recent: List<TransactionRowUi> = emptyList(),
    val hasAnyTransactions: Boolean = false,
    val loaded: Boolean = false,
)

private const val RECENT_COUNT = 5

/**
 * Builds [HomeUiState] from the whole ledger. Pure (no Android, no clock): [today]
 * and [timeZone] are passed in, so it is unit-tested directly.
 */
internal fun homeState(
    transactions: List<Transaction>,
    categories: List<Category>,
    today: LocalDate,
    timeZone: TimeZone,
): HomeUiState {
    val firstOfMonth = LocalDate(today.year, today.monthNumber, 1)
    val prevFirst = firstOfMonth.minus(1, DateTimeUnit.MONTH)
    val daysInMonth = firstOfMonth.lengthOfMonth()
    val byId = categories.associateBy { it.id }

    val dated = transactions.map { it to it.occurredAt.toLocalDateTime(timeZone).date }
    val monthTx = dated.filter { (_, date) -> date >= firstOfMonth }.map { it.first }
    val todayTx = dated.filter { (_, date) -> date == today }.map { it.first }

    val prevMonthTotal = Pence(dated.filter { (_, date) -> date >= prevFirst && date < firstOfMonth }.sumOf { it.first.amount.value })
    // Compare like with like: last month only up to the same day number.
    val prevCutoff = prevFirst.plus(min(today.dayOfMonth, prevFirst.lengthOfMonth()), DateTimeUnit.DAY)
    val prevToDate = Pence(dated.filter { (_, date) -> date >= prevFirst && date < prevCutoff }.sumOf { it.first.amount.value })

    val monthTotal = Pence(monthTx.sumOf { it.amount.value })
    val forecast = forecastMonthEnd(monthTotal, today.dayOfMonth, daysInMonth)

    return HomeUiState(
        dateKicker = "${weekdayName(today)} · ${today.dayOfMonth} ${monthName(today)}",
        monthName = monthName(today),
        lastMonthName = monthName(prevFirst),
        monthTotal = monthTotal,
        deltaPercent = prevToDate.takeIf { it.value > 0 }?.let { ((monthTotal.value - it.value) * 100 / it.value).toInt() },
        daysLeft = daysInMonth - today.dayOfMonth,
        breakdown = categoryBreakdown(monthTx, categories),
        spentToday = Pence(todayTx.sumOf { it.amount.value }),
        todayCount = todayTx.size,
        forecast = forecast,
        forecastGap = prevMonthTotal.takeIf { it.value > 0 }?.let { forecast - it },
        eatingOut = eatingOutSummary(monthTx, categories),
        recent =
            dated.take(RECENT_COUNT).map { (transaction, date) ->
                val category = byId[transaction.categoryId]
                transaction.toRowUi(
                    category = category,
                    subtitle =
                        "${category?.name ?: "Uncategorised"} · ${dayLabel(date, today)}, " +
                            transaction.occurredAt.timeLabel(timeZone),
                )
            },
        hasAnyTransactions = transactions.isNotEmpty(),
        loaded = true,
    )
}

private fun monthName(date: LocalDate): String = date.month.name.lowercase().replaceFirstChar { it.uppercase() }

private fun weekdayName(date: LocalDate): String = date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }

/** Observes the ledger and reduces it to this month's snapshot. */
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        transactionRepository: TransactionRepository,
        categoryRepository: CategoryRepository,
        private val clock: Clock,
    ) : ViewModel() {
        private val timeZone = TimeZone.currentSystemDefault()
        private val today = MutableStateFlow(clock.now().toLocalDateTime(timeZone).date)

        /** Re-reads the date, so the month rolls over when the app is opened after midnight. */
        fun onResume() {
            today.value = clock.now().toLocalDateTime(timeZone).date
        }

        val uiState: StateFlow<HomeUiState> =
            combine(
                transactionRepository.observeAll(),
                categoryRepository.observeAll(),
                today,
            ) { transactions, categories, date ->
                homeState(transactions, categories, date, timeZone)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
    }
