package com.tally.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tally.core.money.Pence
import com.tally.core.time.Clock
import com.tally.di.DefaultDispatcher
import com.tally.domain.insight.CategoryLimitStore
import com.tally.domain.insight.CategorySpend
import com.tally.domain.insight.LimitState
import com.tally.domain.insight.MerchantTotal
import com.tally.domain.insight.categoryBreakdown
import com.tally.domain.insight.dailyTotals
import com.tally.domain.insight.forecastMonthEnd
import com.tally.domain.insight.lengthOfMonth
import com.tally.domain.insight.limitStates
import com.tally.domain.insight.merchantTotals
import com.tally.domain.insight.weekdayTotals
import com.tally.domain.repo.CategoryRepository
import com.tally.domain.repo.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

/** A toggleable filter chip for one category present this month. */
data class CategoryChip(
    val id: String?,
    val name: String,
    val colorIndex: Int?,
    val included: Boolean,
)

data class InsightsUiState(
    val monthLabel: String = "",
    val total: Pence = Pence.ZERO,
    val forecast: Pence = Pence.ZERO,
    val transactionCount: Int = 0,
    val breakdown: List<CategorySpend> = emptyList(),
    // Trend: spend per day of the month (index 0 = the 1st).
    val dailyTotals: List<Pence> = emptyList(),
    val todayIndex: Int = -1,
    // Rhythm: spend per weekday (index 0 = Monday).
    val weekdayTotals: List<Pence> = emptyList(),
    val peakWeekdayIndex: Int = -1,
    // Filter chips for the categories present this month.
    val chips: List<CategoryChip> = emptyList(),
    // Categories the user set a monthly limit on, fullest first.
    val limits: List<LimitState> = emptyList(),
    // Where the money goes by merchant — the small-and-often view.
    val topMerchants: List<MerchantTotal> = emptyList(),
    val monthHasTransactions: Boolean = false,
    val loaded: Boolean = false,
)

/**
 * This month's analysis, re-sliced by a category filter. All figures (total,
 * forecast, breakdown, trend, rhythm) reflect only the *included* categories;
 * toggling a chip excludes that category. Computed on the default dispatcher.
 */
@HiltViewModel
class InsightsViewModel
    @Inject
    constructor(
        transactionRepository: TransactionRepository,
        categoryRepository: CategoryRepository,
        categoryLimitStore: CategoryLimitStore,
        clock: Clock,
        @DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        private val timeZone = TimeZone.currentSystemDefault()
        private val excludedCategoryIds = MutableStateFlow<Set<String?>>(emptySet())

        /** Include/exclude a category from the analysis. */
        fun toggleCategory(categoryId: String?) {
            excludedCategoryIds.update { current ->
                if (categoryId in current) current - categoryId else current + categoryId
            }
        }

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
                    excludedCategoryIds,
                    categoryLimitStore.limits,
                ) { transactions, categories, excluded, limits ->
                    // Chips reflect every category present this month, ordered by
                    // total spend (unfiltered) so toggling one off doesn't reorder them.
                    val chips =
                        categoryBreakdown(transactions, categories).map { spend ->
                            CategoryChip(spend.categoryId, spend.name, spend.colorIndex, spend.categoryId !in excluded)
                        }

                    val filtered = transactions.filter { it.categoryId !in excluded }
                    val total = filtered.fold(Pence.ZERO) { acc, t -> acc + t.amount }
                    val weekday = weekdayTotals(filtered, timeZone)

                    InsightsUiState(
                        monthLabel = "$monthName ${date.year}",
                        total = total,
                        forecast = forecastMonthEnd(total, date.dayOfMonth, firstOfMonth.lengthOfMonth()),
                        transactionCount = filtered.size,
                        breakdown = categoryBreakdown(filtered, categories),
                        dailyTotals = dailyTotals(filtered, timeZone, firstOfMonth),
                        todayIndex = date.dayOfMonth - 1,
                        weekdayTotals = weekday,
                        peakWeekdayIndex = weekday.indices.maxByOrNull { weekday[it].value } ?: -1,
                        chips = chips,
                        // Limits measure the month as it really is, so they ignore
                        // the category filter (which is only a view of the charts).
                        limits =
                            limitStates(
                                transactions = transactions,
                                categories = categories,
                                limits = limits,
                                dayOfMonth = date.dayOfMonth,
                                daysInMonth = firstOfMonth.lengthOfMonth(),
                            ),
                        topMerchants = merchantTotals(filtered),
                        monthHasTransactions = transactions.isNotEmpty(),
                        loaded = true,
                    )
                }.flowOn(defaultDispatcher)
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsUiState())
            }
    }
