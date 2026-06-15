package com.tally.ui.screens.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tally.core.money.Pence
import com.tally.core.time.Clock
import com.tally.domain.repo.CategoryRepository
import com.tally.domain.repo.TransactionRepository
import com.tally.ui.components.TransactionRowUi
import com.tally.ui.model.timeLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

/**
 * State for the Today screen.
 *
 * @property spentToday sum of today's transaction amounts (positive = spend).
 * @property loaded false until the first emission, so the UI can distinguish
 *   "loading" from a genuinely empty day.
 */
data class TodayUiState(
    val spentToday: Pence = Pence.ZERO,
    val items: List<TransactionRowUi> = emptyList(),
    val loaded: Boolean = false,
)

/** Observes today's transactions (joined with categories) and the running total. */
@HiltViewModel
class TodayViewModel
    @Inject
    constructor(
        transactionRepository: TransactionRepository,
        categoryRepository: CategoryRepository,
        clock: Clock,
    ) : ViewModel() {
        private val timeZone = TimeZone.currentSystemDefault()

        val uiState: StateFlow<TodayUiState> =
            run {
                val today = clock.now().toLocalDateTime(timeZone).date
                val start = today.atStartOfDayIn(timeZone)
                val end = today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone)
                combine(
                    transactionRepository.observeBetween(start, end),
                    categoryRepository.observeAll(),
                ) { transactions, categories ->
                    val byId = categories.associateBy { it.id }
                    TodayUiState(
                        spentToday = transactions.fold(Pence.ZERO) { acc, t -> acc + t.amount },
                        items =
                            transactions.map { transaction ->
                                val category = byId[transaction.categoryId]
                                TransactionRowUi(
                                    id = transaction.id,
                                    merchant = transaction.merchant,
                                    amount = transaction.amount,
                                    categoryName = category?.name,
                                    colorIndex = category?.colorIndex,
                                    subtitle = transaction.occurredAt.timeLabel(timeZone),
                                )
                            },
                        loaded = true,
                    )
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())
            }
    }
