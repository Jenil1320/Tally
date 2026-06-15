package com.tally.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tally.core.money.Pence
import com.tally.core.time.Clock
import com.tally.di.DefaultDispatcher
import com.tally.domain.insight.CategorySpend
import com.tally.domain.insight.categoryBreakdown
import com.tally.domain.insight.dailyTotals
import com.tally.domain.insight.forecastMonthEnd
import com.tally.domain.insight.lengthOfMonth
import com.tally.domain.insight.weekdayTotals
import com.tally.domain.repo.CategoryRepository
import com.tally.domain.repo.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

data class InsightsUiState(
    val monthLabel: String = "",
    val total: Pence = Pence.ZERO,
    val forecast: Pence = Pence.ZERO,
    val breakdown: List<CategorySpend> = emptyList(),
    // Trend: spend per day of the month (index 0 = the 1st).
    val dailyTotals: List<Pence> = emptyList(),
    val todayIndex: Int = -1,
    // Rhythm: spend per weekday (index 0 = Monday).
    val weekdayTotals: List<Pence> = emptyList(),
    val peakWeekdayIndex: Int = -1,
    val loaded: Boolean = false,
)

/** This month's total, forecast, category breakdown, daily trend, and weekday rhythm. */
@HiltViewModel
class InsightsViewModel
    @Inject
    constructor(
        transactionRepository: TransactionRepository,
        categoryRepository: CategoryRepository,
        clock: Clock,
        @DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        private val timeZone = TimeZone.currentSystemDefault()

        val uiState: StateFlow<InsightsUiState> =
            run {
                val date = clock.now().toLocalDateTime(timeZone).date
                val firstOfMonth = LocalDate(date.year, date.monthNumber, 1)
                val monthStart = firstOfMonth.atStartOfDayIn(timeZone)
                val monthEnd = firstOfMonth.plus(1, DateTimeUnit.MONTH).atStartOfDayIn(timeZone)
                val monthName = date.month.name.lowercase().replaceFirstChar { it.uppercase() }
                combine(
                    transactionRepository.observeBetween(monthStart, monthEnd),
                    categoryRepository.observeAll(),
                ) { transactions, categories ->
                    val total = transactions.fold(Pence.ZERO) { acc, t -> acc + t.amount }
                    val weekday = weekdayTotals(transactions, timeZone)
                    InsightsUiState(
                        monthLabel = "$monthName ${date.year}",
                        total = total,
                        forecast = forecastMonthEnd(total, date.dayOfMonth, firstOfMonth.lengthOfMonth()),
                        breakdown = categoryBreakdown(transactions, categories),
                        dailyTotals = dailyTotals(transactions, timeZone, firstOfMonth),
                        todayIndex = date.dayOfMonth - 1,
                        weekdayTotals = weekday,
                        peakWeekdayIndex = weekday.indices.maxByOrNull { weekday[it].value } ?: -1,
                        loaded = true,
                    )
                }.flowOn(defaultDispatcher)
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsUiState())
            }
    }
