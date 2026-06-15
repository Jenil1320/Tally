package com.tally.ui.screens.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tally.core.money.Pence
import com.tally.core.time.Clock
import com.tally.domain.model.Category
import com.tally.domain.model.NewTransaction
import com.tally.domain.model.TransactionSource
import com.tally.domain.repo.CategoryRepository
import com.tally.domain.repo.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val MAX_PENCE = 9_999_999_99L // £9,999,999.99 cap

/**
 * State for the Add sheet.
 *
 * The amount is a running pence accumulator driven by the keypad (digits fill
 * from the right). [canSave] gates the Save button: a positive amount, a
 * non-blank merchant, and not already saving.
 */
data class AddUiState(
    val amountPence: Long = 0L,
    val merchant: String = "",
    val note: String = "",
    val selectedCategoryId: String? = null,
    val categories: List<Category> = emptyList(),
    val saving: Boolean = false,
) {
    val amount: Pence get() = Pence(amountPence)
    val canSave: Boolean get() = amountPence > 0 && merchant.isNotBlank() && !saving
}

/**
 * Drives the Add bottom sheet. Manual categorisation only in M2 — the
 * merchant→category engine lands in M4 (see ADR roadmap / DECISIONS).
 *
 * State transitions: keypad taps mutate [AddUiState.amountPence]; field setters
 * mutate their fields; [save] writes a [NewTransaction] then resets the form
 * (keeping the loaded categories) and signals completion via the callback.
 */
@HiltViewModel
class AddViewModel
    @Inject
    constructor(
        private val transactionRepository: TransactionRepository,
        categoryRepository: CategoryRepository,
        private val clock: Clock,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AddUiState())
        val uiState = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                categoryRepository.observeAll().collect { categories ->
                    _uiState.update { state ->
                        state.copy(
                            categories = categories,
                            selectedCategoryId = state.selectedCategoryId ?: categories.firstOrNull()?.id,
                        )
                    }
                }
            }
        }

        fun onDigit(digit: Int) {
            _uiState.update { it.copy(amountPence = (it.amountPence * 10 + digit).coerceAtMost(MAX_PENCE)) }
        }

        fun onDoubleZero() {
            _uiState.update { it.copy(amountPence = (it.amountPence * 100).coerceAtMost(MAX_PENCE)) }
        }

        fun onBackspace() {
            _uiState.update { it.copy(amountPence = it.amountPence / 10) }
        }

        fun onMerchantChange(value: String) {
            _uiState.update { it.copy(merchant = value) }
        }

        fun onNoteChange(value: String) {
            _uiState.update { it.copy(note = value) }
        }

        fun onCategorySelected(categoryId: String) {
            _uiState.update { it.copy(selectedCategoryId = categoryId) }
        }

        fun save(onSaved: () -> Unit) {
            val state = _uiState.value
            if (!state.canSave) return
            _uiState.update { it.copy(saving = true) }
            viewModelScope.launch {
                transactionRepository.add(
                    NewTransaction(
                        amount = state.amount,
                        merchant = state.merchant.trim(),
                        occurredAt = clock.now(),
                        note = state.note.trim().ifBlank { null },
                        categoryId = state.selectedCategoryId,
                        source = TransactionSource.MANUAL,
                    ),
                )
                _uiState.value =
                    AddUiState(
                        categories = state.categories,
                        selectedCategoryId = state.categories.firstOrNull()?.id,
                    )
                onSaved()
            }
        }
    }
