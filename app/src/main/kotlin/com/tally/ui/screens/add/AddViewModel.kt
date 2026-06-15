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
import javax.inject.Inject

private const val MAX_PENCE = 9_999_999_99L // £9,999,999.99 cap

/**
 * State for the Add sheet.
 *
 * The amount is a running pence accumulator driven by the keypad (digits fill
 * from the right). [canSave] gates the Save button: a positive amount, a
 * non-blank merchant, and not already saving.
 *
 * [categoryManuallySet] tracks whether the user has overridden the auto-
 * suggested category, so the categoriser doesn't keep clobbering their choice.
 */
data class AddUiState(
    val amountPence: Long = 0L,
    val merchant: String = "",
    val note: String = "",
    val selectedCategoryId: String? = null,
    val categories: List<Category> = emptyList(),
    val categoryManuallySet: Boolean = false,
    val saving: Boolean = false,
) {
    val amount: Pence get() = Pence(amountPence)
    val canSave: Boolean get() = amountPence > 0 && merchant.isNotBlank() && !saving
}

/**
 * Drives the Add bottom sheet. The categoriser auto-suggests a category from the
 * merchant (learned rules first, then bundled — see `suggestCategory`); the user
 * can override, and a saved transaction teaches the merchant→category mapping.
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

        fun save(onSaved: () -> Unit) {
            val state = _uiState.value
            if (!state.canSave) return
            _uiState.update { it.copy(saving = true) }
            viewModelScope.launch {
                val merchant = state.merchant.trim()
                val categoryId = state.selectedCategoryId
                transactionRepository.add(
                    NewTransaction(
                        amount = state.amount,
                        merchant = merchant,
                        occurredAt = clock.now(),
                        note = state.note.trim().ifBlank { null },
                        categoryId = categoryId,
                        source = TransactionSource.MANUAL,
                    ),
                )
                if (categoryId != null && merchant.isNotBlank()) {
                    merchantRuleStore.learn(normaliseMerchant(merchant), categoryId)
                }
                _uiState.value =
                    AddUiState(
                        categories = state.categories,
                        selectedCategoryId = state.categories.firstOrNull()?.id,
                    )
                onSaved()
            }
        }
    }
