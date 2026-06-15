package com.tally.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tally.core.money.Pence
import com.tally.core.time.Clock
import com.tally.di.DefaultDispatcher
import com.tally.domain.insight.CategorySpend
import com.tally.domain.insight.categoryBreakdown
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
    val breakdown: List<CategorySpend> = emptyList(),
    val loaded: Boolean = false,
)

/** This month's spending total and per-category breakdown for the Insights screen. */
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
                    InsightsUiState(
                        monthLabel = "$monthName ${date.year}",
                        total = transactions.fold(Pence.ZERO) { acc, t -> acc + t.amount },
                        breakdown = categoryBreakdown(transactions, categories),
                        loaded = true,
                    )
                }.flowOn(defaultDispatcher)
                    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsUiState())
            }
    }
