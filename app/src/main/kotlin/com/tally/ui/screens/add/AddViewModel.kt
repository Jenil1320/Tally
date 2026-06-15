package com.tally.ui.screens.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tally.core.money.Pence
import com.tally.core.time.Clock
import com.tally.domain.category.MerchantRuleStore
import com.tally.domain.category.normaliseMerchant
import com.tally.domain.category.suggestCategory
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
import kotlinx.datetime.Instant
import javax.inject.Inject

private const val MAX_PENCE = 9_999_999_99L // £9,999,999.99 cap

/**
 * State for the Add/Edit sheet.
 *
 * The amount is a running pence accumulator driven by the keypad. [canSave]
 * gates Save: a positive amount, a non-blank merchant, and not already saving.
 * [editingId] is non-null when editing an existing transaction (vs adding a new
 * one), which also reveals the Delete and Duplicate actions.
 */
data class AddUiState(
    val amountPence: Long = 0L,
    val merchant: String = "",
    val note: String = "",
    val selectedCategoryId: String? = null,
    val categories: List<Category> = emptyList(),
    val categoryManuallySet: Boolean = false,
    val editingId: String? = null,
    val saving: Boolean = false,
) {
    val amount: Pence get() = Pence(amountPence)
    val isEditing: Boolean get() = editingId != null
    val canSave: Boolean get() = amountPence > 0 && merchant.isNotBlank() && !saving
}

/**
 * Drives the Add/Edit sheet. New entries auto-suggest a category from the
 * merchant (and learn the choice on save). Editing loads the transaction,
 * preserves its original date, and supports delete (soft) and duplicate.
 */
@HiltViewModel
class AddViewModel
    @Inject
    constructor(
        private val transactionRepository: TransactionRepository,
        categoryRepository: CategoryRepository,
        private val merchantRuleStore: MerchantRuleStore,
        private val clock: Clock,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AddUiState())
        val uiState = _uiState.asStateFlow()

        private var learnedRules: Map<String, String> = emptyMap()

        /** Original date of the transaction being edited, preserved on update. */
        private var editingOccurredAt: Instant? = null

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
            viewModelScope.launch {
                merchantRuleStore.rules.collect { learnedRules = it }
            }
        }

        /** Initialise the sheet: a fresh entry when [editingId] is null, else load it. */
        fun start(editingId: String?) {
            if (editingId == null) {
                editingOccurredAt = null
                _uiState.update {
                    AddUiState(categories = it.categories, selectedCategoryId = it.categories.firstOrNull()?.id)
                }
                return
            }
            viewModelScope.launch {
                val transaction = transactionRepository.findById(editingId) ?: return@launch
                editingOccurredAt = transaction.occurredAt
                _uiState.update { state ->
                    state.copy(
                        amountPence = transaction.amount.value,
                        merchant = transaction.merchant,
                        note = transaction.note.orEmpty(),
                        selectedCategoryId = transaction.categoryId ?: state.categories.firstOrNull()?.id,
                        categoryManuallySet = true,
                        editingId = editingId,
                        saving = false,
                    )
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
            _uiState.update { state ->
                val suggestion =
                    if (state.categoryManuallySet) {
                        state.selectedCategoryId
                    } else {
                        suggestCategory(value, state.categories, learnedRules)
                            ?: state.categories.firstOrNull()?.id
                    }
                state.copy(merchant = value, selectedCategoryId = suggestion)
            }
        }

        fun onNoteChange(value: String) {
            _uiState.update { it.copy(note = value) }
        }

        fun onCategorySelected(categoryId: String) {
            _uiState.update { it.copy(selectedCategoryId = categoryId, categoryManuallySet = true) }
        }

        /** Save: update the existing transaction when editing, otherwise add a new one. */
        fun save(onSaved: () -> Unit) {
            val state = _uiState.value
            if (!state.canSave) return
            _uiState.update { it.copy(saving = true) }
            viewModelScope.launch {
                val merchant = state.merchant.trim()
                val categoryId = state.selectedCategoryId
                val draft =
                    NewTransaction(
                        amount = state.amount,
                        merchant = merchant,
                        occurredAt = editingOccurredAt ?: clock.now(),
                        note = state.note.trim().ifBlank { null },
                        categoryId = categoryId,
                        source = TransactionSource.MANUAL,
                    )
                val editingId = state.editingId
                if (editingId != null) {
                    transactionRepository.update(editingId, draft)
                } else {
                    transactionRepository.add(draft)
                }
                if (categoryId != null && merchant.isNotBlank()) {
                    merchantRuleStore.learn(normaliseMerchant(merchant), categoryId)
                }
                resetForm()
                onSaved()
            }
        }

        /** Soft-delete the transaction being edited. */
        fun delete(onDone: () -> Unit) {
            val editingId = _uiState.value.editingId ?: return
            viewModelScope.launch {
                transactionRepository.softDelete(editingId)
                resetForm()
                onDone()
            }
        }

        /** Create a fresh copy of the current entry, dated now. */
        fun duplicate(onDone: () -> Unit) {
            val state = _uiState.value
            val merchant = state.merchant.trim()
            if (state.amountPence <= 0 || merchant.isBlank()) return
            viewModelScope.launch {
                transactionRepository.add(
                    NewTransaction(
                        amount = state.amount,
                        merchant = merchant,
                        occurredAt = clock.now(),
                        note = state.note.trim().ifBlank { null },
                        categoryId = state.selectedCategoryId,
                        source = TransactionSource.MANUAL,
                    ),
                )
                resetForm()
                onDone()
            }
        }

        private fun resetForm() {
            editingOccurredAt = null
            _uiState.value =
                AddUiState(
                    categories = _uiState.value.categories,
                    selectedCategoryId = _uiState.value.categories.firstOrNull()?.id,
                )
        }
    }
