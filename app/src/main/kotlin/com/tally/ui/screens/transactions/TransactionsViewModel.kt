package com.tally.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tally.core.money.Pence
import com.tally.core.time.Clock
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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

/**
 * One day's worth of transactions in the list.
 *
 * @property key stable id for the day (its ISO date), so the list can animate.
 * @property total the full amount paid that day. Cash flow ignores any shares.
 */
data class TransactionDay(
    val key: String,
    val label: String,
    val total: Pence,
    val items: List<TransactionRowUi>,
)

data class TransactionsUiState(
    val days: List<TransactionDay> = emptyList(),
    val loaded: Boolean = false,
)

/** Observes all transactions, grouped by day (newest first), joined with categories. */
@HiltViewModel
class TransactionsViewModel
    @Inject
    constructor(
        transactionRepository: TransactionRepository,
        categoryRepository: CategoryRepository,
        private val clock: Clock,
    ) : ViewModel() {
        private val timeZone = TimeZone.currentSystemDefault()
        private val today = MutableStateFlow(clock.now().toLocalDateTime(timeZone).date)

        /** Re-reads the date, so "Today" and "Yesterday" stay right after midnight. */
        fun onResume() {
            today.value = clock.now().toLocalDateTime(timeZone).date
        }

        val uiState: StateFlow<TransactionsUiState> =
            combine(
                transactionRepository.observeAll(),
                categoryRepository.observeAll(),
                today,
            ) { transactions, categories, todayDate ->
                val byId = categories.associateBy { it.id }
                val days =
                    transactions
                        .groupBy { it.occurredAt.toLocalDateTime(timeZone).date }
                        .toSortedMap(compareByDescending { it })
                        .map { (date, dayTransactions) ->
                            TransactionDay(
                                key = date.toString(),
                                label = dayLabel(date, todayDate),
                                total = dayTransactions.fold(Pence.ZERO) { acc, t -> acc + t.amount },
                                items =
                                    dayTransactions.map { transaction ->
                                        val category = byId[transaction.categoryId]
                                        transaction.toRowUi(
                                            category = category,
                                            subtitle =
                                                "${category?.name ?: "Uncategorised"} · " +
                                                    transaction.occurredAt.timeLabel(timeZone),
                                        )
                                    },
                            )
                        }
                TransactionsUiState(days = days, loaded = true)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState())
    }
