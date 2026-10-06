package com.tally.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tally.core.money.Pence
import com.tally.core.time.Clock
import com.tally.di.DefaultDispatcher
import com.tally.domain.insight.CategoryLimitStore
import com.tally.domain.insight.EatingOutMonth
import com.tally.domain.insight.LimitState
import com.tally.domain.insight.MerchantTotal
import com.tally.domain.insight.MonthTotal
import com.tally.domain.insight.TREND_MONTHS
import com.tally.domain.insight.UnusualSpend
import com.tally.domain.insight.WeekRhythm
import com.tally.domain.insight.categoryBreakdown
import com.tally.domain.insight.dailyTotals
import com.tally.domain.insight.eatingOutByMonth
import com.tally.domain.insight.forecastMonthEnd
import com.tally.domain.insight.lengthOfMonth
import com.tally.domain.insight.limitStates
import com.tally.domain.insight.merchantTotals
import com.tally.domain.insight.monthlyTotals
import com.tally.domain.insight.unusualSpends
import com.tally.domain.insight.weekRhythm
import com.tally.domain.insight.weekdayAverages
import com.tally.domain.repo.CategoryRepository
import com.tally.domain.repo.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

/** A toggleable filter chip for one category present in the last six months. */
data class CategoryChip(
    val id: String?,
    val name: String,
    val colorIndex: Int?,
    val included: Boolean,
)

/**
 * What the Insights tab shows: patterns over time (see `domain/insight/Patterns.kt`).
 * Everything is cash flow, the full amount paid, except [eatingOutMonths], which
 * also carries the owner's own share (ADR-0010).
 *
 * The category filter ([chips]) re-slices [trend], [dailyTotals], [weekdayAverages],
 * [rhythm], [topMerchants], [unusual] and [eatingOutMonths]. [limits] always
 * measure the month as it really is.
 *
 * @property usualMonth the average of the earlier months in [trend] that had any
 *   spend, or null when there is none to compare with.
 * @property transactionCount this month's transactions, after the filter.
 * @property hasData false only when the six-month window holds nothing at all.
 */
data class InsightsUiState(
    val monthLabel: String = "",
    val total: Pence = Pence.ZERO,
    val forecast: Pence = Pence.ZERO,
    val usualMonth: Pence? = null,
    val transactionCount: Int = 0,
    val trend: List<MonthTotal> = emptyList(),
    // Day by day: spend per day of this month (index 0 = the 1st).
    val dailyTotals: List<Pence> = emptyList(),
    val todayIndex: Int = -1,
    // A typical week: the average spend on each weekday (index 0 = Monday).
    val weekdayAverages: List<Pence> = emptyList(),
    val peakWeekdayIndex: Int = -1,
    val rhythm: WeekRhythm? = null,
    // Filter chips for the categories present in the window.
    val chips: List<CategoryChip> = emptyList(),
    // Categories the user set a monthly limit on, fullest first.
    val limits: List<LimitState> = emptyList(),
    // Where the money goes by merchant: the small-and-often view.
    val topMerchants: List<MerchantTotal> = emptyList(),
    val unusual: List<UnusualSpend> = emptyList(),
    val eatingOutMonths: List<EatingOutMonth> = emptyList(),
    val hasData: Boolean = false,
    val loaded: Boolean = false,
)

/**
 * Patterns over the last [TREND_MONTHS] months, re-sliced by a category filter.
 * Computed on the default dispatcher.
 *
 * The window follows the date: [onResume] re-reads it, so opening the app after a
 * month change shows the new month instead of the one first opened.
 */
