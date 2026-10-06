package com.tally.ui.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tally.core.money.Pence
import com.tally.core.time.Clock
import com.tally.domain.repo.CategoryRepository
import com.tally.domain.repo.TransactionRepository
import com.tally.domain.report.MonthlyReport
import com.tally.domain.report.monthLabel
import com.tally.domain.report.monthlyReport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

data class ReportsUiState(
    val loaded: Boolean = false,
    val report: MonthlyReport? = null,
    val previousMonthLabel: String = "",
    val observation: String = "",
    val canGoForward: Boolean = false,
)

/**
 * Backs the Reports tab: a selectable month's [MonthlyReport], newest first.
 * Reads the whole ledger and slices in-memory (single-user data is small), so a
 * month change is a plain state update rather than a new query.
 *
 * [onResume] re-reads the date. The month being viewed stays put, but "next"
 * becomes available once a new month has started.
 */
@HiltViewModel
class ReportsViewModel
    @Inject
    constructor(
        transactionRepository: TransactionRepository,
        categoryRepository: CategoryRepository,
        private val clock: Clock,
    ) : ViewModel() {
        private val timeZone = TimeZone.currentSystemDefault()
        private val currentMonth = MutableStateFlow(thisMonth())
        private val selectedMonth = MutableStateFlow(currentMonth.value)

        val uiState: StateFlow<ReportsUiState> =
            combine(
                transactionRepository.observeAll(),
                categoryRepository.observeAll(),
                selectedMonth,
                currentMonth,
            ) { all, categories, first, latest ->
                val monthTx = all.filter { inMonth(it.occurredAt, first) }
                val prevFirst = first.minus(1, DateTimeUnit.MONTH)
                val prevTotal = Pence(all.filter { inMonth(it.occurredAt, prevFirst) }.sumOf { it.amount.value })
                val report = monthlyReport(monthTx, categories, prevTotal, first, timeZone)
                ReportsUiState(
                    loaded = true,
                    report = report,
                    previousMonthLabel = monthLabel(prevFirst),
                    observation = observationFor(report),
                    canGoForward = first < latest,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsUiState())

        fun previousMonth() = selectedMonth.update { it.minus(1, DateTimeUnit.MONTH) }

        fun nextMonth() = selectedMonth.update { if (it < currentMonth.value) it.plus(1, DateTimeUnit.MONTH) else it }

        /** Re-reads the date, so the next month can be opened once it has started. */
        fun onResume() {
            currentMonth.value = thisMonth()
        }

        private fun thisMonth(): LocalDate = clock.now().toLocalDateTime(timeZone).date.let { LocalDate(it.year, it.monthNumber, 1) }

        private fun inMonth(
            instant: Instant,
            first: LocalDate,
        ): Boolean {
            val date = instant.toLocalDateTime(timeZone).date
            return date.year == first.year && date.monthNumber == first.monthNumber
        }

        private fun observationFor(report: MonthlyReport): String {
            if (report.isEmpty) return ""
            val parts = mutableListOf<String>()
            report.byCategory.firstOrNull()?.let { parts += "${it.name} took the biggest share" }
            if (report.autoCapturedPercent > 0) parts += "${report.autoCapturedPercent}% of entries logged themselves"
            return parts.joinToString(". ").let { if (it.isEmpty()) "" else "$it." }
        }
    }
