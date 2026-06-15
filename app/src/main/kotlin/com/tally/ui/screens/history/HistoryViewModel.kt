package com.tally.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tally.core.money.Pence
import com.tally.core.time.Clock
import com.tally.domain.repo.CategoryRepository
import com.tally.domain.repo.TransactionRepository
import com.tally.ui.components.TransactionRowUi
import com.tally.ui.model.dayLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

/** One day's worth of transactions in the History list. */
data class HistoryDay(
    val label: String,
    val total: Pence,
    val items: List<TransactionRowUi>,
)

data class HistoryUiState(
    val days: List<HistoryDay> = emptyList(),
    val loaded: Boolean = false,
)

/** Observes all transactions, grouped by day (newest first), joined with categories. */
@HiltViewModel
class HistoryViewModel
    @Inject
    constructor(
        transactionRepository: TransactionRepository,
        categoryRepository: CategoryRepository,
        clock: Clock,
    ) : ViewModel() {
        private val timeZone = TimeZone.currentSystemDefault()
        private val today = clock.now().toLocalDateTime(timeZone).date

        val uiState: StateFlow<HistoryUiState> =
            combine(
                transactionRepository.observeAll(),
                categoryRepository.observeAll(),
            ) { transactions, categories ->
                val byId = categories.associateBy { it.id }
                val days =
                    transactions
                        .groupBy { it.occurredAt.toLocalDateTime(timeZone).date }
                        .toSortedMap(compareByDescending { it })
                        .map { (date, dayTransactions) ->
                            HistoryDay(
                                label = dayLabel(date, today),
                                total = dayTransactions.fold(Pence.ZERO) { acc, t -> acc + t.amount },
                                items =
                                    dayTransactions.map { transaction ->
                                        val category = byId[transaction.categoryId]
                                        TransactionRowUi(
                                            id = transaction.id,
                                            merchant = transaction.merchant,
                                            amount = transaction.amount,
                                            categoryName = category?.name,
                                            colorIndex = category?.colorIndex,
                                            subtitle = category?.name ?: "Uncategorised",
                                        )
                                    },
                            )
                        }
                HistoryUiState(days = days, loaded = true)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())
    }