@HiltViewModel
class InsightsViewModel
    @Inject
    constructor(
        transactionRepository: TransactionRepository,
        categoryRepository: CategoryRepository,
        categoryLimitStore: CategoryLimitStore,
        private val clock: Clock,
        @DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        private val timeZone = TimeZone.currentSystemDefault()
        private val excludedCategoryIds = MutableStateFlow<Set<String?>>(emptySet())
        private val today = MutableStateFlow(clock.now().toLocalDateTime(timeZone).date)

        /** Include/exclude a category from the analysis. */
        fun toggleCategory(categoryId: String?) {
            excludedCategoryIds.update { current ->
                if (categoryId in current) current - categoryId else current + categoryId
            }
        }

        /** Re-reads the date so the window rolls over when the app is opened after midnight. */
        fun onResume() {
            today.value = clock.now().toLocalDateTime(timeZone).date
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<InsightsUiState> =
            today
                .flatMapLatest { date ->
                    val firstOfMonth = LocalDate(date.year, date.monthNumber, 1)
                    val windowStart = firstOfMonth.minus(TREND_MONTHS - 1, DateTimeUnit.MONTH)
                    val monthStart = firstOfMonth.atStartOfDayIn(timeZone)
                    val monthEnd = firstOfMonth.plus(1, DateTimeUnit.MONTH).atStartOfDayIn(timeZone)
                    val monthName = date.month.name.lowercase().replaceFirstChar { it.uppercase() }

                    combine(
                        transactionRepository.observeBetween(windowStart.atStartOfDayIn(timeZone), monthEnd),
                        categoryRepository.observeAll(),
                        excludedCategoryIds,
                        categoryLimitStore.limits,
                    ) { window, categories, excluded, limits ->
                        // Chips reflect every category in the window, ordered by total spend
                        // (unfiltered) so toggling one off doesn't reorder them.
                        val chips =
                            categoryBreakdown(window, categories).map { spend ->
                                CategoryChip(spend.categoryId, spend.name, spend.colorIndex, spend.categoryId !in excluded)
                            }

                        val filtered = window.filter { it.categoryId !in excluded }
                        val monthTx = window.filter { it.occurredAt >= monthStart }
                        val filteredMonth = filtered.filter { it.occurredAt >= monthStart }
                        val total = filteredMonth.fold(Pence.ZERO) { acc, t -> acc + t.amount }
                        val trend = monthlyTotals(filtered, timeZone, firstOfMonth)

                        // A typical week only counts days since the owner started logging, so a
                        // young ledger is not diluted by months with no data.
                        val historyStart =
                            window.minOfOrNull { it.occurredAt.toLocalDateTime(timeZone).date } ?: date
                        val weekday =
                            weekdayAverages(filtered, timeZone, from = maxOf(windowStart, historyStart), to = date)

                        InsightsUiState(
                            monthLabel = "$monthName ${date.year}",
                            total = total,
                            forecast = forecastMonthEnd(total, date.dayOfMonth, firstOfMonth.lengthOfMonth()),
                            usualMonth =
                                trend.dropLast(1).filter { it.total.value > 0 }
                                    .takeIf { it.isNotEmpty() }
                                    ?.let { Pence(it.sumOf { month -> month.total.value } / it.size) },
                            transactionCount = filteredMonth.size,
                            trend = trend,
                            dailyTotals = dailyTotals(filteredMonth, timeZone, firstOfMonth),
                            todayIndex = date.dayOfMonth - 1,
                            weekdayAverages = weekday,
                            peakWeekdayIndex = weekday.indices.maxByOrNull { weekday[it].value } ?: -1,
                            rhythm = weekRhythm(weekday),
                            chips = chips,
                            // Limits measure the month as it really is, so they ignore
                            // the category filter (which is only a view of the charts).
                            limits =
                                limitStates(
                                    transactions = monthTx,
                                    categories = categories,
                                    limits = limits,
                                    dayOfMonth = date.dayOfMonth,
                                    daysInMonth = firstOfMonth.lengthOfMonth(),
                                ),
                            topMerchants = merchantTotals(filteredMonth),
                            unusual = unusualSpends(filtered, categories, since = monthStart),
                            eatingOutMonths = eatingOutByMonth(filtered, categories, timeZone, firstOfMonth),
                            hasData = window.isNotEmpty(),
                            loaded = true,
                        )
                    }
                }
                .flowOn(defaultDispatcher)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsUiState())
    }
